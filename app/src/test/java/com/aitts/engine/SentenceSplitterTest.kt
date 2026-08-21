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
    fun testMaxLengthFallbackSplitting() {
        val longNoPunctuation = "这是一个非常长非常长非常长非常长非常长非常长非常长非常长非常长非常长非常长非常长非常长的连续文本没有任何主要标点符号"
        val result = SentenceSplitter.splitText(longNoPunctuation, maxLength = 20)
        assertTrue(result.size > 1)
        for (chunk in result) {
            assertTrue(chunk.isNotEmpty())
        }
    }
}
