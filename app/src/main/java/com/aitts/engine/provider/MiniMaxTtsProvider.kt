package com.aitts.engine.provider

import android.util.Base64
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
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
 * MiniMax (海螺语音) 官方最新 T2A_V2 接口实现
 * 包含官方全量 16+ 角色音色库
 */
class MiniMaxTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        return listOf(
            VoiceModel("male-qn-qingse", "青涩青年 (男声·自然清爽)", "Male", "zh-CN", "自然少年感，适合现代文"),
            VoiceModel("male-qn-jingying", "精英青年 (男声·自信沉稳)", "Male", "zh-CN", "沉稳磁性，适合旁白叙述"),
            VoiceModel("male-qn-badao", "霸道总裁 (男声·气场全开)", "Male", "zh-CN", "低沉有气势，适合都市爽文"),
            VoiceModel("male-qn-daxuesheng", "青年大学生 (男声·阳光活力)", "Male", "zh-CN", "朝气蓬勃，充满青春感"),
            VoiceModel("female-shaonv", "甜美少女 (女声·灵动活泼)", "Female", "zh-CN", "灵动少女音，感情充沛"),
            VoiceModel("female-yujie", "知性御姐 (女声·优雅成熟)", "Female", "zh-CN", "优雅知性，适合都市情感"),
            VoiceModel("female-chengshu", "成熟女性 (女声·温婉端庄)", "Female", "zh-CN", "端庄温和，适合文学故事"),
            VoiceModel("female-tianmei", "甜美女性 (女声·温暖亲切)", "Female", "zh-CN", "温暖甜美，娓娓道来"),
            VoiceModel("presenter_male", "专业男主播 (沉浸叙述)", "Male", "zh-CN", "影视旁白级质感"),
            VoiceModel("presenter_female", "专业女主播 (端庄大气)", "Female", "zh-CN", "标准电台广播腔"),
            VoiceModel("audiobook_male_1", "有声书男声·沉稳讲述", "Male", "zh-CN", "长篇小说专属男声"),
            VoiceModel("audiobook_male_2", "有声书男声·激昂对白", "Male", "zh-CN", "战斗激昂对白表现力强"),
            VoiceModel("audiobook_female_1", "有声书女声·温婉叙述", "Female", "zh-CN", "温婉从容，耐听不累"),
            VoiceModel("audiobook_female_2", "有声书女声·剧情生动", "Female", "zh-CN", "角色对白情绪充沛"),
            VoiceModel("santa_claus", "圣诞老人 (趣味低沉)", "Male", "zh-CN", "浑厚慈祥老年音"),
            VoiceModel("mini_yachun", "牙尖女孩 (特色方言)", "Female", "zh-CN", "生动幽默特色音色")
        )
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = listOf(
        "speech-02-turbo",
        "speech-02",
        "speech-01-turbo",
        "speech-01-hd"
    )

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写 MiniMax API Key"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://api.minimax.chat/v1/t2a_v2"
            }

            val modelName = config.modelName.ifBlank { "speech-02-turbo" }
            val voiceId = config.voiceId.ifBlank { "male-qn-qingse" }

            val payload = buildJsonObject {
                put("model", modelName)
                put("text", text)
                put("stream", false)
                put("voice_setting", buildJsonObject {
                    put("voice_id", voiceId)
                    put("speed", config.speed)
                    put("vol", 1.0)
                    val pitchVal = ((config.pitch - 1.0f) * 12).toInt().coerceIn(-12, 12)
                    put("pitch", pitchVal)
                })
                put("audio_setting", buildJsonObject {
                    put("sample_rate", if (config.sampleRate > 0) config.sampleRate else 32000)
                    put("bitrate", 128000)
                    put("format", config.audioFormat.ifBlank { "mp3" })
                })
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
                .build()

            val response = client.newCall(request).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)
            val bodyString = String(bodyBytes, Charsets.UTF_8)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("MiniMax 请求失败 HTTP ${response.code}: $bodyString")
                )
            }

            try {
                val root = json.decodeFromString<JsonObject>(bodyString)

                val baseResp = root["base_resp"]?.jsonObject
                val statusCode = baseResp?.get("status_code")?.jsonPrimitive?.int ?: 0
                val statusMsg = baseResp?.get("status_msg")?.jsonPrimitive?.content ?: "OK"

                if (statusCode != 0) {
                    return@withContext Result.failure(
                        IOException("MiniMax 接口返回错误 ($statusCode): $statusMsg")
                    )
                }

                val dataObj = root["data"]?.jsonObject
                val audioDataStr = dataObj?.get("audio")?.jsonPrimitive?.content
                    ?: root["audio"]?.jsonPrimitive?.content

                if (!audioDataStr.isNullOrBlank()) {
                    val audioBytes = if (isHex(audioDataStr)) {
                        hexStringToByteArray(audioDataStr)
                    } else {
                        Base64.decode(audioDataStr, Base64.DEFAULT)
                    }
                    return@withContext Result.success(audioBytes)
                }

                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    return@withContext Result.success(bodyBytes)
                }

                Result.failure(IOException("MiniMax 响应中未找到音频载荷: $bodyString"))
            } catch (e: Exception) {
                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    Result.success(bodyBytes)
                } else {
                    Result.failure(IOException("MiniMax 音频解析失败: ${e.message}, 响应: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isHex(s: String): Boolean {
        if (s.length % 2 != 0) return false
        val sample = s.take(100)
        return sample.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
