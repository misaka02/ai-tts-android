package com.aitts.engine.provider

import android.util.Base64
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
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
 * 小米 MiMo 官方语音大模型接入 (基于 MiMo-V2.5-TTS 系列)
 * 官方标准预置音色清单: 茉莉, 冰糖, 苏打, 白桦, mimo_default, Mia, Chloe, Milo, Dean
 * 支持「导演模式 (Director Mode)」：通过 user 角色的提示词动态控制语速、音调、情绪与朗读风格
 */
class MimoTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> = withContext(Dispatchers.IO) {
        val officialVoices = listOf(
            VoiceModel("茉莉", "茉莉 (女声·清澈甜美·官方推荐)", "Female", "zh-CN", "中文女声，清澈温柔自然，适合小说与长文朗读"),
            VoiceModel("冰糖", "冰糖 (女声·灵动活泼)", "Female", "zh-CN", "中文女声，灵动明亮，充满少年感"),
            VoiceModel("苏打", "苏打 (男声·清爽少年)", "Male", "zh-CN", "中文男声，清爽阳光，朝气蓬勃"),
            VoiceModel("白桦", "白桦 (男声·沉稳磁性)", "Male", "zh-CN", "中文男声，浑厚沉稳，适合玄幻/历史/旁白"),
            VoiceModel("mimo_default", "MiMo-默认 (官方基准音色)", "Female", "zh-CN", "官方集群自适应默认音色"),
            VoiceModel("Mia", "Mia (English Female)", "Female", "en-US", "Standard Natural English Female"),
            VoiceModel("Chloe", "Chloe (English Female)", "Female", "en-US", "Expressive English Female"),
            VoiceModel("Milo", "Milo (English Male)", "Male", "en-US", "Youthful English Male"),
            VoiceModel("Dean", "Dean (English Male)", "Male", "en-US", "Deep Resonant English Male")
        )

        if (config.apiKey.isNotBlank()) {
            try {
                val apiKey = config.apiKey.trim()
                val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trim() else "https://api.xiaomimimo.com/v1"
                val cleanUrl = if (baseUrl.endsWith("/chat/completions")) baseUrl.substringBefore("/chat/completions") else baseUrl
                val modelsUrl = if (cleanUrl.endsWith("/")) "${cleanUrl}models" else "$cleanUrl/models"
                val req = Request.Builder()
                    .url(modelsUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("api-key", apiKey)
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = json.decodeFromString<JsonObject>(body)
                    val data = root["data"]?.jsonArray
                    if (data != null && data.isNotEmpty()) {
                        // 成功在线获取官方响应
                        return@withContext officialVoices
                    }
                }
            } catch (e: Exception) {
                // 忽略异常，降级到内置音色
            }
        }

        officialVoices
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = withContext(Dispatchers.IO) {
        val staticModels = listOf("mimo-v2.5-tts", "mimo-v2-tts", "mimo-v1-tts")
        if (config.apiKey.isNotBlank()) {
            try {
                val apiKey = config.apiKey.trim()
                val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trim() else "https://api.xiaomimimo.com/v1"
                val cleanUrl = if (baseUrl.endsWith("/chat/completions")) baseUrl.substringBefore("/chat/completions") else baseUrl
                val modelsUrl = if (cleanUrl.endsWith("/")) "${cleanUrl}models" else "$cleanUrl/models"
                val req = Request.Builder()
                    .url(modelsUrl)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("api-key", apiKey)
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = json.decodeFromString<JsonObject>(body)
                    val data = root["data"]?.jsonArray
                    if (data != null && data.isNotEmpty()) {
                        val models = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                            .filter { it.contains("mimo", ignoreCase = true) || it.contains("tts", ignoreCase = true) }
                        if (models.isNotEmpty()) return@withContext models
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        staticModels
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写小米 MiMo API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.xiaomimimo.com/v1/chat/completions"
            } else if (!url.endsWith("/chat/completions") && !url.contains("/tts")) {
                url = if (url.endsWith("/")) "${url}chat/completions" else "$url/chat/completions"
            }

            val modelName = config.modelName.ifBlank { "mimo-v2.5-tts" }
            val voiceName = config.voiceId.ifBlank { "茉莉" }
            val isStreaming = config.isStreamingEnabled
            val formatStr = if (isStreaming) "pcm16" else (if (config.audioFormat.contains("wav")) "wav" else "mp3")

            val directorInstruction = buildDirectorPrompt(config)

            val payload = buildJsonObject {
                put("model", modelName)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", directorInstruction)
                    })
                    add(buildJsonObject {
                        put("role", "assistant")
                        put("content", text)
                    })
                })
                put("audio", buildJsonObject {
                    put("format", formatStr)
                    put("voice", voiceName)
                    put("speed", config.speed)
                })
                put("stream", isStreaming)
            }.toString()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", if (isStreaming) "text/event-stream, application/json" else "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("api-key", config.apiKey.trim())

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body
                ?: return@withContext Result.failure(IOException("MiMo API 返回空响应体"))

            if (!response.isSuccessful) {
                val errStr = responseBody.string()
                return@withContext Result.failure(
                    IOException("小米 MiMo 请求失败 HTTP ${response.code}: $errStr")
                )
            }

            if (isStreaming) {
                // 流式模式：逐行解析 SSE 实时 PCM16 裸流，无二次编解码损失，彻底根除电音与断续
                val audioOutputStream = java.io.ByteArrayOutputStream()
                var isSseStreamDetected = false

                responseBody.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
                    for (line in reader.lineSequence()) {
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                        if (trimmed == "data: [DONE]" || trimmed == "[DONE]") {
                            isSseStreamDetected = true
                            break
                        }

                        if (trimmed.startsWith("data:")) {
                            isSseStreamDetected = true
                            val jsonStr = trimmed.substring(5).trim()
                            if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                                try {
                                    val chunkObj = json.decodeFromString<JsonObject>(jsonStr)
                                    val choices = chunkObj["choices"]?.jsonArray
                                    if (choices != null && choices.isNotEmpty()) {
                                        val delta = choices[0].jsonObject["delta"]?.jsonObject
                                        val audioObj = delta?.get("audio")?.jsonObject
                                        val b64Data = audioObj?.get("data")?.jsonPrimitive?.content
                                        if (!b64Data.isNullOrBlank()) {
                                            val chunkBytes = Base64.decode(b64Data, Base64.DEFAULT)
                                            audioOutputStream.write(chunkBytes)
                                        }
                                    }
                                } catch (e: Exception) {
                                    // 忽略单帧解析警告
                                }
                            }
                        }
                    }
                }

                val collectedBytes = audioOutputStream.toByteArray()
                if (isSseStreamDetected && collectedBytes.isNotEmpty()) {
                    return@withContext Result.success(collectedBytes)
                }
            } else {
                // 非流式模式：直接解析完整 JSON 响应包
                val bodyString = responseBody.string()
                if (bodyString.isNotBlank()) {
                    try {
                        val root = json.decodeFromString<JsonObject>(bodyString)

                        if (root.containsKey("error")) {
                            val errMsg = root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                                ?: root["error"]?.toString() ?: "未知错误"
                            return@withContext Result.failure(IOException("MiMo API 返回错误: $errMsg"))
                        }

                        val choices = root["choices"]?.jsonArray
                        if (choices != null && choices.isNotEmpty()) {
                            val firstChoice = choices[0].jsonObject
                            val message = firstChoice["message"]?.jsonObject
                            val audioObj = message?.get("audio")?.jsonObject
                            val audioData = audioObj?.get("data")?.jsonPrimitive?.content

                            if (!audioData.isNullOrBlank()) {
                                val decodedBytes = Base64.decode(audioData, Base64.DEFAULT)
                                return@withContext Result.success(decodedBytes)
                            }
                        }

                        val directData = root["data"]?.jsonPrimitive?.content
                            ?: root["audio"]?.jsonPrimitive?.content
                        if (!directData.isNullOrBlank()) {
                            return@withContext Result.success(Base64.decode(directData, Base64.DEFAULT))
                        }
                    } catch (e: Exception) {
                        // ignore and fallback
                    }
                }
            }

            Result.failure(IOException("未能从 MiMo API 解析到有效音频流或数据"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (!config.isStreamingEnabled) {
            val fullResult = synthesize(text, config)
            if (fullResult.isSuccess) {
                val fullBytes = fullResult.getOrNull() ?: ByteArray(0)
                if (fullBytes.isNotEmpty()) {
                    onAudioChunk(fullBytes)
                }
            }
            return@withContext fullResult
        }

        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写小米 MiMo API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.xiaomimimo.com/v1/chat/completions"
            } else if (!url.endsWith("/chat/completions") && !url.contains("/tts")) {
                url = if (url.endsWith("/")) "${url}chat/completions" else "$url/chat/completions"
            }

            val modelName = config.modelName.ifBlank { "mimo-v2.5-tts" }
            val voiceName = config.voiceId.ifBlank { "茉莉" }
            val directorInstruction = buildDirectorPrompt(config)

            val payload = buildJsonObject {
                put("model", modelName)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", directorInstruction)
                    })
                    add(buildJsonObject {
                        put("role", "assistant")
                        put("content", text)
                    })
                })
                put("audio", buildJsonObject {
                    put("format", "pcm16")
                    put("voice", voiceName)
                    put("speed", config.speed)
                })
                put("stream", true)
            }.toString()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream, application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("api-key", config.apiKey.trim())

            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body
                ?: return@withContext Result.failure(IOException("MiMo API 返回空响应体"))

            if (!response.isSuccessful) {
                val errStr = responseBody.string()
                return@withContext Result.failure(
                    IOException("小米 MiMo 请求失败 HTTP ${response.code}: $errStr")
                )
            }

            val audioOutputStream = java.io.ByteArrayOutputStream()
            var isSseStreamDetected = false

            responseBody.byteStream().bufferedReader(Charsets.UTF_8).use { reader ->
                for (line in reader.lineSequence()) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                    if (trimmed == "data: [DONE]" || trimmed == "[DONE]") {
                        isSseStreamDetected = true
                        break
                    }

                    if (trimmed.startsWith("data:")) {
                        isSseStreamDetected = true
                        val jsonStr = trimmed.substring(5).trim()
                        if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                            try {
                                val chunkObj = json.decodeFromString<JsonObject>(jsonStr)
                                val choices = chunkObj["choices"]?.jsonArray
                                if (choices != null && choices.isNotEmpty()) {
                                    val delta = choices[0].jsonObject["delta"]?.jsonObject
                                    val audioObj = delta?.get("audio")?.jsonObject
                                    val b64Data = audioObj?.get("data")?.jsonPrimitive?.content
                                    if (!b64Data.isNullOrBlank()) {
                                        val chunkBytes = Base64.decode(b64Data, Base64.DEFAULT)
                                        if (chunkBytes.isNotEmpty()) {
                                            audioOutputStream.write(chunkBytes)
                                            onAudioChunk(chunkBytes)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // 忽略单帧异常
                            }
                        }
                    }
                }
            }

            val collectedBytes = audioOutputStream.toByteArray()
            if (isSseStreamDetected && collectedBytes.isNotEmpty()) {
                Result.success(collectedBytes)
            } else {
                Result.failure(IOException("MiMo 流式未接收到有效音频分块"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 构建小米 MiMo 导演模式指令
     * 遵循小米官方 MiMo-V2.5-TTS 预训练指令规范，直接生成强指令提示词
     */
    private fun buildDirectorPrompt(config: TtsProviderConfig): String {
        val userPrompt = config.promptInstruction.trim()
        val speed = config.speed
        val pitch = config.pitch

        val speedClause = when {
            speed <= 0.65f -> "请用极慢的语速，极其缓慢、一字一顿地朗读这段文字"
            speed <= 0.85f -> "请用缓慢、从容舒缓的语调朗读这段文字"
            speed in 0.86f..1.14f -> "请用标准自然、流畅的语速朗读这段文字"
            speed <= 1.35f -> "请用较快的语速朗读，保持紧凑轻快的节奏"
            speed <= 1.75f -> "请用快速的语速朗读，极速流畅，明显加快发音速度"
            else -> "请用极快的超快语速朗读，非常急促快速，大幅度加快发音速度"
        }

        val pitchClause = when {
            pitch <= 0.85f -> "声音偏低沉浑厚，富含磁性"
            pitch >= 1.15f -> "声音偏清脆明亮，高昂悦耳"
            else -> ""
        }

        val clauses = mutableListOf<String>()
        clauses.add(speedClause)
        if (pitchClause.isNotBlank()) clauses.add(pitchClause)
        if (userPrompt.isNotBlank()) clauses.add(userPrompt)

        return clauses.joinToString("，") + "。"
    }
}
