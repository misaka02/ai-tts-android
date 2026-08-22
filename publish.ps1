[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:PATH = "C:\Users\s1356\scoop\shims;$env:PATH"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  AI TTS Engine for Android - GitHub 开源自动发布器" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1/3] 正在检查 GitHub 登录状态..." -ForegroundColor Yellow
$null = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "⚠️ 尚未登录 GitHub 账号，即将为您自动打开浏览器进行一键授权..." -ForegroundColor Yellow
    Write-Host "👉 在弹出的浏览器页面中点击绿色的 [Authorize github] 按钮即可完成！" -ForegroundColor Green
    Write-Host ""
    gh auth login -w -p https
}

Write-Host ""
Write-Host "[2/3] 正在为您创建 GitHub 公开开源仓库..." -ForegroundColor Yellow
gh repo create ai-tts-android --public --source=. --remote=origin --description "安卓系统级 AI 在线语音大模型 TTS 引擎替代软件 (100% 全AI开发与归档发布)" 2>&1 | Out-Null

Write-Host ""
Write-Host "[3/3] 正在推送主分支代码与 v1.0.0 归档发布标签..." -ForegroundColor Yellow
git push -u origin main --tags

Write-Host ""
Write-Host "========================================================" -ForegroundColor Green
Write-Host "🎉 恭喜！项目已成功全自动开源并推送到您的 GitHub！" -ForegroundColor Green
Write-Host "🤖 GitHub Actions 云端正在自动构建并发布正式 Release 安装包。" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
