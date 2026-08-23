package com.aitts.engine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    SIDEBAR_VERTICAL,     // 自由移动竖向侧边栏 (宽松大间距防误触)
    PIE_RADIAL,          // 自适应极速扇形轮盘 (Pie Radial Fan Menu)
    EDGE_STASHED         // 独立贴边极简收纳 (仅留侧边小把手)
}

/**
 * 🌟 全主题通用人机工学自由拖拽主控悬浮坞 (Universal Bounded Draggable Floating Master Dock)
 * 1. 严格屏幕边界限制与统一坐标系，杜绝拖拽越界或消失；
 * 2. 状态与位置全局持久化，切换主题与重组时绝不重置或闪烁；
 * 3. 竖排样式完全自由移动，不与侧边强行绑定；
 * 4. 独立贴边收纳模式 (`EDGE_STASHED`)，隐藏时仅留侧边小把手，一键唤醒；
 * 5. 极速响应 Pie 扇形轮盘，120ms 瞬间弹出，靠近屏幕边缘时自适应向屏幕内侧展开；
 * 6. 防误触二级主题下拉菜单，长按所有按键即刻浮出文字说明气泡。
 */
@OptIn(ExperimentalFoundationApi::class)
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

    // 内部形态模式
    var dockMode by remember(dockModeName) {
        mutableStateOf(
            try {
                DockDisplayMode.valueOf(dockModeName)
            } catch (e: Exception) {
                DockDisplayMode.EXPANDED_HORIZONTAL
            }
        )
    }
    var previousMode by remember { mutableStateOf(DockDisplayMode.EXPANDED_HORIZONTAL) }

    // 物理坐标 (以屏幕正中心/底边为基准)
    var posX by remember { mutableFloatStateOf(initialX) }
    var posY by remember { mutableFloatStateOf(initialY) }

    // Pie 轮盘展开状态
    var isPieExpanded by remember { mutableStateOf(false) }

    // 主题与形态二级菜单
    var showThemeMenu by remember { mutableStateOf(false) }

    // 长按文字浮动提示
    var activeTooltipText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(activeTooltipText) {
        if (activeTooltipText != null) {
            delay(2000)
            activeTooltipText = null
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 68.dp)
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // 计算当前形态下的尺寸边界与安全钳位
        val (dockWidthPx, dockHeightPx) = when (dockMode) {
            DockDisplayMode.EXPANDED_HORIZONTAL -> with(density) { Pair((maxWidth - 28.dp).toPx(), 54.dp.toPx()) }
            DockDisplayMode.SIDEBAR_VERTICAL -> with(density) { Pair(60.dp.toPx(), 270.dp.toPx()) }
            DockDisplayMode.PIE_RADIAL -> with(density) { Pair(56.dp.toPx(), 56.dp.toPx()) }
            DockDisplayMode.EDGE_STASHED -> with(density) { Pair(28.dp.toPx(), 64.dp.toPx()) }
        }

        // 安全限制坐标，严禁飞出屏幕
        val minX = when (dockMode) {
            DockDisplayMode.EXPANDED_HORIZONTAL -> 0f
            DockDisplayMode.SIDEBAR_VERTICAL -> -screenWidthPx / 2f + dockWidthPx / 2f + with(density) { 8.dp.toPx() }
            DockDisplayMode.PIE_RADIAL -> -screenWidthPx / 2f + dockWidthPx / 2f + with(density) { 16.dp.toPx() }
            DockDisplayMode.EDGE_STASHED -> -screenWidthPx / 2f + dockWidthPx / 2f
        }
        val maxX = when (dockMode) {
            DockDisplayMode.EXPANDED_HORIZONTAL -> 0f
            DockDisplayMode.SIDEBAR_VERTICAL -> screenWidthPx / 2f - dockWidthPx / 2f - with(density) { 8.dp.toPx() }
            DockDisplayMode.PIE_RADIAL -> screenWidthPx / 2f - dockWidthPx / 2f - with(density) { 16.dp.toPx() }
            DockDisplayMode.EDGE_STASHED -> screenWidthPx / 2f - dockWidthPx / 2f
        }
        val minY = -screenHeightPx + dockHeightPx + with(density) { 72.dp.toPx() }
        val maxY = 0f

        val clampedX = posX.coerceIn(minX, maxX)
        val clampedY = posY.coerceIn(minY, maxY)

        // 判断当前位置偏向屏幕左侧还是右侧
        val isAtLeft = clampedX < 0

        // 统一在底部中央锚定并应用安全偏移
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
                .zIndex(99f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 🌟 长按浮出文字提示气泡 (Floating Tooltip Badge)
                AnimatedVisibility(
                    visible = activeTooltipText != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.94f),
                        shadowElevation = 6.dp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    ) {
                        Text(
                            text = activeTooltipText ?: "",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // 🌟 4 大形态交互视图
                when (dockMode) {
                    DockDisplayMode.EDGE_STASHED -> {
                        // 🗄️ 独立贴边极简收纳把手 (Minimalist Edge Handle)
                        Surface(
                            modifier = Modifier
                                .width(28.dp)
                                .height(64.dp)
                                .shadow(6.dp, if (isAtLeft) RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp) else RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            posY += dragAmount.y
                                            posX += dragAmount.x
                                        },
                                        onDragEnd = {
                                            // 靠边吸附
                                            posX = if (posX < 0) minX else maxX
                                            onUpdateDockState(dockMode.name, posX, posY)
                                        }
                                    )
                                }
                                .combinedClickable(
                                    onClick = {
                                        dockMode = previousMode
                                        posX = 0f
                                        onUpdateDockState(dockMode.name, posX, posY)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onLongClick = {
                                        dockMode = previousMode
                                        posX = 0f
                                        activeTooltipText = "已还原全功能主控坞"
                                        onUpdateDockState(dockMode.name, posX, posY)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ),
                            shape = if (isAtLeft) RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp) else RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp),
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
                                        .size(7.dp)
                                        .background(if (isPlaying) Color(0xFF10B981) else activeBrandColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "点击展开",
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    DockDisplayMode.SIDEBAR_VERTICAL -> {
                        // ↕️ 自由移动竖排侧边栏 (Freely Draggable Spacious Sidebar)
                        Surface(
                            modifier = Modifier
                                .width(60.dp)
                                .shadow(10.dp, RoundedCornerShape(22.dp))
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
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 拖拽手柄
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(4.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))
                                )

                                // 播放/停止按键 (44dp 宽裕触摸区)
                                Surface(
                                    shape = CircleShape,
                                    color = activeBrandColor,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .combinedClickable(
                                            onClick = onPlayToggle,
                                            onLongClick = {
                                                activeTooltipText = if (isPlaying) "点击停止朗读" else "点击试听当前发音"
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
                                    onLongClick = { activeTooltipText = "随机名言语料库" }
                                )

                                // 主题与形态二级菜单
                                Box {
                                    DockIconButton(
                                        icon = Icons.Default.Tune,
                                        tooltip = "主题与形态设置",
                                        tint = MaterialTheme.colorScheme.primary,
                                        onClick = { showThemeMenu = true },
                                        onLongClick = { activeTooltipText = "打开主题与形态菜单" }
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
                                            if (newMode == DockDisplayMode.EDGE_STASHED) {
                                                posX = if (isAtLeft) minX else maxX
                                            }
                                            onUpdateDockState(newMode.name, posX, posY)
                                            showThemeMenu = false
                                        }
                                    )
                                }

                                // 切换至横向大胶囊
                                DockIconButton(
                                    icon = Icons.Default.ViewAgenda,
                                    tooltip = "切换为横向大胶囊",
                                    tint = MaterialTheme.colorScheme.outline,
                                    onClick = {
                                        previousMode = dockMode
                                        dockMode = DockDisplayMode.EXPANDED_HORIZONTAL
                                        posX = 0f
                                        onUpdateDockState(dockMode.name, posX, posY)
                                    },
                                    onLongClick = { activeTooltipText = "转为底部横向胶囊" }
                                )

                                // 切换至贴边收纳
                                DockIconButton(
                                    icon = Icons.Default.CloseFullscreen,
                                    tooltip = "贴边隐藏收纳",
                                    tint = MaterialTheme.colorScheme.outline,
                                    onClick = {
                                        previousMode = dockMode
                                        dockMode = DockDisplayMode.EDGE_STASHED
                                        posX = if (isAtLeft) minX else maxX
                                        onUpdateDockState(dockMode.name, posX, posY)
                                    },
                                    onLongClick = { activeTooltipText = "转为贴边收纳小把手" }
                                )
                            }
                        }
                    }

                    DockDisplayMode.PIE_RADIAL -> {
                        // 🥧 自适应极速 Pie 扇形轮盘 (Adaptive Ultra-Fast Pie Fan Menu)
                        Box(contentAlignment = Alignment.Center) {
                            // 极速展开动画 (120ms 瞬间响应)
                            val expandProgress by animateFloatAsState(
                                targetValue = if (isPieExpanded) 1f else 0f,
                                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
                            )

                            val radiusPx = with(density) { 82.dp.toPx() }

                            // 自适应扇形展开角度：根据是否靠左/靠右/靠顶/靠底动态调整扇区范围
                            val isNearRight = clampedX > screenWidthPx * 0.2f
                            val isNearLeft = clampedX < -screenWidthPx * 0.2f
                            val isNearTop = clampedY < -screenHeightPx * 0.6f

                            val pieItems = listOf(
                                Triple(Icons.Default.Casino, "换语料") { onRandomQuote() },
                                Triple(Icons.Default.Tune, "菜单") { showThemeMenu = true },
                                Triple(Icons.Default.Settings, "配置") { activeProvider?.let { onOpenProviderConfig(it.id) } },
                                Triple(Icons.AutoMirrored.Filled.ViewSidebar, "竖排") {
                                    previousMode = dockMode
                                    dockMode = DockDisplayMode.SIDEBAR_VERTICAL
                                    onUpdateDockState(dockMode.name, posX, posY)
                                },
                                Triple(Icons.Default.CloseFullscreen, "收纳") {
                                    previousMode = dockMode
                                    dockMode = DockDisplayMode.EDGE_STASHED
                                    posX = if (isAtLeft) minX else maxX
                                    onUpdateDockState(dockMode.name, posX, posY)
                                }
                            )

                            // 动态计算每个按键在当前屏幕边缘条件下的安全极坐标角度
                            val angles = remember(isNearRight, isNearLeft, isNearTop) {
                                when {
                                    isNearRight -> listOf(120.0, 150.0, 180.0, 210.0, 240.0) // 靠右：全向左侧展开
                                    isNearLeft -> listOf(-60.0, -30.0, 0.0, 30.0, 60.0)       // 靠左：全向右侧展开
                                    isNearTop -> listOf(30.0, 60.0, 90.0, 120.0, 150.0)      // 靠顶：全向下侧展开
                                    else -> listOf(-140.0, -100.0, -60.0, -20.0, 20.0)       // 默认靠底：全向上方弧形展开
                                }
                            }

                            pieItems.forEachIndexed { index, (icon, label, action) ->
                                val angleRad = Math.toRadians(angles.getOrElse(index) { index * 72.0 - 90.0 })
                                val targetX = (radiusPx * cos(angleRad) * expandProgress).toFloat()
                                val targetY = (radiusPx * sin(angleRad) * expandProgress).toFloat()

                                Surface(
                                    modifier = Modifier
                                        .offset { IntOffset(targetX.roundToInt(), targetY.roundToInt()) }
                                        .scale(expandProgress)
                                        .size(42.dp)
                                        .shadow(6.dp, CircleShape)
                                        .combinedClickable(
                                            onClick = {
                                                action()
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onLongClick = {
                                                activeTooltipText = "扇区功能: $label"
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        ),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.6f))
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
                                    .shadow(10.dp, CircleShape)
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
                                            activeTooltipText = "轻触立刻展开/收起扇形菜单，双击播放/停止"
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ),
                                shape = CircleShape,
                                color = if (isPlaying) activeBrandColor else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(2.dp, activeBrandColor)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSynthesizing) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = activeBrandColor, strokeWidth = 2.5.dp)
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

                            // 二级菜单挂载
                            DockThemeDropdown(
                                expanded = showThemeMenu,
                                currentUiStyle = currentUiStyle,
                                currentDockMode = dockMode,
                                onDismiss = { showThemeMenu = false },
                                onSwitchUiStyle = onSwitchUiStyle,
                                onSwitchDockMode = { newMode ->
                                    previousMode = dockMode
                                    dockMode = newMode
                                    if (newMode == DockDisplayMode.EDGE_STASHED) {
                                        posX = if (isAtLeft) minX else maxX
                                    }
                                    onUpdateDockState(newMode.name, posX, posY)
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
                                            posY += dragAmount.y
                                        },
                                        onDragEnd = {
                                            onUpdateDockState(dockMode.name, posX, posY)
                                        }
                                    )
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
                                                activeTooltipText = "直达【${activeProvider?.name ?: "当前模型"}】参数配置"
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
                                        onLongClick = { activeTooltipText = "随机语料名言" }
                                    )

                                    // 主题与形态二级菜单 (防误触)
                                    Box {
                                        DockIconButton(
                                            icon = Icons.Default.Tune,
                                            tooltip = "主题与形态设置",
                                            tint = MaterialTheme.colorScheme.primary,
                                            onClick = { showThemeMenu = true },
                                            onLongClick = { activeTooltipText = "打开主题与形态菜单" }
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
                                                if (newMode == DockDisplayMode.EDGE_STASHED) {
                                                    posX = minX
                                                }
                                                onUpdateDockState(newMode.name, posX, posY)
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
            text = { Text("↕️ 自由移动竖排侧边栏" + if (currentDockMode == DockDisplayMode.SIDEBAR_VERTICAL) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.SIDEBAR_VERTICAL) }
        )
        DropdownMenuItem(
            text = { Text("🥧 Pie 极速自适应扇形轮盘" + if (currentDockMode == DockDisplayMode.PIE_RADIAL) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.PIE_RADIAL) }
        )
        DropdownMenuItem(
            text = { Text("🗄️ 贴边极简收纳把手" + if (currentDockMode == DockDisplayMode.EDGE_STASHED) " (当前)" else "") },
            onClick = { onSwitchDockMode(DockDisplayMode.EDGE_STASHED) }
        )
    }
}
