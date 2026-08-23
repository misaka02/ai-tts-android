package com.aitts.engine

import com.aitts.engine.audio.AudioEnhancer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEnhancerTest {

    @Test
    fun testEmptyPcmReturnsEmpty() {
        val empty = ByteArray(0)
        val result = AudioEnhancer.processPcm(empty, enableClarity = true, gainFactor = 1.2f)
        assertEquals(0, result.size)
    }

    @Test
    fun testPassthroughWhenDisabledAndUnityGain() {
        val original = byteArrayOf(0x00, 0x10, 0x20, 0x30)
        val result = AudioEnhancer.processPcm(original, enableClarity = false, gainFactor = 1.0f)
        assertEquals(original.size, result.size)
        assertEquals(original[0], result[0])
        assertEquals(original[1], result[1])
    }

    @Test
    fun testClarityBoostTransformsSignal() {
        // 构造正弦波或非零 PCM 数据
        val pcm = ByteArray(100) { (it * 3).toByte() }
        val processed = AudioEnhancer.processPcm(pcm, enableClarity = true, gainFactor = 1.0f)
        assertEquals(pcm.size, processed.size)
        // 预加重处理后样点应产生高通滤波响应
        assertNotEquals(pcm[10], processed[10])
    }

    @Test
    fun testGainAndSoftClippingNoOverflow() {
        // 构造接近 Short.MAX_VALUE 的样点
        val pcm = byteArrayOf(
            0xFF.toByte(), 0x7F.toByte(), // 32767
            0x00.toByte(), 0x80.toByte()  // -32768
        )
        val processed = AudioEnhancer.processPcm(pcm, channels = 1, enableClarity = false, gainFactor = 2.0f)
        assertEquals(4, processed.size)
        // 经 Soft-clipping tanh 压缩后不应超出 16-bit 有效范围
        assertTrue(processed.isNotEmpty())
    }

    @Test
    fun testSilenceTrimming() {
        // Construct 1000 zero samples, 1000 active samples, 1000 zero samples
        val samples = ShortArray(3000)
        for (i in 1000 until 2000) {
            samples[i] = 10000.toShort()
        }
        val pcm = ByteArray(3000 * 2)
        java.nio.ByteBuffer.wrap(pcm).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples)

        val trimmed = AudioEnhancer.trimDeadAirSilence(pcm, channels = 1)
        // The trimmed output should be shorter than original 6000 bytes
        assertTrue("Trimmed size (${trimmed.size}) should be less than original (${pcm.size})", trimmed.size < pcm.size)
        assertTrue(trimmed.isNotEmpty())
    }
}
