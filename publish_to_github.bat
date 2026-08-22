@echo off
setlocal
cd /d "%~dp0"
set "PATH=C:\Users\s1356\scoop\shims;%PATH%"

echo ========================================================
echo   AI Text-To-Speech Engine for Android - GitHub Publisher
echo ========================================================
echo.

echo [1/3] Checking GitHub authentication status...
gh auth status >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Not logged in yet.
    echo [*] Opening your web browser for 1-click GitHub authorization...
    echo [*] Please click the green [Authorize github] button on the browser page.
    echo.
    gh auth login -w -p https
)

echo.
echo [2/3] Creating public GitHub repository 'ai-tts-android'...
gh repo create ai-tts-android --public --source=. --remote=origin --description "AI Text-To-Speech Engine for Android (100% AI Developed & Final Archived Release)" >nul 2>&1

echo.
echo [3/3] Pushing source code and v1.0.0 release tag...
git push -u origin main --tags

echo.
echo ========================================================
echo [OK] Success! Project is now open-sourced on your GitHub!
echo [OK] GitHub Actions is building the Release APK now.
echo ========================================================
echo.
pause
