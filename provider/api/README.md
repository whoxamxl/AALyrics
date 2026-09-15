# `:provider:api`

Pure Kotlin contracts shared by all lyrics providers.

Providers are discovery adapters. They may query remote services, normalize provider-specific data, and return plausible candidates, but they do **not** choose the cross-provider winner.

## Contract

- `LyricsProvider` exposes static provider metadata and a suspending `search` function.
- `LyricsRequest` contains only provider-independent track metadata and an optional timing preference.
- `LyricsCandidate` contains the provider identity, the provider's matched track metadata, and a normalized `LyricsDocument`.
- An empty candidate list means "no usable result".
- Operational failures are surfaced as exceptions. Shared timeout/retry/error handling belongs in orchestration, not in individual provider contracts.
- Final scoring and selection belong in `:core:lyrics`.

Concrete providers must depend on this module and `:core:model`, never on phone or automotive UI modules.
