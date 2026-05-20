@echo off
setlocal enabledelayedexpansion

set "INFRA_DIR=%~dp0..\config"
set "GOPATH_BIN=%GOPATH%\bin"
set "KUBECONFIG="

:: Resolve kwokctl path
if exist "%GOPATH_BIN%\kwokctl.exe" (
    set "KWOKCTL=%GOPATH_BIN%\kwokctl.exe"
) else (
    set "KWOKCTL=kwokctl"
)

:: Resolve kubectl path
if exist "%USERPROFILE%\AppData\Local\Microsoft\WindowsApps\kubectl.exe" (
    set "KUBECTL=kubectl"
) else (
    set "KUBECTL=kubectl"
)

echo.
echo === KWOK Cluster Setup ===

echo.
echo [+] Creating KWOK cluster...
call "%KWOKCTL%" create cluster

echo.
echo [+] Getting kubeconfig...
for /f "delims=" %%k in ('call "%KWOKCTL%" kubeconfig-path 2^>nul') do set "KUBECONFIG=%%k"
set "KUBECONFIG=%KUBECONFIG: =%"
echo Kubeconfig: %KUBECONFIG%

echo.
echo [+] Waiting for cluster...
call "%KUBECTL%" wait --for=condition=Ready node --all --timeout=60s 2>nul

echo.
echo [+] Applying namespace...
call "%KUBECTL%" apply -f "%INFRA_DIR%\namespace.yaml" 2>nul

echo.
echo [+] Applying fake resources...
call "%KUBECTL%" apply -f "%INFRA_DIR%\fake-pods.yaml"
call "%KUBECTL%" apply -f "%INFRA_DIR%\fake-deployment.yaml"

echo.
echo [+] Waiting for pods...
call "%KUBECTL%" wait pod --all -n kwok-test --for=condition=Ready --timeout=60s 2>nul

echo.
echo Setup complete
echo Kubeconfig: %KUBECONFIG%
endlocal
