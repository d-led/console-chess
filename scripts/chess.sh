#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

GRAALVM_HOME="${GRAALVM_HOME:-/Library/Java/JavaVirtualMachines/graalvm-25.jdk/Contents/Home}"
JVM_BIN="build/install/console-chess/bin/console-chess"
NATIVE_BIN="build/native/nativeCompile/chess"

usage() {
    echo "Usage: $0 <command>"
    echo ""
    echo "Commands:"
    echo "  play      Build + run (JVM)"
    echo "  build     Build JVM distribution"
    echo "  test      Run all tests"
    echo "  format    Auto-format Java code + markdown"
    echo "  upgrade   Show available dependency upgrades"
    echo "  native    Build native image (GraalVM)"
    echo "  nrun      Build native if needed, then run"
    echo "  ci        Test + native build"
    exit 1
}

cmd_build() {
    echo "=== Building JVM distribution ==="
    ./gradlew installDist
}

cmd_play() {
    echo "=== Building ==="
    ./gradlew installDist
    echo "=== Launching chess ==="
    exec "$JVM_BIN"
}

cmd_test() {
    echo "=== Running tests ==="
    ./gradlew test
    echo "=== All tests passed ==="
}

cmd_format() {
    echo "=== Formatting code ==="
    ./gradlew spotlessApply
    echo "=== Done ==="
}

cmd_upgrade() {
    echo "=== Checking for dependency upgrades ==="
    ./gradlew dependencyUpdates
}

cmd_native() {
    echo "=== Building native image ==="
    export GRAALVM_HOME
    ./gradlew nativeCompile
    echo "=== Native binary: $NATIVE_BIN ==="
    ls -lh "$NATIVE_BIN"
}

cmd_nrun() {
    if [[ ! -x "$NATIVE_BIN" ]]; then
        cmd_native
    fi
    echo "=== Launching native chess ==="
    exec "$NATIVE_BIN"
}

cmd_ci() {
    cmd_test
    echo ""
    cmd_native
}

case "${1:-}" in
    play)    cmd_play ;;
    build)   cmd_build ;;
    test)    cmd_test ;;
    format)  cmd_format ;;
    upgrade) cmd_upgrade ;;
    native)  cmd_native ;;
    nrun)    cmd_nrun ;;
    ci)      cmd_ci ;;
    *)       usage ;;
esac
