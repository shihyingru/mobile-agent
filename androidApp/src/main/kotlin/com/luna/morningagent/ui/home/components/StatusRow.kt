package com.luna.morningagent.ui.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.RunIcon
import com.luna.morningagent.ui.theme.StatusDot
import com.luna.morningagent.ui.theme.morning
import com.luna.morningagent.ui.theme.slotCopy

/**
 * v4 status row — replaces the v3 AgentStatusCard. Sits flat on the page
 * (no card chrome). Dot + status label + "Next run · ..." caption on the
 * left, accent pill button with leading run-icon on the right.
 *
 * Status label, run-button label, dot color, and run-icon all come from
 * slotCopy() — so the row reshapes through the day with the active slot.
 */
@Composable
fun StatusRow(
    isLoading: Boolean,
    onRunNow: () -> Unit,
    nextRunLabel: String?,
    statusOverride: String? = null,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val copy = slotCopy()
    val statusText = statusOverride ?: stringResource(copy.status)

    Row(
        modifier              = modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier              = Modifier.weight(1f),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DotIndicator(state = copy.statusDot)
            Column {
                Text(
                    text  = statusText,
                    style = MorningType.StatusLabel,
                    color = morning.textPrimary,
                )
                if (nextRunLabel != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = "${stringResource(R.string.label_next_run)} · $nextRunLabel",
                        style = MorningType.Caption,
                        color = morning.textMuted,
                    )
                }
            }
        }

        RunPill(
            label     = stringResource(copy.runLabel),
            icon      = iconFor(copy.runIcon),
            isLoading = isLoading,
            onClick   = onRunNow,
        )
    }
}

@Composable
private fun DotIndicator(state: StatusDot) {
    val morning = MaterialTheme.morning
    val color: Color = when (state) {
        StatusDot.Live      -> morning.success
        StatusDot.Scheduled -> morning.accent
        StatusDot.Idle      -> morning.gold
        StatusDot.Sleeping  -> morning.textMuted
    }
    Box(
        modifier         = Modifier.size(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (state == StatusDot.Live) {
            // Expanding ripple per JSX `pulse` keyframe — scale 0.6→1.6, fade
            // 0.5→0 over 1.6s, then hold transparent until the next ring.
            val transition = rememberInfiniteTransition(label = "statusRipple")
            val rippleScale by transition.animateFloat(
                initialValue  = 0.6f,
                targetValue   = 1.6f,
                animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing)),
                label = "scale",
            )
            val rippleAlpha by transition.animateFloat(
                initialValue  = 0.5f,
                targetValue   = 0f,
                animationSpec = infiniteRepeatable(animation = tween(1600, easing = LinearEasing)),
                label = "alpha",
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .scale(rippleScale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = rippleAlpha)),
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

private fun iconFor(icon: RunIcon): ImageVector = when (icon) {
    RunIcon.Sunrise -> Icons.Outlined.WbSunny
    RunIcon.Refresh -> Icons.Rounded.Refresh
    RunIcon.Check   -> Icons.Rounded.Check
    RunIcon.Moon    -> Icons.Outlined.DarkMode
}

@Composable
private fun RunPill(
    label: String,
    icon: ImageVector,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier = Modifier
            .height(38.dp)
            .shadow(
                elevation    = 14.dp,
                shape        = RoundedCornerShape(19.dp),
                spotColor    = morning.accent,
                ambientColor = morning.accent,
            )
            .clip(RoundedCornerShape(19.dp))
            .background(morning.accent)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(start = 12.dp, end = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp),
                color       = morning.onAccent,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = morning.onAccent,
                modifier           = Modifier.size(16.dp),
            )
        }
        Text(
            text  = label,
            style = MorningType.ButtonLabel,
            color = morning.onAccent,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun StatusRowPreview() {
    MorningAgentTheme {
        Column(
            modifier            = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatusRow(isLoading = false, onRunNow = {}, nextRunLabel = "tomorrow, 9:00")
            StatusRow(isLoading = true,  onRunNow = {}, nextRunLabel = "Now")
            StatusRow(isLoading = false, onRunNow = {}, nextRunLabel = null)
        }
    }
}
