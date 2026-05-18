package com.luna.morningagent.ui.launch.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.morning

// v4 launch orb — simpler than v3. Single accent → accentDeep gradient body
// with a soft halo and a pearl core overlay that takes the theme's text tint
// (white on light, sand on dark). Breathes via an inline scale animation
// (separate from Modifier.accentPulse so the launch orb keeps its 2.6s
// cadence — slower than the home status dot's 1.6s ripple).
@Composable
fun BreathingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 110.dp,
) {
    val morning = MaterialTheme.morning
    val accent  = morning.accent
    val deep    = morning.accentDeep
    val isLight = morning.isLight

    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue  = 1.0f,
        targetValue   = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    // Halo opacity tracks the same breath so the orb visibly inhales.
    val haloAlpha by transition.animateFloat(
        initialValue  = 0.22f,
        targetValue   = 0.36f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo",
    )

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f
        val highlight = Offset(this.size.width * 0.36f, this.size.height * 0.30f)

        // Outer halo — diffuse accent fade.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = haloAlpha),
                    accent.copy(alpha = haloAlpha * 0.3f),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 1.6f,
            ),
            radius = radius * 1.6f,
            center = center,
        )

        // Pigment body — accent → deep gradient.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent, deep),
                center = highlight,
                radius = radius * 1.1f,
            ),
            radius = radius,
            center = center,
        )

        // Pearl overlay — bright tint (white on light theme, sand on dark) drawn
        // upper-left so the orb reads as lit from above.
        val pearlColor = if (isLight) Color.White else Color(0xFFE5E1DD)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    pearlColor.copy(alpha = 0.95f),
                    pearlColor.copy(alpha = 0.30f),
                    pearlColor.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = highlight,
                radius = radius * 0.85f,
            ),
            radius = radius * 0.85f,
            center = highlight,
        )

        // Specular highlight — small bright dot for the lit edge.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                center = highlight,
                radius = radius * 0.20f,
            ),
            radius = radius * 0.20f,
            center = highlight,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun BreathingOrbPreview() {
    MorningAgentTheme {
        BreathingOrb()
    }
}
