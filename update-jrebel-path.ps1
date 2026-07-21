#!/usr/bin/env pwsh
# JRebel 扩展升级后手动运行此脚本更新 launch.json 路径
# 使用：.\update-jrebel-path.ps1

$extensionsDir = "$env:USERPROFILE\.vscode\extensions"
$latestJRebel = Get-ChildItem -LiteralPath $extensionsDir -Filter "jrebelbyperforcesoftware.jrebel-*" |
    Sort-Object Name -Descending |
    Select-Object -First 1

if (-not $latestJRebel) {
    Write-Error "未找到 JRebel 扩展"
    exit 1
}

$agentDll = Join-Path $latestJRebel.FullName "agent\jrebel\lib\jrebel64.dll"
if (-not (Test-Path -LiteralPath $agentDll)) {
    Write-Error "JRebel DLL 不存在: $agentDll"
    exit 1
}

# JSON 格式需要 4 个反斜杠（字符串转义 \\ 表示 \，JSON 解析后再转义一次）
$jsonPath = $agentDll.Replace('\', '\\\\')

Write-Host "找到最新 JRebel: $($latestJRebel.Name)" -ForegroundColor Cyan
Write-Host "DLL 路径: $agentDll" -ForegroundColor Cyan
Write-Host ""
Write-Host "请手动更新 .vscode/launch.json 中的 vmArgs:" -ForegroundColor Yellow
Write-Host ""
Write-Host '  "vmArgs": "-agentpath:' -NoNewline -ForegroundColor Green
Write-Host $jsonPath -NoNewline -ForegroundColor White
Write-Host ' -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8",' -ForegroundColor Green
Write-Host ""
Write-Host "（已复制到剪贴板）" -ForegroundColor Gray

# 复制到剪贴板
$clipText = "-agentpath:$jsonPath"
$clipText | Set-Clipboard
