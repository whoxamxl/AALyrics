# Phone Runtime Host

## Status

This document defines the production application-composition boundary that hosts the Phone UI and connects application-owned runtime state to presentation.

PR #50 established the original runtime-host slice. Subsequent merged work has extended the same boundary for Settings/help/legal surfaces, playback-source policy, the verified update runtime, and Phone Translation/diagnostics without moving those capability implementations into `:ui:phone`.

The current production branch state now has:

- the existing entry/onboarding flow preserved;
- `MainActivity` migrated to a Compose-capable Activity while retaining the existing View-based setup screens;
- `AppEntryState.READY` hosting `AALyricsTheme -> PhoneAppShell`;
- live app-owned playback, lyrics, Details, Translation, model, and Verbose Details state collected lifecycle-aware;
- Playback Surface commands routed through the existing application/platform media boundary;
- selected MediaSession album artwork forwarded through an app-owned Android boundary and rendered in both Track Card and Playback Surface, with the AALyrics mark as the no-artwork fallback;
- playback source packages resolved through an application-owned metadata boundary to a human-readable app label and source app icon where possible, with the package identifier retained as the final normal-UI fallback and always exposed separately in Verbose Details; Android application category and min/target SDK levels are available to Verbose Details from the same resolved metadata;
- Translation persisted default disabled on an unconfigured install while English remains the built-in/default target;
- production Lyrics and Settings presentation mapping, including memoized static Phone lyric-row projection outside the playback clock tick;
- in-app License navigation backed by build-synchronized repository `NOTICE` + `LICENSE`, rendered through the shared Phone Markdown wrapper;
- adopted Phone-local popup and second-level Settings-header primitives;
- an explicit non-functional Sync placeholder;
- a complete application-owned verified update runtime (manual/automatic discovery, download/integrity, install permission, PackageInstaller, recovery, and post-update feedback), while Changelog remains supplied offline from the bundled repository `CHANGELOG.md`.

The debug APK builds with the real Phone shell reachable after onboarding prerequisites are satisfied. Physical-device smoke testing has been exercised for the production Phone shell and, for the Translation follow-up, the refined Secondary/Details behavior; CI/build results and device observations remain separate validation evidence.

The merged Settings legal/help implementation preserves this host boundary: `:app` supplies bundled legal/help document text and owns external browser/Custom-Tab launches, while `:ui:phone` remains presentation-only.

## Goal

Preserve the existing app-entry gates and replace only the READY-state placeholder with the real Phone Compose host:

```text
MainActivity
    ↓
AppEntryState
├─ NOTIFICATION_ACCESS_REQUIRED
│    └─ existing Notification Access setup View
├─ ANDROID_AUTO_COMPATIBILITY
│    └─ existing Android Auto compatibility setup View
└─ READY
     └─ Compose host
          └─ AALyricsTheme
               └─ PhoneAppShell
                    ├─ Lyrics
                    ├─ Sync placeholder
                    ├─ Details
                    └─ Settings
```

The success criterion is not merely that Compose renders. The debug APK must expose the real production Phone shell with live application state and honest interaction availability so physical-device UI testing becomes meaningful.

## Host ownership

The runtime host belongs to `:app`.

`:ui:phone` continues to own rendering and UI-local interaction state only. It must not acquire:

- Android `MediaController` or raw `PlaybackState`;
- media-session discovery/selection;
- Android intents or package launching;
- SharedPreferences or other persistence;
- provider implementations/network clients;
- Translation engine/model-manager implementations;
- update/download network ownership.

Conceptually:

```text
AALyricsApplication / app-owned runtime
        ↓
presentation-ready StateFlow/state
        ↓
MainActivity READY host
        ↓
PhoneAppShell
        ↓
destination composables
        ↑
presentation callbacks
        ↑
app-owned actions/runtime
```

The READY host consumes the normalized playback snapshot produced upstream; it does not splice metadata from one track onto timing from another. The MediaSession runtime must commit track identity and timeline atomically across its 600 ms metadata-stabilization window. Phone mapping may project time from that coherent snapshot, but neither `:app` nor `:ui:phone` owns cross-track repair.

The host may own ephemeral presentation navigation such as the selected primary Phone destination. Durable user preferences remain application/capability owned.

## Entry-state preservation

The existing entry flow is a hard compatibility boundary.

Implementation must preserve:

1. Notification Access gating before the normal app surface.
2. Android Auto compatibility onboarding when its acknowledgement has not been reviewed.
3. `onResume()` re-evaluation of the entry state after returning from system settings.
4. Existing Enabled / Continue without acknowledgement behavior.

The runtime-host slice changes the READY branch only. It must not fold onboarding into `PhoneAppShell` merely because the READY surface moves to Compose.

Re-entering Android Auto compatibility setup from Settings may use host-local transient navigation back to the existing setup presentation, but ownership of the acknowledgement remains with the existing application/onboarding boundary.

## Compose host

The READY path now renders:

```text
AALyricsTheme {
    PhoneAppShell(...)
}
```

A migration from `Activity` to an appropriate Compose-capable Activity base is acceptable when required, provided the existing View-based setup screens and entry-state behavior remain intact.

The host should collect application state in a lifecycle-aware manner and should not create duplicate long-lived application scopes.

Primary destination selection:

- defaults to `PhoneDestination.Lyrics`;
- is owned by the Phone host, not by `AALyricsApplication`;
- should survive ordinary Activity recreation where practical;
- does not require introducing a general navigation framework.

## Shell state

The runtime host provides a real `PhoneShellUiState`:

```text
PhoneShellUiState
├─ selectedDestination   <- host-local primary destination
├─ mediaSourceLabel      <- app-owned playback/source presentation
├─ mediaSourceConnectionState <- Connecting / Connected / Disconnected / Unavailable / Error
├─ mediaSourceUnavailableReason <- concise Unavailable reason when state is Unavailable
├─ mediaSourceErrorReason <- concise Error reason when state is Error
├─ mediaSourceCanOpenApp <- selected source has real launch capability
└─ playbackSurface       <- AALyricsApplication.phonePlaybackSurfaceState
```

The Top Bar, Playback Surface, and Bottom Navigation remain shell-owned.

Playback-source package metadata is owned by `:app`. The label-only resolver is replaced by `PlaybackSourceAppInfoResolver`, which performs one cached `ApplicationInfo` lookup per package and derives the presentation label, optional app icon, diagnostic category, minimum SDK level, and target SDK level. Connected resolution follows the selected playback package. Unavailable resolution prefers the package carried by the runtime state, allowing a policy-rejected session to retain real app identity even when no selected playback snapshot exists; if app metadata still cannot be resolved, the Top Bar falls back to its metadata-independent Unavailable presentation. The package identifier remains the human-readable label fallback after successful package identification but failed label lookup. The Top Bar icon is supplied as caller-owned renderable content, analogous to selected-session artwork, so `ApplicationInfo`, `PackageManager`, and Android `Drawable` objects do not become Phone UI state. When no icon is available for a known app, `PhoneTopBar` retains its cyan-dot fallback.

MediaSession observation publishes framework-neutral source/session health independently from Android application-category policy. The application layer then combines the selected runtime package, `PlaybackSourceAppInfo`, and persisted eligibility settings into an effective Phone/lyrics source state. A raw connected session may therefore remain selected and controllable while its effective AALyrics source state becomes `Unavailable(packageName?, reason)` for lyrics processing. Approved Unavailable reasons are `NON_AUDIO_APP`, `UNCLASSIFIED_APP`, and `UNKNOWN`; Error reasons remain `NOTIFICATION_ACCESS_LOST`, `SESSION_QUERY_FAILED`, `SESSION_ATTACH_FAILED`, and `UNKNOWN`. Category/settings policy must stay outside `:platform:media` because it depends on application metadata and user preferences.

The effective eligibility rules are: filtering OFF allows all selected sources; filtering ON allows `CATEGORY_AUDIO`; known non-audio categories become `NON_AUDIO_APP`; `CATEGORY_UNDEFINED`, unknown/future categories normalized to Undefined, and unresolved `ApplicationInfo` become `UNCLASSIFIED_APP` unless `Allow unclassified apps` is enabled. Rejection happens before provider lookup starts. The application-owned playback sink evaluates category/settings synchronously for each new snapshot, then updates `LyricsDemandGate` with the snapshot and eligibility in one atomic transition. This prevents a newly blocked source from briefly starting provider work and prevents a newly allowed source from replaying the prior blocked snapshot. Changing either eligibility setting re-evaluates the retained current snapshot immediately; blocking clears active lookup ownership, while allowing replays the retained snapshot exactly once when demand is active. MediaSession selection, callback ownership, playback transport, source-app launching, and provider ranking/scoring remain unchanged. Unavailable and Error details remain concise and are exposed from the Top Bar through the shared Phone popup/tooltip surface. Unavailable also has a metadata-independent generic fallback, so missing app identity cannot suppress the status.

Launch capability is resolved independently from track availability using the selected `PlaybackControlState`: explicit MediaSession activity first, then the package launcher fallback. That shared capability feeds both the Playback Surface and Top Bar. A Connected Top Bar pill is clickable only when this capability is true, preventing an active-looking external-link affordance from becoming a no-op.

Conceptually:

```text
PlaybackSourceRuntimeState + PlaybackSourceAppInfo
                    + playback-source eligibility settings
                              ↓
                 app-owned eligibility policy
                    ├─ allowed
                    │    └─ lyrics lookup may start
                    └─ blocked
                         ├─ effective Unavailable(reason)
                         └─ no lyrics-provider lookup
```

## Playback actions

`PhoneAppShell` already emits presentation callbacks for:

- Previous;
- Play/Pause;
- Next;
- seek;
- queue-item selection;
- Open playback app;
- Translation enabled/disabled.

The READY host routes these through application/platform seams that already own media/runtime behavior. The UI module must not import platform media objects.

Capability state remains authoritative. A control must not be made to appear functional by wiring a no-op callback.

This slice does not change media-session selection policy.

## Lyrics destination

`LyricsScreen` and the canonical Phone lyrics mapper are production Compose/runtime behavior. `PhoneRuntimeHost` now collects the already-running `TranslationCoordinator` output through `AALyricsApplication.translationState`, and `mapPhoneLyricsState` projects eligible translated lines without changing canonical lyrics ownership.

PR #79 closes the Phone Translation presentation gap with the following application-owned composition:

```text
PlaybackSnapshot ----------------------┐
LyricsState ---------------------------┤
TranslationState ----------------------┤
TranslationSettings -------------------┼─> Phone lyrics mapper
Phone presentation preference state ---┘
                                            ↓
                                   LyricsScreenUiState
                                            ↓
                                     LyricsScreen
```

The host lifecycle-collects the existing `translationState`, `translationSettings`, and model lifecycle state for presentation. It does not start or cancel Translation execution merely to render state. Explicit Track Card Retry remains an application-owned action through the existing retry boundary.

The PR #86 Phone viewport performance path keeps static row projection separate from high-frequency timing projection:

```text
playback identity + LyricsState + TranslationState/settings
        ↓
remembered canonical + optional translated row list
        │
        ├───────────────┐
        │               │
        ↓               ↓
250 ms normal tick   33 ms effective-Karaoke tick
        │               │
        └───────┬───────┘
                ↓
current timing / Karaoke presentation facts
                ↓
LyricsViewportUiState
```

`PhoneRuntimeHost` recomputes the static row list when playback identity, lyrics state, Translation state, or Translation settings change. A monotonic playback tick does not rebuild the complete row list. `mapPhoneLyricsState` continues to project current line/progress/Karaoke state at the required cadence while reusing the precomputed rows. This split is presentation performance policy only; it does not alter provider retrieval, timing semantics, Translation execution, or lyrics demand ownership.

The Phone mapper owns presentation composition for the permanent Track Card Translation status row. It combines current Translation settings/state with the existing model-lifecycle presentation facts so the UI distinguishes active automatic model download from actual Translation execution. Model-preparation feedback is route-scoped: only the current target plus matching **model-supported** Profile Primary/ACTIVE Secondary may produce `Downloading language models…`; unsupported detected languages, unrelated model work, and superseded model work must not affect the row. The UI receives only a Phone-local state such as OFF / ENABLED / DOWNLOADING_MODELS / TRANSLATING / READY(route) / NOT_REQUIRED / FAILED; it does not inspect ML Kit or Translation core types directly.

The status row is always reserved, preventing Translation transitions from changing Track Card height or shifting the LyricsViewport. A Failed row emits a semantic Retry callback to `:app`. The application retries only failed/timed-out models belonging to the current route (current target plus matching Profile Primary/ACTIVE Secondary, excluding built-in English), then republishes the current canonical lyrics/settings through `TranslationExecutionRuntime`; stale or unrelated model failures are not retried. Retry ownership remains application/capability-side.

The mapper must preserve existing approved semantics:

- canonical lyrics ownership stays in `:core:lyrics`;
- a Ready Translation artifact is used only when Translation is currently enabled, its request target equals the current normalized target setting, and its canonical identity exactly matches the canonical lyrics being mapped;
- preserved artifact lines are not duplicated as translated text;
- pending/not-required/failed Translation stays original-only and never becomes Lyrics failure;
- current-line timing is derived from existing playback/lyrics facts rather than a new timing algorithm;
- PLAIN auto-scroll follows the existing presentation contract;
- WORD-capable source lyrics do not implicitly enable Karaoke mode or translated-word highlighting;
- Translation remains an additive derived capability;
- provider DTOs, Translation engines, ML Kit, and provider-specific logic do not enter `:ui:phone`.

The durable Phone Translation handoff contract is in `docs/TRANSLATION_ARCHITECTURE.md`, `docs/PHONE_LYRICS_VIEWPORT.md`, and `docs/PHONE_DETAILS.md`; `TASK.md` is reserved for the currently active topic branch.

The runtime now forwards selected-session artwork from `METADATA_KEY_ALBUM_ART`, `METADATA_KEY_ART`, or `MediaDescription.iconBitmap` through `:app` into the existing renderable artwork slots. Android Bitmap/MediaSession ownership does not enter `:ui:phone`. Missing artwork uses the shared AALyrics foreground mark derived from `branding/android/AALyrics_foreground_android.svg`.

## Sync destination

Sync remains a deliberate non-functional placeholder. The timing architecture now fixes the downstream clock semantics (`effectiveLyricsPosition = projectedPlaybackPosition + lyricsOffset`, positive = advance lyrics, negative = delay lyrics), but this runtime-host slice still does not own a user offset, persistence, or calibration controls. The projected playback position begins from the identity/timeline-coherent `PlaybackSnapshot` emitted by `:platform:media`; the host does not correct MediaSession timestamp contradictions or metadata-transition mismatches.

For device navigation testing, the runtime host may render a deliberate non-functional placeholder that clearly indicates Sync is not available yet.

That placeholder must not introduce:

- offset controls;
- latency calibration;
- per-provider timing correction;
- automatic calibration;
- persistence;
- playback mutation.

A placeholder is presentation only and does not freeze the future Sync design.

## Details destination

The host renders the production `DetailsScreen` from the application-owned `phoneDetailsState`. Translation Details extends that same state boundary rather than creating a second Details mapper inside the Composable or host.

The existing canonical playback-identity/stale-state rules remain unchanged. Translation source/profile diagnostics must use the same current canonical identity gate; target setting/model inventory remain application-level facts.

Normal Translation Details uses:

- current matching LanguageProfile Primary;
- ACTIVE Secondary only, formatted as `Primary (Secondary)`;
- current normalized target language.

Verbose Details adds only:

- Runtime state;
- one positional Source model row that preserves Primary plus optional ACTIVE Secondary ISO/state pairing;
- one Target model row.

Application-owned mapping adapts Translation/core/model facts into Phone-local Details state. `:ui:phone` must not depend on `TranslationState`, `LanguageProfile`, `TranslationModelState`, ML Kit types, or raw exceptions.

Verbose Details remains presentation-only:

```text
Settings > Advanced > Verbose details
        ↓
application-owned persisted preference
        ↓
playback + lyrics + Translation diagnostics
        ↓
AALyricsApplication.phoneDetailsState
        ↓
DetailsScreen
```

The application-owned `phoneDetailsState` now combines the existing Translation settings/state/diagnostic evidence, model lifecycle state, and startup model-inventory reconciliation fact. `PhoneDetailsMapper` canonical-identity gates per-track profile/runtime evidence and emits Phone-local Translation Details state. Source-model diagnostics remain application-owned and positional: Primary plus optional ACTIVE Secondary are projected independently, and unsupported detected languages are represented as presentation-only `—` rather than model lifecycle work. The host continues consuming that resolved state rather than reconstructing Translation diagnostics ad hoc.

The runtime-host slice must not add provider/network requests for diagnostics. Verbose Details may reuse already-owned playback-source and Translation/model facts. Enabling Verbose Details must not start profiling, Translation, model download, retry, or provider lookup. Opening the Details destination itself also has no such side effects. The only destination-dependent work is presentation-local live progress ticking while playback is active; `verboseDetailsEnabled` is part of that effect's key so toggling Verbose while already on Details starts/stops the ticker immediately.

Failure-capable Details rows follow the `docs/PHONE_DETAILS.md` standard: `Failed` and `Timed out` carry a presentation-ready authoritative reason and use the shared semantic info-tooltip affordance. Missing runtime failure evidence must be fixed at the owning Translation contract rather than replaced by a guessed UI string.

## Settings destination

The host should render the production `SettingsScreen` through presentation-ready state and callbacks.

Existing application/runtime seams should be wired where they already exist or can be exposed without changing capability semantics, including:

- Translation enabled state;
- existing Translation target/model-management capability where already implemented;
- Android Auto compatibility acknowledgement/status and setup re-entry;
- Verbose Details;
- app/build version facts;
- other already-implemented application-owned Settings state.

The runtime-host slice may add the minimal application-owned mapping required to assemble `SettingsScreenUiState`. This includes read-only build/repository document content such as `noticeText`, `licenseText`, `changelogText`, and the approved `privacyPolicyText`: `:app` owns Android asset access and passes presentation-ready text into `:ui:phone`; the UI does not read assets or fetch GitHub directly.

For `Support AALyrics`, the UI emits an external-support callback only. `:app` owns opening the canonical Buy Me a Coffee destination, preferably through Android Custom Tabs with normal external-browser fallback. No payment state or credentials enter the Phone presentation model.

### Unsupported Settings actions

Device-test enablement must not turn unfinished runtime capabilities into fake working controls.

If a Settings control has no backing runtime implementation yet, do **not** wire an active no-op callback. Instead, the runtime-host implementation should make the unavailable state explicit through the smallest presentation change necessary, or keep the action unavailable until its capability slice exists.

This rule is especially important for functionality such as release update/download or any future network-backed Settings surface if no production application runtime currently owns it.

Do not expand this slice into implementation of those capabilities merely because the Settings UI already contains their presentation contract.

## UI-local state

The following may remain UI/host-local because they do not represent durable application policy:

- currently selected Phone primary destination;
- Settings sub-screen selection/visibility (Main / Advanced / Changelog / Privacy Policy / License / Support AALyrics);
- modal/picker visibility;
- Lyrics viewport follow/browse interaction mode;
- expanded/collapsed Playback Surface presentation state where already component-owned.

Persistent settings must not be moved into `MainActivity` or `:ui:phone`.

## Device-test acceptance

After implementation, a debug APK with prerequisites satisfied should allow a user to:

1. launch into the real Phone shell rather than the foundation build message;
2. switch among Lyrics, Sync placeholder, Details, and Settings;
3. observe live current-track/playback changes;
4. open and interact with the Playback Surface according to real media-session capabilities;
5. inspect live Details metadata;
6. toggle `Settings > Advanced > Verbose details` and see Details diagnostics appear/disappear;
7. verify unavailable features are visibly unavailable rather than silently no-op;
8. leave/return through system settings without breaking the onboarding gate.

This slice establishes **manual device UI testability**. It does not claim full instrumentation/end-to-end test coverage.

## Validation

Before merge:

- architecture boundary check passes;
- debug APK builds;
- JVM/unit tests pass;
- focused host/presentation tests cover durable mapping/entry behavior where practical;
- the final diff receives bounded Codex review;
- no current-scope blocking P0/P1/P2 remains.

A physical-device smoke test remains useful evidence in addition to CI/build checks. For PR #86, preliminary device observation indicates materially faster user-visible lyrics appearance after the lazy viewport change; this remains qualitative presentation evidence rather than an instrumented claim about provider/network latency. Merge authorization remains a separate explicit user decision.

## Explicitly deferred

This slice does not implement:

- Sync timing/calibration behavior;
- functional Karaoke mode;
- Karaoke WORD highlighting;
- provider ordering/preferences;
- provider/network changes;
- new lyrics candidate scoring;
- new Translation algorithms;
- persistent Translation Cache;
- media-session selection-policy changes;
- a general navigation framework solely for four local Phone destinations;
- speculative ViewModel layers;
- artwork/network loading infrastructure solely for the Phone host;
- update networking when no existing application runtime owns it;
- log export/viewer;
- theme/appearance settings;
- Android Auto screen redesign.
