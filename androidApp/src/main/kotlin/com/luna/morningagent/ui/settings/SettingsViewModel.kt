package com.luna.morningagent.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.notion.NotionTaskSource
import com.luna.morningagent.data.secure.TokenStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface NotionTestResult {
    data object InProgress : NotionTestResult
    data class  Success(val taskCount: Int) : NotionTestResult
    data class  Failure(val message: String) : NotionTestResult
}

data class SettingsUiState(
    val geminiDraft: String      = "",
    val notionDraft: String      = "",
    val databaseDraft: String    = "",
    val geminiSavedLast4: String? = null,    // null = nothing saved yet
    val notionSavedLast4: String? = null,
    val savedDatabaseId: String  = "",       // shown in field as draft starting value
    val autoRun: Boolean         = true,     // toggled directly, not via Save
    val justSaved: Boolean       = false,
    val notionTest: NotionTestResult? = null,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TokenStore(application)
    private val notionClient: NotionTaskSource = NotionRestClient(store)

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        val savedDb = store.getNotionDatabaseId().orEmpty()
        uiState = uiState.copy(
            geminiSavedLast4 = store.getGeminiKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            notionSavedLast4 = store.getNotionToken()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            savedDatabaseId  = savedDb,
            databaseDraft    = savedDb,
            autoRun          = store.getAutoRun(),
        )
    }

    fun updateGeminiDraft(value: String) {
        uiState = uiState.copy(geminiDraft = value, justSaved = false)
    }

    fun updateNotionDraft(value: String) {
        uiState = uiState.copy(notionDraft = value, justSaved = false)
    }

    fun updateDatabaseDraft(value: String) {
        uiState = uiState.copy(databaseDraft = value, justSaved = false, notionTest = null)
    }

    // Auto-run is a UX preference, not a credential — persist on toggle so users
    // don't have to remember to tap Save after flipping it.
    fun setAutoRun(enabled: Boolean) {
        store.saveAutoRun(enabled)
        uiState = uiState.copy(autoRun = enabled)
    }

    // Reset all in-flight edits back to "what's persisted." Called when the user
    // leaves the Settings page so half-typed values don't reappear next visit.
    fun clearDrafts() {
        uiState = uiState.copy(
            geminiDraft   = "",
            notionDraft   = "",
            databaseDraft = uiState.savedDatabaseId,
            justSaved     = false,
            notionTest    = null,
        )
    }

    fun testNotionConnection() {
        uiState = uiState.copy(notionTest = NotionTestResult.InProgress)
        viewModelScope.launch {
            uiState = uiState.copy(
                notionTest = try {
                    val tasks = notionClient.fetchHighPriorityTasks()
                    NotionTestResult.Success(tasks.size)
                } catch (e: NotionConfigMissingException) {
                    NotionTestResult.Failure(e.message ?: "Missing config")
                } catch (e: Exception) {
                    NotionTestResult.Failure(e.message ?: e::class.simpleName ?: "Unknown error")
                },
            )
        }
    }

    fun save() {
        if (uiState.geminiDraft.isNotEmpty()) store.saveGeminiKey(uiState.geminiDraft)
        if (uiState.notionDraft.isNotEmpty()) store.saveNotionToken(uiState.notionDraft)

        val cleanedDb = extractNotionDatabaseId(uiState.databaseDraft)
        if (cleanedDb.isNotEmpty() && cleanedDb != uiState.savedDatabaseId) {
            store.saveNotionDatabaseId(cleanedDb)
        }

        uiState = SettingsUiState(
            geminiDraft      = "",
            notionDraft      = "",
            databaseDraft    = cleanedDb,
            geminiSavedLast4 = store.getGeminiKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            notionSavedLast4 = store.getNotionToken()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            savedDatabaseId  = cleanedDb,
            autoRun          = uiState.autoRun,
            justSaved        = true,
        )

        viewModelScope.launch {
            delay(2000)
            uiState = uiState.copy(justSaved = false)
        }
    }
}

// Extracts a 32-hex-char Notion database ID from a URL like
// https://www.notion.so/{workspace}/Title-abc123def456... (with or without dashes).
// Returns the input trimmed if no match — so a typo just round-trips and the API surfaces the error.
internal fun extractNotionDatabaseId(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    val cleaned = trimmed.lowercase().replace("-", "")
    return Regex("[a-f0-9]{32}").find(cleaned)?.value ?: trimmed
}