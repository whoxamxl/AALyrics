# AALyrics

AALyrics is a new Android project for synchronized lyrics on phone and Android Auto.

> **Status:** the core lyrics engine, all four providers, production selection, application composition, UI foundation, and live Android media-session runtime are established. Process-wide lyrics-demand gating is the current background/runtime slice; finished end-user presentation remains later work.

## Project direction

AALyrics is built around strict ownership boundaries:

- playback tracking is separate from lyrics retrieval,
- lyrics providers are isolated behind `LyricsProvider`,
- provider fan-out and lifecycle live in the lyrics core,
- cross-provider winner selection is centralized behind `CandidateSelector`,
- phone and automotive presentation consume shared domain state,
- platform-specific code does not leak into provider/domain code,
- timing, translation, caching, and provider implementations remain independently replaceable.

The codebase is greenfield in structure, while proven behavior from `whoxamxl/auto-lyrics` is used as the migration and regression reference. Mature behavior is preserved/refactored into the new boundaries rather than reimplemented without reason.

## Current implementation status

Completed foundation includes:

- provider-independent track/playback/lyrics models,
- explicit lyrics state lifecycle and stale-result protection,
- concurrent provider orchestration with failure isolation,
- playback identity and playback-to-lyrics ownership,
- production cross-provider candidate selection,
- shared metadata/version matching and LRC parsing,
- LRCLIB provider migration (PR #19),
- PetitLyrics provider migration (PR #20),
- Musixmatch provider migration (PR #21),
- SyncLRC provider migration (PR #24),
- manual production application composition (PR #25),
- shared/phone/automotive UI foundation (PRs #27 and #28),
- live Android MediaSession runtime (PR #29).

PR #30 implements the next runtime slice and is ready for explicit merge approval: MediaSession observation stays alive, while provider lookup runs only when phone-process foreground or Android Auto projection demand is active. Demand deactivation clears lyrics work; reactivation resumes immediately from the latest already-observed playback snapshot. Finished phone/Android Auto presentation, cache, translation, timing controls, persistence, and karaoke rendering remain separate later work.

## Distribution and Android Auto sideloading

AALyrics is distributed outside Google Play. The durable distribution path is a **signed release APK attached to a GitHub Release**. Debug APKs remain development-only artifacts.

### Install a release

1. Open the desired GitHub Release and download `AALyrics-vX.Y.Z.apk`.
2. Install the APK on the Android phone. When installing from a browser or file manager, Android may require permission for that app to install unknown apps.
3. Open AALyrics once.
4. In Android system settings, open **Notification access** and enable **AALyrics**. This is required for AALyrics to observe the active media session.
5. For Android Auto, enable Android Auto developer mode once, then enable **Developer settings → Unknown sources**. This is required for non-Play Android Auto media apps even though the APK itself is signed.
6. Reconnect Android Auto or the Desktop Head Unit and enable AALyrics in the Android Auto launcher/customize list if necessary.

The Android Auto host renders the same MediaSession metadata in compact/split and full Now Playing layouts. The active line-timed lyric is published through the display subtitle. Lyrics browse-window UI is intentionally deferred.

If a debug build is already installed, uninstall it before installing the first signed release because debug and release APKs use different signing certificates. After that first switch, later GitHub Release APKs signed with the same release keystore can update the installed release normally.

### Development build

For a local debug build:

```bash
./gradlew :app:installDebug
```

CI also uploads `aalyrics-debug-apk` for pull requests and `main` builds. This artifact is not the durable distribution package.

### Publishing a signed GitHub Release

Release signing material is never committed to the repository. Configure these GitHub Actions repository secrets:

- `AALYRICS_RELEASE_KEYSTORE_BASE64`
- `AALYRICS_RELEASE_STORE_PASSWORD`
- `AALYRICS_RELEASE_KEY_ALIAS`
- `AALYRICS_RELEASE_KEY_PASSWORD`

Generate the signing key once and keep the original keystore backed up securely. Example:

```bash
keytool -genkeypair -v -keystore aalyrics-release.jks -alias aalyrics -keyalg RSA -keysize 4096 -validity 10000
```

On Windows PowerShell, encode the keystore for the GitHub secret with:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("aalyrics-release.jks"))
```

Put that output in `AALYRICS_RELEASE_KEYSTORE_BASE64`, then add the store password, alias, and key password in their matching secrets.

A release is created by pushing a version tag that points to a commit already contained in `main`:

```bash
git checkout main
git pull
git tag v0.1.0
git push origin v0.1.0
```

The Release workflow runs tests, builds and signs `app-release.apk`, renames it to `AALyrics-vX.Y.Z.apk`, writes a SHA-256 checksum, and publishes both files to the corresponding GitHub Release. The tag supplies `versionName`; the release workflow run number supplies a monotonically increasing `versionCode`.

## Modules

```text
app                    Android application / composition root
core:model             Shared domain models
core:lyrics            Lyrics state, orchestration, playback ownership, selector port
provider:api           Provider contracts
provider:matching      Shared provider-neutral matching semantics
provider:lrc           Shared LRC parsing
provider:selection     Production cross-provider candidate selection
provider:lrclib        LRCLIB adapter
provider:petitlyrics   PetitLyrics adapter
provider:musixmatch    Musixmatch adapter
provider:synclrc       SyncLRC karaoke adapter
platform:media         Android media-session / playback integration
ui:designsystem        Shared presentation tokens/components
ui:phone               Phone presentation composition
ui:automotive          Android Auto presentation composition
```

See `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/APPLICATION_COMPOSITION.md`, `docs/MEDIA_SESSION_RUNTIME.md`, `docs/LYRICS_DEMAND_GATING.md`, `docs/UI_ARCHITECTURE.md`, and `docs/PROVIDER_ARCHITECTURE.md` for the authoritative architecture and migration plan.

## Package

`io.github.whoxamxl.aalyrics`

## License

AALyrics is **source-available**, not Open Source Initiative (OSI) open source.

The project is licensed under the **PolyForm Noncommercial License 1.0.0**. Personal and other noncommercial uses are permitted under its terms. Commercial use requires separate permission from the copyright holder.

See [LICENSE](LICENSE).
