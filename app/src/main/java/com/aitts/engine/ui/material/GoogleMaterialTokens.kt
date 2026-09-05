package com.aitts.engine.ui.material

import androidx.compose.ui.graphics.Color

/**
 * 🌟 Google 官方 Material 3 核心设计规范调色板与材质令牌 (Google Material You Tokens)
 * 严格遵循 Google Recorder、Google Pixel 设置、Google Keep 等官方应用设计语言：
 * 1. 扁平、高可读性、高质感 Tonal Surfaces，杜绝任何高饱和赛博朋克深空黑底与杂乱 AI 霓虹；
 * 2. 官方标准药丸（Pill）与圆角容器（20dp ~ 28dp）；
 * 3. 经典 Google 蓝 (#1A73E8 / #8AB4F8) 搭配红/黄/绿状态指示。
 */
data class GoogleColors(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val outline: Color,
    val outlineSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val googleBlue: Color,
    val googleRed: Color,
    val googleYellow: Color,
    val googleGreen: Color
)

object GoogleMaterialTokens {
    fun resolve(isDark: Boolean): GoogleColors {
        return if (isDark) {
            GoogleColors(
                isDark = true,
                primary = Color(0xFF8AB4F8),
                onPrimary = Color(0xFF062E6F),
                primaryContainer = Color(0xFF0842A0),
                onPrimaryContainer = Color(0xFFD3E3FD),
                background = Color(0xFF121316),
                surface = Color(0xFF1E1F24),
                surfaceContainer = Color(0xFF23252A),
                surfaceContainerHigh = Color(0xFF2C2E34),
                surfaceContainerHighest = Color(0xFF35373E),
                outline = Color(0xFF44474E),
                outlineSubtle = Color(0xFF2E3137),
                textPrimary = Color(0xFFE2E2E6),
                textSecondary = Color(0xFFC4C7C5),
                textTertiary = Color(0xFF8E918F),
                googleBlue = Color(0xFF8AB4F8),
                googleRed = Color(0xFFF28B82),
                googleYellow = Color(0xFFFDD663),
                googleGreen = Color(0xFF81C995)
            )
        } else {
            GoogleColors(
                isDark = false,
                primary = Color(0xFF0B57D0),
                onPrimary = Color(0xFFFFFFFF),
                primaryContainer = Color(0xFFD3E3FD),
                onPrimaryContainer = Color(0xFF041E49),
                background = Color(0xFFF8F9FA),
                surface = Color(0xFFFFFFFF),
                surfaceContainer = Color(0xFFF0F4F9),
                surfaceContainerHigh = Color(0xFFE9EEF6),
                surfaceContainerHighest = Color(0xFFE0E6F0),
                outline = Color(0xFFD3D6DC),
                outlineSubtle = Color(0xFFEAECEF),
                textPrimary = Color(0xFF1F1F1F),
                textSecondary = Color(0xFF444746),
                textTertiary = Color(0xFF747775),
                googleBlue = Color(0xFF1A73E8),
                googleRed = Color(0xFFEA4335),
                googleYellow = Color(0xFFFBBC04),
                googleGreen = Color(0xFF1E8E3E)
            )
        }
    }
}
