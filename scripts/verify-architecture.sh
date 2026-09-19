#!/usr/bin/env bash
set -euo pipefail

fail() {
  echo "Architecture check failed: $*" >&2
  exit 1
}

production_dependency_expressions() {
  awk '
    function paren_delta(text, copy, opens, closes) {
      copy = text
      opens = gsub(/\(/, "(", copy)
      copy = text
      closes = gsub(/\)/, ")", copy)
      return opens - closes
    }

    {
      line = $0
      sub(/^[[:space:]]+/, "", line)

      if (!collecting) {
        if (line ~ /^(api|implementation|compileOnly|runtimeOnly)[[:space:]]*\(/) {
          expression = line
          depth = paren_delta(line)
          collecting = 1

          if (depth <= 0) {
            print expression
            collecting = 0
          }
        }
      } else {
        expression = expression " " line
        depth += paren_delta(line)

        if (depth <= 0) {
          print expression
          collecting = 0
        }
      }
    }
  ' "$1"
}

dependency_prefix='^[[:space:]]*(api|implementation|compileOnly|runtimeOnly)[[:space:]]*\([[:space:]]*'
dependency_suffix='[[:space:]]*\)[[:space:]]*(//.*)?$'
core_model_target='(project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":core:model"[[:space:]]*\)|projects\.core\.model)'
core_lyrics_target='(project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":core:lyrics"[[:space:]]*\)|projects\.core\.lyrics)'
provider_api_target='(project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":provider:api"[[:space:]]*\)|projects\.provider\.api)'
coroutines_core_target='"org\.jetbrains\.kotlinx:kotlinx-coroutines-core:[^"]+"'
network_api_refs='(android\.net\.http\.|okhttp3\.|retrofit2\.|io\.ktor\.|org\.apache\.http\.|java\.net\.|javax\.net\.)'

pure_modules=(
  "core/model"
  "provider/api"
  "provider/matching"
  "provider/lrc"
  "core/lyrics"
  "translation/api"
)

for module in "${pure_modules[@]}"; do
  build_file="$module/build.gradle.kts"

  grep -Fq 'id("org.jetbrains.kotlin.jvm")' "$build_file" \
    || fail "$module must remain a Kotlin/JVM module"

  if grep -Eq 'com\.android\.|androidx\.' "$build_file"; then
    fail "$module build configuration must not depend on Android plugins/libraries"
  fi

  production_dependencies="$(production_dependency_expressions "$build_file")"
  case "$module" in
    "core/model"|"provider/matching")
      forbidden_dependencies="$production_dependencies"
      ;;
    "provider/api"|"provider/lrc")
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
    "translation/api")
      forbidden_dependencies=$(
        printf '%s\n' "$production_dependencies" \
          | grep -Ev "${dependency_prefix}${coroutines_core_target}${dependency_suffix}" \
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
        "(android\\.|androidx\\.|${network_api_refs})" \
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

presentation_modules=(
  "ui/phone"
  "ui/automotive"
)

ui_source_dirs=("ui/designsystem/src/main")
for module in "${presentation_modules[@]}"; do
  build_file="$module/build.gradle.kts"
  presentation_dependencies="$(production_dependency_expressions "$build_file")"

  printf '%s\n' "$presentation_dependencies" \
    | grep -Eq "${dependency_prefix}${core_lyrics_target}${dependency_suffix}" \
    || fail "$module must consume the shared lyrics-core contract"

  if printf '%s\n' "$presentation_dependencies" \
    | grep -Eq 'project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":(provider:[^"]+|platform:media)"|projects\.(provider\.|platform\.media)'; then
    fail "$module must not depend directly on providers or the media platform adapter"
  fi

  source_dir="$module/src/main"
  ui_source_dirs+=("$source_dir")
done

designsystem_dependencies="$(production_dependency_expressions "ui/designsystem/build.gradle.kts")"
if printf '%s\n' "$designsystem_dependencies" \
  | grep -Eq 'project[[:space:]]*\([[:space:]]*(path[[:space:]]*=[[:space:]]*)?":(core|provider|platform|ui:(phone|automotive))'; then
  fail "ui/designsystem must remain independent of app/domain/provider/platform modules"
fi

for source_dir in "${ui_source_dirs[@]}"; do
  if [[ -d "$source_dir" ]]; then
    forbidden_network_refs=$(
      (grep -RInE \
        --include='*.kt' --include='*.java' \
        "$network_api_refs" \
        "$source_dir" 2>/dev/null || true) \
        | grep -Ev ':[0-9]+:[[:space:]]*(//|/\*|\*)' \
        || true
    )
    if [[ -n "$forbidden_network_refs" ]]; then
      echo "$forbidden_network_refs"
      fail "$source_dir must not perform networking directly"
    fi
  fi
done

production_source_dirs=()
while IFS= read -r -d '' source_dir; do
  production_source_dirs+=("$source_dir")
done < <(find core provider platform translation ui app -type d -path '*/src/main' -print0 2>/dev/null)

[[ "${#production_source_dirs[@]}" -gt 0 ]] \
  || fail "no production source directories found"

lyrics_state_declarations=$(
  (grep -RIE \
    --include='*.kt' \
    '^[[:space:]]*((public|private|protected|internal|data|sealed|open|abstract|final|value|enum|annotation)[[:space:]]+)*(class|interface|object)[[:space:]]+LyricsState([^[:alnum:]_]|$)|^[[:space:]]*((public|private|protected|internal)[[:space:]]+)*typealias[[:space:]]+LyricsState([^[:alnum:]_]|$)' \
    "${production_source_dirs[@]}" 2>/dev/null || true) \
  | wc -l | tr -d ' '
)

[[ "$lyrics_state_declarations" == "1" ]] \
  || fail "exactly one shared LyricsState declaration is required; found $lyrics_state_declarations"

echo "Shared-state UI-consumption boundary check passed."

if grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]]+io\.github\.whoxamxl\.aalyrics\.provider\.|\b(LyricsProvider|CandidateSelector)\b' \
  "${ui_source_dirs[@]}" 2>/dev/null; then
  fail "UI modules must not fetch from providers or rank candidates"
fi

if grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]].*provider\.(lrclib|musixmatch|petitlyrics|synclrc)\.|\b(LrcLibClient|MusixmatchClient|PetitLyricsClient|SyncLrcClient|SpotifyTrackIdentity)\b' \
  core/model/src/main core/lyrics/src/main provider/api/src/main translation/api/src/main 2>/dev/null; then
  fail "provider-specific implementation details must not become pure-core application behavior"
fi

non_media_source_dirs=()
for source_dir in "${production_source_dirs[@]}"; do
  if [[ "$source_dir" != "platform/media/src/main" ]]; then
    non_media_source_dirs+=("$source_dir")
  fi
done

forbidden_media_refs=$(
  (grep -RInE \
    --include='*.kt' --include='*.java' \
    'android\.media\.(\*|MediaMetadata|session\.(\*|[A-Za-z_][A-Za-z0-9_]*))' \
    "${non_media_source_dirs[@]}" 2>/dev/null || true) \
    | grep -Ev ':[0-9]+:[[:space:]]*(//|/\*|\*)' \
    || true
)
if [[ -n "$forbidden_media_refs" ]]; then
  echo "$forbidden_media_refs"
  fail "MediaSession and MediaController framework types must stay inside platform/media"
fi

grep -RInE \
  --include='*.kt' --include='*.java' \
  '^[[:space:]]*import[[:space:]]+android\.media\.(session\.|MediaMetadata)' \
  platform/media/src/main >/dev/null \
  || fail "platform/media must own the Android media framework adapter"

echo "Provider/UI ownership and Android media confinement checks passed."
