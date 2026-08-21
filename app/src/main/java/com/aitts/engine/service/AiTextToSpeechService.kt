package com.aitts.engine.service

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import kotlinx.coroutines.runBlocking
import java.util.Locale

/**
 * Android 标准 TextToSpeechService 引擎实现
 * 注册到系统全局，可被所有小说阅读器、TalkBack 和第三方 App 调用。
 */
class AiTextToSpeechService : TextToSpeechService() {

    private lateinit var synthesizer: TtsSynthesizer
    private lateinit var configDataStore: ConfigDataStore

    override fun onCreate() {
        super.onCreate()
        configDataStore = ConfigDataStore.getInstance(this)
        synthesizer = TtsSynthesizer(this)
        configDataStore.log("AiTextToSpeechService 系统服务已启动并就绪")
    }

    override fun onDestroy() {
        super.onDestroy()
        synthesizer.stop()
        configDataStore.log("AiTextToSpeechService 系统服务已销毁")
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val language = lang?.lowercase(Locale.getDefault()) ?: ""
        return when {
            language.startsWith("zh") || language.startsWith("zho") || language.startsWith("chi") -> {
                TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
            language.startsWith("en") || language.startsWith("eng") -> {
                TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
            language.startsWith("ja") || language.startsWith("jpn") -> {
                TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
            language.startsWith("ko") || language.startsWith("kor") -> {
                TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
            language.startsWith("yue") || language.startsWith("cant") -> {
                TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
            else -> TextToSpeech.LANG_AVAILABLE
        }
    }

    override fun onGetLanguage(): Array<String> {
        val provider = configDataStore.getActiveProvider()
        return if (provider.voiceId.startsWith("en-")) {
            arrayOf("eng", "USA", "")
        } else {
            arrayOf("zho", "CHN", "")
        }
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    /**
     * 系统 TTS 合成核心回调：
     * Android 系统在内部专属合成工作线程中同步调用此方法，
     * 必须阻塞至当前文本全部流式推送给 callback 完成后才能退出。
     */
    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return

        val text = request.charSequenceText?.toString() ?: ""
        if (text.isBlank()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return
        }

        try {
            runBlocking {
                synthesizer.processSynthesisRequest(request, callback)
            }
        } catch (e: Exception) {
            configDataStore.log("onSynthesizeText 异常: ${e.message}")
            try {
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
            } catch (ce: Exception) {
                // ignore
            }
        }
    }

    override fun onStop() {
        synthesizer.stop()
        configDataStore.log("收到系统 onStop() 终止朗读信号")
    }

    override fun onGetVoices(): MutableList<Voice> {
        val voices = mutableListOf<Voice>()

        // 1. 注入所有 Edge TTS 高质量神经网络音色
        for (v in PresetConfigs.edgeVoices) {
            val locale = try {
                Locale.forLanguageTag(v.locale)
            } catch (e: Exception) {
                Locale.CHINESE
            }
            val voiceObj = Voice(
                v.id,
                locale,
                Voice.QUALITY_VERY_HIGH,
                Voice.LATENCY_NORMAL,
                false,
                setOf(v.gender)
            )
            voices.add(voiceObj)
        }

        // 2. 注入当前已配置的自定义大模型音色 (MIMO、MiniMax、豆包等)
        val customProviders = configDataStore.providersFlow.value
        for (p in customProviders) {
            if (voices.none { it.name == p.voiceId }) {
                val voiceObj = Voice(
                    p.voiceId.ifBlank { p.id },
                    Locale.CHINESE,
                    Voice.QUALITY_VERY_HIGH,
                    Voice.LATENCY_NORMAL,
                    false,
                    setOf("neutral")
                )
                voices.add(voiceObj)
            }
        }

        return voices
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String {
        val active = configDataStore.getActiveProvider()
        if (active.voiceId.isNotBlank()) {
            return active.voiceId
        }
        val language = lang?.lowercase(Locale.getDefault()) ?: ""
        return if (language.startsWith("zh") || language.startsWith("zho") || language.startsWith("chi")) {
            "zh-CN-XiaoxiaoNeural"
        } else {
            "en-US-EmmaMultilingualNeural"
        }
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        if (voiceName == null) return TextToSpeech.ERROR
        return TextToSpeech.SUCCESS
    }
}
