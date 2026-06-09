# Evening reflection — todo

See `plan-evening-reflection.md` for full context, design decisions, and acceptance criteria.

Branches off `develop`, PRs back into `develop`.

---

## PR 1 — Cache + worker + Settings (silent infrastructure) ✓

Shipped via PR #25 (`feature/evening-reflection-pr1`).

- [x] `BriefingKind.kt` enum + `Briefing.kind` field
- [x] `ReflectionPrompt.kt` — forward-looking prompt, reuses `BriefingPayload`
- [x] `AgentRepository.runAgent(kind)` + `getLastReflection()` + `saveDismissal` for both kinds
- [x] `TokenStore` — 4 new keys (evening enabled/hour/minute + lastReflectionJson)
- [x] `EveningReflectionWorker` + `EveningScheduler`
- [x] `MainActivity` calls `EveningScheduler.ensure()`
- [x] Settings UI — evening toggle + time picker
- [x] Build green, verified on device

---

## PR 2 — Home UI: slot-swap + swipe + slot-driven Run ✓

Shipped via PR #26 (`feature/evening-reflection-pr2`), stacked on PR1.

- [x] `HomeViewModel` — `currentSlot`, `displayedBriefing`, `canSwipe`, `swipeToggle()`, `runEveningNow()`, `runNowForCurrentSlot()`
- [x] `HomeScreen` — wired slot-driven run + horizontal swipe gesture (80dp threshold)
- [x] `BriefingBlock` — BRIEFING/REFLECTION label swap + "← MORNING / EVENING →" hint + evening preview
- [x] `PreviewData.sampleEveningReflection`
- [x] `AgentRepository.saveDismissal` handles both morning + evening caches
- [x] Build green

### Pending manual verification (test at night)
- [ ] Evening card renders with dark theme + wrap-up content
- [ ] Swipe toggles morning ↔ evening
- [ ] Run Now dispatches correct agent per slot
- [ ] Apply/Dismiss work on evening actions
- [ ] No regression on morning flow

---

## Out of scope (future)

- **Quiet hours** toggle (DESIGN_SYSTEM §4.3) — suppress notifications during configured hours
- **Cross-day deferral history** — "you've deferred this 3 days, kill it?" needs a separate history table
- **Separate notification channel** for evening vs morning — currently shared
- **"Plan tomorrow" (night, 21:00+)** could prepare tomorrow's morning briefing in advance; for now it shares the evening agent
