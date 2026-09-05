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
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.material.GoogleColors

/**
 * ⚙️ Google Pixel 系统设置规范 - 全能配置中心 (Google Settings Screen)
 * 1. 严格对齐 Google Pixel Settings 原生卡片分组与清晰层级；
 * 2. 完整适配系统集成、长文本流式分段、电池保活、音频缓存清理；
 * 3. 5 大界面风格直选卡片（Google 官方样式 / 极光微胶囊 / 全景网格 / 专业调音 / 复古黑胶）；
 * 4. 配置 JSON 导入导出与关于诊断。
 */
@Composable
fun GoogleSettingsScreen(
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settings by configDataStore.settingsFlow.collectAsState()
    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }

    var cacheSizeMb by remember { mutableStateOf("计算中...") }
    var permState by remember { mutableStateOf(PermissionManager.checkPermissions(context)) }

    LaunchedEffect(Unit) {
        val stats = audioCacheManager.getStats()
        cacheSizeMb = String.format("%.1f MB", stats.second)
        permState = PermissionManager.checkPermissions(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 标题区
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "系统与引擎设置",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "管理 Android 系统集成、合成策略与外观偏好",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        }

        // 分组 1：系统集成与权限保活
        item {
            SettingsGroupCard(title = "系统集成与权限", colors = colors) {
                // 系统默认 TTS
                SettingsActionRow(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "系统默认 TTS 引擎",
                    subtitle = "跳转 Android 文本转语音设置将 AI-TTS 设为首选",
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
                    subtitle = if (permState.isIgnoringBatteryOptimizations) "已加入电池优化白名单 (防系统杀后台)" else "未开启，长时间听书可能被系统休眠中断",
                    actionText = if (permState.isIgnoringBatteryOptimizations) "已就绪" else "申请",
                    colors = colors,
                    isSuccessBadge = permState.isIgnoringBatteryOptimizations,
                    onClick = {
                        activity?.let { PermissionManager.requestIgnoreBatteryOptimizations(it) }
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 全能悬浮主控坞
                SettingsSwitchRow(
                    icon = Icons.Default.Tune,
                    title = "启用全局全能悬浮主控坞",
                    subtitle = "在任何第三方听书/阅读 App 界面悬浮自由取词与控制",
                    checked = settings.isFloatingDockEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isFloatingDockEnabled = it)) }
                )
            }
        }

        // 分组 2：界面外观与多主题风格切换
        item {
            SettingsGroupCard(title = "界面外观与主题风格", colors = colors) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("界面设计风格 (一键无损切换)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    Text("本测试版已全新适配 Google 官方应用风格，您也可以随时切换回其他主题：", fontSize = 12.sp, color = colors.textSecondary)

                    val uiStyles = listOf(
                        Triple("MATERIAL", "Google 官方样式 (Material 3)", "Google Recorder / Pixel 原生质感"),
                        Triple("PULSE", "极光微胶囊 (Pulse)", "经典极光动态微胶囊主控"),
                        Triple("BENTO", "全景网格 (Bento)", "模块化网格矩阵工作台"),
                        Triple("STUDIO", "专业调音台 (Studio)", "DAW 多轨音频控制台"),
                        Triple("VINYL", "复古黑胶 (Vinyl)", "经典黑胶唱片阅览舱")
                    )

                    uiStyles.forEach { (key, name, desc) ->
                        val isCurrent = settings.appUiStyle == key
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    configDataStore.updateSettings(settings.copy(appUiStyle = key))
                                    Toast.makeText(context, "已切换为: $name", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) colors.primaryContainer else colors.surfaceContainerHigh,
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, colors.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium, fontSize = 13.5.sp, color = if (isCurrent) colors.onPrimaryContainer else colors.textPrimary)
                                    Text(desc, fontSize = 11.sp, color = if (isCurrent) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.textSecondary)
                                }

                                if (isCurrent) {
                                    Surface(shape = CircleShape, color = colors.primary) {
                                        Icon(androidx.compose.material.icons.Icons.Default.Radio, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(16.dp).padding(2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                // 深浅色模式切换
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("主题深浅模式", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                    val modes = listOf("SYSTEM" to "跟随系统", "LIGHT" to "浅色模式", "DARK" to "深色模式")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { (modeKey, modeTitle) ->
                            val isSel = settings.appThemeMode.uppercase() == modeKey
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(appThemeMode = modeKey))
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) colors.primaryContainer else colors.surfaceContainerHigh,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                            ) {
                                Text(
                                    text = modeTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) colors.onPrimaryContainer else colors.textSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 分组 3：合成策略与性能
        item {
            SettingsGroupCard(title = "合成策略与性能优化", colors = colors) {
                SettingsSwitchRow(
                    icon = Icons.Default.Speed,
                    title = "极速流式首字直出",
                    subtitle = "收到服务端前几十毫秒裸流音频包即刻播放，消除等待",
                    checked = settings.streamingSynthesis,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(streamingSynthesis = it)) }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                SettingsSwitchRow(
                    icon = Icons.Default.AltRoute,
                    title = "长文本智能段落分片并发",
                    subtitle = "自动按自然段切分并发起流水线预加载，阅读翻页无缝衔接",
                    checked = settings.isSentenceSplittingEnabled,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isSentenceSplittingEnabled = it)) }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                SettingsSwitchRow(
                    icon = Icons.Default.Refresh,
                    title = "网络波动智能抖动重试",
                    subtitle = "在大模型接口遭遇偶发网络抖动时自动轻量级恢复重试",
                    checked = settings.autoRetryOnFailure,
                    colors = colors,
                    onCheckedChange = { configDataStore.updateSettings(settings.copy(autoRetryOnFailure = it)) }
                )
            }
        }

        // 分组 4：缓存与存储
        item {
            SettingsGroupCard(title = "存储与缓存管理", colors = colors) {
                SettingsActionRow(
                    icon = Icons.Default.CleaningServices,
                    title = "音频磁盘缓存",
                    subtitle = "当前已占用空间: $cacheSizeMb",
                    actionText = "一键清理",
                    colors = colors,
                    onClick = {
                        audioCacheManager.clearAll()
                        cacheSizeMb = "0.0 MB"
                        Toast.makeText(context, "音频缓存已全部清空", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // 分组 5：备份、恢复与关于
        item {
            SettingsGroupCard(title = "关于与数据备份", colors = colors) {
                SettingsActionRow(
                    icon = Icons.Default.FileUpload,
                    title = "导出全量配置备份",
                    subtitle = "将所有服务商、发音规则与参数打包为 JSON 复制到剪贴板",
                    actionText = "导出",
                    colors = colors,
                    onClick = {
                        val jsonStr = configDataStore.exportAllConfigJson()
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("AI-TTS-Backup", jsonStr))
                        Toast.makeText(context, "配置备份已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

                SettingsActionRow(
                    icon = Icons.Default.Info,
                    title = "版本信息",
                    subtitle = "AI-TTS Engine v3.8.5-test (Google Material Edition)",
                    actionText = "最新版",
                    colors = colors,
                    isSuccessBadge = true,
                    onClick = {}
                )
            }
        }
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
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
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
