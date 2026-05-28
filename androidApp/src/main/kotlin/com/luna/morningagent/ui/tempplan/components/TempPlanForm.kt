package com.luna.morningagent.ui.tempplan.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import com.luna.morningagent.ui.tempplan.TempPlanUiState
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TempPlanForm(
    state: TempPlanUiState.Creating,
    onUpdateName: (String) -> Unit,
    onUpdateStart: (String) -> Unit,
    onUpdateEnd: (String) -> Unit,
    onCreate: () -> Unit,
    onStartCreating: () -> Unit,
    isEmpty: Boolean,
) {
    val morning = MaterialTheme.morning
    val keyboard = LocalSoftwareKeyboardController.current

    if (isEmpty) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, morning.cardEdge, RoundedCornerShape(18.dp))
                .background(morning.surface)
                .clickable(onClick = onStartCreating)
                .padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        ) {
            Text(
                text  = stringResource(R.string.temp_plan_no_plan),
                style = MorningType.BodyReadItalic,
                color = morning.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = stringResource(R.string.temp_plan_create_button),
                style = MorningType.ButtonLabel,
                color = morning.accent,
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value         = state.name,
            onValueChange = onUpdateName,
            label         = { Text(stringResource(R.string.temp_plan_field_name)) },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateField(
                label    = stringResource(R.string.temp_plan_field_start),
                value    = state.startDate,
                onPick   = onUpdateStart,
                modifier = Modifier.weight(1f),
            )
            DateField(
                label    = stringResource(R.string.temp_plan_field_end),
                value    = state.endDate,
                onPick   = onUpdateEnd,
                modifier = Modifier.weight(1f),
            )
        }

        val canCreate = state.name.isNotBlank() &&
            state.startDate.isNotBlank() &&
            state.endDate.isNotBlank()

        TextButton(
            onClick = {
                keyboard?.hide()
                onCreate()
            },
            enabled  = canCreate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text  = stringResource(R.string.temp_plan_create_button),
                style = MorningType.ButtonLabel,
                color = if (canCreate) morning.accent else morning.textMuted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val morning = MaterialTheme.morning
    val displayValue = if (value.isNotEmpty()) {
        runCatching {
            LocalDate.parse(value).format(
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH)
            )
        }.getOrDefault(value)
    } else {
        ""
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, morning.cardEdge, RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text  = label,
            style = MorningType.Caption,
            color = morning.textMuted,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = displayValue.ifEmpty { "—" },
            style    = MorningType.BodyReadItalic,
            color    = if (displayValue.isEmpty()) morning.textMuted else morning.textPrimary,
            maxLines = 1,
        )
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atOffset(ZoneOffset.UTC)
                            .toLocalDate()
                        onPick(date.toString())
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
