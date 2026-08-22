package com.aitts.engine.rules

import com.aitts.engine.data.SegmentRole
import com.aitts.engine.data.SentenceSegment

/**
 * 智能长句切分算法：
 * 1. 将长段落按自然标点符号（。！？；!?;\n等）切分成短句，支持引号保护与最大长度阈值；
 * 2. 独创小说智能多角色分句 (splitTextWithRoles)：自动识别引号内的角色对话与引号外的旁白，实现有声书双音色协同播报。
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
     * 将文本切分为适合 AI 模型快速合成的短句列表（保持标点与引号完整性）
     */
    fun splitText(text: String, maxLength: Int = 80): List<String> {
        return splitBlockIntoSentences(text, maxLength)
    }

    /**
     * 智能多角色切分：将文本拆分为携带角色（旁白 / 对话）的切片列表
     */
    fun splitTextWithRoles(text: String, maxLength: Int = 80): List<SentenceSegment> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val rawBlocks = mutableListOf<SentenceSegment>()
        var inQuote = false
        var quoteStartChar = ' '
        val currentBlock = StringBuilder()

        // 1. 第一阶段：按引号边界拆分为基础的旁白块与对话块
        for (c in trimmed) {
            if (isOpeningQuote(c) && !inQuote) {
                if (currentBlock.isNotEmpty()) {
                    rawBlocks.add(SentenceSegment(currentBlock.toString().trim(), SegmentRole.NARRATOR))
                    currentBlock.clear()
                }
                inQuote = true
                quoteStartChar = c
                currentBlock.append(c)
            } else if (isMatchingClosingQuote(quoteStartChar, c) && inQuote) {
                currentBlock.append(c)
                rawBlocks.add(SentenceSegment(currentBlock.toString().trim(), SegmentRole.DIALOGUE))
                currentBlock.clear()
                inQuote = false
            } else {
                currentBlock.append(c)
            }
        }

        if (currentBlock.isNotEmpty()) {
            val role = if (inQuote) SegmentRole.DIALOGUE else SegmentRole.NARRATOR
            rawBlocks.add(SentenceSegment(currentBlock.toString().trim(), role))
        }

        // 2. 第二阶段：对每个块内部执行标点短句细分与长度平滑控制
        val finalResult = mutableListOf<SentenceSegment>()
        for (block in rawBlocks) {
            if (block.text.isBlank()) continue
            val sentences = splitBlockIntoSentences(block.text, maxLength)
            for (s in sentences) {
                if (s.isNotBlank()) {
                    finalResult.add(SentenceSegment(s, block.role))
                }
            }
        }

        return finalResult
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
