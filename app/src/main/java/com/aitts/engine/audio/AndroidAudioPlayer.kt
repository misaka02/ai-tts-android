package com.aitts.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 纯内存极速音频播放器 (Zero-Disk In-Memory MediaPlayer)：
 * 采用 InMemoryMediaDataSource 纯内存直通播放各类 TTS 原始音频流 (MP3/WAV/AAC/OGG)，
 * 彻底切除临时文件写入，实现毫秒级试听直出。
 */
class AndroidAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

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
            AudioVisualizerManager.getInstance().resetToSilence()
        } catch (e: Exception) {
            Log.w("AudioPlayer", "stop 异常: ${e.message}")
        }
    }

    suspend fun playAudio(audioBytes: ByteArray) {
        playAudioBytes(audioBytes)
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
            val dataSource = InMemoryMediaDataSource(audioBytes)

            withContext(Dispatchers.Main) {
                try {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        setDataSource(dataSource)
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
                    AudioVisualizerManager.getInstance().attachToSession(player.audioSessionId)

                    // 异步高速解码出真实 PCM 数据并驱动 STFT 频域示波器
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                        try {
                            val decoded = AudioDecoder.decodeToPcm(audioBytes)
                            if (decoded.pcmData.isNotEmpty()) {
                                AudioVisualizerManager.getInstance().startRealPcmAnalysis(decoded.pcmData, decoded.sampleRate)
                            }
                        } catch (e: Exception) {
                            Log.w("AudioPlayer", "示波器 PCM 解码分析异常: ${e.message}")
                        }
                    }
                } catch (e: Exception) {
                    stop()
                    onError("播放器初始化失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            stop()
            withContext(Dispatchers.Main) {
                onError("内存音频播放失败: ${e.message}")
            }
        }
    }
}
