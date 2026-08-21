package com.aitts.engine

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ProviderType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GeminiTtsPayloadTest {

    @Test
    fun testGeminiPresetConfig() {
        val providers = PresetConfigs.createDefaultProviders()
        val geminiConfig = providers.first { it.type == ProviderType.GEMINI }
        assertEquals("gemini-2.5-flash-preview-tts", geminiConfig.modelName)
        assertEquals("Puck", geminiConfig.voiceId)
        assertEquals("https://generativelanguage.googleapis.com/v1beta", geminiConfig.baseUrl)
    }

    @Test
    fun testGeminiResponseParsing() {
        val mockJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "inlineData": {
                          "mimeType": "audio/x-pcm;rate=24000",
                          "data": "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA//tQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                        }
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }
        val root = json.decodeFromString<JsonObject>(mockJson)
        val candidates = root["candidates"]?.jsonArray
        assertNotNull(candidates)
        val parts = candidates!![0].jsonObject["content"]?.jsonObject?.get("parts")?.jsonArray
        assertNotNull(parts)
        val inlineData = parts!![0].jsonObject["inlineData"]?.jsonObject
        val base64Data = inlineData?.get("data")?.jsonPrimitive?.content
        val mimeType = inlineData?.get("mimeType")?.jsonPrimitive?.content

        assertEquals("audio/x-pcm;rate=24000", mimeType)
        assertNotNull(base64Data)
    }
}
