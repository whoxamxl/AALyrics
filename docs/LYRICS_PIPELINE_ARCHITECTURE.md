# Lyrics Capability Pipeline Architecture Foundation

## Purpose

Define the stable architectural relationship between future lyrics capabilities without prematurely fixing module names, concrete APIs, storage technology, translation engines, calibration models, karaoke DTOs, or surface state shapes.

This document is the umbrella for:

- `docs/CACHE_ARCHITECTURE.md`
- `docs/TRANSLATION_ARCHITECTURE.md`
- `docs/TIMING_ARCHITECTURE.md`
- `docs/KARAOKE_ARCHITECTURE.md`
- `docs/PRESENTATION_STATE_ARCHITECTURE.md`

Those documents own capability-specific rules. This document defines only how the capabilities may compose without collapsing their responsibilities into one pipeline object or one oversized state model.

## Stable architectural intent

AALyrics already has a production playback-to-lyrics spine:

```text
MediaSession runtime
        ↓
PlaybackSnapshot
        ↓
LyricsDemandGate
        ↓
PlaybackLyricsController
        ↓
LyricsCoordinator
        ↓
provider fan-out
        ↓
CandidateSelector
        ↓
LyricsState
```

Future capabilities must extend this system through explicit seams rather than move their responsibilities into `LyricsCoordinator`, providers, media-session code, or presentation.

The capability foundation is conceptually:

```text
                provider / normalized lyrics results
                            │
                            v
                     canonical lyrics
                            │
             ┌──────────────┼──────────────┐
             │              │              │
             v              v              v
          caching       translation   timing/calibration
             │              │              │
             │              │              v
             │              │       effective timing
             │              │              │
             │              └──────┐       │
             │                     │       v
             │                     │   karaoke projection
             │                     │       │
             └─────────────────────┴───────┤
                                           v
                              presentation-ready facts
                                  /               \
                                 v                 v
                           Phone state      Automotive state
                                 │                 │
                                 v                 v
                           Compose UI      host-rendered UI
```

This is an ownership/dependency sketch, not a requirement that runtime execution always follows one fixed linear sequence.

## Canonical source versus derived capabilities

The central invariant is that canonical normalized lyrics remain distinguishable from all derived behavior.

```text
canonical lyrics
├─ source text
├─ source timing
├─ normalized metadata/source facts
│
├─ derived translation
├─ derived effective timing/calibration
└─ derived karaoke projection
```

Derived capabilities must not silently mutate canonical provider truth in place.

This distinction preserves:

- provider comparison and debugging;
- cache validity and migration;
- translation invalidation;
- calibration reset/recalculation;
- karaoke determinism;
- clean presentation mapping;
- later replacement of implementation technology without rewriting upstream ownership.

## Capability ownership

### Cache

Cache is a data-access capability. It may persist canonical or explicitly derived artifacts behind a replaceable abstraction, but storage technology and cache policy do not belong in providers, UI, or pure playback/media code.

Cache placement and cache unit remain deferred until implementation evidence exists.

See `docs/CACHE_ARCHITECTURE.md`.

### Translation

Translation is a derived lyrics capability. It consumes canonical lyrics, preserves original content, and produces translation artifacts/status associated with the exact canonical lyrics version and translation configuration.

Translation failure is not lyrics lookup failure.

See `docs/TRANSLATION_ARCHITECTURE.md`.

### Timing and calibration

Timing/calibration transforms canonical source timing into effective timing for playback-dependent behavior without overwriting source timestamps.

The exact calibration scope and transform remain deferred.

See `docs/TIMING_ARCHITECTURE.md`.

### Karaoke projection

Karaoke is a framework-neutral semantic projection over timed lyrics, effective timing, and playback position. It decides semantic playback facts such as active line/word/progress; it does not render Compose, spans, canvas primitives, or Android Auto templates.

See `docs/KARAOKE_ARCHITECTURE.md`.

### Presentation state

Presentation assembles independently owned application/capability facts into surface-specific state. Phone and automotive may share semantic facts but are not forced into one universal UI state shape.

See `docs/PRESENTATION_STATE_ARCHITECTURE.md`.

## Dependency rules

The future implementation must preserve inward-facing stable contracts and outward-facing replaceable adapters.

Allowed conceptual direction:

```text
platform / storage / network adapters
            ↓
replaceable capability boundaries
            ↓
normalized application/domain facts
            ↓
semantic projections
            ↓
surface-specific presentation mapping
```

Forbidden architectural shortcuts include:

- providers owning global cache policy;
- providers invoking translation engines;
- translation mutating canonical source timing;
- calibration formulas duplicated in Phone and automotive UI;
- karaoke semantics implemented separately by each renderer;
- UI reading cache/database storage directly;
- UI invoking provider implementations directly;
- cache/storage adapters depending on UI modules;
- `:ui:phone` depending on `:ui:automotive` or vice versa;
- MediaSession/platform code acquiring cache, translation, timing, karaoke, or presentation ownership;
- turning `LyricsCoordinator` into a general-purpose cache/translation/timing/presentation orchestrator;
- one monolithic application state that recreates the working fork's mixed legacy `LyricsState` ownership.

## Lifecycle and identity

Every asynchronous or persisted capability must respect current ownership and stale-result rules.

At minimum, later implementations must make explicit the identities they depend on:

```text
playback / track identity
        ↓
lyrics lookup identity
        ↓
canonical lyrics identity
        ├─ cache identity
        ├─ translation identity + configuration
        ├─ calibration scope/identity
        └─ karaoke projection input identity
```

Exact identity types are intentionally not defined here.

The stable requirements are:

- obsolete work must not publish as current after track or lyrics supersession;
- demand gating remains upstream ownership of whether lyrics work should be active;
- preference changes must not accidentally reuse incompatible artifacts;
- translation/calibration changes must not automatically refetch providers unless an explicitly documented future policy requires it;
- presentation observes capability state and must not become the lifecycle owner of provider work.

## Failure isolation

Capability failures remain local whenever valid lower-level information is still usable.

Examples:

```text
cache failure        -> fall through to normal retrieval
translation failure  -> original lyrics remain usable
timing setting issue -> source timing remains recoverable
karaoke unavailable  -> ordinary timed/plain lyrics remain usable
surface mapping issue -> must not corrupt canonical/application state
```

Later capability contracts should make these failure boundaries explicit rather than collapsing them into a generic lyrics failure.

## Implementation sequence

The foundation does not mandate one calendar order, but the expected dependency-friendly sequence is:

```text
Cache
  ↓
Translation
  ↓
Timing / Calibration
  ↓
Karaoke Projection
  ↓
Presentation State integration
  ↓
Phone / Automotive feature implementation
```

This order is primarily useful for reducing architectural churn. A later slice may change order if there is concrete product or implementation evidence, provided the dependency and ownership rules in this document remain intact.

Each capability must be implemented on its own topic branch/PR with its own acceptance criteria and current working-fork re-check where applicable.

## Module and API policy

This architecture foundation deliberately does **not** create empty modules or speculative interfaces for every future capability.

A module, port, DTO, state type, or service interface should be introduced only when an implementation slice can justify its actual inputs, outputs, lifecycle, and test contract.

The architecture should therefore stabilize **seams before signatures**:

```text
fix ownership and dependency direction now
                ↓
observe implementation requirements later
                ↓
introduce the smallest concrete contract
```

This avoids locking the project into abstractions that later cache, translation, timing, karaoke, or UI evidence would immediately invalidate.

## Relationship to the working fork

The working `whoxamxl/auto-lyrics` fork remains a behavioral oracle, not an ownership template.

Current migration intent already identifies:

- `LyricsCache` — **REFACTOR**;
- `LyricsTranslator` — **REFACTOR**;
- `TranslationLanguages` — **PRESERVE** where still applicable;
- `KaraokeTiming` / `SyncCalibration` / useful layout semantics — **PRESERVE / REFACTOR**;
- Android-specific karaoke rendering — **REWRITE / REFACTOR**;
- legacy mixed `LyricsState` — **REWRITE**.

Before each implementation slice, re-check current working-fork code and active call sites rather than treating an old snapshot or this foundation document as a substitute for implementation evidence.

## Deferred decisions

This umbrella intentionally does not decide:

- concrete future module names;
- whether each capability needs its own Gradle module;
- cache placement, schema, storage engine, TTL, or invalidation policy;
- translation engine/provider, batching, language detection, or persistence;
- calibration scope, persistence, offset/drift algorithm, or editing workflow;
- karaoke projection DTO shape, interpolation policy, update cadence, or visual rendering;
- shared presentation-facts DTO, ViewModel structure, DI framework, Flow composition, or surface state fields;
- final implementation order when product evidence justifies a different sequence.

Those decisions belong to dedicated implementation slices.

## Foundation acceptance rule

Future implementation is architecturally compatible with this foundation when it can demonstrate all of the following:

1. canonical lyrics/source timing remain distinguishable from derived artifacts;
2. capability-specific infrastructure is replaceable behind an explicit boundary;
3. stale work cannot publish into newer lyrics/playback ownership;
4. failures in optional derived capabilities do not erase valid lower-level state;
5. timing and karaoke semantics are not duplicated by individual presentation surfaces;
6. Phone and automotive presentation remain independent consumers of shared semantic facts;
7. providers, MediaSession runtime, `LyricsCoordinator`, and UI do not absorb unrelated capability ownership;
8. implementation-specific API/module choices are justified by the slice being built, not by speculative scaffolding.

The five capability documents remain authoritative for their specific domains; this file defines their shared architectural relationship.