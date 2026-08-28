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

    @Test
    fun testYearRegexLookbehindProtection() {
        val input = "他闭关修炼了10000年，终于在2026年飞升出关。"
        val output = TextPreprocessor.normalizeChineseNumbers(input)
        assertEquals("他闭关修炼了一万年，终于在二零二六年飞升出关。", output)
    }

    @Test
    fun testConcurrentRegexProcessing() {
        val rules = listOf(
            ReplacementRule(id = "r1", pattern = "(\\d+)米", replacement = "$1公尺", isRegex = true),
            ReplacementRule(id = "r2", pattern = "第(\\d+)集", replacement = "Episode $1", isRegex = true),
            ReplacementRule(id = "r3", pattern = "测试", replacement = "TEST", isRegex = false)
        )
        val threads = (1..16).map { threadIdx ->
            Thread {
                for (i in 0 until 100) {
                    val res = TextPreprocessor.process("这是第${i}集测试，跑了${i * 10}米", rules)
                    assert(res.contains("Episode"))
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}
