#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

BIN="build/native/nativeCompile/chess"

if [[ ! -x "$BIN" ]]; then
    echo "=== Native binary not found. Building first... ==="
    export GRAALVM_HOME="/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home"
    ./gradlew nativeCompile
fi

echo "=== Launching native chess ==="
exec "$BIN"
