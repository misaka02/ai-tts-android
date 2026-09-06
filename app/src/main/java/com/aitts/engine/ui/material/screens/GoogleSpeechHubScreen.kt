package com.aitts.engine.ui.material.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.data.requiresClientSpeedScaling
import com.aitts.engine.provider.GeminiTtsProvider
import com.aitts.engine.provider.MimoTtsProvider
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.material.GoogleColors
import com.aitts.engine.ui.material.components.GoogleAudioWaveform
import com.aitts.engine.ui.material.components.GoogleLogViewerSheet
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 🎙️ Google 官方应用风格 - 朗读与试听全功能中枢 (Google Speech Hub)
 * 全功能无缝适配：
 * 1. 顶栏：Google 标识、实时状态徽章、深浅色一键切换、诊断日志抽屉入口；
 * 2. 核心区：Google Recorder 32 根圆柱状对称声谱 (GoogleAudioWaveform)；
 * 3. 文本输入区：字数统计、粘贴、一言金句、清空；
 * 4. 播放中控：64dp Google 蓝圆形主键、停止、导出音频到本地 Downloads 目录；
 * 5. 音色与参数：当前音色卡片（支持在线选音色与双角色对白提示）、快捷倍速药丸；
 * 6. 进阶声学抽屉：句间自然停顿 (sentencePauseMs)、EQ 音效预设、清晰人声增强 (Clear Voice)、睡眠定时器、软件响度增益。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSpeechHubScreen(
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    onOpenProviders: () -> Unit = {},
    onNavigateToEditProvider: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val rules by configDataStore.rulesFlow.collectAsState()
    val logs by configDataStore.logsFlow.collectAsState()

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()
        ?: TtsProviderConfig(id = "default", type = ProviderType.EDGE_TTS, name = "默认微软引擎")

    val isAiModel = activeProvider.type.requiresApiKey || activeProvider.type in listOf(
        ProviderType.MIMO, ProviderType.MINIMAX, ProviderType.DOUBAO, ProviderType.SILICONFLOW,
        ProviderType.FISH_AUDIO, ProviderType.STEPFUN, ProviderType.OPENAI, ProviderType.GEMINI
    )

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    var textInput by remember { mutableStateOf("您好！欢迎使用 AI-TTS 语音引擎。这是专为 Android 系统打造的高品质语音合成服务。") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastLatencyMs by remember { mutableLongStateOf(0L) }
    var lastSynthesizedBytes by remember { mutableStateOf<ByteArray?>(null) }

    var showVoicePickerSheet by remember { mutableStateOf(false) }
    var availableVoices by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
    var isLoadingVoices by remember { mutableStateOf(false) }

    var showModelSwitcherSheet by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var promptEditValue by remember(activeProvider) { mutableStateOf(activeProvider.promptInstruction) }

    var isTestingPing by remember { mutableStateOf(false) }
    var activePingLatency by remember { mutableStateOf<Long?>(null) }

    var isFetchingOnlineQuote by remember { mutableStateOf(false) }
    var quoteSourceDesc by remember { mutableStateOf<String?>("") }

    var showAdvancedAudioSheet by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }

    fun fetchOnlineQuote() {
        isFetchingOnlineQuote = true
        scope.launch {
            try {
                val q = QuoteService.fetchOnlineHitokoto()
                textInput = q.text
                quoteSourceDesc = q.source ?: "网络精选名句"
                Toast.makeText(context, "已载入在线精选名句", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                val fallback = QuoteService.getRandomLocalQuote()
                textInput = fallback.text
                quoteSourceDesc = fallback.source ?: "内置经典语料"
                Toast.makeText(context, "网络波动，已载入本地经典名言", Toast.LENGTH_SHORT).show()
            } finally {
                isFetchingOnlineQuote = false
            }
        }
    }

    fun fetchLocalQuote() {
        val fallback = QuoteService.getRandomLocalQuote()
        textInput = fallback.text
        quoteSourceDesc = "内置名家金句"
        Toast.makeText(context, "已载入本地金句", Toast.LENGTH_SHORT).show()
    }

    fun testActivePing() {
        isTestingPing = true
        scope.launch {
            val start = System.currentTimeMillis()
            val res = TtsProviderManager.getInstance().synthesize("测试", activeProvider, autoRetry = false)
            val cost = System.currentTimeMillis() - start
            isTestingPing = false
            if (res.isSuccess) {
                activePingLatency = cost
                Toast.makeText(context, "${activeProvider.name} 延迟: ${cost}ms", Toast.LENGTH_SHORT).show()
            } else {
                activePingLatency = -1L
                val err = res.exceptionOrNull()?.message ?: "失败"
                Toast.makeText(context, "测速异常: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun executeSynthesis() {
        if (textInput.isBlank()) {
            Toast.makeText(context, "请输入要朗读的文本", Toast.LENGTH_SHORT).show()
            return
        }

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (isPlaying) {
            audioPlayer.stop()
            isPlaying = false
            return
        }

        val testText = TextPreprocessor.process(textInput.trim(), rules)
        val trialSessionId = "g_hub_${System.currentTimeMillis() % 100000}"
        isSynthesizing = true

        scope.launch {
            try {
                val effectiveSpeed = activeProvider.speed.coerceIn(0.2f, 3.0f)
                val effectivePitch = activeProvider.pitch.coerceIn(0.2f, 2.0f)
                val testConfig = activeProvider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                configDataStore.log("[Google Hub] 发起合成: 模型=${activeProvider.name}, 音色=${activeProvider.voiceId}, 语速=${effectiveSpeed}x, 文本长度=${testText.length}", sessionId = trialSessionId)
                val startTime = System.currentTimeMillis()
                var firstChunkReceived = false

                val result = if (activeProvider.isStreamingEnabled) {
                    val streamBuffer = java.io.ByteArrayOutputStream()
                    val streamRes = TtsProviderManager.getInstance().synthesizeStreaming(testText, testConfig, trialSessionId) { chunk ->
                        if (!firstChunkReceived) {
                            firstChunkReceived = true
                            lastLatencyMs = System.currentTimeMillis() - startTime
                            configDataStore.log("[Stream] 首包到达: ${lastLatencyMs}ms", sessionId = trialSessionId)
                        }
                        streamBuffer.write(chunk)
                    }
                    if (streamRes.isSuccess) {
                        Result.success(streamBuffer.toByteArray())
                    } else {
                        streamRes
                    }
                } else {
                    TtsProviderManager.getInstance().synthesize(testText, testConfig, autoRetry = true, sessionId = trialSessionId)
                }

                val costMs = System.currentTimeMillis() - startTime

                if (result.isSuccess) {
                    val audioData = result.getOrNull() ?: ByteArray(0)
                    if (audioData.isNotEmpty()) {
                        if (!firstChunkReceived) {
                            lastLatencyMs = costMs
                        }
                        lastSynthesizedBytes = audioData
                        isSynthesizing = false
                        isPlaying = true

                        configDataStore.log("[Audio] 合成成功: 大小=${audioData.size}字节, 耗时=${costMs}ms, 采样率=${activeProvider.sampleRate}Hz", sessionId = trialSessionId)

                        val playbackSpeed = if (testConfig.requiresClientSpeedScaling(isStreaming = activeProvider.isStreamingEnabled)) effectiveSpeed else 1.0f
                        audioPlayer.playAudioBytes(
                            audioBytes = audioData,
                            speed = playbackSpeed,
                            onCompletion = {
                                isPlaying = false
                            }
                        )
                    } else {
                        isSynthesizing = false
                        Toast.makeText(context, "合成音频数据为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isSynthesizing = false
                    val errorMsg = result.exceptionOrNull()?.message ?: "未知合成错误"
                    configDataStore.log("[Error] 合成失败: $errorMsg", sessionId = trialSessionId)
                    Toast.makeText(context, "合成失败: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isSynthesizing = false
                Toast.makeText(context, "异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportAudioToDisk() {
        val data = lastSynthesizedBytes
        if (data == null || data.isEmpty()) {
            Toast.makeText(context, "请先播放合成一段音频后再导出", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val ext = if (activeProvider.audioFormat.isNotBlank()) activeProvider.audioFormat else "mp3"
            val fileName = "AITTS_${System.currentTimeMillis()}.$ext"
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val outFile = File(downloadDir, fileName)
            FileOutputStream(outFile).use { it.write(data) }
            Toast.makeText(context, "已成功导出到: Downloads/$fileName", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Google 顶栏 (包含状态绿点、诊断日志入口、深浅色切换)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = colors.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "AI-TTS",
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "AI-TTS 语音服务",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.googleGreen)
                            )
                            Text(
                                text = "就绪 · ${activeProvider.name}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            if (activeProvider.isDualRoleEnabled) {
                                Surface(shape = RoundedCornerShape(4.dp), color = colors.primaryContainer) {
                                    Text("双角色", fontSize = 10.sp, color = colors.onPrimaryContainer, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 实时诊断日志按钮
                    IconButton(onClick = { showLogsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "诊断日志",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // 深浅色切换
                    IconButton(
                        onClick = {
                            val nextMode = if (settings.appThemeMode.uppercase() == "LIGHT") "DARK" else "LIGHT"
                            configDataStore.updateSettings(settings.copy(appThemeMode = nextMode))
                        }
                    ) {
                        Icon(
                            imageVector = if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "切换深浅主题",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        }

        // 2. Google Recorder 风格动态柱状声谱
        item {
            GoogleAudioWaveform(
                isPlaying = isPlaying,
                colors = colors,
                statusText = when {
                    isSynthesizing -> "正在请求云端引擎合成..."
                    isPlaying -> "正在朗读音频 (首包延迟 ${lastLatencyMs}ms)"
                    lastLatencyMs > 0 -> "就绪 · 上次首字延迟 ${lastLatencyMs}ms · ${activeProvider.sampleRate}Hz"
                    else -> "就绪 · 点击播放开始试听"
                }
            )
        }

        // 3. Google Keep 风格文本输入卡片
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        placeholder = {
                            Text(
                                text = "输入或粘贴要朗读的文本...",
                                color = colors.textTertiary,
                                fontSize = 15.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = colors.primary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    )

                    // 出处来源徽章
                    if (!quoteSourceDesc.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "出处来源: $quoteSourceDesc",
                                fontSize = 11.5.sp,
                                color = colors.primary,
                                maxLines = 1
                            )
                        }
                    }

                    // 底部快捷操作条 (字数统计 + 粘贴 + 在线语料 + 本地语料 + 清空)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${textInput.length} 字",
                            fontSize = 12.sp,
                            color = colors.textTertiary,
                            fontWeight = FontWeight.Medium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            // 粘贴
                            AssistChip(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = cm.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                        if (pasted.isNotBlank()) {
                                            textInput = pasted
                                            Toast.makeText(context, "已从剪贴板粘贴", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                label = { Text("粘贴", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.surfaceContainer,
                                    labelColor = colors.textSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // 在线语料 (Hitokoto)
                            AssistChip(
                                onClick = { fetchOnlineQuote() },
                                label = { Text("在线语料", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (isFetchingOnlineQuote) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = colors.primary, strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.surfaceContainer,
                                    labelColor = colors.primary
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // 本地语料
                            AssistChip(
                                onClick = { fetchLocalQuote() },
                                label = { Text("本地名言", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.surfaceContainer,
                                    labelColor = colors.textSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // 诊断日志
                            AssistChip(
                                onClick = { showLogsSheet = true },
                                label = { Text("日志", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = colors.surfaceContainer,
                                    labelColor = colors.textSecondary
                                ),
                                border = null,
                                shape = RoundedCornerShape(12.dp)
                            )

                            // 清空
                            if (textInput.isNotEmpty()) {
                                AssistChip(
                                    onClick = {
                                        textInput = ""
                                        quoteSourceDesc = null
                                    },
                                    label = { Text("清空", fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = colors.surfaceContainer,
                                        labelColor = colors.textSecondary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. 当前模型与音色工作台 (包含直达模型配置、一键换模型、测速与语气提示词)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 头部：图标 + 当前模型名称 + 类型 + 主力徽章 + 直接进入“模型设置”操作图标
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToEditProvider(activeProvider.id) }
                        ) {
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                color = colors.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = colors.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = activeProvider.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "主力",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${activeProvider.type.displayName} · 音色: ${activeProvider.voiceId.ifBlank { "默认" }}",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        // 快速进入当前模型精细设置入口 (一键直达)
                        IconButton(
                            onClick = { onNavigateToEditProvider(activeProvider.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "模型设置",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // 关键参数胶囊行 (测速结果/实时探测 + 对白音色 + 采样率)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 实时延迟徽章 / 测速按键
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isTestingPing) { testActivePing() },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                activePingLatency == null -> colors.surfaceContainerHigh
                                activePingLatency!! < 0 -> colors.googleRed.copy(alpha = 0.15f)
                                activePingLatency!! < 400 -> colors.googleGreen.copy(alpha = 0.15f)
                                activePingLatency!! < 900 -> colors.googleYellow.copy(alpha = 0.2f)
                                else -> colors.googleRed.copy(alpha = 0.15f)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isTestingPing) {
                                    CircularProgressIndicator(color = colors.primary, strokeWidth = 1.5.dp, modifier = Modifier.size(11.dp))
                                    Text("测速中", fontSize = 11.sp, color = colors.primary)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = null,
                                        tint = when {
                                            activePingLatency == null -> colors.textSecondary
                                            activePingLatency!! < 0 -> colors.googleRed
                                            activePingLatency!! < 400 -> colors.googleGreen
                                            else -> colors.textPrimary
                                        },
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = when {
                                            activePingLatency == null -> "测速"
                                            activePingLatency!! < 0 -> "异常"
                                            else -> "${activePingLatency}ms"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            activePingLatency == null -> colors.textSecondary
                                            activePingLatency!! < 0 -> colors.googleRed
                                            activePingLatency!! < 400 -> colors.googleGreen
                                            else -> colors.textPrimary
                                        }
                                    )
                                }
                            }
                        }

                        if (activeProvider.isDualRoleEnabled) {
                            Surface(shape = RoundedCornerShape(10.dp), color = colors.primaryContainer) {
                                Text(
                                    text = "对白: ${activeProvider.dialogueVoiceId}",
                                    fontSize = 11.sp,
                                    color = colors.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(shape = RoundedCornerShape(10.dp), color = colors.surfaceContainerHigh) {
                            Text(
                                text = "${activeProvider.sampleRate}Hz · ${activeProvider.audioFormat.uppercase()}",
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 底部快捷动作行 (换模型 + 换音色 + 语气提示词 + 进入设置)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 换模型
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showModelSwitcherSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("换模型", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = colors.primary)
                            }
                        }

                        // 换音色
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showVoicePickerSheet = true
                                    isLoadingVoices = true
                                    scope.launch {
                                        try {
                                            val list = TtsProviderManager.getInstance().getAvailableVoices(activeProvider)
                                            availableVoices = list
                                        } catch (_: Exception) {}
                                        isLoadingVoices = false
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = colors.primary, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("换音色", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = colors.primary)
                            }
                        }

                        // AI 语气提示词 / 导演指令
                        if (isAiModel) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showPromptDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = colors.googleYellow, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("提示词", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                }
                            }
                        }

                        // 模型设置
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToEditProvider(activeProvider.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("模型设置", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }

        // 5. 播放中控与快捷操作
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 停止按钮
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable(enabled = isPlaying || isSynthesizing) {
                                audioPlayer.stop()
                                isPlaying = false
                                isSynthesizing = false
                            },
                        shape = CircleShape,
                        color = colors.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "停止",
                                tint = if (isPlaying || isSynthesizing) colors.googleRed else colors.textTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 64dp Google Blue 圆形主控播放键
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable { executeSynthesis() },
                        shape = CircleShape,
                        color = colors.primary,
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSynthesizing) {
                                CircularProgressIndicator(
                                    color = colors.onPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "停止" else "播放",
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(24.dp))

                    // 导出音频到本地文件
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable { exportAudioToDisk() },
                        shape = CircleShape,
                        color = colors.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "导出音频",
                                tint = if (lastSynthesizedBytes != null) colors.primary else colors.textTertiary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // 语速快捷控制（自适应四大调速体系：MiMo非流式声学7档/MiMo流式Sonic/传统/原生AI）
                val isPromptGearsMode = (activeProvider.type == ProviderType.MIMO && !activeProvider.isStreamingEnabled) ||
                                        (activeProvider.type == ProviderType.GEMINI)
                val isMimoStreaming = (activeProvider.type == ProviderType.MIMO && activeProvider.isStreamingEnabled)

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "语速调节: ${String.format(java.util.Locale.US, "%.2f", activeProvider.speed)}x",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            if (isMimoStreaming) {
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.googleYellow.copy(alpha = 0.18f)) {
                                    Text(
                                        text = "Sonic 物理拉伸",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isPromptGearsMode) {
                                Surface(shape = RoundedCornerShape(6.dp), color = colors.primary.copy(alpha = 0.12f)) {
                                    Text(
                                        text = "声学指令 7 档",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // 展开听书进阶声学设置抽屉
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAdvancedAudioSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = colors.primary, modifier = Modifier.size(14.dp))
                                Text("声学工具", fontSize = 12.sp, color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    if (isPromptGearsMode) {
                        val mimoGears = listOf(
                            Triple(0.60f, "0.6x 极缓", "极度舒缓"),
                            Triple(0.75f, "0.75x 从容", "从容较慢"),
                            Triple(0.88f, "0.88x 微慢", "悠闲微慢"),
                            Triple(1.00f, "1.0x 标准", "自然流畅"),
                            Triple(1.20f, "1.2x 轻快", "稍快明快"),
                            Triple(1.45f, "1.45x 紧凑", "紧凑干练"),
                            Triple(1.75f, "1.75x 极速", "快速疾读")
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(mimoGears) { (gearSpeed, gearLabel, _) ->
                                val isSelected = kotlin.math.abs(activeProvider.speed - gearSpeed) < 0.05f
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            configDataStore.updateProvider(activeProvider.copy(speed = gearSpeed))
                                            Toast.makeText(context, "${activeProvider.name} 语速: $gearLabel", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                                    border = if (isSelected) BorderStroke(1.dp, colors.primary) else null
                                ) {
                                    Text(
                                        text = gearLabel,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.onPrimaryContainer else colors.textSecondary
                                    )
                                }
                            }
                        }

                        val currentInstruction = when (activeProvider.type) {
                            ProviderType.MIMO -> MimoTtsProvider.getSpeedInstruction(activeProvider.speed)
                            ProviderType.GEMINI -> GeminiTtsProvider.getSpeedInstruction(activeProvider.speed)
                            else -> ""
                        }
                        if (currentInstruction.isNotBlank()) {
                            Text(
                                text = "大模型声学指令：“$currentInstruction” (客户端原声 1.0x 直出)",
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val speedPresets = listOf(0.8f, 1.0f, 1.2f, 1.5f, 2.0f)
                            speedPresets.forEach { speed ->
                                val isSelected = kotlin.math.abs(activeProvider.speed - speed) < 0.05f
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            configDataStore.updateProvider(activeProvider.copy(speed = speed))
                                            Toast.makeText(context, "${activeProvider.name} 语速设为 ${speed}x", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                                    border = if (isSelected) BorderStroke(1.dp, colors.primary) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 6.dp)) {
                                        Text(
                                            text = "${speed}x",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.onPrimaryContainer else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 抽屉 1: 在线音色选择抽屉
    if (showVoicePickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoicePickerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("选择音色 · ${activeProvider.name}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)

                if (isLoadingVoices) {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                } else if (availableVoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("暂无在线音色列表，请在模型配置中手动填写音色代码", color = colors.textTertiary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(340.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableVoices) { v ->
                            val isChosen = v.id == activeProvider.voiceId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = activeProvider.copy(voiceId = v.id)
                                        configDataStore.updateProvider(updated)
                                        showVoicePickerSheet = false
                                        Toast.makeText(context, "已切换为: ${v.name}", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isChosen) colors.primaryContainer else colors.surfaceContainer
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(v.name, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium, fontSize = 14.5.sp, color = if (isChosen) colors.onPrimaryContainer else colors.textPrimary)
                                        Text("${v.id} · ${v.locale}", fontSize = 12.sp, color = if (isChosen) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary)
                                    }
                                    if (isChosen) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 抽屉 2: 听书声学工具箱 (句间停顿、EQ预设、清晰人声、睡眠定时)
    if (showAdvancedAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdvancedAudioSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("听书声学与睡眠定时工具箱", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                // 1. 句间自然停顿
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("标点句间停顿时长", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("${settings.sentencePauseMs}ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Text("在小说分句后注入微小静音，大幅提高听感自然度", fontSize = 11.5.sp, color = colors.textSecondary)
                    Slider(
                        value = settings.sentencePauseMs.toFloat(),
                        onValueChange = { configDataStore.updateSettings(settings.copy(sentencePauseMs = it.toInt())) },
                        valueRange = 0f..800f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }

                // 2. 清晰人声增强滤镜
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("清晰人声增强滤镜 (Clear Voice)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text("优化 1~3kHz 人声元音共振峰，嘈杂环境下更清晰", fontSize = 11.5.sp, color = colors.textSecondary)
                    }
                    Switch(
                        checked = settings.voiceClarityBoostEnabled,
                        onCheckedChange = { configDataStore.updateSettings(settings.copy(voiceClarityBoostEnabled = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
                    )
                }

                // 3. 听书睡眠定时器
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("听书睡眠定时器 (倒计时停止)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val timerMinutes = listOf(0, 15, 30, 45, 60)
                        timerMinutes.forEach { min ->
                            val isSel = settings.sleepTimerMinutes == min
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(sleepTimerMinutes = min))
                                        Toast.makeText(context, if (min == 0) "已关闭定时器" else "已设为 ${min} 分钟后停止", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) colors.primaryContainer else colors.surfaceContainer,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                            ) {
                                Text(
                                    text = if (min == 0) "关闭" else "${min}分",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) colors.onPrimaryContainer else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // 4. 传统引擎音调微调 (Pitch)
                val supportsPitch = when (activeProvider.type) {
                    ProviderType.EDGE_TTS,
                    ProviderType.AZURE,
                    ProviderType.CUSTOM_HTTP,
                    ProviderType.OFFLINE_VITS -> true
                    else -> false
                }
                if (supportsPitch) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("传统引擎音调 (Pitch)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text(String.format(java.util.Locale.US, "%.2fx", activeProvider.pitch), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                        }
                        Slider(
                            value = activeProvider.pitch,
                            onValueChange = { configDataStore.updateProvider(activeProvider.copy(pitch = it)) },
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                        )
                    }
                }

                // 5. 软件级响度动态增益
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("软件级响度增益", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                        Text(String.format(java.util.Locale.US, "%.1fx", settings.loudnessGainFactor), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = settings.loudnessGainFactor,
                        onValueChange = { configDataStore.updateSettings(settings.copy(loudnessGainFactor = it)) },
                        valueRange = 1.0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 抽屉 3: 实时诊断日志抽屉 (Material 3 标准组件，支持请求生命周期聚合、单次独立复制与二次清空确认)
    if (showLogsSheet) {
        GoogleLogViewerSheet(
            configDataStore = configDataStore,
            colors = colors,
            onDismiss = { showLogsSheet = false }
        )
    }

    // 抽屉 4: 底部快捷切换模型抽屉 (支持一键选为主力 + 直达参数设置)
    if (showModelSwitcherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModelSwitcherSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("切换主力语音引擎", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text("${providers.size} 个已就绪", fontSize = 12.sp, color = colors.textSecondary)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(providers, key = { it.id }) { p ->
                        val isCurrent = p.id == activeProvider.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(activeProviderId = p.id))
                                    showModelSwitcherSheet = false
                                    Toast.makeText(context, "已切换主力引擎: ${p.name}", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isCurrent) colors.primaryContainer else colors.surfaceContainer,
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) colors.primary else colors.textTertiary)
                                    )
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = p.name,
                                                fontSize = 15.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCurrent) colors.onPrimaryContainer else colors.textPrimary
                                            )
                                            if (isCurrent) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = colors.primary
                                                ) {
                                                    Text(
                                                        text = "当前主力",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = colors.onPrimary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${p.type.displayName} · 音色: ${p.voiceId.ifBlank { "默认" }}",
                                            fontSize = 12.sp,
                                            color = if (isCurrent) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            showModelSwitcherSheet = false
                                            onNavigateToEditProvider(p.id)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "编辑模型参数",
                                            tint = if (isCurrent) colors.primary else colors.textSecondary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 对话框: 大模型导演提示词 (Prompt Instruction) 快速调整
    if (showPromptDialog) {
        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = {
                Text("语气与导演提示词", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "为大模型指定语气风格、角色情感与语调抑扬顿挫：",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )

                    OutlinedTextField(
                        value = promptEditValue,
                        onValueChange = { promptEditValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("例如：用温柔亲切的语气朗读，带有适当的情感起伏和停顿。", fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    // 快速模板
                    Text("常用风格模板:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            "自然旁白" to "用自然、沉稳的小说旁白口吻朗读，语速适中，富有沉浸感。",
                            "温柔陪伴" to "声音轻柔温暖，带有亲切的微笑感，语速舒缓柔和。",
                            "激情演说" to "用充满激情与感染力的方式演讲，抑扬顿挫，节奏有力。"
                        )
                        presets.forEach { (label, content) ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { promptEditValue = content },
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceContainer
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = activeProvider.copy(promptInstruction = promptEditValue.trim())
                        configDataStore.updateProvider(updated)
                        showPromptDialog = false
                        Toast.makeText(context, "已更新模型导演提示词", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存并生效")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromptDialog = false }) {
                    Text("取消", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
