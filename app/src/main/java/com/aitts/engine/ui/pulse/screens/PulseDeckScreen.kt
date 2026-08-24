package com.aitts.engine.ui.pulse.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ⚡ Pulse 模型矩阵全景管理舱 (Pulse Deck Screen)
 * 1. 完整陈列全部 Provider，支持一键切换活跃主力；
 * 2. 基于 ID 寻址的长按拖拽排序与原子落盘，杜绝跳顶与错位；
 * 3. 毫秒级网络延迟并发测速与快速置顶/复制/删除；
 * 4. 底部右下角新增模型悬浮 FAB。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseDeckScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onAddNewProvider: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()

    var localProviders by remember(providers) { mutableStateOf(providers) }
    val currentProvidersState by rememberUpdatedState(localProviders)

    val latencyMap = remember { mutableStateMapOf<String, Long>() }
    val testingMap = remember { mutableStateMapOf<String, Boolean>() }

    var showAddPresetDialog by remember { mutableStateOf(false) }
    var showImportTokenDialog by remember { mutableStateOf(false) }
    var importTokenInput by remember { mutableStateOf("") }

    val density = LocalDensity.current
    val itemHeightPx = with(density) { 92.dp.toPx() }
    var draggedProviderId by remember { mutableStateOf<String?>(null) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }

    fun testLatency(provider: TtsProviderConfig) {
        testingMap[provider.id] = true
        scope.launch {
            try {
                val start = System.currentTimeMillis()
                val result = TtsProviderManager.getInstance().synthesize("测试", provider, autoRetry = false)
                val cost = System.currentTimeMillis() - start
                testingMap[provider.id] = false
                if (result.isSuccess && (result.getOrNull()?.isNotEmpty() == true)) {
                    latencyMap[provider.id] = cost
                } else {
                    latencyMap[provider.id] = -1L
                }
            } catch (e: Exception) {
                testingMap[provider.id] = false
                latencyMap[provider.id] = -1L
            }
        }
    }

    fun testAllLatencies() {
        localProviders.forEach { testLatency(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "模型矩阵全景舱",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = PulseTokens.TextPrimary
                        )
                        Text(
                            text = "共 ${providers.size} 个大模型语音引擎 · 长按自由排序",
                            fontSize = 11.sp,
                            color = PulseTokens.CyanElectric,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showImportTokenDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                            border = PulseTokens.BorderSubtle,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导入口令", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = { testAllLatencies() },
                            colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                            border = PulseTokens.BorderSubtle,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("一键测速", fontSize = 11.5.sp)
                        }
                    }
                }
            }

            itemsIndexed(localProviders, key = { _, item -> item.id }) { index, provider ->
                val isSelected = provider.id == settings.activeProviderId
                val isBeingDragged = draggedProviderId == provider.id
                val latency = latencyMap[provider.id]
                val isTesting = testingMap[provider.id] == true

                val itemModifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isBeingDragged) 10f else 1f)
                    .offset {
                        if (isBeingDragged) IntOffset(0, dragDeltaY.roundToInt()) else IntOffset.Zero
                    }
                    .scale(if (isBeingDragged) 1.03f else 1f)

                PulseCard(
                    modifier = itemModifier,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = when {
                        isBeingDragged -> PulseTokens.SurfaceElevated
                        isSelected -> PulseTokens.SurfaceCardActive
                        else -> PulseTokens.SurfaceCard
                    },
                    border = when {
                        isBeingDragged -> BorderStroke(2.dp, PulseTokens.CyanElectric)
                        isSelected -> PulseTokens.BorderActive
                        else -> PulseTokens.BorderSubtle
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
                                        Toast.makeText(context, "已设为主力: ${provider.name}", Toast.LENGTH_SHORT).show()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextTertiary)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = provider.name,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextPrimary
                                        )
                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PulseTokens.CyanElectric.copy(alpha = 0.2f)
                                            ) {
                                                Text("主力", fontSize = 9.sp, color = PulseTokens.CyanElectric, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${provider.type.displayName} · 音色: ${provider.voiceId.ifBlank { "默认" }}",
                                        fontSize = 11.sp,
                                        color = PulseTokens.TextSecondary
                                    )
                                }
                            }

                            // 拖拽手柄
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .pointerInput(provider.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedProviderId = provider.id
                                                dragDeltaY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragDeltaY += dragAmount.y
                                                val threshold = itemHeightPx * 0.65f
                                                val list: List<TtsProviderConfig> = currentProvidersState
                                                val cur = list.indexOfFirst { it.id == provider.id }
                                                if (cur != -1) {
                                                    if (dragDeltaY > threshold && cur < list.size - 1) {
                                                        val targetIdx = cur + 1
                                                        val mutable = list.toMutableList()
                                                        val item = mutable.removeAt(cur)
                                                        mutable.add(targetIdx, item)
                                                        localProviders = mutable
                                                        dragDeltaY -= itemHeightPx
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    } else if (dragDeltaY < -threshold && cur > 0) {
                                                        val targetIdx = cur - 1
                                                        val mutable = list.toMutableList()
                                                        val item = mutable.removeAt(cur)
                                                        mutable.add(targetIdx, item)
                                                        localProviders = mutable
                                                        dragDeltaY += itemHeightPx
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                configDataStore.saveProviders(currentProvidersState)
                                                draggedProviderId = null
                                                dragDeltaY = 0f
                                            },
                                            onDragCancel = {
                                                draggedProviderId = null
                                                dragDeltaY = 0f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "长按拖拽排序",
                                    tint = if (isBeingDragged) PulseTokens.CyanElectric else PulseTokens.TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 状态与快捷操作栏
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { testLatency(provider) }
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PulseTokens.CyanElectric, strokeWidth = 1.5.dp)
                                } else {
                                    val (latText, latColor) = when {
                                        latency == null -> "测速" to PulseTokens.TextTertiary
                                        latency < 0 -> "超时/异常" to PulseTokens.MagentaLaser
                                        latency < 400 -> "${latency}ms" to PulseTokens.AcidGreen
                                        latency < 900 -> "${latency}ms" to PulseTokens.AmberWarm
                                        else -> "${latency}ms" to PulseTokens.MagentaLaser
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = latColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(latText, fontSize = 10.sp, color = latColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (provider.isDualRoleEnabled) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PulseTokens.MagentaLaser.copy(alpha = 0.15f)
                                    ) {
                                        Text("双角色", fontSize = 10.sp, color = PulseTokens.MagentaLaser, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val token = configDataStore.exportProviderToken(provider)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS-Model-Token", token))
                                        Toast.makeText(context, "已复制「${provider.name}」模型口令", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制口令", tint = PulseTokens.TextTertiary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { configDataStore.pinProviderToTop(provider.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = PulseTokens.TextTertiary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { onNavigateToEditProvider(provider.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "配置", tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        configDataStore.deleteProvider(provider.id)
                                        Toast.makeText(context, "已删除模型: ${provider.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = PulseTokens.MagentaLaser, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 右下角大拇指悬浮收纳岛 (模型矩阵专属动作组)
        UniversalActionHub(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            items = listOf(
                ActionHubItem(
                    label = "新增模型预设",
                    icon = Icons.Default.Add,
                    color = PulseTokens.CyanElectric,
                    onClick = { showAddPresetDialog = true }
                ),
                ActionHubItem(
                    label = "全矩阵并发测速",
                    icon = Icons.Default.Speed,
                    color = PulseTokens.SonicBlue,
                    onClick = { testAllLatencies() }
                ),
                ActionHubItem(
                    label = "导入单模型口令",
                    icon = Icons.Default.ContentPaste,
                    color = PulseTokens.AmberWarm,
                    onClick = { showImportTokenDialog = true }
                ),
                ActionHubItem(
                    label = "复制当前主力口令",
                    icon = Icons.Default.ContentCopy,
                    color = PulseTokens.MagentaLaser,
                    onClick = {
                        val active = localProviders.find { it.id == settings.activeProviderId } ?: localProviders.firstOrNull()
                        if (active != null) {
                            val token = configDataStore.exportProviderToken(active)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ai_tts_provider", token))
                            Toast.makeText(context, "已复制【${active.name}】口令到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            ),
            icon = Icons.Default.Tune
        )

        if (showAddPresetDialog) {
            AlertDialog(
                onDismissRequest = { showAddPresetDialog = false },
                title = { Text("选择引擎预设并添加", fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ProviderType.values().size) { idx ->
                            val type = ProviderType.values()[idx]
                            PulseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newId = "${type.name.lowercase()}_${System.currentTimeMillis() % 10000}"
                                        val newProvider = TtsProviderConfig(
                                            id = newId,
                                            type = type,
                                            name = type.displayName,
                                            baseUrl = if (type == ProviderType.MIMO) "https://api.xiaomimimo.com/v1/chat/completions" else "",
                                            modelName = if (type == ProviderType.MIMO) "mimo-v2.5-tts" else "",
                                            voiceId = if (type == ProviderType.MIMO) "茉莉" else if (type == ProviderType.EDGE_TTS) "zh-CN-YunxiNeural" else "default"
                                        )
                                        configDataStore.updateProvider(newProvider)
                                        showAddPresetDialog = false
                                        onNavigateToEditProvider(newId)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                backgroundColor = PulseTokens.SurfaceElevated
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(type.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PulseTokens.CyanElectric)
                                    Text(type.description, fontSize = 11.sp, color = PulseTokens.TextSecondary, maxLines = 2)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddPresetDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showImportTokenDialog) {
            AlertDialog(
                onDismissRequest = { showImportTokenDialog = false },
                title = { Text("导入单模型口令", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("支持粘贴 aitts://provider?data=... 或 Base64 格式的模型分享口令：", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                        OutlinedTextField(
                            value = importTokenInput,
                            onValueChange = { importTokenInput = it },
                            placeholder = { Text("在此粘贴模型分享口令...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 6
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val imported = configDataStore.importProviderFromToken(importTokenInput.trim())
                            if (imported != null) {
                                showImportTokenDialog = false
                                Toast.makeText(context, "已成功导入模型: ${imported.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "口令解析失败，请检查格式", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black)
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
    }
}
