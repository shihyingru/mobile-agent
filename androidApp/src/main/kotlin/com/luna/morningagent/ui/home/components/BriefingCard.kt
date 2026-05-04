package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun BriefingCard(
    briefing: Briefing,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val localTime = briefing.generatedAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val timeString = "%02d:%02d:%02d".format(localTime.hour, localTime.minute, localTime.second)
    val taskCount  = briefing.tasks.size
    val highCount  = briefing.tasks.count { it.priority == com.luna.morningagent.data.model.Priority.HIGH }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(morning.surfaceRaised),
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(morning.accent),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "$highCount high-priority tasks today",
                    style = MorningType.Title,
                    color = morning.textPrimary,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text  = "“${briefing.summary}”",
                    style = MorningType.BodyItalic,
                    color = morning.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Footer: model · timestamp · token count
                Text(
                    text  = "${briefing.model}  ·  $timeString  ·  ${briefing.tokens} tok",
                    style = MorningType.Mono,
                    color = morning.textMuted,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            NotionBadge()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun BriefingCardPreview() {
    MorningAgentTheme {
        BriefingCard(briefing = PreviewData.sampleBriefing)
    }
}
