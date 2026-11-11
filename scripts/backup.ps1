[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$destination = Join-Path $root ".backups/$stamp"
New-Item -ItemType Directory -Path $destination -Force | Out-Null
Push-Location $root
try {
    & git bundle create (Join-Path $destination 'repository.bundle') --all
    if ($LASTEXITCODE -ne 0) { throw 'Git bundle failed.' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::Open((Join-Path $destination 'working-tree.zip'), 'Create')
    try {
        Get-ChildItem -LiteralPath $root -Force -Recurse -File | Where-Object {
            $relative = $_.FullName.Substring($root.Length + 1)
            $relative -notmatch '^(\.git|\.backups|\.gradle|\.tools|artifacts|rtk)[\\/]' -and
            $relative -notmatch '(^|[\\/])build[\\/]'
        } | ForEach-Object {
            $relative = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
            [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $_.FullName, $relative) | Out-Null
        }
    } finally { $archive.Dispose() }
    & git status --short --branch | Set-Content -Encoding UTF8 (Join-Path $destination 'git-status.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Git status failed.' }
    $hashes = Get-ChildItem -LiteralPath $destination -File | Where-Object Name -ne 'checksums.csv' |
        Get-FileHash -Algorithm SHA256 | Select-Object Path,Hash
    $hashes | Export-Csv -NoTypeInformation (Join-Path $destination 'checksums.csv')
    Write-Host "Backup: $destination (contains private local files; do not publish)"
} finally { Pop-Location }
