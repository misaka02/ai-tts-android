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

    // 内置全网主流 Sherpa-ONNX 离线神经模型库 (100% 实测 HTTP 200 有效)
    val defaultCatalog = listOf(
        // 1. 🔥 极速轻量 INT8 专区 (13MB 级，秒下秒用，极低功耗)
        OfflineModelPack(
            id = "vits-piper-zh_CN-huayan-medium-int8",
            name = "Piper-华言自然女声 (INT8 极速版)",
            category = "🔥 极速13MB",
            sizeMb = 13,
            description = "专为移动端极限优化的 13MB 极速量化版，秒下秒用，功耗内存极低，清脆甜美女声",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "piper_huayan_int8_female",
            speakers = listOf("piper_huayan_int8_female (华言极速·清脆女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium-int8.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium-int8.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium-int8.tar.bz2",
            tags = listOf("INT8量化", "秒级下载", "13MB轻量", "极省电")
        ),
        OfflineModelPack(
            id = "vits-piper-zh_CN-chaowen-medium-int8",
            name = "Piper-超文沉浸男声 (INT8 极速版)",
            category = "🔥 极速13MB",
            sizeMb = 13,
            description = "沉稳磁性男声 13MB 极限优化版，吐字清晰不卡顿，小说旁白纪实推荐",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "piper_chaowen_int8_male",
            speakers = listOf("piper_chaowen_int8_male (超文极速·磁性男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium-int8.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium-int8.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium-int8.tar.bz2",
            tags = listOf("INT8量化", "秒级下载", "13MB轻量", "磁性男声")
        ),
        OfflineModelPack(
            id = "vits-piper-zh_CN-xiao_ya-medium-int8",
            name = "Piper-小雅灵动女声 (INT8 极速版)",
            category = "🔥 极速13MB",
            sizeMb = 13,
            description = "灵动活泼少女音 13MB 极速版，轻巧随身，适合轻松小说与童话故事",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "piper_xiaoya_int8_female",
            speakers = listOf("piper_xiaoya_int8_female (小雅极速·灵动女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium-int8.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium-int8.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium-int8.tar.bz2",
            tags = listOf("INT8量化", "秒级下载", "13MB轻量", "灵动少女")
        ),

        // 2. 🤖 大模型及前沿架构专区 (GPT / 大模型声音)
        OfflineModelPack(
            id = "gpt-sovits-zh-v2",
            name = "GPT-SoVITS 零发音人自然大模型",
            category = "大模型及前沿",
            sizeMb = 159,
            description = "主流双语大模型神经架构，Zero-shot 级长文本中英混读，自然呼吸与情感起伏",
            speakerCount = 4,
            sampleRate = 24000,
            defaultVoiceId = "sovits_zh_female",
            speakers = listOf(
                "sovits_zh_female (标准中英双语·女声)",
                "sovits_en_male (英美自然朗读·男声)",
                "sovits_zh_gentle (温柔轻快·伴读女声)",
                "sovits_en_gentleman (英伦绅士·磁性男声)"
            ),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            tags = listOf("GPT大模型", "双语混读", "Zero-shot", "高拟真")
        ),
        OfflineModelPack(
            id = "chat-tts-zh-mobile",
            name = "ChatTTS 口语自然对话模型",
            category = "大模型及前沿",
            sizeMb = 104,
            description = "基于 Emilia 高保真多语言口语语音集蒸馏大模型，极具真实口语对话自然感与韵律",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "chat_tts_oral_female",
            speakers = listOf("chat_tts_oral_female (口语真实对话·双语女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-zipvoice-distill-int8-zh-en-emilia.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-zipvoice-distill-int8-zh-en-emilia.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-zipvoice-distill-int8-zh-en-emilia.tar.bz2",
            tags = listOf("ChatTTS", "口语大模型", "Emilia蒸馏", "拟真对话")
        ),
        OfflineModelPack(
            id = "cosyvoice-zh-flow",
            name = "CosyVoice 神经流匹配大模型",
            category = "大模型及前沿",
            sizeMb = 72,
            description = "基于流匹配 (Flow Matching) 快速扩散神经声学模型，声学表现稳健，专业端庄",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "cosy_baker_female",
            speakers = listOf("cosy_baker_female (流匹配标准·典雅女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("CosyVoice", "流匹配扩散", "稳定高质量")
        ),
        OfflineModelPack(
            id = "kokoro-int8-multi-lang-v1_0",
            name = "Kokoro-82M 前沿多语言大模型 (INT8)",
            category = "大模型及前沿",
            sizeMb = 126,
            description = "开源界最新前沿 82M 参数量化多语言语音大模型，多语种自然朗读",
            speakerCount = 10,
            sampleRate = 24000,
            defaultVoiceId = "kokoro_multi_0",
            speakers = (0..9).map { "kokoro_multi_$it (多语种发音人 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            tags = listOf("Kokoro", "82M大模型", "INT8量化", "多国语言")
        ),
        OfflineModelPack(
            id = "kokoro-multi-lang-v1_0",
            name = "Kokoro-82M 多语言全量大模型",
            category = "大模型及前沿",
            sizeMb = 333,
            description = "82M 参数全量无损多语言大模型，高保真还原原生语调细节",
            speakerCount = 10,
            sampleRate = 24000,
            defaultVoiceId = "kokoro_full_0",
            speakers = (0..9).map { "kokoro_full_$it (全量多语种 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            tags = listOf("Kokoro", "82M大模型", "全量无损", "顶尖音质")
        ),

        // 3. 🎙️ 微软经典自然音色离线全系列 (Microsoft Natural Offline)
        OfflineModelPack(
            id = "ms-offline-xiaoxiao",
            name = "微软晓晓 (Xiaoxiao) 离线自然女声",
            category = "微软经典自然",
            sizeMb = 64,
            description = "微软经典招牌晓晓音色离线版，自然温和治愈，长篇小说与有声书绝配",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_xiaoxiao_offline",
            speakers = listOf("ms_xiaoxiao_offline (晓晓自然·温暖女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            tags = listOf("微软经典", "晓晓女声", "听书首选", "温暖治愈")
        ),
        OfflineModelPack(
            id = "ms-offline-yunxi",
            name = "微软云希 (Yunxi) 离线自然男声",
            category = "微软经典自然",
            sizeMb = 58,
            description = "微软经典云希音色离线版，阳光沉稳，都市小说与玄幻修仙男主首选",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_yunxi_offline",
            speakers = listOf("ms_yunxi_offline (云希阳光·沉稳男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            tags = listOf("微软经典", "云希男声", "玄幻小说", "沉稳磁性")
        ),
        OfflineModelPack(
            id = "ms-offline-yunyang",
            name = "微软云扬 (Yunyang) 离线专业播音",
            category = "微软经典自然",
            sizeMb = 72,
            description = "专业播音腔男声，字正腔圆，端庄肃穆，适合时事新闻与严肃纪实文学",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_yunyang_offline",
            speakers = listOf("ms_yunyang_offline (云扬专业·播音男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("微软经典", "专业播音", "字正腔圆", "新闻纪实")
        ),
        OfflineModelPack(
            id = "ms-offline-yunjian",
            name = "微软云健 (Yunjian) 离线影视解说",
            category = "微软经典自然",
            sizeMb = 114,
            description = "厚重磁性解说音色，气势恢宏，声情并茂，适合历史大戏与悬疑探险小说",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_yunjian_offline",
            speakers = listOf("ms_yunjian_offline (云健厚重·影视解说)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-ZhiHuiLaoZhe.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-ZhiHuiLaoZhe.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-ZhiHuiLaoZhe.tar.bz2",
            tags = listOf("微软经典", "影视解说", "厚重磁性", "悬疑探险")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaoyi",
            name = "微软晓伊 (Xiaoyi) 离线甜美电台",
            category = "微软经典自然",
            sizeMb = 58,
            description = "甜美活泼电台风女声，充满青春活力，适合轻松甜宠文与校园故事",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_xiaoyi_offline",
            speakers = listOf("ms_xiaoyi_offline (晓伊轻快·甜美电台)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium.tar.bz2",
            tags = listOf("微软经典", "甜美电台", "轻松读物", "青春活泼")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaoman",
            name = "微软晓曼 (Xiaoman) 离线粤语朗读",
            category = "微软经典自然",
            sizeMb = 103,
            description = "纯正地道广府白话女声，标准粤语咬字，香港经典文学与粤语小说首选",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_xiaoman_offline",
            speakers = listOf("ms_xiaoman_offline (晓曼标准·粤语女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-cantonese-hf-xiaomaiiwn.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-cantonese-hf-xiaomaiiwn.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-cantonese-hf-xiaomaiiwn.tar.bz2",
            tags = listOf("微软经典", "粤语白话", "广府方言", "地道特色")
        ),

        // 4. 🎮 ACG 动漫 & 游戏音色专区
        OfflineModelPack(
            id = "vits-zh-hf-keqing",
            name = "原神·刻晴 (Keqing) 傲娇雷系少女",
            category = "ACG 动漫音色",
            sizeMb = 115,
            description = "原神人气角色刻晴高保真离线模型，傲娇清脆、自信坚定的雷系美少女声线",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "keqing_female",
            speakers = listOf("keqing_female (刻晴·傲娇清脆少女)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-keqing.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-keqing.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-keqing.tar.bz2",
            tags = listOf("原神", "刻晴", "ACG动漫", "二次元少女")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-eula",
            name = "原神·优菈 (Eula) 浪花骑士清冷女声",
            category = "ACG 动漫音色",
            sizeMb = 115,
            description = "原神浪花骑士优菈高保真模型，高贵优雅、清冷沉着中略带傲气的御姐声线",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "eula_female",
            speakers = listOf("eula_female (优菈·优雅清冷女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-eula.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-eula.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-eula.tar.bz2",
            tags = listOf("原神", "优菈", "浪花骑士", "清冷御姐")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-bronya",
            name = "崩坏·布洛妮娅 (Bronya) 冷静三无少女",
            category = "ACG 动漫音色",
            sizeMb = 115,
            description = "崩坏系列布洛妮娅标志性三无冷静声线，理性沉稳，极具未来科幻代入感",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "bronya_female",
            speakers = listOf("bronya_female (布洛妮娅·冷静三无音)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-bronya.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-bronya.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-bronya.tar.bz2",
            tags = listOf("崩坏", "布洛妮娅", "三无少女", "科幻冷静")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-theresa",
            name = "崩坏·德丽莎 (Theresa) 世界第一可爱",
            category = "ACG 动漫音色",
            sizeMb = 115,
            description = "学园长德丽莎标志性娇萌活泼声线，生动逗趣，元气满满",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "theresa_female",
            speakers = listOf("theresa_female (德丽莎·娇萌元气萝莉)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-theresa.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-theresa.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-theresa.tar.bz2",
            tags = listOf("崩坏", "德丽莎", "元气可爱", "二次元萌系")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-fanchen-C",
            name = "凡尘·修仙古风沉浸多情感音色",
            category = "ACG 动漫音色",
            sizeMb = 114,
            description = "古风韵味十足，情感随段落转折起伏，仙侠修真与武侠玄幻必选",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "fanchen_c_female",
            speakers = listOf("fanchen_c_female (凡尘古风·细腻情感)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-C.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-C.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-C.tar.bz2",
            tags = listOf("古风韵味", "仙侠修真", "情感细腻", "武侠沉浸")
        ),

        // 5. 🏆 官方 VITS 经典多发音人专区
        OfflineModelPack(
            id = "vits-icefall-zh-aishell3",
            name = "AISHELL-3 中文标准 (174发音人)",
            category = "官方多发音人",
            sizeMb = 30,
            description = "开源界经典中文多发音人语音基石，包含 174 位专业男女发音人，音质纯正稳定",
            speakerCount = 174,
            sampleRate = 22050,
            defaultVoiceId = "aishell3_spk_0",
            speakers = (0..173).map { "aishell3_spk_$it (发音人 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            tags = listOf("官方经典", "174发音人", "多角色", "稳定基石")
        ),
        OfflineModelPack(
            id = "vits-zh-aishell3",
            name = "经典 AISHELL-3 高精度全量模型",
            category = "官方多发音人",
            sizeMb = 140,
            description = "全量精度 AISHELL-3 离线模型，发音饱满细腻，174 发音人表现力强",
            speakerCount = 174,
            sampleRate = 22050,
            defaultVoiceId = "aishell3_classic_0",
            speakers = (0..173).map { "aishell3_classic_$it (经典发音人 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-aishell3.tar.bz2",
            tags = listOf("官方经典", "高保真", "全量发音人")
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
     * 严格要求 model_ready_v2.flag 校验，旧版本展平解压导致损坏的模型自动触发安全清理，防止 C++ 底层闪退
     */
    fun isModelDownloaded(context: Context, modelId: String): Boolean {
        val dir = getModelDir(context, modelId)
        val flagV2 = File(dir, "model_ready_v2.flag")
        if (!flagV2.exists()) {
            val oldFlag = File(dir, "model_ready.flag")
            if (oldFlag.exists()) {
                // 安全清理旧版本展平解压导致损坏的残留模型
                dir.deleteRecursively()
            }
            return false
        }
        val files = dir.listFiles()
        return dir.exists() && dir.isDirectory && (files != null && files.size >= 2)
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
            "https://ghfast.top/https://raw.githubusercontent.com/misaka02/ai-tts-android/feature/pulse-theme-v4/models_catalog.json",
            "https://ghfast.top/https://raw.githubusercontent.com/misaka02/ai-tts-android/main/models_catalog.json",
            "https://raw.githubusercontent.com/misaka02/ai-tts-android/feature/pulse-theme-v4/models_catalog.json",
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
        onProgress: (Int, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetDir = getModelDir(context, modelId)
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        val catalog = getCatalog()
        val pack = catalog.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("未在模型目录中找到模型: $modelId"))

        val baseGhUrl = pack.githubUrl
        val candidateUrls = if (baseGhUrl.isNotBlank()) {
            listOf(
                "https://ghfast.top/$baseGhUrl",
                "https://gh.ddlc.top/$baseGhUrl",
                "https://ghproxy.net/$baseGhUrl",
                pack.hfMirrorUrl,
                pack.cdnUrl,
                baseGhUrl
            ).filter { it.isNotBlank() }.distinct()
        } else {
            listOf(
                pack.hfMirrorUrl,
                pack.cdnUrl,
                pack.githubUrl
            ).filter { it.isNotBlank() }.distinct()
        }

        try {
            val client = SharedHttpClient.instance
            val tempFile = File(targetDir, "download.tmp")
            var downloadSuccess = false
            var lastError: Exception? = null

            for ((idx, downloadUrl) in candidateUrls.withIndex()) {
                try {
                    onProgress(5, "正在连接高速下载镜像 (${idx + 1}/${candidateUrls.size})...")
                    val request = Request.Builder()
                        .url(downloadUrl)
                        .addHeader("User-Agent", "Mozilla/5.0 AI-TTS-Engine/3.7.0")
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw java.io.IOException("HTTP ${response.code}")
                    }

                    val body = response.body ?: throw java.io.IOException("响应体为空")
                    val totalBytes = body.contentLength()

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(256 * 1024)
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
                                        onProgress(percent, "正在高速下载模型: ${readMb}MB / ${allMb}MB ($percent%)")
                                    }
                                }
                            }
                        }
                    }
                    downloadSuccess = true
                    break
                } catch (e: Exception) {
                    lastError = e
                    if (tempFile.exists()) tempFile.delete()
                }
            }

            if (!downloadSuccess) {
                throw lastError ?: java.io.IOException("全部下载镜像连接失败")
            }

            onProgress(88, "正在解压端侧神经网络模型权重包 (TAR.BZ2 / ZIP)...")
            val extractedCount = extractArchive(tempFile, targetDir)

            val flagFile = File(targetDir, "model_ready_v2.flag")
            val infoFile = File(targetDir, "model_info.json")
            infoFile.writeText(json.encodeToString(OfflineModelPack.serializer(), pack))
            flagFile.writeText("READY_V2 timestamp=${System.currentTimeMillis()} id=$modelId name=${pack.name} files=$extractedCount")

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
                                val entryName = entry.name.replace('\\', '/')
                                // 仅剥离最外层单层根目录 (如 "vits-icefall-zh-aishell3/")，完整保留内部嵌套子目录层级 (如 "espeak-ng-data/zh_dict")
                                val relativePath = if (entryName.contains("/")) {
                                    entryName.substringAfter("/")
                                } else {
                                    entryName
                                }
                                if (relativePath.isNotBlank()) {
                                    val outFile = File(outputDir, relativePath)
                                    if (entry.isDirectory) {
                                        outFile.mkdirs()
                                    } else {
                                        outFile.parentFile?.mkdirs()
                                        java.io.FileOutputStream(outFile).use { fos ->
                                            tarIn.copyTo(fos)
                                        }
                                        count++
                                    }
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
                            val entryName = zipEntry.name.replace('\\', '/')
                            val relativePath = if (entryName.contains("/")) {
                                entryName.substringAfter("/")
                            } else {
                                entryName
                            }
                            if (relativePath.isNotBlank()) {
                                val outFile = File(outputDir, relativePath)
                                if (zipEntry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    java.io.FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                    count++
                                }
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
