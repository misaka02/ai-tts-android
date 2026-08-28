package com.aitts.engine.audio

import android.content.Context
import android.util.Log
import com.aitts.engine.data.TtsProviderConfig
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

/**
 * 生产级高性能音频磁盘缓存管理器 (High-Throughput LRU Audio Cache Engine)：
 * 1. 采用全维度 SHA-256 特征指纹（包含模型、音色、语速、音调、大模型导演提示词、采样率与文本），杜绝情绪串台；
 * 2. 采用 ReentrantReadWriteLock 读写分离架构，支持多句高并发并行预拉取与零阻塞快速读取；
 * 3. 增加 Flash 闪存访问防抖与低频批量 LRU 修剪机制，彻底消除单句写入时的磁盘 I/O 阻塞。
 */
class AudioCacheManager(private val context: Context) {

    private val rwLock = ReentrantReadWriteLock()
    private val writeCounter = AtomicInteger(0)

    private val cacheDir: File by lazy {
        val dir = File(context.cacheDir, "tts_audio_cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * 生成全维度唯一 SHA-256 缓存 Key（包含情感指令与采样率）
     */
    fun generateKey(
        providerId: String,
        modelName: String,
        voiceId: String,
        promptInstruction: String,
        sampleRate: Int,
        speed: Float,
        pitch: Float,
        text: String
    ): String {
        val raw = "${providerId}_${modelName}_${voiceId}_${promptInstruction.trim()}_${sampleRate}_${speed}_${pitch}_${text.trim()}"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    fun getAudio(text: String, config: TtsProviderConfig): ByteArray? {
        val key = generateKey(
            providerId = config.id,
            modelName = config.modelName,
            voiceId = config.voiceId,
            promptInstruction = config.promptInstruction,
            sampleRate = config.sampleRate,
            speed = config.speed,
            pitch = config.pitch,
            text = text
        )
        return get(key)
    }

    fun saveAudio(text: String, config: TtsProviderConfig, bytes: ByteArray, maxCacheSizeMb: Int = 500) {
        val key = generateKey(
            providerId = config.id,
            modelName = config.modelName,
            voiceId = config.voiceId,
            promptInstruction = config.promptInstruction,
            sampleRate = config.sampleRate,
            speed = config.speed,
            pitch = config.pitch,
            text = text
        )
        put(key, bytes, maxCacheSizeMb)
    }

    /**
     * 从磁盘读取缓存音频 (支持多线程无锁并发读取)
     */
    fun get(key: String): ByteArray? {
        val file = File(cacheDir, "$key.bin")
        if (!file.exists() || file.length() <= 0L) return null

        return rwLock.readLock().withLock {
            try {
                val now = System.currentTimeMillis()
                // 10 分钟内访问过则不重复更新文件修改时间，防止高频预读造成 Flash 闪存 I/O 抖动
                if (now - file.lastModified() > 600_000L) {
                    file.setLastModified(now)
                }
                file.readBytes()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 写入音频缓存 (临时文件写入 + 原子重命名 + 批量 LRU 写锁保护清理)
     * 杜绝高并发预拉取时读取线程读到写入一半的损坏残卷音频
     */
    fun put(key: String, data: ByteArray, maxCacheSizeMb: Int = 500) {
        if (data.isEmpty()) return
        val tempFile = File(cacheDir, "$key.tmp_${System.nanoTime()}")
        try {
            FileOutputStream(tempFile).use { it.write(data) }
            val targetFile = File(cacheDir, "$key.bin")
            if (targetFile.exists()) {
                targetFile.delete()
            }
            tempFile.renameTo(targetFile)

            // 每写入 30 句才检查修剪一次缓存，在排他写锁保护下批量修剪老旧文件
            if (writeCounter.incrementAndGet() % 30 == 0) {
                rwLock.writeLock().withLock {
                    trimCacheIfNeeded(maxCacheSizeMb)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioCacheManager", "写入缓存失败: ${e.message}")
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * 清理超出限额的老旧缓存文件 (必须在写锁内执行)
     */
    private fun trimCacheIfNeeded(maxCacheSizeMb: Int) {
        if (maxCacheSizeMb <= 0) return
        val maxSizeBytes = maxCacheSizeMb.toLong() * 1024 * 1024

        val files = cacheDir.listFiles() ?: return
        var totalSize = files.sumOf { it.length() }

        if (totalSize > maxSizeBytes) {
            val sortedFiles = files.sortedBy { it.lastModified() }
            for (f in sortedFiles) {
                val size = f.length()
                if (f.delete()) {
                    totalSize -= size
                    if (totalSize <= maxSizeBytes * 0.8) {
                        break
                    }
                }
            }
        }
    }

    /**
     * 获取缓存统计信息 (文件数, 总大小MB)
     */
    fun getStats(): Pair<Int, Float> {
        return rwLock.readLock().withLock {
            val files = cacheDir.listFiles() ?: emptyArray()
            val count = files.size
            val sizeBytes = files.sumOf { it.length() }
            val sizeMb = sizeBytes.toFloat() / (1024 * 1024)
            Pair(count, sizeMb)
        }
    }

    /**
     * 清空所有缓存 (排他写锁保证安全)
     */
    fun clearAll() {
        rwLock.writeLock().withLock {
            val files = cacheDir.listFiles() ?: return
            for (f in files) {
                f.delete()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: AudioCacheManager? = null

        fun getInstance(context: Context): AudioCacheManager {
            return instance ?: synchronized(this) {
                instance ?: AudioCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
