package com.aitts.engine.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.PrimaryBlue

@Composable
fun SettingsScreen(configDataStore: ConfigDataStore) {
    val context = LocalContext.current
    val activity = context as? Activity
    val settings by configDataStore.settingsFlow.collectAsState()
    val cacheManager = AudioCacheManager.getInstance(context)

    var cacheStats by remember { mutableStateOf(cacheManager.getStats()) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "流式与分句调度",
                subtitle = "优化大段小说阅读时的出声延迟与流式平滑度"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用智能标点分句", fontWeight = FontWeight.SemiBold)
                            Text(
                                "将长段落按。！？拆分，首句立即可播",
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
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
                }
            }
        }

        item {
            SectionHeader(
                title = "音频磁盘缓存管理",
                subtitle = "自动将合成过的音频保存到本地，二次朗读秒开且节省 API 额度"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用 LRU 音频缓存", fontWeight = FontWeight.SemiBold)
                            Text(
                                "已缓存 ${cacheStats.first} 个音频片段 (约 ${"%.2f".format(cacheStats.second)} MB)",
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
                        Text(
                            text = "最大缓存限制: ${settings.maxCacheSizeMb} MB",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = settings.maxCacheSizeMb.toFloat(),
                            onValueChange = {
                                configDataStore.updateSettings(settings.copy(maxCacheSizeMb = it.toInt()))
                            },
                            valueRange = 100f..2000f,
                            steps = 19
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            cacheManager.clearAll()
                            cacheStats = cacheManager.getStats()
                            Toast.makeText(context, "缓存已清空", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text("清空全部音频缓存")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "全局网络代理与连接调优",
                subtitle = "为 Google Gemini、OpenAI、微软等海外或局域网节点配置代理路由"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("启用网络代理", fontWeight = FontWeight.SemiBold)
                            Text(
                                "支持 HTTP / SOCKS5 协议 (如 127.0.0.1:7890)",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = settings.proxyHost,
                                onValueChange = {
                                    configDataStore.updateSettings(settings.copy(proxyHost = it))
                                },
                                label = { Text("代理服务器地址") },
                                modifier = Modifier.weight(2f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = settings.proxyPort.toString(),
                                onValueChange = {
                                    val port = it.toIntOrNull() ?: 7890
                                    configDataStore.updateSettings(settings.copy(proxyPort = port))
                                },
                                label = { Text("端口") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "系统级联动设置",
                subtitle = "TTS 引擎注册状态与系统权限快捷入口"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            activity?.let { PermissionManager.openSystemTtsSettings(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Text("打开系统「文字转语音」设置")
                    }

                    OutlinedButton(
                        onClick = {
                            activity?.let { PermissionManager.requestIgnoreBatteryOptimizations(it) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("打开电池优化白名单设置")
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "配置备份与导入",
                subtitle = "一键导出所有 API Key、模型参数与规则词库"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val jsonStr = configDataStore.exportAllConfigJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("AiTtsBackup", jsonStr))
                            Toast.makeText(context, "配置已复制到剪贴板", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Text("导出备份")
                    }

                    OutlinedButton(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("导入配置")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入配置 JSON") },
            text = {
                Column {
                    Text("请粘贴导出的 JSON 备份数据：", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                }
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
