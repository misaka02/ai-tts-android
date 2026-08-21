package com.aitts.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.MessageDigest

class AudioCacheTest {

    private fun generateTestKey(
        providerId: String,
        voiceId: String,
        speed: Float,
        pitch: Float,
        text: String
    ): String {
        val raw = "${providerId}_${voiceId}_${speed}_${pitch}_${text.trim()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testKeyConsistency() {
        val key1 = generateTestKey("mimo_1", "voice_a", 1.0f, 1.0f, "你好世界")
        val key2 = generateTestKey("mimo_1", "voice_a", 1.0f, 1.0f, "你好世界")
        assertEquals(key1, key2)
    }

    @Test
    fun testKeySensitivity() {
        val key1 = generateTestKey("mimo_1", "voice_a", 1.0f, 1.0f, "你好世界")
        val keyDiffSpeed = generateTestKey("mimo_1", "voice_a", 1.2f, 1.0f, "你好世界")
        val keyDiffVoice = generateTestKey("mimo_1", "voice_b", 1.0f, 1.0f, "你好世界")
        val keyDiffText = generateTestKey("mimo_1", "voice_a", 1.0f, 1.0f, "你好不同")

        assertNotEquals(key1, keyDiffSpeed)
        assertNotEquals(key1, keyDiffVoice)
        assertNotEquals(key1, keyDiffText)
    }
}
