# Musixmatch Provider Profile

## Status

- Migration status: **PLANNED — not yet migrated to `main`**
- Default migration order: third concrete provider
- Working-fork baseline: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Reference files: `MusixmatchClient.kt`, `MusixmatchClientTest.kt`

This file records Musixmatch-specific behavior only. Shared provider rules are defined in `docs/PROVIDER_ARCHITECTURE.md`.

## Capabilities

The mature provider uses the anonymous mobile API flow and may return:

- line-synchronized subtitles,
- RichSync word-timed lyrics.

Expected AALyrics descriptor capability: `LINE`, `WORD`.

## Current transport/authentication behavior

The working implementation uses the mobile API path because the previously tested desktop endpoint produced unreliable/poisoned matches.

Current high-level flow:

```text
token.get
    ↓
short-lived anonymous user token
    ↓
macro.subtitles.get
    ↓
matcher metadata + subtitle / optional RichSync
    ↓
track.richsync.get fallback when required
```

The endpoint is unofficial and must remain isolated as a provider implementation detail. Failure of this provider must not destabilize the rest of the application.

## Mature validation behavior to preserve

- validate matched metadata before emitting a candidate,
- reject instrumental matches for a vocal request,
- use Spotify track identity as strong corroboration when the requested track already carries a valid Spotify reference,
- reject an explicit Spotify-ID conflict,
- prefer embedded RichSync when present,
- fall back to a dedicated RichSync request when metadata indicates it and the macro omitted the payload,
- fall back to line-synchronized subtitle content when RichSync is unavailable,
- report whether an artist-constrained request corroborated the result.

AALyrics must consume Spotify identity through normalized `TrackReference` data. The provider must not depend on Android `MediaSession` metadata or the old `SpotifyTrackIdentity` platform helper directly.

## Token/session ownership

Token acquisition, TTL handling, and provider session state belong to the Musixmatch adapter/supporting provider infrastructure. The old implementation's `SharedPreferences` storage choice is not an architectural requirement.

Do not move token/session state into `:core:lyrics` or UI modules.

## Regression-sensitive behavior

Preserve tests/semantics around:

- mobile macro query construction,
- token acquisition/refresh behavior,
- RichSync parsing and word timing,
- line-subtitle fallback,
- Spotify identity compatibility,
- instrumental rejection,
- metadata validation,
- malformed/missing macro sections,
- fallback RichSync retrieval.

## AALyrics ownership

Provider-local:

- anonymous Musixmatch mobile API transport,
- token/session mechanics,
- macro and RichSync calls,
- Musixmatch DTO/JSON parsing,
- provider-local metadata/Spotify-reference validation,
- RichSync/subtitle normalization.

Outside Musixmatch:

- final cross-provider ranking,
- application state and provider fan-out,
- Android media identity extraction,
- UI, global cache, and translation.

## Migration note

Musixmatch is a **PRESERVE / REFACTOR** migration, but its unofficial endpoint makes failure isolation and regression coverage particularly important. Preserve the proven mobile flow rather than switching APIs during migration unless that is separately researched and explicitly approved.
