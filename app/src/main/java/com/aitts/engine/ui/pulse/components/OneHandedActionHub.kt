package com.aitts.engine.ui.pulse.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import com.aitts.engine.ui.pulse.theme.PulseTokens

data class ActionHubItem(
    val label: String,
    val icon: ImageVector,
    val color: Color = PulseTokens.DefaultCyanElectric,
    val isLoading: Boolean = false,
    val autoDismiss: Boolean = true,
    val onClick: () -> Unit
)

/**
 * ⚡ 全界面通用大拇指单手悬浮收纳岛 (Universal One-Handed Action Hub)
 * 支持在任何主界面或子界面注入自定义高频快捷操作组
 */
@Composable
fun UniversalActionHub(
    modifier: Modifier = Modifier,
    items: List<ActionHubItem>,
    isHighlighted: Boolean = false,
    icon: ImageVector = Icons.Default.Tune
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 物理/手势返回键拦截：大拇指菜单展开时优先收起菜单
    BackHandler(enabled = isExpanded) {
        isExpanded = false
    }

    val hubAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.88f,
        animationSpec = tween(200),
        label = "hubAlpha"
    )

    Box(
        modifier = modifier.zIndex(100f),
        contentAlignment = Alignment.BottomEnd
    ) {
        // 展开的动作列表
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.75f, stiffness = 450f)),
            exit = fadeOut(tween(120)) + scaleOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 56.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                items.forEach { item ->
                    ThumbActionButton(
                        icon = item.icon,
                        label = item.label,
                        color = item.color,
                        isLoading = item.isLoading,
                        onClick = {
                            item.onClick()
                            if (item.autoDismiss) {
                                isExpanded = false
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 常驻悬浮触发按钮 (标准 48dp 大拇指盲操人体工学触控区)
        Surface(
            modifier = Modifier
                .alpha(hubAlpha)
                .size(48.dp)
                .clip(CircleShape)
                .clickable { isExpanded = !isExpanded },
            shape = CircleShape,
            color = if (isExpanded) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceDark.copy(alpha = 0.94f),
            border = if (isExpanded) {
                BorderStroke(1.dp, PulseTokens.CyanElectric)
            } else if (isHighlighted) {
                BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.8f))
            } else {
                PulseTokens.BorderSubtle
            },
            shadowElevation = if (isExpanded) 8.dp else 4.dp
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                if (isExpanded) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "收起",
                        tint = PulseTokens.CyanElectric,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = "快捷操作",
                        tint = if (isHighlighted) PulseTokens.CyanElectric else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * 主中枢专用单手收纳岛
 */
@Composable
fun OneHandedActionHub(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    isSynthesizing: Boolean = false,
    activeModelName: String = "",
    onTogglePlay: () -> Unit = {},
    onChangeText: () -> Unit = {},
    onOpenModelSelector: () -> Unit = {},
    onOpenModelConfig: () -> Unit = {}
) {
    val shortModelName = if (activeModelName.length > 8) activeModelName.take(7) + "…" else activeModelName
    val items = listOf(
        ActionHubItem(
            label = "更换句子",
            icon = Icons.Default.Casino,
            color = PulseTokens.SonicBlue,
            onClick = onChangeText
        ),
        ActionHubItem(
            label = if (shortModelName.isNotBlank()) "切换模型 ($shortModelName)" else "切换模型",
            icon = Icons.Default.SwapHoriz,
            color = PulseTokens.CyanElectric,
            onClick = onOpenModelSelector
        ),
        ActionHubItem(
            label = "模型参数",
            icon = Icons.Default.Settings,
            color = PulseTokens.AmberWarm,
            onClick = onOpenModelConfig
        ),
        ActionHubItem(
            label = if (isPlaying) "停止" else if (isSynthesizing) "合成中..." else "试听",
            icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            color = if (isPlaying) PulseTokens.MagentaLaser else PulseTokens.CyanElectric,
            isLoading = isSynthesizing,
            onClick = onTogglePlay
        )
    )

    UniversalActionHub(
        modifier = modifier,
        items = items,
        isHighlighted = isPlaying || isSynthesizing
    )
}

@Composable
private fun ThumbActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PulseTokens.SurfaceCardActive.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        shadowElevation = 8.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PulseTokens.TextPrimary,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = color,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
