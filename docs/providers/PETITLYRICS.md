# PetitLyrics Provider Profile

## Status

- Migration status: **MIGRATED — merged in PR #20**
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

The following existing configuration values must not be changed unless the user explicitly requests it:

- `PETITLYRICS_USER_ID`
- `PETITLYRICS_APP_NAME`
- `PETITLYRICS_PKG_NAME`
- `PETITLYRICS_CLIENT_APP_ID`

## Configuration source

PetitLyrics configuration values are not stored in source control.

Local development reads the four keys from repository-root `secrets.properties.local`, which is ignored by the existing `*.properties.local` rule. Trusted `main` push builds supply the same key names through GitHub Actions repository secrets, exposed to the build as environment variables. Pull-request builds receive empty values so unreviewed code cannot read provider credentials. When both environment values and the local file are available, environment variables take precedence.

Never commit or log the actual values. Tests use injected/fake configuration and do not require live PetitLyrics credentials.

## Mature discovery behavior preserved

PetitLyrics metadata often differs in script/romanization from playback metadata. The migrated provider searches progressively:

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

Preserved tests/semantics cover:

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

## Implemented adapter and intentional adaptations

- `:provider:petitlyrics` exposes `PetitLyricsProvider`, implementing `LyricsProvider` with `LINE` and `WORD` capabilities. Configuration, HTTP client, endpoint, and app version metadata are injected for deterministic tests and later composition-root wiring.
- The four immutable configuration keys are generated into the app BuildConfig from environment variables first, then ignored root `secrets.properties.local`. Trusted `main` push CI exposes the same repository-secret names to build and test steps; pull-request CI uses empty values. Actual values are never committed or logged; configuration string output reports only completeness.
- Discovery preserves title+artist+album, title+artist, and title-only order, local metadata ranking, word-before-line tie preference, attempted-result deduplication, and artist-query corroboration evidence. When missing metadata makes adjacent discovery stages identical, the actual request fields are deduplicated so one unavailable response cannot add redundant timeout cycles.
- WSY preserves word text spacing, absolute start/end timing, timed blank lines, ordering, and duplicate-line removal. LSY preserves protection-key permutation, centisecond rollover, line/text pairing, blank-line markers, and malformed payload rejection.
- Type-2 companion text is resolved by lyrics ID first. Missing, unusable, or failed ID lookup falls back to provider-native metadata and locally ranks the type-1 candidates rather than trusting response order.
- Neutral metadata plausibility moved from the production selector into `:provider:matching` without changing its calculation. PetitLyrics keeps duration out of local/global matching because its response units are ambiguous.
- Individual request failures do not prevent the mature provider-local fallback sequence. When discovery exhausts without a usable candidate, the first operational failure is surfaced; coroutine cancellation cancels the in-flight OkHttp call. Malformed provider data remains a rejected result rather than an operational failure.
- Provider response metadata is normalized without inventing missing artist, album, or duration. Candidates missing the required title are rejected, and attribution retains the PetitLyrics lyrics ID.
- Application provider wiring remains separate work.
