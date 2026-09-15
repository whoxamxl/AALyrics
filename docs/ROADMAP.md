# AALyrics Core-First Roadmap

## Purpose

This roadmap prevents the project from drifting back into provider-driven architecture while also avoiding unnecessary reinvention of behavior that is already proven in the working Auto Lyrics fork.

AALyrics is built core-first, but the previous fork is an active behavioral reference throughout development. Before implementing non-trivial behavior, check `docs/MIGRATION_INVENTORY.md` and the current fork to determine whether the behavior should be preserved, refactored, rewritten, or dropped.

Implementation code from the previous fork is still not imported before the final stop gate below. Until then, the fork is used to inform boundaries, tests, and migration decisions.

## Completed

### Phase 0 — Foundation ✅

- Greenfield repository and application identity
- Module boundaries
- CI and branch naming checks
- Source-available licensing and contribution policy

### Phase 1 — Domain model ✅

- Track and track references
- Playback snapshot/status/source
- Plain, line-timed, and word-timed lyrics
- Normalized synchronization type
- Timing invariants and unit tests

### Phase 2 — Provider contract ✅

- Provider identity and descriptor
- Provider-independent request
- Normalized lyrics candidate
- Suspending provider search contract
- Contract tests

This phase defines only the boundary. It does not include any concrete provider implementation.

## Current work

### Phase 3 — Lyrics core

Build only the AALyrics-specific provider-independent core that does not already exist as mature, proven behavior in the working fork.

#### 3.1 Lyrics state

Define explicit state for at least:

- idle / no active track
- loading
- resolved lyrics
- no lyrics available
- recoverable provider failure / degraded result
- terminal failure when no usable result exists

State must be consumable by both phone and automotive presentation layers without provider knowledge.

#### 3.2 Candidate selection boundary

Do **not** redesign the mature scoring/selection policy from scratch.

The working fork already contains a substantial `LyricsProviderResolver`, regression tests, recording-version matching, cross-script handling, payload-quality scoring, source-confidence policy, and karaoke-aware selection. That behavior is classified for later preservation/refactoring in `docs/MIGRATION_INVENTORY.md`.

Before the stop gate, AALyrics should define only the narrow core boundary needed by orchestration, for example a provider-independent selector contract and selection preferences. Tests may use a fake selector.

Goals:

- provider execution order must not determine the winner
- `LyricsCoordinator` must not depend on a concrete resolver implementation
- the future fork-derived resolver must be replaceable behind the boundary
- no new scoring weights, metadata similarity algorithm, source preference policy, or recording-version algorithm are invented here

#### 3.3 Lyrics coordinator

Coordinate `LyricsProvider` contracts and publish `LyricsState`.

Cover at least:

- request start for a new track
- multiple provider results
- partial provider failure
- no-result behavior
- cancellation
- stale-result rejection after track change
- handoff to the candidate-selection boundary

Use fake providers and a fake selector only.

#### 3.4 Core integration tests

Prove the complete domain flow without Android or network access:

```text
Fake Playback Input
        ↓
LyricsCoordinator
        ↓
Fake Providers
        ↓
CandidateSelector boundary
        ↓
LyricsState
```

The purpose is to prove orchestration and lifecycle correctness, not to duplicate the fork's mature selection policy.

## Next

### Phase 4 — Playback boundary

Define and test the boundary between Android playback/media events and the pure lyrics core.

Goals:

- normalize MediaSession data into `PlaybackSnapshot`
- make track-change identity explicit
- make position/playback-state updates separate from lyrics lookup identity
- keep Android framework types inside `:platform:media`
- drive the lyrics core through a narrow provider-independent interface

Before implementing playback behavior, consult `docs/MIGRATION_INVENTORY.md` and the fork for existing proven identity/lifecycle behavior such as Spotify track identity and demand control.

Real Android integration may be implemented here, but it must not contain provider logic.

### Phase 5 — Core Readiness validation

Before any concrete lyrics provider is implemented or any previous-fork code is adapted, verify all of the following:

- `:core:model`, `:provider:api`, and `:core:lyrics` contain no Android framework dependency
- core orchestration is exercised entirely with fake providers
- selection is behind a stable provider-independent boundary
- provider execution order cannot determine the selected result accidentally
- track changes cannot allow stale results to overwrite current state
- one provider failure is isolated from other providers
- phone and automotive layers can consume one shared `LyricsState`
- no UI layer performs provider fetching or ranking
- concrete provider quirks are not represented as core application behavior
- architecture and tests make the intended dependency direction difficult to violate accidentally
- migration inventory has been reviewed so proven fork behavior is not needlessly reimplemented

If any item fails, the project stays in core-first development.

## STOP GATE — Concrete provider / code adaptation

**Stop here for explicit review.**

Reaching this point means the AALyrics-specific foundation is ready to receive proven behavior from the working fork behind the new boundaries. Do not automatically continue into LRCLIB, Musixmatch, PetitLyrics, SyncLRC, or bulk code migration.

At this gate we will review:

1. whether the core architecture actually matches the intended design,
2. whether provider contracts or selector boundaries need adjustment,
3. the current `MIGRATION_INVENTORY.md` classifications,
4. which mature fork behaviors should be preserved with minimal semantic change,
5. which implementations need structural refactoring to remove Android/provider coupling,
6. which provider should be adapted first,
7. which code must instead be rewritten or dropped.

Only after that explicit review does implementation adaptation begin.

## Later phases — intentionally not started

After the stop gate, likely work includes adapting the mature resolver/matching logic, concrete provider adapters, cache, translation, Android Auto/phone presentation, timing controls, karaoke behavior, persistence, release/signing, and regression comparison against the previous fork.

These are intentionally not scheduled in detail yet. The migration inventory should be refined as the working fork evolves so AALyrics does not duplicate already-proven work.

## Working method

Each implementation phase should be split into small topic branches and PRs. When a phase is large, a branch-local `TASK.md` may be used as a short execution checklist, while this document remains the durable project roadmap.

Before starting any non-trivial implementation slice:

1. check `docs/MIGRATION_INVENTORY.md`,
2. inspect the current `whoxamxl/auto-lyrics` main branch for equivalent behavior,
3. classify the behavior as PRESERVE, REFACTOR, REWRITE, or DROP,
4. only then implement the AALyrics-specific work that is actually necessary.
