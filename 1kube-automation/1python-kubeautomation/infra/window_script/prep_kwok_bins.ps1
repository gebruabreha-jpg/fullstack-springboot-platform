# KWOK Cluster Binary Pre-population for Windows
# Workaround: kwokctl create cluster fails with "A required privilege is not held by the client"
# when creating symlinks on Windows without Administrator. We pre-copy all binaries instead.
$ErrorActionPreference = 'Stop'

$cacheDir  = Join-Path $env:USERPROFILE ".kwok\cache\https"
$clusterBin = Join-Path $env:USERPROFILE ".kwok\clusters\kwok\bin"

if (-not (Test-Path $clusterBin)) { New-Item -ItemType Directory -Path $clusterBin -Force | Out-Null }

$items = @(
    @{ Url = "https://github.com/etcd-io/etcd/releases/download/v3.5.21/etcd-v3.5.21-windows-amd64.zip"; Name = "etcd.zip"; Zip = $true; Extract = @("etcd.exe") },
    @{ Url = "https://github.com/etcd-io/etcd/releases/download/v3.5.21/etcd-v3.5.21-windows-amd64.zip";        Name = "etcdctl.zip"; Zip = $true; Extract = @("etcdctl.exe") },
    @{ Url = "https://github.com/kwok-ci/k8s/releases/download/v1.33.0-kwok.0-windows-amd64/kube-apiserver.exe";         Name = "kube-apiserver.exe"; Zip = $false },
    @{ Url = "https://github.com/kwok-ci/k8s/releases/download/v1.33.0-kwok.0-windows-amd64/kube-controller-manager.exe"; Name = "kube-controller-manager.exe"; Zip = $false },
    @{ Url = "https://github.com/kwok-ci/k8s/releases/download/v1.33.0-kwok.0-windows-amd64/kube-scheduler.exe";         Name = "kube-scheduler.exe"; Zip = $false },
    @{ Url = "https://github.com/kubernetes-sigs/kwok/releases/download/v0.7.0/kwok-windows-amd64.exe";                 Name = "kwok-controller.exe"; Zip = $false },
    @{ Url = "https://dl.k8s.io/release/v1.33.0/bin/windows/amd64/kubectl.exe"; Name = "kubectl.exe"; Zip = $false }
)

Write-Host "`n=== KWOK Binary Pre-population ===" -ForegroundColor Cyan
foreach ($item in $items) {
    $cached = Join-Path $cacheDir $item.Name
    $binDir = $clusterBin

    # Handle downloaded file(s)
    $files = @()
    if ($item.Zip) {
        if (-not (Test-Path $cached)) {
            Write-Host "`n[DOWNLOAD] $($item.Name)..."
            & curl.exe -L -sS -o $cached $item.Url
            if (-not (Test-Path $cached)) { Write-Host "  [FAIL] download failed"; continue }
        }
        # Extract from zip
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $zipEntries = [System.IO.Compression.ZipFile]::OpenRead($cached).Entries |
            Where-Object { $_.FullName -match '\.exe$' -and -not $_.FullName.StartsWith("__MACOSX") }
        foreach ($found in $zipEntries) { $files += $found.FullName }
    } else {
        if (-not (Test-Path $cached)) {
            Write-Host "`n[DOWNLOAD] $($item.Name)..."
            & curl.exe -L -sS -o $cached $item.Url
            if (-not (Test-Path $cached)) { Write-Host "  [FAIL] download failed"; continue }
        }
        $files = @($item.Name)
    }

    foreach ($f in $files) {
        $dest = Join-Path $binDir (Split-Path $f -Leaf)
        if ($item.Zip) {
            # Extract from zip
            Add-Type -AssemblyName System.IO.Compression.FileSystem
            $zipStream = [System.IO.Compression.ZipFile]::OpenRead($cached)
            $zipEntry  = $zipStream.Entries | Where-Object { $_.FullName -eq $f -or $_.FullName -match ('[\\/]' + [regex]::Escape(Split-Path $f -Leaf) + '$') } | Select-Object -First 1
            if ($zipEntry) {
                $zipEntry.ExtractToFile($dest, $true)
                Write-Host "  [OK] $($zipEntry.FullName) -> $binDir"
            }
            $zipStream.Dispose()
        } else {
            Copy-Item $cached $dest -Force
            Write-Host "  [OK] $f -> $binDir"
        }
    }
}

Write-Host "`n=== Cluster bin directory ==="
Get-ChildItem $clusterBin | Select-Object Name, @{ N='MB'; E={[math]::Round($_.Length/1MB,1)}} | Format-Table -AutoSize
