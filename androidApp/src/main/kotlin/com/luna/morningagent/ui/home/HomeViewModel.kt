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
import com.luna.morningagent.data.agent.GeminiBriefingClient
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.secure.TokenStore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
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
    private val repo = AgentRepository(
        notionTaskSource  = NotionRestClient(tokenStore),
        briefingGenerator = GeminiBriefingClient(tokenStore),
    )

    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Empty)
        private set

    // Selected Gemini model id (e.g. "gemini-2.5-flash"). The Home picker reads
    // this and writes through setModel(); the agent layer resolves it on each
    // generate() call so the next Run Now picks up the new choice.
    var selectedModelId by mutableStateOf(tokenStore.getGeminiModel())
        private set

    // Drives the picker's visibility on Home — no key, no picker. Read fresh each
    // recomposition so it flips on after the user saves a key in Settings.
    val isGeminiConfigured: Boolean
        get() = !tokenStore.getGeminiKey().isNullOrEmpty()

    // Header greeting, resolved from the device clock. Weekend wins over hour-of-day
    // so Saturday morning reads "Slow day, Luna" instead of "Morning, Luna". Read
    // fresh per recomposition so the greeting evolves through the day without a
    // restart — cheap, and matches how Settings-side time changes propagate.
    @get:StringRes
    val greetingRes: Int
        get() = greetingResFor(LocalDateTime.now())

    // Subtitle under the greeting: "Monday · May 11". Forced to ENGLISH so the
    // app's tone stays consistent even on a Chinese-locale phone, matching the
    // English greeting strings above.
    val headerSubtitle: String
        get() = formatHeaderSubtitle(LocalDate.now())

    // "today 9:00 PM" / "tomorrow 7:30 AM" depending on the picked hour/minute and
    // whether that time has already passed today. Null when the daily-briefing
    // toggle is off, which hides the entire "Next run" row. Recomputed on each
    // recomposition so changes in Settings reflect immediately on navigation back.
    val nextRunLabel: String?
        get() {
            if (!tokenStore.getDailyBriefingEnabled()) return null
            return formatNextRunLabel(
                now    = LocalDateTime.now(),
                hour   = tokenStore.getDailyBriefingHour(),
                minute = tokenStore.getDailyBriefingMinute(),
            )
        }

    init {
        // Auto-run on first composition when the user has opted in AND keys are
        // configured. Otherwise stay Empty so they can read Settings or tap Run Now.
        if (tokenStore.getAutoRun() && hasMinimalConfig()) {
            runNow()
        }
    }

    fun setModel(id: String) {
        tokenStore.saveGeminiModel(id)
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
        !tokenStore.getGeminiKey().isNullOrEmpty() &&
        !tokenStore.getNotionToken().isNullOrEmpty() &&
        !tokenStore.getNotionDatabaseId().isNullOrEmpty()
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
