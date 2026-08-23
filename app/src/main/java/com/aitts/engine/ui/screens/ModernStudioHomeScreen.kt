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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.aitts.engine.ui.components.PermissionCard
import com.aitts.engine.ui.components.SystemTtsGuideCard
import com.aitts.engine.ui.theme.BrandTheme
import com.aitts.engine.ui.theme.SuccessGreen
import com.aitts.engine.ui.theme.WarningOrange
import kotlinx.coroutines.launch
import java.io.File

/**
 * Next-Gen AI Audio Studio 工作台界面 (v2.1.0 全新声学调音台架构)
 * 1. 采用专业音频工作站 (DAW) 沉浸式视觉层级；
 * 2. 主控台集成动态微频谱波形动效与实时语速/音调阻尼滑杆；
 * 3. 灵感语料库横向流动卡片流 + 一言金句实时异步拉取；
 * 4. 模块化声线矩阵卡片，支持单卡片前置测速与一键设为主音色；
 * 5. 与经典列表视图支持双向一键实时无缝切换。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModernStudioHomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit,
    onSwitchUiStyle: (String) -> Unit
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

    var showStyleMenu by remember { mutableStateOf(false) }

    var testText by remember {
        mutableStateOf("欢迎使用 AI TTS Studio 专业音频工作台！大模型拟真声线正在为您实时合成发音。")
    }
    var isFetchingHitokoto by remember { mutableStateOf(false) }
    var quoteSourceHint by remember { mutableStateOf<String?>(null) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTestingProviderId by remember { mutableStateOf<String?>(null) }
    var lastSynthesizedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var lastSynthesizedProviderName by remember { mutableStateOf("") }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTag by remember { mutableStateOf("全部") }
    var isProbingSpeed by remember { mutableStateOf(false) }
    val latencyMap = remember { mutableStateMapOf<String, Long>() }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showQuickProviderMenu by remember { mutableStateOf(false) }

    var permissionState by remember {
        mutableStateOf(PermissionManager.checkPermissions(context))
    }

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

    val activeBrandColor = remember(activeProvider?.type) {
        activeProvider?.let { BrandTheme.getColorForType(it.type) }
    } ?: MaterialTheme.colorScheme.primary

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部 Studio 标题栏与风格切换
        item(contentType = "studio_appbar") {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(listOf(activeBrandColor, MaterialTheme.colorScheme.tertiary)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI TTS Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "v${com.aitts.engine.BuildConfig.VERSION_NAME} 专业调音台工作台",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        AssistChip(
                            onClick = { showStyleMenu = true },
                            label = { Text("🎛️ Studio 调音台", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
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
                                text = { Text("📋 经典紧凑列表") },
                                onClick = {
                                    onSwitchUiStyle("CLASSIC")
                                    showStyleMenu = false
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showSleepTimerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "睡眠定时",
                            tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "历史统计",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        if (!permissionState.isAllGranted) {
            item(contentType = "permission") {
                PermissionCard(
                    permissionState = permissionState,
                    onRequestAll = {
                        activity?.let {
                            PermissionManager.requestBasicPermissions(it)
                            PermissionManager.requestAllFilesAccess(it)
                            PermissionManager.requestIgnoreBatteryOptimizations(it)
                            permissionState = PermissionManager.checkPermissions(context)
                        }
                    },
                    onRequestIgnoreBattery = {
                        activity?.let {
                            PermissionManager.requestIgnoreBatteryOptimizations(it)
                            permissionState = PermissionManager.checkPermissions(context)
                        }
                    },
                    onRequestAllFiles = {
                        activity?.let {
                            PermissionManager.requestAllFilesAccess(it)
                            permissionState = PermissionManager.checkPermissions(context)
                        }
                    }
                )
            }
        }

        item(contentType = "guide") {
            SystemTtsGuideCard(
                onOpenSettings = {
                    activity?.let { PermissionManager.openSystemTtsSettings(it) }
                }
            )
        }

        // 🌟 核心主控台 (Master Hero Console Deck)
        item(contentType = "master_console") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, activeBrandColor.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // 主音色选择行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showQuickProviderMenu = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(activeBrandColor.copy(alpha = 0.3f), Color.Transparent)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = activeBrandColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = activeProvider?.name ?: "未选择主模型",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeProvider?.type?.displayName} · 点击快速切换",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    text = "峰值: $peakDb",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = activeBrandColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showQuickProviderMenu,
                            onDismissRequest = { showQuickProviderMenu = false }
                        ) {
                            providers.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (${p.type.displayName})") },
                                    onClick = {
                                        configDataStore.updateSettings(settings.copy(activeProviderId = p.id))
                                        showQuickProviderMenu = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 真实物理 32-Band STFT 频域示波器 (Real Physical Spectrum)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 5.dp)) {
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // 实时阻尼语速与音量调节滑杆
                    activeProvider?.let { provider ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "主音色语速: ${"%.2f".format(provider.speed)}x",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    text = "采样率: ${provider.sampleRate}Hz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

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
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 试听与操作按键组
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isPlaying || isSynthesizing) {
                                    stopPlayback()
                                } else {
                                    activeProvider?.let { playSpeechWithProvider(it, testText) }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            if (isSynthesizing && currentTestingProviderId == activeProvider?.id) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("合成中...", fontSize = 13.sp)
                            } else if (isPlaying && currentTestingProviderId == activeProvider?.id) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("停止", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("试听主音色", fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { activeProvider?.let { onNavigateToEditProvider(it.id) } },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("参数", fontSize = 12.5.sp)
                        }

                        if (lastSynthesizedBytes != null) {
                            OutlinedButton(
                                onClick = {
                                    exportAndShareAudio(lastSynthesizedBytes!!, lastSynthesizedProviderName)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 🌟 灵感语料与在线金句抽屉 (Corpus & Hitokoto Studio)
        item(contentType = "corpus_studio") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("灵感试听语料库", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AssistChip(
                                onClick = {
                                    val item = QuoteService.getRandomLocalQuote()
                                    testText = item.text
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
                                            testText = item.text
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // 分类快捷标签
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("小说剧场", "经典文学", "科技数码", "新闻播报", "日常闲聊").forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.clickable {
                                    val item = QuoteService.getRandomLocalQuote(cat)
                                    testText = item.text
                                    quoteSourceHint = "分类: $cat"
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = testText,
                        onValueChange = {
                            testText = it
                            quoteSourceHint = null
                        },
                        trailingIcon = {
                            if (testText.isNotBlank()) {
                                IconButton(onClick = {
                                    testText = ""
                                    quoteSourceHint = null
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清空", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (quoteSourceHint != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 $quoteSourceHint",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 🌟 模块化声线矩阵 (Studio Engine Matrix)
        item(contentType = "engine_matrix_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已装载声线矩阵 (${filteredProviders.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onNavigateToTestBench) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("流式沙盒", fontSize = 12.5.sp)
                }
            }

            // 过滤标签
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("全部", "官方免Key", "小米MiMo", "微软Edge", "Google", "已启用").forEach { tag ->
                    FilterChip(
                        selected = selectedFilterTag == tag,
                        onClick = { selectedFilterTag = tag },
                        label = { Text(tag, fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filteredProviders, key = { it.id }) { provider ->
            val isCurrentActive = provider.id == settings.activeProviderId
            val brandColor = remember(provider.type) { BrandTheme.getColorForType(provider.type) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isCurrentActive) 1.5.dp else 1.dp,
                    color = if (isCurrentActive) brandColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(brandColor.copy(alpha = 0.25f), Color.Transparent)
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = provider.type.name.take(2),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = brandColor
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = provider.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrentActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = brandColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "生效中",
                                                fontSize = 10.sp,
                                                color = brandColor,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${provider.type.displayName} · 音色: ${provider.voiceId.ifBlank { "默认" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            // 单卡片快速试听
                            IconButton(
                                onClick = { playSpeechWithProvider(provider, testText) }
                            ) {
                                if (isSynthesizing && currentTestingProviderId == provider.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (isPlaying && currentTestingProviderId == provider.id) {
                                    Icon(Icons.Default.Stop, contentDescription = "停止", tint = MaterialTheme.colorScheme.error)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "试听", tint = brandColor)
                                }
                            }

                            IconButton(onClick = { onNavigateToEditProvider(provider.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
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
