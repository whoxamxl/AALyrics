# AALyrics Provider Architecture and Migration Policy

## Purpose

This document defines the rules shared by all concrete lyrics providers and the practical migration policy for bringing the mature provider implementations from `whoxamxl/auto-lyrics` into AALyrics.

Provider-specific behavior belongs in concise profiles under `docs/providers/`. Those profiles are implementation notes and migration references, not separate architectures.

## Authority order

When migration decisions conflict, use this order:

```text
AALyrics architecture and contracts
        ↓
proven provider behavior and regression tests in the working fork
        ↓
legacy implementation structure
```

The working fork is mature provider code written and evolved for this project. Provider migration is therefore normally **PRESERVE / REFACTOR**, not a clean-room rewrite and not a requirement to re-derive already-proven behavior from scratch.

Reuse/refactor mature algorithms, parsing behavior, query strategy, thresholds, compatibility handling, and regression cases where they remain correct. Change structure where needed to fit AALyrics boundaries. Do not preserve legacy ownership merely because a behavior historically lived in one large client or `MediaTracker`.

## Shared provider contract

Every concrete provider implements `LyricsProvider` from `:provider:api`.

Runtime responsibility is intentionally narrow:

```text
LyricsRequest
    ↓
provider-local transport / authentication
    ↓
provider-local discovery / fallback
    ↓
provider-local parsing / validation
    ↓
normalized LyricsCandidate values
    ↓
LyricsCoordinator
    ↓
CandidateSelector
```

A provider may own:

- HTTP transport and provider authentication/session mechanics,
- provider-native query construction,
- provider-native fallback/discovery order,
- provider response DTOs,
- provider payload parsing,
- rejection of malformed or provider-invalid responses,
- provider-local evidence learned during discovery,
- normalization into `Track`, `LyricsDocument`, attribution, and `LyricsCandidate`.

A provider must not own:

- the final winner across providers,
- global source-confidence policy,
- application `LyricsState`,
- playback ownership,
- phone or Android Auto presentation,
- translation,
- global cache policy,
- Android `MediaSession` / `MediaController` types.

## Playback source is not a lyrics provider

A playback application/source and a lyrics provider are separate architectural concepts.

A playback source may contribute stable identity or metadata that improves provider matching without itself being a lyrics source. Spotify is the current example: AALyrics may normalize a Spotify track resource into `TrackReference(namespace = "spotify", ...)` in `:platform:media`, and a concrete lyrics provider may use that reference as corroborating query/match evidence.

```text
Spotify playback
    ↓
:platform:media extracts Spotify TrackReference
    ↓
LyricsRequest / normalized Track identity
    ↓
concrete lyrics providers may consume that reference
    ↓
lyrics still come from that provider
```

This does **not** make Spotify a `LyricsProvider`. The current working fork has no independent Spotify lyrics client/provider; Musixmatch uses Spotify track identity to strengthen matching while the lyric payload still comes from Musixmatch.

Do not create a `SpotifyProvider` merely because Spotify identity is available. A future direct Spotify lyrics source would be a separate provider decision with its own profile and explicit implementation authorization.

## Candidate and evidence rule

Providers return facts, not global scores.

A provider may report provider-neutral evidence already represented by the provider API, such as whether an artist-constrained query independently corroborated a candidate. The production selector decides how that evidence affects cross-provider ranking.

Do not add a provider-specific field to `:provider:api` merely because one service exposes it. First ask whether the fact has provider-independent meaning and whether selection/orchestration actually needs it.

## Matching ownership

There are two distinct matching concerns:

1. **provider-local discovery validation** — whether a response returned by one provider plausibly matches the requested track;
2. **cross-provider selection** — which normalized candidate wins after providers have completed.

The second belongs to `:provider:selection` behind the `CandidateSelector` port.

Generic title/artist/duration/recording-version semantics should not be copied independently into every provider. If multiple provider adapters genuinely need the same pure matching semantics, extract a neutral pure utility boundary that both provider adapters and `:provider:selection` may use. Do not make concrete providers depend on the production selector merely to reuse helper functions.

## Result and failure semantics

Concrete providers follow the shared contract consistently:

- no acceptable lyrics found: return an empty candidate list,
- malformed/unusable provider result: reject that result and continue provider-local fallback when appropriate,
- operational/network/service failure: surface an exception,
- coroutine cancellation: propagate cancellation and cancel underlying work where practical.

Do not convert every operational failure into "not found" inside the provider. `LyricsCoordinator` already owns provider failure isolation.

## Parsing and timing

AALyrics domain timing is milliseconds.

Provider-native timing formats are parsed at the provider/parser boundary and normalized to `TimedLyricLine` / `TimedWord`. Preserve mature parser semantics and regression cases where they already exist; do not rewrite a stable parser solely to make the migration look new.

A provider must only advertise synchronization capabilities that it can actually return through the normalized contract.

## Migration style

Concrete providers are migrated one at a time. For a mature provider, the default workflow is:

```text
re-check working-fork main + provider tests
        ↓
update/read provider profile
        ↓
PRESERVE / REFACTOR mature implementation into AALyrics boundaries
        ↓
port regression coverage
        ↓
verify normalized provider behavior
        ↓
CI + bounded review
        ↓
STOP before merge
```

Do not add an artificial "rewrite from behavior only" step when the existing provider code is already the implementation we intend to preserve. Conversely, do not bulk-copy an old file if that would reintroduce legacy coupling to Android UI, `MediaTracker`, global state, or cross-provider ranking.

The correct unit of preservation is **working behavior plus regression knowledge**; the correct unit of structural change is **AALyrics ownership**.

## Provider slice scope

A provider PR should normally be complete enough to be meaningful on its own: transport/authentication, provider-local search/fallback, parsing, normalization, and provider-specific regression tests may live in one bounded provider slice when that is clearer than landing disconnected scaffolding.

Split a provider into multiple PRs only when a component is independently useful or the provider is too large to review safely. Do not create ceremony solely to make the migration appear more granular.

## Provider profiles

Provider-specific notes live here:

- `docs/providers/LRCLIB.md`
- `docs/providers/PETITLYRICS.md`
- `docs/providers/MUSIXMATCH.md`
- `docs/providers/SYNCLRC.md`

Each profile records capabilities, current working-fork behavior, provider-local quirks/invariants, AALyrics ownership decisions, regression-sensitive behavior, and migration status. The profile should stay concise; shared rules belong in this document.

## Default migration order

Unless current evidence suggests a better order immediately before implementation:

```text
LRCLIB
  ↓
PetitLyrics
  ↓
Musixmatch
  ↓
SyncLRC
```

This is an execution preference, not an architectural dependency. Re-check the current fork and provider value/complexity before each concrete migration slice.

## PetitLyrics immutable configuration

The following existing configuration values must not be changed during migration unless the user explicitly requests a change:

- `PETITLYRICS_USER_ID`
- `PETITLYRICS_APP_NAME`
- `PETITLYRICS_PKG_NAME`
- `PETITLYRICS_CLIENT_APP_ID`

Migration may change where configuration is injected or stored, but not these configured values themselves.

## Authorization boundary

Documentation/planning approval is not implementation approval.

Creating or updating provider profiles, discussing migration order, or approving this architecture document must never be interpreted as permission to begin concrete provider source migration. Start a provider implementation slice only after the user explicitly moves the work from planning/documentation into implementation.

Likewise, experimental provider branches are not canonical migration input merely because they exist. A future provider slice starts from current `main` and the approved provider profile unless the user explicitly authorizes reuse of experimental work.
