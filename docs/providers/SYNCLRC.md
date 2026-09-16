# SyncLRC Provider Profile

## Status

- Migration status: **AUTHORIZED — active migration branch, implementation pending**
- Branch: `feature/synclrc-provider-migration`
- Default migration order: fourth concrete provider
- Working-fork baseline: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`), re-checked 2026-09-16
- Reference files: `SyncLrcClient.kt`, `SyncLrcClientTest.kt`, `LrcParser.kt`, relevant `MediaTracker.kt` call sites
- Public API documentation re-checked 2026-09-16: `https://synclrc.dev/`

This file records SyncLRC-specific behavior only. Shared provider rules are defined in `docs/PROVIDER_ARCHITECTURE.md`.

## Capability and role

SyncLRC is intentionally a karaoke/word-timing provider, not a second source for ordinary plain or line-synchronized lyrics already covered by LRCLIB, PetitLyrics, and Musixmatch.

Expected AALyrics descriptor capability: `WORD` only.

The working fork invokes SyncLRC only when karaoke/word synchronization is preferred. AALyrics already carries that preference through `LyricsRequest.preferredSyncType`, so the migration should preserve the same request gate: when the preference is not `WORD`, return no candidates without making a SyncLRC network request.

This gate is provider-local request behavior, not cross-provider winner policy. The production `CandidateSelector` still decides whether a returned WORD candidate beats other synchronized candidates.

## Public API and mature request behavior

The working implementation uses the public endpoint:

`GET https://api.synclrc.dev/lyrics`

with:

- `track` — required and nonblank,
- `artist` — required and nonblank,
- `type=karaoke`,
- `album` when known,
- `duration` when known, rounded from milliseconds to seconds.

The current public API documentation still describes this request shape and currently documents a 30 requests/minute limit. This migration does not introduce new global rate-limit/retry policy; avoiding non-WORD requests preserves the mature narrow usage pattern.

A blank title or artist is not enough to make a useful karaoke request and must not be sent as if it were complete metadata.

No authentication or provider credential belongs to this adapter.

## Response compatibility

The current API exposes separate `karaoke`, `synced`, and `plain` fields. Older deployments used a type-specific `lyrics` + `type` shape.

Preserve compatibility with both response shapes, but only emit a candidate when a **genuine karaoke payload with timed word tokens** is present:

1. prefer a nonblank current `karaoke` field;
2. otherwise accept nonblank legacy `lyrics` only when `type` equals `karaoke` case-insensitively;
3. never promote `synced`, `plain`, `type=synced`, or `type=plain` fallback data into a SyncLRC candidate.

A `type=karaoke` request that returns only line-synchronized or plain lyrics is deliberately treated as no SyncLRC result. Those formats belong to ordinary providers.

## Parsing, validation, and normalization

- reject instrumental responses;
- parse Enhanced-LRC karaoke content through shared `:provider:lrc` `LrcParser.parseKaraoke`;
- require actual timed word tokens; line timing alone is insufficient;
- require usable lyric text rather than placeholder-only content;
- preserve provider metadata when supplied;
- fall back to the requested title, artist, and album where response metadata is blank/missing;
- normalize provider duration seconds to AALyrics milliseconds;
- report `artistQueryCorroborated` when the request included the required artist constraint;
- preserve provider source/id as attribution where appropriate without leaking provider DTOs into core.

SyncLRC does not need a provider-local copy of the global metadata score. It normalizes the returned metadata and karaoke payload; `:provider:selection` performs final metadata plausibility, source-confidence, quality, and winner selection.

## Regression-sensitive behavior

Preserve the working-fork regressions around:

- stable JSON field names;
- current `karaoke` response field;
- legacy `lyrics` + `type=karaoke` compatibility;
- rejection of synced/plain-only current responses;
- rejection of legacy synced/plain fallbacks;
- rejection of instrumental results;
- requirement for genuine timed word tokens;
- Enhanced-LRC parsing;
- fine-grained Japanese word timing;
- track/artist/type request parameters plus optional album/duration;
- artist-query evidence and response-metadata fallback.

AALyrics-specific deterministic coverage must additionally verify:

- no HTTP request when `preferredSyncType != WORD`;
- normalized `WORD` output and milliseconds;
- operational HTTP/service failure surfaces as an exception instead of "not found";
- malformed/unusable provider payloads remain no-result rather than operational failures;
- coroutine cancellation cancels in-flight HTTP work where practical.

## AALyrics ownership

Provider-local:

- SyncLRC HTTP transport and query construction;
- WORD-preference request gating;
- response DTO/shape compatibility;
- karaoke-only acceptance policy;
- provider-local payload validation;
- normalization into `LyricsCandidate`.

Shared utility:

- Enhanced-LRC syntax and word-timing parsing remain in `:provider:lrc`.

Outside SyncLRC:

- deciding whether a WORD candidate beats a strong LINE candidate;
- metadata/source-confidence/final cross-provider ranking;
- provider fan-out and application `LyricsState`;
- karaoke display/timing rendering;
- UI, cache, translation, and application composition.

## Migration note

SyncLRC is a **PRESERVE / REFACTOR** migration. Its narrow karaoke-only role is intentional. Do not broaden it into a generic plain/line provider merely because the API also exposes those payloads, and do not move karaoke rendering concerns into the provider.

Implementation is explicitly authorized on `feature/synclrc-provider-migration`. Follow `TASK.md` and `AGENTS.md`, complete bounded review, and stop before merge for explicit approval.
