# Android Auto Compatibility Onboarding

## Branch and baseline

- Branch: `feature/android-auto-compat-onboarding`.
- Base: `feature/notification-access-onboarding` / PR #37.
- Classification: **OPTIONAL COMPATIBILITY ONBOARDING**.
- Authoritative references: `AGENTS.md`, Android Auto testing guidance, and the existing legacy `MediaBrowserServiceCompat` fallback.
- The user explicitly authorized implementation.

## Goal

Explain and record the Android Auto `Unknown sources` setup that is required only when AALyrics falls back to the legacy sideloaded MediaBrowserService path.

This is not a hard permission gate:

```text
Notification Access missing
        -> required setup

Notification Access granted
        -> Android Auto compatibility not reviewed
              -> compatibility setup
                 -> "I've enabled Unknown sources"
                    OR
                    "Continue without it"
              -> normal app content

Compatibility already reviewed
        -> normal app content
```

## Acceptance criteria

- Keep Notification Access as the only blocking system-access gate.
- Present Android Auto compatibility setup after Notification Access is granted and before normal phone content on first review.
- Explain that `Unknown sources` is for the legacy sideloaded media fallback, while Car App Library templated media does not require that Android Auto setting.
- Show concise Developer Mode / Developer settings / Unknown sources instructions based on Android's documented flow.
- Do not claim AALyrics can verify the Android Auto setting; Android Auto exposes no public app API for that state.
- Provide two explicit actions: `I've enabled Unknown sources` and `Continue without it`.
- Persist the user's explicit choice as `ENABLED` or `SKIPPED` so the onboarding is not shown repeatedly.
- Keep persistence and entry-flow policy in `:app`; keep the Compose presentation in `:ui:phone`.
- Add deterministic JVM coverage for onboarding-state semantics.
- Add typical, narrow, and enlarged-font Compose Previews using the production screen.
- Do not change MediaBrowserService, Car App Library, provider, lyrics, or automotive runtime behavior in this slice.
- Run CI/review and open a stacked PR against PR #37's branch; stop before merge.

## Planned work

- [x] Prepare stacked branch and task scope.
- [x] Add compatibility acknowledgement state/persistence boundary.
- [x] Add Compose compatibility setup screen.
- [x] Add setup Previews.
- [x] Integrate the optional setup into application entry.
- [x] Add deterministic tests.
- [x] Review diff / CI and open stacked PR.
- [x] Consolidate the 2026-09-19 Android Auto media / sideload / fallback decisions into durable project documentation.

## Scope guard

This slice provides user-facing setup guidance only. It does not yet migrate AALyrics to Car App Library 1.8.x, add `CarAppService`, or change the existing legacy MediaBrowserService fallback.
