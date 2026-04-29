package com.luna.morningagent.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.PreviewData
import com.luna.morningagent.data.model.Briefing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class  Success(val briefing: Briefing) : HomeUiState
    data object Empty   : HomeUiState
    data class  Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {
    var uiState by mutableStateOf<HomeUiState>(HomeUiState.Success(PreviewData.sampleBriefing))
        private set

    fun runNow() {
        uiState = HomeUiState.Loading
        viewModelScope.launch {
            // Phase 1: simulate 1.5s agent run, then show the same mock data
            delay(1500)
            // TODO(Phase 2): replace with AgentRepository.runAgent()
            uiState = HomeUiState.Success(PreviewData.sampleBriefing)
        }
    }
}
