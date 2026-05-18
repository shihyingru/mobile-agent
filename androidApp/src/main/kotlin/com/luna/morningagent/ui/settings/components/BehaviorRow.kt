package com.luna.morningagent.ui.settings.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

/**
 * v4 settings behavior row — sits inside a hairline-divided Column.
 *
 * Title (sans 14.5sp/500) + sub (serif italic 13sp). Right side is either
 * a Material3 Switch (toggle) or a clickable value + chevron (drill-in).
 * All rows share the same left padding so the title column stays aligned
 * across the section — child rows like Briefing time line up with the
 * toggle rows above instead of inset.
 */
@Composable
fun BehaviorToggleRow(
    title: String,
    sub: String,
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    BehaviorRowFrame(modifier = modifier) {
        BehaviorRowText(title = title, sub = sub)
        Switch(
            checked         = on,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = morning.accent,
                checkedBorderColor   = morning.accent,
                uncheckedThumbColor  = Color.White,
                uncheckedTrackColor  = morning.textMuted.copy(alpha = 0.30f),
                uncheckedBorderColor = morning.textMuted.copy(alpha = 0.30f),
            ),
        )
    }
}

@Composable
fun BehaviorValueRow(
    title: String,
    sub: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    BehaviorRowFrame(modifier = modifier.clickable(onClick = onClick)) {
        BehaviorRowText(title = title, sub = sub)
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text  = value,
                style = MorningType.ModelId.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color = morning.accent,
            )
            Icon(
                imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint               = morning.accent,
                modifier           = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun BehaviorRowFrame(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        content()
    }
}

private typealias RowScope = androidx.compose.foundation.layout.RowScope

@Composable
private fun RowScope.BehaviorRowText(title: String, sub: String) {
    val morning = MaterialTheme.morning
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text  = title,
            style = MorningType.RowTitle,
            color = morning.textPrimary,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text  = sub,
            style = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = morning.textSecondary,
        )
    }
}

/** Hairline divider for stacking BehaviorRows. */
@Composable
fun BehaviorDivider(modifier: Modifier = Modifier) {
    val morning = MaterialTheme.morning
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(morning.cardEdge),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun BehaviorRowsPreview() {
    MorningAgentTheme {
        Column(modifier = Modifier.padding(18.dp)) {
            BehaviorDivider()
            BehaviorToggleRow(
                title    = "Auto-run on launch",
                sub      = "Run the agent as soon as you open the app.",
                on       = true,
                onToggle = {},
            )
            BehaviorDivider()
            BehaviorToggleRow(
                title    = "Daily briefing notification",
                sub      = "Push the briefing when it's ready.",
                on       = true,
                onToggle = {},
            )
            BehaviorDivider()
            BehaviorValueRow(
                title    = "Briefing time",
                sub      = "Wakes the agent and delivers the briefing.",
                value    = "9:00 AM",
                onClick  = {},
            )
            BehaviorDivider()
        }
    }
}
