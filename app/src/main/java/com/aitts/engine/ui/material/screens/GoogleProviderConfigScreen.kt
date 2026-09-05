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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * 🛠️ Google 官方应用风格 - 服务商详细参数配置页 (Google Provider Config Screen)
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
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig.baseUrl) }
    var modelName by remember(currentConfig) { mutableStateOf(currentConfig.modelName) }
    var voiceId by remember(currentConfig) { mutableStateOf(currentConfig.voiceId) }
    var speed by remember(currentConfig) { mutableFloatStateOf(currentConfig.speed) }
    var pitch by remember(currentConfig) { mutableFloatStateOf(currentConfig.pitch) }
    var sampleRate by remember(currentConfig) { mutableStateOf(currentConfig.sampleRate.toString()) }
    var isStreamingEnabled by remember(currentConfig) { mutableStateOf(currentConfig.isStreamingEnabled) }
    var testPhrase by remember { mutableStateOf("您好，正在为您试听当前配置的语音合成效果。") }

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    fun saveConfig() {
        val updated = currentConfig.copy(
            name = name.trim(),
            apiKey = apiKey.trim(),
            baseUrl = baseUrl.trim(),
            modelName = modelName.trim(),
            voiceId = voiceId.trim(),
            speed = speed,
            pitch = pitch,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            isStreamingEnabled = isStreamingEnabled
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
            name = name.trim(),
            apiKey = apiKey.trim(),
            baseUrl = baseUrl.trim(),
            modelName = modelName.trim(),
            voiceId = voiceId.trim(),
            speed = speed,
            pitch = pitch,
            sampleRate = sampleRate.toIntOrNull() ?: 24000,
            isStreamingEnabled = isStreamingEnabled
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
                    text = "编辑服务商 · ${currentConfig.type.displayName}",
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本参数卡片
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

                        if (currentConfig.type.requiresApiKey) {
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                label = { Text("API Key / 访问凭证") },
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
                        }

                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it },
                            label = { Text("服务端接口端点 (Base URL)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )
                    }
                }
            }

            // 模型与音色卡片
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("模型与音色配置", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)

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
                            label = { Text("音色标识 (Voice ID / 音色名称)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("流式传输 (Streaming)", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                Text("低首字延迟播放", fontSize = 11.sp, color = colors.textSecondary)
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

            // 语速与音调参数微调
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineSubtle)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("原生发音参数", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.primary)

                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("语速 (Speed)", fontSize = 13.sp, color = colors.textPrimary)
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
                                Text("音调 (Pitch)", fontSize = 13.sp, color = colors.textPrimary)
                                Text(String.format("%.2fx", pitch), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                            }
                            Slider(
                                value = pitch,
                                onValueChange = { pitch = it },
                                valueRange = 0.5f..1.5f,
                                colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary)
                            )
                        }
                    }
                }
            }

            // 试听卡片
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
}
