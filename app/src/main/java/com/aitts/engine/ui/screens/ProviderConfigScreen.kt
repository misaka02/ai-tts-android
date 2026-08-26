package com.aitts.engine.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.BrandTheme
import kotlinx.coroutines.launch

/**
 * 🎛️ 模型参数高级配置工作台 (v2.9.0 左右滑动手势 + 3大Tab分类架构)
 * 1. 🔌 基础连接与鉴权 (Base URL / API Key / 厂商选择 / 标签)
 * 2. 🎙️ 音色与声学参数 (主音色表 / 语速 / 音调 / 格式 / 采样率 / MiMo模式)
 * 3. 🎭 角色与高级调度 (双角色分流 / 导演词 / 自定义模板 / 故障降级)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ProviderConfigScreen(
    providerId: String,
    configDataStore: ConfigDataStore,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val providers by configDataStore.providersFlow.collectAsState()
    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    val initialConfig = providers.find { it.id == providerId }
        ?: PresetConfigs.createDefaultProviders().first()

    var name by remember { mutableStateOf(initialConfig.name) }
    var selectedType by remember { mutableStateOf(initialConfig.type) }
    var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var modelName by remember { mutableStateOf(initialConfig.modelName) }
    var voiceId by remember { mutableStateOf(initialConfig.voiceId) }
    var dialogueVoiceId by remember { mutableStateOf(initialConfig.dialogueVoiceId) }
    var isDualRoleEnabled by remember { mutableStateOf(initialConfig.isDualRoleEnabled) }
    var isPickingDialogueVoice by remember { mutableStateOf(false) }
    var promptInstruction by remember { mutableStateOf(initialConfig.promptInstruction) }
    var speed by remember { mutableFloatStateOf(initialConfig.speed) }
    var pitch by remember { mutableFloatStateOf(initialConfig.pitch) }
    var volume by remember { mutableFloatStateOf(initialConfig.volume) }
    var sampleRate by remember { mutableStateOf(initialConfig.sampleRate.toString()) }
    var audioFormat by remember { mutableStateOf(initialConfig.audioFormat) }
    var customHeadersJson by remember { mutableStateOf(initialConfig.customHeadersJson) }
    var customPayloadTemplate by remember { mutableStateOf(initialConfig.customPayloadTemplate) }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var isFetchingVoices by remember { mutableStateOf(false) }

    var fallbackProviderId by remember { mutableStateOf(initialConfig.fallbackProviderId ?: "") }
    var fallbackExpanded by remember { mutableStateOf(false) }
    var maleVoiceId by remember { mutableStateOf(initialConfig.maleVoiceId) }
    var femaleVoiceId by remember { mutableStateOf(initialConfig.femaleVoiceId) }
    var elderVoiceId by remember { mutableStateOf(initialConfig.elderVoiceId) }
    var tags by remember { mutableStateOf(initialConfig.tags) }
    var newTagInput by remember { mutableStateOf("") }

    val allProviders by configDataStore.providersFlow.collectAsState()

    var availableVoices by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceSearchQuery by remember { mutableStateOf("") }
    var previewingVoiceId by remember { mutableStateOf<String?>(null) }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val brandColor = BrandTheme.getColorForType(selectedType)

    fun buildCurrentConfig(): TtsProviderConfig {
        return initialConfig.copy(
            name = name,
            type = selectedType,
            baseUrl = baseUrl,
            apiKey = apiKey,
            modelName = modelName,
            voiceId = voiceId,
            dialogueVoiceId = dialogueVoiceId,
            isDualRoleEnabled = isDualRoleEnabled,
            promptInstruction = promptInstruction,
            speed = speed,
            pitch = pitch,
            volume = volume,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            audioFormat = audioFormat,
            customHeadersJson = customHeadersJson,
            customPayloadTemplate = customPayloadTemplate,
            fallbackProviderId = fallbackProviderId.ifBlank { null },
            maleVoiceId = maleVoiceId,
            femaleVoiceId = femaleVoiceId,
            elderVoiceId = elderVoiceId,
            tags = tags
        )
    }

    fun applyOfficialDefaults(type: ProviderType) {
        when (type) {
            ProviderType.MIMO -> {
                name = "小米 MiMo-V2.5-TTS 大模型"
                baseUrl = "https://api.xiaomimimo.com/v1/chat/completions"
                modelName = "mimo-v2.5-tts"
                voiceId = "茉莉"
                sampleRate = "24000"
                audioFormat = "pcm16"
                promptInstruction = "用温柔知性的语气朗读，情感丰富自然"
            }
            ProviderType.MINIMAX -> {
                name = "MiniMax Speech-02 (海螺语音)"
                baseUrl = "https://api.minimax.chat/v1/t2a_v2"
                modelName = "speech-02-turbo"
                voiceId = "male-qn-qingse"
                sampleRate = "32000"
                audioFormat = "mp3"
            }
            ProviderType.DOUBAO -> {
                name = "火山豆包大模型 (思思主播)"
                baseUrl = "https://openspeech.bytedance.com/api/v1/tts"
                modelName = "volcano_tts"
                voiceId = "zh_female_shuangkuaisisi_moon_bigtts"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.SILICONFLOW -> {
                name = "硅基流动 (CosyVoice2-0.5B)"
                baseUrl = "https://api.siliconflow.cn/v1/audio/speech"
                modelName = "FunAudioLLM/CosyVoice2-0.5B"
                voiceId = "FunAudioLLM/CosyVoice2-0.5B:alex"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.STEPFUN -> {
                name = "阶跃星辰 (Step-Audio 2.5)"
                baseUrl = "https://api.stepfun.com/v1/audio/speech"
                modelName = "stepaudio-2.5-tts"
                voiceId = "cixingnansheng"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.FISH_AUDIO -> {
                name = "Fish Audio (鱼音官方)"
                baseUrl = "https://api.fish.audio/v1/tts"
                modelName = "speech-v1.4"
                voiceId = "7f92f8afb8ec43bf81429cc1c9199cb1"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.EDGE_TTS -> {
                name = "Edge TTS (晓晓 - 微软大模型)"
                baseUrl = ""
                modelName = "edge-neural"
                voiceId = "zh-CN-XiaoxiaoNeural"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.OPENAI -> {
                name = "OpenAI (TTS-1 / GPT-4o)"
                baseUrl = "https://api.openai.com/v1/audio/speech"
                modelName = "tts-1"
                voiceId = "nova"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.AZURE -> {
                name = "Azure 认知语音 (eastasia)"
                baseUrl = "https://eastasia.tts.speech.microsoft.com/cognitiveservices/v1"
                modelName = "azure-neural"
                voiceId = "zh-CN-XiaoxiaoNeural"
                sampleRate = "24000"
                audioFormat = "mp3"
            }
            ProviderType.GEMINI -> {
                name = "Google Gemini 2.5 Flash TTS"
                baseUrl = "https://generativelanguage.googleapis.com/v1beta"
                modelName = "gemini-2.5-flash-preview-tts"
                voiceId = "Puck"
                sampleRate = "24000"
                audioFormat = "wav"
                promptInstruction = "Please read the text aloud clearly and naturally with appropriate emotions."
            }
            ProviderType.CUSTOM_HTTP -> {
                name = "自定义 GPT-SoVITS 节点"
                baseUrl = "http://192.168.1.100:9880/tts"
                modelName = "gpt-sovits-v2"
                voiceId = "default"
                sampleRate = "32000"
                audioFormat = "wav"
            }
            ProviderType.OFFLINE_VITS -> {
                name = "微软离线自然语音 (晓晓)"
                modelName = "ms-offline-xiaoxiao"
                voiceId = "zh-CN-XiaoxiaoOffline"
                sampleRate = "24000"
                audioFormat = "wav"
            }
        }
        Toast.makeText(context, "已套用 ${type.displayName} 标准模板", Toast.LENGTH_SHORT).show()
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
                availableVoices = list
                showVoiceDialog = true
            } catch (e: Exception) {
                Toast.makeText(context, "获取音色列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isFetchingVoices = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(name.ifBlank { "配置 AI 模型" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text("${selectedType.displayName} · 左右滑动切换选项", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val updated = buildCurrentConfig()
                        configDataStore.updateProvider(updated)
                        Toast.makeText(context, "配置已保存", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = brandColor)
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 4.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (isTesting) {
                                audioPlayer.stop()
                                isTesting = false
                            } else {
                                isTesting = true
                                scope.launch {
                                    try {
                                        val testConfig = buildCurrentConfig()
                                        val res = TtsProviderManager.getInstance().synthesize("您好，正在为您试听 ${testConfig.name} 的语音合成效果。", testConfig)
                                        if (res.isSuccess) {
                                            audioPlayer.playAudio(res.getOrThrow())
                                        } else {
                                            Toast.makeText(context, "试听失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "试听异常: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isTesting = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("试听中...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("试听当前参数", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            val updated = buildCurrentConfig()
                            configDataStore.updateProvider(updated)
                            Toast.makeText(context, "配置已成功保存！", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存配置", fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 14.dp)
        ) {
            val tabs = listOf(
                "🔌 基础连接",
                "🎙️ 音色声学",
                "🎭 角色高级"
            )

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = brandColor,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.5.sp,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (page) {
                        0 -> {
                            // Tab 0: 🔌 基础连接与鉴权
                            item(contentType = "model_type") {
                                SectionHeader(title = "模型类型与官方预设")

                                ExposedDropdownMenuBox(
                                    expanded = isTypeMenuExpanded,
                                    onExpandedChange = { isTypeMenuExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedType.displayName,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("AI 语音厂商 / 协议") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = isTypeMenuExpanded,
                                        onDismissRequest = { isTypeMenuExpanded = false }
                                    ) {
                                        ProviderType.values().forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type.displayName) },
                                                onClick = {
                                                    selectedType = type
                                                    isTypeMenuExpanded = false
                                                    applyOfficialDefaults(type)
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = { applyOfficialDefaults(selectedType) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("一键填入 ${selectedType.displayName} 官方官方标准端点与默认配置", fontSize = 11.5.sp)
                                }
                            }

                            item(contentType = "basic_info") {
                                SectionHeader(title = "配置显示名称")

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("配置显示名称 (如: 小米茉莉 / 微软晓晓)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (selectedType.requiresApiKey || selectedType == ProviderType.CUSTOM_HTTP) {
                                item(contentType = "auth_info") {
                                    SectionHeader(title = "接口鉴权与端点地址")

                                    OutlinedTextField(
                                        value = baseUrl,
                                        onValueChange = { baseUrl = it },
                                        label = { Text("API Base URL / 官方接口地址") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = apiKey,
                                        onValueChange = { apiKey = it },
                                        label = { Text("API Key / Access Token") },
                                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            item(contentType = "tags_section") {
                                SectionHeader(title = "分类标签 (Tags)")

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    tags.forEach { tag ->
                                        InputChip(
                                            selected = false,
                                            onClick = { tags = tags - tag },
                                            label = { Text(tag, fontSize = 11.sp) },
                                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "删除", modifier = Modifier.size(12.dp)) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = newTagInput,
                                        onValueChange = { newTagInput = it },
                                        placeholder = { Text("添加标签 (如: 男声 / 播客 / 角色)...", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        singleLine = true
                                    )
                                    Button(
                                        onClick = {
                                            if (newTagInput.isNotBlank() && !tags.contains(newTagInput.trim())) {
                                                tags = tags + newTagInput.trim()
                                                newTagInput = ""
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("添加", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Tab 1: 🎙️ 音色与声学参数
                            item(contentType = "model_voice") {
                                SectionHeader(title = "模型代号与发音人")

                                OutlinedTextField(
                                    value = modelName,
                                    onValueChange = { modelName = it },
                                    label = { Text("模型名称 (Model Identifier)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = voiceId,
                                        onValueChange = { voiceId = it },
                                        label = { Text("主音色 ID / Speaker") },
                                        modifier = Modifier.weight(1f)
                                    )

                                    Button(
                                        onClick = {
                                            isPickingDialogueVoice = false
                                            fetchOnlineVoices()
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("音色表", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (selectedType == ProviderType.MIMO) {
                                item(contentType = "mimo_workshop") {
                                    SectionHeader(title = "小米 MiMo 模式工作台")

                                    var mimoMode by remember(modelName) {
                                        mutableStateOf(
                                            when {
                                                modelName.contains("voicedesign") -> "voicedesign"
                                                modelName.contains("voiceclone") -> "voiceclone"
                                                else -> "standard"
                                            }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = mimoMode == "standard",
                                            onClick = {
                                                mimoMode = "standard"
                                                modelName = "mimo-v2.5-tts"
                                            },
                                            label = { Text("🎙️ 标准合成", fontSize = 11.5.sp) }
                                        )
                                        FilterChip(
                                            selected = mimoMode == "voicedesign",
                                            onClick = {
                                                mimoMode = "voicedesign"
                                                modelName = "mimo-v2.5-tts-voicedesign"
                                                if (promptInstruction.isBlank() || promptInstruction.contains("温柔知性")) {
                                                    promptInstruction = "一位22岁的江南温婉女子，声音轻柔甜美、带有一点点水乡软糯感，语速舒缓治愈"
                                                }
                                            },
                                            label = { Text("✨ 自然语言音色设计", fontSize = 11.5.sp) }
                                        )
                                    }
                                }
                            }

                            if (selectedType == ProviderType.GEMINI || selectedType == ProviderType.MIMO) {
                                item(contentType = "prompt_instruction") {
                                    SectionHeader(title = "AI 导演情感提示词 (Prompt Instruction)")

                                    OutlinedTextField(
                                        value = promptInstruction,
                                        onValueChange = { promptInstruction = it },
                                        label = { Text("语气指导 / 场景设定") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                }
                            }

                            item(contentType = "acoustics_sliders") {
                                SectionHeader(title = if (selectedType == ProviderType.EDGE_TTS || selectedType == ProviderType.AZURE) "声学微调滑杆 (语速 / 音调 / 音量)" else "声学微调滑杆 (语速 / 音量)")

                                Text(text = "默认语速: ${"%.2f".format(speed)}x", style = MaterialTheme.typography.bodyMedium)
                                Slider(
                                    value = speed,
                                    onValueChange = { speed = (it * 10).toInt() / 10f },
                                    valueRange = 0.5f..2.5f,
                                    steps = 19
                                )

                                if (selectedType == ProviderType.EDGE_TTS || selectedType == ProviderType.AZURE) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "基频音调 (Pitch): ${"%.2f".format(pitch)}x (微软原生 SSML 调音)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Slider(
                                        value = pitch,
                                        onValueChange = { pitch = (it * 10).toInt() / 10f },
                                        valueRange = 0.5f..2.0f,
                                        steps = 14
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "音量增益 (Volume): ${"%.2f".format(volume)}x", style = MaterialTheme.typography.bodyMedium)
                                Slider(
                                    value = volume,
                                    onValueChange = { volume = (it * 10).toInt() / 10f },
                                    valueRange = 0.5f..2.0f,
                                    steps = 14
                                )
                            }

                            item(contentType = "audio_format") {
                                SectionHeader(title = "采样率与编码格式")

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sampleRate,
                                        onValueChange = { sampleRate = it },
                                        label = { Text("采样率 (Hz)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = audioFormat,
                                        onValueChange = { audioFormat = it },
                                        label = { Text("音频格式 (mp3/wav/pcm16)") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        2 -> {
                            // Tab 2: 🎭 角色与高级调度
                            item(contentType = "dual_role") {
                                SectionHeader(title = "小说双角色分流 (旁白 vs 对话)")

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("启用旁白与对白双音色分流", fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "正文旁白使用主音色，引述双引号对话自动切换至独立对话音色",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Switch(
                                                checked = isDualRoleEnabled,
                                                onCheckedChange = { isDualRoleEnabled = it }
                                            )
                                        }

                                        if (isDualRoleEnabled) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = dialogueVoiceId,
                                                    onValueChange = { dialogueVoiceId = it },
                                                    label = { Text("对话专属音色 ID") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Button(
                                                    onClick = {
                                                        isPickingDialogueVoice = true
                                                        fetchOnlineVoices()
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("选音色", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(contentType = "failover_target") {
                                SectionHeader(title = "专属故障降级备用引擎")

                                val fallbackProvider = allProviders.find { it.id == fallbackProviderId }

                                ExposedDropdownMenuBox(
                                    expanded = fallbackExpanded,
                                    onExpandedChange = { fallbackExpanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = fallbackProvider?.name ?: "跟随全局默认备用引擎",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("指定故障时的备用引擎") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fallbackExpanded) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = fallbackExpanded,
                                        onDismissRequest = { fallbackExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("跟随全局默认备用引擎") },
                                            onClick = {
                                                fallbackProviderId = ""
                                                fallbackExpanded = false
                                            }
                                        )
                                        allProviders.filter { it.id != providerId }.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("${p.name} (${p.type.displayName})") },
                                                onClick = {
                                                    fallbackProviderId = p.id
                                                    fallbackExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedType == ProviderType.CUSTOM_HTTP) {
                                item(contentType = "custom_templates") {
                                    SectionHeader(title = "自定义 HTTP 模板与请求头")

                                    OutlinedTextField(
                                        value = customPayloadTemplate,
                                        onValueChange = { customPayloadTemplate = it },
                                        label = { Text("请求体 JSON 模板") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 4
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = customHeadersJson,
                                        onValueChange = { customHeadersJson = it },
                                        label = { Text("自定义 Headers (JSON 格式)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }
        }
    }

    if (showVoiceDialog) {
        val filteredVoices = availableVoices.filter {
            voiceSearchQuery.isBlank() ||
                    it.name.contains(voiceSearchQuery, ignoreCase = true) ||
                    it.id.contains(voiceSearchQuery, ignoreCase = true) ||
                    it.description.contains(voiceSearchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = {
                audioPlayer.stop()
                previewingVoiceId = null
                showVoiceDialog = false
            },
            title = { Text(if (isPickingDialogueVoice) "选择对话专属音色" else "选择主发音人音色") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = voiceSearchQuery,
                        onValueChange = { voiceSearchQuery = it },
                        placeholder = { Text("搜索音色名称 / 性别 / 风格...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filteredVoices.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("未找到符合条件的音色", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        } else {
                            items(filteredVoices) { voice ->
                                val isCurrentSelected = if (isPickingDialogueVoice) dialogueVoiceId == voice.id else voiceId == voice.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isPickingDialogueVoice) {
                                                dialogueVoiceId = voice.id
                                                Toast.makeText(context, "已选择对话音色: ${voice.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                voiceId = voice.id
                                                Toast.makeText(context, "已选择主音色: ${voice.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            audioPlayer.stop()
                                            previewingVoiceId = null
                                            showVoiceDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = voice.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp
                                                )
                                                Text(
                                                    text = "${voice.gender} · ${voice.locale}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = "ID: ${voice.id}",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (previewingVoiceId == voice.id) {
                                                    audioPlayer.stop()
                                                    previewingVoiceId = null
                                                } else {
                                                    previewingVoiceId = voice.id
                                                    scope.launch {
                                                        try {
                                                            val testConfig = buildCurrentConfig().copy(voiceId = voice.id)
                                                            val res = TtsProviderManager.getInstance().synthesize("您好，这是${voice.name}的试听效果。", testConfig)
                                                            if (res.isSuccess) {
                                                                audioPlayer.playAudio(res.getOrThrow())
                                                            } else {
                                                                Toast.makeText(context, "试听失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "试听异常: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        } finally {
                                                            previewingVoiceId = null
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            if (previewingVoiceId == voice.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "试听", tint = brandColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    audioPlayer.stop()
                    previewingVoiceId = null
                    showVoiceDialog = false
                }) {
                    Text("关闭")
                }
            }
        )
    }
}
