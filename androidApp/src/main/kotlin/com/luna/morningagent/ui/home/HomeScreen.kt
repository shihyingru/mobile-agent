package com.luna.morningagent.ui.home

import android.content.Intent
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.ui.common.SnackbarEvent
import com.luna.morningagent.ui.home.components.BriefingActions
import com.luna.morningagent.ui.home.components.BriefingBlock
import com.luna.morningagent.ui.home.components.StatusRow
import com.luna.morningagent.ui.home.components.TaskCard
import com.luna.morningagent.ui.home.components.TempPlanEmptyCard
import com.luna.morningagent.ui.home.components.TempPlanSection
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import com.luna.morningagent.ui.theme.slotCopy

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToTempPlan: () -> Unit = {},
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
) {
    // Re-sync clock labels on entry — covers the "user changed time in Settings
    // and came back" case faster than waiting for the next 30s tick.
    LaunchedEffect(Unit) { vm.refreshClock() }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarEvent = vm.snackbarEvent
    val resolvedSnackbar = when (snackbarEvent) {
        is SnackbarEvent.ResId -> stringResource(snackbarEvent.id)
        is SnackbarEvent.ResIdWithArgs -> stringResource(snackbarEvent.id, *snackbarEvent.args.toTypedArray())
        is SnackbarEvent.Plain -> snackbarEvent.message
        null -> null
    }
    LaunchedEffect(snackbarEvent) {
        resolvedSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.snackbarShown()
        }
    }

    HomeScreenContent(
        uiState                 = vm.uiState,
        nextRunLabel            = vm.nextRunLabel,
        headerDateLine          = vm.headerDateLine,
        pendingSharedPosts      = vm.pendingSharedPostsCount,
        sharedPostsDbConfigured = vm.sharedPostsDbConfigured,
        activeTempPlan          = vm.activeTempPlan,
        snackbarHostState       = snackbarHostState,
        onRunNow                = vm::runNow,
        onApplyAction           = vm::applyAction,
        onDismissAction         = vm::dismissAction,
        onToggleTempTask        = vm::toggleTempTask,
        onPromoteTempTask       = vm::promoteTempTask,
        onNavigateToSettings    = onNavigateToSettings,
        onNavigateToSavedPosts  = onNavigateToSavedPosts,
        onNavigateToTempPlan    = onNavigateToTempPlan,
        modifier                = modifier,
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    nextRunLabel: String?,
    headerDateLine: String,
    pendingSharedPosts: Int,
    sharedPostsDbConfigured: Boolean,
    activeTempPlan: TempPlan?,
    onRunNow: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToTempPlan: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onApplyAction: (ProposedAction) -> Unit = {},
    onDismissAction: (ProposedAction) -> Unit = {},
    onToggleTempTask: (String) -> Unit = {},
    onPromoteTempTask: (String) -> Unit = {},
) {
    val morning = MaterialTheme.morning
    val context = LocalContext.current
    val isLoading = uiState is HomeUiState.Loading
    val briefing  = (uiState as? HomeUiState.Success)?.briefing
    val tasks     = briefing?.tasks.orEmpty()

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars    = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(morning.background),
    ) {
        // Paper radial wash — subtle Sand/Navy bloom at the top.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors  = listOf(morning.backgroundWash, morning.background),
                        center  = Offset(0.5f, 0f),
                        radius  = 1800f,
                    ),
                ),
        )

        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(
                start  = 18.dp,
                end    = 18.dp,
                top    = 22.dp + statusBars.calculateTopPadding(),
                bottom = 32.dp + navBars.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TopBar(
                    onSettingsClick = onNavigateToSettings,
                    onSavedClick    = onNavigateToSavedPosts,
                )
            }

            item { GreetingBlock(dateLine = headerDateLine) }

            // Banner only when Luna shared posts BEFORE configuring the Notion
            // mirror — tap routes to SavedPostsScreen where the inline setup
            // card lives. Hidden once dbId lands.
            if (pendingSharedPosts > 0 && !sharedPostsDbConfigured) {
                item {
                    PendingSavesBanner(
                        count   = pendingSharedPosts,
                        onClick = onNavigateToSavedPosts,
                    )
                }
            }

            if (uiState is HomeUiState.Error) {
                item { ErrorBanner(message = uiState.message, onOpenSettings = onNavigateToSettings) }
            }

            item {
                StatusRow(
                    isLoading    = isLoading,
                    onRunNow     = onRunNow,
                    nextRunLabel = nextRunLabel,
                    modifier     = Modifier.padding(horizontal = 4.dp),
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .height(1.dp)
                        .background(morning.cardEdge),
                )
            }

            item {
                BriefingBlock(
                    briefing = briefing,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }

            if (briefing != null) {
                val visibleActions = briefing.actions
                    .filterNot { it.stableKey() in briefing.dismissedActionIds }
                if (visibleActions.isNotEmpty()) {
                    item {
                        BriefingActions(
                            actions  = visibleActions,
                            tasks    = briefing.tasks,
                            onApply  = onApplyAction,
                            onDismiss = onDismissAction,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }

            item {
                if (activeTempPlan != null) {
                    TempPlanSection(
                        plan          = activeTempPlan,
                        onToggleTask  = onToggleTempTask,
                        onPromoteTask = onPromoteTempTask,
                        onClick       = onNavigateToTempPlan,
                        modifier      = Modifier.padding(horizontal = 4.dp),
                    )
                } else {
                    TempPlanEmptyCard(
                        onClick  = onNavigateToTempPlan,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            item { TodaySectionLabel(count = tasks.size) }

            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task    = task,
                    onClick = {
                        task.notionUrl?.takeIf { it.isNotBlank() }?.let { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                            }
                        }
                    },
                )
            }

            if (briefing == null && uiState !is HomeUiState.Error) {
                // Loading or Empty — keep the cards space visible with skeletons so
                // the page doesn't jump-shrink as content lands.
                items(2) { SkeletonCard() }
            }

            item { Footer() }
        }

        // Status-bar gradient: keeps system icons legible when content scrolls under.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBars.calculateTopPadding() + 16.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            morning.background,
                            morning.background.copy(alpha = 0.85f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        // Snackbar host overlay — anchors above the gesture nav bar.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBars.calculateBottomPadding() + 16.dp),
        )
    }
}

// --- v4 blocks ------------------------------------------------------------

@Composable
private fun TopBar(
    onSettingsClick: () -> Unit,
    onSavedClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 10dp accent dot with a 3dp accent-soft ring — the brand mark.
            Box(
                modifier         = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(morning.accentSoft),
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(morning.accent),
                )
            }
            Text(
                text  = stringResource(R.string.wordmark_agent),
                style = MorningType.Wordmark,
                color = morning.textPrimary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSavedClick) {
                Icon(
                    imageVector        = Icons.Outlined.BookmarkBorder,
                    contentDescription = stringResource(R.string.cd_saved_posts),
                    tint               = morning.textSecondary.copy(alpha = 0.8f),
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector        = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint               = morning.textSecondary.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun PendingSavesBanner(count: Int, onClick: () -> Unit) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(morning.accentSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text     = stringResource(R.string.home_pending_banner, count),
            style    = MorningType.BodyReadItalic.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color    = morning.accent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GreetingBlock(dateLine: String) {
    val morning = MaterialTheme.morning
    val copy    = slotCopy()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
    ) {
        Text(
            text  = dateLine,
            style = MorningType.DateLine,
            color = morning.textMuted,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text  = stringResource(copy.greeting),
            style = MorningType.GreetingDisplay,
            color = morning.textPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text  = stringResource(copy.sub),
            style = MorningType.BodyReadItalic,
            color = morning.textSecondary,
        )
    }
}

@Composable
private fun TodaySectionLabel(count: Int) {
    val morning = MaterialTheme.morning
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp),
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text  = stringResource(R.string.section_today),
            style = MorningType.SectionHeading,
            color = morning.textPrimary,
        )
        Text(
            text  = stringResource(R.string.label_n_items, count),
            style = MorningType.DateLine.copy(letterSpacing = 1.6.sp),
            color = morning.textMuted,
        )
    }
}

@Composable
private fun Footer() {
    val morning = MaterialTheme.morning
    Text(
        text      = stringResource(R.string.footer_powered_by_v4),
        style     = MorningType.MetaMono.copy(letterSpacing = 2.sp),
        color     = morning.textMuted,
        textAlign = TextAlign.Center,
        modifier  = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(morning.error.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = message,
            style    = MorningType.BodyReadItalic,
            color    = morning.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenSettings) {
            Text(
                text  = stringResource(R.string.action_open_settings),
                style = MorningType.ButtonLabel,
                color = morning.accent,
            )
        }
    }
}

@Composable
private fun SkeletonCard() {
    val morning = MaterialTheme.morning
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(morning.surface.copy(alpha = 0.7f)),
    )
}

// --- Previews -------------------------------------------------------------

@Preview(name = "Home – Success (Light)", showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun HomeSuccessPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState                 = HomeUiState.Success(PreviewData.sampleBriefing),
            nextRunLabel            = "tomorrow, 9:00",
            headerDateLine          = "FRIDAY · MAY 15 · 2:23 PM",
            activeTempPlan          = PreviewData.sampleTempPlan,
            onRunNow                = {},
            pendingSharedPosts      = 0,
            sharedPostsDbConfigured = true,
            onNavigateToSettings    = {},
            onNavigateToSavedPosts  = {},
            onNavigateToTempPlan    = {},
        )
    }
}

@Preview(name = "Home – Loading", showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun HomeLoadingPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState                 = HomeUiState.Loading(),
            nextRunLabel            = "tomorrow, 9:00",
            headerDateLine          = "FRIDAY · MAY 15 · 2:23 PM",
            activeTempPlan          = null,
            onRunNow                = {},
            pendingSharedPosts      = 0,
            sharedPostsDbConfigured = true,
            onNavigateToSettings    = {},
            onNavigateToSavedPosts  = {},
            onNavigateToTempPlan    = {},
        )
    }
}

@Preview(name = "Home – Empty", showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun HomeEmptyPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState                 = HomeUiState.Empty,
            nextRunLabel            = "tomorrow, 9:00",
            headerDateLine          = "FRIDAY · MAY 15 · 2:23 PM",
            activeTempPlan          = null,
            onRunNow                = {},
            pendingSharedPosts      = 0,
            sharedPostsDbConfigured = true,
            onNavigateToSettings    = {},
            onNavigateToSavedPosts  = {},
            onNavigateToTempPlan    = {},
        )
    }
}

@Preview(name = "Home – Error", showBackground = true, backgroundColor = 0xFFE5E1DD)
@Composable
private fun HomeErrorPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState                 = HomeUiState.Error("Couldn't reach Notion. Check your token in Settings."),
            nextRunLabel            = null,
            headerDateLine          = "FRIDAY · MAY 15 · 2:23 PM",
            activeTempPlan          = null,
            onRunNow                = {},
            pendingSharedPosts      = 0,
            sharedPostsDbConfigured = true,
            onNavigateToSettings    = {},
            onNavigateToSavedPosts  = {},
            onNavigateToTempPlan    = {},
        )
    }
}
