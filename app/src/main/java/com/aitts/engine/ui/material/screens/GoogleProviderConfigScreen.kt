package com.aitts.engine.ui.material.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.material.GoogleColors
import kotlinx.coroutines.launch

/**
 * 🛠️ Google 官方应用风格 - 服务商全功能精细参数配置页 (Google Provider Config Screen)
 * 适配全量高级参数：
 * 1. 官方预设模板一键快速套用；
 * 2. 主备 API Key、自定义 Headers 与 HTTP 请求体模板；
 * 3. 在线拉取音色列表与模型列表；
 * 4. 小说双角色对白分流（旁白音色 + 对白音色）；
 * 5. 大模型导演情感提示词 (Prompt Instruction)；
 * 6. 原生语速/音调微调、音频格式 (mp3/wav/pcm/opus) 与采样率。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleProviderConfigScreen(
    providerId: String,
    configDataStore: ConfigDataStore,
    colors: GoogleColors,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val providers by configDataStore.providersFlow.collectAsState()

    val currentConfig = remember(providerId, providers) {
        providers.find { it.id == providerId } ?: TtsProviderConfig(
            id = providerId,
            type = ProviderType.EDGE_TTS,
            name = "未命名服务商"
        )
    }

    var name by remember(currentConfig) { mutableStateOf(currentConfig.name) }
    var selectedType by remember(currentConfig) { mutableStateOf(currentConfig.type) }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var secondaryApiKey by remember(currentConfig) { mutableStateOf(currentConfig.secondaryApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isSecApiKeyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig.baseUrl) }
    var modelName by remember(currentConfig) { mutableStateOf(currentConfig.modelName) }
    var voiceId by remember(currentConfig) { mutableStateOf(currentConfig.voiceId) }
    var dialogueVoiceId by remember(currentConfig) { mutableStateOf(currentConfig.dialogueVoiceId) }
    var isDualRoleEnabled by remember(currentConfig) { mutableStateOf(currentConfig.isDualRoleEnabled) }
    var promptInstruction by remember(currentConfig) { mutableStateOf(currentConfig.promptInstruction) }
    var speed by remember(currentConfig) { mutableFloatStateOf(currentConfig.speed) }
    var pitch by remember(currentConfig) { mutableFloatStateOf(currentConfig.pitch) }
    var sampleRate by remember(currentConfig) { mutableStateOf(currentConfig.sampleRate.toString()) }
    var audioFormat by remember(currentConfig) { mutableStateOf(currentConfig.audioFormat) }
    var isStreamingEnabled by remember(currentConfig) { mutableStateOf(currentConfig.isStreamingEnabled) }
    var customPayload by remember(currentConfig) { mutableStateOf(currentConfig.customPayloadTemplate) }
    var testPhrase by remember { mutableStateOf("您好，正在为您试听当前配置的语音合成效果。") }

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    // 在线音色拉取状态
    var isFetchingVoices by remember { mutableStateOf(false) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var isSelectingDialogueVoice by remember { mutableStateOf(false) }
    var fetchedVoices by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
    var voiceSearchQuery by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    fun applyOfficialDefaults(type: ProviderType) {
        selectedType = type
        when (type) {
            ProviderType.MIMO -> {
                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions"
                modelName = "mimo-v2.5-tts"
                voiceId = "茉莉"
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
                audioFormat = "mp3"
            }
            ProviderType.EDGE_TTS -> {
                baseUrl = ""
                modelName = ""
                voiceId = "zh-CN-XiaoxiaoNeural"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.OFFLINE_VITS -> {
                baseUrl = ""
                modelName = "vits-zh-aishell3"
                voiceId = "0"
                sampleRate = "22050"
                audioFormat = "pcm"
            }
            else -> {}
        }
        Toast.makeText(context, "已填充【${type.displayName}】预设参数", Toast.LENGTH_SHORT).show()
    }

    fun saveConfig() {
        val updated = currentConfig.copy(
            name = name.trim().ifBlank { selectedType.displayName },
            type = selectedType,
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            secondaryApiKey = secondaryApiKey.trim(),
            modelName = modelName.trim(),
            voiceId = voiceId.trim(),
            dialogueVoiceId = dialogueVoiceId.trim(),
            isDualRoleEnabled = isDualRoleEnabled,
            promptInstruction = promptInstruction.trim(),
            speed = speed,
            pitch = pitch,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            audioFormat = audioFormat.trim().ifBlank { "mp3" },
            isStreamingEnabled = isStreamingEnabled,
            customPayloadTemplate = customPayload
        )
        configDataStore.updateProvider(updated)
        Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
        onNavigateBack()
    }

    fun executeTest() {
        if (isPlaying) {
            audioPlayer.stop()
            isPlaying = false
            return
        }

        val testCfg = currentConfig.copy(
            name = name.trim().ifBlank { selectedType.displayName },
            type = selectedType,
            baseUrl = baseUrl.trim(),
            apiKey = apiKey.trim(),
            secondaryApiKey = secondaryApiKey.trim(),
            modelName = modelName.trim(),
            voiceId = voiceId.trim(),
            dialogueVoiceId = dialogueVoiceId.trim(),
            isDualRoleEnabled = isDualRoleEnabled,
            promptInstruction = promptInstruction.trim(),
            speed = speed,
            pitch = pitch,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            audioFormat = audioFormat.trim().ifBlank { "mp3" },
            isStreamingEnabled = isStreamingEnabled,
            customPayloadTemplate = customPayload
        )

        isSynthesizing = true
        scope.launch {
            try {
                val res = TtsProviderManager.getInstance().synthesize(testPhrase, testCfg, autoRetry = false)
                isSynthesizing = false
                if (res.isSuccess) {
                    val bytes = res.getOrNull() ?: ByteArray(0)
                    if (bytes.isNotEmpty()) {
                        isPlaying = true
                        audioPlayer.playAudioBytes(bytes, speed = 1.0f) {
                            isPlaying = false
                        }
                    } else {
                        Toast.makeText(context, "音频数据为空", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val msg = res.exceptionOrNull()?.message ?: "未知错误"
                    Toast.makeText(context, "试听失败: $msg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                isSynthesizing = false
                Toast.makeText(context, "异常: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Google TopAppBar
        TopAppBar(
            title = {
                Text(
                    text = "编辑 · ${selectedType.displayName}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = colors.textPrimary
                    )
                }
            },
            actions = {
                Button(
                    onClick = { saveConfig() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("保存", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 官方预设一键套用
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Text("一键套用官方接口预设", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val types = listOf(
                            ProviderType.GEMINI,
                            ProviderType.EDGE_TTS,
                            ProviderType.OPENAI,
                            ProviderType.MIMO,
                            ProviderType.MINIMAX,
                            ProviderType.DOUBAO,
                            ProviderType.OFFLINE_VITS
                        )
                        items(types) { t ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { applyOfficialDefaults(t) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedType == t) colors.primaryContainer else colors.surfaceContainer,
                                border = if (selectedType == t) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                            ) {
                                Text(
                                    text = t.displayName.split(" ")[0],
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedType == t) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedType == t) colors.onPrimaryContainer else colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. 基本信息
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("基本信息", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("服务商显示名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("服务端接口端点 (Base URL)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        if (selectedType.requiresApiKey) {
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("主 API Key (访问凭证)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                        Icon(if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = colors.textSecondary)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                            )

                            OutlinedTextField(
                                value = secondaryApiKey,
                                onValueChange = { secondaryApiKey = it },
                                label = { Text("备用 API Key (配额轮询/选填)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                visualTransformation = if (isSecApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isSecApiKeyVisible = !isSecApiKeyVisible }) {
                                        Icon(if (isSecApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = colors.textSecondary)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                            )
                        }
                    }
                }
            }

            // 2. 模型与音色
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("模型与音色配置", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)

                            // 在线拉取音色按钮
                            OutlinedButton(
                                onClick = {
                                    isFetchingVoices = true
                                    showVoiceSheet = true
                                    isSelectingDialogueVoice = false
                                    scope.launch {
                                        try {
                                            fetchedVoices = TtsProviderManager.getInstance().getAvailableVoices(
                                                currentConfig.copy(type = selectedType, baseUrl = baseUrl, apiKey = apiKey, modelName = modelName)
                                            )
                                        } catch (_: Exception) {}
                                        isFetchingVoices = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("在线选音色", fontSize = 11.5.sp)
                            }
                        }

                        OutlinedTextField(
                            value = modelName,
                            onValueChange = { modelName = it },
                            label = { Text("模型名称 (如: mimo-v2.5-tts, tts-1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        OutlinedTextField(
                            value = voiceId,
                            onValueChange = { voiceId = it },
                            label = { Text("主力/旁白音色代码 (Voice ID)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        // 小说双角色对白分流
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.surfaceContainer
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("小说双角色对白分流", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                        Text("区分旁白叙述与角色引号对白，分流不同音色", fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                    Switch(
                                        checked = isDualRoleEnabled,
                                        onCheckedChange = { isDualRoleEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
                                    )
                                }

                                if (isDualRoleEnabled) {
                                    OutlinedTextField(
                                        value = dialogueVoiceId,
                                        onValueChange = { dialogueVoiceId = it },
                                        label = { Text("角色对话专属音色代码") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                                    )
                                }
                            }
                        }

                        // 大模型导演情感提示词
                        OutlinedTextField(
                            value = promptInstruction,
                            onValueChange = { promptInstruction = it },
                            label = { Text("大模型导演情感提示词 (Prompt / 选填)") },
                            placeholder = { Text("如：声音饱满温和，充满讲故事的情感氛围...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("流式极速传输 (Streaming)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text("首字直出极低延迟", fontSize = 11.sp, color = colors.textSecondary)
                            }
                            Switch(
                                checked = isStreamingEnabled,
                                onCheckedChange = { isStreamingEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // 3. 原生发音与音频格式
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("发音参数与音频格式", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("原生倍速 (Speed)", fontSize = 13.sp, color = colors.textPrimary)
                                Text(String.format("%.2fx", speed), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Slider(
                                value = speed,
                                onValueChange = { speed = it },
                                valueRange = 0.5f..2.5f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("原生音调 (Pitch)", fontSize = 13.sp, color = colors.textPrimary)
                                Text(String.format("%.2fx", pitch), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Slider(
                                value = pitch,
                                onValueChange = { pitch = it },
                                valueRange = 0.5f..1.5f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }

                        // 音频格式选择
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("输出音频格式", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val formats = listOf("mp3", "wav", "pcm", "opus")
                                formats.forEach { fmt ->
                                    val isSel = audioFormat.equals(fmt, ignoreCase = true)
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { audioFormat = fmt },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) colors.primaryContainer else colors.surfaceContainer,
                                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                                    ) {
                                        Text(
                                            text = fmt.uppercase(),
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) colors.onPrimaryContainer else colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 采样率选择
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("输出采样率", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val rates = listOf("16000", "22050", "24000", "48000")
                                rates.forEach { r ->
                                    val isSel = sampleRate == r
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { sampleRate = r },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) colors.primaryContainer else colors.surfaceContainer,
                                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, colors.primary) else null
                                    ) {
                                        Text(
                                            text = "${r}Hz",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) colors.onPrimaryContainer else colors.textSecondary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. 自定义 HTTP 模板 (仅自定义或高级模式)
            if (selectedType == ProviderType.CUSTOM_HTTP) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("自定义 HTTP 请求体模板", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)
                            Text("支持占位符: \${text}, \${voice}, \${speed}, \${pitch}", fontSize = 11.sp, color = colors.textSecondary)

                            OutlinedTextField(
                                value = customPayload,
                                onValueChange = { customPayload = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                            )
                        }
                    }
                }
            }

            // 5. 试听卡片
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("当前配置试听", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)

                        OutlinedTextField(
                            value = testPhrase,
                            onValueChange = { testPhrase = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("试听文本") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { executeTest() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) colors.googleRed else colors.primary,
                                    contentColor = colors.onPrimary
                                )
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("合成中...")
                                } else {
                                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPlaying) "停止" else "试听当前配置")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Google 风格在线音色搜索选择抽屉
    if (showVoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoiceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "选择${if (isSelectingDialogueVoice) "角色对白" else "主力旁白"}音色",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                OutlinedTextField(
                    value = voiceSearchQuery,
                    onValueChange = { voiceSearchQuery = it },
                    placeholder = { Text("搜索音色名称或 ID...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                )

                if (isFetchingVoices) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                } else {
                    val filtered = fetchedVoices.filter {
                        voiceSearchQuery.isBlank() || it.name.contains(voiceSearchQuery, ignoreCase = true) || it.id.contains(voiceSearchQuery, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Text("未发现音色或接口不支持在线探测，可手动输入", color = colors.textTertiary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filtered) { v ->
                                val isCurrent = if (isSelectingDialogueVoice) dialogueVoiceId == v.id else voiceId == v.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelectingDialogueVoice) {
                                                dialogueVoiceId = v.id
                                            } else {
                                                voiceId = v.id
                                            }
                                            showVoiceSheet = false
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isCurrent) colors.primaryContainer else colors.surfaceContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(v.name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp, color = if (isCurrent) colors.onPrimaryContainer else colors.textPrimary)
                                            Text("${v.id} · ${v.locale} · ${v.gender}", fontSize = 11.5.sp, color = colors.textSecondary)
                                        }
                                        if (isCurrent) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
