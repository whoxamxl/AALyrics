#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "Architecture check failed: $*" >&2
  exit 1
}

pure_modules=(
  "core/model"
  "provider/api"
  "core/lyrics"
)

for module in "${pure_modules[@]}"; do
  build_file="$module/build.gradle.kts"

  grep -Fq 'id("org.jetbrains.kotlin.jvm")' "$build_file" \
    || fail "$module must remain a Kotlin/JVM module"

  if grep -Eq 'com\.android\.|androidx\.' "$build_file"; then
    fail "$module build configuration must not depend on Android plugins/libraries"
  fi

  source_dir="$module/src/main"
  if [[ -d "$source_dir" ]] && grep -RInE \
    --include='*.kt' --include='*.java' \
    '^[[:space:]]*import[[:space:]]+(android\.|androidx\.|okhttp3\.|retrofit2\.|java\.net\.|javax\.net\.)' \
    "$source_dir"; then
    fail "$module production sources must stay Android- and network-independent"
  fi
done

echo "Pure-core architecture boundary check passed."

feature_modules=(
  "feature/phone"
  "feature/automotive"
)

for module in "${feature_modules[@]}"; do
  build_file="$module/build.gradle.kts"

  grep -Fq 'project(":core:lyrics")' "$build_file" \
    || fail "$module must consume the shared lyrics-core contract"

  if grep -Eq 'project\(":provider:|project\(":platform:media"\)' "$build_file"; then
    fail "$module must not depend directly on providers or the media platform adapter"
  fi
done

lyrics_state_declarations=$(
  (grep -RIE \
    --include='*.kt' \
    '^[[:space:]]*(sealed[[:space:]]+(interface|class)|class|interface)[[:space:]]+LyricsState\b' \
    core provider platform feature app 2>/dev/null || true) \
  | wc -l | tr -d ' '
)

[[ "$lyrics_state_declarations" == "1" ]] \
  || fail "exactly one shared LyricsState declaration is required; found $lyrics_state_declarations"

echo "Shared-state feature-consumption boundary check passed."

if grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]]+io\.github\.whoxamxl\.aalyrics\.provider\.|\b(LyricsProvider|CandidateSelector)\b' \
  feature 2>/dev/null; then
  fail "UI feature modules must not fetch from providers or rank candidates"
fi

if grep -RInEi \
  --include='*.kt' --include='*.java' \
  '\b(lrclib|musixmatch|petitlyrics|synclrc|spotify)\b' \
  core/model/src/main core/lyrics/src/main provider/api/src/main 2>/dev/null; then
  fail "provider-specific quirks must not become pure-core application behavior"
fi

if grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]]+android\.media\.(session\.|MediaMetadata)' \
  app core provider feature 2>/dev/null; then
  fail "MediaSession and MediaController framework types must stay inside platform/media"
fi

grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]]+android\.media\.(session\.|MediaMetadata)' \
  platform/media/src/main >/dev/null \
  || fail "platform/media must own the Android media framework adapter"

echo "Provider/UI ownership and Android media confinement checks passed."
