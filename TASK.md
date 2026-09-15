# Core Readiness Task

This branch implements only Phase 3.4 of `docs/ROADMAP.md`.

## Scope guard

This phase is validation-first. Do not add concrete providers, resolver/scoring logic, Android media integration, cache, translation, phone UI, Android Auto UI, or import/adapt implementation code from the previous Auto Lyrics fork.

Use the existing AALyrics `LyricsState`, `LyricsCoordinator`, provider contracts, fake providers, and a fake `CandidateSelector`. Add production code only if a test exposes a genuine core boundary defect that cannot be expressed or fixed through existing contracts.

Before adding any non-trivial behavior, re-check `docs/MIGRATION_INVENTORY.md` and the current `whoxamxl/auto-lyrics` main branch so proven fork behavior is not independently reinvented.

## Phase 3.4 — Core integration/readiness tests

- [ ] Exercise the complete provider-independent flow through `LyricsCoordinator` and `LyricsState` using fake providers and a fake selector only.
- [ ] Verify provider completion order does not become the winner-selection rule.
- [ ] Verify multiple providers can contribute normalized candidates before selection.
- [ ] Verify one provider failure does not discard healthy provider results.
- [ ] Verify all-provider failure reaches `Failed` without exposing raw exceptions in shared state.
- [ ] Verify no usable winner with healthy providers reaches `NotFound`.
- [ ] Verify a partial provider failure plus a winner reaches `Degraded`.
- [ ] Verify a newer lookup supersedes older work and stale completion cannot overwrite current state.
- [ ] Verify repeating the same track still creates a fresh lookup identity and stale prior work remains rejected.
- [ ] Verify `clear()` cancels active work and leaves `Idle` as the final owned state.
- [ ] Verify selector invocation happens once per completed lookup after candidate collection.
- [ ] Verify the core flow is independent of Android and network types.
- [ ] Run debug build and all unit tests in CI.
- [ ] Open one Phase 3.4 PR to `main`.

## Explicitly out of scope

- [ ] LRCLIB implementation
- [ ] Musixmatch implementation
- [ ] PetitLyrics implementation or configuration changes
- [ ] SyncLRC implementation
- [ ] adapting `LyricsProviderResolver`
- [ ] scoring weights or metadata similarity logic
- [ ] recording-version / cross-script matching
- [ ] provider-specific timeout/networking policy
- [ ] Android `MediaSession` / `MediaController`
- [ ] playback demand gating or metadata debounce
- [ ] cache / translation
- [ ] phone / Android Auto presentation

## Exit criteria

Phase 3.4 is complete when the pure Kotlin core flow is covered end-to-end with fakes and the tests demonstrate that orchestration, lifecycle ownership, failure isolation, cancellation, and stale-result protection behave correctly without relying on any concrete provider or Android framework type.

Passing Phase 3.4 does **not** authorize provider migration. The project proceeds next to Phase 4 Playback boundary, then Phase 5 Core Readiness validation, then stops at the documented STOP GATE for explicit architecture/migration review.

## Next action

Implement only the Phase 3.4 integration/readiness tests above. If they pass without revealing a core defect, avoid changing production behavior. Then run CI and open the Phase 3.4 PR.
