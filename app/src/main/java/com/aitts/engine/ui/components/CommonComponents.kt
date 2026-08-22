package com.aitts.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.ui.theme.BrandTheme
import com.aitts.engine.ui.theme.PrimaryIndigo
import com.aitts.engine.ui.theme.SuccessGreen
import com.aitts.engine.ui.theme.WarningOrange

private val CardCornerShape = RoundedCornerShape(14.dp)
private val TagCornerShape = RoundedCornerShape(4.dp)

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionCard(
    permissionState: PermissionManager.PermissionState,
    onRequestAll: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onRequestAllFiles: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (permissionState.isAllGranted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            }
        ),
        shape = CardCornerShape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (permissionState.isAllGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (permissionState.isAllGranted) SuccessGreen else WarningOrange,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (permissionState.isAllGranted) "系统常驻与存储权限健全" else "需要完善运行与常驻权限",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "包含文件存储（本地缓存与规则导入）及电池白名单（防止后台听书被杀）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!permissionState.isAllGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onRequestAll,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("一键申请权限", fontSize = 12.sp)
                    }
                    if (!permissionState.isIgnoringBatteryOptimizations) {
                        OutlinedButton(
                            onClick = onRequestIgnoreBattery,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("电池白名单", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SystemTtsGuideCard(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = CardCornerShape
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryIndigo.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "设为系统默认 TTS 引擎",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "在系统设置中选择本引擎，第三方小说/读屏 App 即刻享受真人级发音。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
            ) {
                Text("去设置", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ProviderCard(
    modifier: Modifier = Modifier,
    provider: TtsProviderConfig,
    isActive: Boolean,
    latencyMs: Long? = null,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onTest: () -> Unit,
    onPinTop: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val brandColor = remember(provider.type) { BrandTheme.getColorForType(provider.type) }
    val haptic = LocalHapticFeedback.current
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(CardCornerShape)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = CardCornerShape
            )
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = CardCornerShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧品牌专属色条
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(72.dp)
                    .background(brandColor)
            )

            Row(
                modifier = Modifier
                    .padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 直接可长按拖拽排序的手柄 (带手势识别与震动触觉反馈)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(provider.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragAccumulatedY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccumulatedY += dragAmount.y
                                    if (dragAccumulatedY < -40f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onMoveUp()
                                        dragAccumulatedY = 0f
                                    } else if (dragAccumulatedY > 40f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onMoveDown()
                                        dragAccumulatedY = 0f
                                    }
                                },
                                onDragEnd = { dragAccumulatedY = 0f },
                                onDragCancel = { dragAccumulatedY = 0f }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "长按上下拖动排序",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                RadioButton(
                    selected = isActive,
                    onClick = onSelect,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 品牌厂商小标签
                        Surface(
                            color = brandColor.copy(alpha = 0.12f),
                            shape = TagCornerShape
                        ) {
                            Text(
                                text = provider.type.displayName,
                                color = brandColor,
                                fontSize = 9.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!provider.type.requiresApiKey) {
                            Surface(
                                color = SuccessGreen.copy(alpha = 0.15f),
                                shape = TagCornerShape
                            ) {
                                Text(
                                    text = "免Key",
                                    color = SuccessGreen,
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (provider.isDualRoleEnabled) {
                            Surface(
                                color = PrimaryIndigo.copy(alpha = 0.15f),
                                shape = TagCornerShape
                            ) {
                                Text(
                                    text = "双音色",
                                    color = PrimaryIndigo,
                                    fontSize = 9.5.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (latencyMs != null) {
                            val latencyColor = when {
                                latencyMs < 300 -> SuccessGreen
                                latencyMs < 800 -> WarningOrange
                                else -> MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = "${latencyMs}ms",
                                color = latencyColor,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "音色: ${provider.voiceId.ifBlank { "默认" }} · 语速 ${provider.speed}x",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                // 快捷单键上下微调 (不用进菜单即可秒速上下移动)
                IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "向上移动",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "向下移动",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onTest, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "试听",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑配置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多排序操作",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("🔝 置顶此引擎") },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onPinTop()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("📋 复制配置副本") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ 删除配置", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
