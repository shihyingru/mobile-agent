package com.luna.morningagent.ui.common

import androidx.annotation.StringRes

sealed interface SnackbarEvent {
    data class ResId(@StringRes val id: Int) : SnackbarEvent
    data class ResIdWithArgs(@StringRes val id: Int, val args: List<Any>) : SnackbarEvent
    data class Plain(val message: String) : SnackbarEvent
}
