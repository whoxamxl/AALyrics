# Lyrics Coordinator Task

This branch implements only Phase 3.3 of `docs/ROADMAP.md`.

## Scope guard

Use only AALyrics domain/provider contracts, fake providers, and a fake `CandidateSelector`. Do not add concrete provider implementations, networking, cache, translation, Android media code, or adapt resolver/provider code from the previous Auto Lyrics fork.

Before starting non-trivial orchestration behavior, inspect the current fork and `docs/MIGRATION_INVENTORY.md` so proven behavior is preserved without bringing the old ownership model forward.

## Fork preflight

Reviewed current `whoxamxl/auto-lyrics` main at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).

Behavior to preserve at the new boundary:

- provider attempts are independent; one source failure must not cancel healthy sources,
- providers are queried concurrently rather than winner-by-call-order,
- a newer track/request supersedes older work,
- late results from obsolete work must not replace current state,
- candidate selection happens only after provider results are normalized and collected,
- cancellation must remain cancellation rather than being converted into a provider failure.

Behavior intentionally **not** moved into `LyricsCoordinator` in this slice:

- the existing 600 ms media metadata debounce — playback/platform concern for Phase 4,
- phone/Android Auto demand gating — `LyricsDemandController` remains a Phase 4/application boundary concern,
- cache freshness/variant policy — later cache boundary,
- translation reset/translation jobs — later translation/state concern,
- per-provider HTTP timeout implementation — provider/shared execution policy to review at the stop gate,
- scoring, source confidence, recording-version matching, cross-script matching, and karaoke winner policy — mature resolver behavior reserved for post-gate adaptation.

## Slice 3 — Lyrics coordinator

- [ ] Add coroutine support to `:core:lyrics` without Android dependencies.
- [ ] Define `LyricsCoordinator` with caller-owned `CoroutineScope`.
- [ ] Start each lookup with a fresh `LyricsLookupId` and publish `Loading`.
- [ ] Fan out enabled `LyricsProvider` searches concurrently.
- [ ] Isolate ordinary provider failures while propagating cancellation.
- [ ] Collect normalized candidates and hand them to `CandidateSelector` exactly once.
- [ ] Publish `Ready`, `Degraded`, `NotFound`, or `Failed` through the existing reducer.
- [ ] Cancel/supersede active work on a newer lookup and reject late stale completion through lookup identity.
- [ ] Clear active work to `Idle`.
- [ ] Add tests using fake providers and a fake selector only.

## Failure semantics for this slice

- winner + zero failed providers → `Ready`
- winner + one or more failed providers → `Degraded`
- no winner + zero failed providers → `NotFound`
- no winner + one or more failed providers → `Failed`

This is orchestration status only. Provider-specific diagnostics remain internal and no raw exception enters shared UI state.

## Explicitly out of scope

- [ ] concrete provider implementation
- [ ] resolver/scoring implementation
- [ ] provider-specific timeout/networking logic
- [ ] cache
- [ ] translation
- [ ] Android `MediaSession`
- [ ] playback demand/debounce logic
- [ ] phone UI
- [ ] Android Auto UI

## Next action

Implement the coordinator and its fake-only tests, run CI, and open one Phase 3.3 PR. After that PR is reviewed and merged, Phase 3.4 is core integration/readiness testing. Do not begin Phase 4 or any fork code adaptation in this branch.
