# Phone Details and Advanced Settings Documentation Alignment

## Branch and baseline

- Branch: `docs/phone-details-advanced-alignment`.
- Base: `main` at `ebc1df503a6fc112b0ea14197daf30f4241d6e73` (PR #46 merged).
- Classification: **PHONE UI DOCUMENTATION / DETAILS + ADVANCED SETTINGS CONTRACT**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_UI_SPEC.md`, `docs/PHONE_SETTINGS.md`, `docs/UI_ARCHITECTURE.md`, and the new `docs/PHONE_DETAILS.md`.
- This branch is documentation-only. Production implementation is not authorized by this slice.

## Goal

Align Phone documentation with the post-PR #46 implementation state and freeze the next presentation contracts without prematurely implementing Sync or Karaoke behavior.

The approved direction is:

```text
Settings
└─ Advanced >
   ├─ Debug
   │  └─ Verbose details                  [ON/OFF]
   └─ Experimental features
      └─ Karaoke mode                     [OFF, disabled]
```

`Verbose details` controls only the amount of read-only diagnostic information shown by the Details destination. It must not change provider lookup, candidate selection, timing, translation, playback, or lyrics rendering behavior.

`Karaoke mode` is a future/experimental affordance only in this stage. It remains OFF and disabled, with no runtime setting, callback, provider-selection effect, WORD-sync preference change, or karaoke rendering wiring.

## Documentation acceptance criteria

- [x] Create a topic branch from current `main`.
- [x] Replace stale pre-PR #46 playback-surface status text with the merged implementation state.
- [x] Define the first Phone Details contract in `docs/PHONE_DETAILS.md`.
- [x] Define Normal Details versus Verbose Details.
- [x] Keep machine-facing provider/source identifiers out of the normal Details surface.
- [x] Add `Settings > Advanced` with Debug and Experimental sections.
- [x] Define `Verbose details` as presentation-only behavior.
- [x] Define `Karaoke mode` as disabled/unwired future UI only.
- [x] Keep Sync behavior explicitly deferred.
- [x] Align UI architecture and roadmap status with merged Phone work.
- [x] Review the documentation diff for contradictory ownership or implementation claims.
- [x] Stop before implementation and merge until explicitly authorized.

## Scope guard

Do not implement or wire:

- `DetailsScreen`;
- Advanced Settings Compose UI;
- persistence for Verbose Details;
- Karaoke mode state or callbacks;
- WORD-level rendering changes;
- provider-selection preference changes;
- timing/calibration behavior;
- Sync destination behavior;
- new provider diagnostics collection;
- logging/export infrastructure.

The documentation may name presentation data already available from framework-neutral domain/application state, but it must not require UI code to consume provider DTOs or Android media framework objects directly.
