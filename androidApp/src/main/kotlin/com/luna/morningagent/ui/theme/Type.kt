package com.luna.morningagent.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import com.luna.morningagent.R

// Google Fonts provider — works on any GMS device + emulator. Falls back to
// the system default sans-serif/serif/monospace if a download fails.
private val googleFonts = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

// --- v4 type stacks (mirrors design/themes.jsx SERIF / SERIF_READ / SANS / MONO) ---

// Display serif — greeting, wordmark, section headings. Editorial weight.
val SerifFamily = FontFamily(
    Font(GoogleFont("Instrument Serif"), googleFonts, FontWeight.Normal),
    Font(GoogleFont("Instrument Serif"), googleFonts, FontWeight.Normal, FontStyle.Italic),
)

// Reading serif — task titles, briefing quotes, italic asides.
val SerifReadFamily = FontFamily(
    Font(GoogleFont("Newsreader"), googleFonts, FontWeight.Normal),
    Font(GoogleFont("Newsreader"), googleFonts, FontWeight.Medium),
    Font(GoogleFont("Newsreader"), googleFonts, FontWeight.Normal, FontStyle.Italic),
    Font(GoogleFont("Newsreader"), googleFonts, FontWeight.Medium, FontStyle.Italic),
)

// Sans — chrome, buttons, toggles, badges.
val InterFamily = FontFamily(
    Font(GoogleFont("Inter"), googleFonts, FontWeight.Normal),
    Font(GoogleFont("Inter"), googleFonts, FontWeight.Medium),
    Font(GoogleFont("Inter"), googleFonts, FontWeight.SemiBold),
    Font(GoogleFont("Inter"), googleFonts, FontWeight.Bold),
)

// Mono — date lines, model ids, meta lines, labels.
val MonoFamily = FontFamily(
    Font(GoogleFont("IBM Plex Mono"), googleFonts, FontWeight.Normal),
    Font(GoogleFont("IBM Plex Mono"), googleFonts, FontWeight.Medium),
    Font(GoogleFont("IBM Plex Mono"), googleFonts, FontWeight.SemiBold),
)

// --- Named text styles ---

object MorningType {

    // Greeting block — Instrument Serif 34sp / -0.6 letterSpacing.
    val GreetingDisplay = TextStyle(
        fontFamily    = SerifFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 34.sp,
        letterSpacing = (-0.6).sp,
        lineHeight    = (34 * 1.05f).sp,
    )

    // Top-bar wordmark — "Agent" / "Settings" / "Morning Agent".
    val Wordmark = TextStyle(
        fontFamily    = SerifFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 20.sp,
        letterSpacing = (-0.2).sp,
    )

    // Section headings — "Today", "Pick briefing time".
    val SectionHeading = TextStyle(
        fontFamily    = SerifFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 22.sp,
        letterSpacing = (-0.2).sp,
        lineHeight    = 22.sp,
    )

    // Settings page title — "Settings" (slightly larger than Wordmark).
    val ScreenTitle = TextStyle(
        fontFamily    = SerifFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 26.sp,
        letterSpacing = (-0.3).sp,
        lineHeight    = 26.sp,
    )

    // Task titles — Newsreader 18sp / 500 / -0.1 tracking.
    val TaskTitle = TextStyle(
        fontFamily    = SerifReadFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 18.sp,
        letterSpacing = (-0.1).sp,
        lineHeight    = (18 * 1.25f).sp,
    )

    // Briefing quote — Newsreader 18sp / 1.45 line / left accent border.
    val Quote = TextStyle(
        fontFamily    = SerifReadFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 18.sp,
        letterSpacing = (-0.1).sp,
        lineHeight    = (18 * 1.45f).sp,
    )

    // Italic body — settings intro, greeting sub, model notes, behavior subs.
    val BodyReadItalic = TextStyle(
        fontFamily = SerifReadFamily,
        fontWeight = FontWeight.Normal,
        fontStyle  = FontStyle.Italic,
        fontSize   = 15.sp,
        lineHeight = (15 * 1.4f).sp,
    )

    // Smaller italic — task tip body.
    val Tip = TextStyle(
        fontFamily = SerifReadFamily,
        fontWeight = FontWeight.Normal,
        fontStyle  = FontStyle.Italic,
        fontSize   = 14.sp,
        lineHeight = (14 * 1.5f).sp,
    )

    // Sans button label / toggle title.
    val ButtonLabel = TextStyle(
        fontFamily    = InterFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 13.sp,
        letterSpacing = 0.1.sp,
    )

    // Sans row title — "Auto-run on launch", "Daily briefing notification".
    val RowTitle = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.5.sp,
        lineHeight = (14.5 * 1.25f).sp,
    )

    // Sans status label — "Resting", "Briefing ready".
    val StatusLabel = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = (14 * 1.2f).sp,
    )

    // Mono section labels — "BRIEFING", "AI PROVIDER", "CREDENTIALS".
    val LabelMono = TextStyle(
        fontFamily    = MonoFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 10.sp,
        letterSpacing = 1.8.sp,
    )

    // Mono date line — "FRIDAY · MAY 15 · 2:23 PM" (slightly wider tracking).
    val DateLine = TextStyle(
        fontFamily    = MonoFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        letterSpacing = 2.sp,
    )

    // Mono meta — "gemini-2.5 · 312 tok", model IDs.
    val MetaMono = TextStyle(
        fontFamily    = MonoFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 9.5.sp,
        letterSpacing = 0.4.sp,
    )

    // Mono caption — "Next run · tomorrow, 9:00", task estimate.
    val Caption = TextStyle(
        fontFamily    = MonoFamily,
        fontWeight    = FontWeight.Normal,
        fontSize      = 10.5.sp,
        letterSpacing = 0.3.sp,
    )

    // Mono model id (Settings model picker) — "gemini-2.5-pro".
    val ModelId = TextStyle(
        fontFamily    = MonoFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 13.sp,
        letterSpacing = 0.2.sp,
    )

    // --- Legacy styles kept for compatibility until commit 3 rewrites consumers. ---

    val Display = GreetingDisplay
    val Headline = TaskTitle
    val Title = StatusLabel.copy(fontSize = 17.sp)
    val Body = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp,
        lineHeight = 22.sp,
    )
    val BodyItalic = Body.copy(fontStyle = FontStyle.Italic)
    val Label = LabelMono.copy(fontFamily = InterFamily, letterSpacing = 1.5.sp, fontSize = 12.sp)
    val Mono = Caption.copy(fontSize = 13.sp)
}
