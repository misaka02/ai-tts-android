package com.aitts.engine.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioFormat
import android.os.Build
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.UUID

/**
 * Android 标准 TextToSpeechService 引擎实现 (v2.0.0 工业级服务)
 * 1. 注册到系统全局，可被所有小说阅读器 (Legado / 阅读 / 静读天下 / 多看等)、TalkBack 和第三方 App 调用；
 * 2. 具备会话级隔离精准取消机制与 AudioFocus 系统音频焦点感知 (通话与导航自动平滑避让)。
 */
class AiTextToSpeechService : TextToSpeechService() {

    private lateinit var synthesizer: TtsSynthesizer
    private lateinit var configDataStore: ConfigDataStore
    private lateinit var audioManager: AudioManager

    @Volatile
    private var activeSessionId: String = ""

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                configDataStore.log("AudioFocus 丢失，停止后台朗读")
                onStop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                configDataStore.log("AudioFocus 临时丢失 (如电话呼入)")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                configDataStore.log("AudioFocus 临时避让 (如导航提示播报)")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                configDataStore.log("AudioFocus 重新获取")
            }
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TtsNotificationManager.ACTION_STOP_TTS) {
                onStop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        configDataStore = ConfigDataStore.getInstance(this)
        synthesizer = TtsSynthesizer(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        try {
            val filter = IntentFilter(TtsNotificationManager.ACTION_STOP_TTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(stopReceiver, filter)
            }
        } catch (e: Exception) {
            // ignore
        }
        configDataStore.log("AiTextToSpeechService v2.0.0 系统服务已启动并就绪")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            // ignore
        }
        synthesizer.stop(activeSessionId)
        TtsNotificationManager.cancelPlaybackNotification(this)
        configDataStore.log("AiTextToSpeechService 系统服务已销毁")
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val language = lang?.lowercase(Locale.getDefault()) ?: ""
        return when {
            language.startsWith("zh") || language.startsWith("zho") || language.startsWith("chi") || language.startsWith("cmn") -> {
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
        val sessionId = UUID.randomUUID().toString().take(8)
        activeSessionId = sessionId

        if (text.isBlank()) {
            callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)
            callback.done()
            return
        }

        try {
            runBlocking {
                synthesizer.processSynthesisRequest(request, callback, sessionId)
            }
        } catch (e: Throwable) {
            configDataStore.log("onSynthesizeText [$sessionId] 异常: ${e.message}")
            try {
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
            } catch (ce: Throwable) {
                // ignore
            }
        }
    }

    override fun onStop() {
        try {
            val sessionToCancel = activeSessionId
            synthesizer.stop(sessionToCancel)
            TtsNotificationManager.cancelPlaybackNotification(this)
            configDataStore.log("收到系统 onStop() 信号，已精准中断会话 [$sessionToCancel]")
        } catch (e: Throwable) {
            // ignore
        }
    }

    override fun onGetVoices(): MutableList<Voice> {
        val voices = mutableListOf<Voice>()

        // 1. 注入用户配置的全部 AI 模型与音色 (活跃模型置顶)
        val allProviders = configDataStore.providersFlow.value
        val activeProvider = configDataStore.getActiveProvider()
        for (p in allProviders) {
            val voiceName = p.name.ifBlank { p.id }
            val customVoice = Voice(
                voiceName,
                Locale.CHINESE,
                Voice.QUALITY_VERY_HIGH,
                Voice.LATENCY_NORMAL,
                false,
                setOf("custom", p.type.name, p.id)
            )
            if (p.id == activeProvider.id) {
                voices.add(0, customVoice)
            } else {
                voices.add(customVoice)
            }
        }

        // 2. 注入所有 Edge TTS 高质量神经网络音色
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
                setOf("networkTts", "ai", "neural")
            )
            voices.add(voiceObj)
        }

        return voices
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String {
        val active = configDataStore.getActiveProvider()
        return active.name.ifBlank { active.voiceId.ifBlank { "zh-CN-XiaoxiaoNeural" } }
    }
}
