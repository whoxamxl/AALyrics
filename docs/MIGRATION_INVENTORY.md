# AALyrics Migration Inventory

## Purpose

This file prevents AALyrics from reimplementing behavior that is already mature and working in `whoxamxl/auto-lyrics`.

Before starting a non-trivial implementation slice, inspect the current fork and update this inventory. The classifications below describe **behavioral migration intent** and ownership in the AALyrics architecture.

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

Provider-selection migration re-check:

- Repository: `whoxamxl/auto-lyrics`
- Branch: `main`
- Reviewed commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Re-checked immediately before the first post-gate adaptation slice

The mature resolver/provider clients remain present, `MediaTracker` still combines Android media, provider, cache, translation, timing, and presentation responsibilities, and the explicit Spotify playback identity and lyrics-demand helpers remain separate behaviors.

The fork continues to evolve. Re-check `main` before every later migration slice rather than assuming this snapshot remains current.

## Selection and metadata matching

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LyricsProviderResolver.kt` | **PRESERVE / REFACTOR** | `:provider:selection` implementation of the `:core:lyrics` `CandidateSelector` port | First post-gate migration slice. Preserve mature cross-provider metadata, payload-quality, source-confidence, synchronization, karaoke, cross-script, and fallback policy without making core depend on the implementation. |
| `lyrics/RecordingVersionContext.kt` | **PRESERVE / REFACTOR** | pure matching utility in `:provider:selection` | Explicit live/remix/remaster/etc. context handling is heavily refined and regression-sensitive. Preserve semantics while removing accidental ownership by LRCLIB-specific code. |
| generic similarity helpers currently in `LrcLibClient` (`stringSimilarity`, artist matching, duration similarity, version compatibility) | **PRESERVE / REFACTOR** | generic matching utility in `:provider:selection` | These semantics are used by cross-provider selection and must not remain owned by the future LRCLIB networking adapter. |
| `lyrics/MetadataCleaner.kt` | **PRESERVE / REFACTOR** | metadata normalization utility at an appropriate domain/platform boundary | Not part of the selector migration. Do not independently reinvent metadata cleanup; verify call sites/tests before choosing final ownership. |
| `LyricsProviderResolverTest.kt` and version-context tests | **PRESERVE** | `:provider:selection` regression specification using AALyrics models | Behavioral cases and thresholds are ported rather than recreated from memory. AALyrics additionally makes exact score ties deterministic so provider execution order cannot become winner policy. |

### Intentional structural adaptation

The old resolver directly names provider implementations and calls generic helpers through `LrcLibClient`. AALyrics separates these concerns:

```text
:core:lyrics
    CandidateSelector port
           ↑
           |
:provider:selection
    CrossProviderCandidateSelector
    metadata/version/quality matching
           ↑
           |
normalized LyricsCandidate facts
```

Provider-specific source-confidence preferences remain part of cross-provider selection, but they live outside pure core. Provider adapters may report provider-neutral search evidence such as `artistQueryCorroborated`; they do not assign the final global score.

AALyrics also normalizes candidate duration to milliseconds in `Track`. A provider whose duration metadata is ambiguous should omit that fact (`null`) rather than leaking provider-specific unit exceptions into the central selector.

## Provider-local search and parsing

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LrcLibClient.kt` | **REFACTOR** | future `:provider:lrclib` adapter; generic matching already moved to `:provider:selection` | Preserve mature search/fallback behavior, but keep HTTP/provider concerns separate from generic track matching. |
| `lyrics/MusixmatchClient.kt` | **REFACTOR** | future Musixmatch provider adapter | Preserve proven behavior and tests where legally/technically appropriate; conform output to `LyricsProvider`. Do not let Musixmatch-specific decisions leak into core state. |
| `lyrics/PetitLyricsClient.kt` | **REFACTOR** | future PetitLyrics provider adapter | Preserve working behavior. Existing PetitLyrics configuration values must not be changed during migration unless explicitly requested. |
| `lyrics/SyncLrcClient.kt` | **REFACTOR** | future SyncLRC provider adapter | Preserve useful behavior and R8/regression fixes, but adapt to the common provider contract. |
| `lyrics/LrcParser.kt` | **PRESERVE / REFACTOR** | pure parser utility owned by the relevant provider/parser layer | Parsing behavior is already tested. Reuse semantics rather than writing a new LRC parser without evidence. |

## Playback identity and request lifecycle

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `media/SpotifyTrackIdentity.kt` | **PRESERVE / REFACTOR** | `:platform:media` identity normalization feeding `core:model` references | Preserve the Spotify-specific robustness, but keep Spotify/platform details outside lyrics core. Phase 4 reflects these semantics in the platform boundary. |
| `media/LyricsDemandController.kt` | **PRESERVE / REFACTOR** | playback/application demand boundary after provider/core wiring is stable | Review proven demand/lifecycle behavior before creating a replacement. |
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
| `lyrics/KaraokeTiming.kt` | **PRESERVE / REFACTOR** | future pure timing utility | Keep tested word-timing semantics when karaoke work begins. This is distinct from the selector's already-migrated WORD-candidate preference. |
| `util/SyncCalibration.kt` | **PRESERVE / REFACTOR** | future pure timing/calibration utility | Preserve tested calibration semantics; do not duplicate during provider migration. |
| `util/LyricWordLayout.kt` | **PRESERVE / REFACTOR** | presentation/timing utility later | Valuable tested behavior, but not part of provider selection. |
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

| AALyrics work | Why it remains new or structurally different |
| --- | --- |
| explicit `LyricsLookupId` / request identity | Makes stale-result rejection a first-class core invariant rather than incidental asynchronous behavior. |
| sealed provider-independent `LyricsState` lifecycle | Replaces the old Android/UI-heavy state DTO and creates one shared contract for phone and automotive presentation. |
| pure reducer/state transitions | Provides testable lifecycle semantics independent of Android, providers, cache, translation, and UI. |
| `LyricsCoordinator` boundary | Replaces orchestration responsibilities currently mixed into `MediaTracker`. |
| `CandidateSelector` port + `:provider:selection` implementation | Keeps core dependent on a stable port while adapting the proven resolver in an outer module. |
| normalized `LyricsCandidateEvidence` | Lets providers report search corroboration as facts without assigning cross-provider scores. |
| deterministic exact-score tie break | Preserves the architecture invariant that provider completion/execution order cannot silently determine the winner. |
| explicit playback identity and `PlaybackLyricsController` | Separates track ownership from position/status updates so media churn does not restart lyrics lookup. |

## Migration rule after the Core Readiness Gate

```text
Inspect current fork behavior
        ↓
Update this inventory / classify the slice
        ↓
Choose the existing AALyrics port or add the smallest necessary boundary
        ↓
Adapt mature behavior with regression tests
        ↓
Keep provider/platform quirks outside pure core
        ↓
CI + bounded review
        ↓
STOP before merge
```

Do not bulk-port the old application. Each provider or subsystem is a separate migration slice with explicit acceptance criteria.

## Current migration status

The STOP GATE was accepted after Phase 5. The first migration batch is the mature resolver/matching behavior in `:provider:selection` (PR #17). No concrete provider client is included in that batch.

After this slice, re-check the working fork again and choose the first concrete provider adapter based on coverage value, current implementation maturity, and migration complexity.
