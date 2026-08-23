package com.aitts.engine

import com.aitts.engine.rules.QuoteService
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteServiceTest {

    @Test
    fun testRandomLocalQuotes() {
        val quote1 = QuoteService.getRandomLocalQuote()
        assertNotNull(quote1.text)
        assertTrue(quote1.text.isNotBlank())
        assertTrue(quote1.category.isNotBlank())

        val novelQuote = QuoteService.getRandomLocalQuote("小说剧场")
        assertEquals("小说剧场", novelQuote.category)
        assertTrue(novelQuote.text.contains("“") || novelQuote.text.isNotEmpty())

        val newsQuote = QuoteService.getRandomLocalQuote("新闻播报")
        assertEquals("新闻播报", newsQuote.category)
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
