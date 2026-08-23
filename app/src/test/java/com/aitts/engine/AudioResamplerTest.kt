package com.aitts.engine

import com.aitts.engine.audio.AudioResampler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioResamplerTest {

    @Test
    fun testResample16kTo24kMono() {
        val inSampleRate = 16000
        val targetSampleRate = 24000
        val numSamples = 1600 // 100ms of audio
        val inBytes = ByteArray(numSamples * 2)
        val inBuf = ByteBuffer.wrap(inBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Generate a 440Hz sine wave
        for (i in 0 until numSamples) {
            val sample = (kotlin.math.sin(2.0 * Math.PI * 440.0 * i / inSampleRate) * 16000).toInt().toShort()
            inBuf.putShort(sample)
        }

        val outBytes = AudioResampler.resample(
            pcmData = inBytes,
            sourceSampleRate = inSampleRate,
            sourceChannels = 1,
            targetSampleRate = targetSampleRate,
            targetChannels = 1
        )

        // Target should be 2400 samples (2400 * 2 = 4800 bytes)
        assertEquals(2400 * 2, outBytes.size)
        assertTrue(outBytes.isNotEmpty())
    }

    @Test
    fun testResampleStereoToMono() {
        val inSampleRate = 48000
        val targetSampleRate = 24000
        val numFrames = 4800 // 100ms stereo
        val inBytes = ByteArray(numFrames * 4)
        val inBuf = ByteBuffer.wrap(inBytes).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numFrames) {
            val left = 1000.toShort()
            val right = 3000.toShort()
            inBuf.putShort(left)
            inBuf.putShort(right)
        }

        val outBytes = AudioResampler.resample(
            pcmData = inBytes,
            sourceSampleRate = inSampleRate,
            sourceChannels = 2,
            targetSampleRate = targetSampleRate,
            targetChannels = 1
        )

        // 100ms at 24000Hz mono = 2400 samples * 2 = 4800 bytes
        assertEquals(2400 * 2, outBytes.size)

        val outBuf = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val firstSample = outBuf.get(0)
        // (1000 + 3000) / 2 = 2000
        assertEquals(2000.toShort(), firstSample)
    }

    @Test
    fun testSameRateAndChannelReturnsDirectly() {
        val dummy = byteArrayOf(1, 2, 3, 4, 5, 6)
        val out = AudioResampler.resample(dummy, 24000, 1, 24000, 1)
        assertEquals(dummy.size, out.size)
    }
}
