# Phone Playback Surface Specification

## Status

This document defines the approved Phone playback-surface direction that replaces the current fixed three-button `PlaybackControlsBar`.

- Branch: `feature/phone-playback-surface`
- Base: `main` at `575d53c`
- Classification: **PHONE PLAYBACK SURFACE / MEDIA CONTROL PRESENTATION**
- State: **DOCUMENTED — implementation intentionally not started yet**

The implementation may proceed on the same branch after this contract is reviewed.

## Product intent

AALyrics should keep playback controls continuously reachable without dedicating a large permanent area to transport buttons.

The shell therefore owns one playback surface with two presentation states:

```text
Collapsed Playback Bar
        ⇅
Expanded Player
```

The collapsed state is information-dense and minimally intrusive. The expanded state provides seek, transport, queue/open-app, and quick-control functions only when the user asks for them.

This remains a controller for the media app selected through AALyrics' Notification Access / MediaSession runtime. AALyrics does not become the media owner.

## Terminology and ownership

### Playback Bar

The persistent collapsed surface shown immediately above Phone bottom navigation while a controllable selected media session exists.

### Expanded Player

The shell-owned expanded form of the same surface. It grows upward over destination content rather than navigating to a new destination.

The expanded/collapsed state belongs to the Phone shell presentation layer. It is not provider state, lyrics state, or MediaSession ownership state.

Conceptually:

```text
PhoneAppShell
├─ PhoneTopBar
├─ CurrentDestination
├─ PlaybackSurface
│  ├─ PlaybackBar          (collapsed)
│  └─ ExpandedPlayer      (expanded)
└─ PhoneNavigationBar
```

## Collapsed Playback Bar

The collapsed bar replaces the current permanent Previous / Play-Pause / Next button row.

Conceptually:

```text
┌──────────────────────────────────────────────┐
│ [art]  Track title                       [▶] │
│        Artist                                │
│━━━━━━━━━━━━━━━━ playback progress ━━━━━━━━━━│
└──────────────────────────────────────────────┘
```

Requirements:

- retain the existing thin playback-position indicator along the bottom edge;
- the collapsed progress indicator remains informational and non-interactive;
- show compact artwork when presentation-ready artwork is available;
- show title and artist in the identity area;
- keep Play/Pause as the only direct transport action in the collapsed state;
- tapping the identity/artwork/background portion expands the player;
- tapping Play/Pause must not also expand the player;
- Previous and Next are removed from the collapsed state;
- preserve accessible touch targets even though the visual treatment is compact.

The bar should remain visually compact enough that Lyrics, Sync, Details, and Settings keep the majority of the vertical viewport.

## Long metadata in the Playback Bar

Title and artist are each constrained to one visible line.

Long identity text should reuse the established Track Card marquee behavior rather than increase the bar height:

- remain still at the leading position first;
- scroll only when the identity block overflows;
- use a constant-speed horizontal marquee;
- preserve a clear repeat gap;
- return to the leading position and pause again;
- keep title and artist synchronized as one identity block.

The first implementation should reuse the same timing/velocity semantics as `TrackCard` unless Preview/device tuning shows that the smaller playback surface requires a dedicated token.

## Expanding the player

Tapping any non-Play/Pause portion of the collapsed Playback Bar expands the same shell-owned surface upward.

Expansion must not:

- change the current Phone destination;
- create a navigation-stack entry;
- restart playback observation;
- restart lyrics lookup;
- move MediaSession ownership;
- force destination content to re-layout to the full expanded height.

The existing collapsed playback-overlay inset remains the stable destination layout reservation. The Expanded Player is a temporary overlay above that baseline so opening it does not cause the Lyrics viewport or Settings content to jump.

## Expanded Player layout

Conceptually:

```text
┌──────────────────────────────────────────────┐
│ [art]  Track title                           │
│        Artist                                │
│                                              │
│ 00:52  ━━━━━━━━━━━●━━━━━━━━━━━━━━  03:41    │
│                                              │
│ [Tune]  [Previous]  [Play/Pause]  [Next] [Q]│
└──────────────────────────────────────────────┘
```

The expanded state contains:

1. compact artwork + title + artist identity;
2. an interactive seek row with elapsed time, seek control, and track duration;
3. a five-slot control row:
   - Quick controls;
   - Previous;
   - Play/Pause;
   - Next;
   - Queue when supported, otherwise Open playback app when available.

The title and artist remain one line each and use the same overflow marquee contract as the collapsed bar / Track Card.

The expanded player should remain compact. It is not intended to become a full-screen now-playing destination.

## Collapse triggers

Expanded Player collapse is explicit and predictable.

Collapse when:

- the user taps the destination/backdrop area outside the expanded surface;
- Android Back / system back gesture is invoked while expanded;
- the user performs the supported downward collapse gesture on the expanded surface;
- the player identity/header area is tapped as the inverse of tapping the collapsed bar;
- the selected MediaSession disappears and there is no eligible replacement session.

Do **not** automatically collapse because of:

- Play/Pause;
- Previous/Next;
- direct seek;
- long-press relative seek;
- Translation quick-toggle changes;
- track change;
- playback state change;
- Queue surface opening/closing;
- switching among Lyrics / Sync / Details / Settings.

The expanded/collapsed state therefore survives ordinary destination switching while the selected playback session remains available.

Back handling is ordered:

```text
Expanded Player + Back
    -> collapse player

Collapsed Player + Back
    -> normal destination/activity back behavior
```

## Expanded seek visual language

The Expanded Player seek control uses a One UI-inspired asymmetric dynamic waveform rendered natively with Compose Canvas.

Rules:

- only the active/progress segment becomes a wave while playback is running;
- the inactive segment always remains a straight, low-alpha track;
- the active waveform uses a fixed amplitude envelope across the current active segment: zero at the start, swelling through the middle, and decaying back to zero exactly at the thumb;
- playback animation advances only the sine phase, never the amplitude, so the wave travels without progressively "growing" over time;
- pausing freezes the current waveform shape; disabling the seek capability renders the active segment flat;
- use AALyrics Accent Cyan rather than album-art-derived color;
- keep the waveform restrained: approximately 7dp maximum amplitude, 34dp wavelength, and 4dp stroke;
- retain a compact 18dp round thumb;
- keep elapsed/duration labels below the track;
- the collapsed playback progress indicator remains the existing thin, straight, non-interactive line.

The Material3 Slider continues to own gestures/accessibility/seek semantics. Only its visual track is replaced by the custom Canvas waveform, so the existing direct-seek commit/cancel behavior is preserved.

No third-party slider dependency is required for this treatment.

## Interactive seek bar

The seek bar is interactive only when the selected session exposes seek capability and a usable duration.

Presentation:

```text
elapsed         seek position          duration
00:52   ━━━━━━━━━━━●━━━━━━━━━━━━━━     03:41
```

Direct dragging uses local preview semantics:

1. capture the starting playback position;
2. while the thumb is dragged, update the visible seek position locally and smoothly;
3. do not flood the remote MediaSession with intermediate seek commands;
4. on successful release, emit one `seekTo(previewPositionMs)`;
5. on cancellation, emit no seek and return to the live playback position.

While a seek preview is active, incoming playback-position callbacks must not overwrite the visible preview position. Live position remains tracked underneath and becomes authoritative again after commit/cancel.

All preview positions are clamped to `0..durationMs`.

## Previous / Next tap behavior

Normal taps retain the existing semantics:

```text
Previous tap -> skipToPrevious()
Next tap     -> skipToNext()
```

The tap action is independent from the long-press relative-seek gesture.

## Previous / Next long-press relative seek

AALyrics intentionally does **not** use `TransportControls.rewind()` or `fastForward()` for this gesture.

Those commands are interpreted by the remote media app and therefore do not guarantee a consistent displacement. AALyrics instead previews a deterministic relative seek and sends one `seekTo()` when the user releases.

Gesture state machine:

```text
PRESS_PENDING
    |
    | release before platform long-press threshold
    v
TAP
    -> Previous/Next skip action

PRESS_PENDING
    |
    | platform long-press threshold reached
    v
SCRUBBING
    -> one haptic confirmation
    -> suppress the tap action
    -> animate local preview position continuously

SCRUBBING
    |
    | release
    v
COMMIT
    -> one seekTo(previewPositionMs)

SCRUBBING
    |
    | gesture cancelled
    v
CANCEL
    -> no seekTo()
```

The implementation should use Android/Compose's platform long-press timing rather than inventing a second independent tap-vs-hold threshold.

### Relative-seek speed

Initial behavior:

- Previous hold moves the local preview backward;
- Next hold moves the local preview forward;
- after long-press recognition, preview displacement advances at **5 seconds of media time per 1 second of continued hold**;
- the speed is a presentation interaction constant and may be tuned after device testing without changing the MediaSession contract.

This avoids requiring a 10-second physical hold merely to move 10 seconds while remaining predictable.

During the gesture the seek line should animate smoothly. Time labels follow the preview position, and the UI may additionally expose the signed relative displacement (for example `-18s`) if this remains visually compact in Preview.

### Capability behavior

Long-press seek is available only when `seekTo` is supported and duration is usable.

Therefore a session may legitimately expose:

```text
Previous tap      enabled
Previous hold     unavailable
```

or the equivalent Next behavior.

Lack of seek capability must not disable a supported Previous/Next skip action.

## Play / Pause

Play/Pause remains the primary transport action in both collapsed and expanded states.

Presentation must follow both current playback state and advertised capability. The Phone UI emits a callback only; it never receives a framework `MediaController`.

## Queue and Open-app trailing slot

The rightmost expanded-player slot is capability-driven.

Priority:

```text
usable MediaSession queue
    -> Queue

otherwise launchable playback app/session activity
    -> Open playback app

otherwise
    -> no active trailing action
```

### Queue

Queue is optional because not every media app publishes a useful MediaSession queue.

When available, Queue opens as a compact bottom sheet rather than an alert dialog. The sheet uses the same visual language as the collapsed Playback Bar, but each queue item is a list row with the playback progress indicator and Play/Pause action removed.

The Queue sheet:

- keeps a compact `Queue` header;
- shows the same Open playback app affordance at the right edge of the header when that fallback is available;
- presents each item as a compact artwork/identity row;
- keeps title and artist to one line each with graceful overflow;
- scrolls vertically when its contents exceed the available viewport;
- keeps the sheet itself within the phone viewport rather than growing off-screen;
- selects an item through the corresponding queue-item callback.

The UI must not assume Queue availability from the app package name.

### Open playback app fallback

When Queue is unavailable, the slot opens the currently selected playback app when a safe launch target is available.

Application/platform resolution should prefer the selected session's explicit session activity when usable, then an ordinary package launch intent fallback.

The Phone UI receives only `canOpenPlaybackApp` and an `onOpenPlaybackApp` callback. It must not construct Android intents.

## Quick controls

The leftmost expanded-player slot is a compact Tune/sliders-style action.

Pressing it opens a lightweight anchored quick-controls surface rather than navigating to Settings.

Initial contents:

```text
Quick controls

Translation                     [ON]
```

The Translation toggle is a shortcut to the **same application-owned Translation enabled state** used by Settings. The playback surface must not create a second setting or maintain an independent copy.

Future quick controls may be added only when they are genuinely high-frequency playback/lyrics actions. The first implementation intentionally contains Translation only.

## Presentation capability model

The current `PlaybackControlsUiState` is too small for this surface and should evolve into a capability-aware Phone presentation model.

The exact Kotlin names may follow implementation needs, but the UI needs presentation-ready facts equivalent to:

```text
PlaybackSurfaceUiState
├─ isPlaying
├─ title
├─ artist
├─ artwork presentation handle/content
├─ positionMs
├─ durationMs
├─ progressFraction
├─ canPlay
├─ canPause
├─ canSkipPrevious
├─ canSkipNext
├─ canSeek
├─ queueAvailable / queue presentation
├─ canOpenPlaybackApp
└─ translationEnabled
```

Expanded/collapsed UI state remains Phone-shell local and is not part of the platform MediaSession snapshot.

The UI must not consume raw `PlaybackState` action bitmasks, `MediaController`, `MediaSession.Token`, `PendingIntent`, or framework queue objects.

## MediaSession capability boundary

The platform media layer already owns the selected `MediaController` and current transport route.

This feature may extend that boundary to expose normalized capabilities needed by the Phone surface, including:

- supported transport actions from `PlaybackState.actions`;
- duration and current position;
- queue availability and presentation-safe queue identity;
- selected-session launch capability;
- presentation-ready artwork source information where appropriate.

Android framework objects remain inside `:platform:media` or the application integration layer.

The existing framework-neutral `PlaybackTransport` already provides:

```text
play()
pause()
skipToPrevious()
skipToNext()
seekTo(positionMs)
```

The long-press design deliberately composes on `seekTo()`; it does not require adding `rewind()` or `fastForward()`.

Queue selection, if implemented, may require one additional framework-neutral transport command equivalent to `skipToQueueItem(id)`.

Opening the source app is application/platform navigation behavior, not a transport command.

## Position smoothing

Normal visible playback progress should continue moving smoothly between MediaSession callbacks by using the last normalized position, playback rate, playback status, and monotonic update timestamp where available.

Scrub preview temporarily overrides only the presented position. It does not mutate the underlying live playback state until the one-shot seek is committed.

The collapsed bottom progress indicator and expanded seek control should derive from the same effective-position model so they do not visibly disagree.

## Artwork

Playback-surface artwork is current-track presentation data.

Artwork loading/decoding remains outside the pure playback-surface composable. The UI accepts caller-provided/renderable artwork or a presentation-safe artwork handle and uses a neutral placeholder when unavailable.

The AALyrics brand mark must not be substituted for missing track artwork.

## Accessibility and gesture safety

Required behavior:

- all actionable controls retain at least a 48dp touch target;
- Play/Pause, Previous, Next, Quick controls, Queue/Open app, and collapse/expand affordances expose meaningful semantics;
- the seek control exposes slider/range semantics when enabled;
- long press must never also dispatch the corresponding tap action;
- long-press recognition gives haptic confirmation once;
- cancellation after long-press preview sends no remote seek;
- TalkBack users must still be able to seek through the explicit seek control, so long press is an enhancement rather than the only seek path;
- disabled capabilities are represented semantically, not only by color;
- artwork content descriptions should not redundantly repeat already-announced title/artist unless it conveys additional information.

## Animation

Animations should communicate state, not delay control.

Required direction:

- collapsed ↔ expanded transition is short and spatially continuous from the same bottom surface;
- metadata marquee retains the established initial pause before motion;
- normal playback progress advances smoothly while playing;
- long-press/direct-drag seek preview moves smoothly without emitting intermediate remote commands;
- release commits immediately and then reconciles with the next MediaSession position callback.

Reduced-motion/system animation settings should be respected through standard Compose animation behavior where practical.

## Session changes

If the selected session changes while the player is expanded:

- keep the Expanded Player open when an eligible replacement session is selected;
- replace identity/capabilities with the new selected session;
- cancel any active local seek preview before switching;
- if no eligible session remains, dismiss the playback surface entirely.

A track change within the same selected session does not collapse the player.

## Preview matrix

Deterministic Previews should cover at least:

- collapsed playing;
- collapsed paused;
- collapsed long title/artist marquee case;
- collapsed without artwork;
- expanded playing;
- expanded paused;
- expanded seekable;
- expanded non-seekable;
- active direct seek preview;
- Previous long-press backward preview;
- Next long-press forward preview;
- Queue available;
- Queue unavailable + Open-app fallback;
- neither Queue nor Open-app available;
- Translation quick controls open, enabled and disabled;
- narrow phone width;
- enlarged font;
- playback surface over Lyrics and over Settings.

## Tests

Implementation should add deterministic coverage for presentation/state logic that can be tested outside gesture rendering, including:

- tap Previous/Next remain skip actions;
- long press suppresses skip and commits exactly one seek on release;
- cancelled long press commits no seek;
- relative seek clamps to 0 and duration;
- non-seekable session never enters relative-seek mode;
- direct seek preview commits exactly one seek;
- capability mapping does not enable unsupported actions;
- Queue takes priority over Open-app fallback;
- Translation quick control emits the same application setting callback;
- Back collapses Expanded Player before normal back behavior;
- destination switching does not implicitly collapse;
- session loss clears expanded state.

## Explicitly out of scope

This slice does not redesign:

- MediaSession selection policy;
- Notification Access onboarding;
- Lyrics provider selection/scoring;
- Lyrics Track Card content beyond reusable marquee behavior;
- Android Auto playback UI;
- system notification controls;
- shuffle/repeat unless a later capability contract is explicitly approved;
- arbitrary MediaSession custom actions;
- full-screen now-playing presentation;
- volume controls.

## Implementation sequence

After documentation approval, implementation on this branch should proceed in bounded layers:

1. extend framework-neutral media capability/transport contracts only where required;
2. add application/Phone presentation mapping for playback-surface state;
3. replace the fixed `PlaybackControlsBar` with collapsed Playback Bar presentation;
4. add Expanded Player and collapse/backdrop/back handling;
5. add direct seek preview/commit;
6. add Previous/Next long-press relative-seek state machine;
7. add Queue/Open-app capability slot;
8. add Translation quick controls;
9. add Preview/test matrix;
10. run CI and bounded Codex review before merge.

No implementation step should move Android framework types into `:ui:phone`.
