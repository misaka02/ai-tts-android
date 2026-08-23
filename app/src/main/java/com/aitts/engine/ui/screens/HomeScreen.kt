package com.aitts.engine.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.aitts.engine.data.ConfigDataStore

/**
 * 🌟 主界面顶层路由调度器 (Home Screen Multi-Theme Dynamic Switcher)
 * 根据用户的首选项设定，无缝路由至 3 大主题工作台之一：
 * 1. 🚀 BentoConsoleHomeScreen (未来拟态 Bento 便当盒 3D 全息声球工作台)
 * 2. 🎛️ ModernStudioHomeScreen (专业 DAW 音频工作站与硬件频谱调音台)
 * 3. 📻 VinylDeckHomeScreen (复古黑胶唱机与提词卷轴沉浸式阅览舱)
 */
@Composable
fun HomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit
) {
    val settings by configDataStore.settingsFlow.collectAsState()

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
}
