package com.aitts.engine.audio

import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.tanh

/**
 * 软件级音频增强与人声清晰度均衡器 (Voice Clarity EQ & Loudness Normalizer)：
 * 对合成后解码出的 16-bit PCM 音频流进行实时滤波与增益处理，
 * 解决大模型语音在真机扬声器、车载蓝牙或嘈杂环境中人声发闷、音量偏小或爆音问题。
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
     * @param pcmData 原始 16-bit PCM 字节数组
     * @param enableClarity 是否启用人声清晰度增强（预加重高通滤波，削弱低频发闷，强化辅音齿音）
     * @param gainFactor 响度增益倍率 (1.0f ~ 2.0f)
     * @return 处理后的 16-bit PCM 字节数组
     */
    fun processPcm(
        pcmData: ByteArray,
        enableClarity: Boolean = false,
        gainFactor: Float = 1.0f
    ): ByteArray {
        if (pcmData.size < 2 || (!enableClarity && gainFactor == 1.0f)) {
            return pcmData
        }

        val shortCount = pcmData.size / 2
        val inputBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
        val outputBuffer = ByteBuffer.allocate(pcmData.size).order(ByteOrder.LITTLE_ENDIAN)

        var prevSample = 0.0f
        val filterAlpha = 0.38f // 预加重高通系数

        for (i in 0 until shortCount) {
            val sample = inputBuffer.short.toFloat()

            // 1. 人声清晰度增强 (Pre-emphasis filter)
            val filtered = if (enableClarity) {
                val current = sample - filterAlpha * prevSample
                prevSample = sample
                current
            } else {
                sample
            }

            // 2. 响度增益与 Soft-clipping 动态范围压缩 (避免数字削顶失真)
            val amplified = filtered * gainFactor
            val normalized = amplified / 32767.0f
            val compressed = tanh(normalized.toDouble()).toFloat() * 32767.0f

            val clampedShort = compressed.toInt().coerceIn(-32768, 32767).toShort()
            outputBuffer.putShort(clampedShort)
        }

        return outputBuffer.array()
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
