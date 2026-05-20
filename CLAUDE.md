# CLAUDE.md — Morning Agent

> Operational brief for Claude Code. See `README.md` for setup, `UI_PROTOTYPE.md` for the visual spec.

## Identity

Morning Agent — Luna's personal Android app. Wakes at 9:00 AM, fetches high-priority Notion tasks, asks Gemini for efficiency tips, delivers a briefing notification. Personal tool, not a product — optimize for clarity and craft, not enterprise concerns.

## Non-obvious decisions

- **Kotlin 2.3 jvmTarget DSL.** The AGP-side `android { kotlinOptions {} }` block was removed in 2.3. `jvmTarget` lives in a top-level `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`. Keep that pattern.
- **META-INF excludes.** Koog's transitive deps (Ktor, Netty, OkHttp, AWS SDK) ship overlapping META-INF metadata. `packaging.resources.excludes` covers `INDEX.LIST`, `io.netty.versions.properties`, OSGI `MANIFEST.MF`, license/notice files. New conflicts → extend the block, don't drop deps.
- **Token storage = Preferences DataStore + per-value Tink AEAD.** Replaces the deprecated `androidx.security.crypto` EncryptedSharedPreferences. String values are Tink-sealed (base64 ciphertext); booleans/ints sit in DataStore natively. `TokenStore` keeps a synchronous public API via a `runBlocking` warm-up + in-memory `ConcurrentHashMap`, so callers don't deal with Flows. Tink keyset lives in `morning_agent_tink_keyset.xml`, wrapped by an Android Keystore master key.
- **Notion REST, not MCP.** Hosted Notion MCP rejected bearer tokens, so step 2 went REST direct via Ktor/OkHttp. `NotionTaskSource` is the swap seam for a future MCP implementation.
- **Notion DB schema convention.** `Task` (title) · `Priority` (select; filter on `High`, mapping `Medium`/`Low` from the same select) · `Status` (status; filter `!= "Done"`) · `Date` (date; filter `on_or_before` today, sort descending) · `Area` (relation; the task source resolves the linked page id to its title via `/v1/pages/{id}`, displayed as a tag on each card). `Estimated Time` is not part of the current schema — `Task.estimatedMinutes` defaults to 0 and the card hides the "Est." line when 0.
- **Notion integration must be shared with the Areas DB too.** Relations only carry linked-page IDs, and Notion silently filters out pages the integration can't see — so Areas come back as empty arrays even when set on the task. If Area tags don't render despite tasks having relations, the Areas DB needs to be connected to the same integration in its Notion `...` → Connections menu.
- **Model picker lives on Home, not Settings.** Switching model is a daily decision (today I want speed vs depth), not one-time configuration. Persists immediately via `TokenStore.saveGeminiModel`; resolved fresh on each `GeminiBriefingClient.generate()` call so the next Run Now picks it up without an app restart.
- **Area icon mapping is hardcoded English names.** `work` / `life` / `home` / `healthy` / `learn` map to specific Material rounded icons in `AreaTag`. If an area is renamed in Notion, the tag falls back to text-only by design — no broken icon, no theme-color guess.
- **Tokens not always available.** Gemini's `executor.execute` returns `Message.Assistant.metaInfo.totalTokensCount` *when* the model populates it. The `BriefingCard` footer hides the `· N tok` segment when 0, so missing usage data degrades silently.
- **Briefing time is user-chosen, not hardcoded.** `BriefingScheduler` reads `dailyBriefingHour/Minute` from `TokenStore` on every enqueue, then computes `initialDelay` to the next occurrence in the device timezone. If the user picks a time that's already passed today, the first run is tomorrow — no surprise mid-day fire when toggling on. Cancellations use `WorkManager.cancelUniqueWork(UNIQUE_WORK_NAME)`.
- **MainActivity uses KEEP, Settings uses UPDATE.** App launch (`BriefingScheduler.ensure`) re-enqueues with `ExistingPeriodicWorkPolicy.KEEP` so the existing scheduled run is preserved across cold starts. The Settings toggle/time picker uses `UPDATE` so the new `initialDelay` applies immediately. Calling `KEEP` on every launch is intentional — it's a no-op when the job already exists, but reseeds if WorkManager has dropped it.
- **POST_NOTIFICATIONS is requested lazily.** Only asked on launch when the user has the daily-briefing toggle on. Refusing doesn't break anything: the worker's `hasPostPermission()` check silently skips `notify()` and the briefing is still cached in `TokenStore.lastBriefingJson` for the in-app view.
- **Last briefing persisted in `TokenStore`, not a separate file.** Step 4's worker writes the resulting `Briefing` as JSON into the same Tink-sealed DataStore. Task titles can be personal — keep them encrypted at rest like the credentials. `AgentRepository.getLastBriefing()` returns null on parse failure so a schema change to `Task`/`Briefing` just re-runs the agent instead of crashing.
- **Home header reads the device clock, not stored state.** Greeting picks one of 4 buckets via `greetingResFor()` — weekend wins over hour-of-day so Saturday morning reads `Slow day, Luna` instead of `Morning, Luna`. Late-night (22:00–04:59) folds into evening to keep the palette at the four words Luna asked for. The subtitle uses `DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.ENGLISH)` — locale forced to English so a zh-locale phone doesn't translate "Monday · May 11" mid-render.
- **`AgentStatusCard.nextRunLabel` is nullable, not "off" copy.** When the daily-briefing toggle is off, the row is hidden entirely rather than showing a placeholder. A stale fallback like "tomorrow 09:00" misleads the user when nothing is actually scheduled. Recomputed per recomposition so it tracks Settings changes via the existing AnimatedContent navigation seam — no LifecycleObserver, no Flow.
- **Test-notification button shares the worker's exact code path.** `MorningAgentWorker.Companion.postSampleNotification(context)` builds the same channel/icon/PendingIntent the scheduled fire uses, with stubbed title+body. Lets Luna preview the visual and confirm `POST_NOTIFICATIONS` without burning a Gemini call or waiting for the picked time. Visible in Settings only when the daily toggle is on — sharing visibility with the time row.
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
