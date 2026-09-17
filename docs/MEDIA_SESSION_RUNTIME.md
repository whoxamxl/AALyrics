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

Notification access itself is user-controlled system access. This slice provides the runtime/service boundary but intentionally does not add a settings screen or other presentation UI for that access.

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
- re-evaluates active sessions when the selected session is destroyed;
- unregisters callbacks/listeners when the notification listener disconnects or the service is destroyed.

Normal callback churn relies on the existing `PlaybackLyricsController` identity rules: position, status, rate, and duration changes alone do not start a new lyrics lookup, while a real track-identity change does.

## Metadata stabilization

The runtime retains the working fork's 600 ms delay for track-changing metadata because some media apps publish transient/intermediate metadata while changing tracks. The delay is owned by `SelectedMediaSessionRuntime` in `:platform:media`; playback status and position continue to update immediately against the last stable track identity.

Deterministic regressions verify the delay, replacement of older pending metadata, and callback ordering where playback-state notification arrives before metadata notification. The stabilization remains outside `PlaybackLyricsController` and does not redefine core lookup identity semantics.

Artwork, current-line timing, transport controls, cache, translation, and legacy `MediaTracker` state are intentionally not part of this stabilization logic.

## Demand gating handoff

The MediaSession runtime deliberately keeps session monitoring independent from lyrics-demand policy. PR #29 therefore forwards normalized playback whenever the listener runtime is connected.

The next dedicated lifecycle slice is `feature/lyrics-demand-gating`, documented in `docs/LYRICS_DEMAND_GATING.md`. That slice preserves the working fork's `phone process foreground OR Android Auto projection connected` demand rule while keeping MediaSession discovery/selection alive.

Demand gating belongs between the platform playback sink and `PlaybackLyricsController`: when demand is inactive, the latest normalized snapshot is retained but provider-owning playback is not forwarded; deactivation clears current lyrics work, and reactivation immediately replays the latest snapshot.

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

## Explicitly out of scope

The following remain separate work from the MediaSession runtime itself:

- lyrics-demand gating implementation, now specified separately in `docs/LYRICS_DEMAND_GATING.md`;
- finished phone lyrics UI;
- finished Android Auto presentation/service browsing UI;
- settings/persistence UI;
- cache;
- translation;
- artwork/color extraction;
- transport controls;
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
