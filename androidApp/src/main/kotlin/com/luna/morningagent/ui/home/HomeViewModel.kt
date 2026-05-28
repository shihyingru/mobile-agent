package com.luna.morningagent.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.AgentConfigMissingException
import com.luna.morningagent.data.AgentNetworkException
import com.luna.morningagent.data.AgentRepository
import com.luna.morningagent.data.agent.BriefingGenerator
import com.luna.morningagent.data.agent.ClaudeBriefingClient
import com.luna.morningagent.data.agent.GeminiBriefingClient
import com.luna.morningagent.data.agent.ProviderOption
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.notion.NotionRestMutator
import com.luna.morningagent.data.notion.NotionTaskMutator
import com.luna.morningagent.R
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.ui.common.SnackbarEvent
import com.luna.morningagent.data.sharedposts.SharedPostsRepository
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.data.tempplan.TempPlanRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Clock
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

sealed interface HomeUiState {
    // attempt/total surface the retry hint in the UI: when attempt > 1 we show
    // "Retrying… (attempt/total)" so the user knows the spinner isn't stuck.
    data class  Loading(val attempt: Int = 1, val total: Int = 1) : HomeUiState
    data class  Success(val briefing: Briefing) : HomeUiState
    data object Empty   : HomeUiState
    data class  Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = TokenStore(application)

    // Build the repo per Run Now based on the active provider — switching providers
    // in Settings doesn't require restarting the activity. Cheap; the heavy work is
    // inside the LLM client's executor, which is constructed per generate() anyway.
    private val repo: AgentRepository
        get() = AgentRepository(
            notionTaskSource  = NotionRestClient(tokenStore),
            briefingGenerator = activeBriefingGenerator(),
            tokenStore        = tokenStore,
        )

    private fun activeBriefingGenerator(): BriefingGenerator =
        when (ProviderOption.fromId(tokenStore.getSelectedProvider())) {
            ProviderOption.Gemini -> GeminiBriefingClient(tokenStore)
            ProviderOption.Claude -> ClaudeBriefingClient(tokenStore)
        }

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Empty)
        private set

    private fun activeProvider(): ProviderOption =
        ProviderOption.fromId(tokenStore.getSelectedProvider())

    private fun activeProviderKey(): String? = when (activeProvider()) {
        ProviderOption.Gemini -> tokenStore.getGeminiKey()
        ProviderOption.Claude -> tokenStore.getClaudeKey()
    }

    // Header strings derived from the device clock. Backed by mutableStateOf + a
    // viewModelScope ticker so labels auto-flip across minute boundaries without
    // waiting for a recomposition trigger. HomeScreen also calls refreshClock()
    // on entry so Settings-side time changes land instantly. Greeting copy is
    // now slot-driven (slotCopy()) — the VM no longer chooses string IDs.

    var headerDateLine: String by mutableStateOf(formatDateLine(LocalDateTime.now()))
        private set

    // "today 9:00 PM" / "tomorrow 7:30 AM" depending on the picked hour/minute and
    // whether that time has already passed today. Null when the daily-briefing
    // toggle is off, which hides the entire "Next run" row.
    var nextRunLabel: String? by mutableStateOf(computeNextRunLabel(LocalDateTime.now()))
        private set

    // Shared-posts banner state. Counts posts cached locally but not yet
    // mirrored to Notion. Refreshed on entry + on the ticker so the banner
    // appears / disappears as Luna adds saves or finishes setup.
    private val sharedPostsRepo = SharedPostsRepository(tokenStore)

    var pendingSharedPostsCount: Int by mutableStateOf(0)
        private set

    var sharedPostsDbConfigured: Boolean by mutableStateOf(tokenStore.getSharedPostsDbId() != null)
        private set

    private val tempPlanRepo = TempPlanRepository(tokenStore)

    var activeTempPlan: TempPlan? by mutableStateOf(null)
        private set

    // Transient snackbar message — set by applyAction/dismissAction, consumed
    // by HomeScreen's SnackbarHost via a LaunchedEffect. The screen calls
    // snackbarShown() once the message is displayed so a repeat of the same
    // message can fire again.
    var snackbarEvent: SnackbarEvent? by mutableStateOf(null)
        private set

    fun snackbarShown() {
        snackbarEvent = null
    }

    init {
        // Hydrate from the on-disk cache first so a cold launch shows the last
        // briefing the worker (or a previous Run Now) wrote, not an Empty state.
        // If that cached briefing is from today, skip the auto-run — there's no
        // point burning another Gemini call to regenerate the same data.
        val cached = repo.getLastBriefing()
        val cachedIsFromToday = cached?.let { isFromToday(it) } == true
        if (cached != null) {
            uiState = HomeUiState.Success(cached)
        }

        // Auto-run on first composition only when the user has opted in AND keys
        // are configured AND the cache is stale (older than today, or absent).
        tempPlanRepo.archiveExpiredPlans()
        activeTempPlan = tempPlanRepo.getActivePlan()

        if (!cachedIsFromToday && tokenStore.getAutoRun() && hasMinimalConfig()) {
            runNow()
        }
        startClockTicker()
    }

    private fun isFromToday(briefing: Briefing): Boolean {
        val tz = TimeZone.currentSystemDefault()
        val briefingDate = briefing.generatedAt.toLocalDateTime(tz).date
        val today = Clock.System.now().toLocalDateTime(tz).date
        return briefingDate == today
    }

    // Pulls fresh clock + provider values into the state-backed fields. Called by
    // the ticker every 30s for passive time-of-day progression, and by HomeScreen
    // on entry so changes made in Settings (briefing time, provider, model) land
    // immediately without waiting for the next tick.
    fun refreshClock() {
        val now = LocalDateTime.now()
        headerDateLine          = formatDateLine(now)
        nextRunLabel            = computeNextRunLabel(now)
        pendingSharedPostsCount = sharedPostsRepo.listAll().count { it.pendingSync }
        sharedPostsDbConfigured = tokenStore.getSharedPostsDbId() != null
        activeTempPlan          = tempPlanRepo.getActivePlan()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(TICK_INTERVAL_MS)
                refreshClock()
            }
        }
    }

    private fun computeNextRunLabel(now: LocalDateTime): String? {
        if (!tokenStore.getDailyBriefingEnabled()) return null
        return formatNextRunLabel(
            now    = now,
            hour   = tokenStore.getDailyBriefingHour(),
            minute = tokenStore.getDailyBriefingMinute(),
        )
    }

    fun runNow() {
        uiState = HomeUiState.Loading()
        viewModelScope.launch {
            uiState = try {
                HomeUiState.Success(repo.runAgent { current, total ->
                    uiState = HomeUiState.Loading(current, total)
                })
            } catch (e: AgentConfigMissingException) {
                HomeUiState.Error(e.message ?: "Missing configuration")
            } catch (e: AgentNetworkException) {
                HomeUiState.Error(e.message ?: "Network error")
            } catch (e: Exception) {
                HomeUiState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    // Dismissal updates the cached briefing's dismissedActionIds and re-renders
    // the chip as gone. Persisted via repo.saveDismissal so the dismissal
    // survives process death until the next runAgent() writes a fresh briefing.
    fun dismissAction(action: ProposedAction) {
        val current = (uiState as? HomeUiState.Success)?.briefing ?: return
        val key = action.stableKey()
        if (key in current.dismissedActionIds) return
        uiState = HomeUiState.Success(
            current.copy(dismissedActionIds = current.dismissedActionIds + key)
        )
        repo.saveDismissal(key)
    }

    // Apply mutates Notion, then refetches. Re-validates the taskId against the
    // current briefing as belt-and-suspenders on top of AgentRepository's
    // pre-filter. Snackbar reports success / failure; on failure the previous
    // Success state is restored so the chip remains visible for retry.
    fun applyAction(action: ProposedAction) {
        val prevState = uiState as? HomeUiState.Success ?: return
        val taskIds = prevState.briefing.tasks.mapTo(mutableSetOf()) { it.id }
        if (action.taskId !in taskIds) {
            snackbarEvent = SnackbarEvent.ResId(R.string.snackbar_task_not_in_briefing)
            return
        }
        uiState = HomeUiState.Loading()
        viewModelScope.launch {
            try {
                val mutator: NotionTaskMutator = NotionRestMutator(tokenStore)
                when (action) {
                    is ProposedAction.MarkDone        -> mutator.markDone(action.taskId)
                    is ProposedAction.Reschedule     -> mutator.reschedule(action.taskId, action.newDate)
                    is ProposedAction.ChangePriority -> mutator.changePriority(action.taskId, action.newPriority)
                }
                // Persist the dismissal so the chip doesn't pop back if runNow
                // races slower than the model re-suggesting the same thing.
                repo.saveDismissal(action.stableKey())
                snackbarEvent = SnackbarEvent.ResId(R.string.snackbar_applied)
                runNow()
            } catch (e: Exception) {
                uiState = prevState
                snackbarEvent = if (e.message != null) SnackbarEvent.Plain(e.message!!)
                    else SnackbarEvent.ResId(R.string.snackbar_apply_failed)
            }
        }
    }

    fun toggleTempTask(taskId: String) {
        val plan = activeTempPlan ?: return
        tempPlanRepo.toggleTask(plan.id, taskId)
        activeTempPlan = tempPlanRepo.getActivePlan()
    }

    fun promoteTempTask(taskId: String) {
        val plan = activeTempPlan ?: return
        viewModelScope.launch {
            try {
                tempPlanRepo.promoteTask(plan.id, taskId)
                activeTempPlan = tempPlanRepo.getActivePlan()
                snackbarMessage = "Promoted to Notion"
            } catch (e: Exception) {
                snackbarMessage = e.message ?: "Couldn't promote task"
            }
        }
    }

    private fun hasMinimalConfig(): Boolean =
        !activeProviderKey().isNullOrEmpty() &&
        !tokenStore.getNotionToken().isNullOrEmpty() &&
        !tokenStore.getNotionDatabaseId().isNullOrEmpty()

    private companion object {
        // 30s strikes the balance between minute-precision labels and battery —
        // the labels only change at minute boundaries anyway, so 30s catches
        // every boundary within half a tick of when it happens.
        const val TICK_INTERVAL_MS = 30_000L
    }
}

// v4 date-line format: "FRIDAY · MAY 15 · 2:23 PM" — uppercase day, abbreviated
// month, day, 12-hour time + AM/PM. Locale.ENGLISH keeps the wording stable on
// zh-locale phones; uppercase happens in Kotlin (Locale-aware via the formatter).
private val DATE_LINE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE · MMM d · h:mm a", Locale.ENGLISH)

internal fun formatDateLine(now: LocalDateTime): String =
    now.format(DATE_LINE_FORMAT).uppercase(Locale.ENGLISH)

// Formats a 12-hour clock + AM/PM with a `today`/`tomorrow` prefix. Splits today
// vs tomorrow on equality so a target exactly at `now` still reads "today" — the
// user just picked it and the immediate next firing is still today.
internal fun formatNextRunLabel(now: LocalDateTime, hour: Int, minute: Int): String {
    val target = now.toLocalDate().atTime(hour, minute)
    val day    = if (now.isAfter(target)) "tomorrow" else "today"
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when {
        hour == 0  -> 12
        hour > 12  -> hour - 12
        else       -> hour
    }
    return "%s %d:%02d %s".format(day, h12, minute, period)
}
