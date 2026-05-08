# CLAUDE.md — Morning Agent

> Operational brief for Claude Code. See `README.md` for setup, `UI_PROTOTYPE.md` for the visual spec.

## Identity

Morning Agent — Luna's personal Android app. Wakes at 9:00 AM, fetches high-priority Notion tasks, asks Gemini for efficiency tips, delivers a briefing notification. Personal tool, not a product — optimize for clarity and craft, not enterprise concerns.

## Phase plan

One feature branch per slice, named `feature/<core-detail>`. Don't bring forward later steps; keep their seams as `TODO(Phase 2 stepN: …)` until their slice arrives.

- ✅ Phase 1 — UI prototype: Launch + Home, mock data, edge-to-edge with status-bar gradient scrim, breathing accent dot.
- ✅ Step 1 — `TokenStore` (EncryptedSharedPreferences) + Settings UI with masked fields.
- ✅ Step 2 — `NotionRestClient` against `api.notion.com/v1` (Bearer + `Notion-Version: 2022-06-28`) behind a `NotionTaskSource` interface; Settings adds the database field, intro paragraph, "Test Notion connection", and inline `****<last4>` saved-state mask. Drafts clear on back navigation.
- ⏳ Step 3 — Koog agent + Gemini wiring → real `AgentRepository.runAgent()`.
- ⏳ Step 4 — WorkManager 9:00 AM schedule + briefing notification.

## Non-obvious decisions

- **Kotlin 2.3 jvmTarget DSL.** The AGP-side `android { kotlinOptions {} }` block was removed in 2.3. `jvmTarget` lives in a top-level `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`. Keep that pattern.
- **META-INF excludes.** Koog's transitive deps (Ktor, Netty, OkHttp, AWS SDK) ship overlapping META-INF metadata. `packaging.resources.excludes` covers `INDEX.LIST`, `io.netty.versions.properties`, OSGI `MANIFEST.MF`, license/notice files. New conflicts → extend the block, don't drop deps.
- **EncryptedSharedPreferences pinned at `security-crypto:1.1.0-alpha06`.** Officially deprecated in `1.1.0-alpha07+`. Migrate to DataStore + Tink only when forced.
- **Notion REST, not MCP.** Hosted Notion MCP rejected bearer tokens, so step 2 went REST direct via Ktor/OkHttp. `NotionTaskSource` is the swap seam for a future MCP implementation.
- **Notion DB schema convention.** `Task` (title) · `Priority` (select; filter on `High`, mapping `Medium`/`Low` from the same select) · `Status` (status; filter `!= "Done"`) · `Date` (date; filter `on_or_before` today, sort descending) · `Area` (relation; the task source resolves the linked page id to its title via `/v1/pages/{id}`, displayed as a tag on each card). `Estimated Time` is not part of the current schema — `Task.estimatedMinutes` defaults to 0 and the card hides the "Est." line when 0.
- **Settings ViewModel is activity-scoped.** No NavHost — `MorningAgentApp` switches screens via `AnimatedContent`. Drafts must be cleared explicitly on back (`BackHandler` + wrapped top-bar arrow → `clearDrafts()`), otherwise half-typed values leak across visits.
- **iOS target not configured.** KMP project shape for future portability, only `android()` enabled in `shared/build.gradle.kts`. `shared/src/iosMain/kotlin/.gitkeep` holds the folder open.

## Style rules

1. Composables under ~80 lines — split if larger.
2. No hardcoded colors. Use `MaterialTheme.colorScheme.*` or `MaterialTheme.morning.*` tokens.
3. Strings via `stringResource()` for labels; lenient on prose during prototype.
4. One file per top-level composable — `TaskCard` lives in `TaskCard.kt`, not buried in `HomeScreen.kt`.
5. Every component file gets a `@Preview` wrapped in `MorningAgentTheme {}`.
6. Every `TODO()` tagged with its phase: `// TODO(Phase 2 stepN: …)`.
7. No emojis in code or strings unless explicitly part of the design spec.
8. Don't over-engineer. No DI framework, no use-case classes, no Clean Architecture layers. Repository → ViewModel → Composable.

## Out of scope

Authentication flows, multi-language, tablet layouts, deep linking, analytics, crash reporting, tests (Phase 1 was visual-only; tests come with the agent slices).

When in doubt, ask before guessing. This is a craft project — quality over speed.
