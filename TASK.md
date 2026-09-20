# Phone Playback Surface

## Branch and baseline

- Branch: `feature/phone-playback-surface`.
- Base: `main` at `575d53c00aec66e788b45378b44b91d7d9e8a60d`.
- Classification: **PHONE PLAYBACK SURFACE / MEDIA CONTROL PRESENTATION**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_UI_SPEC.md`, `docs/PHONE_PLAYBACK_SURFACE.md`, and `docs/MEDIA_SESSION_RUNTIME.md`.
- Documentation direction is approved. Core implementation is complete enough for the user visual/interaction checkpoint; final review and merge remain deferred.

## Goal

Replace the current fixed Previous / Play-Pause / Next shell bar with one shell-owned two-state playback surface:

```text
Collapsed Playback Bar
        ⇅
Expanded Player
```

The collapsed state should stay compact and expose only Play/Pause directly. The expanded state should provide interactive seek, transport controls, Queue/Open-app fallback, and a Translation quick-control without changing MediaSession ownership or moving Android framework objects into `:ui:phone`.

## Accepted interaction model

### Collapsed Playback Bar

- compact artwork + one-line title + one-line artist;
- Track Card-style delayed marquee for overflowing identity text;
- Play/Pause is the only direct transport button;
- all other non-Play/Pause bar area expands the player;
- retain the current thin bottom playback-position indicator unchanged in role: informational and non-interactive;
- no permanent Previous/Next buttons in collapsed state.

### Expanded Player

- shell-owned overlay that grows upward from the Playback Bar;
- compact artwork + animated one-line title/artist;
- elapsed time + interactive seek bar + duration;
- control row: Quick controls / Previous / Play-Pause / Next / Queue-or-Open-app;
- destination content should not reflow to the full expanded height.

### Collapse rules

Collapse on:

- destination/backdrop tap outside the player;
- system Back while expanded;
- downward collapse gesture;
- player identity/header tap;
- selected-session loss with no eligible replacement.

Do not auto-collapse for ordinary transport, seek, track changes, Translation toggle, Queue open/close, or Phone destination switching.

### Seek behavior

- direct seek dragging previews locally and emits one `seekTo()` on release;
- cancelled seek emits no command;
- incoming live position must not overwrite active local preview;
- all preview positions clamp to track duration.

### Previous / Next

Tap behavior stays:

```text
Previous tap -> skipToPrevious()
Next tap     -> skipToNext()
```

Long press uses deterministic AALyrics-owned relative-seek preview rather than MediaSession `rewind()` / `fastForward()`:

- platform long-press threshold starts scrub mode;
- one haptic confirms long-press recognition;
- corresponding tap/skip is suppressed;
- seek bar/time preview moves smoothly while held;
- initial target rate is 5 seconds of media per 1 second of continued hold;
- release emits exactly one `seekTo(previewPositionMs)`;
- cancellation emits no seek;
- long press is unavailable when seek is unsupported even if skip is supported.

### Queue / Open playback app

Rightmost slot priority:

```text
usable queue -> Queue
otherwise launchable source -> Open playback app
otherwise -> no active trailing action
```

Queue is capability-driven and never inferred only from package name.

### Quick controls

Leftmost slot opens a compact quick-controls popup.

First approved item only:

```text
Translation                     [ON/OFF]
```

It must use the same application-owned Translation enabled state as Settings.

## Architecture boundaries

`:ui:phone` receives presentation-ready state and callbacks only.

It must not depend on:

- raw `MediaController`;
- `PlaybackState` action bitmasks;
- `MediaSession.Token`;
- framework queue objects;
- `PendingIntent`;
- Android launch intents.

`:platform:media` remains the selected-session/framework owner and may normalize supported actions, queue, launch capability, and other session facts needed by application presentation mapping.

The current framework-neutral `PlaybackTransport` already exposes play/pause/previous/next/seek. The long-press design must compose on `seekTo()`; do not add `rewind()` or `fastForward()` for this feature.

## Documentation checkpoint

- [x] Create `docs/PHONE_PLAYBACK_SURFACE.md`.
- [x] Define collapsed and expanded presentation.
- [x] Define explicit collapse triggers and non-triggers.
- [x] Define direct seek preview/commit behavior.
- [x] Define tap-vs-long-press gesture state machine.
- [x] Define deterministic relative-seek behavior using one-shot `seekTo()`.
- [x] Define Queue/Open-app capability fallback.
- [x] Define Translation quick-control ownership.
- [x] Define capability/presentation boundaries.
- [x] Align `docs/PHONE_UI_SPEC.md`.
- [x] Align `docs/MEDIA_SESSION_RUNTIME.md`.
- [x] Report documentation checkpoint to the user before implementation.

## Implementation acceptance criteria

Implementation begins only after the documentation checkpoint above.

- [x] Add capability-aware playback presentation state.
- [x] Preserve Android framework ownership in `:platform:media`.
- [x] Replace current fixed `PlaybackControlsBar` presentation with collapsed Playback Bar.
- [x] Add Expanded Player overlay and deterministic collapse/back handling.
- [x] Add direct interactive seek preview/one-shot commit.
- [x] Add Previous/Next long-press relative-seek state machine.
- [x] Prevent long press from also dispatching tap skip.
- [x] Add haptic confirmation on long-press recognition.
- [x] Add Queue/Open-app trailing capability slot.
- [x] Add Translation quick-control popup using existing application state.
- [x] Reuse/tune Track Card marquee semantics for compact player identity.
- [x] Keep existing collapsed bottom progress indicator semantics.
- [x] Preserve at least 48dp touch targets and accessibility semantics.
- [x] Add deterministic Preview and test coverage described by the spec.
- [ ] User confirms the visual/interaction direction in Android Studio Preview / Interactive Preview.
- [ ] Run final CI and bounded Codex review after any user-requested visual tuning.
- [ ] Stop before merge until explicit user approval.

## Scope guard

Do not expand this slice into:

- MediaSession selection-policy changes;
- Notification Access onboarding;
- provider/scoring work;
- Android Auto player redesign;
- volume controls;
- shuffle/repeat;
- arbitrary custom MediaSession actions;
- full-screen now-playing;
- unrelated Settings work.
