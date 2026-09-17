# AALyrics UI Architecture

## Purpose

This document defines the UI module layout, source-set rules, Compose design-system ownership, and Preview workflow for AALyrics.

The project uses Jetpack Compose itself as the executable design specification. Static design artifacts may inform visual decisions, but production Compose code is the source of truth for UI behavior and appearance.

## Modules

```text
:ui:designsystem
:ui:phone
:ui:automotive
```

### `:ui:designsystem`

Shared visual foundation used by all presentation surfaces.

Owns:

- AALyrics theme and semantic color tokens
- typography
- spacing, radius, and stroke tokens
- reusable visual components
- shared icons
- debug-only design-system catalogs/previews

Must not own:

- provider access
- networking
- media-session integration
- lyrics orchestration
- phone navigation or screen composition
- automotive navigation or screen composition

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

Automotive-specific screen composition and presentation state.

Owns:

- Android Auto / automotive screen composition
- automotive-specific UI state and interactions
- automotive previews and fixtures

It shares the design system and core state contracts with the phone UI but does not need to mirror phone composition one-to-one.

## Source-set contract

Every UI module follows the same rule:

```text
src/main/
    Production code. Included in debug and release builds.

src/debug/
    Development-only code. Included only in debug builds.
```

For AALyrics, `src/debug` is the home of Compose Preview code and deterministic preview fixtures.

This does **not** mean Preview is a separate implementation. Preview code must render the same production composables from `src/main` that the application uses at runtime.

Example:

```text
src/main/.../LyricsScreen.kt
    ↓                 ↓
Production route    Debug @Preview
real state          deterministic preview state
```

The screen/composable itself is shared. Only its input source differs.

## Production / Preview rule

The desired relationship is:

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
      │     ├─ AutomotiveLyricsScreen.kt
      │     └─ AutomotiveLyricsUiState.kt
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

The design system may use Material 3 primitives internally, but AALyrics semantic tokens are the public visual contract. Product UI should not scatter arbitrary Material defaults or raw hex values through screen code.

### Token usage

Prefer:

```kotlin
AALyricsColors.TextPrimary
AALyricsTypography.LyricsCurrent
AALyricsSpacing.Space16
AALyricsRadius.Radius12
```

Avoid introducing new one-off values in a screen when an existing token expresses the same design decision.

Raw palette values are available for design-system construction. Reusable production components should normally consume semantic values.

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

## Screen-first workflow

Do not grow the design system speculatively.

For each UI slice:

1. Define the screen and its state matrix.
2. Define important interactions and edge cases.
3. Extract reusable visual components that the screen actually needs.
4. Add or refine design-system tokens only when required by those components.
5. Build the production composable under `src/main`.
6. Render normal and edge-case states under `src/debug` Preview.
7. Validate on emulator/device after the Preview shape is stable.

This keeps the design system useful without turning it into an abstract component inventory disconnected from the product.

## State ownership

`LyricsState` remains a shared domain/application contract in `:core:lyrics`.

Presentation modules may map it into surface-specific state when needed:

```text
LyricsState
   ↓
Phone LyricsUiState --------> LyricsScreen
   ↓
Automotive UI state --------> AutomotiveLyricsScreen
```

The UI may decide how a state is presented. It must not decide provider ranking, request fan-out, stale-result ownership, or networking behavior.

## Dependency rules

Allowed direction:

```text
:ui:phone ----------> :ui:designsystem
:ui:automotive -----> :ui:designsystem
:ui:phone ----------> :core:lyrics / :core:model
:ui:automotive -----> :core:lyrics / :core:model
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
```

These boundaries are guarded on a best-effort basis by `scripts/verify-architecture.sh`.

## Compose dependency baseline

The initial UI foundation uses:

- Jetpack Compose
- Material 3 as the underlying theming/component toolkit
- Compose BOM `2026.06.00`
- Compose Compiler Gradle plugin aligned with the repository Kotlin version

The BOM is intentionally pinned to the stable Compose 1.11 generation while the project remains on `compileSdk = 36`. Updating Compose or compileSdk should be a deliberate dependency slice rather than an incidental UI change.

## Naming and package rules

- Shared design-system APIs use `AALyrics` prefixes where a generic name would be ambiguous (`AALyricsTheme`, `AALyricsColors`).
- Screen-specific code stays in its surface module.
- Preview-only packages end in `.preview` and live under `src/debug`.
- Provider names should not appear in reusable component APIs unless the UI is explicitly displaying provider metadata as data.
- Component APIs should accept presentation values/state, not provider DTOs or Android media framework types.

## Validation

A UI foundation change should run at minimum:

```text
./gradlew test check :app:assembleDebug
bash scripts/verify-architecture.sh
git diff --check
```

Later screen/component slices should add UI tests or screenshot tests where they provide durable regression value.
