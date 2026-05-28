# KWOK Tools Installation Script for Windows
# Requires: PowerShell 5.1+, Internet access

Write-Host "=== KWOK Tools Installer ===" -ForegroundColor Cyan

function Install-Winget {
    param($Package)
    if (Get-Command winget -ErrorAction SilentlyContinue) {
        winget install --id $Package --silent
        return $true
    }
    return $false
}

function Install-Chocolatey {
    param($Package)
    if (Get-Command choco -ErrorAction SilentlyContinue) {
        choco install $Package -y
        return $true
    }
    return $false
}

# Ensure Python is on PATH early
$pythonPaths = @(
    "C:\Python314\python.exe",
    "C:\Python312\python.exe",
    "C:\Python311\python.exe",
    "C:\Python310\python.exe",
    "$env:LOCALAPPDATA\Programs\Python\Python314\python.exe",
    "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
    "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe"
)
$pythonExe = $null
foreach ($p in $pythonPaths) {
    if (Test-Path $p) { $pythonExe = $p; break }
}
if (-not $pythonExe) {
    try { $pythonExe = (where.exe python 2>$null | Select-Object -First 1) } catch {}
}
if ($pythonExe) {
    $pyDir = Split-Path $pythonExe -Parent
    $env:PATH = "$pyDir;$pyDir\Scripts;" + $env:PATH
}

# Locate Go (may not be in PATH yet)
function Get-GoExe {
    foreach ($p in @("C:\Go\bin\go.exe", "$env:LOCALAPPDATA\Programs\Go\bin\go.exe",
                     "C:\Program Files\Go\bin\go.exe", "$env:GOPATH\bin\go.exe")) {
        if (Test-Path $p) { return $p }
    }
    # Fallback: search
    foreach ($dir in @("C:\Go", "$env:GOPATH\go", "$env:LOCALAPPDATA\Programs\Go", "C:\Program Files\Go")) {
        $candidate = Join-Path $dir "bin\go.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    return "go"
}

$goExe = Get-GoExe
$gopathBin = (& $goExe env GOPATH).Trim() + "\bin"

Write-Host "`n=== KWOK Tools Installer ===" -ForegroundColor Cyan
Write-Host "  Go      : $goExe"
Write-Host "  GOPATH  : $(& $goExe env GOPATH)"
Write-Host "  GOBIN   : $gopathBin"

# 1. kubectl
Write-Host "`n[+] kubectl..."
$kubectlExe = $null
foreach ($p in @("$env:USERPROFILE\AppData\Local\Microsoft\WindowsApps\kubectl.exe",
                 "$gopathBin\kubectl.exe")) {
    if (Test-Path $p) { $kubectlExe = $p; break }
}
if (-not $kubectlExe) { try { $kubectlExe = (where.exe kubectl 2>$null | Select-Object -First 1) } catch {} }

if (-not $kubectlExe -or -not (Test-Path $kubectlExe)) {
    Write-Host "  Downloading kubectl..."
    curl.exe -sSLO "https://dl.k8s.io/release/v1.30.0/bin/windows/amd64/kubectl.exe"
    if (Test-Path kubectl.exe) {
        $destDir = "$env:USERPROFILE\AppData\Local\Microsoft\WindowsApps"
        if (-not (Test-Path $destDir)) { New-Item -ItemType Directory $destDir -Force | Out-Null }
        Move-Item kubectl.exe "$destDir\kubectl.exe" -Force
        $kubectlExe = "$destDir\kubectl.exe"
    }
}
if ($kubectlExe) {
    Write-Host "  kubectl OK ($kubectlExe)"
} else {
    Write-Host "  kubectl not found"
}

# 2. Go
Write-Host "`n[+] Go..."
Write-Host "  Go version: $(& $goExe version)"

# 3. Docker
Write-Host "`n[+] Docker..."
$dockerExe = $null
try { $dockerExe = (where.exe docker 2>$null | Select-Object -First 1) } catch {}
if (-not $dockerExe) {
    Write-Host "  Docker not found — install Docker Desktop manually or via winget."
} else {
    Write-Host "  Docker OK ($(docker --version))"
}

# 4. Refresh PATH
$env:PATH = "$gopathBin;" + [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path", "User")

# 5. kind
Write-Host "`n[+] kind..."
$kindExe = $null
try { $kindExe = (where.exe kind 2>$null | Select-Object -First 1) } catch {}
if (-not $kindExe) {
    Write-Host "  Installing kind..."
    & $goExe install sigs.k8s.io/kind@v0.26.0 2>&1 | Out-Null
}
$version = (& kind version 2>&1 | Select-Object -First 1)
Write-Host "  kind: $version"

# 6. KWOK
Write-Host "`n[+] KWOK..."
$env:GOFLAGS = '-buildvcs=false'
& $goExe install sigs.k8s.io/kwok/cmd/kwokctl@latest 2>&1 | Out-Null
& $goExe install sigs.k8s.io/kwok/cmd/kwok@latest   2>&1 | Out-Null

# Verification
Write-Host "`n=== Verification ==="
& kubectl version --client 2>&1 | Out-Null; Write-Host "  kubectl OK"
& kwokctl --version       2>&1 | Out-Null; Write-Host "  kwokctl OK"

Write-Host "`nAll tools installed." -ForegroundColor Green
