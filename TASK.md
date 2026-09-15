# Lyrics Core Task

This branch implements Phase 3.2 of `docs/ROADMAP.md`.

## Scope guard

Use only AALyrics domain/provider contracts and synthetic test data. Do not add a scoring algorithm, concrete provider implementation, networking, Android media code, or copied/adapted implementation from the previous Auto Lyrics fork.

Before changing behavior, consult `docs/MIGRATION_INVENTORY.md` and the current `whoxamxl/auto-lyrics` fork. The mature resolver behavior is reserved for post-gate adaptation.

## Slice 2 — Candidate selection boundary (current)

- [ ] Define a provider-independent `CandidateSelector` contract.
- [ ] Define only the minimum selection preferences required by future orchestration.
- [ ] Keep input/output in normalized AALyrics domain/provider types.
- [ ] Add contract-level tests proving the boundary can be faked and does not depend on a concrete resolver.
- [ ] Run CI and open a PR.

## Explicitly out of scope

- No metadata similarity algorithm.
- No scoring weights or thresholds.
- No recording-version matching implementation.
- No source-confidence policy.
- No karaoke winner-selection implementation.
- No LRCLIB, Musixmatch, PetitLyrics, or SyncLRC code.
- No `LyricsCoordinator` yet.

The mature fork `LyricsProviderResolver`, recording-version logic, similarity helpers, and regression tests remain classified for later PRESERVE/REFACTOR work after the Core Readiness Gate.

## Later slices

- [ ] Slice 3: `LyricsCoordinator` using fake providers and a fake selector only.
- [ ] Slice 4: core integration tests.
- [ ] Phase 4: playback boundary, after reviewing proven fork identity/lifecycle behavior.
- [ ] Phase 5: Core Readiness validation.

## Stop gate

After Phase 5, stop for explicit architecture review before any concrete provider/resolver implementation or previous-fork code is adapted.

## Next action

Implement only the minimal candidate-selection port and its contract tests. Stop before `LyricsCoordinator`.
