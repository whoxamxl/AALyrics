# Provider Selection Migration Task

This branch is the first post-Core-Readiness migration slice. It adapts the mature cross-provider resolver behavior from the working `whoxamxl/auto-lyrics` fork behind AALyrics' existing `CandidateSelector` port.

Reference baseline re-checked before implementation:

- repository: `whoxamxl/auto-lyrics`
- branch: `main`
- commit: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- relevant behavior: `LyricsProviderResolver`, generic metadata matching helpers currently embedded in `LrcLibClient`, and `RecordingVersionContext`

## Classification

- mature resolver scoring/selection semantics — **PRESERVE / REFACTOR**
- generic title/artist/duration/version matching — **PRESERVE / REFACTOR** out of `LrcLibClient`
- provider-specific source-confidence policy — **PRESERVE / REFACTOR** outside pure core
- concrete provider clients — not part of this branch
- cache, translation, demand gating, UI, provider networking — not part of this branch

## Architecture decision

Keep `CandidateSelector` as a port in `:core:lyrics`. Put the production cross-provider implementation in a new pure Kotlin `:provider:selection` module so provider-specific source-confidence policy does not leak into pure application core. The selector consumes only normalized `Track` and `LyricsCandidate` values.

Provider adapters may attach provider-neutral match evidence to a candidate when search context corroborates metadata. They still do not assign the final cross-provider score.

## Scope

1. extend normalized candidate evidence only where required by the proven resolver,
2. add `:provider:selection` and generic matching/version utilities,
3. adapt mature metadata/quality/source-confidence scoring behind `CandidateSelector`,
4. interpret `preferredSyncType = WORD` as the proven near-equivalent karaoke preference,
5. port resolver regression cases to AALyrics models,
6. update architecture/migration roadmap documentation,
7. open a PR, run CI, use the normal review policy, and stop before merge.

## Explicit non-goals

- no LRCLIB, Musixmatch, PetitLyrics, or SyncLRC networking/client implementation,
- no changes to PetitLyrics configuration values,
- no cache/translation/timing-adjustment/UI work,
- no competing scoring algorithm,
- no application wiring that requires a concrete provider.

## Acceptance criteria

- [ ] selector implementation lives outside `:core:lyrics` while implementing its existing port,
- [ ] synchronized candidates beat plain fallback candidates,
- [ ] recording-version mismatches are rejected,
- [ ] title/artist/duration/album scoring preserves the mature resolver thresholds and weights,
- [ ] cross-script artist corroboration behavior is preserved,
- [ ] Japanese/Latin interleaved transliteration quality penalty is preserved,
- [ ] provider source-confidence preferences are preserved without entering pure core,
- [ ] WORD preference only overrides the standard winner for near-equivalent metadata/quality and real word timing,
- [ ] provider execution order is not used as winner policy,
- [ ] migrated regression tests pass,
- [ ] full repository CI passes,
- [ ] stop before merge for explicit approval.
