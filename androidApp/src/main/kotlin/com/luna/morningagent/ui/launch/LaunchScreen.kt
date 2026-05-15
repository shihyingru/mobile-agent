package com.luna.morningagent.ui.launch

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.ui.launch.components.BreathingOrb
import com.luna.morningagent.ui.launch.components.LoadingDots
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LaunchScreen(onReady: () -> Unit) {
    val morning = MaterialTheme.morning

    // Animated alpha values for each element
    val orbAlpha      = remember { Animatable(0f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val taglineAlpha  = remember { Animatable(0f) }
    var dotsVisible by remember { mutableStateOf(false) }

    // Wordmark slides up 8dp as it fades in
    val wordmarkOffset = remember { Animatable(8f) }

    LaunchedEffect(Unit) {
        // t=100ms: orb fades in + scales (scale is continuous in BreathingOrb)
        delay(100)
        launch { orbAlpha.animateTo(1f, tween(300, easing = EaseOut)) }

        // t=500ms: wordmark fades in + slides up
        delay(400)
        launch { wordmarkAlpha.animateTo(1f, tween(400, easing = EaseOut)) }
        launch { wordmarkOffset.animateTo(0f, tween(400, easing = EaseOut)) }

        // t=800ms: tagline fades in
        delay(300)
        launch { taglineAlpha.animateTo(1f, tween(300, easing = EaseOut)) }

        // t=1000ms: loading dots appear
        delay(200)
        dotsVisible = true

        // t=1200ms: transition out — MorningAgentApp handles the cross-fade
        delay(200)
        onReady()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(morning.background),
    ) {
        Column(
            modifier            = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BreathingOrb(
                modifier = Modifier.alpha(orbAlpha.value),
                size     = 96.dp,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text     = stringResource(R.string.app_name),
                style    = MorningType.Display,
                color    = morning.textPrimary,
                modifier = Modifier
                    .alpha(wordmarkAlpha.value)
                    .padding(bottom = wordmarkOffset.value.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text     = stringResource(R.string.tagline),
                style    = MorningType.Body,
                color    = morning.textSecondary,
                modifier = Modifier.alpha(taglineAlpha.value),
            )
        }

        if (dotsVisible) {
            LoadingDots(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F, name = "Launch Screen")
@Composable
private fun LaunchScreenPreview() {
    MorningAgentTheme {
        LaunchScreen(onReady = {})
    }
}
