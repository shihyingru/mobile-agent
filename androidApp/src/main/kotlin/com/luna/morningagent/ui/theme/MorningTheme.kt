package com.luna.morningagent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// SOHO Waterworks palette: Sand · Navy · Teal · Aqua · Gold.
// Two themes — Light (Sand bg, Navy ink, Teal accent) and Dark (deepened Navy
// bg, Sand text, Aqua accent). Time-of-day picks ONE of five slots
// (Dawn / Midday / Afternoon / Evening / Night) which drives copy + button
// state and biases toward Light or Dark via TimeOfDayResolver.
//
// Per-slot copy lives in strings.xml (greeting_*, sub_*, runlabel_*,
// status_*, quote_*) and is resolved by SlotCopy.kt. The palette holds
// only color tokens; no string IDs.

/** Five time slots driving greeting + run-button copy + status state. */
enum class TimeSlot { Dawn, Midday, Afternoon, Evening, Night }

/** Status-dot semantic state — picks dot color in StatusRow. */
enum class StatusDot { Live, Idle, Scheduled, Sleeping }

/** Run-button leading icon — picked per slot. */
enum class RunIcon { Sunrise, Refresh, Check, Moon }

/** Three colour stops for the launch BreathingOrb (kept from v3 wiring). */
data class LogoMark(val c1: Color, val c2: Color, val c3: Color)

/** Full per-theme color tokens. Names mirror design/Color Palette.md. */
data class MorningPalette(
    // page
    val background:    Color, // bg
    val backgroundWash: Color, // bgWash — top radial wash for paper depth
    // surfaces
    val surface:       Color, // card
    val surfaceRaised: Color, // card raised (briefing block etc)
    val cardEdge:      Color, // hairline divider + card border
    // text
    val textPrimary:   Color, // ink
    val textSecondary: Color, // inkSoft
    val textMuted:     Color, // inkMute
    // accent + on-accent
    val accent:        Color, // primary accent (Teal / Aqua)
    val accentDeep:    Color, // pressed / hover / orb deep stop
    val accentSoft:    Color, // 12–14% accent — chip bg, focus wash, model row bg
    val onAccent:      Color, // text/icon on accent surfaces
    val accentGlow:    Color, // alias for accentSoft (legacy callers)
    val statusGlow:    Color, // halo behind status dot when present
    // semantic / chip palette
    val priorityHigh:  Color, // = accent
    val priorityMid:   Color, // = gold
    val priorityLow:   Color, // = inkMute
    val gold:          Color, // exposed for medium-priority chip + idle status dot
    val success:       Color, // "Active" / "Saved" green
    val error:         Color,
    // brand
    val logoMark:      LogoMark,
    val isLight:       Boolean,
)

object MorningThemes {

    // --- Light · Sand · Navy · Teal --------------------------------------
    private val LightPalette = MorningPalette(
        background     = Color(0xFFE5E1DD),  // SAND
        backgroundWash = Color(0xFFD8D3CC),
        surface        = Color(0xFFF6F3EF),
        surfaceRaised  = Color(0xFFFBF9F5),
        cardEdge       = Color(0x14083A4F),  // navy @ 8%
        textPrimary    = Color(0xFF083A4F),  // NAVY ink
        textSecondary  = Color(0xFF3F6273),
        textMuted      = Color(0xFF8A95A0),
        accent         = Color(0xFF407E8C),  // TEAL
        accentDeep     = Color(0xFF1E5765),
        accentSoft     = Color(0x24407E8C),  // teal @ ~14%
        onAccent       = Color(0xFFF6F3EF),
        accentGlow     = Color(0x24407E8C),
        statusGlow     = Color(0x1A407E8C),
        priorityHigh   = Color(0xFF407E8C),
        priorityMid    = Color(0xFFA58D66),  // GOLD
        priorityLow    = Color(0xFF8A95A0),
        gold           = Color(0xFFA58D66),
        success        = Color(0xFF5BB58F),
        error          = Color(0xFFB94A3B),
        logoMark       = LogoMark(Color(0xFF8FB3BB), Color(0xFF407E8C), Color(0xFF1E5765)),
        isLight        = true,
    )

    // --- Dark · Navy · Sand · Aqua ---------------------------------------
    private val DarkPalette = MorningPalette(
        background     = Color(0xFF04222F),  // deepened NAVY
        backgroundWash = Color(0xFF072D3D),
        surface        = Color(0xFF0E3D52),
        surfaceRaised  = Color(0xFF134B63),
        cardEdge       = Color(0x1AC0D5D6),  // aqua @ 10%
        textPrimary    = Color(0xFFE5E1DD),  // SAND text
        textSecondary  = Color(0xFFC0D5D6),  // AQUA-tinted soft
        textMuted      = Color(0x73E5E1DD),  // sand @ 45%
        accent         = Color(0xFFC0D5D6),  // AQUA
        accentDeep     = Color(0xFF7DA9AE),
        accentSoft     = Color(0x1FC0D5D6),  // aqua @ ~12%
        onAccent       = Color(0xFF04222F),
        accentGlow     = Color(0x1FC0D5D6),
        statusGlow     = Color(0x1FC0D5D6),
        priorityHigh   = Color(0xFFC0D5D6),
        priorityMid    = Color(0xFFC8B083),  // gold warmed for dark ground
        priorityLow    = Color(0x73E5E1DD),
        gold           = Color(0xFFC8B083),
        success        = Color(0xFF5BB58F),
        error          = Color(0xFFEF6B5B),
        logoMark       = LogoMark(Color(0xFFE3EEEF), Color(0xFFC0D5D6), Color(0xFF7DA9AE)),
        isLight        = false,
    )

    fun light(slot: TimeSlot): MorningTheme = MorningTheme(slot = slot, palette = LightPalette)
    fun dark(slot: TimeSlot):  MorningTheme = MorningTheme(slot = slot, palette = DarkPalette)

    // Preview convenience.
    val Light = light(TimeSlot.Midday)
    val Dark  = dark(TimeSlot.Evening)
}

/** Active palette + active time slot. Copy is resolved per slot from strings.xml. */
data class MorningTheme(
    val slot:    TimeSlot,
    val palette: MorningPalette,
)

val LocalMorningTheme = staticCompositionLocalOf { MorningThemes.Light }

val MaterialTheme.morning: MorningPalette
    @Composable get() = LocalMorningTheme.current.palette

val MaterialTheme.morningSlot: TimeSlot
    @Composable get() = LocalMorningTheme.current.slot
