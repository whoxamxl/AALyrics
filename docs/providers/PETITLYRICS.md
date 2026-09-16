# PetitLyrics Provider Profile

## Status

- Migration status: **PLANNED — not yet migrated to `main`**
- Default migration order: second concrete provider
- Working-fork baseline: `8484bed2dbe8db5ca7b17dec5481b3c22714dc6f` (`v1.13.0`)
- Reference files: `PetitLyricsClient.kt`, `PetitLyricsClientTest.kt`

This file records PetitLyrics-specific behavior only. Shared provider rules are defined in `docs/PROVIDER_ARCHITECTURE.md`.

## Capabilities

The mature provider supports:

- line-synchronized lyrics (`lyricsType=2 / LSY`),
- word-synchronized lyrics (`lyricsType=3 / WSY`),
- type-1 plain text as a companion payload used to reconstruct line-synchronized output.

Expected AALyrics descriptor capability: `LINE`, `WORD`.

## Immutable configuration

The following existing configuration values must not be changed during migration unless the user explicitly requests it:

- `PETITLYRICS_USER_ID`
- `PETITLYRICS_APP_NAME`
- `PETITLYRICS_PKG_NAME`
- `PETITLYRICS_CLIENT_APP_ID`

Migration may refactor configuration injection/ownership, but the values themselves are an invariant.

## Configuration source

PetitLyrics configuration values are not stored in source control.

Local development reads the four keys from repository-root `secrets.properties.local`, which is ignored by the existing `*.properties.local` rule. CI supplies the same key names through GitHub Actions repository secrets, exposed to the build as environment variables. When both are available, environment variables take precedence over the local file.

Never commit or log the actual values. Tests should use injected/fake configuration and must not require live PetitLyrics credentials.

## Mature discovery behavior to preserve

PetitLyrics metadata often differs in script/romanization from playback metadata. The working provider therefore searches progressively:

```text
title + artist + album
        ↓
title + artist
        ↓
title only
```

It ranks/validates candidates locally rather than trusting result order, deduplicates attempted candidates, and records whether an artist-constrained query corroborated the selected result.

## Payload handling

- `lyricsType=3`: decode the WSY payload and preserve real word timing.
- `lyricsType=2`: pair the line-sync timing payload with a `lyricsType=1` plain-text companion for the same lyrics item.
- Prefer companion lookup by lyrics ID; retain the proven metadata fallback when ID lookup is unavailable or fails.
- Reject unusable payloads rather than emitting fake synchronized capability.

## Regression-sensitive behavior

Preserve existing tests/semantics around:

- WSY word timing,
- LSY + type-1 companion reconstruction,
- lyrics-ID companion resolution and metadata fallback,
- Japanese/romanized metadata matching,
- candidate ranking rather than first-result acceptance,
- provider metadata corroboration evidence,
- malformed/Base64/XML/payload handling,
- no usable synchronized candidate behavior.

## AALyrics ownership

Provider-local:

- PetitLyrics request/auth/config protocol,
- provider-native candidate search,
- Base64/XML/provider payload decoding,
- WSY/LSY parsing,
- line-sync companion lookup,
- local candidate validation and normalization.

Outside PetitLyrics:

- final cross-provider ranking,
- source-confidence policy,
- application state/fan-out,
- Android media/session behavior,
- UI, cache, and translation.

## Migration note

PetitLyrics is a **PRESERVE / REFACTOR** migration. Do not simplify away mature fallback or decoding behavior merely to make the adapter smaller. Structural cleanup is welcome only when regression semantics remain covered.
