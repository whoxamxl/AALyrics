# AALyrics Release Policy

## Purpose

AALyrics is distributed outside Google Play. GitHub Releases are the durable distribution channel for signed Android APKs.

A release is a versioned, signed, installable distribution snapshot. It does **not** imply that the application is feature-complete. Early development builds may be published intentionally as GitHub Pre-releases so signing, installation, Android Auto discovery, update compatibility, and real-device behavior can be validated before the product is ready for a stable release.

## Distribution channels

### Development artifacts

Pull-request and `main` CI may upload debug APK workflow artifacts.

These artifacts are for development/testing only:

- they use the Android debug signing identity;
- they are not the durable user distribution package;
- they are not expected to update a release-signed installation.

When `AALYRICS_VERSION_NAME` is not injected, development builds derive Android
`versionName` from the latest reachable release tag and the current short Git commit.
For example, a clean development build after `v0.2.0-alpha.1` may report
`0.2.0-alpha.1-dev+89906f2`. A local build with tracked or untracked working-tree
changes appends `.dirty`. If Git metadata is unavailable, the build falls back to
`0.1.0-dev`.

Release builds are unaffected by this fallback: the Release workflow continues to
inject the tag-derived `AALYRICS_VERSION_NAME` explicitly.

### GitHub Pre-releases

Development milestones use semantic-version-style prerelease tags:

- `v0.1.0-alpha.1`
- `v0.1.0-beta.1`
- `v0.1.0-rc.1`

Any accepted release tag containing a prerelease suffix is published with GitHub's **Pre-release** flag.

The initial signed distribution, `v0.1.0-alpha.1`, has been published successfully as a GitHub Pre-release. It is intentionally functionally incomplete; its immediate purpose is to validate the durable signing/distribution path and provide an installable baseline.

### Stable releases

A tag with no prerelease suffix, such as `v0.1.0`, is published as a normal GitHub Release.

Stable tags are reserved for builds that are intentionally presented as generally usable AALyrics releases. Moving from an alpha/beta/RC build to stable is a product decision, not merely a CI milestone.

## Version tags

The Release workflow accepts only the stable tag form or one of the three defined prerelease channels:

```text
vMAJOR.MINOR.PATCH
vMAJOR.MINOR.PATCH-alpha.N
vMAJOR.MINOR.PATCH-beta.N
vMAJOR.MINOR.PATCH-rc.N
```

Examples:

```text
v0.1.0-alpha.1
v0.1.0-beta.1
v0.1.0-rc.1
v0.1.0
```

Release tags must point to commits contained in `main`.

Do not move or reuse a tag after a release has been published. If a published build is wrong, fix the problem and create a new version tag.

The tag without the leading `v` becomes Android `versionName`. The Release workflow run number is used as Android `versionCode`, providing increasing update ordering across this workflow's releases.

## Signing identity

All durable AALyrics release APKs must be signed by the same release key.

The release keystore must never be committed to Git. The repository ignores common keystore extensions, but that is only a guardrail; the canonical keystore should remain outside the repository and be backed up securely.

Required GitHub Actions repository secrets:

```text
AALYRICS_RELEASE_KEYSTORE_BASE64
AALYRICS_RELEASE_STORE_PASSWORD
AALYRICS_RELEASE_KEY_ALIAS
AALYRICS_RELEASE_KEY_PASSWORD
```

The existing PetitLyrics production credentials are also required by the Release workflow:

```text
PETITLYRICS_USER_ID
PETITLYRICS_APP_NAME
PETITLYRICS_PKG_NAME
PETITLYRICS_CLIENT_APP_ID
```

The workflow fails instead of publishing a release when required signing or provider credentials are missing.

### Keystore backup rule

Loss of the release private key or its passwords can prevent future APKs from updating existing release installations. Keep the original keystore and credentials backed up independently from GitHub Secrets.

Do not rotate the release key casually. A signing-key change requires an explicit migration decision and must not happen as an incidental CI/configuration change.

## Creating the keystore

Example with the JDK `keytool`:

```bash
keytool -genkeypair -v -keystore aalyrics-release.jks -storetype JKS -alias aalyrics -keyalg RSA -keysize 4096 -validity 10000
```

The X.509 subject does not need to contain a personal legal name. A project identity such as the following is sufficient:

```text
CN=AALyrics
OU=Development
O=AALyrics
L=Nagoya
ST=Aichi
C=JP
```

On Windows PowerShell, convert the keystore to the single-line Base64 value used by `AALYRICS_RELEASE_KEYSTORE_BASE64`:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("aalyrics-release.jks"))
```

## Publishing

After the intended commit is merged into `main`, ensure the release version is the newest entry in repository-root `CHANGELOG.md`, then create and push the version tag.

For the first signed development release:

```bash
git checkout main
git pull
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

The Release workflow then:

1. verifies that the tagged commit belongs to `main`;
2. verifies required secrets and restores the release keystore in the ephemeral GitHub Actions runner;
3. validates the version tag and resolves `AALYRICS_VERSION_NAME`;
4. verifies that the newest version heading in `CHANGELOG.md` exactly matches the tag version without the leading `v`;
5. runs the architecture guard and unit tests;
6. builds the signed release APK, which bundles that same checked-in changelog;
7. publishes `AALyrics-vX.Y.Z[-suffix].apk`;
8. publishes a matching SHA-256 checksum;
9. marks prerelease-suffixed tags as GitHub Pre-releases.

A stable tag follows the same process but is published without the Pre-release flag. A tag/changelog mismatch is a release-blocking error: the workflow must fail before building or publishing the APK.

## Changelog policy

Repository-root `CHANGELOG.md` is the canonical **user-facing in-app release history**. It is intentionally distinct from the GitHub Release body:

- `CHANGELOG.md` is concise, curated, newest-first, and bundled into the APK for offline Settings display;
- the GitHub Release body is the distribution-page summary plus the durable auto-generated PR-level ledger;
- the two may summarize the same release at different levels of detail and are not expected to be byte-identical.

Every published version uses a heading in this form:

```md
## [0.2.0-alpha.1] - 2026-09-21
```

Before creating a release tag, add that release entry to `CHANGELOG.md` and merge it into `main`. The release tag must point to that revision (or a later `main` revision whose newest changelog version is still the tag being created).

The Release workflow removes the leading `v` from the tag and compares it with the first version heading in `CHANGELOG.md`. For example, `v0.2.0-alpha.1` requires the newest heading to identify `0.2.0-alpha.1`. This prevents publishing a signed APK whose bundled Changelog does not describe that release.

The workflow does **not** generate or write `CHANGELOG.md` after a tag is pushed. Doing so would be too late for the tagged APK: release-history content is source-controlled release input, while bundling and consistency validation are automatic.

At build time, the application Gradle configuration copies the checked-in file to generated assets as `aalyrics_changelog.md`. `:app` reads that bundled Markdown and passes it to the Phone Settings presentation layer. No GitHub API request is required to open Settings > Changelog.

## Release notes structure

GitHub's automatically generated release notes are useful as the durable change ledger, but they are not the primary product-facing summary for milestone releases.

The standard AALyrics Release body should therefore use this order:

1. **Curated milestone summary first** — explain what materially changed for users and testers, using product-level language rather than a commit-by-commit implementation log.
2. **Highlights / major areas** — group the important behavior into a small number of readable sections such as Phone UI, Lyrics, Playback, Android Auto, Translation, or other release-specific areas.
3. **Known limitations / still in development** — for alpha, beta, and RC releases, state intentionally incomplete or unavailable functionality clearly.
4. **Auto-generated GitHub notes last** — preserve the generated `## What's Changed` section and `Full Changelog` link at the bottom of the Release body.

The generated section should **not be deleted or replaced** when curating a Release. It remains the traceable PR-level record of what entered the version. Curated notes are added above it.

Recommended shape:

```md
## <Milestone summary>

<Short product-facing explanation of this release.>

### Highlights

- ...
- ...

### Still in development

- ...
- ...

---

## What's Changed
<GitHub auto-generated entries remain here>

**Full Changelog**: <GitHub-generated comparison link>
```

For milestone releases, the curated section should emphasize externally meaningful behavior and validation state. Avoid copying internal implementation details, temporary fix commits, or exhaustive PR history into the curated portion when the generated section already records them.

Pre-releases should identify themselves clearly as alpha/beta/RC and should not imply feature completeness. Stable releases may omit a limitations section when there are no material caveats, but the curated-summary-first / generated-notes-last structure still applies.

The current Release workflow intentionally continues to publish with `--generate-notes`. After the workflow has successfully created the signed GitHub Release, its body should be edited to add the curated summary **above** the generated notes while leaving the generated section at the bottom.

## Verifying a downloaded APK

Download both the APK and its `.sha256` file from the same GitHub Release. On a system with `sha256sum`:

```bash
sha256sum -c AALyrics-v0.1.0-alpha.1.apk.sha256
```

The checksum verifies file integrity. Android's package signature separately provides the signing identity used for update compatibility.

## In-app update entry

The Phone Settings surface exposes one combined version/update row. It shows the installed version and moves through check, available, download, success, and failure/retry states.

GitHub Releases remains the authoritative distribution source. The Phone Compose layer only emits presentation callbacks; application/runtime code owns network access, release selection/comparison, APK download, checksum verification, and any future install flow.

When an update is available, the runtime should resolve the signed release asset named `AALyrics-vX.Y.Z[-suffix].apk` plus its matching `.sha256` file. Pressing Download should save the latest eligible APK to the device and verify the published checksum before reporting a completed download.

The installed version shown in Settings should come from the app build metadata (`BuildConfig.VERSION_NAME`), not a duplicated UI constant.

The Settings `Changelog` entry is independent of the update-network path. It renders the repository `CHANGELOG.md` bundled into the installed APK; it does not fetch GitHub Release notes at runtime. GitHub Releases remain authoritative for signed update distribution, while `CHANGELOG.md` is authoritative for the in-app release history.

## Installation and updates

Debug and release APKs use different signing identities. If a debug AALyrics build is installed, uninstall it before installing the first release-signed APK.

After the first release-signed installation, later APKs signed with the same release key and a higher `versionCode` can update it normally.

AALyrics is intentionally distributed outside Google Play. For Android Auto, non-Play media apps still require Android Auto developer mode and **Developer settings → Unknown sources** on the test/user device. APK signing does not remove that Android Auto trust-source requirement.

## Release readiness

For an early Pre-release, feature completeness is not required. At minimum, the release pipeline should establish that:

- CI tests pass;
- the APK is release-signed;
- the GitHub Release contains the APK and checksum;
- the APK installs on a target Android device;
- a later release-signed APK can update the installed release using the same key.

Product-specific behavior, including Android Auto rendering and playback/lyrics behavior, should be documented in release notes with known limitations rather than hidden behind an implication that an alpha release is complete.

Stable releases should use a stricter product-readiness decision appropriate to the functionality present at that time.
