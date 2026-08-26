#!/usr/bin/env bash
# Generates the Chocolatey package (.nupkg) for console-chess from the
# templates in chocolatey/ and the given version/URL/checksum.
#
# Expects the following environment variables:
#   VERSION   e.g. 1.2.3
#   URL       direct download URL of the Windows archive
#   SHA256    sha256 of that archive
#
# Requires chocolatey (choco) on PATH. Output: dist/choco/console-chess.<version>.nupkg
#
# Usage:
#   VERSION=1.2.3 URL=... SHA256=... scripts/generate-chocolatey-package.sh
set -euo pipefail
cd "$(dirname "$0")/.."

version="${VERSION:?VERSION is required}"
url="${URL:?URL is required}"
sha256="${SHA256:?SHA256 is required}"
outdir="${OUTDIR:-dist/choco}"
release_url="https://github.com/d-led/console-chess/releases/tag/v${version}"

mkdir -p "${outdir}/tools"

sed \
  -e "s|__VERSION__|${version}|g" \
  -e "s|__URL__|${url}|g" \
  -e "s|__SHA256__|${sha256}|g" \
  -e "s|__RELEASE_URL__|${release_url}|g" \
  chocolatey/console-chess.nuspec > "${outdir}/console-chess.nuspec"

sed "s|__URL__|${url}|g; s|__SHA256__|${sha256}|g" \
  chocolatey/tools/chocolateyinstall.ps1 > "${outdir}/tools/chocolateyinstall.ps1"

cp chocolatey/tools/chocolateyuninstall.ps1 "${outdir}/tools/chocolateyuninstall.ps1"
cp chocolatey/LICENSE.txt "${outdir}/LICENSE.txt"

choco pack "${outdir}/console-chess.nuspec" --outputdirectory "${outdir}"
