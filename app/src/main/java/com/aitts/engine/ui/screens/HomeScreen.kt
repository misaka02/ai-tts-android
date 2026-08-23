package com.aitts.engine.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.SpeechHistoryItem
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.ui.components.FloatingMasterDock
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * 🌟 主界面顶层路由调度器与全局悬浮坞托管层 (Home Screen Dynamic Theme & Dock Orchestrator)
 * 1. 托管 3 大主题工作台路由（Bento 🚀 / Studio 🎛️ / Vinyl 📻）；
 * 2. 全局统一托管常驻悬浮主控坞 (`FloatingMasterDock`)，切换主题时位置与状态绝对不丢失、不重置；
 * 3. 统一处理全局试听与随机语料。
 */
@Composable
fun HomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var testText by remember { mutableStateOf("欢迎使用 AI TTS 系统语音引擎！当前正在通过智能大模型为您朗读文本。") }

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
        isSynthesizing = false
    }

    fun playSpeechWithProvider(provider: TtsProviderConfig, text: String) {
        if (isPlaying || isSynthesizing) {
            stopPlayback()
            return
        }

        isSynthesizing = true
        scope.launch {
            try {
                val effectiveSpeed = (provider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (provider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = provider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                val startTime = System.currentTimeMillis()
                val result = TtsProviderManager.getInstance().synthesize(text, testConfig)
                val costMs = System.currentTimeMillis() - startTime

                if (result.isSuccess) {
                    val audioData = result.getOrNull() ?: ByteArray(0)
                    if (audioData.isNotEmpty()) {
                        configDataStore.recordSpeechHistory(
                            SpeechHistoryItem(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                text = text,
                                providerName = provider.name,
                                voiceId = provider.voiceId,
                                characterCount = text.length,
                                costMs = costMs,
                                isFallbackUsed = false
                            )
                        )

                        isSynthesizing = false
                        isPlaying = true

                        audioPlayer.playAudioBytes(
                            audioData,
                            onCompletion = {
                                isPlaying = false
                            }
                        )
                    } else {
                        isSynthesizing = false
                        isPlaying = false
                        Toast.makeText(context, "合成音频为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isSynthesizing = false
                    isPlaying = false
                    val err = result.exceptionOrNull()?.message ?: "未知错误"
                    Toast.makeText(context, "试听失败: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isSynthesizing = false
                isPlaying = false
                Toast.makeText(context, "合成失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌟 1. 活跃主题工作台
        when (settings.appUiStyle) {
            "BENTO" -> {
                BentoConsoleHomeScreen(
                    configDataStore = configDataStore,
                    onNavigateToEditProvider = onNavigateToEditProvider,
                    onNavigateToTestBench = onNavigateToTestBench,
                    onSwitchUiStyle = { newStyle ->
                        configDataStore.updateSettings(settings.copy(appUiStyle = newStyle))
                    }
                )
            }
            "STUDIO" -> {
                ModernStudioHomeScreen(
                    configDataStore = configDataStore,
                    onNavigateToEditProvider = onNavigateToEditProvider,
                    onNavigateToTestBench = onNavigateToTestBench,
                    onSwitchUiStyle = { newStyle ->
                        configDataStore.updateSettings(settings.copy(appUiStyle = newStyle))
                    }
                )
            }
            else -> {
                VinylDeckHomeScreen(
                    configDataStore = configDataStore,
                    onNavigateToEditProvider = onNavigateToEditProvider,
                    onNavigateToTestBench = onNavigateToTestBench,
                    onSwitchUiStyle = { newStyle ->
                        configDataStore.updateSettings(settings.copy(appUiStyle = newStyle))
                    }
                )
            }
        }

        // 🌟 2. 全局常驻自由拖拽悬浮主控坞 (Universal Global Floating Master Dock)
        FloatingMasterDock(
            activeProvider = activeProvider,
            currentUiStyle = settings.appUiStyle,
            dockModeName = settings.floatingDockMode,
            initialX = settings.floatingDockX,
            initialY = settings.floatingDockY,
            isPlaying = isPlaying,
            isSynthesizing = isSynthesizing,
            onPlayToggle = {
                if (isPlaying || isSynthesizing) {
                    stopPlayback()
                } else {
                    activeProvider?.let { playSpeechWithProvider(it, testText) }
                }
            },
            onRandomQuote = {
                val item = QuoteService.getRandomLocalQuote()
                testText = item.text
            },
            onSwitchUiStyle = { newStyle ->
                configDataStore.updateSettings(settings.copy(appUiStyle = newStyle))
            },
            onOpenProviderConfig = onNavigateToEditProvider,
            onUpdateDockState = { mode, x, y ->
                configDataStore.updateSettings(
                    settings.copy(
                        floatingDockMode = mode,
                        floatingDockX = x,
                        floatingDockY = y
                    )
                )
            }
        )
    }
}
