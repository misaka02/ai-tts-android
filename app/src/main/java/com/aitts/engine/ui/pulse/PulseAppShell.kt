package com.aitts.engine.ui.pulse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.ui.pulse.screens.BentoHubScreen
import com.aitts.engine.ui.pulse.screens.PulseDeckScreen
import com.aitts.engine.ui.pulse.screens.PulseHubScreen
import com.aitts.engine.ui.pulse.screens.PulsePipelineScreen
import com.aitts.engine.ui.pulse.screens.PulseProviderConfigScreen
import com.aitts.engine.ui.pulse.screens.PulseStudioSettingsScreen
import com.aitts.engine.ui.pulse.screens.StudioHubScreen
import com.aitts.engine.ui.pulse.screens.VinylHubScreen
import com.aitts.engine.ui.pulse.theme.LocalPulseColors
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.launch

/**
 * ⚡ 全局流体容器 (Pulse App Shell & Fluid Dock)
 * 1. 托管 4 大流体分舱：主页工作台、模型矩阵、规则流水线、工作室设置；
 * 2. 左右手势自由滑动（HorizontalPager）与底部中心胶囊导航坞双向联动；
 * 3. 完美承载 Bento、Studio、Vinyl、Pulse 4 大主题的全景专属重构工作台。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PulseAppShell(
    configDataStore: ConfigDataStore,
    onNavigateToTestBench: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })

    val settings by configDataStore.settingsFlow.collectAsState()
    val dynamicColors = remember(settings.appThemePalette, settings.isAmoledPureBlack, settings.appThemeMode) {
        PulseTokens.resolveDynamicColors(settings.appThemePalette, settings.isAmoledPureBlack, settings.appThemeMode)
    }

    var editingProviderId by remember { mutableStateOf<String?>(null) }
    var backPressedOnce by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

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
                    (context as? android.app.Activity)?.finish()
                } else {
                    backPressedOnce = true
                    android.widget.Toast.makeText(context, "再按一次退出 AI-TTS", android.widget.Toast.LENGTH_SHORT).show()
                    scope.launch {
                        kotlinx.coroutines.delay(2000)
                        backPressedOnce = false
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalPulseColors provides dynamicColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PulseTokens.CanvasDeep)
        ) {
            if (editingProviderId != null) {
                PulseProviderConfigScreen(
                    providerId = editingProviderId!!,
                    configDataStore = configDataStore,
                    onNavigateBack = { editingProviderId = null }
                )
            } else {
                // 4 大主工作台手势滑动容器
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) { page ->
                    when (page) {
                        0 -> {
                            when (settings.appUiStyle) {
                                "BENTO" -> BentoHubScreen(
                                    configDataStore = configDataStore,
                                    onNavigateToEditProvider = { editingProviderId = it },
                                    onOpenDeck = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                )
                                "STUDIO" -> StudioHubScreen(
                                    configDataStore = configDataStore,
                                    onNavigateToEditProvider = { editingProviderId = it },
                                    onOpenDeck = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                )
                                "VINYL" -> VinylHubScreen(
                                    configDataStore = configDataStore,
                                    onNavigateToEditProvider = { editingProviderId = it },
                                    onOpenDeck = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    }
                                )
                                else -> PulseHubScreen(
                                    configDataStore = configDataStore,
                                    onNavigateToEditProvider = { editingProviderId = it },
                                    onOpenDeck = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    parentPagerState = pagerState
                                )
                            }
                        }
                        1 -> PulseDeckScreen(
                            configDataStore = configDataStore,
                            onNavigateToEditProvider = { editingProviderId = it },
                            onAddNewProvider = {
                                editingProviderId = "new_${System.currentTimeMillis() % 10000}"
                            },
                            parentPagerState = pagerState
                        )
                        2 -> PulsePipelineScreen(
                            configDataStore = configDataStore,
                            parentPagerState = pagerState
                        )
                        3 -> PulseStudioSettingsScreen(
                            configDataStore = configDataStore,
                            parentPagerState = pagerState,
                            onNavigateBackToRules = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                    }
                }

            // 底部悬浮导航栏
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .shadow(8.dp, CircleShape),
                shape = CircleShape,
                color = PulseTokens.SurfaceDark.copy(alpha = 0.96f),
                border = PulseTokens.BorderSubtle
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        TabItem("主页", Icons.Default.GraphicEq),
                        TabItem("模型", Icons.Default.RecordVoiceOver),
                        TabItem("规则", Icons.Default.Spellcheck),
                        TabItem("设置", Icons.Default.Tune)
                    )

                    tabs.forEachIndexed { index, tab ->
                        val isSelected = pagerState.currentPage == index
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                            shape = CircleShape,
                            color = if (isSelected) PulseTokens.CyanElectric.copy(alpha = 0.2f) else Color.Transparent,
                            border = if (isSelected) BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.6f)) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                if (isSelected) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PulseTokens.CyanElectric
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

private data class TabItem(val title: String, val icon: ImageVector)
