package com.luna.morningagent.ui.tempplan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.morningagent.R
import com.luna.morningagent.ui.tempplan.components.TempPlanDetail
import com.luna.morningagent.ui.tempplan.components.TempPlanHeader
import com.luna.morningagent.ui.theme.morning

// Plan modification page — auto-creates an "Untitled plan" with a [today,
// today+6d] window when the user arrives with no active plan, per design §5.5.
// Once a plan exists, the page is always in the modification view.
@Composable
fun TempPlanScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    vm: TempPlanViewModel = viewModel(),
) {
    BackHandler(onBack = onBack)
    val morning = MaterialTheme.morning

    val defaultName = stringResource(R.string.temp_plan_untitled)
    LaunchedEffect(vm.uiState) {
        if (vm.uiState is TempPlanUiState.Empty) {
            vm.createDefaultPlan(defaultName)
        }
    }

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
        // Paper radial wash at top — mirrors HomeScreen for visual continuity.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors  = listOf(morning.backgroundWash, morning.background),
                        center  = Offset(0.5f, 0f),
                        radius  = 1800f,
                    ),
                ),
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start  = 18.dp,
                        end    = 18.dp,
                        top    = 22.dp + statusBars.calculateTopPadding(),
                        bottom = 40.dp + navBars.calculateBottomPadding(),
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            TempPlanHeader(onBack = onBack)

            val state = vm.uiState
            if (state is TempPlanUiState.Viewing) {
                TempPlanDetail(
                    plan              = state.plan,
                    taskDraft         = vm.taskDraft,
                    onTaskDraftChange = { vm.taskDraft = it },
                    onAddTask         = { vm.addTask() },
                    onToggleTask      = vm::toggleTask,
                    onRemoveTask      = vm::removeTask,
                    onRenamePlan      = vm::renamePlan,
                    onUpdateStartDate = vm::updatePlanStartDate,
                    onUpdateEndDate   = vm::updatePlanEndDate,
                    onDeletePlan      = {
                        vm.deletePlan()
                        onBack()
                    },
                )
            } else {
                // Transient Empty — the LaunchedEffect above flips us to Viewing
                // on the next composition. Empty filler keeps layout stable.
                Box(modifier = Modifier.fillMaxWidth())
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
