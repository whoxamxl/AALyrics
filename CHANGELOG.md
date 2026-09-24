## [0.2.0-alpha.2] - 2026-09-24

### Added

- Complete in-app update flow with manual and automatic update discovery, signed APK download, SHA-256 verification, Android install-source permission guidance, PackageInstaller handoff, retry/recovery, and post-update success feedback.
- In-app Changelog, Privacy Policy, Terms of Use, Help & Feedback, and Support surfaces under Settings.
- Playback source identity, status, app metadata, eligibility handling, and non-audio/unclassified source controls.
- Storage/reset controls for app-owned state and downloaded Translation models.
- Shared semantic VersionChip presentation for installed and available AALyrics versions.

### Changed

- Refined the Phone lyrics viewport, current-line focus motion, vertical positioning, and metadata marquee behavior.
- Refined Playback Surface behavior, queue artwork handling, playback-app routing, and media-session selection.
- Unified the complete post-Update process in a dedicated update dialog, with normal updates automatically continuing from verified download into the existing install stage.
- Improved Settings navigation, Back behavior, nested-screen return behavior, and update-process re-entry.
- Strengthened update recovery across process recreation, installer-session interruption, package replacement, and retained verified APK reuse.
- Development builds now derive their version label from the latest reachable release tag plus the current Git commit.

### Fixed

- Fixed Phone system Back paths that could exit the app before returning through the main Lyrics surface.
- Fixed stale update/install presentation and recovery paths discovered during device validation and review.
- Fixed playback-source and retained-lyrics regressions encountered during Phone UI refinement.

### Known limitations

- Translation is still under active development; the end-to-end translated-lyrics presentation is not yet complete.
- Sync timing / calibration workflow is still in development.
- Karaoke mode remains unavailable / experimental-only.
- Android Auto does not yet have full Phone UI feature parity; further Now Playing / split-view work remains planned.

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
