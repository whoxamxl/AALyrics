# PetitLyrics Provider Migration

## Branch and baseline

- Branch: `feature/petitlyrics-provider-migration` (existing remote branch).
- Current main: `a696083` (LRCLIB PR #19 merged); verified it is an ancestor of the branch.
- Preserved branch commit `a6a65ed`: PetitLyrics configuration-source documentation.
- Re-fetched auto-lyrics main on 2026-09-16: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f`.
- Read required architecture/profile/roadmap/inventory and AGENTS.md; inspected PetitLyricsClient, all 10 reference tests, MediaTracker call sites, and the metadata resolver used by discovery.
- User explicitly authorized PetitLyrics implementation. The inherited LRCLIB TASK.md is not this PR's scope.

## Acceptance criteria

- Preserve progressive title/artist/album discovery, candidate ranking/timing preference, attempted-result deduplication and artist-query corroboration.
- Preserve WSY word spacing/timestamps/end times, LSY key permutation/rollover, lyrics-ID companion selection and metadata fallback.
- Share metadata matching semantics with selection without depending on global selector implementation; keep global winner policy unchanged.
- Normalize provider metadata, attribution and LINE/WORD timing through LyricsProvider. Ambiguous PetitLyrics duration remains unknown; no fabricated missing metadata.
- Preserve all reference regressions; add transport, fallback, malformed payload, configuration and cancellation coverage with fake configuration only.
- Keep all four configured values unchanged. Build reads environment before ignored root secrets.properties.local; never log or commit actual values. Provider receives configuration by injection; no live credentials needed for tests.
- Preserve successful provider-local fallback after individual request failures, while surfacing an operational exception when discovery exhausts without a usable result; propagate cancellation.
- No other provider migration, UI, playback, cache, translation or application provider wiring.
- Targeted tests, full repository validation, PR CI and bounded review; STOP before merge.

## Plan/status

- [x] Verify current remote branch/main and preserve configuration documentation.
- [x] Inspect required documents and latest working-fork behavior/tests/call sites.
- [x] Extract shared metadata score without changing selector policy.
- [x] Migrate PetitLyrics configuration, discovery, transport and WSY/LSY parsing.
- [x] Preserve/add regression coverage and update migration documents.
- [x] Run targeted tests and full repository validation.
- [ ] Open PR and complete bounded review under AGENTS.md.
- [ ] Stop before merge for explicit approval.

Shared matching and selector regression suites passed after extraction. All 10 original PetitLyrics client regressions are ported with normalized domain types and injected fake configuration.

PetitLyrics provider validation: the 10 preserved client regressions and 10 deterministic MockWebServer/configuration/cancellation tests pass. Fake CI-style environment values were verified to override the ignored local configuration source without reading or printing real values.

Full validation passed with fake configuration: 145 tests across 16 suites, Android lint/checks, debug APK assembly, architecture guardrail, and diff whitespace checks.
