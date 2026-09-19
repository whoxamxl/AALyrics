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
