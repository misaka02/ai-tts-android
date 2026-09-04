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

    // 4 频段声学能量独立解耦计算 (80Hz~250Hz 低频基频, 250Hz~1.2kHz 中频共振峰, 1.2kHz~3.5kHz 高频辅音, 3.5kHz~6kHz 超高频空气感)
    val totalBands = if (spectrumBands.isNotEmpty()) spectrumBands.size else BAND_COUNT
    val q1 = (totalBands / 4).coerceAtLeast(1)
    val q2 = (totalBands / 2).coerceAtLeast(q1 + 1)
    val q3 = (3 * totalBands / 4).coerceAtLeast(q2 + 1)

    val bandBass = if (isPlaying && spectrumBands.isNotEmpty()) {
        spectrumBands.take(q1).average().toFloat().coerceIn(0f, 1f)
    } else 0.03f

    val bandMid = if (isPlaying && spectrumBands.isNotEmpty()) {
        spectrumBands.slice(q1 until q2).average().toFloat().coerceIn(0f, 1f)
    } else 0.03f

    val bandHighMid = if (isPlaying && spectrumBands.isNotEmpty()) {
        spectrumBands.slice(q2 until q3).average().toFloat().coerceIn(0f, 1f)
    } else 0.03f

    val bandTreble = if (isPlaying && spectrumBands.isNotEmpty()) {
        spectrumBands.takeLast(totalBands - q3).average().toFloat().coerceIn(0f, 1f)
    } else 0.03f

    // 连续频域平滑余弦插值函数：输入归一化频位 position (0.0=低频基频, 1.0=高频空气感)，输出连续平滑振幅
    fun sampleSpectrumSmooth(position: Float): Float {
        if (!isPlaying || spectrumBands.isEmpty()) return 0.03f
        val maxIdx = spectrumBands.size - 1
        val floatIdx = position.coerceIn(0f, 1f) * maxIdx
        val i0 = floatIdx.toInt().coerceIn(0, maxIdx)
        val i1 = (i0 + 1).coerceIn(0, maxIdx)
        val frac = floatIdx - i0
        val smoothFrac = (1f - cos(frac * PI.toFloat())) * 0.5f
        return (spectrumBands[i0] + (spectrumBands[i1] - spectrumBands[i0]) * smoothFrac).coerceIn(0.02f, 1f)
    }

    val bassEnergy = bandBass
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

                    // 5. 环外 72 段高精度双侧对称声学极光刻度阵列 (Bilateral Symmetrical Frequency Aura)
                    // 底部 6 点钟方向为低频基频 (Bass)，向左右两侧平滑延伸上升至顶部 12 点钟高频空气感 (Treble)
                    val dialTicks = 72
                    val dialAngleStep = (2 * PI / dialTicks).toFloat()

                    for (t in 0 until dialTicks) {
                        val angle = (t * dialAngleStep) - (PI / 2).toFloat()
                        // 归一化双侧频率位置 (0.0=低频基频, 1.0=高频空气感)
                        val normFreq = if (t <= 36) {
                            (36 - t) / 36f
                        } else {
                            (t - 36) / 36f
                        }

                        val bandMag = if (isPlaying && spectrumBands.isNotEmpty()) {
                            sampleSpectrumSmooth(normFreq)
                        } else if (isSynthesizing) 0.22f else 0.02f

                        // 6 格周期内精密阶梯微渐变 (增强表盘微观层次感与精密度)
                        val subIndex = t % 6
                        val (baseLen, baseAlpha, baseWidth) = when (subIndex) {
                            0 -> Triple(3.8.dp.toPx(), 0.30f, 1.30.dp.toPx()) // 0: 主刻度
                            3 -> Triple(2.8.dp.toPx(), 0.22f, 1.00.dp.toPx()) // 3: 中节点刻度
                            2, 4 -> Triple(2.1.dp.toPx(), 0.17f, 0.85.dp.toPx()) // 2,4: 次刻度
                            else -> Triple(1.5.dp.toPx(), 0.12f, 0.65.dp.toPx()) // 1,5: 极细暗刻度
                        }

                        // 频段加权动态律动 (低频量感沉稳深远，高频灵巧微颤，自然呈现人声频响特性)
                        val freqWeight = (1.15f - normFreq * 0.30f)
                        val dynamicLen = if (isPlaying) bandMag * 22.dp.toPx() * freqWeight else 0f
                        val tickLen = baseLen + dynamicLen

                        val tickAlpha = if (isPlaying) {
                            (0.35f + bandMag * 0.62f).coerceIn(0.25f, 0.98f)
                        } else {
                            baseAlpha
                        }

                        val tickWidth = if (isPlaying) {
                            baseWidth * (1.30f + bandMag * 1.60f)
                        } else {
                            baseWidth
                        }

                        // 频率色彩映射：低频深邃海蓝/电青 -> 中频人声蔚蓝/青紫 -> 高频灵动晶白/洋红微芒
                        val baseColor = if (isPlaying) {
                            when {
                                normFreq < 0.38f -> androidx.compose.ui.graphics.lerp(Color(0xFF00E5FF), Color(0xFF00B0FF), bandMag)
                                normFreq < 0.72f -> androidx.compose.ui.graphics.lerp(Color(0xFF38BDF8), Color(0xFF818CF8), bandMag)
                                else -> androidx.compose.ui.graphics.lerp(Color(0xFFFF4081), Color(0xFFFFFFFF), bandMag)
                            }
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

                    // 2. 36 颗双侧对称声谱悬停微珠与磁通光束 (Symmetrical Magnetic Spectrum Nodes)
                    // 底部 6 点钟 (i=18) 为低频基频 (Bass)，两侧上升至顶部 12 点钟 (i=0) 为高频空气感 (Treble)
                    val dotsCount = 36
                    val angleStep = (2 * PI / dotsCount).toFloat()

                    for (i in 0 until dotsCount) {
                        val angle = (i * angleStep) - (PI / 2).toFloat()
                        // 归一化频率位置 (0.0=低频基频, 1.0=高频空气感)
                        val normFreq = if (i <= 18) {
                            (18 - i) / 18f
                        } else {
                            (i - 18) / 18f
                        }

                        val bandMag = if (isPlaying && spectrumBands.isNotEmpty()) {
                            sampleSpectrumSmooth(normFreq)
                        } else if (isSynthesizing) 0.20f else 0.03f

                        val radialHoverOffset = if (isPlaying) {
                            (bandMag * 18.dp.toPx() * (1.15f - normFreq * 0.35f))
                        } else if (isSynthesizing) 3.dp.toPx() else 0.dp.toPx()

                        val dotRadius = trackRadius + 4.dp.toPx() + radialHoverOffset
                        val dotCenter = Offset(center.x + dotRadius * cos(angle), center.y + dotRadius * sin(angle))

                        // 径向微光磁力线 (Magnetic Flux Tether)
                        val tetherStart = Offset(center.x + (trackRadius - 2.dp.toPx()) * cos(angle), center.y + (trackRadius - 2.dp.toPx()) * sin(angle))
                        val tetherAlpha = if (isPlaying) (0.15f + bandMag * 0.55f).coerceIn(0.1f, 0.90f) else 0.10f
                        val tetherColor = when {
                            normFreq < 0.40f -> cyanElectric
                            normFreq < 0.75f -> sonicBlue
                            else -> magentaLaser
                        }
                        drawLine(
                            color = tetherColor.copy(alpha = tetherAlpha),
                            start = tetherStart,
                            end = dotCenter,
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // 悬停量子微珠：低频饱满稳重，中频温润，高频晶莹透亮
                        val beadColor = when {
                            !isPlaying -> sonicBlue
                            normFreq < 0.40f -> androidx.compose.ui.graphics.lerp(cyanElectric, Color(0xFF00E5FF), bandMag)
                            normFreq < 0.75f -> androidx.compose.ui.graphics.lerp(sonicBlue, Color(0xFF818CF8), bandMag)
                            else -> androidx.compose.ui.graphics.lerp(magentaLaser, Color.White, bandMag)
                        }
                        val beadAlpha = if (isPlaying) (0.50f + bandMag * 0.50f).coerceIn(0.4f, 1f) else 0.35f
                        val baseBeadR = (2.2.dp.toPx() * (1.2f - normFreq * 0.4f)) // 低频珠子稍大，高频微珠精细
                        val beadRadius = if (isPlaying) baseBeadR + (bandMag * 1.6.dp.toPx()) else baseBeadR

                        drawCircle(color = beadColor.copy(alpha = beadAlpha * 0.32f), radius = beadRadius * 2.2f, center = dotCenter)
                        drawCircle(color = if (isPlaying && bandMag > 0.45f) Color.White else beadColor.copy(alpha = beadAlpha), radius = beadRadius, center = dotCenter)
                    }

                    // 3. 高清透光全息声学光镜 (Acoustic Glass Aperture & Multi-Band Resonance)
                    val lensRadius = trackRadius * 0.52f * (if (isPlaying) 1f + bandMid * 0.18f else 1f)

                    // A. 底层纯净半透明声学呼吸星云 (随低频基频律动)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                cyanElectric.copy(alpha = if (isPlaying) 0.35f + bandBass * 0.40f else 0.15f),
                                sonicBlue.copy(alpha = if (isPlaying) 0.15f + bandBass * 0.15f else 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = lensRadius * 1.4f
                        ),
                        radius = lensRadius * 1.4f,
                        center = center
                    )

                    // B. 精密全息光圈外环与内同心微环 (随中频人声共振)
                    drawCircle(
                        color = cyanElectric.copy(alpha = if (isPlaying) 0.45f + bandMid * 0.35f else 0.25f),
                        radius = lensRadius,
                        center = center,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawCircle(
                        color = sonicBlue.copy(alpha = if (isPlaying) 0.35f + bandMid * 0.25f else 0.18f),
                        radius = lensRadius * 0.65f * (if (isPlaying) 1f + bandMid * 0.12f else 1f),
                        center = center,
                        style = Stroke(width = 0.9.dp.toPx())
                    )
                    drawCircle(
                        color = cyanElectric.copy(alpha = if (isPlaying) 0.60f + bandBass * 0.40f else 0.30f),
                        radius = lensRadius * 0.28f * (if (isPlaying) 1f + bandBass * 0.25f else 1f),
                        center = center
                    )

                    // C. 四向极简精密声学十字微刻度 (随高频辅音瞬态展开)
                    val crosshairLen = 5.dp.toPx() + (if (isPlaying) bandHighMid * 4.dp.toPx() else 0f)
                    for (deg in listOf(0, 90, 180, 270)) {
                        val rad = deg * (PI / 180f).toFloat()
                        val pStart = Offset(center.x + (lensRadius - crosshairLen) * cos(rad), center.y + (lensRadius - crosshairLen) * sin(rad))
                        val pEnd = Offset(center.x + (lensRadius + crosshairLen * 0.5f) * cos(rad), center.y + (lensRadius + crosshairLen * 0.5f) * sin(rad))
                        drawLine(
                            color = (if (isPlaying && bandHighMid > 0.4f) Color.White else cyanElectric).copy(alpha = if (isPlaying) 0.55f + bandHighMid * 0.40f else 0.25f),
                            start = pStart,
                            end = pEnd,
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // ==========================================
                // 风格 2: 4重天体引力共振轨道 (Celestial 4-Orbit Harmonic System)
                // 4 阶同心轨道分别精准对应：低频基频 (Bass) -> 中频人声 (Mid) -> 高频辅音 (High-Mid) -> 超高频空气感 (Treble)
                // ==========================================
                else -> {
                    val orbit1Energy = if (isPlaying) (bandBass * 0.75f + smoothedRms.value * 0.25f) else 0f
                    val orbit2Energy = if (isPlaying) (bandMid * 0.75f + smoothedRms.value * 0.25f) else 0f
                    val orbit3Energy = if (isPlaying) (bandHighMid * 0.80f + bandTreble * 0.20f) else 0f
                    val orbit4Energy = if (isPlaying) bandTreble else 0f

                    // 4 重基准轨道半径 (各轨道依据所属频段能量独立微膨胀共振)
                    val baseR1 = (baseRadius * 0.56f + orbit1Energy * 18.dp.toPx()) * effectiveScale
                    val baseR2 = (baseRadius * 0.86f + orbit2Energy * 22.dp.toPx()) * effectiveScale
                    val baseR3 = (baseRadius * 1.16f + orbit3Energy * 26.dp.toPx()) * effectiveScale
                    val baseR4 = (baseRadius * 1.42f + orbit4Energy * 20.dp.toPx()) * effectiveScale

                    // 偏心开普勒轨道生成辅助函数
                    fun getKeplerRadius(baseR: Float, angleRad: Float, ecc: Float, periAngleRad: Float): Float {
                        return baseR * (1f - ecc * 0.28f * cos(angleRad - periAngleRad))
                    }

                    // 绘制 4 重微点引力光丝轨道 (采用 48 点精密同心引力丝)
                    fun drawFilamentOrbit(r: Float, col: Color, alphaBase: Float, bandEnergyVal: Float) {
                        val orbitTicks = 48
                        val tickStep = (2 * PI / orbitTicks).toFloat()
                        for (i in 0 until orbitTicks) {
                            val a = i * tickStep
                            val tLen = (2 * PI * r / orbitTicks * 0.45f).toFloat()
                            drawArc(
                                color = col.copy(alpha = if (isPlaying) (alphaBase + bandEnergyVal * 0.45f).coerceIn(0.1f, 0.85f) else alphaBase),
                                startAngle = (a * 180f / PI).toFloat(),
                                sweepAngle = (tLen / r * 180f / PI).toFloat(),
                                useCenter = false,
                                topLeft = Offset(center.x - r, center.y - r),
                                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                                style = Stroke(width = 0.9.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    drawFilamentOrbit(baseR1, cyanElectric, 0.25f, orbit1Energy)
                    drawFilamentOrbit(baseR2, sonicBlue, 0.20f, orbit2Energy)
                    drawFilamentOrbit(baseR3, magentaLaser, 0.16f, orbit3Energy)
                    drawFilamentOrbit(baseR4, amberWarm, 0.12f, orbit4Energy)

                    val tailSegments = if (isPlaying) 18 else 8

                    // 轨道 1: 2 颗低频量子光子 (偏心率 0.52，随低频基频爆发公转)
                    val p1BaseRad = (dynamicAngle1 * (PI / 180f)).toFloat()
                    val maxTail1 = if (isPlaying) (0.35f + orbit1Energy * 0.85f) else 0.18f
                    for (k in 0..1) {
                        val pAngle = p1BaseRad + (k * PI).toFloat()
                        val currentEccR = getKeplerRadius(baseR1, pAngle, 0.52f, 0f)
                        val keplerSpeedFactor = (1f + 0.52f * cos(pAngle)).let { it * it }

                        for (t in 1..tailSegments) {
                            val segmentRatio = t.toFloat() / tailSegments
                            val tAngle = pAngle - (segmentRatio * maxTail1 * (0.6f + keplerSpeedFactor * 0.4f))
                            val tRadius = getKeplerRadius(baseR1, tAngle, 0.52f, 0f)
                            val tPos = Offset(center.x + tRadius * cos(tAngle), center.y + tRadius * sin(tAngle))
                            val tAlpha = (1f - segmentRatio) * (if (isPlaying) 0.65f + orbit1Energy * 0.35f else 0.25f)
                            drawCircle(color = cyanElectric.copy(alpha = tAlpha.coerceIn(0f, 1f)), radius = 2.8.dp.toPx() * (1f - segmentRatio * 0.6f), center = tPos)
                        }
                        val pPos = Offset(center.x + currentEccR * cos(pAngle), center.y + currentEccR * sin(pAngle))
                        val pSize = if (isPlaying) (3.6.dp.toPx() + orbit1Energy * 4.2.dp.toPx()) else 2.6.dp.toPx()
                        drawCircle(color = cyanElectric.copy(alpha = if (isPlaying) 0.60f + orbit1Energy * 0.4f else 0.32f), radius = pSize * 2.5f, center = pPos)
                        drawCircle(color = cyanElectric, radius = pSize, center = pPos)
                        drawCircle(color = Color.White, radius = pSize * 0.60f, center = pPos)
                    }

                    // 轨道 2: 3 颗中频人声主卫星 (偏心率 0.56，随中频共振峰逆行加速)
                    val p2BaseRad = (dynamicAngle2 * (PI / 180f)).toFloat()
                    val maxTail2 = if (isPlaying) (0.35f + orbit2Energy * 0.85f) else 0.18f
                    for (k in 0..2) {
                        val pAngle = p2BaseRad + (k * 2 * PI / 3).toFloat()
                        val currentEccR = getKeplerRadius(baseR2, pAngle, 0.56f, (PI / 2).toFloat())
                        val keplerSpeedFactor = (1f + 0.56f * cos(pAngle + (PI / 2).toFloat())).let { it * it }

                        for (t in 1..tailSegments) {
                            val segmentRatio = t.toFloat() / tailSegments
                            val tAngle = pAngle + (segmentRatio * maxTail2 * (0.6f + keplerSpeedFactor * 0.4f))
                            val tRadius = getKeplerRadius(baseR2, tAngle, 0.56f, (PI / 2).toFloat())
                            val tPos = Offset(center.x + tRadius * cos(tAngle), center.y + tRadius * sin(tAngle))
                            val tAlpha = (1f - segmentRatio) * (if (isPlaying) 0.55f + orbit2Energy * 0.40f else 0.20f)
                            drawCircle(color = sonicBlue.copy(alpha = tAlpha.coerceIn(0f, 1f)), radius = 2.4.dp.toPx() * (1f - segmentRatio * 0.6f), center = tPos)
                        }
                        val pPos = Offset(center.x + currentEccR * cos(pAngle), center.y + currentEccR * sin(pAngle))
                        val pSize = if (isPlaying) (3.2.dp.toPx() + orbit2Energy * 3.6.dp.toPx()) else 2.3.dp.toPx()
                        drawCircle(color = sonicBlue.copy(alpha = if (isPlaying) 0.55f + orbit2Energy * 0.4f else 0.28f), radius = pSize * 2.2f, center = pPos)
                        drawCircle(color = sonicBlue, radius = pSize, center = pPos)
                        drawCircle(color = Color.White, radius = pSize * 0.55f, center = pPos)
                    }

                    // 轨道 3: 2 颗高频偏心彗星 (偏心率 0.48，随齿音与辅音瞬态拖尾)
                    val p3BaseRad = (dynamicAngle3 * (PI / 180f)).toFloat()
                    val maxTail3 = if (isPlaying) (0.35f + orbit3Energy * 1.10f) else 0.18f
                    val cometSegments = if (isPlaying) (14 + (orbit3Energy * 10).toInt()) else 8
                    for (k in 0..1) {
                        val pAngle = p3BaseRad + (k * PI).toFloat()
                        val currentEccR = getKeplerRadius(baseR3, pAngle, 0.48f, PI.toFloat())
                        val keplerSpeedFactor = (1f + 0.48f * cos(pAngle + PI.toFloat())).let { it * it }

                        for (t in 1..cometSegments) {
                            val segmentRatio = t.toFloat() / cometSegments
                            val tAngle = pAngle - (segmentRatio * maxTail3 * 1.4f * (0.6f + keplerSpeedFactor * 0.4f))
                            val tRadius = getKeplerRadius(baseR3, tAngle, 0.48f, PI.toFloat())
                            val tPos = Offset(center.x + tRadius * cos(tAngle), center.y + tRadius * sin(tAngle))
                            val tAlpha = (1f - segmentRatio) * (if (isPlaying) 0.52f + orbit3Energy * 0.45f else 0.18f)
                            drawCircle(color = magentaLaser.copy(alpha = tAlpha.coerceIn(0f, 1f)), radius = 2.2.dp.toPx() * (1f - segmentRatio * 0.6f), center = tPos)
                        }
                        val pPos = Offset(center.x + currentEccR * cos(pAngle), center.y + currentEccR * sin(pAngle))
                        val pSize = if (isPlaying) (3.0.dp.toPx() + orbit3Energy * 3.8.dp.toPx()) else 2.1.dp.toPx()
                        drawCircle(color = magentaLaser.copy(alpha = if (isPlaying) 0.50f + orbit3Energy * 0.45f else 0.22f), radius = pSize * 2.2f, center = pPos)
                        drawCircle(color = magentaLaser, radius = pSize, center = pPos)
                        drawCircle(color = Color.White, radius = pSize * 0.55f, center = pPos)
                    }

                    // 轨道 4: 16 颗超高频微光弥散星尘 (随超高频空气感与泛音闪烁)
                    val dustCount = 16
                    val dustStep = (2 * PI / dustCount).toFloat()
                    for (d in 0 until dustCount) {
                        val dAngle = dustStep * d + (dynamicAngle1 * 0.35f * (PI / 180f)).toFloat()
                        val dPos = Offset(center.x + baseR4 * cos(dAngle), center.y + baseR4 * sin(dAngle))
                        val dAlpha = if (isPlaying) (0.22f + (sin(d.toFloat() + dynamicAngle1 * 0.12f) * 0.20f) + orbit4Energy * 0.45f).coerceIn(0.1f, 0.95f) else 0.15f
                        val dRadius = if (isPlaying) (1.4.dp.toPx() + orbit4Energy * 1.4.dp.toPx()) else 1.4.dp.toPx()
                        drawCircle(color = amberWarm.copy(alpha = dAlpha), radius = dRadius, center = dPos)
                    }

                    // 中央声学共振恒星核心 (随人声低频基频与综合能量动态引力膨胀)
                    val nucleusRadius = baseR1 * 0.65f * (if (isPlaying) 1f + orbit1Energy * 0.30f else 1f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (isPlaying) listOf(
                                cyanElectric.copy(alpha = 0.68f + orbit1Energy * 0.32f),
                                sonicBlue.copy(alpha = 0.38f + orbit2Energy * 0.22f),
                                magentaLaser.copy(alpha = 0.20f + orbit3Energy * 0.20f),
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
            }
        }
    }
}
