package com.aitts.engine.provider

import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fish Audio (鱼音) 官方 REST 接口实现
 * 官方端点: POST https://api.fish.audio/v1/tts
 * 支持在线通过 API Key 动态拉取精选和自建克隆音色
 */
class FishAudioTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> = withContext(Dispatchers.IO) {
        val staticVoices = listOf(
            VoiceModel("7f92f8afb8ec43bf81429cc1c9199cb1", "Fish·官方甜美女声", "Female", "zh-CN", "自然甜美声音"),
            VoiceModel("54a58406560946ea8a70477e682fb10d", "Fish·磁性男声旁白", "Male", "zh-CN", "适合解说与有声书"),
            VoiceModel("e58b0d70a3974b249bc840a3679e82f1", "Fish·清澈少年音", "Male", "zh-CN", "干净纯粹少年感")
        )

        if (config.apiKey.isBlank()) {
            return@withContext staticVoices
        }

        // 尝试从 Fish Audio 在线查询用户的音色库
        try {
            val req = Request.Builder()
                .url("https://api.fish.audio/model?page_size=30")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val root = json.decodeFromString<JsonObject>(bodyStr)
                val items = root["items"]?.jsonArray
                if (items != null && items.isNotEmpty()) {
                    val dynamicList = mutableListOf<VoiceModel>()
                    for (item in items) {
                        val obj = item.jsonObject
                        val id = obj["_id"]?.jsonPrimitive?.content ?: continue
                        val title = obj["title"]?.jsonPrimitive?.content ?: id
                        val desc = obj["description"]?.jsonPrimitive?.content ?: ""

                        dynamicList.add(
                            VoiceModel(
                                id = id,
                                name = title,
                                gender = "Neutral",
                                locale = "zh-CN",
                                description = desc.ifBlank { "Fish Audio 官方/自建模型" }
                            )
                        )
                    }
                    if (dynamicList.isNotEmpty()) {
                        return@withContext dynamicList
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        staticVoices
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写 Fish Audio API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.fish.audio/v1/tts"
            }

            val voiceId = config.voiceId.ifBlank { "7f92f8afb8ec43bf81429cc1c9199cb1" }

            val payload = buildJsonObject {
                put("text", text)
                put("reference_id", voiceId)
                put("format", if (config.audioFormat.contains("wav")) "wav" else "mp3")
                put("latency", "balanced")
                put("streaming", false)
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)

            if (!response.isSuccessful) {
                val err = String(bodyBytes, Charsets.UTF_8)
                return@withContext Result.failure(IOException("Fish Audio 失败 HTTP ${response.code}: $err"))
            }

            if (bodyBytes.isEmpty()) {
                return@withContext Result.failure(IOException("Fish Audio 返回数据为空"))
            }

            Result.success(bodyBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
