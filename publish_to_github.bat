@echo off
chcp 65001 >nul
echo ========================================================
echo   🤖 AI TTS Engine for Android - 一键全自动开源发布脚本
echo ========================================================
echo.

set PATH=C:\Users\s1356\scoop\shims;%PATH%

echo [1/3] 正在检查 GitHub 登录状态...
gh auth status >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ⚠️ 尚未登录 GitHub 账号。
    echo 接下来将为您自动打开浏览器进行一键授权登录：
    echo.
    gh auth login -w -p https
)

echo.
echo [2/3] 正在为您在 GitHub 自动创建开源公开仓库并绑定...
gh repo create ai-tts-android --public --source=. --remote=origin --description "安卓系统级 AI 在线语音大模型 TTS 引擎替代软件 (100% 全AI开发与归档发布)" >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo 仓库已存在或已关联，继续推送...
)

echo.
echo [3/3] 正在自动推送主分支代码及 v1.0.0 标签至 GitHub...
git push -u origin main --tags

echo.
echo ========================================================
echo 🎉 恭喜！项目已成功全自动开源并推送到您的 GitHub！
echo 🤖 GitHub Actions 云端正在自动构建并发布 Release 安装包。
echo ========================================================
pause
