package com.aitts.engine.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 音频解码核心：
 * 将各类在线 AI TTS 返回的 MP3、AAC、OGG、WAV 音频流高效解码为
 * Android TextToSpeechService 所需的标准 PCM 16-bit 线性音频字节流。
 */
object AudioDecoder {

    private const val TAG = "AudioDecoder"

    data class DecodedAudio(
        val pcmData: ByteArray,
        val sampleRate: Int,
        val channelCount: Int,
        val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    )

    /**
     * 对输入的音频二进制数据进行智能解码
     */
    fun decodeToPcm(audioData: ByteArray, fallbackSampleRate: Int = 24000): DecodedAudio {
        if (audioData.isEmpty()) {
            return DecodedAudio(ByteArray(0), fallbackSampleRate, 1)
        }

        // 1. 判断是否为标准 WAV (RIFF header)
        if (isWavFormat(audioData)) {
            val wavInfo = parseWavHeader(audioData)
            if (wavInfo != null) {
                return wavInfo
            }
        }

        // 2. 如果是 MP3/AAC/OGG 等压缩音频，使用 MediaCodec 原生硬件/软件解码
        return try {
            decodeWithMediaCodec(audioData, fallbackSampleRate)
        } catch (e: Exception) {
            Log.e(TAG, "MediaCodec 解码失败，尝试降级: ${e.message}", e)
            // 降级：如果本身就是 raw PCM，直接返回
            DecodedAudio(audioData, fallbackSampleRate, 1)
        }
    }

    /**
     * 判断是否为 WAV 文件头 (RIFF....WAVE)
     */
    private fun isWavFormat(data: ByteArray): Boolean {
        if (data.size < 12) return false
        return data[0] == 'R'.code.toByte() &&
                data[1] == 'I'.code.toByte() &&
                data[2] == 'F'.code.toByte() &&
                data[3] == 'F'.code.toByte() &&
                data[8] == 'W'.code.toByte() &&
                data[9] == 'A'.code.toByte() &&
                data[10] == 'V'.code.toByte() &&
                data[11] == 'E'.code.toByte()
    }

    /**
     * 极速解析标准 PCM WAV 头部，直接切出 PCM 载荷（0 开销）
     */
    private fun parseWavHeader(data: ByteArray): DecodedAudio? {
        try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            var offset = 12

            var channels = 1
            var sampleRate = 24000
            var bitsPerSample = 16
            var dataOffset = -1
            var dataSize = -1

            while (offset + 8 <= data.size) {
                val chunkId = String(data, offset, 4)
                val chunkSize = buffer.getInt(offset + 4)
                offset += 8

                when (chunkId) {
                    "fmt " -> {
                        val formatCode = buffer.getShort(offset).toInt()
                        channels = buffer.getShort(offset + 2).toInt()
                        sampleRate = buffer.getInt(offset + 4)
                        bitsPerSample = buffer.getShort(offset + 14).toInt()
                        // formatCode 1 = PCM
                        if (formatCode != 1 && formatCode != 3) {
                            return null // 非线性 PCM，交给 MediaCodec 解码
                        }
                    }
                    "data" -> {
                        dataOffset = offset
                        dataSize = if (chunkSize > 0 && offset + chunkSize <= data.size) {
                            chunkSize
                        } else {
                            data.size - offset
                        }
                        break
                    }
                }
                offset += chunkSize
            }

            if (dataOffset != -1 && dataSize > 0 && bitsPerSample == 16) {
                val pcmPayload = ByteArray(dataSize)
                System.arraycopy(data, dataOffset, pcmPayload, 0, dataSize)
                return DecodedAudio(pcmPayload, sampleRate, channels)
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析 WAV 头部发生异常: ${e.message}")
        }
        return null
    }

    /**
     * 基于 Android 原生 MediaExtractor + MediaCodec 进行通用解码 (MP3/AAC/OGG/FLAC)
     * 使用临时文件方式保障在所有国内定制 ROM 上的 100% 兼容性
     */
    private fun decodeWithMediaCodec(data: ByteArray, fallbackSampleRate: Int): DecodedAudio {
        val tempFile = File.createTempFile("tts_audio_decode_", ".tmp")
        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write(data)
                fos.flush()
            }

            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(tempFile.absolutePath)

                var audioTrackIndex = -1
                var inputFormat: MediaFormat? = null
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        inputFormat = format
                        break
                    }
                }

                if (audioTrackIndex < 0 || inputFormat == null) {
                    return DecodedAudio(data, fallbackSampleRate, 1)
                }

                extractor.selectTrack(audioTrackIndex)
                val mime = inputFormat.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg"
                val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else {
                    fallbackSampleRate
                }
                val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else {
                    1
                }

                val codec = MediaCodec.createDecoderByType(mime)
                codec.configure(inputFormat, null, null, 0)
                codec.start()

                val outputStream = ByteArrayOutputStream()
                val bufferInfo = MediaCodec.BufferInfo()
                var isExtractorEOS = false
                var isCodecEOS = false
                val timeoutUs = 5000L

                try {
                    while (!isCodecEOS) {
                        if (!isExtractorEOS) {
                            val inIndex = codec.dequeueInputBuffer(timeoutUs)
                            if (inIndex >= 0) {
                                val inBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    codec.getInputBuffer(inIndex)
                                } else {
                                    codec.inputBuffers[inIndex]
                                }
                                inBuffer?.clear()
                                val sampleSize = if (inBuffer != null) extractor.readSampleData(inBuffer, 0) else -1
                                if (sampleSize < 0) {
                                    codec.queueInputBuffer(inIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    isExtractorEOS = true
                                } else {
                                    codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }

                        val outIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                        if (outIndex >= 0) {
                            val outBuffer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                codec.getOutputBuffer(outIndex)
                            } else {
                                codec.outputBuffers[outIndex]
                            }

                            if (outBuffer != null && bufferInfo.size > 0) {
                                outBuffer.position(bufferInfo.offset)
                                outBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                val chunk = ByteArray(bufferInfo.size)
                                outBuffer.get(chunk)
                                outputStream.write(chunk)
                            }

                            codec.releaseOutputBuffer(outIndex, false)

                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                isCodecEOS = true
                            }
                        }
                    }
                } finally {
                    try {
                        codec.stop()
                        codec.release()
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                val decodedPcm = outputStream.toByteArray()
                return DecodedAudio(decodedPcm, sampleRate, channelCount)
            } finally {
                try {
                    extractor.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        } finally {
            try {
                tempFile.delete()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
