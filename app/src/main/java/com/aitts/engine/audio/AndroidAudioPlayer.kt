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
        speed: Float = 1.0f,
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
            // 自动检测音频格式：如果是 raw PCM 裸流，自动封装 44-byte 标准 WAV 头部以供 MediaPlayer 纯内存播放
            val playableBytes = if (isContainerAudio(audioBytes)) {
                audioBytes
            } else {
                wrapPcmWithWavHeader(audioBytes, sampleRate = 24000, channels = 1, bitsPerSample = 16)
            }

            val dataSource = InMemoryMediaDataSource(playableBytes)

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
                            AudioVisualizerManager.getInstance().decayToSilence()
                            stop()
                            onCompletion()
                        }
                        setOnErrorListener { _, what, extra ->
                            AudioVisualizerManager.getInstance().decayToSilence()
                            stop()
                            onError("播放器错误 what=$what, extra=$extra")
                            true
                        }
                        prepare()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            try {
                                playbackParams = playbackParams.setSpeed(speed.coerceIn(0.25f, 3.0f))
                            } catch (e: Exception) {
                                Log.w("AudioPlayer", "设置播放倍速失败: ${e.message}")
                            }
                        }
                        start()
                    }
                    mediaPlayer = player
                    AudioVisualizerManager.getInstance().attachToSession(player.audioSessionId)

                    // 异步高速解码出真实 PCM 数据并驱动 STFT 频域示波器，强力绑定播放器实际播放状态与倍速
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Default).launch {
                        try {
                            val decoded = AudioDecoder.decodeToPcm(audioBytes)
                            if (decoded.pcmData.isNotEmpty()) {
                                AudioVisualizerManager.getInstance().startRealPcmAnalysis(
                                    pcmBytes = decoded.pcmData,
                                    sampleRate = decoded.sampleRate,
                                    speed = speed,
                                    isStillPlaying = {
                                        try {
                                            player.isPlaying
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                )
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

    /**
     * 判断是否已带有 WAV/MP3/AAC/OGG/FLAC 封装头
     */
    private fun isContainerAudio(data: ByteArray): Boolean {
        if (data.size < 4) return false
        // RIFF (WAV)
        if (data[0] == 'R'.code.toByte() && data[1] == 'I'.code.toByte() && data[2] == 'F'.code.toByte() && data[3] == 'F'.code.toByte()) return true
        // ID3 (MP3)
        if (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) return true
        // OggS (OGG)
        if (data[0] == 'O'.code.toByte() && data[1] == 'g'.code.toByte() && data[2] == 'g'.code.toByte() && data[3] == 'S'.code.toByte()) return true
        // fLaC (FLAC)
        if (data[0] == 'f'.code.toByte() && data[1] == 'L'.code.toByte() && data[2] == 'a'.code.toByte() && data[3] == 'C'.code.toByte()) return true
        // MP3 / AAC sync word
        val firstTwo = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        if ((firstTwo and 0xFFE0) == 0xFFE0) return true
        if ((firstTwo and 0xFFF0) == 0xFFF0) return true
        return false
    }

    /**
     * 为纯 PCM 裸流动态添加 44 字节标准 WAV 头部
     */
    private fun wrapPcmWithWavHeader(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM format
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmData.size and 0xff).toByte()
        header[41] = ((pcmData.size shr 8) and 0xff).toByte()
        header[42] = ((pcmData.size shr 16) and 0xff).toByte()
        header[43] = ((pcmData.size shr 24) and 0xff).toByte()

        val wavData = ByteArray(44 + pcmData.size)
        System.arraycopy(header, 0, wavData, 0, 44)
        System.arraycopy(pcmData, 0, wavData, 44, pcmData.size)
        return wavData
    }
}
