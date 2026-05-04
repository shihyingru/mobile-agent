package com.luna.morningagent.ui.launch.components

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
import com.luna.morningagent.ui.theme.ColorAccent
import com.luna.morningagent.ui.theme.MorningAgentTheme

// Reusable pulsing orb — also used (at smaller size) for the agent status dot on HomeScreen.
@Composable
fun BreathingOrb(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    color: Color = ColorAccent,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbScale",
    )
    // Glow opacity pulses slightly in sync with scale — brighter at peak
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue  = 0.32f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbGlow",
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

        // Outer diffuse glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                center = center,
                radius = radius * 1.8f,
            ),
            radius = radius * 1.8f,
            center = center,
        )

        // Orb body — radial gradient offset toward upper-left for subtle 3-D depth
        val highlightCenter = Offset(this.size.width * 0.36f, this.size.height * 0.30f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFD4BFFF),          // bright highlight
                    color,                       // main accent mid-tone
                    Color(0xFF6D4FC7),           // deeper violet at edge
                ),
                center = highlightCenter,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun BreathingOrbPreview() {
    MorningAgentTheme {
        BreathingOrb()
    }
}
