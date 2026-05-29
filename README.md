# Morning Agent

Luna's personal Android app. It fetches high-priority Notion tasks at a user-picked time of day, asks Gemini or Claude for efficiency tips and a few reversible task actions, and delivers a single morning briefing as a notification — with an optional evening wrap-up. It also doubles as a read-it-later inbox: anything shared into the app from another app is cached, auto-categorized by an LLM, and mirrored to Notion.

Personal tool, not a product — optimized for clarity and craft, not enterprise concerns.

## Stack

Kotlin 2.3.21 · AGP 8.7.0 · Jetpack Compose (Material 3) · Compose BOM 2026.04.01 · Koog 0.8 (LLM orchestration) · Ktor 3.4 (Notion REST) · WorkManager 2.10 · Preferences DataStore 1.1 + Tink AEAD 1.18 (encrypted storage) · Coil 2.7 (image loading) · kotlinx-serialization / kotlinx-datetime.

KMP-shaped, Android target only. Min SDK 26, compile/target SDK 35. Single-activity, edge-to-edge, no DI framework, no navigation library (screen switching is a single `AnimatedContent`).

## Build

```
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug   # to an attached device / emulator
```

Open the project in Android Studio Koala or newer.

## Features

### Daily briefing & evening reflection

Two independent WorkManager jobs, each toggled and time-picked in Settings:

- **Daily briefing** (default 9:00 AM) runs the agent on today's high-priority Notion tasks and posts a one-paragraph briefing as a notification. Tapping it opens straight to Home — no Launch screen detour.
- **Evening reflection** (default 7:00 PM) is a mirror job that produces a day-wrap reflection instead of a forward-looking briefing, favouring carry-over of stale items to tomorrow.

Each run can include up to **two reversible proposed actions** on your tasks — `mark_done`, `reschedule`, or `change_priority`. They surface on Home as suggestions with **Apply** (writes the change to Notion) / **Ignore**. A **Send test notification** button posts a stub on the same channel so you can preview the visual and confirm `POST_NOTIFICATIONS` without burning an LLM call.

### Provider & model picker

A provider picker (**Gemini** / **Claude**) sits above a model picker on Home, since the speed-vs-depth trade-off is a daily decision:

- Gemini — Flash-Lite / Flash / Pro
- Claude — Haiku 4.5 / Sonnet 4.6 / Opus 4.6

The active model resolves fresh on each run, and each provider keeps its own key + model choice so flipping between providers loses no state. Adding a third provider is one enum entry plus a `BriefingGenerator` implementation — no schema migration.

### Share-to-save (Saved Posts)

Morning Agent registers in the system share sheet for any `text/plain` share (Threads, Twitter/X, browsers, notes apps…). `ShareReceiverActivity` is windowless (`Theme.NoDisplay`) so you stay inside the source app — it caches the post, then in an app-scoped coroutine:

1. fetches the linked page body + `og:image` for a thumbnail,
2. categorizes it with the **cheapest** LLM tier (Gemini Flash-Lite preferred, Claude Haiku fallback) and writes a short "why this mattered" summary,
3. mirrors it to a **Shared Posts** Notion database (auto-created on first sync).

The local cache is the source of truth; Notion is a mirror. Posts saved before the DB exists carry `pendingSync` / `pendingCategorization` flags and are flushed on a later save. Browse everything in the **Saved Posts** screen; manage the category taxonomy (which grows organically from a `Misc` seed) in Settings.

### Temp plan

A lightweight, time-boxed plan (name + start/end dates + a task list) for short stretches of focused work. It shows as a progress card on Home — progress reflects *time elapsed*, not tasks completed — and opens a dedicated management screen for editing the title, dates, and tasks. Individual plan tasks can be **promoted** into the main Notion Tasks database.

### Time-of-day theming

The whole home surface (greeting, italic tagline, run-button label, briefing quote) cycles through five slots — Dawn (5–9), Midday (9–12), Afternoon (12–17), Evening (17–21), Night (21–5, wraps midnight) — with a live date subtitle. The palette flips Light ↔ Dark across the evening boundary, so the app physically dawns and dusks with the day. The wall clock is ticked once a minute so slot transitions land live.

### Languages

English and Traditional Chinese (`zh-TW`), switchable in-app (Settings → language); the choice is applied per-process via `LocaleHelper`.

## Configuration

All secrets — the active provider's API key (Gemini or Claude), the Notion integration token, and the Notion database URL/ID — are entered in the in-app **Settings** screen and persisted in Preferences DataStore with **per-value Tink AEAD encryption**. Nothing is checked in.

### Notion Tasks database

| Column     | Type     | How it's used                                                                 |
|------------|----------|-------------------------------------------------------------------------------|
| `Task`     | title    | Task title                                                                    |
| `Priority` | select   | Filtered to `High`; `Medium` / `Low` mapped from the same select for display |
| `Status`   | status   | Filtered to `!= Done`                                                         |
| `Date`     | date     | Filtered to `on_or_before` today; results sorted descending                   |
| `Area`     | relation | Linked page's title is shown as a tag on each task card                       |

The integration must be shared with **both** the Tasks database and the **Areas** database — they're separate connections in Notion's `...` → Connections menu. If Area tags don't appear despite tasks having relations set, the Areas DB likely isn't connected to the same integration.

### Shared Posts database

Created automatically (titled "Shared Posts") the first time a share is mirrored, under the same integration. Columns: `Content` (title), `Source`, `Author`, `URL`, `Categories` (multi-select), `Summary`, `Saved At` (date), `Status`.

## Docs

- [`CLAUDE.md`](./CLAUDE.md) — operating principles, conventions, and non-obvious decisions.
- [`UI_PROTOTYPE.md`](./UI_PROTOTYPE.md) — visual design spec.
- [`design/design.md`](./design/design.md) — temp-plan screen design notes.
