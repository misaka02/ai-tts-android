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
}
