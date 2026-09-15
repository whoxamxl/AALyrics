# Candidate Selection Port Task

This branch implements only Phase 3.2 of `docs/ROADMAP.md`.

## Scope guard

Use only AALyrics domain/provider contracts and synthetic test data. Do not add concrete provider implementations, networking, Android media code, or copy/adapt resolver code from the previous Auto Lyrics fork.

Before starting any non-trivial behavior, inspect the current fork and `docs/MIGRATION_INVENTORY.md` so proven behavior is not independently reinvented.

## Slice 2 — Candidate selection boundary

- [x] Define a provider-independent `CandidateSelector` contract.
- [x] Pass the requested `Track` and normalized `LyricsCandidate` values through the boundary.
- [x] Define only the currently required selection preference: preferred synchronization type.
- [x] Allow a selector to return no winner.
- [x] Expose `:provider:api` transitively because the public selector contract uses `LyricsCandidate`.
- [x] Add contract tests using a fake selector.

## Explicitly out of scope

- [ ] scoring weights
- [ ] metadata similarity
- [ ] source-confidence rules
- [ ] recording-version matching
- [ ] cross-script matching
- [ ] karaoke-specific winner policy
- [ ] concrete provider implementation
- [ ] adapting `LyricsProviderResolver`
- [ ] `LyricsCoordinator`

The mature resolver behavior remains reserved for post-gate adaptation behind this port. See `docs/MIGRATION_INVENTORY.md`.

## Stop gate

After Phase 5, stop for explicit architecture review before any LRCLIB, Musixmatch, PetitLyrics, SyncLRC, resolver implementation, or previous-fork code is implemented or adapted.

## Next action

Open and review the Phase 3.2 PR. After it is merged, create `feature/lyrics-coordinator` from `main` and implement only orchestration with fake providers and a fake `CandidateSelector`. Do not start resolver adaptation or any concrete provider in that branch.
