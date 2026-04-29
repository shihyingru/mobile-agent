package com.luna.morningagent.ui.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun AgentStatusCard(
    isLoading: Boolean,
    onRunNow: () -> Unit,
    modifier: Modifier = Modifier,
    nextRunLabel: String = "tomorrow 09:00",
) {
    val morning = MaterialTheme.morning

    val infiniteTransition = rememberInfiniteTransition(label = "statusDot")
    val dotGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue  = 0.6f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dotGlow",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(morning.surface)
            .padding(20.dp),
    ) {
        Text(
            text  = stringResource(R.string.agent_status_label),
            style = MorningType.Label,
            color = morning.textMuted,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulsing accent dot (AccentPulse pattern)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .drawBehind {
                        drawCircle(
                            color  = morning.accent.copy(alpha = dotGlowAlpha),
                            radius = size.minDimension * 0.9f,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(morning.accent),
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text  = stringResource(R.string.agent_status_active),
                style = MorningType.Title,
                color = morning.textPrimary,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = stringResource(R.string.agent_status_next_run),
                style = MorningType.Body,
                color = morning.textSecondary,
            )
            Text(
                text  = " · $nextRunLabel",
                style = MorningType.Mono,
                color = morning.textSecondary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick  = onRunNow,
            enabled  = !isLoading,
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = morning.accent,
                contentColor           = morning.surface,
                disabledContainerColor = morning.accent.copy(alpha = 0.5f),
                disabledContentColor   = morning.surface.copy(alpha = 0.5f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(18.dp),
                    color     = morning.surface,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text  = stringResource(R.string.btn_run_now),
                    style = MorningType.Title,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F, name = "Status – idle")
@Composable
private fun AgentStatusCardIdlePreview() {
    MorningAgentTheme { AgentStatusCard(isLoading = false, onRunNow = {}) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F, name = "Status – loading")
@Composable
private fun AgentStatusCardLoadingPreview() {
    MorningAgentTheme { AgentStatusCard(isLoading = true, onRunNow = {}) }
}
