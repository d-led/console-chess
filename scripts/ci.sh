#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "=== Running tests ==="
./gradlew test

echo ""
echo "=== Building native image ==="
export GRAALVM_HOME="/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home"
./gradlew nativeCompile

echo ""
echo "=== Native binary: build/native/nativeCompile/chess ==="
ls -lh build/native/nativeCompile/chess
