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

- [ ] Add one shared provider architecture/migration document.
- [ ] Add concise provider profiles for LRCLIB, PetitLyrics, Musixmatch, and SyncLRC.
- [ ] Keep provider profiles focused on capabilities, provider-local behavior, quirks, invariants, and migration status rather than duplicating architecture.
- [ ] Align `ARCHITECTURE.md`, `ROADMAP.md`, and `MIGRATION_INVENTORY.md` with the finalized provider migration model.
- [ ] Update `AGENTS.md` so planning/documentation approval is never inferred as authorization to begin provider implementation.
- [ ] Open a documentation PR and run the normal bounded review/CI process.
- [ ] Stop before merge for explicit approval.

## Policy being documented

- AALyrics architecture remains authoritative.
- Existing mature provider implementations in the working fork are normally **PRESERVE / REFACTOR**, not gratuitous rewrites.
- Provider migration may reuse/refactor mature implementation semantics and tested code structure where appropriate; do not force behavioral re-derivation merely to claim a rewrite.
- Provider-local HTTP, authentication, search, parsing, and provider validation stay inside the provider adapter.
- Cross-provider ranking remains in `:provider:selection` behind the `CandidateSelector` port.
- Providers return normalized `LyricsCandidate` values and provider-neutral evidence, not final global scores.
- Each concrete provider is migrated as a bounded slice with regression coverage.
- PetitLyrics configuration values are immutable during migration unless the user explicitly requests otherwise.

## Explicit non-goals

- no concrete provider source migration,
- no changes to provider network behavior,
- no changes to selector scoring,
- no application wiring for providers,
- no reuse or cherry-picking from the existing experimental LRCLIB adaptation branch.
