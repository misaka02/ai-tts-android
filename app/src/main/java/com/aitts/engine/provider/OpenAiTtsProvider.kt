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
 * OpenAI / GPT-4o 兼容标准格式 TTS 接入实现
 */
class OpenAiTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance
    override val supportsNativePcmStreaming: Boolean = true
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        return listOf(
            VoiceModel("nova", "Nova (女声·自然温暖)", "Female", "all", "OpenAI 推荐音色，富有亲和力"),
            VoiceModel("shimmer", "Shimmer (女声·清脆悦耳)", "Female", "all", "清脆明亮女声"),
            VoiceModel("alloy", "Alloy (中性·均衡通用)", "Neutral", "all", "中性均衡音色"),
            VoiceModel("echo", "Echo (男声·温和清晰)", "Male", "all", "温和沉稳男声"),
            VoiceModel("fable", "Fable (英音·富有叙事感)", "Neutral", "all", "故事与小说阅读"),
            VoiceModel("onyx", "Onyx (男声·低沉厚重)", "Male", "all", "深沉厚重男低音"),
            VoiceModel("coral", "Coral (女声·灵动知性·GPT-4o)", "Female", "all", "GPT-4o 旗舰灵动女声"),
            VoiceModel("sage", "Sage (女声·沉着温婉·GPT-4o)", "Female", "all", "GPT-4o 沉着温和女声"),
            VoiceModel("ash", "Ash (男声·清朗自然·GPT-4o)", "Male", "all", "GPT-4o 清朗少年音")
        )
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = withContext(Dispatchers.IO) {
        val staticModels = listOf("tts-1", "tts-1-hd", "gpt-4o-audio-preview")
        if (config.apiKey.isBlank()) return@withContext staticModels
        try {
            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.openai.com/v1/models"
            } else if (url.contains("/audio/speech")) {
                url = url.replace("/audio/speech", "/models")
            } else if (!url.endsWith("/models")) {
                url = if (url.endsWith("/")) "${url}models" else "$url/models"
            }
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: ""
                val root = json.decodeFromString<JsonObject>(body)
                val data = root["data"]?.jsonArray
                if (data != null && data.isNotEmpty()) {
                    val list = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                        .filter { it.contains("tts", ignoreCase = true) || it.contains("audio", ignoreCase = true) }
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
                return@withContext Result.failure(IOException("请先在「模型」界面填写 OpenAI / 中转 API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.openai.com/v1/audio/speech"
            } else if (!url.endsWith("/audio/speech") && !url.contains("/speech")) {
                url = if (url.endsWith("/")) "${url}audio/speech" else "$url/audio/speech"
            }

            val model = config.modelName.ifBlank { "tts-1" }
            val voice = config.voiceId.ifBlank { "nova" }

            val payload = buildJsonObject {
                put("model", model)
                put("input", text)
                put("voice", voice)
                put("response_format", if (config.audioFormat.contains("wav")) "wav" else "mp3")
                put("speed", config.speed)
            }.toString()

            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
            if (sessionId.isNotBlank()) {
                reqBuilder.tag(sessionId)
            }
            val request = reqBuilder.build()

            val response = client.newCall(request).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)

            if (!response.isSuccessful) {
                val err = String(bodyBytes, Charsets.UTF_8)
                return@withContext Result.failure(IOException("OpenAI TTS 失败 HTTP ${response.code}: $err"))
            }

            if (bodyBytes.isEmpty()) {
                return@withContext Result.failure(IOException("OpenAI TTS 返回数据为空"))
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
                return@withContext Result.failure(IOException("请先在「模型」界面填写 OpenAI / 中转 API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.openai.com/v1/audio/speech"
            } else if (!url.endsWith("/audio/speech") && !url.contains("/speech")) {
                url = if (url.endsWith("/")) "${url}audio/speech" else "$url/audio/speech"
            }

            val model = config.modelName.ifBlank { "tts-1" }
            val voice = config.voiceId.ifBlank { "nova" }

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
                tag = "OPENAI",
                title = "发起 HTTP 分块流式推流",
                details = "模型=$model, 音色=$voice, 长度=${text.length}字, 语速=${config.speed}x",
                sessionId = sessionId
            )

            val reqBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("Content-Type", "application/json")
            if (sessionId.isNotBlank()) {
                reqBuilder.tag(sessionId)
            }
            val request = reqBuilder.build()

            val response = client.newCall(request).execute()
            val responseBody = response.body
                ?: return@withContext Result.failure(IOException("OpenAI 返回空响应体"))

            if (!response.isSuccessful) {
                val errStr = responseBody.string()
                configDataStore?.logStructured(
                    level = com.aitts.engine.data.LogLevel.ERROR,
                    tag = "OPENAI",
                    title = "流式 HTTP 异常 (${response.code})",
                    details = errStr.take(200)
                )
                return@withContext Result.failure(
                    IOException("OpenAI 请求失败 HTTP ${response.code}: $errStr")
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
                                tag = "OPENAI",
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
                tag = "OPENAI",
                title = "流式推流完成",
                details = "累计推送 ${collectedBytes.size} 字节, 耗时 ${totalTime}ms"
            )
            Result.success(collectedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
