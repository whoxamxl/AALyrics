# AALyrics Migration Inventory

## Purpose

This file prevents AALyrics from reimplementing behavior that is already mature and working in `whoxamxl/auto-lyrics`.

Before starting a non-trivial implementation slice, inspect the current fork and update this inventory. The classifications below describe **behavioral migration intent** and ownership in the AALyrics architecture.

Concrete-provider migration additionally follows `docs/PROVIDER_ARCHITECTURE.md` and the relevant profile under `docs/providers/`.

## Classification

| Class | Meaning |
| --- | --- |
| **PRESERVE** | Keep behavior with minimal semantic change. Structural cleanup is allowed only if regression behavior remains equivalent. |
| **REFACTOR** | Keep the proven behavior, but move ownership/dependencies to the AALyrics architecture. Existing mature project code may be reused/refactored; this does not require a gratuitous rewrite. |
| **REWRITE** | Preserve the user-facing requirement where applicable, but replace the old implementation because it is too coupled or structurally unsuitable. |
| **DROP** | Do not migrate unless later evidence shows it is still required. |

## Reference baseline

Initial review baseline:

- Repository: `whoxamxl/auto-lyrics`
- Branch: `main`
- Reviewed commit: `6ad450213f1896a5a65e217f15d1544c6e646d0e`

Core Readiness Gate and current provider baseline:

- Repository: `whoxamxl/auto-lyrics`
- Branch: `main`
- Reviewed commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Re-checked for the SyncLRC slice on 2026-09-16; it remains current working-fork `main`.

The mature resolver/provider clients remain present, `MediaTracker` still combines Android media, provider, cache, translation, timing, and presentation responsibilities, and the explicit Spotify playback identity and lyrics-demand helpers remain separate behaviors.

The fork continues to evolve. Re-check `main` before every later migration slice rather than assuming this snapshot remains current.

## Selection and metadata matching

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LyricsProviderResolver.kt` | **PRESERVE / REFACTOR** | `:provider:selection` implementation of the `:core:lyrics` `CandidateSelector` port, with neutral metadata plausibility in `:provider:matching` | Migrated in PR #17. PetitLyrics migration extracted the unchanged metadata plausibility calculation so providers can validate discovery without depending on global selection. Payload quality, source confidence, synchronization, karaoke, and winner policy stay in selection. |
| `lyrics/RecordingVersionContext.kt` | **PRESERVE / REFACTOR** | pure shared `:provider:matching` utility | Explicit live/remix/remaster/etc. context handling is heavily refined and regression-sensitive. Preserve semantics. |
| generic similarity helpers historically in `LrcLibClient` (`stringSimilarity`, artist matching, duration similarity, version compatibility) | **PRESERVE / REFACTOR** | pure shared `:provider:matching`, used where semantics are genuinely provider-neutral | Migrated as generic semantics rather than LRCLIB networking behavior. Concrete providers must not depend on production selector implementation merely to reuse matching. |
| `lyrics/MetadataCleaner.kt` | **PRESERVE / REFACTOR** | metadata normalization utility at an appropriate domain/platform boundary | Not part of provider selection. Do not independently reinvent metadata cleanup; verify call sites/tests before choosing final ownership. |
| `LyricsProviderResolverTest.kt` and version-context tests | **PRESERVE** | `:provider:selection` regression specification using AALyrics models | Behavioral cases and thresholds were migrated rather than recreated from memory. AALyrics additionally makes exact score ties deterministic so provider execution order cannot become winner policy. |

### Intentional structural adaptation

The old resolver directly names provider implementations and calls generic helpers through `LrcLibClient`. AALyrics separates these concerns:

```text
:core:lyrics
    CandidateSelector port
           ↑
           |
:provider:selection
    CrossProviderCandidateSelector
    cross-provider ranking policy
           ↑
           |
normalized LyricsCandidate facts
```

Provider-specific source-confidence preferences remain part of cross-provider selection, but they live outside pure core. Provider adapters may report provider-neutral search evidence such as `artistQueryCorroborated`; they do not assign the final global score.

AALyrics normalizes candidate duration to milliseconds in `Track`. A provider whose duration metadata is ambiguous should omit that fact (`null`) rather than leaking provider-specific unit exceptions into the central selector.

## Provider-local search and parsing

Shared provider migration rules are in `docs/PROVIDER_ARCHITECTURE.md`. Provider-specific behavior is summarized in `docs/providers/`.

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LrcLibClient.kt` | **PRESERVE / REFACTOR** | `:provider:lrclib`; see `docs/providers/LRCLIB.md` | Migrated in PR #19. Preserves mature exact/structured/title-only/free-text/plain fallback and local validation. |
| `lyrics/LrcParser.kt` | **PRESERVE / REFACTOR** | pure `:provider:lrc` shared parser | Migrated with LRCLIB. Parsing behavior is regression-covered and shared without provider networking ownership. SyncLRC reuses Enhanced-LRC `parseKaraoke`. |
| `lyrics/PetitLyricsClient.kt` | **PRESERVE / REFACTOR** | `:provider:petitlyrics`; see `docs/providers/PETITLYRICS.md` | Migrated in PR #20. Preserves progressive search, ranking, attempted-result deduplication, WSY/LSY parsing, companion-text resolution, artist-query evidence, configuration invariants, and provider-contract failure/cancellation semantics. |
| `lyrics/MusixmatchClient.kt` | **PRESERVE / REFACTOR** | `:provider:musixmatch`; see `docs/providers/MUSIXMATCH.md` | Migrated in PR #21. Preserves the anonymous mobile API flow, token/session behavior, macro lookup, RichSync/subtitle fallback, Spotify-aware identity validation, instrumental rejection, metadata validation, and failure isolation. |
| `lyrics/SyncLrcClient.kt` | **PRESERVE / REFACTOR** | `:provider:synclrc` in PR #24; see `docs/providers/SYNCLRC.md` | Implemented and reviewed pending merge approval. Preserves its deliberate karaoke-only role, WORD-preference request gating, current/legacy response-shape compatibility, instrumental rejection, and genuine word-timing requirement. |

Provider migration is not a clean-room exercise. Mature provider implementation may be reused/refactored when it already expresses the behavior we intend to keep. The structural requirement is that legacy coupling does not cross the AALyrics provider boundary.

## Playback identity and request lifecycle

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `media/SpotifyTrackIdentity.kt` | **PRESERVE / REFACTOR** | `:platform:media` identity normalization feeding `core:model` references | Phase 4 preserves Spotify-specific robustness while keeping platform details outside lyrics core. Musixmatch consumes only normalized `TrackReference` identity. |
| `media/LyricsDemandController.kt` | **PRESERVE / REFACTOR** | playback/application demand boundary after provider/core wiring is stable | Review proven demand/lifecycle behavior before creating a replacement. |
| `media/LyricsVariantTransition.kt` | **PRESERVE / REFACTOR** | future state/variant transition policy if still required | Small but potentially regression-sensitive. Inspect call sites before migration. |
| `media/MediaTracker.kt` | **REWRITE** | split across `:platform:media`, `:core:lyrics`, composition root, and later feature-specific services | Preserve observable behavior through tests/reference, but do not migrate the monolithic ownership model. |
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
| `lyrics/KaraokeTiming.kt` | **PRESERVE / REFACTOR** | future pure timing utility | Keep tested word-timing semantics when karaoke work begins. This is distinct from SyncLRC payload parsing and from the selector's already-migrated WORD-candidate preference. |
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
Inspect current fork behavior and tests
        ↓
Read/update migration inventory + relevant provider profile
        ↓
Choose the existing AALyrics boundary
        ↓
PRESERVE / REFACTOR mature implementation and regression knowledge
        ↓
Keep provider/platform quirks outside pure core
        ↓
CI + bounded review
        ↓
STOP before merge
```

Do not bulk-port the old application. Each provider or subsystem remains a separate migration slice with explicit acceptance criteria. At the same time, do not invent a clean-room rewrite step for mature provider code when PRESERVE / REFACTOR is the documented classification.

## Current migration status

- Core Readiness STOP GATE: accepted and merged in PR #14.
- Production candidate selection: migrated and merged in PR #17.
- LRCLIB: migrated and merged in PR #19.
- PetitLyrics: migrated and merged in PR #20.
- Musixmatch: migrated and merged in PR #21.
- SyncLRC: migrated and merged in PR #24.
- Phase 7 concrete-provider migration: complete.
- Application composition: active on `feature/application-composition`.

### LRCLIB implementation re-check

The LRCLIB slice re-fetched working-fork main at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` on 2026-09-15, inspecting `LrcLibClient`, `LrcParser`, their tests, recording-version tests, and the `MediaTracker` normalization call site. No experimental provider branch was reused.

Preserved: local query sequence, scores/thresholds, deduplication, matching/version behavior, ordinary/enhanced LRC semantics, and synchronized-first fallback. Structural changes: normalized provider output, neutral shared matching/parser modules, cancellable HTTP, explicit operational failures, and payload usability before local winner acceptance.

### PetitLyrics implementation re-check

The PetitLyrics slice re-fetched working-fork main at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` on 2026-09-16, inspecting `PetitLyricsClient`, all provider tests, the resolver used by local candidate ranking, and `MediaTracker` normalization/call sites.

Preserved: progressive discovery, local candidate ranking and sync preference, attempted-result deduplication, artist-query evidence, WSY timing/spacing, LSY protection-key/rollover decoding, lyrics-ID companion resolution, metadata companion fallback, and malformed result rejection. Structural changes: provider/config injection, normalized domain output, shared metadata plausibility, cancellable HTTP, and provider-contract failure reporting after local fallback is exhausted.

### Musixmatch implementation re-check

The Musixmatch slice re-fetched working-fork main at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` on 2026-09-16, inspecting `MusixmatchClient.kt`, all Musixmatch regressions, Spotify identity tests, and the relevant `MediaTracker`/resolver call sites.

Preserved: the anonymous mobile `token.get` → `macro.subtitles.get` flow, TTL and rejected-token refresh, embedded RichSync preference, dedicated `track.richsync.get` fallback, line-subtitle fallback, metadata/instrumental validation, Spotify-ID request/match evidence, explicit Spotify-ID conflict rejection, artist-query corroboration, and RichSync word offsets. Structural changes: an in-memory provider session replaces Android preferences, HTTP is cancellable, failures follow the provider contract after local fallback is exhausted, and output is normalized to AALyrics models. Spotify reference extraction remains in `:platform:media`; the Musixmatch adapter consumes normalized identity only.

### SyncLRC implementation re-check

Working-fork main was re-checked at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` on 2026-09-16 and remains current. The slice inspected `SyncLrcClient.kt`, all eight SyncLRC regressions, shared `LrcParser.parseKaraoke`, and the relevant `MediaTracker` fan-out/normalization call sites. The current public SyncLRC API documentation was also re-checked and still matches the mature endpoint/query/response assumptions.

Preserved: request execution only when karaoke/WORD timing is preferred; required nonblank track/artist; `type=karaoke`; optional album/duration; current `karaoke` and legacy `lyrics` + `type=karaoke` compatibility; rejection of synced/plain-only and instrumental responses; Enhanced-LRC parsing; genuine timed-word requirement; response-metadata fallback; fine-grained Japanese timing; and artist-query corroboration.

Structural adaptation: a `WORD`-only `LyricsProvider` gates transport with normalized `LyricsRequest.preferredSyncType`, reuses shared `:provider:lrc` parsing, normalizes duration seconds to domain milliseconds, uses cancellable HTTP, and surfaces operational failures according to the provider contract. Final metadata scoring, source confidence, karaoke preference, and winner selection remain unchanged in `:provider:selection`.

Documentation/planning approval does not authorize unrelated implementation. The current explicit authorization is limited to the first manual application object graph on `feature/application-composition`; presentation and live media-session work remain separate slices.
