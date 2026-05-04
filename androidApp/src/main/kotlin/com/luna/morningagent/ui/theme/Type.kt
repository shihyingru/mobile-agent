package com.luna.morningagent.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import com.luna.morningagent.R

// Google Fonts provider (com.google.android.gms — works on any GMS device and emulator).
// If fonts fail to load, the system falls back to the default sans-serif/monospace.
// To regenerate font_certs.xml: Android Studio → res/font → New → Downloadable Font.
private val googleFonts = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

val InterFamily = FontFamily(
    Font(GoogleFont("Inter"), googleFonts, FontWeight.Normal),
    Font(GoogleFont("Inter"), googleFonts, FontWeight.Medium),
    Font(GoogleFont("Inter"), googleFonts, FontWeight.SemiBold),
    Font(GoogleFont("Inter"), googleFonts, FontWeight.Bold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(GoogleFont("JetBrains Mono"), googleFonts, FontWeight.Normal),
    Font(GoogleFont("JetBrains Mono"), googleFonts, FontWeight.Medium),
)

// --- Named text styles matching the design spec ---

object MorningType {
    // Display: Inter 32sp / SemiBold / tracking -0.5
    val Display = TextStyle(
        fontFamily   = InterFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 32.sp,
        letterSpacing = (-0.5).sp,
    )

    // Headline: Inter 22sp / SemiBold
    val Headline = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
    )

    // Title: Inter 17sp / Medium
    val Title = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 17.sp,
    )

    // Body: Inter 15sp / Regular / line-height 22
    val Body = TextStyle(
        fontFamily  = InterFamily,
        fontWeight  = FontWeight.Normal,
        fontSize    = 15.sp,
        lineHeight  = 22.sp,
    )

    // BodyItalic: same as Body but italic (briefing quote)
    val BodyItalic = Body.copy(fontStyle = FontStyle.Italic)

    // Label: Inter 12sp / Medium / uppercase / tracking 1.5
    val Label = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 12.sp,
        letterSpacing = 1.5.sp,
    )

    // Mono: JetBrains Mono 13sp (timestamps, task IDs)
    val Mono = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp,
    )
}
