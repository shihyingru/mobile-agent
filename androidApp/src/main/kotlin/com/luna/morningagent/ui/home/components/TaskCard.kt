package com.luna.morningagent.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

@Composable
fun TaskCard(
    task: Task,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        label       = "cardScale",
    )
    val estimatedLabel = if (task.estimatedMinutes >= 60) {
        "${task.estimatedMinutes / 60}h"
    } else {
        "${task.estimatedMinutes}m"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(morning.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                )
            }
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            PriorityPill(priority = task.priority)
            Spacer(modifier = Modifier.weight(1f))
            NotionBadge()
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text  = task.title,
            style = MorningType.Headline,
            color = morning.textPrimary,
        )

        Text(
            text  = task.notionRef,
            style = MorningType.Mono,
            color = morning.textMuted,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text  = stringResource(R.string.label_tip),
            style = MorningType.Label,
            color = morning.accent,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text  = task.tip,
            style = MorningType.Body,
            color = morning.textSecondary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "Est. $estimatedLabel",
                style = MorningType.Mono,
                color = morning.textSecondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector        = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = stringResource(R.string.cd_notion_link),
                tint               = morning.accent,
                modifier           = Modifier.padding(end = 2.dp),
            )
            Text(
                text  = stringResource(R.string.notion_link_label),
                style = MorningType.Body,
                color = morning.accent,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun TaskCardPreview() {
    MorningAgentTheme {
        TaskCard(task = PreviewData.sampleBriefing.tasks.first())
    }
}
