package com.luna.morningagent.ui.sharedposts

import android.content.ClipData
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.toClipEntry
import com.luna.morningagent.R
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.ui.settings.components.SettingsInput
import com.luna.morningagent.ui.sharedposts.components.SavedPostActionSheet
import com.luna.morningagent.ui.sharedposts.components.SavedPostCard
import com.luna.morningagent.ui.theme.InterFamily
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: SavedPostsViewModel = viewModel(),
) {
    BackHandler { onBack() }
    LaunchedEffect(Unit) {
        vm.refresh()
        vm.refreshFromNotion()
    }

    // ON_RESUME re-reads the local cache so background shares made via
    // ShareReceiverActivity (which writes to the same store but doesn't touch
    // this VM) become visible the moment Luna swipes back into the app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val morning = MaterialTheme.morning
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars    = WindowInsets.navigationBars.asPaddingValues()

    var expandedId: String? by remember { mutableStateOf(null) }
    var sheetPost: SharedPost? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(morning.background)
            .imePadding(),
    ) {
        PullToRefreshBox(
            isRefreshing = vm.isRefreshing,
            onRefresh    = { vm.refreshFromNotion() },
            state        = pullState,
            modifier     = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start  = 18.dp,
                end    = 18.dp,
                top    = 14.dp + statusBars.calculateTopPadding(),
                bottom = 32.dp + navBars.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SavedHeader(onBack = onBack) }

            // Inline setup card — only when Notion DB isn't configured AND
            // there's pending work waiting to flush. Hidden once dbId lands.
            if (vm.dbId == null && vm.pendingSyncCount > 0) {
                item {
                    SetupCard(
                        urlDraft = vm.setupUrlDraft,
                        pending  = vm.pendingSyncCount,
                        state    = vm.setupState,
                        onChange = vm::onSetupUrlChange,
                        onSetUp  = vm::runSetup,
                    )
                }
            }

            item {
                SavedSearchRow(
                    value         = vm.search,
                    onValueChange = vm::onSearchChange,
                )
            }

            item {
                CategoryFilterRow(
                    categories     = vm.allCategories,
                    activeCategory = vm.activeCategory,
                    onSelect       = vm::onCategorySelect,
                )
            }

            val visible = vm.filteredPosts
            if (visible.isEmpty()) {
                item { EmptyState(hasAnyPosts = vm.posts.isNotEmpty()) }
            } else {
                items(visible, key = { it.localId }) { post ->
                    val openExternal: () -> Unit = {
                        post.url?.let { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            }
                        }
                    }
                    SavedPostCard(
                        post         = post,
                        onTap        = {
                            if (post.url != null) openExternal()
                            else expandedId = if (expandedId == post.localId) null else post.localId
                        },
                        onOverflow   = { sheetPost = post },
                        onDelete     = { vm.delete(post) },
                        bodyMaxLines = if (expandedId == post.localId) Int.MAX_VALUE else 4,
                    )
                }
            }
        }
        }   // PullToRefreshBox
    }

    sheetPost?.let { target ->
        SavedPostActionSheet(
            post           = target,
            onDismiss      = { sheetPost = null },
            onSendToAgent  = {
                // TODO(future): wire to today's briefing feed.
                sheetPost = null
            },
            onCopyLink     = {
                target.url?.let { url ->
                    scope.launch {
                        clipboard.setClipEntry(ClipData.newPlainText("Saved post URL", url).toClipEntry())
                    }
                }
                sheetPost = null
            },
            onDelete       = {
                vm.delete(target)
                sheetPost = null
            },
        )
    }
}

// --- Header --------------------------------------------------------------

@Composable
private fun SavedHeader(onBack: () -> Unit) {
    val morning = MaterialTheme.morning
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
            text  = "Saved",
            style = MorningType.ScreenTitle,
            color = morning.textPrimary,
        )
    }
}

// --- Search row (passive-looking, single-line) ---------------------------

@Composable
private fun SavedSearchRow(value: String, onValueChange: (String) -> Unit) {
    val morning = MaterialTheme.morning
    val isEmpty = value.isEmpty()
    val textStyle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
        color      = if (isEmpty) morning.textMuted else morning.textPrimary,
    )
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(morning.surface)
            .border(width = 1.dp, color = morning.cardEdge, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector        = Icons.Rounded.Search,
            contentDescription = null,
            tint               = morning.textMuted,
            modifier           = Modifier.size(15.dp),
        )
        BasicTextField(
            value           = value,
            onValueChange   = onValueChange,
            modifier        = Modifier.fillMaxWidth(),
            singleLine      = true,
            textStyle       = textStyle,
            cursorBrush     = SolidColor(morning.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            decorationBox   = { inner ->
                Box {
                    if (isEmpty) {
                        Text(
                            text  = stringResource(R.string.saved_search_placeholder),
                            style = textStyle,
                        )
                    }
                    inner()
                }
            },
        )
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
            text  = "Paste a Notion page URL (we'll create the Shared Posts database under it) or an existing Shared Posts database URL to reconnect. Pending saves flush automatically.",
            style = MorningType.BodyReadItalic.copy(fontSize = 13.sp),
            color = morning.textSecondary,
        )
        SettingsInput(
            label         = "Notion page or database URL",
            value         = urlDraft,
            onValueChange = onChange,
            placeholder   = "https://www.notion.so/…",
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
    onSelect: (String?) -> Unit,
) {
    if (categories.isEmpty()) return
    LazyRow(
        modifier              = Modifier.fillMaxWidth(),
        contentPadding        = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { FilterChip(label = "All", active = activeCategory == null, onClick = { onSelect(null) }) }
        items(categories) { name ->
            FilterChip(
                label   = name,
                active  = activeCategory == name,
                onClick = { onSelect(name) },
            )
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    val bg     = if (active) morning.accentSoft else androidx.compose.ui.graphics.Color.Transparent
    val fg     = if (active) morning.accent     else morning.textSecondary
    val border = if (active) morning.accent     else morning.cardEdge
    Box(
        modifier         = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(width = 1.dp, color = border, shape = RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text     = label,
            style    = MorningType.ButtonLabel.copy(fontSize = 12.5.sp, letterSpacing = 0.1.sp),
            color    = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// --- Empty state ----------------------------------------------------------

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
            text      = if (hasAnyPosts) "Clear the search or pick a different chip." else "Share posts to Morning Agent from any app — they'll show up here.",
            style     = MorningType.BodyReadItalic,
            color     = morning.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// --- Previews --------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun SavedPostsScreenPreview() {
    MorningAgentTheme {
        SavedPostsScreen(onBack = {})
    }
}
