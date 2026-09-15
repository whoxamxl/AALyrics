# AALyrics Core-First Roadmap

## Purpose

This roadmap prevents the project from drifting back into provider-driven architecture while also avoiding unnecessary reinvention of behavior that is already proven in the working Auto Lyrics fork.

AALyrics is built core-first, but the previous fork remains an active behavioral reference throughout development. Before implementing non-trivial behavior, check `docs/MIGRATION_INVENTORY.md` and the current fork to determine whether the behavior should be preserved, refactored, rewritten, or dropped.

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

### Phase 3.1 — Lyrics state ✅

- provider-independent lookup identity
- explicit idle/loading/ready/degraded/not-found/failure states
- pure lifecycle reduction
- stale-result rejection by lookup identity
- lifecycle invariants and unit tests

Merged in PR #6.

### Phase 3.2 — Candidate selection boundary ✅

- provider-independent `CandidateSelector`
- normalized `Track` + `LyricsCandidate` inputs
- explicit selection preferences
- nullable no-winner result
- fake-selector contract tests

Merged in PR #8. This phase intentionally defined only the port; the mature resolver was reserved for post-gate adaptation.

### Phase 3.3 — Lyrics coordinator ✅

- fresh request identity for every lookup
- concurrent provider fan-out
- partial provider failure isolation
- cancellation propagation
- no-result and terminal-failure behavior
- cancellation/supersession on a newer lookup
- stale-result protection through lookup identity and atomic state reduction
- one handoff to `CandidateSelector` after candidates are collected

Merged in PR #9, with integration/readiness follow-up in PRs #10 and #11.

### Phase 3.4 — Core integration/readiness tests ✅

The pure provider-independent domain flow is covered end-to-end with fake providers and a fake selector. Validated behavior includes provider-order independence, failure isolation, no-result/degraded/failure states, fresh lookup identity, clear ownership, and stale-result rejection. PR #11 fixed and regression-tested a concurrent stale-publication race by making `LyricsState` transitions atomic through `MutableStateFlow.update`.

### Phase 4 — Playback boundary ✅

Merged in PR #12.

```text
Android MediaController
        ↓
MediaControllerSnapshotAdapter       (:platform:media)
        ↓
PlaybackSnapshot                     (:core:model)
        ↓
PlaybackTrackIdentity                (:core:model)
        ↓
PlaybackLyricsController             (:core:lyrics)
        ↓
LyricsLookupLifecycle
        ↓
LyricsCoordinator
```

Phase 4 established explicit track ownership, stable-reference/source-media/metadata identity fallback, Spotify playback-reference normalization at the platform edge, and JVM tests proving that position/status/duration-only churn does not restart lyrics lookup. Android media framework types remain inside `:platform:media`.

### Phase 5 — Core Readiness validation ✅

Merged in PR #14 after the explicit STOP GATE review.

Phase 5 validated, rather than expanded, the provider-independent foundation:

- executable CI architecture checks for pure Kotlin/JVM core modules,
- one shared `LyricsState` contract for phone and automotive features,
- feature ownership guards preventing provider fetching/ranking in UI modules,
- Android MediaSession / MediaController confinement to `:platform:media`,
- provider-specific behavior exclusion from pure core,
- regression evidence for provider order, stale-result rejection, failure isolation, and playback identity,
- migration-inventory re-review against `whoxamxl/auto-lyrics` `main` at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).

Detailed evidence is recorded in `docs/CORE_READINESS_GATE.md`.

The review process also established `AGENTS.md` review-loop discipline: lightweight architecture checks are best-effort regression guardrails, not formal static-analysis proofs, and review scope must not expand indefinitely around theoretical bypasses.

## Current work

### Phase 6 — Production candidate selection migration

The Core Readiness STOP GATE has been explicitly accepted, so the first implementation-adaptation slice is the mature cross-provider resolver/matching policy.

Current branch/PR scope:

- keep `CandidateSelector` as the dependency-inversion port in `:core:lyrics`,
- add pure Kotlin `:provider:selection` as the production implementation layer,
- refactor generic similarity/version matching out of the old LRCLIB-owned location,
- preserve mature metadata, payload-quality, source-confidence, synchronized/plain fallback, cross-script, and karaoke preference behavior,
- add only provider-neutral candidate evidence required by the proven resolver,
- port resolver/version-context regression cases to AALyrics models,
- keep provider networking/client implementations out of this phase.

The working fork was re-checked before implementation and remains at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).

The key compile-time ownership rule is:

```text
:app (future wiring)
   ↓
:provider:selection ─────→ :core:lyrics (CandidateSelector port)
                                  ↓
                            :provider:api
                                  ↓
                             :core:model
```

`:core:lyrics` does not depend on `:provider:selection`.

## Next

### Concrete provider adapters

After the production selector migration is merged, adapt concrete providers one at a time rather than bulk-porting the old app.

The provider order should be chosen from current coverage/value and migration complexity, while preserving these rules:

- each provider conforms to `LyricsProvider`,
- provider-local HTTP/search/parsing stays in its adapter,
- providers return normalized candidates and search evidence rather than final global scores,
- generic matching logic is not duplicated back into provider clients,
- PetitLyrics configuration values remain unchanged unless explicitly requested,
- every provider slice receives provider-specific regression tests before integration.

The exact first provider is decided immediately before that slice by re-checking the then-current working fork and migration inventory.

## Later phases

Later work includes concrete providers, cache, translation, demand/session gating, Android Auto/phone presentation, timing controls, karaoke rendering, persistence, release/signing, and regression comparison against the previous fork.

These should remain separate responsibilities and topic branches. Do not use future feature needs as a reason to turn `LyricsCoordinator`, `PlaybackLyricsController`, or the production selector into a new god object.

## Working method

Each implementation phase is split into small topic branches and PRs. `TASK.md` records the active branch-local plan/status when relevant, while this document remains the durable project roadmap.

Before starting any non-trivial implementation slice:

1. check `docs/MIGRATION_INVENTORY.md`,
2. inspect the current `whoxamxl/auto-lyrics` main branch for equivalent behavior,
3. classify the behavior as PRESERVE, REFACTOR, REWRITE, or DROP,
4. define the current PR acceptance criteria,
5. implement only the AALyrics-specific work that is necessary,
6. run CI and the bounded review process in `AGENTS.md`,
7. stop before merge for explicit approval.
