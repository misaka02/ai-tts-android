package com.aitts.engine.ui.pulse.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aitts.engine.audio.AndroidAudioPlayer
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.audio.AudioVisualizerManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import kotlin.math.roundToInt
import com.aitts.engine.data.TtsProviderConfig
import com.aitts.engine.provider.TtsProviderManager
import com.aitts.engine.rules.QuoteService
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 📻 Vinyl 沉浸黑胶阅览中枢 (Modern Vinyl Turntable Hub)
 * 基于 Modern Design System 2.0 全新重构：
 * 1. Interactive Turntable Deck：33/45 RPM 物理转速唱片、机械唱臂落针/抬针动效；
 * 2. Analog Tuning & Acoustic Warmth：33/45 RPM 切换、模拟调谐刻度盘、黑胶温润滤镜；
 * 3. 极简语料输入与一言金句；
 * 4. Vinyl 专属大拇指单手收纳岛。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinylHubScreen(
    configDataStore: ConfigDataStore,
    onNavigateToEditProvider: (String) -> Unit,
    onOpenDeck: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()

    val activeProvider = providers.find { it.id == settings.activeProviderId }
        ?: providers.firstOrNull()
        ?: TtsProviderConfig(id = "default", type = ProviderType.EDGE_TTS, name = "默认引擎")

    val isAiModel = activeProvider.type.requiresApiKey || activeProvider.type in listOf(
        ProviderType.MIMO, ProviderType.MINIMAX, ProviderType.DOUBAO, ProviderType.SILICONFLOW,
        ProviderType.FISH_AUDIO, ProviderType.STEPFUN, ProviderType.OPENAI, ProviderType.GEMINI
    )

    val audioPlayer = remember { AndroidAudioPlayer(context) }
    DisposableEffect(Unit) {
        onDispose { audioPlayer.stop() }
    }

    var isSynthesizing by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isFetchingQuote by remember { mutableStateOf(false) }
    var lastLatencyMs by remember { mutableStateOf<Long?>(null) }
    var testText by remember { mutableStateOf("欢迎使用 Vinyl 沉浸声纹唱片舱！黑胶声学物理涟漪与模拟唱针已就绪。") }
    var quoteSourceDesc by remember { mutableStateOf<String?>("《人间词话》 · 王国维") }
    var quoteButtonText by remember { mutableStateOf("一言金句") }

    var showVoicePickerDialog by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var showBottomSheetModelPicker by remember { mutableStateOf(false) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val visualizerManager = remember { AudioVisualizerManager.getInstance() }
    val spectrumBands by visualizerManager.spectrumFlow.collectAsState()
    val rmsEnergy by visualizerManager.rmsEnergyFlow.collectAsState()
    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }

    var speechHistory by remember {
        mutableStateOf(
            listOf(
                "人生若只如初见，何事秋风悲画扇。" to "《木兰花·拟古决绝词》 · 纳兰性德",
                "行到水穷处，坐看云起时。" to "《终南别业》 · 王维",
                "醉后不知天在水，满船清梦压星河。" to "《题龙阳县青草湖》 · 唐珙"
            )
        )
    }

    var is33Rpm by remember { mutableStateOf(true) }

    val activeBrandColor = if (settings.isProviderCardAccentColorEnabled) {
        when (activeProvider.type) {
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
        PulseTokens.AmberWarm
    }

    val infiniteTransition = rememberInfiniteTransition(label = "turntable_spin")
    val turntableAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (is33Rpm) 1800 else 1333,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) 26f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "tonearm_angle"
    )

    fun startSynthesis(provider: TtsProviderConfig, customText: String? = null) {
        val targetText = (customText ?: testText).trim()
        if (targetText.isBlank()) {
            Toast.makeText(context, "请输入需要朗读的文本", Toast.LENGTH_SHORT).show()
            return
        }

        if (isPlaying || isSynthesizing) {
            audioPlayer.stop()
            isPlaying = false
            isSynthesizing = false
            return
        }

        scope.launch {
            isSynthesizing = true
            val startTime = System.currentTimeMillis()

            try {
                val effectiveSpeed = (provider.speed * settings.globalSpeed).coerceIn(0.2f, 3.0f)
                val effectivePitch = (provider.pitch * settings.globalPitch).coerceIn(0.2f, 2.0f)
                val testConfig = provider.copy(speed = effectiveSpeed, pitch = effectivePitch)

                configDataStore.log("💿 [Vinyl] 声纹唱片合成: 【${provider.name}】")
                val result = TtsProviderManager.getInstance().synthesize(targetText, testConfig)
                val costMs = System.currentTimeMillis() - startTime

                if (result.isSuccess) {
                    val audioData = result.getOrNull()
                    if (audioData != null && audioData.isNotEmpty()) {
                        lastLatencyMs = costMs
                        isSynthesizing = false
                        isPlaying = true

                        val newHistory = (listOf(targetText to (quoteSourceDesc ?: "自定义朗读")) + speechHistory).distinctBy { it.first }.take(6)
                        speechHistory = newHistory

                        audioPlayer.playAudioBytes(
                            audioData,
                            onCompletion = { isPlaying = false }
                        )
                    } else {
                        isSynthesizing = false
                        Toast.makeText(context, "未收到有效音频数据", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isSynthesizing = false
                    Toast.makeText(context, "合成失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                isSynthesizing = false
                Toast.makeText(context, "异常: ${t.message ?: t.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTokens.CanvasDeep)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ==================== 1. Vinyl 顶栏标题 ====================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PulseTokens.CyanElectric))
                            Text("VINYL 沉浸声纹阅览舱", fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, color = PulseTokens.TextPrimary)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PulseTokens.CyanElectric.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "v${com.aitts.engine.BuildConfig.VERSION_NAME}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTokens.CyanElectric,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("声纹动态唱盘 · 机械唱臂联动 · 朗读时光胶囊", fontSize = 11.sp, color = PulseTokens.SonicBlue)
                    }

                    IconButton(
                        onClick = { onNavigateToEditProvider(activeProvider.id) },
                        modifier = Modifier.clip(CircleShape).background(PulseTokens.SurfaceElevated).size(36.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "声学参数", tint = PulseTokens.CyanElectric, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // ==================== 2. Interactive Audio-Reactive Vinyl Card ====================
            item {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = PulseTokens.SurfaceDark,
                    border = BorderStroke(1.5.dp, activeBrandColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(activeBrandColor))
                                Text(activeProvider.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = activeBrandColor)
                                Surface(shape = RoundedCornerShape(4.dp), color = activeBrandColor.copy(alpha = 0.2f)) {
                                    Text(if (isPlaying) "PLAYING" else "STANDBY", fontSize = 10.sp, color = activeBrandColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }

                            Button(
                                onClick = { startSynthesis(activeProvider) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPlaying) PulseTokens.MagentaLaser else activeBrandColor,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (isSynthesizing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.Black)
                                } else {
                                    Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isPlaying) "停止" else "落针", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 唱盘与唱臂交互舞台 (声纹物理涟漪响应)
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(PulseTokens.CanvasDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. 动态声纹同心唱片
                            val amberColor = PulseTokens.AmberWarm
                            val magentaColor = PulseTokens.MagentaLaser
                            Canvas(modifier = Modifier.fillMaxSize().rotate(if (isPlaying) turntableAngle else 0f)) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = size.minDimension / 2 * 0.92f

                                // 唱片底色
                                drawCircle(color = Color(0xFF0B1120), radius = radius, center = center)

                                // 随 TTS 实时声音频段波动的动态声纹凹槽 (Waveform Grooves)
                                val grooveCount = 10
                                for (g in 1..grooveCount) {
                                    val bandEnergy = if (isPlaying && spectrumBands.isNotEmpty()) {
                                        spectrumBands.getOrElse(g * 2) { 0.05f }.coerceIn(0.02f, 1f)
                                    } else 0.04f

                                    val dynamicRadius = radius * (0.42f + (g.toFloat() / grooveCount) * 0.52f) + (bandEnergy * 3.dp.toPx())
                                    val grooveColor = if (isPlaying) {
                                        if (g % 2 == 0) activeBrandColor.copy(alpha = 0.35f + bandEnergy * 0.5f) else amberColor.copy(alpha = 0.25f + bandEnergy * 0.4f)
                                    } else {
                                        Color(0xFF1E293B).copy(alpha = 0.4f)
                                    }

                                    drawCircle(
                                        color = grooveColor,
                                        radius = dynamicRadius,
                                        center = center,
                                        style = Stroke(width = (0.9.dp.toPx() + bandEnergy * 1.5.dp.toPx()))
                                    )
                                }

                                // 唱片中心彩胶圆标 (Center Label)
                                val labelRadius = radius * 0.38f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(activeBrandColor, Color(0xFF1E1B4B)),
                                        center = center,
                                        radius = labelRadius
                                    ),
                                    radius = labelRadius,
                                    center = center
                                )
                                drawCircle(color = Color.Black, radius = 6.dp.toPx(), center = center)
                            }

                            // 2. 机械唱臂 (Tonearm) 与声能接触微光
                            val needleColor = PulseTokens.CyanElectric
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val armBase = Offset(size.width * 0.85f, size.height * 0.15f)
                                val armLength = size.width * 0.52f
                                val rad = (tonearmAngle + 45) * (PI / 180f).toFloat()
                                val armEnd = Offset(armBase.x - armLength * cos(rad), armBase.y + armLength * sin(rad))

                                // 唱臂支点金属底座
                                drawCircle(color = Color(0xFF94A3B8), radius = 9.dp.toPx(), center = armBase)
                                drawCircle(color = Color(0xFF475569), radius = 5.dp.toPx(), center = armBase)

                                // 唱臂金属杆
                                drawLine(
                                    color = Color(0xFFCBD5E1),
                                    start = armBase,
                                    end = armEnd,
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )

                                // 唱头与唱针
                                val needleEnd = Offset(armEnd.x - 6.dp.toPx() * sin(rad), armEnd.y - 6.dp.toPx() * cos(rad))
                                drawLine(
                                    color = needleColor,
                                    start = armEnd,
                                    end = needleEnd,
                                    strokeWidth = 4.dp.toPx(),
                                    cap = StrokeCap.Square
                                )

                                // 发音时唱针接触点的声能脉动微光 (Acoustic Needle Glow)
                                if (isPlaying) {
                                    drawCircle(
                                        color = activeBrandColor.copy(alpha = 0.40f + rmsEnergy * 0.50f),
                                        radius = (4.dp.toPx() + rmsEnergy * 8.dp.toPx()),
                                        center = needleEnd
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 3. 沉浸阅读时光胶囊 (Reading History Capsule) ====================
            item {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = PulseTokens.SurfaceCard
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                Text("最近朗读历史与声音胶囊", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                            }
                            Text("共 ${speechHistory.size} 条", fontSize = 11.sp, color = PulseTokens.TextTertiary)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            speechHistory.forEach { (historyText, historySource) ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            testText = historyText
                                            quoteSourceDesc = historySource
                                            startSynthesis(activeProvider, historyText)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = PulseTokens.SurfaceElevated,
                                    border = BorderStroke(1.dp, PulseTokens.SurfaceCard)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(historyText, fontSize = 11.5.sp, color = PulseTokens.TextPrimary, maxLines = 1)
                                            Text(historySource, fontSize = 10.sp, color = PulseTokens.SonicBlue, maxLines = 1)
                                        }
                                        Icon(Icons.Default.PlayArrow, contentDescription = "再次试听", tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 4. 自适应声学/Prompt 控制 ====================
            item {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = PulseTokens.SurfaceCard
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                Text(if (isAiModel) "AI 情绪指令与音色" else "传统 TTS 黑胶声学参数", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = PulseTokens.CyanElectric.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { showVoicePickerDialog = true }
                            ) {
                                Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("音色: ${activeProvider.voiceId.ifBlank { "默认" }}", fontSize = 11.sp, color = PulseTokens.CyanElectric, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(11.dp))
                                }
                            }
                        }

                        if (isAiModel) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("当前系统 Prompt 指令:", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPromptDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseTokens.SurfaceElevated,
                                    border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = if (activeProvider.promptInstruction.isNotBlank()) activeProvider.promptInstruction else "标准自然声调（点击自定义注入 AI 情绪/角色 Prompt）🎭",
                                        fontSize = 12.sp,
                                        color = PulseTokens.CyanElectric,
                                        modifier = Modifier.padding(10.dp),
                                        maxLines = 2
                                    )
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("语速与音高", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                    Text("${String.format("%.2f", activeProvider.speed)}x · ${String.format("%.2f", activeProvider.pitch)}", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PulseTokens.SurfaceElevated,
                                    modifier = Modifier.clickable { onNavigateToEditProvider(activeProvider.id) }
                                ) {
                                    Text("配置参数", fontSize = 11.sp, color = PulseTokens.CyanElectric, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ==================== 5. 文本输入与一言 ====================
            item {
                PulseCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = PulseTokens.SurfaceCard
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("黑胶朗读语料", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                            Button(
                                onClick = {
                                    if (!isFetchingQuote) {
                                        isFetchingQuote = true
                                        quoteButtonText = "获取中..."
                                        scope.launch {
                                            val quote = QuoteService.fetchOnlineHitokoto()
                                            testText = quote.text
                                            quoteSourceDesc = quote.source ?: "一言名句"
                                            isFetchingQuote = false
                                            quoteButtonText = "已载入 ✓"
                                            delay(1500)
                                            quoteButtonText = "一言金句"
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFetchingQuote) PulseTokens.AmberWarm.copy(alpha = 0.2f) else PulseTokens.SurfaceElevated,
                                    contentColor = PulseTokens.AmberWarm
                                ),
                                border = BorderStroke(1.dp, PulseTokens.SurfaceElevated),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                if (isFetchingQuote) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = PulseTokens.AmberWarm, strokeWidth = 1.5.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(Icons.Default.Casino, contentDescription = null, tint = PulseTokens.AmberWarm, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(quoteButtonText, fontSize = 11.5.sp, color = PulseTokens.AmberWarm)
                            }
                        }

                        OutlinedTextField(
                            value = testText,
                            onValueChange = { testText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = activeBrandColor,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = PulseTokens.TextPrimary,
                                unfocusedTextColor = PulseTokens.TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        if (!quoteSourceDesc.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.FormatQuote, contentDescription = null, tint = PulseTokens.AmberWarm, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "出处来源: $quoteSourceDesc",
                                    fontSize = 11.sp,
                                    color = PulseTokens.AmberWarm,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==================== 6. Vinyl 专属大拇指收纳岛 ====================
        val hubActionItems = listOf(
            ActionHubItem(
                label = "切换模型",
                icon = Icons.Default.SwapHoriz,
                color = PulseTokens.CyanElectric,
                onClick = { showBottomSheetModelPicker = true }
            ),
            ActionHubItem(
                label = "切换音色",
                icon = Icons.Default.RecordVoiceOver,
                color = PulseTokens.SonicBlue,
                onClick = { showVoicePickerDialog = true }
            ),
            ActionHubItem(
                label = "一言金句",
                icon = Icons.Default.Casino,
                color = PulseTokens.AmberWarm,
                onClick = {
                    scope.launch {
                        val quote = QuoteService.fetchOnlineHitokoto()
                        testText = quote.text
                        quoteSourceDesc = quote.source ?: "一言名句"
                        Toast.makeText(context, "已载入: ${quote.source ?: "一言"}", Toast.LENGTH_SHORT).show()
                    }
                }
            ),
            ActionHubItem(
                label = "重温声音胶囊",
                icon = Icons.Default.Radio,
                color = PulseTokens.CyanElectric,
                onClick = {
                    if (speechHistory.isNotEmpty()) {
                        val latest = speechHistory.first()
                        testText = latest.first
                        quoteSourceDesc = latest.second
                        startSynthesis(activeProvider)
                        Toast.makeText(context, "正在重温上一条声音胶囊", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "暂无历史声音胶囊", Toast.LENGTH_SHORT).show()
                    }
                }
            ),
            ActionHubItem(
                label = "模型配置",
                icon = Icons.Default.Settings,
                color = PulseTokens.SonicBlue,
                onClick = {
                    onNavigateToEditProvider(activeProvider.id)
                }
            ),
            ActionHubItem(
                label = "清理缓存",
                icon = Icons.Default.CleaningServices,
                color = PulseTokens.MagentaLaser,
                onClick = {
                    audioCacheManager.clearAll()
                    Toast.makeText(context, "音频缓存已全部清空", Toast.LENGTH_SHORT).show()
                }
            ),
            ActionHubItem(
                label = if (isPlaying) "停止" else if (isSynthesizing) "合成中..." else "试听",
                icon = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                color = if (isPlaying) PulseTokens.MagentaLaser else activeBrandColor,
                isLoading = isSynthesizing,
                onClick = { startSynthesis(activeProvider) }
            )
        )

        UniversalActionHub(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            items = hubActionItems,
            icon = Icons.Default.Album
        )

        // ==================== 快捷音色选择弹窗 (在线拉取) ====================
        if (showVoicePickerDialog) {
            var fetchedVoices by remember { mutableStateOf<List<com.aitts.engine.data.VoiceModel>>(emptyList()) }
            var isFetchingOnlineVoices by remember { mutableStateOf(false) }
            var voiceSearchQuery by remember { mutableStateOf("") }
            var customVoiceInput by remember { mutableStateOf(activeProvider.voiceId) }

            fun fetchVoices() {
                scope.launch {
                    isFetchingOnlineVoices = true
                    try {
                        val list = TtsProviderManager.getInstance().getAvailableVoices(activeProvider)
                        fetchedVoices = list
                    } catch (e: Exception) {
                        Toast.makeText(context, "在线获取音色失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isFetchingOnlineVoices = false
                    }
                }
            }

            LaunchedEffect(activeProvider.id) {
                fetchVoices()
            }

            val filteredVoices = remember(fetchedVoices, voiceSearchQuery) {
                if (voiceSearchQuery.isBlank()) fetchedVoices
                else fetchedVoices.filter { it.name.contains(voiceSearchQuery, true) || it.id.contains(voiceSearchQuery, true) }
            }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showVoicePickerDialog = false },
                title = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ 发音人音色库", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                        IconButton(onClick = { fetchVoices() }, modifier = Modifier.size(28.dp)) {
                            if (isFetchingOnlineVoices) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PulseTokens.AmberWarm, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = PulseTokens.AmberWarm, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("当前引擎: ${activeProvider.name} · 当前音色: ${activeProvider.voiceId.ifBlank { "默认" }}", fontSize = 11.5.sp, color = PulseTokens.AmberWarm)

                        OutlinedTextField(
                            value = voiceSearchQuery,
                            onValueChange = { voiceSearchQuery = it },
                            placeholder = { Text("搜索发音人/语言/音色...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (filteredVoices.isEmpty()) {
                            if (isFetchingOnlineVoices) {
                                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = PulseTokens.AmberWarm)
                                }
                            } else {
                                Text("未探测到在线音色，您可手动输入音色 ID：", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                OutlinedTextField(
                                    value = customVoiceInput,
                                    onValueChange = { customVoiceInput = it },
                                    label = { Text("自定义音色 ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(filteredVoices.size) { idx ->
                                    val v = filteredVoices[idx]
                                    val isSelected = activeProvider.voiceId == v.id
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val updated = activeProvider.copy(voiceId = v.id)
                                                configDataStore.updateProvider(updated)
                                                showVoicePickerDialog = false
                                                Toast.makeText(context, "已切换音色: ${v.name}", Toast.LENGTH_SHORT).show()
                                            },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) PulseTokens.AmberWarm.copy(alpha = 0.2f) else PulseTokens.SurfaceCard
                                    ) {
                                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(v.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) PulseTokens.AmberWarm else PulseTokens.TextPrimary)
                                                Text("${v.id} · ${v.locale}", fontSize = 10.sp, color = PulseTokens.TextTertiary)
                                            }
                                            if (isSelected) {
                                                Text("✓", color = PulseTokens.AmberWarm, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customVoiceInput.isNotBlank()) {
                                val updated = activeProvider.copy(voiceId = customVoiceInput.trim())
                                configDataStore.updateProvider(updated)
                            }
                            showVoicePickerDialog = false
                        }
                    ) {
                        Text("完成")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showVoicePickerDialog = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        // ==================== 快捷 Prompt 提示词弹窗 ====================
        if (showPromptDialog) {
            val promptPresets = listOf(
                "自然流畅" to "请用自然、沉稳且富有感染力的语气朗读文本。",
                "激情昂扬" to "请用充满激情、热烈且铿锵有力的语调进行朗读。",
                "温柔低语" to "请用轻柔、舒缓、富有治愈感的伴读语气进行朗读。",
                "严肃新闻" to "请用字正腔圆、严谨庄重的新闻主播声调进行播报。"
            )
            var promptInput by remember { mutableStateOf(activeProvider.promptInstruction) }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showPromptDialog = false },
                title = { Text("🎭 配置 AI TTS 情绪与系统指令", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            label = { Text("系统指令 / 情绪 Prompt") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4
                        )
                        Text("情绪预设模板:", fontSize = 11.5.sp, color = PulseTokens.TextSecondary, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            promptPresets.take(2).forEach { (name, text) ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { promptInput = text },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseTokens.SurfaceCard,
                                    border = PulseTokens.BorderSubtle
                                ) {
                                    Text(name, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            promptPresets.drop(2).forEach { (name, text) ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { promptInput = text },
                                    shape = RoundedCornerShape(8.dp),
                                    color = PulseTokens.SurfaceCard,
                                    border = PulseTokens.BorderSubtle
                                ) {
                                    Text(name, fontSize = 11.sp, color = PulseTokens.AmberWarm, modifier = Modifier.padding(6.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updated = activeProvider.copy(promptInstruction = promptInput.trim())
                            configDataStore.updateProvider(updated)
                            showPromptDialog = false
                            Toast.makeText(context, "Prompt 指令已更新", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("保存")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showPromptDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        // ==================== 页面内模型选择抽屉 (ModalBottomSheet - 支持自由拖拽排序) ====================
        if (showBottomSheetModelPicker) {
            val bottomSheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var hubLocalList by remember(providers) { mutableStateOf(providers) }
            val density = LocalDensity.current
            val sheetItemHeightPx = with(density) { 68.dp.toPx() }
            var sheetDraggedProviderId by remember { mutableStateOf<String?>(null) }
            var sheetDragStartIndex by remember { mutableStateOf(-1) }
            var sheetDragTargetIndex by remember { mutableStateOf(-1) }
            var sheetTotalDragOffsetY by remember { mutableFloatStateOf(0f) }

            LaunchedEffect(providers) {
                if (sheetDraggedProviderId == null) {
                    hubLocalList = providers
                }
            }

            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showBottomSheetModelPicker = false },
                sheetState = bottomSheetState,
                containerColor = PulseTokens.SurfaceDark,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PulseTokens.AmberWarm.copy(alpha = 0.5f))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("黑胶发音唱片库抽屉", fontSize = 17.sp, fontWeight = FontWeight.Black, color = PulseTokens.TextPrimary)
                            Text("点击一键设为主力，长按右侧手柄自由排序", fontSize = 11.sp, color = PulseTokens.AmberWarm)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hubLocalList, key = { it.id }) { provider ->
                            val isSelected = provider.id == activeProvider.id
                            val isBeingDragged = sheetDraggedProviderId == provider.id
                            val itemIndex = hubLocalList.indexOfFirst { it.id == provider.id }

                            val brandColor = if (settings.isProviderCardAccentColorEnabled) {
                                when (provider.type) {
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
                                PulseTokens.AmberWarm
                            }

                            val visualShiftY = remember(sheetDraggedProviderId, sheetDragStartIndex, sheetDragTargetIndex, itemIndex) {
                                if (sheetDraggedProviderId == null || isBeingDragged || sheetDragStartIndex == -1 || sheetDragTargetIndex == -1 || itemIndex == -1) {
                                    0f
                                } else if (sheetDragStartIndex < sheetDragTargetIndex && itemIndex in (sheetDragStartIndex + 1)..sheetDragTargetIndex) {
                                    -sheetItemHeightPx
                                } else if (sheetDragStartIndex > sheetDragTargetIndex && itemIndex in sheetDragTargetIndex until sheetDragStartIndex) {
                                    sheetItemHeightPx
                                } else {
                                    0f
                                }
                            }
                            val animatedShiftY by animateFloatAsState(
                                targetValue = visualShiftY,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                label = "vinyl_sheet_shift"
                            )

                            val sheetCardModifier = if (isBeingDragged) {
                                Modifier
                                    .fillMaxWidth()
                                    .zIndex(99f)
                                    .graphicsLayer {
                                        translationY = sheetTotalDragOffsetY
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                        shadowElevation = 20f
                                    }
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .zIndex(1f)
                                    .graphicsLayer {
                                        translationY = animatedShiftY
                                    }
                            }

                            val surfaceElevatedColor = PulseTokens.SurfaceElevated

                            PulseCard(
                                modifier = sheetCardModifier,
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = when {
                                    isBeingDragged -> surfaceElevatedColor
                                    isSelected -> PulseTokens.SurfaceCardActive
                                    else -> PulseTokens.SurfaceCard
                                },
                                border = when {
                                    isBeingDragged -> BorderStroke(2.dp, brandColor)
                                    isSelected -> BorderStroke(1.5.dp, brandColor)
                                    else -> PulseTokens.BorderSubtle
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                configDataStore.updateSettings(settings.copy(activeProviderId = provider.id))
                                                showBottomSheetModelPicker = false
                                                Toast.makeText(context, "已切换主力为: ${provider.name}", Toast.LENGTH_SHORT).show()
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) brandColor else PulseTokens.TextTertiary)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(provider.name, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) brandColor else PulseTokens.TextPrimary)
                                                if (isSelected) {
                                                    Surface(shape = RoundedCornerShape(4.dp), color = brandColor.copy(alpha = 0.2f)) {
                                                        Text("主力", fontSize = 9.sp, color = brandColor, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text("${provider.type.displayName} · ${provider.voiceId.ifBlank { "默认音色" }}", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                showBottomSheetModelPicker = false
                                                onNavigateToEditProvider(provider.id)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = "配置", tint = PulseTokens.AmberWarm, modifier = Modifier.size(16.dp))
                                        }

                                        // 单一专属长按自由拖拽手柄
                                        val providerId = provider.id
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color = if (isBeingDragged) brandColor.copy(alpha = 0.2f) else surfaceElevatedColor)
                                                .pointerInput(providerId) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            val idx = hubLocalList.indexOfFirst { it.id == providerId }
                                                            if (idx != -1) {
                                                                sheetDraggedProviderId = providerId
                                                                sheetDragStartIndex = idx
                                                                sheetDragTargetIndex = idx
                                                                sheetTotalDragOffsetY = 0f
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            }
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            sheetTotalDragOffsetY += dragAmount.y
                                                            val offsetSteps = (sheetTotalDragOffsetY / sheetItemHeightPx).roundToInt()
                                                            val newTarget = (sheetDragStartIndex + offsetSteps).coerceIn(0, hubLocalList.size - 1)
                                                            if (newTarget != sheetDragTargetIndex) {
                                                                sheetDragTargetIndex = newTarget
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            if (sheetDragStartIndex != -1 && sheetDragTargetIndex != -1 && sheetDragStartIndex != sheetDragTargetIndex) {
                                                                val mutable = hubLocalList.toMutableList()
                                                                val item = mutable.removeAt(sheetDragStartIndex)
                                                                mutable.add(sheetDragTargetIndex, item)
                                                                hubLocalList = mutable
                                                                configDataStore.saveProviders(mutable)
                                                            }
                                                            sheetDraggedProviderId = null
                                                            sheetDragStartIndex = -1
                                                            sheetDragTargetIndex = -1
                                                            sheetTotalDragOffsetY = 0f
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        },
                                                        onDragCancel = {
                                                            sheetDraggedProviderId = null
                                                            sheetDragStartIndex = -1
                                                            sheetDragTargetIndex = -1
                                                            sheetTotalDragOffsetY = 0f
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DragHandle,
                                                contentDescription = "按住拖动排序",
                                                tint = if (isBeingDragged) brandColor else PulseTokens.TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
