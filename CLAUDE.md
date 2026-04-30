# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew :androidApp:assembleDebug        # Build debug APK
./gradlew :androidApp:assembleRelease      # Build release APK
./gradlew :shared:build                    # Build KMP shared module
```

No tests exist yet (Phase 1 is UI-only; tests come in Phase 2). No linter is configured.

## Project Overview

**Morning Agent** is a personal Android app that fetches high-priority tasks from Notion, gets Gemini AI advice for each, and delivers a morning briefing via push notification and in-app UI. Built by Luna as a personal tool — optimize for clarity and craft, not enterprise patterns.

**Read `UI_PROTOTYPE.md` for the full visual design spec.** It is the authoritative reference for colors, typography, spacing, animation timelines, and component behavior.

## Development Phase

**Phase 1 (CURRENT) — UI Prototype.** Single screen, Jetpack Compose, mock data only. No real API calls, no scheduling, no network. Goal: lock the visual direction before wiring real systems.

**Phase 2 (FUTURE — do not implement yet):** Koog agent + Gemini API, Notion MCP over SSE, WorkManager scheduling, EncryptedSharedPreferences for token storage.

When working in Phase 1, leave Phase 2 wiring as `TODO()` stubs with clear comments. Do not silently mock real network calls. Phase 2 dependencies are commented out in `androidApp/build.gradle.kts`.

## Architecture

**KMP structure with Android-only target.** The `shared/` module exists for future portability but is currently empty. All active code lives in `androidApp/`.

**Navigation:** No Navigation Compose — `MorningAgentApp.kt` manages screen state with a simple `enum Screen { Launch, Home, Settings }` and `AnimatedContent` cross-fade.

**Data flow:** `HomeViewModel` holds `HomeUiState` (sealed interface: Loading/Success/Empty/Error) using Compose `mutableStateOf`. In Phase 1, it returns mock data from `PreviewData.kt` with a simulated 1.5s delay for "Run Now".

**Phase 2 stubs** already exist as files with `TODO()` bodies:
- `data/AgentRepository.kt` — will call Koog agent (Gemini + Notion MCP)
- `data/secure/TokenStore.kt` — will use EncryptedSharedPreferences
- `worker/MorningAgentWorker.kt` — will invoke agent on WorkManager schedule

## Key Tech Choices

- Gradle Kotlin DSL with version catalog (`gradle/libs.versions.toml`)
- Jetpack Compose with Material 3, dark theme only (no light mode)
- Min SDK 26, Target SDK 35, Kotlin 2.0+, JVM target 17
- `androidx.core:core-splashscreen` for system splash → compose LaunchScreen handoff
- Google Fonts provider for Inter + JetBrains Mono (no bundled TTFs)
- `kotlinx-datetime` for `Instant` in data models

## Style Rules

1. **Composables are small.** If a composable is over ~80 lines, split it.
2. **No hardcoded colors** — always reference `MaterialTheme.colorScheme.*` or custom theme tokens.
3. **No hardcoded strings** — use `stringResource()`. (Lenient for prose, strict for labels.)
4. **One file per top-level composable.** `TaskCard` lives in `TaskCard.kt`, not buried in `HomeScreen.kt`.
5. **Always provide a `@Preview` for every component file.** Wrap previews in `MorningAgentTheme {}`.
6. **Comment any `TODO()` with the phase it belongs to** (`// TODO(Phase 2): wire Notion MCP`).
7. **No emojis in code or strings** unless explicitly part of the design spec.
8. **Don't over-engineer.** No DI framework, no use-case classes, no Clean Architecture layers. Repository → ViewModel → Composable is enough.

## Out of Scope (Phase 1)

Auth flows, multi-language, tablet layouts, deep linking, analytics, crash reporting, tests.

When in doubt, ask before guessing. This is a craft project — quality over speed.
