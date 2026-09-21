# AALyrics UI Architecture

## Purpose

This document defines the UI module layout, source-set rules, Compose design-system ownership, Phone presentation ownership, Android Auto host-rendered presentation ownership, and Preview workflow for AALyrics.

The project uses Jetpack Compose itself as the executable design specification for Compose surfaces. Static design artifacts may inform visual decisions, but production UI code is the source of truth for behavior and appearance. Android Auto host-rendered templates are a distinct presentation technology and adapt the same semantic design intent through automotive-local builders/components.

Surface-specific product structure is documented separately when useful. The current Phone information architecture is defined in `docs/PHONE_UI_SPEC.md`, with Settings/Advanced behavior in `docs/PHONE_SETTINGS.md`, Details behavior in `docs/PHONE_DETAILS.md`, and the shell playback contract in `docs/PHONE_PLAYBACK_SURFACE.md`.

Branding authority and derivative rules are defined in `docs/BRANDING.md`; `branding/AALyrics_MASTER.svg` is the canonical AALyrics master icon.

## Modules

```text
:ui:designsystem
:ui:phone
:ui:automotive
```

### `:ui:designsystem`

Shared visual foundation used by presentation surfaces.

Owns:

- AALyrics theme and semantic color tokens
- typography
- spacing, radius, and stroke tokens
- reusable Compose visual components
- shared icons
- debug-only design-system catalogs/previews

Must not own:

- provider access
- networking
- media-session integration
- lyrics orchestration
- phone navigation or screen composition
- automotive navigation, Car App Library template lifecycle, or automotive screen composition

`ui:designsystem` intentionally has no project dependency on `core`, `provider`, `platform`, `ui:phone`, or `ui:automotive`.

### `:ui:phone`

Phone-specific shell, screen composition, navigation presentation, and presentation state.

Owns:

- persistent Phone shell composition
- compact top status bar presentation
- persistent playback-controls presentation
- primary bottom-navigation presentation
- Phone destination definitions
- Phone routes/screens
- Phone UI state mapping and UI actions/callbacks
- destination-specific components while their APIs are still Phone-local
- debug-only Phone previews and preview fixtures

It may depend on `:core:model`, `:core:lyrics`, and `:ui:designsystem`. It must not call provider implementations, networking, or `:platform:media` directly.

Phone-specific components are local-first. A component should move to `:ui:designsystem` only after real screen use demonstrates a reusable and stable API without Phone-specific runtime/navigation ownership.

### `:ui:automotive`

Automotive-specific presentation. It owns both host-rendered screen composition and the automotive-local design adapters needed to express AALyrics semantics through Android Auto models.

Owns:

- Android Auto / automotive screen composition
- automotive-local reusable presentation builders/components
- automotive-specific UI state and interactions
- automotive previews/fixtures or DHU-oriented demo support when useful

It shares the core state contracts and semantic design intent with the phone UI but does not need to mirror phone composition or Compose components one-to-one.

## Phone shell and package ownership

The Phone surface has persistent shell chrome around one selected destination:

```text
PhoneAppShell
├─ PhoneTopBar
├─ CurrentDestination
│  ├─ Lyrics
│  ├─ Sync
│  ├─ Details
│  └─ Settings
├─ PlaybackSurface
│  ├─ PlaybackBar
│  └─ ExpandedPlayer
└─ PhoneNavigationBar
```

The approved primary destinations are:

```text
Lyrics   Sync   Details   Settings
```

`Lyrics` is the home destination.

Ownership is divided by package:

```text
ui/phone/shell       persistent Phone chrome/composition
ui/phone/navigation  destination identity/navigation presentation contracts
ui/phone/lyrics      Lyrics destination and Lyrics-local components/state
ui/phone/sync        Sync destination
ui/phone/details     Details destination
ui/phone/settings    Settings destination
ui/phone/state       shell-level presentation state
```

The shell may compose the selected destination and expose presentation-ready transport callbacks, but it does not discover media sessions, own `MediaController`, perform provider lookup, or select lyrics candidates.

The richer current-track card remains Lyrics-destination content. The shell-level Playback Surface may repeat only the compact track identity/artwork needed to make playback control context clear; it must not turn persistent chrome into a second full Track Card.

PR #33 established the persistent shell boundary in production Compose. Subsequent Phone slices added Track Card, LyricsViewport/LyricsScreen, production Settings, and the capability-aware two-state Playback Surface. PR #46 completed the current shell playback contract with collapsed/expanded presentation, seek, Queue/Open-app fallback, and Translation quick controls. PR #49 implements the approved Details destination and the Settings-owned Advanced sub-surface while preserving the existing shell/application boundaries. Sync remains intentionally undefined pending timing/calibration redesign.

PR #50 realizes the application-composition boundary documented in `docs/PHONE_RUNTIME_HOST.md`: `MainActivity` keeps the existing onboarding/permission entry gates, while READY is the lifecycle-aware Compose host for `PhoneAppShell`. Host-local destination selection and Lyrics browse/Plain auto-scroll interaction remain presentation state; durable settings and media/runtime ownership stay outside `:ui:phone`.

The current Phone-local primitive standards are defined in `docs/PHONE_UI_SPEC.md`: `PhonePopupMenu` for anchored compact popup/tooltip surfaces, `SettingsSubscreenHeader` for second-level Settings navigation, and `PhoneMarkdownText` for Markdown documents such as License and future Changelog content. These remain in `:ui:phone` until cross-surface reuse justifies promotion to `:ui:designsystem`.

## Shared vs automotive design system

AALyrics has one shared semantic design language, but it is rendered through different UI technologies.

```text
Shared semantic intent
        ↓
:ui:designsystem
Compose tokens/components
        ↓ conceptual adaptation
:ui:automotive/designsystem
Android Auto host-model builders/components
        ↓
:ui:automotive/screen
Now Playing / Expanded Lyrics
```

The automotive-local `designsystem/` package exists because Android Auto host templates do not provide arbitrary Compose layout, typography, spacing, or drawing. Automotive presentation therefore adapts semantic intent instead of pretending that phone Compose components can be reused directly.

Examples that belong in `ui/automotive/designsystem/` once their APIs are demonstrated by a real screen:

- reusable lyrics-row construction
- reusable current-line emphasis/section construction
- reusable automotive headers
- mapping AALyrics semantic color intent to supported `CarColor`/host styling
- other small host-model builders reused by more than one automotive screen

Examples that do **not** belong in the automotive design-system layer:

- `Screen.onGetTemplate()`
- `SectionedItemTemplate` screen composition
- screen stack/navigation
- `invalidate()` ownership
- follow/manual-browse orchestration
- scroll-state lifecycle
- deciding which screen is currently active

Those are screen/host-contract responsibilities and belong under `ui/automotive/screen/`.

## Source-set contract

Every UI module follows the same rule:

```text
src/main/
    Production code. Included in debug and release builds.

src/debug/
    Development-only code. Included only in debug builds.
```

For Compose UI, `src/debug` is the home of Compose Preview code and deterministic preview fixtures.

This does **not** mean Preview is a separate implementation. Preview code must render the same production composables from `src/main` that the application uses at runtime.

Example:

```text
src/main/.../LyricsScreen.kt
    ↓                 ↓
Production route    Debug @Preview
real state          deterministic preview state
```

The screen/composable itself is shared. Only its input source differs.

Android Auto host-rendered templates are not required to have a Compose Preview equivalent. Their visual validation should use deterministic state/builders plus DHU/emulator/device checks. Debug-only fixtures may still live under `src/debug`.

## Production / Preview rule

For Compose surfaces, the desired relationship is:

```text
Domain/application state
        ↓
Presentation UI state
        ↓
Production Composable
       / \
      /   \
Runtime   @Preview
real data preview fixtures
```

Rules:

1. Never create a second visual implementation solely for Preview.
2. Preview fixtures must not leak into `src/main`.
3. Preview code must not perform network requests or contact providers.
4. Preview code should use deterministic data so screenshots and review are reproducible.
5. Interactive Preview may hold local temporary state to exercise interactions, but the rendered production composables remain the same.
6. Edge cases that are difficult to reproduce on a device should be first-class Preview scenarios.
7. Automotive host-template behavior must be validated in DHU/emulator/device rather than inferred from Compose Preview.

## Initial file structure

```text
ui/
├─ designsystem/
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  └─ java/io/github/whoxamxl/aalyrics/ui/designsystem/
│     │     ├─ theme/
│     │     │  ├─ AALyricsTheme.kt
│     │     │  ├─ Color.kt
│     │     │  ├─ Typography.kt
│     │     │  ├─ Dimensions.kt
│     │     │  └─ Shapes.kt
│     │     ├─ component/
│     │     │  ├─ LyricsLine.kt
│     │     │  ├─ AlbumArtwork.kt
│     │     │  └─ TrackMetadata.kt
│     │     └─ icon/
│     │        └─ AALyricsIcons.kt
│     └─ debug/
│        └─ java/io/github/whoxamxl/aalyrics/ui/designsystem/preview/
│           ├─ DesignSystemPreview.kt
│           ├─ PalettePreview.kt
│           ├─ TypographyPreview.kt
│           └─ ComponentPreviews.kt
│
├─ phone/
│  └─ src/
│     ├─ main/
│     │  └─ java/io/github/whoxamxl/aalyrics/ui/phone/
│     │     ├─ shell/
│     │     │  ├─ PhoneAppShell.kt
│     │     │  ├─ PhoneTopBar.kt
│     │     │  ├─ PlaybackSurface.kt
│     │     │  ├─ PlaybackBar.kt
│     │     │  ├─ ExpandedPlayer.kt
│     │     │  └─ PhoneNavigationBar.kt
│     │     ├─ navigation/
│     │     │  └─ PhoneDestination.kt
│     │     ├─ lyrics/
│     │     │  ├─ LyricsRoute.kt
│     │     │  ├─ LyricsScreen.kt
│     │     │  ├─ LyricsUiState.kt
│     │     │  ├─ LyricsAction.kt
│     │     │  ├─ TrackCard.kt
│     │     │  └─ LyricsViewport.kt
│     │     ├─ sync/
│     │     │  └─ SyncScreen.kt
│     │     ├─ details/
│     │     │  ├─ DetailsScreen.kt
│     │     │  └─ DetailsUiState.kt
│     │     ├─ settings/
│     │     │  ├─ AdvancedSettingsScreen.kt
│     │     │  ├─ SettingsScreen.kt
│     │     │  └─ SettingsUiState.kt
│     │     └─ state/
│     │        └─ PhoneShellUiState.kt
│     └─ debug/
│        └─ java/io/github/whoxamxl/aalyrics/ui/phone/preview/
│           ├─ LyricsScreenPreviews.kt
│           └─ PreviewLyricsData.kt
│
└─ automotive/
   └─ src/
      ├─ main/
      │  └─ java/io/github/whoxamxl/aalyrics/ui/automotive/
      │     ├─ designsystem/
      │     │  ├─ AutomotiveDesignSystem.kt
      │     │  ├─ AutomotiveHeader.kt
      │     │  ├─ LyricsRow.kt
      │     │  └─ CurrentLyricsSection.kt
      │     ├─ screen/
      │     │  ├─ NowPlayingScreen.kt
      │     │  └─ ExpandedLyricsScreen.kt
      │     └─ state/
      │        └─ AutomotiveLyricsUiState.kt
      └─ debug/
         └─ java/io/github/whoxamxl/aalyrics/ui/automotive/preview/
            └─ AutomotiveLyricsPreviews.kt
```

Placeholder files may exist before their APIs are decided. A placeholder must not invent behavior merely to make the tree look complete.

## Design-system foundation

The initial foundation is dark-first and uses the AALyrics brand palette already established by the application icon/branding work.

Foundation categories:

- primitive palette
- semantic colors
- typography
- spacing
- radius
- stroke
- Material 3 bridge values used by `AALyricsTheme`

The shared design system may use Material 3 primitives internally, but AALyrics semantic tokens are the public visual contract. Product UI should not scatter arbitrary Material defaults or raw hex values through screen code.

### Token usage

Prefer:

```kotlin
AALyricsColors.TextPrimary
AALyricsTypography.LyricsCurrent
AALyricsSpacing.Space16
AALyricsRadius.Radius12
```

Avoid introducing new one-off values in a screen when an existing token expresses the same design decision.

Raw palette values are available for design-system construction. Reusable Compose production components should normally consume semantic values. Automotive adapters should preserve the same semantic intent where the host API allows it rather than duplicating unrelated visual policy.

## Preview catalog

The design-system module contains a high-level `DesignSystemPreview` plus focused previews such as palette and typography.

The phone module should eventually cover at least these state families:

```text
Idle
Loading
Ready / line-synced
Ready / word-synced (karaoke)
Ready / unsynced
Degraded
NotFound
Failed
Manual browsing / follow paused
No artwork
Long title / artist
Long lyrics lines
First / last lyric boundary
Playback controls enabled / disabled
Narrow / typical Phone width
```

This list is a coverage target, not a requirement to implement all screen behavior in an architecture-only branch.

Phone Preview work should specifically verify that persistent top status, playback controls, and bottom navigation still leave useful vertical space for the Lyrics viewport. The product target is roughly five to six visible lyric lines on normal phone layouts where practical, not a hard line-count guarantee.

For automotive, equivalent edge cases should be exercised with deterministic presentation state and DHU/emulator/device validation, especially host-dependent row limits, wrapping, current-line emphasis, full/split layouts, and template refresh/scroll behavior.

## Screen-first workflow

Do not grow either design-system layer speculatively.

For each Compose UI slice:

1. Define the screen and its state matrix.
2. Define important interactions and edge cases.
3. Build/prove surface-local components first when ownership is still specific to that screen/surface.
4. Extract reusable visual components into `:ui:designsystem` only when the screen demonstrates a stable reusable API.
5. Add or refine design-system tokens only when required by demonstrated components.
6. Build the production composable under `src/main`.
7. Render normal and edge-case states under `src/debug` Preview.
8. Validate on emulator/device after the Preview shape is stable.

For each automotive host-template slice:

1. Define the automotive screen and state matrix.
2. Define host interactions, refresh behavior, and edge cases.
3. Compose the concrete Car App Library screen under `screen/`.
4. Extract only demonstrated reusable host-model builders into `designsystem/`.
5. Validate host rendering and interaction in DHU/emulator/device.

This keeps both design-system layers useful without turning them into abstract component inventories disconnected from the product.

## State ownership

`LyricsState` remains a shared domain/application contract in `:core:lyrics`.

Presentation modules may map domain/application state into surface-specific state when needed:

```text
Application/domain state
        ↓
AALyricsApplication / app-owned presentation mapping
        ↓
MainActivity READY host
        ├─> PhoneShellUiState ------> PhoneAppShell
        ├─> LyricsUiState ----------> LyricsScreen
        ├─> DetailsScreenUiState ---> DetailsScreen
        └─> SettingsScreenUiState --> SettingsScreen

LyricsState
        ↓
AutomotiveLyricsUiState ------> NowPlayingScreen
                         \-----> ExpandedLyricsScreen
```

The UI may decide how a state is presented. It must not decide provider ranking, request fan-out, stale-result ownership, networking behavior, or active-media-session ownership.

Phone playback controls should receive presentation state plus callbacks such as `onPrevious`, `onPlayPause`, and `onNext`; they should not receive or own a platform `MediaController`.

## Dependency rules

Allowed direction:

```text
:ui:phone ----------> :ui:designsystem
:ui:automotive -----> :ui:designsystem
:ui:phone ----------> :core:lyrics / :core:model
:ui:automotive -----> :core:lyrics / :core:model

ui/phone/shell ------> ui/phone/navigation
ui/phone/shell ------> ui/phone/state
ui/phone/shell ------> selected Phone destination composition

ui/automotive/screen -------> ui/automotive/designsystem
ui/automotive/screen -------> ui/automotive/state
```

Forbidden direction:

```text
:ui:designsystem -X-> :core:*
:ui:designsystem -X-> :provider:*
:ui:designsystem -X-> :platform:media
:ui:designsystem -X-> :ui:phone / :ui:automotive
:ui:*            -X-> concrete providers
:ui:*            -X-> direct networking
:ui:phone/auto   -X-> :platform:media

phone destination code -X-> media-session discovery/ownership
phone local component   -X-> provider selection/networking

automotive/designsystem -X-> automotive/screen lifecycle/navigation
```

These boundaries are guarded on a best-effort basis by `scripts/verify-architecture.sh` where practical.

## Compose dependency baseline

The initial shared UI foundation uses:

- Jetpack Compose
- Material 3 as the underlying theming/component toolkit
- Compose BOM `2026.06.00`
- Compose Compiler Gradle plugin aligned with the repository Kotlin version

The BOM is intentionally pinned to the stable Compose 1.11 generation while the project remains on `compileSdk = 36`. Updating Compose or compileSdk should be a deliberate dependency slice rather than an incidental UI change.

The Android Auto media direction is now decided but remains a separate implementation slice. AALyrics stays a Media app and plans to add a Car App Library templated-media path using `androidx.car.app:app:1.8.0-rc01`, with `SectionedItemTemplate` as the primary browsing structure and the existing `MediaBrowserServiceCompat` path retained for compatibility. The planned Car App Library path must be validated in DHU and on physical Android Auto before host-dependent fallback behavior becomes a durable runtime assumption. See `docs/ANDROID_AUTO_MEDIA_STRATEGY.md` for the full decision, sideload policy, and coexistence rules.

## Naming and package rules

- Shared design-system APIs use `AALyrics` prefixes where a generic name would be ambiguous (`AALyricsTheme`, `AALyricsColors`).
- Phone persistent shell code stays under `.phone.shell`.
- Phone destination identity/navigation presentation contracts stay under `.phone.navigation`.
- Lyrics-specific Phone code stays under `.phone.lyrics`; Sync, Details, and Settings stay under their matching destination packages.
- Shell-level Phone presentation state stays under `.phone.state`.
- Phone-local components remain in `:ui:phone` until demonstrated reusable APIs justify promotion to `:ui:designsystem`.
- Automotive host-screen code stays under `.automotive.screen`.
- Reusable automotive host-model adapters/builders stay under `.automotive.designsystem`.
- Automotive presentation state stays under `.automotive.state`.
- Preview-only packages end in `.preview` and live under `src/debug`.
- Provider names should not appear in reusable component APIs unless the UI is explicitly displaying provider metadata as data.
- Component APIs should accept presentation values/state, not provider DTOs or Android media framework types.

## Validation

A UI architecture change should run at minimum:

```text
./gradlew test check :app:assembleDebug
bash scripts/verify-architecture.sh
git diff --check
```

Later screen/component slices should add UI tests, model-builder tests, or screenshot tests where they provide durable regression value. Android Auto host-dependent behavior must additionally be validated in DHU/emulator/device.
