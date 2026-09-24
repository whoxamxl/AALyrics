# AALyrics Phone UI Specification

## Status

The Phone information architecture and persistent Compose shell are established. Lyrics, Settings, and the shell-owned Playback Surface are implemented in production Compose. PR #46 replaced the legacy fixed three-button playback bar with the compact collapsed Playback Bar plus on-demand Expanded Player defined in `docs/PHONE_PLAYBACK_SURFACE.md`.

PR #49 implements the approved Details contract from `docs/PHONE_DETAILS.md` together with the narrow `Settings > Advanced` extension from `docs/PHONE_SETTINGS.md`. Details remains read-only, Verbose Details is presentation-only, and Karaoke mode remains disabled/unwired. Sync remains intentionally deferred while its timing/calibration interaction model is reconsidered.

PR #50 implements the application-composition slice defined in `docs/PHONE_RUNTIME_HOST.md`. `MainActivity` preserves the existing entry gates and now hosts the production `PhoneAppShell` for READY. Live app-owned state drives Lyrics, Playback Surface, Details, and Settings; Sync remains an explicit non-functional placeholder. Physical-device iteration on this branch also established selected-session artwork with branded fallback, Translation opt-in defaults, human-readable playback-source labeling with package fallback, in-app License presentation, and shared Phone popup/subscreen/Markdown primitives. The playback-source metadata contract now extends that application-owned package resolution to the selected app icon for the persistent Top Bar and Android application category plus min/target SDK levels for Verbose Details diagnostics. The generated debug APK is suitable for continued physical-device Phone UI validation.

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

The Track Card also reflects the current lyrics lifecycle in its metadata/status row: `Loading...` with a compact progress indicator while lookup is active, provider + sync type when lyrics are ready/degraded, `Lyrics not found` for a completed miss, and `Lyrics lookup failed` for a failed lookup. These states are mapped from the matching `LyricsState` for the current playback identity so stale lookup state is not presented for a new track.

Artwork remains caller-owned. `LyricsScreen` forwards viewport interaction-mode changes but does not own media, provider, navigation, or playback-controller objects.

Because the Playback Bar is a floating shell overlay, `PhoneAppShell` also exposes its required bottom overlay inset to destination content. `LyricsScreen` applies that inset only to the flexible LyricsViewport region, keeping the Track Card unchanged while preventing the viewport return-to-playback control and bottom lyric content from sitting under the transport surface. When playback controls are absent, the inset is zero.

Before provider lookup starts, the application layer evaluates the selected source against the persisted playback-source eligibility settings. The default policy accepts apps identified as Audio, rejects known non-audio categories, and rejects unclassified/unresolved apps unless the Advanced override is enabled. A rejected source remains observed as a MediaSession but does not start lyrics-provider lookup and is presented as `Unavailable`.

### Sync

Reserved for synchronization-focused controls and status. Its final timing/calibration interaction model is intentionally not frozen. Do not infer Sync UI behavior from the presence of the destination placeholder; the project is reconsidering synchronization ownership before implementing this screen.

### Details

The approved first Details contract is defined in `docs/PHONE_DETAILS.md`.

Normal Details is read-only and user-facing, covering current track metadata plus resolved lyrics metadata such as provider display name, sync type, language, and line count.

When `Settings > Advanced > Verbose details` is enabled, Details keeps its existing Duration and Lines rows in place but expands them to live `current / total` presentation (`Duration (verbose)` and `Lines (verbose)`). The synchronized line number is one-based; PLAIN lyrics show an unavailable current-line marker rather than inventing timing. Details also exposes a `Developer / Diagnostics` section for machine-facing framework-neutral facts such as the playback app package name, Android application category, min/target SDK levels, provider ID, provider source ID, and normalized track references. Verbose Details changes presentation only; it must not trigger new lookups or alter provider selection, timing, Translation, playback, or rendering behavior.

Candidate scores, raw provider payloads, log export, and deeper resolver diagnostics remain deferred until separately justified.

### Settings

Owns user-facing application configuration while persistence and capability policy remain outside `:ui:phone`.

The production Settings contract is defined in `docs/PHONE_SETTINGS.md`. The `feature/settings-about-support` implementation reorganizes its lower information architecture without changing capability ownership:

- Plain lyrics auto-scroll;
- Ignore non-audio apps (default ON);
- Translation enabled/disabled;
- Translation target language;
- Android Auto compatibility acknowledgement/status and setup re-entry;
- `APP`:
  - durable `Automatically check for updates` switch (default ON) with shared info tooltip;
  - #75 discovery presentation: installed version plus MANUAL Check/Retry, Checking, Up to date, and Check failed;
  - #75 keeps the inherited post-Update download/install process presentation in Settings until #77, including an install-refresh-only `Newer update available -> Download` retarget state when a newer release appears during install preparation;
  - one shared release-available dialog for MANUAL and AUTOMATIC newer-release discovery;
  - PR #76 / `feature/one-step-update` remains legacy/reference only and is not the active implementation baseline;
  - #77 dialog-owned update-process presentation for preparing/download progress, verification, explicit `Ready to install` / Install, install preparation, permission-required handling, typed failure/Retry, install-refresh retargeting, and PackageInstaller handoff;
  - current #77 checkpoint: `PREPARING_DOWNLOAD`, `DOWNLOADING`, dedicated `VerifyingDownload`, `DOWNLOADED / Ready to install`, `DOWNLOAD_FAILED / Retry`, `PREPARING_INSTALL`, `PERMISSION_REQUIRED`, `INSTALLING`, and typed `INSTALL_FAILED / Retry` are dialog-owned; Ready to install and install Retry use the existing `installUpdate()`, download Retry uses the existing `downloadUpdate()`, and Android source-trust / final installer confirmation remain system-owned. Install-refresh retarget is the remaining process-presentation migration;
  - #77 preserves the validated two-stage Update -> Download/Verify -> DOWNLOADED/Ready to install -> Install interaction while moving that process out of Settings;
  - #78 may later remove the second user-facing Install action by orchestrating automatic continuation across the same validated runtime stages; `DOWNLOADED` remains the verified-artifact/recovery boundary;
  - #74 recovery behavior preserved underneath the unified presentation, plus one-time `AALyrics updated` feedback after durable replacement reconciliation;
  - in-app Changelog backed by repository-root `CHANGELOG.md`;
  - external Source code entry;
- `ABOUT & SUPPORT`:
  - in-app Privacy Policy backed by repository-root `PRIVACY.md`;
  - in-app License backed by repository-root `NOTICE` + `LICENSE`;
  - `Support AALyrics` native subscreen with external Buy Me a Coffee handoff only;
- standalone `Advanced` card containing:
  - `Playback source > Allow unclassified apps` escape hatch (default OFF);
  - functional `Verbose details` presentation preference;
  - disabled/unwired `Karaoke mode` future affordance;
  - `Storage > Clear translation models`, which keeps built-in English, turns Translation off, and restores English as the target;
  - `Reset > Reset AALyrics`, which resets AALyrics-owned settings/onboarding/update state, clears app-owned update recovery/cadence state and retained update artifacts, without deleting translation models or changing Android/system settings such as install-source trust;
- permanent AALyrics branding/GitHub footer after Advanced.

The Phone update surfaces follow the shared dialog-header contract introduced by #74. In #75, the shared release-available dialog and post-replacement update-success dialog use `PhoneDialogHeader` for the same trailing X position, 24dp icon, and 48dp touch target. #77 extends unified dialog ownership across the update-process phases and removes the parallel Settings progress/install surface. In #77, `DOWNLOADED / Ready to install` remains an explicit dialog phase with an Install action; #78 changes only the normal continuation UX and does not erase the internal verified-download boundary.

### Version presentation

Phone UI uses the shared `VersionChip` whenever an AALyrics application or release version is presented as a semantic UI value rather than as prose/document content. The chip owns the leading `v` normalization, so callers pass either `0.2.0-alpha.1` or `v0.2.0-alpha.1` without duplicating prefix logic.

The approved channel treatment is intentionally restrained: a low-emphasis tinted pill surface, subtle border, monospace label text, and one channel accent.

```text
DEV     -> neutral gray
ALPHA   -> soft red / Error
BETA    -> AccentCyan
RC      -> AccentBlue
STABLE  -> Success
```

The deterministic `VersionChip` Preview matrix is the visual baseline for these colors and geometry. Version presentation should reuse this component rather than recreating inline `v...` text, channel colors, pill shapes, or prefix normalization per screen.

Apply `VersionChip` to standalone semantic application/release versions in Settings and update surfaces. When the version is grammatically part of supporting copy, keep the chip in the same wrapping phrase rather than creating a separate metadata row. Do not use it for Android SDK numbers, package versions embedded only in diagnostic prose, arbitrary numbers, GitHub/CHANGELOG Markdown document content, or explanatory sentences where the version is not a distinct UI value. The unified update presentation changes only ownership of UI; release discovery, SHA-256 verification, retained-artifact ownership, install-time refresh, package/version/signing preflight, source trust, PackageInstaller, and Android confirmation remain application/platform-owned boundaries.

The approved support flow does not embed checkout, handle payment credentials/state, or unlock app functionality. Browser/Custom-Tab launching remains application-owned. Provider preferences, appearance/theme selection, log export, functional Karaoke wiring, and other future taxonomy remain deferred until separately approved.

## Persistent top status bar

The top bar is compact and persistent across primary destinations.

Purpose:

- left: AALyrics app identity/icon
- right: the media app/session source currently monitored by AALyrics

Examples include `Spotify`, `YouTube Music`, or `Poweramp`. The top bar does not show lyrics format, provider/sync status, track metadata, or playback state; those belong to destination content, the Lyrics Track Card, or playback controls.

The application/runtime boundary resolves playback-source packages through an application-owned `PlaybackSourceAppInfoResolver`. One resolved metadata record supplies the human-readable application label, application icon, Android application category, minimum SDK level, and target SDK level for that package. Connected presentation resolves the selected playback package; Unavailable presentation prefers the package retained by its runtime state so a policy-rejected player can still show real app identity. If a known package's label lookup fails, the package identifier (for example `com.spotify.music`) remains the final label fallback rather than hiding the source. If no package/app identity is available at all, Unavailable still renders its generic metadata-independent fallback. The raw selected-playback package remains explicitly available in Verbose Details when present; Android `CATEGORY_UNDEFINED` is represented as an explicit undefined diagnostic category rather than guessed from app behavior.

The Phone UI receives presentation-ready source information only; media-session discovery, package/application lookup, `ApplicationInfo`, `PackageManager`, Android `Drawable` ownership, and source selection remain outside `:ui:phone`. The persistent visual treatment uses the shared AALyrics brand mark on the left and a compact outlined source pill on the right. The pill presents the MediaSession observation runtime explicitly as `Connecting`, `Connected`, `Disconnected`, `Unavailable`, or `Error`. `Connected` requires the runtime-selected package, current playback package, and resolved app-info package to agree and shows the selected app icon when available, with the cyan dot as icon fallback. If the selected source has a real application launch capability, the entire Connected pill is a button and ends with the same external-link icon used by Settings; tapping it uses the existing session-activity-first, package-launcher-fallback behavior. `Disconnected` means observation is healthy but no active session is available. `Unavailable` means a session exists but AALyrics cannot use that source under the current lyrics eligibility policy. Reasons are `NON_AUDIO_APP`, `UNCLASSIFIED_APP`, and `UNKNOWN`. `NON_AUDIO_APP` means Android supplied a known non-Audio category while Ignore non-audio apps is enabled. `UNCLASSIFIED_APP` covers `CATEGORY_UNDEFINED`, unknown/future categories normalized to Undefined, and unresolved application metadata while the strict filter is enabled and the Advanced override is OFF. Its tooltip directs the user to `Settings > Advanced > Allow unclassified apps`. When application identity is available the pill may show `<App> · Unavailable ⓘ`; when no label/icon can be resolved, the complete fallback is `[blocked icon] Unavailable ⓘ` and does not depend on app metadata. `UNKNOWN` remains a generic fallback and does not suggest a possibly irrelevant override. `Error` preserves a concrete runtime reason and exposes it through a compact information tooltip.

The pill uses one semantic color vocabulary without changing its underlying shape language or spacing tokens. Connecting keeps the existing neutral chrome treatment; Connected uses the shared `Success` token; Disconnected uses the tertiary/disabled neutral; Unavailable uses `Warning`; and Error uses the shared `Error` token. Status text/icons use the semantic foreground directly, while the existing `OverlaySoft` background and `BorderSoft` outline receive only restrained blends of that color. App identity text stays primary and the cyan dot remains the no-icon source fallback, so state color does not replace source identity. Runtime-state pills remain content-sized with the shared 8dp horizontal / 4dp vertical padding and a 220dp maximum width rather than forcing every state into one fixed envelope. Unavailable/Error info affordances stay inside a compact 16dp slot so adding a tooltip does not inflate pill height. Unavailable resolves app identity from the runtime state's package when one is known, so a policy-rejected session can still show the real application label/icon rather than a generic placeholder. The application category display remains diagnostic, but the underlying category metadata may also be used by the explicit lyrics eligibility policy. That policy gates only whether lyrics lookup starts; it does not alter MediaSession selection/transport or provider ordering/scoring. Min/target SDK levels remain diagnostic-only and must not affect eligibility, compatibility gating, or feature availability.

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
- Settings: dismiss modal/draft UI, discard uncommitted Target-language selection, leave Advanced/Changelog/License, return to Settings home, and restore the main Settings scroll position to the top.

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

Four current Phone patterns are now normative within `:ui:phone`:

- **Anchored popup / tooltip surface** — use `PhonePopupMenu`. Its current Quick Controls-derived visual treatment is the standard: Radius16, `BackgroundSurfaceStrong`, `BorderSoft`, zero tonal elevation, and the shared shadow elevation. Do not introduce a default-styled `DropdownMenu` for an equivalent compact popup.
- **Second-level Settings header** — use `SettingsSubscreenHeader`. Its standard back affordance is a Material rounded chevron-left at 32dp inside a 48dp touch target, paired with the subscreen title. This mirrors the chevron-right navigation affordance used when entering a Settings subscreen.
- **Dismissible custom-dialog header** — use `PhoneDialogHeader` whenever a Phone custom dialog requires an explicit X close affordance. The eyebrow and close action share one full-width header row and are vertically centered. The X is always a 24dp close icon inside the standard 48dp touch target at the trailing edge; dialog implementations must not add per-dialog offsets or alternate close-icon sizing. Supporting metadata such as a target version belongs below the header rather than inside the close-action row. Action-oriented Material `AlertDialog` surfaces that already provide explicit Confirm/Cancel or Done/Cancel controls do not gain an X merely for visual consistency.
- **Semantic application/release version** — use the Phone-local `VersionChip` for every user-visible AALyrics application or release version. When the version is standalone metadata, render the chip as its own value. When the version is grammatically part of supporting copy, keep the same chip but compose it inline with the surrounding phrase using a wrapping layout; do not create an extra standalone version row merely for visual consistency.
- **Markdown documents** — use the Phone-local `PhoneMarkdownText` wrapper for bundled or presentation-provided Markdown such as repository `NOTICE`/`LICENSE`, `CHANGELOG.md`, and `PRIVACY.md`. The wrapper delegates Markdown parsing/rendering to `mikepenz/multiplatform-markdown-renderer` Material 3 rather than implementing Markdown syntax in AALyrics. Keep the original document as the source of truth; rendering is presentation-only.

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
│  ├─ ChangelogSettingsScreen.kt
│  ├─ LicenseSettingsScreen.kt
│  ├─ PrivacyPolicySettingsScreen.kt
│  ├─ SupportAALyricsSettingsScreen.kt
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
- playback-source app icon present / icon unavailable fallback in the persistent Top Bar
- Connecting / Connected / Disconnected / Unavailable / Error playback-source states
- Unavailable status with app-identity and generic-fallback forms plus concise reason tooltip
- Error status with concise reason tooltip
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
- in-app Changelog subscreen with long scrollable bundled release history
- in-app Privacy Policy subscreen with long scrollable bundled policy text
- in-app License subscreen with long scrollable bundled legal text
- native Support AALyrics subscreen with external-link presentation
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
