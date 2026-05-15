package com.luna.morningagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import java.time.LocalDateTime

@Composable
fun MorningAgentTheme(
    theme: MorningTheme = remember { resolveTheme(LocalDateTime.now()) },
    content: @Composable () -> Unit,
) {
    val palette = theme.palette
    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary          = palette.accent,
            background       = palette.background,
            surface          = palette.surface,
            surfaceVariant   = palette.surfaceRaised,
            error            = palette.error,
            onPrimary        = palette.accentOnButton,
            onBackground     = palette.textPrimary,
            onSurface        = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline          = palette.border,
        )
    } else {
        darkColorScheme(
            primary          = palette.accent,
            background       = palette.background,
            surface          = palette.surface,
            surfaceVariant   = palette.surfaceRaised,
            error            = palette.error,
            onPrimary        = palette.accentOnButton,
            onBackground     = palette.textPrimary,
            onSurface        = palette.textPrimary,
            onSurfaceVariant = palette.textSecondary,
            outline          = palette.border,
        )
    }

    CompositionLocalProvider(LocalMorningTheme provides theme) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}