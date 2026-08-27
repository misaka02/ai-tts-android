package com.aitts.engine.provider

import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel

/**
 * TTS Provider 统一抽象接口
 */
interface TtsProvider {

    /**
     * 获取支持的音色列表
     */
    suspend fun getAvailableVoices(config: TtsProviderConfig): List<VoiceModel>

    /**
     * 获取支持的模型 ID 列表
     */
    suspend fun getAvailableModels(config: TtsProviderConfig): List<String> {
        return if (config.modelName.isNotBlank()) listOf(config.modelName) else emptyList()
    }

    /**
     * 执行同步/块级语音合成
     * @param text 待合成的文本短句
     * @param config 当前提供商配置
     * @return 合成后的音频字节数据 (MP3/WAV/AAC/PCM)
     */
    suspend fun synthesize(
        text: String,
        config: TtsProviderConfig
    ): Result<ByteArray>

    /**
     * 支持会话级隔离与精确网络取消的语音合成重载
     */
    suspend fun synthesize(
        text: String,
        config: TtsProviderConfig,
        sessionId: String
    ): Result<ByteArray> = synthesize(text, config)

    /**
     * 流式语音合成推流 (Streaming chunk-by-chunk push)
     * 当收到每个音频分块时立即回调 onAudioChunk，实现首包毫秒级秒开播放
     */
    suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> {
        val full = synthesize(text, config)
        if (full.isSuccess) {
            val bytes = full.getOrNull() ?: ByteArray(0)
            if (bytes.isNotEmpty()) onAudioChunk(bytes)
        }
        return full
    }

    /**
     * 支持会话级隔离与精确网络取消的流式语音合成重载
     */
    suspend fun synthesizeStreaming(
        text: String,
        config: TtsProviderConfig,
        sessionId: String,
        onAudioChunk: suspend (ByteArray) -> Unit
    ): Result<ByteArray> = synthesizeStreaming(text, config, onAudioChunk)

    /**
     * 校验配置是否有效（测试连接）
     */
    suspend fun testConnection(config: TtsProviderConfig): Result<Boolean> {
        val testResult = synthesize("测试语音合成", config)
        return if (testResult.isSuccess && (testResult.getOrNull()?.isNotEmpty() == true)) {
            Result.success(true)
        } else {
            Result.failure(testResult.exceptionOrNull() ?: Exception("合成结果为空"))
        }
    }
}
