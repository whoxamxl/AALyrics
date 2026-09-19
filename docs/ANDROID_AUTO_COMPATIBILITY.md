# Android Auto Compatibility Setup

## Purpose

This document defines the user-facing setup behavior for the legacy Android Auto compatibility path.

The broader product and architecture decision is documented in `docs/ANDROID_AUTO_MEDIA_STRATEGY.md`.

AALyrics remains a Media app and plans to add a Car App Library templated-media path using `androidx.car.app:app:1.8.0-rc01` and `SectionedItemTemplate`, while keeping the existing `MediaBrowserServiceCompat` path as a compatibility fallback.

## Why this setup exists

The two Android Auto presentation paths have different sideload behavior:

```text
Car App Library templated media
    -> Android Auto "Unknown sources" is not required

Legacy MediaBrowserService media fallback
    -> sideloaded builds may require Android Auto Developer Mode
       and Developer settings > Unknown sources
```

Android's current testing guidance documents this distinction: the Android Auto `Unknown sources` developer option applies to media, messaging-notification, and parked apps, but not to apps built with the Android for Cars App Library.

Because AALyrics is distributed outside Google Play, the legacy fallback needs a clear setup path even though the planned templated path does not require this Android Auto developer option.

## Do not confuse with Android APK installation

Android Auto `Unknown sources` is not the same setting as Android's per-installer **Install unknown apps** permission.

```text
Install unknown apps
    -> permits installation of the AALyrics APK on the phone

Android Auto > Developer settings > Unknown sources
    -> permits the applicable sideloaded legacy Android Auto media path
```

The compatibility onboarding in this document concerns only the second setting.

## Onboarding order

The phone entry flow is intentionally ordered:

```text
Notification Access missing
        -> required blocking setup

Notification Access granted
        -> Android Auto compatibility not reviewed
              -> compatibility setup
                 -> "I've enabled Unknown sources"
                    OR
                    "Continue without it"
              -> normal app content

Compatibility already reviewed
        -> normal app content
```

Notification Access remains the only blocking system-access gate.

## Onboarding behavior

The compatibility setup is advisory rather than blocking.

Two explicit user actions are provided:

- `I've enabled Unknown sources` -> persist `ENABLED`
- `Continue without it` -> persist `SKIPPED`

Both states count as reviewed, so the compatibility onboarding is not shown repeatedly.

The persisted value is an acknowledgement of the user's choice. It is not a verified copy of Android Auto state.

## Displayed setup instructions

The compatibility screen explains the documented Android Auto flow:

1. Open Android Auto settings.
2. Open `Version`, then tap `Version and permission info` 10 times to enable Developer Mode.
3. Open `Developer settings` from the Android Auto menu.
4. Enable `Unknown sources`.

The screen also explains that this setup is for the legacy sideloaded media fallback rather than the future Car App Library templated path.

## Verification boundary

AALyrics does not attempt to read, infer, or spoof the Android Auto `Unknown sources` developer setting.

Android Auto does not expose a public application API for that state, so:

- the UI explicitly says automatic verification is unavailable;
- AALyrics records only the user's acknowledgement;
- `ENABLED` means the user said they enabled it;
- `SKIPPED` means the user chose to continue without configuring the fallback.

If Android later exposes a supported API, verification may be reconsidered in a dedicated change.

## Ownership

`:app` owns:

- acknowledgement state;
- persistence;
- application-entry ordering.

`:ui:phone` owns:

- the Compose compatibility setup screen;
- setup copy;
- typical / narrow / enlarged-font Previews.

The compatibility UI does not change:

- MediaBrowserService behavior;
- Car App Library behavior;
- MediaSession behavior;
- providers;
- lyrics lookup;
- Android Auto projection state.

## Current implementation scope

The onboarding slice does not:

- enable Android Auto Developer Mode;
- change Android Auto settings;
- change the existing `MediaBrowserServiceCompat` runtime;
- add or configure `CarAppService`;
- upgrade the repository to Car App Library 1.8.x;
- implement `SectionedItemTemplate`;
- change Android Auto projection behavior.

Those templated-media changes belong to a separate implementation slice defined by `docs/ANDROID_AUTO_MEDIA_STRATEGY.md`.

## Follow-up

A later Settings surface should expose the recorded compatibility status and let the user reopen the setup instructions.

When the Car App Library templated-media path is implemented, this onboarding remains useful because the legacy MediaBrowser fallback is intentionally retained.
