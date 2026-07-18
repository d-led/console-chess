#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "=== Building distribution ==="
./gradlew installDist

echo ""
echo "=== Launching chess ==="
exec ./build/install/console-chess/bin/console-chess
