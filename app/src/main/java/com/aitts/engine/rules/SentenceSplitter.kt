package com.aitts.engine.rules

import com.aitts.engine.data.SegmentRole
import com.aitts.engine.data.SentenceSegment
import java.util.ArrayDeque

/**
 * 智能长句切分算法 3.0 (Smart Novel Segmenter & Drama Casting Engine 3.0)：
 * 1. 采用平衡括号/引号栈 (Balanced Quote Stack) 状态机，精准解析多层嵌套引语与对白；
 * 2. 独创小说智能 4 角色声线分流 (旁白 / 男主 / 女主 / 长者反派)；
 * 3. 极速首字直出模式 (ultraLowLatencyMode)：首句微切分，首包 150ms 极速发音，后台并行流水线预取后续句子。
 */
object SentenceSplitter {

    private val SENTENCE_END_PUNCTUATIONS = charArrayOf(
        '。', '！', '？', '；', '\n', '\r',
        '!', '?', ';', '\u3002', '\uFF01', '\uFF1F', '\uFF1B'
    )

    private val SECONDARY_PUNCTUATIONS = charArrayOf(
        '，', ',', '、', '：', ':', '—', '-'
    )

    /**
     * 将文本切分为适合 AI 模型快速合成的短句列表
     */
    fun splitText(text: String, maxLength: Int = 80): List<String> {
        return splitBlockIntoSentences(text, maxLength)
    }

    /**
     * 智能多角色切分：将文本拆分为携带角色（旁白 / 男主 / 女主 / 长者）的切片列表
     */
    fun splitTextWithRoles(
        text: String,
        maxLength: Int = 80,
        ultraLowLatencyMode: Boolean = true
    ): List<SentenceSegment> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val rawBlocks = mutableListOf<SentenceSegment>()
        val quoteStack = ArrayDeque<Char>()
        var currentDialogueRole = SegmentRole.DIALOGUE
        var lastNarratorText = ""
        val currentBlock = StringBuilder()

        // 1. 第一阶段：利用平衡引号栈拆分为旁白块与多层对白块
        for (c in trimmed) {
            if (isOpeningQuote(c)) {
                if (quoteStack.isEmpty()) {
                    if (currentBlock.isNotEmpty()) {
                        lastNarratorText = currentBlock.toString().trim()
                        currentDialogueRole = detectSpeakerRole(lastNarratorText)
                        rawBlocks.add(SentenceSegment(lastNarratorText, SegmentRole.NARRATOR, EmotionDetector.EmotionType.NEUTRAL))
                        currentBlock.clear()
                    } else {
                        currentDialogueRole = SegmentRole.DIALOGUE
                    }
                }
                quoteStack.push(c)
                currentBlock.append(c)
            } else if (quoteStack.isNotEmpty() && isMatchingClosingQuote(quoteStack.peek()!!, c)) {
                quoteStack.pop()
                currentBlock.append(c)
                if (quoteStack.isEmpty()) {
                    val dialogText = currentBlock.toString().trim()
                    val emotion = EmotionDetector.detectEmotion(lastNarratorText, dialogText)
                    rawBlocks.add(SentenceSegment(dialogText, currentDialogueRole, emotion))
                    currentBlock.clear()
                }
            } else {
                currentBlock.append(c)
            }
        }

        if (currentBlock.isNotEmpty()) {
            val remaining = currentBlock.toString().trim()
            val role = if (quoteStack.isNotEmpty()) currentDialogueRole else SegmentRole.NARRATOR
            val emotion = if (quoteStack.isNotEmpty()) EmotionDetector.detectEmotion(lastNarratorText, remaining) else EmotionDetector.EmotionType.NEUTRAL
            rawBlocks.add(SentenceSegment(remaining, role, emotion))
        }

        // 2. 第二阶段：对每个块内部执行标点短句细分与平滑控制
        val finalResult = mutableListOf<SentenceSegment>()
        for ((idx, block) in rawBlocks.withIndex()) {
            if (block.text.isBlank()) continue
            // 极速首字直出模式：首句块优先采用短阈值，首包 150ms 极速发音，消除等待
            val effectiveMaxLength = if (ultraLowLatencyMode && idx == 0 && block.text.length > 18) {
                16
            } else {
                maxLength
            }
            val sentences = splitBlockIntoSentences(block.text, effectiveMaxLength)
            for (s in sentences) {
                if (s.isNotBlank()) {
                    finalResult.add(SentenceSegment(s, block.role, block.emotion))
                }
            }
        }

        return finalResult
    }

    private val ELDER_KEYWORDS = listOf("老者", "老头", "老汉", "老祖", "太上长老", "魔尊", "宗主", "掌门", "魔头", "老僧", "师尊", "前辈")
    private val FEMALE_KEYWORDS = listOf("她", "少女", "姑娘", "女子", "师妹", "师姐", "娘亲", "夫人", "小姐", "公主", "丫头", "仙子", "神女", "母后", "美妇", "皇后", "女帝", "妹妹", "姐姐", "老妇", "大娘")
    private val MALE_KEYWORDS = listOf("他", "少年", "青年", "男子", "师兄", "师弟", "皇帝", "陛下", "兄弟", "父亲", "爹", "大哥", "弟弟", "冷笑", "喝道", "哼道", "怒喝")

    private fun detectSpeakerRole(precedingText: String): SegmentRole {
        val tail = precedingText.takeLast(16)
        if (ELDER_KEYWORDS.any { tail.contains(it) }) {
            return SegmentRole.ELDER_DIALOGUE
        }
        if (FEMALE_KEYWORDS.any { tail.contains(it) }) {
            return SegmentRole.FEMALE_DIALOGUE
        }
        if (MALE_KEYWORDS.any { tail.contains(it) }) {
            return SegmentRole.MALE_DIALOGUE
        }
        return SegmentRole.DIALOGUE
    }

    private fun splitBlockIntoSentences(text: String, maxLength: Int): List<String> {
        val result = mutableListOf<String>()
        val currentChunk = StringBuilder()

        var i = 0
        while (i < text.length) {
            val c = text[i]
            currentChunk.append(c)

            val isPrimaryEnd = c in SENTENCE_END_PUNCTUATIONS
            val isSecondaryEnd = c in SECONDARY_PUNCTUATIONS && currentChunk.length >= maxLength / 2
            val isOverLength = currentChunk.length >= maxLength

            if (isPrimaryEnd || isSecondaryEnd || isOverLength) {
                while (i + 1 < text.length && isClosingQuote(text[i + 1])) {
                    i++
                    currentChunk.append(text[i])
                }

                val sentence = currentChunk.toString().trim()
                if (sentence.isNotEmpty()) {
                    result.add(sentence)
                }
                currentChunk.clear()
            }
            i++
        }

        if (currentChunk.isNotEmpty()) {
            val remaining = currentChunk.toString().trim()
            if (remaining.isNotEmpty()) {
                result.add(remaining)
            }
        }

        return result
    }

    private fun isOpeningQuote(c: Char): Boolean {
        return c == '“' || c == '‘' || c == '「' || c == '『' || c == '"' || c == '\''
    }

    private fun isMatchingClosingQuote(open: Char, close: Char): Boolean {
        return when (open) {
            '“' -> close == '”'
            '‘' -> close == '’'
            '「' -> close == '」'
            '『' -> close == '』'
            '"' -> close == '"'
            '\'' -> close == '\''
            else -> isClosingQuote(close)
        }
    }

    private fun isClosingQuote(c: Char): Boolean {
        return c == '”' || c == '’' || c == '」' || c == '』' || c == '"' || c == '\'' || c == '）' || c == ')'
    }
}
