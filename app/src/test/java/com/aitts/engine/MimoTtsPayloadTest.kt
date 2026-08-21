package com.aitts.engine

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ProviderType
import com.aitts.engine.provider.MimoTtsProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MimoTtsPayloadTest {

    @Test
    fun testMimoPayloadStructure() {
        val providers = PresetConfigs.createDefaultProviders()
        val mimoConfig = providers.first { it.type == ProviderType.MIMO }
        assertEquals("mimo-v2.5-tts", mimoConfig.modelName)
        assertEquals("https://api.xiaomimimo.com/v1/chat/completions", mimoConfig.baseUrl)

        // 模拟小米 MiMo 返回的 JSON
        val mockResponseJson = """
            {
              "id": "chatcmpl-test",
              "object": "chat.completion",
              "created": 1740000000,
              "model": "mimo-v2.5-tts",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "",
                    "audio": {
                      "id": "audio-test",
                      "expires_at": 1740003600,
                      "data": "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA//tQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                    }
                  },
                  "finish_reason": "stop"
                }
              ]
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val root = json.decodeFromString<JsonObject>(mockResponseJson)
        val choices = root["choices"]?.jsonArray
        assertNotNull(choices)
        val message = choices!![0].jsonObject["message"]?.jsonObject
        val audioData = message?.get("audio")?.jsonObject?.get("data")?.jsonPrimitive?.content
        assertNotNull(audioData)
        assertEquals(true, audioData!!.length > 20)
    }
}
