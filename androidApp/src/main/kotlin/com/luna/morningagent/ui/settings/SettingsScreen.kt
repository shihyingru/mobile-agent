package com.luna.morningagent.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.data.agent.ClaudeModelOption
import com.luna.morningagent.data.agent.GeminiModelOption
import com.luna.morningagent.data.agent.ProviderOption
import com.luna.morningagent.ui.settings.components.BehaviorDivider
import com.luna.morningagent.ui.settings.components.BehaviorToggleRow
import com.luna.morningagent.ui.settings.components.BehaviorValueRow
import com.luna.morningagent.ui.settings.components.ModelChip
import com.luna.morningagent.ui.settings.components.ProviderTile
import com.luna.morningagent.ui.settings.components.ProviderTileData
import com.luna.morningagent.ui.settings.components.SettingsInput
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
        savedPostCategories    = vm.savedPostCategories,
        onProviderChange       = vm::setProvider,
        onModelChange          = vm::setSelectedModel,
        onGeminiDraftChange    = vm::updateGeminiDraft,
        onClaudeDraftChange    = vm::updateClaudeDraft,
        onNotionDraftChange    = vm::updateNotionDraft,
        onDatabaseDraftChange  = vm::updateDatabaseDraft,
        onAutoRunChange        = vm::setAutoRun,
        onDailyBriefingChange  = vm::setDailyBriefing,
        onBriefingTimeChange   = vm::setBriefingTime,
        onSendTestNotification = vm::sendTestNotification,
        onTestNotion           = vm::testNotionConnection,
        onSave                 = vm::save,
        onRenameCategory       = vm::renameSavedPostCategory,
        onDeleteCategory       = vm::deleteSavedPostCategory,
        onBack                 = handleBack,
        modifier               = modifier,
    )
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    savedPostCategories: List<CategoryEntry>,
    onProviderChange: (ProviderOption) -> Unit,
    onModelChange: (String) -> Unit,
    onGeminiDraftChange: (String) -> Unit,
    onClaudeDraftChange: (String) -> Unit,
    onNotionDraftChange: (String) -> Unit,
    onDatabaseDraftChange: (String) -> Unit,
    onAutoRunChange: (Boolean) -> Unit,
    onDailyBriefingChange: (Boolean) -> Unit,
    onBriefingTimeChange: (Int, Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onTestNotion: () -> Unit,
    onSave: () -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars    = WindowInsets.navigationBars.asPaddingValues()

    val hasChanges = uiState.geminiDraft.isNotEmpty() ||
        uiState.claudeDraft.isNotEmpty() ||
        uiState.notionDraft.isNotEmpty() ||
        uiState.databaseDraft != uiState.savedDatabaseId

    var showTimePicker by remember { mutableStateOf(false) }
    var renameTarget: CategoryEntry? by remember { mutableStateOf(null) }
    var deleteTarget: CategoryEntry? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(morning.background)
            // imePadding shrinks the available area when the soft keyboard
            // shows, so the verticalScroll + bringIntoView combo can pull the
            // focused field above the IME instead of leaving it occluded.
            .imePadding(),
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start  = 18.dp,
                    end    = 18.dp,
                    top    = 18.dp + statusBars.calculateTopPadding(),
                    bottom = 36.dp + navBars.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IconButton(
                    onClick  = onBack,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint               = morning.textPrimary,
                    )
                }
                Text(
                    text  = stringResource(R.string.settings_title),
                    style = MorningType.ScreenTitle,
                    color = morning.textPrimary,
                )
            }

            // Intro
            Text(
                text     = stringResource(R.string.settings_intro),
                style    = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                color    = morning.textSecondary,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
            )

            // AI Provider
            SectionLabel(text = stringResource(R.string.settings_section_provider))
            ProviderRow(
                selected         = uiState.selectedProvider,
                selectedModelId  = uiState.selectedModelId,
                onSelect         = onProviderChange,
            )

            // Model picker (vertical chips for the active provider)
            ModelSection(
                provider        = uiState.selectedProvider,
                selectedModelId = uiState.selectedModelId,
                onSelect        = onModelChange,
            )

            // Credentials
            SectionLabel(
                text     = stringResource(R.string.settings_section_credentials),
                modifier = Modifier.padding(top = 14.dp),
            )

            // Active provider's API key — show only the matching field.
            when (uiState.selectedProvider) {
                ProviderOption.Gemini -> SettingsInput(
                    label           = stringResource(R.string.settings_field_gemini),
                    value           = uiState.geminiDraft,
                    onValueChange   = onGeminiDraftChange,
                    placeholder     = uiState.geminiSavedLast4?.let { "••••$it" }
                                       ?: stringResource(R.string.settings_field_gemini),
                    secret          = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction      = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                )
                ProviderOption.Claude -> SettingsInput(
                    label           = stringResource(R.string.settings_field_claude),
                    value           = uiState.claudeDraft,
                    onValueChange   = onClaudeDraftChange,
                    placeholder     = uiState.claudeSavedLast4?.let { "••••$it" }
                                       ?: stringResource(R.string.settings_field_claude),
                    secret          = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction      = ImeAction.Next,
                    ),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                )
            }

            SettingsInput(
                label           = stringResource(R.string.settings_field_notion),
                value           = uiState.notionDraft,
                onValueChange   = onNotionDraftChange,
                placeholder     = uiState.notionSavedLast4?.let { "••••$it" }
                                   ?: stringResource(R.string.settings_field_notion),
                secret          = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction      = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            )

            // Data source
            SectionLabel(
                text     = stringResource(R.string.settings_section_data_source),
                modifier = Modifier.padding(top = 14.dp),
            )
            SettingsInput(
                label           = stringResource(R.string.settings_field_database),
                value           = uiState.databaseDraft,
                onValueChange   = onDatabaseDraftChange,
                placeholder     = stringResource(R.string.settings_field_database),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction      = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                }),
            )

            // Save area
            Spacer(modifier = Modifier.height(6.dp))
            SaveButton(
                enabled  = hasChanges,
                onClick  = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    onSave()
                },
            )
            if (uiState.justSaved) {
                SavedFeedback()
            }
            TextButton(
                onClick  = onTestNotion,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text  = stringResource(R.string.settings_button_test),
                    style = MorningType.ButtonLabel,
                    color = morning.accent,
                )
            }
            NotionTestStatus(uiState.notionTest)

            // Behavior
            SectionLabel(
                text     = stringResource(R.string.settings_section_behavior),
                modifier = Modifier.padding(top = 18.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                BehaviorDivider()
                BehaviorToggleRow(
                    title    = stringResource(R.string.settings_auto_run_label),
                    sub      = stringResource(R.string.settings_behavior_auto_run_sub),
                    on       = uiState.autoRun,
                    onToggle = onAutoRunChange,
                )
                BehaviorDivider()
                BehaviorToggleRow(
                    title    = stringResource(R.string.settings_daily_briefing_label),
                    sub      = stringResource(R.string.settings_behavior_daily_briefing_sub),
                    on       = uiState.dailyBriefing,
                    onToggle = onDailyBriefingChange,
                )
                BehaviorDivider()
                if (uiState.dailyBriefing) {
                    BehaviorValueRow(
                        title   = stringResource(R.string.settings_briefing_time_label),
                        sub     = stringResource(R.string.settings_behavior_briefing_time_sub),
                        value   = formatTime(uiState.briefingHour, uiState.briefingMinute),
                        onClick = { showTimePicker = true },
                    )
                    BehaviorDivider()
                }
            }

            // Send test notification (only when daily briefing is on, mirrors the
            // worker visibility — no point posting from a feature that's off).
            if (uiState.dailyBriefing) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick  = onSendTestNotification,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        text  = stringResource(R.string.settings_button_test_notification),
                        style = MorningType.ButtonLabel,
                        color = morning.accent,
                    )
                }
            }

            // Saved posts — shows the taxonomy the agent has grown organically.
            // Always visible (the seed taxonomy is ["Misc"], so the list is
            // never empty); rows where count = 0 just read "0 saves" and can
            // still be renamed or removed.
            if (savedPostCategories.isNotEmpty()) {
                SavedPostsTaxonomySection(
                    categories     = savedPostCategories,
                    onRowClick     = { renameTarget = it },
                    onDeleteClick  = { deleteTarget = it },
                )
            }
        }
    }

    renameTarget?.let { target ->
        RenameCategoryDialog(
            current   = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRenameCategory(target.name, newName)
                renameTarget = null
            },
        )
    }
    deleteTarget?.let { target ->
        DeleteCategoryDialog(
            target    = target,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteCategory(target.name)
                deleteTarget = null
            },
        )
    }

    if (showTimePicker) {
        BriefingTimePickerDialog(
            initialHour   = uiState.briefingHour,
            initialMinute = uiState.briefingMinute,
            onDismiss     = { showTimePicker = false },
            onConfirm     = { hour, minute ->
                onBriefingTimeChange(hour, minute)
                showTimePicker = false
            },
        )
    }
}

// --- Section pieces ------------------------------------------------------

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val morning = MaterialTheme.morning
    Text(
        text     = text,
        style    = MorningType.LabelMono.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color    = morning.textMuted,
        modifier = modifier.padding(top = 4.dp, bottom = 6.dp),
    )
}

@Composable
private fun ProviderRow(
    selected: ProviderOption,
    selectedModelId: String,
    onSelect: (ProviderOption) -> Unit,
) {
    val geminiActive = selected == ProviderOption.Gemini
    val claudeActive = selected == ProviderOption.Claude

    val geminiTile = ProviderTileData(
        id      = "gemini", name = "Gemini", glyph = "G",
        brandBg = Color(0xFF4285F4), brandFg = Color.White,
        activeModelLabel = if (geminiActive) selectedModelId else GeminiModelOption.Default.id,
        extraModelCount  = GeminiModelOption.entries.size - 1,
    )
    val claudeTile = ProviderTileData(
        id      = "claude", name = "Claude", glyph = "C",
        brandBg = Color(0xFFD97757), brandFg = Color.White,
        activeModelLabel = if (claudeActive) selectedModelId else ClaudeModelOption.Default.id,
        extraModelCount  = ClaudeModelOption.entries.size - 1,
    )
    val openaiTile = ProviderTileData(
        id      = "openai", name = "OpenAI", glyph = "O",
        brandBg = Color(0xFF0A0A0A), brandFg = Color.White,
        activeModelLabel = null,                 // → "Coming soon" caption
        extraModelCount  = 0,
        disabled         = true,
    )

    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp),
        contentPadding        = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ProviderTile(
                data     = geminiTile,
                selected = geminiActive,
                onClick  = { onSelect(ProviderOption.Gemini) },
            )
        }
        item {
            ProviderTile(
                data     = claudeTile,
                selected = claudeActive,
                onClick  = { onSelect(ProviderOption.Claude) },
            )
        }
        item {
            ProviderTile(
                data     = openaiTile,
                selected = false,
                onClick  = {},          // disabled — never fires anyway
            )
        }
    }
}

@Composable
private fun ModelSection(
    provider: ProviderOption,
    selectedModelId: String,
    onSelect: (String) -> Unit,
) {
    val morning = MaterialTheme.morning
    val options = when (provider) {
        ProviderOption.Gemini -> GeminiModelOption.entries.map { it.id to it.tagline }
        ProviderOption.Claude -> ClaudeModelOption.entries.map { it.id to it.tagline }
    }
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = stringResource(R.string.section_model),
            style = MorningType.LabelMono.copy(letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = morning.textMuted,
        )
        Text(
            text  = stringResource(R.string.settings_model_options_meta, provider.displayName.lowercase(), options.size),
            style = MorningType.MetaMono,
            color = morning.textMuted,
        )
    }
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, note) ->
            ModelChip(
                label    = id,
                note     = note,
                selected = id == selectedModelId,
                onClick  = { onSelect(id) },
            )
        }
    }
}

@Composable
private fun SaveButton(enabled: Boolean, onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    val bg   = if (enabled) morning.accent else morning.textMuted.copy(alpha = 0.18f)
    val fg   = if (enabled) morning.onAccent else morning.textMuted

    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .let {
                if (enabled) it.shadow(
                    elevation    = 12.dp,
                    shape        = RoundedCornerShape(14.dp),
                    spotColor    = morning.accent,
                    ambientColor = morning.accent,
                ) else it
            }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = stringResource(R.string.settings_button_save),
            style = MorningType.ButtonLabel.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = fg,
        )
    }
}

@Composable
private fun SavedFeedback() {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector        = Icons.Rounded.Check,
            contentDescription = null,
            tint               = morning.success,
            modifier           = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text  = stringResource(R.string.settings_feedback_saved),
            style = MorningType.ButtonLabel,
            color = morning.success,
        )
    }
}

@Composable
private fun NotionTestStatus(test: NotionTestResult?) {
    if (test == null) return
    val morning = MaterialTheme.morning
    val (text, color) = when (test) {
        is NotionTestResult.InProgress -> stringResource(R.string.settings_test_in_progress) to morning.textSecondary
        is NotionTestResult.Success    -> stringResource(R.string.settings_test_success, test.taskCount) to morning.success
        is NotionTestResult.Failure    -> stringResource(R.string.settings_test_failure, test.message) to morning.error
    }
    Text(
        text      = text,
        style     = MorningType.MetaMono.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color     = color,
        textAlign = TextAlign.Center,
        modifier  = Modifier.fillMaxWidth(),
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }
    return "%d:%02d %s".format(h12, minute, period)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefingTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val morning = MaterialTheme.morning
    val state = rememberTimePickerState(
        initialHour   = initialHour,
        initialMinute = initialMinute,
        is24Hour      = false,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = morning.surface,
        title = {
            Text(
                text  = stringResource(R.string.settings_time_picker_title),
                style = MorningType.SectionHeading,
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
                        periodSelectorBorderColor  = morning.cardEdge,
                        clockDialSelectedContentColor   = morning.onAccent,
                        clockDialUnselectedContentColor = morning.textPrimary,
                        periodSelectorSelectedContainerColor   = morning.accent,
                        periodSelectorUnselectedContainerColor = morning.surface,
                        periodSelectorSelectedContentColor     = morning.onAccent,
                        periodSelectorUnselectedContentColor   = morning.textSecondary,
                        timeSelectorSelectedContainerColor     = morning.accentSoft,
                        timeSelectorUnselectedContainerColor   = morning.background,
                        timeSelectorSelectedContentColor       = morning.accent,
                        timeSelectorUnselectedContentColor     = morning.textPrimary,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(
                    text  = stringResource(R.string.settings_time_picker_confirm),
                    style = MorningType.ButtonLabel,
                    color = morning.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = stringResource(R.string.settings_time_picker_cancel),
                    style = MorningType.ButtonLabel,
                    color = morning.textMuted,
                )
            }
        },
    )
}

// --- Saved-posts taxonomy section -----------------------------------------

@Composable
private fun SavedPostsTaxonomySection(
    categories: List<CategoryEntry>,
    onRowClick: (CategoryEntry) -> Unit,
    onDeleteClick: (CategoryEntry) -> Unit,
) {
    val morning = MaterialTheme.morning
    SectionLabel(
        text     = stringResource(R.string.settings_section_saved_posts),
        modifier = Modifier.padding(top = 18.dp),
    )
    Text(
        text     = stringResource(R.string.settings_saved_posts_intro),
        style    = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
        color    = morning.textSecondary,
        modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        BehaviorDivider()
        categories.forEach { entry ->
            CategoryRow(
                entry         = entry,
                onClick       = { onRowClick(entry) },
                onDeleteClick = { onDeleteClick(entry) },
            )
            BehaviorDivider()
        }
    }
}

@Composable
private fun CategoryRow(
    entry: CategoryEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = entry.name,
                style = MorningType.RowTitle,
                color = morning.textPrimary,
            )
            Text(
                text  = stringResource(R.string.settings_saved_posts_count, entry.count),
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector        = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.cd_delete_category),
                tint               = morning.accent,
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RenameCategoryDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val morning = MaterialTheme.morning
    var draft by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = morning.surface,
        title = {
            Text(
                text  = stringResource(R.string.settings_saved_posts_rename_title),
                style = MorningType.SectionHeading,
                color = morning.textPrimary,
            )
        },
        text = {
            SettingsInput(
                label         = current,
                value         = draft,
                onValueChange = { draft = it },
                placeholder   = current,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(
                enabled = draft.trim().isNotBlank() && draft.trim() != current,
                onClick = { onConfirm(draft.trim()) },
            ) {
                Text(
                    text  = stringResource(R.string.settings_time_picker_confirm),
                    style = MorningType.ButtonLabel,
                    color = morning.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = stringResource(R.string.settings_time_picker_cancel),
                    style = MorningType.ButtonLabel,
                    color = morning.textMuted,
                )
            }
        },
    )
}

@Composable
private fun DeleteCategoryDialog(
    target: CategoryEntry,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val morning = MaterialTheme.morning
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = morning.surface,
        title = {
            Text(
                text  = stringResource(R.string.settings_saved_posts_delete_title, target.name),
                style = MorningType.SectionHeading,
                color = morning.textPrimary,
            )
        },
        text = {
            Text(
                text  = stringResource(R.string.settings_saved_posts_delete_body, target.count),
                style = MorningType.BodyReadItalic,
                color = morning.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text  = "Delete",
                    style = MorningType.ButtonLabel,
                    color = morning.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text  = stringResource(R.string.settings_time_picker_cancel),
                    style = MorningType.ButtonLabel,
                    color = morning.textMuted,
                )
            }
        },
    )
}

// --- Previews -------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD, name = "Settings – empty (Light)")
@Composable
private fun SettingsEmptyPreview() {
    MorningAgentTheme {
        SettingsScreenContent(
            uiState                = SettingsUiState(selectedModelId = GeminiModelOption.Default.id),
            onProviderChange       = {},
            onModelChange          = {},
            onGeminiDraftChange    = {},
            onClaudeDraftChange    = {},
            onNotionDraftChange    = {},
            onDatabaseDraftChange  = {},
            onAutoRunChange        = {},
            onDailyBriefingChange  = {},
            onBriefingTimeChange   = { _, _ -> },
            savedPostCategories    = emptyList(),
            onSendTestNotification = {},
            onTestNotion           = {},
            onSave                 = {},
            onRenameCategory       = { _, _ -> },
            onDeleteCategory       = {},
            onBack                 = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD, name = "Settings – saved")
@Composable
private fun SettingsSavedPreview() {
    MorningAgentTheme {
        SettingsScreenContent(
            uiState                = SettingsUiState(
                selectedProvider = ProviderOption.Claude,
                selectedModelId  = ClaudeModelOption.Sonnet.id,
                geminiSavedLast4 = "k7Qa",
                claudeSavedLast4 = "j3xP",
                notionSavedLast4 = "9F2c",
                savedDatabaseId  = "abc123def456789012345678901234ab",
                databaseDraft    = "abc123def456789012345678901234ab",
                dailyBriefing    = true,
            ),
            onProviderChange       = {},
            onModelChange          = {},
            onGeminiDraftChange    = {},
            onClaudeDraftChange    = {},
            onNotionDraftChange    = {},
            onDatabaseDraftChange  = {},
            onAutoRunChange        = {},
            onDailyBriefingChange  = {},
            onBriefingTimeChange   = { _, _ -> },
            savedPostCategories    = emptyList(),
            onSendTestNotification = {},
            onTestNotion           = {},
            onSave                 = {},
            onRenameCategory       = { _, _ -> },
            onDeleteCategory       = {},
            onBack                 = {},
        )
    }
}
