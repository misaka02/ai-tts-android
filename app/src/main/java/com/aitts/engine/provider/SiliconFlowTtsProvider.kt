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
 * 硅基流动 SiliconFlow (CosyVoice / ChatTTS) 接入实现
 * 包含官方全量音色与在线动态列表拉取
 */
class SiliconFlowTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> = withContext(Dispatchers.IO) {
        val staticVoices = listOf(
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:alex", "CosyVoice2·Alex (沉稳男声)", "Male", "zh-CN", "沉稳清晰，小说阅读推荐"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:anna", "CosyVoice2·Anna (亲切女声)", "Female", "zh-CN", "自然亲切，适合长篇阅读"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:bella", "CosyVoice2·Bella (活泼女声)", "Female", "zh-CN", "灵动活泼少女感"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:benjamin", "CosyVoice2·Benjamin (磁性男声)", "Male", "zh-CN", "磁性男声，玄幻修仙"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:charles", "CosyVoice2·Charles (沉着男声)", "Male", "zh-CN", "沉着成熟"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:claire", "CosyVoice2·Claire (优雅女声)", "Female", "zh-CN", "优雅知性"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:david", "CosyVoice2·David (阳光男声)", "Male", "zh-CN", "朝气清朗"),
            VoiceModel("FunAudioLLM/CosyVoice2-0.5B:diana", "CosyVoice2·Diana (温柔女声)", "Female", "zh-CN", "温柔温婉"),
            VoiceModel("FunAudioLLM/CosyVoice-300M:alex", "CosyVoice·Alex (男声)", "Male", "zh-CN", "经典 300M 男声"),
            VoiceModel("FunAudioLLM/CosyVoice-300M:anna", "CosyVoice·Anna (女声)", "Female", "zh-CN", "经典 300M 女声"),
            VoiceModel("2Noise/ChatTTS:default", "ChatTTS·自然对话", "Neutral", "zh-CN", "极强对话感与笑声韵律")
        )

        if (config.apiKey.isBlank()) {
            return@withContext staticVoices
        }

        try {
            val req = Request.Builder()
                .url("https://api.siliconflow.cn/v1/models?sub_type=audio")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val root = json.decodeFromString<JsonObject>(bodyStr)
                val data = root["data"]?.jsonArray
                if (data != null && data.isNotEmpty()) {
                    val dynamicList = mutableListOf<VoiceModel>()
                    for (item in data) {
                        val modelId = item.jsonObject["id"]?.jsonPrimitive?.content ?: continue
                        if (modelId.contains("CosyVoice") || modelId.contains("TTS")) {
                            dynamicList.add(
                                VoiceModel(
                                    id = "$modelId:alex",
                                    name = "$modelId (Alex)",
                                    gender = "Male",
                                    locale = "zh-CN",
                                    description = "动态获取的硅基流动官方模型"
                                )
                            )
                        }
                    }
                    if (dynamicList.isNotEmpty()) {
                        return@withContext dynamicList + staticVoices
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }

        staticVoices
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = withContext(Dispatchers.IO) {
        val staticModels = listOf(
            "FunAudioLLM/CosyVoice2-0.5B",
            "FunAudioLLM/CosyVoice-300M",
            "2Noise/ChatTTS",
            "IndexTeam/IndexTTS-2"
        )
        if (config.apiKey.isBlank()) return@withContext staticModels
        try {
            val req = Request.Builder()
                .url("https://api.siliconflow.cn/v1/models?sub_type=audio")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val root = json.decodeFromString<JsonObject>(body)
                val data = root["data"]?.jsonArray
                if (data != null && data.isNotEmpty()) {
                    val list = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        staticModels
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = synthesize(text, config, "")

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig,
        sessionId: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写硅基流动 API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.siliconflow.cn/v1/audio/speech"
            }

            val model = config.modelName.ifBlank { "FunAudioLLM/CosyVoice2-0.5B" }
            val voice = config.voiceId.ifBlank { "FunAudioLLM/CosyVoice2-0.5B:alex" }

            val payload = buildJsonObject {
                put("model", model)
                put("input", text)
                put("voice", voice)
                put("response_format", if (config.audioFormat.contains("wav")) "wav" else "mp3")
                put("speed", config.speed)
            }.toString()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
            if (!sessionId.isNullOrBlank()) {
                requestBuilder.tag(sessionId)
                requestBuilder.tag(String::class.java, sessionId)
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)

            if (!response.isSuccessful) {
                val err = String(bodyBytes, Charsets.UTF_8)
                return@withContext Result.failure(IOException("硅基流动请求失败 HTTP ${response.code}: $err"))
            }

            if (bodyBytes.isEmpty()) {
                return@withContext Result.failure(IOException("硅基流动返回音频为空"))
            }

            Result.success(bodyBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> = synthesizeStreaming(text, config, "", onAudioChunk)

    override suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        sessionId: String,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写硅基流动 API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.siliconflow.cn/v1/audio/speech"
            }

            val model = config.modelName.ifBlank { "FunAudioLLM/CosyVoice2-0.5B" }
            val voice = config.voiceId.ifBlank { "FunAudioLLM/CosyVoice2-0.5B:alex" }

            val payload = buildJsonObject {
                put("model", model)
                put("input", text)
                put("voice", voice)
                put("response_format", "pcm")
                put("speed", config.speed)
            }.toString()

            val configDataStore = try {
                com.aitts.engine.data.ConfigDataStore.getInstance(com.aitts.engine.AiTtsApp.instance)
            } catch (e: Throwable) {
                null
            }
            val startReqTime = System.currentTimeMillis()
            configDataStore?.logStructured(
                level = com.aitts.engine.data.LogLevel.INFO,
                tag = "SILICON_FLOW",
                title = "发起 HTTP 分块流式推流",
                details = "模型=$model, 音色=$voice, 长度=${text.length}字, 语速=${config.speed}x",
                sessionId = sessionId
            )

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
            if (!sessionId.isNullOrBlank()) {
                requestBuilder.tag(sessionId)
                requestBuilder.tag(String::class.java, sessionId)
            }
            val request = requestBuilder.build()

            val response = client.newCall(request).execute()
            val responseBody = response.body
                ?: return@withContext Result.failure(IOException("硅基流动返回空响应体"))

            if (!response.isSuccessful) {
                val errStr = responseBody.string()
                configDataStore?.logStructured(
                    level = com.aitts.engine.data.LogLevel.ERROR,
                    tag = "SILICON_FLOW",
                    title = "流式 HTTP 异常 (${response.code})",
                    details = errStr.take(200)
                )
                return@withContext Result.failure(
                    IOException("硅基流动请求失败 HTTP ${response.code}: $errStr")
                )
            }

            val audioOutputStream = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var firstChunk = true
            var bytesRead: Int

            responseBody.byteStream().use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (bytesRead > 0) {
                        if (firstChunk) {
                            firstChunk = false
                            val latency = System.currentTimeMillis() - startReqTime
                            configDataStore?.logStructured(
                                level = com.aitts.engine.data.LogLevel.METRIC,
                                tag = "SILICON_FLOW",
                                title = "流式首包已就绪",
                                details = "TTFB=${latency}ms, 正在推流..."
                            )
                        }
                        audioOutputStream.write(buffer, 0, bytesRead)
                        onAudioChunk(buffer.copyOf(bytesRead))
                    }
                }
            }

            val collectedBytes = audioOutputStream.toByteArray()
            val totalTime = System.currentTimeMillis() - startReqTime
            configDataStore?.logStructured(
                level = com.aitts.engine.data.LogLevel.SUCCESS,
                tag = "SILICON_FLOW",
                title = "流式推流完成",
                details = "累计推送 ${collectedBytes.size} 字节, 耗时 ${totalTime}ms"
            )
            Result.success(collectedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
