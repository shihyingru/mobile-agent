package com.luna.morningagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Raw color tokens (spec: UI_PROTOTYPE.md § Color System) ---

val ColorBackground    = Color(0xFF0A0A0F)
val ColorSurface       = Color(0xFF14141C)
val ColorSurfaceRaised = Color(0xFF1C1C28)
val ColorBorder        = Color(0xFF26263A)

val ColorTextPrimary   = Color(0xFFE8E8F0)
val ColorTextSecondary = Color(0xFF8B8BA8)
val ColorTextMuted     = Color(0xFF5A5A75)

val ColorAccent        = Color(0xFFA78BFA)
val ColorAccentGlow    = Color(0x22A78BFA)

val ColorPriorityHigh  = Color(0xFFF87171)
val ColorPriorityMid   = Color(0xFFFBBF24)
val ColorPriorityLow   = Color(0xFF6EE7B7)

val ColorSuccess       = Color(0xFF4ADE80)
val ColorError         = Color(0xFFEF4444)

// --- Custom theme token bundle (accessed via MaterialTheme.morning) ---

data class MorningColors(
    val surface: Color,
    val surfaceRaised: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentGlow: Color,
    val priorityHigh: Color,
    val priorityMid: Color,
    val priorityLow: Color,
    val success: Color,
    val error: Color,
)

val LocalMorningColors = staticCompositionLocalOf {
    MorningColors(
        surface       = ColorSurface,
        surfaceRaised = ColorSurfaceRaised,
        border        = ColorBorder,
        textPrimary   = ColorTextPrimary,
        textSecondary = ColorTextSecondary,
        textMuted     = ColorTextMuted,
        accent        = ColorAccent,
        accentGlow    = ColorAccentGlow,
        priorityHigh  = ColorPriorityHigh,
        priorityMid   = ColorPriorityMid,
        priorityLow   = ColorPriorityLow,
        success       = ColorSuccess,
        error         = ColorError,
    )
}

val MaterialTheme.morning: MorningColors
    @Composable get() = LocalMorningColors.current
