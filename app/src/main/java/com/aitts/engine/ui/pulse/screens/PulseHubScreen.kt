package com.aitts.engine.ui.pulse.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.ui.pulse.components.OneHandedActionHub
import com.aitts.engine.ui.pulse.components.PulseAcousticCore
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ⚡ Pulse 主控中枢界面 (Pulse Core Hub)
 * 1. 顶层高对比度系统 TTS 引导与就绪指示器；
 * 2. 中心声学流体量子球（PulseAcousticCore）与 4 大指标直出；
 * 3. 大拇指单手试听台（一言金句/语料/实时毫秒级诊断日志）；
 * 4. 底部抽屉式模型快速切换（长按防抖平滑拖拽排序）；
 * 5. 右下角单手单拇指悬浮收纳岛。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PulseHubScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onOpenDeck: () -> Unit = {},
    parentPagerState: androidx.compose.foundation.pager.PagerState? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val logs by configDataStore.logsFlow.collectAsState()

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()
        ?: TtsProviderConfig(id = "default", type = com.aitts.engine.data.ProviderType.EDGE_TTS, name = "默认引擎")

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

    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastLatencyMs by remember { mutableStateOf<Long?>(null) }
    var testText by remember { mutableStateOf("欢迎使用 AI TTS 系统语音引擎！当前正在通过智能神经网络模型为您朗读文本。") }
    var quoteSourceDesc by remember { mutableStateOf<String?>(null) }
    var isFetchingQuote by remember { mutableStateOf(false) }

    var showVoicePickerDialog by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var showAcousticDialog by remember { mutableStateOf(false) }
    var showBottomSheetModelPicker by remember { mutableStateOf(false) }
    var showLiveLogsDialog by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visualizerManager = remember { AudioVisualizerManager.getInstance() }
    val spectrumBands by visualizerManager.spectrumFlow.collectAsState()
    val rmsEnergy by visualizerManager.rmsEnergyFlow.collectAsState()

    val density = LocalDensity.current
    val sheetItemHeightPx = with(density) { 68.dp.toPx() }
    var sheetDraggedProviderId by remember { mutableStateOf<String?>(null) }
    var sheetDragStartIndex by remember { mutableStateOf(-1) }
    var sheetDragTargetIndex by remember { mutableStateOf(-1) }
    var sheetTotalDragOffsetY by remember { mutableFloatStateOf(0f) }

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
        isSynthesizing = false
    }

    fun startSynthesis(provider: TtsProviderConfig) {
        if (isPlaying || isSynthesizing) {
            stopPlayback()
            return
        }

        if (testText.isBlank()) {
            Toast.makeText(context, "请输入需要朗读的文本", Toast.LENGTH_SHORT).show()
            return
        }

        isSynthesizing = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        scope.launch {
            try {
                val effectiveSpeed = (provider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (provider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = provider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                configDataStore.log("🚀 发起语音合成: 模型=【${provider.name}】(${provider.type.displayName}), 音色=${provider.voiceId.ifBlank { "默认" }}, 文本=“${testText.take(28)}...”")
                configDataStore.log("🌐 正在连接服务端端点: ${provider.baseUrl.ifBlank { "官方直接协议" }}")

                val startTime = System.currentTimeMillis()
                var firstChunkReceived = false

                val result = if (provider.isStreamingEnabled) {
                    val streamBuffer = java.io.ByteArrayOutputStream()
                    val streamRes = TtsProviderManager.getInstance().synthesizeStreaming(testText, testConfig) { chunk ->
                        if (!firstChunkReceived) {
                            firstChunkReceived = true
                            lastLatencyMs = System.currentTimeMillis() - startTime
                            isSynthesizing = false
                            isPlaying = true
                            configDataStore.log("⚡ [流式首包秒开] 首包到达耗时: ${lastLatencyMs}ms (边推边播启动)")
                        }
                        streamBuffer.write(chunk)
                    }
                    if (streamRes.isSuccess) {
                        Result.success(streamBuffer.toByteArray())
                    } else {
                        streamRes
                    }
                } else {
                    TtsProviderManager.getInstance().synthesize(testText, testConfig)
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

                        configDataStore.log("✅ 收到音频数据: ${audioData.size} 字节, 总耗时=${costMs}ms, 采样率=${provider.sampleRate}Hz")
                        configDataStore.log("🔊 内存音频直出播放开始, 启动 32 频段 STFT 示波分析")

                        // 仅当流式传输且服务端未按提示词变速时由播放器执行时间缩放；非流式音频已在合成期原生注入语速，播放器以 1.0x 原声保真直出，杜绝二次减速/加速
                        val playbackSpeed = if (provider.isStreamingEnabled) effectiveSpeed else 1.0f
                        audioPlayer.playAudioBytes(
                            audioBytes = audioData,
                            speed = playbackSpeed,
                            onCompletion = {
                                isPlaying = false
                                configDataStore.log("⏹️ 朗读播放完成")
                            },
                            onError = { err ->
                                isPlaying = false
                                configDataStore.log("⚠️ 播放器异常: $err")
                            }
                        )
                    } else {
                        isSynthesizing = false
                        isPlaying = false
                        configDataStore.log("⚠️ 合成音频流为空 (0 字节)")
                        Toast.makeText(context, "合成音频流为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isSynthesizing = false
                    isPlaying = false
                    val err = result.exceptionOrNull()?.message ?: "未知异常"
                    configDataStore.log("❌ 合成失败: $err")
                    Toast.makeText(context, "合成失败: $err", Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                isSynthesizing = false
                isPlaying = false
                val msg = t.message ?: t.javaClass.simpleName
                configDataStore.log("💥 调用异常: $msg")
                Toast.makeText(context, "调用异常: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun fetchRandomSentence(isOnline: Boolean) {
        if (isFetchingQuote) return
        isFetchingQuote = true
        scope.launch {
            try {
                if (isOnline) {
                    val item = QuoteService.fetchOnlineHitokoto()
                    testText = item.text
                    quoteSourceDesc = item.source
                } else {
                    val item = QuoteService.getRandomLocalQuote()
                    testText = item.text
                    quoteSourceDesc = item.source ?: "内置语料"
                }
            } catch (e: Exception) {
                val item = QuoteService.getRandomLocalQuote()
                testText = item.text
                quoteSourceDesc = "内置经典语料"
            } finally {
                isFetchingQuote = false
            }
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏 Header
            item(contentType = "hub_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "AI TTS ENGINE",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = PulseTokens.TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PulseTokens.CyanElectric.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "v${com.aitts.engine.BuildConfig.VERSION_NAME}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTokens.CyanElectric,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "系统级语音合成与流式大模型引擎",
                            fontSize = 11.sp,
                            color = PulseTokens.TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) PulseTokens.CyanElectric else PulseTokens.AcidGreen)
                        )
                        Text(
                            text = if (isPlaying) "播放中" else "就绪",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlaying) PulseTokens.CyanElectric else PulseTokens.AcidGreen
                        )
                    }
                }
            }

            // 系统默认 TTS 引导卡片
            item(contentType = "sys_guide") {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = PulseTokens.SurfaceElevated,
                    border = PulseTokens.BorderSubtle
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(PulseTokens.CyanElectric.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = PulseTokens.CyanElectric,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "设为系统默认 TTS 引擎",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseTokens.TextPrimary
                            )
                            Text(
                                text = "在系统设置中指定本引擎，小说/读屏即刻生效",
                                fontSize = 11.sp,
                                color = PulseTokens.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { activity?.let { PermissionManager.openSystemTtsSettings(it) } },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseTokens.CyanElectric,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("去设置", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 中心声学流体量子球卡片
            item(contentType = "acoustic_core_deck") {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = PulseTokens.BorderActive
                ) {
                    val activeBrandColor = if (settings.isProviderCardAccentColorEnabled) {
                        when (activeProvider.type) {
                            ProviderType.EDGE_TTS -> Color(0xFF0078D4)
                            ProviderType.AZURE -> Color(0xFF0089D6)
                            ProviderType.MIMO -> Color(0xFFFF6A00)
                            ProviderType.MINIMAX -> Color(0xFF8B5CF6)
                            ProviderType.DOUBAO -> Color(0xFF3B82F6)
                            ProviderType.STEPFUN -> Color(0xFF06B6D4)
                            ProviderType.OPENAI -> Color(0xFF10A37F)
                            ProviderType.SILICONFLOW -> Color(0xFF6366F1)
                            ProviderType.FISH_AUDIO -> Color(0xFFEC4899)
                            ProviderType.GEMINI -> Color(0xFF9333EA)
                            ProviderType.CUSTOM_HTTP -> Color(0xFFF59E0B)
                            ProviderType.OFFLINE_VITS -> Color(0xFF10B981)
                        }
                    } else {
                        PulseTokens.CyanElectric
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PulseTokens.SurfaceElevated,
                            border = PulseTokens.BorderSubtle,
                            modifier = Modifier.clickable { onNavigateToEditProvider(activeProvider.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(activeBrandColor)
                                )
                                Text(
                                    text = activeProvider.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeBrandColor
                                )
                                Text(
                                    text = "· 切换",
                                    fontSize = 11.sp,
                                    color = PulseTokens.SonicBlue,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { showBottomSheetModelPicker = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        PulseAcousticCore(
                            spectrumBands = spectrumBands,
                            rmsEnergy = rmsEnergy,
                            isPlaying = isPlaying,
                            isSynthesizing = isSynthesizing,
                            coreColor = activeBrandColor,
                            coreStyle = settings.acousticCoreStyle,
                            onClick = { startSynthesis(activeProvider) },
                            onStyleChange = { nextStyle ->
                                configDataStore.updateSettings(settings.copy(acousticCoreStyle = nextStyle % 3))
                                val styleName = when (nextStyle % 3) {
                                    0 -> "极光光晕"
                                    1 -> "物理点阵"
                                    else -> "引力轨道"
                                }
                                Toast.makeText(context, "核心球风格: $styleName", Toast.LENGTH_SHORT).show()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )

                        Text(
                            text = "轻触试听 · 双击切换核心球风格",
                            fontSize = 10.5.sp,
                            color = PulseTokens.TextTertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 大交互式声学状态微胶囊 (支持直接点击执行相应功能与模型自适应)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 卡片 1: 延迟与一键测速
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        startSynthesis(activeProvider)
                                        Toast.makeText(context, "正在测试 ${activeProvider.name} 延迟...", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = PulseTokens.SurfaceElevated,
                                border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("合成延迟", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                    Text("${lastLatencyMs ?: "--"}ms", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                    Text("测速 ⚡", fontSize = 9.sp, color = PulseTokens.CyanElectric.copy(alpha = 0.8f))
                                }
                            }

                            // 卡片 2: 发音人音色与快速更换
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showVoicePickerDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = PulseTokens.SurfaceElevated,
                                border = BorderStroke(1.dp, PulseTokens.SonicBlue.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("发音音色", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                    Text(activeProvider.voiceId.ifBlank { "默认" }.take(6), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.SonicBlue, maxLines = 1)
                                    Text("换音色 🎙️", fontSize = 9.sp, color = PulseTokens.SonicBlue.copy(alpha = 0.8f))
                                }
                            }

                            // 卡片 3: 模型自适应参数 (AI 情绪 Prompt / 传统 TTS 语速音高)
                            if (isAiModel) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showPromptDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = PulseTokens.SurfaceElevated,
                                    border = BorderStroke(1.dp, PulseTokens.AmberWarm.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("AI 情绪", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                        Text(if (activeProvider.promptInstruction.isNotBlank()) "自定义" else "标准", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.AmberWarm)
                                        Text("Prompt 🎭", fontSize = 9.sp, color = PulseTokens.AmberWarm.copy(alpha = 0.8f))
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showAcousticDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = PulseTokens.SurfaceElevated,
                                    border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("语速音高", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                        Text("${String.format("%.1f", activeProvider.speed)}x/${String.format("%.1f", activeProvider.pitch)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                        Text("微调 🎚️", fontSize = 9.sp, color = PulseTokens.CyanElectric.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            // 卡片 4: 缓存空间与一键清理 (支持即时归零响应)
                            var cacheStatsVersion by remember { mutableStateOf(0) }
                            val (cacheCount, cacheSizeMb) = remember(isPlaying, cacheStatsVersion) { audioCacheManager.getStats() }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        audioCacheManager.clearAll()
                                        cacheStatsVersion++
                                        Toast.makeText(context, "已释放本地音频缓存", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = PulseTokens.SurfaceElevated,
                                border = BorderStroke(1.dp, PulseTokens.MagentaLaser.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("缓存空间", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                    Text("${cacheSizeMb}MB", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser)
                                    Text("清理 🧹", fontSize = 9.sp, color = PulseTokens.MagentaLaser.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }

            // 试听与操作输入台
            item(contentType = "input_console") {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("在此输入需要朗读的试听文本...", color = PulseTokens.TextTertiary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseTokens.CyanElectric,
                                unfocusedBorderColor = PulseTokens.SurfaceCardActive,
                                focusedTextColor = PulseTokens.TextPrimary,
                                unfocusedTextColor = PulseTokens.TextPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 4,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        if (!quoteSourceDesc.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = PulseTokens.AmberWarm, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "出处来源: $quoteSourceDesc",
                                    fontSize = 11.sp,
                                    color = PulseTokens.AmberWarm,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { fetchRandomSentence(isOnline = true) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.TextPrimary),
                                    border = PulseTokens.BorderSubtle,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    if (isFetchingQuote) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PulseTokens.CyanElectric, strokeWidth = 1.5.dp)
                                    } else {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("金句", fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = { fetchRandomSentence(isOnline = false) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.TextSecondary),
                                    border = PulseTokens.BorderSubtle,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("语料", fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = { showLiveLogsDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                                    border = PulseTokens.BorderSubtle,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("⚡ 日志", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { startSynthesis(activeProvider) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) PulseTokens.MagentaLaser else PulseTokens.CyanElectric,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.height(40.dp)
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("合成中", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPlaying) "停止" else "试听", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 浮动单手快捷收纳岛
        val hubActionItems = listOf(
            ActionHubItem(
                label = "切换模型",
                icon = Icons.Default.SwapHoriz,
                color = PulseTokens.CyanElectric,
                onClick = { showBottomSheetModelPicker = true }
            ),
            ActionHubItem(
                label = "切换音色",
                icon = Icons.Default.RecordVoiceOver,
                color = PulseTokens.SonicBlue,
                onClick = { showVoicePickerDialog = true }
            ),
            ActionHubItem(
                label = if (isAiModel) "语气提示词" else "语速音调",
                icon = if (isAiModel) Icons.Default.AutoAwesome else Icons.Default.Tune,
                color = PulseTokens.AmberWarm,
                onClick = {
                    if (isAiModel) showPromptDialog = true else showAcousticDialog = true
                }
            ),
            ActionHubItem(
                label = "核心球形态",
                icon = Icons.Default.GraphicEq,
                color = PulseTokens.CyanElectric,
                onClick = {
                    val nextStyle = (settings.acousticCoreStyle + 1) % 3
                    configDataStore.updateSettings(settings.copy(acousticCoreStyle = nextStyle))
                    val styleNames = listOf("极光光晕", "物理点阵", "引力轨道")
                    Toast.makeText(context, "已切换核心形态为: ${styleNames[nextStyle]}", Toast.LENGTH_SHORT).show()
                }
            ),
            ActionHubItem(
                label = "模型配置",
                icon = Icons.Default.Settings,
                color = PulseTokens.SonicBlue,
                onClick = {
                    onNavigateToEditProvider(activeProvider.id)
                }
            ),
            ActionHubItem(
                label = "清理缓存",
                icon = Icons.Default.CleaningServices,
                color = PulseTokens.MagentaLaser,
                onClick = {
                    audioCacheManager.clearAll()
                    Toast.makeText(context, "音频缓存已全部清空", Toast.LENGTH_SHORT).show()
                }
            ),
            ActionHubItem(
                label = if (isPlaying) "停止" else if (isSynthesizing) "合成中..." else "试听",
                icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                color = if (isPlaying) PulseTokens.MagentaLaser else PulseTokens.CyanElectric,
                isLoading = isSynthesizing,
                onClick = { startSynthesis(activeProvider) }
            )
        )

        if (parentPagerState == null || parentPagerState.currentPage == 0) {
            UniversalActionHub(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 76.dp, end = 16.dp),
                items = hubActionItems,
                isHighlighted = isPlaying || isSynthesizing
            )
        }
    }

    // 底部抽屉式模型快速切换
    if (showBottomSheetModelPicker) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheetModelPicker = false },
            sheetState = bottomSheetState,
            containerColor = PulseTokens.SurfaceDark,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = PulseTokens.TextTertiary,
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择发音模型 (长按可自由拖动排序)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PulseTokens.TextPrimary
                    )
                    TextButton(onClick = { onOpenDeck(); showBottomSheetModelPicker = false }) {
                        Text("全景管理", fontSize = 12.5.sp, color = PulseTokens.CyanElectric)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                var hubLocalList by remember(providers) { mutableStateOf(providers) }
                LaunchedEffect(providers) {
                    if (sheetDraggedProviderId == null) {
                        hubLocalList = providers
                    }
                }
                val sheetLazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

                LazyColumn(
                    state = sheetLazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(hubLocalList, key = { it.id }) { provider ->
                        val isSelected = provider.id == activeProvider.id
                        val isBeingDragged = sheetDraggedProviderId == provider.id
                        val itemIndex = hubLocalList.indexOfFirst { it.id == provider.id }

                        val brandColor = if (settings.isProviderCardAccentColorEnabled) {
                            when (provider.type) {
                                ProviderType.EDGE_TTS -> Color(0xFF0078D4)
                                ProviderType.AZURE -> Color(0xFF0089D6)
                                ProviderType.MIMO -> Color(0xFFFF6A00)
                                ProviderType.MINIMAX -> Color(0xFF8B5CF6)
                                ProviderType.DOUBAO -> Color(0xFF3B82F6)
                                ProviderType.STEPFUN -> Color(0xFF06B6D4)
                                ProviderType.OPENAI -> Color(0xFF10A37F)
                                ProviderType.SILICONFLOW -> Color(0xFF6366F1)
                                ProviderType.FISH_AUDIO -> Color(0xFFEC4899)
                                ProviderType.GEMINI -> Color(0xFF9333EA)
                                ProviderType.CUSTOM_HTTP -> Color(0xFFF59E0B)
                                ProviderType.OFFLINE_VITS -> Color(0xFF10B981)
                            }
                        } else {
                            PulseTokens.CyanElectric
                        }

                        val visualShiftY = remember(sheetDraggedProviderId, sheetDragStartIndex, sheetDragTargetIndex, itemIndex) {
                            if (sheetDraggedProviderId == null || isBeingDragged || sheetDragStartIndex == -1 || sheetDragTargetIndex == -1 || itemIndex == -1) {
                                0f
                            } else if (sheetDragStartIndex < sheetDragTargetIndex && itemIndex in (sheetDragStartIndex + 1)..sheetDragTargetIndex) {
                                -sheetItemHeightPx
                            } else if (sheetDragStartIndex > sheetDragTargetIndex && itemIndex in sheetDragTargetIndex until sheetDragStartIndex) {
                                sheetItemHeightPx
                            } else {
                                0f
                            }
                        }
                        val animatedShiftY by animateFloatAsState(
                            targetValue = visualShiftY,
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                            label = "sheet_item_shift"
                        )

                        val sheetCardModifier = if (isBeingDragged) {
                            Modifier
                                .fillMaxWidth()
                                .zIndex(99f)
                                .graphicsLayer {
                                    translationY = sheetTotalDragOffsetY
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                    shadowElevation = 20f
                                }
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .zIndex(1f)
                                .graphicsLayer {
                                    translationY = animatedShiftY
                                }
                        }

                        val surfaceElevatedColor = PulseTokens.SurfaceElevated

                        PulseCard(
                            modifier = sheetCardModifier,
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = when {
                                isBeingDragged -> surfaceElevatedColor
                                isSelected -> PulseTokens.SurfaceCardActive
                                else -> PulseTokens.SurfaceCard
                            },
                            border = when {
                                isBeingDragged -> BorderStroke(2.dp, brandColor)
                                isSelected -> BorderStroke(1.5.dp, brandColor)
                                else -> PulseTokens.BorderSubtle
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
                                            showBottomSheetModelPicker = false
                                            Toast.makeText(context, "已切换为: ${provider.name}", Toast.LENGTH_SHORT).show()
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) brandColor else PulseTokens.TextTertiary)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                provider.name,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) brandColor else PulseTokens.TextPrimary
                                            )
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = brandColor.copy(alpha = 0.2f)
                                                ) {
                                                    Text("主力", fontSize = 9.sp, color = brandColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Text(
                                            "${provider.type.displayName} · ${provider.voiceId}",
                                            fontSize = 11.sp,
                                            color = PulseTokens.TextSecondary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            showBottomSheetModelPicker = false
                                            onNavigateToEditProvider(provider.id)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "配置", tint = PulseTokens.SonicBlue, modifier = Modifier.size(16.dp))
                                    }

                                    // 单一专属长按自由拖拽手柄
                                    val providerId = provider.id
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(color = if (isBeingDragged) brandColor.copy(alpha = 0.2f) else surfaceElevatedColor)
                                            .pointerInput(providerId) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        val idx = hubLocalList.indexOfFirst { it.id == providerId }
                                                        if (idx != -1) {
                                                            sheetDraggedProviderId = providerId
                                                            sheetDragStartIndex = idx
                                                            sheetDragTargetIndex = idx
                                                            sheetTotalDragOffsetY = 0f
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        sheetTotalDragOffsetY += dragAmount.y
                                                        val offsetSteps = (sheetTotalDragOffsetY / sheetItemHeightPx).roundToInt()
                                                        val newTarget = (sheetDragStartIndex + offsetSteps).coerceIn(0, hubLocalList.size - 1)
                                                        if (newTarget != sheetDragTargetIndex) {
                                                            sheetDragTargetIndex = newTarget
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        if (sheetDragStartIndex != -1 && sheetDragTargetIndex != -1 && sheetDragStartIndex != sheetDragTargetIndex) {
                                                            val mutable = hubLocalList.toMutableList()
                                                            val item = mutable.removeAt(sheetDragStartIndex)
                                                            mutable.add(sheetDragTargetIndex, item)
                                                            hubLocalList = mutable
                                                            configDataStore.saveProviders(mutable)
                                                        }
                                                        sheetDraggedProviderId = null
                                                        sheetDragStartIndex = -1
                                                        sheetDragTargetIndex = -1
                                                        sheetTotalDragOffsetY = 0f
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    onDragCancel = {
                                                        sheetDraggedProviderId = null
                                                        sheetDragStartIndex = -1
                                                        sheetDragTargetIndex = -1
                                                        sheetTotalDragOffsetY = 0f
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "按住拖动排序",
                                            tint = if (isBeingDragged) brandColor else PulseTokens.TextSecondary,
                                            modifier = Modifier.size(18.dp)
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

    // ⚡ 实时合成日志与性能诊断台
    if (showLiveLogsDialog) {
        AlertDialog(
            onDismissRequest = { showLiveLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡ 实时合成日志与流水线诊断", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PulseTokens.CyanElectric.copy(alpha = 0.2f)
                    ) {
                        Text("${logs.size} 条", fontSize = 11.sp, color = PulseTokens.CyanElectric, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val logText = logs.joinToString("\n")
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("TTS-Logs", logText))
                                Toast.makeText(context, "已复制全部合成日志", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("复制全部", fontSize = 12.sp, color = PulseTokens.CyanElectric)
                        }

                        TextButton(
                            onClick = {
                                configDataStore.clearLogs()
                                Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("清空日志", fontSize = 12.sp, color = PulseTokens.MagentaLaser)
                        }
                    }

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无合成日志，开始朗读或试听后将在此实时输出毫秒级诊断", fontSize = 12.sp, color = PulseTokens.TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(logs.reversed()) { logLine ->
                                val lineStr = logLine
                                Text(
                                    text = lineStr,
                                    fontSize = 11.sp,
                                    color = when {
                                        lineStr.contains("失败") || lineStr.contains("异常") -> PulseTokens.MagentaLaser
                                        lineStr.contains("就绪") || lineStr.contains("完成") -> PulseTokens.CyanElectric
                                        lineStr.contains("任务接收") -> PulseTokens.AmberWarm
                                        else -> Color.LightGray
                                    },
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLiveLogsDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // ==================== 快捷音色选择弹窗 (支持在线拉取 + 实时搜索) ====================
    if (showVoicePickerDialog) {
        var fetchedVoices by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
        var isFetchingOnlineVoices by remember { mutableStateOf(false) }
        var voiceSearchQuery by remember { mutableStateOf("") }
        var customVoiceInput by remember { mutableStateOf(activeProvider.voiceId) }

        fun fetchVoices() {
            scope.launch {
                isFetchingOnlineVoices = true
                try {
                    val list = TtsProviderManager.getInstance().getAvailableVoices(activeProvider)
                    fetchedVoices = list
                } catch (e: Exception) {
                    Toast.makeText(context, "在线获取音色失败: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isFetchingOnlineVoices = false
                }
            }
        }

        LaunchedEffect(activeProvider.id) {
            fetchVoices()
        }

        val filteredVoices = remember(fetchedVoices, voiceSearchQuery) {
            if (voiceSearchQuery.isBlank()) fetchedVoices
            else fetchedVoices.filter { it.name.contains(voiceSearchQuery, true) || it.id.contains(voiceSearchQuery, true) }
        }

        AlertDialog(
            onDismissRequest = { showVoicePickerDialog = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡ 发音人音色库", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                    IconButton(onClick = { fetchVoices() }, modifier = Modifier.size(28.dp)) {
                        if (isFetchingOnlineVoices) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseTokens.CyanElectric, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("当前引擎: ${activeProvider.name} · 当前音色: ${activeProvider.voiceId.ifBlank { "默认" }}", fontSize = 11.5.sp, color = PulseTokens.CyanElectric)

                    OutlinedTextField(
                        value = voiceSearchQuery,
                        onValueChange = { voiceSearchQuery = it },
                        placeholder = { Text("搜索发音人/语言/音色...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (filteredVoices.isEmpty()) {
                        if (isFetchingOnlineVoices) {
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = PulseTokens.CyanElectric)
                            }
                        } else {
                            Text("未探测到在线音色，您可手动输入音色 ID：", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                            OutlinedTextField(
                                value = customVoiceInput,
                                onValueChange = { customVoiceInput = it },
                                label = { Text("自定义音色 ID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filteredVoices.size) { idx ->
                                val v = filteredVoices[idx]
                                val isSelected = activeProvider.voiceId == v.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val updated = activeProvider.copy(voiceId = v.id)
                                            configDataStore.updateProvider(updated)
                                            showVoicePickerDialog = false
                                            Toast.makeText(context, "已切换音色: ${v.name}", Toast.LENGTH_SHORT).show()
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PulseTokens.CyanElectric.copy(alpha = 0.2f) else PulseTokens.SurfaceCard
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(v.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                            Text("${v.id} · ${v.locale}", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                        }
                                        if (isSelected) {
                                            Text("✓", color = PulseTokens.CyanElectric, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customVoiceInput.isNotBlank()) {
                            val updated = activeProvider.copy(voiceId = customVoiceInput.trim())
                            configDataStore.updateProvider(updated)
                        }
                        showVoicePickerDialog = false
                    }
                ) {
                    Text("完成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoicePickerDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // ==================== 快捷 Prompt 提示词弹窗 ====================
    if (showPromptDialog) {
        val promptPresets = listOf(
            "自然流畅" to "请用自然、沉稳且富有感染力的语气朗读文本。",
            "激情昂扬" to "请用充满激情、热烈且铿锵有力的语调进行朗读。",
            "温柔低语" to "请用轻柔、舒缓、富有治愈感的伴读语气进行朗读。",
            "严肃新闻" to "请用字正腔圆、严谨庄重的新闻主播声调进行播报。",
            "悬疑惊悚" to "请用压抑低沉、节奏缓慢且充满悬疑感的语气朗读。",
            "深沉磁性" to "请用低沉深情、富有磁性与故事感的成熟语调朗读。"
        )
        var promptInput by remember { mutableStateOf(activeProvider.promptInstruction) }

        AlertDialog(
            onDismissRequest = { showPromptDialog = false },
            title = { Text("🎭 配置 AI TTS 情绪与系统指令", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("系统指令 / 情绪 Prompt") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                    Text("情绪预设模板 (点击一键填入):", fontSize = 11.5.sp, color = PulseTokens.TextSecondary, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        promptPresets.take(3).forEach { (name, text) ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { promptInput = text },
                                shape = RoundedCornerShape(8.dp),
                                color = PulseTokens.SurfaceCard,
                                border = PulseTokens.BorderSubtle
                            ) {
                                Text(name, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        promptPresets.drop(3).forEach { (name, text) ->
                            Surface(
                                modifier = Modifier.weight(1f).clickable { promptInput = text },
                                shape = RoundedCornerShape(8.dp),
                                color = PulseTokens.SurfaceCard,
                                border = PulseTokens.BorderSubtle
                            ) {
                                Text(name, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = activeProvider.copy(promptInstruction = promptInput)
                        configDataStore.updateProvider(updated)
                        showPromptDialog = false
                        Toast.makeText(context, "AI 情绪指令已更新", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromptDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // ==================== 快捷传统 TTS 声学参数微调弹窗 ====================
    if (showAcousticDialog) {
        var currentSpeed by remember { mutableFloatStateOf(activeProvider.speed) }
        var currentPitch by remember { mutableFloatStateOf(activeProvider.pitch) }

        AlertDialog(
            onDismissRequest = { showAcousticDialog = false },
            title = { Text("🎚️ 传统 TTS 声学参数微调", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("发音语速", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                            Text("${String.format("%.2f", currentSpeed)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                        }
                        Slider(
                            value = currentSpeed,
                            onValueChange = { currentSpeed = it },
                            valueRange = 0.2f..3.0f,
                            colors = SliderDefaults.colors(thumbColor = PulseTokens.CyanElectric, activeTrackColor = PulseTokens.CyanElectric)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("发音音高", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                            Text(String.format("%.2f", currentPitch), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.SonicBlue)
                        }
                        Slider(
                            value = currentPitch,
                            onValueChange = { currentPitch = it },
                            valueRange = 0.5f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = PulseTokens.SonicBlue, activeTrackColor = PulseTokens.SonicBlue)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = activeProvider.copy(speed = currentSpeed, pitch = currentPitch)
                        configDataStore.updateProvider(updated)
                        showAcousticDialog = false
                        Toast.makeText(context, "声学参数已更新", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAcousticDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
