# Phone Shell Runtime Host

## Branch and baseline

- Branch: `feature/phone-shell-runtime-host`.
- Base: `main` at `105ef4a8511d2fc085cef215d5ecb1eb438a8d03` (PR #49 merged).
- Classification: **PHONE RUNTIME HOST / DEVICE-TEST ENABLEMENT**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/PHONE_UI_SPEC.md`, `docs/UI_ARCHITECTURE.md`, plus the existing destination specifications.
- Documentation alignment was completed first on this branch. Production implementation was then explicitly authorized and is now in progress through PR #50.

## Goal

Replace the current READY-state foundation placeholder with the real production Phone Compose surface so a debug APK can be exercised on a physical device:

```text
MainActivity entry gate
├─ Notification Access required -> existing setup View
├─ Android Auto compatibility   -> existing setup View
└─ READY
    -> AALyricsTheme
       -> PhoneAppShell
          ├─ Lyrics
          ├─ Sync placeholder
          ├─ Details
          └─ Settings
```

This is an application-composition slice. It must connect already-approved Phone presentation contracts without moving Android framework, provider, persistence, or capability ownership into `:ui:phone`.

## Implementation acceptance criteria

### Entry / host ownership
- [x] Start from current `main` on a topic branch.
- [x] Align runtime-host documentation before production changes.
- [x] Preserve Notification Access and Android Auto compatibility entry gating.
- [x] Change only the READY path from the foundation `TextView` to the production Compose host.
- [x] Keep `Lyrics` as the default/home destination.
- [x] Keep primary destination selection host-owned and saveable across normal Activity recreation where practical.

### Phone shell
- [x] Host the production `PhoneAppShell` under `AALyricsTheme`.
- [x] Supply shell state from application-owned presentation/runtime state.
- [x] Keep Top Bar, Playback Surface, and Bottom Navigation shell-owned.
- [x] Wire playback actions through the application/platform boundary rather than importing media framework objects into `:ui:phone`.
- [x] Keep playback-app launching application-owned.
- [x] Keep Translation toggle ownership outside `:ui:phone`.

### Destinations
- [x] Render the production `LyricsScreen` from presentation-ready runtime state.
- [x] Preserve current Lyrics follow/browse ownership rules and do not redesign lyric timing.
- [x] Render a deliberate non-functional Sync placeholder without inventing calibration behavior.
- [x] Render the production `DetailsScreen` from `AALyricsApplication.phoneDetailsState`.
- [x] Render the production `SettingsScreen` from application-owned presentation state and callbacks.
- [x] Wire `Verbose details` to its existing application-owned persisted preference.
- [x] Keep Karaoke mode disabled/unwired.
- [x] Do not expose provider DTOs, `MediaController`, raw `PlaybackState`, Android intents, or persistence objects to Phone composables.

### Settings/runtime scope
- [x] Wire existing implemented capabilities where application/runtime support already exists.
- [x] Add only the minimal application-owned presentation mapping/state required to render the approved Settings contract on-device.
- [x] Do not fabricate update/changelog/model-management behavior merely to make controls appear active.
- [x] Controls whose backing runtime is not yet implemented must be presented honestly (disabled/unavailable or otherwise non-misleading) until separately implemented.
- [x] Re-entering Android Auto compatibility setup must reuse the existing entry/onboarding ownership rather than duplicate it inside `:ui:phone`.

### Device-test readiness
- [ ] Debug APK launches the real Phone shell after onboarding prerequisites are satisfied.
- [ ] Bottom navigation can exercise Lyrics / Sync / Details / Settings on a physical device.
- [ ] Playback Surface can be exercised against a selected live MediaSession when available.
- [ ] Details updates from current playback/lyrics state.
- [ ] Advanced > Verbose details can be toggled and reflected in Details.
- [ ] Existing onboarding remains reachable/functional after host migration.

### Validation
- [x] Add focused state/host tests where durable.
- [x] Run architecture checks, unit tests, and debug APK build.
- [x] Perform bounded Codex review.
- [ ] Stop before merge until explicit user approval.

## Expected implementation shape

Keep commits small and single-purpose. Expected sequence after this documentation checkpoint:

1. host/application presentation-state contracts;
2. READY-path Compose host;
3. Lyrics runtime mapping/route wiring;
4. Settings runtime mapping/callback wiring;
5. Sync placeholder presentation;
6. focused host/runtime tests;
7. documentation/status cleanup.

The exact split may change when the existing runtime seams make a smaller coherent commit preferable.

## Scope guard

Do not implement in this slice:

- Sync timing/calibration algorithms or controls;
- functional Karaoke mode;
- WORD-level Karaoke highlighting activation;
- provider ordering/preferences or provider migration work;
- new lyrics matching/scoring behavior;
- new Translation algorithms;
- persistent Translation Cache;
- new GitHub update/download capability unless already present behind an application-owned runtime seam;
- log viewer/export;
- theme/appearance settings;
- MediaSession selection-policy changes;
- Android Auto browsing/template redesign;
- unrelated UI redesign.

## Current runtime result

PR #50 now hosts the production Phone shell from `MainActivity` READY and produces a green debug APK build.

Implemented runtime behavior:

- READY -> `AALyricsTheme` -> `PhoneAppShell`;
- lifecycle-aware collection of app-owned playback, lyrics, Details, Translation, model, and Verbose Details state;
- host-local saveable primary destination and Plain auto-scroll state;
- live Lyrics mapping with monotonic playback projection;
- WORD-capable source lyrics remain line-oriented while Karaoke is unavailable;
- Playback Surface commands route through the existing application/platform media boundary;
- selected MediaSession artwork is forwarded to the Track Card and Playback Surface with an AALyrics branded fallback when unavailable;
- Translation target/model actions use the existing Translation runtime;
- Translation remains opt-in by default while English stays the built-in default target;
- Update/Changelog remain explicitly unavailable rather than active no-ops;
- Settings can re-enter the existing Android Auto compatibility setup;
- Sync is an explicit non-functional placeholder.

The code/build portion of device-test enablement is complete. The unchecked device-test items below require an actual physical-device smoke test rather than CI inference.

## Remaining stop gate

Do not merge PR #50 until:

1. both bounded Codex review rounds are complete and all current-scope findings are resolved;
2. the final CI head is green;
3. the user explicitly authorizes merge.

Physical-device smoke testing is the only remaining validation that cannot be completed from the repository/CI environment. The generated debug APK is ready for that manual check. Code-side implementation, architecture checks, unit tests, debug APK build, artifact upload, and bounded Codex review are complete.


## Final repository-side status

Latest reviewed implementation head before this status-only commit: `de720f3d889c2b641d463a6d7b26e807e7830dbd`.

Repository-side work is complete:

- production Phone shell is hosted from READY;
- architecture boundary check passes;
- debug APK build passes;
- unit tests pass;
- sideloadable debug APK artifact is produced;
- first bounded Codex review found no major issues;
- second bounded Codex review raised two current-scope P2 findings (platform ActionBar and Lyrics browse-state reset); both were addressed in dedicated commits;
- no unresolved review threads remain.

The six Device-test readiness items intentionally remain unchecked because they require an actual physical Android device and live MediaSession. They must not be inferred from CI.


## Physical-device feedback follow-up

Initial device use exposed two presentation/runtime gaps, now addressed on this branch:

- album artwork was not connected to the production Phone host; selected-session artwork now follows `METADATA_KEY_ALBUM_ART` -> `METADATA_KEY_ART` -> `MediaDescription.iconBitmap`, while missing artwork uses the AALyrics foreground mark derived from `branding/android/AALyrics_foreground_android.svg`;
- Translation appeared enabled on an unconfigured install; the application persistence default is now OFF while English remains the built-in/default target and becomes active only after explicit user enablement.

Validation after these fixes:

- architecture boundary check passes;
- debug APK build passes;
- unit tests pass;
- sideloadable debug APK artifact upload passes.

The next manual device pass should specifically verify real album-art rendering, branded fallback rendering, and Translation OFF on a fresh/unconfigured preference state.


## Verbose Details playback-source follow-up

Physical-device validation confirmed that the normal playback-source label should remain human-readable (for example, `Spotify`). Verbose Details now additionally exposes the underlying playback application package from `PlaybackSource.id` (for example, `com.spotify.music`) as `App package`. This is presentation-only and does not change MediaSession selection or package-label resolution.


## In-app License follow-up

Settings > License now uses the same second-level Settings navigation pattern as Advanced instead of opening GitHub.

- repository-root `LICENSE` is the only license-content source of truth;
- the app Gradle build copies it automatically into generated assets as `aalyrics_license.txt`;
- `:app` reads the bundled file and supplies presentation-ready `licenseText`;
- `:ui:phone` renders it in `LicenseSettingsScreen` using the standard `SettingsSubscreenHeader`;
- the license body is scrollable/selectable and works offline;
- no full license text is hard-coded into Kotlin or Android string resources.


## Markdown renderer adoption

The temporary Phone-local regex Markdown parser has been replaced with the Compose-native `mikepenz/multiplatform-markdown-renderer` Material 3 renderer.

- pinned version: `0.38.1`;
- chosen to stay below the library's Java 21 transition while AALyrics remains on Java 17 / compileSdk 36;
- `PhoneMarkdownText` is now a thin wrapper rather than a Markdown grammar implementation;
- License uses this wrapper now;
- future Changelog / Release-note Markdown should reuse the same wrapper.
