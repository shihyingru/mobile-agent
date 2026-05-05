package com.luna.morningagent.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.secure.TokenStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SettingsUiState(
    val geminiDraft: String     = "",
    val notionDraft: String     = "",
    val geminiSavedLast4: String? = null,  // null = nothing saved yet
    val notionSavedLast4: String? = null,
    val justSaved: Boolean      = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TokenStore(application)

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        uiState = uiState.copy(
            geminiSavedLast4 = store.getGeminiKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            notionSavedLast4 = store.getNotionToken()?.takeLast(4)?.takeIf { it.isNotEmpty() },
        )
    }

    fun updateGeminiDraft(value: String) {
        uiState = uiState.copy(geminiDraft = value, justSaved = false)
    }

    fun updateNotionDraft(value: String) {
        uiState = uiState.copy(notionDraft = value, justSaved = false)
    }

    fun save() {
        if (uiState.geminiDraft.isNotEmpty()) store.saveGeminiKey(uiState.geminiDraft)
        if (uiState.notionDraft.isNotEmpty()) store.saveNotionToken(uiState.notionDraft)

        uiState = SettingsUiState(
            geminiDraft      = "",
            notionDraft      = "",
            geminiSavedLast4 = store.getGeminiKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            notionSavedLast4 = store.getNotionToken()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            justSaved        = true,
        )

        viewModelScope.launch {
            delay(2000)
            uiState = uiState.copy(justSaved = false)
        }
    }
}
