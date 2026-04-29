package com.luna.morningagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val MorningColorScheme = darkColorScheme(
    primary          = ColorAccent,
    background       = ColorBackground,
    surface          = ColorSurface,
    surfaceVariant   = ColorSurfaceRaised,
    error            = ColorError,
    onPrimary        = ColorBackground,
    onBackground     = ColorTextPrimary,
    onSurface        = ColorTextPrimary,
    onSurfaceVariant = ColorTextSecondary,
    outline          = ColorBorder,
)

@Composable
fun MorningAgentTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMorningColors provides LocalMorningColors.current) {
        MaterialTheme(
            colorScheme = MorningColorScheme,
            content     = content,
        )
    }
}
