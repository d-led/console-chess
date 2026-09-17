#!/usr/bin/env bash
# Reports which Gradle dependencies and plugins have newer versions available and,
# with --apply, writes them into build.gradle.kts.
#
# Usage:
#   scripts/update-dependencies.sh                 # list updates to the latest release versions
#   scripts/update-dependencies.sh --milestone     # also offer pre-release (milestone/rc) versions
#   scripts/update-dependencies.sh --check         # exit 1 when updates are available (for CI)
#   scripts/update-dependencies.sh --apply         # write the compatible updates, then run the tests
#   scripts/update-dependencies.sh --apply --major # also write the major (breaking) updates
#
# The list comes from the io.github.ben-manes.versions plugin (dependencyUpdates task)
# in JSON form, so the script reads a structured report instead of the line-wrapped
# console text. Pre-release candidates are left out unless --milestone is given, and
# MAJOR updates (JUnit 5 -> 6, JLine 3 -> 4, ...) need --major because they normally
# need code changes. Nothing is committed; review the diff of build.gradle.kts after.
set -euo pipefail
cd "$(dirname "$0")/.."

gradle_file=build.gradle.kts
report_dir=build/dependencyUpdates
report="$report_dir/report.json"

apply=false
apply_majors=false
check_only=false
include_pre_releases=false

usage() {
    echo "Usage: $0 [--apply] [--check] [--milestone] [--major]"
    echo ""
    echo "  (no arguments)  list dependencies with newer release versions"
    echo "  --milestone     also offer pre-release (milestone / rc) versions"
    echo "  --check         exit 1 when updates are available (for CI)"
    echo "  --apply         write the non-major updates into $gradle_file, then run ./gradlew test"
    echo "  --major         with --apply, also write the major (breaking) updates"
    exit "${1:-1}"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --apply) apply=true ;;
        --major) apply_majors=true ;;
        --check) check_only=true ;;
        --milestone) include_pre_releases=true ;;
        -h | --help) usage 0 ;;
        *)
            echo "unknown argument: $1" >&2
            usage 2
            ;;
    esac
    shift
done

if [[ "$apply" == true && "$check_only" == true ]]; then
    echo "--apply and --check are mutually exclusive" >&2
    exit 2
fi

for tool in python3 perl; do
    if ! command -v "$tool" >/dev/null; then
        echo "$tool is required but not on PATH" >&2
        exit 1
    fi
done

updates_options=(--output-formatter=json --revision=release)
if [[ "$include_pre_releases" == false ]]; then
    updates_options+=(--reject-pre-releases)
fi

echo "=== Checking for dependency updates ==="
log="$report_dir/gradle.log"
mkdir -p "$report_dir"
# Gradle prints its own (line-wrapped) report; the tabular summary below is the readable one.
if ! ./gradlew dependencyUpdates "${updates_options[@]}" >"$log" 2>&1; then
    echo "dependencyUpdates failed:" >&2
    cat "$log" >&2
    exit 1
fi
if [[ ! -f "$report" ]]; then
    echo "expected a JSON report at $report:" >&2
    cat "$log" >&2
    exit 1
fi

# Emits one tab-separated line per outdated dependency:
# coordinate, current version, latest version, update kind, plugin id (empty for libraries).
read_updates() {
    python3 - "$report" "$include_pre_releases" <<'PY'
import json
import re
import sys

report_path, include_pre_releases = sys.argv[1], sys.argv[2] == "true"

# The revision level the report was resolved at, most preferred first.
RELEASE_KEYS = ("release", "milestone", "integration")
PLUGIN_MARKER = ".gradle.plugin"


def target_for(available, with_pre_releases):
    """The newest version the report accepted, and whether it is a pre-release."""
    # preRelease is the topmost step, and is only filled in when it is newer than the release beside it
    if with_pre_releases and available.get("preRelease"):
        return available["preRelease"], True
    for key in RELEASE_KEYS:
        if available.get(key):
            return available[key], False
    return None, False


def numeric_parts(version):
    return [int(number) for number in re.findall(r"\d+", version)[:3]]


def update_kind(current, latest):
    if numeric_parts(current)[:1] != numeric_parts(latest)[:1]:
        return "major"
    if numeric_parts(current)[:2] != numeric_parts(latest)[:2]:
        return "minor"
    return "patch"


with open(report_path) as report:
    outdated = json.load(report)["outdated"]["dependencies"]

# Plugin markers are published as <plugin id>.gradle.plugin, declared as id("<plugin id>").
for dependency in outdated:
    latest, is_pre_release = target_for(dependency["available"], include_pre_releases)
    if not latest or latest == dependency["version"]:
        continue
    name = dependency["name"]
    coordinate = "{}:{}".format(dependency["group"], name)
    plugin_id = name[: -len(PLUGIN_MARKER)] if name.endswith(PLUGIN_MARKER) else ""
    kind = update_kind(dependency["version"], latest)
    if is_pre_release:
        kind = "{} (pre-release)".format(kind)
    print("\t".join([coordinate, dependency["version"], latest, kind, plugin_id]))
PY
}

# The wrapper is upgraded separately (it changes the Gradle version the build runs with).
check_gradle_wrapper() {
    python3 - "$report" <<'PY'
import json
import sys

gradle = json.load(open(sys.argv[1]))["gradle"]
if gradle["current"]["isUpdateAvailable"]:
    running, latest = gradle["running"]["version"], gradle["current"]["version"]
    print("\nGradle wrapper: {} -> {} (./gradlew wrapper --gradle-version {})\n".format(running, latest, latest))
PY
}

updates="$(read_updates)"

if [[ -z "$updates" ]]; then
    echo ""
    echo "=== All dependencies are up to date ==="
    check_gradle_wrapper
    exit 0
fi

count="$(printf '%s\n' "$updates" | wc -l | tr -d ' ')"

echo ""
printf '%-45s %-12s %-12s %s\n' "DEPENDENCY" "CURRENT" "LATEST" "UPDATE"
while IFS=$'\t' read -r coordinate current latest kind plugin; do
    label="$coordinate"
    [[ -n "$plugin" ]] && label="$plugin (plugin)"
    printf '%-45s %-12s %-12s %s\n' "$label" "$current" "$latest" "$kind"
done <<<"$updates"
check_gradle_wrapper

if [[ "$check_only" == true ]]; then
    echo "=== $count update(s) available ==="
    exit 1
fi

if [[ "$apply" == false ]]; then
    echo "=== $count update(s) available — re-run with --apply to write them into $gradle_file ==="
    exit 0
fi

apply_update() { # coordinate current latest plugin
    local coordinate="$1" current="$2" latest="$3" plugin="$4" from to
    if [[ -n "$plugin" ]]; then
        from="id(\"$plugin\") version \"$current\""
        to="id(\"$plugin\") version \"$latest\""
    else
        from="$coordinate:$current"
        to="$coordinate:$latest"
    fi
    if ! grep -qF "$from" "$gradle_file"; then
        echo "  skipped $from — not declared as a coordinate in $gradle_file (plugin-managed, e.g. pmd.toolVersion)"
        return
    fi
    perl -pi -e "s/\Q$from\E/$to/g" "$gradle_file"
    echo "  $from -> $to"
    written=$((written + 1))
}

echo "=== Writing updates to $gradle_file ==="
written=0
deferred=0
while IFS=$'\t' read -r coordinate current latest kind plugin; do
    if [[ "$kind" == major* && "$apply_majors" == false ]]; then
        echo "  deferred $coordinate:$current -> $latest (major, needs --major)"
        deferred=$((deferred + 1))
        continue
    fi
    apply_update "$coordinate" "$current" "$latest" "$plugin"
done <<<"$updates"
echo ""

if [[ "$deferred" -gt 0 ]]; then
    echo "=== $deferred major update(s) left out — re-run with --apply --major to write them too ==="
fi

if [[ "$written" -eq 0 ]]; then
    echo "=== Nothing to write into $gradle_file ==="
    exit 0
fi

echo "=== Running tests ==="
if ./gradlew test; then
    echo ""
    echo "=== Tests pass — review the diff of $gradle_file and commit when ready ==="
else
    echo ""
    echo "=== TESTS FAILED — the new versions are still in $gradle_file ===" >&2
    echo "    Revert with: git checkout $gradle_file" >&2
    exit 1
fi
