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
import com.luna.morningagent.data.agent.GeminiBriefingClient
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.secure.TokenStore
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
