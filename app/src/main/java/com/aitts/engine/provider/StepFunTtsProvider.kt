package com.aitts.engine.provider

import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 阶跃星辰 StepFun (Step-Audio) 接入实现
 * 官方标准端点: POST https://api.stepfun.com/v1/audio/speech
 */
class StepFunTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        return listOf(
            VoiceModel("cixingnansheng", "阶跃·磁性男声", "Male", "zh-CN", "浑厚深沉男声，语境感知"),
            VoiceModel("wenrounvsheng", "阶跃·温柔女声", "Female", "zh-CN", "柔美温和女声"),
            VoiceModel("huopoxiaonv", "阶跃·活泼少女", "Female", "zh-CN", "青春甜美少女"),
            VoiceModel("chenwenyishu", "阶跃·沉稳大叔", "Male", "zh-CN", "磁性沉稳，适合播报"),
            VoiceModel("zhixingnvsheng", "阶跃·知性女声", "Female", "zh-CN", "干练优雅播报")
        )
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = withContext(Dispatchers.IO) {
        val staticModels = listOf("stepaudio-2.5-tts", "step-audio-t2a")
        if (config.apiKey.isNotBlank()) {
            try {
                val req = Request.Builder()
                    .url("https://api.stepfun.com/v1/models")
                    .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(body)
                    val data = root["data"]?.jsonArray
                    if (data != null && data.isNotEmpty()) {
                        val list = data.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.content }
                            .filter { it.contains("step", ignoreCase = true) || it.contains("audio", ignoreCase = true) || it.contains("tts", ignoreCase = true) }
                        if (list.isNotEmpty()) return@withContext list
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
    ): Result<ByteArray> = synthesize(text, config, "")

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig,
        sessionId: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写阶跃星辰 API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.stepfun.com/v1/audio/speech"
            }

            val model = config.modelName.ifBlank { "stepaudio-2.5-tts" }
            val voice = config.voiceId.ifBlank { "cixingnansheng" }

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
                return@withContext Result.failure(IOException("阶跃星辰请求失败 HTTP ${response.code}: $err"))
            }

            if (bodyBytes.isEmpty()) {
                return@withContext Result.failure(IOException("阶跃星辰返回音频为空"))
            }

            Result.success(bodyBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
