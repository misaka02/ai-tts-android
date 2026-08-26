package com.aitts.engine.provider

import android.content.Context
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.offline.OfflineModelManager
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 真实端侧 Sherpa-ONNX 离线神经网络语音合成引擎 (100% On-Device Neural Offline TTS)
 * 1. 采用官方 C++ JNI 运行时，直接驱动本地 .onnx 模型权重；
 * 2. 100% 本地计算，零依赖系统自带 TTS，断网无缝可用；
 * 3. 原生支持多发音人 (Speaker ID) 切换与高精度语速控制；
 * 4. 纯净 PCM/WAV 输出，杜绝任何杂音与破音；
 * 5. 全局统一使用 OfflineModelManager.getModelDir() 存储路径，杜绝路径不一致报错。
 */
class OfflineTtsProvider(private val context: Context) : TtsProvider {

    @Volatile
    private var currentTts: OfflineTts? = null
    @Volatile
    private var loadedModelId: String? = null
    private val lock = Any()

    companion object {
        @Volatile
        private var isLibraryLoaded = false

        /**
         * 检测手机本地是否已具备 Sherpa-ONNX C++ 原生运行库环境
         * 优先检查主程序，其次检查已安装的独立轻量拓展包 com.aitts.engine.offline.runtime
         */
        fun isEngineInstalled(context: Context): Boolean {
            if (isLibraryLoaded) return true
            try {
                System.loadLibrary("sherpa-onnx-jni")
                isLibraryLoaded = true
                return true
            } catch (e: Throwable) {
                // not in main app
            }

            return try {
                val packageInfo = context.packageManager.getPackageInfo("com.aitts.engine.offline.runtime", 0)
                val nativeLibDir = packageInfo.applicationInfo.nativeLibraryDir
                val soFile = File(nativeLibDir, "libsherpa-onnx-jni.so")
                if (soFile.exists()) {
                    System.load(soFile.absolutePath)
                    isLibraryLoaded = true
                    true
                } else {
                    false
                }
            } catch (e: Throwable) {
                false
            }
        }
    }

    override suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel> = withContext(Dispatchers.IO) {
        val catalog = OfflineModelManager.getCatalog()
        val curPack = catalog.find { it.id == config.modelName } ?: catalog.firstOrNull()
        if (curPack != null && curPack.speakers.isNotEmpty()) {
            curPack.speakers.mapIndexed { idx, spkName ->
                VoiceModel(
                    id = "${curPack.id}_spk_$idx",
                    name = spkName,
                    gender = if (spkName.contains("男") || spkName.contains("male", ignoreCase = true)) "Male" else "Female",
                    locale = "zh-CN",
                    description = "${curPack.name} 内置端侧离线音色"
                )
            }
        } else {
            listOf(
                VoiceModel("zh-CN-XiaoxiaoOffline", "微软晓晓 (离线温暖女声)", "Female", "zh-CN", "微软经典自然女声"),
                VoiceModel("zh-CN-YunxiOffline", "微软云希 (离线沉浸男声)", "Male", "zh-CN", "微软经典沉浸男声"),
                VoiceModel("zh-CN-YunyangOffline", "微软云扬 (离线播音男声)", "Male", "zh-CN", "微软专业播音男声")
            )
        }
    }

    override suspend fun getAvailableModels(config: TtsProviderConfig): List<String> = withContext(Dispatchers.IO) {
        OfflineModelManager.getCatalog().map { it.id }
    }

    private fun getOrInitTts(modelId: String): OfflineTts {
        synchronized(lock) {
            if (currentTts != null && loadedModelId == modelId) {
                return currentTts!!
            }

            // 全局统一从 OfflineModelManager 获取模型存储目录，杜绝路径不匹配
            val modelDir = OfflineModelManager.getModelDir(context, modelId)
            if (!modelDir.exists() || !modelDir.isDirectory) {
                throw IOException("模型目录不存在: ${modelDir.absolutePath}。请先在离线模型管理界面点击下载安装。")
            }

            val allFiles = modelDir.walkTopDown().toList()
            val onnxFile = allFiles.firstOrNull { it.extension.equals("onnx", ignoreCase = true) }
                ?: throw IOException("在模型目录未找到 .onnx 权重文件")
            val tokensFile = allFiles.firstOrNull { it.name.equals("tokens.txt", ignoreCase = true) }
                ?: throw IOException("在模型目录未找到 tokens.txt 字典映射文件")
            val lexiconFile = allFiles.firstOrNull { it.name.equals("lexicon.txt", ignoreCase = true) }
            val dictDir = allFiles.firstOrNull { it.isDirectory && it.name.equals("dict", ignoreCase = true) }
            val espeakDir = allFiles.firstOrNull { it.isDirectory && it.name.equals("espeak-ng-data", ignoreCase = true) }

            val vitsConfig = OfflineTtsVitsModelConfig(
                model = onnxFile.absolutePath,
                lexicon = lexiconFile?.absolutePath ?: "",
                tokens = tokensFile.absolutePath,
                dataDir = espeakDir?.absolutePath ?: "",
                dictDir = dictDir?.absolutePath ?: "",
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )

            val modelConfig = OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = 2,
                debug = false,
                provider = "cpu"
            )

            val ttsConfig = OfflineTtsConfig(model = modelConfig)
            val tts = OfflineTts(context.assets, ttsConfig)

            currentTts = tts
            loadedModelId = modelId
            return tts
        }
    }

    override suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success(ByteArray(0))

        val catalog = OfflineModelManager.getCatalog()
        val modelId = config.modelName.ifBlank { "vits-icefall-zh-aishell3" }
        val pack = catalog.find { it.id == modelId }

        // 1. 检查离线模型文件是否已下载就绪 (使用统一路径检测)
        val isDownloaded = OfflineModelManager.isModelDownloaded(context, modelId)
        if (!isDownloaded) {
            val packName = pack?.name ?: modelId
            val size = pack?.sizeMb ?: 48
            return@withContext Result.failure(
                IOException("离线模型【$packName】尚未下载安装 (${size}MB)。请在模型配置界面点击「一键下载」安装后即可离线使用。")
            )
        }

        // 2. 检查手机本地是否已安装端侧 C++ 推理环境组件
        if (!isEngineInstalled(context)) {
            return@withContext Result.failure(
                IOException("端侧离线神经推理环境未安装。安装包本体保持 2MB 极简小巧，请在离线模型配置界面点击「安装离线推理环境」组件后即可离线使用。")
            )
        }

        try {
            val tts = getOrInitTts(modelId)
            val speakerId = parseSpeakerId(config.voiceId)
            val safeSpeed = config.speed.coerceIn(0.5f, 2.5f)

            val generated = tts.generate(text = text, sid = speakerId, speed = safeSpeed)
            val samples = generated.samples
            val sampleRate = generated.sampleRate

            if (samples.isEmpty()) {
                return@withContext Result.failure(IOException("Sherpa-ONNX 离线推理返回空音频采样"))
            }

            val pcmBytes = ByteArray(samples.size * 2)
            val bb = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in samples) {
                val s = (sample.coerceIn(-1.0f, 1.0f) * 32767.0f).toInt()
                bb.putShort(s.toShort())
            }

            val wavBytes = wrapWavHeader(pcmBytes, sampleRate, 1, 16)
            Result.success(wavBytes)
        } catch (e: Throwable) {
            Result.failure(IOException("端侧离线神经网络推理异常: ${e.message}", e))
        }
    }

    override suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val fullResult = synthesize(text, config)
        if (fullResult.isSuccess) {
            val fullBytes = fullResult.getOrNull() ?: ByteArray(0)
            if (fullBytes.isNotEmpty()) {
                val chunkSize = 2048
                var offset = 0
                while (offset < fullBytes.size) {
                    val len = minOf(chunkSize, fullBytes.size - offset)
                    val chunk = fullBytes.copyOfRange(offset, offset + len)
                    onAudioChunk(chunk)
                    offset += len
                    delay(6)
                }
            }
        }
        fullResult
    }

    private fun parseSpeakerId(voiceId: String): Int {
        if (voiceId.isBlank()) return 0
        return try {
            if (voiceId.contains("_spk_")) {
                voiceId.substringAfterLast("_spk_").toIntOrNull() ?: 0
            } else {
                voiceId.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun wrapWavHeader(pcmData: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray())
        bb.putInt(totalDataLen)
        bb.put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray())
        bb.putInt(16) // Subchunk1Size
        bb.putShort(1.toShort()) // PCM
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort((channels * bitsPerSample / 8).toShort())
        bb.putShort(bitsPerSample.toShort())
        bb.put("data".toByteArray())
        bb.putInt(pcmData.size)

        val out = ByteArrayOutputStream(header.size + pcmData.size)
        out.write(header)
        out.write(pcmData)
        return out.toByteArray()
    }
}
