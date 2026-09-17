# Timing and Calibration Architecture Foundation

## Purpose

Define the seam for future timing offset and calibration behavior without prematurely choosing a calibration algorithm, persistence scope, drift model, UI interaction, or synchronization-editing workflow.

The working fork contains `lyrics/KaraokeTiming.kt` and `util/SyncCalibration.kt`. Migration intent remains **PRESERVE / REFACTOR** after a current-fork re-check, with timing math separated from Android/UI rendering.

## Stable ownership rules

Provider/source timing is canonical input. User/session calibration produces a derived effective timing view; it does not rewrite provider truth in place.

```text
source lyrics timing
        +
calibration parameters
        ↓
effective timing
        ↓
karaoke / presentation projections
```

Rules:

- provider adapters normalize source timestamps but do not apply user calibration;
- source timestamps remain recoverable and comparable after calibration;
- timing/calibration logic must be pure or framework-independent wherever possible;
- Phone and Android Auto may expose calibration controls later, but they must not implement timing math independently;
- calibration changes must not trigger provider refetch or cross-provider reselection unless a later explicitly documented product rule requires it;
- playback position is an input to timing projection, not something calibration owns;
- timing state must not leak Android media objects or UI-framework types into pure logic.

## Canonical versus effective timing

AALyrics must preserve two concepts:

1. **source timing** — timestamps obtained from a provider/parser and normalized into AALyrics models;
2. **effective timing** — source timing transformed by current calibration policy for playback/rendering decisions.

The architecture must prevent code from silently replacing canonical source timestamps with calibrated values.

This distinction is important for:

- recalibration;
- provider comparison/debugging;
- cache stability;
- karaoke boundary calculations;
- reset-to-source behavior;
- future persistence/version migration.

## Calibration scope

A future implementation must explicitly choose the scope of calibration values rather than letting scope emerge from UI state.

Possible scopes include:

- transient session-level adjustment;
- track-specific adjustment;
- source/provider-specific adjustment;
- global playback/device adjustment;
- combinations with explicit precedence.

This foundation does not select one. Whatever scope is chosen must be represented outside surface-local UI state and must have deterministic precedence.

## Offset versus drift

Constant offset and playback drift are separate problems.

A first implementation may support only a constant offset. Future drift/rate correction must not require replacing the architecture boundary.

Conceptually:

```text
source timestamp
        ↓
calibration transform
        ↓
effective timestamp
```

The transform may later become richer than `timestamp + offset`, but callers should consume effective timing through one timing/calibration boundary rather than duplicating formulas.

## Relationship to karaoke

Timing owns timestamp transformation and calibration semantics. Karaoke owns semantic projection of timed lyrics at a playback position.

```text
canonical timed lyrics
        ↓
effective timing
        ↓
karaoke projection engine
        ↓
current line / word / progress facts
```

Karaoke must not reach back into provider DTOs or duplicate calibration math.

## Relationship to translation

Translation does not own or mutate timestamps. If translated text is later aligned to original lines or segments, it references the canonical/effective timing structure rather than creating an independent playback clock.

## Deferred decisions

Do not decide in this foundation slice:

- exact offset units/API types beyond normalized time semantics;
- calibration persistence location;
- global versus per-track precedence;
- drift/rate-correction algorithm;
- automatic calibration;
- tap-to-sync workflow;
- waveform/audio analysis;
- calibration UI;
- clamp behavior at negative/overflow timestamps;
- editing or saving corrected lyrics back to source/cache;
- interpolation rules for malformed or sparse timing.

## Future implementation gate

Before implementing timing/calibration:

1. re-check working-fork `KaraokeTiming`, `SyncCalibration`, related tests, and active call sites;
2. classify exact behaviors as PRESERVE / REFACTOR / DROP;
3. define source-versus-effective timing types and calibration scope;
4. specify boundary conditions at line/word starts and ends;
5. add deterministic tests for zero offset, positive/negative offset, reset, track supersession, and any selected precedence rules;
6. ensure calibration changes do not refetch providers by accident;
7. implement on a dedicated topic branch and stop before merge for approval.
