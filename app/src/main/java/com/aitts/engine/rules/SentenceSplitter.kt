package com.aitts.engine.rules

/**
 * 智能长句切分算法：
 * 将长段落按自然标点符号（。！？；!?;\n等）切分成短句，
 * 并支持结合引号保护与最大长度阈值，实现毫秒级首字出声与平滑流式朗读。
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
     * @param text 输入文本
     * @param maxLength 单句最大建议长度（默认 80 字）
     * @return 切分后的短句列表
     */
    fun splitText(text: String, maxLength: Int = 80): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        val currentChunk = StringBuilder()

        var i = 0
        while (i < trimmed.length) {
            val c = trimmed[i]
            currentChunk.append(c)

            val isPrimaryEnd = c in SENTENCE_END_PUNCTUATIONS
            val isSecondaryEnd = c in SECONDARY_PUNCTUATIONS && currentChunk.length >= maxLength / 2
            val isOverLength = currentChunk.length >= maxLength

            if (isPrimaryEnd || isSecondaryEnd || isOverLength) {
                // 检查后续是否紧跟后引号或括号（如 ”’」』）”，一并吸收到本句中
                while (i + 1 < trimmed.length && isClosingQuote(trimmed[i + 1])) {
                    i++
                    currentChunk.append(trimmed[i])
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

    private fun isClosingQuote(c: Char): Boolean {
        return c == '”' || c == '’' || c == '」' || c == '』' || c == '"' || c == '\'' || c == '）' || c == ')'
    }
}
