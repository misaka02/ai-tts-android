package com.aitts.engine.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = AccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = AccentEmerald,
    background = SlateBackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SlateSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = SlateBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryIndigoDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFEFF),
    onSecondaryContainer = Color(0xFF155E75),
    tertiary = AccentEmerald,
    background = SlateBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = SlateSurfaceLight,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = SlateBorderLight
)

@Composable
fun AiTtsEngineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 禁用 Android 12 壁纸动态取色，保持统一高品质主题质感
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
