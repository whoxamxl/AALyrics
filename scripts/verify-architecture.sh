#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "Architecture check failed: $*" >&2
  exit 1
}

production_dependency_lines() {
  grep -En '^[[:space:]]*(api|implementation|compileOnly|runtimeOnly)[[:space:]]*\(' "$1" || true
}

dependency_prefix='^[0-9]+:[[:space:]]*(api|implementation|compileOnly|runtimeOnly)[[:space:]]*\([[:space:]]*'
dependency_suffix='[[:space:]]*\)[[:space:]]*(//.*)?$'
core_model_target='(project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":core:model"[[:space:]]*\)|projects\.core\.model)'
core_lyrics_target='(project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":core:lyrics"[[:space:]]*\)|projects\.core\.lyrics)'
provider_api_target='(project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":provider:api"[[:space:]]*\)|projects\.provider\.api)'
coroutines_core_target='"org\.jetbrains\.kotlinx:kotlinx-coroutines-core:[^"]+"'

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

  production_dependencies="$(production_dependency_lines "$build_file")"
  case "$module" in
    "core/model")
      forbidden_dependencies="$production_dependencies"
      ;;
    "provider/api")
      forbidden_dependencies=$(
        printf '%s\n' "$production_dependencies" \
          | grep -Ev "${dependency_prefix}${core_model_target}${dependency_suffix}" \
          || true
      )
      ;;
    "core/lyrics")
      allowed_target="(${core_model_target}|${provider_api_target}|${coroutines_core_target})"
      forbidden_dependencies=$(
        printf '%s\n' "$production_dependencies" \
          | grep -Ev "${dependency_prefix}${allowed_target}${dependency_suffix}" \
          || true
      )
      ;;
  esac

  if [[ -n "$forbidden_dependencies" ]]; then
    echo "$forbidden_dependencies"
    fail "$module may only use its explicitly approved production dependencies"
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
  feature_dependencies="$(production_dependency_lines "$build_file")"

  printf '%s\n' "$feature_dependencies" \
    | grep -Eq "${dependency_prefix}${core_lyrics_target}${dependency_suffix}" \
    || fail "$module must consume the shared lyrics-core contract"

  if printf '%s\n' "$feature_dependencies" \
    | grep -Eq 'project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":(provider:[^"]+|platform:media)"|projects\.(provider\.|platform\.media)'; then
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
