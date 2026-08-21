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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 语音合成调度器：
 * 负责分句切分、规则替换、缓存读写、网络并发预拉取、PCM 解码与 SynthesisCallback 推流。
 */
class TtsSynthesizer(private val context: Context) {

    private val configDataStore = ConfigDataStore.getInstance(context)
    private val audioCacheManager = AudioCacheManager.getInstance(context)
    private val providerManager = TtsProviderManager.getInstance()

    private val isStopped = AtomicBoolean(false)
    private var currentJob: Job? = null

    fun stop() {
        isStopped.set(true)
        currentJob?.cancel()
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

        // 1. 文本预处理与正则替换
        val preprocessedText = TextPreprocessor.process(rawText, rules)
        if (preprocessedText.isBlank()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        // 2. 智能分句
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

        try {
            // 预加载队列机制：当前在播第 i 句时，后台并发预拉取第 i+1 句
            for (i in sentences.indices) {
                if (isStopped.get() || !isActive) {
                    configDataStore.log("合成任务已中断")
                    return@withContext
                }

                val sentence = sentences[i]
                val nextSentence = if (i + 1 < sentences.size) sentences[i + 1] else null

                // 异步预拉取下一句
                val prefetchJob = nextSentence?.let { next ->
                    async(Dispatchers.IO) {
                        fetchOrSynthesizeAudio(next, mergedConfig, settings)
                    }
                }

                // 获取并解码当前句音频
                val audioResult = fetchOrSynthesizeAudio(sentence, mergedConfig, settings)

                if (audioResult.isFailure) {
                    val err = audioResult.exceptionOrNull()?.message ?: "未知合成错误"
                    configDataStore.log("分句合成失败: $err")
                    if (!callbackInitialized) {
                        callback.error()
                        return@withContext
                    }
                    continue
                }

                val rawAudioBytes = audioResult.getOrNull() ?: ByteArray(0)
                if (rawAudioBytes.isEmpty()) continue

                // PCM 解码
                val decoded = AudioDecoder.decodeToPcm(rawAudioBytes, mergedConfig.sampleRate)
                if (decoded.pcmData.isEmpty()) continue

                if (!callbackInitialized) {
                    val startStatus = callback.start(
                        decoded.sampleRate,
                        AudioFormat.ENCODING_PCM_16BIT,
                        decoded.channelCount
                    )
                    if (startStatus != 0) { // TextToSpeech.SUCCESS is 0
                        configDataStore.log("SynthesisCallback.start 返回非成功状态: $startStatus")
                    }
                    callbackInitialized = true
                }

                // 流式向系统管道写入 PCM 块
                val pcmData = decoded.pcmData
                var offset = 0
                while (offset < pcmData.size) {
                    if (isStopped.get() || !isActive) {
                        return@withContext
                    }
                    val length = Math.min(bufferChunkSize, pcmData.size - offset)
                    callback.audioAvailable(pcmData, offset, length)
                    offset += length
                }

                // 等待预取任务完成
                prefetchJob?.await()
            }

            if (callbackInitialized) {
                callback.done()
                configDataStore.log("合成任务完成推流")
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

        val netResult = providerManager.synthesize(text, config)
        if (netResult.isSuccess) {
            val data = netResult.getOrNull()
            if (data != null && data.isNotEmpty() && settings.isAudioCacheEnabled) {
                audioCacheManager.put(cacheKey, data, settings.maxCacheSizeMb)
            }
        }

        return netResult
    }
}
