package com.aitts.engine.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 支持的 AI TTS 提供商类型
 */
@Serializable
enum class ProviderType(val displayName: String, val description: String, val requiresApiKey: Boolean) {
    MIMO(
        displayName = "MIMO 语音大模型",
        description = "小米 MiMo-V2.5-TTS 语音大模型开放平台，超强拟真与情感表达",
        requiresApiKey = true
    ),
    MINIMAX(
        displayName = "MiniMax (海螺语音)",
        description = "MiniMax Speech-02 拟真大模型，影视级情感表现力与多风格支持",
        requiresApiKey = true
    ),
    DOUBAO(
        displayName = "火山引擎 / 豆包语音",
        description = "字节跳动豆包大模型语音合成，爽快思思与灿灿主播等高拟真音色",
        requiresApiKey = true
    ),
    EDGE_TTS(
        displayName = "微软 Edge TTS (免费)",
        description = "微软 Edge 浏览器神经网络语音，完全免费免 Key，内置晓晓、云希等",
        requiresApiKey = false
    ),
    SILICONFLOW(
        displayName = "硅基流动 (CosyVoice / ChatTTS)",
        description = "极速低延迟大模型语音服务，FunAudioLLM/CosyVoice2-0.5B 开箱即用",
        requiresApiKey = true
    ),
    FISH_AUDIO(
        displayName = "Fish Audio (鱼音)",
        description = "高表现力声音大模型与海量精选声音克隆库",
        requiresApiKey = true
    ),
    STEPFUN(
        displayName = "阶跃星辰 (Step-Audio)",
        description = "阶跃星辰 stepaudio-2.5-tts 多模态大模型，自然语境感知",
        requiresApiKey = true
    ),
    OPENAI(
        displayName = "OpenAI / GPT-4o 兼容格式",
        description = "标准 OpenAI v1/audio/speech 接口格式，支持中转与第三方兼容接口",
        requiresApiKey = true
    ),
    AZURE(
        displayName = "微软 Azure Speech",
        description = "Azure 认知服务官方语音合成，支持 SSML 高级参数",
        requiresApiKey = true
    ),
    GEMINI(
        displayName = "Google Gemini 原生 TTS 大模型",
        description = "Google 官方 Gemini 原生多模态音频生成，支持 gemini-2.5-flash-preview-tts 等与 30 款预置音色",
        requiresApiKey = true
    ),
    CUSTOM_HTTP(
        displayName = "自定义 HTTP 模板引擎",
        description = "支持 GPT-SoVITS、CosyVoice-v2、F5-TTS、VITS 等私有部署节点",
        requiresApiKey = false
    )
}

/**
 * 音色描述模型
 */
@Serializable
data class VoiceModel(
    val id: String,
    val name: String,
    val gender: String = "Female", // Female, Male, Neutral
    val locale: String = "zh-CN",
    val description: String = ""
)

/**
 * 句子角色类型（旁白 vs 角色对话）
 */
@Serializable
enum class SegmentRole {
    NARRATOR, // 旁白叙述
    DIALOGUE  // 引号内角色对话
}

/**
 * 分句实体（携带文本与角色属性）
 */
@Serializable
data class SentenceSegment(
    val text: String,
    val role: SegmentRole = SegmentRole.NARRATOR
)

/**
 * Provider 配置实体
 */
@Immutable
@Serializable
data class TtsProviderConfig(
    val id: String,
    val type: ProviderType,
    val name: String,
    val enabled: Boolean = true,
    val baseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val voiceId: String = "",
    val dialogueVoiceId: String = "", // 智能双角色：小说对话专属音色
    val isDualRoleEnabled: Boolean = false, // 是否启用旁白/对话智能双音色朗读
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val sampleRate: Int = 24000,
    val audioFormat: String = "mp3", // mp3, wav, pcm, opus, aac
    val customHeadersJson: String = "{}",
    val customPayloadTemplate: String = "{\n  \"model\": \"\${model}\",\n  \"input\": \"\${text}\",\n  \"voice\": \"\${voice}\",\n  \"speed\": \${speed}\n}",
    val responseAudioPath: String = "", // 为空表示整个 body 为二进制流，若为 json 路径如 "data.audio_base64" 则自动 Base64 解码
    val promptInstruction: String = "" // 大模型导演指令 / 提示词 (用于 MiMo、CosyVoice 等大模型调整情感、语速、音调及语境)
)

/**
 * 文本正则发音替换规则
 */
@Immutable
@Serializable
data class ReplacementRule(
    val id: String,
    val pattern: String,
    val replacement: String,
    val isRegex: Boolean = true,
    val isCaseSensitive: Boolean = false,
    val enabled: Boolean = true,
    val description: String = ""
)

/**
 * 全局应用设置
 */
@Immutable
@Serializable
data class GlobalSettings(
    val activeProviderId: String = "edge_tts_default",
    val isSentenceSplittingEnabled: Boolean = true,
    val maxSentenceLength: Int = 80,
    val isAudioCacheEnabled: Boolean = true,
    val maxCacheSizeMb: Int = 500,
    val streamingSynthesis: Boolean = true,
    val isNumberNormalizationEnabled: Boolean = true,
    val globalPitch: Float = 1.0f,
    val globalSpeed: Float = 1.0f,
    val isDebugLoggingEnabled: Boolean = true,
    val proxyEnabled: Boolean = false, // 全局代理开关
    val proxyHost: String = "127.0.0.1", // 代理 IP / 域名
    val proxyPort: Int = 7890, // 代理端口
    val proxyType: String = "HTTP", // HTTP 或 SOCKS
    val connectTimeoutSeconds: Int = 15,
    val readTimeoutSeconds: Int = 60,
    val appThemeMode: String = "SYSTEM", // SYSTEM / DARK / LIGHT
    val appThemePalette: String = "OCEAN_AZURE", // OCEAN_AZURE, EMERALD_JADE, TITANIUM_SLATE, SUNSET_AMBER, MORANDI_GRAPHITE
    val sentencePauseMs: Int = 200, // 标点分句后注入静音停顿毫秒数，大幅提升小说听感自然度
    val fallbackProviderId: String = "edge_tts_default", // 主引擎异常时自动故障转移备用引擎
    val autoFallbackOnFailure: Boolean = true, // 启用自动故障降级
    val hapticFeedbackEnabled: Boolean = true // 触觉震动反馈开关
)

/**
 * 全量备份载荷
 */
@Serializable
data class BackupPayload(
    val settings: GlobalSettings,
    val providers: List<TtsProviderConfig>,
    val rules: List<ReplacementRule>
)

/**
 * 预设 Providers 与音色清单
 */
object PresetConfigs {

    val defaultRules = listOf(
        ReplacementRule(
            id = "rule_ellipsis",
            pattern = "[…]{2,}|[\\.]{3,}",
            replacement = "，",
            isRegex = true,
            description = "将过长的省略号替换为自然逗号停顿"
        ),
        ReplacementRule(
            id = "rule_bracket_cleanup",
            pattern = "[【】〖〗「」『』\\[\\]]",
            replacement = " ",
            isRegex = true,
            description = "去除小说中常见的特殊书名或角色对话括号"
        ),
        ReplacementRule(
            id = "rule_watermark_clean",
            pattern = "(?:www\\.[a-zA-Z0-9\\.]+\\.(?:com|cn|net|org)|最新章节请访问|首发更新)",
            replacement = "",
            isRegex = true,
            description = "过滤小说盗版与水印防盗后缀"
        ),
        ReplacementRule(
            id = "rule_chong_qing",
            pattern = "重庆",
            replacement = "崇庆",
            isRegex = false,
            description = "修正多音字：重庆 (chóng -> chóng)"
        ),
        ReplacementRule(
            id = "rule_yin_hang",
            pattern = "银行",
            replacement = "银航",
            isRegex = false,
            description = "修正多音字：银行 (háng)"
        ),
        ReplacementRule(
            id = "rule_can_ci",
            pattern = "参差",
            replacement = "涔呲",
            isRegex = false,
            description = "修正多音字：参差 (cēn cī)"
        ),
        ReplacementRule(
            id = "rule_chai_qian",
            pattern = "差遣",
            replacement = "拆遣",
            isRegex = false,
            description = "修正多音字：差遣 (chāi)"
        ),
        ReplacementRule(
            id = "rule_bian_yi",
            pattern = "便宜行事",
            replacement = "便移形事",
            isRegex = false,
            description = "修正多音字成语：便宜行事 (biàn yí)"
        ),
        ReplacementRule(
            id = "rule_guan_qia",
            pattern = "关卡",
            replacement = "关恰",
            isRegex = false,
            description = "修正多音字：关卡 (qiǎ)"
        ),
        ReplacementRule(
            id = "rule_xue_ruo",
            pattern = "削弱",
            replacement = "薛弱",
            isRegex = false,
            description = "修正多音字：削弱 (xuē)"
        ),
        ReplacementRule(
            id = "rule_mo_sha",
            pattern = "抹杀",
            replacement = "莫杀",
            isRegex = false,
            description = "修正多音字：抹杀 (mǒ)"
        ),
        ReplacementRule(
            id = "rule_bi_lu",
            pattern = "秘鲁",
            replacement = "必鲁",
            isRegex = false,
            description = "修正地名多音字：秘鲁 (bì)"
        ),
        ReplacementRule(
            id = "rule_qiu_ci",
            pattern = "龟兹",
            replacement = "丘慈",
            isRegex = false,
            description = "修正古地名多音字：龟兹 (qiū cí)"
        )
    )

    fun createDefaultProviders(): List<TtsProviderConfig> {
        return listOf(
            // 1. Edge TTS 免费默认
            TtsProviderConfig(
                id = "edge_tts_default",
                type = ProviderType.EDGE_TTS,
                name = "Edge TTS (晓晓 - 微软大模型)",
                enabled = true,
                modelName = "edge-neural",
                voiceId = "zh-CN-XiaoxiaoNeural",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            TtsProviderConfig(
                id = "edge_tts_yunxi",
                type = ProviderType.EDGE_TTS,
                name = "Edge TTS (云希 - 沉浸男声)",
                enabled = true,
                modelName = "edge-neural",
                voiceId = "zh-CN-YunxiNeural",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 2. MIMO 小米语音大模型
            TtsProviderConfig(
                id = "mimo_default",
                type = ProviderType.MIMO,
                name = "小米 MiMo-V2.5-TTS 大模型",
                enabled = false,
                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions",
                apiKey = "",
                modelName = "mimo-v2.5-tts",
                voiceId = "茉莉",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 3. MiniMax 海螺语音
            TtsProviderConfig(
                id = "minimax_default",
                type = ProviderType.MINIMAX,
                name = "MiniMax Speech-02 (海螺语音)",
                enabled = false,
                baseUrl = "https://api.minimax.chat/v1/t2a_v2",
                apiKey = "",
                modelName = "speech-02-turbo",
                voiceId = "male-qn-qingse",
                sampleRate = 32000,
                audioFormat = "mp3"
            ),
            // 4. 火山引擎 / 豆包
            TtsProviderConfig(
                id = "doubao_default",
                type = ProviderType.DOUBAO,
                name = "火山豆包大模型 (思思主播)",
                enabled = false,
                baseUrl = "https://openspeech.bytedance.com/api/v1/tts",
                apiKey = "",
                modelName = "volcano_tts",
                voiceId = "zh_female_shuangkuaisisi_moon_bigtts",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 5. SiliconFlow (CosyVoice / ChatTTS)
            TtsProviderConfig(
                id = "siliconflow_cosyvoice",
                type = ProviderType.SILICONFLOW,
                name = "硅基流动 (CosyVoice2-0.5B)",
                enabled = false,
                baseUrl = "https://api.siliconflow.cn/v1/audio/speech",
                apiKey = "",
                modelName = "FunAudioLLM/CosyVoice2-0.5B",
                voiceId = "FunAudioLLM/CosyVoice2-0.5B:alex",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 6. Fish Audio
            TtsProviderConfig(
                id = "fish_audio_default",
                type = ProviderType.FISH_AUDIO,
                name = "Fish Audio (鱼音官方)",
                enabled = false,
                baseUrl = "https://api.fish.audio/v1/tts",
                apiKey = "",
                modelName = "speech-v1.4",
                voiceId = "7f92f8afb8ec43bf81429cc1c9199cb1",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 7. StepFun 阶跃星辰
            TtsProviderConfig(
                id = "stepfun_default",
                type = ProviderType.STEPFUN,
                name = "阶跃星辰 (Step-Audio 2.5)",
                enabled = false,
                baseUrl = "https://api.stepfun.com/v1/audio/speech",
                apiKey = "",
                modelName = "stepaudio-2.5-tts",
                voiceId = "cixingnansheng",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 8. OpenAI
            TtsProviderConfig(
                id = "openai_tts",
                type = ProviderType.OPENAI,
                name = "OpenAI (TTS-1 / GPT-4o)",
                enabled = false,
                baseUrl = "https://api.openai.com/v1/audio/speech",
                apiKey = "",
                modelName = "tts-1",
                voiceId = "nova",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 9. Azure Speech
            TtsProviderConfig(
                id = "azure_speech",
                type = ProviderType.AZURE,
                name = "Azure 认知语音 (eastasia)",
                enabled = false,
                baseUrl = "https://eastasia.tts.speech.microsoft.com/cognitiveservices/v1",
                apiKey = "",
                modelName = "azure-neural",
                voiceId = "zh-CN-XiaoxiaoNeural",
                sampleRate = 24000,
                audioFormat = "mp3"
            ),
            // 10. Google Gemini 原生 TTS 大模型
            TtsProviderConfig(
                id = "gemini_default",
                type = ProviderType.GEMINI,
                name = "Google Gemini 2.5 Flash TTS",
                enabled = false,
                baseUrl = "https://generativelanguage.googleapis.com/v1beta",
                apiKey = "",
                modelName = "gemini-2.5-flash-preview-tts",
                voiceId = "Puck",
                promptInstruction = "Please read the text aloud clearly and naturally with appropriate emotions.",
                sampleRate = 24000,
                audioFormat = "wav"
            ),
            // 11. 自定义私有模型 (GPT-SoVITS 示例)
            TtsProviderConfig(
                id = "custom_gpt_sovits",
                type = ProviderType.CUSTOM_HTTP,
                name = "自定义 GPT-SoVITS 节点",
                enabled = false,
                baseUrl = "http://192.168.1.100:9880/tts",
                apiKey = "",
                modelName = "gpt-sovits-v2",
                voiceId = "default",
                customPayloadTemplate = "{\n  \"text\": \"\${text}\",\n  \"text_lang\": \"zh\",\n  \"speed\": \${speed}\n}",
                sampleRate = 32000,
                audioFormat = "wav"
            )
        )
    }

    val edgeVoices = listOf(
        VoiceModel("zh-CN-XiaoxiaoNeural", "晓晓 (女声·温暖自然)", "Female", "zh-CN", "微软经典女声，适合长篇小说阅读"),
        VoiceModel("zh-CN-YunxiNeural", "云希 (男声·阳光沉稳)", "Male", "zh-CN", "微软经典男声，适合玄幻/都市小说"),
        VoiceModel("zh-CN-YunjianNeural", "云健 (男声·影视解说)", "Male", "zh-CN", "气势恢宏，适合短视频与热血桥段"),
        VoiceModel("zh-CN-XiaoyiNeural", "晓伊 (女声·甜美电台)", "Female", "zh-CN", "甜美活泼，适合轻松甜宠文"),
        VoiceModel("zh-CN-YunyangNeural", "云扬 (男声·专业播音)", "Male", "zh-CN", "新闻播音腔，字正腔圆"),
        VoiceModel("zh-CN-liaoning-XiaobeiNeural", "东北小北 (女声·幽默方言)", "Female", "zh-CN-liaoning", "东北风情，生动幽默"),
        VoiceModel("zh-CN-shaanxi-XiaoniNeural", "陕西小妮 (女声·陕西方言)", "Female", "zh-CN-shaanxi", "西北方言特色"),
        VoiceModel("zh-TW-HsiaoChenNeural", "曉臻 (台灣女聲·溫柔親切)", "Female", "zh-TW", "台灣國語女聲"),
        VoiceModel("zh-HK-HiuMaanNeural", "曉曼 (粵語女聲·標準清晰)", "Female", "zh-HK", "廣東話 / 粵語朗讀"),
        VoiceModel("en-US-JennyNeural", "Jenny (English Female)", "Female", "en-US", "Standard US English Female"),
        VoiceModel("en-US-GuyNeural", "Guy (English Male)", "Male", "en-US", "Standard US English Male"),
        VoiceModel("ja-JP-NanamiNeural", "七海 (日本語女声)", "Female", "ja-JP", "日本語朗読・アニメ風")
    )
}
