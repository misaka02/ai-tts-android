package com.aitts.engine.audio

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 高性能无锁音频字节缓冲池 (Zero-Allocation Audio Buffer Pool)：
 * 针对 16-bit PCM 音频流推流、重采样与解码过程中的频繁内存分配进行对象复用，
 * 彻底消除长篇听书过程中的 Android 虚拟机 GC 垃圾回收堆内存颠簸。
 */
object AudioBufferPool {

    private const val MAX_POOL_SIZE_PER_BUCKET = 32

    // 按缓冲区大小分桶缓存 (4KB, 8KB, 16KB, 32KB, 64KB 等)
    private val pools = ConcurrentHashMap<Int, ConcurrentLinkedQueue<ByteArray>>()

    /**
     * 从缓冲池中获取指定或最接近规格的 ByteArray
     */
    fun acquire(minSize: Int): ByteArray {
        val bucketSize = getBucketSize(minSize)
        val queue = pools.getOrPut(bucketSize) { ConcurrentLinkedQueue() }
        val buffer = queue.poll()
        return buffer ?: ByteArray(bucketSize)
    }

    /**
     * 将使用完毕的 ByteArray 归还给缓冲池复用
     */
    fun release(buffer: ByteArray?) {
        if (buffer == null) return
        val size = buffer.size
        val queue = pools.getOrPut(size) { ConcurrentLinkedQueue() }
        if (queue.size < MAX_POOL_SIZE_PER_BUCKET) {
            // 清理为零字节避免脏数据
            buffer.fill(0)
            queue.offer(buffer)
        }
    }

    /**
     * 向上取整到 2 的幂次标准分桶规格
     */
    private fun getBucketSize(minSize: Int): Int {
        var size = 4096 // 最小 4KB
        while (size < minSize) {
            size = size shl 1
            if (size <= 0) return minSize // 防止溢出
        }
        return size
    }

    /**
     * 清空全部缓存池
     */
    fun clear() {
        pools.clear()
    }
}
