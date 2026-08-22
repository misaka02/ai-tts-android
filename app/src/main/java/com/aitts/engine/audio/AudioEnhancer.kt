package com.aitts.engine.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.tanh

/**
 * 软件级音频增强与人声清晰度均衡器 (Voice Clarity EQ & Loudness Normalizer)：
 * 对合成后解码出的 16-bit PCM 音频流进行实时滤波与增益处理，
 * 解决大模型语音在真机扬声器、车载蓝牙或嘈杂环境中人声发闷、音量偏小或爆音问题。
 */
object AudioEnhancer {

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
}
