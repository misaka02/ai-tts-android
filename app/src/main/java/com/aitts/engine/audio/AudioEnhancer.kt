package com.aitts.engine.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.tanh

/**
 * 软件级音频增强与人声清晰度均衡器 (Voice Clarity EQ & Loudness Normalizer & VAD Silence Trimmer)：
 * 1. 对 16-bit PCM 音频流进行声道隔离滤波（彻底杜绝双声道反相梳状相位失真）；
 * 2. tanh 软饱和动态范围压缩（防削顶与爆音）；
 * 3. 智能 PCM 能量 VAD 首尾死区静音切除与 5ms 线性抗爆音微渐变 (Anti-Pop Fade)。
 */
object AudioEnhancer {

    enum class EqPreset(
        val id: String,
        val displayName: String,
        val enableClarity: Boolean,
        val gainFactor: Float,
        val description: String
    ) {
        CLEAR_VOICE("clear_voice", "✨ 清澈人声", true, 1.25f, "高通滤波提升 1k~4kHz 人声齿音，削弱低频浑浊，适合通勤与嘈杂环境"),
        WARM_BROADCAST("warm_broadcast", "🎙️ 磁性电台", false, 1.4f, "适度提升近场响度，动态柔和压缩，声音饱满沉稳"),
        GENTLE_EAR_PROTECT("gentle_ear_protect", "🌙 睡前护耳", false, 0.95f, "软饱和限幅平滑动态，削弱刺耳高频，适合夜间长时间听书"),
        PASSTHROUGH("passthrough", "📻 原声直出", false, 1.0f, "不经过任何滤波与响度处理，保留大模型原始音频输出"),
        CUSTOM("custom", "⚙️ 自定义参数", false, 1.0f, "手动调节高通滤波开关与响度增益倍率")
    }

    /**
     * 处理 PCM 16-bit 音频数据
     * @param pcmData 原始 16-bit Little-Endian PCM 字节数组
     * @param channels 声道数 (1 = Mono, 2 = Stereo)
     * @param enableClarity 是否启用人声清晰度增强（独立声道预加重高通滤波）
     * @param gainFactor 响度增益倍率 (1.0f ~ 2.5f)
     * @param trimSilence 是否切除大模型生成的首尾死区静音
     * @return 处理后的 16-bit PCM 字节数组
     */
    fun processPcm(
        pcmData: ByteArray,
        channels: Int = 1,
        enableClarity: Boolean = false,
        gainFactor: Float = 1.0f,
        trimSilence: Boolean = false,
        normalizeLoudness: Boolean = false
    ): ByteArray {
        if (pcmData.size < 2) {
            return pcmData
        }

        // 1. 如果启用了静音切除，先修剪首尾死区
        val activePcm = if (trimSilence) {
            trimDeadAirSilence(pcmData, channels)
        } else {
            pcmData
        }

        // 2. 测量有效 RMS 能量，自适应平衡不同声学模型的音量落差
        var effectiveGain = gainFactor
        if (normalizeLoudness && activePcm.size >= 100) {
            val shortCount = activePcm.size / 2
            val bbTest = ByteBuffer.wrap(activePcm).order(ByteOrder.LITTLE_ENDIAN)
            var sumSquare = 0.0
            var validCount = 0
            for (k in 0 until shortCount) {
                val s = bbTest.short.toDouble()
                if (abs(s) > 100.0) {
                    sumSquare += s * s
                    validCount++
                }
            }
            if (validCount > 50) {
                val rms = kotlin.math.sqrt(sumSquare / validCount)
                // 设定舒适听书 RMS 目标 ~3800 (约 -18.7 dBFS)
                val targetRms = 3800.0
                if (rms in 200.0..3000.0) {
                    val autoBoost = (targetRms / rms).toFloat().coerceIn(1.0f, 2.0f)
                    effectiveGain *= autoBoost
                }
            }
        }

        if (!enableClarity && effectiveGain == 1.0f) {
            return activePcm
        }

        val inFrameSize = 2 * channels
        val totalFrames = activePcm.size / inFrameSize
        if (totalFrames <= 0) return activePcm

        val inputBuffer = ByteBuffer.wrap(activePcm).order(ByteOrder.LITTLE_ENDIAN)
        val outputBuffer = ByteBuffer.allocate(activePcm.size).order(ByteOrder.LITTLE_ENDIAN)

        var prevLeft = 0.0f
        var prevRight = 0.0f
        val filterAlpha = 0.38f // 预加重高通系数

        for (i in 0 until totalFrames) {
            if (channels == 1) {
                val sample = inputBuffer.short.toFloat()
                val filtered = if (enableClarity) {
                    val cur = sample - filterAlpha * prevLeft
                    prevLeft = sample
                    cur
                } else sample

                val amplified = filtered * effectiveGain
                val normalized = amplified / 32767.0f
                val compressed = tanh(normalized.toDouble()).toFloat() * 32767.0f
                outputBuffer.putShort(compressed.toInt().coerceIn(-32768, 32767).toShort())
            } else {
                // 立体声：独立维护左右声道滤波器状态，杜绝相位串扰
                val sampleL = inputBuffer.short.toFloat()
                val sampleR = inputBuffer.short.toFloat()

                val filteredL = if (enableClarity) {
                    val curL = sampleL - filterAlpha * prevLeft
                    prevLeft = sampleL
                    curL
                } else sampleL

                val filteredR = if (enableClarity) {
                    val curR = sampleR - filterAlpha * prevRight
                    prevRight = sampleR
                    curR
                } else sampleR

                val compL = tanh((filteredL * effectiveGain / 32767.0f).toDouble()).toFloat() * 32767.0f
                val compR = tanh((filteredR * effectiveGain / 32767.0f).toDouble()).toFloat() * 32767.0f

                outputBuffer.putShort(compL.toInt().coerceIn(-32768, 32767).toShort())
                outputBuffer.putShort(compR.toInt().coerceIn(-32768, 32767).toShort())
            }
        }

        return outputBuffer.array()
    }

    /**
     * 智能切除大模型生成的首尾死区静音 (PCM Energy VAD Silence Trimming)，并施加 5ms 平滑抗爆音渐变
     */
    fun trimDeadAirSilence(pcmData: ByteArray, channels: Int = 1, thresholdAbs: Short = 300): ByteArray {
        val frameSize = 2 * channels
        val totalFrames = pcmData.size / frameSize
        if (totalFrames < 240) return pcmData // 样本过短不处理

        val shortBuf = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = ShortArray(totalFrames * channels)
        shortBuf.get(samples)

        // 寻找起始非静音帧
        var startFrame = 0
        while (startFrame < totalFrames) {
            var hasSignal = false
            for (ch in 0 until channels) {
                if (abs(samples[startFrame * channels + ch].toInt()) > thresholdAbs) {
                    hasSignal = true
                    break
                }
            }
            if (hasSignal) break
            startFrame++
        }

        // 寻找结束非静音帧
        var endFrame = totalFrames - 1
        while (endFrame > startFrame) {
            var hasSignal = false
            for (ch in 0 until channels) {
                if (abs(samples[endFrame * channels + ch].toInt()) > thresholdAbs) {
                    hasSignal = true
                    break
                }
            }
            if (hasSignal) break
            endFrame--
        }

        // 保留 10ms 的微量自然边距
        val marginFrames = 120
        startFrame = maxOf(0, startFrame - marginFrames)
        endFrame = minOf(totalFrames - 1, endFrame + marginFrames)

        val trimmedFrames = endFrame - startFrame + 1
        if (trimmedFrames <= 0 || trimmedFrames == totalFrames) {
            return pcmData
        }

        // 施加 5ms (约 60 帧) 线性微渐入与渐出，杜绝数字切音爆裂
        val fadeFrames = minOf(60, trimmedFrames / 2)
        val outSamples = ShortArray(trimmedFrames * channels)

        for (i in 0 until trimmedFrames) {
            val srcFrame = startFrame + i
            val fadeFactor = when {
                i < fadeFrames -> i.toFloat() / fadeFrames
                i > trimmedFrames - fadeFrames -> (trimmedFrames - 1 - i).toFloat() / fadeFrames
                else -> 1.0f
            }

            for (ch in 0 until channels) {
                val orig = samples[srcFrame * channels + ch]
                outSamples[i * channels + ch] = (orig * fadeFactor).toInt().toShort()
            }
        }

        val outBytes = ByteArray(outSamples.size * 2)
        ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outSamples)
        return outBytes
    }

    /**
     * 将 16-bit PCM 字节流封装为标准的 RIFF / WAVE (.wav) 文件字节流
     */
    fun encodePcmToWav(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val totalAudioLen = pcmData.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        buffer.position(4)
        buffer.putInt(totalDataLen)

        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        buffer.position(16)
        buffer.putInt(16) // SubChunk1Size (16 for PCM)
        buffer.putShort(1.toShort()) // AudioFormat (1 for PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())

        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        buffer.position(40)
        buffer.putInt(totalAudioLen)

        val wavBytes = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavBytes, 0, 44)
        System.arraycopy(pcmData, 0, wavBytes, 44, pcmData.size)
        return wavBytes
    }

    /**
     * 将 PCM 数据封装写入到本地 WAV 文件
     */
    fun writeWavToFile(
        pcmData: ByteArray,
        outputFile: File,
        sampleRate: Int = 24000,
        channels: Int = 1
    ): File {
        val wavBytes = encodePcmToWav(pcmData, sampleRate, channels)
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            fos.write(wavBytes)
            fos.flush()
        }
        return outputFile
    }
}
