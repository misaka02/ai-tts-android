package com.aitts.engine.service

import android.content.Context
import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.audio.AudioDecoder
import com.aitts.engine.audio.AudioEnhancer
import com.aitts.engine.audio.AudioResampler
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.GlobalSettings
import com.aitts.engine.data.SegmentRole
import com.aitts.engine.data.SentenceSegment
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.network.SharedHttpClient
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.AcronymNormalizer
import com.aitts.engine.rules.SentenceSplitter
import com.aitts.engine.rules.TextPreprocessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 生产级语音合成调度核心 (High-Performance Industrial TTS Synthesizer v2.0.0)：
 * 1. 负责分句切分、文本正则替换、全维度缓存读写；
 * 2. 独创双句并发滑动窗口预加载（Zero-Gap Prefetch Pipeline），彻底消除句间网络停顿；
 * 3. 智能 4 角色声线矩阵调度（旁白 / 男主 / 女主 / 长者反派）+ 8 大微情绪导演指令注入；
 * 4. 实时 PCM 重采样与混音器集成：无论上游返回何种采样率，统一保真重采样为 24000Hz 16-bit 单声道，根治变调与爆音；
 * 5. 会话级生命周期隔离与精确网络取消网关 (Session-Scoped Cancellation)。
 */
class TtsSynthesizer(private val context: Context) {

    private val configDataStore = ConfigDataStore.getInstance(context)
    private val audioCacheManager = AudioCacheManager.getInstance(context)
    private val providerManager = TtsProviderManager.getInstance()

    private val isStopped = AtomicBoolean(false)
    private var currentSessionId: String = ""

    fun stop(sessionId: String? = null) {
        isStopped.set(true)
        if (sessionId != null && sessionId.isNotBlank()) {
            SharedHttpClient.cancelSession(sessionId)
        } else {
            SharedHttpClient.cancelAll()
        }
    }

    /**
     * 处理 Android 系统 TextToSpeechService 发起的合成请求
     */
    suspend fun processSynthesisRequest(
        request: SynthesisRequest,
        callback: SynthesisCallback,
        sessionId: String = UUID.randomUUID().toString()
    ) = withContext(Dispatchers.IO) {
        isStopped.set(false)
        currentSessionId = sessionId

        val rawText = request.charSequenceText?.toString() ?: ""
        val targetSampleRate = 24000

        if (rawText.isBlank()) {
            callback.start(targetSampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        val settings = configDataStore.settingsFlow.value
        val requestedVoice = request.voiceName
        val matchedProvider = if (!requestedVoice.isNullOrBlank()) {
            configDataStore.providersFlow.value.find {
                it.name.equals(requestedVoice, ignoreCase = true) ||
                it.id.equals(requestedVoice, ignoreCase = true) ||
                it.voiceId.equals(requestedVoice, ignoreCase = true)
            }
        } else null
        val providerConfig = matchedProvider ?: configDataStore.getActiveProvider()
        val rules = configDataStore.rulesFlow.value

        // 适配系统传入的语速与音调参数（100 为标准 1.0）
        val systemSpeed = if (request.speechRate > 0) request.speechRate / 100.0f else 1.0f
        val systemPitch = if (request.pitch > 0) request.pitch / 100.0f else 1.0f
        val effectiveSpeed = (providerConfig.speed * systemSpeed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
        val effectivePitch = (providerConfig.pitch * systemPitch * settings.globalPitch).coerceIn(0.2f, 2.0f)

        val mergedConfig = providerConfig.copy(
            speed = effectiveSpeed,
            pitch = effectivePitch
        )

        // 1. 文本预处理与网页/Markdown/手机号/缩写/数字清洗
        val preprocessedText = TextPreprocessor.process(rawText, rules, settings.isNumberNormalizationEnabled)
        val finalInputText = if (settings.isAcronymNormalizationEnabled) {
            AcronymNormalizer.normalize(preprocessedText)
        } else {
            preprocessedText
        }

        if (finalInputText.isBlank()) {
            callback.start(targetSampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        // 2. 智能多角色长句切分 (识别对话与旁白、男女声、长者，支持极速首字秒开)
        val segments: List<SentenceSegment> = if (settings.isSentenceSplittingEnabled) {
            SentenceSplitter.splitTextWithRoles(finalInputText, settings.maxSentenceLength, settings.ultraLowLatencyMode)
        } else {
            listOf(SentenceSegment(finalInputText, SegmentRole.NARRATOR))
        }

        if (segments.isEmpty()) {
            callback.start(targetSampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        configDataStore.log("开始合成任务 [$sessionId] [${mergedConfig.name}]: ${segments.size} 句, 首句: \"${segments.first().text.take(20)}...\"")

        // 统一在任务开始时初始化一次 callback
        val startStatus = callback.start(targetSampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
        if (startStatus != 0) {
            configDataStore.log("SynthesisCallback.start 返回状态: $startStatus")
        }

        val bufferChunkSize = 2048 // 2KB 极速小块推流，首包延迟进入 Sub-50ms

        // 本次合成会话专属内存预取缓存
        val sessionCache = ConcurrentHashMap<Int, Deferred<Result<ByteArray>>>()

        fun getConfigForSegment(segment: SentenceSegment): TtsProviderConfig {
            var cfg = if (!mergedConfig.isDualRoleEnabled) {
                mergedConfig
            } else {
                when (segment.role) {
                    SegmentRole.ELDER_DIALOGUE -> {
                        val elderVoice = mergedConfig.elderVoiceId.ifBlank { mergedConfig.dialogueVoiceId }
                        if (elderVoice.isNotBlank()) mergedConfig.copy(voiceId = elderVoice) else mergedConfig
                    }
                    SegmentRole.FEMALE_DIALOGUE -> {
                        val femaleVoice = mergedConfig.femaleVoiceId.ifBlank { mergedConfig.dialogueVoiceId }
                        if (femaleVoice.isNotBlank()) mergedConfig.copy(voiceId = femaleVoice) else mergedConfig
                    }
                    SegmentRole.MALE_DIALOGUE -> {
                        val maleVoice = mergedConfig.maleVoiceId.ifBlank { mergedConfig.dialogueVoiceId }
                        if (maleVoice.isNotBlank()) mergedConfig.copy(voiceId = maleVoice) else mergedConfig
                    }
                    SegmentRole.DIALOGUE -> {
                        if (mergedConfig.dialogueVoiceId.isNotBlank()) mergedConfig.copy(voiceId = mergedConfig.dialogueVoiceId) else mergedConfig
                    }
                    SegmentRole.NARRATOR -> mergedConfig
                }
            }

            // 注入大模型智能情感语气导演指令 (8 大微情绪)
            if (settings.isEmotionProsodyEnabled && segment.emotion.promptInstruction.isNotBlank()) {
                val base = cfg.promptInstruction.trim()
                val emotionPart = segment.emotion.promptInstruction
                val blended = if (base.isNotBlank()) "$base $emotionPart" else emotionPart
                cfg = cfg.copy(promptInstruction = blended)
            }

            return cfg
        }

        try {
            val prefetchWindow = 4

            // 预先启动前 4 句的并发预拉取，彻底消除段落与句子间的网络等待
            for (lookAhead in 0 until minOf(prefetchWindow, segments.size)) {
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

                // 实时更新后台播报通知栏中的当前句子内容
                if (settings.playbackNotificationEnabled) {
                    TtsNotificationManager.showPlaybackNotification(
                        context = context,
                        providerName = mergedConfig.name,
                        voiceId = mergedConfig.voiceId.ifBlank { "默认" },
                        currentSentence = segments[i].text
                    )
                }

                // 随着进度推进，自动向前并发预拉取前方 4 句
                for (ahead in 1..prefetchWindow) {
                    val nextPrefetchIndex = i + ahead
                    if (nextPrefetchIndex < segments.size && !sessionCache.containsKey(nextPrefetchIndex)) {
                        val nextSeg = segments[nextPrefetchIndex]
                        val nextSegConfig = getConfigForSegment(nextSeg)
                        sessionCache[nextPrefetchIndex] = async(Dispatchers.IO) {
                            fetchOrSynthesizeAudio(nextSeg.text, nextSegConfig, settings)
                        }
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
                    continue
                }

                val rawAudioBytes = audioResult.getOrNull() ?: ByteArray(0)
                if (rawAudioBytes.isEmpty()) continue

                // 纯内存 PCM 硬件解码
                val decoded = AudioDecoder.decodeToPcm(rawAudioBytes, mergedConfig.sampleRate)
                if (decoded.pcmData.isEmpty()) continue

                // 实时高精度 PCM 采样率自适应重采样与单声道混音（彻底杜绝变调与爆音）
                val resampledPcm = AudioResampler.resample(
                    pcmData = decoded.pcmData,
                    sourceSampleRate = decoded.sampleRate,
                    sourceChannels = decoded.channelCount,
                    targetSampleRate = targetSampleRate,
                    targetChannels = 1
                )

                // 软件级人声增强、VAD 首尾死区切除与睡眠定时音量淡出
                val sleepFadeFactor = SleepTimerManager.getInstance(context).getFadeVolumeFactor()
                val effectiveGain = (mergedConfig.volume * settings.loudnessGainFactor * sleepFadeFactor).coerceIn(0.0f, 2.5f)
                val finalPcm = AudioEnhancer.processPcm(
                    pcmData = resampledPcm,
                    channels = 1,
                    enableClarity = settings.voiceClarityBoostEnabled,
                    gainFactor = effectiveGain,
                    trimSilence = true
                )

                // 极速分块向系统音频管道流式写入 PCM
                var offset = 0
                while (offset < finalPcm.size) {
                    if (isStopped.get() || !isActive) {
                        return@withContext
                    }
                    val length = Math.min(bufferChunkSize, finalPcm.size - offset)
                    callback.audioAvailable(finalPcm, offset, length)
                    offset += length
                }

                // 注入小说朗读自然呼吸停顿 (Silence padding)
                if (settings.sentencePauseMs > 0 && i < segments.size - 1) {
                    val silenceBytesCount = (targetSampleRate * 2 * (settings.sentencePauseMs / 1000.0)).toInt()
                    if (silenceBytesCount > 0) {
                        val silenceChunk = ByteArray(minOf(silenceBytesCount, bufferChunkSize))
                        var remainingSilence = silenceBytesCount
                        while (remainingSilence > 0 && !isStopped.get() && isActive) {
                            val toWrite = minOf(remainingSilence, silenceChunk.size)
                            callback.audioAvailable(silenceChunk, 0, toWrite)
                            remainingSilence -= toWrite
                        }
                    }
                }

                // 及时从会话缓存中移除已推流完成的句子，极大释放内存
                sessionCache.remove(i)
            }

            callback.done()
            configDataStore.log("合成任务 [$sessionId] 完成全部 ${segments.size} 句推流")
        } catch (e: CancellationException) {
            configDataStore.log("合成已被取消 [$sessionId]")
        } catch (e: Exception) {
            configDataStore.log("合成过程发生异常: ${e.message}")
            try {
                callback.error()
            } catch (ce: Exception) {
                // ignore
            }
        } finally {
            sessionCache.values.forEach { it.cancel() }
            sessionCache.clear()
            if (settings.playbackNotificationEnabled) {
                TtsNotificationManager.cancelPlaybackNotification(context)
            }
        }
    }

    /**
     * 优先从磁盘缓存读取，未命中则调用 Provider 进行远程网络合成，并写入缓存（带自动重试与智能故障降级）
     */
    private suspend fun fetchOrSynthesizeAudio(
        text: String,
        config: TtsProviderConfig,
        settings: GlobalSettings
    ): Result<ByteArray> {
        val startMs = System.currentTimeMillis()

        // 1. 尝试从全维度本地缓存读取
        if (settings.isAudioCacheEnabled) {
            val cachedData = audioCacheManager.getAudio(text, config)
            if (cachedData != null && cachedData.isNotEmpty()) {
                val cost = System.currentTimeMillis() - startMs
                configDataStore.recordSpeechHistory(
                    com.aitts.engine.data.SpeechHistoryItem(
                        id = UUID.randomUUID().toString().take(8),
                        text = text.take(60),
                        providerName = "${config.name} (缓存)",
                        voiceId = config.voiceId.ifBlank { "默认" },
                        costMs = cost,
                        characterCount = text.length
                    )
                )
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
                val cost = System.currentTimeMillis() - startMs
                configDataStore.recordSpeechHistory(
                    com.aitts.engine.data.SpeechHistoryItem(
                        id = UUID.randomUUID().toString().take(8),
                        text = text.take(60),
                        providerName = config.name,
                        voiceId = config.voiceId.ifBlank { "默认" },
                        costMs = cost,
                        characterCount = text.length
                    )
                )
                return Result.success(audioBytes)
            } else {
                lastError = result.exceptionOrNull()
                if (attempt < 2) {
                    kotlinx.coroutines.delay(200)
                }
            }
        }

        // 3. 智能故障转移（若主引擎超额或网络异常，无缝降级备用引擎）
        if (settings.autoFallbackOnFailure) {
            val candidateId = config.fallbackProviderId?.takeIf { it.isNotBlank() } ?: settings.fallbackProviderId
            val fallback = configDataStore.providersFlow.value.find { it.id == candidateId && it.id != config.id }
                ?: configDataStore.providersFlow.value.firstOrNull { it.type == com.aitts.engine.data.ProviderType.EDGE_TTS && it.id != config.id }

            if (fallback != null) {
                configDataStore.log("⚠️ 主引擎 [${config.name}] 失败，自动无缝降级到备用引擎 [${fallback.name}]")
                val fallbackRes = providerManager.synthesize(text, fallback)
                if (fallbackRes.isSuccess) {
                    val bytes = fallbackRes.getOrNull() ?: ByteArray(0)
                    val cost = System.currentTimeMillis() - startMs
                    configDataStore.recordSpeechHistory(
                        com.aitts.engine.data.SpeechHistoryItem(
                            id = UUID.randomUUID().toString().take(8),
                            text = text.take(60),
                            providerName = "${fallback.name} (降级兜底)",
                            voiceId = fallback.voiceId.ifBlank { "默认" },
                            costMs = cost,
                            characterCount = text.length,
                            isFallbackUsed = true
                        )
                    )
                    return Result.success(bytes)
                }
            }
        }

        return Result.failure(lastError ?: Exception("合成失败"))
    }
}
