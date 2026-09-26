# Media Session Runtime

## Status

- Branch: `feature/media-session-runtime`
- Original base: main `3ea97ce` after application-composition PR #25 merged; reconciled onto current main `1c8a875` before final PR review.
- Working-fork behavioral reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`), re-checked on 2026-09-17 and still current.
- Classification: **PRESERVE / REFACTOR** for session-selection behavior, **REWRITE** for ownership/integration.
- State: **IMPLEMENTED AND MERGED — PR #29.**

## Purpose

The production application graph already composes the lyrics engine, and this runtime now connects Android's active media sessions to the existing `PlaybackLyricsController` without coupling media ownership to phone or Android Auto presentation.

Implemented runtime path:

```text
NotificationListenerService
        |
        v
MediaSessionManager
        |
        v
active MediaController list
        |
        v
session selection / controller ownership
        |
        v
MediaController.Callback
        |
        v
MediaControllerSnapshotAdapter       (:platform:media)
        |
        v
PlaybackSnapshot                     (:core:model)
        |
        v
PlaybackLyricsController             (:core:lyrics)
        |
        v
LyricsCoordinator -> LyricsState
```

The STOP gate for this slice is runtime integration, not rendering: with notification-listener access enabled and a supported media app playing, real playback can drive the existing production lookup pipeline and update `LyricsState` even though finished lyrics presentation is a separate later concern.

## Android access boundary

AALyrics uses an enabled `NotificationListenerService` as the permission path for reading other apps' active media sessions. The service is declared with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` and the `android.service.notification.NotificationListenerService` intent action.

The service waits for `onListenerConnected()` before using notification-listener-backed APIs. `MediaSessionManager.getActiveSessions(...)` and `addOnActiveSessionsChangedListener(...)` receive the notification-listener component so the app does not depend on privileged `MEDIA_CONTENT_CONTROL` permission.

Notification access itself is user-controlled system access. PR #29 provided the runtime/service boundary but intentionally did not add presentation for granting that access.

### Application-entry onboarding follow-up

The later `feature/notification-access-onboarding` slice adds the application-entry behavior for this already-required system access without changing MediaSession ownership:

- `MainActivity` checks the real Notification Listener grant on launch and every resume;
- when access is missing, normal phone content is replaced by a required setup screen with no skip path;
- the grant action opens the app-specific Notification Listener detail page on API 30+ when available, then falls back to the general listener settings page and finally general Settings;
- API 26 remains supported with a read-only check of the enabled-listener setting because `NotificationManager.isNotificationListenerAccessGranted(...)` starts at API 27;
- returning from Settings always causes the actual system state to be checked again;
- `POST_NOTIFICATIONS` is not requested because this flow grants listener access for MediaSession observation, not permission for AALyrics to post notifications.

The finished Android Auto presentation remains a separate surface. The durable Android Auto direction is now documented in `docs/ANDROID_AUTO_MEDIA_STRATEGY.md`: AALyrics remains a Media app, plans a Car App Library 1.8.0-rc01 templated-media path using `SectionedItemTemplate`, and retains the current `MediaBrowserServiceCompat` path for compatibility. This does not change the ownership rules in this document: active-session observation remains a phone-side platform concern, and missing Notification Access must be resolved on the phone rather than from the car display.

The separate `docs/ANDROID_AUTO_COMPATIBILITY.md` document defines the advisory Android Auto `Unknown sources` onboarding for the sideloaded legacy media fallback. That compatibility acknowledgement must not be confused with or weaken the required Notification Access gate.

## Ownership and module boundary

Android media framework types remain in `:platform:media`.

`NotificationListenerService`, `MediaSessionManager`, `MediaController`, session tokens, callbacks, and session-selection implementation belong in `:platform:media`, not `:core:lyrics` and not provider modules.

The Android-created service cannot constructor-inject the app graph directly. A narrow platform-defined host/sink boundary delivers normalized `PlaybackSnapshot` values to the application composition root; the platform layer does not know `LyricsCoordinator`, concrete providers, candidate scoring, or presentation state.

The application boundary forwards normalized snapshots into the already-composed `PlaybackLyricsController`. `:platform:media` does not depend on concrete provider modules or `:provider:selection`.

## Session selection behavior

The runtime preserves the mature working-fork policy:

1. ignore AALyrics' own media session if one exists;
2. keep the currently selected session while it is still active and `PLAYING`;
3. otherwise select the first `PLAYING` session from the active-session list;
4. if nothing is playing, fall back to the first active session;
5. when no eligible session remains, clear selected-session ownership and forward an empty/no-track playback state;
6. retain the selected session by `MediaSession.Token`, not list position, so harmless active-session reordering does not switch sources.

The platform's active-session list is priority ordered, but provider lookup ownership does not depend purely on transient list ordering when the already-selected controller is still playing.

Sessions are re-evaluated when the active-session listener fires. The working fork's notification-posted refresh is retained as a compatibility fallback. Playback-state callbacks also trigger re-evaluation when the selected controller leaves `PLAYING`, allowing an already-active playing replacement to take ownership.

## Selected controller lifecycle

Only the selected controller has the runtime callback attached.

When ownership changes, the runtime:

- unregisters the old `MediaController.Callback`;
- registers the callback on the new controller;
- immediately normalizes and forwards the new controller's current snapshot;
- forwards relevant metadata/playback-state changes through `MediaControllerSnapshotAdapter`;
- keeps same-identity playback updates live while a metadata candidate is pending;
- never rewrites a different track's timeline onto the stable track identity;
- commits a different track identity and its timeline together after the 600 ms stabilization window;
- re-evaluates active sessions when the selected session is destroyed;
- unregisters callbacks/listeners when the notification listener disconnects or the service is destroyed.

Normal callback churn relies on the existing `PlaybackLyricsController` identity rules only after `:platform:media` has produced an internally coherent snapshot. Position, status, rate, and duration changes alone do not start a new lyrics lookup when they belong to the current stable identity; a real track-identity change is committed as one coherent playback snapshot and then starts fresh ownership.

## Playback position clock

`MediaControllerSnapshotAdapter` preserves two monotonic anchors for playback position:

- `PlaybackState.lastPositionUpdateTime` becomes `PlaybackSnapshot.positionUpdatedAtMonotonicMs` when the source supplies a valid timestamp. This is authoritative.
- AALyrics also records `SystemClock.elapsedRealtime()` as `PlaybackSnapshot.positionSampledAtMonotonicMs` at the moment the controller snapshot is sampled.

The local sample timestamp is a fallback only for media sessions that publish a position without a usable `lastPositionUpdateTime`. It stays attached to the immutable snapshot as it crosses the application boundary, so opening/recreating the Phone UI later cannot reinterpret an old `positionMs` value as newly sampled.

A source timestamp is not rejected merely because it is old. Old anchors are normal Android playback-state semantics. `PlaybackClockReconciler` rejects a source timestamp only when the published clock values contradict each other:

- the same source timestamp is observed again while raw `positionMs`, playback status, or playback rate changes;
- the source timestamp moves backwards on the same track;
- the source timestamp is later than the AALyrics local sample time.

Once a source timestamp is rejected, that rejected sample does not become the next comparison baseline. The reconciler retains the last accepted source snapshot and advances that baseline only when a source timestamp is accepted. A temporary snapshot with no source timestamp does not clear quarantine. Recovery requires a new valid non-null timestamp that is consistent with the last accepted source clock, or a track/session identity change.

A newly selected playing session that exposes both timestamps receives one 250ms validation re-sample. This catches the mid-track attach case where a player returns a current-looking raw position while retaining an older `lastPositionUpdateTime`. A valid Android anchor remains stationary at the raw position during that re-sample and is preserved.

Phone presentation projects playing position from the source timestamp when valid, otherwise from the stable local sample timestamp. The fallback clock is presentation-independent: LINE/WORD timing, PLAIN playback progress, Details progress, and the Playback Surface do not create separate anchors, and Karaoke enablement does not affect playback-time projection.

## Metadata stabilization

The runtime retains the working fork's 600 ms delay for track-changing metadata because some media apps publish transient/intermediate metadata while changing tracks. The delay is owned by `SelectedMediaSessionRuntime` in `:platform:media`.

Playback identity and timeline are atomic across this window:

- playback updates whose identity still matches the stable track continue to flow immediately;
- a snapshot whose identity differs from the stable track is never rewritten with the old track/source;
- cross-identity position/status/rate/timestamps are held until the metadata stabilization task commits the new track snapshot as one coherent unit.

This deliberately permits up to the stabilization window of visual staleness during a real track transition rather than fabricating an impossible snapshot such as Track A identity with Track B position.

The invariant at the `:platform:media -> PlaybackSnapshot` boundary is:

```text
emitted track/source identity
        +
emitted position/status/rate/timestamps
        =
one logical playback sample for the same track
```

Downstream application, timing, Karaoke, Details, and playback-surface code may project this sample, but must not repair or reinterpret cross-track identity/timeline mismatches because such mismatches must not cross the platform boundary.

Deterministic regressions verify the delay, replacement of older pending metadata, playback-first callback ordering, same-identity live updates during a pending candidate, and atomic commit of a different track timeline. The stabilization remains outside `PlaybackLyricsController` and does not redefine core lookup identity semantics. Clock hardening also covers the overlap between the 250ms initial clock validation and the 600ms metadata-stabilization task so clock reconciliation cannot leak a pending track timeline into the stable identity.

Artwork, current-line timing, transport controls, cache, translation, and legacy `MediaTracker` state are intentionally not part of this stabilization logic.

## Demand gating handoff

The MediaSession runtime deliberately keeps session monitoring independent from lyrics-demand policy. PR #29 therefore forwards normalized playback whenever the listener runtime is connected.

The next dedicated lifecycle slice is `feature/lyrics-demand-gating`, documented in `docs/LYRICS_DEMAND_GATING.md`. That slice preserves the working fork's `phone process foreground OR Android Auto projection connected` demand rule while keeping MediaSession discovery/selection alive.

Demand gating belongs between the platform playback sink and `PlaybackLyricsController`: when demand is inactive, the latest normalized snapshot is retained but provider-owning playback is not forwarded; deactivation cancels in-flight provider work while preserving an already resolved usable result in process memory, and reactivation immediately replays the latest snapshot.

`:platform:media` must remain unaware of phone/automotive demand policy.

## Failure and lifecycle rules

- Missing/disabled notification access fails safely without crashing the process.
- `SecurityException` from active-session APIs does not crash the service; runtime ownership is cleared/detached.
- A disconnected notification listener stops active-session observation and selected-controller callbacks.
- No eligible session clears the current playback lookup instead of leaving stale lyrics ownership.
- Session-list reordering alone does not switch away from a currently playing selected session.
- A destroyed selected session does not leave a dead callback/controller attached.
- Android framework objects do not leak into pure core/provider APIs.
- Live runtime callbacks do not start provider work directly; they feed normalized playback through the application boundary.

## Phone playback-surface follow-up

The later Phone playback-surface contract is defined in `docs/PHONE_PLAYBACK_SURFACE.md`.

That feature may extend the existing selected-session boundary with normalized playback capabilities needed by presentation, including supported action availability, queue availability/identity, source-app launch capability, and artwork/source metadata. The architectural ownership rule does not change:

- Android `MediaController`, `PlaybackState`, `MediaSession.Token`, framework queue items, and `PendingIntent` remain outside `:ui:phone`;
- the Phone layer receives presentation-ready capability state and callbacks only;
- the existing framework-neutral `PlaybackTransport` remains the transport boundary for play, pause, previous, next, and `seekTo`;
- Previous/Next long-press relative seek is deliberately implemented by local preview plus a single `seekTo()` on release, not by adding framework `rewind()` / `fastForward()` behavior;
- queue selection uses the framework-neutral `skipToQueueItem(id)` command; published non-empty queue data is the availability signal because some sessions, confirmed with Spotify, omit `ACTION_SKIP_TO_QUEUE_ITEM` while still accepting the command;
- queue entries are normalized to at most 20 presentation-safe items; `MediaDescription.iconUri` crosses the boundary only as a String, while embedded `iconBitmap` data uses an Android-owned queue-artwork side channel that stops at `:app`;
- opening the selected playback app remains application/platform navigation behavior rather than a media transport command.

The original PR #29 runtime did not own finished transport presentation. This follow-up consumes and may narrowly extend that runtime without changing its session-selection policy.

## Explicitly out of scope

The following remain separate work from the MediaSession runtime itself:

- lyrics-demand gating implementation, now specified separately in `docs/LYRICS_DEMAND_GATING.md`;
- finished phone lyrics UI;
- finished Android Auto presentation/service browsing UI;
- settings/persistence UI;
- cache;
- translation;
- artwork/color extraction;
- finished Phone playback-surface presentation and its capability mapping (specified separately in `docs/PHONE_PLAYBACK_SURFACE.md`);
- current-line/current-word rendering state;
- timing offset/calibration;
- karaoke rendering;
- provider transport/search behavior;
- cross-provider scoring/selection-policy changes.

## Tests and validation

Deterministic coverage includes:

- current selected playing session retained across list reorder;
- a new playing session selected when the previous selection is no longer playing/available;
- fallback to the first active session when none is playing;
- self-package sessions ignored;
- empty session list clears ownership;
- selected-session change detaches the previous controller callback and attaches the new one;
- session destruction triggers re-selection/clear behavior;
- normalized snapshots reach the playback-controller/application boundary;
- non-identity playback churn does not restart lookup;
- missing permission / `SecurityException` paths fail safely;
- stopped selected session refreshes to an already-active playing replacement;
- metadata stabilization and callback ordering regressions.

Repository validation for PR #29 includes:

```text
./gradlew test check :app:assembleDebug
bash scripts/verify-architecture.sh
git diff --check
```

The bounded review process in `AGENTS.md` completed with one P2 fixed in `7a5e82f`; the targeted second review reported no major issues.

## Implementation result

The implemented runtime keeps all Android session access in `:platform:media`:

- `MediaSessionListenerService` waits for `onListenerConnected()`, observes active sessions through its notification-listener component, and retains notification-posted refresh as a compatibility fallback;
- `SelectedMediaSessionRuntime` owns token-based selection, the single selected callback, clear/re-selection behavior, stopped-session handoff, and the retained 600 ms metadata stabilization;
- `MediaControllerSnapshotAdapter` remains the exclusive Android-to-`PlaybackSnapshot` normalization path;
- `MediaSessionRuntimeHost` is the narrow platform/application handoff;
- disconnect and `SecurityException` paths detach ownership and clear stale playback state.

PR #29 intentionally did not add demand gating, provider changes, selection-policy changes, or finished presentation behavior.
