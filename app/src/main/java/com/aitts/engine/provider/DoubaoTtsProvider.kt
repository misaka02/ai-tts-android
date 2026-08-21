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
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 字节跳动 火山引擎 / 豆包语音大模型 HTTP API 接入
 * 包含官方 BigTTS 全量主播与角色音色库
 */
class DoubaoTtsProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) : TtsProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> {
        return listOf(
            VoiceModel("zh_female_shuangkuaisisi_moon_bigtts", "爽快思思 (豆包经典·女声)", "Female", "zh-CN", "欢快自然，情感饱满，豆包招牌女声"),
            VoiceModel("zh_male_cancan_moon_bigtts", "灿灿主播 (豆包男声·沉浸叙事)", "Male", "zh-CN", "磁性男声，适合长篇小说叙述"),
            VoiceModel("zh_female_tianmeixiaoxuan_moon_bigtts", "甜美小萱 (温柔知性女声)", "Female", "zh-CN", "温柔知性，听书极佳"),
            VoiceModel("zh_male_yangyang_moon_bigtts", "阳光洋洋 (活力开朗男声)", "Male", "zh-CN", "朝气清朗，现代文首选"),
            VoiceModel("zh_female_zhixingdajie_moon_bigtts", "知性大姐 (成熟温和)", "Female", "zh-CN", "温和大度，适合纪实与名著"),
            VoiceModel("zh_male_wenheyuedu_moon_bigtts", "温和阅读 (沉稳讲述男声)", "Male", "zh-CN", "节奏舒缓，长篇不累"),
            VoiceModel("zh_female_xiaoxue_moon_bigtts", "清新小雪 (轻快脱俗女声)", "Female", "zh-CN", "轻盈灵动，适合青春文学"),
            VoiceModel("zh_male_chunhou_moon_bigtts", "醇厚大叔 (浑厚故事男声)", "Male", "zh-CN", "厚重沧桑，适合武侠历史"),
            VoiceModel("zh_female_meilidianshi_moon_bigtts", "魅力电视 (电台女主播)", "Female", "zh-CN", "标准电台广播腔"),
            VoiceModel("zh_female_peiyin_moon_bigtts", "影视配音 (剧情对白女声)", "Female", "zh-CN", "角色情感起伏生动"),
            VoiceModel("BV700_streaming", "玄幻旁白 (气势磅礴男声)", "Male", "zh-CN", "大场面玄幻有声书专用"),
            VoiceModel("BV001_streaming", "标准女声 (通用自然)", "Female", "zh-CN", "火山通用基准女声"),
            VoiceModel("BV002_streaming", "标准男声 (清晰自然)", "Male", "zh-CN", "火山通用基准男声")
        )
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (config.apiKey.isBlank()) {
                return@withContext Result.failure(IOException("请先在「模型」界面填写火山引擎 AccessToken"))
            }

            var url = config.baseUrl.trim()
            if (url.isBlank()) {
                url = "https://openspeech.bytedance.com/api/v1/tts"
            }

            var appId = "8421060938"
            var cluster = "volcano_tts"

            try {
                if (config.customHeadersJson.isNotBlank() && config.customHeadersJson.startsWith("{")) {
                    val customObj = json.decodeFromString<JsonObject>(config.customHeadersJson)
                    customObj["appid"]?.jsonPrimitive?.content?.let { appId = it }
                    customObj["app_id"]?.jsonPrimitive?.content?.let { appId = it }
                    customObj["cluster"]?.jsonPrimitive?.content?.let { cluster = it }
                }
            } catch (e: Exception) {
                // ignore
            }

            val voiceType = config.voiceId.ifBlank { "zh_female_shuangkuaisisi_moon_bigtts" }
            val reqId = UUID.randomUUID().toString()

            val payload = buildJsonObject {
                put("app", buildJsonObject {
                    put("appid", appId)
                    put("token", config.apiKey.trim())
                    put("cluster", cluster)
                })
                put("user", buildJsonObject {
                    put("uid", "ai_tts_android_user")
                })
                put("audio", buildJsonObject {
                    put("voice_type", voiceType)
                    put("encoding", "mp3")
                    put("speed_ratio", config.speed)
                    put("volume_ratio", 1.0)
                    put("pitch_ratio", config.pitch)
                })
                put("request", buildJsonObject {
                    put("reqid", reqId)
                    put("text", text)
                    put("text_type", "plain")
                    put("operation", "query")
                })
            }.toString()

            val authHeader = if (config.apiKey.startsWith("Bearer;")) {
                config.apiKey.trim()
            } else {
                "Bearer;${config.apiKey.trim()}"
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", authHeader)
                .build()

            val response = client.newCall(request).execute()
            val bodyBytes = response.body?.bytes() ?: ByteArray(0)
            val bodyString = String(bodyBytes, Charsets.UTF_8)

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("火山引擎豆包请求失败 HTTP ${response.code}: $bodyString")
                )
            }

            try {
                val root = json.decodeFromString<JsonObject>(bodyString)
                val code = root["code"]?.jsonPrimitive?.int ?: 0
                val message = root["message"]?.jsonPrimitive?.content ?: "OK"

                if (code != 3000 && code != 0) {
                    return@withContext Result.failure(
                        IOException("火山豆包 API 错误 ($code): $message (请检查 AppID 与 AccessToken)")
                    )
                }

                val audioBase64 = root["data"]?.jsonPrimitive?.content
                if (!audioBase64.isNullOrBlank()) {
                    val decodedBytes = Base64.decode(audioBase64, Base64.DEFAULT)
                    return@withContext Result.success(decodedBytes)
                }

                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    return@withContext Result.success(bodyBytes)
                }

                Result.failure(IOException("火山豆包返回中未找到 data 音频: $bodyString"))
            } catch (e: Exception) {
                if (bodyBytes.isNotEmpty() && !bodyString.startsWith("{")) {
                    Result.success(bodyBytes)
                } else {
                    Result.failure(IOException("火山豆包数据解析失败: ${e.message}, 响应: $bodyString"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
