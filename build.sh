#!/usr/bin/env bash
set -euo pipefail

ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
export JAVA_HOME="${JAVA21_HOME:-${JAVA_HOME:-}}"
export JAVA17_HOME="${JAVA17_HOME:-}"

if [[ ! -x "$JAVA_HOME/bin/java" || ! -x "$JAVA17_HOME/bin/javac" ]]; then
    printf '%s\n' 'Building requires JDK 21 for Gradle/Loom and JDK 17 for Minecraft compilation.' 'Set JAVA21_HOME (or JAVA_HOME) and JAVA17_HOME to their complete JDK installations.' >&2
    exit 1
fi

if (( $# == 0 )); then
    set -- build
fi
exec "$ROOT/gradlew" -p "$ROOT" "$@"
