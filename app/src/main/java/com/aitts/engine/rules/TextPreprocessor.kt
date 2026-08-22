package com.aitts.engine.rules

import com.aitts.engine.data.ReplacementRule

/**
 * 文本预处理器：
 * 1. 执行用户配置的正则发音替换规则
 * 2. 规范化特殊字符与排版标点
 * 3. 过滤不可读控制字符、Markdown 噪音与 HTML 网页标签
 * 4. 智能中文数字与章节编号发音转换 (如 "第123章" -> "第一百二十三章", "2026年" -> "二零二六年")
 */
object TextPreprocessor {

    // 预编译正则缓存
    private val regexCache = mutableMapOf<String, Regex>()

    private val digitChars = charArrayOf('零', '一', '二', '三', '四', '五', '六', '七', '八', '九')
    private val units = arrayOf("", "十", "百", "千")
    private val bigUnits = arrayOf("", "万", "亿")

    /**
     * 对文本应用全量清洗与规则替换
     */
    fun process(text: String, rules: List<ReplacementRule>, enableNumberNormalization: Boolean = true): String {
        if (text.isBlank()) return ""

        var result = text

        // 1. 清洗 HTML/Markdown 与网络链接
        result = cleanMarkupAndFormatting(result)

        // 2. 优先应用用户自定义替换规则 (用户规则具备最高优先级)
        for (rule in rules) {
            if (!rule.enabled || rule.pattern.isBlank()) continue
            result = applyRule(result, rule)
        }

        // 3. 智能中文数字与章节转换 (可选)
        if (enableNumberNormalization) {
            result = normalizeChineseNumbers(result)
        }

        // 4. 规范化空白与控制字符
        result = normalizeWhitespace(result)

        return result
    }

    private fun applyRule(input: String, rule: ReplacementRule): String {
        return try {
            if (rule.isRegex) {
                val cacheKey = "${rule.pattern}_${rule.isCaseSensitive}"
                val regex = regexCache.getOrPut(cacheKey) {
                    val options = if (rule.isCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    Regex(rule.pattern, options)
                }
                regex.replace(input, rule.replacement)
            } else {
                input.replace(rule.pattern, rule.replacement, ignoreCase = !rule.isCaseSensitive)
            }
        } catch (e: Exception) {
            input
        }
    }

    private fun normalizeWhitespace(input: String): String {
        return input.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .replace(Regex("[ \\t]+"), " ")
            .trim()
    }

    /**
     * 清理 Markdown/HTML 及网址噪音，避免 TTS 朗读符号
     */
    private fun cleanMarkupAndFormatting(input: String): String {
        var res = input
        // 过滤 HTML 标签
        if (res.contains("<") && res.contains(">")) {
            res = res.replace(Regex("<[^>]+>"), " ")
        }
        // 过滤 HTML 转义符
        if (res.contains("&")) {
            res = res.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
        }
        // 清洗 Markdown 加粗与斜体等符号
        if (res.contains("**") || res.contains("__")) {
            res = res.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                .replace(Regex("__(.*?)__"), "$1")
        }
        // 清洗超链接
        if (res.contains("http://") || res.contains("https://")) {
            res = res.replace(Regex("https?://\\S+"), " 网址链接 ")
        }
        // 清洗零宽字符与特殊乱码空格
        res = res.replace(Regex("[\\u200B-\\u200F\\uFEFF\\u00A0]"), " ")

        // 清洗重复标点（如 ？？？ -> ？，！！！ -> ！）
        res = res.replace(Regex("？{2,}"), "？")
            .replace(Regex("！{2,}"), "！")
            .replace(Regex("，{2,}"), "，")

        // 清洗长省略号为自然呼吸顿号，防止 TTS 发出怪音
        res = res.replace(Regex("…{2,}"), "，")
            .replace(Regex("\\.{3,}"), "，")

        return res
    }

    /**
     * 智能中文数字与章节编号发音转换
     */
    fun normalizeChineseNumbers(input: String): String {
        var res = input

        // 1. 章节与序数转换：如 "第123章" -> "第一百二十三章"
        val ordinalRegex = Regex("第(\\d{1,8})([章节卷回集页名次天年条步把位段])")
        res = ordinalRegex.replace(res) { match ->
            val numStr = match.groupValues[1]
            val suffix = match.groupValues[2]
            val chineseNum = convertIntToChinese(numStr.toLongOrNull() ?: 0)
            "第$chineseNum$suffix"
        }

        // 2. 年份转换：如 "2026年" -> "二零二六年"
        val yearRegex = Regex("(\\d{4})年")
        res = yearRegex.replace(res) { match ->
            val digits = match.groupValues[1]
            val chineseDigits = digits.map { digitChars[it - '0'] }.joinToString("")
            "${chineseDigits}年"
        }

        // 3. 百分比转换：如 "50%" -> "百分之五十", "99.5%" -> "百分之九十九点五"
        val percentRegex = Regex("(\\d+(?:\\.\\d+)?)%")
        res = percentRegex.replace(res) { match ->
            val numStr = match.groupValues[1]
            if (numStr.contains(".")) {
                val parts = numStr.split(".")
                val intPart = convertIntToChinese(parts[0].toLongOrNull() ?: 0)
                val decPart = parts[1].map { digitChars[it - '0'] }.joinToString("")
                "百分之${intPart}点$decPart"
            } else {
                val chineseNum = convertIntToChinese(numStr.toLongOrNull() ?: 0)
                "百分之$chineseNum"
            }
        }

        return res
    }

    /**
     * 将整数转换为自然发音的中文数字 (123 -> 一百二十三, 10 -> 十, 15 -> 十五)
     */
    fun convertIntToChinese(number: Long): String {
        if (number == 0L) return "零"
        if (number < 0) return "负" + convertIntToChinese(-number)

        val digits = number.toString()
        val len = digits.length
        val sb = StringBuilder()
        var needZero = false

        for (i in 0 until len) {
            val d = digits[i] - '0'
            val pos = len - 1 - i
            val unitIdx = pos % 4
            val bigUnitIdx = pos / 4

            if (d == 0) {
                if (sb.isNotEmpty() && !needZero) {
                    needZero = true
                }
            } else {
                if (needZero) {
                    sb.append('零')
                    needZero = false
                }
                sb.append(digitChars[d])
                sb.append(units[unitIdx])
            }

            if (pos % 4 == 0 && bigUnitIdx > 0) {
                if (sb.isNotEmpty() && !sb.endsWith(bigUnits[bigUnitIdx])) {
                    sb.append(bigUnits[bigUnitIdx])
                }
                needZero = false
            }
        }

        val res = sb.toString().trimEnd('零')
        return if (res.startsWith("一十") && len == 2) res.substring(1) else res
    }
}
