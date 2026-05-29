package com.luna.morningagent.ui.tempplan

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.data.tempplan.TempPlanRepository
import java.time.LocalDate
import kotlinx.coroutines.launch

// Two states only — design's modification page auto-creates an "Untitled plan"
// when the user lands with none active, so there's no separate creation form.
// Empty is a transient state that the screen flips to Viewing via createDefault().
sealed interface TempPlanUiState {
    data object Empty : TempPlanUiState
    data class Viewing(val plan: TempPlan) : TempPlanUiState
}

class TempPlanViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = TokenStore(application)
    private val repo = TempPlanRepository(tokenStore)

    var uiState by mutableStateOf<TempPlanUiState>(TempPlanUiState.Empty)
        private set

    var snackbarMessage: String? by mutableStateOf(null)
        private set

    fun snackbarShown() { snackbarMessage = null }

    var taskDraft: String by mutableStateOf("")

    init {
        val active = repo.getActivePlan()
        uiState = if (active != null) TempPlanUiState.Viewing(active) else TempPlanUiState.Empty
    }

    // Called by TempPlanScreen when entered with no active plan — design §5.5:
    // default name "Untitled plan", window [today, today + 6 days]. The user
    // renames via the inline edit pencil after the page lands.
    fun createDefaultPlan(defaultName: String) {
        if (uiState !is TempPlanUiState.Empty) return
        val today = LocalDate.now()
        val plan = repo.createPlan(
            name      = defaultName,
            startDate = today.toString(),
            endDate   = today.plusDays(6).toString(),
        )
        uiState = TempPlanUiState.Viewing(plan)
    }

    fun renamePlan(name: String) {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        val trimmed = name.trim().ifBlank { return }
        repo.renamePlan(plan.id, trimmed)
        refresh()
    }

    fun addTask(dayIndex: Int? = null) {
        val title = taskDraft.trim()
        if (title.isEmpty()) return
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        repo.addTask(plan.id, title, dayIndex)
        taskDraft = ""
        refresh()
    }

    fun toggleTask(taskId: String) {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        repo.toggleTask(plan.id, taskId)
        refresh()
    }

    fun removeTask(taskId: String) {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        repo.removeTask(plan.id, taskId)
        refresh()
    }

    fun promoteTask(taskId: String) {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        viewModelScope.launch {
            try {
                repo.promoteTask(plan.id, taskId)
                refresh()
                snackbarMessage = "Promoted to Notion"
            } catch (e: Exception) {
                snackbarMessage = e.message ?: "Couldn't promote task"
            }
        }
    }

    fun updatePlanStartDate(date: String) {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        repo.updatePlanDates(plan.id, startDate = date, endDate = plan.endDate)
        refresh()
    }

    fun updatePlanEndDate(date: String) {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        repo.updatePlanDates(plan.id, startDate = plan.startDate, endDate = date)
        refresh()
    }

    fun deletePlan() {
        val plan = (uiState as? TempPlanUiState.Viewing)?.plan ?: return
        repo.deletePlan(plan.id)
        uiState = TempPlanUiState.Empty
    }

    private fun refresh() {
        val active = repo.getActivePlan()
        uiState = if (active != null) TempPlanUiState.Viewing(active) else TempPlanUiState.Empty
    }
}
