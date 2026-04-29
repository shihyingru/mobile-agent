# Morning Agent — UI Prototype Spec

> Android prototype — Launch Screen + single Home Screen. Built with Jetpack Compose. Dark, focused, intelligence-dashboard aesthetic.

---

## Design Philosophy

**Mood:** Calm command center. The user wakes up, glances at one screen, and instantly knows what their day looks like and what the agent has prepared for them.

**Visual reference:** Linear's dark theme meets a meditation app. Generous whitespace, single accent color, monospace for data, soft sans-serif for prose.

---

## Color System

```
Background      #0A0A0F   (near-black, slight blue cast)
Surface         #14141C   (card surfaces)
Surface Raised  #1C1C28   (elevated cards)
Border          #26263A   (1dp dividers)

Text Primary    #E8E8F0
Text Secondary  #8B8BA8
Text Muted      #5A5A75

Accent          #A78BFA   (soft violet — agent identity)
Accent Glow     #A78BFA22 (translucent for highlights)

Priority High   #F87171   (warm red)
Priority Mid    #FBBF24   (amber)
Priority Low    #6EE7B7   (mint)

Success         #4ADE80
Error           #EF4444
```

## Typography

```
Display    Inter, 32sp, weight 600, tracking -0.5
Headline   Inter, 22sp, weight 600
Title      Inter, 17sp, weight 500
Body       Inter, 15sp, weight 400, line-height 22
Label      Inter, 12sp, weight 500, uppercase, tracking 1.5
Mono       JetBrains Mono, 13sp (timestamps, task IDs)
```

---

## Launch Screen (`LaunchScreen.kt`)

**Purpose:** First impression. Sets the mood — calm, intentional, almost ritualistic. The user sees this for ~1.2 seconds while the app warms up (loads cached briefing, checks WorkManager schedule status). It should never feel like a delay — it should feel like a deep breath.

**Visual concept:** A single soft violet **breathing orb** centered on a pure dark background, with the app wordmark fading in below it. The orb has a gentle pulse animation that mirrors the agent status dot on the home screen — subliminally tying the brand together.

```
┌─────────────────────────────────┐
│                                 │
│                                 │
│                                 │
│                                 │
│              ◯                  │  ← Breathing orb (96dp)
│           (soft glow)           │     accent color, animated
│                                 │
│                                 │
│         Morning Agent           │  ← Wordmark (Display style)
│                                 │
│      Your day, prepared.        │  ← Tagline (Body, secondary)
│                                 │
│                                 │
│                                 │
│                                 │
│           ─ ─ ─                 │  ← Optional: thin progress
│                                 │     indicator near bottom
│                                 │
└─────────────────────────────────┘
```

### Layout Specs

- **Background:** `#0A0A0F` (same as home — no jarring transition)
- **Orb:** centered horizontally, ~38% from top vertically
  - Diameter: 96dp
  - Fill: radial gradient — center `#A78BFA` (full opacity) → edge `#A78BFA00`
  - Outer glow: 32dp blurred halo, `#A78BFA33`
- **Wordmark:** "Morning Agent"
  - 24dp below orb
  - `Display` style (Inter, 32sp, weight 600, tracking -0.5)
  - Color `Text Primary` (#E8E8F0)
- **Tagline:** "Your day, prepared."
  - 8dp below wordmark
  - `Body` style (Inter, 15sp)
  - Color `Text Secondary` (#8B8BA8)
- **Loading indicator (optional):**
  - 64dp from bottom
  - Three small dots, 4dp each, 8dp spacing
  - Sequential fade animation (each dot pulses 0→1→0 with 200ms offset between dots)
  - Color `Text Muted` (#5A5A75)

### Animation Timeline

```
t=0ms      Screen appears. Background instantly. Everything else hidden.
t=100ms    Orb fades in (300ms ease-out) + scales from 0.8 → 1.0
t=400ms    Orb begins continuous breathing animation
           (scale 1.0 ↔ 1.06, 2.4s loop, ease-in-out)
t=500ms    Wordmark fades in + slides up 8dp (400ms ease-out)
t=800ms    Tagline fades in (300ms ease-out)
t=1000ms   Loading dots begin
t=1200ms   (App ready) Cross-fade transition to HomeScreen (250ms)
```

### Breathing Orb Animation Detail

```kotlin
// Conceptual — Compose implementation
val infiniteTransition = rememberInfiniteTransition()
val scale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.06f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1200, easing = EaseInOut),
        repeatMode = RepeatMode.Reverse
    )
)
```

The orb's glow opacity pulses in sync with the scale — slightly brighter at the peak. Subtle. The user shouldn't consciously notice it; they should just feel something living and calm.

### Behavior Rules

- **Minimum display time:** 1.2 seconds. If the app loads faster, hold the screen until 1.2s elapsed. This protects the ritual feeling.
- **Maximum display time:** 3 seconds. After 3s, transition regardless of load state — show error banner on home if needed.
- **Skip on cold-launch only:** From a warm start (app already in memory), skip the launch screen entirely. Resume directly to home.
- **No tap-to-skip:** The launch screen is not interactive. It is intentionally a moment.

### Empty / First Launch Variant

For the very first app open (no API keys configured yet), the launch screen behaves the same — but transitions to a one-time onboarding screen instead of home. **Onboarding flow is out of scope for this prototype**; just transition to home with mock data.

### Composable Structure

```
LaunchScreen.kt
├── Box (fillMaxSize, background = Background)
│   ├── Column (centered, vertical)
│   │   ├── BreathingOrb()           ← own composable
│   │   ├── Spacer(24dp)
│   │   ├── Text("Morning Agent")    ← wordmark
│   │   ├── Spacer(8dp)
│   │   └── Text("Your day, prepared.")
│   └── LoadingDots()                ← bottom-aligned
└── (effect launches transition timer)
```

### Splash Screen API Integration (Android 12+)

Use the modern `androidx.core:core-splashscreen` library so the system-level splash hands off cleanly to our compose `LaunchScreen`. The system splash uses the same background color and a static version of the orb (no animation possible at system level), then `installSplashScreen()` keeps it visible until our `LaunchScreen` is ready to paint, eliminating any white flash.

```xml
<!-- themes.xml -->
<style name="Theme.MorningAgent.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/background</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_orb</item>
    <item name="postSplashScreenTheme">@style/Theme.MorningAgent</item>
</style>
```

This way: **system splash → compose `LaunchScreen` → home**, all on the same dark canvas, with the orb being the visual anchor throughout.

---

## Home Screen Layout (`HomeScreen.kt`)

The home screen is **one scrollable screen**. Top to bottom:

```
┌─────────────────────────────────┐
│ [Status Bar Area]               │
│                                 │
│  ◐ Morning, Luna       [⚙]     │  ← Header (greeting + settings)
│  Tuesday, April 28              │
│                                 │
│  ┌───────────────────────────┐  │
│  │  AGENT STATUS             │  │  ← Status Card
│  │                           │  │
│  │  ●  Active                │  │
│  │  Next run · Tomorrow 9:00 │  │
│  │                           │  │
│  │  [ Run Now ]              │  │
│  └───────────────────────────┘  │
│                                 │
│  TODAY'S BRIEFING               │  ← Section label
│  Generated 9:02 AM              │
│                                 │
│  ┌───────────────────────────┐  │
│  │ ▎ 3 high-priority tasks   │  │  ← Briefing Summary Card
│  │   from Notion             │  │
│  │                           │  │
│  │  "Focus on the SDK doc    │  │
│  │   first — it unblocks two │  │
│  │   downstream tasks..."    │  │
│  └───────────────────────────┘  │
│                                 │
│  TASKS                          │
│                                 │
│  ┌───────────────────────────┐  │
│  │ ● HIGH                    │  │  ← Task Card 1
│  │ Write SDK documentation   │  │
│  │                           │  │
│  │ TIP                       │  │
│  │ Start with the public API │  │
│  │ surface. Skip examples    │  │
│  │ until structure is solid. │  │
│  │                           │  │
│  │ ⏱ Est. 2h · Notion ↗     │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ ● HIGH                    │  │  ← Task Card 2
│  │ Review PR #482            │  │
│  │ ...                       │  │
│  └───────────────────────────┘  │
│                                 │
│  ┌───────────────────────────┐  │
│  │ ● MID                     │  │  ← Task Card 3
│  │ Reply to design feedback  │  │
│  │ ...                       │  │
│  └───────────────────────────┘  │
│                                 │
│  Powered by Gemini · Notion MCP │  ← Footer
│                                 │
└─────────────────────────────────┘
```

---

## Component Specs

### 1. Header
- 24dp horizontal padding, 32dp top padding
- Greeting uses `Display` style; date uses `Body` color `Text Secondary`
- Settings icon (`Icons.Rounded.Settings`) tappable, 48dp hit target

### 2. Agent Status Card
- Background `Surface`, rounded 16dp, padding 20dp
- Status dot: 8dp circle, `Accent` color, soft glow effect (drop shadow with `Accent Glow`)
- "Active" label in `Title` style
- "Next run" in `Body` `Text Secondary`, monospace timestamp
- "Run Now" button: filled, `Accent` background, `Surface` text, rounded 12dp, height 44dp

### 3. Section Labels
- Style: `Label` (uppercase, tracked)
- Color `Text Muted`
- 32dp top margin, 12dp bottom margin

### 4. Briefing Summary Card
- Background `Surface Raised`, rounded 16dp, padding 20dp
- Left vertical bar (`Accent`, 3dp wide, full height)
- Headline (count + source) in `Title`
- Quoted advice text in `Body`, italic, `Text Primary`

### 5. Task Card
- Background `Surface`, rounded 16dp, padding 20dp
- Margin between cards: 12dp
- Priority pill: small rounded rect, 8dp dot + uppercase label, colored by priority
- Task title: `Headline` style
- "TIP" label in `Label` style, `Accent` color
- Tip body: `Body` style
- Bottom row: time estimate (mono) + Notion link (with arrow icon, `Accent`)

### 6. Footer
- Centered, `Label` style, `Text Muted`
- 24dp top padding, 32dp bottom padding

---

## Empty / Loading / Error States

**Empty (no tasks today):**
- Status card shows "Nothing high-priority today"
- Briefing card hidden
- Show single illustration card with text: "Enjoy your day, Luna."

**Loading (agent running):**
- Status dot pulses
- "Run Now" button shows spinner
- Skeleton cards (shimmer on `Surface`)

**Error (Notion auth expired / Gemini failed):**
- Inline banner above status card, `Error` background at 10% opacity
- Text: "Couldn't reach Notion. Check your token in Settings."
- Action: "Open Settings"

---

## Microinteractions

- Status dot: subtle 2-second pulse animation when active
- Task card press: scale 0.98, 100ms
- Run Now button: ripple in `Accent` color
- Card entry: stagger fade-in (50ms delay per card) on first load
- Pull-to-refresh: triggers manual agent run

---

## Settings Screen (out of scope for prototype)

Reachable from header gear icon. Not implemented in this prototype, but stub composable should exist. Will hold:
- Gemini API key input (masked)
- Notion connection status + reconnect button
- Schedule time picker (default 9:00 AM)
- Test run button

---

## Compose Implementation Notes

- Use `Material3` theme as base, override colors via `darkColorScheme()`
- App entry composable manages a simple two-state navigation: `LaunchScreen` → `HomeScreen` (use a `var screen by remember { mutableStateOf(Screen.Launch) }` and a `LaunchedEffect` timer for the prototype — no Navigation Compose needed yet)
- Single `LazyColumn` for the home screen scroll — items: header, status, section label, briefing, section label, task list, footer
- Each task card is its own `@Composable` `TaskCard(task: Task)`
- Mock data lives in a `PreviewData.kt` file so the prototype renders fully without any real backend
- Wrap everything in `MorningAgentTheme {}` composable for color/typography tokens
- The breathing orb on the launch screen and the agent status dot on home both use the same `AccentPulse` modifier — extract this as a reusable Compose modifier to keep brand consistency

---

## What Claude Code Should NOT Do (for this prototype)

- Do **not** wire up actual Gemini API calls
- Do **not** wire up actual Notion MCP calls
- Do **not** implement WorkManager scheduling yet
- The prototype is **UI-only**, fed by mock data from `PreviewData.kt`
- Keep all logic stubs as `TODO()` with clear comments

The point of this prototype is to validate the look and feel before wiring real systems. Real integration follows in a later phase.
