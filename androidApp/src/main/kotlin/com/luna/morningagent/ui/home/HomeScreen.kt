package com.luna.morningagent.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.ui.home.components.AgentStatusCard
import com.luna.morningagent.ui.home.components.BriefingCard
import com.luna.morningagent.ui.home.components.ModelPicker
import com.luna.morningagent.ui.home.components.SectionLabel
import com.luna.morningagent.ui.home.components.TaskCard
import com.luna.morningagent.ui.theme.MorningAgentTheme
import com.luna.morningagent.ui.theme.MorningType
import com.luna.morningagent.ui.theme.morning
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
) {
    // Re-sync clock labels on entry — covers the "user changed time in Settings
    // and came back" case faster than waiting for the next 30s tick.
    LaunchedEffect(Unit) { vm.refreshClock() }

    HomeScreenContent(
        uiState              = vm.uiState,
        selectedModelId      = vm.selectedModelId,
        isGeminiConfigured   = vm.isGeminiConfigured,
        nextRunLabel         = vm.nextRunLabel,
        greetingRes          = vm.greetingRes,
        headerSubtitle       = vm.headerSubtitle,
        onRunNow             = vm::runNow,
        onSelectModel        = vm::setModel,
        onNavigateToSettings = onNavigateToSettings,
        modifier             = modifier,
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    selectedModelId: String,
    isGeminiConfigured: Boolean,
    nextRunLabel: String?,
    @androidx.annotation.StringRes greetingRes: Int,
    headerSubtitle: String,
    onRunNow: () -> Unit,
    onSelectModel: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val morning = MaterialTheme.morning
    val isLoading = uiState is HomeUiState.Loading

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars = WindowInsets.navigationBars.asPaddingValues()
    val statusBarHeight = statusBars.calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(morning.background),
    ) {
        LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(
            start  = 24.dp,
            end    = 24.dp,
            top    = 32.dp + statusBarHeight,
            bottom = 32.dp + navBars.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // Header
        item {
            HomeHeader(
                greetingRes     = greetingRes,
                subtitle        = headerSubtitle,
                onSettingsClick = onNavigateToSettings,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Error banner
        if (uiState is HomeUiState.Error) {
            item {
                ErrorBanner(
                    message        = uiState.message,
                    onOpenSettings = onNavigateToSettings,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Agent status card
        item {
            AgentStatusCard(
                isLoading = isLoading,
                onRunNow = onRunNow,
                nextRunLabel = nextRunLabel,
            )
            // Retry hint: only after the first attempt has already failed and been
            // absorbed by the transient-error backoff in GeminiBriefingClient.
            val retry = (uiState as? HomeUiState.Loading)?.takeIf { it.attempt > 1 }
            if (retry != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text      = stringResource(
                        R.string.retrying_attempt,
                        retry.attempt,
                        retry.total,
                    ),
                    style     = MorningType.Label,
                    color     = morning.textMuted,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Model picker: hidden until the user saves a Gemini key in Settings.
        if (isGeminiConfigured) {
            item {
                ModelPicker(
                    selected = selectedModelId,
                    onSelect = onSelectModel,
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        when (uiState) {
            is HomeUiState.Loading, is HomeUiState.Success -> {
                val briefing = (uiState as? HomeUiState.Success)?.briefing

                // Briefing section
                item {
                    val generatedLabel = briefing?.let {
                        val t = it.generatedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                        "GENERATED %02d:%02d".format(t.hour, t.minute)
                    }
                    SectionLabel(
                        text     = stringResource(R.string.section_todays_briefing),
                        trailing = generatedLabel,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    if (briefing != null) {
                        BriefingCard(briefing = briefing)
                    } else {
                        SkeletonCard(height = 120)
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // Tasks section
                item {
                    SectionLabel(
                        text     = stringResource(R.string.section_tasks),
                        trailing = briefing?.let { "${it.tasks.size} ITEMS" },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (briefing != null) {
                    itemsIndexed(briefing.tasks) { _, task ->
                        TaskCard(task = task)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    items(3) {
                        SkeletonCard(height = 160)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            is HomeUiState.Empty -> {
                item {
                    EmptyState()
                }
            }

            is HomeUiState.Error -> {
                item {
                    EmptyState(message = stringResource(R.string.empty_enjoy))
                }
            }
        }

        // Footer
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text      = stringResource(R.string.footer_powered_by),
                style     = MorningType.Label,
                color     = morning.textMuted,
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 20.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

        // Gradient protection: keeps system bar icons legible while content scrolls under.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight + 16.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            morning.background,
                            morning.background.copy(alpha = 0.85f),
                            morning.background.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun HomeHeader(
    @androidx.annotation.StringRes greetingRes: Int,
    subtitle: String,
    onSettingsClick: () -> Unit,
) {
    val morning = MaterialTheme.morning
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Accent dot mirrors the orb's identity
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(morning.accent),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = stringResource(greetingRes),
                style = MorningType.Display,
                color = morning.textPrimary,
            )
            Text(
                text  = subtitle,
                style = MorningType.Mono,
                color = morning.textSecondary,
            )
        }
        IconButton(
            onClick  = onSettingsClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector        = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint               = morning.textSecondary,
            )
        }
    }
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
            .clip(RoundedCornerShape(12.dp))
            .background(morning.error.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = message,
            style    = MorningType.Body,
            color    = morning.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenSettings) {
            Text(
                text  = stringResource(R.string.action_open_settings),
                style = MorningType.Body,
                color = morning.accent,
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.empty_enjoy),
) {
    val morning = MaterialTheme.morning
    Column(
        modifier            = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(morning.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text  = stringResource(R.string.empty_high_priority),
            style = MorningType.Title,
            color = morning.textPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = message,
            style = MorningType.Body,
            color = morning.textSecondary,
        )
    }
}

@Composable
private fun SkeletonCard(height: Int, modifier: Modifier = Modifier) {
    val morning = MaterialTheme.morning
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(morning.surface),
    )
}

// --- Previews for all 4 UI states ---

@Preview(name = "Home – Success", showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun HomeSuccessPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState              = HomeUiState.Success(PreviewData.sampleBriefing),
            selectedModelId      = "gemini-2.5-flash",
            isGeminiConfigured   = true,
            nextRunLabel         = "tomorrow 9:00 AM",
            greetingRes          = R.string.greeting_morning,
            headerSubtitle       = "Tuesday · April 28",
            onRunNow             = {},
            onSelectModel        = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(name = "Home – Loading", showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun HomeLoadingPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState              = HomeUiState.Loading(),
            selectedModelId      = "gemini-2.5-flash",
            isGeminiConfigured   = true,
            nextRunLabel         = "tomorrow 9:00 AM",
            greetingRes          = R.string.greeting_morning,
            headerSubtitle       = "Tuesday · April 28",
            onRunNow             = {},
            onSelectModel        = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(name = "Home – Empty", showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun HomeEmptyPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState              = HomeUiState.Empty,
            selectedModelId      = "gemini-2.5-flash",
            isGeminiConfigured   = true,
            nextRunLabel         = "tomorrow 9:00 AM",
            greetingRes          = R.string.greeting_morning,
            headerSubtitle       = "Tuesday · April 28",
            onRunNow             = {},
            onSelectModel        = {},
            onNavigateToSettings = {},
        )
    }
}

@Preview(name = "Home – Error", showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
private fun HomeErrorPreview() {
    MorningAgentTheme {
        HomeScreenContent(
            uiState              = HomeUiState.Error("Couldn't reach Notion. Check your token in Settings."),
            selectedModelId      = "gemini-2.5-flash",
            isGeminiConfigured   = true,
            nextRunLabel         = "tomorrow 9:00 AM",
            greetingRes          = R.string.greeting_morning,
            headerSubtitle       = "Tuesday · April 28",
            onRunNow             = {},
            onSelectModel        = {},
            onNavigateToSettings = {},
        )
    }
}
