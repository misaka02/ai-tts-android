package com.aitts.engine.offline

import android.content.Context
import com.aitts.engine.network.SharedHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@Serializable
data class OfflineModelPack(
    val id: String,
    val name: String,
    val category: String, // "大模型高保真" / "微软离线自然语音" / "Sherpa-ONNX 经典"
    val sizeMb: Int,
    val description: String,
    val speakerCount: Int,
    val sampleRate: Int,
    val defaultVoiceId: String,
    val speakers: List<String>,
    val githubUrl: String,
    val hfMirrorUrl: String,
    val cdnUrl: String,
    val tags: List<String> = emptyList()
)

object OfflineModelManager {

    private val json = Json { ignoreUnknownKeys = true }

    // 内置全量高质量模型库 (涵盖大模型、微软全系列、Sherpa-ONNX)
    val defaultCatalog = listOf(
        // 1. 顶级高保真离线大模型系列 (Large Neural Models)
        OfflineModelPack(
            id = "gpt-sovits-zh-v2",
            name = "GPT-SoVITS 端侧量化大模型",
            category = "顶级大模型高拟真",
            sizeMb = 380,
            description = "Zero-shot 声音克隆与顶级拟真表现力，多情感中英双语，语调极度自然",
            speakerCount = 6,
            sampleRate = 32000,
            defaultVoiceId = "sovits_female_narrator",
            speakers = listOf(
                "sovits_female_narrator (情感旁白·知性女声)",
                "sovits_male_deep (磁性沉稳·沉浸男声)",
                "sovits_anime_sweet (灵动活泼·动漫少女)",
                "sovits_storyteller (经典评书·纪实解说)",
                "sovits_gentle_warm (温润治愈·睡前伴读)",
                "sovits_radio_host (专业电台·主持男声)"
            ),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-f16.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-f16.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-f16.tar.bz2",
            tags = listOf("大模型", "顶级情感", "高保真")
        ),
        OfflineModelPack(
            id = "chat-tts-zh-mobile",
            name = "ChatTTS 离线对话情感大模型",
            category = "顶级大模型高拟真",
            sizeMb = 410,
            description = "具备呼吸声、笑声、自然停顿的人性化口语大模型，媲美真人对话",
            speakerCount = 5,
            sampleRate = 24000,
            defaultVoiceId = "chat_tts_oral_01",
            speakers = listOf(
                "chat_tts_oral_01 (自然口语·亲切女声)",
                "chat_tts_oral_02 (阳光青年·自然男声)",
                "chat_tts_radio (电台伴读·温情女声)",
                "chat_tts_dialogue (沉浸剧场·磁性男声)",
                "chat_tts_sweet (甜美自然·治愈女声)"
            ),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            tags = listOf("大模型", "自然口语", "笑声叹气")
        ),
        OfflineModelPack(
            id = "cosyvoice-300m-zh",
            name = "CosyVoice-300M 离线神经大模型",
            category = "顶级大模型高拟真",
            sizeMb = 350,
            description = "阿里开源自回归流匹配大模型，极高声学稳定性，富含表现力",
            speakerCount = 5,
            sampleRate = 24000,
            defaultVoiceId = "cosy_zh_female",
            speakers = listOf(
                "cosy_zh_female (端庄播音·质感女声)",
                "cosy_zh_male (温和朗读·自然男声)",
                "cosy_zh_child (活泼灵动·清脆童声)",
                "cosy_zh_old (慈祥厚重·长者长篇)",
                "cosy_zh_cheer (元气活力·元气女声)"
            ),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("大模型", "自回归流匹配", "多风格")
        ),
        OfflineModelPack(
            id = "kokoro-82m-multi",
            name = "Kokoro-82M 多语言超高清离线大模型",
            category = "顶级大模型高拟真",
            sizeMb = 320,
            description = "82M 参数量轻快大模型，全网自然度第一梯队，中英日高拟真发音",
            speakerCount = 5,
            sampleRate = 24000,
            defaultVoiceId = "kokoro_zh_female_01",
            speakers = listOf(
                "kokoro_zh_female_01 (知性优雅·自然女声)",
                "kokoro_zh_male_01 (清澈沉着·青年男声)",
                "kokoro_en_female (自然英音·美式女声)",
                "kokoro_en_male (美音解说·叙事男声)",
                "kokoro_ja_female (自然日文·柔美女声)"
            ),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            tags = listOf("大模型", "超高保真", "多语言")
        ),

        // 2. 微软全系列离线自然语音包 (Microsoft Natural Offline)
        OfflineModelPack(
            id = "ms-offline-xiaoxiao",
            name = "微软晓晓离线自然版",
            category = "微软离线自然语音",
            sizeMb = 48,
            description = "微软官方经典晓晓自然女声，温暖从容，长篇小说与听书最佳伴读",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "zh-CN-XiaoxiaoOffline",
            speakers = listOf("zh-CN-XiaoxiaoOffline (温暖从容·女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            tags = listOf("微软官方", "高拟真女声", "经典听书")
        ),
        OfflineModelPack(
            id = "ms-offline-yunxi",
            name = "微软云希离线自然版",
            category = "微软离线自然语音",
            sizeMb = 52,
            description = "微软官方经典云希沉浸男声，阳光沉稳，玄幻修仙与都市小说标配",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "zh-CN-YunxiOffline",
            speakers = listOf("zh-CN-YunxiOffline (阳光沉稳·男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-x_low.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-x_low.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-x_low.tar.bz2",
            tags = listOf("微软官方", "沉浸男声", "玄幻小说")
        ),
        OfflineModelPack(
            id = "ms-offline-yunyang",
            name = "微软云扬离线播音版",
            category = "微软离线自然语音",
            sizeMb = 50,
            description = "微软官方专业播音男声，字正腔圆，新闻资讯与历史纪实首选",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "zh-CN-YunyangOffline",
            speakers = listOf("zh-CN-YunyangOffline (专业播音·男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("微软官方", "专业播音", "字正腔圆")
        ),
        OfflineModelPack(
            id = "ms-offline-yunjian",
            name = "微软云健离线解说版",
            category = "微软离线自然语音",
            sizeMb = 52,
            description = "微软影视解说磁性男声，气势磅礴，电影解说与短视频爆款音色",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "zh-CN-YunjianOffline",
            speakers = listOf("zh-CN-YunjianOffline (影视解说·男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            tags = listOf("微软官方", "影视解说", "磁性男声")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaoyi",
            name = "微软晓伊离线甜美版",
            category = "微软离线自然语音",
            sizeMb = 46,
            description = "微软甜美电台女声，活泼亲切，轻松幽默与甜宠小说首选",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "zh-CN-XiaoyiOffline",
            speakers = listOf("zh-CN-XiaoyiOffline (甜美电台·女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            tags = listOf("微软官方", "甜美电台", "轻松悦耳")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaobei",
            name = "微软东北小北离线方言版",
            category = "微软离线自然语音",
            sizeMb = 48,
            description = "微软生动幽默东北话女声，地道方言风趣幽默，极具趣味性",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "zh-CN-liaoning-XiaobeiOffline",
            speakers = listOf("zh-CN-liaoning-XiaobeiOffline (幽默东北话·女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            tags = listOf("微软官方", "东北方言", "风趣幽默")
        ),

        // 3. Sherpa-ONNX 经典高能效系列
        OfflineModelPack(
            id = "vits-icefall-zh-aishell3",
            name = "Aishell3 中文标准 174 发音人",
            category = "Sherpa-ONNX 经典",
            sizeMb = 88,
            description = "开源界最全中文多发音人语音库，涵盖 174 位专业男女发音人",
            speakerCount = 174,
            sampleRate = 22050,
            defaultVoiceId = "aishell3_spk_0",
            speakers = (0..173).map { "aishell3_spk_$it (发音人 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            tags = listOf("174发音人", "纯净无杂音", "经典多角色")
        ),
        OfflineModelPack(
            id = "vits-melo-tts-zh_en",
            name = "MeloTTS 中英双语自然混合读",
            category = "Sherpa-ONNX 经典",
            sizeMb = 115,
            description = "双语自然混合朗读，英文发音纯正无中国口音，发音流畅自然",
            speakerCount = 4,
            sampleRate = 24000,
            defaultVoiceId = "melo_zh_default",
            speakers = listOf(
                "melo_zh_default (标准中英双语·女声)",
                "melo_en_default (英美混合读·男声)",
                "melo_zh_gentle (温柔轻快·伴读女声)",
                "melo_en_accent (自然英伦·绅士男声)"
            ),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            tags = listOf("中英双语", "混读拟真", "高质量")
        ),
        OfflineModelPack(
            id = "vits-piper-zh_CN-huayan-medium",
            name = "Piper-Zh 华言超轻量中文模型",
            category = "Sherpa-ONNX 经典",
            sizeMb = 42,
            description = "极低内存占用与功耗，老旧手机亦可极速秒开发音，发音清脆",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "piper_huayan_female",
            speakers = listOf("piper_huayan_female (华言清脆·女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            tags = listOf("超轻量", "低功耗", "秒开")
        ),
        OfflineModelPack(
            id = "matcha-icefall-zh-baker",
            name = "Matcha-TTS 烘焙标准标贝女声",
            category = "Sherpa-ONNX 经典",
            sizeMb = 58,
            description = "快速流匹配 (Flow Matching) 架构，极速合成，标贝标准朗读女声",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "baker_female",
            speakers = listOf("baker_female (标贝标准·女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("FlowMatching", "标贝标准", "流式生成")
        )
    )

    private var activeCatalog: List<OfflineModelPack> = defaultCatalog

    fun getCatalog(): List<OfflineModelPack> = activeCatalog

    /**
     * 获取离线模型全局存储目录
     */
    fun getModelsStorageDirectory(context: Context): File {
        val dir = context.getExternalFilesDir("offline_models") ?: File(context.filesDir, "offline_models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * 获取指定模型包在本地的存储根目录
     */
    fun getModelDir(context: Context, modelId: String): File {
        return File(getModelsStorageDirectory(context), modelId)
    }

    /**
     * 检查离线模型包是否已完整下载就绪
     */
    fun isModelDownloaded(context: Context, modelId: String): Boolean {
        val dir = getModelDir(context, modelId)
        val flag = File(dir, "model_ready.flag")
        val files = dir.listFiles()
        return dir.exists() && dir.isDirectory && flag.exists() && (files != null && files.size >= 2)
    }

    /**
     * 删除已下载的离线模型包，彻底清理磁盘空间
     */
    fun deleteModel(context: Context, modelId: String): Boolean {
        val dir = getModelDir(context, modelId)
        return if (dir.exists()) {
            dir.deleteRecursively()
        } else true
    }

    /**
     * 从远程服务器/CDN/GitHub 动态刷新最新离线模型列表清单
     */
    suspend fun refreshRemoteCatalog(context: Context): Result<List<OfflineModelPack>> = withContext(Dispatchers.IO) {
        val client = SharedHttpClient.instance
        val urls = listOf(
            "https://cdn.jsdelivr.net/gh/misaka02/ai-tts-android@main/models_catalog.json",
            "https://raw.githubusercontent.com/misaka02/ai-tts-android/main/models_catalog.json"
        )

        for (url in urls) {
            try {
                val req = Request.Builder().url(url).build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    if (body.isNotBlank()) {
                        val remoteList = json.decodeFromString<List<OfflineModelPack>>(body)
                        if (remoteList.isNotEmpty()) {
                            val merged = (remoteList + defaultCatalog).distinctBy { it.id }
                            activeCatalog = merged
                            saveCatalogCache(context, merged)
                            return@withContext Result.success(merged)
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore and try next
            }
        }

        val cached = loadCatalogCache(context)
        if (cached.isNotEmpty()) {
            activeCatalog = cached
            return@withContext Result.success(cached)
        }

        activeCatalog = defaultCatalog
        Result.success(defaultCatalog)
    }

    /**
     * 真实流式下载模型包并解压就绪
     * @param channel: "hf_mirror" (国内高速) / "cdn" (备用加速) / "github" (官方直连)
     */
    suspend fun downloadModelPackage(
        context: Context,
        modelId: String,
        channel: String = "hf_mirror",
        onProgress: (percent: Int, statusText: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val pack = activeCatalog.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("未找到模型包: $modelId"))

        val targetDir = getModelDir(context, modelId)
        targetDir.mkdirs()

        val downloadUrl = when (channel) {
            "github" -> pack.githubUrl
            "cdn" -> pack.cdnUrl
            else -> pack.hfMirrorUrl
        }

        try {
            onProgress(5, "正在连接下载节点: ${channelName(channel)}...")
            val client = SharedHttpClient.instance
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("User-Agent", "Mozilla/5.0 AI-TTS-Engine/3.7.0")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(java.io.IOException("下载失败 HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(java.io.IOException("响应内容为空"))
            val totalBytes = body.contentLength()
            val tempFile = File(targetDir, "download.tmp")

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastPercent = 5

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val percent = (totalRead * 85 / totalBytes).toInt() + 5
                            if (percent > lastPercent) {
                                lastPercent = percent
                                val readMb = totalRead / (1024 * 1024)
                                val allMb = totalBytes / (1024 * 1024)
                                onProgress(percent, "正在下载模型数据: ${readMb}MB / ${allMb}MB ($percent%)")
                            }
                        }
                    }
                }
            }

            onProgress(88, "正在解压端侧神经网络模型权重包 (TAR.BZ2 / ZIP)...")
            val extractedCount = extractArchive(tempFile, targetDir)

            val flagFile = File(targetDir, "model_ready.flag")
            val infoFile = File(targetDir, "model_info.json")
            infoFile.writeText(json.encodeToString(OfflineModelPack.serializer(), pack))
            flagFile.writeText("READY timestamp=${System.currentTimeMillis()} id=$modelId name=${pack.name} files=$extractedCount")

            if (tempFile.exists()) tempFile.delete()

            onProgress(100, "✅ 模型包安装完成，端侧离线就绪！(已释放 $extractedCount 个神经网络文件)")
            Result.success(targetDir)
        } catch (e: Exception) {
            targetDir.deleteRecursively()
            Result.failure(e)
        }
    }

    private fun extractArchive(archiveFile: File, outputDir: File): Int {
        var count = 0
        try {
            // 优先尝试 TAR.BZ2 流式解压
            java.io.FileInputStream(archiveFile).use { fis ->
                java.io.BufferedInputStream(fis).use { bis ->
                    org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream(bis).use { bzIn ->
                        org.apache.commons.compress.archivers.tar.TarArchiveInputStream(bzIn).use { tarIn ->
                            var entry = tarIn.nextEntry
                            while (entry != null) {
                                val entryName = entry.name
                                val cleanName = if (entryName.contains("/")) entryName.substringAfterLast("/") else entryName
                                if (cleanName.isNotBlank() && !entry.isDirectory) {
                                    val outFile = File(outputDir, cleanName)
                                    java.io.FileOutputStream(outFile).use { fos ->
                                        tarIn.copyTo(fos)
                                    }
                                    count++
                                }
                                entry = tarIn.nextEntry
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 回退尝试标准 ZIP 解压
            try {
                java.io.FileInputStream(archiveFile).use { fis ->
                    java.util.zip.ZipInputStream(java.io.BufferedInputStream(fis)).use { zis ->
                        var zipEntry = zis.nextEntry
                        while (zipEntry != null) {
                            val entryName = zipEntry.name
                            val cleanName = if (entryName.contains("/")) entryName.substringAfterLast("/") else entryName
                            if (cleanName.isNotBlank() && !zipEntry.isDirectory) {
                                val outFile = File(outputDir, cleanName)
                                java.io.FileOutputStream(outFile).use { fos ->
                                    zis.copyTo(fos)
                                }
                                count++
                            }
                            zipEntry = zis.nextEntry
                        }
                    }
                }
            } catch (ze: Exception) {
                // ignore
            }
        }
        return count
    }

    private fun channelName(channel: String): String = when (channel) {
        "github" -> "GitHub 官方直连"
        "cdn" -> "CDN 备用加速"
        else -> "国内高速加速代理"
    }

    private fun saveCatalogCache(context: Context, catalog: List<OfflineModelPack>) {
        try {
            val file = File(context.filesDir, "models_catalog_cache.json")
            file.writeText(json.encodeToString(kotlinx.serialization.builtins.ListSerializer(OfflineModelPack.serializer()), catalog))
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun loadCatalogCache(context: Context): List<OfflineModelPack> {
        return try {
            val file = File(context.filesDir, "models_catalog_cache.json")
            if (file.exists()) {
                json.decodeFromString<List<OfflineModelPack>>(file.readText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
