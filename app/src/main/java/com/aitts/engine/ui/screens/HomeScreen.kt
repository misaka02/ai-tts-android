package com.aitts.engine.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import java.io.File
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import com.aitts.engine.ui.theme.SuccessGreen
import com.aitts.engine.ui.theme.WarningOrange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.service.SleepTimerManager
import com.aitts.engine.audio.AudioEnhancer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.components.PermissionCard
import com.aitts.engine.ui.components.ProviderCard
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.components.SystemTtsGuideCard
import com.aitts.engine.ui.theme.BrandTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    val settings by configDataStore.settingsFlow.collectAsState()

    if (settings.appUiStyle == "BENTO") {
        BentoConsoleHomeScreen(
            configDataStore = configDataStore,
            onNavigateToEditProvider = onNavigateToEditProvider,
            onNavigateToTestBench = onNavigateToTestBench,
            onSwitchUiStyle = { newStyle ->
                configDataStore.updateSettings(settings.copy(appUiStyle = newStyle))
            }
        )
        return
    }

    if (settings.appUiStyle == "STUDIO") {
        ModernStudioHomeScreen(
            configDataStore = configDataStore,
            onNavigateToEditProvider = onNavigateToEditProvider,
            onNavigateToTestBench = onNavigateToTestBench,
            onSwitchUiStyle = { newStyle ->
                configDataStore.updateSettings(settings.copy(appUiStyle = newStyle))
            }
        )
        return
    }

    val providers by configDataStore.providersFlow.collectAsState()
    val historyItems by configDataStore.historyFlow.collectAsState()

    val sleepTimerManager = remember { SleepTimerManager.getInstance(context) }
    val sleepRemainingSec by sleepTimerManager.remainingSecondsFlow.collectAsState()
    val isSleepTimerActive by sleepTimerManager.isActiveFlow.collectAsState()

    var testText by remember { mutableStateOf("欢迎使用 AI TTS 系统语音引擎！当前正在通过智能大模型为您朗读文本。") }
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
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val swapThresholdPx = remember(density) { with(density) { 58.dp.toPx() } }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTag by remember { mutableStateOf("全部") }
    var isProbingSpeed by remember { mutableStateOf(false) }
    val latencyMap = remember { mutableStateMapOf<String, Long>() }
    var showImportTokenDialog by remember { mutableStateOf(false) }
    var importTokenText by remember { mutableStateOf("") }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    var permissionState by remember {
        mutableStateOf(PermissionManager.checkPermissions(context))
    }

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

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

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
        isSynthesizing = false
        currentTestingProviderId = null
    }

    fun playSpeechWithProvider(targetProvider: TtsProviderConfig, textToSpeak: String) {
        stopPlayback()
        isSynthesizing = true
        currentTestingProviderId = targetProvider.id

        scope.launch {
            try {
                val startMs = System.currentTimeMillis()
                val result = TtsProviderManager.getInstance().synthesize(textToSpeak, targetProvider)
                val costMs = System.currentTimeMillis() - startMs

                if (result.isSuccess) {
                    val rawBytes = result.getOrNull() ?: ByteArray(0)
                    lastSynthesizedBytes = rawBytes
                    lastSynthesizedProviderName = targetProvider.name
                    configDataStore.log("试听请求成功 [${targetProvider.name}] (${costMs}ms)，音频大小: ${rawBytes.size} 字节，开始播放")

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

    fun probeAllLatencies() {
        if (isProbingSpeed) return
        isProbingSpeed = true
        latencyMap.clear()

        scope.launch {
            for (provider in providers) {
                try {
                    val start = System.currentTimeMillis()
                    val res = TtsProviderManager.getInstance().synthesize("测试", provider)
                    val cost = System.currentTimeMillis() - start
                    if (res.isSuccess) {
                        latencyMap[provider.id] = cost
                    } else {
                        latencyMap[provider.id] = 9999L
                    }
                } catch (e: Exception) {
                    latencyMap[provider.id] = 9999L
                }
            }
            isProbingSpeed = false
        }
    }

    // 动态标签集合（内置标签 + 自定义标签）
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AI TTS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                        text = "经典列表交互视图",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = { configDataStore.updateSettings(settings.copy(appUiStyle = "BENTO")) },
                        label = { Text("🚀 Bento", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { configDataStore.updateSettings(settings.copy(appUiStyle = "STUDIO")) },
                        label = { Text("🎛️ Studio", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (!permissionState.isAllGranted) {
            item(contentType = "permission") {
                Spacer(modifier = Modifier.height(4.dp))
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

        // 当前激活的主音色卡片
        item(contentType = "active_banner") {
            SectionHeader(
                title = "当前系统生效发音模型",
                subtitle = "阅读/小说/读屏等所有第三方 App 将默认调用此配置"
            )

            val activeBrandColor = remember(activeProvider?.type) {
                activeProvider?.let { BrandTheme.getColorForType(it.type) }
            } ?: MaterialTheme.colorScheme.primary

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(activeBrandColor.copy(alpha = 0.25f), Color.Transparent)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = activeBrandColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeProvider?.name ?: "未选择提供商",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${activeProvider?.type?.displayName} · 音色: ${activeProvider?.voiceId?.ifBlank { "默认" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 核心参数胶囊标签行
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${activeProvider?.sampleRate ?: 24000}Hz",
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "语速 ${activeProvider?.speed ?: 1.0f}x",
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (activeProvider?.isDualRoleEnabled == true) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (activeProvider?.isDualRoleEnabled == true) "4声线有声剧场" else "标准单音色",
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = if (activeProvider?.isDualRoleEnabled == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (settings.isAudioCacheEnabled) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (settings.isAudioCacheEnabled) "极速本地缓存" else "在线直连",
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = if (settings.isAudioCacheEnabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 试听语料快捷生成与在线金句工具栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "试听文本:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(
                                onClick = {
                                    val item = QuoteService.getRandomLocalQuote()
                                    testText = item.text
                                    quoteSourceHint = "分类: ${item.category}"
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                label = { Text("🎲 随机语料", fontSize = 11.sp) }
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
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("拉取中...", fontSize = 11.sp)
                                    } else {
                                        Text("🌐 一言金句", fontSize = 11.sp)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
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
                                    Icon(Icons.Default.Clear, contentDescription = "清空文本", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        placeholder = { Text("输入或随机获取试听文本...") },
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

                    Spacer(modifier = Modifier.height(12.dp))
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
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSynthesizing && currentTestingProviderId == activeProvider?.id) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在合成...", fontSize = 13.sp)
                            } else if (isPlaying && currentTestingProviderId == activeProvider?.id) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("停止播放", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("立即试听", fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { activeProvider?.let { onNavigateToEditProvider(it.id) } },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("调节参数", fontSize = 12.5.sp)
                        }

                        if (lastSynthesizedBytes != null) {
                            OutlinedButton(
                                onClick = {
                                    exportAndShareAudio(lastSynthesizedBytes!!, lastSynthesizedProviderName)
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("导出WAV", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 引擎列表操作栏与过滤检索
        item(contentType = "filter_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "音色引擎列表 (${providers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "长按任意卡片即可拖拽排序，支持一键置顶与复制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {
                            configDataStore.updateSettings(settings.copy(appUiStyle = "STUDIO"))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "已切换为 Next-Gen Studio 调音台工作台", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("Studio 模式", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    )

                    IconButton(
                        onClick = { showSleepTimerDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "听书睡眠倒计时",
                            tint = if (isSleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = { showHistoryDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "朗读历史与统计",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = { isReorderMode = !isReorderMode }
                    ) {
                        Icon(
                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                            contentDescription = "切换排序模式",
                            tint = if (isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = { probeAllLatencies() }
                    ) {
                        if (isProbingSpeed) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = "一键测速", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (latencyMap.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                configDataStore.sortProvidersByLatency(latencyMap)
                                Toast.makeText(context, "已按响应延迟由快到慢排序", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "按速度排序", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val pasted = clip.getItemAt(0).text?.toString() ?: ""
                                if (pasted.startsWith("aitts://") || pasted.startsWith("{")) {
                                    importTokenText = pasted
                                }
                            }
                            showImportTokenDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "导入分享口令", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            val newId = UUID.randomUUID().toString()
                            val newConfig = TtsProviderConfig(
                                id = newId,
                                type = ProviderType.MIMO,
                                name = "新建 AI 语音模型",
                                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions",
                                modelName = "mimo-v2.5-tts",
                                voiceId = "茉莉"
                            )
                            configDataStore.updateProvider(newConfig)
                            onNavigateToEditProvider(newId)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建引擎", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (isReorderMode || draggingProviderId != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DragHandle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (draggingProviderId != null) "正在悬浮拖拽调整位置，上下移动手指即可换位..." else "长按任意卡片或按住右侧手柄 ≡ 即可上下拖动自由排序",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索引擎名称 / 音色 / 模型 / 角色标签...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 快捷分类过滤 Tag 栏 (支持动态自定义标签)
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
            item(contentType = "empty_placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("未找到符合条件的 AI 音色配置", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(filteredProviders, key = { it.id }, contentType = { "provider_card" }) { provider ->
                ProviderCard(
                    provider = provider,
                    isActive = provider.id == settings.activeProviderId,
                    latencyMs = latencyMap[provider.id],
                    isReorderMode = isReorderMode,
                    isDragging = draggingProviderId == provider.id,
                    dragOffsetY = if (draggingProviderId == provider.id) dragOffsetY else 0f,
                    onDragStart = {
                        draggingProviderId = provider.id
                        dragOffsetY = 0f
                    },
                    onDrag = { delta ->
                        handleItemDrag(provider.id, delta)
                    },
                    onDragEnd = {
                        draggingProviderId = null
                        dragOffsetY = 0f
                    },
                    onDragCancel = {
                        draggingProviderId = null
                        dragOffsetY = 0f
                    },
                    onSelect = {
                        configDataStore.setActiveProviderId(provider.id)
                    },
                    onEdit = {
                        onNavigateToEditProvider(provider.id)
                    },
                    onTest = {
                        playSpeechWithProvider(provider, "您好，我是 ${provider.name}，正在为您试听发音效果。")
                    },
                    onPinTop = {
                        configDataStore.pinProviderToTop(provider.id)
                        Toast.makeText(context, "已将 ${provider.name} 置顶", Toast.LENGTH_SHORT).show()
                    },
                    onMoveUp = {
                        configDataStore.moveProviderUp(provider.id)
                    },
                    onMoveDown = {
                        configDataStore.moveProviderDown(provider.id)
                    },
                    onDuplicate = {
                        configDataStore.duplicateProvider(provider.id)
                        Toast.makeText(context, "已复制配置副本", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        if (providers.size <= 1) {
                            Toast.makeText(context, "至少需要保留一个音色配置", Toast.LENGTH_SHORT).show()
                        } else {
                            configDataStore.deleteProvider(provider.id)
                            Toast.makeText(context, "已删除 ${provider.name}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShareToken = {
                        val token = configDataStore.exportProviderToken(provider)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI_TTS_Provider_Token", token))
                        Toast.makeText(context, "已复制【${provider.name}】分享口令到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showImportTokenDialog) {
        AlertDialog(
            onDismissRequest = { showImportTokenDialog = false },
            title = { Text("导入音色口令 / 分享码") },
            text = {
                Column {
                    Text("粘贴他人分享的 aitts://provider?data=... 口令或 JSON，一键自动解析导入", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importTokenText,
                        onValueChange = { importTokenText = it },
                        label = { Text("口令内容") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importTokenText.isNotBlank()) {
                            val imported = configDataStore.importProviderFromToken(importTokenText)
                            if (imported != null) {
                                Toast.makeText(context, "成功导入引擎【${imported.name}】", Toast.LENGTH_SHORT).show()
                                showImportTokenDialog = false
                                importTokenText = ""
                            } else {
                                Toast.makeText(context, "口令格式解析失败，请检查是否完整", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("立即导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportTokenDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("🌙 听书睡眠倒计时器") },
            text = {
                Column {
                    if (isSleepTimerActive) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("正在倒计时", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "${sleepRemainingSec / 60} 分 ${sleepRemainingSec % 60} 秒",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(onClick = { sleepTimerManager.stopTimer() }) {
                                    Text("取消定时")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text("设置定时关闭时长（结束前自动音量淡出并释放资源）：", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90).forEach { mins ->
                            Button(
                                onClick = {
                                    sleepTimerManager.startTimer(mins)
                                    Toast.makeText(context, "已设置 ${mins} 分钟后停止朗读", Toast.LENGTH_SHORT).show()
                                    showSleepTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("${mins} 分钟", fontSize = 12.sp)
                            }
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

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("📜 朗读历史与性能看板 (${historyItems.size})") },
            text = {
                if (historyItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("暂无朗读历史记录", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(historyItems, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.text, fontSize = 12.sp, maxLines = 2, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${item.costMs}ms", fontSize = 10.sp, color = if (item.costMs < 500) SuccessGreen else WarningOrange, fontWeight = FontWeight.Bold)
                                            Text("· ${item.characterCount}字", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            Text("· ${item.providerName}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            if (item.isFallbackUsed) {
                                                Text("⚠️ 降级兜底", fontSize = 10.sp, color = WarningOrange, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            activeProvider?.let { playSpeechWithProvider(it, item.text) }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "重播试听", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                if (historyItems.isNotEmpty()) {
                    TextButton(onClick = {
                        configDataStore.clearHistory()
                        Toast.makeText(context, "已清空朗读历史", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("清空历史", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}
