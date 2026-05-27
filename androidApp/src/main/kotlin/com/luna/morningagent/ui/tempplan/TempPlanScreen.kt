package com.luna.morningagent.ui.tempplan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.ui.tempplan.components.TempPlanDetail
import com.luna.morningagent.ui.tempplan.components.TempPlanForm
import com.luna.morningagent.ui.tempplan.components.TempPlanHeader
import com.luna.morningagent.ui.theme.morning

@Composable
fun TempPlanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: TempPlanViewModel = viewModel(),
) {
    BackHandler(onBack = onBack)

    val morning = MaterialTheme.morning
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.snackbarMessage) {
        vm.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.snackbarShown()
        }
    }

    val statusBars = WindowInsets.statusBars.asPaddingValues()
    val navBars    = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(morning.background),
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start  = 18.dp,
                end    = 18.dp,
                top    = 22.dp + statusBars.calculateTopPadding(),
                bottom = 32.dp + navBars.calculateBottomPadding(),
            ),
        ) {
            item {
                TempPlanHeader(onBack = onBack)
            }

            when (val state = vm.uiState) {
                is TempPlanUiState.Empty -> item {
                    TempPlanForm(
                        state         = TempPlanUiState.Creating(),
                        onUpdateName  = vm::updateName,
                        onUpdateStart = vm::updateStartDate,
                        onUpdateEnd   = vm::updateEndDate,
                        onCreate      = vm::createPlan,
                        onStartCreating = vm::startCreating,
                        isEmpty       = true,
                    )
                }
                is TempPlanUiState.Creating -> item {
                    TempPlanForm(
                        state         = state,
                        onUpdateName  = vm::updateName,
                        onUpdateStart = vm::updateStartDate,
                        onUpdateEnd   = vm::updateEndDate,
                        onCreate      = vm::createPlan,
                        onStartCreating = {},
                        isEmpty       = false,
                    )
                }
                is TempPlanUiState.Viewing -> item {
                    TempPlanDetail(
                        plan              = state.plan,
                        taskDraft         = vm.taskDraft,
                        onTaskDraftChange = { vm.taskDraft = it },
                        onAddTask         = { vm.addTask() },
                        onToggleTask      = vm::toggleTask,
                        onRemoveTask      = vm::removeTask,
                        onPromoteTask     = vm::promoteTask,
                        onUpdateStartDate = vm::updatePlanStartDate,
                        onUpdateEndDate   = vm::updatePlanEndDate,
                        onDeletePlan      = vm::deletePlan,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBars.calculateBottomPadding() + 16.dp),
        )
    }
}
