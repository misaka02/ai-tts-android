package com.aitts.engine

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.provider.EdgeTtsProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeTtsLiveTest {

    @Test
    fun testLiveEdgeTtsSynthesis() = runBlocking {
        val provider = EdgeTtsProvider()
        val config = PresetConfigs.createDefaultProviders().first { it.id == "edge_tts_default" }

        val result = provider.synthesize("你好，这是一段测试语音。", config)
        println("Edge TTS live test result: isSuccess=${result.isSuccess}, size=${result.getOrNull()?.size}, error=${result.exceptionOrNull()?.message}")
        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)
        assertTrue((result.getOrNull()?.size ?: 0) > 1000)
    }
}
