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
 * Google Gemini 原生 TTS 官方大模型接入实现
 * 依据 Google 官方最新文档规范:
 * 1. 官方 TTS 模型: gemini-2.5-flash-preview-tts, gemini-3.1-flash-tts-preview, gemini-2.5-pro-preview-tts, gemini-2.5-flash
 * 2. 官方标准端点: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}
 * 3. 鉴权 Header: x-goog-api-key: {apiKey}
 * 4. 模态配置: generationConfig.responseModalities = ["AUDIO"]
 * 5. 音色配置: speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName = "Puck"
 */
class GeminiTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance
    override val supportsNativePcmStreaming: Boolean = true
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> = withContext(Dispatchers.IO) {
        val staticVoices = listOf(
            VoiceModel("Puck", "Puck (男声·活力幽默·官方推荐)", "Male", "all", "欢快生动，充满活力与幽默感"),
            VoiceModel("Charon", "Charon (男声·深沉稳重)", "Male", "all", "沉稳有信息量，适合知识讲解与小说"),
            VoiceModel("Kore", "Kore (女声·知性温和)", "Female", "all", "温和坚定，长篇朗读首选"),
            VoiceModel("Fenrir", "Fenrir (男声·激情威严)", "Male", "all", "高亢热烈，适合战斗与激昂场景"),
            VoiceModel("Aoede", "Aoede (女声·甜美清脆)", "Female", "all", "轻快悦耳微风般甜美"),
            VoiceModel("Leda", "Leda (女声·优雅成熟)", "Female", "all", "端庄优雅成熟女声"),
            VoiceModel("Orus", "Orus (男声·自信阳光)", "Male", "all", "朝气清朗阳光少年音"),
            VoiceModel("Zephyr", "Zephyr (女声·轻柔温婉)", "Female", "all", "轻柔舒缓，治愈放松"),
            VoiceModel("Callirhoe", "Callirhoe (女声·清晰灵动)", "Female", "all", "清晰自然现代女声"),
            VoiceModel("Autonoe", "Autonoe (女声·自然亲和)", "Female", "all", "亲切自然，富有亲和力"),
            VoiceModel("Enceladus", "Enceladus (男声·浑厚低沉)", "Male", "all", "浑厚低沉男低音"),
            VoiceModel("Iapetus", "Iapetus (男声·磁性叙事)", "Male", "all", "磁性讲述，适合有声书"),
            VoiceModel("Umbriel", "Umbriel (男声·沉稳克制)", "Male", "all", "克制沉稳冷峻音色"),
            VoiceModel("Algieba", "Algieba (女声·明亮欢快)", "Female", "all", "明亮阳光少女音"),
            VoiceModel("Despina", "Despina (女声·柔和甜美)", "Female", "all", "柔和细腻女声"),
            VoiceModel("Erinome", "Erinome (女声·温婉端庄)", "Female", "all", "温婉贤淑端庄"),
            VoiceModel("Algenib", "Algenib (男声·青年朝气)", "Male", "all", "充满朝气的青年音"),
            VoiceModel("Rasalgethi", "Rasalgethi (男声·成熟稳重)", "Male", "all", "成熟有说服力男声"),
            VoiceModel("Laomedeia", "Laomedeia (女声·优雅清丽)", "Female", "all", "清丽脱俗"),
            VoiceModel("Achernar", "Achernar (男声·清亮自然)", "Male", "all", "清亮纯粹"),
            VoiceModel("Alnilam", "Alnilam (男声·深沉有力)", "Male", "all", "深沉有力男中音"),
            VoiceModel("Schedar", "Schedar (女声·知性从容)", "Female", "all", "从容温和"),
            VoiceModel("Gacrux", "Gacrux (男声·温和亲切)", "Male", "all", "温和亲切男声"),
            VoiceModel("Pulcherrima", "Pulcherrima (女声·动人甜美)", "Female", "all", "动人甜美"),
            VoiceModel("Achird", "Achird (男声·清爽少年)", "Male", "all", "清爽自然"),
            VoiceModel("Zubenelgenubi", "Zubenelgenubi (男声·厚重史诗)", "Male", "all", "厚重宏大史诗感"),
            VoiceModel("Vindemiatrix", "Vindemiatrix (女声·端庄严肃)", "Female", "all", "端庄严谨"),
            VoiceModel("Sadachbia", "Sadachbia (男声·沉着冷静)", "Male", "all", "沉着冷静"),
            VoiceModel("Sadaltager", "Sadaltager (男声·阳光活力)", "Male", "all", "阳光开朗"),
            VoiceModel("Sulafat", "Sulafat (女声·轻快脱俗)", "Female", "all", "轻快灵巧女声")
        )

        // 尝试从 Google 官方 API 动态探测在线可用模型
        if (config.apiKey.isNotBlank()) {
            try {
                val apiKey = config.apiKey.trim()
                val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                val req = Request.Builder().url(url).addHeader("x-goog-api-key", apiKey).build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = json.decodeFromString<JsonObject>(body)
                    val models = root["models"]?.jsonArray
                    if (models != null && models.isNotEmpty()) {
                        // 成功在线验证模型列表，返回官方全量音色
                        return@withContext staticVoices
                    }
                }
            } catch (e: Exception) {
                // 忽略网络探测异常，优雅降级到内置音色
            }
        }

        staticVoices
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = listOf(
        "gemini-2.5-flash-preview-tts",
        "gemini-2.5-pro-preview-tts",
        "gemini-2.5-flash",
        "gemini-2.0-flash-exp"
    )

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
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写 Google Gemini API Key"))
            }

            val modelName = config.modelName.ifBlank { "gemini-2.5-flash-preview-tts" }
            val voiceName = config.voiceId.ifBlank { "Puck" }

            var baseUrl = config.baseUrl.trim()
            if (baseUrl.isBlank()) {
                baseUrl = "https://generativelanguage.googleapis.com/v1beta"
            }
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.dropLast(1)
            }

            val targetUrl = when {
                baseUrl.contains("generateContent") -> baseUrl
                baseUrl.contains("/models/") -> "$baseUrl:generateContent?key=$apiKey"
                else -> "$baseUrl/models/$modelName:generateContent?key=$apiKey"
            }

            val promptPrefix = buildGeminiPrompt(config)
            val fullPromptText = if (promptPrefix.isNotBlank()) {
                "$promptPrefix\n\n$text"
            } else {
                text
            }

            val payload = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", fullPromptText)
                            })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("responseModalities", buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("AUDIO"))
                    })
                    put("speechConfig", buildJsonObject {
                        put("voiceConfig", buildJsonObject {
                            put("prebuiltVoiceConfig", buildJsonObject {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }.toString()

            val reqBuilder = Request.Builder()
                .url(targetUrl)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("x-goog-api-key", apiKey)
            if (sessionId.isNotBlank()) {
                reqBuilder.tag(sessionId)
            }
            val request = reqBuilder.build()

            val response = client.newCall(request).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)
            val bodyString = String(bodyBytes, Charsets.UTF_8)

            if (!response.isSuccessful) {
                try {
                    val root = json.decodeFromString<JsonObject>(bodyString)
                    val errorObj = root["error"]?.jsonObject
                    val message = errorObj?.get("message")?.jsonPrimitive?.content ?: bodyString
                    return@withContext Result.failure(
                        IOException("Gemini API 请求失败 HTTP ${response.code}: $message")
                    )
                } catch (e: Exception) {
                    return@withContext Result.failure(
                        IOException("Gemini 请求失败 HTTP ${response.code}: $bodyString")
                    )
                }
            }

            try {
                val root = json.decodeFromString<JsonObject>(bodyString)

                if (root.containsKey("error")) {
                    val errMsg = root["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        ?: root["error"]?.toString() ?: "未知错误"
                    return@withContext Result.failure(IOException("Gemini API 返回错误: $errMsg"))
                }

                val candidates = root["candidates"]?.jsonArray
                if (candidates != null && candidates.isNotEmpty()) {
                    val firstCandidate = candidates[0].jsonObject
                    val contentObj = firstCandidate["content"]?.jsonObject
                    val parts = contentObj?.get("parts")?.jsonArray
                    if (parts != null && parts.isNotEmpty()) {
                        for (partElem in parts) {
                            val partObj = partElem.jsonObject
                            val inlineData = partObj["inlineData"]?.jsonObject ?: partObj["inline_data"]?.jsonObject
                            if (inlineData != null) {
                                val mimeType = inlineData["mimeType"]?.jsonPrimitive?.content ?: inlineData["mime_type"]?.jsonPrimitive?.content ?: ""
                                val base64Data = inlineData["data"]?.jsonPrimitive?.content ?: ""

                                if (base64Data.isNotBlank()) {
                                    val audioRaw = Base64.decode(base64Data, Base64.DEFAULT)

                                    return@withContext if (isPcm(mimeType, audioRaw)) {
                                        val sampleRate = extractSampleRate(mimeType, config.sampleRate.coerceAtLeast(24000))
                                        Result.success(wrapPcmToWav(audioRaw, sampleRate = sampleRate))
                                    } else {
                                        Result.success(audioRaw)
                                    }
                                }
                            }

                            // 如果模型输出了文本而不是音频
                            val textReply = partObj["text"]?.jsonPrimitive?.content
                            if (!textReply.isNullOrBlank()) {
                                return@withContext Result.failure(
                                    IOException("Gemini 模型未生成音频，返回了文本提示: $textReply")
                                )
                            }
                        }
                    }
                }

                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    return@withContext Result.success(bodyBytes)
                }

                Result.failure(IOException("Gemini 响应中未找到 candidates[0].content.parts[].inlineData 音频载荷: $bodyString"))
            } catch (e: Exception) {
                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    Result.success(bodyBytes)
                } else {
                    Result.failure(IOException("解析 Gemini 音频数据失败: ${e.message}, 原始响应: $bodyString"))
                }
            }
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
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写 Google Gemini API Key"))
            }

            var baseUrl = config.baseUrl.trim()
            if (baseUrl.isBlank()) {
                baseUrl = "https://generativelanguage.googleapis.com/v1beta"
            }
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.dropLast(1)
            }

            val modelName = config.modelName.ifBlank { "gemini-2.5-flash-preview-tts" }
            val voiceName = config.voiceId.ifBlank { "Puck" }

            val targetUrl = when {
                baseUrl.contains("streamGenerateContent") -> if (baseUrl.contains("alt=sse")) baseUrl else "$baseUrl&alt=sse"
                baseUrl.contains("generateContent") -> baseUrl.replace("generateContent", "streamGenerateContent") + (if (baseUrl.contains("?")) "&alt=sse" else "?alt=sse")
                baseUrl.contains("/models/") -> "$baseUrl:streamGenerateContent?alt=sse&key=$apiKey"
                else -> "$baseUrl/models/$modelName:streamGenerateContent?alt=sse&key=$apiKey"
            }

            val promptPrefix = buildGeminiPrompt(config)
            val fullPromptText = if (promptPrefix.isNotBlank()) {
                "$promptPrefix\n\n$text"
            } else {
                text
            }

            val payload = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject {
                                put("text", fullPromptText)
                            })
                        })
                    })
                })
                put("generationConfig", buildJsonObject {
                    put("responseModalities", buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("AUDIO"))
                    })
                    put("speechConfig", buildJsonObject {
                        put("voiceConfig", buildJsonObject {
                            put("prebuiltVoiceConfig", buildJsonObject {
                                put("voiceName", voiceName)
                            })
                        })
                    })
                })
            }.toString()

            val configDataStore = try {
                com.aitts.engine.data.ConfigDataStore.getInstance(com.aitts.engine.AiTtsApp.instance)
            } catch (e: Throwable) {
                null
            }
            val startReqTime = System.currentTimeMillis()
            configDataStore?.logStructured(
                level = com.aitts.engine.data.LogLevel.INFO,
                tag = "GEMINI",
                title = "发起实时 SSE 流式推流",
                details = "模型=$modelName, 音色=$voiceName, 长度=${text.length}字",
                sessionId = sessionId
            )

            val reqBuilder = Request.Builder()
                .url(targetUrl)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .addHeader("x-goog-api-key", apiKey)
            if (sessionId.isNotBlank()) {
                reqBuilder.tag(sessionId)
            }
            val request = reqBuilder.build()

            val response = client.newCall(request).execute()
            val responseBody = response.body
                ?: return@withContext Result.failure(IOException("Gemini 流式返回空响应体"))

            if (!response.isSuccessful) {
                val errStr = responseBody.string()
                configDataStore?.logStructured(
                    level = com.aitts.engine.data.LogLevel.ERROR,
                    tag = "GEMINI",
                    title = "流式 HTTP 异常 (${response.code})",
                    details = errStr.take(200),
                    sessionId = sessionId
                )
                return@withContext Result.failure(
                    IOException("Gemini 流式请求失败 HTTP ${response.code}: $errStr")
                )
            }

            val audioOutputStream = java.io.ByteArrayOutputStream()
            var isSseStreamDetected = false
            var firstChunkReceived = false

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
                                val candidates = chunkObj["candidates"]?.jsonArray
                                if (candidates != null && candidates.isNotEmpty()) {
                                    val firstCandidate = candidates[0].jsonObject
                                    val contentObj = firstCandidate["content"]?.jsonObject
                                    val parts = contentObj?.get("parts")?.jsonArray
                                    if (parts != null && parts.isNotEmpty()) {
                                        for (partElem in parts) {
                                            val partObj = partElem.jsonObject
                                            val inlineData = partObj["inlineData"]?.jsonObject ?: partObj["inline_data"]?.jsonObject
                                            val b64Data = inlineData?.get("data")?.jsonPrimitive?.content
                                            if (!b64Data.isNullOrBlank()) {
                                                val pcmBytes = Base64.decode(b64Data, Base64.DEFAULT)
                                                if (pcmBytes.isNotEmpty()) {
                                                    if (!firstChunkReceived) {
                                                        firstChunkReceived = true
                                                        val latency = System.currentTimeMillis() - startReqTime
                                                        configDataStore?.logStructured(
                                                            level = com.aitts.engine.data.LogLevel.METRIC,
                                                            tag = "GEMINI",
                                                            title = "流式首包已就绪",
                                                            details = "TTFB=${latency}ms, 正在推流...",
                                                            sessionId = sessionId
                                                        )
                                                    }
                                                    audioOutputStream.write(pcmBytes)
                                                    onAudioChunk(pcmBytes)
                                                }
                                            }
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
                val totalTime = System.currentTimeMillis() - startReqTime
                configDataStore?.logStructured(
                    level = com.aitts.engine.data.LogLevel.SUCCESS,
                    tag = "GEMINI",
                    title = "流式传输完成",
                    details = "累计获取 ${collectedBytes.size} 字节, 耗时 ${totalTime}ms",
                    sessionId = sessionId
                )
                Result.success(collectedBytes)
            } else {
                // 仅当从未推送过任何流式 chunk 时才允许全量兜底，防止音频重叠与鬼畜
                if (!firstChunkReceived) {
                    val fallbackRes = synthesize(text, config, sessionId)
                    val fullBytes = fallbackRes.getOrNull()
                    if (fullBytes != null && fullBytes.isNotEmpty()) {
                        val purePcm = com.aitts.engine.audio.AudioDecoder.decodeToPcm(fullBytes, config.sampleRate).pcmData
                        onAudioChunk(purePcm)
                        Result.success(purePcm)
                    } else {
                        fallbackRes
                    }
                } else {
                    Result.failure(IOException("Gemini 流式推流意外中断，已终止避免音频重叠"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildGeminiPrompt(config: TtsProviderConfig): String {
        val userPrompt = config.promptInstruction.trim()
        val speed = config.speed
        val pitch = config.pitch

        val instructions = mutableListOf<String>()
        if (userPrompt.isNotBlank()) {
            instructions.add(userPrompt)
        }

        val speedInstruction = getSpeedInstruction(speed)
        instructions.add(speedInstruction)

        if (pitch <= 0.85f) {
            instructions.add("Use a lower, deeper tone of voice")
        } else if (pitch >= 1.15f) {
            instructions.add("Use a higher, brighter tone of voice")
        }

        return if (instructions.isNotEmpty()) {
            "Instruction: ${instructions.joinToString(", ")}."
        } else {
            ""
        }
    }

    companion object {
        fun getSpeedInstruction(speed: Float): String = when {
            speed <= 0.65f -> "Speak at an extremely slow, calm and gentle pace with prolonged, soothing syllables."
            speed <= 0.80f -> "Speak at a slower, measured and steady pace with a composed tone."
            speed <= 0.95f -> "Speak at a slightly relaxed and unhurried pace, smooth and natural."
            speed <= 1.10f -> "Speak at a standard, natural and fluent reading pace with moderate cadence."
            speed <= 1.30f -> "Speak at a slightly brisk and lively pace, crisp and energetic."
            speed <= 1.60f -> "Speak at a fast, tight and continuous pace with prompt and decisive delivery."
            else -> "Speak at a very rapid, fluent and agile pace, flowing seamlessly without hesitation."
        }
    }

    private fun isPcm(mimeType: String, data: ByteArray): Boolean {
        if (mimeType.contains("pcm", ignoreCase = true)) return true
        if (data.size >= 4) {
            if (data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() &&
                data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte()) {
                return false
            }
            if (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) {
                return false
            }
            if ((data[0].toInt() and 0xFF) == 0xFF && (data[1].toInt() and 0xE0) == 0xE0) {
                return false
            }
        }
        return true
    }

    private fun extractSampleRate(mimeType: String, fallback: Int): Int {
        val rateRegex = Regex("rate=(\\d+)")
        val match = rateRegex.find(mimeType)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: fallback
    }

    private fun wrapPcmToWav(pcmData: ByteArray, sampleRate: Int = 24000, channels: Int = 1): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * 2
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0

        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        val dataLen = pcmData.size
        header[40] = (dataLen and 0xff).toByte()
        header[41] = ((dataLen shr 8) and 0xff).toByte()
        header[42] = ((dataLen shr 16) and 0xff).toByte()
        header[43] = ((dataLen shr 24) and 0xff).toByte()

        return header + pcmData
    }
}
