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
