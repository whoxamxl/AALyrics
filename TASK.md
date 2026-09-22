# In-app Update Check

## Branch and baseline

- Branch: `feature/check-for-updates`.
- Base: `main` at `fefa59c3cc2bb22dda6d75f859172ad797357d34`.
- Classification: **SETTINGS / RELEASE NETWORK / VERSION COMPARISON**.
- Authoritative references: `AGENTS.md`, `docs/RELEASES.md`, `docs/PHONE_SETTINGS.md`, the current GitHub Release workflow, and the existing Settings update-state presentation.

## Goal

Make **Check for updates** functional without expanding this slice into APK download, checksum verification, Package Installer handoff, background update polling, or a user-configurable update channel.

The slice owns exactly three runtime capabilities:

1. parse and compare AALyrics release versions;
2. read the public AALyrics GitHub Releases feed through an application-owned client;
3. map an explicit user-triggered check into the existing Phone Settings presentation lifecycle.

## Version contract

Published AALyrics releases use only:

```text
MAJOR.MINOR.PATCH
MAJOR.MINOR.PATCH-alpha.N
MAJOR.MINOR.PATCH-beta.N
MAJOR.MINOR.PATCH-rc.N
```

A leading `v` is accepted when parsing GitHub tag names and removed before comparison.

Development builds additionally use:

```text
<release-version>-dev+<short-sha>
<release-version>-dev+<short-sha>.dirty
<release-version>-dev
```

The final form is the existing Git-metadata-unavailable fallback; it still compares as its embedded base release version.

The development suffix is build identity, not a release precedence level. For update comparison, a development build compares as its embedded base release version.

Ordering is:

```text
alpha.N < beta.N < rc.N < stable
```

after comparing `MAJOR`, `MINOR`, and `PATCH` numerically. Sequence numbers within alpha/beta/rc are also numeric.

Examples:

```text
0.2.0-alpha.1 < 0.2.0-alpha.2
0.2.0-alpha.2 < 0.2.0-beta.1
0.2.0-beta.1  < 0.2.0-rc.1
0.2.0-rc.1    < 0.2.0
0.2.0-alpha.1-dev+abcdef0 == 0.2.0-alpha.1 for update comparison
```

Malformed installed versions are a check failure rather than being guessed into an ordering. Malformed or unrelated GitHub Release tags are ignored.

## Release selection contract

GitHub Releases is the authoritative update-discovery source.

The client reads the repository's public Release collection rather than relying on GitHub's single "latest release" concept, because AALyrics must also discover alpha/beta/RC releases.

Candidate rules:

- ignore Draft releases;
- ignore tags outside the AALyrics version grammar;
- select by parsed version precedence, not publication timestamp alone;
- a stable installed build considers only stable releases eligible;
- an alpha/beta/rc installed build considers prerelease and stable releases eligible;
- a development build uses the channel of its embedded base version: stable-base dev builds consider stable only, prerelease-base dev builds consider prerelease and stable;
- no GitHub token, repository secret, or other credential is embedded in the APK.

A successful feed response with no newer eligible version maps to `UP_TO_DATE`. A transport/protocol failure, malformed installed version, or a feed from which no comparable AALyrics release can be established maps to `CHECK_FAILED`.

## Phone presentation contract

This slice makes only the check lifecycle production-reachable:

```text
IDLE
CHECKING
UP_TO_DATE
UPDATE_AVAILABLE
CHECK_FAILED
```

`DOWNLOADING`, `DOWNLOADED`, and `DOWNLOAD_FAILED` remain reserved for the follow-up download/integrity slice.

Presentation:

```text
Version                v0.2.0-alpha.1-dev+abcdef0
                              Check for updates

Checking for updates…                         ◌

Up to date                                    ✓

Update available: v0.2.0-alpha.2

Update check failed                 ⓘ   ↻ Retry
```

During this PR, `UPDATE_AVAILABLE` is informational only. Do **not** show an enabled Download action that still routes to a no-op callback.

The check begins only from the explicit `Check for updates` / `Retry` action. Entering Settings, launching AALyrics, or returning to the foreground must not automatically contact GitHub.

Only one check may be active at a time. Active work is application-owned and survives leaving Settings; completed results remain visit-local and normalize back to `IDLE` on the next Settings entry under the existing short-lived result contract.

## Ownership

- `:ui:phone` owns presentation only and emits callbacks.
- `:app` owns the runtime orchestration and lifecycle.
- Release HTTP access belongs behind an application-owned release client boundary.
- Version parsing/comparison must remain Android-independent and directly unit-testable.
- No provider, playback, Translation, Android Auto, Changelog, legal/help, or release-publishing behavior changes in this slice.

## Acceptance criteria

### Documentation / contract

- [x] Create `feature/check-for-updates` from current `main`.
- [x] Replace the stale `TASK.md` with this slice's scope.
- [x] Define release-version parsing and precedence.
- [x] Define development-version comparison semantics.
- [x] Define stable/prerelease eligibility.
- [x] Define the explicit-user-action network policy.
- [x] Define Check-only presentation and defer Download.
- [x] Align `docs/RELEASES.md` and `docs/PHONE_SETTINGS.md`.
- [x] Re-evaluate `Reset AALyrics`: this slice introduces no persisted preference or downloaded asset, so reset semantics remain unchanged.

### Implementation

- [x] Add an Android-independent AALyrics version parser/comparator with focused tests.
- [x] Add an application-owned GitHub Release client/model boundary.
- [x] Select the highest eligible non-Draft AALyrics release.
- [x] Wire `onCheckForUpdates` to application-owned check orchestration.
- [x] Replace the production `UNAVAILABLE` update state with `IDLE`.
- [x] Map checking, up-to-date, available, and failure results into `SettingsScreenUiState`.
- [x] Keep `UPDATE_AVAILABLE` informational until download support exists.
- [x] Add focused mapping/lifecycle/presentation tests and update deterministic Previews where required.
- [x] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Scope guard

Do not implement APK download, `.sha256` retrieval/verification, Package Installer integration, signing-certificate verification, background/periodic update checks, notifications, automatic checks on Settings entry, authentication/token storage, update-channel Settings, or persisted update results in this PR.

Those are separate implementation slices after Check for updates is proven stable.

## Current stop point

The Check-only runtime is wired end to end and validated: version comparison, GitHub Release discovery, candidate selection, application-owned lifecycle, Settings mapping/callbacks, informational UPDATE_AVAILABLE presentation, focused tests, Preview alignment, synchronized-main CI, Codex review, and real-device verification are complete.
