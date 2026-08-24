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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ⚡ 声学流体灵动核心球 (Pulse Acoustic Core)
 * 采用柔和极光流体光晕、双环呼吸波纹与平滑有机正弦曲线，告别任何生硬尖锐线条。
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
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val synthesizingPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "synthPulse"
    )

    val energyAnim = remember { Animatable(0f) }
    LaunchedEffect(rmsEnergy) {
        energyAnim.animateTo(
            targetValue = rmsEnergy.coerceIn(0f, 1f),
            animationSpec = tween(90)
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
            val baseRadius = (size.minDimension / 2) * 0.65f

            val effectiveScale = when {
                isPlaying -> 1f + (energyAnim.value * 0.22f)
                isSynthesizing -> synthesizingPulse
                else -> breathingScale
            }

            // 1. 最外层极光柔雾光晕 (Ambient Aurora Glow)
            val outerGlowColor = when {
                isPlaying -> PulseTokens.CyanElectric.copy(alpha = 0.35f)
                isSynthesizing -> PulseTokens.AmberWarm.copy(alpha = 0.30f)
                else -> PulseTokens.SonicBlue.copy(alpha = 0.18f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(outerGlowColor, Color.Transparent),
                    center = center,
                    radius = baseRadius * 1.5f * effectiveScale
                ),
                radius = baseRadius * 1.5f * effectiveScale,
                center = center
            )

            // 2. 外层极光流体涟漪波浪 (Smooth Liquid Wave Outer Halo)
            val wavePoints = 48
            val waveAngleStep = (2 * PI / wavePoints).toFloat()
            val wavePath = Path()
            val waveRadius = baseRadius * 1.18f * effectiveScale

            for (i in 0 until wavePoints) {
                val angle = (i * waveAngleStep) + Math.toRadians(idleRotation.toDouble()).toFloat()
                val bandVal = spectrumBands.getOrNull(i % spectrumBands.size.coerceAtLeast(1)) ?: 0.05f
                val dynamicMod = if (isPlaying) {
                    sin(angle * 4 + wavePhase) * (8.dp.toPx() + bandVal * 12.dp.toPx())
                } else if (isSynthesizing) {
                    sin(angle * 3 + wavePhase) * 6.dp.toPx()
                } else {
                    sin(angle * 3 + wavePhase) * 3.dp.toPx()
                }
                val r = waveRadius + dynamicMod
                val x = center.x + (r * cos(angle))
                val y = center.y + (r * sin(angle))
                if (i == 0) {
                    wavePath.moveTo(x, y)
                } else {
                    wavePath.lineTo(x, y)
                }
            }
            wavePath.close()

            val waveColor = when {
                isPlaying -> PulseTokens.CyanElectric.copy(alpha = 0.7f)
                isSynthesizing -> PulseTokens.AmberWarm.copy(alpha = 0.6f)
                else -> PulseTokens.SonicBlue.copy(alpha = 0.4f)
            }
            drawPath(
                path = wavePath,
                color = waveColor,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. 中层平滑能量流环 (Concentric Energy Ring)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        PulseTokens.CyanElectric.copy(alpha = 0.8f),
                        PulseTokens.MagentaLaser.copy(alpha = 0.6f),
                        PulseTokens.SonicBlue.copy(alpha = 0.7f),
                        PulseTokens.CyanElectric.copy(alpha = 0.8f)
                    ),
                    center = center
                ),
                radius = baseRadius * 0.96f * effectiveScale,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. 内层全息量子实体光球 (Inner Quantum Luminous Core)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when {
                            isPlaying -> PulseTokens.CyanElectric.copy(alpha = 0.75f)
                            isSynthesizing -> PulseTokens.AmberWarm.copy(alpha = 0.70f)
                            else -> PulseTokens.SonicBlue.copy(alpha = 0.55f)
                        },
                        PulseTokens.SurfaceDark.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.82f * effectiveScale
                ),
                radius = baseRadius * 0.82f * effectiveScale,
                center = center
            )

            // 5. 核心柔和呼吸亮点 (Center Micro Core Highlight)
            drawCircle(
                color = if (isPlaying) Color.White.copy(alpha = 0.9f) else PulseTokens.CyanElectric.copy(alpha = 0.7f),
                radius = 5.dp.toPx() * (if (isPlaying) 1f + energyAnim.value * 0.5f else breathingScale),
                center = center
            )
        }
    }
}
