package com.luna.morningagent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luna.morningagent.ui.home.HomeScreen
import com.luna.morningagent.ui.launch.LaunchScreen
import com.luna.morningagent.ui.settings.SettingsScreen
import com.luna.morningagent.ui.theme.MorningAgentTheme

private enum class Screen { Launch, Home, Settings }

@Composable
fun MorningAgentApp() {
    var screen by remember { mutableStateOf(Screen.Launch) }

    MorningAgentTheme {
        AnimatedContent(
            targetState  = screen,
            transitionSpec = {
                fadeIn(tween(250)) togetherWith fadeOut(tween(250))
            },
            label        = "screenTransition",
        ) { target ->
            when (target) {
                Screen.Launch   -> LaunchScreen(onReady = { screen = Screen.Home })
                Screen.Home     -> HomeScreen(onNavigateToSettings = { screen = Screen.Settings })
                Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
            }
        }
    }
}
