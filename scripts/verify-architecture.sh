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

  production_dependencies=$(
    (grep -En '^[[:space:]]*(api|implementation|compileOnly|runtimeOnly)[[:space:]]*\(' "$build_file" || true) \
      | grep -Ev 'project[[:space:]]*\(|projects\.|org\.jetbrains\.kotlinx:kotlinx-coroutines-core' \
      || true
  )
  if [[ -n "$production_dependencies" ]]; then
    echo "$production_dependencies"
    fail "$module may not add arbitrary production libraries; pure-core dependencies must remain explicitly allowlisted"
  fi

  source_dir="$module/src/main"
  if [[ -d "$source_dir" ]]; then
    forbidden_source_refs=$(
      (grep -RInE \
        --include='*.kt' --include='*.java' \
        '(android\.|androidx\.|okhttp3\.|retrofit2\.|io\.ktor\.|org\.apache\.http\.|java\.net\.|javax\.net\.)' \
        "$source_dir" 2>/dev/null || true) \
        | grep -Ev ':[0-9]+:[[:space:]]*(//|/\*|\*)' \
        || true
    )
    if [[ -n "$forbidden_source_refs" ]]; then
      echo "$forbidden_source_refs"
      fail "$module production sources must stay Android- and network-independent"
    fi
  fi
done

echo "Pure-core architecture boundary check passed."

feature_modules=(
  "feature/phone"
  "feature/automotive"
)

for module in "${feature_modules[@]}"; do
  build_file="$module/build.gradle.kts"

  grep -Eq ':core:lyrics|projects\.core\.lyrics' "$build_file" \
    || fail "$module must consume the shared lyrics-core contract"

  if grep -Eq ':provider:|:platform:media|projects\.provider\.|projects\.platform\.media' "$build_file"; then
    fail "$module must not depend directly on providers or the media platform adapter"
  fi
done

lyrics_state_declarations=$(
  (grep -RIE \
    --include='*.kt' \
    '^[[:space:]]*((public|private|protected|internal|data|sealed|open|abstract|final|value|enum|annotation)[[:space:]]+)*(class|interface|object)[[:space:]]+LyricsState([^[:alnum:]_]|$)|^[[:space:]]*((public|private|protected|internal)[[:space:]]+)*typealias[[:space:]]+LyricsState([^[:alnum:]_]|$)' \
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

if grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]].*provider\.(lrclib|musixmatch|petitlyrics|synclrc)\.|\b(LrcLibClient|MusixmatchClient|PetitLyricsClient|SyncLrcClient|SpotifyTrackIdentity)\b' \
  core/model/src/main core/lyrics/src/main provider/api/src/main 2>/dev/null; then
  fail "provider-specific implementation details must not become pure-core application behavior"
fi

if grep -RInE \
  --include='*.kt' --include='*.java' \
  'android\.media\.(MediaMetadata|session\.[A-Za-z_][A-Za-z0-9_]*)' \
  app core provider feature 2>/dev/null; then
  fail "MediaSession and MediaController framework types must stay inside platform/media"
fi

grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]]+android\.media\.(session\.|MediaMetadata)' \
  platform/media/src/main >/dev/null \
  || fail "platform/media must own the Android media framework adapter"

echo "Provider/UI ownership and Android media confinement checks passed."
