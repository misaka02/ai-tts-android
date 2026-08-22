package com.aitts.engine.ui.screens

import android.app.Activity
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.permission.PermissionManager
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.components.PermissionCard
import com.aitts.engine.ui.components.ProviderCard
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.components.SystemTtsGuideCard
import com.aitts.engine.ui.theme.BrandTheme
import com.aitts.engine.ui.theme.PrimaryIndigo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
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

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTag by remember { mutableStateOf("全部") }
    var isProbingSpeed by remember { mutableStateOf(false) }
    val latencyMap = remember { mutableStateMapOf<String, Long>() }

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
                                configDataStore.log("播放错误: $err")
                                stopPlayback()
                            }
                        )
                    } else {
                        stopPlayback()
                    }
                } else {
                    configDataStore.log("试听请求失败: ${result.exceptionOrNull()?.message}")
                    stopPlayback()
                }
            } catch (e: Exception) {
                configDataStore.log("试听异常: ${e.message}")
                stopPlayback()
            }
        }
    }

    fun probeAllLatencies() {
        if (isProbingSpeed) return
        isProbingSpeed = true
        scope.launch {
            Toast.makeText(context, "正在并发测试各引擎网络延迟...", Toast.LENGTH_SHORT).show()
            val tasks = providers.map { provider ->
                async {
                    val start = System.currentTimeMillis()
                    val result = TtsProviderManager.getInstance().synthesize("测试", provider)
                    val duration = System.currentTimeMillis() - start
                    if (result.isSuccess) {
                        provider.id to duration
                    } else {
                        provider.id to -1L
                    }
                }
            }
            val results = tasks.awaitAll()
            results.forEach { (id, duration) ->
                if (duration > 0) {
                    latencyMap[id] = duration
                }
            }
            isProbingSpeed = false
            Toast.makeText(context, "网络延迟探测完成！", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredProviders = providers.filter { provider ->
        val matchesSearch = provider.name.contains(searchQuery, ignoreCase = true) ||
                provider.voiceId.contains(searchQuery, ignoreCase = true) ||
                provider.type.displayName.contains(searchQuery, ignoreCase = true)

        val matchesTag = when (selectedFilterTag) {
            "官方免Key" -> !provider.type.requiresApiKey
            "小米MiMo" -> provider.type == ProviderType.MIMO
            "微软Edge" -> provider.type == ProviderType.EDGE_TTS
            "Google" -> provider.type == ProviderType.GEMINI
            "已启用" -> provider.enabled
            else -> true
        }

        matchesSearch && matchesTag
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
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
            SystemTtsGuideCard(
                onOpenSettings = {
                    activity?.let { PermissionManager.openSystemTtsSettings(it) }
                }
            )
        }

        // 当前激活的主音色卡片
        item {
            SectionHeader(
                title = "当前系统生效发音模型",
                subtitle = "阅读/小说/读屏等所有第三方 App 将默认调用此配置"
            )

            val activeBrandColor = activeProvider?.let { BrandTheme.getColorForType(it.type) } ?: PrimaryIndigo

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(activeBrandColor.copy(alpha = 0.3f), Color.Transparent)
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = activeBrandColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeProvider?.name ?: "未选择提供商",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${activeProvider?.type?.displayName} · 音色: ${activeProvider?.voiceId?.ifBlank { "默认" }} · 语速 ${activeProvider?.speed}x",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testText,
                        onValueChange = { testText = it },
                        label = { Text("发音快速试听文本") },
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
                            colors = ButtonDefaults.buttonColors(containerColor = activeBrandColor),
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
                                Text("立即试听", fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { activeProvider?.let { onNavigateToEditProvider(it.id) } }
                        ) {
                            Text("调节参数")
                        }
                    }
                }
            }
        }

        // 引擎列表操作栏与过滤检索
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "音色引擎列表 (${providers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "支持上下调整排序、一键置顶与复制配置副本",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { probeAllLatencies() }
                    ) {
                        if (isProbingSpeed) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = "一键测速", tint = PrimaryIndigo)
                        }
                    }

                    IconButton(
                        onClick = {
                            val newId = "custom_${UUID.randomUUID().toString().take(6)}"
                            val newConfig = TtsProviderConfig(
                                id = newId,
                                type = ProviderType.MIMO,
                                name = "新建 AI 音色配置",
                                enabled = true,
                                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions",
                                modelName = "mimo-v2.5-tts",
                                voiceId = "茉莉"
                            )
                            configDataStore.updateProvider(newConfig)
                            onNavigateToEditProvider(newId)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建引擎", tint = PrimaryIndigo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索引擎名称 / 音色 / 模型...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 快捷分类过滤 Tag 栏
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tags = listOf("全部", "官方免Key", "小米MiMo", "微软Edge", "Google", "已启用")
                tags.forEach { tag ->
                    FilterChip(
                        selected = selectedFilterTag == tag,
                        onClick = { selectedFilterTag = tag },
                        label = { Text(tag, fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredProviders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("未找到符合条件的 AI 音色配置", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(filteredProviders, key = { it.id }) { provider ->
                ProviderCard(
                    provider = provider,
                    isActive = provider.id == settings.activeProviderId,
                    latencyMs = latencyMap[provider.id],
                    onSelect = {
                        configDataStore.setActiveProviderId(provider.id)
                    },
                    onEdit = {
                        onNavigateToEditProvider(provider.id)
                    },
                    onTest = {
                        playSpeechWithProvider(provider, "您好，我是 ${provider.name}，正在为您试听发音效果。")
                    },
                    onPinTop = {
                        configDataStore.pinProviderToTop(provider.id)
                        Toast.makeText(context, "已将 ${provider.name} 置顶", Toast.LENGTH_SHORT).show()
                    },
                    onMoveUp = {
                        configDataStore.moveProviderUp(provider.id)
                    },
                    onMoveDown = {
                        configDataStore.moveProviderDown(provider.id)
                    },
                    onDuplicate = {
                        configDataStore.duplicateProvider(provider.id)
                        Toast.makeText(context, "已复制配置副本", Toast.LENGTH_SHORT).show()
                    },
                    onDelete = {
                        if (providers.size <= 1) {
                            Toast.makeText(context, "至少需要保留一个音色配置", Toast.LENGTH_SHORT).show()
                        } else {
                            configDataStore.deleteProvider(provider.id)
                            Toast.makeText(context, "已删除 ${provider.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
