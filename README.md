# AALyrics

AALyrics is a new Android project for synchronized lyrics on phone and Android Auto.

> **Status:** early foundation. The project is being built from a new codebase and is not yet intended for daily use.

## Project direction

AALyrics is designed around a few strict boundaries from the start:

- playback tracking is separate from lyrics retrieval
- lyrics providers are isolated behind provider contracts
- provider selection is centralized in the lyrics domain
- phone and automotive presentation consume shared application state
- platform-specific code does not leak into provider/domain code
- timing, translation, caching, and provider implementations remain independently replaceable

The current repository contains only the new project foundation. No implementation code has been imported from the previous Auto Lyrics fork.

## Planned modules

```text
app                  Android application / composition root
core:model           Shared domain models
core:lyrics          Lyrics orchestration and selection domain
provider:api         Provider contracts only
platform:media       Android media-session / playback integration
feature:phone        Phone UI
feature:automotive   Android Auto presentation
```

Provider implementations, translation, caching, karaoke timing, and other application features will be added incrementally after the foundation is stable.

## Package

`io.github.whoxamxl.aalyrics`

## License

AALyrics is **source-available**, not Open Source Initiative (OSI) open source.

The project is licensed under the **PolyForm Noncommercial License 1.0.0**. Personal and other noncommercial uses are permitted under its terms. Commercial use requires separate permission from the copyright holder.

See [LICENSE](LICENSE).
