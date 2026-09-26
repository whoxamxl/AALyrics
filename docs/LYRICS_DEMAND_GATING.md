# Lyrics Demand Gating

## Status

- Branch: `feature/lyrics-demand-gating`.
- Base: main `c0bfb15` after live MediaSession runtime PR #29 merged.
- Working-fork reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).
- Classification: **PRESERVE / REFACTOR** for the proven demand semantics; **REWRITE** for ownership/integration into the AALyrics architecture.
- State: **IMPLEMENTED AND MERGED — PR #30.**
- Current production extension: PR #84 (`feature/android-auto-now-playing`) adds active `LyricsBrowserService` lifetime as a third independent demand source so Android Auto process recreation does not depend solely on `CarConnection` delivery.

## Purpose

The live MediaSession runtime can now continuously discover/select playback and feed normalized `PlaybackSnapshot` values into the production lyrics pipeline. Without demand gating, that means provider lookup may continue while no AALyrics surface is actually being used.

This slice separates lightweight playback/session tracking from expensive lyrics work:

```text
MediaSession runtime
        |
        v
latest PlaybackSnapshot
        |
        v
Lyrics demand gate
   |             |
 demand OFF      demand ON
   |             |
 suspend work    v
                 PlaybackLyricsController
                        |
                        v
                  provider lookup
```

MediaSession discovery and selected-controller observation stay alive independently. Demand controls only whether normalized playback is allowed to own/start lyrics work.

No-demand retention is deliberately narrower than a lyrics cache. It keeps only the currently resolved usable lookup state inside the live process; it is not persisted, does not add multi-track cache policy, and does not survive process death. A real provider/disk cache remains separate future work.

## Demand semantics

PR #30 originally preserved the mature working-fork two-source rule. Current production semantics extend it with the actual Android Auto browser-service lifetime:

```text
phone process foreground
        OR
Android Auto projection connected
        OR
LyricsBrowserService active for the Android Auto host
        =
lyrics demand active
```

All demand sources are independent. Demand remains active until the final active source is removed. The service-lifetime signal is recovery-oriented: it does not make the process immortal, but it ensures an Android Auto host-driven process recreation immediately re-establishes lyrics demand even if Phone UI is closed and `CarConnection` has not yet republished projection state.

### Phone demand

Phone demand represents the AALyrics application process being in the foreground, not a particular composable being visible.

Use process lifecycle semantics equivalent to the working fork's `ProcessLifecycleOwner` behavior so brief activity recreation/configuration changes do not cause provider work to flap off/on between activity instances.

This slice must not add finished phone lyrics UI merely to generate demand.

### Android Auto demand

Automotive demand now has two independent application-level signals:

- `CarConnection.CONNECTION_TYPE_PROJECTION` keeps the original projection-wide demand semantics, so provider work may remain ready while Android Auto is connected even if the AALyrics surface is not foreground;
- active `LyricsBrowserService` lifetime reflects that the Android Auto host is actually bound to the legacy media surface. `onCreate()` adds host-service demand and `onDestroy()` removes only that source.

The second signal was added by the Android Auto Now Playing completion so process recreation through the media-browser service can recover without reopening the Phone Activity. It remains a demand signal only; the Automotive UI/service does not call providers directly.

## Gate ownership

Demand aggregation belongs at the application/runtime lifecycle boundary, not inside providers, `LyricsCoordinator`, or UI composables.

`:platform:media` should remain responsible for Android media-session observation and normalization only. It must not learn phone/automotive presentation lifecycle policy.

The application composition root should own or wire a narrow demand-gating component between the existing `MediaSessionRuntimeHost` playback sink and `PlaybackLyricsController`.

Conceptually:

```text
MediaSessionRuntimeHost
        |
        v
PlaybackSnapshot sink
        |
        v
LyricsDemandGate              (:app / application lifecycle boundary)
        |
        v
PlaybackLyricsController      (:core:lyrics)
```

The gate may depend on normalized `PlaybackSnapshot` and the existing playback-controller boundary. It must not call concrete providers, candidate selection, or `LyricsCoordinator` directly.

"Normalized" includes identity/timeline coherence from `:platform:media`: a retained snapshot must describe one logical track sample. The gate stores and replays that snapshot as-is; it must not combine a stable identity with timing from a pending different track or attempt to repair MediaSession timestamp contradictions.

## Snapshot and transition behavior

The gate must retain the latest normalized `PlaybackSnapshot` even while demand is inactive.

Required behavior:

1. Initial demand is inactive until a demand source reports active.
2. While demand is inactive, incoming playback snapshots update the retained latest snapshot but are not forwarded into lyrics lookup.
3. Transition `OFF -> ON` immediately replays the latest retained snapshot into `PlaybackLyricsController` so current playback does not need to change before lyrics load.
4. While demand remains active, playback snapshots flow through normally; existing `PlaybackLyricsController` identity semantics continue to suppress lookup restarts caused only by position/status/rate/duration churn.
5. Transition `ON -> OFF` suspends current lyrics work. In-flight provider work is cancelled. A completed usable `Ready` / `Degraded` result may remain owned in process memory so the same playback identity can resume without refetching; incomplete or unusable states drop ownership and restart normally when demand returns.
6. Repeated identical demand updates must be idempotent: no repeated clear and no duplicate activation replay.
7. If playback becomes empty/no-track while demand is inactive, the retained latest snapshot must reflect that state so a later activation does not resurrect an obsolete track.

Demand gating must not stop or detach MediaSession observation merely to stop provider work.

## Lifecycle and failure rules

- Demand aggregation must be safe when phone and automotive lifecycle events arrive independently.
- Removing one demand source must not deactivate lyrics work while any other demand source remains active.
- Notification-listener disconnect/security failure continues to be owned by the MediaSession runtime; disconnect now also requests the system listener binding again so active-session observation can recover after process/service recreation. Any resulting no-track snapshot remains compatible with demand gating.
- Demand deactivation must suspend current lookup through the existing playback/lookup boundary rather than reaching into provider jobs directly. Resolved usable lyrics may remain in memory; in-flight work must still be cancelled.
- Demand activation must not synthesize track metadata or bypass `MediaControllerSnapshotAdapter`.
- Process/activity recreation must not create rapid OFF/ON provider churn.
- The gate must not change provider ranking, lookup identity, or MediaSession session-selection semantics.

## Explicitly out of scope

Do not implement in this branch:

- finished phone lyrics UI;
- finished Android Auto presentation/browsing UI;
- general-purpose provider/disk lyrics cache;
- translation;
- artwork/color extraction;
- transport controls;
- timing offset/calibration;
- current-line/current-word rendering;
- karaoke rendering;
- settings/persistence UI;
- provider transport/search changes;
- cross-provider scoring/selection changes;
- MediaSession selection-policy changes.

## Regression coverage

Add deterministic coverage for at least:

- initial no-demand state does not forward provider-owning playback;
- latest playback is retained while demand is off;
- `OFF -> ON` replays the latest snapshot exactly once;
- `ON -> OFF` suspends lookup work exactly once;
- repeated same-value demand updates are no-ops;
- phone demand alone activates the gate;
- CarConnection projection demand alone activates the gate;
- Automotive host-service demand alone activates the gate;
- removing one source while another remains active keeps demand on;
- only removing the final active source deactivates demand;
- track changes while demand is off do not trigger lookup but the newest track is used on activation;
- empty/no-track playback while demand is off prevents stale replay;
- normal playback churn while demand is on retains existing `PlaybackLyricsController` no-refetch behavior;
- resolved same-track lyrics survive demand loss and resume without another provider request;
- loading lookup work is cancelled on demand loss and restarts when the same track resumes;
- brief phone activity recreation does not create a false process-level demand drop where `ProcessLifecycleOwner` semantics are used.

Prefer pure deterministic tests for demand aggregation/gate transitions, with narrow Android tests only for lifecycle adapters that genuinely require framework behavior.

## STOP gate

This slice is complete when:

> Media sessions continue to be tracked independently from provider demand. Provider lookup occurs while Phone foreground, Android Auto projection, or active Automotive host-service demand is present; disabling the final demand source cancels in-flight provider work while preserving an already resolved usable result in process memory, and re-enabling demand immediately resumes the latest observed playback without refetching when its identity is unchanged.

Run the normal repository validation and bounded review from `AGENTS.md`, then stop before merge for explicit approval.

## Implementation result

The implemented application boundary preserves the working-fork demand rule without moving lifecycle policy into media or presentation modules:

- `LyricsDemandGate` retains every normalized `PlaybackSnapshot` received from `MediaSessionRuntimeHost` and forwards it to `PlaybackLyricsController` only while combined demand is active;
- phone-process, automotive-projection, and Automotive host-service inputs are stored independently and combined with logical OR;
- activation replays the retained snapshot once; final demand deactivation suspends the playback/lookup boundary, retaining an already resolved usable result but cancelling incomplete work; unchanged source/aggregate states are no-ops;
- source ineligibility remains a hard clear and does not reuse a retained result from a blocked playback source;
- `ProcessLifecycleOwner` supplies phone demand, preserving its delayed process-stop behavior across brief Activity recreation;
- `CarConnection.CONNECTION_TYPE_PROJECTION` supplies projection-wide automotive demand through an application-owned observer;
- `LyricsBrowserService` supplies host-service demand through the narrow `AutomotiveHostDemand` application boundary, allowing Android Auto-driven process recreation to reactivate lyrics work without Phone foreground;
- MediaSession discovery, selected-controller callbacks, normalization, providers, and selection remain independently owned; the Automotive service only reports lifecycle demand and consumes presentation-ready state.

Deterministic tests cover all gate transitions, independent sources, retained/newest/empty snapshots, active playback churn, process-lifecycle idempotence, application preference ownership, cancellation of in-flight provider work when the final demand source turns off, and same-track foreground resume without a second provider request.
