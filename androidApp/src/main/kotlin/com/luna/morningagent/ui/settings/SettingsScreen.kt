package com.luna.morningagent.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.ui.theme.ColorBackground
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
        uiState               = vm.uiState,
        onGeminiDraftChange   = vm::updateGeminiDraft,
        onNotionDraftChange   = vm::updateNotionDraft,
        onDatabaseDraftChange = vm::updateDatabaseDraft,
        onSave                = vm::save,
        onTestNotion          = vm::testNotionConnection,
        onBack                = handleBack,
        modifier              = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onGeminiDraftChange: (String) -> Unit,
    onNotionDraftChange: (String) -> Unit,
    onDatabaseDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onTestNotion: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val canSave = uiState.geminiDraft.isNotEmpty() ||
        uiState.notionDraft.isNotEmpty() ||
        uiState.databaseDraft != uiState.savedDatabaseId

    Scaffold(
        modifier       = modifier,
        containerColor = ColorBackground,
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
                    containerColor    = ColorBackground,
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
                text  = stringResource(R.string.settings_section_credentials),
                style = MorningType.Label,
                color = morning.textMuted,
            )

            SecretField(
                label       = stringResource(R.string.settings_field_gemini),
                draft       = uiState.geminiDraft,
                savedLast4  = uiState.geminiSavedLast4,
                onChange    = onGeminiDraftChange,
                imeAction   = ImeAction.Next,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                ),
            )

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
                isSaved     = uiState.savedDatabaseId.isNotEmpty(),
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
        }
    }
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
    val savedHint: (@Composable () -> Unit)? = if (!savedLast4.isNullOrEmpty()) {
        {
            Text(
                text  = stringResource(R.string.settings_saved_hint, savedLast4),
                color = morning.textSecondary,
            )
        }
    } else null

    OutlinedTextField(
        value         = draft,
        onValueChange = onChange,
        label         = { Text(label, style = MorningType.Body) },
        supportingText = savedHint,
        singleLine    = true,
        visualTransformation = PasswordVisualTransformation(),
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

@Composable
private fun PlainField(
    label: String,
    value: String,
    isSaved: Boolean,
    onChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val morning = MaterialTheme.morning
    val savedHint: (@Composable () -> Unit)? = if (isSaved) {
        {
            Text(
                text  = stringResource(R.string.settings_saved_hint_db),
                color = morning.textSecondary,
            )
        }
    } else null

    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label, style = MorningType.Body) },
        supportingText = savedHint,
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
            onSave                = {},
            onTestNotion          = {},
            onBack                = {},
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
            onSave                = {},
            onTestNotion          = {},
            onBack                = {},
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
            onSave                = {},
            onTestNotion          = {},
            onBack                = {},
        )
    }
}
