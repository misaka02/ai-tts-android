package com.aitts.engine.ui.pulse.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.launch

/**
 * ⚡ Pulse 模型精细参数配置台 (Pulse Provider Config Screen)
 * 1. 顶部引擎快捷预设与一键套用；
 * 2. 凭证参数、音色选择、发音倍速/音调微调（双精度滑块 + 步进器）；
 * 3. 小说双角色对白分流与采样率高级输出；
 * 4. 底部右下角悬浮 [ ▶ 试听 | ✔ 保存 ] 操作岛。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseProviderConfigScreen(
    providerId: String,
    configDataStore: ConfigDataStore,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val providers by configDataStore.providersFlow.collectAsState()
    val settings by configDataStore.settingsFlow.collectAsState()

    val initialConfig = remember(providerId, providers) {
        providers.find { it.id == providerId } ?: TtsProviderConfig(
            id = providerId,
            type = ProviderType.MIMO,
            name = "新建模型"
        )
    }

    var name by remember(initialConfig) { mutableStateOf(initialConfig.name) }
    var selectedType by remember(initialConfig) { mutableStateOf(initialConfig.type) }
    var baseUrl by remember(initialConfig) { mutableStateOf(initialConfig.baseUrl) }
    var apiKey by remember(initialConfig) { mutableStateOf(initialConfig.apiKey) }
    var secondaryApiKey by remember(initialConfig) { mutableStateOf(initialConfig.secondaryApiKey) }
    var modelName by remember(initialConfig) { mutableStateOf(initialConfig.modelName) }
    var voiceId by remember(initialConfig) { mutableStateOf(initialConfig.voiceId) }
    var dialogueVoiceId by remember(initialConfig) { mutableStateOf(initialConfig.dialogueVoiceId) }
    var isDualRoleEnabled by remember(initialConfig) { mutableStateOf(initialConfig.isDualRoleEnabled) }
    var promptInstruction by remember(initialConfig) { mutableStateOf(initialConfig.promptInstruction) }
    var speed by remember(initialConfig) { mutableFloatStateOf(initialConfig.speed) }
    var pitch by remember(initialConfig) { mutableFloatStateOf(initialConfig.pitch) }
    var sampleRate by remember(initialConfig) { mutableStateOf(initialConfig.sampleRate.toString()) }
    var audioFormat by remember(initialConfig) { mutableStateOf(initialConfig.audioFormat) }
    var isStreamingEnabled by remember(initialConfig) { mutableStateOf(initialConfig.isStreamingEnabled) }
    var customPayload by remember(initialConfig) { mutableStateOf(initialConfig.customPayloadTemplate) }

    val brandColor = if (settings.isProviderCardAccentColorEnabled) {
        when (selectedType) {
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

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSecondaryPasswordVisible by remember { mutableStateOf(false) }
    var isTestingAudio by remember { mutableStateOf(false) }

    var isFetchingVoices by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var isSelectingDialogueVoice by remember { mutableStateOf(false) }
    var voiceSearchQuery by remember { mutableStateOf("") }
    var fetchedVoicesList by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
    var voicesFetchStatus by remember { mutableStateOf<String?>("") }

    var isFetchingModels by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var modelSearchQuery by remember { mutableStateOf("") }
    var fetchedModelsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var modelsFetchStatus by remember { mutableStateOf<String?>("") }

    var offlineCatalog by remember { mutableStateOf(com.aitts.engine.offline.OfflineModelManager.getCatalog()) }
    var isRefreshingOfflineCatalog by remember { mutableStateOf(false) }
    var selectedOfflineCategory by remember { mutableStateOf("全部") }
    var downloadProgressMap by remember { mutableStateOf<Map<String, Pair<Int, String>>>(emptyMap()) }
    var downloadChannel by remember { mutableStateOf("hf_mirror") }

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.stop() }
    }

    fun buildCurrentConfig(): TtsProviderConfig {
        return initialConfig.copy(
            name = name.trim().ifBlank { selectedType.displayName },
            type = selectedType,
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            secondaryApiKey = secondaryApiKey.trim(),
            modelName = modelName.trim(),
            voiceId = voiceId.trim(),
            dialogueVoiceId = dialogueVoiceId.trim(),
            isDualRoleEnabled = isDualRoleEnabled,
            isStreamingEnabled = isStreamingEnabled,
            promptInstruction = promptInstruction.trim(),
            speed = speed,
            pitch = pitch,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            audioFormat = audioFormat.trim(),
            customPayloadTemplate = customPayload
        )
    }

    fun applyOfficialDefaults(type: ProviderType) {
        selectedType = type
        when (type) {
            ProviderType.MIMO -> {
                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions"
                modelName = "mimo-v2.5-tts"
                voiceId = "mimo_default"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.MINIMAX -> {
                baseUrl = "https://api.minimaxi.com/v1/t2a_v2"
                modelName = "speech-02-turbo"
                voiceId = "female-shaonv"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.DOUBAO -> {
                baseUrl = "https://openspeech.bytedance.com/api/v1/tts"
                modelName = "volcano_bigtts"
                voiceId = "zh_female_shuangkuaisisi_moon_bigtts"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.OPENAI -> {
                baseUrl = "https://api.openai.com/v1/audio/speech"
                modelName = "tts-1"
                voiceId = "nova"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.GEMINI -> {
                baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"
                modelName = "gemini-2.5-flash-preview-tts"
                voiceId = "Puck"
                sampleRate = "24000"
                audioFormat = "wav"
            }
            ProviderType.SILICONFLOW -> {
                baseUrl = "https://api.siliconflow.cn/v1/audio/speech"
                modelName = "FunAudioLLM/CosyVoice2-0.5B"
                voiceId = "FunAudioLLM/CosyVoice2-0.5B:alex"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.STEPFUN -> {
                baseUrl = "https://api.stepfun.com/v1/audio/speech"
                modelName = "stepaudio-2.5-tts"
                voiceId = "cixingnansheng"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.FISH_AUDIO -> {
                baseUrl = "https://api.fish.audio/v1/tts"
                modelName = "speech-v1.4"
                voiceId = "7f92f8afb8ec43bf81429cc1c9199cb1"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.EDGE_TTS -> {
                baseUrl = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"
                modelName = "edge-neural"
                voiceId = "zh-CN-XiaoxiaoNeural"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.AZURE -> {
                baseUrl = "https://eastasia.tts.speech.microsoft.com/cognitiveservices/v1"
                modelName = "azure-neural"
                voiceId = "zh-CN-XiaoxiaoNeural"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.OFFLINE_VITS -> {
                name = "微软离线自然语音 (晓晓)"
                modelName = "ms-offline-xiaoxiao"
                voiceId = "zh-CN-XiaoxiaoOffline"
                sampleRate = "24000"
                audioFormat = "wav"
            }
            else -> {
                baseUrl = "http://127.0.0.1:9880/tts"
                modelName = "gpt-sovits-v2"
                voiceId = "default"
                sampleRate = "32000"
                audioFormat = "wav"
            }
        }
        Toast.makeText(context, "已套用 ${type.displayName} 预设", Toast.LENGTH_SHORT).show()
    }

    fun fetchOnlineVoices() {
        if (isFetchingVoices) return
        isFetchingVoices = true
        scope.launch {
            try {
                val current = buildCurrentConfig()
                val list = TtsProviderManager.getInstance().getAvailableVoices(current)
                fetchedVoicesList = list
                voicesFetchStatus = "🟢 已从 ${current.type.displayName} 官方端点实时拉取成功 (${list.size} 款音色)"
                showVoiceDialog = true
                Toast.makeText(context, "已获取到 ${list.size} 款音色", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                voicesFetchStatus = "⚠️ 探测异常: ${e.message}，已展示离线推荐音色"
                Toast.makeText(context, "获取音色: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isFetchingVoices = false
            }
        }
    }

    fun fetchOnlineModels() {
        if (isFetchingModels) return
        isFetchingModels = true
        scope.launch {
            try {
                val current = buildCurrentConfig()
                val list = TtsProviderManager.getInstance().getAvailableModels(current)
                fetchedModelsList = list
                modelsFetchStatus = "🟢 已从 ${current.type.displayName} 官方端点实时拉取成功 (${list.size} 个模型)"
                showModelDialog = true
                Toast.makeText(context, "已获取到 ${list.size} 个可用模型", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                modelsFetchStatus = "⚠️ 探测异常: ${e.message}，已展示离线推荐模型"
                Toast.makeText(context, "获取模型: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isFetchingModels = false
            }
        }
    }

    fun testAudioPlayback() {
        if (isTestingAudio) {
            audioPlayer.stop()
            isTestingAudio = false
            return
        }
        isTestingAudio = true
        scope.launch {
            try {
                val cfg = buildCurrentConfig()
                val text = "这是一段发音测试，当前模型为${cfg.name}，音色为${cfg.voiceId.ifBlank { "默认" }}。"
                val res = TtsProviderManager.getInstance().synthesize(text, cfg)
                if (res.isSuccess) {
                    val bytes = res.getOrNull() ?: ByteArray(0)
                    if (bytes.isNotEmpty()) {
                        audioPlayer.playAudioBytes(audioBytes = bytes, speed = cfg.speed, onCompletion = { isTestingAudio = false })
                    } else {
                        isTestingAudio = false
                        Toast.makeText(context, "合成音频为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isTestingAudio = false
                    Toast.makeText(context, "试听失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isTestingAudio = false
                Toast.makeText(context, "调用异常: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val hasUnsavedChanges = remember(
        name, selectedType, baseUrl, apiKey, secondaryApiKey, modelName,
        voiceId, dialogueVoiceId, isDualRoleEnabled, promptInstruction,
        speed, pitch, sampleRate, audioFormat, isStreamingEnabled, customPayload
    ) {
        name != initialConfig.name ||
        selectedType != initialConfig.type ||
        baseUrl != initialConfig.baseUrl ||
        apiKey != initialConfig.apiKey ||
        secondaryApiKey != initialConfig.secondaryApiKey ||
        modelName != initialConfig.modelName ||
        voiceId != initialConfig.voiceId ||
        dialogueVoiceId != initialConfig.dialogueVoiceId ||
        isDualRoleEnabled != initialConfig.isDualRoleEnabled ||
        promptInstruction != initialConfig.promptInstruction ||
        speed != initialConfig.speed ||
        pitch != initialConfig.pitch ||
        sampleRate != initialConfig.sampleRate.toString() ||
        audioFormat != initialConfig.audioFormat ||
        isStreamingEnabled != initialConfig.isStreamingEnabled ||
        customPayload != initialConfig.customPayloadTemplate
    }

    var showUnsavedDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onNavigateBack()
        }
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = name.ifBlank { "模型参数配置" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PulseTokens.TextPrimary
                        )
                        Text(
                            text = "${selectedType.displayName} · ${voiceId.ifBlank { "未选音色" }}",
                            fontSize = 11.sp,
                            color = PulseTokens.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) {
                            showUnsavedDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = PulseTokens.TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { applyOfficialDefaults(selectedType) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = brandColor),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("套用预设", fontSize = 11.5.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseTokens.CanvasDeep)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    },
                contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 引擎与服务凭证
                item {
                    PulseCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, brandColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🔌 引擎与连接凭证", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = brandColor)

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(ProviderType.values()) { type ->
                                    val isSelected = selectedType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { applyOfficialDefaults(type) },
                                        label = { Text(type.displayName, fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = brandColor,
                                            selectedLabelColor = Color.Black,
                                            containerColor = PulseTokens.SurfaceElevated,
                                            labelColor = PulseTokens.TextSecondary
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("模型别名") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                            )

                            if (selectedType != ProviderType.OFFLINE_VITS) {
                                OutlinedTextField(
                                    value = baseUrl,
                                    onValueChange = { baseUrl = it },
                                    label = { Text("服务 Base URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                )

                                if (selectedType.requiresApiKey) {
                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { apiKey = it },
                                        label = { Text("主 API Key 密钥") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )

                                    OutlinedTextField(
                                        value = secondaryApiKey,
                                        onValueChange = { secondaryApiKey = it },
                                        label = { Text("第二 API Key (预加载并发分流/选填)") },
                                        placeholder = { Text("选填：配置第二 Key 实现双通道轮询防限流") },
                                        visualTransformation = if (isSecondaryPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isSecondaryPasswordVisible = !isSecondaryPasswordVisible }) {
                                                Icon(imageVector = if (isSecondaryPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )
                                    Text(
                                        text = "💡 提示：配置第二 Key 后，系统在分段预加载时将自动与主 Key 进行 50/50 轮询分流，有效翻倍服务端 RPM/TPM 速率限制，避免 429 报错。",
                                        fontSize = 11.sp,
                                        color = PulseTokens.TextTertiary,
                                        lineHeight = 15.sp
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = PulseTokens.AcidGreen.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, PulseTokens.AcidGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚡ 100% 本地端侧离线神经网络引擎 · 零流量消耗，断网可用，无需填写任何 API Key 与 Base URL",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PulseTokens.AcidGreen,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. 音色与倍速声学
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("🎙️ 音色与发音倍速", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.TextPrimary)

                            if (selectedType != ProviderType.OFFLINE_VITS) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = modelName,
                                        onValueChange = { modelName = it },
                                        label = { Text("模型 ID (Model Name)") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )

                                    OutlinedButton(
                                        onClick = { fetchOnlineModels() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = PulseTokens.SurfaceElevated,
                                            contentColor = PulseTokens.CyanElectric
                                        ),
                                        border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        if (isFetchingModels) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseTokens.CyanElectric, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("模型", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = voiceId,
                                        onValueChange = { voiceId = it },
                                        label = { Text("主音色 (Voice ID)") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )

                                    OutlinedButton(
                                        onClick = { isSelectingDialogueVoice = false; fetchOnlineVoices() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = PulseTokens.SurfaceElevated,
                                            contentColor = PulseTokens.CyanElectric
                                        ),
                                        border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        if (isFetchingVoices && !isSelectingDialogueVoice) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseTokens.CyanElectric, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("音色", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = modelName,
                                        onValueChange = { modelName = it },
                                        label = { Text("离线模型包 ID") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )
                                    OutlinedTextField(
                                        value = voiceId,
                                        onValueChange = { voiceId = it },
                                        label = { Text("离线发音人 ID") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )
                                }
                            }

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("发音倍速: ${String.format(java.util.Locale.US, "%.2f", speed)}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable { speed = (speed - 0.05f).coerceAtLeast(0.5f) }) {
                                            Icon(Icons.Default.Remove, contentDescription = null, tint = PulseTokens.TextPrimary, modifier = Modifier.padding(6.dp).size(14.dp))
                                        }
                                        Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable { speed = (speed + 0.05f).coerceAtMost(2.5f) }) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.padding(6.dp).size(14.dp))
                                        }
                                    }
                                }
                                Slider(
                                    value = speed,
                                    onValueChange = { speed = it },
                                    valueRange = 0.5f..2.5f,
                                    steps = 39
                                )
                            }

                            // 高级 AI 自回归大模型（如 MiMo、MiniMax、豆包、阶跃、OpenAI等）由端到端神经网络直出，仅传统参数化引擎支持 Pitch
                            val supportsPitch = when (selectedType) {
                                ProviderType.EDGE_TTS,
                                ProviderType.AZURE,
                                ProviderType.CUSTOM_HTTP -> true
                                else -> false
                            }

                            if (supportsPitch) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("音调微调: ${String.format(java.util.Locale.US, "%.2f", pitch)}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable { pitch = (pitch - 0.05f).coerceAtLeast(0.5f) }) {
                                                Icon(Icons.Default.Remove, contentDescription = null, tint = PulseTokens.TextPrimary, modifier = Modifier.padding(6.dp).size(14.dp))
                                            }
                                            Surface(shape = CircleShape, color = PulseTokens.SurfaceElevated, modifier = Modifier.clip(CircleShape).clickable { pitch = (pitch + 0.05f).coerceAtMost(2.0f) }) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.padding(6.dp).size(14.dp))
                                            }
                                        }
                                    }
                                    Slider(
                                        value = pitch,
                                        onValueChange = { pitch = it },
                                        valueRange = 0.5f..2.0f,
                                        steps = 29
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseTokens.SurfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "端到端神经网络直出大模型原生自适应音高，无需人工调节音调",
                                            fontSize = 11.5.sp,
                                            color = PulseTokens.TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. AI 语气提示词与系统指令 (System Prompt - 仅针对 AI 引擎展示)
                val isAiModel = when (selectedType) {
                    ProviderType.EDGE_TTS, ProviderType.AZURE -> false
                    else -> true
                }
                if (isAiModel) {
                    item {
                        PulseCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, PulseTokens.AmberWarm.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎭 AI 语气与导演指令", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.AmberWarm)
                                    Text("System Prompt", fontSize = 11.sp, color = PulseTokens.TextTertiary)
                                }

                                OutlinedTextField(
                                    value = promptInstruction,
                                    onValueChange = { promptInstruction = it },
                                    label = { Text("语气提示词 / 系统指令") },
                                    placeholder = { Text("例如：请用自然、沉稳且富有感染力的语气朗读文本...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 5,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.AmberWarm)
                                )

                                Text("情绪预设模板 (点击一键填入):", fontSize = 11.5.sp, color = PulseTokens.TextSecondary, fontWeight = FontWeight.Bold)
                                val promptPresets = listOf(
                                    "自然流畅" to "请用自然、沉稳且富有感染力的语气朗读文本。",
                                    "激情昂扬" to "请用充满激情、热烈且铿锵有力的语调进行朗读。",
                                    "温柔低语" to "请用轻柔、舒缓、富有治愈感的伴读语气进行朗读。",
                                    "严肃新闻" to "请用字正腔圆、严谨庄重的新闻主播声调进行播报。",
                                    "悬疑惊悚" to "请用压抑低沉、节奏缓慢且充满悬疑感的语气朗读。",
                                    "深沉磁性" to "请用低沉深情、富有磁性与故事感的成熟语调朗读。"
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    promptPresets.take(3).forEach { (pName, pText) ->
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { promptInstruction = pText },
                                            shape = RoundedCornerShape(8.dp),
                                            color = PulseTokens.SurfaceElevated,
                                            border = BorderStroke(1.dp, PulseTokens.AmberWarm.copy(alpha = 0.35f))
                                        ) {
                                            Text(pName, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    promptPresets.drop(3).forEach { (pName, pText) ->
                                        Surface(
                                            modifier = Modifier.weight(1f).clickable { promptInstruction = pText },
                                            shape = RoundedCornerShape(8.dp),
                                            color = PulseTokens.SurfaceElevated,
                                            border = BorderStroke(1.dp, PulseTokens.AmberWarm.copy(alpha = 0.35f))
                                        ) {
                                            Text(pName, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. 双角色对白分流与高级设置
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("🎭 双角色与高级输出", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("启用双角色分流", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                    Text("旁白主音色，引呈对白专属音色", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = isDualRoleEnabled,
                                    onCheckedChange = { isDualRoleEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                )
                            }

                            if (isDualRoleEnabled) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = dialogueVoiceId,
                                        onValueChange = { dialogueVoiceId = it },
                                        label = { Text("对话专属音色 (Voice ID)") },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                    )
                                    OutlinedButton(
                                        onClick = { isSelectingDialogueVoice = true; fetchOnlineVoices() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = PulseTokens.SurfaceElevated,
                                            contentColor = PulseTokens.CyanElectric
                                        ),
                                        border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(52.dp)
                                    ) {
                                        Text("选择", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            if (selectedType != ProviderType.OFFLINE_VITS) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("流式传输 (Streaming)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                        Text(if (isStreamingEnabled) "开启：边生成边实时推流，首包延迟极低" else "关闭：接收服务端完整高质量音频，音质平滑纯净", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                    }
                                    Switch(
                                        checked = isStreamingEnabled,
                                        onCheckedChange = { isStreamingEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SonicBlue)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = sampleRate,
                                onValueChange = { sampleRate = it },
                                label = { Text("采样率 (Hz)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                            )
                        }
                    }
                }

                // 离线神经网络专属配置卡片 (大模型库自选管理、端侧推理加速、多通道下载、在线动态同步)
                if (selectedType == ProviderType.OFFLINE_VITS) {
                    item(contentType = "offline_model_pack") {
                        PulseCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = PulseTokens.SurfaceElevated,
                            border = PulseTokens.BorderSubtle,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("📦 离线神经网络语音库", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)
                                        Text("顶级大模型 / 微软全系列 / Sherpa-ONNX", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (!isRefreshingOfflineCatalog) {
                                                isRefreshingOfflineCatalog = true
                                                scope.launch {
                                                    val res = com.aitts.engine.offline.OfflineModelManager.refreshRemoteCatalog(context)
                                                    if (res.isSuccess) {
                                                        offlineCatalog = res.getOrNull() ?: offlineCatalog
                                                        Toast.makeText(context, "已从云端同步最新离线模型列表 (${offlineCatalog.size}款)", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "刷新失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isRefreshingOfflineCatalog = false
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        if (isRefreshingOfflineCatalog) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PulseTokens.CyanElectric, strokeWidth = 1.5.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text("刷新最新模型", fontSize = 11.sp, color = PulseTokens.CyanElectric, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                // 下载节点镜像切换
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("下载节点:", fontSize = 11.sp, color = PulseTokens.TextTertiary)
                                    val channels = listOf("hf_mirror" to "国内高速(HF)", "github" to "GitHub直连", "cdn" to "CDN加速")
                                    channels.forEach { (cId, cName) ->
                                        val isSel = downloadChannel == cId
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { downloadChannel = cId },
                                            color = if (isSel) PulseTokens.CyanElectric.copy(alpha = 0.2f) else PulseTokens.SurfaceDark,
                                            border = if (isSel) BorderStroke(1.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = cName,
                                                fontSize = 10.5.sp,
                                                color = if (isSel) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                // 分类筛选标签
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val cats = listOf("全部", "顶级大模型高拟真", "微软离线自然语音", "Sherpa-ONNX 经典")
                                    cats.forEach { cat ->
                                        val isSel = selectedOfflineCategory == cat
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .clickable { selectedOfflineCategory = cat },
                                            color = if (isSel) PulseTokens.SonicBlue.copy(alpha = 0.25f) else PulseTokens.SurfaceDark,
                                            border = if (isSel) BorderStroke(1.dp, PulseTokens.SonicBlue) else PulseTokens.BorderSubtle,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (cat == "全部") "全部" else cat.take(4),
                                                fontSize = 11.sp,
                                                color = if (isSel) PulseTokens.SonicBlue else PulseTokens.TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                val displayedPacks = offlineCatalog.filter {
                                    selectedOfflineCategory == "全部" || it.category == selectedOfflineCategory
                                }

                                displayedPacks.forEach { pack ->
                                    val isCur = modelName == pack.id
                                    val isDownloaded = com.aitts.engine.offline.OfflineModelManager.isModelDownloaded(context, pack.id)
                                    val progressInfo = downloadProgressMap[pack.id]
                                    val isDownloading = progressInfo != null && progressInfo.first < 100

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp)),
                                        color = if (isCur) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceDark,
                                        border = if (isCur) BorderStroke(1.2.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(pack.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCur) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = PulseTokens.SurfaceElevated,
                                                            border = PulseTokens.BorderSubtle
                                                        ) {
                                                            Text("${pack.sizeMb}MB", fontSize = 10.sp, color = PulseTokens.TextTertiary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                    Text(pack.description, fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                                }
                                                if (isCur) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                                                }
                                            }

                                            // 下载与选用控制条
                                            if (isDownloading) {
                                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    androidx.compose.material3.LinearProgressIndicator(
                                                        progress = { (progressInfo.first.toFloat() / 100f).coerceIn(0f, 1f) },
                                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                                        color = PulseTokens.CyanElectric,
                                                        trackColor = PulseTokens.SurfaceElevated
                                                    )
                                                    Text(progressInfo.second, fontSize = 10.5.sp, color = PulseTokens.CyanElectric)
                                                }
                                            } else {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (isDownloaded) {
                                                        Text("🟢 本地已就绪", fontSize = 11.sp, color = PulseTokens.AcidGreen, fontWeight = FontWeight.SemiBold)
                                                    } else {
                                                        Text("⚪ 待下载 (${pack.sizeMb}MB)", fontSize = 11.sp, color = PulseTokens.TextTertiary)
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        if (!isDownloaded) {
                                                            Button(
                                                                onClick = {
                                                                    scope.launch {
                                                                        downloadProgressMap = downloadProgressMap + (pack.id to (5 to "正在连接节点..."))
                                                                        val res = com.aitts.engine.offline.OfflineModelManager.downloadModelPackage(
                                                                            context = context,
                                                                            modelId = pack.id,
                                                                            channel = downloadChannel,
                                                                            onProgress = { pct, txt ->
                                                                                downloadProgressMap = downloadProgressMap + (pack.id to (pct to txt))
                                                                            }
                                                                        )
                                                                        if (res.isSuccess) {
                                                                            Toast.makeText(context, "【${pack.name}】下载安装就绪！", Toast.LENGTH_SHORT).show()
                                                                            modelName = pack.id
                                                                            voiceId = pack.defaultVoiceId
                                                                            sampleRate = pack.sampleRate.toString()
                                                                        } else {
                                                                            downloadProgressMap = downloadProgressMap - pack.id
                                                                            Toast.makeText(context, "下载失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                                        }
                                                                    }
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black),
                                                                modifier = Modifier.height(30.dp)
                                                            ) {
                                                                Text("一键下载", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }

                                                        Button(
                                                            onClick = {
                                                                modelName = pack.id
                                                                voiceId = pack.defaultVoiceId
                                                                sampleRate = pack.sampleRate.toString()
                                                                Toast.makeText(context, "已选用模型: ${pack.name}", Toast.LENGTH_SHORT).show()
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isCur) PulseTokens.SurfaceElevated else PulseTokens.SonicBlue,
                                                                contentColor = if (isCur) PulseTokens.TextSecondary else Color.Black
                                                            ),
                                                            modifier = Modifier.height(30.dp)
                                                        ) {
                                                            Text(if (isCur) "已选用" else "选用此模型", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // 端侧 CPU 推理加速与运行设置
                                Text("⚡ 端侧 CPU 推理加速与精度设置", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PulseTokens.TextPrimary)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = PulseTokens.SurfaceDark,
                                        border = PulseTokens.BorderSubtle
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("CPU 推理线程数", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                            Text("4 线程 (自动负载均衡)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = PulseTokens.SurfaceDark,
                                        border = PulseTokens.BorderSubtle
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("运算精度", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                            Text("INT8 (极速省电)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.AcidGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 底部紧凑双操作栏 (试听当前配置 + 保存配置)
                item(contentType = "bottom_action_bar") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { testAudioPlayback() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isTestingAudio) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                contentColor = PulseTokens.CyanElectric
                            ),
                            border = BorderStroke(1.2.dp, PulseTokens.CyanElectric.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PulseTokens.CyanElectric
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isTestingAudio) "停止试听" else "试听当前配置",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                val updated = buildCurrentConfig()
                                configDataStore.updateProvider(updated)
                                Toast.makeText(context, "已保存模型配置: ${updated.name}", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PulseTokens.CyanElectric,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存配置", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // 未保存更改退出二次确认拦截弹窗
        if (showUnsavedDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedDialog = false },
                title = { Text("是否保存更改？", fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary) },
                text = { Text("检测到模型「$name」的配置已被编辑修改，退出前是否保存？", fontSize = 13.5.sp, color = PulseTokens.TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = buildCurrentConfig()
                            configDataStore.updateProvider(updated)
                            Toast.makeText(context, "已保存模型: ${updated.name}", Toast.LENGTH_SHORT).show()
                            showUnsavedDialog = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black)
                    ) {
                        Text("保存并退出", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showUnsavedDialog = false }) {
                            Text("取消")
                        }
                        TextButton(onClick = {
                            showUnsavedDialog = false
                            onNavigateBack()
                        }) {
                            Text("放弃更改", color = PulseTokens.MagentaLaser)
                        }
                    }
                }
            )
        }

        // 右下角大拇指悬浮操作岛 (模型配置专属动作组)
        UniversalActionHub(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp),
            items = listOf(
                ActionHubItem(
                    label = if (isTestingAudio) "停止试听" else "试听当前发音",
                    icon = if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow,
                    color = if (isTestingAudio) PulseTokens.MagentaLaser else PulseTokens.CyanElectric,
                    isLoading = isTestingAudio,
                    onClick = { testAudioPlayback() }
                ),
                ActionHubItem(
                    label = "保存模型配置",
                    icon = Icons.Default.Check,
                    color = PulseTokens.CyanElectric,
                    onClick = {
                        val updated = buildCurrentConfig()
                        configDataStore.updateProvider(updated)
                        Toast.makeText(context, "已保存模型: ${updated.name}", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                ),
                ActionHubItem(
                    label = "在线拉取官方音色",
                    icon = Icons.Default.RecordVoiceOver,
                    color = PulseTokens.SonicBlue,
                    isLoading = isFetchingVoices,
                    onClick = { isSelectingDialogueVoice = false; fetchOnlineVoices() }
                ),
                ActionHubItem(
                    label = "套用官方最优预设",
                    icon = Icons.Default.AutoFixHigh,
                    color = PulseTokens.AmberWarm,
                    onClick = { applyOfficialDefaults(selectedType) }
                )
            ),
            isHighlighted = isTestingAudio,
            icon = Icons.Default.Tune
        )

        // 模型选择弹窗
        if (showModelDialog) {
            AlertDialog(
                onDismissRequest = { showModelDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("选择可用模型 ID", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { fetchOnlineModels() }, modifier = Modifier.size(32.dp)) {
                            if (isFetchingModels) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseTokens.CyanElectric, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "重新探测", tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!modelsFetchStatus.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (modelsFetchStatus!!.startsWith("🟢")) PulseTokens.AcidGreen.copy(alpha = 0.15f) else PulseTokens.AmberWarm.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = modelsFetchStatus!!,
                                    fontSize = 11.sp,
                                    color = if (modelsFetchStatus!!.startsWith("🟢")) PulseTokens.AcidGreen else PulseTokens.AmberWarm,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = modelSearchQuery,
                            onValueChange = { modelSearchQuery = it },
                            placeholder = { Text("搜索或输入自定义模型 ID...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true
                        )

                        val filteredModels = fetchedModelsList.filter {
                            it.contains(modelSearchQuery, ignoreCase = true)
                        }

                        if (filteredModels.isEmpty() && modelSearchQuery.isNotBlank()) {
                            Button(
                                onClick = {
                                    modelName = modelSearchQuery.trim()
                                    showModelDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric)
                            ) {
                                Text("直接使用输入值: \"$modelSearchQuery\"", fontSize = 12.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredModels) { modelId ->
                                val isCur = modelName == modelId
                                PulseCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            modelName = modelId
                                            showModelDialog = false
                                        },
                                    backgroundColor = if (isCur) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                    border = if (isCur) BorderStroke(1.2.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(modelId, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCur) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                        if (isCur) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showModelDialog = false }) { Text("关闭") }
                }
            )
        }

        // 音色选择弹窗
        if (showVoiceDialog) {
            AlertDialog(
                onDismissRequest = { showVoiceDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSelectingDialogueVoice) "选择对话专属音色" else "选择主音色", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { fetchOnlineVoices() }, modifier = Modifier.size(32.dp)) {
                            if (isFetchingVoices) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseTokens.CyanElectric, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "重新探测", tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!voicesFetchStatus.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (voicesFetchStatus!!.startsWith("🟢")) PulseTokens.AcidGreen.copy(alpha = 0.15f) else PulseTokens.AmberWarm.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = voicesFetchStatus!!,
                                    fontSize = 11.sp,
                                    color = if (voicesFetchStatus!!.startsWith("🟢")) PulseTokens.AcidGreen else PulseTokens.AmberWarm,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = voiceSearchQuery,
                            onValueChange = { voiceSearchQuery = it },
                            placeholder = { Text("搜索或输入自定义音色 ID...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true
                        )

                        val filtered = fetchedVoicesList.filter {
                            it.name.contains(voiceSearchQuery, ignoreCase = true) ||
                            it.id.contains(voiceSearchQuery, ignoreCase = true) ||
                            it.description.contains(voiceSearchQuery, ignoreCase = true)
                        }

                        if (filtered.isEmpty() && voiceSearchQuery.isNotBlank()) {
                            Button(
                                onClick = {
                                    if (isSelectingDialogueVoice) {
                                        dialogueVoiceId = voiceSearchQuery.trim()
                                    } else {
                                        voiceId = voiceSearchQuery.trim()
                                    }
                                    showVoiceDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric)
                            ) {
                                Text("直接使用输入值: \"$voiceSearchQuery\"", fontSize = 12.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered) { voice ->
                                val isCur = if (isSelectingDialogueVoice) dialogueVoiceId == voice.id else voiceId == voice.id
                                PulseCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelectingDialogueVoice) {
                                                dialogueVoiceId = voice.id
                                            } else {
                                                voiceId = voice.id
                                            }
                                            showVoiceDialog = false
                                        },
                                    backgroundColor = if (isCur) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                    border = if (isCur) BorderStroke(1.2.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(voice.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCur) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                            Text("${voice.id} · ${voice.gender} · ${voice.description}", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                        }
                                        if (isCur) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVoiceDialog = false }) { Text("关闭") }
                }
            )
        }
    }
}
