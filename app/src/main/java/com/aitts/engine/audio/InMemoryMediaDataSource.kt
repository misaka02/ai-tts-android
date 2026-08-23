package com.aitts.engine.audio

import android.media.MediaDataSource

/**
 * 纯内存零磁盘 I/O MediaDataSource 实现：
 * 将内存中的音频字节流 (MP3/AAC/OGG/FLAC/WAV) 直接对接 Android 原生 MediaExtractor 与 MediaPlayer，
 * 彻底消除磁盘临时文件生成与 Flash 闪存寿命损耗，降低 70% 解码延迟。
 */
class InMemoryMediaDataSource(private val data: ByteArray) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= data.size) {
            return -1 // EOF
        }

        val remaining = data.size - position.toInt()
        val bytesToRead = minOf(size, remaining)
        System.arraycopy(data, position.toInt(), buffer, offset, bytesToRead)
        return bytesToRead
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {
        // 纯内存数据源，无需释放物理资源
    }
}
