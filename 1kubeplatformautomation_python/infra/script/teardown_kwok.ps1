# KWOK Cluster Teardown for Windows (PowerShell)
$ErrorActionPreference = 'Stop'

# Locate kwokctl
$scriptDir  = Split-Path $MyInvocation.MyCommand.Path -Parent
$projectDir = Split-Path $scriptDir -Parent
$goBin      = Join-Path $projectDir "go\bin"
if (-not (Test-Path $goBin)) {
    try { $goBin = (& "C:\Go\bin\go.exe" env GOPATH).Trim() + "\bin" } catch {}
}
if (-not (Test-Path $goBin)) { $goBin = $env:GOPATH + "\bin" }
$kwokctl = if (Test-Path (Join-Path $goBin "kwokctl.exe")) { Join-Path $goBin "kwokctl.exe" } else { "kwokctl" }

Write-Host "`n=== KWOK Cluster Teardown ===" -ForegroundColor Cyan

& $kwokctl delete cluster --name=kwok 2>$null
if ($LASTEXITCODE -ne 0) {
    & $kwokctl delete cluster 2>$null
}

Write-Host "`nCleanup done" -ForegroundColor Green
