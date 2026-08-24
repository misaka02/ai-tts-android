package com.aitts.engine.provider

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * 微软 Edge TTS 官方最新逆向协议实现
 * 免 API Key，支持全量微软神经网络大模型音色 (晓晓、云希等) 及动态音色库抓取
 */
class EdgeTtsProvider(
    customClient: OkHttpClient? = null
) : TtsProvider {

    private val client: OkHttpClient get() = com.aitts.engine.network.SharedHttpClient.instance

    private val trustedClientToken = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private val winEpoch = 11644473600.0
    private val secMsGecVersion = "1-143.0.3650.75"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> = withContext(Dispatchers.IO) {
        val staticVoices = PresetConfigs.edgeVoices

        // 尝试从微软官方在线拉取最新的全部 300+ 多国音色列表
        try {
            val voiceListUrl = "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/voices/list?trustedclienttoken=$trustedClientToken"
            val req = Request.Builder()
                .url(voiceListUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val bodyStr = resp.body?.string() ?: ""
                val array = json.decodeFromString<JsonObject>("{\"voices\":$bodyStr}")["voices"]?.jsonArray
                if (array != null && array.isNotEmpty()) {
                    val onlineVoices = mutableListOf<VoiceModel>()
                    for (elem in array) {
                        val obj = elem.jsonObject
                        val shortName = obj["ShortName"]?.jsonPrimitive?.content ?: continue
                        val friendlyName = obj["FriendlyName"]?.jsonPrimitive?.content ?: shortName
                        val gender = obj["Gender"]?.jsonPrimitive?.content ?: "Female"
                        val locale = obj["Locale"]?.jsonPrimitive?.content ?: "zh-CN"

                        onlineVoices.add(
                            VoiceModel(
                                id = shortName,
                                name = friendlyName.replace("Microsoft ", "").replace(" Online (Natural) - ", " (").replace(" - ", " (") + if (!friendlyName.endsWith(")")) ")" else "",
                                gender = gender,
                                locale = locale,
                                description = "$locale · $gender"
                            )
                        )
                    }

                    if (onlineVoices.isNotEmpty()) {
                        // 优先排前中文
                        val zhVoices = onlineVoices.filter { it.locale.startsWith("zh") }
                        val otherVoices = onlineVoices.filter { !it.locale.startsWith("zh") }
                        return@withContext zhVoices + otherVoices
                    }
                }
            }
        } catch (e: Exception) {
            // fallback to static
        }

        staticVoices
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = listOf(
        "edge-neural",
        "edge-v1"
    )

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val deferred = CompletableDeferred<ByteArray>()
        val audioStream = ByteArrayOutputStream()

        val ratePercent = ((config.speed - 1.0f) * 100).toInt()
        val pitchPercent = ((config.pitch - 1.0f) * 100).toInt()
        val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
        val pitchStr = if (pitchPercent >= 0) "+$pitchPercent%" else "$pitchPercent%"
        val volumeStr = "+0%"

        val voiceName = formatEdgeVoiceName(config.voiceId.ifBlank { "zh-CN-XiaoxiaoNeural" })
        val timestamp = getFormattedDate()
        val connectionId = UUID.randomUUID().toString().replace("-", "")
        val secMsGec = generateSecMsGec()
        val muid = UUID.randomUUID().toString().replace("-", "").uppercase()

        val wssUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=$trustedClientToken" +
                "&ConnectionId=$connectionId" +
                "&Sec-MS-GEC=$secMsGec" +
                "&Sec-MS-GEC-Version=$secMsGecVersion"

        val request = Request.Builder()
            .url(wssUrl)
            .addHeader("Pragma", "no-cache")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
            .addHeader("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .addHeader("Accept-Encoding", "gzip, deflate, br, zstd")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("Cookie", "muid=$muid;")
            .build()

        var webSocket: WebSocket? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                // 1. 发送 speech.config
                val configMsg = "X-Timestamp:$timestamp\r\n" +
                        "Content-Type:application/json; charset=utf-8\r\n" +
                        "Path:speech.config\r\n\r\n" +
                        "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n"
                ws.send(configMsg)

                val lang = when {
                    voiceName.contains("zh-CN") -> "zh-CN"
                    voiceName.contains("zh-TW") -> "zh-TW"
                    voiceName.contains("zh-HK") -> "zh-HK"
                    voiceName.contains("ja-JP") -> "ja-JP"
                    voiceName.contains("en-US") -> "en-US"
                    voiceName.contains("en-GB") -> "en-GB"
                    else -> "zh-CN"
                }

                // 2. 发送 SSML
                val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$lang'>" +
                        "<voice name='$voiceName'>" +
                        "<prosody pitch='$pitchStr' rate='$rateStr' volume='$volumeStr'>" +
                        escapeXml(text) +
                        "</prosody></voice></speak>"

                val ssmlMsg = "X-RequestId:$requestId\r\n" +
                        "Content-Type:application/ssml+xml\r\n" +
                        "X-Timestamp:${timestamp}Z\r\n" +
                        "Path:ssml\r\n\r\n" +
                        ssml
                ws.send(ssmlMsg)
            }

            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                if (data.size < 2) return

                val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                if (data.size > headerLen + 2) {
                    val headerStr = String(data, 2, headerLen, Charsets.UTF_8)
                    if (headerStr.contains("Path:audio")) {
                        val payloadOffset = 2 + headerLen
                        val payloadLength = data.size - payloadOffset
                        audioStream.write(data, payloadOffset, payloadLength)
                    }
                }
            }

            override fun onMessage(ws: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    ws.close(1000, "Normal Closure")
                    deferred.complete(audioStream.toByteArray())
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                deferred.completeExceptionally(t)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (!deferred.isCompleted) {
                    deferred.complete(audioStream.toByteArray())
                }
            }
        }

        webSocket = client.newWebSocket(request, listener)

        try {
            val audioBytes = withTimeoutOrNull(25000L) {
                deferred.await()
            }
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                Result.success(audioBytes)
            } else {
                Result.failure(IOException("Edge TTS 合成超时或未接收到音频数据"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                webSocket.cancel()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun formatEdgeVoiceName(voiceId: String): String {
        if (voiceId.startsWith("Microsoft Server Speech")) return voiceId
        val regex = Pattern.compile("^([a-z]{2,})-([A-Z]{2,})-(.+Neural)$")
        val matcher = regex.matcher(voiceId)
        if (matcher.find()) {
            val lang = matcher.group(1) ?: "zh"
            var region = matcher.group(2) ?: "CN"
            var name = matcher.group(3) ?: "XiaoxiaoNeural"
            if (name.contains("-")) {
                val subRegion = name.substringBefore("-")
                name = name.substringAfter("-")
                region = "$region-$subRegion"
            }
            return "Microsoft Server Speech Text to Speech Voice ($lang-$region, $name)"
        }
        return "Microsoft Server Speech Text to Speech Voice (zh-CN, XiaoxiaoNeural)"
    }

    private fun escapeXml(input: String): String {
        val sanitized = input.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
        return sanitized.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun generateSecMsGec(): String {
        val unixTimestamp = System.currentTimeMillis() / 1000.0
        var ticks = unixTimestamp + winEpoch
        ticks -= ticks % 300.0
        ticks *= 10000000.0
        val strToHash = "${ticks.toLong()}$trustedClientToken"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(strToHash.toByteArray(Charsets.US_ASCII))
            hash.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            "3A37BE09B0E11DB2F9082C5E7AEF9B3D3B51CE3D96CD1F64E3FB8C3C78B95A2F"
        }
    }
}
