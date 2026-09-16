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
- [x] Open PR #19 and perform bounded review; no material in-scope findings.
- [x] Stop before merge; explicit user approval remains required.

## Intentional adaptations

- HTTP/service failures become exceptions, rather than the fork's catch-all no-result behavior, to satisfy the existing provider contract.
- Parsed payload usability is checked before accepting fast-path/local winners, so malformed or empty timed payloads cannot block valid fallback.
- Shared parser retains enhanced-LRC coverage; LRCLIB advertises and returns only PLAIN/LINE.

Shared utility validation: matching, selection and LRC parser tests passed locally. Java 25 needs a short jdk.net.unixdomain.tmpdir path on this machine; this is a command-only workaround.

LRCLIB validation: 21 deterministic MockWebServer tests passed (discovery/fallback, deduplication, parsing, metadata, HTTP failures and in-flight cancellation). Full `test check :app:assembleDebug` (including Android lint), architecture and diff checks passed. PR review completed; final CI status is tracked on PR #19 and must be green before pre-merge approval.

## Bounded review record

- PR: https://github.com/whoxamxl/AALyrics/pull/19
- Normal round 1: Codex self-review of PR head `2dbb67d`, using this task, PR description, architecture and LRCLIB profile as acceptance criteria.
- Reviewed discovery/fallback/local scoring against the working fork; normalized output, malformed payload handling, coroutine/HTTP cancellation and resource closure; shared matching/parser ownership; regression tests and Gradle/architecture boundaries.
- Verified discovery/scoring source matches the fork after only the documented suspend, payload-usability and shared-duration substitutions. Global selector code changes are imports only.
- No unresolved P0/P1 or current-scope blocking P2. The latest review has no material in-scope findings, so the AGENTS.md exit condition permits ending broad review after one round.
- This final record changes documentation only; verify final-head CI through the PR checks. No merge or application wiring was performed.
