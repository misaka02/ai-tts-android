package com.aitts.engine

import com.aitts.engine.rules.AcronymNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class AcronymNormalizerTest {

    @Test
    fun testEmptyOrBlank() {
        assertEquals("", AcronymNormalizer.normalize(""))
        assertEquals("   ", AcronymNormalizer.normalize("   "))
    }

    @Test
    fun testCommonTechAcronyms() {
        val input = "这个系统基于AI技术，搭载强大的CPU与GPU，支持WiFi连接。"
        val expected = "这个系统基于A-I技术，搭载强大的C-P-U与G-P-U，支持W-i-F-i连接。"
        assertEquals(expected, AcronymNormalizer.normalize(input))
    }

    @Test
    fun testNovelGamingTerms() {
        val input = "主角打开APP，发现了副本中的隐藏BOSS与神秘NPC，画面拥有4K和3D效果。"
        val expected = "主角打开A-P-P，发现了副本中的隐藏B-O-S-S与神秘N-P-C，画面拥有四K和三D效果。"
        assertEquals(expected, AcronymNormalizer.normalize(input))
    }

    @Test
    fun testNoAcronymUntouched() {
        val input = "这是一段完全没有英文缩写的普通中文文本。"
        assertEquals(input, AcronymNormalizer.normalize(input))
    }

    @Test
    fun testSpecialAcronymsWithDotAndDash() {
        val input = "我们使用了GPT-3.5和GPT-4o模型，通过USB-C接口传输数据。"
        val expected = "我们使用了G-P-T-三点五和G-P-T-四-o模型，通过U-S-B-C接口传输数据。"
        assertEquals(expected, AcronymNormalizer.normalize(input))
    }
}
