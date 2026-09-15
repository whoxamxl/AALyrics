# SyncLRC Provider Profile

## Status

- Migration status: **PLANNED — not yet migrated to `main`**
- Default migration order: fourth concrete provider
- Working-fork baseline: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Reference files: `SyncLrcClient.kt`, `SyncLrcClientTest.kt`, `LrcParser.kt`

This file records SyncLRC-specific behavior only. Shared provider rules are defined in `docs/PROVIDER_ARCHITECTURE.md`.

## Capability and role

SyncLRC is intentionally treated as a karaoke/word-timing provider, not as a second source for ordinary plain or line-synchronized lyrics already covered elsewhere.

Expected AALyrics descriptor capability: `WORD`.

## Mature request behavior to preserve

The current provider requests the public API with:

- track,
- artist,
- `type=karaoke`,
- album when known,
- duration when known.

A blank title or artist is not enough to make a useful karaoke request and should not be sent as if it were complete metadata.

## Response compatibility

The current API may expose separate `karaoke`, `synced`, and `plain` fields. Older deployments used a type-specific `lyrics` + `type` shape.

Preserve compatibility with both response shapes, but only emit a candidate when a **genuine karaoke payload with timed word tokens** is present.

A `type=karaoke` request that returns only line-synchronized or plain lyrics is not promoted to a SyncLRC candidate. Those formats are deliberately left to ordinary providers such as LRCLIB.

## Validation and normalization

- reject instrumental responses,
- parse Enhanced-LRC karaoke content into timed lines/words,
- require actual word timing; line timing alone is insufficient,
- preserve provider metadata when supplied,
- provider-neutral artist-query corroboration may be reported when the request included artist evidence,
- normalize timing to milliseconds.

## Regression-sensitive behavior

Preserve tests/semantics around:

- current `karaoke` response field,
- legacy `lyrics` + `type=karaoke` response shape,
- rejection of synced/plain-only responses,
- rejection of instrumental results,
- requirement for timed word tokens,
- Enhanced-LRC parsing,
- metadata/duration request parameters.

## AALyrics ownership

Provider-local:

- SyncLRC HTTP request/DTO handling,
- response-shape compatibility,
- karaoke-only acceptance policy,
- Enhanced-LRC parsing and normalization,
- provider-local validation.

Outside SyncLRC:

- deciding whether a WORD candidate beats a strong LINE candidate,
- final cross-provider ranking/source confidence,
- application state/provider fan-out,
- UI, cache, and translation.

The WORD-vs-LINE preference remains the responsibility of the production `CandidateSelector`.

## Migration note

SyncLRC is a **PRESERVE / REFACTOR** migration. Its narrow karaoke-only role is intentional. Do not broaden it into a generic plain/line provider during migration merely because the API also exposes those payloads.
