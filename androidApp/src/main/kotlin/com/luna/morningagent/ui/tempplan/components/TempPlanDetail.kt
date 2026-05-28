package com.luna.morningagent.ui.tempplan.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempPlanDetail(
    plan: TempPlan,
    taskDraft: String,
    onTaskDraftChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onPromoteTask: (String) -> Unit,
    onUpdateStartDate: (String) -> Unit,
    onUpdateEndDate: (String) -> Unit,
    onDeletePlan: () -> Unit,
) {
    val morning = MaterialTheme.morning
    val keyboard = LocalSoftwareKeyboardController.current
    val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(plan.endDate)).toInt()
    val countdownText = when {
        daysLeft < 0  -> stringResource(R.string.temp_plan_expired)
        daysLeft == 0 -> stringResource(R.string.temp_plan_last_day)
        else          -> stringResource(R.string.temp_plan_days_left, daysLeft)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = plan.name,
                style = MorningType.SectionHeading,
                color = morning.textPrimary,
            )
            Text(
                text  = countdownText,
                style = MorningType.MetaMono,
                color = if (daysLeft < 0) morning.error else morning.gold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EditableDateField(
                label    = stringResource(R.string.temp_plan_field_start),
                value    = plan.startDate,
                onPick   = onUpdateStartDate,
                modifier = Modifier.weight(1f),
            )
            EditableDateField(
                label    = stringResource(R.string.temp_plan_field_end),
                value    = plan.endDate,
                onPick   = onUpdateEndDate,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        val hasDays = plan.tasks.any { it.dayIndex != null }
        if (hasDays) {
            val grouped = plan.tasks.groupBy { it.dayIndex }
            val ungrouped = grouped[null].orEmpty()
            if (ungrouped.isNotEmpty()) {
                ungrouped.forEach { task ->
                    TempTaskRow(
                        task      = task,
                        onToggle  = { onToggleTask(task.id) },
                        onPromote = { onPromoteTask(task.id) },
                        onRemove  = { onRemoveTask(task.id) },
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            grouped.keys.filterNotNull().sorted().forEach { day ->
                Text(
                    text     = stringResource(R.string.temp_plan_day_header, day),
                    style    = MorningType.LabelMono,
                    color    = morning.accent,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                grouped[day]?.forEach { task ->
                    TempTaskRow(
                        task      = task,
                        onToggle  = { onToggleTask(task.id) },
                        onPromote = { onPromoteTask(task.id) },
                        onRemove  = { onRemoveTask(task.id) },
                    )
                }
            }
        } else {
            plan.tasks.forEach { task ->
                TempTaskRow(
                    task      = task,
                    onToggle  = { onToggleTask(task.id) },
                    onPromote = { onPromoteTask(task.id) },
                    onRemove  = { onRemoveTask(task.id) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value         = taskDraft,
            onValueChange = onTaskDraftChange,
            placeholder   = { Text(stringResource(R.string.temp_plan_task_placeholder)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboard?.hide()
                onAddTask()
            }),
        )

        TextButton(onClick = onAddTask, modifier = Modifier.fillMaxWidth()) {
            Text(
                text  = stringResource(R.string.temp_plan_add_task),
                style = MorningType.ButtonLabel,
                color = morning.accent,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = onDeletePlan) {
                Icon(
                    imageVector        = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.temp_plan_delete_button),
                    tint               = morning.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableDateField(
    label: String,
    value: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val morning = MaterialTheme.morning
    val display = runCatching {
        LocalDate.parse(value).format(
            java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH)
        )
    }.getOrDefault(value)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, morning.cardEdge, RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = label, style = MorningType.Caption, color = morning.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(text = display, style = MorningType.BodyReadItalic, color = morning.textPrimary, maxLines = 1)
    }

    if (showDialog) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        onPick(date.toString())
                    }
                    showDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = state) }
    }
}
