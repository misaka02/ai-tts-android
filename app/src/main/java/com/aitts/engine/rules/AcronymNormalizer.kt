package com.aitts.engine.rules

import java.util.regex.Pattern

/**
 * 智能中英混读 / 英文缩写与专有名词发音规整器：
 * 解决大模型与神经网络 TTS 在朗读网文/科技小说中的英文缩写（如 AI、CPU、WiFi、APP、NPC、GPT-4o 等）时
 * 出现怪异音调、吞字、拼读错误或突兀切英文音色的问题。
 */
object AcronymNormalizer {

    // 常用固定缩写/专有名词映射表
    private val explicitMap = mapOf(
        "GPT-4o" to "G-P-T-四-o",
        "GPT-4" to "G-P-T-四",
        "GPT-3.5" to "G-P-T-三点五",
        "ChatGPT" to "Chat-G-P-T",
        "USB-C" to "U-S-B-C",
        "Type-C" to "Type-C",
        "1080P" to "一零八零P",
        "1080p" to "一零八零p",
        "720P" to "七二零P",
        "720p" to "七二零p",
        "2160P" to "二一六零P",
        "WiFi 6" to "W-i-F-i-六",
        "IPv4" to "I-P-v-四",
        "IPv6" to "I-P-v-六",
        "JSON" to "J-S-O-N",
        "SDK" to "S-D-K",
        "AI" to "A-I",
        "Ai" to "A-I",
        "APP" to "A-P-P",
        "App" to "A-P-P",
        "app" to "A-P-P",
        "WiFi" to "W-i-F-i",
        "wifi" to "W-i-F-i",
        "WIFI" to "W-i-F-i",
        "CPU" to "C-P-U",
        "GPU" to "G-P-U",
        "NPU" to "N-P-U",
        "TPU" to "T-P-U",
        "USB" to "U-S-B",
        "PDF" to "P-D-F",
        "GPS" to "G-P-S",
        "VIP" to "V-I-P",
        "KFC" to "K-F-C",
        "API" to "A-P-I",
        "TTS" to "T-T-S",
        "LLM" to "L-L-M",
        "NPC" to "N-P-C",
        "PK" to "P-K",
        "CP" to "C-P",
        "HP" to "H-P",
        "MP" to "M-P",
        "EXP" to "E-X-P",
        "CD" to "C-D",
        "BUG" to "B-U-G",
        "Bug" to "B-U-G",
        "bug" to "B-U-G",
        "BOSS" to "B-O-S-S",
        "Boss" to "B-O-S-S",
        "boss" to "B-O-S-S",
        "PC" to "P-C",
        "RAM" to "R-A-M",
        "ROM" to "R-O-M",
        "SSD" to "S-S-D",
        "HDD" to "H-D-D",
        "URL" to "U-R-L",
        "HTML" to "H-T-M-L",
        "HTTP" to "H-T-T-P",
        "HTTPS" to "H-T-T-P-S",
        "DNS" to "D-N-S",
        "IP" to "I-P",
        "ID" to "I-D",
        "OS" to "O-S",
        "iOS" to "i-O-S",
        "UI" to "U-I",
        "FPS" to "F-P-S",
        "3D" to "三D",
        "4K" to "四K",
        "2K" to "二K",
        "8K" to "八K",
        "5G" to "五G",
        "4G" to "四G",
        "3G" to "三G"
    )

    // 预编译内置词表正则列表 (带特殊字符安全转义，杜绝反复构造正则对象的堆内存颠簸)
    private val compiledExplicitRules: List<Pair<Pattern, String>> = explicitMap.map { (target, replacement) ->
        val quoted = Pattern.quote(target)
        val pattern = Pattern.compile("(?<![a-zA-Z])$quoted(?![a-zA-Z])")
        pattern to replacement
    }

    // 通用 2~5 位纯大写字母缩写正则（如 NASA, FBI, CIA, MIT 等）
    private val generalAcronymPattern = Pattern.compile("(?<![a-zA-Z])([A-Z]{2,5})(?![a-zA-Z])")

    /**
     * 规范化文本中的英文缩写与专有名词
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return text

        var result = text

        // 1. 精准替换内置词表 (预编译 Pattern，零内存开销)
        for ((pattern, replacement) in compiledExplicitRules) {
            val matcher = pattern.matcher(result)
            if (matcher.find()) {
                result = matcher.replaceAll(replacement)
            }
        }

        // 2. 通用大写缩写拆分为字母连读（如 FBI -> F-B-I）
        val matcher = generalAcronymPattern.matcher(result)
        val sb = StringBuffer()
        while (matcher.find()) {
            val acronym = matcher.group(1) ?: ""
            if (!acronym.contains("-")) {
                val hyphenated = acronym.map { it }.joinToString("-")
                matcher.appendReplacement(sb, hyphenated)
            } else {
                matcher.appendReplacement(sb, acronym)
            }
        }
        matcher.appendTail(sb)

        return sb.toString()
    }
}
