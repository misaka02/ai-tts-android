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
import com.aitts.engine.data.SegmentRole
import com.aitts.engine.data.SentenceSegment
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.network.SharedHttpClient
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
 * 3. 智能多角色双音色协同播报（旁白沉稳叙述 + 对话灵动角色音）
 * 4. PCM 解码、硬件增益调节与 SynthesisCallback 流式推流
 */
class TtsSynthesizer(private val context: Context) {

    private val configDataStore = ConfigDataStore.getInstance(context)
    private val audioCacheManager = AudioCacheManager.getInstance(context)
    private val providerManager = TtsProviderManager.getInstance()

    private val isStopped = AtomicBoolean(false)

    fun stop() {
        isStopped.set(true)
        SharedHttpClient.cancelAll()
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

        // 2. 智能多角色长句切分 (识别对话与旁白)
        val segments: List<SentenceSegment> = if (settings.isSentenceSplittingEnabled) {
            SentenceSplitter.splitTextWithRoles(preprocessedText, settings.maxSentenceLength)
        } else {
            listOf(SentenceSegment(preprocessedText, SegmentRole.NARRATOR))
        }

        if (segments.isEmpty()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        configDataStore.log("开始合成任务 [${mergedConfig.name}]: ${segments.size} 句, 首句: \"${segments.first().text.take(20)}...\"")

        var callbackInitialized = false
        val bufferChunkSize = 8192

        // 本次合成会话专属内存预取缓存（即使磁盘缓存关闭也能确保流畅无缝）
        val sessionCache = ConcurrentHashMap<Int, Deferred<Result<ByteArray>>>()

        fun getConfigForSegment(segment: SentenceSegment): TtsProviderConfig {
            return if (mergedConfig.isDualRoleEnabled && segment.role == SegmentRole.DIALOGUE && mergedConfig.dialogueVoiceId.isNotBlank()) {
                mergedConfig.copy(voiceId = mergedConfig.dialogueVoiceId)
            } else {
                mergedConfig
            }
        }

        try {
            // 预先启动首句及第二句的并发拉取
            for (lookAhead in 0 until minOf(2, segments.size)) {
                val seg = segments[lookAhead]
                val segConfig = getConfigForSegment(seg)
                sessionCache[lookAhead] = async(Dispatchers.IO) {
                    fetchOrSynthesizeAudio(seg.text, segConfig, settings)
                }
            }

            for (i in segments.indices) {
                if (isStopped.get() || !isActive) {
                    configDataStore.log("合成任务已中断")
                    return@withContext
                }

                // 随着进度推进，自动向前预拉取第 i+2 句
                val nextPrefetchIndex = i + 2
                if (nextPrefetchIndex < segments.size && !sessionCache.containsKey(nextPrefetchIndex)) {
                    val nextSeg = segments[nextPrefetchIndex]
                    val nextSegConfig = getConfigForSegment(nextSeg)
                    sessionCache[nextPrefetchIndex] = async(Dispatchers.IO) {
                        fetchOrSynthesizeAudio(nextSeg.text, nextSegConfig, settings)
                    }
                }

                // 取出当前句的异步任务并等待结果
                val currentDeferred = sessionCache[i] ?: async(Dispatchers.IO) {
                    val seg = segments[i]
                    fetchOrSynthesizeAudio(seg.text, getConfigForSegment(seg), settings)
                }

                val audioResult = currentDeferred.await()

                if (audioResult.isFailure) {
                    val err = audioResult.exceptionOrNull()?.message ?: "未知合成错误"
                    configDataStore.log("第 ${i + 1}/${segments.size} 句合成失败: $err")
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
                configDataStore.log("合成任务完成全部 ${segments.size} 句推流")
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
        } finally {
            sessionCache.values.forEach { it.cancel() }
            sessionCache.clear()
        }
    }

    /**
     * 优先从磁盘缓存读取，未命中则调用 Provider 进行远程网络合成，并写入缓存（带自动重试）
     */
    private suspend fun fetchOrSynthesizeAudio(
        text: String,
        config: TtsProviderConfig,
        settings: GlobalSettings
    ): Result<ByteArray> {
        // 1. 尝试从本地缓存读取
        if (settings.isAudioCacheEnabled) {
            val cachedData = audioCacheManager.getAudio(text, config)
            if (cachedData != null && cachedData.isNotEmpty()) {
                return Result.success(cachedData)
            }
        }

        // 2. 网络合成与智能重试机制 (最多尝试 2 次)
        var lastError: Throwable? = null
        for (attempt in 1..2) {
            if (isStopped.get()) {
                return Result.failure(CancellationException("已中断"))
            }

            val result = providerManager.synthesize(text, config)
            if (result.isSuccess) {
                val audioBytes = result.getOrNull() ?: ByteArray(0)
                if (audioBytes.isNotEmpty() && settings.isAudioCacheEnabled) {
                    audioCacheManager.saveAudio(text, config, audioBytes)
                }
                return Result.success(audioBytes)
            } else {
                lastError = result.exceptionOrNull()
                if (attempt < 2) {
                    kotlinx.coroutines.delay(200)
                }
            }
        }

        return Result.failure(lastError ?: Exception("合成失败"))
    }

    /**
     * 线性 PCM 音量增益调节（带防爆音削峰处理）
     */
    private fun applyPcmVolume(pcm: ByteArray, volume: Float): ByteArray {
        if (pcm.isEmpty() || volume == 1.0f) return pcm

        val output = ByteArray(pcm.size)
        var i = 0
        while (i < pcm.size - 1) {
            val low = pcm[i].toInt() and 0xFF
            val high = pcm[i + 1].toInt()
            var sample = (high shl 8) or low

            var newSample = (sample * volume).toInt()
            if (newSample > Short.MAX_VALUE) newSample = Short.MAX_VALUE.toInt()
            if (newSample < Short.MIN_VALUE) newSample = Short.MIN_VALUE.toInt()

            output[i] = (newSample and 0xFF).toByte()
            output[i + 1] = ((newSample shr 8) and 0xFF).toByte()
            i += 2
        }
        return output
    }
}
