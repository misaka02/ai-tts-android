package com.aitts.engine.ui.pulse.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ⚡ 声学流体灵动核心球 (Pulse Acoustic Core)
 * 深度还原极简声学流体美学：
 * 1. 外层抗锯齿同心渐变圆环 (Concentric Gradient Halo)；
 * 2. 64 根精细声学频谱刻度线 (Radial Spectrum Ticks)，完全由 32 频段 FFT 频谱能量直接驱动跳跃；
 * 3. 内层漫射星云量子呼吸光球 (Inner Diffuse Nebula Core)，发音时辉光完全随音频真实 RMS 音量脉动；
 * 4. 彻底消除多边形直线锯齿与自转干扰，呈现极致纯净温润的声学质感。
 */
@Composable
fun PulseAcousticCore(
    modifier: Modifier = Modifier,
    spectrumBands: FloatArray = FloatArray(0),
    rmsEnergy: Float = 0f,
    isPlaying: Boolean = false,
    isSynthesizing: Boolean = false,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_core_anim")

    // 静谧环境呼吸 (仅在空闲时微幅生效，发音时让位给真实声学物理)
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleBreathing"
    )

    val synthPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "synthPulse"
    )

    // 平滑 RMS 能量插值 (低通阻尼滤波，消除抽搐与生硬抖动)
    val smoothedRms = remember { Animatable(0f) }
    LaunchedEffect(rmsEnergy, isPlaying) {
        if (isPlaying) {
            smoothedRms.animateTo(
                targetValue = rmsEnergy.coerceIn(0f, 1f),
                animationSpec = tween(60, easing = LinearEasing)
            )
        } else {
            smoothedRms.animateTo(0f, animationSpec = tween(180))
        }
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2) * 0.62f

            val effectiveScale = when {
                isPlaying -> 1f + (smoothedRms.value * 0.20f)
                isSynthesizing -> synthPulse
                else -> idleBreathing
            }

            // 1. 最外层环境漫射光晕 (Ambient Aurora Glow)
            val outerGlowAlpha = when {
                isPlaying -> 0.25f + (smoothedRms.value * 0.25f)
                isSynthesizing -> 0.22f
                else -> 0.12f
            }
            val outerGlowColor = when {
                isPlaying -> PulseTokens.CyanElectric
                isSynthesizing -> PulseTokens.AmberWarm
                else -> PulseTokens.SonicBlue
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        outerGlowColor.copy(alpha = outerGlowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f * effectiveScale
                ),
                radius = baseRadius * 1.55f * effectiveScale,
                center = center
            )

            // 2. 64 根精细声学频谱放射刻度线 (Radial Acoustic Spectrum Ticks)
            val tickCount = 64
            val angleStep = (2 * PI / tickCount).toFloat()
            val ringRadius = baseRadius * 1.05f * effectiveScale
            val defaultTickLen = 2.5.dp.toPx()
            val maxDynamicTickLen = 16.dp.toPx()

            for (i in 0 until tickCount) {
                val angle = (i * angleStep) - (PI / 2).toFloat() // 从 12 点钟方向起算
                
                // 将 32 频段频谱对称映射到左右两侧 (0~31 对应左半圆，31~0 对应右半圆)
                val bandIdx = if (i < 32) i else (63 - i)
                val bandMagnitude = if (isPlaying && spectrumBands.isNotEmpty()) {
                    spectrumBands.getOrElse(bandIdx) { 0.05f }.coerceIn(0.02f, 1f)
                } else if (isSynthesizing) {
                    0.20f
                } else {
                    0.03f
                }

                val tickLen = if (isPlaying) {
                    defaultTickLen + (bandMagnitude * maxDynamicTickLen)
                } else if (isSynthesizing) {
                    defaultTickLen + (bandMagnitude * 4.dp.toPx())
                } else {
                    defaultTickLen
                }

                val startX = center.x + (ringRadius * cos(angle))
                val startY = center.y + (ringRadius * sin(angle))
                val endX = center.x + ((ringRadius + tickLen) * cos(angle))
                val endY = center.y + ((ringRadius + tickLen) * sin(angle))

                val tickAlpha = when {
                    isPlaying -> 0.35f + (bandMagnitude * 0.65f)
                    isSynthesizing -> 0.40f
                    else -> 0.20f
                }
                val tickColor = when {
                    isPlaying -> if (bandIdx > 16) PulseTokens.MagentaLaser else PulseTokens.CyanElectric
                    isSynthesizing -> PulseTokens.AmberWarm
                    else -> PulseTokens.SonicBlue
                }

                drawLine(
                    color = tickColor.copy(alpha = tickAlpha.coerceIn(0f, 1f)),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. 中层抗锯齿同心圆能量环 (Concentric Gradient Ring)
            val ringBrush = Brush.sweepGradient(
                colors = listOf(
                    PulseTokens.CyanElectric.copy(alpha = 0.85f),
                    PulseTokens.MagentaLaser.copy(alpha = 0.75f),
                    PulseTokens.SonicBlue.copy(alpha = 0.80f),
                    PulseTokens.CyanElectric.copy(alpha = 0.85f)
                ),
                center = center
            )
            drawCircle(
                brush = ringBrush,
                radius = ringRadius,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )

            // 4. 内层漫射星云量子光球 (Inner Diffuse Nebula Core)
            val coreAlpha = when {
                isPlaying -> 0.40f + (smoothedRms.value * 0.45f)
                isSynthesizing -> 0.45f
                else -> 0.30f
            }
            val coreRadius = ringRadius * 0.88f * (if (isPlaying) 1f + smoothedRms.value * 0.15f else 1f)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isPlaying) PulseTokens.CyanElectric else PulseTokens.SonicBlue).copy(alpha = coreAlpha),
                        PulseTokens.SurfaceDark.copy(alpha = 0.70f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // 5. 核心柔和光点 (Center Highlight Focus)
            val centerBeadAlpha = if (isPlaying) 0.85f + (smoothedRms.value * 0.15f) else 0.60f
            drawCircle(
                color = if (isPlaying) Color.White.copy(alpha = centerBeadAlpha) else PulseTokens.CyanElectric.copy(alpha = centerBeadAlpha),
                radius = (4.dp.toPx() + (smoothedRms.value * 3.dp.toPx())).coerceAtLeast(3.dp.toPx()),
                center = center
            )
        }
    }
}
