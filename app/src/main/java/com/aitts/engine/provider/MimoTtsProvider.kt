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

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = listOf(
        "mimo-v2.5-tts",
        "mimo-v2-tts",
        "mimo-v1-tts"
    )

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
            val formatStr = if (config.audioFormat.contains("wav")) "wav" else if (config.audioFormat.contains("pcm")) "pcm16" else "mp3"

            // 依据官方导演模式规范，在 user role 中根据提示词、语速、音调动态组装指导指令
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
                })
                put("stream", false)
            }.toString()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .addHeader("api-key", config.apiKey.trim())

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)
            val bodyString = String(bodyBytes, Charsets.UTF_8)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("小米 MiMo 请求失败 HTTP ${response.code}: $bodyString")
                )
            }

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
                    val decodedBytes = Base64.decode(directData, Base64.DEFAULT)
                    return@withContext Result.success(decodedBytes)
                }

                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    return@withContext Result.success(bodyBytes)
                }

                Result.failure(IOException("MiMo 响应中未找到 choices[0].message.audio.data 音频数据: $bodyString"))
            } catch (e: Exception) {
                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    Result.success(bodyBytes)
                } else {
                    Result.failure(IOException("解析 MiMo 音频数据失败: ${e.message}, 原始响应: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 构建小米 MiMo 导演模式指令
     * 将滑动条设定的语速、音调以及用户自定义的情感/场景提示词综合编译为高质量自然语言导演指令
     */
    private fun buildDirectorPrompt(config: TtsProviderConfig): String {
        val userPrompt = config.promptInstruction.trim()
        val speed = config.speed
        val pitch = config.pitch

        val instructions = mutableListOf<String>()

        if (userPrompt.isNotBlank()) {
            instructions.add(userPrompt)
        }

        // 语速自然语言映射
        if (speed <= 0.7f) {
            instructions.add("语速极慢，从容徐缓，字正腔圆")
        } else if (speed <= 0.85f) {
            instructions.add("语速稍慢，沉稳从容")
        } else if (speed >= 1.35f) {
            instructions.add("语速较快，紧凑流畅")
        } else if (speed >= 1.15f) {
            instructions.add("语速稍快，轻快生动")
        }

        // 音调自然语言映射
        if (pitch <= 0.85f) {
            instructions.add("音调偏低沉浑厚，带磁性")
        } else if (pitch >= 1.15f) {
            instructions.add("音调偏清脆明亮，高昂悦耳")
        }

        return if (instructions.isEmpty()) {
            "请用自然生动的语气朗读以下内容"
        } else {
            "请按照以下导演要求朗读：${instructions.joinToString("，")}。"
        }
    }
}
