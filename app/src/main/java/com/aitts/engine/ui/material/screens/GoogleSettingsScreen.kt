package com.aitts.engine.ui.material.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.BuildConfig
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.material.GoogleColors
import com.aitts.engine.ui.material.components.GoogleLogViewerSheet
import java.util.Locale

/**
 * Google 官方应用风格 (Material Design 3) 系统设置界面
 *
 * 遵循 Material 3 与 Android AOSP 偏好设置官方规范：
 * 1. 【信息分级呈现】：主列表仅展示核心开关与生效值状态摘要 (Summary)，避免超长表单平铺轰炸；
 * 2. 【大段配置收纳】：切分策略、预加载深度、代理参数、连接超时、多主题风格等均收纳至标准 Dialog，选完即收起；
 * 3. 【彻底消除幽灵伪选项】：剥离未生效的自制悬浮坞形态选择（如“水平微胶囊/环形扇面盘”），保持官方规范的纯净质感；
 * 4. 【依赖性折叠】：主开关关闭时自动隐藏下属微调项，杜绝屏幕噪音。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSettingsScreen(
    modifier: Modifier = Modifier,
    configDataStore: ConfigDataStore,
    colors: GoogleColors
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val logs by configDataStore.logsFlow.collectAsState()

    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }

    fun getCacheSizeString(): String {
        val (count, sizeMb) = audioCacheManager.getStats()
        return if (sizeMb > 1024) String.format(Locale.getDefault(), "%.1f GB (%d 条)", sizeMb / 1024.0, count)
        else String.format(Locale.getDefault(), "%.1f MB (%d 条)", sizeMb, count)
    }

    var cacheSizeText by remember { mutableStateOf("计算中...") }
    var permState by remember { mutableStateOf(PermissionManager.checkPermissions(context)) }

    // 弹窗状态管理 (规范收纳)
    var showFallbackSelectorDialog by remember { mutableStateOf(false) }
    var showSegmentationModeDialog by remember { mutableStateOf(false) }
    var showPreloadDepthDialog by remember { mutableStateOf(false) }
    var showProxyConfigDialog by remember { mutableStateOf(false) }
    var tempProxyType by remember(settings.proxyType) { mutableStateOf(settings.proxyType) }
    var tempProxyHost by remember(settings.proxyHost) { mutableStateOf(settings.proxyHost) }
    var tempProxyPort by remember(settings.proxyPort) { mutableStateOf(settings.proxyPort.toString()) }
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var showThemeStyleDialog by remember { mutableStateOf(false) }
    var showThemeModeDialog by remember { mutableStateOf(false) }

    var showImportTextDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showResetStep1Dialog by remember { mutableStateOf(false) }
    var showResetStep2Dialog by remember { mutableStateOf(false) }
    var showLogsSheet by remember { mutableStateOf(false) }

    // 系统 SAF 文件选择器（导出与导入）
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonStr = configDataStore.exportAllConfigJson(desensitize = false)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "全量配置已成功导出至文件！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: ""
                if (jsonStr.isNotBlank() && configDataStore.importConfigJson(jsonStr)) {
                    cacheSizeText = getCacheSizeString()
                    Toast.makeText(context, "已从文件成功恢复所有配置！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "配置文件解析失败，请检查格式", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        cacheSizeText = getCacheSizeString()
        permState = PermissionManager.checkPermissions(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部大标题
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "系统与引擎设置",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Android 系统级集成、声学引擎、网络代理与全套外观偏好",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        // ==================== 1. 系统状态概览卡片 ====================
        item {
            val activeProvider = providers.find { it.id == settings.activeProviderId } ?: providers.firstOrNull()
            val fallbackProvider = providers.find { it.id == settings.fallbackProviderId }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = colors.primary, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Column {
                                Text("当前主力服务商", fontSize = 11.sp, color = colors.primary, fontWeight = FontWeight.SemiBold)
                                Text(activeProvider?.name ?: "未配置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = activeProvider?.type?.displayName ?: "EDGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.primary.copy(alpha = 0.15f), thickness = 0.8.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AltRoute, contentDescription = null, tint = colors.googleGreen, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (settings.autoFallbackOnFailure) "备用降级: ${fallbackProvider?.name ?: "Edge-TTS"}" else "备用降级: 未开启",
                                fontSize = 12.sp,
                                color = if (settings.autoFallbackOnFailure) colors.onPrimaryContainer else colors.textSecondary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = colors.googleYellow, modifier = Modifier.size(16.dp))
                            Text(
                                text = "缓存: $cacheSizeText",
                                fontSize = 12.sp,
                                color = colors.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // ==================== 2. 系统集成与权限保活 ====================
        item {
            SettingsGroupCard(title = "Android 系统集成与后台保活", colors = colors) {
                // 系统默认 TTS
                SettingsActionRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "系统默认 TTS 引擎",
                    subtitle = "跳转 Android 文本转语音设置将 AI-TTS 设为首选引擎",
                    actionText = "去设置",
                    colors = colors,
                    onClick = {
                        activity?.let { PermissionManager.openSystemTtsSettings(it) }
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 忽略电池优化
                SettingsActionRow(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "忽略电池优化 (后台保活)",
                    subtitle = if (permState.isIgnoringBatteryOptimizations) "已加入电池优化白名单 (系统不休眠杀后台)" else "未开启，长时间熄屏听书可能被系统电源管理中断",
                    actionText = if (permState.isIgnoringBatteryOptimizations) "已就绪" else "去申请",
                    colors = colors,
                    isSuccessBadge = permState.isIgnoringBatteryOptimizations,
                    onClick = {
                        activity?.let { PermissionManager.requestIgnoreBatteryOptimizations(it) }
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 全文件访问权限
                SettingsActionRow(
                    icon = Icons.Default.FolderShared,
                    title = "全文件管理访问权限 (Android 11+)",
                    subtitle = if (permState.hasAllFilesAccess) "已获取全文件管理权限 (支持本地离线模型与音色读取)" else "未授权，无法直接加载外部存储中的自定义音色与配置",
                    actionText = if (permState.hasAllFilesAccess) "已授权" else "去授权",
                    colors = colors,
                    isSuccessBadge = permState.hasAllFilesAccess,
                    onClick = {
                        activity?.let { PermissionManager.requestAllFilesAccess(it) }
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 后台通知栏状态条
                SettingsSwitchRow(
                    icon = Icons.Default.Notifications,
                    title = "后台朗读通知栏控制卡片",
                    subtitle = "在系统通知中心展示当前朗读进度与一键暂停/停止",
                    checked = settings.playbackNotificationEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(playbackNotificationEnabled = it)) }
                )
            }
        }

        // ==================== 3. 阅读器长文本切分与预加载 ====================
        item {
            SettingsGroupCard(title = "阅读器文本切分与预加载", colors = colors) {
                SettingsSwitchRow(
                    icon = Icons.Default.FormatQuote,
                    title = "启用文本切分预处理",
                    subtitle = if (settings.isSentenceSplittingEnabled) "开启：按设定策略智能切分并流水线并发预加载" else "关闭：阅读器传入文本整篇透传",
                    checked = settings.isSentenceSplittingEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isSentenceSplittingEnabled = it)) }
                )

                if (settings.isSentenceSplittingEnabled) {
                    HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                    val segModeLabel = when (settings.textSegmentationMode) {
                        "PARAGRAPH" -> "按换行自然段落"
                        "PUNCTUATION" -> "按句末标点断句"
                        "SMART_HYBRID" -> "智能对白角色混合"
                        else -> "按换行自然段落"
                    }
                    SettingsActionRow(
                        icon = Icons.Default.Tune,
                        title = "文本切分划分策略",
                        subtitle = "当前策略: $segModeLabel",
                        actionText = "更改",
                        colors = colors,
                        onClick = { showSegmentationModeDialog = true }
                    )

                    HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                    SettingsSwitchRow(
                        icon = Icons.Default.SwapHoriz,
                        title = "相邻短段落自动合并",
                        subtitle = "将连续极短段落 (<30字) 合并发送，消除频繁请求与语意割裂",
                        checked = settings.mergeShortParagraphs,
                        colors = colors,
                        onCheckedChange = { configDataStore.updateSettings(settings.copy(mergeShortParagraphs = it)) }
                    )

                    HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                    SettingsSwitchRow(
                        icon = Icons.Default.AltRoute,
                        title = "超长段落标点强制拆分",
                        subtitle = if (settings.splitLongParagraphs) "阈值: ${settings.maxSegmentLength} 字，超长时严格在句末断开" else "关闭：超长段落不强制截断",
                        checked = settings.splitLongParagraphs,
                        colors = colors,
                        onCheckedChange = { configDataStore.updateSettings(settings.copy(splitLongParagraphs = it)) }
                    )

                    if (settings.splitLongParagraphs) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("拆分字数阈值", fontSize = 12.5.sp, color = colors.textSecondary)
                                Text("${settings.maxSegmentLength} 字", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Slider(
                                value = settings.maxSegmentLength.toFloat(),
                                onValueChange = { configDataStore.updateSettings(settings.copy(maxSegmentLength = it.toInt())) },
                                valueRange = 80f..500f,
                                steps = 20,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }

                    HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                    SettingsSwitchRow(
                        icon = Icons.Default.Bolt,
                        title = "分段流式并发预加载",
                        subtitle = "朗读当前段时，在后台异步预合成接下来的段落",
                        checked = settings.enableSegmentPreload,
                        colors = colors,
                        onCheckedChange = { configDataStore.updateSettings(settings.copy(enableSegmentPreload = it)) }
                    )

                    if (settings.enableSegmentPreload) {
                        HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                        SettingsActionRow(
                            icon = Icons.Default.Speed,
                            title = "预加载前瞻深度",
                            subtitle = "当前深度: 提前准备 ${settings.preloadAheadCount} 段",
                            actionText = "调整",
                            colors = colors,
                            onClick = { showPreloadDepthDialog = true }
                        )
                    }
                }
            }
        }

        // ==================== 4. 声学发音与语意规范化 ====================
        item {
            SettingsGroupCard(title = "声学发音与语意规范化", colors = colors) {
                // 低延迟首包音频直出
                SettingsSwitchRow(
                    icon = Icons.Default.Speed,
                    title = "低延迟首包音频直出",
                    subtitle = "收到服务端前几十毫秒音频裸流分片即刻送入声卡播放，降低等待耗时",
                    checked = settings.ultraLowLatencyMode,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(ultraLowLatencyMode = it)) }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 标点停顿时间
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("标点分句自然停顿时间", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Text("逗号、句号及换行处的微停顿 (推荐 200~350ms)", fontSize = 11.5.sp, color = colors.textSecondary)
                        }
                        Text("${settings.sentencePauseMs} ms", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    Slider(
                        value = settings.sentencePauseMs.toFloat(),
                        onValueChange = { configDataStore.updateSettings(settings.copy(sentencePauseMs = it.toInt())) },
                        valueRange = 0f..1000f,
                        steps = 19,
                        colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                    )
                }

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 英文缩写规范化
                SettingsSwitchRow(
                    icon = Icons.Default.Spellcheck,
                    title = "英文缩写与专有名词规范化",
                    subtitle = "将 AI, CPU, WiFi, APP, GPU 等英文缩写转换为规范自然字母读音",
                    checked = settings.isAcronymNormalizationEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isAcronymNormalizationEnabled = it)) }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 数字转汉字规范化
                SettingsSwitchRow(
                    icon = Icons.Default.Tune,
                    title = "数字与度量衡智能读音规范",
                    subtitle = "自动将阿拉伯数字 (如 2026年, 120km/h) 转换为符合汉语习惯的文字",
                    checked = settings.isNumberNormalizationEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isNumberNormalizationEnabled = it)) }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 小说对白智能情感语气
                SettingsSwitchRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "小说对白智能情感语气注入",
                    subtitle = "自动识别双引号内对话角色语境，在提示词中动态注入情绪重音",
                    checked = settings.isEmotionProsodyEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isEmotionProsodyEnabled = it)) }
                )
            }
        }

        // ==================== 5. 网络代理与高可用故障转移 ====================
        item {
            SettingsGroupCard(title = "网络代理与高可用故障转移", colors = colors) {
                // 故障自动降级
                val fallbackProvider = providers.find { it.id == settings.fallbackProviderId }
                SettingsSwitchRow(
                    icon = Icons.Default.AltRoute,
                    title = "主备自动故障转移 (Auto Fallback)",
                    subtitle = if (settings.autoFallbackOnFailure) "发生异常时降级至: ${fallbackProvider?.name ?: "Edge-TTS"}" else "未开启",
                    checked = settings.autoFallbackOnFailure,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = it)) }
                )

                if (settings.autoFallbackOnFailure) {
                    HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                    SettingsActionRow(
                        icon = Icons.Default.SwapHoriz,
                        title = "指定备用兜底引擎",
                        subtitle = "当前: ${fallbackProvider?.name ?: "Edge-TTS (默认)"}",
                        actionText = "更换",
                        colors = colors,
                        onClick = { showFallbackSelectorDialog = true }
                    )
                }

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 网络错误重试
                SettingsSwitchRow(
                    icon = Icons.Default.Refresh,
                    title = "网络错误自动重试",
                    subtitle = "遇偶发网络抖动时按指数退避自动重试 2 次",
                    checked = settings.autoRetryOnFailure,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(autoRetryOnFailure = it)) }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 全局网络代理
                SettingsSwitchRow(
                    icon = Icons.Default.Language,
                    title = "全局网络代理",
                    subtitle = if (settings.proxyEnabled) "${settings.proxyType} · ${settings.proxyHost.ifBlank { "127.0.0.1" }}:${settings.proxyPort}" else "未开启，直连各 TTS 服务商",
                    checked = settings.proxyEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(proxyEnabled = it)) }
                )

                if (settings.proxyEnabled) {
                    HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                    SettingsActionRow(
                        icon = Icons.Default.Tune,
                        title = "代理服务器配置",
                        subtitle = "${settings.proxyType}://${settings.proxyHost.ifBlank { "127.0.0.1" }}:${settings.proxyPort}",
                        actionText = "配置",
                        colors = colors,
                        onClick = {
                            tempProxyType = settings.proxyType
                            tempProxyHost = settings.proxyHost
                            tempProxyPort = settings.proxyPort.toString()
                            showProxyConfigDialog = true
                        }
                    )
                }

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 连接超时
                SettingsActionRow(
                    icon = Icons.Default.Timer,
                    title = "请求连接超时",
                    subtitle = "当前限制: ${settings.connectTimeoutSeconds} 秒",
                    actionText = "修改",
                    colors = colors,
                    onClick = { showTimeoutDialog = true }
                )
            }
        }

        // ==================== 6. 界面外观与多主题风格 ====================
        item {
            SettingsGroupCard(title = "界面外观与主题风格", colors = colors) {
                val currentStyleName = when (settings.appUiStyle) {
                    "MATERIAL" -> "Google 官方样式 (Material 3)"
                    "PULSE" -> "极光微胶囊 (Pulse)"
                    "BENTO" -> "全景网格 (Bento)"
                    "STUDIO" -> "专业调音台 (Studio)"
                    "VINYL" -> "复古黑胶 (Vinyl)"
                    else -> "Google 官方样式 (Material 3)"
                }
                SettingsActionRow(
                    icon = Icons.Default.Palette,
                    title = "界面设计系统",
                    subtitle = "当前: $currentStyleName",
                    actionText = "切换风格",
                    colors = colors,
                    onClick = { showThemeStyleDialog = true }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                val currentModeName = when (settings.appThemeMode.uppercase()) {
                    "LIGHT" -> "浅色模式"
                    "DARK" -> "深色模式"
                    else -> "跟随系统"
                }
                SettingsActionRow(
                    icon = Icons.Default.DarkMode,
                    title = "明暗外观模式",
                    subtitle = "当前: $currentModeName",
                    actionText = "切换模式",
                    colors = colors,
                    onClick = { showThemeModeDialog = true }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                SettingsSwitchRow(
                    icon = Icons.Default.GraphicEq,
                    title = "触觉震动微反馈 (Haptics)",
                    subtitle = "在拖拽排序、长按与按键点击时提供物理触觉震动",
                    checked = settings.hapticFeedbackEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(hapticFeedbackEnabled = it)) }
                )
            }
        }

        // ==================== 7. 存储缓存、配置备份与恢复 ====================
        item {
            SettingsGroupCard(title = "存储缓存、配置备份与恢复", colors = colors) {
                // 音频缓存清理
                SettingsActionRow(
                    icon = Icons.Default.CleaningServices,
                    title = "本地音频 LRU 磁盘缓存",
                    subtitle = "已占用存储: $cacheSizeText",
                    actionText = "立即清空",
                    colors = colors,
                    onClick = { showClearCacheDialog = true }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 文件级导出 / 导入
                SettingsActionRow(
                    icon = Icons.Default.FileDownload,
                    title = "导出全量配置到 JSON 文件",
                    subtitle = "保存全部模型参数、发音正则库与首选项到本地文件",
                    actionText = "导出文件",
                    colors = colors,
                    onClick = {
                        exportFileLauncher.launch("ai-tts-backup-${System.currentTimeMillis()}.json")
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                SettingsActionRow(
                    icon = Icons.Default.FileUpload,
                    title = "从本地 JSON 文件恢复配置",
                    subtitle = "一键无损读取本地备份文件并覆盖恢复所有配置",
                    actionText = "从文件恢复",
                    colors = colors,
                    onClick = {
                        importFileLauncher.launch(arrayOf("application/json", "text/*"))
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 剪贴板复制 / 粘贴
                SettingsActionRow(
                    icon = Icons.Default.ContentCopy,
                    title = "复制全量配置到剪贴板",
                    subtitle = "以标准 JSON 文本形式复制到剪贴板，方便跨设备快速迁移",
                    actionText = "复制文本",
                    colors = colors,
                    onClick = {
                        val jsonStr = configDataStore.exportAllConfigJson(desensitize = false)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS-Backup", jsonStr))
                        Toast.makeText(context, "全量配置已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                SettingsActionRow(
                    icon = Icons.Default.ContentPaste,
                    title = "粘贴文本恢复配置",
                    subtitle = "从剪贴板粘贴已备份的 JSON 字符串进行解析恢复",
                    actionText = "粘贴恢复",
                    colors = colors,
                    onClick = { showImportTextDialog = true }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 恢复出厂
                SettingsActionRow(
                    icon = Icons.Default.RestartAlt,
                    title = "恢复出厂官方初始预设",
                    subtitle = "两步防误触安全机制，抹除所有自定义项并还原为开箱初始状态",
                    actionText = "出厂重置",
                    colors = colors,
                    onClick = { showResetStep1Dialog = true }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 实时调试日志抽屉
                SettingsActionRow(
                    icon = Icons.Default.Terminal,
                    title = "实时请求诊断日志抽屉",
                    subtitle = "查看引擎最近发起的请求详情、首字耗时与网络响应",
                    actionText = "查看日志",
                    colors = colors,
                    onClick = { showLogsSheet = true }
                )
            }
        }

        // ==================== 8. 关于与开源致谢 ====================
        item {
            SettingsGroupCard(title = "关于软件与开源生态致谢", colors = colors) {
                SettingsActionRow(
                    icon = Icons.Default.Info,
                    title = "软件版本",
                    subtitle = "AI-TTS Engine v${BuildConfig.VERSION_NAME} (Google Material 3 Edition)",
                    actionText = "测试版",
                    colors = colors,
                    isSuccessBadge = true,
                    onClick = {}
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("项目开源协议与致谢:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text(
                        text = "• 开发者: Antigravity (Google DeepMind) & Misaka02\n" +
                                "• 开源协议: Apache-2.0 License\n" +
                                "• 变频变速: Sonic (Bill Cox)\n" +
                                "• 神经网络: Sherpa-ONNX (k2-fsa)\n" +
                                "• 离线模型: Piper, MeloTTS, Matcha, Kokoro, ChatTTS, CosyVoice, GPT-SoVITS\n" +
                                "• 听书生态: 开源阅读 (Legado), 静读天下 (Moon+ Reader)",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/misaka02/ai-tts-android"))
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法打开浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("打开 GitHub", fontSize = 12.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("GitHub URL", "https://github.com/misaka02/ai-tts-android"))
                                Toast.makeText(context, "已复制 GitHub 仓库主页链接！", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(40.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("复制链接", fontSize = 12.5.sp)
                        }
                    }
                }
            }
        }
    }

    // ==================== 规范收纳弹窗区 ====================

    // 1. 备用兜底降级模型选择弹窗
    if (showFallbackSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showFallbackSelectorDialog = false },
            title = { Text("选择备用降级模型", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("当主力引擎出现网络异常、429 超频限流或超时时，将自动切换至此模型合成：", fontSize = 12.sp, color = colors.textSecondary)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(providers) { p ->
                            val isSelected = settings.fallbackProviderId == p.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(fallbackProviderId = p.id, autoFallbackOnFailure = true))
                                        showFallbackSelectorDialog = false
                                        Toast.makeText(context, "已指定备用降级模型: ${p.name}", Toast.LENGTH_SHORT).show()
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colors.primaryContainer else colors.surfaceContainerHigh,
                                border = if (isSelected) BorderStroke(1.2.dp, colors.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(p.name, fontSize = 13.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) colors.onPrimaryContainer else colors.textPrimary)
                                        Text("${p.type.displayName} · ${p.voiceId.ifBlank { "默认音色" }}", fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFallbackSelectorDialog = false }) { Text("完成") }
            }
        )
    }

    // 2. 文本切分划分策略选择弹窗
    if (showSegmentationModeDialog) {
        val segModes = listOf(
            Triple("PARAGRAPH", "按换行自然段落", "保留小说作者自然意境段落，换行即分段"),
            Triple("PUNCTUATION", "按句末标点断句", "遇到句号/问号/感叹号即切分，超长段落极速切碎播放"),
            Triple("SMART_HYBRID", "智能对白角色混合", "自动识别引号内对话与叙述旁白混合划分")
        )
        AlertDialog(
            onDismissRequest = { showSegmentationModeDialog = false },
            title = { Text("选择文本切分策略", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    segModes.forEach { (modeKey, title, desc) ->
                        val isCurrent = settings.textSegmentationMode == modeKey
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(textSegmentationMode = modeKey))
                                    showSegmentationModeDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh,
                            border = if (isCurrent) BorderStroke(1.2.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium, fontSize = 13.5.sp, color = if (isCurrent) colors.onPrimaryContainer else colors.textPrimary)
                                    Text(desc, fontSize = 11.5.sp, color = if (isCurrent) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary)
                                }
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSegmentationModeDialog = false }) { Text("关闭") }
            }
        )
    }

    // 3. 预加载前瞻深度选择弹窗
    if (showPreloadDepthDialog) {
        val depths = listOf(
            1 to "提前 1 块 (省流量/低消耗)",
            2 to "提前 2 块 (标准推荐，平衡流畅)",
            3 to "提前 3 块 (深度预热，长篇连续朗读)",
            4 to "提前 4 块 (强劲管线，极速网络直通)"
        )
        AlertDialog(
            onDismissRequest = { showPreloadDepthDialog = false },
            title = { Text("选择预加载前瞻深度", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    depths.forEach { (count, label) ->
                        val isCurrent = settings.preloadAheadCount == count
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(preloadAheadCount = count))
                                    showPreloadDepthDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh,
                            border = if (isCurrent) BorderStroke(1.2.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) colors.onPrimaryContainer else colors.textPrimary)
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreloadDepthDialog = false }) { Text("关闭") }
            }
        )
    }

    // 4. 网络代理服务器配置弹窗
    if (showProxyConfigDialog) {
        AlertDialog(
            onDismissRequest = { showProxyConfigDialog = false },
            title = { Text("配置网络代理服务器", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("请选择代理协议并输入本地代理端口 (如 Clash / V2Ray / Sing-box)：", fontSize = 12.sp, color = colors.textSecondary)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HTTP", "SOCKS").forEach { type ->
                            val isCur = tempProxyType.uppercase() == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { tempProxyType = type },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCur) colors.primaryContainer else colors.surfaceContainerHigh,
                                border = if (isCur) BorderStroke(1.2.dp, colors.primary) else null
                            ) {
                                Text(
                                    text = "$type 代理",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isCur) colors.onPrimaryContainer else colors.textSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempProxyHost,
                        onValueChange = { tempProxyHost = it },
                        label = { Text("代理 Host") },
                        placeholder = { Text("127.0.0.1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = tempProxyPort,
                        onValueChange = { tempProxyPort = it },
                        label = { Text("代理端口 (Port)") },
                        placeholder = { Text("7890") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val port = tempProxyPort.toIntOrNull() ?: 7890
                    configDataStore.updateSettings(
                        settings.copy(
                            proxyType = tempProxyType,
                            proxyHost = tempProxyHost.ifBlank { "127.0.0.1" },
                            proxyPort = port,
                            proxyEnabled = true
                        )
                    )
                    showProxyConfigDialog = false
                    Toast.makeText(context, "代理配置已更新", Toast.LENGTH_SHORT).show()
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProxyConfigDialog = false }) { Text("取消") }
            }
        )
    }

    // 5. 请求连接超时选择弹窗
    if (showTimeoutDialog) {
        val timeouts = listOf(
            10 to "10 秒 (超快失败，快速降级)",
            15 to "15 秒 (标准推荐，适用绝大多数场景)",
            30 to "30 秒 (弱网宽松，应对偶发波动)",
            60 to "60 秒 (超长文本，大模型慢速推理)"
        )
        AlertDialog(
            onDismissRequest = { showTimeoutDialog = false },
            title = { Text("设置连接超时", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeouts.forEach { (sec, label) ->
                        val isSel = settings.connectTimeoutSeconds == sec
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(connectTimeoutSeconds = sec))
                                    showTimeoutDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) colors.primaryContainer else colors.surfaceContainerHigh,
                            border = if (isSel) BorderStroke(1.2.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) colors.onPrimaryContainer else colors.textPrimary)
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimeoutDialog = false }) { Text("关闭") }
            }
        )
    }

    // 6. 界面设计系统切换弹窗
    if (showThemeStyleDialog) {
        val uiStyles = listOf(
            Triple("MATERIAL", "Google 官方样式 (Material 3)", "Google Recorder / Pixel 原生设计，极简纯净"),
            Triple("PULSE", "极光微胶囊 (Pulse)", "经典极光流动微胶囊主控，灵动光效"),
            Triple("BENTO", "全景网格 (Bento)", "模块化网格矩阵工作台，全信息聚合"),
            Triple("STUDIO", "专业调音台 (Studio)", "DAW 多轨音频控制台，高密专业掌控"),
            Triple("VINYL", "复古黑胶 (Vinyl)", "经典黑胶唱片阅览舱，优雅典藏黑胶质感")
        )
        AlertDialog(
            onDismissRequest = { showThemeStyleDialog = false },
            title = { Text("切换界面设计系统", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiStyles.forEach { (key, name, desc) ->
                        val isCurrent = settings.appUiStyle == key
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(appUiStyle = key))
                                    showThemeStyleDialog = false
                                    Toast.makeText(context, "已切换为: $name", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh,
                            border = if (isCurrent) BorderStroke(1.2.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp, color = if (isCurrent) colors.onPrimaryContainer else colors.textPrimary)
                                    Text(desc, fontSize = 11.sp, color = if (isCurrent) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary)
                                }
                                if (isCurrent) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeStyleDialog = false }) { Text("关闭") }
            }
        )
    }

    // 7. 明暗外观模式选择弹窗
    if (showThemeModeDialog) {
        val modes = listOf("SYSTEM" to "跟随系统", "LIGHT" to "浅色模式", "DARK" to "深色模式")
        AlertDialog(
            onDismissRequest = { showThemeModeDialog = false },
            title = { Text("选择明暗外观模式", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { (modeKey, modeTitle) ->
                        val isSel = settings.appThemeMode.uppercase() == modeKey
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(appThemeMode = modeKey))
                                    showThemeModeDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) colors.primaryContainer else colors.surfaceContainerHigh,
                            border = if (isSel) BorderStroke(1.2.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(modeTitle, fontSize = 13.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) colors.onPrimaryContainer else colors.textPrimary)
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeModeDialog = false }) { Text("关闭") }
            }
        )
    }

    // 8. 粘贴 JSON 文本恢复配置弹窗
    if (showImportTextDialog) {
        AlertDialog(
            onDismissRequest = { showImportTextDialog = false },
            title = { Text("从 JSON 文本恢复配置", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("请在下方输入框中粘贴备份的完整 JSON 字符串：", fontSize = 12.sp, color = colors.textSecondary)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        placeholder = { Text("{\"settings\":{...}, \"providers\":[...]}") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank() && configDataStore.importConfigJson(importJsonText)) {
                            cacheSizeText = getCacheSizeString()
                            showImportTextDialog = false
                            importJsonText = ""
                            Toast.makeText(context, "配置已成功恢复！", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "JSON 解析失败，请检查文本格式是否完整", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("确认恢复")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportTextDialog = false }) { Text("取消") }
            }
        )
    }

    // 9. 清理本地音频缓存二次确认弹窗
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清空本地音频磁盘缓存", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "确定要清空全部离线音频缓存吗？\n当前已占用: $cacheSizeText\n清空后再次朗读相同句子将重新发起在线网络请求。",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        audioCacheManager.clearAll()
                        cacheSizeText = getCacheSizeString()
                        showClearCacheDialog = false
                        Toast.makeText(context, "本地音频缓存已全部清空！", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.googleRed, contentColor = Color.White)
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("取消") }
            }
        )
    }

    // 10. 恢复出厂设置第 1 步确认
    if (showResetStep1Dialog) {
        AlertDialog(
            onDismissRequest = { showResetStep1Dialog = false },
            title = { Text("⚠️ 恢复出厂设置 (第 1/2 步)", fontWeight = FontWeight.Bold, color = colors.googleRed) },
            text = {
                Text(
                    "此操作将清除所有自定义服务商配置、API Key 密钥、自定义发音规则与参数，彻底恢复为官方默认预设。确定继续吗？",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetStep1Dialog = false
                        showResetStep2Dialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.googleRed, contentColor = Color.White)
                ) {
                    Text("下一步 (最终确认)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep1Dialog = false }) { Text("取消") }
            }
        )
    }

    // 11. 恢复出厂设置第 2 步高危最终确认
    if (showResetStep2Dialog) {
        AlertDialog(
            onDismissRequest = { showResetStep2Dialog = false },
            title = { Text("🔥 最终警告: 数据抹除 (第 2/2 步)", fontWeight = FontWeight.Black, color = colors.googleRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("【高危警告】此操作绝对无法撤回！", fontWeight = FontWeight.Bold, color = colors.googleRed, fontSize = 13.sp)
                    Text("点击确认后，所有自定义服务商与规则将立即被永久抹除，应用将重置为刚安装时的初始出厂状态。", fontSize = 12.5.sp, color = colors.textSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        configDataStore.resetToDefaults()
                        cacheSizeText = getCacheSizeString()
                        showResetStep2Dialog = false
                        Toast.makeText(context, "已彻底恢复为官方初始出厂预设！", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.googleRed, contentColor = Color.White)
                ) {
                    Text("知晓后果，立即重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStep2Dialog = false }) { Text("取消放弃") }
            }
        )
    }

    // 12. 实时诊断日志抽屉 (Material 3 标准组件，支持生命周期聚合、单次独立复制与二次清空确认)
    if (showLogsSheet) {
        GoogleLogViewerSheet(
            configDataStore = configDataStore,
            colors = colors,
            onDismiss = { showLogsSheet = false }
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    colors: GoogleColors,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outlineSubtle)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    colors: GoogleColors,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = colors.surfaceContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Text(subtitle, fontSize = 11.5.sp, color = colors.textSecondary)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.textTertiary,
                uncheckedTrackColor = colors.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionText: String,
    colors: GoogleColors,
    isSuccessBadge: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = colors.surfaceContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                }
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Text(subtitle, fontSize = 11.5.sp, color = colors.textSecondary)
            }
        }

        Surface(
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = if (isSuccessBadge) colors.googleGreen.copy(alpha = 0.15f) else colors.primaryContainer
        ) {
            Text(
                text = actionText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSuccessBadge) colors.googleGreen else colors.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
