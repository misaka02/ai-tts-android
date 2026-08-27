package com.aitts.engine.ui.pulse.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.audio.AudioCacheManager
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.ProviderType
import com.aitts.engine.ui.pulse.components.ActionHubItem
import com.aitts.engine.ui.pulse.components.UniversalActionHub
import com.aitts.engine.ui.pulse.theme.PulseCard
import com.aitts.engine.ui.pulse.theme.PulseTokens
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ⚡ Pulse 系统与引擎设置中心 (4 大中枢卡片全手势左右滑动 + 上下文自适应大拇指收纳岛)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PulseStudioSettingsScreen(
    configDataStore: ConfigDataStore,
    parentPagerState: PagerState? = null,
    onNavigateBackToRules: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by configDataStore.settingsFlow.collectAsState()
    val providers by configDataStore.providersFlow.collectAsState()
    val audioCacheManager = remember { AudioCacheManager.getInstance(context) }
    val innerPagerState = rememberPagerState(initialPage = 0) { 4 }

    fun getCacheSizeString(): String {
        val (count, sizeMb) = audioCacheManager.getStats()
        return if (sizeMb > 1024) String.format(Locale.getDefault(), "%.1f GB (%d 条)", sizeMb / 1024.0, count)
        else String.format(Locale.getDefault(), "%.1f MB (%d 条)", sizeMb, count)
    }

    var cacheSizeText by remember { mutableStateOf(getCacheSizeString()) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }
    var showFallbackSelectorDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirmDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetSecondConfirmDialog by remember { mutableStateOf(false) }
    var showQuickNavDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonStr = configDataStore.exportAllConfigJson(desensitize = false)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonStr.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "全量配置已成功导出至文件！", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val jsonStr = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: ""
                if (jsonStr.isNotBlank() && configDataStore.importConfigJson(jsonStr)) {
                    cacheSizeText = getCacheSizeString()
                    Toast.makeText(context, "已从文件成功恢复所有配置！", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "配置文件解析失败", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val categories = listOf(
        "🎨 外观与主题",
        "⚡ 引擎与声学",
        "🌐 网络与代理",
        "💾 备份与系统"
    )

    // 双层 HorizontalPager 互斥协同状态控制 (100% 实时像素级跟手 + 往返闭环)
    val nestedScrollConnection = remember(parentPagerState, innerPagerState) {
        object : NestedScrollConnection {
            /**
             * 滚动前拦截 (onPreScroll):
             * 当父级大页面已被向右拖出偏移时（处于 Page 2 与 Page 3 之间），
             * 如果用户中途反悔向左滑动 (available.x < 0)，
             * 必须优先将该位移用来收起/推回父级大页面归位到 Page 3！
             * 在父级完全归位前，严禁子级内部卡片发生任何切换消费！
             */
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (parentPagerState == null) return Offset.Zero

                val parentFraction = parentPagerState.currentPageOffsetFraction
                val isParentDraggedOut = parentPagerState.currentPage == 2 || parentFraction < -0.001f

                if (isParentDraggedOut && available.x < 0f) {
                    // 父级向左推回避让 (offset 逐渐增大向 0f 靠近)
                    val consumed = parentPagerState.dispatchRawDelta(available.x)
                    return Offset(consumed, 0f)
                }
                return Offset.Zero
            }

            /**
             * 滚动后接力 (onPostScroll):
             * 当子级卡片处于 Page 0 最左端，且用户继续向右滑动 (available.x > 0) 时，
             * 子级已无法消费（已到边界），未消费的向右位移无缝 1:1 驱动父级 Pager 实时平移跟手！
             */
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (parentPagerState == null) return Offset.Zero

                val isInnerAtEdge = innerPagerState.currentPage == 0 && innerPagerState.currentPageOffsetFraction <= 0.001f

                if (isInnerAtEdge && available.x > 0f) {
                    val delta = parentPagerState.dispatchRawDelta(available.x)
                    return Offset(delta, 0f)
                }
                return Offset.Zero
            }

            /**
             * 手指离屏释放 (onPostFling):
             * 当手指离开屏幕时，若父级已被拖出位移，执行最终的决定性吸附结算，
             * 彻底消耗释放惯性速度，消除抖动与悬空。
             */
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (parentPagerState == null) return Velocity.Zero

                val parentFraction = parentPagerState.currentPageOffsetFraction
                val isParentDragged = parentPagerState.currentPage == 2 || parentFraction < -0.001f

                if (isParentDragged) {
                    // 阈值判定：向右初速度超标 (快速轻弹) 或向右拖动幅度越过 20%
                    val shouldSnapToRules = available.x > 250f || parentFraction < -0.2f

                    if (shouldSnapToRules) {
                        parentPagerState.animateScrollToPage(
                            page = 2,
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        )
                    } else {
                        parentPagerState.animateScrollToPage(
                            page = 3,
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                        )
                    }
                    return available // 完整消费释放能量
                }
                return Velocity.Zero
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
                .padding(top = 24.dp)
        ) {
            // 顶部大标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "系统与引擎首选项",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = PulseTokens.TextPrimary
                    )
                    Text(
                        text = "分类微胶囊快速直达 · 单手操作收纳岛",
                        fontSize = 11.sp,
                        color = PulseTokens.CyanElectric,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = { showQuickNavDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(PulseTokens.SurfaceElevated)
                        .size(36.dp)
                    ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "直达卡片",
                        tint = PulseTokens.CyanElectric,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 顶部流体微胶囊分舱 Tab
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(categories) { idx, title ->
                    val isSelected = innerPagerState.currentPage == idx
                    FilterChip(
                        selected = isSelected,
                        onClick = { scope.launch { innerPagerState.animateScrollToPage(idx) } },
                        label = { Text(title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PulseTokens.CyanElectric,
                            selectedLabelColor = Color.Black,
                            containerColor = PulseTokens.SurfaceElevated,
                            labelColor = PulseTokens.TextSecondary
                        ),
                        border = if (isSelected) null else PulseTokens.BorderSubtle
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==================== 4 大分类可滑动卡片视图 (HorizontalPager) ====================
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                HorizontalPager(
                    state = innerPagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) { page ->
                when (page) {
                    // ==================== 卡片 0: 外观主题与设计风格 ====================
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text("🎨 界面外观与设计风格", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                                        Text("全局 UI 主题架构", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                        val styleOptions = listOf(
                                            "PULSE" to "⚡ Pulse 极光灵动微胶囊中枢",
                                            "BENTO" to "🚀 Bento 全景网格矩阵工作台",
                                            "STUDIO" to "🎛️ Modern Studio 专业声学调音台",
                                            "VINYL" to "📻 Vinyl 复古黑胶阅览中枢"
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            styleOptions.forEach { (key, label) ->
                                                val isCurrent = settings.appUiStyle == key
                                                PulseCard(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            configDataStore.updateSettings(settings.copy(appUiStyle = key))
                                                            Toast.makeText(context, "已切换为: $label", Toast.LENGTH_SHORT).show()
                                                        },
                                                    backgroundColor = if (isCurrent) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                                    border = if (isCurrent) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(label, fontSize = 13.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                                        if (isCurrent) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text("主题明暗模式 (Theme Mode)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            val themeModes = listOf(
                                                "DARK" to "🌙 深邃夜间",
                                                "LIGHT" to "☀️ 灵动明亮"
                                            )
                                            themeModes.forEach { (modeKey, modeTitle) ->
                                                val isCurrent = settings.appThemeMode.uppercase() == modeKey
                                                PulseCard(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            configDataStore.updateSettings(settings.copy(appThemeMode = modeKey))
                                                            Toast.makeText(context, "已切换为: $modeTitle", Toast.LENGTH_SHORT).show()
                                                        },
                                                    backgroundColor = if (isCurrent) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                                    border = if (isCurrent) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(modeTitle, fontSize = 12.5.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                                        if (isCurrent) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(15.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("色彩方案 (Color Palette)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("当前: ${settings.appThemePalette}", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Button(
                                                onClick = { showPaletteDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                                                border = PulseTokens.BorderSubtle,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("更换色彩", fontSize = 12.sp)
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("AMOLED 极夜绝对纯黑", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("关闭所有底色发光，深度节能且更纯粹", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.isAmoledPureBlack,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(isAmoledPureBlack = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("模型卡片厂商印象色", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("开启时按厂商预设展示个性化专属品牌色，关闭时统一极简色", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.isProviderCardAccentColorEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(isProviderCardAccentColorEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("触觉震动微反馈 (Haptics)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("在拖拽排序、长按与按键时提供触觉反馈", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.hapticFeedbackEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(hapticFeedbackEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("系统默认 TTS 引擎", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("跳转至 Android 文本转语音设置页", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = Intent("com.android.settings.TTS_SETTINGS").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        context.startActivity(Intent(AndroidSettings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                                                border = PulseTokens.BorderSubtle,
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("去设置", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 卡片 1: 引擎声学与发音微调 ====================
                    1 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. 阅读器文本切分与分段规则
                            item {
                                PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("📜 阅读器文本切分与预加载", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)
                                            Text(if (settings.isSentenceSplittingEnabled) "规则已生效" else "原文整篇透传", fontSize = 11.sp, color = if (settings.isSentenceSplittingEnabled) PulseTokens.CyanElectric else PulseTokens.TextTertiary, fontWeight = FontWeight.Bold)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("启用文本分段流水线", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("关闭时阅读器给什么就向引擎发送什么 (不做任何切分)；开启时按下方规则分段处理", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.isSentenceSplittingEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(isSentenceSplittingEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        if (settings.isSentenceSplittingEnabled) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("分段划分策略:", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    val modes = listOf(
                                                        "PARAGRAPH" to "按换行段落",
                                                        "PUNCTUATION" to "按标点断句",
                                                        "SMART_HYBRID" to "智能对白角色"
                                                    )
                                                    modes.forEach { (modeKey, modeTitle) ->
                                                        val isSelected = settings.textSegmentationMode == modeKey
                                                        Surface(
                                                            modifier = Modifier.weight(1f).clickable {
                                                                configDataStore.updateSettings(settings.copy(textSegmentationMode = modeKey))
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (isSelected) PulseTokens.CyanElectric.copy(alpha = 0.15f) else PulseTokens.SurfaceElevated,
                                                            border = BorderStroke(1.dp, if (isSelected) PulseTokens.CyanElectric else PulseTokens.SurfaceElevated)
                                                        ) {
                                                            Text(
                                                                text = modeTitle,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                                                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("相邻短段落自动合并", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PulseTokens.TextPrimary)
                                                    Text("自动将连续极短的段落 (<30字) 合并发送，减少频繁请求与语意割裂", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                                }
                                                Switch(
                                                    checked = settings.mergeShortParagraphs,
                                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(mergeShortParagraphs = it)) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                                )
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("超长段落句号拆分", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PulseTokens.TextPrimary)
                                                    Text("单段超长时严格在句号/问号/感叹号处断句，避免大模型单次请求超时", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                                }
                                                Switch(
                                                    checked = settings.splitLongParagraphs,
                                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(splitLongParagraphs = it)) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                                )
                                            }

                                            if (settings.splitLongParagraphs) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("超长拆分阈值 (句末标点节点)", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                                        Text("${settings.maxSegmentLength} 字", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                                    }
                                                    Slider(
                                                        value = settings.maxSegmentLength.toFloat(),
                                                        onValueChange = { configDataStore.updateSettings(settings.copy(maxSegmentLength = it.toInt())) },
                                                        valueRange = 80f..500f,
                                                        steps = 20,
                                                        colors = SliderDefaults.colors(thumbColor = PulseTokens.CyanElectric, activeTrackColor = PulseTokens.CyanElectric)
                                                    )
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("分段提前请求预加载", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = PulseTokens.TextPrimary)
                                                    Text("按分段规则在后台异步提前准备好接下来的音频分块，消除段落等待时间", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                                }
                                                Switch(
                                                    checked = settings.enableSegmentPreload,
                                                    onCheckedChange = { configDataStore.updateSettings(settings.copy(enableSegmentPreload = it)) },
                                                    colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                                )
                                            }

                                            if (settings.enableSegmentPreload) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text("预加载前瞻深度 (提前合成段数):", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        listOf(1 to "1 块 (极省流量)", 2 to "2 块 (均衡推荐)", 3 to "3 块 (极速秒开)", 4 to "4 块 (强劲管线)").forEach { (count, label) ->
                                                            val isSelected = settings.preloadAheadCount == count
                                                            Surface(
                                                                modifier = Modifier.weight(1f).clickable {
                                                                    configDataStore.updateSettings(settings.copy(preloadAheadCount = count))
                                                                },
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = if (isSelected) PulseTokens.CyanElectric.copy(alpha = 0.15f) else PulseTokens.SurfaceElevated,
                                                                border = BorderStroke(1.dp, if (isSelected) PulseTokens.CyanElectric else PulseTokens.SurfaceElevated)
                                                            ) {
                                                                Text(
                                                                    text = "${count} 块",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                                                                    modifier = Modifier.padding(vertical = 6.dp),
                                                                    maxLines = 1
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. 引擎声学与发音微调
                            item {
                                PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text("⚡ 引擎声学与微调", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("极速首字秒开直出 (<150ms)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("流式切片直达声卡，大幅降低小说朗读首包延迟", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.ultraLowLatencyMode,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(ultraLowLatencyMode = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("标点分句停顿时间", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                                Text("${settings.sentencePauseMs} ms", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                            }
                                            Slider(
                                                value = settings.sentencePauseMs.toFloat(),
                                                onValueChange = { configDataStore.updateSettings(settings.copy(sentencePauseMs = it.toInt())) },
                                                valueRange = 0f..1000f,
                                                steps = 19,
                                                colors = SliderDefaults.colors(thumbColor = PulseTokens.CyanElectric, activeTrackColor = PulseTokens.CyanElectric)
                                            )
                                            Text("逗号、句号及段落换行时的微停顿（推荐 150~350ms）", fontSize = 11.sp, color = PulseTokens.TextTertiary)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("英文缩写智能规范化", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("自动将 AI、CPU、WiFi 等缩写转换为规范字母读音", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.isAcronymNormalizationEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(isAcronymNormalizationEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("小说对白智能情感语气", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("自动识别双引号内对白并注入情绪重音", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.isEmotionProsodyEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(isEmotionProsodyEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("人声清晰度增强 (Clear Voice EQ)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("动态提亮中高频人声齿音，降低背景混响", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.voiceClarityBoostEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(voiceClarityBoostEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 卡片 2: 网络代理与故障转移 ====================
                    2 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text("🌐 网络代理与故障转移", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("智能故障转移 (Fallback)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("主引擎请求超时或报错时自动降级到备用引擎", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.autoFallbackOnFailure,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        if (settings.autoFallbackOnFailure) {
                                            val currentFallback = providers.find { it.id == settings.fallbackProviderId }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(PulseTokens.SurfaceElevated)
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("备用兜底模型", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                                    Text("当前指定: ${currentFallback?.name ?: "Edge-TTS (默认)"}", fontSize = 11.5.sp, color = PulseTokens.CyanElectric)
                                                }
                                                Button(
                                                    onClick = { showFallbackSelectorDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceCardActive, contentColor = PulseTokens.CyanElectric),
                                                    border = PulseTokens.BorderSubtle,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Text("选择备用", fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("大模型网络自愈重试", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("遇到 429 限流或 503 抖动时自动指数避让重试 2 次", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.autoRetryOnFailure,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(autoRetryOnFailure = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("全局 HTTP/SOCKS 代理", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("为所有 TTS 请求统一配置代理服务器", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Switch(
                                                checked = settings.proxyEnabled,
                                                onCheckedChange = { configDataStore.updateSettings(settings.copy(proxyEnabled = it)) },
                                                colors = SwitchDefaults.colors(checkedThumbColor = PulseTokens.CyanElectric, checkedTrackColor = PulseTokens.SurfaceElevated)
                                            )
                                        }

                                        if (settings.proxyEnabled) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = settings.proxyHost,
                                                    onValueChange = { configDataStore.updateSettings(settings.copy(proxyHost = it)) },
                                                    label = { Text("代理地址 (Host)") },
                                                    placeholder = { Text("127.0.0.1") },
                                                    modifier = Modifier.weight(2f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PulseTokens.CyanElectric,
                                                        unfocusedBorderColor = PulseTokens.SurfaceCardActive
                                                    )
                                                )
                                                OutlinedTextField(
                                                    value = settings.proxyPort.toString(),
                                                    onValueChange = {
                                                        val p = it.toIntOrNull() ?: 7890
                                                        configDataStore.updateSettings(settings.copy(proxyPort = p))
                                                    },
                                                    label = { Text("端口") },
                                                    placeholder = { Text("7890") },
                                                    modifier = Modifier.weight(1f),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = PulseTokens.CyanElectric,
                                                        unfocusedBorderColor = PulseTokens.SurfaceCardActive
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ==================== 卡片 3: 备份与存储维护 ====================
                    3 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                PulseCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text("💾 数据备份与存储维护", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PulseTokens.CyanElectric)

                                        Text("全量配置导入与导出", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { exportFileLauncher.launch("ai-tts-backup-${System.currentTimeMillis()}.json") },
                                                modifier = Modifier.weight(1f).height(42.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                                                border = PulseTokens.BorderSubtle,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("导出至文件", fontSize = 12.5.sp)
                                            }

                                            Button(
                                                onClick = { importFileLauncher.launch(arrayOf("application/json", "text/*")) },
                                                modifier = Modifier.weight(1f).height(42.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.CyanElectric),
                                                border = PulseTokens.BorderSubtle,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("从文件恢复", fontSize = 12.5.sp)
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    val jsonStr = configDataStore.exportAllConfigJson(desensitize = false)
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS-Config", jsonStr))
                                                    Toast.makeText(context, "配置 JSON 已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("复制配置文本", fontSize = 12.sp)
                                            }

                                            OutlinedButton(
                                                onClick = { showImportDialog = true },
                                                modifier = Modifier.weight(1f).height(38.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("粘贴恢复文本", fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("本地音频 LRU 缓存", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.TextPrimary)
                                                Text("当前占用: $cacheSizeText", fontSize = 12.sp, color = PulseTokens.CyanElectric)
                                            }
                                            Button(
                                                onClick = {
                                                    showClearCacheConfirmDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.SurfaceElevated, contentColor = PulseTokens.MagentaLaser),
                                                border = BorderStroke(1.dp, PulseTokens.MagentaLaser.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("立即清空", fontSize = 12.sp)
                                            }
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("恢复出厂官方预设", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.MagentaLaser)
                                                Text("重置全部模型、替换规则与系统配置", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                            }
                                            Button(
                                                onClick = { showResetDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.MagentaLaser.copy(alpha = 0.15f), contentColor = PulseTokens.MagentaLaser),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("重置配置", fontSize = 12.sp)
                                            }
                                        }

                                        // 分割线
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color.White.copy(alpha = 0.08f))
                                        )

                                        // ==================== 关于系统与开发者生态 ====================
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("关于软件与开源致谢", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PulseTokens.CyanElectric)
                                                    Text("系统级语音大模型与离线神经网络 TTS 引擎", fontSize = 11.5.sp, color = PulseTokens.TextSecondary)
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = PulseTokens.CyanElectric.copy(alpha = 0.15f)
                                                ) {
                                                    Text("v${com.aitts.engine.BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = PulseTokens.CyanElectric, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            PulseCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                backgroundColor = PulseTokens.SurfaceDark,
                                                border = PulseTokens.BorderSubtle,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("开发者 (Developer)", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                                        Text("Antigravity (Google DeepMind) & Misaka02", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.TextPrimary)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("开源协议 (License)", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                                        Text("Apache-2.0 License", fontSize = 12.sp, color = PulseTokens.AcidGreen, fontWeight = FontWeight.SemiBold)
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("GitHub 官方主页", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                                                        Text("misaka02/ai-tts-android", fontSize = 12.sp, color = PulseTokens.CyanElectric, fontWeight = FontWeight.Medium)
                                                    }
                                                }
                                            }

                                            PulseCard(
                                                modifier = Modifier.fillMaxWidth(),
                                                backgroundColor = PulseTokens.SurfaceDark,
                                                border = PulseTokens.BorderSubtle,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text("开源项目与技术致谢 (Credits)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PulseTokens.CyanElectric)
                                                    Text("• Sonic (Bill Cox) - 本地变频变速算法 (Apache-2.0)\n• Sherpa-ONNX (k2-fsa) - 端侧神经网络推理引擎 (Apache-2.0)\n• Commons Compress (Apache) - 离线解包组件 (Apache-2.0)\n• OkHttp & Okio (Square) - 高性能流式通信 (Apache-2.0)\n• 神经声学模型: Piper, MeloTTS, Matcha, Kokoro, ChatTTS, CosyVoice, GPT-SoVITS\n• 听书生态: 开源阅读 (Legado), 静读天下 (Moon+ Reader)", fontSize = 10.5.sp, color = PulseTokens.TextSecondary, lineHeight = 16.sp)
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/misaka02/ai-tts-android"))
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "无法打开浏览器: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f).height(38.dp)
                                                ) {
                                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("打开 GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("GitHub URL", "https://github.com/misaka02/ai-tts-android"))
                                                        Toast.makeText(context, "已复制 GitHub 仓库主页链接！", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseTokens.CyanElectric),
                                                    border = BorderStroke(1.dp, PulseTokens.CyanElectric.copy(alpha = 0.5f)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f).height(38.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("复制主页链接", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // 弹窗: 快速直达卡片轮盘
        if (showQuickNavDialog) {
            AlertDialog(
                onDismissRequest = { showQuickNavDialog = false },
                title = { Text("大拇指快速直达卡片", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEachIndexed { index, title ->
                            val isSelected = innerPagerState.currentPage == index
                            PulseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch { innerPagerState.animateScrollToPage(index) }
                                        showQuickNavDialog = false
                                    },
                                backgroundColor = if (isSelected) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                border = if (isSelected) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(title, fontSize = 13.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQuickNavDialog = false }) { Text("关闭") }
                }
            )
        }

        // 弹窗: 切换主题风格
        if (showThemeDialog) {
            val styles = listOf(
                "PULSE" to "⚡ Pulse 极光灵动微胶囊中枢",
                "BENTO" to "🚀 Bento 全景网格矩阵工作台",
                "STUDIO" to "🎛️ Modern Studio 专业声学调音台",
                "VINYL" to "📻 Vinyl 复古黑胶阅览中枢"
            )
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("切换设计系统风格", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        styles.forEach { (key, name) ->
                            val isCurrent = settings.appUiStyle == key
                            PulseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(appUiStyle = key))
                                        showThemeDialog = false
                                        Toast.makeText(context, "已切换风格为: $name", Toast.LENGTH_SHORT).show()
                                    },
                                backgroundColor = if (isCurrent) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                border = if (isCurrent) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 13.sp, color = if (isCurrent) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                    if (isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
                }
            )
        }

        // 弹窗: 切换调色板
        if (showPaletteDialog) {
            val palettes = listOf(
                "OCEAN_AZURE" to "⚡ 电光蔚蓝 (极客科技)",
                "EMERALD_JADE" to "🍃 翡翠翠玉 (温润自然)",
                "TITANIUM_SLATE" to "🪨 钛金岩灰 (低调沉稳)",
                "SUNSET_AMBER" to "🌅 暮光琥珀 (温暖夜间)",
                "NEON_CYBER" to "⚡ 赛博霓虹 (高能动感)",
                "SAKURA_PINK" to "🌸 樱花落粉 (甜美清新)",
                "AMETHYST_PURPLE" to "🔮 幻晶紫曜 (梦幻深邃)",
                "MORANDI_GRAPHITE" to "🎨 莫兰迪石墨 (低饱和度)"
            )
            AlertDialog(
                onDismissRequest = { showPaletteDialog = false },
                title = { Text("选择配色方案", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        palettes.forEach { (key, name) ->
                            val isCurrent = settings.appThemePalette == key
                            PulseCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(appThemePalette = key))
                                        showPaletteDialog = false
                                        Toast.makeText(context, "已切换配色为: $name", Toast.LENGTH_SHORT).show()
                                    },
                                backgroundColor = if (isCurrent) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                border = if (isCurrent) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name, fontSize = 13.sp, color = if (isCurrent) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                    if (isCurrent) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = PulseTokens.CyanElectric, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPaletteDialog = false }) { Text("关闭") }
                }
            )
        }

        // 弹窗: 备用降级模型选择器 (故障转移专属弹窗，含总开关)
        if (showFallbackSelectorDialog) {
            AlertDialog(
                onDismissRequest = { showFallbackSelectorDialog = false },
                title = { Text("故障自动转移与备用模型", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 顶部总开关
                        PulseCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (settings.autoFallbackOnFailure) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                            border = if (settings.autoFallbackOnFailure) BorderStroke(1.5.dp, PulseTokens.CyanElectric) else PulseTokens.BorderSubtle,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = !settings.autoFallbackOnFailure))
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("启用故障自动转移", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = if (settings.autoFallbackOnFailure) PulseTokens.CyanElectric else PulseTokens.TextPrimary)
                                    Text("当主力引擎异常或超时时，自动秒级无缝切换至备用引擎", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                }
                                Switch(
                                    checked = settings.autoFallbackOnFailure,
                                    onCheckedChange = { isChecked ->
                                        configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = isChecked))
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = PulseTokens.CyanElectric)
                                )
                            }
                        }

                        Text("选择备用兜底模型：", fontSize = 12.sp, color = PulseTokens.TextSecondary, fontWeight = FontWeight.SemiBold)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(providers) { p ->
                                val isCurrentFallback = settings.fallbackProviderId == p.id
                                val fallbackBrandColor = if (settings.isProviderCardAccentColorEnabled) {
                                    when (p.type) {
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

                                PulseCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            configDataStore.updateSettings(settings.copy(fallbackProviderId = p.id, autoFallbackOnFailure = true))
                                            showFallbackSelectorDialog = false
                                            Toast.makeText(context, "已指定备用兜底模型为: ${p.name}，并已开启自动故障转移", Toast.LENGTH_SHORT).show()
                                        },
                                    backgroundColor = if (isCurrentFallback) PulseTokens.SurfaceCardActive else PulseTokens.SurfaceElevated,
                                    border = if (isCurrentFallback) BorderStroke(1.5.dp, fallbackBrandColor) else PulseTokens.BorderSubtle,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(fallbackBrandColor)
                                            )
                                            Column {
                                                Text(p.name, fontSize = 13.sp, fontWeight = if (isCurrentFallback) FontWeight.Bold else FontWeight.Normal, color = if (isCurrentFallback) fallbackBrandColor else PulseTokens.TextPrimary)
                                                Text("${p.type.displayName} · ${p.voiceId.ifBlank { "默认" }}", fontSize = 11.sp, color = PulseTokens.TextSecondary)
                                            }
                                        }
                                        if (isCurrentFallback) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = fallbackBrandColor, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFallbackSelectorDialog = false }) { Text("完成") }
                }
            )
        }

        // 弹窗: 文本粘贴恢复配置
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("从 JSON 文本恢复配置", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("请在下方粘贴导出的完整 JSON 配置内容：", fontSize = 12.sp, color = PulseTokens.TextSecondary)
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            modifier = Modifier.fillMaxWidth().height(140.dp),
                            placeholder = { Text("{\"version\":1, ...}") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PulseTokens.CyanElectric,
                                unfocusedBorderColor = PulseTokens.SurfaceCardActive
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importJsonText.isNotBlank() && configDataStore.importConfigJson(importJsonText)) {
                                cacheSizeText = getCacheSizeString()
                                showImportDialog = false
                                importJsonText = ""
                                Toast.makeText(context, "已成功恢复所有配置！", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "配置格式错误，解析失败", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.CyanElectric, contentColor = Color.Black)
                    ) {
                        Text("确认恢复", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) { Text("取消") }
                }
            )
        }

        // 弹窗: 清理本地音频缓存二次确认
        if (showClearCacheConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearCacheConfirmDialog = false },
                title = { Text("🧹 清理本地音频缓存", fontWeight = FontWeight.Bold, color = PulseTokens.AmberWarm) },
                text = {
                    Text(
                        "确定要清空本地所有已缓存的离线音频吗？\n当前占用: $cacheSizeText\n清空后再次朗读相同句子将重新发起在线合成请求。",
                        fontSize = 13.sp,
                        color = PulseTokens.TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            audioCacheManager.clearAll()
                            cacheSizeText = getCacheSizeString()
                            showClearCacheConfirmDialog = false
                            Toast.makeText(context, "已清空本地全部音频缓存", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.AmberWarm, contentColor = Color.Black)
                    ) {
                        Text("立即清空", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearCacheConfirmDialog = false }) {
                        Text("取消", color = PulseTokens.TextSecondary)
                    }
                }
            )
        }

        // 弹窗: 出厂重置第一道确认 (Step 1/2)
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("⚠️ 恢复出厂设置 (第 1/2 步)", fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser) },
                text = { Text("此操作将重置所有自定义模型、发音规则与参数，恢复为初始官方预设。确定进入下一步吗？", fontSize = 13.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetDialog = false
                            showResetSecondConfirmDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.MagentaLaser, contentColor = Color.White)
                    ) {
                        Text("下一步 (二次确认)", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("取消") }
                }
            )
        }

        // 弹窗: 出厂重置高危二次确认 (Step 2/2 - 最终确认)
        if (showResetSecondConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetSecondConfirmDialog = false },
                title = { Text("🔥 最终确认: 数据抹除警告 (第 2/2 步)", fontWeight = FontWeight.Black, color = PulseTokens.MagentaLaser) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("【高危警告】此操作绝对不可撤销！", fontWeight = FontWeight.Bold, color = PulseTokens.MagentaLaser, fontSize = 13.sp)
                        Text("点击下方按钮将立即清除全部自定义 API 密钥、自定义模型配置及所有发音修正正则规则，并将应用参数彻底重置为刚安装时的初始出厂状态。", fontSize = 12.5.sp, color = PulseTokens.TextSecondary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            configDataStore.resetToDefaults()
                            cacheSizeText = getCacheSizeString()
                            showResetSecondConfirmDialog = false
                            Toast.makeText(context, "已恢复为官方初始配置！", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PulseTokens.MagentaLaser, contentColor = Color.White)
                    ) {
                        Text("我已知晓后果，立即重置", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetSecondConfirmDialog = false }) { Text("取消放弃") }
                }
            )
        }

        // 浮动单手快捷收纳岛 (按当前选中的 4 大卡片深度动态自适应)
        val allPalettes = listOf(
            "OCEAN_AZURE" to "电光蔚蓝",
            "EMERALD_JADE" to "翡翠翠玉",
            "TITANIUM_SLATE" to "钛金岩灰",
            "SUNSET_AMBER" to "暮光琥珀",
            "NEON_CYBER" to "赛博霓虹",
            "SAKURA_PINK" to "樱花落粉",
            "AMETHYST_PURPLE" to "幻晶紫曜",
            "MORANDI_GRAPHITE" to "莫兰迪石墨"
        )

        val settingsHubItems = when (innerPagerState.currentPage) {
            0 -> listOf(
                ActionHubItem(
                    label = "切换配色",
                    icon = Icons.Default.Palette,
                    color = PulseTokens.CyanElectric,
                    onClick = {
                        val currentIdx = allPalettes.indexOfFirst { it.first == settings.appThemePalette }
                        val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % allPalettes.size
                        val nextPalette = allPalettes[nextIdx]
                        configDataStore.updateSettings(settings.copy(appThemePalette = nextPalette.first))
                        Toast.makeText(context, "已切换配色: ${nextPalette.second}", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = if (settings.appThemeMode.uppercase() == "LIGHT") "深色模式" else "亮色模式",
                    icon = Icons.Default.DarkMode,
                    color = PulseTokens.SonicBlue,
                    onClick = {
                        val nextMode = if (settings.appThemeMode.uppercase() == "LIGHT") "DARK" else "LIGHT"
                        configDataStore.updateSettings(settings.copy(appThemeMode = nextMode))
                        Toast.makeText(context, if (nextMode == "LIGHT") "已切换为明亮模式" else "已切换为深色模式", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = "UI 布局",
                    icon = Icons.Default.Dashboard,
                    color = PulseTokens.CyanElectric,
                    onClick = {
                        val styles = listOf("PULSE", "BENTO", "STUDIO", "VINYL")
                        val currentIdx = styles.indexOf(settings.appUiStyle)
                        val nextStyle = styles[(if (currentIdx >= 0) currentIdx + 1 else 0) % styles.size]
                        configDataStore.updateSettings(settings.copy(appUiStyle = nextStyle))
                        val styleNames = mapOf("PULSE" to "极光中枢", "BENTO" to "全景网格", "STUDIO" to "声学调音台", "VINYL" to "复古黑胶")
                        Toast.makeText(context, "已切换主页布局为: ${styleNames[nextStyle]}", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = "核心球形态",
                    icon = Icons.Default.GraphicEq,
                    color = PulseTokens.SonicBlue,
                    onClick = {
                        val nextStyle = (settings.acousticCoreStyle + 1) % 3
                        configDataStore.updateSettings(settings.copy(acousticCoreStyle = nextStyle))
                        val names = listOf("极光光晕", "物理点阵", "引力轨道")
                        Toast.makeText(context, "已切换核心形态为: ${names[nextStyle]}", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = if (settings.isProviderCardAccentColorEnabled) "强调色: 开" else "强调色: 关",
                    icon = Icons.Default.ColorLens,
                    color = PulseTokens.AmberWarm,
                    onClick = {
                        val next = !settings.isProviderCardAccentColorEnabled
                        configDataStore.updateSettings(settings.copy(isProviderCardAccentColorEnabled = next))
                        Toast.makeText(context, if (next) "已开启模型品牌色" else "已关闭模型品牌色", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            1 -> listOf(
                ActionHubItem(
                    label = "默认音色",
                    icon = Icons.Default.RecordVoiceOver,
                    color = PulseTokens.CyanElectric,
                    onClick = {
                        val defProvider = providers.firstOrNull()
                        if (defProvider != null) {
                            Toast.makeText(context, "全局默认引擎: ${defProvider.name} (${defProvider.voiceId.ifBlank { "默认音色" }})", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                ActionHubItem(
                    label = if (settings.autoFallbackOnFailure) "主备回退: 开启" else "主备回退: 关闭",
                    icon = Icons.Default.AltRoute,
                    color = if (settings.autoFallbackOnFailure) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                    onClick = {
                        val next = !settings.autoFallbackOnFailure
                        configDataStore.updateSettings(settings.copy(autoFallbackOnFailure = next))
                        Toast.makeText(context, if (next) "已开启故障主备自动降级" else "已关闭主备降级", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = "选择备用模型",
                    icon = Icons.Default.SwapHoriz,
                    color = PulseTokens.SonicBlue,
                    onClick = { showFallbackSelectorDialog = true }
                ),
                ActionHubItem(
                    label = "分段: ${when(settings.textSegmentationMode) { "PARAGRAPH" -> "段落"; "PUNCTUATION" -> "标点"; else -> "混合" }}",
                    icon = Icons.Default.FormatQuote,
                    color = PulseTokens.AmberWarm,
                    onClick = {
                        val nextMode = when (settings.textSegmentationMode) {
                            "PARAGRAPH" -> "PUNCTUATION"
                            "PUNCTUATION" -> "SMART_HYBRID"
                            else -> "PARAGRAPH"
                        }
                        configDataStore.updateSettings(settings.copy(textSegmentationMode = nextMode))
                        val modeNames = mapOf("PARAGRAPH" to "按换行段落", "PUNCTUATION" to "按标点断句", "SMART_HYBRID" to "智能混合切分")
                        Toast.makeText(context, "分段规则: ${modeNames[nextMode]}", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = if (settings.enableSegmentPreload) "预加载: 开启" else "预加载: 关闭",
                    icon = Icons.Default.Bolt,
                    color = if (settings.enableSegmentPreload) PulseTokens.MagentaLaser else PulseTokens.TextSecondary,
                    onClick = {
                        val next = !settings.enableSegmentPreload
                        configDataStore.updateSettings(settings.copy(enableSegmentPreload = next))
                        Toast.makeText(context, if (next) "已开启流式并发预加载" else "已关闭并发预加载", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            2 -> listOf(
                ActionHubItem(
                    label = if (settings.proxyEnabled) "代理: 开启" else "代理: 直连",
                    icon = Icons.Default.Language,
                    color = if (settings.proxyEnabled) PulseTokens.CyanElectric else PulseTokens.TextSecondary,
                    onClick = {
                        val next = !settings.proxyEnabled
                        configDataStore.updateSettings(settings.copy(proxyEnabled = next))
                        Toast.makeText(context, if (next) "已开启网络代理" else "已切换为直连模式", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = "超时: ${settings.connectTimeoutSeconds}秒",
                    icon = Icons.Default.Timer,
                    color = PulseTokens.SonicBlue,
                    onClick = {
                        val timeouts = listOf(10, 15, 30, 60)
                        val curIdx = timeouts.indexOf(settings.connectTimeoutSeconds)
                        val nextTimeout = timeouts[(if (curIdx >= 0) curIdx + 1 else 0) % timeouts.size]
                        configDataStore.updateSettings(settings.copy(connectTimeoutSeconds = nextTimeout))
                        Toast.makeText(context, "连接超时设为: ${nextTimeout}秒", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = if (settings.autoRetryOnFailure) "自动重试: 开启" else "自动重试: 关闭",
                    icon = Icons.Default.Refresh,
                    color = PulseTokens.AmberWarm,
                    onClick = {
                        val next = !settings.autoRetryOnFailure
                        configDataStore.updateSettings(settings.copy(autoRetryOnFailure = next))
                        Toast.makeText(context, if (next) "已开启网络抖动自动重试" else "已关闭自动重试", Toast.LENGTH_SHORT).show()
                    }
                )
            )

            else -> listOf(
                ActionHubItem(
                    label = "导出配置",
                    icon = Icons.Default.FileDownload,
                    color = PulseTokens.CyanElectric,
                    onClick = {
                        val json = configDataStore.exportAllConfigJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI-TTS Config", json))
                        Toast.makeText(context, "完整配置已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                ),
                ActionHubItem(
                    label = "导入配置",
                    icon = Icons.Default.FileUpload,
                    color = PulseTokens.SonicBlue,
                    onClick = { showImportDialog = true }
                ),
                ActionHubItem(
                    label = "清理缓存",
                    icon = Icons.Default.CleaningServices,
                    color = PulseTokens.AmberWarm,
                    onClick = {
                        showClearCacheConfirmDialog = true
                    }
                ),
                ActionHubItem(
                    label = "恢复出厂",
                    icon = Icons.Default.RestartAlt,
                    color = PulseTokens.MagentaLaser,
                    onClick = { showResetDialog = true }
                )
            )
        }

        if (parentPagerState == null || parentPagerState.currentPage == 3) {
            UniversalActionHub(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 76.dp, end = 16.dp),
                items = settingsHubItems,
                icon = Icons.Default.Settings
            )
        }
    }
}
