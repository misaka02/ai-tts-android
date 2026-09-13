package com.aitts.engine.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitts.engine.data.ConfigDataStore
import com.aitts.engine.data.GlobalSettings
import com.aitts.engine.service.FloatingSubtitleManager

/**
 * 前台小说悬浮文字个性化定制面板：
 * 1. 实时排版所见即所得效果预览 (Live Preview)；
 * 2. 4 大背景风格质感预设 (深色磨砂 / 纯粹全透 / 暖阳羊皮纸 / 纯黑极夜)；
 * 3. 背景透明度细调 (0% ~ 100%)；
 * 4. 字体字号自由缩放 (11sp ~ 24sp) 与 5 大护眼高对比配色方案；
 * 5. 排版对齐 (居左/居中) 与默认折叠行数 (1/2/3/5 行)；
 * 6. 悬浮窗物理锁定开关（防听书翻页手滑误触移位）与动态发音图标显隐。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingSubtitleCustomizer(
    settings: GlobalSettings,
    configDataStore: ConfigDataStore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    fun updateAndApply(newSettings: GlobalSettings) {
        configDataStore.updateSettings(newSettings)
        FloatingSubtitleManager.getInstance(context).applySettings(newSettings)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==================== 1. 所见即所得排版预览 (Live Preview) ====================
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Preview,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "排版效果实时预览",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 模拟壁纸/书本背景底板
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (settings.floatingSubtitleBgStyle == "PARCHMENT") Color(0xFF1E1A16) else Color(0xFF0F1115)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 模拟悬浮字幕胶囊卡片
                    val previewBgColor = when (settings.floatingSubtitleBgStyle) {
                        "PURE_TRANSPARENT" -> Color.Transparent
                        "PARCHMENT" -> Color(0xFF2B231D).copy(alpha = settings.floatingSubtitleOpacity)
                        "AMOLED_BLACK" -> Color(0xFF000000).copy(alpha = settings.floatingSubtitleOpacity)
                        else -> Color(0xFF181A20).copy(alpha = settings.floatingSubtitleOpacity)
                    }

                    val previewStrokeColor = when (settings.floatingSubtitleBgStyle) {
                        "PURE_TRANSPARENT" -> Color.Transparent
                        "PARCHMENT" -> Color(0xFFFFD54F).copy(alpha = settings.floatingSubtitleOpacity * 0.4f)
                        else -> Color.White.copy(alpha = settings.floatingSubtitleOpacity * 0.35f)
                    }

                    val previewTextColor = try {
                        Color(android.graphics.Color.parseColor(settings.floatingSubtitleTextColor))
                    } catch (e: Exception) {
                        Color(0xFFF5F5F7)
                    }

                    val previewTextAlign = if (settings.floatingSubtitleAlignment == "CENTER") TextAlign.Center else TextAlign.Start

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .shadow(
                                elevation = if (settings.floatingSubtitleBgStyle == "PURE_TRANSPARENT") 0.dp else 4.dp,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(previewBgColor)
                            .border(1.dp, previewStrokeColor, RoundedCornerShape(14.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (settings.floatingSubtitleShowIcon) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = previewTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = "正在朗读：江南好，风景旧曾谙。日出江花红胜火，春来江水绿如蓝。能不忆江南？",
                            color = previewTextColor,
                            fontSize = settings.floatingSubtitleFontSize.sp,
                            maxLines = settings.floatingSubtitleMaxLines,
                            textAlign = previewTextAlign,
                            lineHeight = (settings.floatingSubtitleFontSize * 1.35).sp,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFB0B5C5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // ==================== 2. 背景风格质感预设 ====================
        Column {
            Text(
                text = "悬浮窗背景质感风格",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val bgStyles = listOf(
                "DARK_FROST" to "🌫️ 深色磨砂",
                "PURE_TRANSPARENT" to "🪟 纯粹全透",
                "PARCHMENT" to "📜 暖阳羊皮纸",
                "AMOLED_BLACK" to "🖤 纯黑极夜"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bgStyles.forEach { (styleKey, styleName) ->
                    val isSelected = settings.floatingSubtitleBgStyle == styleKey
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val nextOpacity = if (styleKey == "PURE_TRANSPARENT") 0.0f else if (settings.floatingSubtitleOpacity == 0f) 0.90f else settings.floatingSubtitleOpacity
                            updateAndApply(settings.copy(floatingSubtitleBgStyle = styleKey, floatingSubtitleOpacity = nextOpacity))
                        },
                        label = { Text(styleName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor.copy(alpha = 0.2f),
                            selectedLabelColor = primaryColor
                        )
                    )
                }
            }
        }

        // ==================== 3. 背景透明度调节 ====================
        if (settings.floatingSubtitleBgStyle != "PURE_TRANSPARENT") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("背景不透明度 (Opacity)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${(settings.floatingSubtitleOpacity * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                Slider(
                    value = settings.floatingSubtitleOpacity,
                    onValueChange = { updateAndApply(settings.copy(floatingSubtitleOpacity = it)) },
                    valueRange = 0.1f..1.0f,
                    steps = 18,
                    colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
                )
            }
        }

        HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ==================== 4. 字体字号大小调节 ====================
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("字体字号大小 (Font Size)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "${settings.floatingSubtitleFontSize} sp",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
            Slider(
                value = settings.floatingSubtitleFontSize.toFloat(),
                onValueChange = { updateAndApply(settings.copy(floatingSubtitleFontSize = it.toInt())) },
                valueRange = 11f..24f,
                steps = 13,
                colors = SliderDefaults.colors(thumbColor = primaryColor, activeTrackColor = primaryColor)
            )
        }

        // ==================== 5. 文字配色预设 ====================
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("文字高对比护眼配色", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val colorPresets = listOf(
                "#F5F5F7" to ("⚪ 象牙白" to Color(0xFFF5F5F7)),
                "#80DEEA" to ("🔵 极光青" to Color(0xFF80DEEA)),
                "#FFE082" to ("🟡 暖阳杏" to Color(0xFFFFE082)),
                "#A7F3D0" to ("🟢 护眼绿" to Color(0xFFA7F3D0)),
                "#FF8A80" to ("🔴 珊瑚粉" to Color(0xFFFF8A80))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colorPresets.forEach { (colorHex, pair) ->
                    val (label, colorObj) = pair
                    val isSelected = settings.floatingSubtitleTextColor.equals(colorHex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colorObj)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { updateAndApply(settings.copy(floatingSubtitleTextColor = colorHex)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (colorHex == "#F5F5F7" || colorHex == "#FFE082" || colorHex == "#A7F3D0") Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ==================== 6. 排版对齐与默认行数 ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("排版对齐方式", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = settings.floatingSubtitleAlignment == "LEFT",
                        onClick = { updateAndApply(settings.copy(floatingSubtitleAlignment = "LEFT")) },
                        label = { Text("居左自然", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = settings.floatingSubtitleAlignment == "CENTER",
                        onClick = { updateAndApply(settings.copy(floatingSubtitleAlignment = "CENTER")) },
                        label = { Text("居中歌词", fontSize = 11.sp) }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("折叠显示行数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1, 2, 3, 5).forEach { lines ->
                        FilterChip(
                            selected = settings.floatingSubtitleMaxLines == lines,
                            onClick = { updateAndApply(settings.copy(floatingSubtitleMaxLines = lines)) },
                            label = { Text("${lines}行", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 0.6.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ==================== 7. 防误触位置锁定 ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (settings.floatingSubtitleLockPosition) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (settings.floatingSubtitleLockPosition) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("锁定悬浮窗位置 (防翻页误触)", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    Text(
                        "锁定后禁止手指拖拽移位，避免看书翻页时意外带跑悬浮窗",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = settings.floatingSubtitleLockPosition,
                onCheckedChange = { updateAndApply(settings.copy(floatingSubtitleLockPosition = it)) }
            )
        }

        // ==================== 8. 显示播报状态动态图标 ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("显示播报指示图标", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    Text(
                        "在字幕左侧显示发声微光喇叭，关闭后为纯文字极简 HUD",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = settings.floatingSubtitleShowIcon,
                onCheckedChange = { updateAndApply(settings.copy(floatingSubtitleShowIcon = it)) }
            )
        }
    }
}
