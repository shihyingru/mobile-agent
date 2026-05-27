package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.data.tempplan.TempTask
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun TempPlanSection(
    plan: TempPlan,
    onToggleTask: (String) -> Unit,
    onPromoteTask: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(plan.endDate)).toInt()
    val countdownText = when {
        daysLeft <= 0 -> stringResource(R.string.temp_plan_last_day)
        else          -> stringResource(R.string.temp_plan_days_left, daysLeft)
    }
    val checked = plan.tasks.count { it.checked }
    val total   = plan.tasks.size
    val progress = if (total > 0) checked.toFloat() / total else 0f
    val visible = plan.tasks.take(MAX_VISIBLE)
    val overflow = plan.tasks.size - MAX_VISIBLE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, morning.cardEdge, RoundedCornerShape(18.dp))
            .background(morning.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = plan.name,
                style    = MorningType.SectionHeading,
                color    = morning.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = countdownText,
                style = MorningType.MetaMono,
                color = morning.gold,
            )
        }

        LinearProgressIndicator(
            progress  = { progress },
            modifier  = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp)),
            color     = morning.accent,
            trackColor = morning.cardEdge,
        )

        visible.forEach { task ->
            TempTaskRow(
                task       = task,
                onToggle   = { onToggleTask(task.id) },
                onPromote  = { onPromoteTask(task.id) },
            )
        }

        if (overflow > 0) {
            Text(
                text  = stringResource(R.string.temp_plan_more_tasks, overflow),
                style = MorningType.Caption,
                color = morning.textMuted,
            )
        }
    }
}

@Composable
private fun TempTaskRow(
    task: TempTask,
    onToggle: () -> Unit,
    onPromote: () -> Unit,
) {
    val morning = MaterialTheme.morning
    val promoted = task.promotedToNotionId != null
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(1.5.dp, if (task.checked) morning.success else morning.textMuted, CircleShape)
                .background(if (task.checked) morning.success else morning.surface)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (task.checked) {
                Icon(
                    imageVector        = Icons.Rounded.Check,
                    contentDescription = null,
                    tint               = morning.onAccent,
                    modifier           = Modifier.size(14.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text           = task.title,
            style          = MorningType.BodyReadItalic,
            color          = if (task.checked) morning.textMuted else morning.textPrimary,
            textDecoration = if (task.checked) TextDecoration.LineThrough else TextDecoration.None,
            maxLines       = 1,
            overflow       = TextOverflow.Ellipsis,
            modifier       = Modifier.weight(1f),
        )
        if (promoted) {
            Text(
                text  = stringResource(R.string.temp_plan_promoted),
                style = MorningType.MetaMono,
                color = morning.success,
            )
        } else {
            TextButton(onClick = onPromote) {
                Text(
                    text  = stringResource(R.string.temp_plan_promote),
                    style = MorningType.ButtonLabel,
                    color = morning.accent,
                )
            }
        }
    }
}

private const val MAX_VISIBLE = 4

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun TempPlanSectionPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            TempPlanSection(
                plan          = PreviewData.sampleTempPlan,
                onToggleTask  = {},
                onPromoteTask = {},
                onClick       = {},
            )
        }
    }
}
