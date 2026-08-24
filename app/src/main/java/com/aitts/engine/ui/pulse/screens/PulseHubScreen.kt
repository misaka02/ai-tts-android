package com.aitts.engine.ui.pulse.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.ui.pulse.components.OneHandedActionHub
import com.aitts.engine.ui.pulse.components.PulseAcousticCore
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseHubScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onOpenDeck: () -> Unit = {}
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

    val audioPlayer = remember { AndroidAudioPlayer(context) }
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

    var showBottomSheetModelPicker by remember { mutableStateOf(false) }
    var showLiveLogsDialog by remember { mutableStateOf(false) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val visualizerManager = remember { AudioVisualizerManager.getInstance() }
    val spectrumBands by visualizerManager.spectrumFlow.collectAsState()
    val rmsEnergy by visualizerManager.rmsEnergyFlow.collectAsState()

    val density = LocalDensity.current
    val sheetItemHeightPx = with(density) { 68.dp.toPx() }
    var sheetDraggedProviderId by remember { mutableStateOf<String?>(null) }
    var sheetDragDeltaY by remember { mutableFloatStateOf(0f) }

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
                val result = TtsProviderManager.getInstance().synthesize(testText, testConfig)
                val costMs = System.currentTimeMillis() - startTime

                if (result.isSuccess) {
                    val audioData = result.getOrNull() ?: ByteArray(0)
                    if (audioData.isNotEmpty()) {
                        lastLatencyMs = costMs
                        isSynthesizing = false
                        isPlaying = true

                        configDataStore.log("✅ 收到音频数据: ${audioData.size} 字节, 首包耗时=${costMs}ms, 采样率=${provider.sampleRate}Hz")
                        configDataStore.log("🔊 内存音频直出播放开始, 启动 32 频段 STFT 示波分析")

                        audioPlayer.playAudioBytes(
                            audioData,
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
            } catch (e: Exception) {
                isSynthesizing = false
                isPlaying = false
                configDataStore.log("💥 调用异常: ${e.message}")
                Toast.makeText(context, "调用异常: ${e.message}", Toast.LENGTH_LONG).show()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
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
                                    text = "v3.4.2",
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
                            modifier = Modifier.clickable { showBottomSheetModelPicker = true }
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
                                        .background(PulseTokens.CyanElectric)
                                )
                                Text(
                                    text = activeProvider.name,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTokens.TextPrimary
                                )
                                Text(
                                    text = "· 切换",
                                    fontSize = 11.sp,
                                    color = PulseTokens.SonicBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        PulseAcousticCore(
                            spectrumBands = spectrumBands,
                            rmsEnergy = rmsEnergy,
                            isPlaying = isPlaying,
                            isSynthesizing = isSynthesizing,
                            onClick = { startSynthesis(activeProvider) }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("合成延迟", fontSize = 10.5.sp, color = PulseTokens.TextTertiary)
                                Text("${lastLatencyMs ?: "--"}ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("音频采样率", fontSize = 10.5.sp, color = PulseTokens.TextTertiary)
                                Text("${activeProvider.sampleRate / 1000} kHz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("混合流水线", fontSize = 10.5.sp, color = PulseTokens.TextTertiary)
                                Text("首句秒开", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.AmberWarm)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("清晰度增强", fontSize = 10.5.sp, color = PulseTokens.TextTertiary)
                                Text(if (settings.voiceClarityBoostEnabled) "开启" else "关闭", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.SonicBlue)
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
                            maxLines = 4
                        )

                        if (!quoteSourceDesc.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "来源: $quoteSourceDesc",
                                fontSize = 11.sp,
                                color = PulseTokens.AmberWarm,
                                maxLines = 1
                            )
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

        // 右下角大拇指悬浮收纳岛
        OneHandedActionHub(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp),
            isPlaying = isPlaying,
            isSynthesizing = isSynthesizing,
            activeModelName = activeProvider.name,
            onTogglePlay = { startSynthesis(activeProvider) },
            onChangeText = { fetchRandomSentence(isOnline = false) },
            onOpenModelSelector = { showBottomSheetModelPicker = true },
            onOpenModelConfig = { onNavigateToEditProvider(activeProvider.id) }
        )
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
                val currentHubListState by rememberUpdatedState(hubLocalList)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(hubLocalList, key = { _, p -> p.id }) { index, provider ->
                        val isSelected = provider.id == activeProvider.id
                        val isBeingDragged = sheetDraggedProviderId == provider.id

                        val itemModifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isBeingDragged) 10f else 1f)
                            .offset {
                                if (isBeingDragged) IntOffset(0, sheetDragDeltaY.roundToInt()) else IntOffset.Zero
                            }
                            .scale(if (isBeingDragged) 1.03f else 1f)

                        PulseCard(
                            modifier = itemModifier,
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = when {
                                isBeingDragged -> PulseTokens.SurfaceElevated
                                isSelected -> PulseTokens.SurfaceCardActive
                                else -> PulseTokens.SurfaceCard
                            },
                            border = when {
                                isBeingDragged -> BorderStroke(2.dp, PulseTokens.CyanElectric)
                                isSelected -> PulseTokens.BorderActive
                                else -> PulseTokens.BorderSubtle
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column {
                                        Text(
                                            provider.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextPrimary
                                        )
                                        Text(
                                            "${provider.type.displayName} · ${provider.voiceId}",
                                            fontSize = 11.sp,
                                            color = PulseTokens.TextSecondary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            configDataStore.pinProviderToTop(provider.id)
                                            Toast.makeText(context, "已将 ${provider.name} 置顶", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.VerticalAlignTop, contentDescription = "置顶", tint = PulseTokens.CyanElectric, modifier = Modifier.size(15.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                configDataStore.moveProviderUp(provider.id)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "上移", tint = if (index > 0) PulseTokens.TextSecondary else Color.DarkGray, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < hubLocalList.size - 1) {
                                                configDataStore.moveProviderDown(provider.id)
                                            }
                                        },
                                        enabled = index < hubLocalList.size - 1,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "下移", tint = if (index < hubLocalList.size - 1) PulseTokens.TextSecondary else Color.DarkGray, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            showBottomSheetModelPicker = false
                                            onNavigateToEditProvider(provider.id)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "配置", tint = PulseTokens.SonicBlue, modifier = Modifier.size(15.dp))
                                    }

                                    // 长按平滑自由拖拽手柄
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .pointerInput(provider.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        sheetDraggedProviderId = provider.id
                                                        sheetDragDeltaY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        sheetDragDeltaY += dragAmount.y
                                                        val currentIndex = hubLocalList.indexOfFirst { it.id == provider.id }
                                                        if (currentIndex != -1) {
                                                            val offsetSteps = (sheetDragDeltaY / sheetItemHeightPx).toInt()
                                                            val targetIndex = (currentIndex + offsetSteps).coerceIn(0, hubLocalList.size - 1)
                                                            if (targetIndex != currentIndex) {
                                                                val mutable = hubLocalList.toMutableList()
                                                                val item = mutable.removeAt(currentIndex)
                                                                mutable.add(targetIndex, item)
                                                                hubLocalList = mutable
                                                                sheetDragDeltaY -= (targetIndex - currentIndex) * sheetItemHeightPx
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        sheetDraggedProviderId = null
                                                        sheetDragDeltaY = 0f
                                                        configDataStore.saveProviders(hubLocalList)
                                                    },
                                                    onDragCancel = {
                                                        sheetDraggedProviderId = null
                                                        sheetDragDeltaY = 0f
                                                    }
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DragHandle,
                                            contentDescription = "按住拖动排序",
                                            tint = if (isBeingDragged) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
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
}
