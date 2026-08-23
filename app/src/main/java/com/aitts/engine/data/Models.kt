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
    DIALOGUE, // 通用角色对话
    MALE_DIALOGUE, // 男声角色对白
    FEMALE_DIALOGUE, // 女声角色对白
    ELDER_DIALOGUE // 长者/反派角色对白
}

/**
 * 分句实体（携带文本与角色属性）
 */
@Serializable
data class SentenceSegment(
    val text: String,
    val role: SegmentRole = SegmentRole.NARRATOR,
    val emotion: com.aitts.engine.rules.EmotionDetector.EmotionType = com.aitts.engine.rules.EmotionDetector.EmotionType.NEUTRAL
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
    val promptInstruction: String = "", // 大模型导演指令 / 提示词 (用于 MiMo、CosyVoice 等大模型调整情感、语速、音调及语境)
    val fallbackProviderId: String? = null, // 专属备用引擎（当主力接口遇到 429/503/超时时无缝自动降级）
    val maleVoiceId: String = "", // 多角色剧场：男主专属音色
    val femaleVoiceId: String = "", // 多角色剧场：女主专属音色
    val elderVoiceId: String = "", // 多角色剧场：长者/反派音色
    val tags: List<String> = emptyList() // 自定义标签分类（如 "玄幻男主", "知性女主", "悬疑解说" 等）
)

/**
 * 历史朗读记录与分析看板模型
 */
@Immutable
@Serializable
data class SpeechHistoryItem(
    val id: String,
    val text: String,
    val providerName: String,
    val voiceId: String,
    val costMs: Long,
    val characterCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isFallbackUsed: Boolean = false
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
    val fallbackProviderId: String = "edge_tts_default", // 主引擎异常时全局自动故障转移备用引擎
    val autoFallbackOnFailure: Boolean = true, // 启用自动故障降级
    val autoRetryOnFailure: Boolean = true, // 大模型 TTS 智能网络自愈抖动重试
    val hapticFeedbackEnabled: Boolean = true, // 触觉震动反馈开关
    val playbackNotificationEnabled: Boolean = true, // 启用后台朗读通知栏状态条与停止控制
    val ultraLowLatencyMode: Boolean = true, // 极速首字直出模式 (Sub-150ms 极低延迟响应)
    val isAcronymNormalizationEnabled: Boolean = true, // 智能英文缩写与专有名词发音规范化 (AI/CPU/WiFi/APP等)
    val isEmotionProsodyEnabled: Boolean = true, // 小说对白智能情感语气与大模型导演提示词动态注入
    val eqPresetId: String = "passthrough", // 音效 EQ 预设方案 (clear_voice, warm_broadcast, gentle_ear_protect, passthrough, custom)
    val voiceClarityBoostEnabled: Boolean = false, // 人声清晰度增强滤镜 (Clear Voice EQ)
    val loudnessGainFactor: Float = 1.0f, // 软件级响度增益与动态均衡 (1.0x ~ 2.0x)
    val sleepTimerMinutes: Int = 0, // 听书睡眠定时器 (分钟，0为关闭)
    val appUiStyle: String = "STUDIO" // 界面设计风格 (STUDIO: 全新沉浸式工作台, CLASSIC: 经典列表)
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
        ),
        ReplacementRule(
            id = "rule_html_entities",
            pattern = "&(?:nbsp|gt|lt|amp|quot);",
            replacement = " ",
            isRegex = true,
            description = "过滤小说网页导入残留的 HTML 转义符号"
        ),
        ReplacementRule(
            id = "rule_novel_chapter_end",
            pattern = "(?:\\(本章完\\)|（本章完）|PS[:：].*|求月票|求推荐票|求追读|作者有话说.*)",
            replacement = "",
            isRegex = true,
            description = "过滤章节末尾防盗广告与作者求月票打扰语"
        ),
        ReplacementRule(
            id = "rule_repeat_symbols",
            pattern = "[\\~\\-_=\\+]{3,}",
            replacement = "，",
            isRegex = true,
            description = "将小说中连续波浪线或横线转为自然停顿"
        ),
        ReplacementRule(
            id = "rule_yyds",
            pattern = "\\byyds\\b",
            replacement = "永远的神",
            isRegex = true,
            description = "网络缩写纠正：yyds ➔ 永远的神"
        ),
        ReplacementRule(
            id = "rule_u1s1",
            pattern = "\\bu1s1\\b",
            replacement = "有一说一",
            isRegex = true,
            description = "网络缩写纠正：u1s1 ➔ 有一说一"
        )
    )

    // 精选规则包 1：修仙玄幻高频字音与多音字校正包
    val xianxiaRulesPreset = listOf(
        ReplacementRule(id = "xianxia_dan_tian", pattern = "丹田", replacement = "单田", isRegex = false, description = "丹田 (dān tián)"),
        ReplacementRule(id = "xianxia_zhu_ji", pattern = "筑基", replacement = "住基", isRegex = false, description = "筑基 (zhù jī)"),
        ReplacementRule(id = "xianxia_jie_jie", pattern = "桀桀", replacement = "节节", isRegex = false, description = "反派怪笑：桀桀 (jié jié)"),
        ReplacementRule(id = "xianxia_chi_xiao", pattern = "嗤笑", replacement = "吃笑", isRegex = false, description = "嗤笑 (chī xiào)"),
        ReplacementRule(id = "xianxia_qian_kun", pattern = "乾坤", replacement = "前坤", isRegex = false, description = "乾坤 (qián kūn)"),
        ReplacementRule(id = "xianxia_shi_hai", pattern = "识海", replacement = "拾海", isRegex = false, description = "识海 (shí hǎi)"),
        ReplacementRule(id = "xianxia_dun_guang", pattern = "遁光", replacement = "盾光", isRegex = false, description = "遁光 (dùn guāng)"),
        ReplacementRule(id = "xianxia_kui_lei", pattern = "傀儡", replacement = "魁累", isRegex = false, description = "傀儡 (kuǐ lěi)"),
        ReplacementRule(id = "xianxia_gui_xi", pattern = "龟息", replacement = "归息", isRegex = false, description = "龟息功 (guī xī)"),
        ReplacementRule(id = "xianxia_zhi_gu", pattern = "桎梏", replacement = "至固", isRegex = false, description = "桎梏 (zhì gù)"),
        ReplacementRule(id = "xianxia_pi_ni", pattern = "睥睨", replacement = "辟逆", isRegex = false, description = "睥睨天下 (pì nì)"),
        ReplacementRule(id = "xianxia_cui_can", pattern = "璀璨", replacement = "翠灿", isRegex = false, description = "璀璨 (cuǐ càn)"),
        ReplacementRule(id = "xianxia_mo_da", pattern = "莫大", replacement = "墨大", isRegex = false, description = "莫大机缘 (mò)")
    )

    // 精选规则包 2：网络小说排版特殊符号与防盗乱码净化包
    val novelSymbolsPreset = listOf(
        ReplacementRule(id = "symbol_block", pattern = "[▓█■□▲▼◆◇★☆※●○◎▶▷]+", replacement = " ", isRegex = true, description = "清理章节排版装饰特殊方块与星号"),
        ReplacementRule(id = "symbol_anti_theft", pattern = "[【】〖〗\\[\\]\\{\\}「」『』]", replacement = " ", isRegex = true, description = "净化对话与特殊标题框括号"),
        ReplacementRule(id = "symbol_separator", pattern = "[-=_~*]{3,}", replacement = "，", isRegex = true, description = "转换章节分割线为自然逗号停顿"),
        ReplacementRule(id = "symbol_url", pattern = "(?:https?://|www\\.)[a-zA-Z0-9./?=_-]+", replacement = "", isRegex = true, description = "过滤章节内夹带的盗版网址链接")
    )

    // 精选规则包 3：现代科技与二次元专有名词发音包
    val techAcronymsPreset = listOf(
        ReplacementRule(id = "tech_ai", pattern = "\\bAI\\b", replacement = "A-I", isRegex = true, description = "AI ➔ A-I"),
        ReplacementRule(id = "tech_wifi", pattern = "\\bWiFi\\b", replacement = "W-i-F-i", isRegex = true, isCaseSensitive = false, description = "WiFi ➔ W-i-F-i"),
        ReplacementRule(id = "tech_cpu", pattern = "\\bCPU\\b", replacement = "C-P-U", isRegex = true, description = "CPU ➔ C-P-U"),
        ReplacementRule(id = "tech_gpu", pattern = "\\bGPU\\b", replacement = "G-P-U", isRegex = true, description = "GPU ➔ G-P-U"),
        ReplacementRule(id = "tech_npc", pattern = "\\bNPC\\b", replacement = "N-P-C", isRegex = true, description = "NPC ➔ N-P-C"),
        ReplacementRule(id = "tech_boss", pattern = "\\bBOSS\\b", replacement = "B-O-S-S", isRegex = true, isCaseSensitive = false, description = "BOSS ➔ B-O-S-S")
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
