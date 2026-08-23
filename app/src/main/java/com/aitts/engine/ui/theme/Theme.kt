package com.aitts.engine.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppPalette = staticCompositionLocalOf { AppPaletteTheme.OCEAN_AZURE }

fun buildColorScheme(palette: AppPaletteTheme, isDark: Boolean): ColorScheme {
    val primary = palette.primaryColor
    val primaryDark = palette.previewColor

    return if (isDark) {
        val (bg, surface, card, border) = when (palette) {
            AppPaletteTheme.OCEAN_AZURE -> Quad(Color(0xFF090E17), Color(0xFF101726), Color(0xFF182338), Color(0xFF22314E))
            AppPaletteTheme.EMERALD_JADE -> Quad(Color(0xFF08120D), Color(0xFF0E1F17), Color(0xFF152D22), Color(0xFF1E4031))
            AppPaletteTheme.TITANIUM_SLATE -> Quad(Color(0xFF0D121D), Color(0xFF161E2E), Color(0xFF212B3E), Color(0xFF2E3D56))
            AppPaletteTheme.SUNSET_AMBER -> Quad(Color(0xFF130E07), Color(0xFF1F170D), Color(0xFF2E2214), Color(0xFF3F301D))
            AppPaletteTheme.MORANDI_GRAPHITE -> Quad(Color(0xFF101317), Color(0xFF1A1F26), Color(0xFF252D37), Color(0xFF343F4D))
            AppPaletteTheme.NEON_CYBERPUNK -> Quad(Color(0xFF150A18), Color(0xFF200F25), Color(0xFF2D1534), Color(0xFF451F4F))
            AppPaletteTheme.AURORA_MINT -> Quad(Color(0xFF071412), Color(0xFF0C201D), Color(0xFF132F2A), Color(0xFF1C423B))
            AppPaletteTheme.CHERRY_BLOSSOM -> Quad(Color(0xFF17090D), Color(0xFF240E14), Color(0xFF33141D), Color(0xFF4B1D2A))
            AppPaletteTheme.OBSIDIAN_NIGHT -> Quad(Color(0xFF0A0A16), Color(0xFF111124), Color(0xFF191933), Color(0xFF26264C))
            AppPaletteTheme.CORAL_CRIMSON -> Quad(Color(0xFF170A06), Color(0xFF241009), Color(0xFF33170D), Color(0xFF4A2213))
        }

        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = primaryDark,
            background = bg,
            onBackground = Color(0xFFF1F5F9),
            surface = surface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = card,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f)
        )
    } else {
        val (bg, surface, card, border) = when (palette) {
            AppPaletteTheme.OCEAN_AZURE -> Quad(Color(0xFFF0F4F8), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE2E8F0))
            AppPaletteTheme.EMERALD_JADE -> Quad(Color(0xFFF0F7F4), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE2EBE5))
            AppPaletteTheme.TITANIUM_SLATE -> Quad(Color(0xFFF8FAFC), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE2E8F0))
            AppPaletteTheme.SUNSET_AMBER -> Quad(Color(0xFFFDF8F3), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFEDE3D8))
            AppPaletteTheme.MORANDI_GRAPHITE -> Quad(Color(0xFFECEFF1), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFDCE1E4))
            AppPaletteTheme.NEON_CYBERPUNK -> Quad(Color(0xFFFDF4FF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF5D0FE))
            AppPaletteTheme.AURORA_MINT -> Quad(Color(0xFFF0FDFA), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFCCFBF1))
            AppPaletteTheme.CHERRY_BLOSSOM -> Quad(Color(0xFFFFF1F2), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFE4E6))
            AppPaletteTheme.OBSIDIAN_NIGHT -> Quad(Color(0xFFF5F5FF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFE0E0FE))
            AppPaletteTheme.CORAL_CRIMSON -> Quad(Color(0xFFFFF7ED), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFEDD5))
        }

        lightColorScheme(
            primary = primaryDark,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.12f),
            onPrimaryContainer = primaryDark,
            secondary = primary,
            background = bg,
            onBackground = Color(0xFF0F172A),
            surface = surface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = card,
            onSurfaceVariant = Color(0xFF64748B),
            outline = border,
            outlineVariant = border.copy(alpha = 0.7f)
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun AiTtsEngineTheme(
    themeMode: String = "SYSTEM",
    themePalette: String = "OCEAN_AZURE",
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val palette = AppPaletteTheme.fromKey(themePalette)
    val colorScheme = buildColorScheme(palette, isDark)

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
