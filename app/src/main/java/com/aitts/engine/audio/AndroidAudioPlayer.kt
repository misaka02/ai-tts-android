package com.aitts.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 通用音频播放器（基于原生 MediaPlayer 与缓存直通）：
 * 支持直接播放各类 TTS 返回的原始 MP3/WAV/AAC/OGG 音频字节流，
 * 避免 AudioTrack 低级 PCM 格式不匹配导致的无声或崩溃问题。
 */
class AndroidAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var tempAudioFile: File? = null

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.w("AudioPlayer", "stop 异常: ${e.message}")
        } finally {
            cleanupTempFile()
        }
    }

    suspend fun playAudioBytes(
        audioBytes: ByteArray,
        onCompletion: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        stop()

        if (audioBytes.isEmpty()) {
            withContext(Dispatchers.Main) {
                onError("音频数据为空")
            }
            return@withContext
        }

        try {
            // 写入 App 私有缓存目录临时文件
            val tempFile = File.createTempFile("tts_preview_", ".audio", context.cacheDir)
            tempAudioFile = tempFile
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
                fos.flush()
            }

            withContext(Dispatchers.Main) {
                try {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        setDataSource(tempFile.absolutePath)
                        setOnCompletionListener {
                            stop()
                            onCompletion()
                        }
                        setOnErrorListener { _, what, extra ->
                            stop()
                            onError("播放器错误 what=$what, extra=$extra")
                            true
                        }
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                } catch (e: Exception) {
                    stop()
                    onError("播放器初始化失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            stop()
            withContext(Dispatchers.Main) {
                onError("写入临时音频失败: ${e.message}")
            }
        }
    }

    private fun cleanupTempFile() {
        try {
            tempAudioFile?.delete()
            tempAudioFile = null
        } catch (e: Exception) {
            // ignore
        }
    }
}
