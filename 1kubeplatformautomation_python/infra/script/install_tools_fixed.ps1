# KWOK Tools Installation Script for Windows
# Requires: PowerShell 5.1+, Administrator privileges for some installs

Write-Host "=== KWOK Tools Installer ===" -ForegroundColor Cyan

# Optional: Install via winget/chocolatey if available
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

# 1. Install kubectl
Write-Host "`n[+] kubectl..."
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "  Downloading kubectl..."
    curl.exe -LO "https://dl.k8s.io/release/v1.30.0/bin/windows/amd64/kubectl.exe"
    
    $binDir = "$env:USERPROFILE\AppData\Local\Microsoft\WindowsApps"
    if (-not (Test-Path $binDir)) {
        New-Item -ItemType Directory -Path $binDir -Force | Out-Null
    }
    Move-Item kubectl.exe "$binDir\kubectl.exe" -Force
    Write-Host "  Installed kubectl"
} else {
    Write-Host "  kubectl already installed"
}

# 2. Install Go
Write-Host "`n[+] Go..."
if (-not (Get-Command go -ErrorAction SilentlyContinue)) {
    Write-Host "  Go not found. Install options:"
    Write-Host "    - winget: winget install Go.Go"
    Write-Host "    - chocolatey: choco install golang"
    Write-Host "    - Manual: https://go.dev/dl/"
    
    if (Install-Winget "Go.Go") { return }
    if (Install-Chocolatey "golang") { return }
    
    Write-Host "  Skipping Go installation - install manually and re-run"
    exit 1
}
Write-Host "  Go version: $(go version)"

# 3. Install Docker
Write-Host "`n[+] Docker..."
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "  Docker not found. Install options:"
    Write-Host "    - Docker Desktop: https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"
    Write-Host "    - winget: winget install Docker.DockerDesktop"
    Write-Host "    - chocolatey: choco install docker-desktop"
    
    if (Install-Winget "Docker.DockerDesktop") {
        Write-Host "  Docker Desktop installed. Please launch it and wait for it to start."
        pause
    } else {
        Write-Host "  Skipping Docker installation - install manually and re-run"
    }
} else {
    Write-Host "  Docker version: $(docker --version)"
}

# 4. Install kind (for KWOK)
Write-Host "`n[+] kind..."
if (-not (Get-Command kind -ErrorAction SilentlyContinue)) {
    go install sigs.k8s.io/kind@v0.26.0
    $env:PATH = "$(go env GOPATH)\bin;$env:PATH"
    Write-Host "  Installed kind: $(kind version)"
} else {
    Write-Host "  kind already installed: $(kind version)"
}

# 5. Install KWOK
Write-Host "`n[+] KWOK..."
go install github.com/kubernetes-sigs/kwok/cmd/kwokctl@latest
go install github.com/kubernetes-sigs/kwok/cmd/kwok@latest
$env:PATH = "$(go env GOPATH)\bin;$env:PATH"

# Verify installations
Write-Host "`n=== Verification ==="
kubectl version --client
kwokctl version

Write-Host "`n✔ Tools installed" -ForegroundColor Green
Write-Host "NOTE: Restart PowerShell to refresh PATH for kwokctl"
