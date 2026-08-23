package com.aitts.engine

import com.aitts.engine.rules.EmotionDetector
import com.aitts.engine.rules.SentenceSplitter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmotionDetectorTest {

    @Test
    fun testAngerDetection() {
        val angerQuotes = listOf(
            "他愤怒地咆哮道",
            "林枫怒吼一声",
            "她厉声喝道",
            "长老勃然大怒"
        )
        for (q in angerQuotes) {
            assertEquals("Should detect ANGRY for: $q", EmotionDetector.EmotionType.ANGRY, EmotionDetector.detectEmotion(q))
        }
    }

    @Test
    fun testSorrowDetection() {
        val sorrowQuotes = listOf(
            "她哭泣着说道",
            "少女抽泣着低语",
            "他哽咽着倾诉",
            "他绝望地喊道"
        )
        for (q in sorrowQuotes) {
            assertEquals("Should detect SAD for: $q", EmotionDetector.EmotionType.SAD, EmotionDetector.detectEmotion(q))
        }
    }

    @Test
    fun testFearDetection() {
        val fearQuotes = listOf(
            "他惊恐地尖叫",
            "守卫瑟瑟发抖地说道",
            "士兵慌乱地喊道"
        )
        for (q in fearQuotes) {
            assertEquals("Should detect FEARFUL for: $q", EmotionDetector.EmotionType.FEARFUL, EmotionDetector.detectEmotion(q))
        }
    }

    @Test
    fun testTenderDetection() {
        val tenderQuotes = listOf(
            "少女娇羞地说道",
            "她温柔地轻唤",
            "妻子撒娇着说"
        )
        for (q in tenderQuotes) {
            assertEquals("Should detect GENTLE for: $q", EmotionDetector.EmotionType.GENTLE, EmotionDetector.detectEmotion(q))
        }
    }

    @Test
    fun testWhisperDetection() {
        val whisperQuotes = listOf(
            "他悄声耳语道",
            "同伴压低声音说道",
            "密探窃窃私语"
        )
        for (q in whisperQuotes) {
            assertEquals("Should detect WHISPER for: $q", EmotionDetector.EmotionType.WHISPER, EmotionDetector.detectEmotion(q))
        }
    }

    @Test
    fun testJoyDetection() {
        val joyQuotes = listOf(
            "众人欢呼雀跃道",
            "他狂喜大笑道",
            "少年欣喜若狂"
        )
        for (q in joyQuotes) {
            assertEquals("Should detect EXCITED for: $q", EmotionDetector.EmotionType.EXCITED, EmotionDetector.detectEmotion(q))
        }
    }

    @Test
    fun testColdArrogantAndConfusedDetection() {
        val coldQuote = "魔尊漠然冷笑道"
        assertEquals(EmotionDetector.EmotionType.COLD_ARROGANT, EmotionDetector.detectEmotion(coldQuote))

        val confusedQuote = "弟子疑惑不解地问道"
        assertEquals(EmotionDetector.EmotionType.CONFUSED_INQUIRING, EmotionDetector.detectEmotion(confusedQuote))
    }

    @Test
    fun testIntegratedSentenceSplittingWithEmotion() {
        val input = "他愤怒地咆哮道：“谁敢动我妹妹一根汗毛！”随后拔剑出鞘。"
        val segments = SentenceSplitter.splitTextWithRoles(input)
        assertEquals(3, segments.size)
        assertEquals("“谁敢动我妹妹一根汗毛！”", segments[1].text)
        assertEquals(EmotionDetector.EmotionType.ANGRY, segments[1].emotion)
    }
}
