# Android Auto Media Strategy

## Status

- Decision date: 2026-09-19.
- Product classification: **Media app**.
- Current production path: legacy `MediaBrowserServiceCompat` + `MediaSessionCompat`.
- Planned primary Android Auto presentation: **Car App Library templated media**.
- Planned Car App Library baseline: `androidx.car.app:app:1.8.0-rc01`.
- Planned primary browsing template: `SectionedItemTemplate`.
- Compatibility policy: keep the existing legacy media path as a fallback instead of replacing it outright.
- Distribution policy: AALyrics is distributed outside Google Play; phone installation is therefore sideloaded.

This document records the durable product/architecture decisions. Concrete Car App Library implementation remains a separate authorized slice.

## Core decision

AALyrics remains a **Media app** regardless of which Android Auto presentation path is active.

```text
AALyrics Media App
│
├─ shared playback / lyrics state
├─ MediaSession
│
├─ planned primary Android Auto presentation
│   └─ Car App Library
│       └─ MEDIA category
│           ├─ SectionedItemTemplate
│           └─ MediaPlaybackTemplate where appropriate
│
└─ compatibility presentation
    └─ MediaBrowserServiceCompat
        └─ legacy Android Auto media UI
```

Using Car App Library does not change AALyrics into a non-media product category. It adds a templated Media-app presentation path.

## Why Car App Library 1.8.0-rc01

AALyrics plans to move the templated Android Auto presentation to `androidx.car.app:app:1.8.0-rc01`.

Reasons:

- Car App Library 1.8 adds `SectionedItemTemplate`.
- 1.8 adds/enhances media playback support.
- `1.8.0-rc01` has the same feature set as `1.8.0-beta01` and includes a security fix.
- `SectionedItemTemplate` provides the sectioned list/grid structure needed for a richer AALyrics automotive experience than the legacy host-generated MediaBrowser UI.

The repository may still contain an older Car App Library dependency until the dedicated implementation slice changes it. Documentation of this decision does not authorize an incidental dependency upgrade in unrelated work.

## Planned templated-media requirements

The dedicated Car App Library implementation slice should introduce and validate the Android for Cars templated-media contract, including:

- a `CarAppService`;
- `androidx.car.app.category.MEDIA`;
- the `androidx.car.app.MEDIA_TEMPLATES` permission where required by the selected media templates;
- Car App API level 8 as the minimum for the planned media playback template path;
- Android Auto metadata with both:
  - `<uses name="media" />`
  - `<uses name="template" />`;
- `SectionedItemTemplate` for the planned browse/presentation experience;
- `MediaPlaybackTemplate` where it matches the playback surface;
- DHU and physical-device validation of the real host behavior.

These are planned implementation requirements, not claims that the current branch already provides them.

## MediaSession and MediaBrowserService remain

Car App Library templated media does **not** mean removing the normal Android media architecture.

The planned architecture retains:

- a `MediaSession` for playback state and controls;
- a `MediaBrowserService` or `MediaLibraryService` for recommendations and other smart experiences;
- the existing AALyrics normalized playback/lyrics state boundaries.

The current `LyricsBrowserService : MediaBrowserServiceCompat` therefore remains useful and is not considered obsolete merely because a `CarAppService` is added.

## Compatibility / fallback policy

AALyrics intentionally keeps the legacy MediaBrowser path alongside the future templated path.

Reasons:

1. host support for the newer templated media experience is not universal;
2. templated media on Android Auto is still documented as an Early Access Program capability;
3. the legacy path is already implemented and provides a known compatibility route;
4. keeping both paths reduces the risk that adopting the richer template experience makes AALyrics unavailable on an otherwise usable host.

For a single-package architecture, the intended shape is:

```text
one AALyrics APK
│
├─ Car App Library templated media path
└─ MediaBrowserService compatibility path
```

Android's official templated-media guidance explicitly supports coexistence of Car App Library and `MediaBrowserService` / `MediaLibraryService` implementations. The exact Android Auto host-selection and fallback behavior must still be validated with DHU and real devices before AALyrics treats any automatic fallback assumption as a guaranteed runtime contract.

## Sideloading: two different settings

AALyrics is not planned for Google Play distribution, so the phone APK itself is installed by sideloading.

Do not conflate these two settings:

```text
Android "Install unknown apps"
    -> allows the chosen installer/source to install the AALyrics APK on the phone

Android Auto Developer settings > Unknown sources
    -> allows a sideloaded legacy Media app to run through the applicable Android Auto path
```

They solve different problems.

## Android Auto Unknown sources policy

Android Auto's `Unknown sources` developer option applies to sideloaded media apps, but Android's current testing guidance explicitly states that it does **not** apply to apps built using the Android for Cars App Library.

Therefore:

```text
Car App Library templated media path
    -> Android Auto Unknown sources NOT required

Legacy MediaBrowserService compatibility path
    -> sideloaded build may require:
       Android Auto Developer Mode
       + Developer settings
       + Unknown sources
```

This is why `Unknown sources` is a compatibility concern, not a global AALyrics permission requirement.

## Phone onboarding policy

AALyrics has two different onboarding classes and they must remain distinct.

### 1. Notification Access — blocking

Notification Access is required for AALyrics to observe other apps' active media sessions.

Behavior:

```text
missing
    -> block normal app entry
    -> open Android system access settings
    -> re-check the real system state on return

granted
    -> continue
```

The app verifies the actual system state. Merely pressing the setup button is not sufficient.

### 2. Android Auto compatibility — advisory

Android Auto `Unknown sources` exists only for the legacy sideload compatibility path.

Behavior:

```text
not reviewed
    -> show compatibility instructions
       -> "I've enabled Unknown sources" -> ENABLED
       OR
       -> "Continue without it"          -> SKIPPED

ENABLED / SKIPPED
    -> continue without repeatedly showing onboarding
```

AALyrics does not claim to verify the Android Auto setting because there is no public application API for reading that state. The stored value records only the user's explicit acknowledgement.

A future Settings screen should let the user revisit these instructions and the recorded compatibility status.

## Setup order

The intended phone entry flow is:

```text
Launch
  |
  v
Notification Access?
  |
  +-- no --> blocking Notification Access setup
  |
  +-- yes
        |
        v
Android Auto compatibility reviewed?
        |
        +-- no --> advisory Unknown sources instructions
        |           ├─ enabled acknowledgement
        |           └─ skip acknowledgement
        |
        +-- yes
              |
              v
        normal phone UI
```

Android Auto compatibility guidance must never weaken or bypass the required Notification Access gate.

## UI ownership

- `:app` owns:
  - system-access checks;
  - application-entry policy;
  - compatibility acknowledgement persistence;
  - navigation to Android system settings.
- `:ui:phone` owns:
  - Notification Access setup presentation;
  - Android Auto compatibility instruction presentation;
  - deterministic Compose Previews.
- `:ui:automotive` owns:
  - Android Auto host-rendered presentation;
  - future Car App Library screen/template composition;
  - automotive presentation state/adapters.

The phone setup UI must not own Android Auto runtime behavior, provider behavior, or MediaSession discovery.

## Current vs planned state

### Current

```text
MediaBrowserServiceCompat
MediaSessionCompat
automotive_app_desc.xml -> <uses name="media" />
legacy Android Auto media presentation
```

### Planned separate implementation

```text
androidx.car.app 1.8.0-rc01
CarAppService
MEDIA category
MEDIA_TEMPLATES
Car App API >= 8 where required
automotive_app_desc.xml:
    <uses name="media" />
    <uses name="template" />
SectionedItemTemplate
MediaPlaybackTemplate
legacy MediaBrowser path retained
```

The planned slice must preserve the existing working media/runtime behavior while adding the templated path.

## Validation requirements for the future templated-media slice

At minimum:

- repository unit tests and architecture checks;
- debug APK build;
- DHU validation;
- physical Android Auto validation;
- host behavior with Car App Library templates available;
- legacy MediaBrowser path validation;
- sideload behavior with and without Android Auto `Unknown sources`;
- confirmation that Notification Access setup remains a phone-side requirement;
- no regression in MediaSession transport/state behavior.

## References

Authoritative Android documentation used for this decision:

- Android Developers — **Build a templated media app**  
  https://developer.android.com/training/cars/apps/media
- Android Developers — **Test Android apps for cars**  
  https://developer.android.com/training/cars/testing
- AndroidX release notes — **Car App 1.8.0-rc01**  
  https://developer.android.com/jetpack/androidx/releases/car-app
- Android for Cars overview / supported media categories  
  https://developer.android.com/training/cars

When implementation begins, re-check these sources because Car App Library templated media support, host coverage, and distribution status are evolving.
