package com.luna.morningagent.ui.home.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CallSplit
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

// Inline section under the briefing block. Not a card — matches design's
// "BRIEFING (read) → SUGGESTED (decide) → PLAN (track)" rhythm. Rows are
// glyph + italic title + sans reason + Apply / Dismiss, separated by hairline
// dividers. Renders nothing when actions is empty.
@Composable
fun BriefingActions(
    actions: List<ProposedAction>,
    tasks: List<Task>,
    model: String,
    onApply: (ProposedAction) -> Unit,
    onDismiss: (ProposedAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return
    val morning = MaterialTheme.morning
    val tasksById = tasks.associateBy { it.id }

    Column(modifier = modifier.fillMaxWidth()) {
        // Eyebrow row: "SUGGESTED" left · "<model> · N change(s)" right
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = stringResource(R.string.section_suggested),
                style = MorningType.LabelMono,
                color = morning.accent,
            )
            Text(
                text  = stringResource(R.string.suggested_meta, model, actions.size),
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
        }

        Spacer(Modifier.height(10.dp))

        actions.forEachIndexed { index, action ->
            val task = tasksById[action.taskId] ?: return@forEachIndexed
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 14.dp)
                        .height(1.dp)
                        .background(morning.cardEdge),
                )
            }
            SuggestionRow(
                action    = action,
                task      = task,
                onApply   = { onApply(action) },
                onDismiss = { onDismiss(action) },
            )
        }
    }
}

@Composable
private fun SuggestionRow(
    action: ProposedAction,
    task: Task,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 26dp glyph chip — accentSoft surface, accent icon.
        Box(
            modifier         = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(morning.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = glyphFor(action),
                contentDescription = null,
                tint               = morning.accent,
                modifier           = Modifier.size(14.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = action.label(task),
                style = MorningType.BodyReadItalic.copy(
                    fontSize   = 17.sp,
                    lineHeight = (17 * 1.3f).sp,
                ),
                color = morning.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = action.reason,
                style = MorningType.RowTitle.copy(
                    fontSize   = 13.sp,
                    lineHeight = (13 * 1.45f).sp,
                ),
                color = morning.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.padding(start = (-10).dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                ApplyButton(onClick = onApply)
                DismissButton(onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ApplyButton(onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.Check,
            contentDescription = null,
            tint               = morning.accent,
            modifier           = Modifier.size(14.dp),
        )
        Text(
            text  = stringResource(R.string.action_apply),
            style = MorningType.ButtonLabel.copy(fontSize = 13.sp),
            color = morning.accent,
        )
    }
}

@Composable
private fun DismissButton(onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text  = stringResource(R.string.action_dismiss),
            style = MorningType.ButtonLabel.copy(fontSize = 13.sp),
            color = morning.textMuted,
        )
    }
}

// Kind → glyph map. Our ProposedAction kinds (MarkDone / Reschedule /
// ChangePriority) don't map 1:1 to the design's (MOVE / DEMOTE / SPLIT), so:
//   - Reschedule       → arrow-up-right (MOVE)
//   - ChangePriority   → arrow-down (DEMOTE) when going lower, split otherwise
//   - MarkDone         → check (no design equivalent — completion glyph)
private fun glyphFor(action: ProposedAction): ImageVector = when (action) {
    is ProposedAction.MarkDone       -> Icons.Outlined.Check
    is ProposedAction.Reschedule     -> Icons.Outlined.NorthEast
    is ProposedAction.ChangePriority ->
        if (action.newPriority == Priority.LOW) Icons.Outlined.KeyboardArrowDown
        else Icons.Outlined.CallSplit
}

private fun ProposedAction.label(task: Task): String = when (this) {
    is ProposedAction.MarkDone       -> "Mark \"${task.title}\" done"
    is ProposedAction.Reschedule     -> "Move \"${task.title}\" to $newDate"
    is ProposedAction.ChangePriority -> "Bump \"${task.title}\" to ${newPriority.label()}"
}

private fun Priority.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD, name = "Two actions")
@Composable
private fun BriefingActionsPreview() {
    MorningAgentTheme {
        Box(modifier = Modifier.padding(18.dp)) {
            BriefingActions(
                actions   = PreviewData.sampleProposedActions,
                tasks     = PreviewData.sampleBriefing.tasks,
                model     = "gemini-2.5",
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
                model     = "gemini-2.5",
                onApply   = {},
                onDismiss = {},
            )
        }
    }
}
