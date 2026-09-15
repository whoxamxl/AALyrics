# Lyrics Core Task

This branch implements Phase 3 of `docs/ROADMAP.md` in small slices.

## Scope guard

Use only AALyrics domain/provider contracts and synthetic test data. Do not add concrete provider implementations, networking, Android media code, or copy/adapt code from the previous Auto Lyrics fork.

## Slice 1 — Lyrics state lifecycle (current)

- [x] Define a provider-independent lookup identity tied to a track.
- [x] Define explicit `LyricsState` variants for idle, loading, resolved, degraded, not found, and terminal failure.
- [x] Define pure state events/reduction rules.
- [x] Reject stale completion events after a newer lookup starts.
- [x] Add unit tests for lifecycle and invariants.
- [x] Run CI and open PR #6.

## Later slices — not part of Slice 1

- [ ] Slice 2: deterministic `CandidateResolver` with synthetic candidates.
- [ ] Slice 3: `LyricsCoordinator` using fake providers only.
- [ ] Slice 4: core integration tests.
- [ ] Phase 4: playback boundary.
- [ ] Phase 5: Core Readiness validation.

## Stop gate

After Phase 5, stop for explicit architecture review before any LRCLIB, Musixmatch, PetitLyrics, SyncLRC, or previous-fork code is implemented or adapted.

## Next action

Review and merge PR #6. After it is merged, create a new topic branch for Slice 2 and implement only the deterministic `CandidateResolver` with synthetic candidates. Do not start `LyricsCoordinator` or any concrete provider in the same slice.
