# Provider Migration Documentation Task

## Current branch

`docs/provider-migration-architecture`

## Goal

Define the provider migration/documentation policy before any concrete provider implementation resumes.

This branch is documentation-only. It does **not** authorize or contain LRCLIB, PetitLyrics, Musixmatch, or SyncLRC implementation migration.

Reference baseline re-checked before documenting provider profiles:

- repository: `whoxamxl/auto-lyrics`
- branch: `main`
- commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)

## Work slices

- [x] Add one shared provider architecture/migration document.
- [x] Add concise provider profiles for LRCLIB, PetitLyrics, Musixmatch, and SyncLRC.
- [x] Keep provider profiles focused on capabilities, provider-local behavior, quirks, invariants, and migration status rather than duplicating architecture.
- [x] Clarify that playback sources such as Spotify are not lyrics providers, while normalized Spotify identity may be consumed by Musixmatch for stronger matching.
- [x] Align `ARCHITECTURE.md`, `ROADMAP.md`, and `MIGRATION_INVENTORY.md` with the finalized provider migration model.
- [x] Update `AGENTS.md` so planning/documentation approval is never inferred as authorization to begin provider implementation.
- [x] Open a documentation PR and run CI.
- [ ] Complete the bounded review process.
- [ ] Stop before merge for explicit approval.

## Policy documented

- AALyrics architecture remains authoritative.
- Existing mature provider implementations in the working fork are normally **PRESERVE / REFACTOR**, not gratuitous rewrites.
- Provider migration may reuse/refactor mature implementation semantics and tested code structure where appropriate; do not force behavioral re-derivation merely to claim a rewrite.
- Provider-local HTTP, authentication, search, parsing, and provider validation stay inside the provider adapter.
- Cross-provider ranking remains in `:provider:selection` behind the `CandidateSelector` port.
- Providers return normalized `LyricsCandidate` values and provider-neutral evidence, not final global scores.
- A playback source is not automatically a lyrics provider; Spotify currently supplies normalized track identity that Musixmatch may use as corroboration while Musixmatch remains the lyrics source.
- Each concrete provider is migrated as a bounded slice with regression coverage.
- PetitLyrics configuration values are immutable during migration unless the user explicitly requests otherwise.
- Documentation/planning approval is not implementation approval.

## Explicit non-goals

- no concrete provider source migration,
- no changes to provider network behavior,
- no changes to selector scoring,
- no application wiring for providers,
- no reuse or cherry-picking from the existing experimental LRCLIB adaptation branch.

## Durable documents

- `docs/PROVIDER_ARCHITECTURE.md` — shared provider architecture and migration rules.
- `docs/providers/LRCLIB.md` — LRCLIB provider profile.
- `docs/providers/PETITLYRICS.md` — PetitLyrics provider profile and immutable configuration rule.
- `docs/providers/MUSIXMATCH.md` — Musixmatch mobile-flow provider profile, including Spotify-aware matching semantics.
- `docs/providers/SYNCLRC.md` — SyncLRC karaoke-only provider profile.
- `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, and `docs/MIGRATION_INVENTORY.md` — aligned system/migration references.
- `AGENTS.md` — execution authorization guardrail.
