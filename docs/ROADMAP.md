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
- one shared `LyricsState` contract for phone and automotive presentation,
- presentation ownership guards preventing provider fetching/ranking in UI modules,
- Android MediaSession / MediaController confinement to `:platform:media`,
- provider-specific behavior exclusion from pure core,
- regression evidence for provider order, stale-result rejection, failure isolation, and playback identity,
- migration-inventory re-review against `whoxamxl/auto-lyrics` `main` at `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).

Detailed evidence is recorded in `docs/CORE_READINESS_GATE.md`.

### Phase 6 — Production candidate selection migration ✅

Merged in PR #17.

Phase 6 adapted the mature cross-provider resolver behind the existing `CandidateSelector` port without making `:core:lyrics` depend on the production implementation.

Completed work includes:

- pure Kotlin `:provider:selection` production selector,
- mature metadata, payload-quality, source-confidence, synchronized/plain fallback, cross-script, recording-version, and karaoke preference behavior,
- provider-neutral candidate evidence needed by the resolver,
- deterministic exact-score tie breaking so provider execution order is not winner policy,
- regression coverage adapted to AALyrics models,
- rejection of unusable lyric candidates before selection.

### Phase 7 — Concrete provider adapters ✅

Provider migration proceeded one adapter at a time behind `LyricsProvider`.

- LRCLIB — merged in PR #19.
- PetitLyrics — merged in PR #20.
- Musixmatch — merged in PR #21.
- SyncLRC — merged in PR #24.

### Phase 8 — Application composition ✅

Merged in PR #25.

The application now constructs one production object graph containing all four providers, `CrossProviderCandidateSelector`, `LyricsCoordinator`, and `PlaybackLyricsController`. The graph is process-owned by `AALyricsApplication`, exposes the shared `LyricsState`, uses existing BuildConfig values for PetitLyrics, and preserves the karaoke-enabled production default through `preferredSyncType = WORD`.

Playback lookup ownership now includes both track identity and candidate-selection preferences. Same-track preference changes trigger a fresh lookup while position/status/rate/duration churn does not.

### Phase 8.1 — UI foundation ✅

Merged in PRs #27 and #28, with Phone information architecture/package scaffolding added in PR #31 and the persistent Phone Compose shell in PR #33.

Subsequent Phone presentation work has substantially advanced the production surface:

- PR #35 refined the shell visuals;
- PR #36 implemented the Phone Track Card;
- PR #40 implemented the responsive LyricsViewport;
- PR #42 composed the production Lyrics destination;
- PRs #44 and #45 implemented and polished the first Settings destination;
- PR #46 replaced the legacy fixed playback controls with the capability-aware collapsed Playback Bar + Expanded Player, including interactive seek, Queue/Open-app fallback, and Translation quick controls.

Presentation uses `:ui:designsystem` for shared tokens/components, `:ui:phone` for phone-specific composition, and `:ui:automotive` for automotive-specific composition. Production UI lives in `src/main`; deterministic Preview/development fixtures live in `src/debug` and render the production composables. Phone and automotive remain separate presentation surfaces rather than one universal UI model.

PR #49 implements the approved read-only Details destination plus the Settings-owned Advanced surface with a persisted presentation-only Verbose Details preference and an intentionally disabled/unwired Karaoke affordance. Sync remains the only primary Phone destination that is both a placeholder and interaction-model-deferred pending timing/calibration redesign.

PR #50 established the **Phone runtime host / device-test enablement** boundary documented in `docs/PHONE_RUNTIME_HOST.md`. Subsequent merged Phone work completed the Settings legal/help surfaces, playback-source eligibility and diagnostics, storage/reset controls, queue/artwork refinements, and the verified in-app update flow. `MainActivity` continues to preserve onboarding while hosting live application-owned Lyrics/Playback/Details/Settings state. Sync remains a deliberate non-functional placeholder and Karaoke remains disabled/unwired.

The production Settings information architecture is now merged on `main`: Version/Changelog/Source code and automatic/manual update controls live under `APP`; Privacy Policy, Terms of Use, License/third-party licenses, Help & Feedback, and Support AALyrics live under `ABOUT & SUPPORT`; Advanced remains a separate application-owned settings surface. External browser/payment handoffs remain app-owned and do not move runtime capability ownership into `:ui:phone`.

### Phase 9 — Live MediaSession runtime ✅

Merged in PR #29.

Its purpose is to connect Android's live active media sessions to the already-composed lyrics engine:

```text
NotificationListenerService
        ↓
MediaSessionManager
        ↓
selected MediaController
        ↓
MediaControllerSnapshotAdapter
        ↓
PlaybackSnapshot
        ↓
PlaybackLyricsController
        ↓
LyricsState
```

The runtime preserves/refactors the mature working-fork session-selection behavior while keeping Android framework ownership in `:platform:media` and avoiding the old `MediaTracker` monolith. It retains token-based ownership and the platform-owned 600 ms track-metadata stabilization, and hands normalized snapshots to the existing application graph through a narrow host/sink boundary.

### Phase 10 — Lyrics demand gating ✅

Merged in PR #30.

This background/runtime slice preserves the working fork's proven demand rule while moving ownership into the AALyrics application lifecycle boundary:

```text
phone process foreground ─────┐
                              ├──> lyrics demand active
Android Auto projection ──────┘

MediaSession runtime
        ↓
latest PlaybackSnapshot
        ↓
Lyrics demand gate
        ↓ when active
PlaybackLyricsController
        ↓
provider lookup
```

MediaSession discovery and selected-controller callbacks remain alive even when demand is inactive. The gate controls only lyrics work. While demand is off it retains the latest normalized playback snapshot but does not start provider lookup; deactivation cancels in-flight work while allowing an already resolved usable result to stay owned in process memory, and reactivation immediately replays the latest snapshot without refetching when its identity is unchanged.

Detailed behavior is defined in `docs/LYRICS_DEMAND_GATING.md`.

### Phase 11 — Lyrics capability architecture foundation ✅

Defined on `architecture/lyrics-capability-foundation` as a documentation-first architecture slice.

This phase does not implement cache, translation, timing/calibration, karaoke, or production presentation state. It defines their stable seams so later implementation can proceed without collapsing unrelated responsibilities into `LyricsCoordinator`, providers, MediaSession code, or UI.

Umbrella architecture:

- `docs/LYRICS_PIPELINE_ARCHITECTURE.md`

Capability-specific architecture:

- `docs/CACHE_ARCHITECTURE.md`
- `docs/TRANSLATION_ARCHITECTURE.md`
- `docs/TIMING_ARCHITECTURE.md`
- `docs/KARAOKE_ARCHITECTURE.md`
- `docs/PRESENTATION_STATE_ARCHITECTURE.md`

The foundation fixes:

- canonical lyrics/source timing versus derived artifacts;
- capability ownership and dependency direction;
- stale-result and identity expectations;
- failure isolation between optional capabilities;
- shared karaoke/timing semantics before surface-specific rendering;
- independent Phone and automotive presentation state;
- the rule that implementation must stabilize seams before signatures rather than pre-create speculative modules/interfaces.

It intentionally leaves concrete modules, API signatures, storage engines, translation engines, calibration algorithms, karaoke DTOs, and final presentation-state fields to the implementation slices that have evidence to define them.

## Next implementation slices

The capability foundation does not by itself authorize implementation. Phase 11.3a was explicitly authorized and implemented on `feature/effective-timing-foundation`; unrelated capability slices remain inactive until separately authorized.

The expected dependency-friendly sequence is:

### Phase 11.1 — Cache — deferred

Cache remains an independent capability, but persistent Translation Cache is not a prerequisite for Translation implementation.

Project policy now forbids persistent Translation Cache through stable `v1.0.0`; after `v1.0.0` it remains disabled unless explicitly authorized. This keeps Translation behavior directly observable during development and avoids stale cached output masking algorithm changes.

General lyrics/cache work still follows `docs/CACHE_ARCHITECTURE.md` when separately authorized.

### Phase 11.2a — Translation background scaffold ✅

Prepare Translation without changing unfinished foreground presentation:

- preserve the working fork's nine target languages and normalization semantics;
- refactor Translation settings into application/capability-owned state;
- refactor mature ML Kit model availability/download/retry/thermal behavior into an Android adapter;
- allow background preparation of the persisted target language model;
- define only the smallest contracts required by that background work;
- at Phase 11.2a, do not wire translated lyrics into Phone or Android Auto yet; Phone presentation is now explicitly authorized by Phase 11.2c below, while Android Auto remains deferred.

The scaffold must not implement speculative LanguageProfiler thresholds, contextual block algorithms, Musixmatch Translation alignment, Translation Provider selection, or persistent Translation Cache.

### Phase 11.2b — Translation execution/orchestration ✅

Merged in PR #43.

Translation now runs as an additive derived capability over canonical lyrics without moving execution into Lyrics Providers or `LyricsCoordinator`.

The implementation includes the approved complete-lyrics LanguageProfiler, Primary/Secondary activation policy, contextual Core + Context Halo translation, structural alignment/fallback, Translation-specific provider/session contracts, complete Translation Artifact assembly, stale-result rejection, cancellation, and atomic publication.

Musixmatch native Translation remains deferred until its endpoint/entitlement is re-verified and its source text can be aligned confidently to canonical lyrics.

The durable ownership contract remains `docs/TRANSLATION_ARCHITECTURE.md`.

### Phase 11.2c — Phone Translation presentation/integration ✅

Merged in PR #79.

Phone presentation now consumes the already-implemented atomic Translation result without reopening Translation execution architecture:

- `PhoneRuntimeHost` lifecycle-observes `AALyricsApplication.translationState`;
- only an enabled, current-target, exact-canonical-identity `TranslationState.Ready` artifact is eligible for translated presentation;
- canonical/source text remains primary, with actual translated lines rendered as secondary text inside the same logical viewport row;
- pending/not-required/failed/stale Translation remains original-only and never becomes Lyrics failure;
- Track Card runtime/model feedback and Retry stay presentation/application-owned rather than moving execution into `:ui:phone`;
- read-only Translation Details diagnostics expose current profile/runtime/model evidence without starting new work;
- canonical timing, current-line ownership, provider attribution, Lyrics Provider selection, and existing Follow/Browse geometry remain unchanged.

Android Auto Translation presentation, Musixmatch native Translation, persistent Translation Cache, timing/calibration, and Karaoke Translation behavior remain deferred to separate slices.

The durable merged contract is recorded in `docs/TRANSLATION_ARCHITECTURE.md`, `docs/PHONE_LYRICS_VIEWPORT.md`, `docs/PHONE_RUNTIME_HOST.md`, and `docs/PHONE_DETAILS.md`.

### Phase 11.3a — Effective Timing Foundation — implemented on topic branch

Establish the framework-independent timing engine before any Sync UI is introduced.

Approved first-slice semantics:

- canonical LINE/WORD source timestamps remain immutable;
- define a signed lyrics timing offset;
- positive offset advances lyrics and negative offset delays lyrics;
- derive `effectiveLyricsPosition = projectedPlaybackPosition + lyricsOffset`;
- zero offset preserves current behavior;
- add a pure Kotlin/JVM `:core:timing` capability plus deterministic tests and architecture guardrails;
- do not integrate the new engine into Phone/Android Auto presentation yet;
- do not add persistence, calibration controls, drift correction, or Karaoke semantics.

The working-fork `SyncCalibration.offsetForTap(target, raw) = target - raw` sign behavior was re-checked at `v1.13.0` and is compatible with this contract. The old tap workflow itself remains deferred.

The implementation contract and validation record are `TASK.md` plus `docs/TIMING_ARCHITECTURE.md`.

### Phase 11.3b — Existing timed-lyrics integration — implemented on topic branch

The existing Phone synchronized-line/current-line decision now consumes `EffectiveLyricsPosition` from `:core:timing` while the production offset remains zero by default.

The integration deliberately keeps `projectedPlaybackPosition(...)` unchanged, keeps canonical source timestamps immutable, and keeps playback progress based on the real projected playback position. Focused regression coverage proves positive/negative timing offsets can cross canonical line boundaries without introducing Sync UI or persistence.

This slice establishes the real timing-consumer boundary before any user calibration state exists.

### Phase 11.4a — Shared Timing Semantic Engine — active documentation scope

Build one framework-neutral projection from canonical timed lyrics + `EffectiveLyricsPosition` into shared timing facts:

- active line;
- active word when WORD timing exists;
- deterministic word progress when duration is defensible;
- explicit word boundary state for before/active/gap/after cases.

The engine is presentation-mode agnostic. Karaoke ON/OFF is not an input.

After semantic parity is proven, wire the current Phone production path to the engine while continuing to consume only `activeLineIndex`. LINE_SYNC, WORD_SYNC normal presentation, PLAIN lyrics, playback progress, Translation, Sync UI, and Android Auto must remain behaviourally unchanged.

The existing timing topic branch continues for this work. Draft PR #80 was closed before review because it represented only the earlier 11.3a/11.3b checkpoint.

### Phase 11.3c — Sync calibration UX — deferred

UX deliberately follows the semantic engine.

Only after shared timing semantics and behaviour-preserving wiring are proven, define:

- adjustment controls and step sizes;
- scope (session/global/track/provider/presentation-specific);
- persistence;
- reset semantics;
- Phone/Android Auto compensation policy;
- any later manual/automatic calibration workflow.

Do not infer these choices from the legacy app or from the current placeholder.

### Phase 11.4b — Karaoke consumer/rendering — deferred

Karaoke presentation will consume the already-computed shared timing projection rather than own current-line/current-word/progress calculations.

The detailed Karaoke consumer/rendering contract is a separate documentation checkpoint before implementation. Phone Compose and Android Auto may render differently, but neither may duplicate timing semantics.

### Phase 11.5 — Presentation state integration

Compose application/domain capability facts into the smallest presentation contracts demonstrated by actual Phone and automotive requirements.

Must follow `docs/PRESENTATION_STATE_ARCHITECTURE.md`. Do not create one universal giant UI state. Keep shared semantic facts shareable and surface-local state local.

These implementation slices are separate responsibilities and should normally use separate topic branches/PRs. The order may change when concrete implementation or product evidence justifies it, but the dependency and ownership rules in the Phase 11 foundation remain the guardrail.

## Release engineering

PR #38 merged the release-engineering foundation alongside the Android Auto Now Playing slice. The first signed prerelease, `v0.1.0-alpha.1`, was subsequently published through the GitHub Release workflow with both APK and SHA-256 assets.

The durable distribution policy is defined in `docs/RELEASES.md`:

- Google Play is not a distribution channel for AALyrics;
- durable builds are release-signed APKs published through GitHub Releases;
- prerelease version tags such as `v0.1.0-alpha.1` are GitHub Pre-releases and may intentionally represent incomplete development milestones;
- suffix-free tags such as `v0.1.0` are reserved for stable releases;
- release tags must point to commits contained in `main`;
- one persistent release signing identity is required for update compatibility.

Signed prereleases have been published successfully through `v0.2.0-alpha.2`; later tags continue to follow the same documented release policy.

## Android Auto media presentation track

The Android Auto product direction is now fixed even though its Car App Library implementation is not yet authorized as an active slice.

AALyrics remains a **Media app** and will keep two presentation paths in the same product:

```text
planned primary path
    Car App Library templated media
    -> androidx.car.app 1.8.0-rc01
    -> MEDIA category
    -> SectionedItemTemplate
    -> MediaPlaybackTemplate where appropriate

compatibility path
    existing MediaBrowserServiceCompat
    -> legacy Android Auto media presentation
```

The existing MediaSession/media-service architecture is retained. Adding Car App Library does not mean deleting the MediaBrowser path.

Sideload policy is also fixed:

- AALyrics is distributed outside Google Play.
- Android Auto `Unknown sources` is a legacy-media compatibility requirement, not a global requirement for the Car App Library templated path.
- Notification Access remains the only blocking phone setup gate.
- Android Auto compatibility onboarding is advisory and records `ENABLED` or `SKIPPED`; it does not pretend to verify the Android Auto developer setting.
- Exact host-selection/fallback behavior must be validated in DHU and on real Android Auto hardware before being treated as guaranteed runtime behavior.

See:

- `docs/ANDROID_AUTO_MEDIA_STRATEGY.md`
- `docs/ANDROID_AUTO_COMPATIBILITY.md`

The future Car App Library implementation must remain a dedicated topic branch/PR and must preserve the working legacy media path unless a later explicit decision changes this strategy.

## Later phases

Remaining later work includes Timing/Sync integration, Karaoke projection/rendering, Android Auto Translation and the authorized Car App Library implementation described above, capability-specific settings/persistence not yet justified, release-process refinement beyond the established GitHub Release baseline, and continued regression comparison against the previous fork.

Do not use future feature needs as a reason to turn `LyricsCoordinator`, `PlaybackLyricsController`, the media-session runtime, demand gate, production selector, concrete providers, capability services, or the shared design system into god objects.

## Working method

Each implementation phase is split into small topic branches and PRs. `TASK.md` records the active branch-local plan/status when relevant, while this document remains the durable project roadmap.

Before starting any non-trivial implementation slice:

1. check `docs/MIGRATION_INVENTORY.md`,
2. inspect the current `whoxamxl/auto-lyrics` main branch for equivalent behavior when relevant,
3. read `docs/LYRICS_PIPELINE_ARCHITECTURE.md` plus the relevant capability architecture document for Phase 11.x work,
4. classify inherited behavior as PRESERVE, REFACTOR, REWRITE, or DROP when migration is involved,
5. define the current PR acceptance criteria and the smallest justified contracts,
6. implement only after the user has explicitly authorized implementation for that slice,
7. add executable architecture guardrails only for concrete boundaries that now exist in code,
8. run CI and the bounded review process in `AGENTS.md`,
9. stop before merge for explicit approval.
