# Phone Details and Advanced Settings Implementation

## Branch and baseline

- Branch: `feature/phone-details-advanced`.
- Base: current `main` at `dbeafbdb46ae40f38eb5d3f754772394c9610252`.
- Classification: **PHONE DETAILS / ADVANCED SETTINGS IMPLEMENTATION**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_DETAILS.md`, `docs/PHONE_SETTINGS.md`, `docs/PHONE_UI_SPEC.md`, and `docs/UI_ARCHITECTURE.md`.
- Product direction was approved in PR #47. This slice implements that approved contract without expanding Sync or Karaoke behavior.

## Goal

Implement the Phone Details destination and the narrow Settings > Advanced extension:

```text
Settings
└─ Advanced >
   ├─ Debug
   │  └─ Verbose details                  [ON/OFF]
   └─ Experimental features
      └─ Karaoke mode                     [OFF, disabled]
```

`Verbose details` is an application-owned persisted presentation preference. It only controls whether the read-only Developer / Diagnostics section is shown in Details.

`Karaoke mode` remains a disabled, non-interactive affordance with no persisted state and no runtime wiring.

## Implementation acceptance criteria

### Planning / ownership
- [x] Start from current `main` on a topic branch.
- [x] Record this implementation plan before production changes.
- [ ] Preserve existing Settings and playback behavior.
- [ ] Keep provider lookup/scoring, timing, Translation execution, MediaSession selection, and Sync untouched.

### Advanced Settings
- [ ] Add an `Advanced` navigation row to Settings.
- [ ] Add a second-level Advanced Settings presentation.
- [ ] Add functional `Verbose details` switch.
- [ ] Persist Verbose Details outside `:ui:phone`.
- [ ] Add disabled `Karaoke mode` row with no callback/runtime behavior.
- [ ] Preserve back/navigation behavior without creating a fifth primary destination.

### Details
- [ ] Replace the Details placeholder with a production read-only screen.
- [ ] Add Phone-local Details presentation state.
- [ ] Show Track fields from authoritative current playback/track state.
- [ ] Show Lyrics provider display name, sync type, language, and line count when available.
- [ ] Show Developer / Diagnostics only when Verbose Details is enabled.
- [ ] Limit diagnostics to already-available framework-neutral provider/source IDs and normalized track references.
- [ ] Avoid stale previous-track metadata during loading/no-session states.
- [ ] Respect the shell Playback Surface bottom inset.

### Integration / validation
- [ ] Wire Settings and Details through the existing application/Phone composition boundary.
- [ ] Add deterministic Previews for normal/partial/verbose/Advanced states.
- [ ] Add focused JVM/UI-state tests where durable.
- [ ] Run architecture checks, unit tests, and debug APK build.
- [ ] Review the final diff and complete bounded Codex review.
- [ ] Stop before merge until explicit user approval.

## Commit plan

Keep commits small and single-purpose. Expected shape:

1. `docs: plan Phone Details and Advanced implementation`
2. application-owned Verbose Details preference
3. Advanced Settings presentation/navigation
4. Details presentation model/screen
5. runtime mapping/wiring
6. Preview/test coverage
7. documentation/status cleanup if implementation changes require it

The exact split may be adjusted to keep each commit coherent.

## Scope guard

Do not implement:

- functional Karaoke mode;
- Karaoke projection or WORD highlighting;
- provider preference changes;
- candidate scoring/ranking UI;
- extra provider/network diagnostics;
- log viewer/export;
- timing/calibration changes;
- Sync destination behavior;
- cache controls;
- unrelated Settings taxonomy;
- MediaSession selection-policy changes.
