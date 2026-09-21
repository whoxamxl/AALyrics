# Changelog

This file is the canonical user-facing release history for AALyrics. Update it before creating a release tag so the tagged APK can bundle the same release history it was built from.

## [0.2.0-alpha.1] - 2026-09-21

### Added

- Production Phone UI hosted directly in the application with live playback, lyrics, artwork, Settings, Details, and Translation state.
- Persistent collapsed and expanded Playback Surface with transport controls, direct seek, relative seek, queue handling, and playback-app fallback.
- Production Settings and Details surfaces, including Advanced / Verbose Details.
- In-app Markdown-rendered License.
- Notification Access and Android Auto compatibility onboarding.

### Changed

- Refined responsive lyrics follow/browse behavior and Phone layout across narrow and normal device widths.
- Refined track identity overflow with row-aware marquee behavior, manual inspection, and fixed ellipsis in the collapsed Playback Bar.
- Refined Playback Surface expansion/collapse into one finger-following transformation model.

### Known limitations

- Sync timing / calibration workflow is still in development.
- Karaoke mode is not yet available.
- In-app update and Changelog runtime are not yet available in this release.

## [0.1.0-alpha.1] - 2026-09-19

### Added

- Initial AALyrics application, core lyrics domain, provider-selection architecture, and race-safe lyrics lifecycle.
- LRCLIB, PetitLyrics, Musixmatch, and SyncLRC provider integrations.
- Live MediaSession playback runtime and Android Auto Now Playing support.
- Initial Compose Phone shell, Track Card, design system, and responsive UI foundation.
- Signed GitHub Release distribution with APK and SHA-256 checksum assets.

### Development status

- This is the first signed alpha snapshot and establishes the foundation for later Phone UI, Settings, Translation, and playback work.
