# AALyrics Core-First Roadmap

## Purpose

This roadmap prevents the project from drifting back into provider-driven architecture. AALyrics core must be complete enough to define behavior before any real lyrics provider or previous-fork implementation is ported.

The previous Auto Lyrics fork is a behavioral reference only until the final stop gate below.

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

Build the provider-independent AALyrics decision engine using fake providers only.

#### 3.1 Lyrics state

Define explicit state for at least:

- idle / no active track
- loading
- resolved lyrics
- no lyrics available
- recoverable provider failure / degraded result
- terminal failure when no usable result exists

State must be consumable by both phone and automotive presentation layers without provider knowledge.

#### 3.2 Candidate resolver

Define deterministic cross-provider selection independently from provider call order.

Cover at least:

- track identity similarity
- duration agreement when known
- synchronization precision
- candidate completeness
- deterministic tie-breaking
- provider metadata without hard-wiring provider implementations into the resolver

The exact scoring policy should be unit-tested with synthetic candidates.

#### 3.3 Lyrics coordinator

Coordinate `LyricsProvider` contracts and publish `LyricsState`.

Cover at least:

- request start for a new track
- multiple provider results
- partial provider failure
- no-result behavior
- cancellation
- stale-result rejection after track change
- deterministic handoff to `CandidateResolver`

Use fake providers only.

#### 3.4 Core integration tests

Prove the complete domain flow without Android or network access:

```text
Fake Playback Input
        ↓
LyricsCoordinator
        ↓
Fake Providers
        ↓
CandidateResolver
        ↓
LyricsState
```

## Next

### Phase 4 — Playback boundary

Define and test the boundary between Android playback/media events and the pure lyrics core.

Goals:

- normalize MediaSession data into `PlaybackSnapshot`
- make track-change identity explicit
- make position/playback-state updates separate from lyrics lookup identity
- keep Android framework types inside `:platform:media`
- drive the lyrics core through a narrow provider-independent interface

Real Android integration may be implemented here, but it must not contain provider logic.

### Phase 5 — Core Readiness validation

Before any concrete lyrics provider is implemented or any previous-fork code is considered for adaptation, verify all of the following:

- `:core:model`, `:provider:api`, and `:core:lyrics` contain no Android framework dependency
- core behavior is exercised entirely with fake providers
- provider execution order cannot determine the selected result accidentally
- track changes cannot allow stale results to overwrite current state
- one provider failure is isolated from other providers
- phone and automotive layers can consume one shared `LyricsState`
- no UI layer performs provider fetching or ranking
- concrete provider quirks are not represented as core application behavior
- architecture and tests make the intended dependency direction difficult to violate accidentally

If any item fails, the project stays in core-first development.

## STOP GATE — Concrete provider / code adaptation

**Stop here for explicit review.**

Reaching this point means the AALyrics-specific foundation is ready for the first real provider adapter. Do not automatically continue into LRCLIB, Musixmatch, PetitLyrics, SyncLRC, or previous-fork code.

At this gate we will review:

1. whether the core architecture actually matches the intended design,
2. whether provider contracts need adjustment based on the now-working core,
3. which provider should be implemented first,
4. what behavior may be referenced or adapted from the previous fork,
5. what must instead be reimplemented cleanly from public API behavior/documentation.

Only after that explicit review does provider implementation begin.

## Later phases — intentionally not started

After the stop gate, likely work includes concrete provider adapters, cache, translation, Android Auto/phone presentation, timing controls, karaoke behavior, persistence, release/signing, and regression comparison against the previous fork.

These are intentionally not scheduled in detail yet. The architecture should be informed by proven core behavior rather than speculative future implementation.

## Working method

Each implementation phase should be split into small topic branches and PRs. When a phase is large, a branch-local `TASK.md` may be used as a short execution checklist, while this document remains the durable project roadmap.
