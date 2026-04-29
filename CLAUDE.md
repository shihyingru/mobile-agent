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

**Phase 1 (CURRENT) — UI Prototype**
- Single screen, Jetpack Compose, mock data only
- No real API calls
- No scheduling
- Goal: lock the visual direction before wiring real systems

**Phase 2 (FUTURE — do not implement yet)**
- Wire Koog agent + Gemini API
- Wire Notion MCP over SSE
- Wire WorkManager scheduling
- Wire EncryptedSharedPreferences for token storage

When working in Phase 1, leave Phase 2 wiring as `TODO()` stubs with clear comments. Do not silently mock real network calls.

---

## Tech Stack

| Layer | Choice | Notes |
|---|---|---|
| Build system | Gradle Kotlin DSL | `build.gradle.kts` everywhere |
| Project structure | **Kotlin Multiplatform** | But only Android target is implemented. iOS source set scaffolded as empty. |
| UI | Jetpack Compose (Material 3) | Single-activity app |
| Min SDK | 26 (Android 8.0) | EncryptedSharedPreferences requires 23+; we go higher for safety |
| Target SDK | 35 |  |
| Kotlin | 2.0+ |  |
| Agent framework | Koog (`ai.koog:koog-agents-jvm:0.7.3+`) | Phase 2 only |
| LLM provider | Google Gemini | via Koog's Google client (Phase 2) |
| Tool integration | Notion MCP (SSE transport) | via Koog MCP client (Phase 2) |
| Background work | WorkManager (`androidx.work`) | Phase 2 only |
| Secure storage | Android Keystore + `EncryptedSharedPreferences` (`androidx.security:security-crypto`) | Phase 2 only |
| Notifications | `NotificationCompat` | Phase 2 only |

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
│       │   │   │   └── components/
│       │   │   │       ├── AgentStatusCard.kt
│       │   │   │       ├── BriefingCard.kt
│       │   │   │       ├── TaskCard.kt
│       │   │   │       ├── SectionLabel.kt
│       │   │   │       └── PriorityPill.kt
│       │   │   └── settings/
│       │   │       └── SettingsScreen.kt        (stub — empty composable)
│       │   └── data/
│       │       ├── model/
│       │       │   ├── Task.kt              (data class)
│       │       │   ├── Priority.kt          (enum)
│       │       │   └── Briefing.kt          (data class)
│       │       └── PreviewData.kt           (mock data for prototype)
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

---

## Phase 2 Stubs (Add Now, Implement Later)

Create empty/stub files now so the structure is in place, but implement them in Phase 2:

### `data/AgentRepository.kt` (stub)
```kotlin
class AgentRepository {
    suspend fun runAgent(): Briefing {
        TODO("Phase 2: call Koog agent → Gemini + Notion MCP")
    }

    suspend fun getLastBriefing(): Briefing? {
        TODO("Phase 2: read from local cache")
    }
}
```

### `data/secure/TokenStore.kt` (stub)
```kotlin
class TokenStore(context: Context) {
    fun saveGeminiKey(key: String) { TODO("Phase 2: EncryptedSharedPreferences") }
    fun getGeminiKey(): String? { TODO("Phase 2: EncryptedSharedPreferences") }

    fun saveNotionToken(token: String) { TODO("Phase 2: EncryptedSharedPreferences") }
    fun getNotionToken(): String? { TODO("Phase 2: EncryptedSharedPreferences") }
}
```

### `worker/MorningAgentWorker.kt` (stub)
```kotlin
class MorningAgentWorker(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        TODO("Phase 2: invoke AgentRepository.runAgent() and post notification")
    }
}
```

These stubs make the architecture visible without committing to the implementation yet.

---

## Gradle Dependencies (androidApp/build.gradle.kts)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity + Lifecycle
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Splash Screen API (system-level splash hand-off)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // kotlinx.datetime (used in data models)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Phase 2 — DECLARED BUT UNUSED IN PHASE 1
    // implementation("ai.koog:koog-agents-jvm:0.7.3")
    // implementation("ai.koog:koog-agents-mcp:0.7.3")
    // implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // implementation("androidx.work:work-runtime-ktx:2.10.0")
}
```

Comment out Phase 2 dependencies during Phase 1 — they bloat the build for no reason yet.

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
- `CLAUDE.md` — tech and architecture brief

When in doubt, ask before guessing. This is a craft project — quality over speed.
