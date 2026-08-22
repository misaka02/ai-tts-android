@echo off
setlocal
cd /d "%~dp0"
set "PATH=C:\Users\s1356\scoop\shims;%PATH%"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish.ps1"
echo.
pause
