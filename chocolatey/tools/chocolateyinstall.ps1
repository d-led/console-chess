# Downloads and installs the console-chess native binary.
# The URL and checksum are substituted by scripts/generate-chocolatey-package.sh.
$ErrorActionPreference = 'Stop'
$toolsDir = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$url      = '__URL__'
$checksum = '__SHA256__'

Install-ChocolateyZipPackage -PackageName 'console-chess' -Url "$url" -UnzipLocation "$toolsDir" -Checksum "$checksum" -ChecksumType 'sha256'
