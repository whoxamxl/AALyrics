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

### GitHub Pre-releases

Development milestones use semantic-version-style prerelease tags:

- `v0.1.0-alpha.1`
- `v0.1.0-beta.1`
- `v0.1.0-rc.1`

Any accepted release tag containing a prerelease suffix is published with GitHub's **Pre-release** flag.

The initial signed distribution should use `v0.1.0-alpha.1`. It is acceptable for this release to be functionally incomplete. Its immediate purpose is to validate the durable signing/distribution path and provide an installable baseline.

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

After the intended commit is merged into `main`, create and push a version tag.

For the first signed development release:

```bash
git checkout main
git pull
git tag v0.1.0-alpha.1
git push origin v0.1.0-alpha.1
```

The Release workflow then:

1. verifies that the tagged commit belongs to `main`;
2. validates the version tag;
3. verifies required secrets;
4. restores the release keystore in the ephemeral GitHub Actions runner;
5. runs the architecture guard and unit tests;
6. builds the signed release APK;
7. publishes `AALyrics-vX.Y.Z[-suffix].apk`;
8. publishes a matching SHA-256 checksum;
9. marks prerelease-suffixed tags as GitHub Pre-releases.

A stable tag follows the same process but is published without the Pre-release flag.

## Verifying a downloaded APK

Download both the APK and its `.sha256` file from the same GitHub Release. On a system with `sha256sum`:

```bash
sha256sum -c AALyrics-v0.1.0-alpha.1.apk.sha256
```

The checksum verifies file integrity. Android's package signature separately provides the signing identity used for update compatibility.

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
