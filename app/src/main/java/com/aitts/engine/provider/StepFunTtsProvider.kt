package com.aitts.engine.provider

import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            VoiceModel("zhengqiqingnian", "阶跃·正气青年", "Male", "zh-CN", "充满力量感青年音")
        )
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
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
