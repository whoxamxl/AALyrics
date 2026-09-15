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
- [ ] Run CI and open a PR.

## Later slices — not part of Slice 1

- [ ] Slice 2: deterministic `CandidateResolver` with synthetic candidates.
- [ ] Slice 3: `LyricsCoordinator` using fake providers only.
- [ ] Slice 4: core integration tests.
- [ ] Phase 4: playback boundary.
- [ ] Phase 5: Core Readiness validation.

## Stop gate

After Phase 5, stop for explicit architecture review before any LRCLIB, Musixmatch, PetitLyrics, SyncLRC, or previous-fork code is implemented or adapted.

## Next action

Open the Slice 1 PR and verify the branch-name/build/unit-test checks. Do not start CandidateResolver until Slice 1 has been reviewed and merged.
