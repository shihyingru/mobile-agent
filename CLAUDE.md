# CLAUDE.md — Morning Agent

> Project brief for Claude Code. Read this file first. Then read `UI_PROTOTYPE.md` for the visual spec.

---

## Project Identity

**Name:** Morning Agent
**Owner:** Luna (sole developer)
**Goal:** A personal Android app that wakes up at 9:00 AM every morning, fetches the user's high-priority tasks from a Notion database, asks Gemini to generate efficiency advice for each task, and delivers it as a single push notification + a beautifully formatted in-app briefing.

This is a **personal tool**, not a product. Optimize for clarity and craft, not enterprise concerns.

---

## Stage of Development

**Phase 1 — UI Prototype.** ✅ Shipped. Launch + Home screens, mock data, edge-to-edge with status-bar gradient scrim, breathing accent dot.

**Phase 2 — Real wiring.** In progress, one slice per branch:
- ✅ **Step 1: secure token storage + Settings UI** (`feature/secure-token-storage`) — `EncryptedSharedPreferences` behind `TokenStore`, real `SettingsScreen` with masked fields and IME Next/Send wiring.
- ⏳ Step 2: Notion MCP client over SSE (Koog MCP).
- ⏳ Step 3: Koog agent + Gemini wiring → real `AgentRepository.runAgent()`.
- ⏳ Step 4: WorkManager 9:00 AM schedule + briefing notification.

When extending Phase 2, ship one slice per branch named `feature/<core-detail>`. Do not silently mock real network calls — keep unimplemented pieces as `TODO(Phase 2 stepN: …)` until their slice arrives.

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Build system | Gradle Kotlin DSL | `build.gradle.kts` everywhere |
| Project structure | **Kotlin Multiplatform** | But only Android target is implemented. iOS source set scaffolded as empty. |
| UI | Jetpack Compose (Material 3) | Single-activity app |
| Min SDK | 26 (Android 8.0) | EncryptedSharedPreferences requires 23+; we go higher for safety |
| Target SDK | 35 |  |
| Kotlin | 2.3+ | Required by Koog 0.8 metadata. `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` (the old AGP `kotlinOptions {}` was removed in Kotlin 2.3). |
| Compose BOM | 2026.04.01 | Compose Compiler 2.3 compatible |
| Agent framework | Koog (`ai.koog:koog-agents:0.8.0`) | Single artifact — Gemini client + MCP client. Phase 2. |
| LLM provider | Google Gemini | via Koog's Google client (Phase 2 step 3) |
| Tool integration | Notion MCP (SSE transport) | via Koog MCP client (Phase 2 step 2) |
| Background work | WorkManager (`androidx.work:work-runtime-ktx:2.10.0`) | Phase 2 step 4 |
| Secure storage | Android Keystore + `EncryptedSharedPreferences` (`androidx.security:security-crypto:1.1.0-alpha06`) | ✅ Phase 2 step 1 — `TokenStore`. ESP is officially deprecated in `1.1.0-alpha07+`; we'll migrate to DataStore + Tink only when something forces it. |
| Notifications | `NotificationCompat` | Phase 2 step 4 |

---

## Project Structure (KMP Skeleton)

```
morning-agent/
├── build.gradle.kts                    (root)
├── settings.gradle.kts
├── gradle.properties
├── shared/                             (KMP shared module — empty for now)
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/kotlin/          (shared Kotlin code, Phase 2)
│       ├── androidMain/kotlin/         (Android-specific impls, Phase 2)
│       └── iosMain/kotlin/             (iOS — leave empty, just create the folder)
├── androidApp/                         (Android application module)
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/luna/morningagent/
│       │   ├── MainActivity.kt
│       │   ├── MorningAgentApp.kt           (App composable + theme wrap)
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt
│       │   │   │   ├── Type.kt
│       │   │   │   └── Theme.kt
│       │   │   ├── launch/
│       │   │   │   ├── LaunchScreen.kt          (cold-start screen)
│       │   │   │   └── components/
│       │   │   │       ├── BreathingOrb.kt
│       │   │   │       └── LoadingDots.kt
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt            (the main screen)
│       │   │   │   ├── HomeViewModel.kt
│       │   │   │   └── components/
│       │   │   │       ├── AgentStatusCard.kt
│       │   │   │       ├── BriefingCard.kt
│       │   │   │       ├── TaskCard.kt
│       │   │   │       ├── SectionLabel.kt
│       │   │   │       └── PriorityPill.kt
│       │   │   └── settings/
│       │   │       ├── SettingsScreen.kt        (masked Gemini/Notion fields, IME Next→Send)
│       │   │       └── SettingsViewModel.kt     (AndroidViewModel owning TokenStore)
│       │   └── data/
│       │       ├── model/
│       │       │   ├── Task.kt              (data class)
│       │       │   ├── Priority.kt          (enum)
│       │       │   └── Briefing.kt          (data class)
│       │       ├── secure/
│       │       │   └── TokenStore.kt         (EncryptedSharedPreferences — Phase 2 step 1)
│       │       └── PreviewData.kt           (mock data; Clock from kotlin.time)
│       └── res/
│           ├── values/
│           │   ├── strings.xml
│           │   └── themes.xml               (minimal — Compose handles theming)
│           ├── drawable/
│           └── mipmap-*/                    (launcher icons)
```

**iOS folder rule:** Create `shared/src/iosMain/kotlin/` and put a single empty `.gitkeep` file inside. No iOS targets configured in `shared/build.gradle.kts` for now — only `android()` target is enabled. Add a comment in `shared/build.gradle.kts`:

```kotlin
// iOS target intentionally not configured.
// Project uses KMP structure for future portability, but Android is the only
// active target. Add iosX64(), iosArm64(), iosSimulatorArm64() here when ready.
```

---

## Data Models

```kotlin
// data/model/Priority.kt
enum class Priority { HIGH, MID, LOW }

// data/model/Task.kt
data class Task(
    val id: String,
    val title: String,
    val priority: Priority,
    val tip: String,              // AI-generated efficiency advice
    val estimatedMinutes: Int,
    val notionUrl: String
)

// data/model/Briefing.kt
data class Briefing(
    val generatedAt: Instant,     // kotlinx.datetime
    val summary: String,          // one-paragraph overview
    val tasks: List<Task>
)
```

## Mock Data

`data/PreviewData.kt` provides three realistic tasks for visual development:

```kotlin
object PreviewData {
    val sampleBriefing = Briefing(
        generatedAt = Clock.System.now(),
        summary = "Focus on the SDK doc first — it unblocks two downstream tasks. The PR review is shorter than it looks. Save design feedback for after lunch.",
        tasks = listOf(
            Task(
                id = "1",
                title = "Write SDK documentation",
                priority = Priority.HIGH,
                tip = "Start with the public API surface. Skip examples until structure is solid.",
                estimatedMinutes = 120,
                notionUrl = "https://notion.so/..."
            ),
            Task(
                id = "2",
                title = "Review PR #482",
                priority = Priority.HIGH,
                tip = "The diff is large but most changes are mechanical. Focus review on the auth module.",
                estimatedMinutes = 45,
                notionUrl = "https://notion.so/..."
            ),
            Task(
                id = "3",
                title = "Reply to design feedback",
                priority = Priority.MID,
                tip = "Batch all responses into one message rather than scattered replies.",
                estimatedMinutes = 20,
                notionUrl = "https://notion.so/..."
            )
        )
    )
}
```

---

## UI Implementation

**Read `UI_PROTOTYPE.md` for the full visual spec.** Key reminders:

- Pure dark theme — no light mode for prototype
- Use `Material3` `darkColorScheme` overridden with custom colors from `Color.kt`
- All cards rounded 16dp, padding 20dp
- Accent color `#A78BFA` (soft violet) — used sparingly
- Single `LazyColumn` in `HomeScreen.kt`
- Each component in its own file under `ui/home/components/`
- **Edge-to-edge:** `MainActivity` calls `enableEdgeToEdge()`. Each screen consumes `WindowInsets.statusBars` / `navigationBars` itself (added to `LazyColumn` `contentPadding`). The home screen overlays a `Brush.verticalGradient` scrim over the status bar zone so content scrolls *under* the system icons cleanly — reuse this pattern on any new full-screen surface.

---

## Phase 2 — Shipped vs. Stubs

### ✅ Shipped — `data/secure/TokenStore.kt`
Real implementation backed by `EncryptedSharedPreferences` (`MasterKey` AES256_GCM, AES256_SIV/AES256_GCM for keys/values). Same `TokenStore(context)` constructor surface; uses `applicationContext` internally. Methods: `saveGeminiKey`, `getGeminiKey`, `saveNotionToken`, `getNotionToken`.

### ⏳ Still stubs — leave as `TODO(Phase 2 stepN: …)` until their slice arrives

```kotlin
// data/AgentRepository.kt — Phase 2 step 3
class AgentRepository {
    suspend fun runAgent(): Briefing { TODO("Phase 2 step 3: call Koog agent → Gemini + Notion MCP") }
    suspend fun getLastBriefing(): Briefing? { TODO("Phase 2 step 3: read from local cache") }
}

// worker/MorningAgentWorker.kt — Phase 2 step 4
class MorningAgentWorker(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        TODO("Phase 2 step 4: invoke AgentRepository.runAgent() and post notification")
    }
}
```

---

## Gradle Dependencies (androidApp/build.gradle.kts)

Versions are pinned in `gradle/libs.versions.toml` and referenced via the typesafe `libs.*` accessors. Active set:

```kotlin
dependencies {
    // Compose BOM (2026.04.01 — Compose Compiler 2.3 compatible)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Google Fonts + Material Icons
    implementation(libs.androidx.compose.google.fonts)
    implementation(libs.androidx.compose.material.icons)

    // Activity + Lifecycle + Splash + datetime
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.datetime)

    // Phase 2 — agent + secure storage + scheduling
    implementation(libs.koog.agents)              // single artifact: Gemini client + MCP client
    implementation(libs.androidx.security.crypto) // EncryptedSharedPreferences
    implementation(libs.androidx.work.runtime.ktx)
}
```

Two non-obvious bits in `androidApp/build.gradle.kts`:

1. **Top-level Kotlin DSL** — Kotlin 2.3 removed the AGP-side `android { kotlinOptions {} }` block, so `jvmTarget` lives in a top-level `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`. Keep that pattern when touching this file.

2. **`packaging.resources.excludes`** — Koog's transitive deps (Ktor, Netty, OkHttp, AWS SDK) ship overlapping META-INF metadata. The current exclude set covers `INDEX.LIST`, `io.netty.versions.properties`, OSGI `MANIFEST.MF`, license/notice files. If a future dep adds new conflicts, extend that block rather than dropping deps.

---

## What "Done" Looks Like for Phase 1

- [ ] Project opens cleanly in Android Studio Koala or newer
- [ ] `./gradlew :androidApp:assembleDebug` succeeds
- [ ] App launches into `LaunchScreen` (system splash hands off cleanly — no white flash)
- [ ] Launch screen shows breathing orb + wordmark + tagline per `UI_PROTOTYPE.md`
- [ ] After ~1.2s, transitions smoothly to `HomeScreen` showing 3 mock tasks
- [ ] All home screen visual states implemented: default, loading, empty, error (use Compose previews to demonstrate)
- [ ] Settings icon tap navigates to empty `SettingsScreen` (stub)
- [ ] "Run Now" button shows loading state, then re-renders the same mock data after 1.5s simulated delay
- [ ] No real network calls anywhere
- [ ] No crashes on rotation
- [ ] iOS source folder exists but is empty (verify with `ls shared/src/iosMain/kotlin/`)

---

## Style Rules for Claude Code

1. **Composables are small.** If a composable is over ~80 lines, split it.
2. **No hardcoded colors in composables** — always reference `MaterialTheme.colorScheme.*` or custom theme tokens.
3. **No hardcoded strings** — use `stringResource()`. (For prototype, you can be lenient on this for prose, strict for labels.)
4. **One file per top-level composable.** `TaskCard` lives in `TaskCard.kt`, not buried in `HomeScreen.kt`.
5. **Always provide a `@Preview` for every component file.** Wrap previews in `MorningAgentTheme {}`.
6. **Comment any `TODO()` with the phase it belongs to** (`// TODO(Phase 2): wire Notion MCP`).
7. **No emojis in code or strings** unless explicitly part of the design spec.
8. **Don't over-engineer.** This is a one-screen personal app. No DI framework, no use-case classes, no Clean Architecture layers. Repository → ViewModel → Composable is enough.

---

## Out of Scope for Now

- Authentication flows (no login screen)
- Multi-language support (English only)
- Tablet layouts
- Deep linking
- Analytics
- Crash reporting
- Tests (Phase 1 is pure visual prototype; tests come in Phase 2)

---

## Reference Files

- `UI_PROTOTYPE.md` — full visual design spec, mandatory read
- This file (`CLAUDE.md`) — tech and architecture brief

When in doubt, ask before guessing. This is a craft project — quality over speed.
