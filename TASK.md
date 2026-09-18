# Phone Visual Refinement

## Branch and baseline

- Branch: `feature/phone-visual-refinement`.
- Base: `main` at `cd5df721803cc1eec67740ea64bcc1e7d85af7ce` after PR #34 merged.
- Classification: **PHONE SHELL VISUAL REFINEMENT**.
- Authoritative references: `AGENTS.md`, `docs/UI_ARCHITECTURE.md`, and `docs/PHONE_UI_SPEC.md`.
- The user explicitly authorized Phone visual refinement with small, single-purpose commits.

## Goal

Refine the already-implemented persistent Phone shell from structural placeholders into the approved AALyrics visual direction while preserving the existing presentation/runtime boundaries.

The shell remains:

```text
PhoneAppShell
├─ PhoneTopBar
├─ CurrentDestination
├─ PlaybackControlsBar
└─ PhoneNavigationBar
```

This slice should improve the shell itself and its production component Previews. The real Lyrics destination, Track Card, Lyrics viewport behavior, ViewModels, navigation runtime, and media wiring remain later work.

## Acceptance criteria

- Replace the temporary `A` badge in `PhoneTopBar` with an AALyrics brand mark owned by `:ui:designsystem`.
- Keep the top bar compact, preserve status-bar inset handling, and use its right-side pill for the connected/monitored media source without adding media-session policy to UI.
- Replace text-only Previous / Play-Pause / Next controls with icon-first transport controls and keep valid touch targets plus disabled-state presentation.
- Replace temporary bottom-navigation line markers with destination icons for Lyrics / Sync / Details / Settings.
- Preserve the four approved destinations, Lyrics as home, and caller-owned destination composition.
- Continue using AALyrics semantic theme tokens instead of ad-hoc visual constants where practical.
- Keep ordinary visual Previews deterministic and render the real production composables.
- Verify typical and narrow Preview layouts still prioritize destination/lyrics vertical space.
- Do not introduce MediaSession/MediaController references, ViewModels, provider/network work, Navigation runtime, real Lyrics-screen behavior, or Android Auto changes.
- Keep commits small and single-purpose.
- Run repository validation, review the complete diff, open a PR, and stop before merge for explicit approval.

## Planned commits

- [x] Prepare Phone visual-refinement task and branch.
- [x] Add shared AALyrics shell icon assets/APIs.
- [x] Refine `PhoneTopBar` and assign the right-side pill to media-source identity.
- [x] Refine `PlaybackControlsBar`.
- [x] Refine `PhoneNavigationBar`.
- [ ] Re-check shell Previews and narrow layouts.
- [x] Align durable UI docs only where implementation decisions became stable.
- [ ] Run validation and review.
- [ ] Open PR and stop before merge.

## Scope guard

This branch refines the visual presentation of the existing Phone shell. It must not turn into the Lyrics-screen implementation branch. If a visual change requires real destination state/behavior, record it for the next slice rather than adding that behavior here.
