# KWOK Cluster Setup for Windows (PowerShell) — no admin needed
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$scriptDir  = Split-Path $MyInvocation.MyCommand.Path -Parent
$projectDir = Split-Path $scriptDir -Parent
$infraDir   = Join-Path $projectDir "infra\config"

# locate kwokctl
$goBin = $null
try { $src = (Get-Command go -ErrorAction SilentlyContinue).Source; if ($src) { $goBin = Split-Path $src -Parent } } catch {}
if (-not $goBin) { $c = & "C:\Go\bin\go.exe" env GOPATH 2>$null; if ($c) { $goBin = $c.Trim() + "\bin" } }
if (-not $goBin) { $goBin = "C:\Go\bin" }
$env:PATH = "$goBin;" + $env:PATH

$clusterDir = Join-Path $env:USERPROFILE ".kwok\clusters\kwok"
$clusterBin = Join-Path $clusterDir "bin"
$kubeconf   = Join-Path $clusterDir "kubeconfig.yaml"
$tmpZip     = Join-Path $env:TEMP "kwok_dl.zip"
$nhctl      = Join-Path $clusterBin "kubectl.exe"

if (-not (Test-Path $clusterBin)) { New-Item -ItemType Directory -Path $clusterBin -Force | Out-Null }

function Dl([string]$Url, [string]$Path) {
    if (Test-Path $Path) { return }
    & curl.exe -L -sS -o $Path $Url 2>$null
}
function Unzip([string]$ZipPath, [string]$ExeName) {
    $outFile = Join-Path $clusterBin $ExeName
    if (Test-Path $outFile) { return }
    if (-not (Test-Path $ZipPath)) { return }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
    $entry = $zip.Entries | Where-Object { $_.FullName -match [regex]::Escape($ExeName) } | Select-Object -First 1
    if ($entry) {
        $fs = [System.IO.File]::Create($outFile)
        $s  = $entry.Open()
        $s.CopyTo($fs); $fs.Close(); $s.Close()
    }
    $zip.Dispose()
}

Write-Host ""
Write-Host "=== KWOK Cluster Setup (Windows) ===" -ForegroundColor Cyan

# ---- 1. Download ----
Write-Host ""
Write-Host "[1] Downloading cluster binaries"
$arch  = "https://github.com/etcd-io/etcd/releases/download/v3.5.21/etcd-v3.5.21-windows-amd64.zip"
$k8s   = "https://github.com/kwok-ci/k8s/releases/download/v1.33.0-kwok.0-windows-amd64"
$kwokDl   = "https://github.com/kubernetes-sigs/kwok/releases/download/v0.7.0/kwok-windows-amd64.exe"
$kubectlDl = "https://dl.k8s.io/release/v1.33.0/bin/windows/amd64/kubectl.exe"

Dl $arch $tmpZip; Unzip $tmpZip "etcd.exe"; Unzip $tmpZip "etcdctl.exe"
Dl "$k8s\kube-apiserver.exe"             (Join-Path $clusterBin "kube-apiserver.exe")
Dl "$k8s\kube-controller-manager.exe"    (Join-Path $clusterBin "kube-controller-manager.exe")
Dl "$k8s\kube-scheduler.exe"             (Join-Path $clusterBin "kube-scheduler.exe")
Dl $kwokDl                               (Join-Path $clusterBin "kwok-controller.exe")
Dl $kubectlDl                            (Join-Path $clusterBin "kubectl.exe")

# ---- 2. Certs + kubeconfig ----
Write-Host ""
Write-Host "[2] Generating TLS certs and kubeconfig"
$scriptDir2 = Split-Path $MyInvocation.MyCommand.Path -Parent
& python "$scriptDir2\generate_certs.py"
$rc = $LASTEXITCODE
if ($rc -ne 0) { Write-Host "  Cert generation failed rc=$rc"; exit 1 }

# ---- 3. Start etcd ----
Write-Host ""
Write-Host "[3] Starting etcd"
$etcdDataDir = Join-Path $clusterDir "etcd-data"
if (-not (Test-Path $etcdDataDir)) { New-Item -ItemType Directory -Path $etcdDataDir -Force | Out-Null }
Start-Process -FilePath "$clusterBin\etcd.exe" -ArgumentList @(
    "--data-dir=$etcdDataDir"
    "--listen-client-urls=http://127.0.0.1:2379"
    "--advertise-client-urls=http://127.0.0.1:2379") -WindowStyle Hidden
Start-Sleep 2

# ---- 4. Start kube-apiserver ----
Write-Host ""
Write-Host "[4] Starting kube-apiserver"
$etcdCafile   = Join-Path $clusterDir "ca.crt"
$etcdCertfile = Join-Path $clusterDir "admin.crt"
$etcdKeyfile  = Join-Path $clusterDir "admin.key"
Start-Process -FilePath "$clusterBin\kube-apiserver.exe" -ArgumentList @(
    "--etcd-servers=http://127.0.0.1:2379"
    "--secure-port=16443"
    "--advertise-address=127.0.0.1"
    "--service-cluster-ip-range=10.96.0.0/16"
    "--admission-control=AlwaysAdmit,NamespaceLifecycle,ResourceQuota"
    "--authorization-mode=AlwaysAllow"
    "--requestheader-client-ca-file=$etcdCafile"
    "--proxy-client-cert-file=$etcdCertfile"
    "--proxy-client-key-file=$etcdKeyfile"
    "--tls-cert-file=$etcdCertfile"
    "--tls-private-key-file=$etcdKeyfile"
    "--client-ca-file=$etcdCafile") -WindowStyle Hidden
Start-Sleep 5

# ---- 5. Start controller-manager ----
Write-Host ""
Write-Host "[5] Starting kube-controller-manager"
Start-Process -FilePath "$clusterBin\kube-controller-manager.exe" -ArgumentList @(
    "--master=http://127.0.0.1:16443"
    "--cluster-cidr=10.244.0.0/16"
    "--service-cluster-ip-range=10.96.0.0/16"
    "--root-ca-file=$etcdCafile"
    "--use-service-account-credentials=true"
    "--controllers=*,bootstrapserviceaccount,cloud-node-lifecycle") -WindowStyle Hidden
Start-Sleep 2

# ---- 6. Start scheduler ----
Write-Host ""
Write-Host "[6] Starting kube-scheduler"
Start-Process -FilePath "$clusterBin\kube-scheduler.exe" -ArgumentList @(
    "--master=http://127.0.0.1:16443"
    "--kubeconfig=$kubeconf") -WindowStyle Hidden
Start-Sleep 2

# ---- 7. Start kwok-controller ----
Write-Host ""
Write-Host "[7] Starting kwok-controller"
$kwokYaml = Join-Path $clusterDir "kwok.yaml"
Start-Process -FilePath "$clusterBin\kwok-controller.exe" -ArgumentList @(
    "--kubeconfig=$kubeconf"
    "--config=$kwokYaml") -WindowStyle Hidden
Start-Sleep 3

# ---- 8. Apply manifests ----
Write-Host ""
Write-Host "[8] Applying namespace"
& $nhctl --kubeconfig $kubeconf apply -f (Join-Path $infraDir "namespace.yaml") 2>$null

Write-Host "  Applying fake-pods.yaml"
& $nhctl --kubeconfig $kubeconf apply -f (Join-Path $infraDir "fake-pods.yaml") 2>$null

Write-Host "  Applying fake-deployment.yaml"
& $nhctl --kubeconfig $kubeconf apply -f (Join-Path $infraDir "fake-deployment.yaml") 2>$null

Write-Host "  Applying configmap.yaml"
& $nhctl --kubeconfig $kubeconf apply -f (Join-Path $infraDir "configmap.yaml") 2>$null

Write-Host ""
Write-Host "[9] Waiting for pods Ready"
& $nhctl --kubeconfig $kubeconf wait pod --all -n kwok-test --for=condition=Ready --timeout=60s 2>$null

Write-Host ""
Write-Host "Setup complete" -ForegroundColor Green
