package com.luna.morningagent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun MorningAgentApp(notificationTick: Int = 0) {
    // Skip the Launch screen entirely when the activity was opened from a
    // briefing notification — landing on Launch would force Luna to wait
    // through an orb animation just to read the thing she was already
    // looking at in the shade.
    var screen by remember {
        mutableStateOf(if (notificationTick > 0) Screen.Home else Screen.Launch)
    }

    // onNewIntent: notification tapped while the activity is already running
    // (possibly on Settings). Each tap increments the tick; we react by
    // snapping back to Home, since the notification means "show me my briefing."
    LaunchedEffect(notificationTick) {
        if (notificationTick > 0) screen = Screen.Home
    }

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
