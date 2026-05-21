package com.luna.morningagent.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.luna.morningagent.R
import com.luna.morningagent.data.sharedposts.CategoryDefinition
import com.luna.morningagent.ui.settings.CATEGORY_NAME_MAX_CHARS
import com.luna.morningagent.ui.settings.CATEGORY_NAME_MIN_CHARS
import com.luna.morningagent.ui.settings.SEED_CATEGORY_NAME
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import kotlinx.coroutines.launch

/**
 * Section in Settings that lists every saved-post category as a preview row.
 * Tapping a row (or the "Add category" affordance) opens a bottom sheet for
 * focused editing — name + keyword chips + delete.
 *
 * UI rules:
 *  · "Misc" (the categorizer's fallback seed) is editable but not deletable.
 *  · Names must be [CATEGORY_NAME_MIN_CHARS]–[CATEGORY_NAME_MAX_CHARS] chars.
 *  · Keywords are stored verbatim; the agent treats them as fuzzy semantic
 *    hints (synonyms / related concepts / cross-language all count).
 */
@Composable
fun SavedPostCategoriesSection(
    categories: List<CategoryDefinition>,
    postCountsByCategory: Map<String, Int>,
    onAdd: (name: String, keywords: List<String>) -> Unit,
    onUpdate: (oldName: String, newName: String, keywords: List<String>) -> Unit,
    onRemove: (name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetMode: SheetMode by remember { mutableStateOf(SheetMode.Hidden) }

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text     = stringResource(R.string.settings_saved_posts_count, categories.size),
            style    = MorningType.MetaMono,
            color    = MaterialTheme.morning.textMuted,
            modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 6.dp),
        )

        categories.forEach { cat ->
            CategoryPreviewRow(
                category = cat,
                count    = postCountsByCategory[cat.name] ?: 0,
                onClick  = { sheetMode = SheetMode.Editing(cat) },
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        AddCategoryRow(onClick = { sheetMode = SheetMode.Adding })
    }

    when (val mode = sheetMode) {
        SheetMode.Hidden -> Unit
        SheetMode.Adding -> CategoryEditSheet(
            initial    = null,
            isSeed     = false,
            onDismiss  = { sheetMode = SheetMode.Hidden },
            onSave     = { name, keywords ->
                onAdd(name, keywords)
                sheetMode = SheetMode.Hidden
            },
            onDelete   = null,
        )
        is SheetMode.Editing -> CategoryEditSheet(
            initial    = mode.target,
            isSeed     = mode.target.name == SEED_CATEGORY_NAME,
            onDismiss  = { sheetMode = SheetMode.Hidden },
            onSave     = { name, keywords ->
                onUpdate(mode.target.name, name, keywords)
                sheetMode = SheetMode.Hidden
            },
            onDelete   = {
                onRemove(mode.target.name)
                sheetMode = SheetMode.Hidden
            },
        )
    }
}

private sealed interface SheetMode {
    data object Hidden : SheetMode
    data object Adding : SheetMode
    data class  Editing(val target: CategoryDefinition) : SheetMode
}

// --- Preview row + add row ------------------------------------------------

@Composable
private fun CategoryPreviewRow(
    category: CategoryDefinition,
    count: Int,
    onClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    val previewText = if (category.keywords.isEmpty()) {
        stringResource(R.string.settings_saved_posts_no_keywords)
    } else {
        category.keywords.joinToString("  ·  ")
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text     = category.name,
                style    = MorningType.RowTitle,
                color    = morning.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text  = count.toString(),
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector        = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint               = morning.textMuted,
                modifier           = Modifier.size(18.dp),
            )
        }
        Text(
            text     = previewText,
            style    = MorningType.MetaMono,
            color    = if (category.keywords.isEmpty()) morning.textMuted.copy(alpha = 0.6f)
                       else morning.textSecondary,
            maxLines = 1,
            modifier = Modifier.padding(start = 1.dp),
        )
    }
}

@Composable
private fun AddCategoryRow(onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
    ) {
        Text(
            text  = stringResource(R.string.settings_saved_posts_add_category),
            style = MorningType.RowTitle,
            color = morning.accent,
        )
    }
}

// --- Bottom-sheet edit form ----------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditSheet(
    initial: CategoryDefinition?,
    isSeed: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, keywords: List<String>) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val morning      = MaterialTheme.morning
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutine    = rememberCoroutineScope()

    var name        by remember { mutableStateOf(initial?.name.orEmpty()) }
    var keywords    by remember { mutableStateOf(initial?.keywords.orEmpty()) }
    var keywordDraft by remember { mutableStateOf("") }

    val nameOk      = name.trim().length in CATEGORY_NAME_MIN_CHARS..CATEGORY_NAME_MAX_CHARS
    val nameTrimmed = name.trim()

    fun commitKeyword() {
        val v = keywordDraft.trim()
        if (v.isBlank()) return
        if (v in keywords) { keywordDraft = ""; return }
        keywords = keywords + v
        keywordDraft = ""
    }
    fun closeSheet() {
        coroutine.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = morning.surface,
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text  = stringResource(
                    if (initial == null) R.string.settings_saved_posts_sheet_title_add
                    else                  R.string.settings_saved_posts_sheet_title_edit,
                ),
                style = MorningType.ScreenTitle,
                color = morning.textPrimary,
            )

            // Name
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = stringResource(R.string.settings_saved_posts_field_name),
                    style = MorningType.LabelMono,
                    color = morning.textMuted,
                )
                OutlinedTextField(
                    value           = name,
                    onValueChange   = { input ->
                        // Soft cap so the field can't grow past the limit. Below the
                        // floor is still allowed while typing — the Save button is
                        // what enforces it.
                        name = input.take(CATEGORY_NAME_MAX_CHARS)
                    },
                    singleLine      = true,
                    enabled         = !isSeed,                            // Misc name is immutable
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction      = ImeAction.Next,
                    ),
                    supportingText  = {
                        Text(
                            text = stringResource(
                                R.string.settings_saved_posts_name_counter,
                                nameTrimmed.length, CATEGORY_NAME_MAX_CHARS,
                            ),
                            style = MorningType.MetaMono,
                            color = if (nameOk || name.isEmpty()) morning.textMuted else morning.accent,
                        )
                    },
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = morning.accent,
                        unfocusedBorderColor = morning.textMuted.copy(alpha = 0.4f),
                    ),
                    modifier        = Modifier.fillMaxWidth(),
                )
            }

            // Keywords
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text  = stringResource(R.string.settings_saved_posts_field_keywords),
                    style = MorningType.LabelMono,
                    color = morning.textMuted,
                )
                Text(
                    text  = stringResource(R.string.settings_saved_posts_field_keywords_sub),
                    style = MorningType.MetaMono,
                    color = morning.textMuted.copy(alpha = 0.85f),
                )
                if (keywords.isNotEmpty()) {
                    FlowRow(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(6.dp),
                    ) {
                        keywords.forEach { kw ->
                            KeywordChip(
                                label    = kw,
                                onRemove = { keywords = keywords - kw },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value           = keywordDraft,
                    onValueChange   = { keywordDraft = it },
                    singleLine      = true,
                    placeholder     = {
                        Text(
                            text  = stringResource(R.string.settings_saved_posts_keyword_placeholder),
                            style = MorningType.BodyReadItalic,
                            color = morning.textMuted,
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction      = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commitKeyword() }),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = morning.accent,
                        unfocusedBorderColor = morning.textMuted.copy(alpha = 0.4f),
                    ),
                    modifier        = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }

            // Save
            SheetActionButton(
                label   = stringResource(R.string.settings_saved_posts_save),
                enabled = nameOk,
                onClick = {
                    val finalKeywords = if (keywordDraft.isNotBlank()) keywords + keywordDraft.trim()
                                        else keywords
                    onSave(nameTrimmed, finalKeywords.distinct())
                },
            )

            if (onDelete != null) {
                if (isSeed) {
                    Text(
                        text     = stringResource(R.string.settings_saved_posts_seed_lock),
                        style    = MorningType.MetaMono,
                        color    = morning.textMuted,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                } else {
                    TextButton(
                        onClick  = onDelete,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            text  = stringResource(R.string.settings_saved_posts_delete),
                            style = MorningType.ButtonLabel,
                            color = morning.accent,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun KeywordChip(label: String, onRemove: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(morning.accent.copy(alpha = 0.12f))
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = label,
            style = MorningType.MetaMono,
            color = morning.textPrimary,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier         = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cd_remove_keyword),
                tint               = morning.textPrimary.copy(alpha = 0.7f),
                modifier           = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun SheetActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    val bg = if (enabled) morning.accent else morning.textMuted.copy(alpha = 0.18f)
    val fg = if (enabled) morning.onAccent else morning.textMuted
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text  = label,
            style = MorningType.ButtonLabel,
            color = fg,
        )
    }
}
