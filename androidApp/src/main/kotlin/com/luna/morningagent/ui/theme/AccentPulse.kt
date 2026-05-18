package com.luna.morningagent.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

// Brand-consistent breathing pulse used by both the launch BreathingOrb and the
// home AgentStatusCard's status dot. One animation source so the two surfaces
// always feel like the same living agent.
@Composable
fun Modifier.accentPulse(
    minScale: Float = 1.0f,
    maxScale: Float = 1.06f,
    minAlpha: Float = 0.85f,
    maxAlpha: Float = 1.0f,
    durationMillis: Int = 2000,
): Modifier {
    val transition = rememberInfiniteTransition(label = "accentPulse")
    val scale by transition.animateFloat(
        initialValue  = minScale,
        targetValue   = maxScale,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = durationMillis, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue  = minAlpha,
        targetValue   = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = durationMillis, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .alpha(pulseAlpha)
}