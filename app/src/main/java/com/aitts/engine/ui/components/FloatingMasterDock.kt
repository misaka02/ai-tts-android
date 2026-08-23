package com.aitts.engine.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.ui.theme.BrandTheme
import kotlin.math.roundToInt

/**
 * 悬浮坞形态模式
 */
enum class DockDisplayMode {
    EXPANDED_HORIZONTAL, // 底部横向交互胶囊
    SIDEBAR_VERTICAL,     // 贴边竖向侧边栏
    MINI_BUBBLE          // 贴边极简收起微气泡
}

/**
 * 🌟 全主题通用人机工学自由拖拽主控悬浮坞 (Universal Draggable Floating Master Dock)
 * 1. 任意手势物理拖拽至屏幕任意舒适位置；
 * 2. 支持 3 大形态随时切换（横向全功能栏 / 靠边竖排侧边栏 / 极简挂起微气泡）；
 * 3. 内置 3 大主题一键快速轮转切换（BENTO 🚀 / STUDIO 🎛️ / VINYL 📻）；
 * 4. 实时显示当前生效模型、一键试听/停止、随机语料与双击直达配置。
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
    val activeBrandColor = remember(activeProvider?.type) {
        activeProvider?.let { BrandTheme.getColorForType(it.type) }
    } ?: MaterialTheme.colorScheme.primary

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dockMode by remember { mutableStateOf(DockDisplayMode.EXPANDED_HORIZONTAL) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .zIndex(100f)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        when (dockMode) {
            DockDisplayMode.MINI_BUBBLE -> {
                // 🌟 极简贴边收起微气泡 (Mini Floating Bubble)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.5.dp, activeBrandColor),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .combinedClickable(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dockMode = DockDisplayMode.EXPANDED_HORIZONTAL
                            },
                            onDoubleClick = {
                                onPlayToggle()
                            }
                        )
                ) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (isSynthesizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                                color = activeBrandColor
                            )
                        } else if (isPlaying) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "停止",
                                tint = activeBrandColor,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "展开",
                                tint = activeBrandColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            DockDisplayMode.SIDEBAR_VERTICAL -> {
                // 🌟 靠边竖排侧边栏 (Vertical Sidebar Dock)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.5f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.width(54.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 拖拽手柄
                        Icon(
                            imageVector = Icons.Default.DragIndicator,
                            contentDescription = "按住拖拽移动",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )

                        // 播放/停止大按键
                        Surface(
                            shape = CircleShape,
                            color = activeBrandColor,
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlayToggle()
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                } else if (isPlaying) {
                                    Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // 随机语料
                        IconButton(
                            onClick = onRandomQuote,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "换语料", modifier = Modifier.size(18.dp))
                        }

                        // 快速风格轮换
                        IconButton(
                            onClick = {
                                val nextStyle = when (currentUiStyle) {
                                    "BENTO" -> "STUDIO"
                                    "STUDIO" -> "VINYL"
                                    else -> "BENTO"
                                }
                                onSwitchUiStyle(nextStyle)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            when (currentUiStyle) {
                                "STUDIO" -> Icon(Icons.Default.Tune, contentDescription = "调音台", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                                "VINYL" -> Icon(Icons.Default.Radio, contentDescription = "黑胶唱机", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                                else -> Icon(Icons.Default.RocketLaunch, contentDescription = "全息声球", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                            }
                        }

                        // 展开/收起切换
                        IconButton(
                            onClick = { dockMode = DockDisplayMode.MINI_BUBBLE },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.CloseFullscreen, contentDescription = "收起为小气泡", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            DockDisplayMode.EXPANDED_HORIZONTAL -> {
                // 🌟 横向全功能悬浮胶囊 (Expanded Horizontal Master Dock)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, activeBrandColor.copy(alpha = 0.45f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 拖拽手柄 + 引擎信息（点击试听，双击进设置）
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .combinedClickable(
                                    onClick = onPlayToggle,
                                    onDoubleClick = { activeProvider?.let { onOpenProviderConfig(it.id) } }
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "按住拖拽",
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(activeBrandColor.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = activeProvider?.name ?: "未激活引擎",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "双击进设置 · 可自由拖拽位置",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 功能按键区
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            // 换语料
                            IconButton(
                                onClick = onRandomQuote,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = "换语料", modifier = Modifier.size(16.dp))
                            }

                            // 切换主题模式
                            IconButton(
                                onClick = {
                                    val nextStyle = when (currentUiStyle) {
                                        "BENTO" -> "STUDIO"
                                        "STUDIO" -> "VINYL"
                                        else -> "BENTO"
                                    }
                                    onSwitchUiStyle(nextStyle)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                when (currentUiStyle) {
                                    "STUDIO" -> Icon(Icons.Default.Tune, contentDescription = "调音台", tint = activeBrandColor, modifier = Modifier.size(16.dp))
                                    "VINYL" -> Icon(Icons.Default.Radio, contentDescription = "黑胶唱机", tint = activeBrandColor, modifier = Modifier.size(16.dp))
                                    else -> Icon(Icons.Default.RocketLaunch, contentDescription = "全息声球", tint = activeBrandColor, modifier = Modifier.size(16.dp))
                                }
                            }

                            // 切换形态 (转为侧边栏 / 收起)
                            IconButton(
                                onClick = { dockMode = DockDisplayMode.SIDEBAR_VERTICAL },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ViewSidebar, contentDescription = "转为侧边栏", modifier = Modifier.size(16.dp))
                            }

                            // 播放 / 停止
                            Button(
                                onClick = onPlayToggle,
                                colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color.White)
                                } else if (isPlaying) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("停止", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("试听", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
