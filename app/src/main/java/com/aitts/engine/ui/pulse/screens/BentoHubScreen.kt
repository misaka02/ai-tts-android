package com.aitts.engine.ui.pulse.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import kotlin.math.roundToInt
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 🚀 Bento 全景网格矩阵工作台 (Bento Panoramic Grid Hub)
 * 基于 Modern Design System 2.0 全新重构：
 * 1. Master Bento Card：32-Band 实时 FFT 物理频域示波器与主力发音中枢；
 * 2. 2x2 Telemetry Bento Matrix：首字延迟、采样率/吞吐、声学倍率、自愈缓存；
 * 3. 4-Scenario Corpus Grid：小说、新闻、对白、极客 4 大场景即时试听矩阵；
 * 4. Bento 专属大拇指单手收纳岛。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BentoHubScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onOpenDeck: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()
        ?: TtsProviderConfig(id = "default", type = ProviderType.EDGE_TTS, name = "默认引擎")

    val isAiModel = activeProvider.type.requiresApiKey || activeProvider.type in listOf(
        ProviderType.MIMO, ProviderType.MINIMAX, ProviderType.DOUBAO, ProviderType.SILICONFLOW,
        ProviderType.FISH_AUDIO, ProviderType.STEPFUN, ProviderType.OPENAI, ProviderType.GEMINI
    )

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.stop() }
    }

    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var lastLatencyMs by remember { mutableStateOf<Long?>(null) }
    var testText by remember { mutableStateOf("欢迎使用 Bento 网格工作台。系统已启动多模态高保真神经网络语音流。") }
    var quoteSourceDesc by remember { mutableStateOf<String?>("《经典文学》 · 编选") }
    var quoteButtonText by remember { mutableStateOf("一言金句") }
    var isFetchingQuote by remember { mutableStateOf(false) }

    var showVoicePickerDialog by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var showAcousticDialog by remember { mutableStateOf(false) }
    var showBottomSheetModelPicker by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val visualizerManager = remember { AudioVisualizerManager.getInstance() }
    val spectrumBands by visualizerManager.spectrumFlow.collectAsState()
    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }

    val activeBrandColor = if (settings.isProviderCardAccentColorEnabled) {
        when (activeProvider.type) {
            ProviderType.EDGE_TTS -> Color(0xFF0078D4)
            ProviderType.OPENAI -> Color(0xFF10A37F)
            ProviderType.DOUBAO -> Color(0xFF3B82F6)
            ProviderType.SILICONFLOW -> Color(0xFF7C3AED)
            ProviderType.MINIMAX -> Color(0xFFE11D48)
            ProviderType.FISH_AUDIO -> Color(0xFFEC4899)
            ProviderType.MIMO -> Color(0xFFFF6A00)
            ProviderType.STEPFUN -> Color(0xFF06B6D4)
            ProviderType.AZURE -> Color(0xFF0089D6)
            ProviderType.GEMINI -> Color(0xFF9333EA)
            ProviderType.CUSTOM_HTTP -> Color(0xFF0D9488)
            ProviderType.OFFLINE_VITS -> Color(0xFF10B981)
        }
    } else {
        PulseTokens.CyanElectric
    }

    fun startSynthesis(provider: TtsProviderConfig, customText: String? = null) {
        val targetText = (customText ?: testText).trim()
        if (targetText.isBlank()) {
            Toast.makeText(context, "请输入测试文本", Toast.LENGTH_SHORT).show()
            return
        }

        if (isPlaying || isSynthesizing) {
            audioPlayer.stop()
            isPlaying = false
            isSynthesizing = false
            return
        }

        scope.launch {
            isSynthesizing = true
            val startTime = System.currentTimeMillis()

            try {
                val effectiveSpeed = (provider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (provider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = provider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                configDataStore.log("🚀 [Bento] 开始网格矩阵合成: 【${provider.name}】")
                val result = TtsProviderManager.getInstance().synthesize(targetText, testConfig)
                val costMs = System.currentTimeMillis() - startTime

                if (result.isSuccess) {
                    val audioData = result.getOrNull()
                    if (audioData != null && audioData.isNotEmpty()) {
                        lastLatencyMs = costMs
                        isSynthesizing = false
                        isPlaying = true

                        audioPlayer.playAudioBytes(
                            audioData,
                            onCompletion = { isPlaying = false }
                        )
                    } else {
                        isSynthesizing = false
                        Toast.makeText(context, "合成音频流为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isSynthesizing = false
                    Toast.makeText(context, "合成失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isSynthesizing = false
                Toast.makeText(context, "调用异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 1. Bento 顶栏标题 ====================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PulseTokens.CyanElectric))
                            Text("BENTO 全景网格矩阵", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = PulseTokens.TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PulseTokens.CyanElectric.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "v3.7.0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTokens.CyanElectric,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("32-Band 频域物理分析 · 模块化声学工作台", fontSize = 11.sp, color = PulseTokens.CyanElectric)
                    }

                    IconButton(
                        onClick = { onNavigateToEditProvider(activeProvider.id) },
                        modifier = Modifier.clip(CircleShape).background(PulseTokens.SurfaceElevated).size(36.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "引擎配置", tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ==================== 2. Master Bento Card (32-Band 示波器主卡片) ====================
            item {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = PulseTokens.SurfaceDark,
                    border = BorderStroke(1.5.dp, activeBrandColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(activeBrandColor))
                                Text(activeProvider.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = activeBrandColor)
                                Surface(shape = RoundedCornerShape(4.dp), color = activeBrandColor.copy(alpha = 0.2f)) {
                                    Text(activeProvider.type.displayName, fontSize = 10.sp, color = activeBrandColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }

                            Button(
                                onClick = { startSynthesis(activeProvider) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) PulseTokens.MagentaLaser else activeBrandColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.Black)
                                } else {
                                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPlaying) "停止" else "试听", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 32-Band 实时物理频域示波画布
                        val magentaLaserColor = PulseTokens.MagentaLaser
                        val sonicBlueColor = PulseTokens.SonicBlue

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PulseTokens.CanvasDeep)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barCount = 32
                                val totalWidth = size.width
                                val totalHeight = size.height
                                val barSpacing = 3.dp.toPx()
                                val barWidth = (totalWidth - (barCount - 1) * barSpacing) / barCount

                                for (i in 0 until barCount) {
                                    val magnitude = if (isPlaying && spectrumBands.isNotEmpty()) {
                                        spectrumBands.getOrElse(i) { 0.05f }.coerceIn(0.04f, 1f)
                                    } else if (isSynthesizing) {
                                        0.25f
                                    } else {
                                        0.06f
                                    }

                                    val barHeight = (totalHeight * magnitude).coerceAtLeast(4.dp.toPx())
                                    val left = i * (barWidth + barSpacing)
                                    val top = totalHeight - barHeight

                                    val barBrush = Brush.verticalGradient(
                                        colors = listOf(
                                            if (i > 20) magentaLaserColor else activeBrandColor,
                                            sonicBlueColor.copy(alpha = 0.6f)
                                        ),
                                        startY = top,
                                        endY = totalHeight
                                    )

                                    drawRoundRect(
                                        brush = barBrush,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 3. Telemetry Bento 2x2 实用功能矩阵 ====================
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Card 1: 延迟与采样规格总览
                    PulseCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = PulseTokens.SurfaceCard
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(14.dp))
                                Text("遥测指标", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                            }
                            Text(if (lastLatencyMs != null) "${lastLatencyMs}ms" else "-- ms", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                            Text("${activeProvider.sampleRate}Hz · 16-Bit", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                        }
                    }

                    // Card 2: 快捷音色选择 (可点击弹出快速音色库)
                    PulseCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showVoicePickerDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = PulseTokens.SurfaceCard,
                        border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = PulseTokens.SonicBlue, modifier = Modifier.size(14.dp))
                                    Text("发音音色", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                }
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PulseTokens.SonicBlue, modifier = Modifier.size(12.dp))
                            }
                            Text(
                                activeProvider.voiceId.ifBlank { "默认音色" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PulseTokens.SonicBlue,
                                maxLines = 1
                            )
                            Text("点击更换发音人音色 ⚡", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Card 3: 自适应参数卡片 (AI 大模型展示 Prompt/情绪; 传统 TTS 展示 语速/音高微调)
                    if (isAiModel) {
                        PulseCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showPromptDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = PulseTokens.SurfaceCard,
                            border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(14.dp))
                                        Text("Prompt 提示词", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                    }
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    if (activeProvider.promptInstruction.isNotBlank()) activeProvider.promptInstruction else "标准自然",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTokens.CyanElectric,
                                    maxLines = 1
                                )
                                Text("点击配置 AI 情绪/系统指令 🎭", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                            }
                        }
                    } else {
                        PulseCard(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showAcousticDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = PulseTokens.SurfaceCard,
                            border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Speed, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(14.dp))
                                        Text("语速与音高", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                    }
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    "${String.format("%.2f", activeProvider.speed)}x · ${String.format("%.2f", activeProvider.pitch)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTokens.CyanElectric,
                                    maxLines = 1
                                )
                                Text("点击微调传统 TTS 声学参数 🎚️", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                            }
                        }
                    }

                    // Card 4: 缓存空间与一键清理
                    val (cacheCount, cacheSizeMb) = remember(isPlaying) { audioCacheManager.getStats() }
                    PulseCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = PulseTokens.SurfaceCard
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = null, tint = PulseTokens.MagentaLaser, modifier = Modifier.size(14.dp))
                                    Text("缓存管理", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PulseTokens.MagentaLaser.copy(alpha = 0.20f),
                                    modifier = Modifier.clickable {
                                        audioCacheManager.clearAll()
                                        Toast.makeText(context, "已释放本地音频缓存", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("清理", fontSize = 10.sp, color = PulseTokens.MagentaLaser, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("${String.format("%.1f", cacheSizeMb)} MB", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser)
                            Text("已缓存 $cacheCount 条合成音频", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                        }
                    }
                }
            }

            // ==================== 4. 4 场景快捷语料矩阵 ====================
            item {
                Text("📋 场景语料快捷测试矩阵", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                val presetScenarios = listOf(
                    "文学朗读" to "那年盛夏，清风掠过树梢，带走了最后一抹金色的晚霞，唯留下悠长的蝉鸣。",
                    "新闻播报" to "央视国际快讯：最新一代神经网络多模态语音大模型今日正式完成全量公测。",
                    "情感对白" to "如果有一天我们走散在茫茫人海里，请一定要记得初次相遇时的约定。",
                    "极客科技" to "系统已启动 32-Band 频域物理分析，Sub-150ms 极速推理引擎正在稳定运行。"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetScenarios.take(2).forEach { (tag, text) ->
                        PulseCard(
                            modifier = Modifier.weight(1f).clickable {
                                testText = text
                                startSynthesis(activeProvider, text)
                            },
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = PulseTokens.SurfaceElevated,
                            border = PulseTokens.BorderSubtle
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                Text(text, fontSize = 10.sp, color = PulseTokens.TextSecondary, maxLines = 2)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetScenarios.drop(2).forEach { (tag, text) ->
                        PulseCard(
                            modifier = Modifier.weight(1f).clickable {
                                testText = text
                                startSynthesis(activeProvider, text)
                            },
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = PulseTokens.SurfaceElevated,
                            border = PulseTokens.BorderSubtle
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(tag, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser)
                                Text(text, fontSize = 10.sp, color = PulseTokens.TextSecondary, maxLines = 2)
                            }
                        }
                    }
                }
            }

            // ==================== 5. 实时测试文本输入台 ====================
            item {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = PulseTokens.SurfaceCard,
                    border = PulseTokens.BorderSubtle
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(15.dp))
                                Text("测试文本输入台", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PulseTokens.CyanElectric.copy(alpha = 0.12f))
                                    .clickable {
                                        if (isFetchingQuote) return@clickable
                                        scope.launch {
                                            isFetchingQuote = true
                                            quoteButtonText = "获取中..."
                                            try {
                                                val quote = QuoteService.fetchOnlineHitokoto()
                                                testText = quote.text
                                                quoteSourceDesc = quote.source ?: "一言精选"
                                                quoteButtonText = "一言金句"
                                                Toast.makeText(context, "已载入: ${quote.source ?: "一言"}", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                quoteButtonText = "一言金句"
                                                Toast.makeText(context, "获取一言失败", Toast.LENGTH_SHORT).show()
                                            } finally {
                                                isFetchingQuote = false
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isFetchingQuote) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PulseTokens.CyanElectric, strokeWidth = 1.5.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(Icons.Default.Casino, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(quoteButtonText, fontSize = 11.5.sp, color = PulseTokens.CyanElectric)
                            }
                        }

                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeBrandColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = PulseTokens.TextPrimary,
                                unfocusedTextColor = PulseTokens.TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        if (!quoteSourceDesc.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = PulseTokens.AmberWarm, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "出处来源: $quoteSourceDesc",
                                    fontSize = 11.sp,
                                    color = PulseTokens.AmberWarm,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== 6. Bento 专属单手大拇指收纳岛 ====================
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
                    if (isAiModel) showPromptDialog = true else onNavigateToEditProvider(activeProvider.id)
                }
            ),
            ActionHubItem(
                label = "一言金句",
                icon = Icons.Default.Casino,
                color = PulseTokens.CyanElectric,
                onClick = {
                    scope.launch {
                        val quote = QuoteService.fetchOnlineHitokoto()
                        testText = quote.text
                        quoteSourceDesc = quote.source ?: "一言名句"
                        Toast.makeText(context, "已载入: ${quote.source ?: "一言"}", Toast.LENGTH_SHORT).show()
                    }
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
                color = if (isPlaying) PulseTokens.MagentaLaser else activeBrandColor,
                isLoading = isSynthesizing,
                onClick = { startSynthesis(activeProvider) }
            )
        )

        UniversalActionHub(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            items = hubActionItems,
            icon = Icons.Default.Dashboard
        )

        // ==================== 快捷音色选择弹窗 (支持在线拉取 + 实时搜索) ====================
        if (showVoicePickerDialog) {
            var fetchedVoices by remember { mutableStateOf<List<com.aitts.engine.data.VoiceModel>>(emptyList()) }
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

            androidx.compose.material3.AlertDialog(
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
                    androidx.compose.material3.TextButton(onClick = { showVoicePickerDialog = false }) {
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
                "严肃新闻" to "请用字正腔圆、严谨庄重的新闻主播声调进行播报。"
            )
            var promptInput by remember { mutableStateOf(activeProvider.promptInstruction) }

            androidx.compose.material3.AlertDialog(
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
                        Text("情绪预设模板:", fontSize = 11.5.sp, color = PulseTokens.TextSecondary, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            promptPresets.take(2).forEach { (name, text) ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { promptInput = text },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseTokens.SurfaceCard,
                                    border = PulseTokens.BorderSubtle
                                ) {
                                    Text(name, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            promptPresets.drop(2).forEach { (name, text) ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { promptInput = text },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseTokens.SurfaceCard,
                                    border = PulseTokens.BorderSubtle
                                ) {
                                    Text(name, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = activeProvider.copy(promptInstruction = promptInput.trim())
                            configDataStore.updateProvider(updated)
                            showPromptDialog = false
                            Toast.makeText(context, "Prompt 指令已更新", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showPromptDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // ==================== 传统 TTS 声学参数微调弹窗 ====================
        if (showAcousticDialog) {
            var speedVal by remember { mutableFloatStateOf(activeProvider.speed) }
            var pitchVal by remember { mutableFloatStateOf(activeProvider.pitch) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAcousticDialog = false },
                title = { Text("🎚️ 传统 TTS 声学微调", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("发音语速", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                Text("${String.format("%.2f", speedVal)}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                            }
                            androidx.compose.material3.Slider(
                                value = speedVal,
                                onValueChange = { speedVal = it },
                                valueRange = 0.5f..2.0f
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("音调微调", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                Text(String.format("%.2f", pitchVal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                            }
                            androidx.compose.material3.Slider(
                                value = pitchVal,
                                onValueChange = { pitchVal = it },
                                valueRange = 0.5f..1.5f
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = activeProvider.copy(speed = speedVal, pitch = pitchVal)
                            configDataStore.updateProvider(updated)
                            showAcousticDialog = false
                            Toast.makeText(context, "声学参数已更新", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showAcousticDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // ==================== 页面内模型选择抽屉 (ModalBottomSheet - 支持自由拖拽排序) ====================
        if (showBottomSheetModelPicker) {
            val bottomSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var hubLocalList by remember(providers) { mutableStateOf(providers) }
            val density = LocalDensity.current
            val sheetItemHeightPx = with(density) { 68.dp.toPx() }
            var sheetDraggedProviderId by remember { mutableStateOf<String?>(null) }
            var sheetDragStartIndex by remember { mutableStateOf(-1) }
            var sheetDragTargetIndex by remember { mutableStateOf(-1) }
            var sheetTotalDragOffsetY by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(providers) {
                if (sheetDraggedProviderId == null) {
                    hubLocalList = providers
                }
            }

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showBottomSheetModelPicker = false },
                sheetState = bottomSheetState,
                containerColor = PulseTokens.SurfaceDark,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PulseTokens.CyanElectric.copy(alpha = 0.5f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("发音模型库抽屉", fontSize = 17.sp, fontWeight = FontWeight.Black, color = PulseTokens.TextPrimary)
                            Text("点击一键设为主力，长按右侧手柄自由排序", fontSize = 11.sp, color = PulseTokens.CyanElectric)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
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
                                label = "bento_sheet_shift"
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
                                                Toast.makeText(context, "已切换主力为: ${provider.name}", Toast.LENGTH_SHORT).show()
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) brandColor else PulseTokens.TextTertiary)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(provider.name, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) brandColor else PulseTokens.TextPrimary)
                                                if (isSelected) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = brandColor.copy(alpha = 0.2f)) {
                                                        Text("主力", fontSize = 9.sp, color = brandColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text("${provider.type.displayName} · ${provider.voiceId.ifBlank { "默认音色" }}", fontSize = 11.sp, color = PulseTokens.TextSecondary)
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
                                            Icon(Icons.Default.Tune, contentDescription = "配置", tint = PulseTokens.SonicBlue, modifier = Modifier.size(16.dp))
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
