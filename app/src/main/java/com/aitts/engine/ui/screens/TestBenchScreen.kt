package com.aitts.engine.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.SentenceSplitter
import com.aitts.engine.rules.TextPreprocessor
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.PrimaryBlue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

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
            "第一章：林间风声。山林之中，微风拂过竹叶沙沙作响。\n" +
            "他停下脚步，握紧手中的长剑，目光敏锐地环顾四周。\n" +
            "“既然来了，何必藏头露尾？”他沉声说道。"
        )
    }

    var isRunning by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }

    fun stopTest() {
        audioPlayer.stop()
        isRunning = false
    }

    fun startTest() {
        if (activeProvider == null) return
        stopTest()
        isRunning = true
        latencyMs = null

        scope.launch {
            try {
                configDataStore.log("=== 开始测试工作台朗读任务 [${activeProvider.name}] ===")
                val preprocessed = TextPreprocessor.process(textInput, rules)
                val sentences = SentenceSplitter.splitText(preprocessed, settings.maxSentenceLength)
                configDataStore.log("切分得到 ${sentences.size} 句进行合成")

                val totalStart = System.currentTimeMillis()

                for ((idx, s) in sentences.withIndex()) {
                    if (!isRunning) break
                    val sentenceStart = System.currentTimeMillis()
                    val result = TtsProviderManager.getInstance().synthesize(s, activeProvider)
                    val cost = System.currentTimeMillis() - sentenceStart

                    if (idx == 0) {
                        latencyMs = cost
                        configDataStore.log("⚡ 首句首包出声延迟 (TTFB): ${cost}ms")
                    }

                    if (result.isSuccess) {
                        val rawBytes = result.getOrNull() ?: ByteArray(0)
                        if (rawBytes.isNotEmpty() && isRunning) {
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
            subtitle = "实时监测大模型首字延迟(TTFB)、分句流式衔接与 PCM 播放日志"
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
                    label = { Text("长篇测试文本") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )

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
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
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
                        Text(
                            text = "TTFB: ${latencyMs}ms",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            fontSize = 13.sp
                        )
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
