package com.aitts.engine.ui.pulse.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    var modelName by remember(initialConfig) { mutableStateOf(initialConfig.modelName) }
    var voiceId by remember(initialConfig) { mutableStateOf(initialConfig.voiceId) }
    var dialogueVoiceId by remember(initialConfig) { mutableStateOf(initialConfig.dialogueVoiceId) }
    var isDualRoleEnabled by remember(initialConfig) { mutableStateOf(initialConfig.isDualRoleEnabled) }
    var speed by remember(initialConfig) { mutableFloatStateOf(initialConfig.speed) }
    var pitch by remember(initialConfig) { mutableFloatStateOf(initialConfig.pitch) }
    var sampleRate by remember(initialConfig) { mutableStateOf(initialConfig.sampleRate.toString()) }
    var audioFormat by remember(initialConfig) { mutableStateOf(initialConfig.audioFormat) }
    var customPayload by remember(initialConfig) { mutableStateOf(initialConfig.customPayloadTemplate) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTestingAudio by remember { mutableStateOf(false) }

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.stop() }
    }

    var isFetchingVoices by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var isSelectingDialogueVoice by remember { mutableStateOf(false) }
    var voiceSearchQuery by remember { mutableStateOf("") }
    var fetchedVoicesList by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }

    fun buildCurrentConfig(): TtsProviderConfig {
        return initialConfig.copy(
            name = name.trim().ifBlank { selectedType.displayName },
            type = selectedType,
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            modelName = modelName.trim(),
            voiceId = voiceId.trim(),
            dialogueVoiceId = dialogueVoiceId.trim(),
            isDualRoleEnabled = isDualRoleEnabled,
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
                baseUrl = "https://api.minimax.chat/v1/t2a_v2"
                modelName = "speech-02-turbo"
                voiceId = "male-qn-qingse"
                sampleRate = "32000"
                audioFormat = "mp3"
            }
            ProviderType.DOUBAO -> {
                baseUrl = "https://openspeech.bytedance.com/api/v1/tts"
                modelName = "volcano_tts"
                voiceId = "zh_female_shuangkuaisisi_moon_bigtts"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.EDGE_TTS -> {
                baseUrl = ""
                modelName = "edge-neural"
                voiceId = "zh-CN-XiaoxiaoNeural"
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
                baseUrl = "https://generativelanguage.googleapis.com/v1beta"
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
                val list = when (selectedType) {
                    ProviderType.EDGE_TTS -> PresetConfigs.edgeVoices
                    ProviderType.MIMO -> PresetConfigs.mimoVoices
                    ProviderType.MINIMAX -> PresetConfigs.minimaxVoices
                    ProviderType.GEMINI -> PresetConfigs.geminiVoices
                    ProviderType.DOUBAO -> PresetConfigs.doubaoVoices
                    ProviderType.SILICONFLOW -> PresetConfigs.siliconFlowVoices
                    ProviderType.STEPFUN -> PresetConfigs.stepFunVoices
                    ProviderType.FISH_AUDIO -> PresetConfigs.fishAudioVoices
                    ProviderType.OPENAI -> PresetConfigs.openAiVoices
                    else -> TtsProviderManager.getInstance().getAvailableVoices(current)
                }
                fetchedVoicesList = list
                showVoiceDialog = true
                Toast.makeText(context, "已获取到 ${list.size} 款音色", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "获取音色失败: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isFetchingVoices = false
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
                        audioPlayer.playAudioBytes(bytes, onCompletion = { isTestingAudio = false })
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = PulseTokens.TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { applyOfficialDefaults(selectedType) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                        border = PulseTokens.BorderSubtle
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("套用预设", fontSize = 11.5.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PulseTokens.CanvasDeep)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 引擎与服务凭证
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("🔌 引擎与连接凭证", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(ProviderType.values()) { type ->
                                    val isSelected = selectedType == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { applyOfficialDefaults(type) },
                                        label = { Text(type.displayName, fontSize = 11.5.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PulseTokens.CyanElectric,
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
                                    label = { Text("API Key 密钥") },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                                )
                            }
                        }
                    }
                }

                // 2. 音色与倍速声学
                item {
                    PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("🎙️ 音色与发音倍速", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.TextPrimary)

                            OutlinedTextField(
                                value = modelName,
                                onValueChange = { modelName = it },
                                label = { Text("模型 ID (Model Name)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseTokens.CyanElectric)
                            )

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
                        }
                    }
                }

                // 3. 双角色对白分流与高级设置
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
            }
        }

        // 右下角大拇指悬浮操作岛 [ 🎙️ 试听 | 💾 保存 ]
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = PulseTokens.SurfaceDark.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.6f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { testAudioPlayback() },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTestingAudio) PulseTokens.MagentaLaser else PulseTokens.SurfaceElevated,
                        contentColor = if (isTestingAudio) Color.White else PulseTokens.CyanElectric
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(if (isTestingAudio) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTestingAudio) "停止" else "试听", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val updated = buildCurrentConfig()
                        configDataStore.updateProvider(updated)
                        Toast.makeText(context, "已保存模型: ${updated.name}", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 音色选择弹窗
        if (showVoiceDialog) {
            AlertDialog(
                onDismissRequest = { showVoiceDialog = false },
                title = { Text(if (isSelectingDialogueVoice) "选择对话专属音色" else "选择主音色", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = voiceSearchQuery,
                            onValueChange = { voiceSearchQuery = it },
                            placeholder = { Text("搜索音色关键字...") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true
                        )

                        val filtered = fetchedVoicesList.filter {
                            it.name.contains(voiceSearchQuery, ignoreCase = true) ||
                            it.id.contains(voiceSearchQuery, ignoreCase = true) ||
                            it.description.contains(voiceSearchQuery, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filtered) { voice ->
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
                                    backgroundColor = PulseTokens.SurfaceElevated,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(voice.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                        Text("${voice.id} · ${voice.gender} · ${voice.description}", fontSize = 11.sp, color = PulseTokens.TextSecondary)
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
