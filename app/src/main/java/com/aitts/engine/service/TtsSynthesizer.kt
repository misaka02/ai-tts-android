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
import com.aitts.engine.data.LogLevel
import com.aitts.engine.data.ProviderType
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
        // 严格遵从用户在 AI-TTS 中枢主控当前激活的模型 (如小米 MiMo 大模型)，无条件直接生效
        val providerConfig = configDataStore.getActiveProvider()
        val rules = configDataStore.rulesFlow.value

        // 适配系统与阅读器传入的语速与音调参数 (智能适配静读天下/阅读 1~30 刻度与标准 Android 100 刻度)
        val systemSpeed = when {
            request.speechRate <= 0 || request.speechRate == 10 || request.speechRate == 100 -> 1.0f
            request.speechRate in 1..30 -> (request.speechRate / 10.0f).coerceIn(0.25f, 3.0f)
            else -> (request.speechRate / 100.0f).coerceIn(0.25f, 3.0f)
        }

        val systemPitch = when {
            request.pitch <= 0 || request.pitch == 10 || request.pitch == 100 -> 1.0f
            request.pitch in 1..30 -> (request.pitch / 10.0f).coerceIn(0.5f, 2.0f)
            else -> (request.pitch / 100.0f).coerceIn(0.5f, 2.0f)
        }

        val effectiveSpeed = (providerConfig.speed * systemSpeed * settings.globalSpeed).coerceIn(0.25f, 3.0f)
        val effectivePitch = (providerConfig.pitch * systemPitch * settings.globalPitch).coerceIn(0.5f, 2.0f)

        val mergedConfig = providerConfig.copy(
            speed = effectiveSpeed,
            pitch = effectivePitch
        )

        val reqStartTime = System.currentTimeMillis()
        configDataStore.logStructured(
            level = LogLevel.INFO,
            tag = "TTS_SERVICE",
            title = "收到朗读请求",
            details = "引擎=[${mergedConfig.name}], 文本=${rawText.length}字, 语速=${systemSpeed}x, 模式=${if (mergedConfig.isStreamingEnabled) "流式" else "非流式"}",
            sessionId = sessionId
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

        // 2. 文本分段规则控制流水线 (关闭切分时 100% 保持阅读器原文本整篇透传；开启时严格按分段/合并/长段拆分规则处理)
        val segments: List<SentenceSegment> = if (!settings.isSentenceSplittingEnabled) {
            // 用户未启用切分：阅读器传入什么就向引擎发送什么，整段完整发送，不做任何切分
            listOf(SentenceSegment(finalInputText, SegmentRole.NARRATOR))
        } else {
            // 启用切分控制：严格按分段模式、短段落合并与句号长段拆分规则执行
            SentenceSplitter.splitTextWithFineRules(
                text = finalInputText,
                mode = settings.textSegmentationMode,
                mergeShort = settings.mergeShortParagraphs,
                minMergeLen = settings.minMergeParagraphLength,
                splitLong = settings.splitLongParagraphs,
                maxLen = settings.maxSegmentLength,
                ultraLowLatency = settings.ultraLowLatencyMode
            )
        }

        if (segments.isEmpty()) {
            callback.start(targetSampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return@withContext
        }

        if (segments.size > 1) {
            configDataStore.logStructured(
                level = LogLevel.INFO,
                tag = "PIPELINE",
                title = "文本已分段",
                details = "共切分出 ${segments.size} 段 (${settings.textSegmentationMode})",
                sessionId = sessionId
            )
        }

        // 统一在任务开始时初始化一次 callback
        val startStatus = callback.start(targetSampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)
        if (startStatus != 0) {
            configDataStore.log("SynthesisCallback.start 返回状态: $startStatus", sessionId = sessionId)
        }

        val bufferChunkSize = 2048 // 2KB 极速小块推流，首包延迟进入 Sub-50ms

        // 本次合成会话专属内存预取缓存 (以分段规则切出的分块为基准单元)
        val sessionCache = ConcurrentHashMap<Int, Deferred<Result<ByteArray>>>()

        fun getConfigForSegment(segment: SentenceSegment, segmentIndex: Int = 0): TtsProviderConfig {
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

            // 双 API Key 智能轮询分流 (Dual-Key Round-Robin & Concurrency Multiplier)
            if (mergedConfig.secondaryApiKey.isNotBlank()) {
                val useSecondary = (segmentIndex % 2 != 0)
                cfg = cfg.copy(
                    apiKey = if (useSecondary) mergedConfig.secondaryApiKey else mergedConfig.apiKey
                )
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
            // 预加载前瞻窗口深度 (当开启预加载时提前准备接下来 1~2 块分段音频，避免等待；关闭预加载时单块串行)
            val prefetchWindow = if (settings.enableSegmentPreload && segments.size > 1) {
                settings.preloadAheadCount.coerceIn(1, 4)
            } else {
                1
            }

            // 预先启动前方分段的并发预拉取 (若首段开启流式，则首段无需预取，直接进入实时推流)
            val startPrefetchIdx = if (mergedConfig.isStreamingEnabled) 1 else 0
            for (lookAhead in startPrefetchIdx until minOf(prefetchWindow + startPrefetchIdx, segments.size)) {
                val seg = segments[lookAhead]
                val segConfig = getConfigForSegment(seg, lookAhead)
                sessionCache[lookAhead] = async(Dispatchers.IO) {
                    fetchOrSynthesizeAudio(seg.text, segConfig, settings, sessionId)
                }
            }

            for (i in segments.indices) {
                if (isStopped.get() || !isActive) {
                    configDataStore.logStructured(LogLevel.WARN, "TTS_SERVICE", "合成任务已中断", sessionId = sessionId)
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

                // 随着播放进度推进，按照分段规则自动提前异步预拉取前方分块音频
                if (settings.enableSegmentPreload) {
                    for (ahead in 1..prefetchWindow) {
                        val nextPrefetchIndex = i + ahead
                        if (nextPrefetchIndex < segments.size && !sessionCache.containsKey(nextPrefetchIndex)) {
                            val nextSeg = segments[nextPrefetchIndex]
                            val nextSegConfig = getConfigForSegment(nextSeg, nextPrefetchIndex)
                            sessionCache[nextPrefetchIndex] = async(Dispatchers.IO) {
                                fetchOrSynthesizeAudio(nextSeg.text, nextSegConfig, settings, sessionId)
                            }
                        }
                    }
                }

                val seg = segments[i]
                val segConfig = getConfigForSegment(seg, i)
                val sleepFadeFactor = SleepTimerManager.getInstance(context).getFadeVolumeFactor()
                val effectiveGain = (segConfig.volume * settings.loudnessGainFactor * sleepFadeFactor).coerceIn(0.0f, 2.5f)

                if (segConfig.isStreamingEnabled && !sessionCache.containsKey(i)) {
                    configDataStore.logStructured(
                        level = LogLevel.INFO,
                        tag = "STREAM",
                        title = "第 ${i + 1}/${segments.size} 段流式推流启动",
                        details = "引擎=${segConfig.name}, 语速=${segConfig.speed}x",
                        sessionId = sessionId
                    )
                    val sonic = if (segConfig.type != ProviderType.GEMINI && kotlin.math.abs(segConfig.speed - 1.0f) >= 0.03f) {
                        com.aitts.engine.audio.Sonic(targetSampleRate, 1).apply {
                            speed = segConfig.speed
                        }
                    } else null
                    var streamChunkCount = 0
                    var streamTotalBytes = 0

                    val streamRes = providerManager.synthesizeStreaming(seg.text, segConfig, sessionId) { rawChunk ->
                        if (isStopped.get() || !isActive) return@synthesizeStreaming
                        try {
                            val resampled = AudioResampler.resample(
                                pcmData = rawChunk,
                                sourceSampleRate = segConfig.sampleRate,
                                sourceChannels = 1,
                                targetSampleRate = targetSampleRate,
                                targetChannels = 1
                            )

                            val pcmToPush = if (sonic != null) {
                                sonic.writeBytesToStream(resampled, resampled.size)
                                val available = sonic.samplesAvailable() * 2
                                if (available > 0) {
                                    val tempBuf = ByteArray(available)
                                    val readBytes = sonic.readBytesFromStream(tempBuf, tempBuf.size)
                                    if (readBytes > 0) tempBuf.copyOf(readBytes) else ByteArray(0)
                                } else {
                                    ByteArray(0)
                                }
                            } else {
                                resampled
                            }

                            if (pcmToPush.isNotEmpty()) {
                                streamChunkCount++
                                streamTotalBytes += pcmToPush.size
                                val enhanced = AudioEnhancer.processPcm(
                                    pcmData = pcmToPush,
                                    channels = 1,
                                    enableClarity = settings.voiceClarityBoostEnabled,
                                    gainFactor = effectiveGain,
                                    trimSilence = false
                                )
                                var off = 0
                                while (off < enhanced.size) {
                                    if (isStopped.get() || !isActive) break
                                    val len = minOf(bufferChunkSize, enhanced.size - off)
                                    callback.audioAvailable(enhanced, off, len)
                                    off += len
                                }
                            }
                        } catch (e: Throwable) {
                            configDataStore.log("⚠️ 流式数据块处理告警: ${e.message}", sessionId = sessionId)
                        }
                    }

                    // 流式尾包平滑冲刷
                    if (sonic != null) {
                        try {
                            sonic.flushStream()
                            val available = sonic.samplesAvailable() * 2
                            if (available > 0) {
                                val tailBuf = ByteArray(available)
                                val readBytes = sonic.readBytesFromStream(tailBuf, tailBuf.size)
                                if (readBytes > 0) {
                                    val enhanced = AudioEnhancer.processPcm(
                                        pcmData = tailBuf.copyOf(readBytes),
                                        channels = 1,
                                        enableClarity = settings.voiceClarityBoostEnabled,
                                        gainFactor = effectiveGain,
                                        trimSilence = false
                                    )
                                    var off = 0
                                    while (off < enhanced.size) {
                                        if (isStopped.get() || !isActive) break
                                        val len = minOf(bufferChunkSize, enhanced.size - off)
                                        callback.audioAvailable(enhanced, off, len)
                                        off += len
                                    }
                                }
                            }
                        } catch (e: Throwable) {
                            // ignore flush error
                        }
                    }

                    if (streamRes.isFailure) {
                        configDataStore.logStructured(
                            level = LogLevel.ERROR,
                            tag = "STREAM",
                            title = "第 ${i + 1}/${segments.size} 段推流受阻",
                            details = streamRes.exceptionOrNull()?.message ?: "未知异常",
                            sessionId = sessionId
                        )
                    } else {
                        configDataStore.logStructured(
                            level = LogLevel.SUCCESS,
                            tag = "STREAM",
                            title = "第 ${i + 1}/${segments.size} 段推流完成",
                            details = "共推送 $streamChunkCount 帧 ($streamTotalBytes 字节)",
                            sessionId = sessionId
                        )
                    }

                    if (streamRes.isSuccess && settings.isAudioCacheEnabled) {
                        val allBytes = streamRes.getOrNull()
                        if (allBytes != null && allBytes.isNotEmpty()) {
                            audioCacheManager.saveAudio(seg.text, segConfig, allBytes)
                        }
                    }
                } else {
                    // 取出当前句的异步任务并等待结果 (全量/预加载/非流式通道)
                    val currentDeferred = sessionCache[i] ?: async(Dispatchers.IO) {
                        fetchOrSynthesizeAudio(seg.text, segConfig, settings, sessionId)
                    }

                    val fetchResult = currentDeferred.await()

                    if (fetchResult.isFailure) {
                        throw fetchResult.exceptionOrNull() ?: Exception("合成失败: 未知错误")
                    }

                    val rawAudioData = fetchResult.getOrNull()
                        ?: throw Exception("合成失败: 返回空音频数据")

                    if (settings.isAudioCacheEnabled) {
                        audioCacheManager.saveAudio(seg.text, segConfig, rawAudioData)
                    }

                    val decodedPcm = AudioDecoder.decodeToPcm(rawAudioData, segConfig.sampleRate)
                    val resampled = AudioResampler.resample(
                        pcmData = decodedPcm.pcmData,
                        sourceSampleRate = decodedPcm.sampleRate,
                        sourceChannels = decodedPcm.channelCount,
                        targetSampleRate = targetSampleRate,
                        targetChannels = 1
                    )

                    val enhanced = AudioEnhancer.processPcm(
                        pcmData = resampled,
                        channels = 1,
                        enableClarity = settings.voiceClarityBoostEnabled,
                        gainFactor = effectiveGain,
                        trimSilence = true,
                        normalizeLoudness = true
                    )

                    // 对非流式音频进行倍速/音调处理 (仅当客户端需要局部变速且非大模型导演模式时生效)
                    val finalPcm = if (kotlin.math.abs(segConfig.speed - 1.0f) >= 0.03f &&
                        segConfig.type != ProviderType.MIMO &&
                        segConfig.type != ProviderType.GEMINI) {
                        val sonic = com.aitts.engine.audio.Sonic(targetSampleRate, 1).apply {
                            speed = segConfig.speed
                        }
                        sonic.writeBytesToStream(enhanced, enhanced.size)
                        sonic.flushStream()
                        val available = sonic.samplesAvailable() * 2
                        if (available > 0) {
                            val tempBuf = ByteArray(available)
                            val readBytes = sonic.readBytesFromStream(tempBuf, tempBuf.size)
                            if (readBytes > 0) tempBuf.copyOf(readBytes) else enhanced
                        } else {
                            enhanced
                        }
                    } else {
                        enhanced
                    }

                    var offset = 0
                    while (offset < finalPcm.size) {
                        if (isStopped.get() || !isActive) {
                            return@withContext
                        }
                        val length = minOf(bufferChunkSize, finalPcm.size - offset)
                        callback.audioAvailable(finalPcm, offset, length)
                        offset += length
                    }
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
            val totalCost = System.currentTimeMillis() - reqStartTime
            configDataStore.logStructured(
                level = LogLevel.SUCCESS,
                tag = "TTS_SERVICE",
                title = "朗读播音结束",
                details = "共完成 ${segments.size} 段推流, 总耗时 ${totalCost}ms",
                sessionId = sessionId
            )
        } catch (e: CancellationException) {
            configDataStore.logStructured(LogLevel.WARN, "TTS_SERVICE", "朗读已被取消", "会话=[$sessionId]", sessionId = sessionId)
        } catch (e: Exception) {
            configDataStore.logStructured(LogLevel.ERROR, "TTS_SERVICE", "朗读发生异常", e.message ?: "未知错误", sessionId = sessionId)
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
        settings: GlobalSettings,
        sessionId: String = ""
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

            val result = providerManager.synthesize(text, config, sessionId = sessionId)
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
                configDataStore.log("⚠️ 主引擎 [${config.name}] 失败，自动无缝降级到备用引擎 [${fallback.name}]", sessionId = sessionId)
                val fallbackRes = providerManager.synthesize(text, fallback, sessionId = sessionId)
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
