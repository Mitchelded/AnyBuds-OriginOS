$ErrorActionPreference = 'Stop'
$script:ProjectRoot = Split-Path $PSScriptRoot -Parent
$script:Artifacts = Join-Path $script:ProjectRoot 'artifacts'

function Invoke-ProjectGradle {
    param([string[]]$Tasks, [string]$LogName)
    New-Item -ItemType Directory -Force -Path $script:Artifacts | Out-Null
    Push-Location $script:ProjectRoot
    try {
        $arguments = @('--no-daemon', '--console=plain', '--stacktrace') + $Tasks
        Write-Host "Gradle: $($Tasks -join ' ')"
        # Windows PowerShell turns native stderr into ErrorRecords. Preserve warnings
        # in the log and use the process exit code to decide success.
        $ErrorActionPreference = 'Continue'
        if ($env:OS -eq 'Windows_NT') {
            & (Join-Path $script:ProjectRoot 'gradlew.bat') @arguments 2>&1 |
                ForEach-Object { $_.ToString() } |
                Tee-Object -FilePath (Join-Path $script:Artifacts $LogName)
        } else {
            & bash (Join-Path $script:ProjectRoot 'gradlew') @arguments 2>&1 |
                ForEach-Object { $_.ToString() } |
                Tee-Object -FilePath (Join-Path $script:Artifacts $LogName)
        }
        $gradleExitCode = $LASTEXITCODE
        $ErrorActionPreference = 'Stop'
        if ($gradleExitCode -ne 0) { throw "Gradle failed with exit code $gradleExitCode. See artifacts/$LogName" }
    } finally { Pop-Location }
}

function Assert-ReleaseSigning {
    foreach ($name in @('ANYBUDS_SIGNING_STORE_FILE', 'ANYBUDS_SIGNING_STORE_PASSWORD', 'ANYBUDS_SIGNING_KEY_ALIAS', 'ANYBUDS_SIGNING_KEY_PASSWORD')) {
        if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))) { throw "Release signing requires $name." }
    }
    if (-not (Test-Path -LiteralPath $env:ANYBUDS_SIGNING_STORE_FILE -PathType Leaf)) { throw 'Release keystore file does not exist.' }
}
