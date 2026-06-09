# Evening reflection — plan

## Context

The morning briefing answers "what should I focus on today?" The evening counterpart, designed but not yet built, answers **"is today's plan still right for tomorrow?"** Per `design/project/DESIGN_SYSTEM.md §4.2` the evening (17:00–20:59) and night (21:00–04:59) slots already define greeting, subhead, button label ("Confirm tomorrow" / "Plan tomorrow"), status copy, and a dark theme. We're filling in the content + plumbing that those slots have been waiting for.

Voice is **forward-looking**, not past-tense reflection: "Tomorrow is set." The agent re-reads today's Notion state, compares to this morning's cached briefing, and proposes **carry-overs** (reschedule, mark done, change priority) — reusing the `BriefingActions` chips infrastructure from the two-way-briefing feature.

## Design decisions (locked)

1. **Single briefing card, slot-driven content.** When the slot is evening/night and an evening cache exists, the briefing card swaps to show the evening reflection. Otherwise it shows morning. Two-card stack rejected (DESIGN_SYSTEM `§4.2` shows one card).
2. **Swipe to inspect morning briefing in evening.** When both caches exist, a horizontal swipe on the briefing card toggles morning ↔ evening. No persistent affordance; the swipe is the entire mechanic.
3. **Reuse the `Briefing` data class** with a new `kind: BriefingKind = MORNING` field. `BriefingActions` chips work for free since the JSON shape is identical.
4. **Default fire time 19:00** (configurable in Settings, mirrors the morning briefing time picker).
5. **Notification reuses the morning channel** with a different title (`"Today's wrap-up — N carry-overs"`).

## Architecture

```
                         ┌─────────────────────────────┐
                         │  Slot resolver (clock-based) │
                         └──────────┬──────────────────┘
                                    ▼
            morning slot ───┐               ┌─── evening/night slot
                            ▼               ▼
        TokenStore.lastBriefingJson    TokenStore.lastReflectionJson
                            │               │
                            └───────┬───────┘
                                    ▼
                         displayedBriefing: Briefing?
                                    │
                                    ▼
                            HomeScreen.BriefingBlock + BriefingActions

                         ┌─────────────────────────────┐
                         │  Refresh button (slot-driven) │
                         └──────────┬──────────────────┘
                                    ▼
        morning slot ───┐                       ┌─── evening/night slot
                        ▼                       ▼
              HomeViewModel.runNow()    HomeViewModel.runEveningNow()
                        │                       │
                        ▼                       ▼
              AgentRepository.runAgent(MORNING) / runAgent(EVENING)
                                    │
                                    ▼
                      Notion fetch + LLM (BriefingPrompt vs ReflectionPrompt)
                                    │
                                    ▼
                  Briefing(kind, actions, dismissedActionIds, ...)
                                    │
                                    ▼
                  TokenStore (separate cache key per kind)
```

Workers (`MorningAgentWorker`, new `EveningReflectionWorker`) call the same `runAgent(kind)` path. Schedulers (`BriefingScheduler`, new `EveningScheduler`) are parallel: each reads its own time from TokenStore.

## PR slicing

Two PRs, both branch off `develop`, target `develop`.

---

### PR 1 — Cache + worker + Settings (silent)

**Goal:** A scheduled evening agent fires at the configured time, produces a Reflection-flavored Briefing, persists it to a new cache key, and posts a notification. Home UI is untouched — the user sees the morning briefing as before, plus a new evening notification at 19:00.

**Files (new):**

- `data/model/BriefingKind.kt` — enum (`MORNING`, `EVENING`).
- `data/agent/ReflectionPrompt.kt` — system prompt + `buildReflectionUserMessage(morningBriefing, currentTasks, today)` + reuse of `parseBriefingResponse` / `BriefingPayload`. Voice: forward-looking, wind-down. Asks for `proposedActions` shaped to be carry-overs (favors `reschedule` with `newDate = today + 1`, `mark_done` for completed, `change_priority` for bumps).
- `worker/EveningReflectionWorker.kt` — mirrors `MorningAgentWorker`. Reads tokens, builds repo, calls `repo.runAgent(EVENING)`, posts notification, reschedules.
- `worker/EveningScheduler.kt` — mirrors `BriefingScheduler`. Reads `dailyEveningHour/Minute` from TokenStore. KEEP on launch, UPDATE on Settings change. Default 19:00.

**Files (edited):**

- `data/model/Briefing.kt` — add `kind: BriefingKind = BriefingKind.MORNING` (default keeps old caches decoding cleanly).
- `data/AgentRepository.kt` — refactor `runAgent()` to `runAgent(kind: BriefingKind = MORNING)`. Selects prompt builder + cache key per kind. Add `getLastReflection(): Briefing?`. `saveDismissal` works for both kinds (it operates on whichever cache the dismissed action belongs to — keyed lookup, both keys).
- `data/secure/TokenStore.kt` — new keys: `dailyEveningEnabled` (bool), `dailyEveningHour` (int, default 19), `dailyEveningMinute` (int, default 0), `lastReflectionJson` (string, Tink-sealed like the morning one).
- `MainActivity.kt` — call `EveningScheduler.ensure(this)` alongside `BriefingScheduler.ensure(this)`.
- `ui/settings/SettingsScreen.kt` + `SettingsViewModel.kt` — new BEHAVIOR rows:
  - "Evening reflection" toggle (writes `dailyEveningEnabled`)
  - "Evening time" row indented under it (TimePicker overlay, default 19:00)
  - Mirror the existing "Daily briefing" + "Briefing time" pattern; reuse the same TimePicker composable.

**Acceptance:**

- `./gradlew :androidApp:assembleDebug` green.
- Toggling "Evening reflection" on + picking 19:00 enqueues the worker.
- At 19:00, the worker fires, calls Notion + LLM, writes `lastReflectionJson`, posts a notification titled `"Today's wrap-up — N carry-overs"`.
- Old cached morning briefings decode without crash (additive `kind` field with default).
- Settings UI matches the morning briefing pattern visually — no new design tokens.
- No regression on the existing morning briefing flow.

**Verification steps:**

1. Build + install.
2. Settings → enable "Evening reflection", set time to ~2 minutes from now.
3. Wait for fire.
4. See notification with the wrap-up title.
5. Inspect `lastReflectionJson` in DataStore via Android Studio's App Inspector (or just confirm next launch shows no crash).
6. Disable toggle → worker is canceled.

---

### PR 2 — Home UI: slot-swap + swipe + slot-driven Run

**Goal:** During evening/night slots, the briefing card shows the evening reflection (if cached); Refresh button label + action route to the evening agent; horizontal swipe toggles morning ↔ evening when both exist.

**Files (edited):**

- `ui/home/HomeViewModel.kt`:
  - New `displayedBriefing: Briefing?` computed from `(currentSlot, cachedMorning, cachedEvening, swipeOverride)` — when override is null, use the slot default; when set, force the override.
  - New `swipeToggle()` — flips `swipeOverride` between `MORNING` and `EVENING`; only meaningful when both caches exist.
  - New `runEveningNow()` — mirrors `runNow()` but with `BriefingKind.EVENING`; uses the same `HomeUiState.Loading/Success/Error` plumbing.
  - `runNowForCurrentSlot()` — dispatches by slot to `runNow()` (morning slots) or `runEveningNow()` (evening/night). Bound to the Refresh button.
- `ui/home/HomeScreen.kt`:
  - Wire `onRunNow = vm::runNowForCurrentSlot`.
  - Swap `briefing` argument from `(uiState as Success).briefing` to `vm.displayedBriefing`.
  - Add horizontal-swipe gesture on `BriefingBlock`: `pointerInput` + `detectHorizontalDragGestures`, threshold ~80dp → call `vm.swipeToggle()`. Only attach the gesture when both caches exist (otherwise no-op).
  - The existing `BriefingActions` block already filters on `displayedBriefing.dismissedActionIds` — no change needed.
- `ui/home/components/BriefingBlock.kt`:
  - Optional: subtle visual hint that swipe is available (e.g. small mono "← MORNING / EVENING →" beside the eyebrow when both caches exist). One line max. Skippable if it crowds the layout.

**Acceptance:**

- During morning/midday/afternoon slots: card shows morning briefing; Refresh button label per slot copy; Refresh fires `runNow()`.
- During evening/night slots with evening cache populated: card shows evening reflection; button label is "Confirm tomorrow" / "Plan tomorrow" (already driven by slot copy); Refresh fires `runEveningNow()`.
- During evening/night slots with NO evening cache yet: card shows morning briefing (degraded gracefully); Refresh still fires `runEveningNow()`.
- Horizontal swipe on the briefing card toggles morning ↔ evening when both exist. Single-card state means no swipe target.
- `BriefingActions` chips work end-to-end on the evening reflection (Apply / Dismiss / Snackbar all wired via existing infra).
- @Preview added for evening-slot variant of `BriefingBlock` (dark theme, evening reflection content).

**Verification steps:**

1. Set device clock to ~19:00 (or wait), fire evening agent.
2. Re-open Home → confirm card shows wrap-up content + dark theme.
3. Swipe horizontally → see morning briefing.
4. Swipe back → evening.
5. Tap Apply on a carry-over chip → confirm Notion task date moves to tomorrow.
6. Tap Dismiss → chip removed; force-stop + reopen → still dismissed (PR 3 persistence from two-way-briefing carries over).

---

## Risks

| Risk | Mitigation |
|---|---|
| Old cached morning briefing missing `kind` field crashes decode | Default `kind = MORNING` + `ignoreUnknownKeys = true` already in `briefingJson`. |
| User toggles evening time during the day; worker double-fires | `EveningScheduler.UPDATE` policy on Settings change (mirror morning's UPDATE pattern). |
| Evening worker fires when device offline | Same WorkManager retry policy as `MorningAgentWorker`; cap at MAX_RETRIES, then defer until tomorrow. |
| Model returns past dates for `newDate` (we saw this in PR 0 spike) | Prompt already tightened with "MUST be on or after today" in PR 1 of two-way-briefing. Inherited. |
| Swipe gesture conflicts with vertical LazyColumn scroll | `detectHorizontalDragGestures` with a meaningful threshold (~80dp) avoids accidental toggles. |
| Notification at 19:00 is annoying alongside calendar dinner reminders | Out of scope; design spec's "Quiet hours" toggle is the future seam. v1 ships notification on; user can mute the channel. |
| The "morning briefing" experience disappears in evening | Swipe mechanic addresses this. If discoverability is poor, PR 3 can add a small "swap" affordance. |

## Out of scope (later)

- **Quiet hours** toggle (DESIGN_SYSTEM `§4.3`) — silences notifications during configured hours. Independent feature.
- **Per-channel notifications** for morning vs evening — currently one channel. Could split later if user wants different ringtones.
- **Cross-day reflection** — "you've deferred this 3 days, kill it?" Requires a deferral history table; not in this slice.
- **"Plan tomorrow" (night slot, 21:00+)** could later prepare tomorrow's morning briefing in advance. For now it triggers the same evening agent.

## Critical files reference

**New:**
- `data/model/BriefingKind.kt`
- `data/agent/ReflectionPrompt.kt`
- `worker/EveningReflectionWorker.kt`
- `worker/EveningScheduler.kt`

**Edited:**
- `data/model/Briefing.kt` — `kind` field
- `data/AgentRepository.kt` — `runAgent(kind)` + `getLastReflection()`
- `data/secure/TokenStore.kt` — 4 new keys
- `MainActivity.kt` — call `EveningScheduler.ensure()`
- `ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt` — toggle + time picker
- `ui/home/HomeViewModel.kt` — slot-driven `displayedBriefing` + `swipeToggle` + `runEveningNow`
- `ui/home/HomeScreen.kt` — wire slot-driven actions + swipe gesture
- `ui/home/components/BriefingBlock.kt` — optional swap hint

**Untouched:** `BriefingPrompt.kt`, `GeminiBriefingClient.kt`, `ClaudeBriefingClient.kt`, `BriefingActions.kt`, `NotionTaskMutator.kt`, `NotionRestMutator.kt`, `NotionRestClient.kt`. The reflection reuses the same LLM clients + same action chips — no provider-layer changes.
