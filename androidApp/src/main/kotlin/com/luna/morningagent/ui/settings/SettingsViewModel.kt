package com.luna.morningagent.ui.settings

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.agent.ProviderOption
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.notion.NotionTaskSource
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.CategoryDefinition
import com.luna.morningagent.data.sharedposts.SharedPostsRepository
import com.luna.morningagent.worker.BriefingScheduler
import com.luna.morningagent.worker.MorningAgentWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface NotionTestResult {
    data object InProgress : NotionTestResult
    data class  Success(val taskCount: Int) : NotionTestResult
    data class  Failure(val message: String) : NotionTestResult
}

data class SettingsUiState(
    val selectedProvider: ProviderOption = ProviderOption.Default,
    // Selected model id for the active provider — derived from TokenStore on init
    // and updated by setSelectedModel(). Each provider's model preference is
    // stored independently in TokenStore so flipping back doesn't lose state.
    val selectedModelId: String  = "",
    val geminiDraft: String      = "",
    val claudeDraft: String      = "",
    val notionDraft: String      = "",
    val databaseDraft: String    = "",
    val geminiSavedLast4: String? = null,    // null = nothing saved yet
    val claudeSavedLast4: String? = null,
    val notionSavedLast4: String? = null,
    val savedDatabaseId: String  = "",       // shown in field as draft starting value
    val autoRun: Boolean         = true,     // toggled directly, not via Save
    val dailyBriefing: Boolean   = false,    // schedules WorkManager when on
    val briefingHour: Int        = 9,
    val briefingMinute: Int      = 0,
    val justSaved: Boolean       = false,
    val notionTest: NotionTestResult? = null,
    val categories: List<CategoryDefinition> = emptyList(),
    val postCountsByCategory: Map<String, Int> = emptyMap(),
)

/** Hard limits enforced by both UI and prompt rule — keep them in sync. */
const val CATEGORY_NAME_MIN_CHARS = 3
const val CATEGORY_NAME_MAX_CHARS = 15
/** Category name that can never be removed — categorizer falls back here. */
const val SEED_CATEGORY_NAME = "Misc"

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = TokenStore(application)
    private val notionClient: NotionTaskSource = NotionRestClient(store)
    private val sharedPostsRepo = SharedPostsRepository(store)

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        val savedDb  = store.getNotionDatabaseId().orEmpty()
        val provider = ProviderOption.fromId(store.getSelectedProvider())
        uiState = uiState.copy(
            selectedProvider = provider,
            selectedModelId  = modelIdFor(provider),
            geminiSavedLast4 = store.getGeminiKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            claudeSavedLast4 = store.getClaudeKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            notionSavedLast4 = store.getNotionToken()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            savedDatabaseId  = savedDb,
            databaseDraft    = savedDb,
            autoRun          = store.getAutoRun(),
            dailyBriefing    = store.getDailyBriefingEnabled(),
            briefingHour     = store.getDailyBriefingHour(),
            briefingMinute   = store.getDailyBriefingMinute(),
        )
        refreshCategories()
    }

    private fun refreshCategories() {
        val cats   = store.getSharedPostsCategories()
        val counts = sharedPostsRepo.listAll()
            .flatMap { it.categories }
            .groupingBy { it }
            .eachCount()
        uiState = uiState.copy(categories = cats, postCountsByCategory = counts)
    }

    private fun modelIdFor(provider: ProviderOption): String = when (provider) {
        ProviderOption.Gemini -> store.getGeminiModel()
        ProviderOption.Claude -> store.getClaudeModel()
    }

    fun updateGeminiDraft(value: String) {
        uiState = uiState.copy(geminiDraft = value, justSaved = false)
    }

    fun updateClaudeDraft(value: String) {
        uiState = uiState.copy(claudeDraft = value, justSaved = false)
    }

    // Provider is a one-time setup choice, not a daily decision — persist on
    // toggle so the user doesn't have to remember to tap Save. The other
    // provider's key + model preference stay intact so flipping back later
    // doesn't lose state.
    fun setProvider(option: ProviderOption) {
        store.saveSelectedProvider(option.id)
        uiState = uiState.copy(
            selectedProvider = option,
            // Switch the visible model id to whatever was last picked for the new
            // provider so the model chips reflect the right selection on switch.
            selectedModelId  = modelIdFor(option),
            justSaved        = false,
        )
    }

    // Per-provider model choice (gemini-2.5-flash, claude-sonnet-4-6, …).
    // Persisted immediately so the next agent run picks it up — no Save tap.
    fun setSelectedModel(modelId: String) {
        when (uiState.selectedProvider) {
            ProviderOption.Gemini -> store.saveGeminiModel(modelId)
            ProviderOption.Claude -> store.saveClaudeModel(modelId)
        }
        uiState = uiState.copy(selectedModelId = modelId)
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

    // Toggles the WorkManager schedule. Application-scoped context dodges the
    // activity-lifecycle leak warning when the ViewModel outlives a config change.
    fun setDailyBriefing(enabled: Boolean) {
        store.saveDailyBriefingEnabled(enabled)
        uiState = uiState.copy(dailyBriefing = enabled)
        val appContext = getApplication<Application>().applicationContext
        if (enabled) BriefingScheduler.replace(appContext)
        else BriefingScheduler.disable(appContext)
    }

    // Persists immediately + re-enqueues with the new initialDelay. Cheap to
    // call from the time picker's confirm callback.
    fun setBriefingTime(hour: Int, minute: Int) {
        store.saveDailyBriefingTime(hour, minute)
        uiState = uiState.copy(briefingHour = hour, briefingMinute = minute)
        if (uiState.dailyBriefing) {
            BriefingScheduler.replace(getApplication<Application>().applicationContext)
        }
    }

    // Posts a stub notification with the channel/icon/layout the worker uses.
    // Lets the user preview the visual + confirm POST_NOTIFICATIONS without
    // waiting until the picked time or burning a Gemini call.
    fun sendTestNotification() {
        MorningAgentWorker.postSampleNotification(
            getApplication<Application>().applicationContext,
        )
    }

    // Reset all in-flight edits back to "what's persisted." Called when the user
    // leaves the Settings page so half-typed values don't reappear next visit.
    fun clearDrafts() {
        uiState = uiState.copy(
            geminiDraft   = "",
            claudeDraft   = "",
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

    // --- Saved post categories ----------------------------------------------

    fun addCategory(name: String, keywords: List<String>) {
        viewModelScope.launch {
            sharedPostsRepo.addCategory(name, keywords)
            refreshCategories()
        }
    }

    fun updateCategory(oldName: String, newName: String, keywords: List<String>) {
        viewModelScope.launch {
            sharedPostsRepo.updateCategory(oldName, newName, keywords)
            refreshCategories()
        }
    }

    fun removeCategory(name: String) {
        viewModelScope.launch {
            sharedPostsRepo.removeCategory(name)
            refreshCategories()
        }
    }

    fun save() {
        if (uiState.geminiDraft.isNotEmpty()) store.saveGeminiKey(uiState.geminiDraft)
        if (uiState.claudeDraft.isNotEmpty()) store.saveClaudeKey(uiState.claudeDraft)
        if (uiState.notionDraft.isNotEmpty()) store.saveNotionToken(uiState.notionDraft)

        val cleanedDb = extractNotionDatabaseId(uiState.databaseDraft)
        if (cleanedDb.isNotEmpty() && cleanedDb != uiState.savedDatabaseId) {
            store.saveNotionDatabaseId(cleanedDb)
        }

        // Rebuild state from store so all the last-4 masks refresh, but carry
        // the toggle-style fields (provider, model, autoRun, daily briefing)
        // forward explicitly — they were never tied to Save in the first place.
        uiState = SettingsUiState(
            selectedProvider = uiState.selectedProvider,
            selectedModelId  = uiState.selectedModelId,
            geminiDraft      = "",
            claudeDraft      = "",
            notionDraft      = "",
            databaseDraft    = cleanedDb,
            geminiSavedLast4 = store.getGeminiKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            claudeSavedLast4 = store.getClaudeKey()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            notionSavedLast4 = store.getNotionToken()?.takeLast(4)?.takeIf { it.isNotEmpty() },
            savedDatabaseId  = cleanedDb,
            autoRun          = uiState.autoRun,
            dailyBriefing    = uiState.dailyBriefing,
            briefingHour     = uiState.briefingHour,
            briefingMinute   = uiState.briefingMinute,
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