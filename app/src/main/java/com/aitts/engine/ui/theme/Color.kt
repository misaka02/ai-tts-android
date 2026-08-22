package com.aitts.engine.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aitts.engine.data.ProviderType

// --- 现代化核心主色调 (Electric Indigo & Radiant Azure) ---
val PrimaryIndigo = Color(0xFF6366F1)
val PrimaryIndigoDark = Color(0xFF4F46E5)
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueDark = Color(0xFF2563EB)
val AccentCyan = Color(0xFF06B6D4)
val AccentEmerald = Color(0xFF10B981)
val AccentRose = Color(0xFFF43F5E)
val AccentAmber = Color(0xFFF59E0B)

// --- 界面基底色调 (Slate & Obsidian) ---
val SlateBackgroundLight = Color(0xFFF8FAFC)
val SlateSurfaceLight = Color(0xFFFFFFFF)
val SlateBorderLight = Color(0xFFE2E8F0)

val SlateBackgroundDark = Color(0xFF0F172A)
val SlateSurfaceDark = Color(0xFF1E293B)
val SlateCardDark = Color(0xFF334155)
val SlateBorderDark = Color(0xFF475569)

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
            colors = listOf(color, color.copy(alpha = 0.75f))
        )
    }
}
