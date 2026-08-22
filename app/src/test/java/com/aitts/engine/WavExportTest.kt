package com.aitts.engine

import com.aitts.engine.audio.AudioEnhancer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WavExportTest {

    @Test
    fun testPcmToWavEncoding() {
        val pcm = ByteArray(4800) { (it % 128).toByte() }
        val wav = AudioEnhancer.encodePcmToWav(pcm, sampleRate = 24000, channels = 1, bitsPerSample = 16)

        // Standard RIFF/WAVE header is 44 bytes
        assertEquals(44 + pcm.size, wav.size)

        // Check RIFF magic header
        assertEquals('R'.code.toByte(), wav[0])
        assertEquals('I'.code.toByte(), wav[1])
        assertEquals('F'.code.toByte(), wav[2])
        assertEquals('F'.code.toByte(), wav[3])

        // Check WAVE format
        assertEquals('W'.code.toByte(), wav[8])
        assertEquals('A'.code.toByte(), wav[9])
        assertEquals('V'.code.toByte(), wav[10])
        assertEquals('E'.code.toByte(), wav[11])

        // Check fmt subchunk
        assertEquals('f'.code.toByte(), wav[12])
        assertEquals('m'.code.toByte(), wav[13])
        assertEquals('t'.code.toByte(), wav[14])
        assertEquals(' '.code.toByte(), wav[15])

        // Check data subchunk
        assertEquals('d'.code.toByte(), wav[36])
        assertEquals('a'.code.toByte(), wav[37])
        assertEquals('t'.code.toByte(), wav[38])
        assertEquals('a'.code.toByte(), wav[39])
    }

    @Test
    fun testWriteWavToFile() {
        val tempFile = File.createTempFile("test_tts_", ".wav")
        try {
            val pcm = ByteArray(2400)
            AudioEnhancer.writeWavToFile(pcm, tempFile, sampleRate = 24000)
            assertTrue(tempFile.exists())
            assertEquals(44 + 2400L, tempFile.length())
        } finally {
            tempFile.delete()
        }
    }
}
