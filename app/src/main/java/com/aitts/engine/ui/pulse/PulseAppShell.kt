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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.ui.pulse.screens.PulseDeckScreen
import com.aitts.engine.ui.pulse.screens.PulseHubScreen
import com.aitts.engine.ui.pulse.screens.PulsePipelineScreen
import com.aitts.engine.ui.pulse.screens.PulseProviderConfigScreen
import com.aitts.engine.ui.pulse.screens.PulseStudioSettingsScreen
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.launch

/**
 * ⚡ Pulse 第四主题全局流体容器 (Pulse App Shell & Fluid Dock)
 * 1. 托管 4 大流体分舱：中枢、模型、规则、设置；
 * 2. 左右手势自由滑动（HorizontalPager）与底部中心胶囊导航坞双向联动；
 * 3. 悬浮底栏严格处于大拇指落点舒适区，与右下角 Action Hub 层次分明。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PulseAppShell(
    configDataStore: ConfigDataStore,
    onNavigateToTestBench: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })

    var editingProviderId by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = editingProviderId != null) {
        editingProviderId = null
    }

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
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> PulseHubScreen(
                        configDataStore = configDataStore,
                        onNavigateToEditProvider = { editingProviderId = it },
                        onOpenDeck = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> PulseDeckScreen(
                        configDataStore = configDataStore,
                        onNavigateToEditProvider = { editingProviderId = it },
                        onAddNewProvider = {
                            editingProviderId = "new_${System.currentTimeMillis() % 10000}"
                        }
                    )
                    2 -> PulsePipelineScreen(
                        configDataStore = configDataStore
                    )
                    3 -> PulseStudioSettingsScreen(
                        configDataStore = configDataStore
                    )
                }
            }

            // 底部流体微胶囊导航坞（居中悬浮在大拇指舒适区）
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .shadow(12.dp, CircleShape),
                shape = CircleShape,
                color = PulseTokens.SurfaceDark.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf(
                        TabItem("中枢", Icons.Default.GraphicEq),
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

private data class TabItem(val title: String, val icon: ImageVector)
