package com.luna.morningagent.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.luna.morningagent.R

// SOHO Waterworks palette: Sand · Navy · Teal · Aqua · Gold.
//   - Sand   #E5E1DD : page bg (light) / ink (dark)
//   - Navy   #083A4F : ink (light) / page bg (dark, deepened to #04222F)
//   - Teal   #407E8C : accent (light)
//   - Aqua   #C0D5D6 : accent (dark)
//   - Gold   #A58D66 : medium-priority chip (warmed to #C8B083 on dark)
//
// Time-of-day still buckets greeting copy (Morning / Afternoon / Evening /
// Weekend), but only LIGHT vs DARK reach the palette. See TimeOfDayResolver.

enum class TimeOfDay { Morning, Afternoon, Evening, Weekend }

/** Three colour stops for the header logo-mark radial gradient. */
data class LogoMark(val c1: Color, val c2: Color, val c3: Color)

/** Full per-theme colour token bundle. */
data class MorningPalette(
    val background: Color,    // page (Sand / deepened Navy)
    val surface: Color,       // card / input
    val surfaceRaised: Color, // briefing card (one tone above surface)
    val border: Color,        // hairlines (cardEdge)
    val textPrimary: Color,   // ink
    val textSecondary: Color, // inkSoft
    val textMuted: Color,     // inkMute
    val accent: Color,        // primary accent (Teal / Aqua)
    val accentDeep: Color,    // hover / pressed accent
    val accentOnButton: Color,// onAccent label color
    val accentGlow: Color,    // accentSoft — chip bg, focus wash
    val statusGlow: Color,    // halo behind status dot
    val logoMark: LogoMark,   // header dot / orb gradient
    val priorityHigh: Color,  // = accent
    val priorityMid: Color,   // = gold
    val priorityLow: Color,   // = inkMute
    val success: Color,       // "Active" / "Saved" green
    val gold: Color,          // exposed for the bronze CTA + medium chips
    val error: Color,
    val isLight: Boolean,
)

/** Per-theme copy variants — values are string-resource IDs so localisation still works. */
data class MorningCopy(
    @StringRes val greeting: Int,
    @StringRes val statusLabel: Int,
    @StringRes val runButton: Int,
    @StringRes val briefingSection: Int,
    @StringRes val tasksSection: Int,
)

data class MorningTheme(
    val timeOfDay: TimeOfDay,
    val palette: MorningPalette,
    val copy: MorningCopy,
)

object MorningThemes {

    // --- SOHO Waterworks: Light ---------------------------------------------
    private val LightPalette = MorningPalette(
        background     = Color(0xFFE5E1DD), // Sand
        surface        = Color(0xFFF6F3EF), // card
        surfaceRaised  = Color(0xFFFBF9F5), // card raised
        border         = Color(0x14083A4F), // navy @ 8% — cardEdge
        textPrimary    = Color(0xFF083A4F), // Navy ink
        textSecondary  = Color(0xFF3F6273), // inkSoft
        textMuted      = Color(0xFF8A95A0), // inkMute
        accent         = Color(0xFF407E8C), // Teal
        accentDeep     = Color(0xFF1E5765),
        accentOnButton = Color(0xFFF6F3EF), // onAccent (light card tone)
        accentGlow     = Color(0x24407E8C), // accentSoft (~14%)
        statusGlow     = Color(0x1A407E8C),
        logoMark       = LogoMark(Color(0xFF8FB3BB), Color(0xFF407E8C), Color(0xFF1E5765)),
        priorityHigh   = Color(0xFF407E8C),
        priorityMid    = Color(0xFFA58D66), // Gold
        priorityLow    = Color(0xFF8A95A0),
        success        = Color(0xFF5BB58F),
        gold           = Color(0xFFA58D66),
        error          = Color(0xFFB94A3B),
        isLight        = true,
    )

    // --- SOHO Waterworks: Dark ----------------------------------------------
    private val DarkPalette = MorningPalette(
        background     = Color(0xFF04222F), // Deepened Navy
        surface        = Color(0xFF0E3D52), // card
        surfaceRaised  = Color(0xFF134B63), // card raised
        border         = Color(0x1AC0D5D6), // aqua @ 10% — cardEdge
        textPrimary    = Color(0xFFE5E1DD), // Sand
        textSecondary  = Color(0xFFC0D5D6), // Aqua-tinted inkSoft
        textMuted      = Color(0x73E5E1DD), // sand @ 45% — inkMute
        accent         = Color(0xFFC0D5D6), // Aqua
        accentDeep     = Color(0xFF7DA9AE),
        accentOnButton = Color(0xFF04222F), // onAccent (dark navy)
        accentGlow     = Color(0x1FC0D5D6), // accentSoft (~12%)
        statusGlow     = Color(0x1FC0D5D6),
        logoMark       = LogoMark(Color(0xFFE3EEEF), Color(0xFFC0D5D6), Color(0xFF7DA9AE)),
        priorityHigh   = Color(0xFFC0D5D6),
        priorityMid    = Color(0xFFC8B083), // Gold warmed for dark ground
        priorityLow    = Color(0x73E5E1DD),
        success        = Color(0xFF5BB58F),
        gold           = Color(0xFFC8B083),
        error          = Color(0xFFEF6B5B),
        isLight        = false,
    )

    // --- Copy variants (still 4 buckets) ------------------------------------

    private val MorningCopySet = MorningCopy(
        greeting        = R.string.greeting_morning,
        statusLabel     = R.string.agent_status_ready,
        runButton       = R.string.btn_start_the_day,
        briefingSection = R.string.section_your_briefing,
        tasksSection    = R.string.section_focus_today,
    )

    private val AfternoonCopySet = MorningCopy(
        greeting        = R.string.greeting_afternoon,
        statusLabel     = R.string.agent_status_resting,
        runButton       = R.string.btn_refresh_briefing,
        briefingSection = R.string.section_unfinished,
        tasksSection    = R.string.section_loose_ends,
    )

    private val EveningCopySet = MorningCopy(
        greeting        = R.string.greeting_evening,
        statusLabel     = R.string.agent_status_active,
        runButton       = R.string.btn_run_now,
        briefingSection = R.string.section_todays_briefing,
        tasksSection    = R.string.section_tasks,
    )

    private val WeekendCopySet = MorningCopy(
        greeting        = R.string.greeting_weekend,
        statusLabel     = R.string.agent_status_paused,
        runButton       = R.string.btn_plan_next_week,
        briefingSection = R.string.section_reflection,
        tasksSection    = R.string.section_goals_next_week,
    )

    // --- Final theme presets: 2 palettes × 4 copy variants ------------------

    fun light(timeOfDay: TimeOfDay): MorningTheme = MorningTheme(
        timeOfDay = timeOfDay,
        palette   = LightPalette,
        copy      = copyFor(timeOfDay),
    )

    fun dark(timeOfDay: TimeOfDay): MorningTheme = MorningTheme(
        timeOfDay = timeOfDay,
        palette   = DarkPalette,
        copy      = copyFor(timeOfDay),
    )

    // Convenience presets for previews — Evening/Morning copy on each palette.
    val Light = light(TimeOfDay.Morning)
    val Dark  = dark(TimeOfDay.Evening)

    private fun copyFor(timeOfDay: TimeOfDay): MorningCopy = when (timeOfDay) {
        TimeOfDay.Morning   -> MorningCopySet
        TimeOfDay.Afternoon -> AfternoonCopySet
        TimeOfDay.Evening   -> EveningCopySet
        TimeOfDay.Weekend   -> WeekendCopySet
    }
}

val LocalMorningTheme = staticCompositionLocalOf { MorningThemes.Dark }

val MaterialTheme.morning: MorningPalette
    @Composable get() = LocalMorningTheme.current.palette

val MaterialTheme.morningCopy: MorningCopy
    @Composable get() = LocalMorningTheme.current.copy
