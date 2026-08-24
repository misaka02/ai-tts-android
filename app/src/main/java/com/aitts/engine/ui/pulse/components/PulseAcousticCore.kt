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
import kotlin.math.cos
import kotlin.math.sin

/**
 * ⚡ 声学流体灵动核心球 (Pulse Acoustic Core)
 * 实时响应音频 FFT 频段与 RMS 能量，模拟高科技全息量子声球。
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

    val idleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val synthesizingPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "synthPulse"
    )

    val energyAnim = remember { Animatable(0f) }
    LaunchedEffect(rmsEnergy) {
        energyAnim.animateTo(
            targetValue = rmsEnergy.coerceIn(0f, 1f),
            animationSpec = tween(80)
        )
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
            val baseRadius = (size.minDimension / 2) * 0.72f

            val effectiveScale = when {
                isPlaying -> 1f + (energyAnim.value * 0.25f)
                isSynthesizing -> synthesizingPulse
                else -> breathingScale
            }

            // 1. 核心底光晕
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when {
                            isPlaying -> PulseTokens.CyanElectric.copy(alpha = 0.45f)
                            isSynthesizing -> PulseTokens.AmberWarm.copy(alpha = 0.4f)
                            else -> PulseTokens.SonicBlue.copy(alpha = 0.25f)
                        },
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.3f * effectiveScale
                ),
                radius = baseRadius * 1.3f * effectiveScale,
                center = center
            )

            // 2. 环绕能量轨道与粒子频谱
            val bandsCount = 28
            val angleStep = (2 * Math.PI / bandsCount).toFloat()
            val baseAngle = Math.toRadians(idleRotation.toDouble()).toFloat()

            for (i in 0 until bandsCount) {
                val angle = baseAngle + (i * angleStep)
                val bandValue = spectrumBands.getOrNull(i % spectrumBands.size.coerceAtLeast(1)) ?: 0.1f
                val length = if (isPlaying) {
                    baseRadius + (bandValue * 30.dp.toPx())
                } else if (isSynthesizing) {
                    baseRadius + (sin(angle * 3 + idleRotation * 0.05f).toFloat() * 12.dp.toPx())
                } else {
                    baseRadius + 4.dp.toPx()
                }

                val startX = center.x + (baseRadius * 0.85f * cos(angle))
                val startY = center.y + (baseRadius * 0.85f * sin(angle))
                val endX = center.x + (length * cos(angle))
                val endY = center.y + (length * sin(angle))

                val strokeColor = when {
                    isPlaying -> if (i % 2 == 0) PulseTokens.CyanElectric else PulseTokens.MagentaLaser
                    isSynthesizing -> PulseTokens.AmberWarm
                    else -> PulseTokens.CyanElectric.copy(alpha = 0.5f)
                }

                drawLine(
                    color = strokeColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 3. 量子内球光圈
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        PulseTokens.CyanElectric,
                        PulseTokens.MagentaLaser,
                        PulseTokens.SonicBlue,
                        PulseTokens.CyanElectric
                    ),
                    center = center
                ),
                radius = baseRadius * 0.85f * effectiveScale,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )
        }
    }
}
