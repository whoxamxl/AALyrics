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

## Android Auto sideload development

AALyrics exposes a MediaBrowserService-backed Android Auto media surface for development builds. The host renders the same MediaSession metadata in compact/split and full Now Playing layouts; the current line-timed lyric is published through the display subtitle. Lyrics browse-window UI is intentionally not part of this slice.

Build and install locally:

```bash
./gradlew :app:installDebug
```

For a fresh sideload:

1. Open AALyrics on the phone once after installation.
2. In Android system settings, open **Notification access** (search Settings for "Notification access" if needed) and enable **AALyrics**. This access is required for AALyrics to observe the active media session.
3. Open Android Auto settings on the phone.
4. Open **About** and tap **Version and permission info** repeatedly until Android Auto developer mode is enabled.
5. Open the overflow menu, choose **Developer settings**, and enable **Unknown sources**.
6. Reconnect Android Auto or the Desktop Head Unit after installing AALyrics.
7. Enable AALyrics in the Android Auto launcher/customize list if it is not already visible.

The CI build also uploads `aalyrics-debug-apk` as a workflow artifact. This is a development sideload artifact; durable release signing/versioned distribution remains a separate release-engineering slice.

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
