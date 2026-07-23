<#
.SYNOPSIS
    BOntoLink API_NAME 冲突检测工具
.DESCRIPTION
    检测数据库中所有 api_name 字段的重复和冲突情况
    包括表内重复、跨表冲突、命名规范检查等
.PARAMETER DbPath
    数据库文件路径，默认为 backend/bontolink.db
.PARAMETER OutputFile
    输出结果文件路径（可选），默认只显示到控制台
.EXAMPLE
    .\check-api-name.ps1
.EXAMPLE
    .\check-api-name.ps1 -DbPath "C:\path\to\bontolink.db" -OutputFile "conflicts.txt"
#>

param(
    [string]$DbPath = "c:\beiktech-jyx\BOntoLink02\backend\bontolink.db",
    [string]$OutputFile = ""
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptDir "check-api-name-conflicts.sql"

Write-Host "==============================================`n" -ForegroundColor Cyan
Write-Host "  BOntoLink API_NAME 冲突检测工具" -ForegroundColor Green
Write-Host "`n==============================================" -ForegroundColor Cyan

# 检查数据库文件是否存在
if (-not (Test-Path $DbPath)) {
    Write-Host "[错误] 数据库文件不存在: $DbPath" -ForegroundColor Red
    exit 1
}

# 检查 SQL 脚本文件是否存在
if (-not (Test-Path $sqlFile)) {
    Write-Host "[错误] SQL 脚本文件不存在: $sqlFile" -ForegroundColor Red
    exit 1
}

# 检查 sqlite3 命令是否可用
try {
    $null = Get-Command sqlite3 -ErrorAction Stop
} catch {
    Write-Host "[错误] 未找到 sqlite3 命令，请先安装 SQLite" -ForegroundColor Red
    Write-Host "下载地址: https://www.sqlite.org/download.html" -ForegroundColor Yellow
    exit 1
}

Write-Host "`n[信息] 数据库路径: $DbPath" -ForegroundColor Gray
Write-Host "[信息] SQL 脚本: $sqlFile" -ForegroundColor Gray

# 执行检测
Write-Host "`n[执行] 开始检测..." -ForegroundColor Yellow

if ($OutputFile) {
    # 输出到文件
    $OutputFile = [System.IO.Path]::GetFullPath($OutputFile)
    sqlite3 $DbPath < $sqlFile | Out-File -FilePath $OutputFile -Encoding utf8
    Write-Host "`n[完成] 检测结果已保存到: $OutputFile" -ForegroundColor Green

    # 同时显示摘要
    Write-Host "`n[摘要] 文件前 50 行预览:" -ForegroundColor Cyan
    Get-Content $OutputFile -Head 50
} else {
    # 只输出到控制台
    sqlite3 $DbPath < $sqlFile
    Write-Host "`n[完成] 检测完成" -ForegroundColor Green
}

Write-Host "`n==============================================`n" -ForegroundColor Cyan
