# AALyrics

AALyrics is a new Android project for synchronized lyrics on phone and Android Auto.

> **Status:** the core lyrics engine, all four providers, production selection, application composition, and the UI foundation are established. The live Android media-session runtime is implemented in PR #29 and is pending merge; finished end-user presentation remains later work.

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
- shared/phone/automotive UI foundation (PRs #27 and #28).

The current `feature/media-session-runtime` branch implements live Android media-session discovery, token-based session selection, selected-controller callback ownership, playback normalization, and handoff into the existing production lyrics graph. Phone/Android Auto feature completion, demand gating, cache, translation, timing controls, persistence, and karaoke rendering remain separate later work.

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

See `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/APPLICATION_COMPOSITION.md`, `docs/MEDIA_SESSION_RUNTIME.md`, `docs/UI_ARCHITECTURE.md`, and `docs/PROVIDER_ARCHITECTURE.md` for the authoritative architecture and migration plan.

## Package

`io.github.whoxamxl.aalyrics`

## License

AALyrics is **source-available**, not Open Source Initiative (OSI) open source.

The project is licensed under the **PolyForm Noncommercial License 1.0.0**. Personal and other noncommercial uses are permitted under its terms. Commercial use requires separate permission from the copyright holder.

See [LICENSE](LICENSE).
