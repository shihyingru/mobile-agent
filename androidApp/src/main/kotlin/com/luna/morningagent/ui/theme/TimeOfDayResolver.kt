package com.luna.morningagent.ui.theme

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Single source of truth for time-of-day bucketing. Weekend wins over hour-of-day;
 * 22:00–04:59 folds into evening so late-night doesn't trip the morning bucket.
 *
 * `HomeViewModel.greetingResFor` delegates here so theme + greeting always agree.
 */
fun resolveTimeOfDay(now: LocalDateTime): TimeOfDay {
    val weekend = now.dayOfWeek == DayOfWeek.SATURDAY || now.dayOfWeek == DayOfWeek.SUNDAY
    if (weekend) return TimeOfDay.Weekend
    return when (now.hour) {
        in 5..11  -> TimeOfDay.Morning
        in 12..17 -> TimeOfDay.Afternoon
        else      -> TimeOfDay.Evening
    }
}

/**
 * Time-of-day → live MorningTheme. Light palette during daylight (Morning,
 * Afternoon, and weekend daytime), Dark in the evening. Weekend evening hours
 * still get the Dark palette so the app physically dawns and dusks with Luna's
 * day instead of staying bright at 11pm on a Saturday.
 */
fun resolveTheme(now: LocalDateTime): MorningTheme {
    val timeOfDay = resolveTimeOfDay(now)
    val daytimeHour = now.hour in 5..17
    return if (daytimeHour) {
        MorningThemes.light(timeOfDay)
    } else {
        MorningThemes.dark(timeOfDay)
    }
}
