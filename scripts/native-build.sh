#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

export GRAALVM_HOME="/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home"

echo "=== Building native image (this takes a few minutes first time) ==="
./gradlew nativeCompile

echo ""
echo "=== Native binary built at: build/native/nativeCompile/chess ==="
echo "=== Run it with: ./build/native/nativeCompile/chess ==="
