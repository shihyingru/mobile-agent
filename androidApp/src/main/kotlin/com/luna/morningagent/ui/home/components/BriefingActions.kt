package com.luna.morningagent.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// Reversible-action chips under the briefing summary. Apply pushes the mutation
// to Notion (via HomeViewModel.applyAction) then refetches; Dismiss removes
// the chip from view locally. PR 3 persists dismissals; for now they're VM-scoped.
@Composable
fun BriefingActions(
    actions: List<ProposedAction>,
    tasks: List<Task>,
    onApply: (ProposedAction) -> Unit,
    onDismiss: (ProposedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    val morning = MaterialTheme.morning
    val tasksById = tasks.associateBy { it.id }

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text  = "SUGGESTED",
            style = MorningType.LabelMono,
            color = morning.accent,
        )
        actions.forEach { action ->
            val task = tasksById[action.taskId] ?: return@forEach
            ActionRow(
                action    = action,
                task      = task,
                onApply   = { onApply(action) },
                onDismiss = { onDismiss(action) },
            )
        }
    }
}

@Composable
private fun ActionRow(
    action: ProposedAction,
    task: Task,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(morning.accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = action.label(task),
                style = MorningType.BodyReadItalic,
                color = morning.textPrimary,
            )
            Text(
                text  = action.reason,
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick        = onApply,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text  = "Apply",
                        style = MorningType.ButtonLabel,
                        color = morning.accent,
                    )
                }
                TextButton(
                    onClick        = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text  = "Dismiss",
                        style = MorningType.ButtonLabel,
                        color = morning.textMuted,
                    )
                }
            }
        }
    }
}

private fun ProposedAction.label(task: Task): String = when (this) {
    is ProposedAction.MarkDone       -> "Mark \"${task.title}\" done"
    is ProposedAction.Reschedule     -> "Move \"${task.title}\" to $newDate"
    is ProposedAction.ChangePriority -> "Bump \"${task.title}\" to ${newPriority.label()}"
}

private fun com.luna.morningagent.data.model.Priority.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD, name = "Two actions")
@Composable
private fun BriefingActionsPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            BriefingActions(
                actions   = PreviewData.sampleProposedActions,
                tasks     = PreviewData.sampleBriefing.tasks,
                onApply   = {},
                onDismiss = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD, name = "Empty (renders nothing)")
@Composable
private fun BriefingActionsEmptyPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            BriefingActions(
                actions   = emptyList(),
                tasks     = emptyList(),
                onApply   = {},
                onDismiss = {},
            )
        }
    }
}
