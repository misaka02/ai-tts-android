package com.aitts.engine

import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ReplacementRule
import com.aitts.engine.rules.TextPreprocessor
import org.junit.Assert.assertEquals
import org.junit.Test

class TextPreprocessorTest {

    @Test
    fun testDefaultRulesProcessing() {
        val input = "他在【重庆】的银行工作……"
        val rules = PresetConfigs.defaultRules
        val output = TextPreprocessor.process(input, rules)

        // 验证多音字替换和特殊符号清洗
        assertEquals("他在 崇庆 的银航工作，", output)
    }

    @Test
    fun testCustomRegexRule() {
        val rules = listOf(
            ReplacementRule(
                id = "test_rule",
                pattern = "第([0-9]+)卷",
                replacement = "卷$1",
                isRegex = true
            )
        )
        val input = "请翻到第12卷进行查看"
        val output = TextPreprocessor.process(input, rules)
        assertEquals("请翻到卷12进行查看", output)
    }

    @Test
    fun testDisabledRule() {
        val rules = listOf(
            ReplacementRule(
                id = "test_rule",
                pattern = "测试",
                replacement = "成功",
                isRegex = false,
                enabled = false
            )
        )
        val input = "这是一个测试"
        val output = TextPreprocessor.process(input, rules)
        assertEquals("这是一个测试", output)
    }
}
