package com.aitts.engine.ui.screens

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
import com.aitts.engine.ui.theme.PrimaryBlue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestBenchScreen(configDataStore: ConfigDataStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logs by configDataStore.logsFlow.collectAsState()
    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val rules by configDataStore.rulesFlow.collectAsState()

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    val activeProvider = providers.find { it.id == settings.activeProviderId } ?: providers.firstOrNull()

    var textInput by remember {
        mutableStateOf(
            "第123章：林间风声。\n" +
            "更新于2026年，完读率达到了99.5%。\n" +
            "山林之中微风拂过，他停下脚步，握紧长剑。\n" +
            "“既然来了，何必藏头露尾？”他沉声说道。\n" +
            "“多年不见，你依然如此警觉。”一道清脆灵动的女声从树梢上传来。"
        )
    }

    var isRunning by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var totalChars by remember { mutableStateOf(0) }
    var totalSentences by remember { mutableStateOf(0) }

    fun stopTest() {
        audioPlayer.stop()
        isRunning = false
    }

    fun startTest() {
        if (activeProvider == null) return
        stopTest()
        isRunning = true
        latencyMs = null
        totalChars = textInput.length

        scope.launch {
            try {
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

                configDataStore.log("=== 朗读任务结束，总耗时 ${System.currentTimeMillis() - totalStart}ms ===")
                isRunning = false
            } catch (e: Exception) {
                configDataStore.log("测试异常: ${e.message}")
                isRunning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(
            title = "全流程流式测试工作台",
            subtitle = "实时监测大模型出声延迟 (TTFB)、智能多角色双音色流式衔接与 PCM 播放"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("小说长篇测试文本 (${textInput.length} 字)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text("快速载入典型测试片段：", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AssistChip(
                        onClick = {
                            textInput = "第123章：林间风声。\n更新于2026年，完读率达到了99.5%。\n山林之中微风拂过，他停下脚步，握紧长剑。\n“既然来了，何必藏头露尾？”他沉声说道。\n“多年不见，你依然如此警觉。”一道清脆灵动的女声从树梢上传来。"
                        },
                        label = { Text("⚔️ 玄幻对白+数字", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            textInput = "江南三月，烟雨蒙蒙。\n“小姐，雨势大了，快进亭子避避吧。”丫鬟轻声催促着。\n“无妨，这雨中的荷塘，倒比晴日更有几分意境。”她莞尔一笑，声音轻柔动人。"
                        },
                        label = { Text("🌸 言情水乡双角色", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = {
                            textInput = "深夜十二点，警局审讯室里一片死寂。\n“说说吧，昨晚十一点四十五分，你究竟在哪里？”刑警队长目光如炬地盯着他。\n“我……我真的什么都没做，警官，请相信我！”男子声音颤抖。"
                        },
                        label = { Text("🕵️ 悬疑审讯对白", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 律动声波组件
                if (isRunning) {
                    AudioWaveformVisualizer(modifier = Modifier.fillMaxWidth().height(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isRunning) stopTest() else startTest()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) MaterialTheme.colorScheme.error else PrimaryBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isRunning) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("停止朗读")
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("开始全流程流式测试")
                        }
                    }

                    if (latencyMs != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TTFB: ${latencyMs}ms",
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue,
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
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
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                items(logs) { log ->
                    Text(
                        text = log,
                        color = if (log.contains("失败") || log.contains("异常") || log.contains("错误")) Color(0xFFF87171) else Color(0xFF4ADE80),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * 律动跳动声波可视化 Canvas
 */
@Composable
fun AudioWaveformVisualizer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bars"
    )

    Canvas(modifier = modifier) {
        val barCount = 20
        val totalWidth = size.width
        val barWidth = totalWidth / (barCount * 1.8f)
        val gap = (totalWidth - barCount * barWidth) / (barCount - 1)
        val maxHeight = size.height

        val primaryBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF38BDF8), Color(0xFF2563EB))
        )

        for (i in 0 until barCount) {
            val phase = (i * 0.35f)
            val dynamicScale = (Math.sin((animProgress * Math.PI * 2 + phase).toDouble()).toFloat() + 1.2f) / 2.2f
            val barHeight = (maxHeight * dynamicScale.coerceIn(0.15f, 0.95f))
            val x = i * (barWidth + gap)
            val y = (maxHeight - barHeight) / 2

            drawRoundRect(
                brush = primaryBrush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
