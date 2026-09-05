package com.aitts.engine.ui.material.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.material.GoogleColors
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 📦 Google 官方应用风格 - 模型与服务商管理 (Google Providers & Models)
 * 1. Material 3 Tonal Card 清爽层级与圆角卡片；
 * 2. 状态标签（默认引擎 / 需 API Key / 离线就绪）；
 * 3. 一键测速（真实毫秒级连通性探测）；
 * 4. Google Extended FAB 快捷添加与模板选择。
 */
@Composable
fun GoogleProvidersScreen(
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    onNavigateToEditProvider: (String) -> Unit,
    onAddNewProvider: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()

    val testLatencyMap = remember { mutableStateMapOf<String, Long>() }
    val testingMap = remember { mutableStateMapOf<String, Boolean>() }
    var providerToDelete by remember { mutableStateOf<TtsProviderConfig?>(null) }
    var showAddTemplateDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏标题区
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "模型服务商",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "配置云端多模态大模型与端侧神经网络语音引擎",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // 服务商列表
            items(providers, key = { it.id }) { provider ->
                val isDefault = provider.id == settings.activeProviderId
                val isTesting = testingMap[provider.id] == true
                val latency = testLatencyMap[provider.id]

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDefault) colors.surface else colors.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isDefault) 1.5.dp else 1.dp,
                        color = if (isDefault) colors.primary else colors.outlineSubtle
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 头部：图标 + 名称 + 类型 + 状态徽章
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (isDefault) colors.primaryContainer else colors.surfaceContainerHigh
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.RecordVoiceOver,
                                            contentDescription = null,
                                            tint = if (isDefault) colors.onPrimaryContainer else colors.textSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = provider.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = provider.type.displayName,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            // 默认徽章
                            if (isDefault) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "默认引擎",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        // 关键参数简述 (音色、格式、测速结果)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceContainerHigh
                            ) {
                                Text(
                                    text = "音色: ${provider.voiceId.ifBlank { "默认" }}",
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (provider.type.requiresApiKey) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = colors.textTertiary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = if (provider.apiKey.isNotBlank()) "Key 已配置" else "未填 Key",
                                            fontSize = 11.5.sp,
                                            color = if (provider.apiKey.isNotBlank()) colors.textSecondary else colors.googleRed
                                        )
                                    }
                                }
                            }

                            if (latency != null && latency > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.googleGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${latency}ms",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.googleGreen,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 底部操作栏 (设为默认、测速、编辑、删除)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isDefault) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
                                                Toast.makeText(context, "已设为默认: ${provider.name}", Toast.LENGTH_SHORT).show()
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.primaryContainer
                                    ) {
                                        Text(
                                            text = "设为默认",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                // 快速测速按钮
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !isTesting) {
                                            testingMap[provider.id] = true
                                            scope.launch {
                                                val start = System.currentTimeMillis()
                                                val res = TtsProviderManager.getInstance().synthesize("测试", provider, autoRetry = false)
                                                val cost = System.currentTimeMillis() - start
                                                testingMap[provider.id] = false
                                                if (res.isSuccess) {
                                                    testLatencyMap[provider.id] = cost
                                                    Toast.makeText(context, "${provider.name} 测速成功: ${cost}ms", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val err = res.exceptionOrNull()?.message ?: "失败"
                                                    Toast.makeText(context, "测速失败: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isTesting) {
                                            CircularProgressIndicator(
                                                color = colors.primary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.FlashOn,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Text(
                                            text = if (isTesting) "测试中" else "测速",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { onNavigateToEditProvider(provider.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = colors.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (providers.size > 1) {
                                    IconButton(
                                        onClick = { providerToDelete = provider },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = colors.googleRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Google Extended FAB 按钮
        ExtendedFloatingActionButton(
            onClick = { showAddTemplateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 20.dp),
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加服务商", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }

    // 删除确认弹窗
    if (providerToDelete != null) {
        val target = providerToDelete!!
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("确认删除服务商", color = colors.textPrimary) },
            text = { Text("确定要删除【${target.name}】吗？删除后配置将无法找回。", color = colors.textSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        configDataStore.deleteProvider(target.id)
                        providerToDelete = null
                        Toast.makeText(context, "已删除服务商", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("删除", color = colors.googleRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) {
                    Text("取消", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // 添加服务商模板选择弹窗
    if (showAddTemplateDialog) {
        val templates = listOf(
            Triple(ProviderType.GEMINI, "Google Gemini 原生 TTS", "gemini-2.5-flash-preview-tts"),
            Triple(ProviderType.EDGE_TTS, "微软 Edge TTS (免费免Key)", "zh-CN-XiaoxiaoNeural"),
            Triple(ProviderType.OPENAI, "OpenAI / 标准中转 TTS", "tts-1"),
            Triple(ProviderType.MIMO, "小米 MiMo 语音大模型", "mimo-v2.5-tts"),
            Triple(ProviderType.MINIMAX, "MiniMax (海螺语音)", "speech-01-turbo"),
            Triple(ProviderType.DOUBAO, "火山引擎 / 豆包语音", "zh_female_shuangkuaisisi_moon_bigtts"),
            Triple(ProviderType.OFFLINE_VITS, "离线神经网络引擎 (Sherpa-ONNX)", "vits-zh-aishell3")
        )

        AlertDialog(
            onDismissRequest = { showAddTemplateDialog = false },
            title = { Text("选择服务商引擎类型", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(templates.size) { idx ->
                        val (type, name, defaultModel) = templates[idx]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newId = "custom_${UUID.randomUUID().toString().take(8)}"
                                    val newConfig = TtsProviderConfig(
                                        id = newId,
                                        type = type,
                                        name = name,
                                        enabled = true,
                                        modelName = defaultModel,
                                        voiceId = if (type == ProviderType.EDGE_TTS) "zh-CN-XiaoxiaoNeural" else ""
                                    )
                                    configDataStore.updateProvider(newConfig)
                                    showAddTemplateDialog = false
                                    onNavigateToEditProvider(newId)
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceContainer
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = colors.textPrimary)
                                Text(type.description, fontSize = 11.5.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddTemplateDialog = false }) {
                    Text("取消", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
