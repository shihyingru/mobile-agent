# Mobile Agent App — Design System

> Drop this file into your Android project (e.g. `/docs/DESIGN_SYSTEM.md`) as the source-of-truth reference for tokens, type, components, and behavior. Tokens are given in hex / ARGB so they map cleanly to `colors.xml` or Jetpack Compose `Color(0xFF…)`.

---

## 1. Foundations

### 1.1 Palette — "SOHO Waterworks"

Five source swatches drive both themes.

| Name | Hex       | ARGB         | RGB             |
|------|-----------|--------------|-----------------|
| Navy | `#083A4F` | `0xFF083A4F` | 8 · 58 · 79     |
| Gold | `#A58D66` | `0xFFA58D66` | 165 · 141 · 102 |
| Aqua | `#C0D5D6` | `0xFFC0D5D6` | 192 · 213 · 214 |
| Teal | `#407E8C` | `0xFF407E8C` | 64 · 126 · 140  |
| Sand | `#E5E1DD` | `0xFFE5E1DD` | 229 · 225 · 221 |

**Light theme** reads as Sand paper with Navy ink and a Teal accent. **Dark theme** inverts to a deep Navy ground with Sand text and an Aqua accent. Gold is the shared medium-priority tone in both.

---

### 1.2 Theme tokens

Token names are the single source of truth — use them everywhere instead of raw hex.

#### Light theme

| Token         | Value                                                                 | Usage                                              |
|---------------|-----------------------------------------------------------------------|----------------------------------------------------|
| `bg`          | `#E5E1DD`                                                             | Page background (Sand)                             |
| `bgWash`      | `#D8D3CC`                                                             | Top radial wash for depth                          |
| `card`        | `#F6F3EF`                                                             | Card / input surface                               |
| `cardEdge`    | `#083A4F` @ 8% (`0x14083A4F`)                                         | Card border, hairline dividers                     |
| `ink`         | `#083A4F`                                                             | Primary text (Navy)                                |
| `inkSoft`     | `#3F6273`                                                             | Secondary text, italic asides                      |
| `inkMute`     | `#8A95A0`                                                             | Meta, placeholders, mono labels                    |
| `accent`      | `#407E8C`                                                             | Primary accent (Teal) — buttons, dots, links       |
| `accentDeep`  | `#1E5765`                                                             | Accent hover / pressed                             |
| `accentSoft`  | `#407E8C` @ 14% (`0x24407E8C`)                                        | Accent tint — chip backgrounds, hover wash         |
| `onAccent`    | `#F6F3EF`                                                             | Text on accent surfaces                            |
| `gold`        | `#A58D66`                                                             | Medium-priority chips                              |
| `statusGood`  | `#5BB58F`                                                             | Saved / live confirmations                         |

#### Dark theme

| Token         | Value                                                                 | Usage                                              |
|---------------|-----------------------------------------------------------------------|----------------------------------------------------|
| `bg`          | `#04222F`                                                             | Page background (deepened Navy)                    |
| `bgWash`      | `#072D3D`                                                             | Top radial wash                                    |
| `card`        | `#0E3D52`                                                             | Card / input surface                               |
| `cardEdge`    | `#C0D5D6` @ 10% (`0x1AC0D5D6`)                                        | Card border, hairline dividers                     |
| `ink`         | `#E5E1DD`                                                             | Primary text (Sand)                                |
| `inkSoft`     | `#C0D5D6`                                                             | Secondary text (Aqua tint)                         |
| `inkMute`     | `#E5E1DD` @ 45% (`0x73E5E1DD`)                                        | Meta, placeholders, mono labels                    |
| `accent`      | `#C0D5D6`                                                             | Primary accent (Aqua)                              |
| `accentDeep`  | `#7DA9AE`                                                             | Accent hover / pressed                             |
| `accentSoft`  | `#C0D5D6` @ 12% (`0x1FC0D5D6`)                                        | Accent tint — chip backgrounds, hover wash         |
| `onAccent`    | `#04222F`                                                             | Text on accent surfaces                            |
| `gold`        | `#C8B083`                                                             | Medium-priority chips (warmed for dark ground)     |
| `statusGood`  | `#5BB58F`                                                             | Saved / live confirmations                         |

#### Semantic tokens (shared)

| Token              | Maps to        | Usage                                  |
|--------------------|----------------|----------------------------------------|
| `priority.high`    | `accent`       | High-priority chip                     |
| `priority.med`     | `gold`         | Medium-priority chip                   |
| `priority.low`     | `inkMute`      | Low-priority chip                      |
| `status.live`      | `statusGood`   | Active / live status dot               |
| `status.saved`     | `statusGood`   | "Saved" confirmation                   |
| `status.scheduled` | `accent`       | Scheduled / set status                 |
| `status.idle`      | `gold`         | Idle / resting status                  |
| `status.sleeping`  | `inkMute`      | Quiet hours / sleeping                 |

#### Source-logo brand colors

Used as-is in both themes for connected service chips.

| Source   | Background | Foreground |
|----------|------------|------------|
| Notion   | `#FFFFFF`  | `#231C12`  |
| Linear   | `#5E6AD2`  | `#FFFFFF`  |
| GitHub   | `#1F2328`  | `#F0F6FC`  |
| Gemini   | `#4285F4`  | `#FFFFFF`  |
| Claude   | `#D97757`  | `#FFFFFF`  |
| OpenAI   | `#0A0A0A`  | `#FFFFFF`  |
| Mistral  | `#FA520F`  | `#FFFFFF`  |
| DeepSeek | `#4D6BFE`  | `#FFFFFF`  |
| Llama    | `#0866FF`  | `#FFFFFF`  |

---

### 1.3 Typography

Three families, one fallback stack each. Use Google Fonts via `androidx.compose.ui.text.googlefonts` or bundle `.ttf` files in `res/font/`.

| Role         | Family              | Fallback                                    | Notes                                  |
|--------------|---------------------|---------------------------------------------|----------------------------------------|
| Display      | **Instrument Serif**| Noto Serif SC, Songti SC, Georgia, serif    | Greetings, screen titles, wordmark     |
| Reading      | **Newsreader**      | Noto Serif SC, Songti SC, Georgia, serif    | Task titles, briefings, italic asides  |
| UI / Sans    | **Inter**           | Noto Sans SC, PingFang SC, system-ui        | Buttons, labels, body UI               |
| Mono         | **IBM Plex Mono**   | Sarasa Mono SC, ui-monospace, monospace     | Dates, meta, model IDs, masked values  |

#### Type scale

| Token              | Family   | Size  | Weight | Line  | Letter-spacing | Use                                          |
|--------------------|----------|-------|--------|-------|----------------|----------------------------------------------|
| `display.lg`       | Display  | 38 sp | 400    | 1.0   | -0.6 px        | Launch screen wordmark                       |
| `display.md`       | Display  | 34 sp | 400    | 1.05  | -0.6 px        | Home greeting                                |
| `display.sm`       | Display  | 26 sp | 400    | 1.0   | -0.3 px        | Settings header                              |
| `title.md`         | Display  | 22 sp | 400    | 1.0   | -0.2 px        | "Today" section header, dialog title         |
| `title.sm`         | Display  | 20 sp | 400    | n/a   | -0.2 px        | Wordmark in top bar                          |
| `body.lg.read`     | Reading  | 18 sp | 500    | 1.25  | -0.1 px        | Task titles                                  |
| `body.md.read`     | Reading  | 18 sp | 400    | 1.45  | -0.1 px        | Briefing quote                               |
| `body.md.italic`   | Reading  | 15 sp | 400 it.| 1.4   | 0              | Greeting subhead                             |
| `body.sm.italic`   | Reading  | 14 sp | 400 it.| 1.45  | 0              | Tips, behavior-row subtitles                 |
| `body.xs.italic`   | Reading  | 12 sp | 400 it.| 1.3   | 0              | Model note in picker                         |
| `body.md`          | Sans     | 14 sp | 500    | 1.25  | 0              | Behavior row title                           |
| `body.sm`          | Sans     | 13.5  | 600    | 1.2   | 0              | Provider tile name                           |
| `label.lg`         | Sans     | 14 sp | 600    | n/a   | 0.1 px         | Save button                                  |
| `label.md`         | Sans     | 13 sp | 600    | n/a   | 0.1 px         | Inline action, secondary button              |
| `label.sm`         | Sans     | 12 sp | 600    | n/a   | 0.2 px         | "Saved" confirmation                         |
| `label.chip`       | Sans     | 11 sp | 600    | n/a   | 0.2 px         | Priority chip                                |
| `mono.sm`          | Mono     | 13 sp | 500    | n/a   | 0.2 px         | Model ID in picker, time picker value        |
| `mono.xs`          | Mono     | 10–11 | 400–500| n/a   | 0.3–0.4 px     | Meta, source line, masked credentials        |
| `mono.eyebrow`     | Mono     | 10 sp | 600    | n/a   | 2 px UPPER     | Date line, section labels                    |
| `mono.footer`      | Mono     | 9.5   | 500    | n/a   | 2 px UPPER     | Footer credit                                |

**Rules**
- Display + Reading set in *Title-case sentences*, not ALL CAPS.
- Eyebrow / footer / section labels are Mono, uppercase, 1.6–2 px tracked.
- Body italic comes from Reading (Newsreader italic), never from synthetic obliquing of Sans.
- Never mix Display and Reading inside the same line.

---

### 1.4 Spacing

4 px base unit. Card padding leans `16`; screen padding leans `18`.

| Token | px | Use                                            |
|-------|----|------------------------------------------------|
| `0`   | 0  |                                                |
| `1`   | 4  | Hairline insets                                |
| `2`   | 8  | Icon → label, chip gap, row gap inside cards   |
| `3`   | 10 | Card → card gap (tight), button icon → label   |
| `4`   | 12 | Inter-row gap, dialog gap                      |
| `5`   | 14 | Section gap, card → next section               |
| `6`   | 16 | Default card padding                           |
| `7`   | 18 | Screen edge padding                            |
| `8`   | 22 | Screen top padding                             |
| `9`   | 32 | Screen bottom padding                          |

---

### 1.5 Shape (corner radius)

| Token         | px | Use                                            |
|---------------|----|------------------------------------------------|
| `radius.xs`   | 4  | Source-logo glyph chip                         |
| `radius.sm`   | 7  | Provider glyph tile                            |
| `radius.md`   | 12 | Inputs, model picker rows                      |
| `radius.lg`   | 14 | Provider tiles, primary buttons                |
| `radius.xl`   | 18 | Task cards, briefing cards                     |
| `radius.xxl`  | 22 | Dialogs / modal sheets                         |
| `radius.pill` | 999| Priority chips, run button, toggle track       |

---

### 1.6 Elevation & dividers

Shadows are intentionally low. Cards rely on the inset highlight + a long blurred drop. Dark-theme shadows are deeper and warmer.

**Card shadow — light**
- Inset top highlight: `0 1 0 rgba(255,255,255,0.6) inset`
- Soft contact: `0 1 2 rgba(8,58,79,0.04)`
- Drop: `0 12 28 -16 rgba(8,58,79,0.18)`

**Card shadow — dark**
- Inset top highlight: `0 1 0 rgba(192,213,214,0.05) inset`
- Drop: `0 12 32 -16 rgba(0,0,0,0.6)`

**Primary-button drop**
- `0 4 14 rgba(accent, 0.20)` (Home run button)
- `0 6 18 rgba(accent, 0.20)` (Settings Save button)

**Dialog drop**
- `0 30 60 rgba(0,0,0,0.35) + 0 0 0 1 cardEdge`

**Dividers**
- 1 px solid `cardEdge`. Used between behavior rows and inside cards under the title.

---

### 1.7 Motion

| Token             | Duration | Easing            | Use                                  |
|-------------------|----------|-------------------|--------------------------------------|
| `motion.fast`     | 180 ms   | standard          | Toggle thumb slide                   |
| `motion.medium`   | 380 ms   | ease-out          | Slot fade (greeting/status/button)   |
| `motion.slow`     | 420 ms   | ease-out          | Briefing-quote fade                  |
| `motion.breath`   | 2600 ms  | ease-in-out, loop | Launch orb (scale 1.0 → 1.06)        |
| `motion.pulse`    | 1600 ms  | ease-out, loop    | Live status dot ring                 |
| `motion.dot`      | 1400 ms  | ease-in-out, loop | Loading dots (staggered 200 ms)      |

**Slot fade**: `opacity 0 → 1` + `translateY -3 → 0`.
**Fade-in-up**: `opacity 0 → 1` + `translateY 8 → 0` (launch wordmark / subtitle).

---

## 2. Android resource mapping

### 2.1 `res/values/colors.xml`

```xml
<resources>
    <!-- Source -->
    <color name="brand_navy">#083A4F</color>
    <color name="brand_gold">#A58D66</color>
    <color name="brand_aqua">#C0D5D6</color>
    <color name="brand_teal">#407E8C</color>
    <color name="brand_sand">#E5E1DD</color>

    <!-- Light theme -->
    <color name="bg_light">#E5E1DD</color>
    <color name="bg_wash_light">#D8D3CC</color>
    <color name="card_light">#F6F3EF</color>
    <color name="card_edge_light">#14083A4F</color>
    <color name="ink_light">#083A4F</color>
    <color name="ink_soft_light">#3F6273</color>
    <color name="ink_mute_light">#8A95A0</color>
    <color name="accent_light">#407E8C</color>
    <color name="accent_deep_light">#1E5765</color>
    <color name="accent_soft_light">#24407E8C</color>
    <color name="on_accent_light">#F6F3EF</color>
    <color name="gold_light">#A58D66</color>

    <!-- Dark theme -->
    <color name="bg_dark">#04222F</color>
    <color name="bg_wash_dark">#072D3D</color>
    <color name="card_dark">#0E3D52</color>
    <color name="card_edge_dark">#1AC0D5D6</color>
    <color name="ink_dark">#E5E1DD</color>
    <color name="ink_soft_dark">#C0D5D6</color>
    <color name="ink_mute_dark">#73E5E1DD</color>
    <color name="accent_dark">#C0D5D6</color>
    <color name="accent_deep_dark">#7DA9AE</color>
    <color name="accent_soft_dark">#1FC0D5D6</color>
    <color name="on_accent_dark">#04222F</color>
    <color name="gold_dark">#C8B083</color>

    <!-- Shared -->
    <color name="status_good">#5BB58F</color>
</resources>
```

### 2.2 `res/values/dimens.xml`

```xml
<resources>
    <!-- Spacing -->
    <dimen name="space_1">4dp</dimen>
    <dimen name="space_2">8dp</dimen>
    <dimen name="space_3">10dp</dimen>
    <dimen name="space_4">12dp</dimen>
    <dimen name="space_5">14dp</dimen>
    <dimen name="space_6">16dp</dimen>
    <dimen name="space_7">18dp</dimen>
    <dimen name="space_8">22dp</dimen>
    <dimen name="space_9">32dp</dimen>

    <!-- Radii -->
    <dimen name="radius_xs">4dp</dimen>
    <dimen name="radius_sm">7dp</dimen>
    <dimen name="radius_md">12dp</dimen>
    <dimen name="radius_lg">14dp</dimen>
    <dimen name="radius_xl">18dp</dimen>
    <dimen name="radius_xxl">22dp</dimen>

    <!-- Component heights -->
    <dimen name="btn_run_height">38dp</dimen>
    <dimen name="btn_primary_height">46dp</dimen>
    <dimen name="toggle_track_w">44dp</dimen>
    <dimen name="toggle_track_h">26dp</dimen>
    <dimen name="toggle_thumb">22dp</dimen>
    <dimen name="status_dot">10dp</dimen>
    <dimen name="hairline">1dp</dimen>
</resources>
```

### 2.3 Jetpack Compose theme sketch

```kotlin
// ui/theme/Color.kt
package com.example.agent.ui.theme
import androidx.compose.ui.graphics.Color

object AgentPalette {
    // Light
    val BgLight        = Color(0xFFE5E1DD)
    val BgWashLight    = Color(0xFFD8D3CC)
    val CardLight      = Color(0xFFF6F3EF)
    val CardEdgeLight  = Color(0x14083A4F)
    val InkLight       = Color(0xFF083A4F)
    val InkSoftLight   = Color(0xFF3F6273)
    val InkMuteLight   = Color(0xFF8A95A0)
    val AccentLight    = Color(0xFF407E8C)
    val AccentDeepLight= Color(0xFF1E5765)
    val AccentSoftLight= Color(0x24407E8C)
    val OnAccentLight  = Color(0xFFF6F3EF)
    val GoldLight      = Color(0xFFA58D66)

    // Dark
    val BgDark         = Color(0xFF04222F)
    val BgWashDark     = Color(0xFF072D3D)
    val CardDark       = Color(0xFF0E3D52)
    val CardEdgeDark   = Color(0x1AC0D5D6)
    val InkDark        = Color(0xFFE5E1DD)
    val InkSoftDark    = Color(0xFFC0D5D6)
    val InkMuteDark    = Color(0x73E5E1DD)
    val AccentDark     = Color(0xFFC0D5D6)
    val AccentDeepDark = Color(0xFF7DA9AE)
    val AccentSoftDark = Color(0x1FC0D5D6)
    val OnAccentDark   = Color(0xFF04222F)
    val GoldDark       = Color(0xFFC8B083)

    // Shared
    val StatusGood     = Color(0xFF5BB58F)
}

// ui/theme/AgentColors.kt — semantic token bundle
@Immutable
data class AgentColors(
    val bg: Color, val bgWash: Color,
    val card: Color, val cardEdge: Color,
    val ink: Color, val inkSoft: Color, val inkMute: Color,
    val accent: Color, val accentDeep: Color, val accentSoft: Color,
    val onAccent: Color, val gold: Color, val statusGood: Color,
    val isLight: Boolean,
)

val LightAgentColors = AgentColors(
    bg = AgentPalette.BgLight, bgWash = AgentPalette.BgWashLight,
    card = AgentPalette.CardLight, cardEdge = AgentPalette.CardEdgeLight,
    ink = AgentPalette.InkLight, inkSoft = AgentPalette.InkSoftLight,
    inkMute = AgentPalette.InkMuteLight,
    accent = AgentPalette.AccentLight, accentDeep = AgentPalette.AccentDeepLight,
    accentSoft = AgentPalette.AccentSoftLight, onAccent = AgentPalette.OnAccentLight,
    gold = AgentPalette.GoldLight, statusGood = AgentPalette.StatusGood,
    isLight = true,
)

val DarkAgentColors = LightAgentColors.copy(
    bg = AgentPalette.BgDark, bgWash = AgentPalette.BgWashDark,
    card = AgentPalette.CardDark, cardEdge = AgentPalette.CardEdgeDark,
    ink = AgentPalette.InkDark, inkSoft = AgentPalette.InkSoftDark,
    inkMute = AgentPalette.InkMuteDark,
    accent = AgentPalette.AccentDark, accentDeep = AgentPalette.AccentDeepDark,
    accentSoft = AgentPalette.AccentSoftDark, onAccent = AgentPalette.OnAccentDark,
    gold = AgentPalette.GoldDark, isLight = false,
)

val LocalAgentColors = staticCompositionLocalOf { LightAgentColors }

// ui/theme/Type.kt
val InstrumentSerif = FontFamily(/* res/font/instrument_serif_*.ttf */)
val Newsreader      = FontFamily(/* res/font/newsreader_*.ttf */)
val Inter           = FontFamily(/* res/font/inter_*.ttf */)
val IbmPlexMono     = FontFamily(/* res/font/ibm_plex_mono_*.ttf */)

// Map to Material3 Typography roles as a starting point — but reach for
// the named tokens above for screen layout.
val AgentTypography = Typography(
    displayLarge = TextStyle(fontFamily = InstrumentSerif, fontSize = 38.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp),
    displayMedium= TextStyle(fontFamily = InstrumentSerif, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    displaySmall = TextStyle(fontFamily = InstrumentSerif, fontSize = 26.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    titleLarge   = TextStyle(fontFamily = InstrumentSerif, fontSize = 22.sp, letterSpacing = (-0.2).sp),
    bodyLarge    = TextStyle(fontFamily = Newsreader,      fontSize = 18.sp, lineHeight = 22.sp),
    bodyMedium   = TextStyle(fontFamily = Newsreader,      fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall    = TextStyle(fontFamily = Newsreader,      fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge   = TextStyle(fontFamily = Inter,           fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelMedium  = TextStyle(fontFamily = Inter,           fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    labelSmall   = TextStyle(fontFamily = Inter,           fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
)

// ui/theme/AgentTheme.kt
@Composable
fun AgentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAgentColors else LightAgentColors
    CompositionLocalProvider(LocalAgentColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme)
                darkColorScheme(
                    primary = colors.accent, onPrimary = colors.onAccent,
                    background = colors.bg, onBackground = colors.ink,
                    surface = colors.card, onSurface = colors.ink,
                )
            else lightColorScheme(
                primary = colors.accent, onPrimary = colors.onAccent,
                background = colors.bg, onBackground = colors.ink,
                surface = colors.card, onSurface = colors.ink,
            ),
            typography = AgentTypography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(14.dp),
                large = RoundedCornerShape(18.dp),
            ),
            content = content,
        )
    }
}

// Convenience accessor — use this everywhere instead of MaterialTheme.colorScheme
object AgentTheme { val colors @Composable get() = LocalAgentColors.current }
```

---

## 3. Components

Geometry, padding, and behavior. Names are intentionally generic — they will translate to Compose `@Composable` functions or `View` subclasses.

### 3.1 PhoneShell (frame)
- Outer corner: 44 dp, padding 8 dp, gradient bezel.
- Inner screen corner: 36 dp.
- Status bar height: 36 dp, side padding 24 dp.
- Home-indicator bar: 120 × 4 dp pill, bottom 24 dp gutter.

> Reference-only. In a real Android build the system handles the status bar — use `WindowCompat.setDecorFitsSystemWindows(false)` and let content draw edge-to-edge.

### 3.2 Card
- Background: `card`
- Border: 1 dp `cardEdge`
- Corner: `radius.xl` (18 dp)
- Padding: 16–18 dp
- Elevation: see §1.6 card shadow
- Used for: task cards, briefing card, input fields *(yes — inputs are styled like flat cards)*.

### 3.3 Button — Run (Home, inline)
- Shape: pill, height 38 dp, padding `12 dp` left / `14 dp` right
- Background: `accent`, foreground: `onAccent`
- Icon (16 dp) + label, gap 8 dp
- Shadow: `0 4 14 rgba(accent, 0.20)`
- Animates in on slot change (`motion.medium`)

### 3.4 Button — Primary (Settings Save)
- Shape: 14 dp corner, height 46 dp, full-width
- **Enabled**: `accent` bg + `onAccent` text + `0 6 18 rgba(accent, 0.20)` shadow
- **Disabled**: ink @ 10% (light) / ink @ 12% (dark), `inkMute` text, no shadow
- Followed by an inline "Saved" confirmation (label.sm in `statusGood`) when the most recent save succeeded.

### 3.5 Button — Tertiary
- No background or border; `accent` label.lg; 6 dp top/bottom padding.
- Used for "Test Notion connection", "Send test notification", dialog Cancel/Set.

### 3.6 Status dot
- 10 dp circle, color by state (§1.2 semantic).
- Live state adds a pulsing ring: -4 dp outset, same color @ 25% opacity, `motion.pulse`.

### 3.7 Priority chip
- Pill, height ~20 dp, padding `3 dp` vertical / `9 dp` horizontal.
- Layout: 5 dp dot + label, 6 dp gap.
- Variants: high = `accentSoft` / `accent`; med = gold @ 16% / `gold`; low = inkMute @ 16% / `inkMute`.
- Label: label.chip.

### 3.8 Source logo
- 16–18 dp square, `radius.xs` (4 dp).
- Single-letter glyph at 58% of the square, Sans 700, letter-spacing -0.5 px.
- Background + foreground from §1.2 source-logo brand table.

### 3.9 Input field
- Looks like a small card, not an underlined `EditText`.
- Background: `card`, border: 1 dp `cardEdge`, corner: `radius.md` (12 dp), padding: 10 dp vertical / 14 dp horizontal.
- Two-line layout: 11 sp Sans label (`inkMute`) over 14 sp value.
- Value is **Mono** when non-empty (mimics a credential); **Sans** in `inkMute` when empty (placeholder).
- Masked values use bullet `•` characters, never `*` or `x`.

### 3.10 Provider tile
- Horizontal scroll, min width 124 dp, padding 12 / 14 dp.
- Idle: 1 dp `cardEdge` border, transparent bg.
- Active: 1.5 dp `accent` border, `accent` @ 8% (light) / 10% (dark) bg.
- Top row: 26 dp brand glyph (corner `radius.sm`) + 16 dp radio dot (active = filled `accent`).
- Bottom: provider name (body.sm) + mono.xs model line. If >1 model, show a `+N` pill on the right.

### 3.11 Model picker row
- Stacked vertically (no scroll), gap 8 dp.
- Idle: `card` bg, 1 dp `cardEdge` border.
- Active: `accentSoft` bg, 1 dp `accent` border.
- Left: mono.sm model id + body.xs.italic note.
- Right: 18 dp radio dot.
- Corner: 12 dp; padding: 10 / 14 dp.

### 3.12 Behavior row (list-form, no card chrome)
- Inline row, padding `14 dp` top/bottom.
- Bottom hairline divider (`cardEdge`).
- Title: body.md (`ink`). Sub: body.sm.italic (`inkSoft`).
- Trailing slot: Toggle, or mono.sm value + 14 dp chevron in `accent`.
- "Briefing time" row is **indented** behind a 2 dp left rule in `accentSoft` to nest it under the notification toggle it depends on.

### 3.13 Toggle
- Track: 44 × 26 dp pill. On = `accent`, Off = ink @ 18% (light) / sand @ 20% (dark).
- Thumb: 22 dp white circle, 2 dp inset, `0 1 3 rgba(0,0,0,0.18)` shadow.
- Slide: 180 ms standard.

### 3.14 Dialog / modal sheet (TimePicker pattern)
- Width: container minus 16 dp gutter each side.
- Background: `card`, corner `radius.xxl` (22 dp), padding `22 dp` top / `18 dp` sides / `14 dp` bottom.
- Scrim: `#04161E` @ 55%, 2 px backdrop blur.
- Header: title.md (Display).
- Footer: right-aligned Cancel / Set tertiary buttons, 28 dp gap.

### 3.15 Section label (eyebrow)
- mono.eyebrow, `inkMute`, 4 dp top / 10 dp bottom margin.
- Optional right-side meta (mono.xs, `inkMute`) — e.g. "gemini · 3 options".

### 3.16 Background wash
- Every full-screen surface gets a top-anchored radial wash:
  `radial-gradient(120% 60% at 50% 0%, bgWash 0%, bg 60%)`
- Render as a `Box` with this brush below the scroll content.

### 3.17 BreathingOrb (launch)
- 110 dp pearl with a Teal/Aqua pigment ring.
- Three stacked layers: outer halo (`accent` @ 20% radial, blur 8), pigment ring (`accent → accentDeep`, blur 6), pearl core (white/sand radial, mix-blend screen).
- Specular highlight at 35%/30%.
- All layers scale 1.0 → 1.06 on `motion.breath`.

### 3.18 PostCard (Saved Posts list item)
- Container: `card` bg, 1 dp `cardEdge` border, `radius.lg` (16 dp) corner, padding `14 dp` vertical / `14 dp` right / `16 dp` left, card shadow per §1.6.
- **Unread rule**: a 2 dp vertical bar in `accent`, flush-left at the inset (top 14 dp / bottom 14 dp), 2 dp corner. Replaces the legacy unread dot in the meta row.
- **Meta row** (top): leading 14 dp tile shows the post's first **category** initial in monochrome (1 dp `cardEdge` border, `inkSoft` letter, Sans 600 8.5 sp). Falls back to a middle dot `·` while categorization is pending so the slot is never empty. To the right: source name + author in body.sm `inkSoft` (e.g. *"Threads · @author"*). Right cluster contains timestamp (mono.xs `inkMute`) and a single overflow `⋯` button (see §3.20). The glyph used to mirror the source (T/X/O over a brand-colored fill); that was dropped because it duplicated the text and added nothing scan-wise compared to the category.
- **Body**: `body.md.read` (Newsreader 15.5 sp, 1.45 line, `ink`), `textWrap: pretty`. Default clamp 4 lines; tap card to expand or open detail.
- **Selected state** (multi-select, future): border switches to 1 dp `accent`, left rule hidden, 22 dp check-circle inset on the left replacing the rule (padding-left bumps to 50 dp).
- **Press feedback**: card scales to 0.99 with `motion.fast`; background tints to `accentSoft` @ 50% on press-hold (entry into swipe).
- NEVER place a destructive button (× / trash) inline on the card. Destructive lives in the swipe panel or overflow sheet only.

### 3.19 Swipe-to-action panel (PostCard)
Native swipe-to-reveal applied to every PostCard. Implement with Compose `SwipeToDismissBox` (or hand-rolled `pointerInput` + `Animatable`).

- **Trigger axis**: horizontal, left-only (right swipe is reserved for future Snooze).
- **Geometry**: a single 88 dp panel behind the card. Card translates by the same amount.
- **Delete panel**: bg `#E5484D` (Radix red 9 — the only hard red token in the system; do **not** swap to the brand palette here), white icon (§I `trash`, 18 dp, 1.7 stroke) over 10.5 sp Sans 700 label "Delete", stacked, 4 dp gap.
- **Thresholds** (two-tier):
  - 0 – 36 dp: rubber-band, panel not committed; release snaps back.
  - 36 – 66 dp: parks at the 88 dp reveal position. Tap Delete to commit. Tap card body to dismiss the panel.
  - > 66 dp (75% of reveal): full-bleed delete — the Delete panel expands across the row and commits on release with no tap needed. Card fades + collapses height over 280 ms `cubic-bezier(.2,.7,.2,1)`.
- **Motion**: card translation uses `cubic-bezier(.2,.7,.2,1)`, 280 ms on release. Underlay panel does not animate — it's revealed by the card moving off it.
- **Haptics**: light tick at 36 dp park, medium tick at 66 dp full-bleed threshold (Android `HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE`).
- **Undo**: every commit (tap or full-bleed) drops a Snackbar at the bottom: "Deleted. **Undo**" — 4 s timeout, `card` bg, `accent` Undo label. Until the snackbar dismisses, the item is soft-deleted; only then is it removed from the Notion sync queue.
- **A11y**: the panel exposes a TalkBack custom action ("Delete") on the parent card so screen-reader users skip the swipe gesture entirely.

### 3.20 Overflow button + action sheet (PostCard)
The single in-card affordance for non-swipe interaction.

**Trigger — overflow button**
- 26 × 22 dp tap target inside the meta row, right-aligned after the timestamp. Negative margin `-4 dp -6 dp -4 dp 0` so the icon visually aligns with the card edge while keeping the hit-target generous.
- Glyph: three horizontal 1.4 dp dots, `inkMute`, gap 7 dp. Rotates 90° to vertical on tablet layouts (width ≥ 600 dp).
- Press state: 6 dp corner background in `accentSoft`.

**Bottom action sheet**
- Anchored to bottom of screen, full-width minus 0 dp gutter. Background `card`, top corners `radius.xxl` (22 dp), shadow `0 -20 40 rgba(0,0,0,0.35) + 0 -1 0 cardEdge`.
- Drag handle: 36 × 4 dp pill, `inkMute` @ 40% opacity, 4 dp top / 14 dp bottom margin, horizontally centered.
- **Preview row** (optional, when sheet is invoked from a single card): body.sm.italic in `inkSoft`, padding `0 4 12`, truncated to 1 line with ellipsis — "…the original post's first ~50 chars."
- **Action rows**, top to bottom, each `14 dp` vertical padding, 14 dp icon → label gap, 1 dp `cardEdge` divider between rows:
  1. **Send to agent** — icon `send`, hint *"Add to today's briefing"* (body.xs.italic, `inkMute`, right-aligned).
  2. **Copy link** — icon `link`, hint *"Original post URL"* (disabled with hint *"No URL on this post"* when `post.url == null`).
  3. **Delete** — icon `trash`, label color `#E5484D` (no hint). Always last; always red.
- Archive is intentionally absent — Notion is the archive; the app inbox only tracks what's visible/actionable. Removing here drops the local cache entry and the Notion sync mirror.
- Sheet height auto-fits content; never use a fixed snap point. Tap outside or drag-down dismisses with a 240 ms ease-out.
- **Why Delete lives last + red**: thumb naturally lands on row 1 first; destructive is the longest reach. Mirrors iOS / Mail / Things 3.

### 3.21 Pending-sync strip
Replaces the legacy full-bleed sync card with a compact strip. Pin it to the top of the Saved screen above the search row when Notion is not yet configured *or* when ≥ 1 saves are queued.

- Container: 1 dp `gold` @ 32% border, `gold` @ 10% bg, `radius.md` (12 dp), padding `10 dp` vertical / `12 dp 14 dp` horizontal.
- Left: 8 dp `gold` dot + two-line text — title body.sm `ink` ("4 saves waiting") + sub body.sm.italic `inkSoft` ("Finish Notion sync to flush").
- Right: "Set up →" — body.sm 600 in `gold`, with a 13 dp arrow icon (1.8 stroke). The whole strip is tappable (one target).
- When count = 0 but Notion *is* configured: hide the strip entirely. When ≥1 saves queued but Notion configured: swap label to "Syncing 4 saves…" and dot becomes `statusGood`, pulsing.

### 3.22 Filter chip row
Horizontal pill chips that filter the post list. Used on Saved Posts (`All / Misc` for now; future categories e.g. `Articles / Videos` slot in next to Misc).

- Pill, padding `8 dp` vertical / `14 dp` horizontal — the extra vertical breathing room makes the row read as a coherent control strip rather than crowding the cards below.
- Idle: 1 dp `cardEdge` border, transparent bg, label body.sm 600 `inkSoft`.
- Active: 1 dp `accent` border, `accentSoft` bg, label `accent`.
- Inter-chip gap 8 dp. Horizontal scroll with hidden scrollbar; never wrap to a second line.
- **No counts.** Chips are a category list, not a status display — numeric badges add noise without helping the user decide which chip to tap.

### 3.23 Search row (passive)
A non-focused search bar that opens a full-screen search overlay on tap (do not let it inflate into an inline expanding state).

- Looks like a SettingsInput (§3.9) but single-line. `card` bg, 1 dp `cardEdge`, `radius.md` (12 dp), padding `10 dp 12 dp`, height 40 dp.
- Left: 15 dp `search` icon in `inkMute`. Then placeholder body.sm `inkMute` "Search content, author, source". No caret at rest.
- On tap: navigate to a full-screen search view (`Saved/Search.kt`) — do not animate the bar to fill the screen, just push.

---

## 4. Screens & behavior

### 4.1 Launch
- Centered orb at 38% vertical.
- Wordmark: display.lg, 56 dp below orb. Subhead: body.md.italic 12 dp below.
- Loading dots: 3 × 4 dp `inkMute`, gap 8 dp, 64 dp from bottom, staggered `motion.dot`.
- Footer credit: mono.footer, 28 dp from bottom.
- Animations: orb breathes from t=0; wordmark fades in at 500 ms, subhead 800 ms, dots 1000 ms, credit 1200 ms.
- Suggested duration on device: ~1.4 s minimum before route → Home.

### 4.2 Home — time-aware
Greeting, status, run button label, and briefing copy rotate by clock. Task list is constant.

| Slot       | Hours       | Greeting              | Subhead                                       | Run button         | Status            | Suggested theme |
|------------|-------------|-----------------------|-----------------------------------------------|--------------------|-------------------|-----------------|
| Dawn       | 05:00–08:59 | Good morning          | Fresh start. Three things matter today.       | Begin the day      | Briefing ready    | Light           |
| Midday     | 09:00–11:59 | Mid-morning           | Halfway through your sharpest hours.          | Refresh briefing   | Last run 09:02    | Light           |
| Afternoon  | 12:00–16:59 | Good afternoon        | Tidy the loose threads before dusk.           | Refresh briefing   | Resting           | Light           |
| Evening    | 17:00–20:59 | Good evening          | Wind down. Tomorrow is set.                   | Confirm tomorrow   | Set for 09:00     | Dark            |
| Night      | 21:00–04:59 | It's late             | Sleep on it. The agent has tomorrow.          | Plan tomorrow      | Quiet hours       | Dark            |

**Layout**
- Top bar: accent-haloed dot + "Agent" wordmark (title.sm) on the left; settings gear (18 dp, `inkSoft`) on the right.
- Greeting block (eyebrow date · display.md greeting · body.md.italic sub).
- Status row: StatusDot · status label (body.sm) + "Next run · …" mono.xs on the left; Run button on the right.
- Hairline divider.
- Briefing: mono.eyebrow "Briefing" + model/token meta · 2 dp left rule in `accent` · body.md.read quote.
- Section label "Today" (title.md) with mono.eyebrow item count.
- Stack of task Cards.
- Footer credit (mono.footer).

**Slot transitions**
- Greeting, status text, and run button each re-mount on slot change with `motion.medium` slotFade.
- Briefing quote re-mounts with `motion.slow` slotFade.

### 4.3 Settings
Three states, each renders the same chrome:
- `empty` — no values entered; Save disabled.
- `saved` — values show masked (`••••k7Qa`, `•••••••9F2c`); inline "Saved" confirmation under Save.
- `typing` — partial values; Save enabled.

**Sections (top → bottom)**
1. **Header** — back chevron + "Settings" (display.sm) + intro sentence (body.sm.italic).
2. **AI Provider** — horizontal scroll of provider tiles, edge-bleed (`-18 dp` margins so first tile aligns to screen edge on scroll).
3. **Model** — section label with `n options` meta + vertical stack of model rows.
4. **Credentials** — two SettingsInputs: `${provider} API key`, `Notion integration token`.
5. **Data source** — one SettingsInput: `Notion database (URL or ID)`.
6. **Save** — primary button + "Saved" / "Test Notion connection" tertiary.
7. **Behavior** — list of inline rows: Auto-run on launch (toggle), Daily briefing notification (toggle), Briefing time (value + chevron, **indented**), Quiet hours (toggle).
8. **Send test notification** — centered tertiary button.

**TimePicker overlay** — see §3.14. Two large hour/minute slots (display.lg Display, 76 dp tall, `radius.lg`) + AM/PM stack + 240 dp analog clock. Cancel / Set on the bottom-right.

### 4.4 Saved Posts
Inbox of posts the user shared into the app from the system share-sheet. The agent later syncs them to a Notion database. **No destructive button is ever drawn on the card itself** — see §3.18.

**Sections (top → bottom)**
1. **Header** — back chevron + "Saved" (display.sm) on the left. No item-count eyebrow on the right; this is a list, the cards self-evidence the count.
2. **Pending-sync state** — when Notion isn't yet configured *and* there are pending saves, the current build renders an **inline setup card** (paste a Notion page URL, button to provision the database, error/success states) instead of the slim §3.21 strip. The strip remains the documented future-direction; the inline card is kept because it lets the user complete setup without an extra screen push.
3. **Search row** (§3.23).
4. **Filter chips** (§3.22).
5. **Post list** — vertical stack of PostCards (§3.18) at 10 dp gap.

List padding matches every other screen: `18 dp` screen edges, `18 dp` top, `32 dp` bottom.

**Delete pattern — swipe + overflow**

The app exposes **two complementary paths** to delete a saved post. Together they cover discoverability and speed without putting a destructive button on the surface. There is **no Archive action** — Notion is the archive; deleting from the inbox only removes from the local feed and the sync queue.

| Path | Affordance | When to use | Implementation |
|---|---|---|---|
| **Primary — swipe** | None visible | The 90% case: one user, one post, one gesture | §3.19 Swipe-to-action panel. Two-tier: 36 dp park → tap; 66 dp full-bleed → instant commit. Single red Delete panel. |
| **Secondary — overflow** | `⋯` button in the meta row of every card | User who never discovered swipe; or wants Send-to-agent / Copy-link in the same gesture | §3.20 Overflow → bottom action sheet. Send / Copy / Delete (Delete last, red). |

Both paths commit through the same `deletePost(id)` function and both trigger the same Undo snackbar (4 s, `card` bg, `accent` Undo label). The snackbar is the only chance to recover — once it dismisses, the row is removed from the local cache and the Notion sync queue.

**Empty state** — when zero posts are saved: center the screen on a 96 dp `BreathingOrb` (§3.17) at 50% scale + display.sm title "Nothing saved yet" + body.md.italic sub "Share a post from any app to send it here." — no CTA button, the share-sheet is the entry.

**Loading state** — first load draws 3 skeleton PostCards: meta row replaced by a 14 dp × 90 dp `inkMute` @ 12% rounded rect, body replaced by three 14 dp × varying-width rects. No shimmer; subtle fade-in only (`motion.medium`).

**Behavior notes**
- Tapping anywhere on a card (not the overflow button, not the swipe handle area) opens the post detail view in a forward push, where the full thread/article is rendered and Delete sits in the detail's top-bar overflow. *Current build:* URL cards open externally via `ACTION_VIEW`; the in-app detail view is a future iteration.
- A successful swipe-Delete fades the card height to 0 over 280 ms `cubic-bezier(.2,.7,.2,1)`; sibling cards translate up via Compose `animateItemPlacement()`.

**Content enrichment from URL-only shares**
- Threads, X, Instagram, TikTok deliberately ship **only the post URL** in `Intent.EXTRA_TEXT`; the body never crosses the share boundary. The app fills it in post-save:
  1. **Scrape** the URL with `User-Agent: facebookexternalhit/1.1` (Threads and friends gate `og:description` behind a known-crawler UA — Chrome mobile gets a JS-only shell). Take the longest of `og:description` / `twitter:description` / `description` / `<title>`, decoded for HTML entities.
  2. **AI fallback** when scrape returns < 40 chars: Gemini 2.5 Flash with the `url_context` tool, prompted to return the verbatim body in the original language.
- The categorizer runs against the enriched body, not the URL, so summaries are about the actual post.

**URL hygiene**
- Cached + Notion URLs are stripped of tracking params at save time:
  - Threads (`*.threads.com` / `*.threads.net`): `xmt`, `slof`
  - X (`x.com` / `twitter.com`): `t`, `s`
  - Instagram: `igsh`, `igshid`
  - Universal: `utm_*` (prefix), `fbclid`, `gclid`, `mc_eid`, `mc_cid`, `_branch_match_id`
- Structurally-meaningful query params (e.g. `?v=`, `?page=`) are preserved — only the per-host allowlist of trackers is removed.

**Notion as backend**
- Notion is treated as the read-side source of truth. Every entry to the Saved screen triggers a paginated `databases/{id}/query` and merges the result into the local cache.
- **Merge rules** (key = `notionId`):
  - Notion wins on `content`, `categories`, `summary`, `status`, `savedAt`, `author`, `url`.
  - Local wins on `localId`, `pendingSync`, `pendingCategorization`.
  - **Outbox preserved**: posts with `pendingSync = true` and no `notionId` survive every fetch — they're the queue waiting to flush.
  - **Archive in Notion = soft delete in app**: notionIds present locally but absent from the remote list drop on next fetch.
  - **Remote-only rows** (created or imported directly in Notion) are appended with a fresh `localId`.
- **Taxonomy is rebuilt** each fetch from the union of merged posts' categories plus the `Misc` seed. Deletions of a category value in Notion therefore remove the corresponding filter chip in the app on next entry.
- **Editing in Notion** — rename, retag, add a summary, archive — propagates back into the app on the next Saved-screen entry without any explicit "sync" affordance. Conflict policy is last-write-wins on a per-field basis; concurrent local-AI + Notion-edit races resolve to Notion (the AI never re-runs on a post that already has categories).

**Error surfacing**
- Notion's HTTP client doesn't `expectSuccess` — 4xx responses come back as parseable JSON envelopes. A `throwIfNotionError()` guard checks `object == "error"` on every response and throws with the real `{code, message}` so the UI shows e.g. *"Notion createDatabase · object_not_found: Could not find page with ID …"* instead of swallowing failures under a generic "no id returned" fallback.

---

## 5. Content & copy rules

- Address the user by first name in greetings (e.g. "Good morning, Luna.") — but Luna is the placeholder in this design; replace with the signed-in user.
- Briefing copy is 1–2 sentences, action-oriented, no marketing voice. Past tense for evening/night, imperative for dawn/midday.
- Times use lowercase `am`/`pm` in body text, uppercase in chips and the picker.
- Source attribution at footer reads: `Powered by ${model} · ${integration} MCP`.
- Token counts appear inline with the model name (e.g. `gemini-2.5 · 312 tok`) in mono.xs, separated by `·`.
- Use `·` (middle dot) as the universal separator, not `|` or `–`.
- Never use emoji.

---

## 6. Accessibility

- Minimum tap target: 44 dp (run button is 38 dp tall but ≥80 dp wide — keep it). Behavior rows ≥48 dp inclusive of padding.
- Text contrast (WCAG AA):
  - Light: `ink` on `bg` = 11.3 : 1. `inkSoft` on `bg` = 5.6 : 1. `inkMute` on `bg` = 3.2 : 1 — use only for non-essential meta ≥13 sp.
  - Dark: `ink` on `bg` = 11.6 : 1. `inkSoft` on `bg` = 8.4 : 1. `inkMute` on `bg` = 4.1 : 1.
- Motion: respect `Settings.Global.ANIMATOR_DURATION_SCALE` — when 0, swap orb pulse / dot pulse / slot fade for instantaneous transitions.
- Dynamic type: never scale by < 1.0×; cap at 1.4× to keep the briefing card readable on small phones.
- Status dot must always be paired with a label — color is never the only signal.

---

## 7. Folder layout (recommended)

```
app/
├─ src/main/
│  ├─ java/.../ui/
│  │  ├─ theme/
│  │  │  ├─ AgentTheme.kt
│  │  │  ├─ Color.kt
│  │  │  ├─ Type.kt
│  │  │  └─ Shape.kt
│  │  ├─ components/
│  │  │  ├─ AgentCard.kt
│  │  │  ├─ PriorityChip.kt
│  │  │  ├─ StatusDot.kt
│  │  │  ├─ SourceLogo.kt
│  │  │  ├─ SettingsInput.kt
│  │  │  ├─ ProviderTile.kt
│  │  │  ├─ ModelRow.kt
│  │  │  ├─ BehaviorRow.kt
│  │  │  ├─ AgentToggle.kt
│  │  │  ├─ BreathingOrb.kt
│  │  │  ├─ PostCard.kt            ← §3.18
│  │  │  ├─ SwipeActions.kt        ← §3.19 (Delete underlay)
│  │  │  ├─ PostActionSheet.kt     ← §3.20
│  │  │  ├─ PendingSyncStrip.kt    ← §3.21
│  │  │  ├─ FilterChipRow.kt       ← §3.22
│  │  │  └─ SearchRow.kt           ← §3.23
│  │  └─ screens/
│  │     ├─ LaunchScreen.kt
│  │     ├─ HomeScreen.kt
│  │     ├─ SettingsScreen.kt
│  │     └─ SavedScreen.kt         ← §4.4
│  └─ res/
│     ├─ values/{colors,dimens,strings,themes}.xml
│     └─ font/{instrument_serif,newsreader,inter,ibm_plex_mono}_*.ttf
└─ docs/
   └─ DESIGN_SYSTEM.md   ← this file
```

---

*Source-of-truth visuals live in `Morning Agent.html`. When a token here disagrees with that file, the HTML wins — update this doc.*
