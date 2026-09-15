# Core Readiness Gate

## Purpose

This document records the Phase 5 validation evidence for the AALyrics core-first architecture. Passing this gate means the provider-independent foundation is ready for an explicit migration review. It does **not** authorize automatic provider or resolver adaptation.

Validation baseline:

- AALyrics base: Phase 4 merged in PR #12
- working-fork reference: `whoxamxl/auto-lyrics` `main` at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- migration inventory re-reviewed on 2026-09-15

## Gate evidence

| Readiness invariant | Evidence | Result |
| --- | --- | --- |
| Pure core has no Android dependency | `:core:model`, `:provider:api`, and `:core:lyrics` use the Kotlin/JVM plugin. `scripts/verify-architecture.sh` rejects Android/AndroidX imports and Android/network dependencies in their production sources. | PASS |
| Core orchestration is provider-independent | `LyricsCoordinatorTest` and `CoreFlowIntegrationTest` exercise orchestration with fake `LyricsProvider` and fake `CandidateSelector` implementations. No concrete provider is required by the core tests. | PASS |
| Selection is behind one stable boundary | `CandidateSelector` is the selection port. `LyricsCoordinator` aggregates normalized candidates and delegates the winner decision through that port. | PASS |
| Provider completion order is not winner policy | Core integration coverage delays provider completion independently and verifies that selection happens only after candidate collection. | PASS |
| Stale lookup results cannot overwrite current ownership | Lookup identity is explicit, reducer completion checks reject obsolete ids, and PR #11 changed state transitions to atomic `MutableStateFlow.update`; the concurrent stale-publication regression is covered. | PASS |
| One provider failure is isolated | Core integration tests cover healthy-result + failed-provider → `Degraded`, all failures → `Failed`, and healthy no-result → `NotFound`. | PASS |
| Playback churn does not restart lookup | `PlaybackTrackIdentity` excludes position/status/rate/duration, and `PlaybackLyricsControllerTest` covers preserve/supersede/clear behavior. | PASS |
| Android media framework types stay at the platform edge | `MediaControllerSnapshotAdapter` lives in `:platform:media`; the architecture audit rejects MediaSession/MediaController imports elsewhere. | PASS |
| Phone and automotive consume the same lyrics state | Both feature modules depend on `:core:lyrics`; the architecture audit requires a single `LyricsState` declaration and shared core consumption. | PASS |
| UI does not fetch/rank providers | Feature modules have no direct provider/platform-media dependency, and the architecture audit rejects provider API imports plus `LyricsProvider` / `CandidateSelector` ownership in feature sources. | PASS |
| Provider quirks do not become core behavior | The architecture audit rejects LRCLIB, Musixmatch, PetitLyrics, SyncLRC, and Spotify-specific behavior in pure-core production sources. Spotify playback normalization remains in `:platform:media`. | PASS |
| Dependency direction is guarded, not only documented | The architecture audit runs in CI before build/test, while module declarations and existing JVM tests enforce the core boundaries at compile/test time. | PASS |
| Migration inventory reflects the current fork | `docs/MIGRATION_INVENTORY.md` was re-reviewed against fork `main` at `8484bed2...`; resolver/provider/playback classifications remain valid. | PASS |

## Architecture conclusion

The core-first split is ready for the migration decision point:

```text
Android MediaController
        ↓
:platform:media
        ↓
PlaybackSnapshot / PlaybackTrackIdentity
        ↓
PlaybackLyricsController
        ↓
LyricsCoordinator
        ↓
LyricsProvider[] → normalized LyricsCandidate[]
        ↓
CandidateSelector
        ↓
LyricsState
        ↓
:feature:phone / :feature:automotive
```

The important ownership rules are now executable constraints rather than only prose: Android media types are confined to the platform adapter, features cannot own provider selection/fetching, and provider-specific behavior cannot be introduced into pure core without failing CI.

## No blockers found

Phase 5 did not expose a production architecture defect. No production implementation change was required for this gate; the new executable checks are build/architecture validation only.

## STOP GATE

Provider/resolver adaptation must stop here until the migration review is explicitly accepted.

The next review should decide:

1. whether `CandidateSelector` is sufficient for adapting the mature `LyricsProviderResolver`,
2. which generic matching/version utilities should be extracted before provider adapters,
3. the first concrete provider migration order,
4. how to preserve resolver regression cases with minimal semantic change,
5. where cache, translation, demand gating, timing, and presentation behavior belong,
6. whether any migration-inventory classification should change before implementation begins.

Until that review is complete, do not adapt LRCLIB, Musixmatch, PetitLyrics, SyncLRC, the production resolver, or other previous-fork implementation code.
