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
import androidx.compose.material.icons.filled.ContentCopy
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
import com.aitts.engine.data.AppLogEntry
import com.aitts.engine.data.LogLevel
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.requiresClientSpeedScaling
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
private data class RequestLogSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val summaryTitle: String,
    val subtitle: String?,
    val isComplete: Boolean,
    val isError: Boolean,
    val entries: List<AppLogEntry>
)

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
    val structuredLogs by configDataStore.structuredLogsFlow.collectAsState()

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
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    var showClearLogsConfirmDialog by remember { mutableStateOf(false) }
    var logViewMode by remember { mutableStateOf(0) }

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
    var sheetDraggedItemViewportY by remember { mutableFloatStateOf(-1f) }

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
        isSynthesizing = false
        val sId = configDataStore.activeSessionId
        if (sId != null) {
            com.aitts.engine.network.SharedHttpClient.cancelSession(sId)
            configDataStore.log("⏹️ 朗读播音结束 (手动停止)", sessionId = sId)
            configDataStore.activeSessionId = null
        }
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

        val trialSessionId = "trial-" + java.util.UUID.randomUUID().toString().take(6)
        configDataStore.activeSessionId = trialSessionId

        scope.launch {
            try {
                val effectiveSpeed = (provider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (provider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = provider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                configDataStore.log("🚀 发起语音合成: 模型=【${provider.name}】(${provider.type.displayName}), 音色=${provider.voiceId.ifBlank { "默认" }}, 文本=“${testText.take(28)}...”", sessionId = trialSessionId)
                configDataStore.log("🌐 正在连接服务端端点: ${provider.baseUrl.ifBlank { "官方直接协议" }}", sessionId = trialSessionId)

                val startTime = System.currentTimeMillis()
                var firstChunkReceived = false

                val result = if (provider.isStreamingEnabled) {
                    val streamBuffer = java.io.ByteArrayOutputStream()
                    val streamRes = TtsProviderManager.getInstance().synthesizeStreaming(testText, testConfig, trialSessionId) { chunk ->
                        if (!firstChunkReceived) {
                            firstChunkReceived = true
                            lastLatencyMs = System.currentTimeMillis() - startTime
                            configDataStore.log("[Stream] 首包耗时: ${lastLatencyMs}ms (推流中)", sessionId = trialSessionId)
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

                        configDataStore.log("[Audio] 收到数据: ${audioData.size} 字节, 耗时=${costMs}ms, 采样率=${provider.sampleRate}Hz", sessionId = trialSessionId)
                        configDataStore.log("[Audio] 开始播放", sessionId = trialSessionId)

                        // 仅当引擎流式裸流时钟固定(如 MiMo)或自定义节点未配 ${speed} 时由播放器执行时间缩放；其余所有引擎已在合成期原生注入语速，播放器以 1.0x 原声保真直出，杜绝二次倍速/减速
                        val playbackSpeed = if (provider.copy(speed = effectiveSpeed).requiresClientSpeedScaling(isStreaming = provider.isStreamingEnabled)) effectiveSpeed else 1.0f
                        audioPlayer.playAudioBytes(
                            audioBytes = audioData,
                            speed = playbackSpeed,
                            onCompletion = {
                                isPlaying = false
                                configDataStore.log("[Player] 播放完毕", sessionId = trialSessionId)
                                if (configDataStore.activeSessionId == trialSessionId) {
                                    configDataStore.activeSessionId = null
                                }
                            },
                            onError = { err ->
                                isPlaying = false
                                configDataStore.log("[Error] 播放异常: $err", sessionId = trialSessionId)
                                if (configDataStore.activeSessionId == trialSessionId) {
                                    configDataStore.activeSessionId = null
                                }
                            }
                        )
                    } else {
                        isSynthesizing = false
                        isPlaying = false
                        configDataStore.log("[Warn] 合成音频流为空 (0 字节)", sessionId = trialSessionId)
                        if (configDataStore.activeSessionId == trialSessionId) {
                            configDataStore.activeSessionId = null
                        }
                        Toast.makeText(context, "合成音频流为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isSynthesizing = false
                    isPlaying = false
                    val err = result.exceptionOrNull()?.message ?: "未知异常"
                    configDataStore.log("[Error] 合成失败: $err", sessionId = trialSessionId)
                    if (configDataStore.activeSessionId == trialSessionId) {
                        configDataStore.activeSessionId = null
                    }
                    Toast.makeText(context, "合成失败: $err", Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                isSynthesizing = false
                isPlaying = false
                val msg = t.message ?: t.javaClass.simpleName
                configDataStore.log("[Error] 调用异常: $msg", sessionId = trialSessionId)
                if (configDataStore.activeSessionId == trialSessionId) {
                    configDataStore.activeSessionId = null
                }
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
                                configDataStore.updateSettings(settings.copy(acousticCoreStyle = nextStyle % 4))
                                val styleName = when (nextStyle % 4) {
                                    0 -> "极光光晕"
                                    1 -> "物理点阵"
                                    2 -> "引力轨道"
                                    else -> "专业声学频谱仪"
                                }
                                Toast.makeText(context, "频谱风格: $styleName", Toast.LENGTH_SHORT).show()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        )

                        Text(
                            text = "轻触试听 · 双击切换频谱风格",
                            fontSize = 10.5.sp,
                            color = PulseTokens.TextTertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4 大交互式状态卡片
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
                                    Text("测速", fontSize = 9.sp, color = PulseTokens.CyanElectric.copy(alpha = 0.8f))
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
                                    Text("换音色", fontSize = 9.sp, color = PulseTokens.SonicBlue.copy(alpha = 0.8f))
                                }
                            }

                            // 卡片 3: 模型参数 (提示词 Prompt / 语速音高)
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
                                        Text("提示词", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                        Text(if (activeProvider.promptInstruction.isNotBlank()) "自定义" else "标准", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.AmberWarm)
                                        Text("Prompt", fontSize = 9.sp, color = PulseTokens.AmberWarm.copy(alpha = 0.8f))
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
                                        Text("微调", fontSize = 9.sp, color = PulseTokens.CyanElectric.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            // 卡片 4: 缓存空间与一键清理 (支持紧凑显示与二次确认拦截)
                            var cacheStatsVersion by remember { mutableStateOf(0) }
                            val (cacheCount, cacheSizeMb) = remember(isPlaying, cacheStatsVersion) { audioCacheManager.getStats() }
                            val formattedCacheSize = when {
                                cacheSizeMb <= 0.05f -> "0 MB"
                                cacheSizeMb < 10f -> String.format(java.util.Locale.US, "%.1f MB", cacheSizeMb)
                                else -> String.format(java.util.Locale.US, "%.0f MB", cacheSizeMb)
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        showClearCacheConfirmDialog = true
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = PulseTokens.SurfaceElevated,
                                border = BorderStroke(1.dp, PulseTokens.MagentaLaser.copy(alpha = 0.25f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("缓存空间", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                    Text(formattedCacheSize, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser, maxLines = 1)
                                    Text("清理", fontSize = 9.sp, color = PulseTokens.MagentaLaser.copy(alpha = 0.8f))
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
                                    Text("在线语料", fontSize = 11.5.sp)
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
                                    Text("本地语料", fontSize = 11.5.sp)
                                }

                                Button(
                                    onClick = { showLiveLogsDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                                    border = PulseTokens.BorderSubtle,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text("日志", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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

        // 浮动单手快捷收纳岛 (功能深度模块化成组 + 核心球形态支持连续连按预览)
        val hubActionItems = listOf(
            // 组 1: 发音控制核心组 (4大模型音色操作紧密相连)
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
                label = "模型配置",
                icon = Icons.Default.Settings,
                color = PulseTokens.SonicBlue,
                onClick = {
                    onNavigateToEditProvider(activeProvider.id)
                }
            ),
            // 组 2: 视觉与试听组 (核心球形态支持连续点按循环预览，autoDismiss = false)
            ActionHubItem(
                label = "核心球形态",
                icon = Icons.Default.GraphicEq,
                color = PulseTokens.CyanElectric,
                autoDismiss = false,
                onClick = {
                    val nextStyle = (settings.acousticCoreStyle + 1) % 3
                    configDataStore.updateSettings(settings.copy(acousticCoreStyle = nextStyle))
                    val styleNames = listOf("极光光晕", "物理点阵", "引力轨道")
                    Toast.makeText(context, "核心形态: ${styleNames[nextStyle]}", Toast.LENGTH_SHORT).show()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ),
            ActionHubItem(
                label = if (isPlaying) "停止" else if (isSynthesizing) "合成中..." else "试听",
                icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                color = if (isPlaying) PulseTokens.MagentaLaser else PulseTokens.CyanElectric,
                isLoading = isSynthesizing,
                onClick = { startSynthesis(activeProvider) }
            ),
            // 组 3: 系统维护组 (带安全二次确认)
            ActionHubItem(
                label = "清理缓存",
                icon = Icons.Default.CleaningServices,
                color = PulseTokens.MagentaLaser,
                onClick = {
                    showClearCacheConfirmDialog = true
                }
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

                LaunchedEffect(sheetDraggedProviderId) {
                    if (sheetDraggedProviderId != null) {
                        while (true) {
                            val layoutInfo = sheetLazyListState.layoutInfo
                            val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                            if (sheetDraggedItemViewportY >= 0f && viewportHeight > 100f) {
                                val edgeThreshold = 80f
                                val scrollDelta = when {
                                    sheetDraggedItemViewportY < edgeThreshold -> {
                                        val factor = ((edgeThreshold - sheetDraggedItemViewportY) / edgeThreshold).coerceIn(0f, 1f)
                                        -(factor * 14f).coerceAtLeast(3f)
                                    }
                                    sheetDraggedItemViewportY > (viewportHeight - edgeThreshold) -> {
                                        val factor = ((sheetDraggedItemViewportY - (viewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                                        (factor * 14f).coerceAtLeast(3f)
                                    }
                                    else -> 0f
                                }
                                if (scrollDelta != 0f) {
                                    val consumed = sheetLazyListState.scrollBy(scrollDelta)
                                    if (consumed != 0f) {
                                        sheetTotalDragOffsetY += consumed
                                        val offsetSteps = (sheetTotalDragOffsetY / sheetItemHeightPx).roundToInt()
                                        val newTarget = (sheetDragStartIndex + offsetSteps).coerceIn(0, hubLocalList.size - 1)
                                        if (newTarget != sheetDragTargetIndex) {
                                            sheetDragTargetIndex = newTarget
                                        }
                                    }
                                }
                            }
                            delay(16)
                        }
                    }
                }

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
                                                            val itemInfo = sheetLazyListState.layoutInfo.visibleItemsInfo.find { it.index == idx }
                                                            sheetDraggedItemViewportY = (itemInfo?.offset?.toFloat() ?: 0f) + sheetItemHeightPx / 2f
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        sheetTotalDragOffsetY += dragAmount.y
                                                        sheetDraggedItemViewportY += dragAmount.y
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
                                                        sheetDraggedItemViewportY = -1f
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    },
                                                    onDragCancel = {
                                                        sheetDraggedProviderId = null
                                                        sheetDragStartIndex = -1
                                                        sheetDragTargetIndex = -1
                                                        sheetTotalDragOffsetY = 0f
                                                        sheetDraggedItemViewportY = -1f
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
        var expandedSessionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
        val requestSessions = remember(structuredLogs) {
            val sessions = mutableListOf<RequestLogSession>()
            var currentSessionEntries = mutableListOf<AppLogEntry>()
            var currentSessionId: String? = null

            fun finalizeSession() {
                if (currentSessionEntries.isEmpty()) return
                val first = currentSessionEntries.first()
                val last = currentSessionEntries.last()
                val sId = currentSessionId ?: first.sessionId ?: ("req-" + first.timestamp.take(5).replace(":", ""))

                val hasErr = currentSessionEntries.any { 
                    it.level == LogLevel.ERROR || it.title.contains("失败") || it.title.contains("异常") 
                }
                val hasComplete = currentSessionEntries.any { 
                    it.title.contains("播音结束") || it.title.contains("播放完成") || it.title.contains("全部完成") 
                }

                var summaryTitle = first.title
                if (summaryTitle.startsWith("🚀 发起语音合成")) {
                    val snippet = summaryTitle.substringAfter("文本=“", "").substringBefore("”", "")
                    if (snippet.isNotBlank()) {
                        summaryTitle = "试听合成 · “${snippet.take(24)}...”"
                    }
                } else if (summaryTitle.startsWith("收到朗读请求")) {
                    summaryTitle = "系统朗读 · " + (first.details ?: "${first.title}")
                }

                val ttfbEntry = currentSessionEntries.find { it.title.contains("TTFB") || it.details?.contains("TTFB") == true }
                val bytesEntry = currentSessionEntries.find { it.title.contains("字节") || it.details?.contains("字节") == true }
                
                val metricParts = mutableListOf<String>()
                if (ttfbEntry != null) {
                    val m = Regex("""TTFB[=:]?\s*(\d+)ms""").find(ttfbEntry.title + " " + (ttfbEntry.details ?: ""))
                    if (m != null) metricParts.add("首包 ${m.groupValues[1]}ms")
                }
                if (bytesEntry != null) {
                    val m = Regex("""(\d+)\s*字节""").find(bytesEntry.title + " " + (bytesEntry.details ?: ""))
                    if (m != null) {
                        val kb = m.groupValues[1].toIntOrNull()?.let { it / 1024 } ?: 0
                        metricParts.add("${kb} KB")
                    }
                }
                val subText = if (metricParts.isNotEmpty()) metricParts.joinToString(" · ") else (currentSessionEntries.find { !it.details.isNullOrBlank() }?.details)

                sessions.add(
                    RequestLogSession(
                        sessionId = sId,
                        startTime = first.timestamp,
                        endTime = last.timestamp,
                        summaryTitle = summaryTitle,
                        subtitle = subText,
                        isComplete = hasComplete,
                        isError = hasErr,
                        entries = currentSessionEntries.toList()
                    )
                )
                currentSessionEntries = mutableListOf()
                currentSessionId = null
            }

            for (entry in structuredLogs) {
                val isStart = entry.title.contains("收到朗读请求") || 
                              entry.title.contains("发起语音合成") || 
                              entry.title.contains("阅读器调用")
                val isEnd = entry.title.contains("播音结束") || 
                            entry.title.contains("播放完成") || 
                            entry.title.contains("全部完成") || 
                            entry.title.contains("合成失败") || 
                            entry.title.contains("异常中止") ||
                            entry.title.contains("已取消")

                val entrySessionId = entry.sessionId

                if (isStart && currentSessionEntries.isNotEmpty()) {
                    finalizeSession()
                } else if (entrySessionId != null && currentSessionId != null && entrySessionId != currentSessionId) {
                    finalizeSession()
                }

                if (entrySessionId != null) {
                    currentSessionId = entrySessionId
                }
                currentSessionEntries.add(entry)

                if (isEnd) {
                    finalizeSession()
                }
            }

            if (currentSessionEntries.isNotEmpty()) {
                finalizeSession()
            }

            sessions.reversed()
        }

        AlertDialog(
            onDismissRequest = { showLiveLogsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚡ 实时合成日志诊断", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PulseTokens.CyanElectric.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (logViewMode == 0) "${requestSessions.size} 批请求" else "${logs.size} 条",
                            fontSize = 11.sp,
                            color = PulseTokens.CyanElectric,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 顶部控制条: 模式选择 + 复制全部 + 清空
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (logViewMode == 0) PulseTokens.CyanElectric.copy(alpha = 0.25f) else Color.Transparent,
                                border = if (logViewMode == 0) BorderStroke(1.dp, PulseTokens.CyanElectric) else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.clickable { logViewMode = 0 }
                            ) {
                                Text(
                                    text = "按请求分组",
                                    fontSize = 11.sp,
                                    fontWeight = if (logViewMode == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (logViewMode == 0) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (logViewMode == 1) PulseTokens.CyanElectric.copy(alpha = 0.25f) else Color.Transparent,
                                border = if (logViewMode == 1) BorderStroke(1.dp, PulseTokens.CyanElectric) else BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.clickable { logViewMode = 1 }
                            ) {
                                Text(
                                    text = "原始流水",
                                    fontSize = 11.sp,
                                    fontWeight = if (logViewMode == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (logViewMode == 1) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                                Text("复制全部", fontSize = 11.5.sp, color = PulseTokens.CyanElectric)
                            }

                            TextButton(
                                onClick = {
                                    showClearLogsConfirmDialog = true
                                }
                            ) {
                                Text("清空", fontSize = 11.5.sp, color = PulseTokens.MagentaLaser)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "暂无合成日志，开始朗读或试听后将在此实时输出结构化诊断记录",
                                fontSize = 12.sp,
                                color = PulseTokens.TextTertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else if (logViewMode == 0) {
                        // 按请求会话分组视图
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(requestSessions) { session ->
                                val isExpanded = expandedSessionIds.contains(session.sessionId)
                                val (statusText, statusColor) = when {
                                    session.isError -> "失败/异常" to PulseTokens.MagentaLaser
                                    session.isComplete -> "播音完成" to Color(0xFF34D399)
                                    else -> "推流中" to PulseTokens.CyanElectric
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF161B22),
                                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = statusColor.copy(alpha = 0.2f)
                                                ) {
                                                    Text(statusText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                                Text(
                                                    text = "#${session.sessionId}",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.LightGray,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                            Text("${session.startTime} ➔ ${session.endTime.takeLast(6)}", fontSize = 8.5.sp, color = PulseTokens.TextTertiary)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = session.summaryTitle,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = if (isExpanded) 4 else 2
                                        )

                                        if (!session.subtitle.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = session.subtitle,
                                                fontSize = 10.sp,
                                                color = Color(0xFF8B949E),
                                                maxLines = if (isExpanded) 4 else 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // 单请求控制按钮：复制本条 + 展开/折叠
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PulseTokens.CyanElectric.copy(alpha = 0.15f),
                                                border = BorderStroke(0.8.dp, PulseTokens.CyanElectric.copy(alpha = 0.5f)),
                                                modifier = Modifier.clickable {
                                                    val sessionText = session.entries.joinToString("\n") { it.formatToString() }
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("TTS-Request-${session.sessionId}", sessionText))
                                                    Toast.makeText(context, "已复制单次完整请求日志 (#${session.sessionId})", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(11.dp))
                                                    Text("复制本条请求", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PulseTokens.CyanElectric)
                                                }
                                            }

                                            Text(
                                                text = if (isExpanded) "收起详情流程 ▲" else "查看详情流程 (${session.entries.size}步) ▼",
                                                fontSize = 10.sp,
                                                color = PulseTokens.CyanElectric,
                                                modifier = Modifier
                                                    .clickable {
                                                        expandedSessionIds = if (isExpanded) {
                                                            expandedSessionIds - session.sessionId
                                                        } else {
                                                            expandedSessionIds + session.sessionId
                                                        }
                                                    }
                                                    .padding(2.dp)
                                            )
                                        }

                                        // 展开后的具体单条事件
                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF090D13), RoundedCornerShape(4.dp))
                                                    .padding(4.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                session.entries.forEachIndexed { stepIdx, subEntry ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${stepIdx + 1}. ${subEntry.level.label} [${subEntry.tag}] ${subEntry.title}",
                                                            fontSize = 10.sp,
                                                            color = when (subEntry.level) {
                                                                LogLevel.ERROR -> PulseTokens.MagentaLaser
                                                                LogLevel.SUCCESS -> Color(0xFF34D399)
                                                                LogLevel.METRIC -> Color(0xFFA78BFA)
                                                                LogLevel.WARN -> PulseTokens.AmberWarm
                                                                else -> Color.White
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(subEntry.timestamp, fontSize = 8.5.sp, color = PulseTokens.TextTertiary)
                                                    }
                                                    if (!subEntry.details.isNullOrBlank()) {
                                                        Text(
                                                            text = subEntry.details,
                                                            fontSize = 9.sp,
                                                            color = Color.Gray,
                                                            modifier = Modifier.padding(start = 12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 原始流水视图
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Color(0xFF0D1117), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (structuredLogs.isNotEmpty()) {
                                items(structuredLogs.reversed()) { entry ->
                                    val (badgeBg, badgeText) = when (entry.level) {
                                        LogLevel.INFO -> PulseTokens.CyanElectric.copy(alpha = 0.18f) to PulseTokens.CyanElectric
                                        LogLevel.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.2f) to Color(0xFF34D399)
                                        LogLevel.WARN -> PulseTokens.AmberWarm.copy(alpha = 0.2f) to PulseTokens.AmberWarm
                                        LogLevel.ERROR -> PulseTokens.MagentaLaser.copy(alpha = 0.22f) to PulseTokens.MagentaLaser
                                        LogLevel.METRIC -> Color(0xFF8B5CF6).copy(alpha = 0.2f) to Color(0xFFA78BFA)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF161B22), RoundedCornerShape(6.dp))
                                            .padding(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = badgeBg) {
                                                    Text(entry.level.label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeText, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                                Surface(shape = RoundedCornerShape(4.dp), color = Color.White.copy(alpha = 0.1f)) {
                                                    Text(entry.tag, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.LightGray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                            Text(entry.timestamp, fontSize = 9.5.sp, color = PulseTokens.TextTertiary, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(entry.title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        if (!entry.details.isNullOrBlank()) {
                                            Text(entry.details, fontSize = 10.sp, color = Color(0xFF8B949E), lineHeight = 13.sp)
                                        }
                                    }
                                }
                            } else {
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
                }
            },
            confirmButton = {
                TextButton(onClick = { showLiveLogsDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 清理缓存二次确认弹窗
    if (showClearCacheConfirmDialog) {
        val (cacheCount, cacheSizeMb) = remember(isPlaying) { audioCacheManager.getStats() }
        val formattedCacheSize = when {
            cacheSizeMb <= 0.05f -> "0 MB"
            cacheSizeMb < 10f -> String.format(java.util.Locale.US, "%.1f MB", cacheSizeMb)
            else -> String.format(java.util.Locale.US, "%.0f MB", cacheSizeMb)
        }
        AlertDialog(
            onDismissRequest = { showClearCacheConfirmDialog = false },
            title = { Text("确认清空音频缓存？", fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser) },
            text = {
                Text(
                    text = "当前已缓存 ${cacheCount} 个音频分块（共 $formattedCacheSize）。\n\n清空后将释放本地存储空间，下次朗读或试听需重新联网或离线推理生成。确定清空吗？",
                    fontSize = 13.sp,
                    color = PulseTokens.TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        audioCacheManager.clearAll()
                        showClearCacheConfirmDialog = false
                        Toast.makeText(context, "已释放本地音频缓存 ($formattedCacheSize)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.MagentaLaser, contentColor = Color.White)
                ) {
                    Text("确定清空", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                    Text("取消", color = PulseTokens.TextSecondary)
                }
            },
            containerColor = PulseTokens.SurfaceElevated
        )
    }

    // 清空日志二次确认弹窗
    if (showClearLogsConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirmDialog = false },
            title = { Text("确认清空全部合成日志？", fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser) },
            text = {
                Text(
                    text = "清空后当前的调试与网络首包/吞吐诊断记录将无法恢复。确定清空吗？",
                    fontSize = 13.sp,
                    color = PulseTokens.TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        configDataStore.clearLogs()
                        showClearLogsConfirmDialog = false
                        Toast.makeText(context, "日志已清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.MagentaLaser, contentColor = Color.White)
                ) {
                    Text("确定清空", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsConfirmDialog = false }) {
                    Text("取消", color = PulseTokens.TextSecondary)
                }
            },
            containerColor = PulseTokens.SurfaceElevated
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
