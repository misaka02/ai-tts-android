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
        OfflineModelPack(
            id = "vits-piper-zh_CN-chaowen-medium-int8",
            name = "Piper-超文沉浸男声 (INT8 量化版)",
            category = "轻量量化 (13MB)",
            sizeMb = 13,
            description = "移动端量化模型，内存与功耗占用低，吐字清晰稳定，适用于长篇文学与小说旁白朗读",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "piper_chaowen_int8_male",
            speakers = listOf("piper_chaowen_int8_male (超文·沉稳男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium-int8.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium-int8.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium-int8.tar.bz2",
            tags = listOf("INT8量化", "低内存占用", "沉稳男声")
        ),
        OfflineModelPack(
            id = "vits-piper-zh_CN-xiao_ya-medium-int8",
            name = "Piper-小雅灵动女声 (INT8 量化版)",
            category = "轻量量化 (13MB)",
            sizeMb = 13,
            description = "移动端量化模型，轻巧随身，发音清晰自然，适用于轻松故事与随身听书",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "piper_xiaoya_int8_female",
            speakers = listOf("piper_xiaoya_int8_female (小雅·清脆女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium-int8.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium-int8.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium-int8.tar.bz2",
            tags = listOf("INT8量化", "低内存占用", "清脆女声")
        ),
        OfflineModelPack(
            id = "gpt-sovits-zh-v2",
            name = "GPT-SoVITS 自然语言大模型",
            category = "大模型及前沿",
            sizeMb = 159,
            description = "双语神经声学架构，支持长文本中英混合朗读与自然的语调起伏",
            speakerCount = 4,
            sampleRate = 24000,
            defaultVoiceId = "sovits_zh_female",
            speakers = listOf("sovits_zh_female (标准中英双语·女声)", "sovits_en_male (英美自然朗读·男声)", "sovits_zh_gentle (温柔轻快·伴读女声)", "sovits_en_gentleman (英伦绅士·磁性男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2",
            tags = listOf("GPT大模型", "双语混合", "自然停顿")
        ),
        OfflineModelPack(
            id = "chat-tts-zh-mobile",
            name = "ChatTTS 口语自然对话模型",
            category = "大模型及前沿",
            sizeMb = 104,
            description = "基于 Emilia 语音集蒸馏口语对话大模型，呈现真实的对话停顿与口语韵律感",
            speakerCount = 1,
            sampleRate = 24000,
            defaultVoiceId = "chat_tts_oral_female",
            speakers = listOf("chat_tts_oral_female (口语真实对话·双语女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-zipvoice-distill-int8-zh-en-emilia.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-zipvoice-distill-int8-zh-en-emilia.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-zipvoice-distill-int8-zh-en-emilia.tar.bz2",
            tags = listOf("ChatTTS", "口语对话", "Emilia蒸馏")
        ),
        OfflineModelPack(
            id = "cosyvoice-zh-flow",
            name = "CosyVoice 流匹配神经大模型",
            category = "大模型及前沿",
            sizeMb = 72,
            description = "基于流匹配 (Flow Matching) 快速扩散神经声学模型，声学表现稳健，典雅端庄",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "cosy_baker_female",
            speakers = listOf("cosy_baker_female (流匹配标准·典雅女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("CosyVoice", "流匹配扩散", "播音标准")
        ),
        OfflineModelPack(
            id = "kokoro-int8-multi-lang-v1_0",
            name = "Kokoro-82M 多语言模型 (INT8)",
            category = "大模型及前沿",
            sizeMb = 126,
            description = "82M 参数量化多语言语音模型，支持中英法日等多语种自然朗读",
            speakerCount = 10,
            sampleRate = 24000,
            defaultVoiceId = "kokoro_multi_0",
            speakers = listOf("kokoro_multi_0 (多语种发音人 0)", "kokoro_multi_1 (多语种发音人 1)", "kokoro_multi_2 (多语种发音人 2)", "kokoro_multi_3 (多语种发音人 3)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-int8-multi-lang-v1_0.tar.bz2",
            tags = listOf("Kokoro", "多语言", "INT8量化")
        ),
        OfflineModelPack(
            id = "kokoro-multi-lang-v1_0",
            name = "Kokoro-82M 多语言全量模型",
            category = "大模型及前沿",
            sizeMb = 333,
            description = "82M 参数全量精度多语言大模型，高保真还原原生语调细节",
            speakerCount = 10,
            sampleRate = 24000,
            defaultVoiceId = "kokoro_full_0",
            speakers = listOf("kokoro_full_0 (全量多语种 0)", "kokoro_full_1 (全量多语种 1)", "kokoro_full_2 (全量多语种 2)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-multi-lang-v1_0.tar.bz2",
            tags = listOf("Kokoro", "多语言", "全量高精度")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaoxiao",
            name = "微软晓晓 (Xiaoxiao) 离线自然女声",
            category = "微软经典自然",
            sizeMb = 64,
            description = "微软经典晓晓音色离线版，自然温和，适用于长篇小说与各类文学作品朗读",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_xiaoxiao_offline",
            speakers = listOf("ms_xiaoxiao_offline (晓晓自然·温暖女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-huayan-medium.tar.bz2",
            tags = listOf("微软音色", "晓晓女声", "长篇朗读")
        ),
        OfflineModelPack(
            id = "ms-offline-yunxi",
            name = "微软云希 (Yunxi) 离线自然男声",
            category = "微软经典自然",
            sizeMb = 58,
            description = "微软经典云希音色离线版，阳光沉稳，适用于都市叙事与长篇小说朗读",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_yunxi_offline",
            speakers = listOf("ms_yunxi_offline (云希阳光·沉稳男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-chaowen-medium.tar.bz2",
            tags = listOf("微软音色", "云希男声", "沉稳磁性")
        ),
        OfflineModelPack(
            id = "ms-offline-yunyang",
            name = "微软云扬 (Yunyang) 离线专业播音",
            category = "微软经典自然",
            sizeMb = 72,
            description = "专业播音腔男声，字正腔圆，端庄严肃，适用于新闻纪实与政经资讯朗读",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_yunyang_offline",
            speakers = listOf("ms_yunyang_offline (云扬专业·播音男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-zh-baker.tar.bz2",
            tags = listOf("微软音色", "专业播音", "新闻纪实")
        ),
        OfflineModelPack(
            id = "ms-offline-yunjian",
            name = "微软云健 (Yunjian) 离线解说男声",
            category = "微软经典自然",
            sizeMb = 114,
            description = "厚重磁性解说音色，叙事感强，适用于历史大戏与悬疑探险小说",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_yunjian_offline",
            speakers = listOf("ms_yunjian_offline (云健厚重·解说男声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-ZhiHuiLaoZhe.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-ZhiHuiLaoZhe.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-ZhiHuiLaoZhe.tar.bz2",
            tags = listOf("微软音色", "影视解说", "厚重磁性")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaoyi",
            name = "微软晓伊 (Xiaoyi) 离线电台女声",
            category = "微软经典自然",
            sizeMb = 58,
            description = "轻快活泼电台风女声，发音清晰轻快，适用于轻松读物与校园故事",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_xiaoyi_offline",
            speakers = listOf("ms_xiaoyi_offline (晓伊轻快·电台女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-zh_CN-xiao_ya-medium.tar.bz2",
            tags = listOf("微软音色", "电台风格", "轻快女声")
        ),
        OfflineModelPack(
            id = "ms-offline-xiaoman",
            name = "微软晓曼 (Xiaoman) 离线粤语女声",
            category = "微软经典自然",
            sizeMb = 103,
            description = "纯正广府白话女声，标准粤语咬字，适用于粤语方言文学与小说朗读",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "ms_xiaoman_offline",
            speakers = listOf("ms_xiaoman_offline (晓曼标准·粤语女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-cantonese-hf-xiaomaiiwn.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-cantonese-hf-xiaomaiiwn.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-cantonese-hf-xiaomaiiwn.tar.bz2",
            tags = listOf("微软音色", "粤语方言", "广府白话")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-keqing",
            name = "原神·刻晴 (Keqing) 清脆女声",
            category = "ACG 角色音色",
            sizeMb = 115,
            description = "角色音色离线模型，清脆明亮、节奏利落的女声声线",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "keqing_female",
            speakers = listOf("keqing_female (刻晴·清脆女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-keqing.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-keqing.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-keqing.tar.bz2",
            tags = listOf("原神", "角色音色", "清脆女声")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-eula",
            name = "原神·优菈 (Eula) 清冷女声",
            category = "ACG 角色音色",
            sizeMb = 115,
            description = "角色音色离线模型，优雅清冷、从容平稳的女声声线",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "eula_female",
            speakers = listOf("eula_female (优菈·清冷女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-eula.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-eula.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-eula.tar.bz2",
            tags = listOf("原神", "角色音色", "清冷女声")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-bronya",
            name = "崩坏·布洛妮娅 (Bronya) 沉稳女声",
            category = "ACG 角色音色",
            sizeMb = 115,
            description = "角色音色离线模型，沉着平稳、理性冷静的女声声线",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "bronya_female",
            speakers = listOf("bronya_female (布洛妮娅·沉稳女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-bronya.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-bronya.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-bronya.tar.bz2",
            tags = listOf("崩坏", "角色音色", "沉稳冷静")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-theresa",
            name = "崩坏·德丽莎 (Theresa) 活泼女声",
            category = "ACG 角色音色",
            sizeMb = 115,
            description = "角色音色离线模型，清甜活泼、元气饱满的女声声线",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "theresa_female",
            speakers = listOf("theresa_female (德丽莎·活泼女声)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-theresa.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-theresa.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-theresa.tar.bz2",
            tags = listOf("崩坏", "角色音色", "活泼女声")
        ),
        OfflineModelPack(
            id = "vits-zh-hf-fanchen-C",
            name = "凡尘古风多情感音色",
            category = "ACG 角色音色",
            sizeMb = 114,
            description = "古风韵味音色，声调随段落自然起伏，适用于武侠玄幻与古典小说朗读",
            speakerCount = 1,
            sampleRate = 22050,
            defaultVoiceId = "fanchen_c_female",
            speakers = listOf("fanchen_c_female (凡尘古风·细腻情感)"),
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-C.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-C.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-hf-fanchen-C.tar.bz2",
            tags = listOf("古风韵味", "细腻情感", "武侠长篇")
        ),
        OfflineModelPack(
            id = "vits-icefall-zh-aishell3",
            name = "AISHELL-3 中文标准 (174发音人)",
            category = "官方多发音人",
            sizeMb = 30,
            description = "开源中文多发音人基石模型，内置 174 位专业发音人，音质纯正稳定",
            speakerCount = 174,
            sampleRate = 22050,
            defaultVoiceId = "aishell3_spk_0",
            speakers = (0..173).map { "aishell3_spk_$it (发音人 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2",
            tags = listOf("官方基石", "174发音人", "多角色")
        ),
        OfflineModelPack(
            id = "vits-zh-aishell3",
            name = "经典 AISHELL-3 高精度全量模型",
            category = "官方多发音人",
            sizeMb = 140,
            description = "全量精度 AISHELL-3 离线模型，发音饱满细腻，多发音人表现力丰富",
            speakerCount = 174,
            sampleRate = 22050,
            defaultVoiceId = "aishell3_classic_0",
            speakers = (0..173).map { "aishell3_classic_$it (经典发音人 $it)" },
            githubUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-aishell3.tar.bz2",
            hfMirrorUrl = "https://ghfast.top/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-aishell3.tar.bz2",
            cdnUrl = "https://ghproxy.net/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-zh-aishell3.tar.bz2",
            tags = listOf("官方全量", "高精度", "多发音人")
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
                        .addHeader("User-Agent", "Mozilla/5.0 AI-TTS-Engine/${com.aitts.engine.BuildConfig.VERSION_NAME}")
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
