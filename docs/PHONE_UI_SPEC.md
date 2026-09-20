# AALyrics Phone UI Specification

## Status

The Phone information architecture and persistent Compose shell are established. The Lyrics destination has a production composition boundary built from `TrackCard` and `LyricsViewport`. The first production Settings destination is implemented from the contract in `docs/PHONE_SETTINGS.md`. The next shell-control contract is defined in `docs/PHONE_PLAYBACK_SURFACE.md`: the current fixed three-button playback bar will evolve into a compact collapsed Playback Bar plus an on-demand Expanded Player. Sync, Details, application/runtime wiring, and final visual tuning remain deliberately staged.

## Product intent

The Phone surface should keep lyrics as the primary content while making app state, playback controls, and destination switching consistently reachable with one hand.

The approved shell is:

```text
┌──────────────────────────────┐
│ Top Status Bar               │  persistent
├──────────────────────────────┤
│                              │
│ Current Destination          │
│                              │
├──────────────────────────────┤
│ Playback Bar        │  persistent when media is available
├──────────────────────────────┤
│ Lyrics  Sync  Details  Settings │ persistent
└──────────────────────────────┘
```

The shell owns persistent chrome. Destination-specific content owns its own screen composition.

## Primary destinations

The primary navigation has four destinations:

```text
Lyrics   Sync   Details   Settings
```

`Lyrics` is the home destination.

### Lyrics

Primary now-playing lyrics experience.

Owns:

- Track Card
- Lyrics viewport
- current-line emphasis
- follow/manual-browse presentation
- lyrics-specific empty/loading/error presentation

The Track Card is destination-specific rather than shell-level so non-Lyrics destinations do not duplicate large track metadata.

The production `LyricsScreen` composes the Track Card above a flexible `LyricsViewport`. The Track Card uses compact 16dp destination-side/top insets; the viewport keeps its own internal 20dp lyric inset rather than inheriting another screen-level horizontal inset. A 12dp gap separates the card from the viewport, and the viewport receives the remaining destination height through flexible weight.

Artwork remains caller-owned. `LyricsScreen` forwards viewport interaction-mode changes but does not own media, provider, navigation, or playback-controller objects.

Because the Playback Bar is a floating shell overlay, `PhoneAppShell` also exposes its required bottom overlay inset to destination content. `LyricsScreen` applies that inset only to the flexible LyricsViewport region, keeping the Track Card unchanged while preventing the viewport return-to-playback control and bottom lyric content from sitting under the transport surface. When playback controls are absent, the inset is zero.

### Sync

Reserved for synchronization-focused controls and status. Likely responsibilities include timing mode/status and manual timing correction/calibration, but the exact interaction model is not frozen by this architecture slice.

### Details

Reserved for current track/lyrics metadata and diagnostics that are useful to a user without turning the main Lyrics screen into a dense status panel. Candidate information includes provider, lyrics format, lookup/match information, and related current-result details. Exact fields are deferred.

### Settings

Owns user-facing application configuration while persistence and capability policy remain outside `:ui:phone`.

The first production Settings contract is defined in `docs/PHONE_SETTINGS.md` and intentionally includes only already-established product settings:

- Plain lyrics auto-scroll;
- Translation enabled/disabled;
- Translation target language;
- Android Auto compatibility acknowledgement/status and setup re-entry;
- app update entry and installed version;
- GitHub Release changelog;
- source-code and license entries;
- permanent AALyrics branding/GitHub footer.

Provider preferences, appearance/theme selection, diagnostics, and other future taxonomy remain deferred until separately approved.

## Persistent top status bar

The top bar is compact and persistent across primary destinations.

Purpose:

- left: AALyrics app identity/icon
- right: the media app/session source currently monitored by AALyrics

Examples include `Spotify`, `YouTube Music`, or `Poweramp`. The top bar does not show lyrics format, provider/sync status, track metadata, or playback state; those belong to destination content, the Lyrics Track Card, or playback controls.

The UI receives the media-source label as presentation data only; media-session discovery and source selection remain outside `:ui:phone`. The persistent visual treatment uses the shared AALyrics brand mark on the left and a compact outlined source pill with a cyan dot on the right.

## Lyrics Track Card

The richer current-track presentation belongs inside the Lyrics destination.

Intended content:

- artwork
- title
- artist
- compact lyrics/provider/sync metadata

Conceptually:

```text
┌──────────────────────────────┐
│ [Artwork]  Track title       │
│            Artist            │
│            Provider • Sync   │
└──────────────────────────────┘
```

The card is informational. Playback transport actions stay in the persistent Playback Bar so information and actions have separate, predictable locations.

The production Track Card keeps album artwork caller-owned in a compact 64dp slot. When artwork is unavailable, the card shows a neutral placeholder rather than substituting the AALyrics brand mark. Artwork loading/decoding policy remains outside the component.

Long title/artist text uses a synchronized horizontal marquee only when the combined identity block overflows. Title and artist move together, pause for 4 seconds at the leading position, scroll at a constant speed, keep a small repeat gap, then return to the leading position and repeat. Short text remains static. Provider/sync metadata stays fixed and truncates rather than joining the marquee.

## Lyrics viewport

The Lyrics viewport is the visual priority of the Lyrics destination.

The detailed responsive scrolling, sync-mode, edge-fade, manual-browse, return-control, and PLAIN auto-scroll contract is defined in `docs/PHONE_LYRICS_VIEWPORT.md`.

Goals:

- current lyric remains visually dominant
- previous and next lines provide context
- normal phone layouts should preserve roughly five to six visible lyric lines where practical
- fixed shell elements should remain compact enough not to consume the majority of vertical space

This is a layout target, not a hard line-count guarantee. Exact typography, spacing, and dp values must be tuned in Compose Preview and device testing rather than frozen in this architecture document.

## Persistent playback surface

The approved shell playback contract is defined in detail in `docs/PHONE_PLAYBACK_SURFACE.md`.

The shell owns one two-state playback surface:

```text
Collapsed Playback Bar
        ⇅
Expanded Player
```

The collapsed state remains continuously reachable immediately above bottom navigation and stays compact:

```text
[art]  title / artist                         Play-Pause
──────────────── thin playback progress ────────────────
```

Play/Pause is the only direct transport action in the collapsed state. Tapping the remaining bar surface expands the player. The existing thin bottom progress indicator remains non-interactive.

The Expanded Player grows upward from the same shell surface without creating a destination or forcing the destination body to re-layout to the expanded height. It adds:

- compact artwork + one-line title/artist using Track Card-style delayed marquee behavior;
- elapsed time + interactive seek control + duration;
- Quick controls / Previous / Play-Pause / Next / Queue-or-Open-app;
- Previous/Next long-press relative seek implemented as local preview followed by one `seekTo()` on release;
- explicit collapse via backdrop tap, Back, downward gesture, header tap, or terminal session loss.

Ordinary transport, seek, track changes, Queue use, Translation quick-toggle, and primary-destination switching do not implicitly collapse the player.

Queue, seek, skip, Play/Pause, and source-app actions are capability-driven. The Phone UI receives normalized presentation facts and callbacks; it does not inspect Android MediaSession objects or action bitmasks.

When no eligible/controllable selected media session remains, the playback surface is removed rather than leaving stale controls visible.

## Bottom navigation

The bottom navigation is persistent and optimized for one-handed reachability.

Destinations:

```text
Lyrics   Sync   Details   Settings
```

The navigation bar performs destination switching only. Each destination uses a stable semantic icon plus label, with cyan emphasis for the selected destination. Playback actions belong to the Playback Bar and current-track information belongs to destination content.

## Phone shell ownership

The intended ownership model is:

```text
PhoneAppShell
├─ PhoneTopBar
├─ CurrentDestination
│  ├─ Lyrics
│  │  ├─ TrackCard
│  │  └─ LyricsViewport
│  ├─ Sync
│  ├─ Details
│  └─ Settings
├─ PlaybackSurface
│  ├─ PlaybackBar
│  └─ ExpandedPlayer
└─ PhoneNavigationBar
```

The shell controls composition of persistent chrome and the selected destination. It should not own provider lookup, media-session discovery, or lyrics selection policy.

## Presentation state boundary

The Phone UI receives presentation state and emits actions/callbacks. It does not directly manipulate Android media framework objects or provider implementations.

Conceptually:

```text
Application/domain state
        ↓
Phone presentation mapping
        ↓
Phone shell + destination state
        ↓
Production composables
```

Playback-surface UI should depend on presentation-ready capabilities/state plus callbacks such as:

```text
onPrevious
onPlayPause
onNext
onSeekTo
onQueueItemSelected
onOpenPlaybackApp
onTranslationEnabledChanged
```

rather than a `MediaController`, raw `PlaybackState.actions`, framework queue objects, or Android launch intents.

Likewise, lyrics screens consume presentation-ready values/state rather than provider DTOs or networking clients.

## Local-first component extraction

Phone-specific components should begin inside `:ui:phone` while their behavior and API are still being proven.

Examples:

- `PhoneTopBar`
- `PlaybackSurface` / collapsed `PlaybackBar` / `ExpandedPlayer`
- `PhoneNavigationBar`
- `TrackCard`
- `LyricsViewport`

A component should move to `:ui:designsystem` only when it is genuinely reusable, has a stable presentation API, and does not pull Phone-specific navigation/runtime ownership into the shared module.

This follows the project rule: screen needs demonstrate reusable design-system APIs; the component library should not be grown speculatively.

## Intended source structure

```text
ui/phone/src/main/java/io/github/whoxamxl/aalyrics/ui/phone/
├─ shell/
│  ├─ PhoneAppShell.kt
│  ├─ PhoneTopBar.kt
│  ├─ PlaybackSurface.kt
│  ├─ PlaybackBar.kt
│  ├─ ExpandedPlayer.kt
│  └─ PhoneNavigationBar.kt
├─ navigation/
│  └─ PhoneDestination.kt
├─ lyrics/
│  ├─ LyricsRoute.kt
│  ├─ LyricsScreen.kt
│  ├─ LyricsUiState.kt
│  ├─ LyricsAction.kt
│  ├─ TrackCard.kt
│  └─ LyricsViewport.kt
├─ sync/
│  └─ SyncScreen.kt
├─ details/
│  └─ DetailsScreen.kt
├─ settings/
│  ├─ SettingsScreen.kt
│  └─ SettingsUiState.kt
└─ state/
   └─ PhoneShellUiState.kt
```

The persistent shell, navigation identity, and shell-level state files contain production Compose behavior. The current `PlaybackControlsBar` is the implementation being superseded by the approved `PHONE_PLAYBACK_SURFACE` contract. `LyricsScreen` is the production Lyrics destination composition. The Settings destination follows the separately approved `docs/PHONE_SETTINGS.md` contract. Sync and Details may remain placeholders until their own approved implementation slices; do not invent destination behavior merely to complete the tree.

## Preview and validation direction

Production shell composables live in `src/main`, while deterministic shell Preview fixtures live in `src/debug`. PR #33 validates the real production shell at typical and narrow widths, across all selected destinations, with playing/paused and disabled/unavailable transport states.

Preview coverage should eventually exercise at least:

- active media / no media
- long title and artist
- no artwork
- loading / ready / degraded / not found / failed lyrics states
- line-synced / word-synced / unsynced lyrics
- long lyric lines
- first/last-line boundaries
- follow vs manual browse
- collapsed/expanded playback-surface states and capability combinations
- narrow and typical phone widths

Implementation should validate that the persistent top bar, playback controls, and bottom navigation still leave adequate room for the lyrics viewport.

## Explicitly deferred

The persistent shell implementation intentionally still does not decide or implement:

- exact dp heights or typography sizes for shell elements
- final icons or animation
- navigation framework/runtime
- ViewModels or state-mapper classes
- media-session ownership
- provider behavior
- final Sync interaction model
- final Details fields
- Settings features outside the approved first-slice taxonomy
- Settings application/runtime wiring
- Sync and Details destination implementation or their destination-specific Compose APIs
