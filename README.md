# AALyrics

AALyrics is a new Android project for synchronized lyrics on phone and Android Auto.

> **Status:** the production Phone shell, all four Lyrics Providers, live Android media-session runtime, demand gating, Settings/Details, verified in-app update flow, Phone Translation, and gated Phone WORD_SYNC Karaoke are established. The current `feature/android-auto-now-playing` branch completes the legacy Android Auto Now Playing path with artwork, real transport capabilities, line-oriented synchronized lyrics, identity-gated Translation, process-recovery hardening, and bounded cold-start lookup recovery. Sync calibration UX/persistence and the richer Car App Library presentation remain later work.

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

Current production capabilities include:

- provider-independent track/playback/lyrics models, race-safe lyrics lifecycle, and stale-result protection;
- concurrent provider orchestration and production cross-provider candidate selection;
- LRCLIB, PetitLyrics, Musixmatch, and SyncLRC provider adapters;
- live Android MediaSession observation plus Phone/projection/Automotive-host lyrics-demand gating and listener-rebind recovery;
- the production Phone Compose shell with Lyrics, Playback Surface, Details, Settings, and the explicit Sync placeholder;
- playback-source eligibility, queue/artwork handling, legal/help surfaces, storage/reset controls, and Android Auto compatibility onboarding;
- Translation background/model lifecycle, execution/orchestration, Phone translated lyric rows, Track Card runtime feedback, read-only Translation diagnostics, and the current branch's line-oriented Android Auto Translation projection;
- manual and automatic GitHub Release discovery, verified APK download, SHA-256 checking, package/signing preflight, install-source permission handling, PackageInstaller handoff, recovery, and post-update feedback;
- signed GitHub prerelease distribution, currently through `v0.2.0-alpha.2`.

The framework-independent timing engine derives line and word timing facts from canonical lyrics and effective lyrics position. Normal Phone presentation and Android Auto consume the shared active-line fact; gated Phone WORD_SYNC Karaoke additionally consumes the shared word/progress facts. Sync controls/calibration remain separate future work, and Android Auto intentionally remains line-oriented rather than adopting Karaoke sweep presentation.

## Distribution and Android Auto sideloading

AALyrics is distributed outside Google Play. The durable distribution path is a **signed release APK attached to a GitHub Release**. Debug APKs remain development-only artifacts.

### Install a release

1. Open the desired GitHub Release and download `AALyrics-vX.Y.Z.apk`.
2. Install the APK on the Android phone. When installing from a browser or file manager, Android may require permission for that app to install unknown apps.
3. Open AALyrics once.
4. In Android system settings, open **Notification access** and enable **AALyrics**. This is required for AALyrics to observe the active media session.
5. For the current legacy Android Auto media path, enable Android Auto developer mode once, then enable **Developer settings → Unknown sources**. This applies to the sideloaded `MediaBrowserServiceCompat` compatibility path; the separately planned Car App Library templated-media path has different sideload rules.
6. Reconnect Android Auto or the Desktop Head Unit and enable AALyrics in the Android Auto launcher/customize list if necessary.

The legacy Android Auto host renders AALyrics' `MediaSessionCompat` metadata in compact/split and full Now Playing layouts. The display title uses track title plus compact Automotive artist identity (album artist when available, otherwise track artist), the current synchronized source lyric is published through the display subtitle, and an eligible translated line uses the display description. Host layout/ellipsis remains Android Auto controlled. Lyrics Browse/Expanded UI is intentionally deferred.

If a debug build is already installed, uninstall it before installing the first signed release because debug and release APKs use different signing certificates. After that first switch, later GitHub Release APKs signed with the same release keystore can update the installed release normally.

### Development build

For a local debug build:

```bash
./gradlew :app:installDebug
```

CI also uploads `aalyrics-debug-apk` for pull requests and `main` builds. This artifact is not the durable distribution package.

### Release policy

Signed GitHub Releases are the durable distribution channel. Early functional snapshots use GitHub Pre-releases rather than pretending to be stable builds:

- `v0.1.0-alpha.1`, `-beta.N`, and `-rc.N` are published as **Pre-releases**;
- `v0.1.0`-style tags with no suffix are normal/stable GitHub Releases;
- release tags must point to commits contained in `main`;
- the initial signed distribution `v0.1.0-alpha.1` has been published as a GitHub Pre-release;
- release signing material is stored only through GitHub Actions secrets, never in Git.

An alpha release may be functionally incomplete. Its purpose can be to validate signing, installation, update compatibility, Android Auto discovery, and the durable distribution pipeline while clearly documenting current limitations.

See [docs/RELEASES.md](docs/RELEASES.md) for the authoritative versioning, signing, secret setup, publishing, checksum, backup, and update policy.

## Modules

```text
app                    Android application / composition root
core:model             Shared domain models
core:lyrics            Lyrics state, orchestration, playback ownership, selector port
core:timing            Effective lyrics clock + shared LINE/WORD timing semantics
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
