package com.aitts.engine.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.components.PermissionCard
import com.aitts.engine.ui.components.ProviderCard
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.components.SystemTtsGuideCard
import com.aitts.engine.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onNavigateToTestBench: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()

    var testText by remember { mutableStateOf("欢迎使用 AI TTS 系统语音引擎！当前正在通过智能大模型为您朗读文本。") }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTestingProviderId by remember { mutableStateOf<String?>(null) }

    var permissionState by remember {
        mutableStateOf(PermissionManager.checkPermissions(context))
    }

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()

    fun stopPlayback() {
        audioPlayer.stop()
        isPlaying = false
        isSynthesizing = false
        currentTestingProviderId = null
    }

    fun playSpeechWithProvider(targetProvider: TtsProviderConfig, textToSpeak: String) {
        stopPlayback()
        isSynthesizing = true
        currentTestingProviderId = targetProvider.id

        scope.launch {
            try {
                val startMs = System.currentTimeMillis()
                val result = TtsProviderManager.getInstance().synthesize(textToSpeak, targetProvider)
                val costMs = System.currentTimeMillis() - startMs

                if (result.isSuccess) {
                    val rawBytes = result.getOrNull() ?: ByteArray(0)
                    configDataStore.log("试听请求成功 [${targetProvider.name}] (${costMs}ms)，音频大小: ${rawBytes.size} 字节，开始播放")

                    if (rawBytes.isNotEmpty()) {
                        isSynthesizing = false
                        isPlaying = true

                        audioPlayer.playAudioBytes(
                            audioBytes = rawBytes,
                            onCompletion = {
                                isPlaying = false
                                currentTestingProviderId = null
                            },
                            onError = { err ->
                                isPlaying = false
                                currentTestingProviderId = null
                                configDataStore.log("播放错误: $err")
                            }
                        )
                    } else {
                        configDataStore.log("返回音频数据为空")
                        isSynthesizing = false
                        currentTestingProviderId = null
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "未知错误"
                    configDataStore.log("试听失败 [${targetProvider.name}]: $err")
                    isSynthesizing = false
                    currentTestingProviderId = null
                }
            } catch (e: Exception) {
                configDataStore.log("试听异常: ${e.message}")
                isSynthesizing = false
                currentTestingProviderId = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // 权限检测卡片
            PermissionCard(
                permissionState = permissionState,
                onRequestAll = {
                    activity?.let {
                        PermissionManager.requestBasicPermissions(it)
                        PermissionManager.requestAllFilesAccess(it)
                        PermissionManager.requestIgnoreBatteryOptimizations(it)
                        permissionState = PermissionManager.checkPermissions(context)
                    }
                },
                onRequestIgnoreBattery = {
                    activity?.let {
                        PermissionManager.requestIgnoreBatteryOptimizations(it)
                        permissionState = PermissionManager.checkPermissions(context)
                    }
                },
                onRequestAllFiles = {
                    activity?.let {
                        PermissionManager.requestAllFilesAccess(it)
                        permissionState = PermissionManager.checkPermissions(context)
                    }
                }
            )
        }

        item {
            // 系统 TTS 引擎设置引导
            SystemTtsGuideCard(
                onOpenSettings = {
                    activity?.let { PermissionManager.openSystemTtsSettings(it) }
                }
            )
        }

        item {
            SectionHeader(
                title = "当前激活的 AI 音色",
                subtitle = "小说阅读器等第三方 App 将默认使用此音色合成"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeProvider?.name ?: "未选择提供商",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeProvider?.type?.displayName} · 语速 ${activeProvider?.speed}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("试听文本") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isPlaying || isSynthesizing) {
                                    stopPlayback()
                                } else {
                                    activeProvider?.let { playSpeechWithProvider(it, testText) }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSynthesizing && currentTestingProviderId == activeProvider?.id) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("正在合成...", fontSize = 13.sp)
                            } else if (isPlaying && currentTestingProviderId == activeProvider?.id) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("停止播放", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("快速试听", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "音色快速切换",
                subtitle = "点击单选按钮直接切换全局默认发音模型"
            )
        }

        items(providers) { provider ->
            ProviderCard(
                provider = provider,
                isActive = provider.id == settings.activeProviderId,
                onSelect = {
                    configDataStore.setActiveProviderId(provider.id)
                },
                onEdit = {
                    onNavigateToEditProvider(provider.id)
                },
                onTest = {
                    playSpeechWithProvider(provider, "你好，我是 ${provider.name}，正在为您试听发音。")
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
