package com.luna.morningagent.ui.theme

import java.time.LocalDateTime

/**
 * Resolves the active TimeSlot from a wall-clock LocalDateTime.
 *
 * Slot ranges (mirrors design/themes.jsx SLOTS):
 *   - Dawn      05:00 – 08:59
 *   - Midday    09:00 – 11:59
 *   - Afternoon 12:00 – 16:59
 *   - Evening   17:00 – 20:59
 *   - Night     21:00 – 04:59 (wraps midnight)
 *
 * No weekend special-case — the v4 design treats weekends as just-another-day
 * and keeps the daily ritual constant.
 */
fun resolveSlot(now: LocalDateTime): TimeSlot = when (now.hour) {
    in 5..8   -> TimeSlot.Dawn
    in 9..11  -> TimeSlot.Midday
    in 12..16 -> TimeSlot.Afternoon
    in 17..20 -> TimeSlot.Evening
    else      -> TimeSlot.Night
}

/**
 * Slot → live MorningTheme. Daytime slots (Dawn/Midday/Afternoon) get the
 * Light palette; Evening + Night get Dark — so the app physically dawns and
 * dusks with Luna's day.
 */
fun resolveTheme(now: LocalDateTime): MorningTheme {
    val slot = resolveSlot(now)
    val isDaytime = slot == TimeSlot.Dawn || slot == TimeSlot.Midday || slot == TimeSlot.Afternoon
    return if (isDaytime) MorningThemes.light(slot) else MorningThemes.dark(slot)
}
