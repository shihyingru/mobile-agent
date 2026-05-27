package com.luna.morningagent

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luna.morningagent.ui.home.HomeScreen
import com.luna.morningagent.ui.launch.LaunchScreen
import com.luna.morningagent.ui.settings.SettingsScreen
import com.luna.morningagent.ui.sharedposts.SavedPostsScreen
import com.luna.morningagent.ui.tempplan.TempPlanScreen
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.resolveTheme
import java.time.LocalDateTime
import kotlinx.coroutines.delay

private enum class Screen { Launch, Home, Settings, SavedPosts, TempPlan }

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

    // Tick the wall clock once a minute so resolveTheme() picks up slot
    // transitions live (Dawn → Midday at 09:00, Midday → Afternoon at 12:00,
    // etc.). derivedStateOf keeps the theme reference stable when the slot
    // doesn't change, so a 60s tick that lands inside the same slot doesn't
    // cause a global recomposition cascade.
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = LocalDateTime.now()
        }
    }
    val theme by remember { derivedStateOf { resolveTheme(now) } }

    MorningAgentTheme(theme = theme) {
        AnimatedContent(
            targetState  = screen,
            transitionSpec = {
                fadeIn(tween(250)) togetherWith fadeOut(tween(250))
            },
            label        = "screenTransition",
        ) { target ->
            when (target) {
                Screen.Launch     -> LaunchScreen(onReady = { screen = Screen.Home })
                Screen.Home       -> HomeScreen(
                    onNavigateToSettings   = { screen = Screen.Settings },
                    onNavigateToSavedPosts = { screen = Screen.SavedPosts },
                    onNavigateToTempPlan   = { screen = Screen.TempPlan },
                )
                Screen.Settings   -> SettingsScreen(onBack = { screen = Screen.Home })
                Screen.SavedPosts -> SavedPostsScreen(onBack = { screen = Screen.Home })
                Screen.TempPlan   -> TempPlanScreen(onBack = { screen = Screen.Home })
            }
        }
    }
}
