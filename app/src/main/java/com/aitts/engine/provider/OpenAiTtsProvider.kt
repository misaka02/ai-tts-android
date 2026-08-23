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
 * OpenAI / GPT-4o 兼容标准格式 TTS 接入实现
 */
class OpenAiTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

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

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
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
}
