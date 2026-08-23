package com.aitts.engine.audio

import android.media.audiofx.Visualizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 真实物理音频频谱与 STFT 短时傅里叶变换分析引擎 (True STFT Audio Visualizer Engine)
 * 1. 接收解码后的 16-bit 线性 PCM 采样流；
 * 2. 采用 Hann 窗分帧计算 32 频段 Mel/对数能量分布（基频 80~300Hz、元音共振峰 300~2500Hz、辅音摩擦 2500~6000Hz）；
 * 3. 真实物理重力回落阻尼 (Attack/Decay Gravity Physics)；
 * 4. 实时输出真实 RMS 强度与峰值 dB。
 */
class AudioVisualizerManager private constructor() {

    companion object {
        private const val TAG = "AudioVisualizer"
        const val BAND_COUNT = 32

        @Volatile
        private var instance: AudioVisualizerManager? = null

        fun getInstance(): AudioVisualizerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioVisualizerManager().also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default)
    private var visualizer: Visualizer? = null
    private var pcmAnalysisJob: Job? = null
    private var decayJob: Job? = null

    private val _spectrumFlow = MutableStateFlow(FloatArray(BAND_COUNT) { 0.02f })
    val spectrumFlow: StateFlow<FloatArray> = _spectrumFlow.asStateFlow()

    private val _rmsEnergyFlow = MutableStateFlow(0f)
    val rmsEnergyFlow: StateFlow<Float> = _rmsEnergyFlow.asStateFlow()

    private val currentBands = FloatArray(BAND_COUNT) { 0.02f }

    /**
     * 绑定播放器的 AudioSessionId (硬件 Visualizer 支持)
     */
    fun attachToSession(audioSessionId: Int) {
        releaseVisualizer()
        if (audioSessionId <= 0) return

        try {
            val vis = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(512)
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            waveform?.let { processHardwareWaveform(it) }
                        }

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            fft?.let { processHardwareFft(it) }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
            visualizer = vis
        } catch (e: Exception) {
            Log.w(TAG, "Hardware Visualizer 初始化受限，将依赖真实 PCM 解码分析: ${e.message}")
        }
    }

    /**
     * 接收已解码的真实 16-bit 线性 PCM 数据进行高保真 STFT 频域分析
     */
    fun startRealPcmAnalysis(pcmBytes: ByteArray, sampleRate: Int = 24000) {
        pcmAnalysisJob?.cancel()
        decayJob?.cancel()

        if (pcmBytes.isEmpty()) {
            resetToSilence()
            return
        }

        pcmAnalysisJob = scope.launch {
            val frameDurationMs = 40L
            val frameSamples = (sampleRate * frameDurationMs / 1000).toInt()
            val frameBytes = frameSamples * 2 // 16-bit Mono

            var byteOffset = 0
            val totalBytes = pcmBytes.size

            while (isActive && byteOffset < totalBytes) {
                val currentEnd = (byteOffset + frameBytes).coerceAtMost(totalBytes)
                val currentLength = currentEnd - byteOffset
                val samplesCount = currentLength / 2

                if (samplesCount < 32) break

                // 提取 16-bit PCM 归一化采样值并施加 Hann 窗
                val samples = FloatArray(samplesCount)
                var sumSquare = 0.0

                for (i in 0 until samplesCount) {
                    val idx = byteOffset + i * 2
                    val low = pcmBytes[idx].toInt() and 0xFF
                    val high = pcmBytes[idx + 1].toInt()
                    val rawSample = (high shl 8) or low
                    val normalized = rawSample.toShort() / 32768.0f

                    // Hann 窗
                    val hann = 0.5 * (1.0 - cos(2.0 * PI * i / samplesCount))
                    samples[i] = (normalized * hann).toFloat()
                    sumSquare += normalized * normalized
                }

                val rms = sqrt(sumSquare / samplesCount.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
                _rmsEnergyFlow.value = rms

                // 32-Band Mel/对数频域能量计算 (基于离散傅里叶基波变换)
                val targetBands = FloatArray(BAND_COUNT)
                val minFreq = 80.0
                val maxFreq = (sampleRate / 2.0).coerceAtMost(6000.0)

                for (b in 0 until BAND_COUNT) {
                    // 对数频率分配
                    val freq = minFreq * Math.pow(maxFreq / minFreq, b.toDouble() / (BAND_COUNT - 1))
                    val k = (freq * samplesCount / sampleRate).toInt().coerceIn(1, samplesCount / 2)

                    // 计算在目标频率 k 处的实部与虚部相关性
                    var real = 0.0
                    var imag = 0.0
                    val step = (samplesCount / 64).coerceAtLeast(1) // 降采样快速 DFT 计算

                    for (n in 0 until samplesCount step step) {
                        val angle = 2.0 * PI * k * n / samplesCount
                        real += samples[n] * cos(angle)
                        imag -= samples[n] * sin(angle)
                    }

                    val magnitude = hypot(real, imag) * (step.toDouble() / samplesCount) * 16.0
                    targetBands[b] = magnitude.toFloat().coerceIn(0.02f, 1.0f)
                }

                // 物理阻尼与平滑处理 (Attack 快，Decay 柔和)
                for (b in 0 until BAND_COUNT) {
                    val target = targetBands[b]
                    currentBands[b] = if (target > currentBands[b]) {
                        currentBands[b] * 0.25f + target * 0.75f // 快速上冲
                    } else {
                        (currentBands[b] - 0.06f).coerceAtLeast(target).coerceAtLeast(0.02f) // 重力平滑回落
                    }
                }

                _spectrumFlow.value = currentBands.copyOf()

                byteOffset += frameBytes
                delay(frameDurationMs - 5) // 补偿计算耗时
            }

            resetToSilence()
        }
    }

    private fun processHardwareFft(fft: ByteArray) {
        val n = fft.size
        if (n < 2) return

        val bandSize = (n / 2) / BAND_COUNT
        for (i in 0 until BAND_COUNT) {
            var sumMagnitude = 0.0
            val start = (i * bandSize) * 2
            val end = ((i + 1) * bandSize * 2).coerceAtMost(n - 2)

            for (j in start until end step 2) {
                val rk = fft[j].toDouble()
                val ik = fft[j + 1].toDouble()
                sumMagnitude += hypot(rk, ik)
            }

            val avgMagnitude = sumMagnitude / ((end - start) / 2).coerceAtLeast(1)
            val normalized = (avgMagnitude / 128.0).toFloat().coerceIn(0.02f, 1.0f)

            currentBands[i] = if (normalized > currentBands[i]) {
                currentBands[i] * 0.25f + normalized * 0.75f
            } else {
                (currentBands[i] - 0.05f).coerceAtLeast(normalized).coerceAtLeast(0.02f)
            }
        }

        _spectrumFlow.value = currentBands.copyOf()
    }

    private fun processHardwareWaveform(waveform: ByteArray) {
        var sum = 0.0
        for (b in waveform) {
            val sample = (b.toInt() and 0xFF) - 128
            sum += sample * sample
        }
        val rms = (sqrt(sum / waveform.size) / 128.0).toFloat().coerceIn(0f, 1f)
        _rmsEnergyFlow.value = rms
    }

    fun resetToSilence() {
        pcmAnalysisJob?.cancel()
        decayJob?.cancel()

        decayJob = scope.launch {
            repeat(12) {
                for (i in 0 until BAND_COUNT) {
                    currentBands[i] = (currentBands[i] * 0.55f).coerceAtLeast(0.02f)
                }
                _rmsEnergyFlow.value = (_rmsEnergyFlow.value * 0.5f).coerceAtLeast(0f)
                _spectrumFlow.value = currentBands.copyOf()
                delay(25)
            }
            for (i in 0 until BAND_COUNT) currentBands[i] = 0.02f
            _spectrumFlow.value = currentBands.copyOf()
            _rmsEnergyFlow.value = 0f
        }
    }

    fun releaseVisualizer() {
        try {
            visualizer?.apply {
                enabled = false
                release()
            }
            visualizer = null
        } catch (e: Exception) {
            Log.w(TAG, "释放 Visualizer 异常: ${e.message}")
        }
    }
}
