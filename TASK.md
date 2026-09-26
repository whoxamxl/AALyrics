# AALyrics v1.0.0-beta.1 Release Preparation

## Branch and baseline

- Branch: `release/v1.0.0-beta.1`.
- Base: `main` at `ad4ca154604e4846e273b21a098ba0e03c98bc97`.
- This slice prepares the first AALyrics 1.0 Beta release. It does not change lyrics retrieval, playback, timing, Translation, Karaoke, Android Auto runtime behavior, persistence, or provider behavior.

## Product decision

AALyrics has reached the point where its primary purpose is functional as a product: it can observe supported Android playback, resolve synchronized lyrics from production providers, present those lyrics on Phone and Android Auto, and expose the established Phone playback/settings/update experience.

The release therefore advances from the 0.2 alpha line to `v1.0.0-beta.1`.

Beta means the intended AALyrics 1.0 product identity and core lyrics experience are established, while some advanced functionality and validation remain intentionally incomplete. It does not claim feature completeness or stable-release readiness.

## Goals

- Make the repository README a human-facing AALyrics landing page rather than a construction/status document.
- Present current user-visible capabilities before architecture and implementation details.
- Keep installation and Android Auto sideload requirements concise and discoverable.
- Record `1.0.0-beta.1` as the next in-app changelog version.
- Keep the canonical Git tag/version machine-readable as `v1.0.0-beta.1`.
- Give the GitHub Release a separate human-readable title, `AALyrics 1.0 Beta 1`.
- Keep GitHub's generated PR-level release ledger below the curated milestone summary.
- Preserve the existing signed APK/checksum pipeline and prerelease classification.

## README contract

The README should answer, in this order:

1. What is AALyrics?
2. What can I do with it now?
3. How do I install it?
4. What does Android Auto support?
5. What does Beta mean / what is still evolving?
6. Where do developers find build, architecture, release, and license details?

Internal implementation terminology should not dominate the opening sections. Detailed architecture remains authoritative in `docs/`.

Authentic product screenshots are stored under `docs/screenshots/` and are used for the README showcase. This release-prep slice must not fabricate UI screenshots merely to fill the README.

## Release naming contract

- Git tag: `v1.0.0-beta.1`
- Android `versionName`: `1.0.0-beta.1`
- APK: `AALyrics-v1.0.0-beta.1.apk`
- GitHub Release title: `AALyrics 1.0 Beta 1`
- GitHub classification: **Pre-release**

The display title is presentation only. Update comparison, APK naming, changelog validation, and installed version identity continue to use the canonical SemVer-style version.

## Checkpoints

- [x] Establish release-prep branch, scope, and Beta product decision.
- [x] Rewrite README as a product-first, visual-first landing page using the canonical brand master and authentic device screenshots.
- [x] Align Release policy and workflow with distinct human-readable Release titles.
- [x] Add the `1.0.0-beta.1` user-facing changelog entry.
- [x] Review the branch-wide diff for stale alpha/construction wording and release-version mismatches.
- [x] Run release-relevant pre-PR validation, record evidence here, open a Draft PR, and STOP per `AGENTS.md`.

## Known Beta limitations to communicate

- Sync calibration controls/persistence are not yet part of the finished user experience.
- Phone WORD_SYNC Karaoke remains experimental and opt-in.
- Android Auto currently focuses on line-oriented Now Playing lyrics rather than Phone feature parity or Karaoke presentation.
- AALyrics is distributed outside Google Play; Android Auto use of the sideloaded compatibility path requires the documented developer/unknown-source setup.

## Acceptance criteria

- A first-time repository visitor can understand AALyrics and reach installation instructions without reading architecture terminology.
- README contains no working-branch status text and does not present AALyrics as merely a newly constructed project.
- User-visible capabilities described in README match current `main`.
- `CHANGELOG.md` has `1.0.0-beta.1` as its newest version before tagging.
- The Release workflow accepts `v1.0.0-beta.1`, publishes it as a prerelease, retains canonical version/APK naming, and uses the human-facing Release title.
- `docs/RELEASES.md` documents the distinction between canonical tag/version and Release display title.
- No production runtime behavior changes as part of this release-prep slice.


## Visual README refinement

- `branding/AALyrics_MASTER.svg` remains authoritative. The README uses `docs/branding/aalyrics-readme-icon-rounded.svg`, a documentation-only derivative that preserves the master artwork and adds only a rounded-corner clip.
- Three authentic Phone screenshots cover the synchronized Lyrics surface, Expanded Player, and Queue, displayed as one responsive three-column row near the top of the README.
- Two authentic DHU screenshots cover Android Auto full Now Playing and split view alongside navigation.
- The README opening follows a product-page flow: rounded brand mark -> product name/tagline -> Android/Beta/license badges -> Download APK CTA -> real product screenshot -> concise Highlights.
- Phone Expanded Player/Queue and Android Auto full/split presentation provide visual proof of the current product state before installation and developer documentation.
- Phone screenshots are cropped uniformly by 60 px at the top to remove the Android system status bar while preserving the AALyrics header, resulting in 709×1476 assets. To avoid stale GitHub image caching, README references use new `*-cropped.webp` filenames. Android Auto screenshots are cropped by 120 px on all four edges, resulting in 1296×624 assets. README derivatives use high-quality WebP encoding rather than the earlier over-compressed 5–7 KB assets.
- Project structure and architecture links remain available but are collapsed below the user-facing product/install sections rather than dominating the landing surface.
- No application runtime, UI implementation, provider, timing, playback, Translation, Karaoke, or Android Auto behavior changed in this refinement.

## Release-prep validation record

- The `Android 8.0+` README badge is grounded in `app/build.gradle.kts` `minSdk = 26` (Android 8.0 / API 26). It describes the APK installation floor, not a separate Android Auto host compatibility guarantee.

- High-quality README screenshot derivatives retain UI detail. Phone assets are uniformly status-bar-cropped to 709×1476 and remain approximately 64–79 KB. Android Auto assets are cropped by 120 px on all edges to 1296×624 and remain approximately 57–102 KB. The `*-cropped.webp` filenames intentionally bust stale README image caching.
- The README hero uses the rounded documentation derivative while `branding/AALyrics_MASTER.svg` remains untouched and authoritative.

- Baseline alignment: the release-prep branch remains zero commits behind the intended `main @ ad4ca154604e4846e273b21a098ba0e03c98bc97` baseline.
- Branch-wide scope is limited to release/docs surfaces: `.github/workflows/release.yml`, `CHANGELOG.md`, `README.md`, `TASK.md`, `docs/BRANDING.md`, `docs/RELEASES.md`, the README-only rounded brand derivative, and five documentation screenshots; no production Kotlin/Java/resources or provider/runtime behavior changed.
- README no longer contains the stale working-branch status or the old "new Android project" construction framing. Installation, Android Auto setup, Beta scope, user-visible capabilities, development entry points, and license are directly discoverable.
- The newest in-app changelog heading is exactly `1.0.0-beta.1`, matching the intended canonical tag after removing its leading `v`.
- Release-title mapping was exercised for Beta, Alpha, RC, stable, and non-zero patch examples. The intended tag maps to `AALyrics 1.0 Beta 1`; canonical `versionName`, tag, and APK naming remain `1.0.0-beta.1` / `v1.0.0-beta.1`.
- Release policy documents that zero patch components may be omitted only from the human-facing GitHub Release title.
- The first title implementation produced `AALyrics 1.0.0 Beta 1`, which conflicted with the approved human-facing contract. This was caught during validation and corrected before PR creation.
- Full Gradle, unit-test, branch-policy, and architecture validation is delegated to the normal pull-request Build workflow. The visual-only asset/refinement commits continue to change no production implementation code.

## Result

The branch is ready for Draft PR review as a release-preparation-only change. It does **not** create the tag, publish the Release, or merge into `main`. Those remain separate explicit gates.
