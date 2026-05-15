package com.luna.morningagent.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.data.agent.ProviderOption
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SettingsViewModel = viewModel(),
) {
    // Drafts must not leak across visits. The ViewModel is activity-scoped, so
    // reset state on every back-navigation (arrow + system back).
    val handleBack = {
        vm.clearDrafts()
        onBack()
    }
    BackHandler { handleBack() }
    SettingsScreenContent(
        uiState                = vm.uiState,
        onProviderChange       = vm::setProvider,
        onGeminiDraftChange    = vm::updateGeminiDraft,
        onClaudeDraftChange    = vm::updateClaudeDraft,
        onNotionDraftChange    = vm::updateNotionDraft,
        onDatabaseDraftChange  = vm::updateDatabaseDraft,
        onAutoRunChange        = vm::setAutoRun,
        onDailyBriefingChange  = vm::setDailyBriefing,
        onBriefingTimeChange   = vm::setBriefingTime,
        onSendTestNotification = vm::sendTestNotification,
        onSave                 = vm::save,

        onTestNotion           = vm::testNotionConnection,
        onBack                 = handleBack,
        modifier               = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onProviderChange: (ProviderOption) -> Unit,
    onGeminiDraftChange: (String) -> Unit,
    onClaudeDraftChange: (String) -> Unit,
    onNotionDraftChange: (String) -> Unit,
    onDatabaseDraftChange: (String) -> Unit,
    onAutoRunChange: (Boolean) -> Unit,
    onDailyBriefingChange: (Boolean) -> Unit,
    onBriefingTimeChange: (Int, Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onSave: () -> Unit,
    onTestNotion: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val canSave = uiState.geminiDraft.isNotEmpty() ||
        uiState.claudeDraft.isNotEmpty() ||
        uiState.notionDraft.isNotEmpty() ||
        uiState.databaseDraft != uiState.savedDatabaseId

    Scaffold(
        modifier       = modifier,
        containerColor = morning.background,
        topBar = {
            TopAppBar(
                title          = { Text(stringResource(R.string.settings_title), style = MorningType.Title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint               = morning.textSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = morning.background,
                    titleContentColor = morning.textPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text  = stringResource(R.string.settings_intro),
                style = MorningType.Body,
                color = morning.textSecondary,
            )

            Text(
                text  = stringResource(R.string.settings_section_provider),
                style = MorningType.Label,
                color = morning.textMuted,
            )

            ProviderRow(
                selected = uiState.selectedProvider,
                onSelect = onProviderChange,
            )

            Text(
                text  = stringResource(R.string.settings_section_credentials),
                style = MorningType.Label,
                color = morning.textMuted,
            )

            // Conditional API-key field: shows only the active provider's key.
            // The other provider's key stays saved silently — flipping providers
            // doesn't wipe it.
            when (uiState.selectedProvider) {
                ProviderOption.Gemini -> SecretField(
                    label       = stringResource(R.string.settings_field_gemini),
                    draft       = uiState.geminiDraft,
                    savedLast4  = uiState.geminiSavedLast4,
                    onChange    = onGeminiDraftChange,
                    imeAction   = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
                ProviderOption.Claude -> SecretField(
                    label       = stringResource(R.string.settings_field_claude),
                    draft       = uiState.claudeDraft,
                    savedLast4  = uiState.claudeSavedLast4,
                    onChange    = onClaudeDraftChange,
                    imeAction   = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    ),
                )
            }

            SecretField(
                label       = stringResource(R.string.settings_field_notion),
                draft       = uiState.notionDraft,
                savedLast4  = uiState.notionSavedLast4,
                onChange    = onNotionDraftChange,
                imeAction   = ImeAction.Next,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text  = stringResource(R.string.settings_section_data_source),
                style = MorningType.Label,
                color = morning.textMuted,
            )

            PlainField(
                label       = stringResource(R.string.settings_field_database),
                value       = uiState.databaseDraft,
                onChange    = onDatabaseDraftChange,
                imeAction   = ImeAction.Send,
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSave) {
                            keyboard?.hide()
                            focusManager.clearFocus()
                            onSave()
                        }
                    },
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick  = onSave,
                enabled  = canSave,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = morning.accent,
                    contentColor           = morning.surface,
                    disabledContainerColor = morning.accent.copy(alpha = 0.3f),
                    disabledContentColor   = morning.surface.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(text = stringResource(R.string.settings_button_save), style = MorningType.Title)
            }

            AnimatedVisibility(
                visible = uiState.justSaved,
                enter   = fadeIn(),
                exit    = fadeOut(),
            ) {
                Text(
                    text     = stringResource(R.string.settings_feedback_saved),
                    style    = MorningType.Body,
                    color    = morning.success,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Notion connection test — only useful once a token + DB are saved.
            val testEnabled = uiState.notionSavedLast4 != null &&
                uiState.savedDatabaseId.isNotEmpty() &&
                uiState.notionTest !is NotionTestResult.InProgress

            TextButton(
                onClick  = onTestNotion,
                enabled  = testEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text  = stringResource(R.string.settings_button_test),
                    style = MorningType.Body,
                    color = if (testEnabled) morning.accent else morning.textMuted,
                )
            }

            when (val r = uiState.notionTest) {
                NotionTestResult.InProgress -> Text(
                    text  = stringResource(R.string.settings_test_in_progress),
                    style = MorningType.Body,
                    color = morning.textSecondary,
                )
                is NotionTestResult.Success -> Text(
                    text  = stringResource(R.string.settings_test_success, r.taskCount),
                    style = MorningType.Body,
                    color = morning.success,
                )
                is NotionTestResult.Failure -> Text(
                    text  = stringResource(R.string.settings_test_failure, r.message),
                    style = MorningType.Body,
                    color = morning.error,
                )
                null -> Unit
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text  = stringResource(R.string.settings_section_behavior),
                style = MorningType.Label,
                color = morning.textMuted,
            )

            AutoRunRow(
                checked = uiState.autoRun,
                onCheckedChange = onAutoRunChange,
            )

            DailyBriefingRow(
                checked         = uiState.dailyBriefing,
                onCheckedChange = onDailyBriefingChange,
            )

            // Time row visible only when the schedule is on — picker would have
            // no effect otherwise, and an inert control reads as broken.
            AnimatedVisibility(
                visible = uiState.dailyBriefing,
                enter   = fadeIn(),
                exit    = fadeOut(),
            ) {
                Column {
                    BriefingTimeRow(
                        hour    = uiState.briefingHour,
                        minute  = uiState.briefingMinute,
                        onPick  = onBriefingTimeChange,
                    )
                    TextButton(
                        onClick  = onSendTestNotification,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text  = stringResource(R.string.settings_button_test_notification),
                            style = MorningType.Body,
                            color = morning.accent,
                        )
                    }
                }
            }
        }
    }
}

// Segmented two-chip row for picking the active LLM provider. Mirrors the
// Home ModelPicker chip style for visual consistency. Persists immediately
// via onSelect; the parent flips the visible API-key field accordingly.
@Composable
private fun ProviderRow(
    selected: ProviderOption,
    onSelect: (ProviderOption) -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProviderOption.entries.forEach { option ->
            val isSelected = option == selected
            val bg = if (isSelected) morning.accent.copy(alpha = 0.12f) else morning.surface
            val borderColor = if (isSelected) morning.accent else morning.border
            val labelColor = if (isSelected) morning.accent else morning.textPrimary
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                    .clickable { onSelect(option) },
            ) {
                Text(text = option.displayName, style = MorningType.Body, color = labelColor)
            }
        }
    }
}

@Composable
private fun AutoRunRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsToggleRow(
        title    = stringResource(R.string.settings_auto_run_label),
        subtitle = stringResource(R.string.settings_auto_run_help),
        checked  = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun DailyBriefingRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsToggleRow(
        title    = stringResource(R.string.settings_daily_briefing_label),
        subtitle = stringResource(R.string.settings_daily_briefing_help),
        checked  = checked,
        onCheckedChange = onCheckedChange,
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title,    style = MorningType.Body, color = morning.textPrimary)
            Text(text = subtitle, style = MorningType.Body, color = morning.textMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = morning.surface,
                checkedTrackColor    = morning.accent,
                uncheckedThumbColor  = morning.textMuted,
                uncheckedTrackColor  = morning.surface,
                uncheckedBorderColor = morning.border,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingTimeRow(
    hour: Int,
    minute: Int,
    onPick: (Int, Int) -> Unit,
) {
    val morning = MaterialTheme.morning
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = stringResource(R.string.settings_briefing_time_label),
            style    = MorningType.Body,
            color    = morning.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text  = formatTime(hour, minute),
            style = MorningType.Mono,
            color = morning.accent,
        )
    }

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour    = hour,
            initialMinute  = minute,
            is24Hour       = false,
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            containerColor   = morning.surface,
            title = {
                Text(
                    text  = stringResource(R.string.settings_time_picker_title),
                    style = MorningType.Title,
                    color = morning.textPrimary,
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state  = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor             = morning.background,
                            selectorColor              = morning.accent,
                            containerColor             = morning.surface,
                            periodSelectorBorderColor  = morning.border,
                            clockDialSelectedContentColor   = morning.surface,
                            clockDialUnselectedContentColor = morning.textPrimary,
                            periodSelectorSelectedContainerColor   = morning.accent,
                            periodSelectorUnselectedContainerColor = morning.surface,
                            periodSelectorSelectedContentColor     = morning.surface,
                            periodSelectorUnselectedContentColor   = morning.textSecondary,
                            timeSelectorSelectedContainerColor     = morning.accent.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor   = morning.background,
                            timeSelectorSelectedContentColor       = morning.accent,
                            timeSelectorUnselectedContentColor     = morning.textPrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onPick(state.hour, state.minute)
                    showPicker = false
                }) {
                    Text(
                        text  = stringResource(R.string.settings_time_picker_confirm),
                        style = MorningType.Body,
                        color = morning.accent,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(
                        text  = stringResource(R.string.settings_time_picker_cancel),
                        style = MorningType.Body,
                        color = morning.textMuted,
                    )
                }
            },
        )
    }
}

// 12-hour clock with AM/PM — matches the picker's default and Luna's locale.
private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0       -> 12
        hour > 12       -> hour - 12
        else            -> hour
    }
    return "%d:%02d %s".format(h12, minute, period)
}

@Composable
private fun SecretField(
    label: String,
    draft: String,
    savedLast4: String?,
    onChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val morning = MaterialTheme.morning
    var focused by remember { mutableStateOf(false) }
    // When the user has a stored value but hasn't typed anything yet, paint
    // a literal "****abcd" inside the field so the saved state is visible at
    // a glance. Drop it the moment the field gains focus so typing isn't
    // appended to the mask.
    val showSavedMask = draft.isEmpty() && !focused && !savedLast4.isNullOrEmpty()
    val display = if (showSavedMask) "****$savedLast4" else draft

    OutlinedTextField(
        value         = display,
        onValueChange = onChange,
        label         = { Text(label, style = MorningType.Body) },
        singleLine    = true,
        visualTransformation = if (showSavedMask) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            capitalization     = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction          = imeAction,
        ),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = morning.textPrimary,
            unfocusedTextColor      = morning.textPrimary,
            focusedBorderColor      = morning.accent,
            unfocusedBorderColor    = morning.border,
            focusedLabelColor       = morning.accent,
            unfocusedLabelColor     = morning.textSecondary,
            cursorColor             = morning.accent,
            focusedContainerColor   = morning.surface,
            unfocusedContainerColor = morning.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused },
    )
}

@Composable
private fun PlainField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val morning = MaterialTheme.morning
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label, style = MorningType.Body) },
        singleLine    = true,
        keyboardOptions = KeyboardOptions(
            capitalization     = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            imeAction          = imeAction,
        ),
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = morning.textPrimary,
            unfocusedTextColor      = morning.textPrimary,
            focusedBorderColor      = morning.accent,
            unfocusedBorderColor    = morning.border,
            focusedLabelColor       = morning.accent,
            unfocusedLabelColor     = morning.textSecondary,
            cursorColor             = morning.accent,
            focusedContainerColor   = morning.surface,
            unfocusedContainerColor = morning.surface,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F, name = "Settings — empty")
@Composable
private fun SettingsEmptyPreview() {
    MorningAgentTheme {
        SettingsScreenContent(
            uiState               = SettingsUiState(),
            onGeminiDraftChange   = {},
            onNotionDraftChange   = {},
            onDatabaseDraftChange = {},
            onProviderChange       = {},
            onClaudeDraftChange    = {},
            onAutoRunChange        = {},
            onDailyBriefingChange  = {},
            onBriefingTimeChange   = { _, _ -> },
            onSendTestNotification = {},
            onSave                 = {},
            onTestNotion           = {},
            onBack                 = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F, name = "Settings — saved")
@Composable
private fun SettingsSavedPreview() {
    MorningAgentTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                geminiSavedLast4 = "k7Qa",
                notionSavedLast4 = "9F2c",
                savedDatabaseId  = "abc123def456789012345678901234ab",
                databaseDraft    = "abc123def456789012345678901234ab",
                justSaved        = true,
            ),
            onGeminiDraftChange   = {},
            onNotionDraftChange   = {},
            onDatabaseDraftChange = {},
            onProviderChange       = {},
            onClaudeDraftChange    = {},
            onAutoRunChange        = {},
            onDailyBriefingChange  = {},
            onBriefingTimeChange   = { _, _ -> },
            onSendTestNotification = {},
            onSave                 = {},
            onTestNotion           = {},
            onBack                 = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0F, name = "Settings — typing")
@Composable
private fun SettingsTypingPreview() {
    MorningAgentTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                geminiDraft   = "AIzaSy_PreviewKey",
                databaseDraft = "https://www.notion.so/Tasks-abc123def456789012345678901234ab",
            ),
            onGeminiDraftChange   = {},
            onNotionDraftChange   = {},
            onDatabaseDraftChange = {},
            onProviderChange       = {},
            onClaudeDraftChange    = {},
            onAutoRunChange        = {},
            onDailyBriefingChange  = {},
            onBriefingTimeChange   = { _, _ -> },
            onSendTestNotification = {},
            onSave                 = {},
            onTestNotion           = {},
            onBack                 = {},
        )
    }
}
