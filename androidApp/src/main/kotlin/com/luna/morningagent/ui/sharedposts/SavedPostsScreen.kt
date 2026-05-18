package com.luna.morningagent.ui.sharedposts

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.ui.settings.components.SettingsInput
import com.luna.morningagent.ui.sharedposts.components.SavedPostCard
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning

@Composable
fun SavedPostsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SavedPostsViewModel = viewModel(),
) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) { vm.refresh() }

    val morning = MaterialTheme.morning
    val context = LocalContext.current

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars    = WindowInsets.navigationBars.asPaddingValues()

    var expandedId: String? by remember { mutableStateOf(null) }
    var pendingDelete: SharedPost? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(morning.background)
            .imePadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start  = 18.dp,
                end    = 18.dp,
                top    = 14.dp + statusBars.calculateTopPadding(),
                bottom = 32.dp + navBars.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SavedHeader(
                    count   = vm.posts.size,
                    onBack  = onBack,
                )
            }

            // Inline setup card — only when Notion DB isn't configured AND
            // there's pending work waiting to flush. Hidden once dbId lands.
            if (vm.dbId == null && vm.pendingSyncCount > 0) {
                item {
                    SetupCard(
                        urlDraft   = vm.setupUrlDraft,
                        pending    = vm.pendingSyncCount,
                        state      = vm.setupState,
                        onChange   = vm::onSetupUrlChange,
                        onSetUp    = vm::runSetup,
                    )
                }
            }

            item {
                SettingsInput(
                    label           = "Search saved posts",
                    value           = vm.search,
                    onValueChange   = vm::onSearchChange,
                    placeholder     = "Search content, summary, author, category",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                )
            }

            item {
                CategoryFilterRow(
                    categories      = vm.allCategories,
                    activeCategory  = vm.activeCategory,
                    counts          = vm.posts.flatMap { it.categories }.groupingBy { it }.eachCount(),
                    onSelect        = vm::onCategorySelect,
                )
            }

            val visible = vm.filteredPosts
            if (visible.isEmpty()) {
                item {
                    EmptyState(hasAnyPosts = vm.posts.isNotEmpty())
                }
            } else {
                items(visible, key = { it.localId }) { post ->
                    SavedPostCard(
                        post        = post,
                        expanded    = post.localId == expandedId,
                        onTap       = {
                            if (post.url != null) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, post.url.toUri()),
                                    )
                                }
                            } else {
                                expandedId = if (expandedId == post.localId) null else post.localId
                            }
                        },
                        onLongPress = { pendingDelete = post },
                    )
                }
            }
        }
    }

    pendingDelete?.let { target ->
        DeleteConfirmDialog(
            onDismiss = { pendingDelete = null },
            onConfirm = {
                vm.delete(target)
                pendingDelete = null
            },
        )
    }
}

// --- Header --------------------------------------------------------------

@Composable
private fun SavedHeader(count: Int, onBack: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
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
            text  = "Saved",
            style = MorningType.ScreenTitle,
            color = morning.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (count > 0) {
            Text(
                text  = "$count saved",
                style = MorningType.MetaMono,
                color = morning.textMuted,
            )
        }
    }
}

// --- Setup card ----------------------------------------------------------

@Composable
private fun SetupCard(
    urlDraft: String,
    pending: Int,
    state: SetupState,
    onChange: (String) -> Unit,
    onSetUp: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(morning.accentSoft)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text  = "$pending saves waiting · finish Notion sync",
            style = MorningType.RowTitle,
            color = morning.textPrimary,
        )
        Text(
            text  = "Paste the Notion page URL where Morning Agent should create its Shared Posts database. Pending saves flush automatically.",
            style = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = morning.textSecondary,
        )
        SettingsInput(
            label         = "Notion page URL",
            value         = urlDraft,
            onValueChange = onChange,
            placeholder   = "https://www.notion.so/Your-Page-…",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        when (state) {
            SetupState.Idle, SetupState.Done -> {
                SetupButton(label = "Set up sync", enabled = urlDraft.isNotBlank(), loading = false, onClick = onSetUp)
            }
            SetupState.InProgress -> {
                SetupButton(label = "Setting up…", enabled = false, loading = true, onClick = {})
            }
            is SetupState.Error -> {
                Text(
                    text  = state.message,
                    style = MorningType.MetaMono,
                    color = morning.error,
                )
                SetupButton(label = "Try again", enabled = urlDraft.isNotBlank(), loading = false, onClick = onSetUp)
            }
        }
    }
}

@Composable
private fun SetupButton(label: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    val bg = if (enabled) morning.accent else morning.textMuted.copy(alpha = 0.18f)
    val fg = if (enabled) morning.onAccent else morning.textMuted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .let {
                if (enabled) it.shadow(
                    elevation    = 10.dp,
                    shape        = RoundedCornerShape(12.dp),
                    spotColor    = morning.accent,
                    ambientColor = morning.accent,
                ) else it
            }
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color       = fg,
                strokeWidth = 2.dp,
                modifier    = Modifier.size(18.dp),
            )
        } else {
            Text(
                text  = label,
                style = MorningType.ButtonLabel,
                color = fg,
            )
        }
    }
}

// --- Category filter row -------------------------------------------------

@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    activeCategory: String?,
    counts: Map<String, Int>,
    onSelect: (String?) -> Unit,
) {
    if (categories.isEmpty()) return
    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentPadding        = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterChip(label = "All", count = null, active = activeCategory == null, onClick = { onSelect(null) }) }
        items(categories) { name ->
            FilterChip(
                label   = name,
                count   = counts[name],
                active  = activeCategory == name,
                onClick = { onSelect(name) },
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, count: Int?, active: Boolean, onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    val bg = if (active) morning.accent else morning.surface
    val fg = if (active) morning.onAccent else morning.textPrimary
    val border = if (active) morning.accent else morning.cardEdge
    Row(
        modifier              = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text  = label,
            style = MorningType.ButtonLabel,
            color = fg,
        )
        count?.let {
            Text(
                text  = it.toString(),
                style = MorningType.MetaMono,
                color = if (active) morning.onAccent.copy(alpha = 0.7f) else morning.textMuted,
            )
        }
    }
}

// --- Empty state + delete dialog ------------------------------------------

@Composable
private fun EmptyState(hasAnyPosts: Boolean) {
    val morning = MaterialTheme.morning
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text      = if (hasAnyPosts) "Nothing matches that filter" else "Nothing saved yet",
            style     = MorningType.SectionHeading,
            color     = morning.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text      = if (hasAnyPosts) "Clear the search or pick a different chip." else "Share posts to Morning Agent from any app — they’ll show up here.",
            style     = MorningType.BodyReadItalic,
            color     = morning.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val morning = MaterialTheme.morning
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = morning.surface,
        title = {
            Text(
                text  = "Delete this saved post?",
                style = MorningType.SectionHeading,
                color = morning.textPrimary,
            )
        },
        text = {
            Text(
                text  = "It’ll be removed from the local cache and archived in Notion (if synced).",
                style = MorningType.BodyReadItalic,
                color = morning.textSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", style = MorningType.ButtonLabel, color = morning.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", style = MorningType.ButtonLabel, color = morning.textMuted)
            }
        },
    )
}

// --- Previews --------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun SavedPostsScreenPreview() {
    MorningAgentTheme {
        SavedPostsScreen(onBack = {})
    }
}
