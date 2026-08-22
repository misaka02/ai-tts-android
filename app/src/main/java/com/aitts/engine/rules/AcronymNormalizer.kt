package com.aitts.engine.rules

import java.util.regex.Pattern

/**
 * 智能中英混读 / 英文缩写与专有名词发音规整器：
 * 解决大模型与神经网络 TTS 在朗读网文/科技小说中的英文缩写（如 AI、CPU、WiFi、APP、NPC 等）时
 * 出现怪异音调、吞字、拼读错误或突兀切英文音色的问题。
 */
object AcronymNormalizer {

    // 常用固定缩写/专有名词映射表
    private val explicitMap = mapOf(
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

    // 通用 2~5 位纯大写字母缩写正则（如 NASA, FBI, CIA, MIT 等）
    private val generalAcronymPattern = Pattern.compile("(?<![a-zA-Z])([A-Z]{2,5})(?![a-zA-Z])")

    /**
     * 规范化文本中的英文缩写与专有名词
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return text

        var result = text

        // 1. 精准替换内置词表
        for ((target, replacement) in explicitMap) {
            val regex = "(?<![a-zA-Z])$target(?![a-zA-Z])"
            result = result.replace(Regex(regex), replacement)
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
