package com.aitts.engine.ui.material.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.ui.material.GoogleColors
import kotlin.math.sin

/**
 * 🎙️ Google Recorder (官方录音机) 极简圆角柱状声谱波形组件
 * 1. 采用 Google 官方标准的对称圆柱状声波 (Pill waveform bars)；
 * 2. 实时采样 AudioVisualizerManager 真实物理声学 FFT 频段；
 * 3. 待机静默时呈现极低阻尼的自然正弦呼吸流，发音时呈现纯净利落的声谱跃动；
 * 4. 彻底抛弃深空黑底与赛博渐变，融入 Material 3 Tonal Surface。
 */
@Composable
fun GoogleAudioWaveform(
    isPlaying: Boolean,
    colors: GoogleColors,
    modifier: Modifier = Modifier,
    statusText: String = if (isPlaying) "正在合成并播放..." else "待命中 · 点击下方开始试听"
) {
    val visualizerManager = AudioVisualizerManager.getInstance()
    val spectrum by visualizerManager.spectrumFlow.collectAsState()
    val rmsEnergy by visualizerManager.rmsEnergyFlow.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "GoogleWaveTransition")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idlePhase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 声波波形绘制区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                val barCount = 32
                val totalWidth = size.width
                val barWidth = (totalWidth / (barCount * 1.7f)).coerceIn(3.5f, 7.5f)
                val spacing = (totalWidth - (barCount * barWidth)) / (barCount - 1)
                val centerY = size.height / 2f
                val maxBarHeight = size.height * 0.88f
                val minBarHeight = 5.dp.toPx()

                for (i in 0 until barCount) {
                    val x = i * (barWidth + spacing)
                    val height: Float
                    val barColor: Color

                    if (isPlaying) {
                        // 播放中：镜像折叠采样真实 FFT，让中央与两侧呈现优美的对称声谱展开
                        val mirrorIdx = if (i < barCount / 2) {
                            (i * (AudioVisualizerManager.BAND_COUNT - 1) / (barCount / 2)).coerceIn(0, AudioVisualizerManager.BAND_COUNT - 1)
                        } else {
                            ((barCount - 1 - i) * (AudioVisualizerManager.BAND_COUNT - 1) / (barCount / 2)).coerceIn(0, AudioVisualizerManager.BAND_COUNT - 1)
                        }
                        val energy = spectrum.getOrElse(mirrorIdx) { 0.05f }.coerceIn(0f, 1f)
                        height = (minBarHeight + energy * (maxBarHeight - minBarHeight)).coerceIn(minBarHeight, maxBarHeight)

                        // 活跃时：Google 经典蓝与主色点缀
                        val alpha = (0.55f + energy * 0.45f).coerceIn(0.55f, 1f)
                        barColor = colors.primary.copy(alpha = alpha)
                    } else {
                        // 待机中：自然正弦波呼吸
                        val wave = (sin(idlePhase + (i * 0.22f)) + 1f) / 2f
                        height = minBarHeight + wave * (12.dp.toPx())
                        barColor = colors.textTertiary.copy(alpha = 0.4f)
                    }

                    val top = centerY - (height / 2f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, top),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }

        // 状态说明文字与音频指标
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (isPlaying) colors.primary else colors.textTertiary,
                fontWeight = if (isPlaying) FontWeight.Medium else FontWeight.Normal
            )

            Text(
                text = if (isPlaying) "24kHz · STFT" else "Google M3",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textTertiary
            )
        }
    }
}
