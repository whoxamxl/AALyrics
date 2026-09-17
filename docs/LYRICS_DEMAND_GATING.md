# Lyrics Demand Gating

## Status

- Branch: `feature/lyrics-demand-gating`.
- Base: main `c0bfb15` after live MediaSession runtime PR #29 merged.
- Working-fork reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`).
- Classification: **PRESERVE / REFACTOR** for the proven demand semantics; **REWRITE** for ownership/integration into the AALyrics architecture.
- State: **IMPLEMENTED — validated and reviewed in PR #30; awaiting explicit merge approval.**

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
 clear lookup    v
                 PlaybackLyricsController
                        |
                        v
                  provider lookup
```

MediaSession discovery and selected-controller observation stay alive independently. Demand controls only whether normalized playback is allowed to own/start lyrics work.

## Demand semantics

Preserve the mature working-fork rule:

```text
phone process foreground
        OR
Android Auto projection connected
        =
lyrics demand active
```

The two demand sources are independent. Demand remains active while either source is active.

### Phone demand

Phone demand represents the AALyrics application process being in the foreground, not a particular composable being visible.

Use process lifecycle semantics equivalent to the working fork's `ProcessLifecycleOwner` behavior so brief activity recreation/configuration changes do not cause provider work to flap off/on between activity instances.

This slice must not add finished phone lyrics UI merely to generate demand.

### Android Auto demand

Automotive demand represents an active Android Auto projection connection, not whether the AALyrics automotive surface is currently the foreground AA app.

Preserve the working fork's `CarConnection.CONNECTION_TYPE_PROJECTION` semantics: while projection is connected, lyrics demand stays active so lyrics can remain ready when the user returns to AALyrics.

This slice must not implement Android Auto browsing/presentation UI.

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

## Snapshot and transition behavior

The gate must retain the latest normalized `PlaybackSnapshot` even while demand is inactive.

Required behavior:

1. Initial demand is inactive until a demand source reports active.
2. While demand is inactive, incoming playback snapshots update the retained latest snapshot but are not forwarded into lyrics lookup.
3. Transition `OFF -> ON` immediately replays the latest retained snapshot into `PlaybackLyricsController` so current playback does not need to change before lyrics load.
4. While demand remains active, playback snapshots flow through normally; existing `PlaybackLyricsController` identity semantics continue to suppress lookup restarts caused only by position/status/rate/duration churn.
5. Transition `ON -> OFF` clears current lyrics lookup ownership so in-flight/background provider work is cancelled and stale lyrics are not retained as active work.
6. Repeated identical demand updates must be idempotent: no repeated clear and no duplicate activation replay.
7. If playback becomes empty/no-track while demand is inactive, the retained latest snapshot must reflect that state so a later activation does not resurrect an obsolete track.

Demand gating must not stop or detach MediaSession observation merely to stop provider work.

## Lifecycle and failure rules

- Demand aggregation must be safe when phone and automotive lifecycle events arrive independently.
- Removing one demand source must not deactivate lyrics work while the other remains active.
- Notification-listener disconnect/security failure continues to be owned by the MediaSession runtime; any resulting no-track snapshot must remain compatible with demand gating.
- Demand deactivation must cancel/clear current lookup through the existing playback/lookup boundary rather than reaching into provider jobs directly.
- Demand activation must not synthesize track metadata or bypass `MediaControllerSnapshotAdapter`.
- Process/activity recreation must not create rapid OFF/ON provider churn.
- The gate must not change provider ranking, lookup identity, or MediaSession session-selection semantics.

## Explicitly out of scope

Do not implement in this branch:

- finished phone lyrics UI;
- finished Android Auto presentation/browsing UI;
- cache;
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
- `ON -> OFF` clears lookup ownership exactly once;
- repeated same-value demand updates are no-ops;
- phone demand alone activates the gate;
- automotive demand alone activates the gate;
- removing one source while the other remains active keeps demand on;
- only removing the final active source deactivates demand;
- track changes while demand is off do not trigger lookup but the newest track is used on activation;
- empty/no-track playback while demand is off prevents stale replay;
- normal playback churn while demand is on retains existing `PlaybackLyricsController` no-refetch behavior;
- brief phone activity recreation does not create a false process-level demand drop where `ProcessLifecycleOwner` semantics are used.

Prefer pure deterministic tests for demand aggregation/gate transitions, with narrow Android tests only for lifecycle adapters that genuinely require framework behavior.

## STOP gate

This slice is complete when:

> Media sessions continue to be tracked in the background, but provider lookup occurs only while phone-process foreground or Android Auto projection demand is active; disabling the final demand source clears active lyrics work, and re-enabling demand immediately resumes from the latest already-observed playback snapshot without requiring a track change.

Run the normal repository validation and bounded review from `AGENTS.md`, then stop before merge for explicit approval.

## Implementation result

The implemented application boundary preserves the working-fork demand rule without moving lifecycle policy into media or presentation modules:

- `LyricsDemandGate` retains every normalized `PlaybackSnapshot` received from `MediaSessionRuntimeHost` and forwards it to `PlaybackLyricsController` only while combined demand is active;
- phone-process and automotive-projection inputs are stored independently and combined with logical OR;
- activation replays the retained snapshot once, final deactivation sends one empty snapshot through the existing playback boundary, and unchanged source/aggregate states are no-ops;
- `ProcessLifecycleOwner` supplies phone demand, preserving its delayed process-stop behavior across brief Activity recreation;
- `CarConnection.CONNECTION_TYPE_PROJECTION` supplies projection-wide automotive demand through an application-owned observer;
- MediaSession discovery, selected-controller callbacks, normalization, providers, selection, and UI modules are unchanged.

Deterministic tests cover all gate transitions, independent sources, retained/newest/empty snapshots, active playback churn, process-lifecycle idempotence, application preference ownership, and cancellation of in-flight provider work when the final demand source turns off.
