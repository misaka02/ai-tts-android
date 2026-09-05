package com.aitts.engine.ui.material.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.ui.material.GoogleColors
import com.aitts.engine.ui.material.components.GoogleAudioWaveform
import kotlinx.coroutines.launch

/**
 * 🎙️ Google 官方应用风格 - 朗读与试听中枢 (Google Speech Hub)
 * 1. 顶栏：Google 药丸标识与状态胶囊 (🟢 服务就绪)，深浅色快速切换；
 * 2. 核心区：Google Recorder 录音机动态圆角柱状声谱 (GoogleAudioWaveform)；
 * 3. 文本输入区：Google Keep 风格大圆角卡片，含字数统计、粘贴、一言金句、清空；
 * 4. 播放控制：64dp Google 蓝圆形主控，停止、导出音频药丸；
 * 5. 音色与参数：当前音色快捷卡片 + 语速快捷药丸 (0.8x, 1.0x, 1.2x, 1.5x) + 细调滑块。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSpeechHubScreen(
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    onOpenProviders: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()
        ?: TtsProviderConfig(id = "default", type = ProviderType.EDGE_TTS, name = "默认微软引擎")

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
    var showVoicePickerSheet by remember { mutableStateOf(false) }
    var availableVoices by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
    var isLoadingVoices by remember { mutableStateOf(false) }
    var showFineTuneSliders by remember { mutableStateOf(false) }

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

        val testText = textInput.trim()
        val trialSessionId = "g_hub_${System.currentTimeMillis() % 100000}"
        isSynthesizing = true

        scope.launch {
            try {
                val effectiveSpeed = (activeProvider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (activeProvider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = activeProvider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                configDataStore.log("[Google Hub] 发起合成: 模型=${activeProvider.name}, 音色=${activeProvider.voiceId}", sessionId = trialSessionId)
                val startTime = System.currentTimeMillis()
                var firstChunkReceived = false

                val result = if (activeProvider.isStreamingEnabled) {
                    val streamBuffer = java.io.ByteArrayOutputStream()
                    val streamRes = TtsProviderManager.getInstance().synthesizeStreaming(testText, testConfig, trialSessionId) { chunk ->
                        if (!firstChunkReceived) {
                            firstChunkReceived = true
                            lastLatencyMs = System.currentTimeMillis() - startTime
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
                        isSynthesizing = false
                        isPlaying = true

                        val playbackSpeed = if (activeProvider.copy(speed = effectiveSpeed).requiresClientSpeedScaling(isStreaming = activeProvider.isStreamingEnabled)) effectiveSpeed else 1.0f
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
                    Toast.makeText(context, "合成失败: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isSynthesizing = false
                Toast.makeText(context, "异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        // 1. Google 官方风格应用顶栏
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
                    // Google 风格圆形标识
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
                        }
                    }
                }

                // 右侧深浅色快速切换
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

        // 2. Google Recorder 风格动态柱状声谱
        item {
            GoogleAudioWaveform(
                isPlaying = isPlaying,
                colors = colors,
                statusText = when {
                    isSynthesizing -> "正在请求云端引擎合成..."
                    isPlaying -> "正在朗读音频 (延迟 ${lastLatencyMs}ms)"
                    lastLatencyMs > 0 -> "就绪 · 上次首字延迟 ${lastLatencyMs}ms"
                    else -> "就绪 · 点击播放按钮试听"
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

                    // 底部快捷操作条 (字数统计 + 粘贴 + 样句 + 清空)
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

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                            // 样句
                            AssistChip(
                                onClick = {
                                    textInput = QuoteService.getRandomLocalQuote().text
                                },
                                label = { Text("样句", fontSize = 12.sp) },
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

                            // 清空
                            if (textInput.isNotEmpty()) {
                                AssistChip(
                                    onClick = { textInput = "" },
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

        // 4. 当前音色与服务商药丸卡片 (Google Assistant Card)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
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
                shape = RoundedCornerShape(20.dp),
                color = colors.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
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
                            Text(
                                text = activeProvider.voiceId.ifBlank { "默认音色" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${activeProvider.name} · ${activeProvider.type.displayName}",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "换音色",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }

        // 5. Google Recorder 居中圆形主控播放键与控制台
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 播放控制大按钮行
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

                    Spacer(modifier = Modifier.width(28.dp))

                    // Google Blue 大圆形播放/暂停主按键 (68dp)
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

                    Spacer(modifier = Modifier.width(28.dp))

                    // 导出/微调开关
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable { showFineTuneSliders = !showFineTuneSliders },
                        shape = CircleShape,
                        color = if (showFineTuneSliders) colors.primaryContainer else colors.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "调节参数",
                                tint = if (showFineTuneSliders) colors.onPrimaryContainer else colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // 语速快捷药丸 (Google Speed Pills)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val speedPresets = listOf(0.8f, 1.0f, 1.2f, 1.5f, 2.0f)
                    speedPresets.forEach { speed ->
                        val isSelected = kotlin.math.abs(settings.globalSpeed - speed) < 0.05f
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(globalSpeed = speed))
                                    Toast.makeText(context, "全局语速设为 ${speed}x", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primaryContainer else colors.surfaceContainer,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                        ) {
                            Text(
                                text = "${speed}x",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.onPrimaryContainer else colors.textSecondary
                            )
                        }
                    }
                }

                // 展开细调参数 (语速、语调)
                if (showFineTuneSliders) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 语速滑块
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("语速调节", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                    Text(String.format("%.2fx", settings.globalSpeed), fontSize = 13.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = settings.globalSpeed,
                                    onValueChange = { configDataStore.updateSettings(settings.copy(globalSpeed = it)) },
                                    valueRange = 0.5f..2.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primary,
                                        activeTrackColor = colors.primary,
                                        inactiveTrackColor = colors.outlineSubtle
                                    )
                                )
                            }

                            // 音调滑块
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("音调调节", fontSize = 13.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium)
                                    Text(String.format("%.2fx", settings.globalPitch), fontSize = 13.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = settings.globalPitch,
                                    onValueChange = { configDataStore.updateSettings(settings.copy(globalPitch = it)) },
                                    valueRange = 0.5f..1.5f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.primary,
                                        activeTrackColor = colors.primary,
                                        inactiveTrackColor = colors.outlineSubtle
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Google 风格底部音色选择抽屉
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
                Text(
                    text = "选择音色 · ${activeProvider.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                if (isLoadingVoices) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                } else if (availableVoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无在线音色列表，请在模型配置中手动填写音色代码", color = colors.textTertiary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableVoices.size) { idx ->
                            val v = availableVoices[idx]
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = v.name,
                                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.5.sp,
                                            color = if (isChosen) colors.onPrimaryContainer else colors.textPrimary
                                        )
                                        Text(
                                            text = "${v.id} · ${v.locale}",
                                            fontSize = 12.sp,
                                            color = if (isChosen) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary
                                        )
                                    }

                                    if (isChosen) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = colors.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
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
}
