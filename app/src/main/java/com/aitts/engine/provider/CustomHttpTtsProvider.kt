package com.aitts.engine.provider

import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 通用自定义 HTTP 模板引擎 Provider
 * 支持任意私有部署或开源大模型（GPT-SoVITS, CosyVoice, F5-TTS, MeloTTS, VITS 等）
 */
class CustomHttpTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        return listOf(
            VoiceModel(config.voiceId.ifBlank { "default" }, "自定义节点默认音色 (${config.voiceId})", "Neutral", "zh-CN", "根据后端服务实际音色配置")
        )
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
            if (config.baseUrl.isBlank()) {
                return@withContext Result.failure(IOException("自定义 HTTP 服务 URL 不能为空"))
            }

            // 1. 替换占位符（URL 中进行严格 URL 编码，Body 中进行 JSON 转义）
            val processedUrl = replacePlaceholders(config.baseUrl, text, config, isUrl = true)
            val processedPayload = replacePlaceholders(config.customPayloadTemplate, text, config, isUrl = false)

            val requestBuilder = Request.Builder().url(processedUrl)

            // 2. 解析并添加自定义 Headers
            try {
                if (config.customHeadersJson.isNotBlank() && config.customHeadersJson.trim().startsWith("{")) {
                    val headerObj = json.decodeFromString<JsonObject>(config.customHeadersJson)
                    for ((k, v) in headerObj) {
                        val headerValue = replacePlaceholders(v.jsonPrimitive.content, text, config, isUrl = false)
                        requestBuilder.addHeader(k, headerValue)
                    }
                }
            } catch (e: Exception) {
                // 忽略非标准 header json
            }

            if (config.apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey}")
            }

            // 3. 构建请求 Body
            if (processedPayload.isNotBlank() && processedPayload.trim().startsWith("{")) {
                requestBuilder.post(processedPayload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                requestBuilder.addHeader("Content-Type", "application/json")
            } else {
                requestBuilder.get()
            }

            if (!sessionId.isNullOrBlank()) {
                requestBuilder.tag(sessionId)
                requestBuilder.tag(String::class.java, sessionId)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            response.use { resp ->
                val bodyBytes = resp.body?.bytes() ?: ByteArray(0)
                val contentType = resp.header("Content-Type") ?: ""

                if (!resp.isSuccessful) {
                    val err = String(bodyBytes, Charsets.UTF_8)
                    return@withContext Result.failure(IOException("自定义节点请求失败 HTTP ${resp.code}: $err"))
                }

                // 4. 检查是否为 JSON Base64 音频
                if (config.responseAudioPath.isNotBlank() || contentType.contains("application/json") || isJsonStart(bodyBytes)) {
                    try {
                        val root = json.decodeFromString<JsonObject>(String(bodyBytes, Charsets.UTF_8))
                        val pathParts = if (config.responseAudioPath.isNotBlank()) {
                            config.responseAudioPath.split(".")
                        } else {
                            listOf("audio", "data", "audio_base64")
                        }

                        var current: JsonObject? = root
                        var foundBase64: String? = null

                        for (part in pathParts) {
                            val elem = current?.get(part)
                            if (elem is JsonObject) {
                                current = elem
                            } else if (elem != null) {
                                foundBase64 = elem.jsonPrimitive.content
                                break
                            }
                        }

                        if (foundBase64 != null) {
                            val cleanBase64 = if (foundBase64.contains(",")) {
                                foundBase64.substringAfter(",")
                            } else {
                                foundBase64
                            }
                            val decoded = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                            return@withContext Result.success(decoded)
                        }
                    } catch (e: Exception) {
                        // fallthrough to raw bytes
                    }
                }

                if (bodyBytes.isEmpty()) {
                    return@withContext Result.failure(IOException("自定义节点未返回有效音频数据"))
                }

                Result.success(bodyBytes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun replacePlaceholders(template: String, text: String, config: TtsProviderConfig, isUrl: Boolean = false): String {
        val encodedText = if (isUrl) {
            try {
                URLEncoder.encode(text, "UTF-8")
            } catch (e: Exception) {
                escapeJson(text)
            }
        } else {
            escapeJson(text)
        }

        return template
            .replace("\${text}", encodedText)
            .replace("\${speed}", config.speed.toString())
            .replace("\${pitch}", config.pitch.toString())
            .replace("\${volume}", config.volume.toString())
            .replace("\${model}", config.modelName)
            .replace("\${voice}", config.voiceId)
            .replace("\${sample_rate}", config.sampleRate.toString())
            .replace("\${format}", config.audioFormat)
            .replace("\${apiKey}", config.apiKey)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun isJsonStart(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val first = bytes[0].toInt().toChar()
        return first == '{' || first == '['
    }
}
