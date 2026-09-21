# AALyrics Phone UI Specification

## Status

The Phone information architecture and persistent Compose shell are established. Lyrics, Settings, and the shell-owned Playback Surface are implemented in production Compose. PR #46 replaced the legacy fixed three-button playback bar with the compact collapsed Playback Bar plus on-demand Expanded Player defined in `docs/PHONE_PLAYBACK_SURFACE.md`.

PR #49 implements the approved Details contract from `docs/PHONE_DETAILS.md` together with the narrow `Settings > Advanced` extension from `docs/PHONE_SETTINGS.md`. Details remains read-only, Verbose Details is presentation-only, and Karaoke mode remains disabled/unwired. Sync remains intentionally deferred while its timing/calibration interaction model is reconsidered.

PR #50 implements the application-composition slice defined in `docs/PHONE_RUNTIME_HOST.md`. `MainActivity` preserves the existing entry gates and now hosts the production `PhoneAppShell` for READY. Live app-owned state drives Lyrics, Playback Surface, Details, and Settings; Sync remains an explicit non-functional placeholder. Physical-device iteration on this branch also established selected-session artwork with branded fallback, Translation opt-in defaults, human-readable playback-source labeling with package fallback, in-app License presentation, and shared Phone popup/subscreen/Markdown primitives. The generated debug APK is suitable for continued physical-device Phone UI validation.

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


### System Back navigation

The Phone shell treats `Lyrics` as the home/start destination for top-level Back behavior.

System Back follows this priority:

1. An active child surface that owns Back, such as the Expanded Player or a Settings subscreen, handles it first.
2. From the top level of `Sync`, `Details`, or `Settings`, Back returns directly to `Lyrics`.
3. From the top level of `Lyrics`, the Phone shell does not consume Back, so the Activity/system default may leave the app.

Primary destinations do not form a historical Back stack. For example, navigating `Lyrics → Sync → Details → Settings` and then pressing Back returns to `Lyrics`, not to `Details`.

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

Reserved for synchronization-focused controls and status. Its final timing/calibration interaction model is intentionally not frozen. Do not infer Sync UI behavior from the presence of the destination placeholder; the project is reconsidering synchronization ownership before implementing this screen.

### Details

The approved first Details contract is defined in `docs/PHONE_DETAILS.md`.

Normal Details is read-only and user-facing, covering current track metadata plus resolved lyrics metadata such as provider display name, sync type, language, and line count.

When `Settings > Advanced > Verbose details` is enabled, Details keeps its existing Duration and Lines rows in place but expands them to live `current / total` presentation (`Duration (verbose)` and `Lines (verbose)`). The synchronized line number is one-based; PLAIN lyrics show an unavailable current-line marker rather than inventing timing. Details also exposes a `Developer / Diagnostics` section for machine-facing framework-neutral facts such as the playback app package name, provider ID, provider source ID, and normalized track references. Verbose Details changes presentation only; it must not trigger new lookups or alter provider selection, timing, Translation, playback, or rendering behavior.

Candidate scores, raw provider payloads, log export, and deeper resolver diagnostics remain deferred until separately justified.

### Settings

Owns user-facing application configuration while persistence and capability policy remain outside `:ui:phone`.

The production Settings contract is defined in `docs/PHONE_SETTINGS.md`. Its implemented settings remain deliberately focused, with the Advanced extension limited to Verbose Details plus a disabled future Karaoke affordance:

- Plain lyrics auto-scroll;
- Translation enabled/disabled;
- Translation target language;
- Android Auto compatibility acknowledgement/status and setup re-entry;
- installed version plus explicit unavailable Update presentation until release-network runtime exists;
- explicit unavailable Changelog presentation until release-note runtime exists;
- external Source code entry;
- in-app License entry backed by the repository-root `LICENSE`;
- permanent AALyrics branding/GitHub footer;
- an `Advanced` entry containing:
  - functional `Verbose details` presentation preference;
  - disabled/unwired `Karaoke mode` future affordance;
  - `Storage > Clear translation models`, which keeps built-in English, turns Translation off, and restores English as the target;
  - `Reset > Reset AALyrics`, which resets AALyrics-owned settings/onboarding without deleting translation models or changing Android/system settings.

Provider preferences, appearance/theme selection, log export, functional Karaoke wiring, and other future taxonomy remain deferred until separately approved.

## Persistent top status bar

The top bar is compact and persistent across primary destinations.

Purpose:

- left: AALyrics app identity/icon
- right: the media app/session source currently monitored by AALyrics

Examples include `Spotify`, `YouTube Music`, or `Poweramp`. The top bar does not show lyrics format, provider/sync status, track metadata, or playback state; those belong to destination content, the Lyrics Track Card, or playback controls.

The application/runtime boundary resolves the selected playback package to a human-readable application label where possible. If label resolution fails, the package identifier (for example `com.spotify.music`) is the final presentation fallback rather than hiding the source. The raw package remains explicitly available in Verbose Details regardless of label resolution.

The UI receives the resolved media-source label as presentation data only; media-session discovery, package-label resolution, and source selection remain outside `:ui:phone`. The persistent visual treatment uses the shared AALyrics brand mark on the left and a compact outlined source pill with a cyan dot on the right.

The shell chrome uses the dedicated `BackgroundChrome` tone. The visual transition from the top bar into destination content is owned by `PhoneAppShell`, not `PhoneTopBar`: a 24dp multi-stop navy tonal fade is shifted 5dp upward and drawn behind destination content. This preserves the Lyrics Track Card's existing 16dp top inset without covering its rounded top edge. `PhoneTopBar` therefore remains a flat chrome component in isolation; the full transition is validated in shell Preview/device rendering.

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

The production Track Card keeps album artwork caller-owned in a compact 64dp slot. The READY runtime host forwards artwork from the selected Android MediaSession without moving Android media objects into `:ui:phone`. When artwork is unavailable, the shared AALyrics foreground mark derived from `branding/android/AALyrics_foreground_android.svg` is shown over the existing artwork background instead of leaving the slot visually empty.

Long title/artist text in the Lyrics Track Card uses row-aware overflow marquee behavior. If only the title overflows, only the title moves and the artist remains fixed; if only the artist overflows, only the artist moves and the title remains fixed. When both title and artist overflow, they retain one synchronized offset so their leading edges stay aligned and both lines move at the same speed. Marquee motion pauses for 4 seconds at the leading position, scrolls at a constant speed, keeps a 32dp repeat gap, then returns seamlessly to the leading position and repeats. Overflowing marquee content can also be dragged horizontally by hand within exactly one marquee cycle: the leading position is the start bound and the repeated-copy start after the text width + gap is the end bound. Manual drag pauses auto motion; release holds the chosen position briefly for 250ms, then auto motion resumes from that position at the same velocity. Non-overflowing text remains static and is not draggable. When both lines overflow, manual drag moves title and artist together with the same shared offset. Provider/sync metadata stays fixed and truncates rather than joining the marquee. The Expanded Player reuses this richer auto + manual marquee behavior, while the persistent Collapsed Playback Bar stays motionless and represents long title/artist text with one-line ellipsis.

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

The navigation bar distinguishes first selection from reselection. Tapping a different primary tab switches destinations. Tapping the already-selected tab emits a destination-reselection event whose contract is to return that destination to its root presentation rather than creating history or performing a no-op. Each destination uses a stable semantic icon plus label, with cyan emphasis for the selected destination. Playback actions belong to the Playback Bar and current-track information belongs to destination content.

Current reselection behavior:

- Lyrics: return the lyrics viewport from manual browse to FOLLOW;
- Sync: no visible change while the destination remains a root-only placeholder;
- Details: return the read-only Details surface to its top scroll position;
- Settings: dismiss modal/draft UI, discard uncommitted Target-language selection, leave Advanced/License, return to Settings home, and restore the main Settings scroll position to the top.

Future child surfaces under any primary destination must use the same root-reset contract instead of defining ad hoc tab-reselection behavior.

Its background uses the same `BackgroundChrome` tone as the top bar, but intentionally remains a flat surface with no mirrored tonal fade. This keeps the lower hierarchy quiet beside the Playback Surface and selected-tab cyan indicator.

The current production tab row is 54dp tall. Each destination keeps 4dp horizontal padding, 4dp top padding, and 2dp bottom padding around the 24dp icon / label / 2dp selected indicator stack. `navigationBarsPadding()` preserves the platform navigation/gesture inset outside the app-owned tab-row height, so the effective bottom chrome on a physical device includes the system inset without making the app-owned row itself oversized.

`PhoneNavigationBar` is the final child of the shell `Column`, while the destination/Playback Surface region above it owns the remaining height through `weight(1f)`. Therefore reducing the Navigation Bar height automatically gives that space back to the destination region: the collapsed Playback Surface remains bottom-aligned inside that region, and `LyricsViewport` receives the larger available height without any compensating playback offset or bottom-overlay-inset change.

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

### Adopted Phone UI primitives

Three current Phone patterns are now normative within `:ui:phone`:

- **Anchored popup / tooltip surface** — use `PhonePopupMenu`. Its current Quick Controls-derived visual treatment is the standard: Radius16, `BackgroundSurfaceStrong`, `BorderSoft`, zero tonal elevation, and the shared shadow elevation. Do not introduce a default-styled `DropdownMenu` for an equivalent compact popup.
- **Second-level Settings header** — use `SettingsSubscreenHeader`. Its standard back affordance is a Material rounded chevron-left at 32dp inside a 48dp touch target, paired with the subscreen title. This mirrors the chevron-right navigation affordance used when entering a Settings subscreen.
- **Markdown documents** — use the Phone-local `PhoneMarkdownText` wrapper for bundled or presentation-provided Markdown such as the repository `LICENSE` and future GitHub Release changelogs. The wrapper delegates Markdown parsing/rendering to `mikepenz/multiplatform-markdown-renderer` Material 3 rather than implementing Markdown syntax in AALyrics. Keep the original document as the source of truth; rendering is presentation-only.

These are Phone-local standards. They should remain in `:ui:phone` until reuse outside the Phone surface justifies promotion to `:ui:designsystem`.

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
│  ├─ DetailsScreen.kt
│  └─ DetailsUiState.kt
├─ settings/
│  ├─ AdvancedSettingsScreen.kt
│  ├─ LicenseSettingsScreen.kt
│  ├─ SettingsScreen.kt
│  └─ SettingsUiState.kt
└─ state/
   └─ PhoneShellUiState.kt
```

The persistent shell, navigation identity, and shell-level state files contain production Compose behavior. PR #46 completed the `PlaybackSurface` replacement of the legacy `PlaybackControlsBar`. `LyricsScreen` is the production Lyrics destination composition, and Settings follows `docs/PHONE_SETTINGS.md`.

PR #49 replaces the Details placeholder with the approved read-only production surface and adds the Settings-owned Advanced sub-surface. `SyncScreen.kt` remains a placeholder and its interaction model remains deferred; do not invent Sync behavior merely to complete the tree.

## Preview and validation direction

Production shell composables live in `src/main`, while deterministic shell Preview fixtures live in `src/debug`. PR #33 validates the real production shell at typical and narrow widths, across all selected destinations, with playing/paused and disabled/unavailable transport states.

Preview coverage should eventually exercise at least:

- active media / no media
- Track Card / Expanded Player title-only overflow, artist-only overflow, and both-overflow synchronized auto marquee + manual horizontal drag
- Collapsed Playback Bar title/artist ellipsis without marquee
- no artwork
- loading / ready / degraded / not found / failed lyrics states
- line-synced / word-synced / unsynced lyrics
- long lyric lines
- first/last-line boundaries
- follow vs manual browse
- collapsed/expanded playback-surface states and capability combinations
- Details normal and Verbose modes
- Advanced Settings with Verbose details and disabled Karaoke mode
- in-app License subscreen with long scrollable bundled license text
- narrow and typical phone widths
- the shell-owned top-chrome tonal transition and Track Card boundary/spacing

Implementation should validate that the persistent top bar, playback controls, and bottom navigation still leave adequate room for the lyrics viewport. Standalone `PhoneTopBar` Preview intentionally shows only the flat component; `PhoneAppShell` Preview is authoritative for the top-chrome fade because the transition is shell-owned. `PhoneNavigationBarPreviews` uses the production Navigation Bar directly and also includes a simple gesture-navigation-context mock for visual proportion only; physical-device rendering remains authoritative for the actual system navigation inset.

## Explicitly deferred

The persistent shell implementation intentionally still does not decide or implement:

- exact dp heights or typography sizes for shell elements not explicitly locked by the tuned production values above
- final icons or animation
- a general navigation framework beyond host-local primary destination selection
- speculative ViewModel layers; the approved runtime-host slice may add minimal application-owned presentation mappers
- media-session ownership
- provider behavior
- final Sync interaction model
- Details capabilities beyond the approved `PHONE_DETAILS` contract
- functional Karaoke mode or Karaoke runtime wiring
- Settings features outside the approved Settings + Advanced taxonomy
- Settings application/runtime wiring
- Sync destination implementation or its destination-specific Compose API
