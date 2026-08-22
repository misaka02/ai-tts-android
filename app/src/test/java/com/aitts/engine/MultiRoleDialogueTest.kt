package com.aitts.engine

import com.aitts.engine.data.SegmentRole
import com.aitts.engine.rules.SentenceSplitter
import org.junit.Assert.assertEquals
import org.junit.Test

class MultiRoleDialogueTest {

    @Test
    fun testMaleSpeakerDetection() {
        val input = "少年冷笑一声：“就凭你也配与我一战？”"
        val segments = SentenceSplitter.splitTextWithRoles(input, maxLength = 80, ultraLowLatencyMode = false)
        assertEquals(2, segments.size)
        assertEquals(SegmentRole.NARRATOR, segments[0].role)
        assertEquals(SegmentRole.MALE_DIALOGUE, segments[1].role)
    }

    @Test
    fun testFemaleSpeakerDetection() {
        val input = "少女抿嘴轻笑：“林动哥哥，你可真厉害。”"
        val segments = SentenceSplitter.splitTextWithRoles(input, maxLength = 80, ultraLowLatencyMode = false)
        assertEquals(2, segments.size)
        assertEquals(SegmentRole.NARRATOR, segments[0].role)
        assertEquals(SegmentRole.FEMALE_DIALOGUE, segments[1].role)
    }

    @Test
    fun testGenericSpeakerFallback() {
        val input = "四周一片寂静，突然有人喊道：“小心身后！”"
        val segments = SentenceSplitter.splitTextWithRoles(input, maxLength = 80, ultraLowLatencyMode = false)
        assertEquals(2, segments.size)
        assertEquals(SegmentRole.NARRATOR, segments[0].role)
        assertEquals(SegmentRole.DIALOGUE, segments[1].role)
    }
}
