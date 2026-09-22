# Settings About & Support

## Branch and baseline

- Branch: `feature/settings-about-support`
- Base: `main` at `6c1b7f956feecb8a7c01cda9e039804079ea09c3` (PR #59 merged).
- Classification: **SETTINGS / BUNDLED DOCUMENT / EXTERNAL SUPPORT HANDOFF**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_SETTINGS.md`, `docs/PHONE_UI_SPEC.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/UI_ARCHITECTURE.md`, and `.github/FUNDING.yml`.

## Goal

Refine the lower Phone Settings information architecture and add privacy/support surfaces without moving document, browser, or payment ownership into `:ui:phone`.

Target Settings order:

```text
APP
  Version / Check for updates
  Changelog                       >
  Source code              GitHub ↗

ABOUT & SUPPORT
  Privacy Policy                  >
  License                         >
  Support AALyrics                >

[standalone card]
  Advanced                        >

[AALyrics footer]
```

## Approved ownership

### Privacy Policy

`PRIVACY.md` will be the repository source of truth and will follow the existing bundled-document path:

```text
repository PRIVACY.md
    -> app build asset
    -> :app reads text
    -> SettingsScreenUiState
    -> PrivacyPolicySettingsScreen
    -> PhoneMarkdownText
```

The policy must describe actual AALyrics behavior; placeholder privacy claims are not acceptable.

### Support AALyrics

`Support AALyrics` is a native Settings subscreen with one external CTA: `Support on Buy Me a Coffee ↗`.

- `:ui:phone` renders the surface and emits a callback.
- `:app` owns Custom Tabs / external-browser launching.
- Buy Me a Coffee owns amount, message, authentication, payment, and completion.
- Keep the destination aligned with `.github/FUNDING.yml` (currently `whoxamxi`).
- No WebView checkout.
- No payment credentials or transaction state in AALyrics.
- No amount/message fields until an official supported prefill contract is intentionally adopted.
- Supporting AALyrics must not unlock features, content, badges, or entitlements.

## Acceptance criteria

### Documentation stage

- [x] Create the topic branch from current `main`.
- [x] Freeze the Settings grouping/order in `docs/PHONE_SETTINGS.md`.
- [x] Define Privacy Policy bundled-document ownership.
- [x] Define the Support AALyrics external-handoff boundary.
- [x] Align `docs/PHONE_UI_SPEC.md`, `docs/PHONE_RUNTIME_HOST.md`, `docs/UI_ARCHITECTURE.md`, and `docs/ROADMAP.md`.
- [x] Preserve Advanced as a standalone card and the branding footer as the final Settings content.
- [x] Preserve Settings subscreen Back and Settings-tab root-reset semantics in the target contract.

### Implementation stage

- [x] Audit current data/privacy behavior and add repository-root `PRIVACY.md`.
- [x] Bundle `PRIVACY.md` through the existing generated-asset path.
- [x] Expose `privacyPolicyText` through application-owned Settings presentation mapping.
- [x] Add `PrivacyPolicySettingsScreen` using `SettingsSubscreenHeader` + `PhoneMarkdownText`.
- [x] Split the current lower Settings layout into `APP` and `ABOUT & SUPPORT` without changing unrelated sections.
- [x] Move License visually under `ABOUT & SUPPORT` without changing its existing NOTICE/LICENSE behavior.
- [x] Add `Support AALyrics` native subscreen.
- [x] Refine the Support surface into one branded card using the official Buy Me a Coffee full logo and yellow CTA button; keep QR assets out of the phone UI.
- [x] Wire Buy Me a Coffee handoff through an app-owned Custom Tab / external-browser action.
- [x] Keep `Advanced` in its own existing card and keep the branding footer last.
- [x] Update deterministic Previews for Settings home, Privacy Policy, Support AALyrics, narrow width, and enlarged font.
- [x] Add/update focused tests for presentation mapping and Settings root-reset/subscreen behavior as needed.
- [x] Re-evaluate `Reset AALyrics`; document that no new persisted state is introduced by this slice unless implementation changes that assumption.
- [x] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Scope guard

Do not implement payment UI, embedded checkout, WebView payment flows, transaction tracking, donor entitlements, or undocumented Buy Me a Coffee URL-prefill behavior. Do not redesign unrelated Settings sections, Sync, provider behavior, Translation semantics, or Advanced functionality.

## Validation status

Implementation is complete on `feature/settings-about-support` and PR #60 is open for review.

Completed locally/by repository inspection:

- [x] Privacy behavior audited against current manifest, providers, persistence, and Translation implementation.
- [x] No new app-owned persisted state was introduced; the existing `Reset AALyrics` contract therefore requires no behavior change.
- [x] Branch-name, commit-message, and architecture checks passed in GitHub Actions.

Pending before merge:

- [x] Debug APK build passes.
- [x] JVM/unit tests pass.
- [x] Generated APK contains `aalyrics_privacy.md`.
- [x] PR diff/Preview alignment review is complete.
- [ ] Re-run CI and bounded Codex review after the Buy Me a Coffee visual refinement.

## Current stop point

Support AALyrics visual refinement is implemented. Re-run repository validation and bounded review before returning PR #60 to the explicit pre-merge approval gate.
