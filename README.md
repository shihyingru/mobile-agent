# Morning Agent

A personal Android app that fetches high-priority tasks from Notion at 9:00 AM, asks Gemini for efficiency tips, and delivers a single morning briefing.

## Status

Phase 2 — real wiring, in progress.

- ✅ Phase 1 — UI prototype
- ✅ Step 1 — secure token storage + Settings
- ✅ Step 2 — Notion REST client + connection test
- ⏳ Step 3 — Koog agent + Gemini
- ⏳ Step 4 — WorkManager schedule + briefing notification

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

The Notion database needs columns: `Task` (title), `Priority` (select with `High` / `Medium` / `Low`), `Estimated Time` (number, optional). Extra columns are ignored.

## Docs

- [`CLAUDE.md`](./CLAUDE.md) — operational guidance (conventions, phase plan, non-obvious decisions).
- [`UI_PROTOTYPE.md`](./UI_PROTOTYPE.md) — visual design spec.