package com.aitts.engine.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aitts.engine.data.ProviderType

// --- 现代化核心主色调 (Electric Indigo & Radiant Cyan) ---
val PrimaryIndigo = Color(0xFF6366F1)
val PrimaryIndigoDark = Color(0xFF4F46E5)
val PrimaryIndigoLight = Color(0xFF818CF8)
val AccentCyan = Color(0xFF06B6D4)
val AccentEmerald = Color(0xFF10B981)
val AccentRose = Color(0xFFF43F5E)
val AccentAmber = Color(0xFFF59E0B)

// --- 深色模式色盘 (Deep Obsidian & Night Slate) ---
val ObsidianBackground = Color(0xFF090D16)
val ObsidianSurface = Color(0xFF111726)
val ObsidianCard = Color(0xFF182236)
val ObsidianCardElevated = Color(0xFF1F2C46)
val ObsidianBorder = Color(0xFF263554)
val ObsidianTextPrimary = Color(0xFFF1F5F9)
val ObsidianTextSecondary = Color(0xFF94A3B8)

// --- 浅色模式色盘 (Crisp Snow & Soft Slate) ---
val SnowBackground = Color(0xFFF4F6F9)
val SnowSurface = Color(0xFFFFFFFF)
val SnowCard = Color(0xFFFFFFFF)
val SnowCardElevated = Color(0xFFF8FAFC)
val SnowBorder = Color(0xFFE2E8F0)
val SnowTextPrimary = Color(0xFF0F172A)
val SnowTextSecondary = Color(0xFF64748B)

// --- 状态提示色 ---
val SuccessGreen = Color(0xFF22C55E)
val WarningOrange = Color(0xFFF97316)
val ErrorRed = Color(0xFFEF4444)

// --- 各 AI 大模型专属品牌色与渐变 ---
object BrandTheme {
    val MiMoColor = Color(0xFFFF6900)
    val EdgeColor = Color(0xFF0078D4)
    val GeminiColor = Color(0xFF4285F4)
    val MiniMaxColor = Color(0xFFF59E0B)
    val DoubaoColor = Color(0xFFFF3B30)
    val SiliconFlowColor = Color(0xFF10B981)
    val FishAudioColor = Color(0xFF06B6D4)
    val StepFunColor = Color(0xFF8B5CF6)
    val OpenAiColor = Color(0xFF10A37F)
    val CustomColor = Color(0xFF64748B)

    fun getColorForType(type: ProviderType): Color {
        return when (type) {
            ProviderType.MIMO -> MiMoColor
            ProviderType.EDGE_TTS -> EdgeColor
            ProviderType.AZURE -> EdgeColor
            ProviderType.GEMINI -> GeminiColor
            ProviderType.MINIMAX -> MiniMaxColor
            ProviderType.DOUBAO -> DoubaoColor
            ProviderType.SILICONFLOW -> SiliconFlowColor
            ProviderType.FISH_AUDIO -> FishAudioColor
            ProviderType.STEPFUN -> StepFunColor
            ProviderType.OPENAI -> OpenAiColor
            ProviderType.CUSTOM_HTTP -> CustomColor
            else -> CustomColor
        }
    }

    fun getGradientForType(type: ProviderType): Brush {
        val color = getColorForType(type)
        return Brush.horizontalGradient(
            colors = listOf(color, color.copy(alpha = 0.7f))
        )
    }
}
