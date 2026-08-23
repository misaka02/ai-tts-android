package com.aitts.engine.provider

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 微软 Azure 官方 Cognitive Services Speech REST API 接入实现
 */
class AzureTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        return PresetConfigs.edgeVoices
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val url = if (config.baseUrl.isNotBlank()) {
                config.baseUrl
            } else {
                "https://eastasia.tts.speech.microsoft.com/cognitiveservices/v1"
            }

            val voice = if (config.voiceId.isNotBlank()) config.voiceId else "zh-CN-XiaoxiaoNeural"
            val ratePercent = ((config.speed - 1.0f) * 100).toInt()
            val pitchPercent = ((config.pitch - 1.0f) * 100).toInt()
            val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
            val pitchStr = if (pitchPercent >= 0) "+$pitchPercent%" else "$pitchPercent%"

            val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>" +
                    "<voice name='$voice'>" +
                    "<prosody rate='$rateStr' pitch='$pitchStr'>" +
                    escapeXml(text) +
                    "</prosody></voice></speak>"

            val outputFormat = if (config.audioFormat.contains("wav")) {
                "riff-24khz-16bit-mono-pcm"
            } else {
                "audio-24khz-48kbitrate-mono-mp3"
            }

            val request = Request.Builder()
                .url(url)
                .post(ssml.toRequestBody("application/ssml+xml".toMediaType()))
                .addHeader("Ocp-Apim-Subscription-Key", config.apiKey)
                .addHeader("X-Microsoft-OutputFormat", outputFormat)
                .addHeader("User-Agent", "AiTtsEngineAndroid")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string() ?: ""
                return@withContext Result.failure(IOException("Azure Speech 失败 HTTP ${response.code}: $err"))
            }

            val bodyBytes = response.body?.bytes() ?: ByteArray(0)
            if (bodyBytes.isEmpty()) {
                return@withContext Result.failure(IOException("Azure Speech 返回数据为空"))
            }

            Result.success(bodyBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
