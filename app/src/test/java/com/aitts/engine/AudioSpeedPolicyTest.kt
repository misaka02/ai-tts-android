package com.aitts.engine

import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.requiresClientSpeedScaling
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSpeedPolicyTest {

    @Test
    fun testEdgeTtsSpeedPolicy() {
        val edgeConfig = TtsProviderConfig(id = "edge", type = ProviderType.EDGE_TTS, name = "Edge", speed = 0.85f)
        // Edge TTS: SSML prosody handles speed on Microsoft server in both modes
        assertFalse(edgeConfig.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(edgeConfig.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testMiniMaxSpeedPolicy() {
        val minimax = TtsProviderConfig(id = "minimax", type = ProviderType.MINIMAX, name = "MiniMax", speed = 1.5f)
        // MiniMax: API voice_setting.speed handles speed on server
        assertFalse(minimax.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(minimax.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testOpenAiSpeedPolicy() {
        val openai = TtsProviderConfig(id = "openai", type = ProviderType.OPENAI, name = "OpenAI", speed = 1.5f)
        // OpenAI: API speed handles speed on server
        assertFalse(openai.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(openai.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testSiliconFlowSpeedPolicy() {
        val sf = TtsProviderConfig(id = "sf", type = ProviderType.SILICONFLOW, name = "SiliconFlow", speed = 1.25f)
        // SiliconFlow: API speed handles speed on server
        assertFalse(sf.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(sf.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testDoubaoSpeedPolicy() {
        val doubao = TtsProviderConfig(id = "doubao", type = ProviderType.DOUBAO, name = "Doubao", speed = 1.3f)
        // Doubao: API audio.speed_ratio handles speed on server
        assertFalse(doubao.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(doubao.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testGeminiSpeedPolicy() {
        val gemini = TtsProviderConfig(id = "gemini", type = ProviderType.GEMINI, name = "Gemini", speed = 1.5f)
        // Gemini: Transformer Attention handles speed natively via Prompt
        assertFalse(gemini.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(gemini.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testOfflineVitsSpeedPolicy() {
        val offline = TtsProviderConfig(id = "offline", type = ProviderType.OFFLINE_VITS, name = "Sherpa", speed = 1.5f)
        // Offline VITS: C++ JNI handles speed natively
        assertFalse(offline.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(offline.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testMimoSpeedPolicy() {
        val mimo = TtsProviderConfig(id = "mimo", type = ProviderType.MIMO, name = "MiMo", speed = 1.5f)
        // MiMo: Non-streaming Prompt handles speed (no Sonic); Streaming SSE clock is fixed (requires Sonic)
        assertFalse(mimo.requiresClientSpeedScaling(isStreaming = false))
        assertTrue(mimo.requiresClientSpeedScaling(isStreaming = true))
    }

    @Test
    fun testCustomHttpSpeedPolicy() {
        // Custom with ${speed}: server handles speed
        val customWithSpeed = TtsProviderConfig(
            id = "c1",
            type = ProviderType.CUSTOM_HTTP,
            name = "Custom1",
            speed = 1.5f,
            customPayloadTemplate = "{\"speed\": \${speed}}"
        )
        assertFalse(customWithSpeed.requiresClientSpeedScaling(isStreaming = false))
        assertFalse(customWithSpeed.requiresClientSpeedScaling(isStreaming = true))

        // Custom without ${speed}: client Sonic handles speed
        val customWithoutSpeed = TtsProviderConfig(
            id = "c2",
            type = ProviderType.CUSTOM_HTTP,
            name = "Custom2",
            speed = 1.5f,
            customPayloadTemplate = "{\"text\": \"\${text}\"}"
        )
        assertTrue(customWithoutSpeed.requiresClientSpeedScaling(isStreaming = false))
        assertTrue(customWithoutSpeed.requiresClientSpeedScaling(isStreaming = true))
    }
}
