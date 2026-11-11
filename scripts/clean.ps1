[CmdletBinding()]
param()
. "$PSScriptRoot/common.ps1"
try {
    Invoke-ProjectGradle -Tasks @(':app:clean') -LogName 'clean.log'
    Write-Host 'Gradle module outputs cleaned. Exported APKs and logs remain in artifacts/.'
    exit 0
} catch { Write-Error $_ -ErrorAction Continue; exit 1 }
