# Phone Runtime Host

## Status

This document defines the approved application-composition slice that makes the production Phone UI reachable from a debug APK on a physical device.

The implementation branch is `feature/phone-shell-runtime-host`, based on `main` after PR #49.

PR #50 implements this application-composition slice on `feature/phone-shell-runtime-host`.

The current production branch state now has:

- the existing entry/onboarding flow preserved;
- `MainActivity` migrated to a Compose-capable Activity while retaining the existing View-based setup screens;
- `AppEntryState.READY` hosting `AALyricsTheme -> PhoneAppShell`;
- live app-owned playback, lyrics, Details, Translation, model, and Verbose Details state collected lifecycle-aware;
- Playback Surface commands routed through the existing application/platform media boundary;
- selected MediaSession album artwork forwarded through an app-owned Android boundary and rendered in both Track Card and Playback Surface, with the AALyrics mark as the no-artwork fallback;
- playback source packages resolved to human-readable app labels where possible, with the package identifier retained as the final normal-UI fallback and always exposed separately in Verbose Details;
- Translation persisted default disabled on an unconfigured install while English remains the built-in/default target;
- production Lyrics and Settings presentation mapping;
- in-app License navigation backed by build-synchronized repository `NOTICE` + `LICENSE`, rendered through the shared Phone Markdown wrapper;
- adopted Phone-local popup and second-level Settings-header primitives;
- an explicit non-functional Sync placeholder;
- unsupported Update controls presented unavailable rather than wired to no-ops, while Changelog is supplied offline from the bundled repository `CHANGELOG.md`.

The debug APK now builds with the real Phone shell reachable after onboarding prerequisites are satisfied. Physical-device smoke testing remains the final empirical validation step; CI alone does not claim that device interaction has been observed.

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
└─ playbackSurface       <- AALyricsApplication.phonePlaybackSurfaceState
```

The Top Bar, Playback Surface, and Bottom Navigation remain shell-owned.

The runtime-host slice may add the minimal application-owned source-label/shell mapping needed to avoid duplicating package-label logic in the Activity.

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

`LyricsScreen` is production Compose, but the production runtime route/mapping is still incomplete.

This slice may add the minimal app-owned Phone lyrics presentation mapper/route necessary to render live data from the existing canonical sources:

```text
PlaybackSnapshot
LyricsState
TranslationState / Translation settings where already approved
Phone presentation preference state
        ↓
LyricsScreenUiState
        ↓
LyricsScreen
```

The mapper must preserve existing approved semantics:

- canonical lyrics ownership stays in `:core:lyrics`;
- current-line timing is derived from existing playback/lyrics facts rather than a new timing algorithm;
- PLAIN auto-scroll follows the existing presentation contract;
- WORD-capable source lyrics do not implicitly enable Karaoke mode;
- Translation remains an additive derived capability;
- provider DTOs and provider-specific logic do not enter `:ui:phone`.

The runtime now forwards selected-session artwork from `METADATA_KEY_ALBUM_ART`, `METADATA_KEY_ART`, or `MediaDescription.iconBitmap` through `:app` into the existing renderable artwork slots. Android Bitmap/MediaSession ownership does not enter `:ui:phone`. Missing artwork uses the shared AALyrics foreground mark derived from `branding/android/AALyrics_foreground_android.svg`.

## Sync destination

Sync remains interaction-model-deferred.

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

The host should render the production `DetailsScreen` from the existing application-owned `phoneDetailsState`.

The existing canonical playback-identity/stale-state rules remain unchanged.

Verbose Details remains presentation-only:

```text
Settings > Advanced > Verbose details
        ↓
application-owned persisted preference
        ↓
AALyricsApplication.phoneDetailsState
        ↓
DetailsScreen
```

The runtime-host slice must not add provider/network requests for diagnostics.

## Settings destination

The host should render the production `SettingsScreen` through presentation-ready state and callbacks.

Existing application/runtime seams should be wired where they already exist or can be exposed without changing capability semantics, including:

- Translation enabled state;
- existing Translation target/model-management capability where already implemented;
- Android Auto compatibility acknowledgement/status and setup re-entry;
- Verbose Details;
- app/build version facts;
- other already-implemented application-owned Settings state.

The runtime-host slice may add the minimal application-owned mapping required to assemble `SettingsScreenUiState`. This includes read-only build/repository document content such as `licenseText`: `:app` owns Android asset access and passes presentation-ready text into `:ui:phone`; the UI does not read assets or fetch GitHub directly.

### Unsupported Settings actions

Device-test enablement must not turn unfinished runtime capabilities into fake working controls.

If a Settings control has no backing runtime implementation yet, do **not** wire an active no-op callback. Instead, the runtime-host implementation should make the unavailable state explicit through the smallest presentation change necessary, or keep the action unavailable until its capability slice exists.

This rule is especially important for functionality such as release update/download or any future network-backed Settings surface if no production application runtime currently owns it.

Do not expand this slice into implementation of those capabilities merely because the Settings UI already contains their presentation contract.

## UI-local state

The following may remain UI/host-local because they do not represent durable application policy:

- currently selected Phone primary destination;
- Settings sub-screen selection/visibility (currently Main / Advanced / Changelog / License);
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

A physical-device smoke test is strongly useful once the APK is produced, but merge authorization remains a separate user decision.

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
