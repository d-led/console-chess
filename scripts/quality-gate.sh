#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

FAILED=0

step() { echo ""; echo "=== $1 ==="; }

step "1/4: Tests"
./gradlew test || FAILED=1

step "2/4: PMD Static Analysis"
./gradlew pmdMain || FAILED=1

step "3/4: Code formatting"
./gradlew spotlessCheck || FAILED=1

step "4/4: jscpd — copy-paste detection"
npx jscpd \
    --pattern "src/main/java/**/*.java" \
    --min-tokens 75 \
    --mode strict \
    --silent \
    --reporters console \
    || FAILED=1

echo ""
if [[ "$FAILED" -eq 0 ]]; then
    echo "=== Quality gate PASSED ==="
else
    echo "=== Quality gate FAILED ==="
    exit 1
fi
