package com.aitts.engine.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.aitts.engine.audio.AudioEnhancer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.AppPaletteTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(configDataStore: ConfigDataStore) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val cacheManager = AudioCacheManager.getInstance(context)

    var cacheStats by remember { mutableStateOf(cacheManager.getStats()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var fallbackExpanded by remember { mutableStateOf(false) }

    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write(pendingExportJson ?: "")
                    }
                }
                Toast.makeText(context, "已成功保存配置备份文件！", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "写入备份文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val content = context.contentResolver.openInputStream(it)?.use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
                if (!content.isNullOrBlank()) {
                    val success = configDataStore.importConfigJson(content)
                    if (success) {
                        Toast.makeText(context, "从备份文件恢复配置成功！", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "备份文件格式无效，恢复失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取备份文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 主题色系与外观切换
        item(contentType = "theme_section") {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "外观主题与设计色系",
                subtitle = "内置 5 套高品质专业色系，彻底告别单调色彩"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("界面深浅模式", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("SYSTEM" to "跟随系统", "DARK" to "纯粹暗黑", "LIGHT" to "清爽亮色").forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.appThemeMode == mode,
                                onClick = {
                                    configDataStore.updateSettings(settings.copy(appThemeMode = mode))
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("A 屏纯黑极夜模式 (AMOLED Black)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(
                                    "开启后深色模式下所有配色统一强制 #000000 纯黑背景与底栏，关闭发光像素极致省电",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.isAmoledPureBlack,
                                onCheckedChange = {
                                    configDataStore.updateSettings(settings.copy(isAmoledPureBlack = it))
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("预设设计色调 (10+ 套配色方案)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppPaletteTheme.entries.forEach { palette ->
                            val isSelected = settings.appThemePalette == palette.key
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(if (isSelected) palette.primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(appThemePalette = palette.key))
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(palette.primaryColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = palette.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = palette.primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("界面设计与交互布局风格", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "BENTO" to "🚀 Bento 全息声球工作台",
                            "STUDIO" to "🎛️ DAW 专业调音台",
                            "CLASSIC" to "📋 经典紧凑列表"
                        ).forEach { (style, label) ->
                            FilterChip(
                                selected = settings.appUiStyle == style,
                                onClick = {
                                    configDataStore.updateSettings(settings.copy(appUiStyle = style))
                                },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // 小说分句与自然停顿调度
        item(contentType = "novel_section") {
            SectionHeader(
                title = "小说朗读分句与自然停顿",
                subtitle = "标点智能拆分、首句即播与句间自然呼吸停顿"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用智能标点分句", fontWeight = FontWeight.SemiBold)
                            Text(
                                "长段落按标点拆分并发预拉取，彻底告别卡顿",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isSentenceSplittingEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(isSentenceSplittingEnabled = it))
                            }
                        )
                    }

                    if (settings.isSentenceSplittingEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "单句最大字数阈值: ${settings.maxSentenceLength} 字",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.maxSentenceLength.toFloat(),
                            onValueChange = {
                                configDataStore.updateSettings(settings.copy(maxSentenceLength = it.toInt()))
                            },
                            valueRange = 30f..150f,
                            steps = 12
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "句间自然呼吸停顿: ${settings.sentencePauseMs} ms",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "在每句话播完后自动注入微量静音，模拟真人呼吸节奏（设为0则紧凑快读）。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.sentencePauseMs.toFloat(),
                            onValueChange = {
                                configDataStore.updateSettings(settings.copy(sentencePauseMs = it.toInt()))
                            },
                            valueRange = 0f..600f,
                            steps = 12
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("极速首字秒开模式 (Sub-150ms)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "长段落首句微切分秒级发音，后台并行流水线预取后续句子",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.ultraLowLatencyMode,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(ultraLowLatencyMode = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("小说数字与章节发音优化", fontWeight = FontWeight.SemiBold)
                            Text(
                                "将“第123章”转为“第一百二十三章”，“2026年”转为“二零二六年”",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isNumberNormalizationEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(isNumberNormalizationEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("英文缩写与专有名词发音规整", fontWeight = FontWeight.SemiBold)
                            Text(
                                "自动规范识别 AI、WiFi、CPU、NPC、BOSS、3D、4K 等缩写，字正腔圆朗读",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isAcronymNormalizationEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(isAcronymNormalizationEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("小说对白智能情感语气注入 (Emotion Prosody)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "识别引述语中的情感状态（咆哮/抽泣/惊恐/温婉/耳语），动态指导大模型发音语气",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isEmotionProsodyEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(isEmotionProsodyEnabled = it))
                            }
                        )
                    }
                }
            }
        }

        // 人声清晰度与响度动态增强
        item(contentType = "audio_enhancer_section") {
            SectionHeader(
                title = "专业声学 EQ 与人声增强 (Audio EQ)",
                subtitle = "软件级 16-bit PCM 预加重滤波与防爆音动态范围压缩"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("声学 EQ 音效预设矩阵", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AudioEnhancer.EqPreset.values().forEach { preset ->
                            FilterChip(
                                selected = settings.eqPresetId == preset.name,
                                onClick = {
                                    configDataStore.updateSettings(
                                        settings.copy(
                                            eqPresetId = preset.name,
                                            voiceClarityBoostEnabled = preset.enableClarity,
                                            loudnessGainFactor = preset.gainFactor
                                        )
                                    )
                                },
                                label = { Text(preset.displayName, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用人声清晰度增强 (Voice Clarity Boost)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "高通预加重滤波，削弱手机扬声器低频发闷，大幅提升齿音和人声通透度",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.voiceClarityBoostEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(voiceClarityBoostEnabled = it, eqPresetId = AudioEnhancer.EqPreset.CUSTOM.name))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "全局响度增益: ${String.format(java.util.Locale.US, "%.1f", settings.loudnessGainFactor)}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "内置 Soft-clipping 软饱和压缩算法，提升弱音音量且极大音量时不破音。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = settings.loudnessGainFactor,
                        onValueChange = {
                            configDataStore.updateSettings(settings.copy(loudnessGainFactor = it, eqPresetId = AudioEnhancer.EqPreset.CUSTOM.name))
                        },
                        valueRange = 0.8f..2.2f,
                        steps = 14
                    )
                }
            }
        }

        // 智能故障自动降级 (Auto-Failover)
        item(contentType = "failover_section") {
            SectionHeader(
                title = "引擎高可用与故障自动转移",
                subtitle = "当主力大模型欠费或网络异常时，自动无缝降级备用引擎，听书永不断流"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("大模型微秒级自愈重试 (Jittered Retry)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "遇偶发网络抖动时在毫秒级微延迟自动重试，失败后再降级备用引擎",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoRetryOnFailure,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(autoRetryOnFailure = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用故障自动降级 (Auto-Failover)", fontWeight = FontWeight.SemiBold)
                            Text(
                                "当主力大模型请求报错或超时，自动切换备用引擎继续朗读",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoFallbackOnFailure,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = it))
                            }
                        )
                    }

                    if (settings.autoFallbackOnFailure) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val fallbackProvider = providers.find { it.id == settings.fallbackProviderId }
                        ExposedDropdownMenuBox(
                            expanded = fallbackExpanded,
                            onExpandedChange = { fallbackExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = fallbackProvider?.name ?: "默认: 微软 Edge TTS (免Key)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("指定的备用应急引擎") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fallbackExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = fallbackExpanded,
                                onDismissRequest = { fallbackExpanded = false }
                            ) {
                                providers.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("${p.name} (${p.type.displayName})") },
                                        onClick = {
                                            configDataStore.updateSettings(settings.copy(fallbackProviderId = p.id))
                                            fallbackExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 全局网络代理卡片
        item(contentType = "proxy_section") {
            SectionHeader(
                title = "全局网络代理与连接调优",
                subtitle = "Google Gemini 等境外服务可在此直接配置代理"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用全局 HTTP / SOCKS5 代理", fontWeight = FontWeight.SemiBold)
                            Text(
                                "支持绕过网络限制直连 Google 等官方服务",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.proxyEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(proxyEnabled = it))
                            }
                        )
                    }

                    if (settings.proxyEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = settings.proxyHost,
                                onValueChange = { configDataStore.updateSettings(settings.copy(proxyHost = it)) },
                                label = { Text("代理 IP / 域名") },
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = settings.proxyPort.toString(),
                                onValueChange = {
                                    val p = it.toIntOrNull() ?: settings.proxyPort
                                    configDataStore.updateSettings(settings.copy(proxyPort = p))
                                },
                                label = { Text("端口") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("代理协议: ", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            listOf("HTTP", "SOCKS").forEach { type ->
                                FilterChip(
                                    selected = settings.proxyType == type,
                                    onClick = { configDataStore.updateSettings(settings.copy(proxyType = type)) },
                                    label = { Text(type) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        }

        // 本地音频缓存卡片
        item(contentType = "cache_section") {
            SectionHeader(
                title = "本地音频智能缓存",
                subtitle = "相同文本第二次直接从本地毫秒级秒播，无需重复消耗 API 配额"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用本地音频缓存", fontWeight = FontWeight.SemiBold)
                            Text(
                                "当前缓存已占用: %.2f MB (%d 个文件)".format(cacheStats.second, cacheStats.first),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.isAudioCacheEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(isAudioCacheEnabled = it))
                            }
                        )
                    }

                    if (settings.isAudioCacheEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("最大缓存上限: ${settings.maxCacheSizeMb} MB", style = MaterialTheme.typography.bodyMedium)
                        Slider(
                            value = settings.maxCacheSizeMb.toFloat(),
                            onValueChange = {
                                configDataStore.updateSettings(settings.copy(maxCacheSizeMb = it.toInt()))
                            },
                            valueRange = 100f..2000f,
                            steps = 19
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            cacheManager.clearAll()
                            cacheStats = cacheManager.getStats()
                            Toast.makeText(context, "缓存已清空", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("一键清理所有已缓存音频")
                    }
                }
            }
        }

        // 触觉震动与系统交互
        item(contentType = "haptic_section") {
            SectionHeader(
                title = "系统交互与触觉反馈",
                subtitle = "拖拽排序、按键与模式切换震动控制"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用触觉微震动反馈", fontWeight = FontWeight.SemiBold)
                            Text(
                                "长按拖拽排序与快捷调整时提供细腻物理手感",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.hapticFeedbackEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(hapticFeedbackEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("后台朗读通知栏状态与停止控制", fontWeight = FontWeight.SemiBold)
                            Text(
                                "在通知栏与锁屏实时同步当前正在朗读的句子内容，并提供快捷停止按键",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.playbackNotificationEnabled,
                            onCheckedChange = {
                                configDataStore.updateSettings(settings.copy(playbackNotificationEnabled = it))
                            }
                        )
                    }
                }
            }
        }

        // 配置备份与迁移
        item(contentType = "backup_section") {
            SectionHeader(
                title = "全量配置备份与迁移",
                subtitle = "一键导出全部引擎 API Key、自定义规则与全局参数"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("文件级安全备份 (推荐，无容量上限):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                pendingExportJson = configDataStore.exportAllConfigJson(desensitize = false)
                                createFileLauncher.launch("AI_TTS_Backup_${System.currentTimeMillis()}.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出为文件", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                openFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("从文件恢复", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("剪贴板快捷分享:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val json = configDataStore.exportAllConfigJson(desensitize = false)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI_TTS_Config", json))
                                Toast.makeText(context, "完整配置已复制到剪贴板！", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制含Key", fontSize = 11.5.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val json = configDataStore.exportAllConfigJson(desensitize = true)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI_TTS_Config_Safe", json))
                                Toast.makeText(context, "脱敏配置已复制，可安全分享！", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("复制脱敏", fontSize = 11.5.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("手动粘贴 JSON 文本导入", fontSize = 12.sp)
                    }
                }
            }
        }

        // 系统设置直达卡片
        item(contentType = "system_section") {
            SectionHeader(
                title = "系统文字转语音 (TTS) 快捷直达",
                subtitle = "前往安卓系统设置验证或切换默认 TTS"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            activity?.let { PermissionManager.openSystemTtsSettings(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("打开系统「文字转语音」设置")
                    }
                }
            }
        }

        // 项目开源与开发者信息
        item(contentType = "about_section") {
            SectionHeader(
                title = "项目开源与开发者",
                subtitle = "GitHub 源码仓库与开源许可证"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                        Column {
                            Text(
                                "AI TTS Android Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "v${com.aitts.engine.BuildConfig.VERSION_NAME} (Build ${com.aitts.engine.BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                "Apache 2.0",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👨‍💻 开发者", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Antigravity (DeepMind) & misaka02",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🐙 GitHub 仓库", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "misaka02/ai-tts-android",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com/misaka02/ai-tts-android")
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/misaka02/ai-tts-android")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("访问 GitHub 仓库主页 / 获取更新", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入配置") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = { Text("粘贴备份 JSON 内容") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (configDataStore.importConfigJson(importJsonText)) {
                            Toast.makeText(context, "配置导入成功", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "导入失败，格式错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("确认导入")
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
