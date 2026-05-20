@echo off
setlocal

set "GOPATH_BIN=%GOPATH%\bin"

:: Resolve kwokctl path
if exist "%GOPATH_BIN%\kwokctl.exe" (
    set "KWOKCTL=%GOPATH_BIN%\kwokctl.exe"
) else (
    set "KWOKCTL=kwokctl"
)

echo.
echo === KWOK Cluster Teardown ===

echo.
echo [+] Deleting KWOK cluster...
call "%KWOKCTL%" delete cluster --name=kwok 2>nul
if errorlevel 1 (
    call "%KWOKCTL%" delete cluster 2>nul
)

echo.
echo Cleanup done
endlocal
