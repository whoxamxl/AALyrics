# Android Auto Now Playing and Sideload

## Branch and baseline

- Branch: `feature/android-auto-now-playing`.
- Base: `main` at `342c84a6023785c4b8682cfa6a7bd5999a3148ed` after PR #35 merged.
- Working-fork behavioral reference: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (v1.13.0).
- Classification: **PRESERVE / REFACTOR** for proven Android Auto media-host behavior, **REWRITE** for AALyrics ownership/integration.
- Authoritative references: `AGENTS.md`, `docs/MIGRATION_INVENTORY.md`, `docs/UI_ARCHITECTURE.md`, `docs/PRESENTATION_STATE_ARCHITECTURE.md`, and `docs/KARAOKE_ARCHITECTURE.md`.
- The user explicitly authorized implementation of Android Auto split/full Now Playing behavior and a sideloadable Android Auto build path.

## Goal

Expose AALyrics as a sideloadable Android Auto media app and project the existing normalized playback + lyrics state into the host-managed Now Playing surfaces without reintroducing the working fork's `MediaTracker` monolith.

The runtime path is:

```text
existing selected MediaSession runtime
        ↓
PlaybackSnapshot + LyricsState
        ↓
application composition
        ↓
automotive presentation binding
        ↓
MediaBrowserServiceCompat + MediaSessionCompat
        ↓
Android Auto host
   ├─ split / compact Now Playing
   └─ full Now Playing / lyrics browse surface
```

## Acceptance criteria

- Add Android Auto media-app discovery metadata and an `automotive_app_desc.xml` declaring `media`.
- Add a thin `MediaBrowserServiceCompat` host service that publishes a `MediaSessionCompat`.
- Populate `DISPLAY_TITLE` with current track/artist and `DISPLAY_SUBTITLE` with the active line-timed lyric so Android Auto can render useful split/compact and full Now Playing metadata.
- Expose a bounded lyrics browse window for the expanded/full automotive surface without provider access or networking.
- Mirror normalized playback state and position into the automotive MediaSession.
- Forward host Play/Pause/Previous/Next controls through an explicit transport boundary owned by `:platform:media`; do not rediscover active sessions from `:ui:automotive`.
- Keep current-line selection line-level only in this slice. Do not implement word-level karaoke, translation, timing calibration/offset, cache, or sync editing.
- Preserve `:ui:automotive` independence from concrete providers and `:platform:media`.
- Make debug APKs available as CI artifacts suitable for sideloading.
- Document Android Auto developer-mode / Unknown sources setup and local `installDebug` workflow.
- Add deterministic tests for automotive state projection and transport routing.
- Run repository validation, review the complete diff, open a PR, and stop before merge for explicit approval.

## Planned commits

- [x] Prepare Android Auto Now Playing task and branch.
- [ ] Add platform transport-control boundary.
- [ ] Add automotive runtime binding and presentation projection.
- [ ] Add Android Auto MediaBrowser/MediaSession host service and media discovery descriptor.
- [ ] Wire application playback/lyrics state into the automotive host.
- [ ] Add sideloadable CI artifact and setup documentation.
- [ ] Add deterministic regression tests.
- [ ] Run validation and architecture checks.
- [ ] Review complete diff and open PR.
- [ ] Stop before merge.

## Scope guard

This slice migrates the proven Android Auto media-host path only. It must not copy legacy provider, translation, calibration, cache, artwork/color, or word-karaoke logic into AALyrics. Any richer timing/karaoke behavior remains behind the dedicated capability architecture and should be added in a later slice.
