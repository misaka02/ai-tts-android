package com.aitts.engine.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppPalette = staticCompositionLocalOf { AppPaletteTheme.OCEAN_AZURE }

fun buildColorScheme(palette: AppPaletteTheme, isDark: Boolean, isAmoledPureBlack: Boolean = false): ColorScheme {
    val primary = palette.primaryColor
    val primaryDark = palette.previewColor

    return if (isDark) {
        val (bg, surface, card, border) = if (isAmoledPureBlack || palette == AppPaletteTheme.OLED_BLACK) {
            Quad(Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFF141414), Color(0xFF262626))
        } else {
            when (palette) {
                AppPaletteTheme.OCEAN_AZURE -> Quad(Color(0xFF070B12), Color(0xFF101726), Color(0xFF182338), Color(0xFF2B3D5C))
                AppPaletteTheme.EMERALD_JADE -> Quad(Color(0xFF050E09), Color(0xFF0C1D15), Color(0xFF142B20), Color(0xFF244837))
                AppPaletteTheme.TITANIUM_SLATE -> Quad(Color(0xFF090D15), Color(0xFF141B28), Color(0xFF1F293B), Color(0xFF33435C))
                AppPaletteTheme.SUNSET_AMBER -> Quad(Color(0xFF0F0A04), Color(0xFF1A1308), Color(0xFF291E10), Color(0xFF45341E))
                AppPaletteTheme.MORANDI_GRAPHITE -> Quad(Color(0xFF0C0E12), Color(0xFF161B22), Color(0xFF222832), Color(0xFF37414F))
                AppPaletteTheme.NEON_CYBERPUNK -> Quad(Color(0xFF100512), Color(0xFF1C0A20), Color(0xFF2B1032), Color(0xFF4C1D57))
                AppPaletteTheme.AURORA_MINT -> Quad(Color(0xFF040F0D), Color(0xFF0A1B18), Color(0xFF112925), Color(0xFF1F453F))
                AppPaletteTheme.CHERRY_BLOSSOM -> Quad(Color(0xFF110508), Color(0xFF1E0B10), Color(0xFF2E121A), Color(0xFF4E202C))
                AppPaletteTheme.OBSIDIAN_NIGHT -> Quad(Color(0xFF070712), Color(0xFF0F0F20), Color(0xFF181830), Color(0xFF2C2C52))
                AppPaletteTheme.CORAL_CRIMSON -> Quad(Color(0xFF110603), Color(0xFF1F0C06), Color(0xFF2E140B), Color(0xFF4C2415))
                AppPaletteTheme.OLED_BLACK -> Quad(Color(0xFF000000), Color(0xFF0D0D0D), Color(0xFF141414), Color(0xFF262626))
            }
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
            onSurfaceVariant = Color(0xFFA0AEC0),
            outline = border,
            outlineVariant = border.copy(alpha = 0.7f)
        )
    } else {
        val (bg, surface, card, border) = when (palette) {
            AppPaletteTheme.OCEAN_AZURE -> Quad(Color(0xFFF1F5F9), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFCBD5E1))
            AppPaletteTheme.EMERALD_JADE -> Quad(Color(0xFFF0FDF4), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFBBF7D0))
            AppPaletteTheme.TITANIUM_SLATE -> Quad(Color(0xFFF8FAFC), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFCBD5E1))
            AppPaletteTheme.SUNSET_AMBER -> Quad(Color(0xFFFFFBEB), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFDE68A))
            AppPaletteTheme.MORANDI_GRAPHITE -> Quad(Color(0xFFF3F4F6), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFD1D5DB))
            AppPaletteTheme.NEON_CYBERPUNK -> Quad(Color(0xFFFDF4FF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFF5D0FE))
            AppPaletteTheme.AURORA_MINT -> Quad(Color(0xFFF0FDFA), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF99F6E4))
            AppPaletteTheme.CHERRY_BLOSSOM -> Quad(Color(0xFFFFF1F2), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFECDD3))
            AppPaletteTheme.OBSIDIAN_NIGHT -> Quad(Color(0xFFF5F5FF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFC7D2FE))
            AppPaletteTheme.CORAL_CRIMSON -> Quad(Color(0xFFFFF7ED), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFED7AA))
            AppPaletteTheme.OLED_BLACK -> Quad(Color(0xFFF4F4F5), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFD4D4D8))
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
            onSurfaceVariant = Color(0xFF475569),
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f)
        )
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun AiTtsEngineTheme(
    themeMode: String = "SYSTEM",
    themePalette: String = "OCEAN_AZURE",
    isAmoledPureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val palette = AppPaletteTheme.fromKey(themePalette)
    val colorScheme = buildColorScheme(palette, isDark, isAmoledPureBlack)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val statusBarColor = if (isAmoledPureBlack && isDark) android.graphics.Color.BLACK else colorScheme.background.toArgb()
            val navBarColor = if (isAmoledPureBlack && isDark) android.graphics.Color.BLACK else colorScheme.surface.toArgb()
            window.statusBarColor = statusBarColor
            window.navigationBarColor = navBarColor
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
