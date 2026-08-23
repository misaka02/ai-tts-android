package com.aitts.engine

import com.aitts.engine.audio.AudioVisualizerManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioVisualizerTest {

    @Test
    fun testVisualizerInitialState() {
        val manager = AudioVisualizerManager.getInstance()
        assertNotNull(manager)
        val spectrum = manager.spectrumFlow.value
        assertEquals(AudioVisualizerManager.BAND_COUNT, spectrum.size)
    }

    @Test
    fun testPcmSimulationAndDecay() = runBlocking {
        val manager = AudioVisualizerManager.getInstance()
        
        // 生成 1 秒正弦波 PCM 16-bit 24kHz
        val sampleRate = 24000
        val pcm = ByteArray(sampleRate * 2)
        for (i in 0 until sampleRate) {
            val sample = (kotlin.math.sin(2 * Math.PI * 440 * i / sampleRate) * 16000).toInt().toShort()
            pcm[i * 2] = (sample.toInt() and 0xFF).toByte()
            pcm[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        manager.startRealPcmAnalysis(pcm, sampleRate)
        kotlinx.coroutines.delay(100)

        val bands = manager.spectrumFlow.value
        assertEquals(AudioVisualizerManager.BAND_COUNT, bands.size)
        
        manager.resetToSilence()
        kotlinx.coroutines.delay(350)
        
        val silenceBands = manager.spectrumFlow.value
        for (b in silenceBands) {
            assertTrue(b <= 0.05f)
        }
    }
}
