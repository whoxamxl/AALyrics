# Phone Playback Surface Specification

## Status

This document defines the approved and implemented Phone playback-surface contract that replaced the legacy fixed three-button `PlaybackControlsBar`.

- Historical implementation branch: `feature/phone-playback-surface`
- Historical base: `main` at `575d53c`
- Classification: **PHONE PLAYBACK SURFACE / MEDIA CONTROL PRESENTATION**
- State: **IMPLEMENTED — merged into `main` via PR #46**

The production shell now uses `PlaybackSurface`, `PlaybackBar`, and `ExpandedPlayer`. This document remains the behavioral contract for that implementation; later changes should update the contract and production surface together.

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

The collapsed bar replaced the legacy permanent Previous / Play-Pause / Next button row.

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
- dragging upward from that same non-Play/Pause portion expands the player with finger-following motion;
- tapping Play/Pause must not also expand the player;
- Previous and Next are removed from the collapsed state;
- preserve accessible touch targets even though the visual treatment is compact.

The bar should remain visually compact enough that Lyrics, Sync, Details, and Settings keep the majority of the vertical viewport.

## Long metadata in the Playback Bar

Title and artist are each constrained to one visible line.

The collapsed Playback Bar intentionally does **not** marquee long identity text. It is persistent shell chrome and may be visible at the same time as the richer Lyrics Track Card, so duplicating the same horizontal motion adds visual noise without adding information.

Collapsed overflow behavior:

- title stays fixed on one line and truncates with an ellipsis when needed;
- artist stays fixed on one line and truncates with an ellipsis when needed;
- title and artist overflow independently;
- no overflow measurement or marquee animation is required in the collapsed state.

The Track Card and Expanded Player retain the row-aware marquee behavior so full metadata remains discoverable in the richer surfaces.

## Interactive marquee in richer identity surfaces

The Lyrics Track Card and Expanded Player support both automatic marquee motion and direct horizontal inspection for overflowing title/artist text.

Interaction contract:

- automatic marquee retains the 4-second leading/repeat pause, 30dp/s motion, and 32dp repeat gap;
- only overflowing rows participate;
- title-only overflow -> only title auto-scrolls and only title accepts horizontal drag;
- artist-only overflow -> only artist auto-scrolls and only artist accepts horizontal drag;
- both-overflow -> title and artist share one offset for both auto motion and manual drag;
- manual drag is finger-following and bounded to one marquee cycle from the leading edge through `content width + repeat gap`;
- dragging beyond either bound clamps at that bound rather than allowing free/infinite panning;
- manual drag pauses automatic motion;
- after release, hold the manual position for 1.5 seconds, then resume automatic motion from that exact position;
- the repeated visual copy used for seamless cycling does not create duplicate accessibility semantics;
- the Collapsed Playback Bar remains fixed ellipsis and does not expose this drag interaction.

In the Expanded Player, horizontal marquee drag is intentionally local to the title/artist identity. The existing vertical header drag remains the collapse gesture. Gesture-direction arbitration must allow a primarily horizontal gesture to inspect marquee text and a primarily vertical gesture to transform/collapse the player without making seek/transport controls participants in either gesture.

## Expanding the player

Tapping any non-Play/Pause portion of the collapsed Playback Bar expands the same shell-owned surface upward.

The same region also supports an upward drag. During that drag the transformation follows the finger continuously rather than waiting for a release threshold. Releasing settles toward Expanded or Collapsed according to the current transformation position, with a sufficiently directional fling allowed to choose the corresponding destination.

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

Quick Controls defines the shared compact Phone popup visual language. Anchored explanatory tooltips elsewhere in the Phone UI reuse the same popup surface tokens rather than falling back to the default Material `DropdownMenu` appearance.

## Collapse triggers

Expanded Player collapse is explicit and predictable.

Collapse when:

- the user taps the destination/backdrop area outside the expanded surface;
- Android Back / system back gesture is invoked while expanded;
- the user performs the supported downward finger-following collapse drag on the expanded header surface;
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

## Surface transformation interaction

Collapsed and Expanded are two states of the same Playback Surface.

Transformation affordances:

- collapsed identity/artwork/background tap -> animate toward Expanded;
- collapsed identity/artwork/background upward drag -> follow the finger toward Expanded;
- expanded identity/header tap -> animate toward Collapsed;
- expanded identity/header downward drag -> follow the finger toward Collapsed;
- backdrop tap -> animate toward Collapsed;
- Android Back while expanded -> animate toward Collapsed.

Both drag directions use the same continuous transformation position. A drag updates that position directly while the pointer moves; release then settles to an anchor based on position and, for a directional fling, release velocity. An incomplete slow drag may return to the state it started from.

These transformation affordances do not use a press/ripple indication. The spatial surface motion itself is the feedback. Ordinary playback controls remain ordinary controls and retain their normal press indication.

This interaction refinement does **not** change the existing backdrop darkness, Expanded/Collapsed surface colors, or their alpha values as a flash/brightness workaround. Those visual values stay as designed unless a separate visual change is explicitly approved.

## Expanded seek visual language

The Expanded Player seek control follows the simpler iPhone media-player interaction shown in the recorded reference.

Rules:

- the resting state is a thin rounded pill with no visible standalone thumb;
- the active segment is bright and the inactive segment remains muted;
- while the user directly drags the Slider, animate the entire track thickness from about 4dp to about 12dp;
- the active/inactive boundary itself reads as the seek position while dragging, rather than exposing a separate circular thumb;
- return the track smoothly to the thin resting state after drag stop/cancel;
- keep the track shape stable across playing and paused playback; playback state does not create decorative waveform animation;
- keep elapsed/duration labels below the track;
- preserve the existing direct-seek preview / one-shot commit behavior;
- the collapsed playback progress indicator remains the existing thin, straight, non-interactive line.

The Material3 Slider continues to own gestures/accessibility/seek semantics. The implementation customizes only the visual track and hides the visual thumb, keeping the interaction model small and framework-native.

No third-party slider dependency is required.

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

Queue is optional because not every media app publishes a useful MediaSession queue. A non-empty framework queue is not sufficient by itself: Queue is actionable only when the selected session also advertises skip-to-queue-item capability.

When available, Queue opens as a compact bottom sheet rather than an alert dialog. The sheet uses the same visual language as the collapsed Playback Bar, but each queue item is a list row with the playback progress indicator and Play/Pause action removed.

The Queue sheet:

- uses the same rounded top corners in production and Preview;
- exposes a small centered drag handle without the oversized default Material handle region;
- keeps a compact `Queue` header;
- shows the same Open playback app affordance at the right edge of the header when that fallback is available;
- presents each item as a compact artwork/identity row;
- keeps title and artist to one line each with graceful overflow;
- lets the Queue list consume all remaining sheet height instead of leaving unused space below short viewport caps;
- scrolls only the rows that no longer fit inside that remaining viewport;
- overlays the bottom approximately 15% of the list viewport with a transparent-to-surface fade so clipped rows visually dissolve into the sheet edge;
- keeps enough bottom list padding for the final row to scroll above the fade and remain fully readable/selectable;
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

Artwork extraction remains outside the pure playback-surface composable. The selected-session Android boundary forwards `METADATA_KEY_ALBUM_ART`, then `METADATA_KEY_ART`, then `MediaDescription.iconBitmap` when available. The Phone UI still accepts caller-provided/renderable artwork rather than Android MediaSession objects.

When no track artwork is available, the artwork slot uses the shared AALyrics foreground mark derived from `branding/android/AALyrics_foreground_android.svg` as the branded fallback.

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
- metadata marquee retains the established initial pause before motion and resumes from a manually dragged position after the shorter manual-release pause;
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
- collapsed title-only ellipsis;
- collapsed artist-only ellipsis;
- collapsed both-overflow ellipsis;
- expanded title-only overflow marquee;
- expanded artist-only overflow marquee;
- expanded both-overflow synchronized marquee;
- manual horizontal marquee drag in title-only, artist-only, and synchronized overflow modes;
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

The debug-only interactive full-surface Preview uses the production `PlaybackSurface` composable directly. It is the authoritative Preview for interaction checks that cannot be represented meaningfully in a static frame, including tap expand/collapse, upward finger-following expand drag, downward finger-following collapse drag, release settling, direct seek, Queue, and Translation quick-control interaction.

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
- session loss clears expanded state;
- row-aware marquee mode selects static, title-only, artist-only, or synchronized behavior from per-line overflow;
- manual marquee offset clamps to one cycle in both drag directions;
- auto marquee travel duration preserves the configured constant velocity;
- transformation progress maps deterministically between Collapsed and Expanded anchors;
- slow release settles to the nearest anchor;
- sufficiently directional upward/downward fling selects the corresponding Expanded/Collapsed anchor.

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

## Historical implementation sequence

PR #46 implemented this contract in the following bounded layers:

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
