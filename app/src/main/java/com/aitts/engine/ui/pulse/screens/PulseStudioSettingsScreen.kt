package com.aitts.engine.ui.pulse.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import java.util.Locale

/**
 * ⚡ Pulse 系统与引擎设置中心 (OneUI 沉浸式全景流体布局)
 * 1. 顶部 6 大分舱快捷筛选（支持全部展示与分舱聚焦），绝无留白；
 * 2. 真实控件直接在屏上响应：微句断句停顿步进器、防杀保活、网络自愈、缓存管理、备份恢复；
 * 3. 完美适配大拇指操作区与底部导航栏，底边距留足 140dp 顺畅滑动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseStudioSettingsScreen(
    configDataStore: ConfigDataStore
) {
    val context = LocalContext.current
    val settings by configDataStore.settingsFlow.collectAsState()
    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }

    fun getCacheSizeString(): String {
        val (_, sizeMb) = audioCacheManager.getStats()
        return String.format(Locale.US, "%.1f MB", sizeMb)
    }

    var cacheSizeText by remember { mutableStateOf(getCacheSizeString()) }
    var selectedCategory by remember { mutableIntStateOf(0) }
    var importDialogText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonStr = configDataStore.exportAllConfigJson(desensitize = false)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "配置已成功导出为文件！", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "配置文件解析失败", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val categories = listOf(
        "✨ 全部配置",
        "⚙️ 系统权限",
        "📖 听书断句",
        "⚡ 网络自愈",
        "🎨 外观触觉",
        "💾 数据备份"
    )

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
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                    Text(
                        text = "系统与引擎首选项",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = PulseTokens.TextPrimary
                    )
                    Text(
                        text = "单手掌控 · 真实控件直出交互",
                        fontSize = 11.sp,
                        color = PulseTokens.CyanElectric,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 顶部快捷分舱筛选条
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(categories) { idx, title ->
                        val isSelected = selectedCategory == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = idx },
                            label = { Text(title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PulseTokens.CyanElectric,
                                selectedLabelColor = Color.Black,
                                containerColor = PulseTokens.SurfaceElevated,
                                labelColor = PulseTokens.TextSecondary
                            ),
                            border = if (isSelected) null else PulseTokens.BorderSubtle
                        )
                    }
                }
            }

            // 1. ⚙️ 系统与权限
            if (selectedCategory == 0 || selectedCategory == 1) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("⚙️ 系统服务与权限", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("设为系统默认 TTS 引擎", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("在系统设置中指定 AI TTS 为首选服务", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
                                        } catch (e: Exception) {
                                            context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS))
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black)
                                ) {
                                    Text("去设置", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("后台电池白名单保护", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("防止锁屏听书长时间后台被系统强杀", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Button(
                                    onClick = { PermissionManager.requestIgnoreBatteryOptimizations(context as Activity) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.TextPrimary),
                                    border = PulseTokens.BorderSubtle
                                ) {
                                    Text("防杀保活", fontSize = 12.sp)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("通知栏播放控制中心", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("在下拉通知栏显示实时朗读卡片与停播键", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.playbackNotificationEnabled,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(playbackNotificationEnabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 📖 听书断句与停顿
            if (selectedCategory == 0 || selectedCategory == 2) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("📖 听书断句与停顿微调", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("智能自然标点断句", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("仅在句末标点（。！？）处切分，绝不断开词句", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.isSentenceSplittingEnabled,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isSentenceSplittingEnabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("微句停顿: ${settings.sentencePauseMs} ms", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable {
                                            val next = (settings.sentencePauseMs - 10).coerceAtLeast(0)
                                            configDataStore.updateSettings(settings.copy(sentencePauseMs = next))
                                        }) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = PulseTokens.TextPrimary, modifier = Modifier.padding(6.dp).size(14.dp))
                                        }
                                        Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable {
                                            val next = (settings.sentencePauseMs + 10).coerceAtMost(500)
                                            configDataStore.updateSettings(settings.copy(sentencePauseMs = next))
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.padding(6.dp).size(14.dp))
                                        }
                                    }
                                }
                                Slider(
                                    value = settings.sentencePauseMs.toFloat(),
                                    onValueChange = { configDataStore.updateSettings(settings.copy(sentencePauseMs = it.toInt())) },
                                    valueRange = 0f..500f,
                                    steps = 10
                                )
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("单句最大字数限制: ${settings.maxSentenceLength} 字", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable {
                                            val next = (settings.maxSentenceLength - 10).coerceAtLeast(30)
                                            configDataStore.updateSettings(settings.copy(maxSentenceLength = next))
                                        }) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = PulseTokens.TextPrimary, modifier = Modifier.padding(6.dp).size(14.dp))
                                        }
                                        Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable {
                                            val next = (settings.maxSentenceLength + 10).coerceAtMost(500)
                                            configDataStore.updateSettings(settings.copy(maxSentenceLength = next))
                                        }) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.padding(6.dp).size(14.dp))
                                        }
                                    }
                                }
                                Slider(
                                    value = settings.maxSentenceLength.toFloat(),
                                    onValueChange = { configDataStore.updateSettings(settings.copy(maxSentenceLength = it.toInt())) },
                                    valueRange = 30f..500f,
                                    steps = 47
                                )
                            }
                        }
                    }
                }
            }

            // 3. ⚡ 网络自愈与缓存
            if (selectedCategory == 0 || selectedCategory == 3) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("⚡ 网络自愈与性能缓存", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("网络抖动自愈重试", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("遇到网络瞬时抖动时自动进行指数退避重试", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.autoRetryOnFailure,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(autoRetryOnFailure = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("主模型异常自动降级", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("主力模型超时不可用时无缝切换备用模型", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.autoFallbackOnFailure,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("本地音频缓存加速", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("已占用: $cacheSizeText / 上限 ${settings.maxCacheSizeMb} MB", fontSize = 11.5.sp, color = PulseTokens.CyanElectric)
                                }
                                Switch(
                                    checked = settings.isAudioCacheEnabled,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isAudioCacheEnabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }

                            Button(
                                onClick = {
                                    audioCacheManager.clearAll()
                                    cacheSizeText = getCacheSizeString()
                                    Toast.makeText(context, "已清空本地音频缓存", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.MagentaLaser),
                                border = PulseTokens.BorderSubtle,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("一键清理音频缓存", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. 🎨 外观与触觉
            if (selectedCategory == 0 || selectedCategory == 4) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("🎨 外观主题与触觉反馈", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("A 屏纯黑极夜模式", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("OLED 屏幕绝对 #000000 纯黑省电", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.isAmoledPureBlack,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(isAmoledPureBlack = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("物理触觉震动反馈", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = PulseTokens.TextPrimary)
                                    Text("拖动与点击时提供物理震动确认", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.hapticFeedbackEnabled,
                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(hapticFeedbackEnabled = it)) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }
                        }
                    }
                }
            }

            // 5. 💾 数据备份与恢复
            if (selectedCategory == 0 || selectedCategory == 5) {
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("💾 完整数据备份与恢复", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { exportFileLauncher.launch("ai-tts-backup-${System.currentTimeMillis()}.json") },
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("导出文件", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { importFileLauncher.launch(arrayOf("application/json", "text/*")) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.TextPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(44.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("恢复文件", fontSize = 12.sp)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val jsonStr = configDataStore.exportAllConfigJson(desensitize = false)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS-Backup", jsonStr))
                                        Toast.makeText(context, "已复制配置到剪贴板！", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("复制文本", fontSize = 11.5.sp)
                                }

                                OutlinedButton(
                                    onClick = { showImportDialog = true },
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("粘贴导入", fontSize = 11.5.sp)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        configDataStore.saveProviders(PresetConfigs.createDefaultProviders())
                                        Toast.makeText(context, "已恢复内置默认模型", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("出厂模型", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        configDataStore.saveRules(PresetConfigs.defaultRules)
                                        Toast.makeText(context, "已恢复内置默认规则", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("出厂规则", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("粘贴配置文本恢复") },
                text = {
                    OutlinedTextField(
                        value = importDialogText,
                        onValueChange = { importDialogText = it },
                        placeholder = { Text("在此粘贴导出的完整 JSON 配置文本...") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 8
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (configDataStore.importConfigJson(importDialogText.trim())) {
                                showImportDialog = false
                                cacheSizeText = getCacheSizeString()
                                Toast.makeText(context, "配置恢复成功！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "配置格式错误，请核对 JSON", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black)
                    ) {
                        Text("立即恢复")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
