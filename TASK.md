# Core Readiness Task

This branch implements only Phase 3.4 of `docs/ROADMAP.md`.

## Scope guard

This phase is validation-first. Do not add concrete providers, resolver/scoring logic, Android media integration, cache, translation, phone UI, Android Auto UI, or import/adapt implementation code from the previous Auto Lyrics fork.

Use the existing AALyrics `LyricsState`, `LyricsCoordinator`, provider contracts, fake providers, and a fake `CandidateSelector`. Add production code only if a test exposes a genuine core boundary defect that cannot be expressed or fixed through existing contracts.

Before adding any non-trivial behavior, re-check `docs/MIGRATION_INVENTORY.md` and the current `whoxamxl/auto-lyrics` main branch so proven fork behavior is not independently reinvented.

## Commit strategy

Keep commits small and single-purpose so each change is easy to review and revert.

- one logical test group per commit where practical,
- production fixes, if any, in a separate commit from the test that exposes them,
- documentation/checklist updates in their own commit,
- avoid bundling unrelated readiness checks into one large commit,
- keep PR-level squashing separate from branch-level development history.

For Phase 3.4, prefer roughly this sequence:

1. baseline end-to-end happy-path integration test,
2. provider-order / multi-provider aggregation tests,
3. failure-state tests,
4. cancellation / stale-result / repeated-lookup tests,
5. clear()/selector invocation ownership tests,
6. documentation/checklist update.

## Phase 3.4 — Core integration/readiness tests

- [x] Exercise the complete provider-independent flow through `LyricsCoordinator` and `LyricsState` using fake providers and a fake selector only.
- [x] Verify provider completion order does not become the winner-selection rule.
- [x] Verify multiple providers can contribute normalized candidates before selection.
- [x] Verify one provider failure does not discard healthy provider results.
- [x] Verify all-provider failure reaches `Failed` without exposing raw exceptions in shared state.
- [x] Verify no usable winner with healthy providers reaches `NotFound`.
- [x] Verify a partial provider failure plus a winner reaches `Degraded`.
- [x] Verify a newer lookup supersedes older work and stale completion cannot overwrite current state.
- [x] Verify repeating the same track still creates a fresh lookup identity and stale prior work remains rejected.
- [x] Verify `clear()` cancels active work and leaves `Idle` as the final owned state.
- [x] Verify selector invocation happens once per completed lookup after candidate collection.
- [x] Verify the core flow is independent of Android and network types.
- [x] Run debug build and all unit tests in CI.
- [x] Open one Phase 3.4 PR to `main`.

## Readiness evidence

- `CoreFlowIntegrationTest` covers the end-to-end fake-only flow and lifecycle/failure invariants.
- Existing `LyricsStateTest` retains direct stale-completion reducer coverage.
- `:core:model`, `:provider:api`, and `:core:lyrics` all use the pure Kotlin JVM plugin rather than an Android plugin.
- The only non-project runtime dependency in `:core:lyrics` is `kotlinx-coroutines-core`; no Android framework or networking library is present.
- No production source changed during Phase 3.4; all behavior under validation is the already-merged core.
- PR #10 is open and its branch-name check, debug APK build, and unit tests passed in CI run `34944334320`.

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

Review and squash-merge PR #10. After merge, Phase 4 begins on a new topic branch. Do not start Phase 4 or any fork code adaptation on this branch.
