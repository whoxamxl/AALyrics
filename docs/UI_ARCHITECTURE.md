# AALyrics UI Architecture

## Purpose

This document defines the UI module layout, source-set rules, Compose design-system ownership, Android Auto host-rendered presentation ownership, and Preview workflow for AALyrics.

The project uses Jetpack Compose itself as the executable design specification for Compose surfaces. Static design artifacts may inform visual decisions, but production UI code is the source of truth for behavior and appearance. Android Auto host-rendered templates are a distinct presentation technology and adapt the same semantic design intent through automotive-local builders/components.

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

Phone-specific screen composition and presentation state.

Owns:

- phone routes/screens
- phone UI state mapping
- phone-specific user actions
- settings/navigation that are specific to the phone surface
- debug-only phone previews and preview fixtures

It may depend on `:core:model`, `:core:lyrics`, and `:ui:designsystem`. It must not call provider implementations or `:platform:media` directly.

### `:ui:automotive`

Automotive-specific presentation. It owns both host-rendered screen composition and the automotive-local design adapters needed to express AALyrics semantics through Android Auto models.

Owns:

- Android Auto / automotive screen composition
- automotive-local reusable presentation builders/components
- automotive-specific UI state and interactions
- automotive previews/fixtures or DHU-oriented demo support when useful

It shares the core state contracts and semantic design intent with the phone UI but does not need to mirror phone composition or Compose components one-to-one.

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
│     │     ├─ LyricsRoute.kt
│     │     ├─ LyricsScreen.kt
│     │     ├─ LyricsUiState.kt
│     │     └─ LyricsAction.kt
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
```

This list is a coverage target, not a requirement to implement all screen behavior in the foundation branch.

For automotive, the equivalent edge cases should be exercised with deterministic presentation state and DHU/emulator/device validation, especially host-dependent row limits, wrapping, current-line emphasis, full/split layouts, and template refresh/scroll behavior.

## Screen-first workflow

Do not grow either design-system layer speculatively.

For each Compose UI slice:

1. Define the screen and its state matrix.
2. Define important interactions and edge cases.
3. Extract reusable visual components that the screen actually needs.
4. Add or refine design-system tokens only when required by those components.
5. Build the production composable under `src/main`.
6. Render normal and edge-case states under `src/debug` Preview.
7. Validate on emulator/device after the Preview shape is stable.

For each automotive host-template slice:

1. Define the automotive screen and state matrix.
2. Define host interactions, refresh behavior, and edge cases.
3. Compose the concrete Car App Library screen under `screen/`.
4. Extract only demonstrated reusable host-model builders into `designsystem/`.
5. Validate host rendering and interaction in DHU/emulator/device.

This keeps both design-system layers useful without turning them into abstract component inventories disconnected from the product.

## State ownership

`LyricsState` remains a shared domain/application contract in `:core:lyrics`.

Presentation modules may map it into surface-specific state when needed:

```text
LyricsState
   ↓
Phone LyricsUiState ----------> LyricsScreen
   ↓
AutomotiveLyricsUiState ------> NowPlayingScreen
                         \-----> ExpandedLyricsScreen
```

The UI may decide how a state is presented. It must not decide provider ranking, request fan-out, stale-result ownership, or networking behavior.

## Dependency rules

Allowed direction:

```text
:ui:phone ----------> :ui:designsystem
:ui:automotive -----> :ui:designsystem
:ui:phone ----------> :core:lyrics / :core:model
:ui:automotive -----> :core:lyrics / :core:model

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

Android Auto host-template dependencies are intentionally not selected by this architecture-only slice. The planned `SectionedItemTemplate` experiment should introduce its Car App Library version deliberately in its own implementation slice and validate the actual DHU host behavior before that dependency becomes part of durable production assumptions.

## Naming and package rules

- Shared design-system APIs use `AALyrics` prefixes where a generic name would be ambiguous (`AALyricsTheme`, `AALyricsColors`).
- Phone screen-specific code stays in the phone surface module.
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
