# Morning Agent

A personal Android app that fetches high-priority tasks from Notion at 9:00 AM, asks Gemini for efficiency tips, and delivers a single morning briefing.

## Status

Phase 2 shipped.

- ✅ Phase 1 — UI prototype
- ✅ Step 1 — secure token storage + Settings
- ✅ Step 2 — Notion REST client + connection test
- ✅ Step 3 — Koog + Gemini agent (token surfacing, retry hint, model picker on Home, auto-run toggle, overdue+today Notion filter, Area tag with per-area icons)
- ✅ Step 4 — WorkManager daily briefing notification + time picker + time-aware home header

## Stack

Kotlin 2.3 · Jetpack Compose (Material 3) · Compose BOM 2026.04.01 · Koog 0.8 · Ktor 3.4 · WorkManager · EncryptedSharedPreferences. KMP-shaped, Android target only. Min SDK 26, target SDK 35, single-activity, edge-to-edge.

## Build

```
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug   # to attached device / emulator
```

Open the project in Android Studio Koala or newer.

## Configuration

API keys (Gemini key, Notion integration token, Notion database URL/ID) are entered in the in-app Settings screen and persisted with `EncryptedSharedPreferences`. Nothing is checked in.

The Notion database needs these columns:

| Column     | Type     | How it's used                                                                 |
|------------|----------|-------------------------------------------------------------------------------|
| `Task`     | title    | Task title                                                                    |
| `Priority` | select   | Filtered to `High`; `Medium` / `Low` mapped from the same select for display |
| `Status`   | status   | Filtered to `!= Done`                                                         |
| `Date`     | date     | Filtered to `on_or_before` today; results sorted descending                   |
| `Area`     | relation | Linked page's title is shown as a tag on each task card                       |

The Notion integration must be shared with **both** the Tasks database and the Areas database — they're separate connections in Notion's `...` → Connections menu. If Area tags don't appear despite tasks having relations set, the Areas DB likely isn't connected to the same integration.

On the home screen, a model picker (visible once a Gemini key is saved) lets you switch between `gemini-2.5-flash-lite` / `flash` / `pro` for the next Run Now. The pick persists across launches.

## Daily briefing

Settings → **Daily briefing notification** toggle enables a background WorkManager job that runs the agent at a user-picked time of day and posts the briefing as a notification. The time is set via a Material 3 `TimePicker` dialog (default 9:00 AM). A **Send test notification** button posts a stub on the same channel so you can preview the visual and confirm `POST_NOTIFICATIONS` without burning a Gemini call. Tapping the briefing notification opens the app directly on Home — no Launch screen detour.

The home header reflects the device clock: the greeting cycles `Morning / Afternoon / Evening / Slow day, Luna` (weekend wins over hour-of-day), the date subtitle is live, and the "Next run" row reads e.g. `today 9:00 PM` or `tomorrow 9:00 PM` based on the picked time. The row hides entirely when the toggle is off.

## Docs

- [`CLAUDE.md`](./CLAUDE.md) — operational guidance (conventions, phase plan, non-obvious decisions).
- [`UI_PROTOTYPE.md`](./UI_PROTOTYPE.md) — visual design spec.