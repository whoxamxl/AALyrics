# Lyrics Core Task

This branch implements Phase 3 of `docs/ROADMAP.md` in small slices.

## Scope guard

Use only AALyrics domain/provider contracts and synthetic test data. Do not add concrete provider implementations, networking, Android media code, or copy/adapt code from the previous Auto Lyrics fork.

Before starting any non-trivial behavior, inspect the current fork and the migration inventory so proven behavior is not independently reinvented.

## Slice 1 — Lyrics state lifecycle (current)

- [x] Define a provider-independent lookup identity tied to a track.
- [x] Define explicit `LyricsState` variants for idle, loading, resolved, degraded, not found, and terminal failure.
- [x] Define pure state events/reduction rules.
- [x] Reject stale completion events after a newer lookup starts.
- [x] Add unit tests for lifecycle and invariants.
- [x] Run CI and open PR #6.

## Later slices — not part of Slice 1

- [ ] Slice 2: define only the provider-independent candidate-selection port/preferences; do not create a new scoring algorithm.
- [ ] Slice 3: `LyricsCoordinator` using fake providers and a fake selector only.
- [ ] Slice 4: core integration tests.
- [ ] Phase 4: playback boundary, after reviewing proven fork identity/lifecycle behavior.
- [ ] Phase 5: Core Readiness validation.

The mature `LyricsProviderResolver` and its matching/scoring behavior are reserved for post-gate adaptation behind the selection port. See PR #7 / `docs/MIGRATION_INVENTORY.md` once merged.

## Stop gate

After Phase 5, stop for explicit architecture review before any LRCLIB, Musixmatch, PetitLyrics, SyncLRC, resolver implementation, or previous-fork code is implemented or adapted.

## Next action

Review and merge PR #6 and the fork-aware documentation PR #7. After both are on `main`, create a new topic branch for Slice 2 and define only the candidate-selection boundary required by `LyricsCoordinator`.
