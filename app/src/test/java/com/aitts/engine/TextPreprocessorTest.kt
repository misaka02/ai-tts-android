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

    @Test
    fun testMarkdownAndHtmlCleaning() {
        val input = "<b>你好</b>，请访问 https://example.com/test **加粗文本**&nbsp;&amp;&nbsp;内容"
        val output = TextPreprocessor.process(input, emptyList())
        assertEquals("你好 ，请访问 网址链接 加粗文本 & 内容", output.trim())
    }

    @Test
    fun testChineseNumberNormalization() {
        val input1 = "请看第123章，更新于2026年，完成率达到了99.5%。"
        val output1 = TextPreprocessor.normalizeChineseNumbers(input1)
        assertEquals("请看第一百二十三章，更新于二零二六年，完成率达到了百分之九十九点五。", output1)

        val input2 = "第10节，第15集，第1名"
        val output2 = TextPreprocessor.normalizeChineseNumbers(input2)
        assertEquals("第十节，第十五集，第一名", output2)

        val input3 = "联系电话13800138000，时间是14:30，费用是¥100.5。"
        val output3 = TextPreprocessor.normalizeChineseNumbers(input3)
        assertEquals("联系电话幺三八零零幺三八零零零，时间是十四点三十分，费用是一百点五元。", output3)
    }
}
