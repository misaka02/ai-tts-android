package com.aitts.engine.service

import android.content.Context
import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.util.Log
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.audio.AudioDecoder
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.GlobalSettings
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.SentenceSplitter
import com.aitts.engine.rules.TextPreprocessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 语音合成调度器：
 * 1. 负责分句切分、规则替换、缓存读写
 * 2. 独创双句并发滑动窗口预加载（Zero-Gap Prefetch Pipeline），彻底消除句间网络停顿
 * 3. PCM 解码、硬件增益调节与 SynthesisCallback 流式推流
 */
class TtsSynthesizer(private val context: Context) {

    private val configDataStore = ConfigDataStore.getInstance(context)
    private val audioCacheManager = AudioCacheManager.getInstance(context)
    private val providerManager = TtsProviderManager.getInstance()

    private val isStopped = AtomicBoolean(false)

    fun stop() {
        isStopped.set(true)
        com.aitts.engine.network.SharedHttpClient.cancelAll()
    }

    /**
     * 处理 Android 系统 TextToSpeechService 发起的合成请求
     */
    suspend fun processSynthesisRequest(
        request: SynthesisRequest,
        callback: SynthesisCallback
    ) = withContext(Dispatchers.IO) {
        isStopped.set(false)
        val rawText = request.charSequenceText?.toString() ?: ""
        if (rawText.isBlank()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        val settings = configDataStore.settingsFlow.value
        val providerConfig = configDataStore.getActiveProvider()
        val rules = configDataStore.rulesFlow.value

        // 适配系统传入的语速与音调参数（100 为标准 1.0）
        val systemSpeed = request.speechRate / 100.0f
        val systemPitch = request.pitch / 100.0f
        val effectiveSpeed = (providerConfig.speed * systemSpeed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
        val effectivePitch = (providerConfig.pitch * systemPitch * settings.globalPitch).coerceIn(0.2f, 2.0f)

        val mergedConfig = providerConfig.copy(
            speed = effectiveSpeed,
            pitch = effectivePitch
        )

        // 1. 文本预处理与网页/Markdown/多音字清洗
        val preprocessedText = TextPreprocessor.process(rawText, rules, settings.isNumberNormalizationEnabled)
        if (preprocessedText.isBlank()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        // 2. 智能长句切分
        val sentences = if (settings.isSentenceSplittingEnabled) {
            SentenceSplitter.splitText(preprocessedText, settings.maxSentenceLength)
        } else {
            listOf(preprocessedText)
        }

        if (sentences.isEmpty()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        configDataStore.log("开始合成任务 [${mergedConfig.name}]: ${sentences.size} 句, 首句: \"${sentences.first().take(20)}...\"")

        var callbackInitialized = false
        val bufferChunkSize = 8192

        // 本次合成会话专属内存预取缓存（即使磁盘缓存关闭也能确保流畅无缝）
        val sessionCache = ConcurrentHashMap<Int, Deferred<Result<ByteArray>>>()

        try {
            // 预先启动首句及第二句的并发拉取
            for (lookAhead in 0 until minOf(2, sentences.size)) {
                val sentence = sentences[lookAhead]
                sessionCache[lookAhead] = async(Dispatchers.IO) {
                    fetchOrSynthesizeAudio(sentence, mergedConfig, settings)
                }
            }

            for (i in sentences.indices) {
                if (isStopped.get() || !isActive) {
                    configDataStore.log("合成任务已中断")
                    return@withContext
                }

                // 随着进度推进，自动向前预拉取第 i+2 句
                val nextPrefetchIndex = i + 2
                if (nextPrefetchIndex < sentences.size && !sessionCache.containsKey(nextPrefetchIndex)) {
                    val nextSentence = sentences[nextPrefetchIndex]
                    sessionCache[nextPrefetchIndex] = async(Dispatchers.IO) {
                        fetchOrSynthesizeAudio(nextSentence, mergedConfig, settings)
                    }
                }

                // 取出当前句的异步任务并等待结果
                val currentDeferred = sessionCache[i] ?: async(Dispatchers.IO) {
                    fetchOrSynthesizeAudio(sentences[i], mergedConfig, settings)
                }

                val audioResult = currentDeferred.await()

                if (audioResult.isFailure) {
                    val err = audioResult.exceptionOrNull()?.message ?: "未知合成错误"
                    configDataStore.log("第 ${i + 1}/${sentences.size} 句合成失败: $err")
                    if (!callbackInitialized) {
                        callback.error()
                        return@withContext
                    }
                    continue
                }

                val rawAudioBytes = audioResult.getOrNull() ?: ByteArray(0)
                if (rawAudioBytes.isEmpty()) continue

                // PCM 解码与音量增益处理
                val decoded = AudioDecoder.decodeToPcm(rawAudioBytes, mergedConfig.sampleRate)
                if (decoded.pcmData.isEmpty()) continue

                // 应用音量增益
                val finalPcm = if (mergedConfig.volume != 1.0f) {
                    applyPcmVolume(decoded.pcmData, mergedConfig.volume)
                } else {
                    decoded.pcmData
                }

                if (!callbackInitialized) {
                    val startStatus = callback.start(
                        decoded.sampleRate,
                        AudioFormat.ENCODING_PCM_16BIT,
                        decoded.channelCount
                    )
                    if (startStatus != 0) {
                        configDataStore.log("SynthesisCallback.start 返回状态: $startStatus")
                    }
                    callbackInitialized = true
                }

                // 流式向系统音频管道写入 PCM 块
                var offset = 0
                while (offset < finalPcm.size) {
                    if (isStopped.get() || !isActive) {
                        return@withContext
                    }
                    val length = Math.min(bufferChunkSize, finalPcm.size - offset)
                    callback.audioAvailable(finalPcm, offset, length)
                    offset += length
                }

                // 及时从会话缓存中移除已推流完成的句子，极大释放内存占用
                sessionCache.remove(i)
            }

            if (callbackInitialized) {
                callback.done()
                configDataStore.log("合成任务完成全部 ${sentences.size} 句推流")
            } else {
                callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
                callback.done()
            }
        } catch (e: CancellationException) {
            configDataStore.log("合成已被取消")
        } catch (e: Exception) {
            configDataStore.log("合成过程发生异常: ${e.message}")
            if (!callbackInitialized) {
                callback.error()
            }
        }
    }

    /**
     * 优先命中磁盘缓存，未命中则请求网络并回填缓存
     */
    private suspend fun fetchOrSynthesizeAudio(
        text: String,
        config: TtsProviderConfig,
        settings: GlobalSettings
    ): Result<ByteArray> {
        val cacheKey = audioCacheManager.generateKey(
            providerId = config.id,
            voiceId = config.voiceId,
            speed = config.speed,
            pitch = config.pitch,
            text = text
        )

        if (settings.isAudioCacheEnabled) {
            val cachedData = audioCacheManager.get(cacheKey)
            if (cachedData != null && cachedData.isNotEmpty()) {
                return Result.success(cachedData)
            }
        }

        // 网络请求（内置重试机制）
        var lastResult: Result<ByteArray>? = null
        for (attempt in 0..1) {
            val netResult = providerManager.synthesize(text, config)
            if (netResult.isSuccess) {
                val data = netResult.getOrNull()
                if (data != null && data.isNotEmpty() && settings.isAudioCacheEnabled) {
                    audioCacheManager.put(cacheKey, data, settings.maxCacheSizeMb)
                }
                return netResult
            }
            lastResult = netResult
            if (attempt == 0) {
                kotlinx.coroutines.delay(200)
            }
        }

        return lastResult ?: Result.failure(Exception("合成网络请求异常"))
    }

    /**
     * 对 16-bit PCM 字节流进行线性音量增益与防爆音裁剪
     */
    private fun applyPcmVolume(pcm: ByteArray, volume: Float): ByteArray {
        if (volume == 1.0f || pcm.isEmpty()) return pcm
        val result = ByteArray(pcm.size)
        var i = 0
        while (i + 1 < pcm.size) {
            val sample = (pcm[i].toInt() and 0xFF) or (pcm[i + 1].toInt() shl 8)
            val sampleShort = sample.toShort()
            val newSample = (sampleShort * volume).toInt().coerceIn(-32768, 32767).toShort()
            result[i] = (newSample.toInt() and 0xFF).toByte()
            result[i + 1] = ((newSample.toInt() shr 8) and 0xFF).toByte()
            i += 2
        }
        return result
    }
}
