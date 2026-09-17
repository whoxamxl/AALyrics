# Translation Architecture Foundation

## Purpose

Define the seam for future lyrics translation without selecting a translation provider, language-detection strategy, batching algorithm, persistence model, or UI contract too early.

The working fork contains `lyrics/LyricsTranslator.kt` and `lyrics/TranslationLanguages.kt`. Migration intent remains **REFACTOR** for translation orchestration and **PRESERVE** for stable language/config semantics after current-fork re-check.

## Stable ownership rules

Translation is a derived lyrics capability. It must not become part of provider retrieval, cross-provider candidate ranking, MediaSession handling, or surface-specific rendering.

Stable direction:

```text
canonical normalized lyrics
        ↓
translation capability
        ↓
translated lyrics artifact / status
        ↓
presentation projection
```

Rules:

- concrete lyrics providers do not call translation engines;
- `LyricsCoordinator` remains responsible for lookup/provider orchestration, not translation execution policy;
- translation engines/adapters live outside pure domain contracts when they require networking, SDKs, credentials, Android APIs, or other infrastructure;
- Phone and Android Auto consume translation results through application/presentation state rather than invoking a translator directly;
- translation failure must not destroy or replace valid original lyrics;
- translated content must remain associated with the exact canonical lyrics version from which it was derived;
- cancellation and supersession must prevent translation for obsolete track/lyrics ownership from publishing as current state.

## Original lyrics remain canonical

Translation must be additive rather than destructive.

```text
Canonical lyrics
├─ original text/timing
└─ derived translation
```

A translated artifact must not overwrite provider text or provider timing in place. The application must always be able to distinguish:

- original source content;
- target language;
- translation result/status;
- canonical lyrics identity/version used as input.

This keeps cache invalidation, timing, karaoke, debugging, and provider comparison tractable.

## Timing relationship

Translation and timing are separate concerns.

A translation may be associated with lines or semantic text units, but translation does not own source timestamps, calibration, playback position, or karaoke progression.

Future implementation may choose line-level alignment, segment mapping, or another representation. The stable invariant is that timing truth remains in the timing/lyrics domain and translated text references it rather than mutating it.

## Status and failure semantics

A future contract should make translation lifecycle explicit enough to distinguish states such as unavailable, disabled, pending, ready, and failed where product behavior needs that distinction.

The exact state model is deferred. What is fixed:

- valid original lyrics remain usable during translation work;
- a translator/network failure is not a lyrics lookup failure;
- a language change may invalidate/restart translation without refetching providers;
- obsolete translation results must be rejected when the underlying lyrics or target language changes.

## Cache relationship

Translation caching is logically separate from canonical lyrics caching.

A later implementation may cache translated artifacts, but they must be keyed to both canonical lyrics identity and translation configuration such as target language and any engine/version factors required for correctness.

Do not make translation persistence a reason to merge translation and lyrics cache into one opaque storage model.

## Deferred decisions

Do not decide in this foundation slice:

- translation service/provider/SDK;
- on-device versus remote translation;
- automatic language detection implementation;
- line-by-line versus batched requests;
- translation prompt/model configuration;
- retry/rate-limit strategy;
- exact translation state types;
- persistence or TTL;
- user-selectable language UI;
- whether word-level/karaoke text is translated;
- formatting policy for repeated/empty/instrumental lines.

## Future implementation gate

Before implementing translation:

1. re-check working-fork translator behavior, status handling, language configuration, and call sites;
2. classify behaviors as PRESERVE / REFACTOR / DROP;
3. define canonical lyrics identity and supersession behavior;
4. define a replaceable translation boundary independent of a concrete engine;
5. test original-lyrics preservation, cancellation, language changes, stale-result rejection, and partial/failed translation;
6. keep translation networking/infrastructure outside provider and UI modules;
7. implement on a dedicated topic branch and stop before merge for approval.
