package com.luna.morningagent.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

/**
 * v4 task card — repeating list rows are the only place where card chrome
 * still lives. 18dp radius, soft tint shadow, 16dp padding.
 *
 * Layout (top to bottom):
 *   - top row: PriorityChip · spacer · estimate (mono) · separator dot · SourceLogo(16dp)
 *   - title    (Newsreader 18sp/500)
 *   - tip      (Newsreader italic 14sp, textSecondary)
 *   - hairline cardEdge divider     ← only when task.notionUrl != null
 *   - footer:  "Open task" (sans, accent) · chevron (accent)   ← ditto
 *
 * Tap fires [onClick] only when the task has a Notion URL — manual-entry
 * tasks with no source link are passive cards (no affordance, no footer).
 */
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hasLink = !task.notionUrl.isNullOrBlank()
    val morning = MaterialTheme.morning
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label       = "cardScale",
    )

    val estimateLabel = if (task.estimatedMinutes > 0) {
        if (task.estimatedMinutes >= 60) "${task.estimatedMinutes / 60}h" else "${task.estimatedMinutes}m"
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation    = 12.dp,
                shape        = RoundedCornerShape(18.dp),
                ambientColor = morning.textPrimary.copy(alpha = 0.18f),
                spotColor    = morning.textPrimary.copy(alpha = 0.18f),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(morning.surface)
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(18.dp))
            .pointerInput(hasLink) {
                if (!hasLink) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            }
            .padding(16.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PriorityChip(priority = task.priority)
            Spacer(modifier = Modifier.weight(1f))
            if (estimateLabel != null) {
                Text(
                    text  = estimateLabel,
                    style = MorningType.Caption,
                    color = morning.textMuted,
                )
                Text(
                    text  = "·",
                    style = MorningType.Caption,
                    color = morning.textMuted.copy(alpha = 0.4f),
                )
            }
            SourceLogo(source = com.luna.morningagent.data.sharedposts.SharedPost.SOURCE_NOTION, size = 16.dp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text  = task.title,
            style = MorningType.TaskTitle,
            color = morning.textPrimary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text  = task.tip,
            style = MorningType.Tip,
            color = morning.textSecondary,
        )

        if (hasLink) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(morning.cardEdge),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = stringResource(R.string.action_open_task),
                    style = MorningType.ButtonLabel,
                    color = morning.accent,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.cd_notion_link),
                    tint               = morning.accent.copy(alpha = 0.7f),
                    modifier           = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun TaskCardPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            TaskCard(task = PreviewData.sampleBriefing.tasks.first())
        }
    }
}
