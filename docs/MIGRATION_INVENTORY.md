# AALyrics Migration Inventory

## Purpose

This file prevents AALyrics from reimplementing behavior that is already mature and working in `whoxamxl/auto-lyrics`.

Before starting a non-trivial implementation slice, inspect the current fork and update this inventory. The classifications below describe **behavioral migration intent** and ownership in the AALyrics architecture.

Concrete-provider migration additionally follows `docs/PROVIDER_ARCHITECTURE.md` and the relevant profile under `docs/providers/`. Future cache, translation, timing/calibration, karaoke, and presentation-state migration also follows `docs/LYRICS_PIPELINE_ARCHITECTURE.md` plus the relevant capability-specific architecture document.

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

Core Readiness Gate and current provider/runtime/lifecycle baseline:

- Repository: `whoxamxl/auto-lyrics`
- Branch: `main`
- Reviewed commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Re-checked for the SyncLRC slice on 2026-09-16, for the live media-session runtime on 2026-09-17, for lyrics-demand-gating planning on 2026-09-17, for both Translation implementation slices on 2026-09-19, and for the Effective Timing Foundation planning on 2026-09-25. It remains the current working-fork release baseline.

The mature resolver/provider clients remain present in that baseline, `MediaTracker` combines Android media, provider, cache, translation, timing, and presentation responsibilities, and the explicit Spotify playback identity and lyrics-demand helpers remain separate behaviors.

The Phase 11 lyrics-capability architecture foundation intentionally does not treat this baseline as implementation evidence for cache, translation, timing/calibration, karaoke, or presentation state. Each Phase 11.x implementation slice must re-check current working-fork `main`, active call sites, and tests before selecting concrete behavior or signatures.

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
| `lyrics/SyncLrcClient.kt` | **PRESERVE / REFACTOR** | `:provider:synclrc`; see `docs/providers/SYNCLRC.md` | Migrated and merged in PR #24. Preserves its deliberate karaoke-only role, WORD-preference request gating, current/legacy response-shape compatibility, instrumental rejection, and genuine word-timing requirement. |

Provider migration is not a clean-room exercise. Mature provider implementation may be reused/refactored when it already expresses the behavior we intend to keep. The structural requirement is that legacy coupling does not cross the AALyrics provider boundary.

## Playback identity and request lifecycle

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `media/SpotifyTrackIdentity.kt` | **PRESERVE / REFACTOR** | `:platform:media` identity normalization feeding `core:model` references | Phase 4 preserves Spotify-specific robustness while keeping platform details outside lyrics core. Musixmatch consumes only normalized `TrackReference` identity. |
| `media/LyricsDemandController.kt` | **PRESERVE / REFACTOR** | implemented application/runtime lifecycle boundary; see `docs/LYRICS_DEMAND_GATING.md` | Migrated in PR #30. Preserves `phone foreground OR Android Auto projection connected` while gating normalized playback before `PlaybackLyricsController` instead of coupling demand to legacy `MediaTracker`. |
| `AutoLyricsApp.kt` demand wiring | **PRESERVE / REFACTOR** | `:app` lifecycle adapters feeding the demand gate | Migrated in PR #30. Preserves process-level phone foreground semantics through `ProcessLifecycleOwner` and projection-wide automotive demand through `CarConnection.CONNECTION_TYPE_PROJECTION`; unrelated translation/UI/service startup ownership was not migrated into the application object. |
| `media/LyricsVariantTransition.kt` | **PRESERVE / REFACTOR** | future state/variant transition policy if still required | Small but potentially regression-sensitive. Inspect current call sites before migration and fit any retained semantics into the capability/presentation boundaries rather than reviving mixed state ownership. |
| `media/MediaTracker.kt` | **REWRITE** | split across `:platform:media`, `:core:lyrics`, composition root, demand lifecycle, and later capability-specific services | Preserve observable behavior through tests/reference, but do not migrate the monolithic ownership model. Phase 11 explicitly keeps cache/translation/timing/presentation out of a replacement monolith. |
| `media/MediaListenerService.kt` | **REWRITE / REFACTOR** | thin Android adapter in `:platform:media` | Implemented in PR #29 as `MediaSessionListenerService` plus testable selection/observation/runtime ownership; domain orchestration remains outside the service. |

## State and presentation

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `model/Models.kt` legacy `LyricsState` | **REWRITE** | existing normalized core state plus future capability-specific application facts and surface-specific presentation mapping; see `docs/PRESENTATION_STATE_ARCHITECTURE.md` | Old state mixes Android `Bitmap`, playback, lyrics, translation, UI indices, offset, and colors. Phase 11 explicitly rejects recreating one giant state object. Shared semantic facts may be reused, but Phone and automotive keep independent surface state. |
| `MainActivity.kt` | **REWRITE** | `:ui:phone` | Use as behavioral/UI reference only. New UI consumes shared application/domain facts and does not own provider, cache, translation, timing, or karaoke orchestration. |
| `auto/LyricsBrowserService.kt` | **REWRITE** | `:ui:automotive` plus thin Android service boundary | Preserve useful Android Auto UX/behavior, but the service must not fetch/rank lyrics or duplicate timing/karaoke semantics owned below presentation. |
| `PerformanceActivity.kt` / `PerformanceLyricsView.kt` | **REVIEW BEFORE DECISION** | future phone feature if retained | Do not reimplement until product intent is explicitly decided. |

## Timing, karaoke, and calibration

Phase 11 established the ownership seam in `docs/TIMING_ARCHITECTURE.md` and `docs/KARAOKE_ARCHITECTURE.md`. The effective-timing foundation, zero-offset Phone integration, and shared LINE/WORD Timing Semantic Engine are now implemented on `feature/effective-timing-foundation`. Sync calibration UX and Karaoke consumer/rendering remain separate later work.

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/KaraokeTiming.kt` | **PRESERVE / REFACTOR** | implemented shared WORD timing semantics in `:core:timing` | Re-checked at working-fork `v1.13.0` on 2026-09-25. Its latest-started-word, explicit-end gap, no-reactivation, and backward-seek semantics were preserved/refactored into the mode-agnostic Timing Semantic Engine. Future Karaoke consumes that projection and must not recalculate timing or reapply calibration. |
| `util/SyncCalibration.kt` | **PRESERVE / REFACTOR** | timing/calibration capability in `:core:timing`; Sync UX still deferred | Re-checked at working-fork `v1.13.0` on 2026-09-25. The proven sign behavior `offsetForTap = targetTime - rawPosition` matches AALyrics `effectivePosition = projectedPosition + offset` and is preserved. The legacy three-tap/upcoming-line workflow remains later Sync UX/policy. |
| `util/LyricWordLayout.kt` | **PRESERVE / REFACTOR** | future Karaoke display-mapping/presentation support | Valuable tested behavior may survive, but shared timing projection and surface text layout are separate responsibilities. Do not move lexical/display-range mapping into `:core:timing`. |
| `ui/KaraokeSweepSpan.kt` | **REWRITE / REFACTOR** | future Phone Karaoke rendering downstream of shared timing projection | Rendering is Android-specific. Preserve useful visual behavior where desired, but do not migrate it as timing/domain logic or make Android Auto share Android span primitives. |

## Translation, cache, artwork, and other later features

Phase 11 defines the architectural seams in `docs/CACHE_ARCHITECTURE.md` and `docs/TRANSLATION_ARCHITECTURE.md`; concrete migration decisions remain Phase 11.1/11.2 implementation work.

| Fork implementation | Classification | AALyrics destination / rule | Notes |
| --- | --- | --- | --- |
| `lyrics/LyricsTranslator.kt` | **REFACTOR** | split across `:translation:core` execution and `:translation:mlkit` model/engine adapters; see `docs/TRANSLATION_ARCHITECTURE.md` | Re-checked at working-fork `v1.13.0` for both 2026-09-19 Translation slices. Preserve route/model preparation, active model-task/monitor reuse, availability checks, latched failure/timeout state with explicit retry, cancellation semantics, thermal waiting, active-time timeout behavior, original fallback, and whole-result publication. Do **not** preserve the first-five-lines Language ID algorithm or monolithic UI/status ownership. |
| `lyrics/TranslationLanguages.kt` | **PRESERVE / REFACTOR** | pure Translation target/config semantics plus ML Kit-specific model planning in the ML Kit adapter | Preserve the approved nine target languages, default target, and language-tag normalization. Move ML Kit-specific required-model planning out of generic language configuration instead of copying the legacy mixed responsibility verbatim. |
| `TranslationTargetView.kt` / `TranslationStatusView.kt` | **DROP / REWRITE later** | future Phone Settings/presentation downstream of Translation state | Do not migrate legacy View injection into the scaffold. Settings variables/persistence and model lifecycle are implemented below presentation; future Compose UI consumes those boundaries. |
| `MediaTracker` Translation preference/target handling | **REFACTOR** | application-owned Translation settings/runtime plus `TranslationCoordinator` | Target changes now cancel/supersede Translation independently of Lyrics Provider lookup. Do not restore `MediaTracker` as owner of lyrics, Translation, model downloads, and UI state. |
| `lyrics/LyricsCache.kt` | **REFACTOR** | future replaceable cache boundary + storage adapter; see `docs/CACHE_ARCHITECTURE.md` | Re-check implementation and call sites before Phase 11.1. Preserve useful semantics without fixing provider-result versus selected-result cache placement, storage engine, schema, TTL, or invalidation in the foundation. UI and providers must not own global storage policy. |
| `util/AlbumColorExtractor.kt` | **PRESERVE / REFACTOR** | presentation/platform utility | Android-specific feature; not part of lyrics core or the current Phase 11 capability foundation. |
| `util/AudioSyncHelper.kt` | **DROP unless proven used** | none by default | Previous inspection found no clear current usage. Do not migrate dead auto-sync logic without an active call path and explicit product requirement. |

## Current AALyrics work versus fork behavior

| AALyrics work | Why it remains new or structurally different |
| --- | --- |
| explicit `LyricsLookupId` / request identity | Makes stale-result rejection a first-class core invariant rather than incidental asynchronous behavior. |
| sealed provider-independent `LyricsState` lifecycle | Replaces the old Android/UI-heavy state DTO and creates one shared contract for lyrics lookup outcomes without absorbing every later capability. |
| pure reducer/state transitions | Provides testable lifecycle semantics independent of Android, providers, cache, translation, and UI. |
| `LyricsCoordinator` boundary | Replaces provider orchestration responsibilities currently mixed into `MediaTracker` while explicitly excluding future cache/translation/timing/presentation ownership from a new god object. |
| `CandidateSelector` port + `:provider:selection` implementation | Keeps core dependent on a stable port while adapting the proven resolver in an outer module. |
| normalized `LyricsCandidateEvidence` | Lets providers report search corroboration as facts without assigning cross-provider scores. |
| deterministic exact-score tie break | Preserves the architecture invariant that provider completion/execution order cannot silently determine the winner. |
| explicit playback identity and `PlaybackLyricsController` | Separates track ownership from position/status updates so media churn does not restart lyrics lookup. |
| live media-session runtime | Preserves mature session-selection behavior while splitting Android discovery/callback ownership from lyrics orchestration and presentation. |
| lyrics demand gating | Preserves the mature phone/Android Auto demand policy while moving the gate to a narrow application/runtime boundary that can clear/replay normalized playback without stopping MediaSession observation. |
| lyrics capability architecture foundation | Splits the legacy monolithic cache/translation/timing/karaoke/presentation ownership into explicit future seams while deliberately deferring concrete signatures until each implementation slice has evidence. See `docs/LYRICS_PIPELINE_ARCHITECTURE.md`. |

## Migration rule after the Core Readiness Gate

```text
Inspect current fork behavior and tests
        ↓
Read/update migration inventory
        ↓
Read the relevant architecture document(s)
        ↓
Choose the existing AALyrics seam
        ↓
PRESERVE / REFACTOR / REWRITE / DROP with regression evidence
        ↓
Introduce only the smallest implementation-specific contract
        ↓
Keep provider/platform/storage/UI quirks outside unrelated pure capabilities
        ↓
CI + bounded review
        ↓
STOP before merge
```

For Phase 11.x work, read `docs/LYRICS_PIPELINE_ARCHITECTURE.md` plus the relevant cache, translation, timing, karaoke, or presentation-state document before writing production code.

Do not bulk-port the old application. Each provider or subsystem remains a separate migration slice with explicit acceptance criteria. At the same time, do not invent a clean-room rewrite step for mature code when PRESERVE / REFACTOR is the documented classification, and do not pre-create speculative abstractions merely because the foundation identifies a future seam.

## Current migration status

- Core Readiness STOP GATE: accepted and merged in PR #14.
- Production candidate selection: migrated and merged in PR #17.
- LRCLIB: migrated and merged in PR #19.
- PetitLyrics: migrated and merged in PR #20.
- Musixmatch: migrated and merged in PR #21.
- SyncLRC: migrated and merged in PR #24.
- Phase 7 concrete-provider migration: complete.
- Application composition: merged in PR #25.
- Live media-session runtime: merged in PR #29.
- Lyrics demand gating: migrated and merged in PR #30.
- Lyrics capability architecture foundation: merged in PR #32.
- Translation background scaffold: merged in PR #41 as Phase 11.2a.
- Translation execution/orchestration: merged in PR #43 as Phase 11.2b.
- Phone Translation presentation/diagnostics: merged in PR #79 as Phase 11.2c; Android Auto Translation and persistent Translation Cache remain deferred.
- Timing capability: Phase 11.3a effective-position foundation, Phase 11.3b Phone line integration, and Phase 11.4a shared LINE/WORD semantic engine are implemented and validated on `feature/effective-timing-foundation`; Sync UX, persistence, and Karaoke consumer/rendering remain deferred.


### Translation scaffold implementation re-check

Working-fork main was re-checked at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`) on 2026-09-19. The scaffold review inspected `LyricsTranslator.kt`, `TranslationLanguages.kt`, `TranslationTargetView.kt`, `TranslationStatusView.kt`, the Translation preference/target call sites in `MediaTracker.kt`, the target-language PR regressions, and the working-fork ML Kit dependencies.

Preserve/refactor into the scaffold: the nine-language target list; BCP-47-to-language normalization; explicit model availability checks; shared model download tasks/monitors; latched model failure/timeout state with explicit retry; cancellation behavior that lets useful process-level model monitoring outlive a cancelled foreground waiter; thermal-wait handling; and a timeout measured against active download time rather than thermally blocked wall time.

Intentionally not migrated in the scaffold: legacy foreground Views; legacy `MediaTracker` ownership; actual text translation execution; the first-five-nonblank-lines source-language detector; per-line Translation fallback policy; and any persistent Translation cache.

The scaffold deliberately left complete-lyrics Primary/Secondary profiling, INCIDENTAL/UNCERTAIN preservation, contextual Core + Context Halo blocks, independent Translation Provider selection, and atomic Translation Artifact publication to the execution slice recorded below. Musixmatch Translation alignment remains deferred pending endpoint and entitlement verification.

### Translation execution implementation re-check

Working-fork `origin/main` was fetched and remained at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`) on 2026-09-19. The execution review re-read `LyricsTranslator.kt`, `TranslationLanguages.kt`, the Translation preference/target call sites in `MediaTracker.kt`, and the ML Kit dependencies/tests.

Preserve/refactor into the execution slice: normalized source/target route setup; preparation of both non-English model packs through the already migrated model manager; cancellation propagation; whole-result publication; original-line preservation after isolated Translation failure when the artifact remains structurally valid; and final publication guards for track, enablement, and selected target.

Intentionally replace: the first-five-nonblank-lines detector, one global source-language assumption, context-free per-line Translation as the primary path, legacy `MediaTracker` ownership, and foreground Translation Views. AALyrics owns complete-document profiling, Primary/Secondary activation, contextual Core + Context Halo blocks, structural fallback, provider-independent provenance, request-generation stale rejection, and atomic artifact state.

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

Working-fork main was re-checked at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` on 2026-09-16 and was current at that review point. The slice inspected `SyncLrcClient.kt`, all eight SyncLRC regressions, shared `LrcParser.parseKaraoke`, and the relevant `MediaTracker` fan-out/normalization call sites. The public SyncLRC API documentation was also re-checked and matched the mature endpoint/query/response assumptions at implementation time.

Preserved: request execution only when karaoke/WORD timing is preferred; required nonblank track/artist; `type=karaoke`; optional album/duration; current `karaoke` and legacy `lyrics` + `type=karaoke` compatibility; rejection of synced/plain-only and instrumental responses; Enhanced-LRC parsing; genuine timed-word requirement; response-metadata fallback; fine-grained Japanese timing; and artist-query corroboration.

Structural adaptation: a `WORD`-only `LyricsProvider` gates transport with normalized `LyricsRequest.preferredSyncType`, reuses shared `:provider:lrc` parsing, normalizes duration seconds to domain milliseconds, uses cancellable HTTP, and surfaces operational failures according to the provider contract. Final metadata scoring, source confidence, karaoke preference, and winner selection remain unchanged in `:provider:selection`.

Each later implementation slice still requires explicit authorization. Translation is implemented through Phone presentation, and the timing foundation plus shared Timing Semantic Engine are implemented on the current timing topic branch. Persistent cache, Sync calibration UX/persistence, Karaoke consumer/rendering, Android Auto Translation, and broader presentation-state integration remain separate future slices until explicitly authorized.
