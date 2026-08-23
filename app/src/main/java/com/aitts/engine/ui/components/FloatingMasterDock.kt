package com.aitts.engine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.ui.theme.BrandTheme
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 悬浮主控坞形态模式 (Universal Floating Master Dock Display Modes)
 */
enum class DockDisplayMode {
    EXPANDED_HORIZONTAL, // 底部横向全功能大胶囊
    SIDEBAR_VERTICAL,     // 贴边宽裕竖向侧边栏 (防误触宽松间距)
    PIE_RADIAL,          // 极坐标扇形/环形轮盘 (Pie Radial Fan)
    EDGE_STASHED         // 贴边自动收纳把手 (微缩贴边挂起)
}

/**
 * 🌟 全主题通用人机工学自由拖拽主控悬浮坞 (Universal Draggable Floating Master Dock)
 * 1. 物理位置持久化 (`rememberSaveable`)，默认底边对齐，不再在操作或重组时重回顶部；
 * 2. 靠边松手自动吸附收纳 (`EDGE_STASHED`)，侧边显示拖动把手，长按/点击把手还原；
 * 3. 防误触二级主题菜单 (`DropdownMenu`)，可安全切换工作台主题与悬浮坞形态；
 * 4. 所有内部功能按键支持长按浮出文字说明气泡 (`TooltipBadge`)；
 * 5. 竖排侧边栏加大按键尺寸与间距，彻底消除拥挤误触；
 * 6. 全新 Pie 扇形轮盘模式，以极坐标扇形展开 5 瓣发光声控按键；
 * 7. 实时流式波形响应与双击直达模型配置。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingMasterDock(
    modifier: Modifier = Modifier,
    activeProvider: TtsProviderConfig?,
    currentUiStyle: String,
    isPlaying: Boolean,
    isSynthesizing: Boolean,
    onPlayToggle: () -> Unit,
    onRandomQuote: () -> Unit,
    onSwitchUiStyle: (String) -> Unit,
    onOpenProviderConfig: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val activeBrandColor = remember(activeProvider?.type) {
        activeProvider?.let { BrandTheme.getColorForType(it.type) }
    } ?: MaterialTheme.colorScheme.primary

    // 🌟 位置与模式持久化（严禁操作时重置到顶部）
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var dockMode by rememberSaveable { mutableStateOf(DockDisplayMode.EXPANDED_HORIZONTAL) }
    var previousMode by rememberSaveable { mutableStateOf(DockDisplayMode.EXPANDED_HORIZONTAL) }
    var isLeftEdge by rememberSaveable { mutableStateOf(false) }

    // Pie 轮盘展开状态
    var isPieExpanded by rememberSaveable { mutableStateOf(false) }

    // 主题与形态二级菜单
    var showThemeMenu by remember { mutableStateOf(false) }

    // 长按文字浮动提示
    var activeTooltipText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeTooltipText) {
        if (activeTooltipText != null) {
            delay(2200)
            activeTooltipText = null
        }
    }

    // 整体悬浮外层容器：默认锚定在屏幕底部
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 72.dp),
        contentAlignment = when (dockMode) {
            DockDisplayMode.SIDEBAR_VERTICAL -> if (isLeftEdge) Alignment.CenterStart else Alignment.CenterEnd
            DockDisplayMode.EDGE_STASHED -> if (isLeftEdge) Alignment.CenterStart else Alignment.CenterEnd
            DockDisplayMode.PIE_RADIAL -> Alignment.BottomCenter
            DockDisplayMode.EXPANDED_HORIZONTAL -> Alignment.BottomCenter
        }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .zIndex(99f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 🌟 长按浮出文字提示气泡 (Floating Tooltip Badge)
                AnimatedVisibility(
                    visible = activeTooltipText != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = activeTooltipText ?: "",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // 🌟 根据形态模式渲染对应交互界面
                when (dockMode) {
                    DockDisplayMode.EDGE_STASHED -> {
                        // 🗄️ 贴边自动收纳把手 (Minimalist Edge Handle)
                        Surface(
                            modifier = Modifier
                                .width(32.dp)
                                .height(68.dp)
                                .shadow(8.dp, if (isLeftEdge) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetY += dragAmount.y
                                            offsetX += dragAmount.x
                                        },
                                        onDragEnd = {
                                            isLeftEdge = offsetX < 0
                                            if (kotlin.math.abs(offsetX) < 120) {
                                                dockMode = previousMode
                                                offsetX = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        }
                                    )
                                }
                                .combinedClickable(
                                    onClick = {
                                        dockMode = previousMode
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onLongClick = {
                                        dockMode = previousMode
                                        activeTooltipText = "已退出收纳模式"
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ),
                            shape = if (isLeftEdge) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isPlaying) Color(0xFF10B981) else activeBrandColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "展开收纳",
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    DockDisplayMode.SIDEBAR_VERTICAL -> {
                        // ↕️ 贴边宽裕竖排侧边栏 (Spacious Ergonomic Sidebar)
                        Surface(
                            modifier = Modifier
                                .width(60.dp)
                                .shadow(10.dp, RoundedCornerShape(22.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            isLeftEdge = offsetX < 0
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 拖拽指示条
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                                )

                                // 播放/停止大按键 (44dp 触摸区)
                                Surface(
                                    shape = CircleShape,
                                    color = activeBrandColor,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .combinedClickable(
                                            onClick = onPlayToggle,
                                            onLongClick = {
                                                activeTooltipText = if (isPlaying) "点击停止当前朗读" else "点击立即试听当前发音"
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isSynthesizing) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "播放/停止",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                // 随机语料
                                DockIconButton(
                                    icon = Icons.Default.Casino,
                                    tooltip = "随机换句小说语料",
                                    tint = activeBrandColor,
                                    onClick = onRandomQuote,
                                    onLongClick = { activeTooltipText = "随机切换语料库名言" }
                                )

                                // 主题与形态菜单
                                Box {
                                    DockIconButton(
                                        icon = Icons.Default.Tune,
                                        tooltip = "主题与形态切换菜单",
                                        tint = MaterialTheme.colorScheme.primary,
                                        onClick = { showThemeMenu = true },
                                        onLongClick = { activeTooltipText = "弹出主题与形态设置菜单" }
                                    )
                                    DockThemeDropdown(
                                        expanded = showThemeMenu,
                                        currentUiStyle = currentUiStyle,
                                        currentDockMode = dockMode,
                                        onDismiss = { showThemeMenu = false },
                                        onSwitchUiStyle = onSwitchUiStyle,
                                        onSwitchDockMode = { newMode ->
                                            previousMode = dockMode
                                            dockMode = newMode
                                            showThemeMenu = false
                                        }
                                    )
                                }

                                // 切换至横向
                                DockIconButton(
                                    icon = Icons.Default.ViewAgenda,
                                    tooltip = "切换为横向大胶囊",
                                    tint = MaterialTheme.colorScheme.outline,
                                    onClick = {
                                        previousMode = dockMode
                                        dockMode = DockDisplayMode.EXPANDED_HORIZONTAL
                                    },
                                    onLongClick = { activeTooltipText = "转为底部横向大胶囊" }
                                )

                                // 收纳至边框
                                DockIconButton(
                                    icon = Icons.Default.CloseFullscreen,
                                    tooltip = "收纳至屏幕侧边",
                                    tint = MaterialTheme.colorScheme.outline,
                                    onClick = {
                                        previousMode = dockMode
                                        dockMode = DockDisplayMode.EDGE_STASHED
                                    },
                                    onLongClick = { activeTooltipText = "收缩为极简贴边把手" }
                                )
                            }
                        }
                    }

                    DockDisplayMode.PIE_RADIAL -> {
                        // 🥧 全新 Pie 扇形/极坐标轮盘模式 (Radial Pie Fan Menu)
                        Box(contentAlignment = Alignment.Center) {
                            // 展开的 5 瓣扇形功能按键
                            val radiusPx = with(density) { 86.dp.toPx() }
                            val pieButtons = listOf(
                                Triple(Icons.Default.Casino, "换名言") { onRandomQuote() },
                                Triple(Icons.Default.Tune, "菜单") { showThemeMenu = true },
                                Triple(Icons.Default.Settings, "配置") { activeProvider?.let { onOpenProviderConfig(it.id) } },
                                Triple(Icons.Default.CloseFullscreen, "收纳") {
                                    previousMode = dockMode
                                    dockMode = DockDisplayMode.EDGE_STASHED
                                },
                                Triple(Icons.Default.ViewAgenda, "横胶囊") {
                                    previousMode = dockMode
                                    dockMode = DockDisplayMode.EXPANDED_HORIZONTAL
                                }
                            )

                            pieButtons.forEachIndexed { index, (icon, label, action) ->
                                val angleRad = Math.toRadians((index * (360.0 / pieButtons.size) - 90.0))
                                val targetX = (radiusPx * cos(angleRad)).toFloat()
                                val targetY = (radiusPx * sin(angleRad)).toFloat()

                                val animatedX by animateFloatAsState(if (isPieExpanded) targetX else 0f, spring())
                                val animatedY by animateFloatAsState(if (isPieExpanded) targetY else 0f, spring())
                                val animatedScale by animateFloatAsState(if (isPieExpanded) 1f else 0f, spring())

                                Surface(
                                    modifier = Modifier
                                        .offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
                                        .scale(animatedScale)
                                        .size(42.dp)
                                        .shadow(6.dp, CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                action()
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onLongClick = {
                                                activeTooltipText = "Pie扇区: $label"
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        ),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.5f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(icon, contentDescription = label, tint = activeBrandColor, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            // 核心中心声灵球 (Central Pie Hub)
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(12.dp, CircleShape)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetX += dragAmount.x
                                                offsetY += dragAmount.y
                                            },
                                            onDragEnd = {
                                                isLeftEdge = offsetX < 0
                                            }
                                        )
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            isPieExpanded = !isPieExpanded
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDoubleClick = {
                                            onPlayToggle()
                                        },
                                        onLongClick = {
                                            activeTooltipText = "轻触展开/折叠扇形轮盘，双击播放/停止"
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ),
                                shape = CircleShape,
                                color = if (isPlaying) activeBrandColor else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(2.dp, activeBrandColor)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSynthesizing) {
                                        CircularProgressIndicator(modifier = Modifier.size(26.dp), color = activeBrandColor, strokeWidth = 2.5.dp)
                                    } else {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PieChart,
                                            contentDescription = "Pie轮盘核心",
                                            tint = if (isPlaying) Color.White else activeBrandColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }

                            // 菜单弹窗挂载
                            DockThemeDropdown(
                                expanded = showThemeMenu,
                                currentUiStyle = currentUiStyle,
                                currentDockMode = dockMode,
                                onDismiss = { showThemeMenu = false },
                                onSwitchUiStyle = onSwitchUiStyle,
                                onSwitchDockMode = { newMode ->
                                    previousMode = dockMode
                                    dockMode = newMode
                                    showThemeMenu = false
                                }
                            )
                        }
                    }

                    DockDisplayMode.EXPANDED_HORIZONTAL -> {
                        // ↔️ 底部横向全功能大胶囊 (Full Horizontal Capsule)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .shadow(10.dp, RoundedCornerShape(20.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            isLeftEdge = offsetX < 0
                                        }
                                    )
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：模型名称与配置直达
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .combinedClickable(
                                            onClick = {
                                                activeProvider?.let { onOpenProviderConfig(it.id) }
                                            },
                                            onLongClick = {
                                                activeTooltipText = "点击直达【${activeProvider?.name ?: "当前模型"}】高级配置"
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(
                                                if (isPlaying) Color(0xFF10B981) else activeBrandColor,
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = activeProvider?.name ?: "未选择引擎",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${activeProvider?.type?.displayName ?: "系统"} · 音色: ${activeProvider?.voiceId?.ifBlank { "默认" }}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 右侧操作功能键组
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 随机语料
                                    DockIconButton(
                                        icon = Icons.Default.Casino,
                                        tooltip = "随机换句小说语料",
                                        tint = activeBrandColor,
                                        onClick = onRandomQuote,
                                        onLongClick = {
                                            activeTooltipText = "随机名言语料库"
                                        }
                                    )

                                    // 主题与形态二级菜单 (防误触)
                                    Box {
                                        DockIconButton(
                                            icon = Icons.Default.Tune,
                                            tooltip = "主题与形态切换菜单",
                                            tint = MaterialTheme.colorScheme.primary,
                                            onClick = { showThemeMenu = true },
                                            onLongClick = {
                                                activeTooltipText = "弹出主题与形态设置菜单"
                                            }
                                        )
                                        DockThemeDropdown(
                                            expanded = showThemeMenu,
                                            currentUiStyle = currentUiStyle,
                                            currentDockMode = dockMode,
                                            onDismiss = { showThemeMenu = false },
                                            onSwitchUiStyle = onSwitchUiStyle,
                                            onSwitchDockMode = { newMode ->
                                                previousMode = dockMode
                                                dockMode = newMode
                                                showThemeMenu = false
                                            }
                                        )
                                    }

                                    // 播放 / 停止实体按键
                                    Button(
                                        onClick = onPlayToggle,
                                        colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        if (isSynthesizing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                contentDescription = "播放/停止",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isPlaying) "停止" else "试听", fontSize = 12.sp, color = Color.White)
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
}

/**
 * 带有长按气泡提示的悬浮按键
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockIconButton(
    icon: ImageVector,
    tooltip: String,
    tint: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(36.dp)
            .combinedClickable(
                onClick = {
                    onClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = tooltip, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * 🌟 防误触二级主题与形态下拉选择菜单
 */
@Composable
private fun DockThemeDropdown(
    expanded: Boolean,
    currentUiStyle: String,
    currentDockMode: DockDisplayMode,
    onDismiss: () -> Unit,
    onSwitchUiStyle: (String) -> Unit,
    onSwitchDockMode: (DockDisplayMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = "🎨 软件工作台主题",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        DropdownMenuItem(
            text = { Text("🚀 Bento 全息声球工作台" + if (currentUiStyle == "BENTO") " (当前)" else "") },
            onClick = { onSwitchUiStyle("BENTO"); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("🎛️ DAW 专业调音台" + if (currentUiStyle == "STUDIO") " (当前)" else "") },
            onClick = { onSwitchUiStyle("STUDIO"); onDismiss() }
        )
        DropdownMenuItem(
            text = { Text("📻 Vinyl 黑胶沉浸阅览舱" + if (currentUiStyle == "VINYL") " (当前)" else "") },
            onClick = { onSwitchUiStyle("VINYL"); onDismiss() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "📐 悬浮主控坞形态",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        DropdownMenuItem(
            text = { Text("↔️ 横向全功能大胶囊" + if (currentDockMode == DockDisplayMode.EXPANDED_HORIZONTAL) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.EXPANDED_HORIZONTAL) }
        )
        DropdownMenuItem(
            text = { Text("↕️ 宽裕竖排侧边栏" + if (currentDockMode == DockDisplayMode.SIDEBAR_VERTICAL) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.SIDEBAR_VERTICAL) }
        )
        DropdownMenuItem(
            text = { Text("🥧 Pie 扇形极坐标轮盘" + if (currentDockMode == DockDisplayMode.PIE_RADIAL) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.PIE_RADIAL) }
        )
        DropdownMenuItem(
            text = { Text("🗄️ 贴边极简收纳把手" + if (currentDockMode == DockDisplayMode.EDGE_STASHED) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.EDGE_STASHED) }
        )
    }
}
