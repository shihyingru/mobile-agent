package com.luna.morningagent.ui.home

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.R
import com.luna.morningagent.data.AgentConfigMissingException
import com.luna.morningagent.data.AgentNetworkException
import com.luna.morningagent.data.AgentRepository
import com.luna.morningagent.data.agent.BriefingGenerator
import com.luna.morningagent.data.agent.ClaudeBriefingClient
import com.luna.morningagent.data.agent.ClaudeModelOption
import com.luna.morningagent.data.agent.GeminiBriefingClient
import com.luna.morningagent.data.agent.GeminiModelOption
import com.luna.morningagent.data.agent.ProviderOption
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.ui.home.components.ModelChoice
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    // Selected model id for the active provider (e.g. "gemini-2.5-flash" or
    // "claude-sonnet-4-6"). The Home picker reads this and writes through
    // setModel(); the agent layer resolves it on each generate() call so the
    // next Run Now picks up today's choice.
    var selectedModelId by mutableStateOf(currentProviderModelId())
        private set

    // Picker option list for the active provider. Recomputed when the provider
    // flips in Settings — HomeScreen's refreshClock() seam catches the change.
    var modelOptions: List<ModelChoice> by mutableStateOf(buildModelOptions(activeProvider()))
        private set

    // Drives the picker's visibility on Home — no key for the active provider,
    // no picker. Read fresh each recomposition so it flips on after the user
    // saves a key in Settings.
    val isProviderConfigured: Boolean
        get() = !activeProviderKey().isNullOrEmpty()

    private fun activeProvider(): ProviderOption =
        ProviderOption.fromId(tokenStore.getSelectedProvider())

    private fun activeProviderKey(): String? = when (activeProvider()) {
        ProviderOption.Gemini -> tokenStore.getGeminiKey()
        ProviderOption.Claude -> tokenStore.getClaudeKey()
    }

    private fun currentProviderModelId(): String = when (activeProvider()) {
        ProviderOption.Gemini -> tokenStore.getGeminiModel()
        ProviderOption.Claude -> tokenStore.getClaudeModel()
    }

    private fun buildModelOptions(provider: ProviderOption): List<ModelChoice> =
        when (provider) {
            ProviderOption.Gemini -> GeminiModelOption.entries.map {
                ModelChoice(id = it.id, displayName = it.displayName, tagline = it.tagline)
            }
            ProviderOption.Claude -> ClaudeModelOption.entries.map {
                ModelChoice(id = it.id, displayName = it.displayName, tagline = it.tagline)
            }
        }

    // Header strings derived from the device clock. Backed by mutableStateOf + a
    // viewModelScope ticker so labels auto-flip across midnight or the picked
    // briefing time without waiting for a recomposition trigger. HomeScreen also
    // calls refreshClock() on entry so Settings-side time changes land instantly.

    @get:StringRes
    var greetingRes: Int by mutableStateOf(greetingResFor(LocalDateTime.now()))
        private set

    var headerSubtitle: String by mutableStateOf(formatHeaderSubtitle(LocalDate.now()))
        private set

    // "today 9:00 PM" / "tomorrow 7:30 AM" depending on the picked hour/minute and
    // whether that time has already passed today. Null when the daily-briefing
    // toggle is off, which hides the entire "Next run" row.
    var nextRunLabel: String? by mutableStateOf(computeNextRunLabel(LocalDateTime.now()))
        private set

    init {
        // Auto-run on first composition when the user has opted in AND keys are
        // configured. Otherwise stay Empty so they can read Settings or tap Run Now.
        if (tokenStore.getAutoRun() && hasMinimalConfig()) {
            runNow()
        }
        startClockTicker()
    }

    // Pulls fresh clock + provider values into the state-backed fields. Called by
    // the ticker every 30s for passive time-of-day progression, and by HomeScreen
    // on entry so changes made in Settings (briefing time, provider, model) land
    // immediately without waiting for the next tick.
    fun refreshClock() {
        val now = LocalDateTime.now()
        greetingRes    = greetingResFor(now)
        headerSubtitle = formatHeaderSubtitle(now.toLocalDate())
        nextRunLabel   = computeNextRunLabel(now)
        modelOptions   = buildModelOptions(activeProvider())
        selectedModelId = currentProviderModelId()
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

    fun setModel(id: String) {
        when (activeProvider()) {
            ProviderOption.Gemini -> tokenStore.saveGeminiModel(id)
            ProviderOption.Claude -> tokenStore.saveClaudeModel(id)
        }
        selectedModelId = id
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

private val HEADER_SUBTITLE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.ENGLISH)

internal fun formatHeaderSubtitle(today: LocalDate): String =
    today.format(HEADER_SUBTITLE_FORMAT)

// Picks the greeting string id from the device clock. Buckets per CLAUDE.md:
//   • Sat/Sun any hour          → weekend
//   • Mon–Fri 05:00–11:59       → morning
//   • Mon–Fri 12:00–17:59       → afternoon
//   • Mon–Fri 18:00–04:59       → evening
// Late-night absorbs into evening rather than spawning a fifth bucket — keeps the
// vibe palette tight and matches the four words Luna asked for.
@StringRes
internal fun greetingResFor(now: LocalDateTime): Int {
    val weekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
    if (weekend) return R.string.greeting_weekend
    return when (now.hour) {
        in 5..11  -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        else      -> R.string.greeting_evening
    }
}

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
