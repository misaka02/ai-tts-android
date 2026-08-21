package com.aitts.engine.provider

import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel

/**
 * TTS 提供商统一管理调度中心
 */
class TtsProviderManager {

    private val providers = mutableMapOf<ProviderType, TtsProvider>()

    init {
        providers[ProviderType.MIMO] = MimoTtsProvider()
        providers[ProviderType.MINIMAX] = MiniMaxTtsProvider()
        providers[ProviderType.DOUBAO] = DoubaoTtsProvider()
        providers[ProviderType.EDGE_TTS] = EdgeTtsProvider()
        providers[ProviderType.SILICONFLOW] = SiliconFlowTtsProvider()
        providers[ProviderType.FISH_AUDIO] = FishAudioTtsProvider()
        providers[ProviderType.STEPFUN] = StepFunTtsProvider()
        providers[ProviderType.OPENAI] = OpenAiTtsProvider()
        providers[ProviderType.AZURE] = AzureTtsProvider()
        providers[ProviderType.GEMINI] = GeminiTtsProvider()
        providers[ProviderType.CUSTOM_HTTP] = CustomHttpTtsProvider()
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
