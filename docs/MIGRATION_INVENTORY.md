# AALyrics Migration Inventory

## Purpose

This file prevents AALyrics from reimplementing behavior that is already mature and working in `whoxamxl/auto-lyrics`.

Before starting a non-trivial implementation slice, inspect the current fork and update this inventory. The classifications below describe **behavioral migration intent**, not permission to copy code before the Core Readiness Gate.

## Classification

| Class | Meaning |
| --- | --- |
| **PRESERVE** | Keep behavior with minimal semantic change. Structural cleanup is allowed only if regression behavior remains equivalent. |
| **REFACTOR** | Keep the proven behavior, but move ownership/dependencies to the AALyrics architecture. |
| **REWRITE** | Preserve the user-facing requirement where applicable, but replace the old implementation because it is too coupled or structurally unsuitable. |
| **DROP** | Do not migrate unless later evidence shows it is still required. |

## Reference baseline

Initial review baseline:

- Repository: `whoxamxl/auto-lyrics`
- Branch: `main`
- Reviewed commit: `6ad450213f1896a5a65e217f15d1544c6e646d0e`

Core Readiness Gate re-review:

- Repository: `whoxamxl/auto-lyrics`
- Branch: `main`
- Reviewed commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Re-reviewed: 2026-09-15

The gate re-review confirmed that the mature resolver and provider clients remain present, `MediaTracker` still combines Android media, provider, cache, translation, timing, and presentation responsibilities, and the explicit Spotify playback identity and lyrics-demand helpers remain separate behaviors. No classification below needs to change before the STOP GATE.

The fork continues to evolve. Re-check `main` before migration work rather than assuming this snapshot remains current.

## Selection and metadata matching

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LyricsProviderResolver.kt` | **REFACTOR** | production implementation behind the `:core:lyrics` candidate-selection port | Preserve the mature cross-provider behavior instead of inventing a second scoring system. Current behavior includes metadata, payload-quality, source-confidence, synchronization, karaoke, cross-script, and fallback policy. Remove dependencies on concrete provider clients while retaining semantics. |
| `lyrics/RecordingVersionContext.kt` | **PRESERVE / REFACTOR** | pure matching utility used by the future production selector and provider-local matchers | Explicit live/remix/remaster/etc. context handling is already heavily refined and regression-sensitive. Preserve semantics while removing accidental ownership by LRCLIB-specific code. |
| generic similarity helpers currently in `LrcLibClient` (`stringSimilarity`, artist matching, duration similarity, version compatibility) | **REFACTOR** | provider-independent matching utility/port implementation | These are used beyond LRCLIB semantics and should not remain owned by one provider adapter. Preserve tested behavior; relocate it. |
| `lyrics/MetadataCleaner.kt` | **PRESERVE / REFACTOR** | metadata normalization utility at an appropriate domain/platform boundary | Do not independently reinvent metadata cleanup. Verify call sites and tests before choosing the final module. |
| `LyricsProviderResolverTest.kt` and version-context tests | **PRESERVE** | regression specification for the migrated selector | Adapt tests to AALyrics domain models. Prefer preserving behavioral cases and thresholds over rewriting expectations from memory. |

## Provider-local search and parsing

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LrcLibClient.kt` | **REFACTOR** | future `:provider:lrclib` adapter plus extracted generic matching utilities | Preserve mature search/fallback behavior, but separate HTTP/provider concerns from generic track matching. Do not start before the stop gate. |
| `lyrics/MusixmatchClient.kt` | **REFACTOR** | future Musixmatch provider adapter | Preserve proven behavior and tests where legally/technically appropriate; conform output to `LyricsProvider`. Do not let Musixmatch-specific decisions leak into core state. |
| `lyrics/PetitLyricsClient.kt` | **REFACTOR** | future PetitLyrics provider adapter | Preserve working behavior. Existing PetitLyrics configuration values are not to be changed during migration unless explicitly requested. |
| `lyrics/SyncLrcClient.kt` | **REFACTOR** | future SyncLRC provider adapter | Preserve useful behavior and R8/regression fixes, but adapt to the common provider contract. |
| `lyrics/LrcParser.kt` | **PRESERVE / REFACTOR** | pure parser utility owned by the relevant provider/parser layer | Parsing behavior is already tested. Reuse semantics rather than writing a new LRC parser without evidence. |

## Playback identity and request lifecycle

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `media/SpotifyTrackIdentity.kt` | **PRESERVE / REFACTOR** | `:platform:media` identity normalization feeding `core:model` references | Preserve the Spotify-specific robustness, but keep Spotify/platform details outside lyrics core. Phase 4 now reflects these semantics in the platform boundary. |
| `media/LyricsDemandController.kt` | **PRESERVE / REFACTOR** | playback/application demand boundary after core lifecycle is stable | Review proven demand/lifecycle behavior before creating a replacement. It remains intentionally outside the Core Readiness Gate implementation. |
| `media/LyricsVariantTransition.kt` | **PRESERVE / REFACTOR** | future state/variant transition policy if still required | Small but potentially regression-sensitive. Inspect call sites before migration. |
| `media/MediaTracker.kt` | **REWRITE** | split across `:platform:media`, `:core:lyrics`, composition root, and later feature-specific services | Preserve observable behavior through tests/reference, but do not migrate the monolithic ownership model. This class mixes playback tracking, provider calls, cache, translation, timing, art/colors, and state. |
| `media/MediaListenerService.kt` | **REWRITE / REFACTOR** | thin Android adapter in `:platform:media` | Preserve required Android behavior, but keep domain orchestration outside the service. |

## State and presentation

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `model/Models.kt` legacy `LyricsState` | **REWRITE** | AALyrics `:core:model` + `:core:lyrics` state types | Old state mixes Android `Bitmap`, playback, lyrics, translation, UI indices, offset, and colors. Preserve required capabilities later, but not the monolithic DTO. |
| `MainActivity.kt` | **REWRITE** | `:feature:phone` | Use as behavioral/UI reference only. New UI consumes shared domain state and does not own provider orchestration. |
| `auto/LyricsBrowserService.kt` | **REWRITE** | `:feature:automotive` plus thin Android service boundary | Preserve useful Android Auto UX/behavior, but the service must not fetch/rank lyrics. |
| `PerformanceActivity.kt` / `PerformanceLyricsView.kt` | **REVIEW BEFORE DECISION** | future phone feature if retained | Do not reimplement until product intent is explicitly decided. |

## Timing, karaoke, and calibration

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/KaraokeTiming.kt` | **PRESERVE / REFACTOR** | future pure timing utility | Keep tested word-timing semantics when karaoke work begins. |
| `util/SyncCalibration.kt` | **PRESERVE / REFACTOR** | future pure timing/calibration utility | Preserve tested calibration semantics; do not duplicate during current core work. |
| `util/LyricWordLayout.kt` | **PRESERVE / REFACTOR** | presentation/timing utility later | Valuable tested behavior, but not part of current core readiness work. |
| `ui/KaraokeSweepSpan.kt` | **REWRITE / REFACTOR** | Android phone presentation only | Rendering is Android-specific; preserve visual behavior where useful but isolate it from timing/domain logic. |

## Translation, cache, artwork, and other later features

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LyricsTranslator.kt` | **REFACTOR** | future translation service/feature boundary | Preserve working translation behavior and status handling, but do not put it in `LyricsCoordinator`. |
| `lyrics/TranslationLanguages.kt` | **PRESERVE** | future translation model/config | Small stable domain/config behavior. |
| `lyrics/LyricsCache.kt` | **REFACTOR** | future cache port + storage adapter | Preserve cache semantics where useful, but core should depend on a cache abstraction rather than Android storage details. |
| `util/AlbumColorExtractor.kt` | **PRESERVE / REFACTOR** | presentation/platform utility | Android-specific feature; not part of lyrics core. |
| `util/AudioSyncHelper.kt` | **DROP unless proven used** | none by default | Previous inspection found no clear current usage. Do not migrate dead auto-sync logic without an active call path and explicit product requirement. |

## Current AALyrics work versus fork behavior

The following current AALyrics work is intentionally new rather than duplicated from the fork:

| AALyrics work | Why it remains new |
| --- | --- |
| explicit `LyricsLookupId` / request identity | Needed to make stale-result rejection a first-class core invariant rather than incidental asynchronous behavior. |
| sealed provider-independent `LyricsState` lifecycle | Replaces the old Android/UI-heavy state DTO and creates one shared contract for phone and automotive presentation. |
| pure reducer/state transitions | Provides testable lifecycle semantics independent of Android, providers, cache, translation, and UI. |
| `LyricsCoordinator` boundary | Replaces orchestration responsibilities currently mixed into `MediaTracker`. |
| candidate-selection **port** | Allows the proven fork resolver to be adapted later without coupling the coordinator to its implementation. |
| explicit playback identity and `PlaybackLyricsController` | Separates track ownership from position/status updates so media churn does not restart lyrics lookup. |

## Migration rule for the current phase

Before the Core Readiness Gate:

```text
Inspect fork behavior
        ↓
Update this inventory
        ↓
Define AALyrics boundary only
        ↓
Use fakes in core tests
        ↓
STOP before implementation adaptation
```

Do not port resolver/provider code yet. Do not create a competing selection algorithm either.

## Stop-gate review

At the Core Readiness Gate, review this file against the then-current fork and decide the first migration batch. The likely first batch is the mature resolver/matching behavior because it is already well-tested and central to preserving current app quality, but that decision is intentionally deferred until the core interfaces are stable.
