<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="AALyrics" width="128" />
</p>

# AALyrics

**Synchronized lyrics for Android and Android Auto.**

AALyrics follows what is playing on your Android device and presents matched lyrics in real time. The current Beta already covers the core experience: synchronized Phone lyrics, Android Auto Now Playing lyrics, multiple lyric providers, on-device Translation support, experimental word-synced Karaoke, and signed in-app updates.

AALyrics is distributed as a signed APK through [GitHub Releases](https://github.com/whoxamxl/AALyrics/releases). It is not currently distributed through Google Play.

## What you can do

- **Follow synchronized lyrics on your phone** with automatic current-line tracking and manual browsing.
- **See the current lyric in Android Auto Now Playing** together with artwork and the media app's available transport controls.
- **Use multiple lyric sources** — LRCLIB, PetitLyrics, Musixmatch, and SyncLRC — with provider-independent candidate selection.
- **Translate lyrics on-device** when a supported Translation route/model is available.
- **Try experimental WORD_SYNC Karaoke** for genuine word-timed lyrics on Phone.
- **Control playback from the Phone surface** with play/pause, previous/next, seek, relative seek, and queue support where the active media app exposes them.
- **Update AALyrics from inside the app** using signed GitHub Release APKs with SHA-256 verification and Android package/signing checks.

## AALyrics 1.0 Beta

AALyrics has moved beyond the construction-only stage: its primary lyrics experience is functional on Phone and Android Auto, and the project is now preparing the **1.0 Beta** line.

Beta does not mean every planned feature is complete. The main areas still evolving are:

- Sync calibration controls and persistence;
- Phone WORD_SYNC Karaoke, which remains experimental and opt-in;
- broader Android Auto feature parity beyond the current line-oriented Now Playing experience.

These are Beta limitations rather than blockers for using AALyrics for its primary purpose: following lyrics for the track currently playing.

## Install AALyrics

### Phone

1. Open [GitHub Releases](https://github.com/whoxamxl/AALyrics/releases) and download the current signed `AALyrics-vX.Y.Z[-channel.N].apk`.
2. Install the APK. Android may ask you to allow the browser or file manager to install unknown apps.
3. Open AALyrics.
4. Grant **Notification access** when prompted. AALyrics uses Android's media-session access to identify the active playback session and follow playback state.

The Release also includes a matching `.sha256` file if you want to verify the downloaded APK before installation.

After the first release-signed installation, later AALyrics releases signed with the same key can update the installed app normally. AALyrics can also discover, verify, and hand off newer GitHub Release APKs through its in-app update flow.

> If you already have a debug build installed, uninstall it before installing the first signed Release APK. Debug and Release builds use different signing identities.

### Android Auto

AALyrics currently uses a sideloaded Android Auto media compatibility path.

For Android Auto use:

1. Install and open AALyrics on the phone and enable its Notification access.
2. Enable **Android Auto developer mode**.
3. In Android Auto Developer settings, enable **Unknown sources**.
4. Reconnect Android Auto if needed.
5. Make sure AALyrics is enabled in the Android Auto launcher/customize list.

The current Android Auto experience is intentionally focused on **Now Playing**. It publishes the active track, artwork, supported playback actions, the current synchronized lyric line, and an eligible translated line when available. Android Auto controls the final host layout and text truncation.

## Lyrics and timing

AALyrics can consume plain, line-synchronized, and genuine word-synchronized lyric data depending on what the selected provider returns.

The app keeps lyric selection provider-independent and uses shared playback/timing semantics across Phone and Android Auto. On Phone, the lyrics viewport follows the current line while still allowing manual browsing; word-synchronized data can additionally drive the experimental Karaoke presentation.

Actual lyric availability and timing quality depend on the metadata exposed by the active media app and on the matching lyric data available from the providers.

## Translation

Phone Translation runs through the established AALyrics Translation pipeline and uses on-device language models where supported.

Translated text is presented as secondary lyric text and remains tied to the identity and timing of the canonical lyric document. Model availability and language routing can be inspected from the app's Details/Settings surfaces.

## Development

AALyrics is an Android project built with Gradle. For a local debug installation:

```bash
./gradlew :app:installDebug
```

The CI build uses JDK 17. Debug APKs produced by local/CI development builds are development artifacts and are not interchangeable with the signed GitHub Release channel.

### Project structure

```text
app                    Android application / composition root
core:model             Shared domain models
core:lyrics            Lyrics lifecycle and orchestration
core:timing            Shared synchronized-lyrics timing semantics
provider:api           Provider contracts
provider:matching      Provider-neutral matching semantics
provider:lrc           Shared LRC parsing
provider:selection     Cross-provider candidate selection
provider:lrclib        LRCLIB integration
provider:petitlyrics   PetitLyrics integration
provider:musixmatch    Musixmatch integration
provider:synclrc       SyncLRC integration
platform:media         Android media-session / playback integration
ui:designsystem        Shared UI tokens and components
ui:phone               Phone presentation
ui:automotive          Android Auto presentation
```

For implementation details, start with:

- [Architecture](docs/ARCHITECTURE.md)
- [Lyrics pipeline](docs/LYRICS_PIPELINE_ARCHITECTURE.md)
- [Timing architecture](docs/TIMING_ARCHITECTURE.md)
- [Translation architecture](docs/TRANSLATION_ARCHITECTURE.md)
- [Android Auto Now Playing](docs/ANDROID_AUTO_NOW_PLAYING.md)
- [Release policy](docs/RELEASES.md)
- [Roadmap](docs/ROADMAP.md)

## Package

`io.github.whoxamxl.aalyrics`

## License

AALyrics is **source-available**, not Open Source Initiative (OSI) open source.

The project is licensed under the **PolyForm Noncommercial License 1.0.0**. Personal and other noncommercial uses are permitted under its terms. Commercial use requires separate permission from the copyright holder.

See [LICENSE](LICENSE) for the complete terms.
