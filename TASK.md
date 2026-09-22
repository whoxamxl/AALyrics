# Settings Legal & Help Integration

## Branch and baseline

- Branch: `feature/settings-legal-help`
- Base: `main` at `89906f2624f3cf9d7b5b833d31a88de28cffdedd`.
- Classification: **SETTINGS / BUNDLED DOCUMENT / HELP ROUTING**.
- Authoritative references: `AGENTS.md`, `docs/PHONE_SETTINGS.md`, repository-root `PRIVACY.md`, `TERMS_OF_USE.md`, `THIRD_PARTY_LICENSES.md`, `SUPPORT.md`, and the existing bundled-document Settings implementation.

## Goal

Complete the lower Settings information architecture using the current main baseline without changing unrelated Settings, provider, playback, Translation, or Android Auto behavior.

Target order:

```text
ABOUT & SUPPORT
  Privacy Policy                  >
  Terms of Use                    >
  License                         >
  Help & Feedback                 >
  Support AALyrics                >
```

`Support AALyrics` keeps its existing Buy Me a Coffee purpose and visual treatment. `Help & Feedback` is a separate routing hub for users who need help, want to report a problem, or want to provide feedback.

## Approved ownership

### Privacy Policy

No ownership change. `PRIVACY.md` remains the repository source of truth and the existing bundled-document path remains intact.

### Terms of Use

`TERMS_OF_USE.md` follows the existing bundled-document model:

```text
repository TERMS_OF_USE.md
    -> app build asset
    -> :app reads text
    -> SettingsScreenUiState
    -> TermsOfUseSettingsScreen
    -> PhoneMarkdownText
```

The document is available offline and is not fetched from GitHub at runtime.

### License and third-party licenses

The existing `License` entry remains a second-level Settings screen.

`LicenseSettingsScreen` continues to present the AALyrics required notice and AALyrics license terms, and gains one internal navigation row:

```text
License
├─ REQUIRED NOTICE
├─ LICENSE TERMS
└─ Third-party licenses           >
```

`THIRD_PARTY_LICENSES.md` is bundled through the same application-owned document path and rendered by a dedicated read-only Markdown screen.

Navigation depth is intentional:

```text
Settings
  -> License
      -> Third-party licenses
```

System Back from `Third-party licenses` returns to `License`. System Back from `License` returns to Settings home.

### Help & Feedback

`Help & Feedback` is a native routing hub rather than a Markdown dump of `SUPPORT.md`.

The repository `SUPPORT.md` remains the canonical GitHub-facing support policy. The in-app surface exposes only end-user-relevant routes:

```text
Help & Feedback
├─ Report a bug                   ↗
├─ Ask a question                 ↗
├─ Suggest an idea                ↗
├─ General discussion             ↗
└─ Report a security issue        ↗
```

External destinations remain application-owned browser actions. `:ui:phone` renders presentation state and emits callbacks only.

Security reporting must route to the repository's private vulnerability reporting surface rather than a public Issue or Discussion.

### Support AALyrics

No product-purpose change. It remains the voluntary project-support surface and external Buy Me a Coffee handoff.

Do not rename this screen to `Help & Feedback`, and do not combine support payments with technical help routing.

## Navigation contract

Normal System Back follows the current hierarchy.

- Terms of Use -> Settings home
- Help & Feedback -> Settings home
- Support AALyrics -> Settings home
- License -> Settings home
- Third-party licenses -> License
- Settings-tab reselection from any Settings depth -> Settings home and scroll home content to top

Root reselection must remain stronger than hierarchical Back and must discard any transient Settings-local modal/draft state as it does today.

## Acceptance criteria

### Documentation / contract

- [x] Create the topic branch from current `main`.
- [x] Freeze the final About & Support order.
- [x] Define Terms of Use bundled-document ownership.
- [x] Define License -> Third-party licenses navigation.
- [x] Define Help & Feedback routing ownership.
- [x] Preserve Support AALyrics as the voluntary funding surface.
- [x] Define Back and Settings-root reset semantics for the new third-level License child.

### Implementation

- [x] Bundle `TERMS_OF_USE.md` and `THIRD_PARTY_LICENSES.md` as generated app assets.
- [x] Expose both documents through the application-owned Settings presentation boundary.
- [x] Add `TermsOfUseSettingsScreen` using the shared Settings header and Markdown renderer.
- [x] Add `ThirdPartyLicensesSettingsScreen` using the shared Settings header and Markdown renderer.
- [x] Add the License -> Third-party licenses navigation row.
- [ ] Add the native `Help & Feedback` routing hub.
- [ ] Add application-owned external-link callbacks for the Help & Feedback destinations.
- [ ] Preserve the existing `Support AALyrics` implementation and behavior.
- [x] Extend Settings navigation state so Third-party licenses Back returns to License while root reselection still returns directly to Settings home.
- [ ] Add/update strings, deterministic Previews, presentation mapping coverage, and focused navigation tests.
- [ ] Align `docs/PHONE_SETTINGS.md` with the implemented final state.
- [ ] Re-evaluate `Reset AALyrics`; this slice is expected to add no persisted state, so no reset behavior change should be required unless implementation changes that assumption.
- [ ] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Scope guard

Do not redesign unrelated Settings sections, change existing Support AALyrics visuals/payment behavior, add embedded WebViews, add runtime GitHub document fetching, change provider behavior, change Translation semantics, or introduce new persisted Settings state.

## Current stop point

Terms of Use is now a Settings-level bundled-document screen. License now contains a dedicated THIRD-PARTY SOFTWARE section whose Third-party licenses row opens the bundled third-party license document as a child screen. Hierarchical Back returns that child to License, while Settings-tab reselection still resets directly to Settings home. Help & Feedback has not been implemented yet; stop at this legal-document UI checkpoint.
