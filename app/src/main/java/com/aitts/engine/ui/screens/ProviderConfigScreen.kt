package com.aitts.engine.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.PresetConfigs
import com.aitts.engine.data.ProviderType
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.data.VoiceModel
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.ui.components.SectionHeader
import com.aitts.engine.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderConfigScreen(
    providerId: String,
    configDataStore: ConfigDataStore,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val providers by configDataStore.providersFlow.collectAsState()

    val initialConfig = providers.find { it.id == providerId }
        ?: PresetConfigs.createDefaultProviders().first()

    var name by remember { mutableStateOf(initialConfig.name) }
    var selectedType by remember { mutableStateOf(initialConfig.type) }
    var baseUrl by remember { mutableStateOf(initialConfig.baseUrl) }
    var apiKey by remember { mutableStateOf(initialConfig.apiKey) }
    var modelName by remember { mutableStateOf(initialConfig.modelName) }
    var voiceId by remember { mutableStateOf(initialConfig.voiceId) }
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
    var testMessage by remember { mutableStateOf<String?>(null) }

    var availableVoices by remember { mutableStateOf<List<VoiceModel>>(emptyList()) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var voiceSearchQuery by remember { mutableStateOf("") }

    fun buildCurrentConfig(): TtsProviderConfig {
        return initialConfig.copy(
            name = name,
            type = selectedType,
            baseUrl = baseUrl,
            apiKey = apiKey,
            modelName = modelName,
            voiceId = voiceId,
            promptInstruction = promptInstruction,
            speed = speed,
            pitch = pitch,
            volume = volume,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            audioFormat = audioFormat,
            customHeadersJson = customHeadersJson,
            customPayloadTemplate = customPayloadTemplate
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
                audioFormat = "mp3"
                promptInstruction = "用温柔知性的语气朗读，情感丰富细腻"
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
                name = "自定义 HTTP 节点"
                baseUrl = "http://192.168.1.100:9880/tts"
                modelName = "gpt-sovits-v2"
                voiceId = "default"
                sampleRate = "32000"
                audioFormat = "wav"
            }
        }
    }

    fun loadVoices() {
        isFetchingVoices = true
        scope.launch {
            val config = buildCurrentConfig()
            val voices = TtsProviderManager.getInstance().getAvailableVoices(config)
            availableVoices = voices
            isFetchingVoices = false
            showVoiceDialog = true
        }
    }

    if (showVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("选择官方音色 / 角色")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    OutlinedTextField(
                        value = voiceSearchQuery,
                        onValueChange = { voiceSearchQuery = it },
                        placeholder = { Text("搜索音色名称 / ID / 风格...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredVoices = availableVoices.filter {
                        it.name.contains(voiceSearchQuery, ignoreCase = true) ||
                        it.id.contains(voiceSearchQuery, ignoreCase = true) ||
                        it.description.contains(voiceSearchQuery, ignoreCase = true)
                    }

                    if (filteredVoices.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("未找到匹配音色", color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredVoices) { voice ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            voiceId = voice.id
                                            showVoiceDialog = false
                                            Toast.makeText(context, "已选择音色: ${voice.name}", Toast.LENGTH_SHORT).show()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (voiceId == voice.id) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = voice.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "${voice.gender} · ${voice.locale}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "ID: ${voice.id}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        if (voice.description.isNotBlank()) {
                                            Text(
                                                text = voice.description,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑 AI 模型配置", fontWeight = FontWeight.Bold) },
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
                        Icon(Icons.Default.Check, contentDescription = "保存", tint = PrimaryBlue)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionHeader(title = "基础信息")

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Provider Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = isTypeMenuExpanded,
                    onExpandedChange = { isTypeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("服务提供商类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeMenuExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
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
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("自动重置为【${selectedType.displayName}】官方默认参数与地址", fontSize = 12.sp)
                }
            }

            if (selectedType.requiresApiKey || selectedType == ProviderType.CUSTOM_HTTP) {
                item {
                    SectionHeader(title = "接口鉴权与地址")

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

            item {
                SectionHeader(title = "模型与官方音色库")

                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("模型名称 (Model ID)") },
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
                        label = { Text("音色标识 (Voice ID)") },
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = { loadVoices() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        if (isFetchingVoices) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("获取音色", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 大模型导演模式 / 提示词控制专区
            if (selectedType == ProviderType.MIMO || selectedType == ProviderType.GEMINI || selectedType == ProviderType.SILICONFLOW || selectedType == ProviderType.OPENAI || selectedType == ProviderType.CUSTOM_HTTP) {
                item {
                    SectionHeader(title = "大模型导演模式 / 提示词控制 (Director Mode)")

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("提示词指令控制语气、情感与语速", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "MiMo 等大模型通过自然语言指导朗读。滑动条的语速音调将自动与下方提示词融合，向大模型下发高质量导演指令。",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = promptInstruction,
                                onValueChange = { promptInstruction = it },
                                label = { Text("自定义情感 / 场景提示词 (Prompt)") },
                                placeholder = { Text("例如：用温柔知性的语气朗读，情感细腻，适合言情小说...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("常用预设风格快捷标签:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AssistChip(
                                    onClick = { promptInstruction = "用温柔知性的语气朗读，情感丰富细腻，适合有声小说" },
                                    label = { Text("📖 有声小说·温婉", fontSize = 11.sp) }
                                )
                                AssistChip(
                                    onClick = { promptInstruction = "用悬疑小说旁白语气，沉稳低沉，富有戏剧张力与压迫感" },
                                    label = { Text("🔮 悬疑玄幻·沉稳", fontSize = 11.sp) }
                                )
                                AssistChip(
                                    onClick = { promptInstruction = "语速偏快，充满青春活力与朝气，欢快清脆" },
                                    label = { Text("⚡ 紧凑快读·生动", fontSize = 11.sp) }
                                )
                                AssistChip(
                                    onClick = { promptInstruction = "语速缓慢从容，语调温和轻柔，富有治愈与放松感" },
                                    label = { Text("🧘 徐缓从容·治愈", fontSize = 11.sp) }
                                )
                                AssistChip(
                                    onClick = { promptInstruction = "标准新闻广播腔，字正腔圆，严肃端庄" },
                                    label = { Text("🎙️ 标准播音·端庄", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = "音频与发音微调")

                val speedLabel = when {
                    speed <= 0.7f -> "极慢 (指令: 从容徐缓，字正腔圆)"
                    speed <= 0.85f -> "稍慢 (指令: 沉稳从容)"
                    speed >= 1.35f -> "较快 (指令: 紧凑流畅)"
                    speed >= 1.15f -> "稍快 (指令: 轻快生动)"
                    else -> "正常 (100%)"
                }
                Text(
                    text = "语速: ${(speed * 100).toInt()}% · $speedLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = speed,
                    onValueChange = { speed = it },
                    valueRange = 0.5f..2.0f
                )

                val pitchLabel = when {
                    pitch <= 0.85f -> "低沉 (指令: 浑厚低沉带磁性)"
                    pitch >= 1.15f -> "高昂 (指令: 清脆高亢明亮)"
                    else -> "正常 (100%)"
                }
                Text(
                    text = "音调: ${(pitch * 100).toInt()}% · $pitchLabel",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = pitch,
                    onValueChange = { pitch = it },
                    valueRange = 0.5f..1.5f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sampleRate,
                        onValueChange = { sampleRate = it },
                        label = { Text("采样率 (Hz)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = audioFormat,
                        onValueChange = { audioFormat = it },
                        label = { Text("音频格式") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (selectedType == ProviderType.CUSTOM_HTTP) {
                item {
                    SectionHeader(title = "自定义模板引擎参数")

                    OutlinedTextField(
                        value = customHeadersJson,
                        onValueChange = { customHeadersJson = it },
                        label = { Text("自定义 Headers (JSON)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customPayloadTemplate,
                        onValueChange = { customPayloadTemplate = it },
                        label = { Text("自定义 Request Body 模板 (\${text}, \${voice}, \${speed})") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        isTesting = true
                        testMessage = null
                        scope.launch {
                            val config = buildCurrentConfig()
                            val result = TtsProviderManager.getInstance().synthesize("测试当前大模型配置与导演模式语音合成", config)
                            isTesting = false
                            if (result.isSuccess) {
                                testMessage = "✅ 连接并合成成功！音频大小: ${result.getOrNull()?.size} 字节"
                            } else {
                                testMessage = "❌ 合成失败: ${result.exceptionOrNull()?.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在测试连接与合成...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("测试此模型配置")
                    }
                }

                if (testMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = testMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (testMessage!!.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
