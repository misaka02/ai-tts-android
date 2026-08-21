package com.aitts.engine.rules

import com.aitts.engine.data.ReplacementRule
import java.util.regex.Pattern

/**
 * 文本预处理器：
 * 1. 执行用户配置的正则发音替换规则
 * 2. 规范化特殊字符与排版标点
 * 3. 过滤不可读控制字符、Markdown 噪音与 HTML 网页标签
 */
object TextPreprocessor {

    // 预编译正则缓存
    private val regexCache = mutableMapOf<String, Regex>()

    /**
     * 对文本应用全量清洗与规则替换
     */
    fun process(text: String, rules: List<ReplacementRule>): String {
        if (text.isBlank()) return ""

        var result = text

        // 1. 清洗 HTML/Markdown 与网络链接
        result = cleanMarkupAndFormatting(result)

        // 2. 依次应用启用的自定义规则
        for (rule in rules) {
            if (!rule.enabled || rule.pattern.isBlank()) continue
            result = applyRule(result, rule)
        }

        // 3. 规范化空白与控制字符
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
        return res
    }
}
