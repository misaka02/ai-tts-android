package com.aitts.engine.ui.pulse.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * ⚡ Pulse 极光设计系统视觉令牌 (Pulse Design System Tokens)
 * 高对比度赛博流体美学与 AMOLED 纯黑底层
 */
object PulseTokens {
    // 🎨 核心主题色盘
    val CyanElectric = Color(0xFF00F2FE)
    val SonicBlue = Color(0xFF4FACFE)
    val MagentaLaser = Color(0xFFFF0844)
    val AcidGreen = Color(0xFF00FF87)
    val AmberWarm = Color(0xFFFFB300)

    // 🌌 极夜背景与卡片表面
    val CanvasDeep = Color(0xFF06080F)
    val SurfaceDark = Color(0xFF0D111C)
    val SurfaceCard = Color(0xFF131826)
    val SurfaceElevated = Color(0xFF1A2133)
    val SurfaceCardActive = Color(0xFF1E283D)

    // 🪟 边框与微光
    val BorderSubtle = BorderStroke(1.dp, Color(0xFF222B42))
    val BorderActive = BorderStroke(1.5.dp, CyanElectric)
    val BorderGlow = BorderStroke(1.dp, Brush.horizontalGradient(listOf(CyanElectric, SonicBlue)))

    // 📝 文本高对比度阶梯
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA0AEC0)
    val TextTertiary = Color(0xFF64748B)

    // 🌟 渐变画刷
    val CoreGradient = Brush.radialGradient(
        colors = listOf(
            CyanElectric.copy(alpha = 0.35f),
            SonicBlue.copy(alpha = 0.15f),
            Color.Transparent
        )
    )

    val BrandPillGradient = Brush.horizontalGradient(
        colors = listOf(CyanElectric, SonicBlue)
    )

    val CardCornerRadius = 18.dp
}

@Composable
fun PulseCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(PulseTokens.CardCornerRadius),
    backgroundColor: Color = PulseTokens.SurfaceCard,
    border: BorderStroke? = PulseTokens.BorderSubtle,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        border = border
    ) {
        content()
    }
}
