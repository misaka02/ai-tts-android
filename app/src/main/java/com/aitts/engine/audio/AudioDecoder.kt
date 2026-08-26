package com.aitts.engine.audio

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 纯内存音频解码核心 (Zero-Disk In-Memory MediaCodec Decoder)：
 * 1. 纯 RAM 内存流解码，彻底消除磁盘临时文件生成与 Flash 闪存磨损；
 * 2. 极速零拷贝解析标准 WAV/RIFF 头部；
 * 3. 将各类在线 AI TTS 返回的 MP3、AAC、OGG、FLAC 高效解码为标准 PCM 16-bit 线性音频流。
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
     * 对输入的音频二进制数据进行智能纯内存解码
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

        // 2. 如果包含明确的 MP3/AAC/OGG/FLAC 文件头，使用 MediaCodec 原生 100% 纯内存解码
        if (isCompressedAudio(audioData)) {
            return try {
                decodeWithInMemoryMediaCodec(audioData, fallbackSampleRate)
            } catch (e: Exception) {
                Log.e(TAG, "MediaCodec 纯内存解码失败，尝试降级: ${e.message}", e)
                DecodedAudio(audioData, fallbackSampleRate, 1)
            }
        }

        // 3. 非压缩格式，本身即为纯净原生 PCM16 裸流，直接返回（0 开销，0 损耗，彻底消除误解码电音）
        return DecodedAudio(audioData, fallbackSampleRate, 1)
    }

    /**
     * 判断是否为 MP3/AAC/OGG/FLAC 等压缩音频格式
     */
    private fun isCompressedAudio(data: ByteArray): Boolean {
        if (data.size < 4) return false
        // ID3v2 tag (MP3)
        if (data[0] == 'I'.code.toByte() && data[1] == 'D'.code.toByte() && data[2] == '3'.code.toByte()) return true
        // OggS container
        if (data[0] == 'O'.code.toByte() && data[1] == 'g'.code.toByte() && data[2] == 'g'.code.toByte() && data[3] == 'S'.code.toByte()) return true
        // fLaC
        if (data[0] == 'f'.code.toByte() && data[1] == 'L'.code.toByte() && data[2] == 'a'.code.toByte() && data[3] == 'C'.code.toByte()) return true
        // MP3 / AAC frame sync header
        val firstTwo = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        if ((firstTwo and 0xFFE0) == 0xFFE0) return true
        if ((firstTwo and 0xFFF0) == 0xFFF0) return true
        return false
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
                        // formatCode 1 = PCM, 3 = IEEE float
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
     * 基于 Android 原生 InMemoryMediaDataSource + MediaExtractor + MediaCodec 进行纯内存硬件解码
     * 100% 零磁盘 I/O，零闪存擦写，无文件锁延迟
     */
    private fun decodeWithInMemoryMediaCodec(data: ByteArray, fallbackSampleRate: Int): DecodedAudio {
        val dataSource = InMemoryMediaDataSource(data)
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(dataSource)

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

            val outputStream = ByteArrayOutputStream(data.size * 4)
            val bufferInfo = MediaCodec.BufferInfo()
            var isExtractorEOS = false
            var isCodecEOS = false
            val timeoutUs = 5000L

            try {
                while (!isCodecEOS) {
                    if (!isExtractorEOS) {
                        val inIndex = codec.dequeueInputBuffer(timeoutUs)
                        if (inIndex >= 0) {
                            val inBuffer = codec.getInputBuffer(inIndex)
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
                        val outBuffer = codec.getOutputBuffer(outIndex)

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
    }
}
