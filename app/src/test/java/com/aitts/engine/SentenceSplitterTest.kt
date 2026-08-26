package com.aitts.engine

import com.aitts.engine.rules.SentenceSplitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceSplitterTest {

    @Test
    fun testEmptyAndBlankText() {
        assertEquals(emptyList<String>(), SentenceSplitter.splitText(""))
        assertEquals(emptyList<String>(), SentenceSplitter.splitText("   \n\t  "))
    }

    @Test
    fun testShortSentenceNotSplit() {
        val input = "这是一句很短的测试文本。"
        val result = SentenceSplitter.splitText(input, maxLength = 80)
        assertEquals(listOf(input), result)
    }

    @Test
    fun testMultiSentenceSplitting() {
        val input = "第一句话很有趣！第二句话很精彩？第三句话则是结论。"
        val result = SentenceSplitter.splitText(input, maxLength = 80)
        assertEquals(3, result.size)
        assertEquals("第一句话很有趣！", result[0])
        assertEquals("第二句话很精彩？", result[1])
        assertEquals("第三句话则是结论。", result[2])
    }

    @Test
    fun testQuotationMarkPreservation() {
        val input = "他转过头说道：“我们必须立刻出发！”随后快步向前走去。"
        val result = SentenceSplitter.splitText(input, maxLength = 80)
        assertEquals(2, result.size)
        assertEquals("他转过头说道：“我们必须立刻出发！”", result[0])
        assertEquals("随后快步向前走去。", result[1])
    }

    @Test
    fun testMultiRoleSplitting() {
        val input = "他转过头说道：“我们必须立刻出发！”随后快步向前走去。"
        val segments = SentenceSplitter.splitTextWithRoles(input, maxLength = 80, ultraLowLatencyMode = false)
        assertEquals(3, segments.size)
        assertEquals("他转过头说道：", segments[0].text)
        assertEquals(com.aitts.engine.data.SegmentRole.NARRATOR, segments[0].role)

        assertEquals("“我们必须立刻出发！”", segments[1].text)
        assertEquals(com.aitts.engine.data.SegmentRole.MALE_DIALOGUE, segments[1].role)

        assertEquals("随后快步向前走去。", segments[2].text)
        assertEquals(com.aitts.engine.data.SegmentRole.NARRATOR, segments[2].role)
    }

    @Test
    fun testUltraLowLatencySplitting() {
        val input = "在很久很久以前的大陆上，生活着一群勇敢的冒险家。他们翻山越岭寻找宝藏。"
        val segments = SentenceSplitter.splitTextWithRoles(input, maxLength = 80, ultraLowLatencyMode = true)
        assertTrue(segments.isNotEmpty())
        // 首句被微切分，保证首包极低延迟
        assertTrue(segments[0].text.length <= 16)
    }

    @Test
    fun testFineRulesParagraphMode() {
        val input = "第一自然段内容，讲述背景。\n\n第二自然段内容，推动情节发展。\n第三自然段内容，总结篇章。"
        val segments = SentenceSplitter.splitTextWithFineRules(
            text = input,
            mode = "PARAGRAPH",
            mergeShort = false,
            splitLong = false
        )
        assertEquals(3, segments.size)
        assertEquals("第一自然段内容，讲述背景。", segments[0].text)
        assertEquals("第二自然段内容，推动情节发展。", segments[1].text)
        assertEquals("第三自然段内容，总结篇章。", segments[2].text)
    }

    @Test
    fun testFineRulesShortParagraphMerge() {
        val input = "短段一。\n短段二。\n这是一个比较长的正常段落，字数超过了短段落阈值，不会被合并到一起。"
        val segments = SentenceSplitter.splitTextWithFineRules(
            text = input,
            mode = "PARAGRAPH",
            mergeShort = true,
            minMergeLen = 15,
            splitLong = false
        )
        assertEquals(2, segments.size)
        assertEquals("短段一。\n短段二。", segments[0].text)
        assertTrue(segments[1].text.startsWith("这是一个比较长的正常段落"))
    }

    @Test
    fun testFineRulesLongParagraphSplitByPunctuation() {
        val longParagraph = "这是第一句话，描述春天的风景。这是第二句话，讲述鸟儿在歌唱！这是第三句话，太阳高高升起？这是第四句话，微风轻拂面庞。"
        val segments = SentenceSplitter.splitTextWithFineRules(
            text = longParagraph,
            mode = "PARAGRAPH",
            mergeShort = false,
            splitLong = true,
            maxLen = 30 // 强制触发拆分
        )
        // 验证每一段拆分都是以完整的句号/感叹号/问号结尾，绝不硬截断
        assertTrue(segments.size >= 2)
        for (seg in segments) {
            val lastChar = seg.text.last()
            assertTrue(lastChar == '。' || lastChar == '！' || lastChar == '？' || lastChar == '!' || lastChar == '?')
        }
    }
}
