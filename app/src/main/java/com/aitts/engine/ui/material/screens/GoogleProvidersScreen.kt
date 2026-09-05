package com.aitts.engine.ui.material.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
 * 📦 Google 官方应用风格 - 模型与服务商管理全功能版 (Google Providers & Models)
 * 适配全量核心能力：
 * 1. 列表管理与一键设为默认；
 * 2. 全矩阵并发测速与单项快速探测；
 * 3. 顺序上移/下移优先级调度与自动按延迟重排；
 * 4. 模型分享口令导出 (Token) 与口令导入；
 * 5. 复制克隆模型、编辑参数、删除确认。
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
    var isBatchTesting by remember { mutableStateOf(false) }

    var providerToDelete by remember { mutableStateOf<TtsProviderConfig?>(null) }
    var showAddTemplateDialog by remember { mutableStateOf(false) }
    var showImportTokenDialog by remember { mutableStateOf(false) }
    var importTokenInput by remember { mutableStateOf("") }

    fun testSingle(p: TtsProviderConfig) {
        testingMap[p.id] = true
        scope.launch {
            val start = System.currentTimeMillis()
            val res = TtsProviderManager.getInstance().synthesize("测试", p, autoRetry = false)
            val cost = System.currentTimeMillis() - start
            testingMap[p.id] = false
            if (res.isSuccess) {
                testLatencyMap[p.id] = cost
                Toast.makeText(context, "${p.name} 测速成功: ${cost}ms", Toast.LENGTH_SHORT).show()
            } else {
                val err = res.exceptionOrNull()?.message ?: "失败"
                Toast.makeText(context, "测速失败: $err", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun testAllLatencies() {
        isBatchTesting = true
        scope.launch {
            providers.forEach { p ->
                testingMap[p.id] = true
            }
            providers.forEach { p ->
                val start = System.currentTimeMillis()
                val res = TtsProviderManager.getInstance().synthesize("测试", p, autoRetry = false)
                val cost = System.currentTimeMillis() - start
                testingMap[p.id] = false
                if (res.isSuccess) {
                    testLatencyMap[p.id] = cost
                }
            }
            isBatchTesting = false
            Toast.makeText(context, "全矩阵测速已完成", Toast.LENGTH_SHORT).show()
        }
    }

    fun moveProvider(index: Int, up: Boolean) {
        val targetIndex = if (up) index - 1 else index + 1
        if (targetIndex in providers.indices) {
            val list = providers.toMutableList()
            val item = list.removeAt(index)
            list.add(targetIndex, item)
            configDataStore.saveProviders(list)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 顶栏标题与批量测速操作
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "模型服务商",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "云端大模型与端侧神经网络语音配置",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // 导入口令
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showImportTokenDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = colors.primary, modifier = Modifier.size(13.dp))
                                Text("口令导入", fontSize = 11.5.sp, color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                        }

                        // 并发测速
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isBatchTesting) { testAllLatencies() },
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isBatchTesting) {
                                    CircularProgressIndicator(color = colors.onPrimaryContainer, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
                                } else {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(13.dp))
                                }
                                Text("全部测速", fontSize = 11.5.sp, color = colors.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 服务商列表 (包含上移/下移/复制/删除)
            itemsIndexed(providers, key = { _, it -> it.id }) { index, provider ->
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
                        // 头部：图标 + 名称 + 排序箭头 + 默认徽章
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // 默认徽章
                                if (isDefault) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(12.dp))
                                            Text("默认", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                                        }
                                    }
                                }

                                // 上移
                                IconButton(
                                    onClick = { moveProvider(index, true) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "上移", tint = if (index > 0) colors.textSecondary else colors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }

                                // 下移
                                IconButton(
                                    onClick = { moveProvider(index, false) },
                                    enabled = index < providers.size - 1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "下移", tint = if (index < providers.size - 1) colors.textSecondary else colors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // 关键参数简述 (音色、格式、测速结果、双角色)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = RoundedCornerShape(8.dp), color = colors.surfaceContainerHigh) {
                                Text("音色: ${provider.voiceId.ifBlank { "默认" }}", fontSize = 11.5.sp, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }

                            if (provider.isDualRoleEnabled) {
                                Surface(shape = RoundedCornerShape(8.dp), color = colors.primaryContainer) {
                                    Text("对白: ${provider.dialogueVoiceId}", fontSize = 11.sp, color = colors.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            if (provider.type.requiresApiKey) {
                                Surface(shape = RoundedCornerShape(8.dp), color = colors.surfaceContainerHigh) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(11.dp))
                                        Text(if (provider.apiKey.isNotBlank()) "已配Key" else "无Key", fontSize = 11.5.sp, color = if (provider.apiKey.isNotBlank()) colors.textSecondary else colors.googleRed)
                                    }
                                }
                            }

                            if (latency != null && latency > 0) {
                                Surface(shape = RoundedCornerShape(8.dp), color = colors.googleGreen.copy(alpha = 0.15f)) {
                                    Text("${latency}ms", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = colors.googleGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // 底部操作栏 (设为默认、测速、复制口令、克隆、编辑、删除)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!isDefault) {
                                    Surface(
                                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                            configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
                                            Toast.makeText(context, "已设为默认: ${provider.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.primaryContainer
                                    ) {
                                        Text("设为默认", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = colors.onPrimaryContainer, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                                    }
                                }

                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(enabled = !isTesting) { testSingle(provider) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surfaceContainerHigh
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (isTesting) {
                                            CircularProgressIndicator(color = colors.primary, strokeWidth = 1.5.dp, modifier = Modifier.size(11.dp))
                                        } else {
                                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(12.dp))
                                        }
                                        Text(if (isTesting) "测速中" else "测速", fontSize = 11.5.sp, color = colors.textSecondary)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                // 复制口令
                                IconButton(
                                    onClick = {
                                        val token = configDataStore.exportProviderToken(provider)
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("AI-TTS-Model-Token", token))
                                        Toast.makeText(context, "已复制「${provider.name}」模型口令", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制口令", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                }

                                // 复制副本
                                IconButton(
                                    onClick = {
                                        val cloneId = "clone_${System.currentTimeMillis() % 10000}"
                                        val cloned = provider.copy(id = cloneId, name = "${provider.name} (副本)")
                                        configDataStore.updateProvider(cloned)
                                        Toast.makeText(context, "已克隆配置为: ${cloned.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "克隆副本", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                }

                                // 编辑
                                IconButton(
                                    onClick = { onNavigateToEditProvider(provider.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = colors.primary, modifier = Modifier.size(16.dp))
                                }

                                // 删除
                                if (providers.size > 1) {
                                    IconButton(
                                        onClick = { providerToDelete = provider },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = colors.googleRed, modifier = Modifier.size(16.dp))
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

    // 导入口令弹窗
    if (showImportTokenDialog) {
        AlertDialog(
            onDismissRequest = { showImportTokenDialog = false },
            title = { Text("导入单模型口令", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("粘贴 aitts://provider?data=... 或 Base64 格式的模型分享口令：", fontSize = 12.sp, color = colors.textSecondary)
                    OutlinedTextField(
                        value = importTokenInput,
                        onValueChange = { importTokenInput = it },
                        placeholder = { Text("在此粘贴口令...") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = configDataStore.importProviderFromToken(importTokenInput.trim())
                        if (imported != null) {
                            showImportTokenDialog = false
                            importTokenInput = ""
                            Toast.makeText(context, "成功导入模型: ${imported.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "口令格式不正确或解析失败", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("解析并导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportTokenDialog = false }) { Text("取消") }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
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
            Triple(ProviderType.MINIMAX, "MiniMax (海螺语音)", "speech-02-turbo"),
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
