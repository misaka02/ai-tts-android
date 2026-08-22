package com.aitts.engine.rules

/**
 * 小说对话智能情感语气识别器 (Dialogue Emotion & Tone Prosody Enhancer)：
 * 根据对话前后的引述语、动词与标点语境，智能提取角色所处的情感状态，
 * 并在大模型（如小米 MiMo、Gemini、OpenAI、SiliconFlow）合成时动态注入导演控制提示词。
 */
object EmotionDetector {

    enum class EmotionType(val displayName: String, val promptInstruction: String) {
        ANGRY("愤怒冷酷", "【语气要求：极其愤怒激昂、语气冷酷严厉带有压迫感】"),
        SAD("哀伤哽咽", "【语气要求：语气哀伤低沉、带有轻微哭腔与叹息】"),
        FEARFUL("惊恐紧迫", "【语气要求：情绪惊恐慌乱、呼吸急促带有急迫颤抖感】"),
        GENTLE("娇柔温婉", "【语气要求：语气轻柔温婉、声线甜美细腻带有柔情】"),
        WHISPER("悄声耳语", "【语气要求：压低声线、悄声耳语带有神秘感】"),
        EXCITED("激动狂喜", "【语气要求：情绪极其激动兴奋、语调高昂充满狂喜】"),
        NEUTRAL("自然生动", "")
    }

    private val ANGRY_KEYWORDS = listOf(
        "愤怒", "怒吼", "咆哮", "暴怒", "冷笑", "冷喝", "厉喝", "大怒", "杀意", "冰冷",
        "怒斥", "咬牙切齿", "阴沉", "厉声", "怒道", "喝道", "滚", "找死"
    )

    private val SAD_KEYWORDS = listOf(
        "哭泣", "抽泣", "哽咽", "叹息", "落泪", "哀求", "凄凉", "悲痛", "绝望", "委屈",
        "轻叹", "叹道", "泪流满面", "心碎", "哽住"
    )

    private val FEARFUL_KEYWORDS = listOf(
        "惊呼", "骇然", "惊骇", "颤抖", "尖叫", "失声", "慌乱", "倒吸凉气", "面色煞白",
        "大惊", "惊恐", "战战兢兢", "恐惧", "骇然失色", "发抖", "发颤", "瑟瑟"
    )

    private val GENTLE_KEYWORDS = listOf(
        "温柔", "轻柔", "娇嗔", "娇羞", "软糯", "甜美", "羞涩", "含羞", "轻声细语",
        "呢喃", "撒娇", "柔声", "浅笑", "温言", "轻唤"
    )

    private val WHISPER_KEYWORDS = listOf(
        "低语", "耳语", "悄声", "附耳", "神秘", "压低声音", "暗中", "窃窃私语",
        "小声", "偷偷", "低声道", "私语"
    )

    private val EXCITED_KEYWORDS = listOf(
        "狂喜", "大笑", "兴奋", "激动", "惊喜", "狂热", "欢呼", "哈哈大笑", "跃跃欲试",
        "欣喜", "若狂", "雀跃", "大喜"
    )

    /**
     * 智能识别引述语中的情感类型
     * @param precedingText 引述前文（例如：“少女眼眶泛红，哽咽着说道：”）
     * @param dialogueText 引述对话内容（例如：“你真的要走吗？”）
     */
    fun detectEmotion(precedingText: String, dialogueText: String = ""): EmotionType {
        val tailContext = precedingText.takeLast(24)

        if (ANGRY_KEYWORDS.any { tailContext.contains(it) }) return EmotionType.ANGRY
        if (SAD_KEYWORDS.any { tailContext.contains(it) }) return EmotionType.SAD
        if (FEARFUL_KEYWORDS.any { tailContext.contains(it) }) return EmotionType.FEARFUL
        if (GENTLE_KEYWORDS.any { tailContext.contains(it) }) return EmotionType.GENTLE
        if (WHISPER_KEYWORDS.any { tailContext.contains(it) }) return EmotionType.WHISPER
        if (EXCITED_KEYWORDS.any { tailContext.contains(it) }) return EmotionType.EXCITED

        // 对话文本内含强烈叹号或特殊叹词分析
        if (dialogueText.endsWith("！！！") || dialogueText.endsWith("!！")) {
            return EmotionType.EXCITED
        }

        return EmotionType.NEUTRAL
    }
}
