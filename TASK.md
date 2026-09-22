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

`LicenseSettingsScreen` presents all three legal/license blocks inline:

```text
License
├─ REQUIRED NOTICE
├─ LICENSE TERMS
└─ THIRD-PARTY LICENSES
```

`THIRD_PARTY_LICENSES.md` is bundled through the same application-owned document path and rendered directly inside the third License section through `PhoneMarkdownText`. It does not introduce another navigation level.

System Back from `License` returns directly to Settings home.

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
- Settings-tab reselection from any Settings depth -> Settings home and scroll home content to top

Root reselection must remain stronger than hierarchical Back and must discard any transient Settings-local modal/draft state as it does today.

## Acceptance criteria

### Documentation / contract

- [x] Create the topic branch from current `main`.
- [x] Freeze the final About & Support order.
- [x] Define Terms of Use bundled-document ownership.
- [x] Define inline third-party license ownership inside the License screen.
- [x] Define Help & Feedback routing ownership.
- [x] Preserve Support AALyrics as the voluntary funding surface.
- [x] Confirm all legal/help subscreens remain direct children of Settings home.

### Implementation

- [x] Bundle `TERMS_OF_USE.md` and `THIRD_PARTY_LICENSES.md` as generated app assets.
- [x] Expose both documents through the application-owned Settings presentation boundary.
- [x] Add `TermsOfUseSettingsScreen` using the shared Settings header and Markdown renderer.
- [x] Render `THIRD_PARTY_LICENSES.md` inline as the third section of `LicenseSettingsScreen`.
- [x] Add the native `Help & Feedback` routing hub.
- [x] Add application-owned external-link callbacks for the Help & Feedback destinations.
- [x] Preserve the existing `Support AALyrics` implementation and behavior.
- [x] Add/update strings, deterministic Previews, presentation mapping coverage, and focused navigation tests.
- [x] Align `docs/PHONE_SETTINGS.md` with the implemented final state.
- [x] Re-evaluate `Reset AALyrics`; this slice adds no persisted state, so the reset implementation and user-facing reset scope remain unchanged.
- [ ] Run architecture checks, unit tests, debug APK build, CI, and bounded review before merge.

## Scope guard

Do not redesign unrelated Settings sections, change existing Support AALyrics visuals/payment behavior, add embedded WebViews, add runtime GitHub document fetching, change provider behavior, change Translation semantics, or introduce new persisted Settings state.

## Validation checkpoint

Focused coverage is now present for:

- Settings-local Back/root-reset navigation across every current subscreen;
- exact Help & Feedback GitHub routing, including a guard that security reporting does not use public Issues or Discussions;
- unchanged pass-through of bundled Terms of Use and third-party license Markdown into Settings presentation state.

Preview coverage is aligned with the implemented structure:

- Terms of Use: typical, 320dp narrow, enlarged font;
- License with inline THIRD-PARTY LICENSES: typical, 320dp narrow, enlarged font;
- Help & Feedback: typical, 320dp narrow, enlarged font;
- the main Settings previews consume the final About & Support ordering.

Reset contract review found no new persisted key, durable preference, onboarding acknowledgement, cache, model, or downloaded asset in this slice. The new bundled documents are read-only build assets, Help & Feedback is callback-only routing, and Settings-local navigation remains transient presentation state. Existing `resetAppOwnedSettings()` ownership therefore remains correct with no reset behavior or copy change.

The validation tests have been added but the full unit-test/build/CI pass is intentionally deferred to the next checkpoint.

## Current stop point

Implementation, focused coverage, Reset review, documentation, and deterministic Previews are aligned. Stop here before running architecture checks, unit tests, debug APK build, CI, and bounded review.
