package com.aitts.engine.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 工业级 WSOLA (Waveform Similarity Overlap-Add) 变速不变调纯 Kotlin 处理器：
 * 1. 在时域实现毫秒级平滑时间压缩与延展 (0.5x ~ 3.0x)；
 * 2. 100% 保持神经网络语音的原声基频与共振峰，彻底消除花栗鼠变调与电音破音；
 * 3. 专为实时流式与整句 PCM 16-bit 单声道音频设计。
 */
object SonicAudioProcessor {

    /**
     * 处理 16-bit PCM 字节流，按指定倍速缩放时长时间，保持音调与采样率不变
     * @param pcmData 原始 16-bit PCM 裸流 (小端序)
     * @param sampleRate 采样率 (如 24000Hz 或 22050Hz)
     * @param speed 目标倍速 (例如 1.25f, 1.5f, 0.8f)
     * @return 变速不变调后的 16-bit PCM 字节流
     */
    fun process(pcmData: ByteArray, sampleRate: Int, speed: Float): ByteArray {
        if (pcmData.isEmpty()) return pcmData
        val safeSpeed = speed.coerceIn(0.25f, 3.0f)
        // 极微小偏差直接透传，无计算开销
        if (abs(safeSpeed - 1.0f) < 0.02f) return pcmData

        val numSamples = pcmData.size / 2
        val input = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val low = pcmData[i * 2].toInt() and 0xFF
            val high = pcmData[i * 2 + 1].toInt()
            input[i] = ((high shl 8) or low).toShort()
        }

        val output = processShorts(input, sampleRate, safeSpeed)

        val result = ByteArray(output.size * 2)
        for (i in output.indices) {
            val s = output[i].toInt()
            result[i * 2] = (s and 0xFF).toByte()
            result[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return result
    }

    private fun processShorts(input: ShortArray, sampleRate: Int, speed: Float): ShortArray {
        val n = input.size
        // 窗口大小约 20ms
        val winSize = ((sampleRate * 0.020f).toInt()).coerceIn(256, 1024)
        // 叠加大致为窗口的 50%
        val ovl = winSize / 2
        // 合成跳跃步长
        val hs = ovl
        // 分析步长
        val ha = (hs * speed).toInt().coerceAtLeast(1)
        // 相似度搜索容差范围 (+-10ms)
        val searchDelta = ((sampleRate * 0.010f).toInt()).coerceIn(64, 256)

        val estimatedOutputSize = (n / speed).toInt() + winSize * 2
        val output = ShortArray(max(estimatedOutputSize, winSize * 2))
        var inPos = 0
        var outPos = 0

        // 复制第一帧
        val firstLen = min(winSize, n)
        System.arraycopy(input, 0, output, 0, firstLen)
        outPos += hs
        inPos += ha

        while (inPos + winSize + searchDelta < n) {
            // 在 [inPos - searchDelta, inPos + searchDelta] 寻找与上一帧重叠区波形最相似的位置
            val targetStart = inPos
            val minSearch = max(0, targetStart - searchDelta)
            val maxSearch = min(n - winSize, targetStart + searchDelta)

            var bestOffset = inPos
            var bestScore = Long.MIN_VALUE

            // 上一帧的重叠尾部
            val prevOverlapStart = outPos - hs

            var cand = minSearch
            while (cand <= maxSearch) {
                var crossCorr = 0L
                for (k in 0 until ovl step 2) {
                    val s1 = output[prevOverlapStart + k].toLong()
                    val s2 = input[cand + k].toLong()
                    crossCorr += s1 * s2
                }
                if (crossCorr > bestScore) {
                    bestScore = crossCorr
                    bestOffset = cand
                }
                cand += 2
            }

            // 执行平滑交叉渐变 (Cross-fade)
            for (k in 0 until ovl) {
                val alpha = k.toFloat() / ovl
                val sPrev = output[outPos + k].toInt()
                val sNext = input[bestOffset + k].toInt()
                val blended = ((1.0f - alpha) * sPrev + alpha * sNext).toInt()
                output[outPos + k] = blended.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            // 复制重叠后的非重叠部分
            val copyLen = winSize - ovl
            val remaining = output.size - (outPos + ovl)
            if (copyLen > 0 && remaining > 0) {
                val actualCopy = min(copyLen, remaining)
                System.arraycopy(input, bestOffset + ovl, output, outPos + ovl, actualCopy)
            }

            outPos += hs
            inPos = bestOffset + ha
        }

        return output.copyOf(min(outPos, output.size))
    }
}
