# AALyrics Branding

## Canonical source

`branding/AALyrics_MASTER.svg` is the canonical AALyrics master icon and the source of truth for the product brand mark.

All in-app brand-icon rendering, Compose Previews, documentation imagery, release artwork, and platform-specific icon derivatives must preserve the master icon's core geometry, color treatment, and composition unless a platform requirement explicitly demands a crop, mask, monochrome treatment, or adaptive-icon safe-zone adjustment.

## Android derivatives

The Android launcher resources and the shared Compose `AALyricsBrandMark` are derivatives of the canonical master.

- `branding/AALyrics_MASTER.svg` remains authoritative.
- `branding/android/*` contains Android-oriented source derivatives.
- `app/src/main/res/drawable/ic_launcher_*` contains launcher-ready Android resources.
- `branding/android/AALyrics_foreground_android.svg` is the source for the foreground-only in-app brand mark.
- `ui/designsystem/src/main/res/drawable/ic_aalyrics_mark.xml` is the Android VectorDrawable derivative used by `AALyricsBrandMark`; it preserves the foreground artwork at the SVG's native 1024×1024 composition without adaptive-icon scaling or a separate background tile.

Normal product UI and Compose Preview surfaces should render the foreground mark directly over the existing app surface rather than wrapping it in a launcher-style background tile. Do not invent a substitute AALyrics glyph or invert the master treatment. When a derivative needs to change, regenerate or adapt it from the master rather than treating the derivative as a new source of truth.

## Preview rule

Production composables and Compose Previews that show AALyrics identity should use the shared `AALyricsBrandMark` rather than embedding a local logo approximation. This keeps Preview output aligned with the product brand and makes future master-icon updates propagate through the shared design-system path.


## README presentation derivative

`docs/branding/aalyrics-readme-icon-rounded.svg` is a documentation-only presentation derivative of the canonical master. It preserves the master artwork and applies only a rounded-corner clip for the GitHub README hero.

- `branding/AALyrics_MASTER.svg` remains the source of truth.
- The README derivative must not become the source for launcher, Compose, or future branding assets.
- Changes to the README derivative should be regenerated from the current master rather than edited as an independent logo.


## GitHub Social Preview

The repository Social Preview is product marketing artwork, but it must remain grounded in the canonical AALyrics brand and authentic product behavior.

### Brand source

- Use `branding/AALyrics_MASTER.svg` as the source for the AALyrics mark.
- Do not redraw, approximate, or AI-reinvent the logo geometry for Social Preview artwork.
- A raster/vector derivative may be created for composition, but it must preserve the master silhouette, waveform, proportions, and color treatment.
- Small AALyrics marks shown inside an Android Auto composition should use the same canonical derivative rather than a visually similar substitute.

### Product-image source

- Use authentic AALyrics Phone / Android Auto captures as the factual UI reference. The maintained README captures live under `docs/screenshots/`.
- Marketing composition may crop, scale, blur, frame, shadow, perspective-transform, or place authentic UI into a stylized environment.
- Do not fabricate an unsupported AALyrics control, playback capability, lyric state, metadata field, or product surface merely to make the artwork more dramatic.
- If a stylized vehicle/display frame is used, the AALyrics UI shown inside it must remain recognizably grounded in the current product rather than becoming a fictional replacement UI.

### Current visual direction

The preferred GitHub Social Preview direction is:

- **1280×640** output, under **1 MB**;
- dark automotive / navy environment with cyan-blue accents;
- a blurred Android Auto context as the background layer;
- the authentic AALyrics Android Auto experience as the foreground focal display;
- restrained glassmorphism, depth, edge light, and shadow to make the foreground surface appear elevated without obscuring the real UI;
- no giant glass cards that replace the original automotive promotional composition.

Approved primary copy:

```text
Synchronized lyrics for Android Auto
Multi-provider lyrics matching for your music player of choice.
```

Approved feature labels:

```text
Multi-provider matching
Intelligent provider selection
Synchronized lyrics
Built for Android Auto
```

The provider row may identify Musixmatch, LRCLIB, PetitLyrics, and SyncLRC.

Social Preview artwork is not a new source of truth for application UI or branding. Future edits must flow back to the master brand assets and current product captures described above.
