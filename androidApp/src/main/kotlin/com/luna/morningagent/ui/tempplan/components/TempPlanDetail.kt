package com.luna.morningagent.ui.tempplan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun TempPlanDetail(
    plan: TempPlan,
    taskDraft: String,
    onTaskDraftChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onPromoteTask: (String) -> Unit,
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

        Text(
            text  = "${plan.startDate}  —  ${plan.endDate}",
            style = MorningType.Caption,
            color = morning.textMuted,
        )

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

        TextButton(onClick = onDeletePlan, modifier = Modifier.fillMaxWidth()) {
            Text(
                text  = stringResource(R.string.temp_plan_delete_button),
                style = MorningType.ButtonLabel,
                color = morning.error,
            )
        }
    }
}
