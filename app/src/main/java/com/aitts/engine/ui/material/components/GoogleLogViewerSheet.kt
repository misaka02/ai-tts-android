package com.aitts.engine.ui.material.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.AppLogEntry
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.LogLevel
import com.aitts.engine.ui.material.GoogleColors

private data class RequestLogSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val summaryTitle: String,
    val subtitle: String?,
    val isComplete: Boolean,
    val isError: Boolean,
    val entries: List<AppLogEntry>
)

/**
 * 📋 Google 官方 Material 3 风格请求诊断日志抽屉
 * 1. 支持按单次请求会话生命周期聚合（展示首包 TTFB、耗时、数据量、文本摘要）；
 * 2. 支持单次请求独立复制，方便精准排查问题；
 * 3. 支持「按请求分组」与「原始流水」双模式无缝切换；
 * 4. 清空日志具备防误触二次确认拦截。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleLogViewerSheet(
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val structuredLogs by configDataStore.structuredLogsFlow.collectAsState()
    val rawLogs by configDataStore.logsFlow.collectAsState()

    var logViewMode by remember { mutableIntStateOf(0) } // 0: 按会话分组, 1: 原始流水
    var expandedSessionIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val requestSessions = remember(structuredLogs) {
        val sessions = mutableListOf<RequestLogSession>()
        var currentSessionEntries = mutableListOf<AppLogEntry>()
        var currentSessionId: String? = null

        fun finalizeSession() {
            if (currentSessionEntries.isEmpty()) return
            val first = currentSessionEntries.first()
            val last = currentSessionEntries.last()
            val sId = currentSessionId ?: first.sessionId ?: ("req-" + first.timestamp.take(5).replace(":", ""))

            val hasErr = currentSessionEntries.any {
                it.level == LogLevel.ERROR || it.title.contains("失败") || it.title.contains("异常")
            }
            val hasComplete = currentSessionEntries.any {
                it.title.contains("播音结束") || it.title.contains("播放完成") || it.title.contains("全部完成")
            }

            var summaryTitle = first.title
            if (summaryTitle.startsWith("🚀 发起语音合成") || summaryTitle.contains("发起合成")) {
                val snippet = summaryTitle.substringAfter("文本=“", "").substringBefore("”", "")
                if (snippet.isNotBlank()) {
                    summaryTitle = "试听合成 · “${snippet.take(22)}...”"
                }
            } else if (summaryTitle.contains("收到朗读请求") || summaryTitle.contains("阅读器调用")) {
                summaryTitle = "系统朗读 · " + (first.details ?: first.title)
            }

            val ttfbEntry = currentSessionEntries.find { it.title.contains("TTFB") || it.details?.contains("TTFB") == true || it.title.contains("首包") }
            val bytesEntry = currentSessionEntries.find { it.title.contains("字节") || it.details?.contains("字节") == true }

            val metricParts = mutableListOf<String>()
            if (ttfbEntry != null) {
                val m = Regex("""(?:TTFB[=:]?\s*|首包[耗时到达]*[:\s]*)(\d+)ms""").find(ttfbEntry.title + " " + (ttfbEntry.details ?: ""))
                if (m != null) metricParts.add("首包 ${m.groupValues[1]}ms")
            }
            if (bytesEntry != null) {
                val m = Regex("""(\d+)\s*字节""").find(bytesEntry.title + " " + (bytesEntry.details ?: ""))
                if (m != null) {
                    val kb = m.groupValues[1].toIntOrNull()?.let { it / 1024 } ?: 0
                    metricParts.add("${kb} KB")
                }
            }
            val subText = if (metricParts.isNotEmpty()) metricParts.joinToString(" · ") else (currentSessionEntries.find { !it.details.isNullOrBlank() }?.details)

            sessions.add(
                RequestLogSession(
                    sessionId = sId,
                    startTime = first.timestamp,
                    endTime = last.timestamp,
                    summaryTitle = summaryTitle,
                    subtitle = subText,
                    isComplete = hasComplete,
                    isError = hasErr,
                    entries = currentSessionEntries.toList()
                )
            )
            currentSessionEntries = mutableListOf()
            currentSessionId = null
        }

        for (entry in structuredLogs) {
            val isStart = entry.title.contains("收到朗读请求") ||
                    entry.title.contains("发起语音合成") ||
                    entry.title.contains("发起合成") ||
                    entry.title.contains("阅读器调用")
            val isEnd = entry.title.contains("播音结束") ||
                    entry.title.contains("播放完成") ||
                    entry.title.contains("全部完成") ||
                    entry.title.contains("合成失败") ||
                    entry.title.contains("异常中止") ||
                    entry.title.contains("已取消")

            val entrySessionId = entry.sessionId

            if (isStart && currentSessionEntries.isNotEmpty()) {
                finalizeSession()
            } else if (entrySessionId != null && currentSessionId != null && entrySessionId != currentSessionId) {
                finalizeSession()
            }

            if (entrySessionId != null) {
                currentSessionId = entrySessionId
            }
            currentSessionEntries.add(entry)

            if (isEnd) {
                finalizeSession()
            }
        }

        if (currentSessionEntries.isNotEmpty()) {
            finalizeSession()
        }

        sessions.reversed()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 顶栏：标题 + 模式切换 + 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("请求诊断日志", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(
                        text = if (logViewMode == 0) "共 ${requestSessions.size} 批请求会话" else "共 ${rawLogs.size} 条原始流水",
                        fontSize = 11.5.sp,
                        color = colors.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // 模式切换胶囊
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceContainerHigh
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (logViewMode == 0) colors.primary else Color.Transparent,
                                modifier = Modifier.clickable { logViewMode = 0 }
                            ) {
                                Text(
                                    text = "按请求分组",
                                    fontSize = 11.sp,
                                    fontWeight = if (logViewMode == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (logViewMode == 0) colors.onPrimary else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (logViewMode == 1) colors.primary else Color.Transparent,
                                modifier = Modifier.clickable { logViewMode = 1 }
                            ) {
                                Text(
                                    text = "原始流水",
                                    fontSize = 11.sp,
                                    fontWeight = if (logViewMode == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (logViewMode == 1) colors.onPrimary else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 复制全部
                    IconButton(
                        onClick = {
                            val allLogs = rawLogs.joinToString("\n")
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("aitts_all_logs", allLogs))
                            Toast.makeText(context, "已复制全部日志", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制全部", tint = colors.primary, modifier = Modifier.size(17.dp))
                    }

                    // 清空 (二次确认弹窗)
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "清空日志", tint = colors.googleRed, modifier = Modifier.size(17.dp))
                    }
                }
            }

            HorizontalDivider(color = colors.outlineSubtle, thickness = 0.8.dp)

            // 日志主视图
            if (logViewMode == 0) {
                // 模式 0: 按请求会话分组卡片
                if (requestSessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Text("暂无请求记录，发起试听或阅读朗读后将在此展示", color = colors.textTertiary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(requestSessions, key = { it.sessionId + it.startTime }) { session ->
                            val isExpanded = expandedSessionIds.contains(session.sessionId)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = colors.surfaceContainer,
                                border = BorderStroke(1.dp, if (session.isError) colors.googleRed.copy(alpha = 0.4f) else colors.outlineSubtle)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).clickable {
                                                expandedSessionIds = if (isExpanded) {
                                                    expandedSessionIds - session.sessionId
                                                } else {
                                                    expandedSessionIds + session.sessionId
                                                }
                                            },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    session.isError -> Icons.Default.Error
                                                    session.isComplete -> Icons.Default.CheckCircle
                                                    else -> Icons.Default.HourglassTop
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    session.isError -> colors.googleRed
                                                    session.isComplete -> colors.googleGreen
                                                    else -> colors.googleYellow
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = session.summaryTitle,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${session.startTime} · ${session.subtitle ?: "处理中..."}",
                                                    fontSize = 11.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // 独立复制本次请求
                                            IconButton(
                                                onClick = {
                                                    val sessionLog = session.entries.joinToString("\n") { e ->
                                                        "[${e.timestamp}] [${e.level}] [${e.tag}] ${e.title}${if (!e.details.isNullOrBlank()) " | ${e.details}" else ""}"
                                                    }
                                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    cm.setPrimaryClip(ClipData.newPlainText("session_log_${session.sessionId}", sessionLog))
                                                    Toast.makeText(context, "已复制本次完整请求日志 (#${session.sessionId})", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "复制该次请求", tint = colors.primary, modifier = Modifier.size(15.dp))
                                            }

                                            // 展开/折叠箭头
                                            IconButton(
                                                onClick = {
                                                    expandedSessionIds = if (isExpanded) {
                                                        expandedSessionIds - session.sessionId
                                                    } else {
                                                        expandedSessionIds + session.sessionId
                                                    }
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 展开详情流水
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp)
                                                .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            for (entry in session.entries) {
                                                val isErr = entry.level == LogLevel.ERROR || entry.title.contains("失败")
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Text(entry.timestamp, fontSize = 10.sp, color = colors.textTertiary, fontFamily = FontFamily.Monospace)
                                                        Surface(
                                                            shape = RoundedCornerShape(3.dp),
                                                            color = if (isErr) colors.googleRed.copy(alpha = 0.2f) else colors.primary.copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = entry.tag,
                                                                fontSize = 9.sp,
                                                                color = if (isErr) colors.googleRed else colors.primary,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = entry.title,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = if (isErr) colors.googleRed else colors.textPrimary,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    if (!entry.details.isNullOrBlank()) {
                                                        Text(
                                                            text = entry.details!!,
                                                            fontSize = 10.sp,
                                                            color = colors.textSecondary,
                                                            fontFamily = FontFamily.Monospace,
                                                            modifier = Modifier.padding(start = 50.dp, top = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 模式 1: 原始日志流水
                if (rawLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        Text("暂无日志记录", color = colors.textTertiary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(rawLogs.takeLast(150).reversed()) { log ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.surfaceContainer
                            ) {
                                Text(
                                    text = log,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.contains("ERROR", ignoreCase = true) || log.contains("失败", ignoreCase = true)) colors.googleRed else colors.textPrimary,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 清空日志防误触二次确认弹窗
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("确认清空所有诊断日志？", fontWeight = FontWeight.Bold, color = colors.textPrimary) },
            text = {
                Text(
                    text = "清空后所有通信历史与请求会话记录将被彻底删除，无法找回。确定要清空吗？",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        configDataStore.clearLogs()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "已清空所有诊断日志", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.googleRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确定清空", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消", color = colors.textSecondary)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp)
        )
    }
}
