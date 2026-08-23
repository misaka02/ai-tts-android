package com.aitts.engine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.ui.theme.BrandTheme
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 悬浮主控坞形态模式 (Universal Floating Master Dock Display Modes)
 */
enum class DockDisplayMode {
    EXPANDED_HORIZONTAL, // 底部横向全功能大胶囊
    SIDEBAR_VERTICAL,     // 竖向侧边栏 (宽松大间距防误触)
    PIE_RADIAL,          // 自适应极速扇形轮盘 (Pie Radial Fan Menu)
    EDGE_STASHED         // 独立贴边极简收纳 (仅留侧边小把手)
}

enum class TooltipPosition {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT
}

/**
 * 🌟 全主题通用人机工学自由拖拽主控悬浮坞 (v3.3.0 Ultra-Fluid Edition)
 * 1. 【长按禁止位移，滑动直接拖拽】：长按菜单不动时 100% 锁定悬浮坞，手势绝不串动；快速按下并滑动时取消长按，平滑拖动悬浮坞。
 * 2. 【多句并行预拉取流水线】：彻底消除整页阅读时段落与句子间的 2~3 秒停顿。
 * 3. 【顶层 Popup 气泡】：浮动提示绝不被手指遮挡。
 */
@Composable
fun FloatingMasterDock(
    modifier: Modifier = Modifier,
    activeProvider: TtsProviderConfig?,
    currentUiStyle: String,
    dockModeName: String = "EXPANDED_HORIZONTAL",
    initialX: Float = 0f,
    initialY: Float = 0f,
    isPlaying: Boolean,
    isSynthesizing: Boolean,
    onPlayToggle: () -> Unit,
    onRandomQuote: () -> Unit,
    onSwitchUiStyle: (String) -> Unit,
    onOpenProviderConfig: (String) -> Unit,
    onUpdateDockState: (mode: String, x: Float, y: Float) -> Unit = { _, _, _ -> }
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val activeBrandColor = remember(activeProvider?.type) {
        activeProvider?.let { BrandTheme.getColorForType(it.type) }
    } ?: MaterialTheme.colorScheme.primary

    var dockMode by remember(dockModeName) {
        mutableStateOf(
            try {
                DockDisplayMode.valueOf(dockModeName)
            } catch (e: Exception) {
                DockDisplayMode.EXPANDED_HORIZONTAL
            }
        )
    }

    var posX by remember { mutableFloatStateOf(initialX) }
    var posY by remember { mutableFloatStateOf(initialY) }

    var isPieExpanded by remember { mutableStateOf(false) }
    var showModeMenu by remember { mutableStateOf(false) }
    var showPlayQuickConfig by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 68.dp)
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        val (dockWidthPx, dockHeightPx) = when (dockMode) {
            DockDisplayMode.EXPANDED_HORIZONTAL -> with(density) { Pair((maxWidth - 28.dp).toPx(), 54.dp.toPx()) }
            DockDisplayMode.SIDEBAR_VERTICAL -> with(density) { Pair(62.dp.toPx(), 280.dp.toPx()) }
            DockDisplayMode.PIE_RADIAL -> with(density) { Pair(56.dp.toPx(), 56.dp.toPx()) }
            DockDisplayMode.EDGE_STASHED -> with(density) { Pair(26.dp.toPx(), 64.dp.toPx()) }
        }

        val minX = when (dockMode) {
            DockDisplayMode.EXPANDED_HORIZONTAL -> 0f
            DockDisplayMode.SIDEBAR_VERTICAL -> -screenWidthPx / 2f + dockWidthPx / 2f + with(density) { 6.dp.toPx() }
            DockDisplayMode.PIE_RADIAL -> -screenWidthPx / 2f + dockWidthPx / 2f + with(density) { 20.dp.toPx() }
            DockDisplayMode.EDGE_STASHED -> -screenWidthPx / 2f + dockWidthPx / 2f
        }
        val maxX = when (dockMode) {
            DockDisplayMode.EXPANDED_HORIZONTAL -> 0f
            DockDisplayMode.SIDEBAR_VERTICAL -> screenWidthPx / 2f - dockWidthPx / 2f - with(density) { 6.dp.toPx() }
            DockDisplayMode.PIE_RADIAL -> screenWidthPx / 2f - dockWidthPx / 2f - with(density) { 20.dp.toPx() }
            DockDisplayMode.EDGE_STASHED -> screenWidthPx / 2f - dockWidthPx / 2f
        }
        val minY = -screenHeightPx + dockHeightPx + with(density) { 72.dp.toPx() }
        val maxY = 0f

        val clampedX = if (dockMode == DockDisplayMode.EDGE_STASHED) {
            if (posX < 0) minX else maxX
        } else {
            posX.coerceIn(minX, maxX)
        }
        val clampedY = posY.coerceIn(minY, maxY)

        val isAtLeft = clampedX < 0

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                .zIndex(99f)
        ) {
            when (dockMode) {
                DockDisplayMode.EDGE_STASHED -> {
                    // 🗄️ 贴边极简收纳把手（严格贴合侧边，上下滑动定位，轻触展开对应位置侧面板）
                    Surface(
                        modifier = Modifier
                            .width(26.dp)
                            .height(64.dp)
                            .graphicsLayer {
                                shadowElevation = 8.dp.toPx()
                                shape = if (isAtLeft) RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp) else RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                                clip = true
                            },
                        shape = if (isAtLeft) RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp) else RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.6f))
                    ) {
                        FluidTouchWrapper(
                            tooltip = "展开侧边面板",
                            tooltipPosition = if (isAtLeft) TooltipPosition.RIGHT else TooltipPosition.LEFT,
                            onClick = {
                                dockMode = DockDisplayMode.SIDEBAR_VERTICAL
                                posX = if (isAtLeft) minX else maxX
                                onUpdateDockState(dockMode.name, posX, posY)
                            },
                            onDrag = { delta ->
                                posY += delta.y
                                posX += delta.x
                            },
                            onDragEnd = {
                                posX = if (posX < 0) minX else maxX
                                onUpdateDockState(dockMode.name, posX, posY)
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isPlaying) Color(0xFF10B981) else activeBrandColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "点击展开侧面板",
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                DockDisplayMode.SIDEBAR_VERTICAL -> {
                    // ↕️ 竖排侧边栏 (宽松布局，绑定对应侧边位置)
                    Surface(
                        modifier = Modifier
                            .width(62.dp)
                            .graphicsLayer {
                                shadowElevation = 10.dp.toPx()
                                shape = RoundedCornerShape(22.dp)
                                clip = true
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 拖拽手柄（专职拖拽，轻触不触发事件）
                            Box(
                                modifier = Modifier
                                    .width(32.dp)
                                    .height(14.dp)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                posX += dragAmount.x
                                                posY += dragAmount.y
                                            },
                                            onDragEnd = {
                                                onUpdateDockState(dockMode.name, posX, posY)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                                )
                            }

                            // 播放 / 停止主键 (长按弹出配置快捷键，长按不动禁止移动)
                            Box {
                                FluidTouchWrapper(
                                    tooltip = if (isPlaying) "停止" else "试听",
                                    tooltipPosition = if (isAtLeft) TooltipPosition.RIGHT else TooltipPosition.LEFT,
                                    onClick = onPlayToggle,
                                    onLongRelease = { showPlayQuickConfig = true },
                                    onDrag = { delta ->
                                        posX += delta.x
                                        posY += delta.y
                                    },
                                    onDragEnd = {
                                        onUpdateDockState(dockMode.name, posX, posY)
                                    }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = activeBrandColor,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isSynthesizing) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
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
                                }

                                if (showPlayQuickConfig) {
                                    Popup(
                                        alignment = if (isAtLeft) Alignment.CenterEnd else Alignment.CenterStart,
                                        offset = IntOffset(if (isAtLeft) 130 else -130, 0),
                                        onDismissRequest = { showPlayQuickConfig = false }
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.inverseSurface,
                                            shadowElevation = 8.dp,
                                            modifier = Modifier.pointerInput(Unit) {
                                                awaitEachGesture {
                                                    awaitFirstDown()
                                                    activeProvider?.let { onOpenProviderConfig(it.id) }
                                                    showPlayQuickConfig = false
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "⚙️ 进入模型设置",
                                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // 随机语料
                            FluidTouchWrapper(
                                tooltip = "换句",
                                tooltipPosition = if (isAtLeft) TooltipPosition.RIGHT else TooltipPosition.LEFT,
                                onClick = onRandomQuote,
                                onDrag = { delta ->
                                    posX += delta.x
                                    posY += delta.y
                                },
                                onDragEnd = {
                                    onUpdateDockState(dockMode.name, posX, posY)
                                }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Casino, contentDescription = "随机语料", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // 形态与主题二级菜单
                            Box {
                                FluidTouchWrapper(
                                    tooltip = "菜单",
                                    tooltipPosition = if (isAtLeft) TooltipPosition.RIGHT else TooltipPosition.LEFT,
                                    onClick = { showModeMenu = true },
                                    onDrag = { delta ->
                                        posX += delta.x
                                        posY += delta.y
                                    },
                                    onDragEnd = {
                                        onUpdateDockState(dockMode.name, posX, posY)
                                    }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "功能菜单", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                DockModeDropdown(
                                    expanded = showModeMenu,
                                    currentDockMode = dockMode,
                                    currentUiStyle = currentUiStyle,
                                    onDismiss = { showModeMenu = false },
                                    onSwitchDockMode = { newMode ->
                                        dockMode = newMode
                                        if (newMode == DockDisplayMode.EDGE_STASHED) {
                                            posX = if (isAtLeft) minX else maxX
                                        }
                                        onUpdateDockState(newMode.name, posX, posY)
                                        showModeMenu = false
                                    },
                                    onSwitchUiStyle = onSwitchUiStyle
                                )
                            }

                            // 贴边收纳快捷键
                            FluidTouchWrapper(
                                tooltip = "收纳",
                                tooltipPosition = if (isAtLeft) TooltipPosition.RIGHT else TooltipPosition.LEFT,
                                onClick = {
                                    dockMode = DockDisplayMode.EDGE_STASHED
                                    posX = if (isAtLeft) minX else maxX
                                    onUpdateDockState(dockMode.name, posX, posY)
                                },
                                onDrag = { delta ->
                                    posX += delta.x
                                    posY += delta.y
                                },
                                onDragEnd = {
                                    onUpdateDockState(dockMode.name, posX, posY)
                                }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloseFullscreen, contentDescription = "贴边收纳", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                DockDisplayMode.PIE_RADIAL -> {
                    // 🥧 自适应极速 Pie 扇形轮盘 (GPU hardware accelerated via graphicsLayer)
                    Box(contentAlignment = Alignment.Center) {
                        val expandProgress by animateFloatAsState(
                            targetValue = if (isPieExpanded) 1f else 0f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 800f)
                        )

                        val radiusPx = with(density) { 92.dp.toPx() }

                        val isNearRight = clampedX > screenWidthPx * 0.18f
                        val isNearLeft = clampedX < -screenWidthPx * 0.18f
                        val isNearTop = clampedY < -screenHeightPx * 0.6f

                        data class PieItem(
                            val icon: ImageVector,
                            val label: String,
                            val isPrimary: Boolean = false,
                            val action: () -> Unit
                        )

                        val pieItems = listOf(
                            PieItem(
                                icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                label = if (isPlaying) "停止" else "试听",
                                isPrimary = true,
                                action = {
                                    onPlayToggle()
                                    isPieExpanded = false
                                }
                            ),
                            PieItem(
                                icon = Icons.Default.Casino,
                                label = "换句",
                                action = {
                                    // 连续快速换句，不强制收起轮盘
                                    onRandomQuote()
                                }
                            ),
                            PieItem(
                                icon = Icons.Default.Settings,
                                label = "配置",
                                action = {
                                    activeProvider?.let { onOpenProviderConfig(it.id) }
                                    isPieExpanded = false
                                }
                            ),
                            PieItem(
                                icon = Icons.Default.MoreVert,
                                label = "菜单",
                                action = {
                                    showModeMenu = true
                                    isPieExpanded = false
                                }
                            ),
                            PieItem(
                                icon = Icons.Default.CloseFullscreen,
                                label = "收纳",
                                action = {
                                    dockMode = DockDisplayMode.EDGE_STASHED
                                    posX = if (isAtLeft) minX else maxX
                                    onUpdateDockState(dockMode.name, posX, posY)
                                    isPieExpanded = false
                                }
                            )
                        )

                        val angles = remember(isNearRight, isNearLeft, isNearTop) {
                            when {
                                isNearRight -> listOf(115.0, 145.0, 180.0, 215.0, 245.0) // 靠右侧：向左扇形展开
                                isNearLeft -> listOf(-65.0, -35.0, 0.0, 35.0, 65.0)       // 靠左侧：向右扇形展开
                                isNearTop -> listOf(25.0, 55.0, 90.0, 125.0, 155.0)      // 靠顶部：向下扇形展开
                                else -> listOf(-150.0, -115.0, -75.0, -35.0, 0.0)       // 靠底部：向上扇形展开
                            }
                        }

                        pieItems.forEachIndexed { index, item ->
                            val angleRad = Math.toRadians(angles.getOrElse(index) { index * 72.0 - 90.0 })
                            val targetX = (radiusPx * cos(angleRad)).toFloat()
                            val targetY = (radiusPx * sin(angleRad)).toFloat()

                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationX = targetX * expandProgress
                                        translationY = targetY * expandProgress
                                        scaleX = expandProgress
                                        scaleY = expandProgress
                                        alpha = expandProgress
                                    }
                            ) {
                                FluidTouchWrapper(
                                    tooltip = item.label,
                                    tooltipPosition = TooltipPosition.ABOVE,
                                    onClick = item.action
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(42.dp),
                                            shape = CircleShape,
                                            color = if (item.isPrimary) activeBrandColor else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.5.dp, activeBrandColor.copy(alpha = 0.7f)),
                                            shadowElevation = 6.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.label,
                                                    tint = if (item.isPrimary) Color.White else activeBrandColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                        ) {
                                            Text(
                                                text = item.label,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 中心主控 Hub 球（长按不动锁定，按下拖动即位移）
                        Surface(
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer {
                                    shadowElevation = 10.dp.toPx()
                                    shape = CircleShape
                                    clip = true
                                },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(2.dp, activeBrandColor)
                        ) {
                            FluidTouchWrapper(
                                tooltip = if (isPieExpanded) "收起轮盘" else "展开轮盘",
                                tooltipPosition = TooltipPosition.ABOVE,
                                onClick = {
                                    isPieExpanded = !isPieExpanded
                                },
                                onDrag = { delta ->
                                    posX += delta.x
                                    posY += delta.y
                                },
                                onDragEnd = {
                                    onUpdateDockState(dockMode.name, posX, posY)
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = "Pie轮盘控制中心",
                                        tint = activeBrandColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        DockModeDropdown(
                            expanded = showModeMenu,
                            currentDockMode = dockMode,
                            currentUiStyle = currentUiStyle,
                            onDismiss = { showModeMenu = false },
                            onSwitchDockMode = { newMode ->
                                dockMode = newMode
                                if (newMode == DockDisplayMode.EDGE_STASHED) {
                                    posX = if (isAtLeft) minX else maxX
                                }
                                onUpdateDockState(newMode.name, posX, posY)
                                showModeMenu = false
                            },
                            onSwitchUiStyle = onSwitchUiStyle
                        )
                    }
                }

                DockDisplayMode.EXPANDED_HORIZONTAL -> {
                    // ↔️ 横向全功能胶囊（左侧拖拽/配置，右侧功能键长按锁定位移）
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .graphicsLayer {
                                shadowElevation = 10.dp.toPx()
                                shape = RoundedCornerShape(20.dp)
                                clip = true
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：模型名称与可拖拽把手区 (长按可配置，按下拖动即位移)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                FluidTouchWrapper(
                                    tooltip = "进入模型配置",
                                    tooltipPosition = TooltipPosition.ABOVE,
                                    onClick = { activeProvider?.let { onOpenProviderConfig(it.id) } },
                                    onDrag = { delta ->
                                        posY += delta.y
                                    },
                                    onDragEnd = {
                                        onUpdateDockState(dockMode.name, posX, posY)
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                }
                            }

                            // 右侧功能键组（换语料、形态切换、试听，内部长按 100% 锁定悬浮坞位移）
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FluidTouchWrapper(
                                    tooltip = "换句",
                                    tooltipPosition = TooltipPosition.ABOVE,
                                    onClick = onRandomQuote,
                                    onDrag = { delta -> posY += delta.y },
                                    onDragEnd = { onUpdateDockState(dockMode.name, posX, posY) }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Casino, contentDescription = "换语料", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Box {
                                    FluidTouchWrapper(
                                        tooltip = "菜单",
                                        tooltipPosition = TooltipPosition.ABOVE,
                                        onClick = { showModeMenu = true },
                                        onDrag = { delta -> posY += delta.y },
                                        onDragEnd = { onUpdateDockState(dockMode.name, posX, posY) }
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.MoreVert, contentDescription = "形态菜单", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }

                                    DockModeDropdown(
                                        expanded = showModeMenu,
                                        currentDockMode = dockMode,
                                        currentUiStyle = currentUiStyle,
                                        onDismiss = { showModeMenu = false },
                                        onSwitchDockMode = { newMode ->
                                            dockMode = newMode
                                            if (newMode == DockDisplayMode.EDGE_STASHED) {
                                                posX = minX
                                            }
                                            onUpdateDockState(newMode.name, posX, posY)
                                            showModeMenu = false
                                        },
                                        onSwitchUiStyle = onSwitchUiStyle
                                    )
                                }

                                // 播放 / 停止实体按键 (长按弹出配置快捷键，长按不动锁定位移)
                                Box {
                                    FluidTouchWrapper(
                                        tooltip = if (isPlaying) "停止" else "试听",
                                        tooltipPosition = TooltipPosition.ABOVE,
                                        onClick = onPlayToggle,
                                        onLongRelease = { showPlayQuickConfig = true },
                                        onDrag = { delta -> posY += delta.y },
                                        onDragEnd = { onUpdateDockState(dockMode.name, posX, posY) }
                                    ) {
                                        Button(
                                            onClick = {}, // 由 FluidTouchWrapper 接管手势
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

                                    if (showPlayQuickConfig) {
                                        Popup(
                                            alignment = Alignment.TopCenter,
                                            offset = IntOffset(0, -110),
                                            onDismissRequest = { showPlayQuickConfig = false }
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.inverseSurface,
                                                shadowElevation = 8.dp,
                                                modifier = Modifier.pointerInput(Unit) {
                                                    awaitEachGesture {
                                                        awaitFirstDown()
                                                        activeProvider?.let { onOpenProviderConfig(it.id) }
                                                        showPlayQuickConfig = false
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "⚙️ 进入模型设置",
                                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
    }
}

/**
 * 🌟 流畅触控包裹器 (Fluid Touch Gesture & Precision Drag vs Hold Discrimination)
 * - 按住且手指数毫米内不动：100% 锁定悬浮坞，触发图标微放大与顶层 Popup 气泡，松手触发 onClick/onLongRelease；
 * - 按下并立刻滑动（超过 18px）：取消长按与气泡，立即触发平滑拖动。
 */
@Composable
private fun FluidTouchWrapper(
    tooltip: String,
    tooltipPosition: TooltipPosition = TooltipPosition.ABOVE,
    onClick: () -> Unit,
    onLongRelease: (() -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    var isInside by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var pressStartTime by remember { mutableStateOf(0L) }
    var initialTouchPos by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isInside && !isDragging) 1.22f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 500f)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressStartTime = System.currentTimeMillis()
                    initialTouchPos = down.position
                    isPressed = true
                    isInside = true
                    isDragging = false
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (!change.pressed) {
                            // 抬起
                            val elapsed = System.currentTimeMillis() - pressStartTime
                            if (isInside && !isDragging) {
                                if (elapsed > 550 && onLongRelease != null) {
                                    onLongRelease()
                                } else {
                                    onClick()
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            if (isDragging && onDragEnd != null) {
                                onDragEnd()
                            }
                            break
                        } else {
                            val pos = change.position
                            val dist = (pos - initialTouchPos).getDistance()

                            // 如果位移超过 18px，判定为用户意图拖动悬浮坞，取消按钮的长按与放大状态
                            if (!isDragging && dist > 18f && onDrag != null) {
                                isDragging = true
                                isPressed = false
                                isInside = false
                            }

                            if (isDragging) {
                                val delta = change.position - change.previousPosition
                                onDrag?.invoke(delta)
                            } else {
                                val inside = pos.x in 0f..size.width.toFloat() && pos.y in 0f..size.height.toFloat()
                                if (inside != isInside) {
                                    isInside = inside
                                }
                            }
                            change.consume()
                        }
                    }
                    isPressed = false
                    isInside = false
                    isDragging = false
                }
            }
    ) {
        // 浮动提示文字气泡（通过 Popup 顶层浮动渲染，绝不被手指挡住，绝不被任何父容器裁剪）
        if (isPressed && isInside && !isDragging && tooltip.isNotBlank()) {
            Popup(
                alignment = when (tooltipPosition) {
                    TooltipPosition.ABOVE -> Alignment.TopCenter
                    TooltipPosition.BELOW -> Alignment.BottomCenter
                    TooltipPosition.LEFT -> Alignment.CenterStart
                    TooltipPosition.RIGHT -> Alignment.CenterEnd
                },
                offset = when (tooltipPosition) {
                    TooltipPosition.ABOVE -> IntOffset(0, -120)
                    TooltipPosition.BELOW -> IntOffset(0, 120)
                    TooltipPosition.LEFT -> IntOffset(-130, 0)
                    TooltipPosition.RIGHT -> IntOffset(130, 0)
                },
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = tooltip,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }
        }

        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * 🌟 纯粹形态与工作台主题下拉切换菜单 (二级菜单全功能)
 */
@Composable
private fun DockModeDropdown(
    expanded: Boolean,
    currentDockMode: DockDisplayMode,
    currentUiStyle: String,
    onDismiss: () -> Unit,
    onSwitchDockMode: (DockDisplayMode) -> Unit,
    onSwitchUiStyle: (String) -> Unit
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
            text = { Text("Bento 工作台" + if (currentUiStyle == "BENTO") " ✓" else "") },
            onClick = {
                onSwitchUiStyle("BENTO")
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("DAW 调音台" + if (currentUiStyle == "STUDIO") " ✓" else "") },
            onClick = {
                onSwitchUiStyle("STUDIO")
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("黑胶阅览舱" + if (currentUiStyle == "VINYL" || currentUiStyle == "CLASSIC") " ✓" else "") },
            onClick = {
                onSwitchUiStyle("VINYL")
                onDismiss()
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = "📐 悬浮坞形态",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
        DropdownMenuItem(
            text = { Text("横向胶囊" + if (currentDockMode == DockDisplayMode.EXPANDED_HORIZONTAL) " ✓" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.EXPANDED_HORIZONTAL) }
        )
        DropdownMenuItem(
            text = { Text("竖排侧栏" + if (currentDockMode == DockDisplayMode.SIDEBAR_VERTICAL) " ✓" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.SIDEBAR_VERTICAL) }
        )
        DropdownMenuItem(
            text = { Text("Pie 轮盘" + if (currentDockMode == DockDisplayMode.PIE_RADIAL) " ✓" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.PIE_RADIAL) }
        )
        DropdownMenuItem(
            text = { Text("贴边收纳" + if (currentDockMode == DockDisplayMode.EDGE_STASHED) " ✓" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.EDGE_STASHED) }
        )
    }
}
