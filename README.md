# Morning Agent

A personal Android app that fetches high-priority tasks from Notion at a user-picked time of day, asks Gemini or Claude for efficiency tips, and delivers a single morning briefing.

## Stack

Kotlin 2.3 · Jetpack Compose (Material 3) · Compose BOM 2026.04.01 · Koog 0.8 · Ktor 3.4 · WorkManager · Preferences DataStore + Tink AEAD. KMP-shaped, Android target only. Min SDK 26, target SDK 35, single-activity, edge-to-edge.

## Build

```
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug   # to attached device / emulator
```

Open the project in Android Studio Koala or newer.

## Configuration

API keys (provider key — Gemini or Claude — Notion integration token, Notion database URL/ID) are entered in the in-app Settings screen and persisted in Preferences DataStore with per-value Tink AEAD encryption. Nothing is checked in.

The Notion database needs these columns:

| Column     | Type     | How it's used                                                                 |
|------------|----------|-------------------------------------------------------------------------------|
| `Task`     | title    | Task title                                                                    |
| `Priority` | select   | Filtered to `High`; `Medium` / `Low` mapped from the same select for display |
| `Status`   | status   | Filtered to `!= Done`                                                         |
| `Date`     | date     | Filtered to `on_or_before` today; results sorted descending                   |
| `Area`     | relation | Linked page's title is shown as a tag on each task card                       |

The Notion integration must be shared with **both** the Tasks database and the Areas database — they're separate connections in Notion's `...` → Connections menu. If Area tags don't appear despite tasks having relations set, the Areas DB likely isn't connected to the same integration.

In Settings, a provider picker (Gemini / Claude / OpenAI greyed as "Coming soon") sits above a model picker — Flash-Lite / Flash / Pro for Gemini, Haiku 4.5 / Sonnet 4.6 / Opus 4.6 for Claude. The active model resolves fresh on each Run Now, and each provider's key + model choice persist independently so flipping between providers doesn't lose state.

## Daily briefing

Settings → **Daily briefing notification** toggle enables a background WorkManager job that runs the agent at a user-picked time of day and posts the briefing as a notification. The time is set via a Material 3 `TimePicker` dialog (default 9:00 AM). A **Send test notification** button posts a stub on the same channel so you can preview the visual and confirm `POST_NOTIFICATIONS` without burning a Gemini call. Tapping the briefing notification opens the app directly on Home — no Launch screen detour.

The home header reflects the device clock: the greeting + italic tagline + run-button label + briefing quote all cycle through 5 time slots — `Dawn` (5–9), `Midday` (9–12), `Afternoon` (12–17), `Evening` (17–21), `Night` (21–5, wraps midnight). The palette flips Light ↔ Dark across the evening boundary so the app physically dawns and dusks with the day. The date subtitle is live (`FRIDAY · MAY 15 · 2:23 PM` — uppercase mono, English locale), and the "Next run" row reads e.g. `tomorrow 9:00 PM` based on the picked time. The row hides entirely when the toggle is off.

## Docs

- [`CLAUDE.md`](./CLAUDE.md) — operational guidance (conventions, non-obvious decisions).
- [`UI_PROTOTYPE.md`](./UI_PROTOTYPE.md) — visual design spec.