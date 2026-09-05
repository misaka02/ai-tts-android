package com.aitts.engine.ui.material

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.ui.material.screens.GoogleProviderConfigScreen
import com.aitts.engine.ui.material.screens.GoogleProvidersScreen
import com.aitts.engine.ui.material.screens.GoogleRulesScreen
import com.aitts.engine.ui.material.screens.GoogleSettingsScreen
import com.aitts.engine.ui.material.screens.GoogleSpeechHubScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🌟 Google 官方应用风格顶层主外壳 (Google Material App Shell)
 * 1. 采用标准的 Google Material Design 3 Scaffold 与原生 NavigationBar；
 * 2. 4 大完整功能舱（朗读工作台、模型服务商、发音规则流水线、系统设置）；
 * 3. 严格遵循 Google Recorder、Google Pixel 设置等官方应用的设计美学与色调体系；
 * 4. 支持双击返回防误触与流畅横滑切换。
 */
@OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)
@Composable
fun GoogleMaterialAppShell(
    configDataStore: ConfigDataStore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (settings.appThemeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
    }
    val colors = remember(isDark) { GoogleMaterialTokens.resolve(isDark) }

    var editingProviderId by remember { mutableStateOf<String?>(null) }
    var backPressedOnce by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        focusManager.clearFocus()
        when {
            editingProviderId != null -> {
                editingProviderId = null
            }
            pagerState.currentPage != 0 -> {
                scope.launch { pagerState.animateScrollToPage(0) }
            }
            else -> {
                if (backPressedOnce) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(context, "再按一次退出 AI-TTS", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        delay(2000)
                        backPressedOnce = false
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (editingProviderId != null) {
            GoogleProviderConfigScreen(
                providerId = editingProviderId!!,
                configDataStore = configDataStore,
                colors = colors,
                onNavigateBack = { editingProviderId = null }
            )
        } else {
            Scaffold(
                containerColor = colors.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = colors.surface,
                        contentColor = colors.textPrimary,
                        tonalElevation = 2.dp
                    ) {
                        val tabs = listOf(
                            Triple("朗读", Icons.Default.GraphicEq, 0),
                            Triple("模型", Icons.Default.RecordVoiceOver, 1),
                            Triple("规则", Icons.Default.Spellcheck, 2),
                            Triple("设置", Icons.Default.Tune, 3)
                        )

                        tabs.forEach { (label, icon, index) ->
                            val isSelected = pagerState.currentPage == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                icon = {
                                    Icon(imageVector = icon, contentDescription = label)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.onPrimaryContainer,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primaryContainer,
                                    unselectedIconColor = colors.textSecondary,
                                    unselectedTextColor = colors.textSecondary
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { page ->
                    when (page) {
                        0 -> GoogleSpeechHubScreen(
                            configDataStore = configDataStore,
                            colors = colors,
                            onOpenProviders = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            }
                        )
                        1 -> GoogleProvidersScreen(
                            configDataStore = configDataStore,
                            colors = colors,
                            onNavigateToEditProvider = { editingProviderId = it },
                            onAddNewProvider = {
                                editingProviderId = "new_${System.currentTimeMillis() % 10000}"
                            }
                        )
                        2 -> GoogleRulesScreen(
                            configDataStore = configDataStore,
                            colors = colors
                        )
                        3 -> GoogleSettingsScreen(
                            configDataStore = configDataStore,
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}
