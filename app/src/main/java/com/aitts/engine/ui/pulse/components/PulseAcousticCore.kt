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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BAND_COUNT = 32

/**
 * ⚡ 声学灵动核心球 (Pulse Acoustic Core)
 * 支持 4 大独创高保真声学物理风格（双击核心球极速切换）：
 * 0. 极光光谱能量柱 (Aurora Spectrum Bars) - 经典圆角能量柱/空闲微珠
 * 1. 物理跃动流体点阵 (Bouncing Acoustic Dots) - 双层柔光星尘，彻底消除生硬细线
 * 2. 量子共振轨道与光子风暴 (Quantum Resonant Rings) - 环径与粗细随低中高频声能剧烈膨胀共振
 * 3. 流体极光声学曲面 (Fluid Aurora Mesh Ribbon) - 4 阶正弦极光流体，波幅随人声频谱调制
 */
@Composable
fun PulseAcousticCore(
    modifier: Modifier = Modifier,
    spectrumBands: FloatArray = FloatArray(0),
    rmsEnergy: Float = 0f,
    isPlaying: Boolean = false,
    isSynthesizing: Boolean = false,
    coreColor: Color = PulseTokens.CyanElectric,
    coreStyle: Int = 0,
    onClick: () -> Unit = {},
    onStyleChange: (Int) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_core_anim")

    // 静谧环境呼吸
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
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "synthPulse"
    )

    // 物理声学非匀速旋转驱动系统 (开普勒天体非匀速公转 + 语音瞬态爆发冲量)
    var dynamicAngle1 by remember { mutableStateOf(0f) }
    var dynamicAngle2 by remember { mutableStateOf(0f) }
    var dynamicAngle3 by remember { mutableStateOf(0f) }

    // 平滑 RMS 能量插值 (低通阻尼滤波)
    val smoothedRms = remember { Animatable(0f) }
    LaunchedEffect(rmsEnergy, isPlaying) {
        if (isPlaying) {
            smoothedRms.animateTo(
                targetValue = rmsEnergy.coerceIn(0f, 1f),
                animationSpec = tween(40, easing = LinearEasing)
            )
        } else {
            smoothedRms.animateTo(0f, animationSpec = tween(150))
        }
    }

    val bassEnergy = if (isPlaying && spectrumBands.isNotEmpty()) spectrumBands.take(8).average().toFloat().coerceIn(0f, 1f) else 0.04f
    val speechEnergy = if (isPlaying) (smoothedRms.value * 3.8f + bassEnergy * 2.4f).coerceIn(0f, 1f) else 0f

    LaunchedEffect(isPlaying, isSynthesizing) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { nowNanos ->
                val dtSec = ((nowNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = nowNanos

                val rms = smoothedRms.value
                val curSpeechEnergy = if (isPlaying) (rms * 3.8f + bassEnergy * 2.4f).coerceIn(0f, 1f) else 0f
                // 声能爆发冲量：静默时 1.0x，发音朗读时随人声能量爆发冲刺至 7.5 倍
                val baseSpeedMultiplier = if (isPlaying) (1.0f + curSpeechEnergy * 6.5f) else if (isSynthesizing) 2.2f else 1.0f

                // 开普勒角速度调制因子：在近日点 (cos=1) 速度加速到 (1+e)^2，在远日点 (cos=-1) 速度降为 (1-e)^2
                val rad1 = dynamicAngle1 * (PI / 180f).toFloat()
                val kepler1 = (1f + 0.52f * cos(rad1)).let { it * it }

                val rad2 = (dynamicAngle2 + 90f) * (PI / 180f).toFloat()
                val kepler2 = (1f + 0.56f * cos(rad2)).let { it * it }

                val rad3 = (dynamicAngle3 + 180f) * (PI / 180f).toFloat()
                val kepler3 = (1f + 0.48f * cos(rad3)).let { it * it }

                dynamicAngle1 = (dynamicAngle1 + 48f * kepler1 * baseSpeedMultiplier * dtSec) % 360f
                dynamicAngle2 = (dynamicAngle2 - 36f * kepler2 * baseSpeedMultiplier * dtSec + 360f) % 360f
                dynamicAngle3 = (dynamicAngle3 + 26f * kepler3 * baseSpeedMultiplier * dtSec) % 360f
            }
        }
    }

    val cyanElectric = coreColor
    val sonicBlue = PulseTokens.SonicBlue
    val magentaLaser = PulseTokens.MagentaLaser
    val amberWarm = PulseTokens.AmberWarm

    Box(
        modifier = modifier
            .size(240.dp)
            .clip(CircleShape)
            .pointerInput(coreStyle) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = {
                        val nextStyle = (coreStyle + 1) % 3
                        onStyleChange(nextStyle)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = (size.minDimension / 2) * 0.62f

            val effectiveScale = when {
                isPlaying -> 1f + (smoothedRms.value * 0.22f)
                isSynthesizing -> synthPulse
                else -> idleBreathing
            }

            when (coreStyle % 3) {
                // ==========================================
                // 风格 0: 经典极光光晕 (Classic Aurora Halo - 像素级精准还原实机截图)
                // ==========================================
                0 -> {
                    // 1. 中央纯净柔和漫射星云光晕 (Center Soft Diffuse Nebula Halo)
                    val coreRadius = baseRadius * 0.65f * effectiveScale
                    val nebulaAlpha = if (isPlaying) 0.40f + (smoothedRms.value * 0.30f) else 0.24f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isPlaying) cyanElectric else sonicBlue).copy(alpha = nebulaAlpha),
                                sonicBlue.copy(alpha = nebulaAlpha * 0.40f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = coreRadius
                        ),
                        radius = coreRadius,
                        center = center
                    )

                    // 2. 内层 1.5dp 极细全息同心渐变圆环 (Thin 1.5dp Sweep Gradient Ring)
                    val ringRadius = baseRadius * 0.84f * effectiveScale
                    val ringColors = listOf(
                        Color(0xFFFF4081), // 顶部洋红粉紫
                        Color(0xFF00E5FF), // 右侧电光青
                        Color(0xFF00B0FF), // 底部声波蓝
                        Color(0xFF7C4DFF), // 左侧罗兰紫
                        Color(0xFFFF4081)  // 闭合
                    )
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = ringColors,
                            center = center
                        ),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 3. 环外透光通透暗隙与刻度起点
                    val tickBaseRadius = ringRadius + 8.5.dp.toPx()

                    // 4. 表盘外部扩散型大范围薄雾状柔光光晕 (Outer Hazy Mist Halo - 笼罩表盘外侧)
                    val mistOuterRadius = tickBaseRadius + 24.dp.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.55f to Color(0xFF2D5260).copy(alpha = if (isPlaying) 0.22f + smoothedRms.value * 0.15f else 0.13f),
                                0.82f to Color(0xFF1E3A47).copy(alpha = if (isPlaying) 0.12f else 0.06f),
                                1.0f to Color.Transparent
                            ),
                            center = center,
                            radius = mistOuterRadius
                        ),
                        radius = mistOuterRadius,
                        center = center
                    )

                    // 5. 环外 72 段周期性表盘刻度线 (单色朦胧微光、6 格周期粗细/亮度微渐变、强劲声波律动)
                    val dialTicks = 72
                    val dialAngleStep = (2 * PI / dialTicks).toFloat()

                    for (t in 0 until dialTicks) {
                        val angle = (t * dialAngleStep) - (PI / 2).toFloat()
                        val bandIdx = (t % BAND_COUNT)
                        val bandMag = if (isPlaying && spectrumBands.isNotEmpty()) {
                            spectrumBands.getOrElse(bandIdx) { 0.05f }.coerceIn(0.02f, 1f)
                        } else if (isSynthesizing) 0.22f else 0.02f

                        // 6 格周期内精密阶梯微渐变 (增强表盘微观层次感与精密度)
                        val subIndex = t % 6
                        val (baseLen, baseAlpha, baseWidth) = when (subIndex) {
                            0 -> Triple(3.6.dp.toPx(), 0.28f, 1.25.dp.toPx()) // 0: 主刻度
                            3 -> Triple(2.6.dp.toPx(), 0.20f, 0.95.dp.toPx()) // 3: 中节点刻度
                            2, 4 -> Triple(2.0.dp.toPx(), 0.16f, 0.80.dp.toPx()) // 2,4: 次刻度
                            else -> Triple(1.5.dp.toPx(), 0.12f, 0.65.dp.toPx()) // 1,5: 极细暗刻度
                        }

                        // 强劲声波律动 (延伸量提升至 20dp，伴随高光脉冲)
                        val dynamicLen = if (isPlaying) bandMag * 20.dp.toPx() else 0f
                        val tickLen = baseLen + dynamicLen

                        val tickAlpha = if (isPlaying) {
                            (0.35f + bandMag * 0.62f).coerceIn(0.25f, 0.98f)
                        } else {
                            baseAlpha
                        }

                        val tickWidth = if (isPlaying) {
                            baseWidth * (1.35f + bandMag * 1.65f)
                        } else {
                            baseWidth
                        }

                        val baseColor = if (isPlaying) {
                            androidx.compose.ui.graphics.lerp(Color(0xFF4EE2EC), Color(0xFF7DF9FF), bandMag)
                        } else {
                            Color(0xFF3A6B7C)
                        }

                        val tickColor = baseColor.copy(alpha = tickAlpha)

                        val startX = center.x + (tickBaseRadius * cos(angle))
                        val startY = center.y + (tickBaseRadius * sin(angle))
                        val endX = center.x + ((tickBaseRadius + tickLen) * cos(angle))
                        val endY = center.y + ((tickBaseRadius + tickLen) * sin(angle))

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = tickWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // ==========================================
                // 风格 1: 精密磁场点阵表盘 + 高清透光全息声学光镜 (Acoustic Matrix Dial)
                // ==========================================
                1 -> {
                    val trackRadius = (baseRadius * 0.88f) * effectiveScale

                    // 1. 底层 72 段精密声学同心磁场刻度表盘
                    val dialTicks = 72
                    val dialAngleStep = (2 * PI / dialTicks).toFloat()
                    for (t in 0 until dialTicks) {
                        val tAngle = t * dialAngleStep
                        val isMajor = (t % 6 == 0)
                        val tickLen = if (isMajor) 4.5.dp.toPx() else 2.2.dp.toPx()
                        val tickAlpha = if (isMajor) (if (isPlaying) 0.45f else 0.25f) else (if (isPlaying) 0.20f else 0.10f)
                        val tickWidth = if (isMajor) 1.2.dp.toPx() else 0.8.dp.toPx()

                        val tStart = Offset(center.x + (trackRadius - tickLen) * cos(tAngle), center.y + (trackRadius - tickLen) * sin(tAngle))
                        val tEnd = Offset(center.x + trackRadius * cos(tAngle), center.y + trackRadius * sin(tAngle))

                        drawLine(
                            color = (if (isMajor) cyanElectric else sonicBlue).copy(alpha = tickAlpha),
                            start = tStart,
                            end = tEnd,
                            strokeWidth = tickWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // 2. 36 颗高精度悬停微珠与径向磁力束 (Magnetic Flux Tether)
                    val dotsCount = 36
                    val angleStep = (2 * PI / dotsCount).toFloat()

                    for (i in 0 until dotsCount) {
                        val angle = i * angleStep
                        val bandIdx = (i % BAND_COUNT)
                        val bandMag = if (isPlaying && spectrumBands.isNotEmpty()) {
                            spectrumBands.getOrElse(bandIdx) { 0.05f }.coerceIn(0.02f, 1f)
                        } else if (isSynthesizing) 0.20f else 0.03f

                        val radialHoverOffset = if (isPlaying) (bandMag * 16.dp.toPx()) else if (isSynthesizing) 3.dp.toPx() else 0.dp.toPx()
                        val dotRadius = trackRadius + 4.dp.toPx() + radialHoverOffset
                        val dotCenter = Offset(center.x + dotRadius * cos(angle), center.y + dotRadius * sin(angle))

                        // 径向微光磁力线 (Magnetic Flux Tether)
                        val tetherStart = Offset(center.x + (trackRadius - 2.dp.toPx()) * cos(angle), center.y + (trackRadius - 2.dp.toPx()) * sin(angle))
                        drawLine(
                            color = (if (i % 2 == 0) cyanElectric else magentaLaser).copy(alpha = if (isPlaying) 0.30f + bandMag * 0.40f else 0.12f),
                            start = tetherStart,
                            end = dotCenter,
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        val beadColor = if (isPlaying) (if (bandIdx > 9) magentaLaser else cyanElectric) else sonicBlue
                        val beadAlpha = if (isPlaying) 0.50f + (bandMag * 0.50f) else 0.35f
                        val beadRadius = if (isPlaying) 2.2.dp.toPx() + (bandMag * 1.6.dp.toPx()) else 1.8.dp.toPx()

                        drawCircle(color = beadColor.copy(alpha = beadAlpha * 0.35f), radius = beadRadius * 2.2f, center = dotCenter)
                        drawCircle(color = if (isPlaying && bandMag > 0.4f) Color.White else beadColor.copy(alpha = beadAlpha), radius = beadRadius, center = dotCenter)
                    }

                    // 3. 高清透光全息声学光镜 (Acoustic Glass Aperture & Radial Nebula)
                    val lensRadius = trackRadius * 0.52f * (if (isPlaying) 1f + smoothedRms.value * 0.16f else 1f)

                    // A. 底层纯净半透明声学呼吸星云
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                cyanElectric.copy(alpha = if (isPlaying) 0.35f + smoothedRms.value * 0.35f else 0.15f),
                                sonicBlue.copy(alpha = if (isPlaying) 0.15f else 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = lensRadius * 1.4f
                        ),
                        radius = lensRadius * 1.4f,
                        center = center
                    )

                    // B. 精密全息光圈外环与内同心微环
                    drawCircle(
                        color = cyanElectric.copy(alpha = if (isPlaying) 0.45f + smoothedRms.value * 0.30f else 0.25f),
                        radius = lensRadius,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawCircle(
                        color = sonicBlue.copy(alpha = if (isPlaying) 0.35f else 0.18f),
                        radius = lensRadius * 0.65f,
                        center = center,
                        style = Stroke(width = 0.9.dp.toPx())
                    )
                    drawCircle(
                        color = cyanElectric.copy(alpha = if (isPlaying) 0.60f + smoothedRms.value * 0.40f else 0.30f),
                        radius = lensRadius * 0.28f,
                        center = center
                    )

                    // C. 四向极简精密声学十字微刻度
                    val crosshairLen = 5.dp.toPx()
                    for (deg in listOf(0, 90, 180, 270)) {
                        val rad = deg * (PI / 180f).toFloat()
                        val pStart = Offset(center.x + (lensRadius - crosshairLen) * cos(rad), center.y + (lensRadius - crosshairLen) * sin(rad))
                        val pEnd = Offset(center.x + (lensRadius + crosshairLen * 0.5f) * cos(rad), center.y + (lensRadius + crosshairLen * 0.5f) * sin(rad))
                        drawLine(
                            color = cyanElectric.copy(alpha = if (isPlaying) 0.50f else 0.25f),
                            start = pStart,
                            end = pEnd,
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // ==========================================
                // 风格 2: 4重天体引力共振轨道 (Celestial 4-Orbit Keplerian System - 微点引力光丝 + 毫秒级声学瞬态冲量)
                // ==========================================
                2 -> {
                    // 瞬时音频能量（毫秒级同步）
                    val liveBass = if (isPlaying && spectrumBands.isNotEmpty()) spectrumBands.take(8).average().toFloat().coerceIn(0f, 1f) else 0.04f
                    val liveEnergy = if (isPlaying) (rmsEnergy * 0.65f + liveBass * 0.35f) else 0f

                    // 4 重基准轨道半径 (发音时随人声能量产生径向脉冲膨胀)
                    val baseR1 = (baseRadius * 0.56f + liveEnergy * 16.dp.toPx()) * effectiveScale
                    val baseR2 = (baseRadius * 0.86f + liveEnergy * 22.dp.toPx()) * effectiveScale
                    val baseR3 = (baseRadius * 1.16f + liveEnergy * 26.dp.toPx()) * effectiveScale
                    val baseR4 = (baseRadius * 1.42f + liveEnergy * 30.dp.toPx()) * effectiveScale

                    // 偏心开普勒轨道生成辅助函数
                    fun getKeplerRadius(baseR: Float, angleRad: Float, ecc: Float, periAngleRad: Float): Float {
                        return baseR * (1f - ecc * 0.28f * cos(angleRad - periAngleRad))
                    }

                    // 绘制 4 重微点引力光丝轨道 (采用 48 点精密同心引力丝)
                    fun drawFilamentOrbit(r: Float, col: Color, alphaBase: Float) {
                        val orbitTicks = 48
                        val tickStep = (2 * PI / orbitTicks).toFloat()
                        for (i in 0 until orbitTicks) {
                            val a = i * tickStep
                            val tLen = (2 * PI * r / orbitTicks * 0.45f).toFloat()
                            drawArc(
                                color = col.copy(alpha = if (isPlaying) (alphaBase + liveEnergy * 0.35f).coerceIn(0.1f, 0.85f) else alphaBase),
                                startAngle = (a * 180f / PI).toFloat(),
                                sweepAngle = (tLen / r * 180f / PI).toFloat(),
                                useCenter = false,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    drawFilamentOrbit(baseR1, cyanElectric, 0.25f)
                    drawFilamentOrbit(baseR2, sonicBlue, 0.20f)
                    drawFilamentOrbit(baseR3, magentaLaser, 0.16f)
                    drawFilamentOrbit(baseR4, amberWarm, 0.12f)

                    val tailSegments = if (isPlaying) 18 else 8
                    val maxTailAngle = if (isPlaying) (0.35f + liveEnergy * 0.85f) else 0.18f

                    // 轨道 1: 2 颗量子光子 (偏心率 0.52，近日点爆发公转)
                    val p1BaseRad = (dynamicAngle1 * (PI / 180f)).toFloat()
                    for (k in 0..1) {
                        val pAngle = p1BaseRad + (k * PI).toFloat()
                        val currentEccR = getKeplerRadius(baseR1, pAngle, 0.52f, 0f)
                        val keplerSpeedFactor = (1f + 0.52f * cos(pAngle)).let { it * it }

                        for (t in 1..tailSegments) {
                            val segmentRatio = t.toFloat() / tailSegments
                            val tAngle = pAngle - (segmentRatio * maxTailAngle * (0.6f + keplerSpeedFactor * 0.4f))
                            val tRadius = getKeplerRadius(baseR1, tAngle, 0.52f, 0f)
                            val tPos = Offset(center.x + tRadius * cos(tAngle), center.y + tRadius * sin(tAngle))
                            val tAlpha = (1f - segmentRatio) * (if (isPlaying) 0.65f + liveEnergy * 0.35f else 0.25f)
                            drawCircle(color = cyanElectric.copy(alpha = tAlpha.coerceIn(0f, 1f)), radius = 2.8.dp.toPx() * (1f - segmentRatio * 0.6f), center = tPos)
                        }
                        val pPos = Offset(center.x + currentEccR * cos(pAngle), center.y + currentEccR * sin(pAngle))
                        val pSize = if (isPlaying) (3.6.dp.toPx() + liveEnergy * 4.2.dp.toPx()) else 2.6.dp.toPx()
                        drawCircle(color = cyanElectric.copy(alpha = if (isPlaying) 0.60f + liveEnergy * 0.4f else 0.32f), radius = pSize * 2.5f, center = pPos)
                        drawCircle(color = cyanElectric, radius = pSize, center = pPos)
                        drawCircle(color = Color.White, radius = pSize * 0.60f, center = pPos)
                    }

                    // 轨道 2: 3 颗反向主卫星 (偏心率 0.56，逆行非匀速加速)
                    val p2BaseRad = (dynamicAngle2 * (PI / 180f)).toFloat()
                    for (k in 0..2) {
                        val pAngle = p2BaseRad + (k * 2 * PI / 3).toFloat()
                        val currentEccR = getKeplerRadius(baseR2, pAngle, 0.56f, (PI / 2).toFloat())
                        val keplerSpeedFactor = (1f + 0.56f * cos(pAngle + (PI / 2).toFloat())).let { it * it }

                        for (t in 1..tailSegments) {
                            val segmentRatio = t.toFloat() / tailSegments
                            val tAngle = pAngle + (segmentRatio * maxTailAngle * (0.6f + keplerSpeedFactor * 0.4f))
                            val tRadius = getKeplerRadius(baseR2, tAngle, 0.56f, (PI / 2).toFloat())
                            val tPos = Offset(center.x + tRadius * cos(tAngle), center.y + tRadius * sin(tAngle))
                            val tAlpha = (1f - segmentRatio) * (if (isPlaying) 0.55f + liveEnergy * 0.40f else 0.20f)
                            drawCircle(color = sonicBlue.copy(alpha = tAlpha.coerceIn(0f, 1f)), radius = 2.4.dp.toPx() * (1f - segmentRatio * 0.6f), center = tPos)
                        }
                        val pPos = Offset(center.x + currentEccR * cos(pAngle), center.y + currentEccR * sin(pAngle))
                        val pSize = if (isPlaying) (3.2.dp.toPx() + liveEnergy * 3.6.dp.toPx()) else 2.3.dp.toPx()
                        drawCircle(color = sonicBlue.copy(alpha = if (isPlaying) 0.55f + liveEnergy * 0.4f else 0.28f), radius = pSize * 2.2f, center = pPos)
                        drawCircle(color = sonicBlue, radius = pSize, center = pPos)
                        drawCircle(color = Color.White, radius = pSize * 0.55f, center = pPos)
                    }

                    // 轨道 3: 2 颗偏心彗星 (偏心率 0.48，长周期正向公转)
                    val p3BaseRad = (dynamicAngle3 * (PI / 180f)).toFloat()
                    for (k in 0..1) {
                        val pAngle = p3BaseRad + (k * PI).toFloat()
                        val currentEccR = getKeplerRadius(baseR3, pAngle, 0.48f, PI.toFloat())
                        val keplerSpeedFactor = (1f + 0.48f * cos(pAngle + PI.toFloat())).let { it * it }

                        for (t in 1..tailSegments) {
                            val segmentRatio = t.toFloat() / tailSegments
                            val tAngle = pAngle - (segmentRatio * maxTailAngle * 1.4f * (0.6f + keplerSpeedFactor * 0.4f))
                            val tRadius = getKeplerRadius(baseR3, tAngle, 0.48f, PI.toFloat())
                            val tPos = Offset(center.x + tRadius * cos(tAngle), center.y + tRadius * sin(tAngle))
                            val tAlpha = (1f - segmentRatio) * (if (isPlaying) 0.52f + liveEnergy * 0.45f else 0.18f)
                            drawCircle(color = magentaLaser.copy(alpha = tAlpha.coerceIn(0f, 1f)), radius = 2.2.dp.toPx() * (1f - segmentRatio * 0.6f), center = tPos)
                        }
                        val pPos = Offset(center.x + currentEccR * cos(pAngle), center.y + currentEccR * sin(pAngle))
                        val pSize = if (isPlaying) (3.0.dp.toPx() + liveEnergy * 3.8.dp.toPx()) else 2.1.dp.toPx()
                        drawCircle(color = magentaLaser.copy(alpha = if (isPlaying) 0.50f + liveEnergy * 0.45f else 0.22f), radius = pSize * 2.2f, center = pPos)
                        drawCircle(color = magentaLaser, radius = pSize, center = pPos)
                        drawCircle(color = Color.White, radius = pSize * 0.55f, center = pPos)
                    }

                    // 轨道 4: 16 颗微光弥散星尘
                    val dustCount = 16
                    val dustStep = (2 * PI / dustCount).toFloat()
                    for (d in 0 until dustCount) {
                        val dAngle = dustStep * d + (dynamicAngle1 * 0.35f * (PI / 180f)).toFloat()
                        val dPos = Offset(center.x + baseR4 * cos(dAngle), center.y + baseR4 * sin(dAngle))
                        val dAlpha = if (isPlaying) (0.25f + (sin(d.toFloat() + dynamicAngle1 * 0.12f) * 0.22f) + liveEnergy * 0.30f).coerceIn(0.1f, 0.85f) else 0.15f
                        drawCircle(color = amberWarm.copy(alpha = dAlpha), radius = 1.5.dp.toPx(), center = dPos)
                    }

                    // 中央声学共振恒星核心 (随人声动态引力膨胀)
                    val nucleusRadius = baseR1 * 0.65f * (if (isPlaying) 1f + liveEnergy * 0.30f else 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (isPlaying) listOf(
                                cyanElectric.copy(alpha = 0.68f + liveEnergy * 0.32f),
                                sonicBlue.copy(alpha = 0.38f + liveEnergy * 0.22f),
                                magentaLaser.copy(alpha = 0.20f),
                                Color.Transparent
                            ) else listOf(
                                cyanElectric.copy(alpha = 0.32f),
                                sonicBlue.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = nucleusRadius
                        ),
                        radius = nucleusRadius,
                        center = center
                    )
                }

                // ==========================================
                // 风格 3: 专业声学频谱仪 (Studio Level Meter & Waveform)
                // ==========================================
                else -> {
                    val meterRadius = (baseRadius * 0.92f) * effectiveScale

                    // 1. 外部极简专业硬件表盘外环与基准刻度
                    drawCircle(
                        color = coreColor.copy(alpha = if (isPlaying) 0.35f else 0.18f),
                        radius = meterRadius,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    val ticks = 48
                    val tickAngleStep = (2 * PI / ticks).toFloat()
                    for (t in 0 until ticks) {
                        val angle = t * tickAngleStep
                        val isMajor = (t % 6 == 0)
                        val tickLen = if (isMajor) 4.5.dp.toPx() else 2.dp.toPx()
                        val tickAlpha = if (isMajor) (if (isPlaying) 0.6f else 0.3f) else (if (isPlaying) 0.25f else 0.12f)
                        val tickWidth = if (isMajor) 1.2.dp.toPx() else 0.7.dp.toPx()

                        val startX = center.x + (meterRadius - tickLen) * cos(angle)
                        val startY = center.y + (meterRadius - tickLen) * sin(angle)
                        val endX = center.x + meterRadius * cos(angle)
                        val endY = center.y + meterRadius * sin(angle)

                        drawLine(
                            color = coreColor.copy(alpha = tickAlpha),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = tickWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // 2. 内部 24 频段高精度声学频谱柱 (左右对称镜像分布，如同 DAW 调音台电平总线)
                    val barCount = 24
                    val barSpacing = (meterRadius * 1.45f) / barCount
                    val startX = center.x - (barCount * barSpacing) / 2f + barSpacing / 2f
                    val maxBarHeight = meterRadius * 0.68f

                    for (b in 0 until barCount) {
                        val bandIdx = if (b < barCount / 2) {
                            (b * (BAND_COUNT / (barCount / 2)))
                        } else {
                            ((barCount - 1 - b) * (BAND_COUNT / (barCount / 2)))
                        }.coerceIn(0, BAND_COUNT - 1)

                        val bandMag = if (isPlaying && spectrumBands.isNotEmpty()) {
                            spectrumBands.getOrElse(bandIdx) { 0.04f }.coerceIn(0.04f, 1f)
                        } else if (isSynthesizing) 0.25f else 0.05f

                        val currentBarH = if (isPlaying) {
                            (bandMag * maxBarHeight + smoothedRms.value * 8.dp.toPx()).coerceAtLeast(3.dp.toPx())
                        } else {
                            2.dp.toPx()
                        }

                        val barX = startX + b * barSpacing
                        val barAlpha = if (isPlaying) (0.45f + bandMag * 0.55f).coerceIn(0.3f, 1f) else 0.25f

                        val barColor = if (isPlaying && bandMag > 0.6f) {
                            androidx.compose.ui.graphics.lerp(coreColor, Color(0xFFFFB300), (bandMag - 0.6f) * 2f)
                        } else {
                            coreColor
                        }

                        drawLine(
                            color = barColor.copy(alpha = barAlpha),
                            start = Offset(barX, center.y - currentBarH / 2f),
                            end = Offset(barX, center.y + currentBarH / 2f),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        if (isPlaying && bandMag > 0.25f) {
                            val peakY = center.y - currentBarH / 2f - 2.5.dp.toPx()
                            drawCircle(
                                color = barColor.copy(alpha = (barAlpha * 0.9f).coerceIn(0f, 1f)),
                                radius = 1.2.dp.toPx(),
                                center = Offset(barX, peakY)
                            )
                        }
                    }

                    // 3. 中心水平基准与动态声能环
                    val innerRmsR = meterRadius * 0.20f * (if (isPlaying) 1f + smoothedRms.value * 0.35f else 1f)
                    drawCircle(
                        color = coreColor.copy(alpha = if (isPlaying) 0.18f + smoothedRms.value * 0.25f else 0.06f),
                        radius = innerRmsR,
                        center = center
                    )
                    drawCircle(
                        color = coreColor.copy(alpha = if (isPlaying) 0.45f + smoothedRms.value * 0.4f else 0.16f),
                        radius = innerRmsR,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}
