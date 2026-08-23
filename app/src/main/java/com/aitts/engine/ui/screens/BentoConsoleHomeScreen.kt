package com.aitts.engine.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import kotlin.math.roundToInt
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
import com.aitts.engine.ui.components.PermissionCard
import com.aitts.engine.ui.components.SystemTtsGuideCard
import com.aitts.engine.ui.theme.BrandTheme
import com.aitts.engine.ui.theme.SuccessGreen
import com.aitts.engine.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import java.io.File

/**
 * 🚀 全新未来拟态 Bento 便当盒全息工作台 (v2.2.0 Next-Gen Bento Paradigm)
 * 1. 核心交互中心：触控全息声灵球 (Holographic Voice Core)，能量随真实物理音频振幅实时呼吸扩张；
 * 2. 真实物理 32-Band FFT 频域频谱分析仪 (Real-Time Hardware Audio Spectrum)；
 * 3. 几何 Bento 模块化卡片布局（声球浮岛、物理声谱、灵感瀑布、模型声线 Dock）；
 * 4. 三套 UI 风格自由切换并支持 100% 完整核心功能。
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BentoConsoleHomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit,
    onSwitchUiStyle: (String) -> Unit,
    testText: String = "欢迎体验 AI TTS 全新 Bento 全息声球工作台！真实物理频域示波器正在实时捕获声学能量。",
    onTestTextChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

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

    var isFetchingHitokoto by remember { mutableStateOf(false) }
    var quoteSourceHint by remember { mutableStateOf<String?>(null) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTestingProviderId by remember { mutableStateOf<String?>(null) }
    var lastSynthesizedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var lastSynthesizedProviderName by remember { mutableStateOf("") }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var draggingProviderId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swapThresholdPx = remember(density) { with(density) { 56.dp.toPx() } }

    var permissionState by remember {
        mutableStateOf(PermissionManager.checkPermissions(context))
    }

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

    val activeBrandColor = remember(activeProvider?.type) {
        activeProvider?.let { BrandTheme.getColorForType(it.type) }
    } ?: MaterialTheme.colorScheme.primary

    // 触控声球呼吸能量动效
    val orbScale = if (isPlaying || isSynthesizing) {
        1.0f + (rmsEnergy * 0.35f).coerceIn(0f, 0.4f)
    } else {
        1.0f
    }

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

        currentTestingProviderId = provider.id
        isSynthesizing = true

        scope.launch {
            try {
                val effectiveSpeed = (provider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (provider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = provider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                val result = TtsProviderManager.getInstance().synthesize(textToSpeak, testConfig)
                if (result.isSuccess) {
                    val bytes = result.getOrNull() ?: ByteArray(0)
                    if (bytes.isNotEmpty()) {
                        lastSynthesizedBytes = bytes
                        lastSynthesizedProviderName = provider.name
                        isSynthesizing = false
                        isPlaying = true
                        audioPlayer.playAudioBytes(bytes, onCompletion = {
                            isPlaying = false
                            currentTestingProviderId = null
                        })
                    } else {
                        Toast.makeText(context, "合成音频为空", Toast.LENGTH_SHORT).show()
                        stopPlayback()
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "未知合成错误"
                    Toast.makeText(context, "试听失败: $err", Toast.LENGTH_LONG).show()
                    configDataStore.log("试听失败 [${provider.name}]: $err")
                    stopPlayback()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "试听异常: ${e.message}", Toast.LENGTH_SHORT).show()
                configDataStore.log("试听异常: ${e.message}")
                stopPlayback()
            }
        }
    }

    fun exportAndShareAudio(bytes: ByteArray, providerName: String) {
        try {
            val fileName = "AI_TTS_${providerName.replace(" ", "_").replace("/", "_")}_${System.currentTimeMillis()}.wav"
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetDir = File(downloadDir, "AI_TTS")
            targetDir.mkdirs()
            val targetFile = File(targetDir, fileName)

            AudioEnhancer.writeWavToFile(bytes, targetFile, sampleRate = 24000)
            Toast.makeText(context, "已成功导出到 Download/AI_TTS/${fileName}", Toast.LENGTH_SHORT).show()

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                targetFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享/导出 AI 朗读音频"))
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            userScrollEnabled = draggingProviderId == null,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // 🌟 顶部全息状态胶囊栏
        item(contentType = "bento_topbar") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .background(if (isPlaying) SuccessGreen else activeBrandColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI TTS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    val installedVersion = remember(context) {
                        try {
                            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: com.aitts.engine.BuildConfig.VERSION_NAME
                        } catch (e: Exception) {
                            com.aitts.engine.BuildConfig.VERSION_NAME
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "v$installedVersion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                            label = { Text("网格面板", fontSize = 11.sp) }
                        )
                        DropdownMenu(
                            expanded = showStyleMenu,
                            onDismissRequest = { showStyleMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("网格面板") },
                                onClick = {
                                    onSwitchUiStyle("BENTO")
                                    showStyleMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("调音台") },
                                onClick = {
                                    onSwitchUiStyle("STUDIO")
                                    showStyleMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("黑胶唱机") },
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

        // 🌟 BENTO HERO: 3D 触控全息声灵球 + 32-Band 物理示波器一体化控制台 (Integrated Master Voice Deck)
        item(contentType = "bento_master_deck") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, activeBrandColor.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部引擎信息与参数快捷按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProvider?.name ?: "未配置发音引擎",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${activeProvider?.type?.displayName} · 音色: ${activeProvider?.voiceId?.ifBlank { "默认" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val peakDb = if (isPlaying && rmsEnergy > 0.001f) {
                                "%.1f dB".format(20 * kotlin.math.log10(rmsEnergy.toDouble()))
                            } else {
                                "-inf dB"
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = peakDb,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = activeBrandColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { activeProvider?.let { onNavigateToEditProvider(it.id) } },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "配置", tint = activeBrandColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 触控声灵球核心 (Tap Orb to Play/Pause)
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .scale(orbScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        activeBrandColor,
                                        activeBrandColor.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                width = if (isPlaying) 2.5.dp else 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(activeBrandColor, MaterialTheme.colorScheme.tertiary, activeBrandColor)
                                ),
                                shape = CircleShape
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isPlaying || isSynthesizing) {
                                    stopPlayback()
                                } else {
                                    activeProvider?.let { playSpeechWithProvider(it, testText) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSynthesizing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else if (isPlaying) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "停止",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "试听",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isSynthesizing) "大模型音频生成中..." else if (isPlaying) "正在播放实时物理声学音频 (轻触声球停止)" else "轻触全息声球试听",
                        fontSize = 11.5.sp,
                        color = if (isPlaying) activeBrandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 真实物理 32-Band 频谱渲染画布 (集成在 Hero 卡片底部)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
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
                                val h = (maxHeight * energy).coerceAtLeast(2.5.dp.toPx())
                                val y = (size.height - h) / 2f

                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            activeBrandColor,
                                            activeBrandColor.copy(alpha = 0.35f)
                                        )
                                    ),
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, h),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }
        }

        // 🌟 BENTO TILE 1: 灵感语料与紧凑试听卡片 (Corpus & Quick Prompt)
        item(contentType = "bento_corpus") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("灵感试听语料", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AssistChip(
                                onClick = {
                                    val item = QuoteService.getRandomLocalQuote()
                                    onTestTextChange(item.text)
                                    quoteSourceHint = "分类: ${item.category}"
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
                                            quoteSourceHint = item.source ?: "一言金句"
                                            isFetchingHitokoto = false
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                },
                                label = {
                                    if (isFetchingHitokoto) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    } else {
                                        Text("🌐 一言", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = testText,
                        onValueChange = {
                            onTestTextChange(it)
                            quoteSourceHint = null
                        },
                        trailingIcon = {
                            if (testText.isNotBlank()) {
                                IconButton(onClick = {
                                    onTestTextChange("")
                                    quoteSourceHint = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清空", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (quoteSourceHint != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "💡 $quoteSourceHint",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 🌟 BENTO TILE 2: 2列紧凑分栏 (语速微调 + 引擎快速切换)
        item(contentType = "bento_dual_deck") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 左卡片：语速调节
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("发音语速", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            Text(
                                text = "${"%.2f".format(activeProvider?.speed ?: 1.0f)}x",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeBrandColor
                            )
                        }

                        activeProvider?.let { provider ->
                            Slider(
                                value = provider.speed,
                                onValueChange = { newSpeed ->
                                    val updated = provider.copy(speed = (newSpeed * 10).toInt() / 10f)
                                    configDataStore.updateProvider(updated)
                                },
                                valueRange = 0.5f..2.5f,
                                steps = 19,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (lastSynthesizedBytes != null) {
                            OutlinedButton(
                                onClick = { exportAndShareAudio(lastSynthesizedBytes!!, lastSynthesizedProviderName) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导出WAV", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 右卡片：流式沙盒与快速切换
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("测试与沙盒", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Text(
                            text = "24000Hz 16-bit Mono",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = onNavigateToTestBench,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("流式沙盒", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 🌟 BENTO TILE 3: 极速声线矩阵 Dock (Voice Matrix Dock)
        item(contentType = "bento_dock") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "极速音色矩阵 (${providers.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = { isReorderMode = !isReorderMode }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                                contentDescription = "排序模式",
                                tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        providers.forEach { provider ->
                            val isSelected = provider.id == settings.activeProviderId
                            val brandColor = remember(provider.type) { BrandTheme.getColorForType(provider.type) }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) brandColor else Color.Transparent
                                ),
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
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
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(brandColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = provider.name,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 🌟 声线列表与排序区域 (Voice Models & Draggable Reorder)
        items(providers, key = { it.id }) { provider ->
            val isCurrentActive = provider.id == settings.activeProviderId
            val brandColor = remember(provider.type) { BrandTheme.getColorForType(provider.type) }
            val isItemDragging = draggingProviderId == provider.id

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isItemDragging) 10f else 1f)
                    .offset { IntOffset(0, if (isItemDragging) dragOffsetY.roundToInt() else 0) }
                    .shadow(if (isItemDragging) 10.dp else 1.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = if (isCurrentActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isCurrentActive) 1.5.dp else 1.dp,
                    color = if (isCurrentActive) brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                                        dragOffsetY += dragAmount.y
                                        val fromIdx = providers.indexOfFirst { it.id == provider.id }
                                        if (fromIdx != -1) {
                                            if (dragOffsetY > swapThresholdPx && fromIdx < providers.size - 1) {
                                                configDataStore.reorderProviders(fromIdx, fromIdx + 1)
                                                dragOffsetY -= swapThresholdPx
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } else if (dragOffsetY < -swapThresholdPx && fromIdx > 0) {
                                                configDataStore.reorderProviders(fromIdx, fromIdx - 1)
                                                dragOffsetY += swapThresholdPx
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
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
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    Brush.radialGradient(listOf(brandColor.copy(alpha = 0.25f), Color.Transparent)),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = provider.type.name.take(2),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = brandColor
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = provider.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (isCurrentActive) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = brandColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "生效中",
                                            fontSize = 9.sp,
                                            color = brandColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${provider.type.displayName} · ${provider.voiceId.ifBlank { "默认" }}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isReorderMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { configDataStore.pinProviderToTop(provider.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                            }
                            IconButton(onClick = { configDataStore.moveProviderUp(provider.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { configDataStore.moveProviderDown(provider.id) }, modifier = Modifier.size(26.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        IconButton(onClick = { onNavigateToEditProvider(provider.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    // 睡眠定时器对话框
    if (showSleepTimerDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("听书睡眠定时器") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isSleepTimerActive) "倒计时进行中，剩余: ${sleepRemainingSec / 60}分${sleepRemainingSec % 60}秒" else "选择定时关闭时间，结束前 15 秒平滑淡出音量：",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        Button(
                            onClick = {
                                sleepTimerManager.startTimer(mins)
                                showSleepTimerDialog = false
                                Toast.makeText(context, "已设置 ${mins} 分钟后停止听书", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${mins} 分钟")
                        }
                    }
                    if (isSleepTimerActive) {
                        OutlinedButton(
                            onClick = {
                                sleepTimerManager.stopTimer()
                                showSleepTimerDialog = false
                                Toast.makeText(context, "已取消睡眠定时器", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("取消定时器")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    // 历史记录对话框
    if (showHistoryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("朗读历史与统计 (${historyItems.size})") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (historyItems.isEmpty()) {
                        item {
                            Text("暂无朗读历史记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(historyItems.take(30)) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(item.text, maxLines = 2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${item.providerName} · ${item.characterCount}字", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${item.costMs}ms", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    configDataStore.clearHistory()
                    Toast.makeText(context, "历史记录已清空", Toast.LENGTH_SHORT).show()
                }) {
                    Text("清空历史")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
    }
}
