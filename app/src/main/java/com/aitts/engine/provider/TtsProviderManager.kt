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
    }

    fun getProvider(type: ProviderType): TtsProvider {
        return providers[type] ?: providers[ProviderType.EDGE_TTS]!!
    }

    suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> {
        val provider = getProvider(config.type)
        return provider.synthesize(text, config)
    }

    suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        val provider = getProvider(config.type)
        return provider.getAvailableVoices(config)
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
