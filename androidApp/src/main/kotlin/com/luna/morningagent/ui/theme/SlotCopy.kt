package com.luna.morningagent.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.luna.morningagent.R

/**
 * String-resource handles for the active time slot. Composables call e.g.
 * `stringResource(slotCopy().greeting)` so the live greeting / sub / button
 * label / status / quote update whenever the active slot changes.
 *
 * Per-slot copy lives in res/values/strings.xml (greeting_dawn, sub_dawn, …).
 * The status and runIcon enums drive the StatusDot color and run-button icon
 * choice in the Home StatusRow.
 */
data class SlotCopyHandles(
    @StringRes val greeting: Int,
    @StringRes val sub:      Int,
    @StringRes val runLabel: Int,
    @StringRes val status:   Int,
    @StringRes val quote:    Int,
    val statusDot: StatusDot,
    val runIcon:   RunIcon,
)

/** Resolves slot copy + enums from the currently-provided MorningTheme. */
@Composable
fun slotCopy(): SlotCopyHandles = copyFor(MaterialTheme.morningSlot)

/** Slot → copy handles. Mirrors design/themes.jsx SLOTS field-for-field. */
fun copyFor(slot: TimeSlot): SlotCopyHandles = when (slot) {
    TimeSlot.Dawn -> SlotCopyHandles(
        greeting  = R.string.greeting_dawn,
        sub       = R.string.sub_dawn,
        runLabel  = R.string.runlabel_dawn,
        status    = R.string.status_dawn,
        quote     = R.string.quote_dawn,
        statusDot = StatusDot.Live,
        runIcon   = RunIcon.Sunrise,
    )
    TimeSlot.Midday -> SlotCopyHandles(
        greeting  = R.string.greeting_midday,
        sub       = R.string.sub_midday,
        runLabel  = R.string.runlabel_midday,
        status    = R.string.status_midday,
        quote     = R.string.quote_midday,
        statusDot = StatusDot.Idle,
        runIcon   = RunIcon.Refresh,
    )
    TimeSlot.Afternoon -> SlotCopyHandles(
        greeting  = R.string.greeting_afternoon_v4,
        sub       = R.string.sub_afternoon,
        runLabel  = R.string.runlabel_afternoon,
        status    = R.string.status_afternoon,
        quote     = R.string.quote_afternoon,
        statusDot = StatusDot.Idle,
        runIcon   = RunIcon.Refresh,
    )
    TimeSlot.Evening -> SlotCopyHandles(
        greeting  = R.string.greeting_evening_v4,
        sub       = R.string.sub_evening,
        runLabel  = R.string.runlabel_evening,
        status    = R.string.status_evening,
        quote     = R.string.quote_evening,
        statusDot = StatusDot.Scheduled,
        runIcon   = RunIcon.Check,
    )
    TimeSlot.Night -> SlotCopyHandles(
        greeting  = R.string.greeting_night,
        sub       = R.string.sub_night,
        runLabel  = R.string.runlabel_night,
        status    = R.string.status_night,
        quote     = R.string.quote_night,
        statusDot = StatusDot.Sleeping,
        runIcon   = RunIcon.Moon,
    )
}
