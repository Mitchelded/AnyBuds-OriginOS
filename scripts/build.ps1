[CmdletBinding()]
param([ValidateSet('Debug','Release')][string]$Configuration = 'Debug')
. "$PSScriptRoot/common.ps1"
try {
    if ($Configuration -eq 'Release') { Assert-ReleaseSigning }
    Invoke-ProjectGradle -Tasks @(':app:assemble' + $Configuration) -LogName "build-$($Configuration.ToLowerInvariant()).log"
    $variant = $Configuration.ToLowerInvariant()
    $directory = Join-Path $script:ProjectRoot "app/build/outputs/apk/$variant"
    $metadata = Get-Content -LiteralPath (Join-Path $directory 'output-metadata.json') -Raw | ConvertFrom-Json
    if (@($metadata.elements).Count -ne 1) { throw 'Expected one APK output; update the script for split APKs.' }
    $apk = Join-Path $directory $metadata.elements[0].outputFile
    if (-not (Test-Path -LiteralPath $apk -PathType Leaf)) { throw "APK missing: $apk" }
    $target = Join-Path $script:Artifacts "AnyBuds-OriginOS-$($metadata.elements[0].versionName)-$variant.apk"
    Copy-Item -LiteralPath $apk -Destination $target -Force
    $sha = [Security.Cryptography.SHA256]::Create()
    try { $hash = [BitConverter]::ToString($sha.ComputeHash([IO.File]::ReadAllBytes($target))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose() }
    "$hash  $([IO.Path]::GetFileName($target))" | Set-Content -Encoding ASCII "$target.sha256"
    Write-Host "APK: $target"
    Write-Host "SHA256: $hash"
    exit 0
} catch { Write-Error $_ -ErrorAction Continue; exit 1 }
