# Bundled Changelog

## Branch and baseline

- Branch: `feature/bundled-changelog`
- Base: `main` at `3eb5d07ea2675497ae10bfb4fa372b02936ffa0f` (PR #58 merged).
- Classification: **SETTINGS / BUNDLED DOCUMENT / RELEASE PROCESS**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_SETTINGS.md`, `docs/RELEASES.md`, the existing bundled License/NOTICE implementation, and the current Settings root-reset contract.

## Goal

Make the repository-root `CHANGELOG.md` the canonical user-facing release history and display that exact document offline inside Settings.

The implementation should follow the existing bundled License/NOTICE ownership model:

```text
repository CHANGELOG.md
    -> app build copies generated asset
    -> :app reads bundled text
    -> SettingsScreenUiState
    -> ChangelogSettingsScreen
    -> PhoneMarkdownText
```

GitHub Releases remain the signed distribution channel and PR-level change ledger. The app Changelog is a concise curated history, not a runtime GitHub API surface.

## Acceptance criteria

- [x] Add repository-root `CHANGELOG.md` with the existing published releases in newest-first order.
- [x] Generalize the current bundled License/NOTICE asset task and include `CHANGELOG.md`.
- [x] Keep Android asset ownership in `:app`; `:ui:phone` receives presentation-ready Markdown text only.
- [x] Replace the Changelog loading/network scaffold with a read-only second-level Settings screen.
- [x] Reuse `SettingsSubscreenHeader` and `PhoneMarkdownText`.
- [x] Preserve System Back and Settings-tab root reselection behavior.
- [x] Remove obsolete Changelog loading/failure/retry presentation state and callbacks without changing Update behavior.
- [x] Align deterministic Changelog Previews and Settings mapper coverage.
- [x] Update `docs/PHONE_SETTINGS.md` and `docs/RELEASES.md`.
- [x] Require each release tag's version to match the newest version heading in `CHANGELOG.md` before release build/publish.
- [x] Keep GitHub Release `--generate-notes` behavior and the curated-summary-first / generated-notes-last policy unchanged.
- [x] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Validation

Completed before merge:

- [x] Branch name and commit-message validation passed.
- [x] Architecture boundary check passed.
- [x] Debug APK build passed.
- [x] JVM/unit tests passed.
- [x] CodeQL passed for Actions and Java/Kotlin.
- [x] Generated APK was inspected and contains `aalyrics_changelog.md`, `aalyrics_notice.txt`, and `aalyrics_license.txt`.
- [x] Bundled notice was verified as `Required Notice: © 2026 Yuta Miura`.
- [x] Codex review on the implementation head reported no major issues.
- [x] Release changelog guard was exercised directly with the workflow's extraction logic:
  - matching version `0.2.0-alpha.1` -> pass / exit 0;
  - mismatching version `0.3.0-alpha.1` -> fail / non-zero exit.

## Release contract

Before creating a release tag:

1. add the new release entry to `CHANGELOG.md`;
2. merge that change into `main`;
3. tag that exact `main` revision;
4. release CI verifies the newest changelog version matches the tag;
5. the signed APK bundles the same checked-in changelog;
6. GitHub Release generation continues independently with generated PR notes retained at the bottom.

The release workflow must fail rather than publish an APK whose bundled Changelog does not contain the tagged version as its newest release entry.

## Scope guard

Do not implement the unfinished in-app update/download runtime in this slice. Do not add GitHub API access to `:ui:phone`, change the Markdown renderer, alter release signing, or redesign unrelated Settings behavior.
