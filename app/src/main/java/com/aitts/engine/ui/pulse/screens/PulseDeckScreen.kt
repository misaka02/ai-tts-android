package com.aitts.engine.ui.pulse.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.zIndex
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * ⚡ Pulse 模型矩阵全景管理舱 (Pulse Deck Screen)
 * 1. 完整陈列全部 Provider，支持一键切换活跃主力；
 * 2. 基于 ID 寻址的长按拖拽排序与原子落盘，杜绝跳顶与错位；
 * 3. 毫秒级网络延迟并发测速与快速置顶/复制/删除；
 * 4. 底部右下角新增模型悬浮 FAB。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PulseDeckScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onAddNewProvider: () -> Unit = {},
    parentPagerState: androidx.compose.foundation.pager.PagerState? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()

    var localProviders by remember(providers) { mutableStateOf(providers) }
    var draggedProviderId by remember { mutableStateOf<String?>(null) }
    var dragStartIndex by remember { mutableStateOf(-1) }
    var dragTargetIndex by remember { mutableStateOf(-1) }
    var totalDragOffsetY by remember { mutableFloatStateOf(0f) }
    var draggedItemViewportY by remember { mutableFloatStateOf(-1f) }

    LaunchedEffect(providers) {
        if (draggedProviderId == null) {
            localProviders = providers
        }
    }

    val latencyMap = remember { mutableStateMapOf<String, Long>() }
    val testingMap = remember { mutableStateMapOf<String, Boolean>() }

    var showAddPresetDialog by remember { mutableStateOf(false) }
    var showImportTokenDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf<TtsProviderConfig?>(null) }
    var importTokenInput by remember { mutableStateOf("") }

    var selectedDeckTab by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 96.dp.toPx() }

    fun moveProviderToTop(provider: TtsProviderConfig) {
        val curList = localProviders.toMutableList()
        val curIdx = curList.indexOfFirst { it.id == provider.id }
        if (curIdx > 0) {
            val item = curList.removeAt(curIdx)
            curList.add(0, item)
            localProviders = curList
            configDataStore.saveProviders(curList)
            Toast.makeText(context, "已将【${provider.name}】置顶", Toast.LENGTH_SHORT).show()
        }
    }

    fun moveProviderStep(provider: TtsProviderConfig, step: Int) {
        val curList = localProviders.toMutableList()
        val curIdx = curList.indexOfFirst { it.id == provider.id }
        if (curIdx != -1) {
            val targetIdx = (curIdx + step).coerceIn(0, curList.size - 1)
            if (targetIdx != curIdx) {
                val item = curList.removeAt(curIdx)
                curList.add(targetIdx, item)
                localProviders = curList
                configDataStore.saveProviders(curList)
            }
        }
    }

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

    val lazyListState = rememberLazyListState()

    LaunchedEffect(draggedProviderId) {
        if (draggedProviderId != null) {
            while (true) {
                val layoutInfo = lazyListState.layoutInfo
                val viewportHeight = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).toFloat()
                if (draggedItemViewportY >= 0f && viewportHeight > 200f) {
                    val edgeThreshold = 130f
                    val scrollDelta = when {
                        draggedItemViewportY < edgeThreshold -> {
                            val factor = ((edgeThreshold - draggedItemViewportY) / edgeThreshold).coerceIn(0f, 1f)
                            -(factor * 16f).coerceAtLeast(3f)
                        }
                        draggedItemViewportY > (viewportHeight - edgeThreshold) -> {
                            val factor = ((draggedItemViewportY - (viewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                            (factor * 16f).coerceAtLeast(3f)
                        }
                        else -> 0f
                    }
                    if (scrollDelta != 0f) {
                        val consumed = lazyListState.scrollBy(scrollDelta)
                        if (consumed != 0f) {
                            totalDragOffsetY += consumed
                            val offsetSteps = (totalDragOffsetY / itemHeightPx).roundToInt()
                            val newTarget = (dragStartIndex + offsetSteps).coerceIn(0, localProviders.size - 1)
                            if (newTarget != dragTargetIndex) {
                                dragTargetIndex = newTarget
                            }
                        }
                    }
                }
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
    ) {
        LazyColumn(
            state = lazyListState,
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
                            text = "共 ${providers.size} 个模型引擎 · 长按手柄拖拽排序",
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

            item {
                val deckTabs = listOf("全部模型 (${providers.size})", "⭐ 主力引擎", "☁️ 云端大模型", "⚡ 离线/直连")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(deckTabs) { idx, title ->
                        val isSelected = selectedDeckTab == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDeckTab = idx },
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

            val displayedProviders = when (selectedDeckTab) {
                1 -> localProviders.filter { it.id == settings.activeProviderId }
                2 -> localProviders.filter { it.type.requiresApiKey }
                3 -> localProviders.filter { !it.type.requiresApiKey }
                else -> localProviders
            }

            items(displayedProviders, key = { it.id }) { provider ->
                val isSelected = provider.id == settings.activeProviderId
                val isBeingDragged = draggedProviderId == provider.id
                val latency = latencyMap[provider.id]
                val isTesting = testingMap[provider.id] == true

                val itemIndex = localProviders.indexOfFirst { it.id == provider.id }

                val brandColor = if (settings.isProviderCardAccentColorEnabled) {
                    when (provider.type) {
                        ProviderType.EDGE_TTS -> Color(0xFF0078D4)
                        ProviderType.AZURE -> Color(0xFF0089D6)
                        ProviderType.MIMO -> Color(0xFFFF6A00)
                        ProviderType.MINIMAX -> Color(0xFF8B5CF6)
                        ProviderType.DOUBAO -> Color(0xFF3B82F6)
                        ProviderType.STEPFUN -> Color(0xFF06B6D4)
                        ProviderType.OPENAI -> Color(0xFF10A37F)
                        ProviderType.SILICONFLOW -> Color(0xFF6366F1)
                        ProviderType.FISH_AUDIO -> Color(0xFFEC4899)
                        ProviderType.GEMINI -> Color(0xFF9333EA)
                        ProviderType.CUSTOM_HTTP -> Color(0xFFF59E0B)
                        ProviderType.OFFLINE_VITS -> Color(0xFF10B981)
                    }
                } else {
                    PulseTokens.CyanElectric
                }

                // 悬浮非侵入式动态位移计算
                val visualShiftY = remember(draggedProviderId, dragStartIndex, dragTargetIndex, itemIndex) {
                    if (draggedProviderId == null || isBeingDragged || dragStartIndex == -1 || dragTargetIndex == -1 || itemIndex == -1) {
                        0f
                    } else if (dragStartIndex < dragTargetIndex && itemIndex in (dragStartIndex + 1)..dragTargetIndex) {
                        -itemHeightPx
                    } else if (dragStartIndex > dragTargetIndex && itemIndex in dragTargetIndex until dragStartIndex) {
                        itemHeightPx
                    } else {
                        0f
                    }
                }
                val animatedShiftY by animateFloatAsState(
                    targetValue = visualShiftY,
                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    label = "item_shift"
                )

                val cardModifier = if (isBeingDragged) {
                    Modifier
                        .fillMaxWidth()
                        .zIndex(99f)
                        .graphicsLayer {
                            translationY = totalDragOffsetY
                            scaleX = 1.035f
                            scaleY = 1.035f
                            shadowElevation = 24f
                        }
                } else {
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f)
                        .graphicsLayer {
                            translationY = animatedShiftY
                        }
                }

                val surfaceElevatedColor = PulseTokens.SurfaceElevated

                PulseCard(
                    modifier = cardModifier,
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = when {
                        isBeingDragged -> surfaceElevatedColor
                        isSelected -> PulseTokens.SurfaceCardActive
                        else -> PulseTokens.SurfaceCard
                    },
                    border = when {
                        isBeingDragged -> BorderStroke(2.dp, brandColor)
                        isSelected -> BorderStroke(1.5.dp, brandColor)
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
                                        .background(color = if (isSelected) brandColor else PulseTokens.TextTertiary)
                                        .then(if (isSelected) Modifier.shadow(4.dp, CircleShape) else Modifier)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = provider.name,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) brandColor else PulseTokens.TextPrimary
                                        )
                                        if (isSelected) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = brandColor.copy(alpha = 0.2f)
                                            ) {
                                                Text("主力", fontSize = 9.sp, color = brandColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold)
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

                            // 专属长按自由悬浮拖拽手柄
                            val providerId = provider.id
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color = if (isBeingDragged) brandColor.copy(alpha = 0.2f) else surfaceElevatedColor)
                                    .pointerInput(providerId) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                val idx = localProviders.indexOfFirst { it.id == providerId }
                                                if (idx != -1) {
                                                    draggedProviderId = providerId
                                                    dragStartIndex = idx
                                                    dragTargetIndex = idx
                                                    totalDragOffsetY = 0f
                                                    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.find { it.index == idx }
                                                    draggedItemViewportY = (itemInfo?.offset?.toFloat() ?: 0f) + itemHeightPx / 2f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                totalDragOffsetY += dragAmount.y
                                                draggedItemViewportY += dragAmount.y
                                                val offsetSteps = (totalDragOffsetY / itemHeightPx).roundToInt()
                                                val newTarget = (dragStartIndex + offsetSteps).coerceIn(0, localProviders.size - 1)
                                                if (newTarget != dragTargetIndex) {
                                                    dragTargetIndex = newTarget
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            },
                                            onDragEnd = {
                                                if (dragStartIndex != -1 && dragTargetIndex != -1 && dragStartIndex != dragTargetIndex) {
                                                    val mutable = localProviders.toMutableList()
                                                    val item = mutable.removeAt(dragStartIndex)
                                                    mutable.add(dragTargetIndex, item)
                                                    localProviders = mutable
                                                    configDataStore.saveProviders(mutable)
                                                    Toast.makeText(context, "已调整排列顺序", Toast.LENGTH_SHORT).show()
                                                }
                                                draggedProviderId = null
                                                dragStartIndex = -1
                                                dragTargetIndex = -1
                                                totalDragOffsetY = 0f
                                                draggedItemViewportY = -1f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragCancel = {
                                                draggedProviderId = null
                                                dragStartIndex = -1
                                                dragTargetIndex = -1
                                                totalDragOffsetY = 0f
                                                draggedItemViewportY = -1f
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "长按拖拽排序",
                                    tint = if (isBeingDragged) brandColor else PulseTokens.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // 状态与快捷操作栏 (含一键置顶、上移、下移快捷键)
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

                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                                // 快捷微调按钮：置顶
                                IconButton(
                                    onClick = { moveProviderToTop(provider) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.PushPin, contentDescription = "一键置顶", tint = PulseTokens.TextTertiary, modifier = Modifier.size(15.dp))
                                }

                                // 快捷微调按钮：上移
                                IconButton(
                                    onClick = { moveProviderStep(provider, -1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "上移", tint = PulseTokens.TextTertiary, modifier = Modifier.size(15.dp))
                                }

                                // 快捷微调按钮：下移
                                IconButton(
                                    onClick = { moveProviderStep(provider, 1) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "下移", tint = PulseTokens.TextTertiary, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val token = configDataStore.exportProviderToken(provider)
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS-Model-Token", token))
                                        Toast.makeText(context, "已复制「${provider.name}」模型口令", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "复制口令", tint = PulseTokens.TextTertiary, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = { onNavigateToEditProvider(provider.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "配置", tint = PulseTokens.CyanElectric, modifier = Modifier.size(15.dp))
                                }

                                IconButton(
                                    onClick = {
                                        providerToDelete = provider
                                        showDeleteConfirmDialog = true
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = PulseTokens.MagentaLaser, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 右下角大拇指悬浮收纳岛 (模型卡片全景动作组 - 仅在当前页面活跃时渲染)
        if (parentPagerState == null || parentPagerState.currentPage == 1) {
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
        }

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

        if (showDeleteConfirmDialog && providerToDelete != null) {
            val target = providerToDelete!!
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirmDialog = false
                    providerToDelete = null
                },
                title = { Text("⚠️ 确认删除模型", fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("确定要删除以下模型配置吗？此操作无法撤销。", fontSize = 13.sp, color = PulseTokens.TextPrimary)
                        Text("• 名称: ${target.name}", fontSize = 12.sp, color = PulseTokens.CyanElectric, fontWeight = FontWeight.Bold)
                        Text("• 厂商: ${target.type.displayName}", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            configDataStore.deleteProvider(target.id)
                            showDeleteConfirmDialog = false
                            providerToDelete = null
                            Toast.makeText(context, "已删除模型: ${target.name}", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.MagentaLaser, contentColor = Color.White)
                    ) {
                        Text("确定删除", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirmDialog = false
                        providerToDelete = null
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
