# LRCLIB Provider Profile

## Status

- Migration status: **IMPLEMENTED in LRCLIB migration slice — pending merge**
- Default migration order: first concrete provider
- Working-fork baseline: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Reference files: `LrcLibClient.kt`, `LrcParser.kt`, `LrcLibClientTest.kt`, `LrcParserTest.kt`

This file records LRCLIB-specific behavior only. Shared provider rules are defined in `docs/PROVIDER_ARCHITECTURE.md`.

## Capabilities

- plain lyrics: yes
- line-synchronized lyrics: yes
- word-synchronized lyrics: no current LRCLIB provider requirement
- authentication: none in the current working implementation

Expected AALyrics descriptor capability: `PLAIN`, `LINE`.

## Mature discovery behavior to preserve

The working provider progressively broadens discovery while validating candidates locally:

```text
precise /get when album + duration are available
        ↓
structured search: title + artist + album
        ↓
structured search without album
        ↓
featured/primary metadata variant when applicable
        ↓
title-only search
        ↓
free-text fallback
        ↓
plain lyrics only after synchronized candidates are exhausted
```

A very strong synchronized `/get` match may return early. Search results are deduplicated before local candidate selection.

## Validation and matching

Preserve the mature title, artist, duration, album, contributor-component, and recording-version validation semantics. These semantics are generic and must not become duplicated LRCLIB-only copies if another provider or the production selector also needs them.

LRCLIB owns the provider-local decision about whether an LRCLIB response is plausible enough to emit. It does not assign the final cross-provider winner score.

## Parsing and normalization

- synchronized LRC is parsed to line-timed domain rows in milliseconds,
- multiple timestamps on one LRC row remain supported,
- empty timed text may be represented by the parser but must not make an otherwise unusable candidate win,
- plain lyrics are normalized to plain domain rows,
- provider result ID should remain available as attribution/source identity when present,
- provider metadata becomes the candidate's matched track metadata; unknown provider metadata should remain unknown rather than being fabricated from the request merely to improve scoring.

## Regression-sensitive behavior

Preserve tests covering at least:

- full-width/NFKC metadata normalization,
- contributor-list artist matching,
- live/acoustic/remix/remaster/instrumental version compatibility,
- album evidence that corroborates but does not erase explicit title-version conflicts,
- duration scoring boundaries,
- enhanced/ordinary LRC timestamp parsing behavior that remains relevant to the shared parser design,
- synchronized-over-plain fallback order,
- weak or malformed result rejection.

## AALyrics ownership

Provider-local:

- LRCLIB HTTP endpoints and request construction,
- LRCLIB search/fallback sequence,
- LRCLIB DTOs,
- LRC/plain parsing and response normalization,
- local result plausibility checks.

Outside LRCLIB:

- cross-provider source confidence and winner selection,
- application state and provider fan-out,
- Android media/playback identity,
- UI, cache, and translation.

## Migration note

Do not reuse an experimental LRCLIB branch by default. The approved migration slice starts from current `main`, this profile, the current working-fork implementation/tests, and the shared provider architecture. Reuse of experimental commits requires explicit user authorization.

## Implemented adapter and intentional adaptations

- `:provider:lrclib` exposes `LrcLibProvider`, implementing `LyricsProvider`; HTTP client/base URL injection supports deterministic local-server tests.
- `:provider:matching` shares preserved metadata/version helpers with selection. LRCLIB retains its local weights and thresholds; no final cross-provider score crosses the API.
- `:provider:lrc` preserves the ordinary and enhanced parser and all five original enhanced-LRC tests. Invalid decreasing word timestamps reject that malformed row to satisfy domain invariants; LRCLIB uses ordinary parsing only.
- The fork's whole-second playback query/validation is preserved. Response durations normalize to milliseconds; non-positive/non-finite values remain unknown. Response artist/album are not replaced with request values, and missing titles fail local validation.
- The fork's catch-all network failure suppression is replaced with operational exceptions and cancellable OkHttp calls, as required by the provider contract. Exact `/get` 404 is a miss; other unsuccessful statuses are failures. Malformed payloads/rows are rejected while discovery continues.
- Timed payloads must contain real text before fast-path/local acceptance. This preserves the fork call site's usability rule while allowing later synchronized fallback instead of letting malformed LRC stop discovery. Empty timed rows retain the existing `♪` representation.
- User-Agent identifies AALyrics and its repository instead of the old app/version. HTTP endpoints and 10s connect/15s read timeouts are preserved.
- Deduplication retains the first response by result ID, or by metadata when ID is missing, as in the fork. One provider-local winner is emitted; global selection remains external. Artist-query corroboration remains false as in the original LRCLIB call site.
- Application wiring and other providers remain separate future work.
