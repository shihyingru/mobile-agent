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
import kotlinx.coroutines.launch

sealed interface TempPlanUiState {
    data object Empty : TempPlanUiState
    data class Creating(
        val name: String = "",
        val startDate: String = "",
        val endDate: String = "",
    ) : TempPlanUiState
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

    fun startCreating() {
        uiState = TempPlanUiState.Creating()
    }

    fun updateName(name: String) {
        val s = uiState as? TempPlanUiState.Creating ?: return
        uiState = s.copy(name = name)
    }

    fun updateStartDate(date: String) {
        val s = uiState as? TempPlanUiState.Creating ?: return
        uiState = s.copy(startDate = date)
    }

    fun updateEndDate(date: String) {
        val s = uiState as? TempPlanUiState.Creating ?: return
        uiState = s.copy(endDate = date)
    }

    fun createPlan() {
        val s = uiState as? TempPlanUiState.Creating ?: return
        if (s.name.isBlank() || s.startDate.isBlank() || s.endDate.isBlank()) return
        val plan = repo.createPlan(s.name.trim(), s.startDate, s.endDate)
        uiState = TempPlanUiState.Viewing(plan)
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
