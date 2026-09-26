# Documentation Alignment — 1.0 Beta / README / Social Preview

## Branch and baseline

- Branch: `docs/post-beta-alignment`.
- Base: `main @ 6f0ff14f9afe2daaa4262af1df37f6d73a593ce0`.
- This slice aligns durable documentation after the `v1.0.0-beta.1` release and the product-facing README compatibility update.
- No application runtime, provider, playback, timing, Translation, Karaoke, Android Auto, persistence, signing, or update behavior changes are in scope.

## Current public state

- `v1.0.0-beta.1` is published as **AALyrics 1.0 Beta 1**.
- The canonical tag / Android version identity remains `v1.0.0-beta.1` / `1.0.0-beta.1`.
- The GitHub Release is currently a normal Release with no explicit Pre-release label, so GitHub exposes it as the repository's current **Latest** release.
- This GitHub presentation choice does not change the product channel: the release is still **1.0 Beta** and is not a stable `1.0.0` product release.
- The Release workflow still defaults prerelease-suffixed tags to GitHub Pre-release; the current Beta's label is a deliberate post-publication presentation exception.

## README compatibility contract

The public README uses this concise compatibility statement:

> Works with Spotify, YouTube Music, Apple Music and other Android MediaSession players.

This means:

- AALyrics observes Android's active MediaSession / MediaController surface.
- Spotify, YouTube Music, and Apple Music are named examples of media apps that can participate through that Android contract.
- AALyrics does not claim a private/direct API integration, partnership, or endorsement with those services.
- Exact metadata, artwork, queue, seek, and transport capabilities remain dependent on what the active media app exposes through Android.

## Social Preview contract

The GitHub Social Preview should communicate the real Android Auto product without inventing a substitute application UI.

- Use the canonical AALyrics brand mark from `branding/AALyrics_MASTER.svg` or a derivative generated from it.
- Use authentic AALyrics / Android Auto product imagery as the factual UI reference.
- Preferred composition: dark automotive / navy background, blurred Android Auto context, and the authentic AALyrics Android Auto surface presented in the foreground with restrained glassmorphism, depth, and cyan-blue edge light.
- Marketing treatment may crop, blur, frame, shadow, or composite authentic UI, but should not fabricate unsupported controls, metadata, capabilities, or a replacement AALyrics logo.
- Target GitHub Social Preview output: **1280×640**, under **1 MB**.
- Approved primary copy:
  - `Synchronized lyrics for Android Auto`
  - `Multi-provider lyrics matching for your music player of choice.`
- Approved feature labels:
  - `Multi-provider matching`
  - `Intelligent provider selection`
  - `Synchronized lyrics`
  - `Built for Android Auto`
- Provider row may identify Musixmatch, LRCLIB, PetitLyrics, and SyncLRC.

## Alignment targets

- [x] Replace stale release-preparation `TASK.md` with the current documentation slice.
- [x] Align `docs/RELEASES.md` with the published 1.0 Beta state and GitHub Release-label exception.
- [x] Align `docs/ROADMAP.md` with the completed 1.0 Beta milestone and merged Android Auto work.
- [x] Document the public MediaSession player-compatibility meaning in `docs/MEDIA_SESSION_RUNTIME.md`.
- [x] Align `docs/ANDROID_AUTO_NOW_PLAYING.md` with that player-source contract.
- [x] Record the GitHub Social Preview design/brand contract in `docs/BRANDING.md`.
- [x] Run a branch-wide documentation regression/staleness review and normal repository validation.

## Acceptance criteria

- Durable docs no longer describe `v1.0.0-beta.1` as an upcoming release.
- Durable docs distinguish the semantic Beta channel from GitHub's current Latest/label presentation.
- README player examples are explicitly grounded in Android MediaSession compatibility rather than implied service-specific integrations.
- Branding docs prevent future Social Preview work from substituting an approximate AALyrics logo or fictional product UI.
- Roadmap status no longer points at a historical working branch/TASK as if it were current execution state.
- No production application behavior changes.


## Validation record

- Branch-wide diff is documentation-only: `TASK.md`, `docs/RELEASES.md`, `docs/ROADMAP.md`, `docs/MEDIA_SESSION_RUNTIME.md`, `docs/ANDROID_AUTO_NOW_PLAYING.md`, and `docs/BRANDING.md`.
- The branch is zero commits behind `main` at the final documentation review point.
- README compatibility wording is present on `main` and matches the MediaSession compatibility contract added here.
- The live GitHub Release state was re-verified: `v1.0.0-beta.1` is published, `prerelease=false`, and is returned by `/releases/latest`; durable docs explicitly distinguish that presentation state from the semantic Beta version channel.
- Roadmap no longer treats the historical Android Auto working branch or release-preparation TASK as current execution state.
- Branding documentation now requires the canonical AALyrics logo and authentic product UI as the factual source for Social Preview artwork, with 1280×640 / under-1-MB output guidance and the approved glassmorphism direction.
- No production source, Android resources, provider implementation, runtime behavior, signing configuration, or workflow behavior changed.
- Branch pushes do not start the normal Build workflow in this repository; the Draft PR Build remains the normal CI gate after PR creation.
