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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.aitts.engine.ui.pulse.theme.PulseTokens

/**
 * ⚡ 右下角大拇指单手快捷收纳岛 (One-Handed Thumb Action Hub)
 * 1. 默认形态：常驻低干扰呼吸光圈；
 * 2. 展开形态：向上纵列弹出 [更换句子 🎲]、[切换模型 ⇆]、[模型参数 ⚙️]、[试听 ▶]；
 * 3. 独立大拇指黄金三角区，极大提升单手掌控体验。
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
    var isExpanded by remember { mutableStateOf(false) }

    val hubAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.85f,
        animationSpec = tween(220),
        label = "hubAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // 展开后的动作卡片组
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.7f, stiffness = 400f)),
            exit = fadeOut(tween(140)) + scaleOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = 56.dp, end = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // 1. 更换试听句子
                ThumbActionButton(
                    icon = Icons.Default.Casino,
                    label = "更换句子",
                    color = PulseTokens.SonicBlue,
                    onClick = {
                        onChangeText()
                        isExpanded = false
                    }
                )

                // 2. 切换当前模型
                ThumbActionButton(
                    icon = Icons.Default.SwapHoriz,
                    label = if (activeModelName.isNotBlank()) "切换模型 ($activeModelName)" else "切换模型",
                    color = PulseTokens.CyanElectric,
                    onClick = {
                        onOpenModelSelector()
                        isExpanded = false
                    }
                )

                // 3. 模型设置
                ThumbActionButton(
                    icon = Icons.Default.Settings,
                    label = "模型参数",
                    color = PulseTokens.AmberWarm,
                    onClick = {
                        onOpenModelConfig()
                        isExpanded = false
                    }
                )

                // 4. 播放 / 停止
                ThumbActionButton(
                    icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    label = if (isPlaying) "停止" else if (isSynthesizing) "合成中..." else "试听",
                    color = if (isPlaying) PulseTokens.MagentaLaser else PulseTokens.CyanElectric,
                    isLoading = isSynthesizing,
                    onClick = {
                        onTogglePlay()
                        isExpanded = false
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 常驻收纳悬浮触发按钮
        Surface(
            modifier = Modifier
                .alpha(hubAlpha)
                .size(if (isExpanded) 48.dp else 44.dp)
                .clip(CircleShape)
                .clickable { isExpanded = !isExpanded },
            shape = CircleShape,
            color = if (isExpanded) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceDark.copy(alpha = 0.9f),
            border = BorderStroke(
                1.dp,
                if (isExpanded) PulseTokens.CyanElectric.copy(alpha = 0.8f) else PulseTokens.CyanElectric.copy(alpha = 0.4f)
            ),
            shadowElevation = if (isExpanded) 8.dp else 3.dp
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                if (isExpanded) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "收起",
                        tint = PulseTokens.CyanElectric,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "单手快捷区",
                        tint = if (isPlaying) PulseTokens.CyanElectric else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
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
        color = PulseTokens.SurfaceDark.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
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
                color = PulseTokens.TextPrimary
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
