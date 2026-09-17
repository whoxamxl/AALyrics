# Lyrics Capability Architecture Foundation

## Branch and baseline

- Branch: `docs/lyrics-capability-foundation`.
- Base: main `55f918896df501ffc3c196a0cda92c88b6a159d8` after PR #30 merged.
- Scope: documentation-only architecture foundation for cache, translation, timing/calibration, karaoke projection, and presentation state.
- No production implementation is authorized in this slice.

## Goal

Define stable responsibility boundaries and dependency direction before feature implementation begins, while deliberately leaving algorithm, storage, provider, UI, and API details open where implementation evidence is still missing.

This slice should make later implementations difficult to place in the wrong layer without prematurely locking the project into interfaces that may prove unsuitable.

## Acceptance criteria

- Document each capability independently.
- Define ownership, allowed dependencies, forbidden dependencies, invariants, and deferred decisions.
- Keep provider/network/storage/platform details outside pure transformation logic.
- Preserve source lyrics and source timing as canonical inputs; later features should derive views/projections rather than mutating provider truth in place.
- Keep Phone and Android Auto surface state separate while allowing shared presentation-ready facts where semantics are genuinely shared.
- Keep karaoke/timing calculations UI-framework-independent.
- Keep cache storage replaceable and prevent UI/provider modules from directly owning persistence policy.
- Keep translation independent of `LyricsCoordinator` orchestration and concrete provider adapters.
- Do not create speculative modules, interfaces, DTOs, databases, or production code in this branch.
- Record unresolved choices explicitly instead of deciding them without implementation evidence.

## Completed documents

- [x] `docs/CACHE_ARCHITECTURE.md`
- [x] `docs/TRANSLATION_ARCHITECTURE.md`
- [x] `docs/TIMING_ARCHITECTURE.md`
- [x] `docs/KARAOKE_ARCHITECTURE.md`
- [x] `docs/PRESENTATION_STATE_ARCHITECTURE.md`
- [x] `docs/LYRICS_PIPELINE_ARCHITECTURE.md`
- [x] integrate the foundation into `docs/ARCHITECTURE.md`
- [x] define Phase 11.x implementation slices in `docs/ROADMAP.md`
- [x] align `docs/MIGRATION_INVENTORY.md` with the capability foundation
- [x] complete docs review; stale TASK completion/file-name metadata was corrected before merge
- [x] move the reviewed head to a CI-compliant `docs/*` branch before PR because repository branch-name validation does not allow `architecture/*`

## Scope guard

This branch establishes seams, not implementation contracts. It must not add production dependencies, Gradle modules, framework integrations, persistence engines, translation engines, timing algorithms, karaoke rendering code, or finished Phone/Android Auto state models.

Each future implementation capability remains a separate topic branch and PR with its own migration re-check, acceptance criteria, tests, review, and merge approval.
