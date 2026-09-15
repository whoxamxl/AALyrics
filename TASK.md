# Lyrics Core Task

This branch implements Phase 3 of `docs/ROADMAP.md` in small slices.

## Scope guard

Use only AALyrics domain/provider contracts and synthetic test data. Do not add concrete provider implementations, networking, Android media code, or copy/adapt code from the previous Auto Lyrics fork.

## Slice 1 — Lyrics state lifecycle (current)

- [ ] Define a provider-independent lookup identity tied to a track.
- [ ] Define explicit `LyricsState` variants for idle, loading, resolved, degraded, not found, and terminal failure.
- [ ] Define pure state events/reduction rules.
- [ ] Reject stale completion events after a newer lookup starts.
- [ ] Add unit tests for lifecycle and invariants.
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

Implement Slice 1 only. Do not start CandidateResolver in this branch until Slice 1 has been reviewed and merged.
