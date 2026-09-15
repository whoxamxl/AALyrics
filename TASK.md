# LRCLIB Provider Migration

## Branch and reference

- Branch: `feature/lrclib-provider-migration`
- AALyrics base: `c7280dedbe73bf69998ad9b9256fcc75a352e9c1` (latest main).
- Working fork: `whoxamxl/auto-lyrics` main `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f`, fetched and re-checked before implementation.
- Read AGENTS.md, architecture, provider architecture, migration inventory, LRCLIB profile, roadmap, and fork client/parser/matching tests and call sites.
- User explicitly authorized LRCLIB implementation as PRESERVE / REFACTOR.

## Acceptance criteria

- Preserve exact-get fast path, structured/featured/title-only/free-text discovery, deduplication, local validation thresholds, and synchronized-before-plain fallback.
- Extract shared metadata/version semantics to a neutral pure provider matching module; keep cross-provider ranking in selection.
- Implement LRCLIB transport, parsing and normalization behind LyricsProvider; report provider metadata/attribution without fabricating missing metadata.
- Preserve existing matching/version/parser regression cases and add deterministic transport/fallback, malformed-result, operational-failure and cancellation tests.
- Reject unusable payloads before local selection; surface operational errors and propagate cancellation as required by the AALyrics contract.
- No application wiring, other provider migration, UI, cache, translation, or cross-provider scoring changes.
- Relevant tests, full repository checks and PR CI pass; complete bounded review and stop before merge.

## Plan/status

- [x] Verify tools/access and latest branches; inspect approved docs and mature implementation/tests.
- [x] Extract shared matching and port matching/version regression tests.
- [x] Preserve parser and implement LRCLIB adapter with regression tests.
- [x] Update durable migration documentation.
- [x] Run targeted tests, full repository tests/build and architecture checks.
- [ ] Open PR and perform at most two normal review rounds; fix material in-scope findings.
- [ ] Stop before merge for explicit approval.

## Intentional adaptations

- HTTP/service failures become exceptions, rather than the fork's catch-all no-result behavior, to satisfy the existing provider contract.
- Parsed payload usability is checked before accepting fast-path/local winners, so malformed or empty timed payloads cannot block valid fallback.
- Shared parser retains enhanced-LRC coverage; LRCLIB advertises and returns only PLAIN/LINE.

Shared utility validation: matching, selection and LRC parser tests passed locally. Java 25 needs a short jdk.net.unixdomain.tmpdir path on this machine; this is a command-only workaround.


LRCLIB validation: 21 deterministic MockWebServer tests passed (discovery/fallback, deduplication, parsing, metadata, HTTP failures and in-flight cancellation). Full `test check :app:assembleDebug` (including Android lint), architecture and diff checks passed. PR review remains pending.
