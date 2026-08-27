package com.aitts.engine.provider

import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.network.SharedHttpClient

/**
 * TTS 提供商统一管理调度中心
 * 统一复用 SharedHttpClient 全局 HTTP/2 连接池
 */
class TtsProviderManager {

    private val providers = mutableMapOf<ProviderType, TtsProvider>()

    init {
        val client = SharedHttpClient.instance
        providers[ProviderType.MIMO] = MimoTtsProvider(client)
        providers[ProviderType.MINIMAX] = MiniMaxTtsProvider(client)
        providers[ProviderType.DOUBAO] = DoubaoTtsProvider(client)
        providers[ProviderType.EDGE_TTS] = EdgeTtsProvider(client)
        providers[ProviderType.SILICONFLOW] = SiliconFlowTtsProvider(client)
        providers[ProviderType.FISH_AUDIO] = FishAudioTtsProvider(client)
        providers[ProviderType.STEPFUN] = StepFunTtsProvider(client)
        providers[ProviderType.OPENAI] = OpenAiTtsProvider(client)
        providers[ProviderType.AZURE] = AzureTtsProvider(client)
        providers[ProviderType.GEMINI] = GeminiTtsProvider(client)
        providers[ProviderType.CUSTOM_HTTP] = CustomHttpTtsProvider(client)
        providers[ProviderType.OFFLINE_VITS] = OfflineTtsProvider(com.aitts.engine.AiTtsApp.instance)
    }

    fun getProvider(type: ProviderType): TtsProvider {
        return providers[type] ?: providers[ProviderType.EDGE_TTS]!!
    }

    suspend fun synthesize(
        text: String,
        config: TtsProviderConfig,
        autoRetry: Boolean = true,
        sessionId: String = ""
    ): Result<ByteArray> {
        val provider = getProvider(config.type)
        var result = if (sessionId.isNotBlank()) {
            provider.synthesize(text, config, sessionId)
        } else {
            provider.synthesize(text, config)
        }

        if (result.isFailure && autoRetry && config.type != ProviderType.EDGE_TTS) {
            // 网络抖动或临时性错误，微秒级轻量抖动自愈重试 (80~200ms Jitter)
            val jitterDelay = 80L + (Math.random() * 120).toLong()
            kotlinx.coroutines.delay(jitterDelay)
            result = if (sessionId.isNotBlank()) {
                provider.synthesize(text, config, sessionId)
            } else {
                provider.synthesize(text, config)
            }
        }

        return result
    }

    suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> = synthesizeStreaming(text, config, "", onAudioChunk)

    suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        sessionId: String = "",
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> {
        val provider = getProvider(config.type)
        return if (sessionId.isNotBlank()) {
            provider.synthesizeStreaming(text, config, sessionId, onAudioChunk)
        } else {
            provider.synthesizeStreaming(text, config, onAudioChunk)
        }
    }

    suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        val provider = getProvider(config.type)
        return provider.getAvailableVoices(config)
    }

    suspend fun getAvailableModels(config: TtsProviderConfig): List<String> {
        val provider = getProvider(config.type)
        return provider.getAvailableModels(config)
    }

    companion object {
        @Volatile
        private var instance: TtsProviderManager? = null

        fun getInstance(): TtsProviderManager {
            return instance ?: synchronized(this) {
                instance ?: TtsProviderManager().also { instance = it }
            }
        }
    }
}
