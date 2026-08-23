package com.aitts.engine.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.aitts.engine.audio.AudioEnhancer
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.service.SleepTimerManager
import com.aitts.engine.ui.components.FloatingMasterDock
import com.aitts.engine.ui.components.HistoryDialog
import com.aitts.engine.ui.components.PermissionCard
import com.aitts.engine.ui.components.SleepTimerDialog
import com.aitts.engine.ui.components.SystemTtsGuideCard
import com.aitts.engine.ui.theme.BrandTheme
import com.aitts.engine.ui.theme.SuccessGreen
import com.aitts.engine.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/**
 * 📻 Vinyl Deck 沉浸式黑胶唱机工作台 (v2.8.0 殿堂级全新主题)
 * 1. 拟物化旋转黑胶唱盘与真实 STFT 频谱光晕；
 * 2. 实时小说/语料字幕提词卷轴 (Teleprompter Quote Roll)；
 * 3. 唱片架声线阵列，兼具视听冲击力与实用性。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun VinylDeckHomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit,
    onSwitchUiStyle: (String) -> Unit,
    testText: String = "欢迎使用 AI TTS Vinyl 黑胶沉浸式阅览舱！旋转唱盘正在为您流式合成拟真声线。",
    onTestTextChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    val visualizerManager = remember { AudioVisualizerManager.getInstance() }
    val spectrumBands by visualizerManager.spectrumFlow.collectAsState()
    val rmsEnergy by visualizerManager.rmsEnergyFlow.collectAsState()

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val historyItems by configDataStore.historyFlow.collectAsState()

    val sleepTimerManager = remember { SleepTimerManager.getInstance(context) }
    val sleepRemainingSec by sleepTimerManager.remainingSecondsFlow.collectAsState()
    val isSleepTimerActive by sleepTimerManager.isActiveFlow.collectAsState()

    var showStyleMenu by remember { mutableStateOf(false) }

    var isFetchingHitokoto by remember { mutableStateOf(false) }
    var quoteSourceHint by remember { mutableStateOf<String?>(null) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTestingProviderId by remember { mutableStateOf<String?>(null) }
    var lastSynthesizedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var lastSynthesizedProviderName by remember { mutableStateOf("") }

    var isReorderMode by remember { mutableStateOf(false) }
    var draggingProviderId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val swapThresholdPx = remember(density) { with(density) { 56.dp.toPx() } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTag by remember { mutableStateOf("全部") }
    var isProbingSpeed by remember { mutableStateOf(false) }
    val latencyMap = remember { mutableStateMapOf<String, Long>() }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showImportTokenDialog by remember { mutableStateOf(false) }
    var importTokenText by remember { mutableStateOf("") }

    var permissionState by remember {
        mutableStateOf(PermissionManager.checkPermissions(context))
    }

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

    val activeBrandColor = remember(activeProvider?.type) {
        activeProvider?.let { BrandTheme.getColorForType(it.type) }
    } ?: MaterialTheme.colorScheme.primary

    // 旋转黑胶唱盘动画
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
        isSynthesizing = false
        currentTestingProviderId = null
    }

    fun playSpeechWithProvider(provider: TtsProviderConfig, textToSpeak: String) {
        if (isPlaying || isSynthesizing) {
            stopPlayback()
            return
        }

        isSynthesizing = true
        currentTestingProviderId = provider.id

        scope.launch {
            try {
                val startMs = System.currentTimeMillis()
                val result = TtsProviderManager.getInstance().synthesize(textToSpeak, provider)
                val costMs = System.currentTimeMillis() - startMs

                if (result.isSuccess) {
                    val rawBytes = result.getOrNull() ?: ByteArray(0)
                    lastSynthesizedBytes = rawBytes
                    lastSynthesizedProviderName = provider.name
                    configDataStore.log("Vinyl 试听成功 [${provider.name}] (${costMs}ms)")

                    if (rawBytes.isNotEmpty()) {
                        isSynthesizing = false
                        isPlaying = true

                        audioPlayer.playAudioBytes(
                            audioBytes = rawBytes,
                            onCompletion = {
                                isPlaying = false
                                currentTestingProviderId = null
                            },
                            onError = { err ->
                                configDataStore.log("播放错误: $err")
                                stopPlayback()
                            }
                        )
                    } else {
                        stopPlayback()
                    }
                } else {
                    configDataStore.log("试听请求失败: ${result.exceptionOrNull()?.message}")
                    stopPlayback()
                }
            } catch (e: Exception) {
                configDataStore.log("试听异常: ${e.message}")
                stopPlayback()
            }
        }
    }

    fun handleItemDrag(providerId: String, deltaY: Float) {
        if (draggingProviderId != providerId) return
        dragOffsetY += deltaY
        val currentIndex = providers.indexOfFirst { it.id == providerId }
        if (currentIndex == -1) return

        if (dragOffsetY > swapThresholdPx && currentIndex < providers.lastIndex) {
            configDataStore.reorderProviders(currentIndex, currentIndex + 1)
            dragOffsetY -= swapThresholdPx
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } else if (dragOffsetY < -swapThresholdPx && currentIndex > 0) {
            configDataStore.reorderProviders(currentIndex, currentIndex - 1)
            dragOffsetY += swapThresholdPx
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun probeAllLatencies() {
        if (isProbingSpeed) return
        isProbingSpeed = true
        latencyMap.clear()

        scope.launch {
            for (p in providers) {
                try {
                    val start = System.currentTimeMillis()
                    val res = TtsProviderManager.getInstance().synthesize("测试", p)
                    val cost = System.currentTimeMillis() - start
                    latencyMap[p.id] = if (res.isSuccess) cost else 9999L
                } catch (e: Exception) {
                    latencyMap[p.id] = 9999L
                }
            }
            isProbingSpeed = false
        }
    }

    val dynamicTags = remember(providers) {
        (listOf("全部", "官方免Key", "小米MiMo", "微软Edge", "Google", "已启用") + providers.flatMap { it.tags }).distinct()
    }

    val filteredProviders = remember(providers, searchQuery, selectedFilterTag) {
        providers.filter { provider ->
            val matchesSearch = searchQuery.isBlank() ||
                    provider.name.contains(searchQuery, ignoreCase = true) ||
                    provider.voiceId.contains(searchQuery, ignoreCase = true) ||
                    provider.modelName.contains(searchQuery, ignoreCase = true) ||
                    provider.tags.any { it.contains(searchQuery, ignoreCase = true) } ||
                    provider.type.displayName.contains(searchQuery, ignoreCase = true)

            val matchesTag = when (selectedFilterTag) {
                "全部" -> true
                "官方免Key" -> !provider.type.requiresApiKey
                "小米MiMo" -> provider.type == ProviderType.MIMO
                "微软Edge" -> provider.type == ProviderType.EDGE_TTS
                "Google" -> provider.type == ProviderType.GEMINI
                "已启用" -> provider.enabled
                else -> provider.tags.contains(selectedFilterTag)
            }

            matchesSearch && matchesTag
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            userScrollEnabled = draggingProviderId == null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 🌟 顶部黑胶电台状态栏
            item(contentType = "vinyl_topbar") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Brush.linearGradient(listOf(activeBrandColor, MaterialTheme.colorScheme.secondary)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radio,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI TTS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "v${com.aitts.engine.BuildConfig.VERSION_NAME}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "黑胶唱机沉浸式阅览舱",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSleepTimerDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "睡眠定时",
                                tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(onClick = { showHistoryDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "历史统计",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box {
                            AssistChip(
                                onClick = { showStyleMenu = true },
                                label = { Text("📻 Vinyl 唱机", fontSize = 11.sp) }
                            )
                            DropdownMenu(
                                expanded = showStyleMenu,
                                onDismissRequest = { showStyleMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🚀 Bento 全息声球工作台") },
                                    onClick = {
                                        onSwitchUiStyle("BENTO")
                                        showStyleMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎛️ DAW 专业调音台") },
                                    onClick = {
                                        onSwitchUiStyle("STUDIO")
                                        showStyleMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📻 Vinyl 黑胶沉浸阅览舱") },
                                    onClick = {
                                        onSwitchUiStyle("VINYL")
                                        showStyleMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item(contentType = "guide") {
                SystemTtsGuideCard(
                    onOpenSettings = {
                        activity?.let { PermissionManager.openSystemTtsSettings(it) }
                    }
                )
            }

        // 🌟 核心黑胶留声机沉浸式主舱 (Hero Vinyl Turntable Station)
        item(contentType = "vinyl_player") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // 黑胶留声唱盘与提词滚动器并排舱
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 旋转黑胶唱盘与物理频谱光晕
                        Box(
                            modifier = Modifier
                                .size(116.dp)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 动态物理频域能量圆环
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = size.minDimension / 2f
                                val center = Offset(size.width / 2f, size.height / 2f)

                                // 黑胶外盘暗纹
                                drawCircle(
                                    color = Color(0xFF161616),
                                    radius = radius,
                                    center = center
                                )
                                drawCircle(
                                    color = Color(0xFF222222),
                                    radius = radius * 0.85f,
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF2A2A2A),
                                    radius = radius * 0.70f,
                                    center = center,
                                    style = Stroke(width = 0.75.dp.toPx())
                                )
                                drawCircle(
                                    color = Color(0xFF333333),
                                    radius = radius * 0.55f,
                                    center = center,
                                    style = Stroke(width = 0.5.dp.toPx())
                                )

                                if (isPlaying) {
                                    // 播放中：发光边缘环
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(activeBrandColor, activeBrandColor.copy(alpha = 0.2f), activeBrandColor)
                                        ),
                                        radius = radius - 1.dp.toPx(),
                                        center = center,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }

                            // 唱盘中央品牌标签（点击直达模型设置）
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .rotate(if (isPlaying) vinylRotation else 0f)
                                    .clickable { activeProvider?.let { onNavigateToEditProvider(it.id) } }
                                    .background(
                                        Brush.radialGradient(
                                            listOf(activeBrandColor, activeBrandColor.copy(alpha = 0.65f))
                                        ),
                                        CircleShape
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 2. 实时小说/语料字幕提词卷轴 (Teleprompter Quote Roll)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeProvider?.name ?: "未激活引擎",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                val peakDb = if (isPlaying && rmsEnergy > 0.001f) {
                                    "%.1f dB".format(20 * kotlin.math.log10(rmsEnergy.toDouble()))
                                } else {
                                    "-inf dB"
                                }
                                Text(
                                    text = peakDb,
                                    fontSize = 10.sp,
                                    color = activeBrandColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "${activeProvider?.type?.displayName} · 采样率 ${activeProvider?.sampleRate}Hz",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 提词卡片
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = testText,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 3,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. 32-Band STFT 频域示波器
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                            val count = spectrumBands.size
                            val spacing = 2.dp.toPx()
                            val totalSpacing = spacing * (count - 1)
                            val barWidth = (size.width - totalSpacing) / count
                            val maxHeight = size.height * 0.9f

                            for (i in 0 until count) {
                                val x = i * (barWidth + spacing)
                                val energy = spectrumBands[i].coerceIn(0.02f, 1.0f)
                                val h = (maxHeight * energy).coerceAtLeast(2.dp.toPx())
                                val y = (size.height - h) / 2f

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(activeBrandColor, activeBrandColor.copy(alpha = 0.3f))
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, h),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. 语速转速选择器 (RPM Speed Selector)
                    activeProvider?.let { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "唱盘转速: ${provider.speed}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(0.8f to "0.8x 慢品", 1.0f to "1.0x 标准", 1.25f to "1.25x 快阅", 1.5f to "1.5x 飞速").forEach { (speed, label) ->
                                    FilterChip(
                                        selected = (provider.speed - speed) < 0.05f && (provider.speed - speed) > -0.05f,
                                        onClick = {
                                            configDataStore.updateProvider(provider.copy(speed = speed))
                                        },
                                        label = { Text(label, fontSize = 10.5.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. 试听按键与语料工具
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                activeProvider?.let { playSpeechWithProvider(it, testText) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            if (isSynthesizing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("合成中...", fontSize = 12.sp)
                            } else if (isPlaying) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("停止", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("播放唱片", fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { activeProvider?.let { onNavigateToEditProvider(it.id) } },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("参数", fontSize = 11.5.sp)
                        }

                        AssistChip(
                            onClick = {
                                val item = QuoteService.getRandomLocalQuote()
                                onTestTextChange(item.text)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            label = { Text("🎲 随机", fontSize = 11.sp) }
                        )

                        AssistChip(
                            onClick = {
                                if (!isFetchingHitokoto) {
                                    isFetchingHitokoto = true
                                    scope.launch {
                                        val item = QuoteService.fetchOnlineHitokoto()
                                        onTestTextChange(item.text)
                                        isFetchingHitokoto = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            },
                            label = { Text("💡 一言", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // 🌟 唱片架声线阵列 (Vinyl Record Crates / Collection)
        item(contentType = "crate_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "音色唱片收藏 (${filteredProviders.size}/${providers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { probeAllLatencies() },
                        label = { Text(if (isProbingSpeed) "测速中" else "测速", fontSize = 11.sp) },
                        leadingIcon = {
                            if (isProbingSpeed) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(13.dp))
                            }
                        }
                    )

                    IconButton(onClick = { isReorderMode = !isReorderMode }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                            contentDescription = "排序",
                            tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newId = UUID.randomUUID().toString()
                            val newConfig = TtsProviderConfig(
                                id = newId,
                                type = ProviderType.MIMO,
                                name = "新建 AI 语音唱片",
                                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions",
                                modelName = "mimo-v2.5-tts",
                                voiceId = "茉莉"
                            )
                            configDataStore.updateProvider(newConfig)
                            onNavigateToEditProvider(newId)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建唱片", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 搜索框与标签
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索唱片名称 / 音色 / 标签...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dynamicTags.forEach { tag ->
                    FilterChip(
                        selected = selectedFilterTag == tag,
                        onClick = { selectedFilterTag = tag },
                        label = { Text(tag, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredProviders.isEmpty()) {
            item(contentType = "empty_state") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("未找到符合条件的唱片", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(filteredProviders, key = { it.id }, contentType = { "vinyl_card" }) { provider ->
                val isItemActive = provider.id == settings.activeProviderId
                val brandColor = BrandTheme.getColorForType(provider.type)
                val isItemDragging = draggingProviderId == provider.id

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .zIndex(if (isItemDragging) 10f else 1f)
                        .offset { IntOffset(0, if (isItemDragging) dragOffsetY.roundToInt() else 0) }
                        .shadow(if (isItemDragging) 12.dp else 1.dp, RoundedCornerShape(12.dp))
                        .scale(if (isItemDragging) 1.025f else 1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isItemActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isItemActive) 1.5.dp else 0.5.dp,
                        color = if (isItemActive) brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    configDataStore.setActiveProviderId(provider.id)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDoubleClick = {
                                    onNavigateToEditProvider(provider.id)
                                },
                                onLongClick = {
                                    isReorderMode = !isReorderMode
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 拖拽把手 (Dedicated Drag Handle)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .pointerInput(provider.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isReorderMode = true
                                            draggingProviderId = provider.id
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            handleItemDrag(provider.id, dragAmount.y)
                                        },
                                        onDragEnd = {
                                            draggingProviderId = null
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingProviderId = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "按住拖拽排序",
                                tint = if (isItemDragging) brandColor else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // 唱片封套微缩图
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF1C1C1C), CircleShape)
                                .border(1.dp, brandColor.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(brandColor, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = provider.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isItemActive) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = brandColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = provider.type.displayName,
                                        fontSize = 9.5.sp,
                                        color = brandColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }
                                Text(
                                    text = "音色: ${provider.voiceId.ifBlank { "默认" }} · ${provider.speed}x",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isReorderMode) {
                            // 排序模式实体按键组
                            IconButton(onClick = { configDataStore.pinProviderToTop(provider.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { configDataStore.moveProviderUp(provider.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { configDataStore.moveProviderDown(provider.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(
                                onClick = { playSpeechWithProvider(provider, "您好，我是 ${provider.name}，正在为您试听黑胶发音效果。") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying && currentTestingProviderId == provider.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "试听",
                                    tint = brandColor
                                )
                            }
                            IconButton(onClick = { onNavigateToEditProvider(provider.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            sleepTimerManager = sleepTimerManager,
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    if (showHistoryDialog) {
        HistoryDialog(
            historyItems = historyItems,
            onDismiss = { showHistoryDialog = false },
            onClearHistory = { configDataStore.clearHistory() }
        )
    }
    }
}
