[CmdletBinding()]
param()
. "$PSScriptRoot/common.ps1"
try {
    Invoke-ProjectGradle -Tasks @(':app:testDebugUnitTest', ':app:lintDebug') -LogName 'test.log'
    $testReport = Join-Path $script:ProjectRoot 'app/build/reports/tests/testDebugUnitTest'
    if (Test-Path -LiteralPath $testReport) { Write-Host "Unit-test report: $testReport" }
    else { Write-Host 'No unit-test report generated. With no test sources, this task does not test application behavior.' }
    Write-Host 'Lint report: app/build/reports/lint-results-debug.html'
    exit 0
} catch { Write-Error $_ -ErrorAction Continue; exit 1 }
