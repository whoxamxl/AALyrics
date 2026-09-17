# Cache Architecture Foundation

## Purpose

Define the architectural seam for future lyrics caching without choosing a storage engine, cache schema, TTL policy, or final cache unit before implementation evidence exists.

The working fork contains `lyrics/LyricsCache.kt`. Migration intent remains **REFACTOR**: preserve useful cache behavior after re-checking the current fork, but do not carry Android/storage coupling into lyrics core.

## Stable ownership rules

Cache is a data-access capability. It is not presentation state, provider ranking policy, MediaSession state, or translation/timing policy.

Stable direction:

```text
lyrics/application policy
        ↓
cache abstraction
        ↓
storage adapter
        ↓
platform/storage technology
```

Rules:

- core/application logic may depend on a cache abstraction, never an Android database/preferences implementation;
- Phone and Android Auto UI must not read or write cache storage directly;
- concrete providers must not own global cache policy;
- provider-specific transport DTOs must not become the durable cross-feature cache contract;
- cache hits must still respect current request identity, selection preferences, stale-result protection, and cancellation ownership;
- cache use must not allow obsolete track results to replace current state;
- persistence failures must degrade to normal retrieval rather than corrupt lyrics lifecycle state.

## Canonical data and derivation

A cache must preserve the distinction between canonical fetched lyrics and later derived capabilities.

```text
provider result / normalized lyrics facts
        ↓
canonical cacheable material
        ↓
selection / translation / calibration / karaoke projections
```

Translation, user timing calibration, active karaoke position, and surface-specific UI state must not be silently folded into one opaque lyrics cache entry.

Where useful, those capabilities may later define their own associated cached artifacts keyed to canonical lyrics identity, but that is a later implementation decision.

## Placement intentionally deferred

The exact primary cache unit is **not fixed by this document**. Viable implementation shapes include:

1. normalized provider-result caching before cross-provider selection;
2. selected normalized lyrics caching after candidate selection;
3. a layered combination of both where evidence justifies the complexity.

The implementation slice must inspect the current working fork, expected provider cost/identity behavior, preference-change semantics, and invalidation requirements before choosing.

The architecture requirement is that whichever placement is chosen remains behind a replaceable cache boundary and does not make `LyricsCoordinator`, providers, or UI own storage details.

## Identity requirements

A future cache design must explicitly define:

- track/request identity used for lookup;
- provider identity when provider-local artifacts are cached;
- lyrics/content identity when derived artifacts reference canonical lyrics;
- candidate-selection preference interaction;
- schema/version compatibility;
- stale or invalid entry behavior.

Identity should be based on normalized domain facts, not Android object identity or presentation state.

## Deferred decisions

Do not decide in this foundation slice:

- Room, SQLite, DataStore, files, memory-only, or another storage engine;
- exact table/schema layout;
- TTL values;
- LRU/size policy;
- negative caching;
- whether provider failures/not-found outcomes are cached;
- encryption/compression;
- prefetching;
- provider-result versus selected-result primary cache unit;
- cache invalidation UI or settings.

These decisions require implementation-specific evidence and tests.

## Future implementation gate

Before implementing cache:

1. re-check the current working-fork `LyricsCache` implementation and call sites;
2. classify concrete behaviors as PRESERVE / REFACTOR / DROP;
3. decide cache placement and identity explicitly;
4. define cache-hit equivalence and invalidation tests;
5. prove that preference changes, track supersession, provider failure isolation, and demand gating remain correct;
6. keep storage adapters outside pure core;
7. implement on a dedicated topic branch and stop before merge for approval.
