package com.aitts.engine.ui.screens

import androidx.compose.runtime.Composable
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.ui.pulse.PulseAppShell

/**
 * 🌟 主界面顶层路由调度器 (Home Screen Universal Modern Shell)
 * 全主题统一托管现代化 4 分舱手势滑动架构与大拇指单手收纳岛
 */
@Composable
fun HomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit = {},
    onNavigateToTestBench: () -> Unit = {}
) {
    PulseAppShell(
        configDataStore = configDataStore,
        onNavigateToTestBench = onNavigateToTestBench
    )
}
