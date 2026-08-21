package com.aitts.engine

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ProviderType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MimoAndProviderTest {

    @Test
    fun testDefaultProvidersContainAllMajorModels() {
        val providers = PresetConfigs.createDefaultProviders()
        val types = providers.map { it.type }.toSet()

        assertTrue(types.contains(ProviderType.MIMO))
        assertTrue(types.contains(ProviderType.MINIMAX))
        assertTrue(types.contains(ProviderType.DOUBAO))
        assertTrue(types.contains(ProviderType.EDGE_TTS))
        assertTrue(types.contains(ProviderType.SILICONFLOW))
        assertTrue(types.contains(ProviderType.FISH_AUDIO))
        assertTrue(types.contains(ProviderType.STEPFUN))
        assertTrue(types.contains(ProviderType.OPENAI))
        assertTrue(types.contains(ProviderType.AZURE))
        assertTrue(types.contains(ProviderType.CUSTOM_HTTP))
    }

    @Test
    fun testEdgeVoicesNotEmpty() {
        val voices = PresetConfigs.edgeVoices
        assertTrue(voices.isNotEmpty())
        assertNotNull(voices.find { it.id == "zh-CN-XiaoxiaoNeural" })
        assertNotNull(voices.find { it.id == "zh-CN-YunxiNeural" })
    }
}
