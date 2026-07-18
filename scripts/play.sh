#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

BIN="build/install/console-chess/bin/console-chess"
BUILD=false

for arg in "$@"; do
    case "$arg" in
        --build|-b) BUILD=true ;;
        *) echo "Usage: $0 [--build|-b]"; exit 1 ;;
    esac
done

if [[ "$BUILD" == true ]] || [[ ! -x "$BIN" ]]; then
    echo "=== Building... ==="
    ./gradlew installDist
fi

echo "=== Launching chess ==="
exec "$BIN"
