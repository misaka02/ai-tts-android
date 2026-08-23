package com.aitts.engine.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aitts.engine.data.ProviderType

// --- 10 套高品质预设主题色系方案 ---
enum class AppPaletteTheme(val key: String, val title: String, val primaryColor: Color, val previewColor: Color) {
    OCEAN_AZURE("OCEAN_AZURE", "深海曜蓝", Color(0xFF0EA5E9), Color(0xFF0284C7)),
    EMERALD_JADE("EMERALD_JADE", "极客翡翠", Color(0xFF10B981), Color(0xFF059669)),
    TITANIUM_SLATE("TITANIUM_SLATE", "钛金极简", Color(0xFF64748B), Color(0xFF475569)),
    SUNSET_AMBER("SUNSET_AMBER", "落日暖金", Color(0xFFF59E0B), Color(0xFFD97706)),
    MORANDI_GRAPHITE("MORANDI_GRAPHITE", "莫兰迪灰", Color(0xFF546E7A), Color(0xFF37474F)),
    NEON_CYBERPUNK("NEON_CYBERPUNK", "赛博霓虹", Color(0xFFD946EF), Color(0xFFC026D3)),
    AURORA_MINT("AURORA_MINT", "极光薄荷", Color(0xFF14B8A6), Color(0xFF0D9488)),
    CHERRY_BLOSSOM("CHERRY_BLOSSOM", "樱花幽粉", Color(0xFFF43F5E), Color(0xFFE11D48)),
    OBSIDIAN_NIGHT("OBSIDIAN_NIGHT", "暗夜曜石", Color(0xFF6366F1), Color(0xFF4F46E5)),
    CORAL_CRIMSON("CORAL_CRIMSON", "炽阳枫红", Color(0xFFEA580C), Color(0xFFC2410C));

    companion object {
        fun fromKey(key: String): AppPaletteTheme {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: OCEAN_AZURE
        }
    }
}

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
