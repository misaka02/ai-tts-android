package com.aitts.engine.ui.pulse.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * ⚡ Pulse 动态视觉色彩令牌 (支持完整 Light / Dark Mode 与 8 种个性化调色板)
 */
@Immutable
data class PulseDynamicColors(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val canvasDeep: Color,
    val surfaceDark: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceCardActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val borderSubtle: BorderStroke,
    val borderActive: BorderStroke,
    val brandPillGradient: Brush
)

val LocalPulseColors = compositionLocalOf { PulseTokens.defaultColors }

/**
 * Pulse 极光设计系统视觉令牌 (Pulse Design System Tokens)
 */
object PulseTokens {
    // 🎨 专业原生色系
    val DefaultCyanElectric = Color(0xFF38BDF8)
    val DefaultSonicBlue = Color(0xFF0284C7)
    val DefaultMagentaLaser = Color(0xFFE11D48)
    val AcidGreen = Color(0xFF10B981)
    val AmberWarm = Color(0xFFF59E0B)

    val defaultColors = PulseDynamicColors(
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF0284C7),
        accent = Color(0xFFE11D48),
        canvasDeep = Color(0xFF0B0F19),
        surfaceDark = Color(0xFF111827),
        surfaceCard = Color(0xFF182234),
        surfaceElevated = Color(0xFF1F293D),
        surfaceCardActive = Color(0xFF24324A),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFA0AEC0),
        textTertiary = Color(0xFF64748B),
        borderSubtle = BorderStroke(1.dp, Color(0xFF1E293B)),
        borderActive = BorderStroke(1.dp, Color(0xFF38BDF8)),
        brandPillGradient = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
    )

    fun resolveDynamicColors(palette: String, isAmoledPureBlack: Boolean, themeMode: String = "DARK"): PulseDynamicColors {
        val isLight = themeMode.uppercase() == "LIGHT"

        val (primary, secondary, accent) = when (palette.uppercase()) {
            "EMERALD_JADE" -> if (isLight) Triple(Color(0xFF059669), Color(0xFF10B981), Color(0xFFD97706))
                             else Triple(Color(0xFF10B981), Color(0xFF34D399), Color(0xFFF59E0B))
            "TITANIUM_SLATE" -> if (isLight) Triple(Color(0xFF0284C7), Color(0xFF475569), Color(0xFF0EA5E9))
                               else Triple(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF38BDF8))
            "SUNSET_AMBER" -> if (isLight) Triple(Color(0xFFEA580C), Color(0xFFF97316), Color(0xFFD97706))
                             else Triple(Color(0xFFF59E0B), Color(0xFFEA580C), Color(0xFFFBBF24))
            "NEON_CYBER" -> if (isLight) Triple(Color(0xFF7C3AED), Color(0xFF0284C7), Color(0xFFE11D48))
                            else Triple(Color(0xFFA855F7), Color(0xFF38BDF8), Color(0xFFF43F5E))
            "SAKURA_PINK" -> if (isLight) Triple(Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFF2563EB))
                             else Triple(Color(0xFFFB7185), Color(0xFFFDA4AF), Color(0xFF60A5FA))
            "AMETHYST_PURPLE" -> if (isLight) Triple(Color(0xFF7C3AED), Color(0xFF4F46E5), Color(0xFF0284C7))
                                else Triple(Color(0xFF818CF8), Color(0xFF6366F1), Color(0xFF38BDF8))
            "MORANDI_GRAPHITE" -> if (isLight) Triple(Color(0xFF475569), Color(0xFF7C3AED), Color(0xFFB45309))
                                  else Triple(Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFD97706))
            else -> if (isLight) Triple(Color(0xFF0284C7), Color(0xFF2563EB), Color(0xFFE11D48))
                    else Triple(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFFE11D48)) // OCEAN_AZURE default
        }

        val canvasDeep = when {
            isLight -> Color(0xFFF8FAFC)
            isAmoledPureBlack -> Color(0xFF000000)
            else -> Color(0xFF0B0F19)
        }
        val surfaceDark = when {
            isLight -> Color(0xFFF1F5F9)
            isAmoledPureBlack -> Color(0xFF060606)
            else -> Color(0xFF111827)
        }
        val surfaceCard = when {
            isLight -> Color(0xFFFFFFFF)
            isAmoledPureBlack -> Color(0xFF0D0D0D)
            else -> Color(0xFF182234)
        }
        val surfaceElevated = when {
            isLight -> Color(0xFFF1F5F9)
            isAmoledPureBlack -> Color(0xFF141414)
            else -> Color(0xFF1F293D)
        }
        val surfaceCardActive = when {
            isLight -> Color(0xFFE2E8F0)
            isAmoledPureBlack -> Color(0xFF1C1C1C)
            else -> Color(0xFF24324A)
        }
        val textPrimary = if (isLight) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        val textSecondary = if (isLight) Color(0xFF475569) else Color(0xFFA0AEC0)
        val textTertiary = if (isLight) Color(0xFF94A3B8) else Color(0xFF64748B)

        val borderSubtle = when {
            isLight -> BorderStroke(1.dp, Color(0xFFE2E8F0))
            isAmoledPureBlack -> BorderStroke(1.dp, Color(0xFF222222))
            else -> BorderStroke(1.dp, Color(0xFF1E293B))
        }
        val borderActive = BorderStroke(1.dp, primary)
        val brandPillGradient = Brush.horizontalGradient(listOf(primary, secondary))

        return PulseDynamicColors(
            primary = primary,
            secondary = secondary,
            accent = accent,
            canvasDeep = canvasDeep,
            surfaceDark = surfaceDark,
            surfaceCard = surfaceCard,
            surfaceElevated = surfaceElevated,
            surfaceCardActive = surfaceCardActive,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            textTertiary = textTertiary,
            borderSubtle = borderSubtle,
            borderActive = borderActive,
            brandPillGradient = brandPillGradient
        )
    }

    // 🎨 动态属性访问器 (自动响应当前 LocalPulseColors)
    val CyanElectric: Color @Composable get() = LocalPulseColors.current.primary
    val SonicBlue: Color @Composable get() = LocalPulseColors.current.secondary
    val MagentaLaser: Color @Composable get() = LocalPulseColors.current.accent

    // 🌌 极夜背景与卡片表面
    val CanvasDeep: Color @Composable get() = LocalPulseColors.current.canvasDeep
    val SurfaceDark: Color @Composable get() = LocalPulseColors.current.surfaceDark
    val SurfaceCard: Color @Composable get() = LocalPulseColors.current.surfaceCard
    val SurfaceElevated: Color @Composable get() = LocalPulseColors.current.surfaceElevated
    val SurfaceCardActive: Color @Composable get() = LocalPulseColors.current.surfaceCardActive

    // 🪟 边框与微光
    val BorderSubtle: BorderStroke @Composable get() = LocalPulseColors.current.borderSubtle
    val BorderActive: BorderStroke @Composable get() = LocalPulseColors.current.borderActive
    val BorderGlow: BorderStroke @Composable get() = BorderStroke(1.dp, LocalPulseColors.current.brandPillGradient)

    // 📝 文本高对比度阶梯
    val TextPrimary: Color @Composable get() = LocalPulseColors.current.textPrimary
    val TextSecondary: Color @Composable get() = LocalPulseColors.current.textSecondary
    val TextTertiary: Color @Composable get() = LocalPulseColors.current.textTertiary

    // 🌟 渐变画刷
    val CoreGradient: Brush @Composable get() = Brush.radialGradient(
        colors = listOf(
            LocalPulseColors.current.primary.copy(alpha = 0.35f),
            LocalPulseColors.current.secondary.copy(alpha = 0.15f),
            Color.Transparent
        )
    )

    val BrandPillGradient: Brush @Composable get() = LocalPulseColors.current.brandPillGradient

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
