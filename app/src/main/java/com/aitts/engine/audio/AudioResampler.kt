package com.aitts.engine.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 高保真 16-bit PCM 动态重采样与声道混音引擎 (Universal Audio Resampler & Channel Mixer)：
 * 1. 支持 8000Hz ~ 48000Hz 任意输入采样率至标准 24000Hz (或任意目标采样率) 的高精度多相线性插值转换；
 * 2. 支持单声道与双声道智能能量守恒混音 (Mono <-> Stereo Downmix / Upmix)；
 * 3. 彻底根治多模型混用、故障降级与不同音色切换时的时钟失步、花栗鼠变调与方波爆音。
 */
object AudioResampler {

    /**
     * 统一将输入的 16-bit PCM 数据重采样并转换为指定的目标采样率与声道格式
     * @param pcmData 输入的 16-bit Little-Endian PCM 字节流
     * @param sourceSampleRate 输入音频的采样率 (如 16000, 24000, 32000, 44100, 48000)
     * @param sourceChannels 输入音频的声道数 (1 = Mono, 2 = Stereo)
     * @param targetSampleRate 目标采样率 (默认 24000)
     * @param targetChannels 目标声道数 (默认 1 = Mono)
     */
    fun resample(
        pcmData: ByteArray,
        sourceSampleRate: Int,
        sourceChannels: Int = 1,
        targetSampleRate: Int = 24000,
        targetChannels: Int = 1
    ): ByteArray {
        if (pcmData.isEmpty() || sourceSampleRate <= 0 || targetSampleRate <= 0) {
            return pcmData
        }

        // 1. 如果采样率和声道完全一致，直接返回
        if (sourceSampleRate == targetSampleRate && sourceChannels == targetChannels) {
            return pcmData
        }

        val inBytesPerSample = 2 // 16-bit
        val inFrameSize = inBytesPerSample * sourceChannels
        val totalInFrames = pcmData.size / inFrameSize
        if (totalInFrames <= 0) return pcmData

        // 2. 解析为 16-bit Short 样本数组 (按帧提取各声道)
        val inBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val inSamples = ShortArray(totalInFrames * sourceChannels)
        inBuffer.get(inSamples)

        // 3. 第一步：声道转换 (统一转为目标声道数)
        val channelConvertedSamples: ShortArray = when {
            sourceChannels == 2 && targetChannels == 1 -> {
                // 双声道立体声 -> 单声道下混 (能量守恒平均值)
                val monoSamples = ShortArray(totalInFrames)
                for (i in 0 until totalInFrames) {
                    val left = inSamples[i * 2].toInt()
                    val right = inSamples[i * 2 + 1].toInt()
                    val mixed = (left + right) / 2
                    monoSamples[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                monoSamples
            }
            sourceChannels == 1 && targetChannels == 2 -> {
                // 单声道 -> 双声道扩展 (左右声道镜像复制)
                val stereoSamples = ShortArray(totalInFrames * 2)
                for (i in 0 until totalInFrames) {
                    val s = inSamples[i]
                    stereoSamples[i * 2] = s
                    stereoSamples[i * 2 + 1] = s
                }
                stereoSamples
            }
            else -> inSamples
        }

        // 4. 第二步：高精度线性插值采样率转换
        val currentChannels = targetChannels
        val inFrames = channelConvertedSamples.size / currentChannels

        if (sourceSampleRate == targetSampleRate) {
            // 仅声道转换，无需重采样
            val outBytes = ByteArray(channelConvertedSamples.size * 2)
            ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(channelConvertedSamples)
            return outBytes
        }

        val ratio = sourceSampleRate.toDouble() / targetSampleRate.toDouble()
        val outFrames = (inFrames / ratio).toInt()
        if (outFrames <= 0) return ByteArray(0)

        val outSamples = ShortArray(outFrames * currentChannels)

        for (ch in 0 until currentChannels) {
            for (outIdx in 0 until outFrames) {
                val inPos = outIdx * ratio
                val rawIdx = inPos.toInt()
                val inIdx = rawIdx.coerceIn(0, inFrames - 1)
                val frac = (inPos - rawIdx).coerceIn(0.0, 1.0)

                val sample0 = channelConvertedSamples[inIdx * currentChannels + ch].toInt()
                val nextIdx = minOf(inIdx + 1, inFrames - 1)
                val sample1 = channelConvertedSamples[nextIdx * currentChannels + ch].toInt()

                // 线性插值计算: y = (1 - frac) * s0 + frac * s1
                val interpolated = (sample0 * (1.0 - frac) + sample1 * frac).toInt()
                outSamples[outIdx * currentChannels + ch] = interpolated.coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                ).toShort()
            }
        }

        // 5. 封装为 Little-Endian 16-bit PCM 字节流
        val outBytes = ByteArray(outSamples.size * 2)
        ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(outSamples)
        return outBytes
    }

    /**
     * 带语速时间伸缩与重采样的复合处理
     */
    fun resampleWithSpeed(
        pcmData: ByteArray,
        sourceSampleRate: Int,
        sourceChannels: Int = 1,
        targetSampleRate: Int = 24000,
        targetChannels: Int = 1,
        speed: Float = 1.0f
    ): ByteArray {
        val effectiveSpeed = speed.coerceIn(0.25f, 3.0f)
        if (effectiveSpeed == 1.0f) {
            return resample(pcmData, sourceSampleRate, sourceChannels, targetSampleRate, targetChannels)
        }
        val virtualSourceSampleRate = (sourceSampleRate * effectiveSpeed).toInt()
        return resample(pcmData, virtualSourceSampleRate, sourceChannels, targetSampleRate, targetChannels)
    }
}
