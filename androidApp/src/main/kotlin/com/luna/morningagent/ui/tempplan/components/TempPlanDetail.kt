package com.luna.morningagent.ui.tempplan.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luna.morningagent.R
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.ui.home.components.TimeProgressBar
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

// Plan modification body — sits below the top bar. Implements §5.5 (title +
// status + progress), §5.6 (date fields), §5.7 (tasks card + composer),
// §5.8 (two-step destructive delete).
@Composable
fun TempPlanDetail(
    plan: TempPlan,
    taskDraft: String,
    onTaskDraftChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onRenamePlan: (String) -> Unit,
    onUpdateStartDate: (String) -> Unit,
    onUpdateEndDate: (String) -> Unit,
    onDeletePlan: () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        TitleBlock(
            plan         = plan,
            onRenamePlan = onRenamePlan,
        )
        DateFieldsRow(
            plan              = plan,
            onUpdateStartDate = onUpdateStartDate,
            onUpdateEndDate   = onUpdateEndDate,
        )
        TasksCard(
            plan              = plan,
            taskDraft         = taskDraft,
            onTaskDraftChange = onTaskDraftChange,
            onAddTask         = onAddTask,
            onToggleTask      = onToggleTask,
            onRemoveTask      = onRemoveTask,
        )
        DeletePlanBlock(onDelete = onDeletePlan)
    }
}

// --- §5.5 Title block --------------------------------------------------------

@Composable
private fun TitleBlock(plan: TempPlan, onRenamePlan: (String) -> Unit) {
    val morning = MaterialTheme.morning
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var renaming  by remember { mutableStateOf(false) }
    var hadFocus  by remember { mutableStateOf(false) }
    // TextFieldValue (not a bare String) so we can seed the cursor at the end
    // of the title when edit-mode opens, instead of defaulting to position 0.
    var draft     by remember(plan.id, plan.name) {
        mutableStateOf(TextFieldValue(plan.name, TextRange(plan.name.length)))
    }

    // requestFocus() can throw if the modifier isn't attached yet on the very
    // first frame after the field enters composition — runCatching swallows
    // that; the user can always tap the field directly as a fallback.
    LaunchedEffect(renaming) {
        if (renaming) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        } else {
            hadFocus = false
        }
    }

    val commitRename: () -> Unit = {
        val next = draft.text.trim()
        if (next.isNotEmpty() && next != plan.name) onRenamePlan(next)
        renaming = false
        keyboard?.hide()
    }

    val start = LocalDate.parse(plan.startDate)
    val end   = LocalDate.parse(plan.endDate)
    val today = LocalDate.now()
    val total    = max(1L, ChronoUnit.DAYS.between(start, end))
    val elapsed  = ChronoUnit.DAYS.between(start, today).coerceIn(0, total)
    val daysLeft = max(0L, ChronoUnit.DAYS.between(today, end)).toInt()
    val ratio    = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    val statusLine = when {
        daysLeft == 0 -> stringResource(R.string.temp_plan_status_wraps_today)
        else          -> stringResource(R.string.temp_plan_status_elapsed, daysLeft, elapsed, total)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        if (renaming) {
            // Visible edit affordance: soft accent wash + accent underline so
            // tap → edit-mode transition is unambiguous (was silent before).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(morning.accent.copy(alpha = 0.06f))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                BasicTextField(
                    value         = draft,
                    onValueChange = { draft = it },
                    singleLine    = true,
                    cursorBrush   = SolidColor(morning.accent),
                    textStyle     = MorningType.GreetingDisplay.copy(
                        color = morning.textPrimary,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitRename() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            // Initial onFocusChanged fires with isFocused=false
                            // *before* focus is granted — only commit on a real
                            // loss after focus was actually gained.
                            if (state.isFocused) {
                                hadFocus = true
                            } else if (hadFocus && renaming) {
                                commitRename()
                            }
                        },
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(morning.accent.copy(alpha = 0.6f)),
                )
            }
        } else {
            Row(
                modifier              = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        draft = TextFieldValue(plan.name, TextRange(plan.name.length))
                        renaming = true
                    },
                verticalAlignment     = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text     = plan.name,
                    style    = MorningType.GreetingDisplay,
                    color    = morning.textPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector        = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.cd_rename_plan),
                    tint               = morning.textMuted.copy(alpha = 0.6f),
                    modifier           = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text  = statusLine,
            style = MorningType.BodyReadItalic,
            color = morning.textSecondary,
        )

        Spacer(Modifier.height(14.dp))

        TimeProgressBar(
            ratio    = ratio,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// --- §5.6 Date fields --------------------------------------------------------

@Composable
private fun DateFieldsRow(
    plan: TempPlan,
    onUpdateStartDate: (String) -> Unit,
    onUpdateEndDate: (String) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DateFieldCard(
            label    = stringResource(R.string.temp_plan_field_start),
            isoDate  = plan.startDate,
            onPick   = onUpdateStartDate,
            modifier = Modifier.weight(1f),
        )
        DateFieldCard(
            label    = stringResource(R.string.temp_plan_field_end),
            isoDate  = plan.endDate,
            onPick   = onUpdateEndDate,
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFieldCard(
    label: String,
    isoDate: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    var open by remember { mutableStateOf(false) }
    val display = runCatching {
        LocalDate.parse(isoDate)
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    }.getOrDefault(isoDate)

    Row(
        modifier              = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, morning.cardEdge, RoundedCornerShape(12.dp))
            .background(morning.surface)
            .clickable { open = true }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MorningType.ButtonLabel.copy(fontSize = 11.sp),
                color = morning.textMuted,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = display,
                style = MorningType.ModelId.copy(fontSize = 13.5.sp),
                color = morning.textPrimary,
            )
        }
        Icon(
            imageVector        = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint               = morning.textMuted.copy(alpha = 0.7f),
            modifier           = Modifier.size(14.dp),
        )
    }

    if (open) {
        val initialMillis = runCatching {
            LocalDate.parse(isoDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        onPick(date.toString())
                    }
                    open = false
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }
}

// --- §5.7 Tasks section ------------------------------------------------------

@Composable
private fun TasksCard(
    plan: TempPlan,
    taskDraft: String,
    onTaskDraftChange: (String) -> Unit,
    onAddTask: () -> Unit,
    onToggleTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
) {
    val morning = MaterialTheme.morning
    val done = plan.tasks.count { it.checked }
    val total = plan.tasks.size

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        // Label row above the card
        Row(
            modifier              = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = stringResource(R.string.temp_plan_tasks_section),
                style = MorningType.LabelMono,
                color = morning.accent,
            )
            Text(
                text  = stringResource(R.string.temp_plan_tasks_count, done, total),
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
        }

        // Single tasks card containing all rows + the composer at the bottom.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, morning.cardEdge, RoundedCornerShape(18.dp))
                .background(morning.surface),
        ) {
            if (plan.tasks.isEmpty()) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = stringResource(R.string.temp_plan_no_tasks_yet),
                        style = MorningType.BodyReadItalic.copy(fontSize = 14.sp),
                        color = morning.textMuted,
                    )
                }
            } else {
                plan.tasks.forEachIndexed { index, task ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(morning.cardEdge),
                        )
                    }
                    TempTaskRow(
                        task     = task,
                        onToggle = { onToggleTask(task.id) },
                        onRemove = { onRemoveTask(task.id) },
                    )
                }
            }

            // Composer — pinned to the bottom of the same card. Top border only
            // when there are tasks above (matches §5.7 composer rules).
            Composer(
                value         = taskDraft,
                onValueChange = onTaskDraftChange,
                onSubmit      = onAddTask,
                showTopBorder = plan.tasks.isNotEmpty(),
            )
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    showTopBorder: Boolean,
) {
    val morning  = MaterialTheme.morning
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopBorder) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(morning.cardEdge),
            )
        }
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 18dp dashed plus tile
            Box(
                modifier         = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .border(
                        width = 1.5.dp,
                        color = morning.textMuted.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(999.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "+",
                    style = MorningType.ButtonLabel.copy(fontSize = 11.sp),
                    color = morning.textMuted,
                )
            }

            BasicTextField(
                value         = value,
                onValueChange = onValueChange,
                singleLine    = true,
                cursorBrush   = SolidColor(morning.accent),
                textStyle     = TextStyle(
                    fontFamily = MorningType.TaskTitle.fontFamily,
                    fontSize   = 15.5.sp,
                    color      = morning.textPrimary,
                ),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text  = stringResource(R.string.temp_plan_task_placeholder),
                            style = MorningType.TaskTitle.copy(fontSize = 15.5.sp),
                            color = morning.textMuted.copy(alpha = 0.7f),
                        )
                    }
                    inner()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    onSubmit()
                }),
                modifier        = Modifier.weight(1f),
            )

            if (value.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            keyboard?.hide()
                            onSubmit()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text  = stringResource(R.string.temp_plan_add_task_short),
                        style = MorningType.ButtonLabel.copy(fontSize = 12.sp),
                        color = morning.accent,
                    )
                }
            }
        }
    }
}

// --- §5.8 Two-step destructive delete ---------------------------------------

@Composable
private fun DeletePlanBlock(onDelete: () -> Unit) {
    val morning = MaterialTheme.morning
    var confirming by remember { mutableStateOf(false) }
    val destructive = Color(0xFFE5484D)

    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp)
    ) {
        if (!confirming) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, morning.cardEdge, RoundedCornerShape(14.dp))
                    .clickable { confirming = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint               = destructive,
                    modifier           = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text  = stringResource(R.string.temp_plan_delete_button),
                    style = MorningType.ButtonLabel.copy(fontSize = 13.sp),
                    color = destructive,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(destructive.copy(alpha = 0.08f))
                    .border(1.dp, destructive.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text  = stringResource(R.string.temp_plan_delete_confirm),
                    style = MorningType.BodyReadItalic.copy(fontSize = 14.5.sp),
                    color = morning.textPrimary,
                )
                Spacer(Modifier.size(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { confirming = false }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text  = stringResource(R.string.dialog_cancel),
                            style = MorningType.ButtonLabel.copy(fontSize = 13.sp),
                            color = morning.textSecondary,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(destructive)
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text  = stringResource(R.string.temp_plan_delete_button),
                            style = MorningType.ButtonLabel.copy(fontSize = 13.sp),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

