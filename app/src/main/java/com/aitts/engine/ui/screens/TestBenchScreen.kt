package com.aitts.engine.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.SegmentRole
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.SentenceSplitter
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.components.SectionHeader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestBenchScreen(configDataStore: ConfigDataStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by configDataStore.logsFlow.collectAsState()
    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val rules by configDataStore.rulesFlow.collectAsState()

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    var textInput by remember {
        mutableStateOf("“你确定这台AI引擎的延迟能低于300ms吗？”林萧紧盯着控制台屏幕问道。苏月微微一笑：“不但低于300毫秒，而且还支持旁白与对话自动双角色协同切换，听感如同专业CV配音。”")
    }

    var isRunning by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var totalSentences by remember { mutableStateOf(0) }
    var totalChars by remember { mutableStateOf(0) }
    var lastSynthesizedBytes by remember { mutableStateOf<ByteArray?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val barPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    fun stopTest() {
        isRunning = false
        audioPlayer.stop()
    }

    fun startTest() {
        if (activeProvider == null) return
        stopTest()
        isRunning = true
        latencyMs = null
        totalChars = textInput.length

        scope.launch {
            try {
                val byteStream = ByteArrayOutputStream()
                configDataStore.log("=== 开始测试工作台朗读任务 [${activeProvider.name}] ===")
                val preprocessed = TextPreprocessor.process(textInput, rules, settings.isNumberNormalizationEnabled)
                val segments = SentenceSplitter.splitTextWithRoles(preprocessed, settings.maxSentenceLength)
                totalSentences = segments.size
                configDataStore.log("切分得到 ${segments.size} 句进行合成 (双音色模式: ${activeProvider.isDualRoleEnabled})")

                val totalStart = System.currentTimeMillis()

                for ((idx, seg) in segments.withIndex()) {
                    if (!isRunning) break
                    val sentenceStart = System.currentTimeMillis()

                    val currentConfig = if (activeProvider.isDualRoleEnabled && seg.role == SegmentRole.DIALOGUE && activeProvider.dialogueVoiceId.isNotBlank()) {
                        activeProvider.copy(voiceId = activeProvider.dialogueVoiceId)
                    } else {
                        activeProvider
                    }

                    val roleLabel = if (seg.role == SegmentRole.DIALOGUE) "【对话·${currentConfig.voiceId}】" else "【旁白·${currentConfig.voiceId}】"
                    val result = TtsProviderManager.getInstance().synthesize(seg.text, currentConfig)
                    val cost = System.currentTimeMillis() - sentenceStart

                    if (idx == 0) {
                        latencyMs = cost
                        configDataStore.log("⚡ 首句出声延迟 (TTFB): ${cost}ms")
                    }

                    if (result.isSuccess) {
                        val rawBytes = result.getOrNull() ?: ByteArray(0)
                        if (rawBytes.isNotEmpty() && isRunning) {
                            byteStream.write(rawBytes)
                            configDataStore.log("第 ${idx + 1}/${segments.size} 句 $roleLabel 合成成功 (${cost}ms), 音频 ${rawBytes.size} 字节: \"${seg.text.take(15)}...\"")
                            val playDone = CompletableDeferred<Unit>()
                            audioPlayer.playAudioBytes(
                                audioBytes = rawBytes,
                                onCompletion = {
                                    playDone.complete(Unit)
                                },
                                onError = { err ->
                                    configDataStore.log("第 ${idx + 1} 句播放异常: $err")
                                    playDone.complete(Unit)
                                }
                            )
                            playDone.await()
                        }
                    } else {
                        configDataStore.log("第 ${idx + 1} 句合成失败: ${result.exceptionOrNull()?.message}")
                    }
                }

                lastSynthesizedBytes = byteStream.toByteArray()
                configDataStore.log("=== 朗读任务结束，总耗时 ${System.currentTimeMillis() - totalStart}ms ===")
                isRunning = false
            } catch (e: Exception) {
                configDataStore.log("测试异常: ${e.message}")
                isRunning = false
            }
        }
    }

    fun exportAudio() {
        val bytes = lastSynthesizedBytes
        if (bytes == null || bytes.isEmpty()) {
            Toast.makeText(context, "请先执行朗读测试生成音频", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val fileName = "AI_TTS_Audio_${System.currentTimeMillis()}.mp3"
            val file = File(downloadDir, fileName)
            file.writeBytes(bytes)
            Toast.makeText(context, "音频已成功导出至: Downloads/${fileName}", Toast.LENGTH_LONG).show()
            configDataStore.log("💾 音频已导出到文件: ${file.absolutePath} (${bytes.size} 字节)")
        } catch (e: Exception) {
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        SectionHeader(
            title = "全流程 AI 语音流式沙盒",
            subtitle = "实时可视化波形、首字出声 TTFB 延迟与分句吞吐量探测"
        )

        // 动态声波示波器
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val barCount = 48
                    val barSpacing = 2.dp.toPx()
                    val totalSpacing = barSpacing * (barCount - 1)
                    val barWidth = (size.width - totalSpacing) / barCount
                    val maxHeight = size.height * 0.9f

                    for (i in 0 until barCount) {
                        val x = i * (barWidth + barSpacing)
                        val factor = if (isRunning) {
                            val seed1 = kotlin.math.sin((i * 0.28f + barPhase * 6.28f).toDouble()).toFloat()
                            val seed2 = kotlin.math.cos((i * 0.15f - barPhase * 4.14f).toDouble()).toFloat()
                            ((seed1 + seed2) * 0.35f + 0.5f).coerceIn(0.12f, 1.0f)
                        } else {
                            0.08f
                        }
                        val h = maxHeight * factor
                        val y = (size.height - h) / 2f

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(
                                    primaryColor,
                                    primaryColor.copy(alpha = 0.4f)
                                )
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, h),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // 测试文本输入框与预设 Prompt
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("小说测试文本 (支持引号对白双音色)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("快捷小说文本片段预设:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {
                            textInput = "“这柄天玄诛仙剑，乃是上古神魔遗留在凡间的至宝。”老者抚须长叹道。少年握紧剑柄，目光坚定：“前辈放心，我必以它荡平魔域！”"
                        },
                        label = { Text("玄幻修仙对白", fontSize = 10.5.sp) }
                    )
                    AssistChip(
                        onClick = {
                            textInput = "公元2026年第128章，他在江南水乡重逢了青梅竹马。女孩撑着油纸伞轻声说：“你终于回来了，我等了整整五年。”"
                        },
                        label = { Text("言情重逢+数字", fontSize = 10.5.sp) }
                    )
                    AssistChip(
                        onClick = {
                            textInput = "“嫌疑人在昨晚23点45分离开案发现场，并在城东路口丢弃了凶器。”警官冷冷地看着审讯椅上的男人。"
                        },
                        label = { Text("悬疑审讯对白", fontSize = 10.5.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isRunning) stopTest() else startTest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) MaterialTheme.colorScheme.error else primaryColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isRunning) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("停止朗读")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("开始全流程测试")
                        }
                    }

                    if (lastSynthesizedBytes != null && lastSynthesizedBytes!!.isNotEmpty()) {
                        OutlinedButton(onClick = { exportAudio() }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("导出MP3", fontSize = 12.sp)
                        }
                    }

                    if (latencyMs != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TTFB: ${latencyMs}ms",
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                fontSize = 13.sp
                            )
                            if (totalSentences > 0) {
                                Text(
                                    text = "$totalSentences 句 / $totalChars 字",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("实时运行日志", fontWeight = FontWeight.Bold)
            IconButton(onClick = { configDataStore.clearLogs() }) {
                Icon(Icons.Default.Delete, contentDescription = "清空日志", tint = MaterialTheme.colorScheme.outline)
            }
        }

        // 日志控制台
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "暂无运行日志，点击上方测试按钮查看网络交互与解码推流日志...",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    items(logs.reversed(), contentType = { "log_item" }) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("失败") || log.contains("异常") || log.contains("错误")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
