package com.aitts.engine.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 基于 LRU 策略的本地音频磁盘缓存管理器
 * 采用 ReentrantLock 确保高并发预加载时的线程安全
 */
class AudioCacheManager(private val context: Context) {

    private val lock = ReentrantLock()

    private val cacheDir: File by lazy {
        val dir = File(context.cacheDir, "tts_audio_cache")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * 生成唯一 SHA-256 缓存 Key
     */
    fun generateKey(
        providerId: String,
        voiceId: String,
        speed: Float,
        pitch: Float,
        text: String
    ): String {
        val raw = "${providerId}_${voiceId}_${speed}_${pitch}_${text.trim()}"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    fun getAudio(text: String, config: com.aitts.engine.data.TtsProviderConfig): ByteArray? {
        val key = generateKey(config.id, config.voiceId, config.speed, config.pitch, text)
        return get(key)
    }

    fun saveAudio(text: String, config: com.aitts.engine.data.TtsProviderConfig, bytes: ByteArray, maxCacheSizeMb: Int = 500) {
        val key = generateKey(config.id, config.voiceId, config.speed, config.pitch, text)
        put(key, bytes, maxCacheSizeMb)
    }

    /**
     * 从磁盘读取缓存音频 (线程安全)
     */
    fun get(key: String): ByteArray? {
        return lock.withLock {
            val file = File(cacheDir, "$key.bin")
            if (file.exists() && file.length() > 0) {
                try {
                    file.setLastModified(System.currentTimeMillis())
                    file.readBytes()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * 写入音频缓存 (线程安全 + LRU 自动清理)
     */
    fun put(key: String, data: ByteArray, maxCacheSizeMb: Int = 500) {
        if (data.isEmpty()) return
        lock.withLock {
            try {
                val file = File(cacheDir, "$key.bin")
                FileOutputStream(file).use { it.write(data) }
                trimCacheIfNeeded(maxCacheSizeMb)
            } catch (e: Exception) {
                Log.w("AudioCacheManager", "写入缓存失败: ${e.message}")
            }
        }
    }

    /**
     * 清理超出限额的老旧缓存文件 (必须在锁内执行)
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
        return lock.withLock {
            val files = cacheDir.listFiles() ?: emptyArray()
            val count = files.size
            val sizeBytes = files.sumOf { it.length() }
            val sizeMb = sizeBytes.toFloat() / (1024 * 1024)
            Pair(count, sizeMb)
        }
    }

    /**
     * 清空所有缓存 (线程安全)
     */
    fun clearAll() {
        lock.withLock {
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
