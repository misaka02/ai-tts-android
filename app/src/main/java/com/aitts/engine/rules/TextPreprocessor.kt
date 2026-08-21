package com.aitts.engine.rules

import com.aitts.engine.data.ReplacementRule
import java.util.regex.Pattern

/**
 * 文本预处理器：
 * 1. 执行用户配置的正则发音替换规则
 * 2. 规范化特殊字符与排版标点
 * 3. 过滤非法或不可读控制字符
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

        // 1. 基础排版规范化（去除多余空白字符、控制符）
        result = normalizeWhitespace(result)

        // 2. 依次应用启用的自定义规则
        for (rule in rules) {
            if (!rule.enabled || rule.pattern.isBlank()) continue
            result = applyRule(result, rule)
        }

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
        // 替换不可见控制字符，合并连续空白
        return input.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .replace(Regex("[ \\t]+"), " ")
    }
}
