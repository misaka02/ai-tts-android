package com.aitts.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.MessageDigest

class AudioCacheTest {

    private fun generateTestKey(
        providerId: String,
        modelName: String = "mimo-v2.5",
        voiceId: String,
        promptInstruction: String = "",
        sampleRate: Int = 24000,
        speed: Float,
        pitch: Float,
        text: String
    ): String {
        val raw = "${providerId}_${modelName}_${voiceId}_${promptInstruction.trim()}_${sampleRate}_${speed}_${pitch}_${text.trim()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    @Test
    fun testKeyConsistency() {
        val key1 = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "", 24000, 1.0f, 1.0f, "你好世界")
        val key2 = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "", 24000, 1.0f, 1.0f, "你好世界")
        assertEquals(key1, key2)
    }

    @Test
    fun testKeySensitivity() {
        val key1 = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "自然语气", 24000, 1.0f, 1.0f, "你好世界")
        val keyDiffEmotion = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "愤怒咆哮", 24000, 1.0f, 1.0f, "你好世界")
        val keyDiffSampleRate = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "自然语气", 16000, 1.0f, 1.0f, "你好世界")
        val keyDiffSpeed = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "自然语气", 24000, 1.2f, 1.0f, "你好世界")
        val keyDiffVoice = generateTestKey("mimo_1", "mimo-v2.5", "voice_b", "自然语气", 24000, 1.0f, 1.0f, "你好世界")
        val keyDiffText = generateTestKey("mimo_1", "mimo-v2.5", "voice_a", "自然语气", 24000, 1.0f, 1.0f, "你好不同")

        assertNotEquals(key1, keyDiffEmotion)
        assertNotEquals(key1, keyDiffSampleRate)
        assertNotEquals(key1, keyDiffSpeed)
        assertNotEquals(key1, keyDiffVoice)
        assertNotEquals(key1, keyDiffText)
    }
}
