# 测试硅基流动 Qwen/Qwen2.5-7B-Instruct 模型调用
# 使用配置文件中的 API key

# 从配置文件中读取 API key
$configPath = "F:\aiProject\BOnotLink-Manager\backend\bontolink-admin\src\main\resources\application.yml"
$configContent = Get-Content $configPath -Raw

# 提取 API key
if ($configContent -match "siliconflow:\s*\n\s*api-key:\s*(sk-[a-zA-Z0-9]+)") {
    $apiKey = $Matches[1]
    Write-Host "找到 API key: $apiKey" -ForegroundColor Green
} else {
    Write-Host "未找到硅基流动 API key" -ForegroundColor Red
    exit 1
}

# 测试 API 调用
$url = "https://api.siliconflow.cn/v1/chat/completions"
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Bearer $apiKey"
}

$body = @{
    model = "Qwen/Qwen2.5-7B-Instruct"
    messages = @(
        @{
            role = "system"
            content = "You are a helpful assistant."
        },
        @{
            role = "user"
            content = "你好，请介绍一下你自己"
        }
    )
    max_tokens = 200
    temperature = 0.7
} | ConvertTo-Json -Depth 10

Write-Host "正在测试硅基流动 Qwen/Qwen2.5-7B-Instruct 模型..." -ForegroundColor Yellow
Write-Host "API URL: $url" -ForegroundColor Cyan

try {
    $response = Invoke-RestMethod -Uri $url -Method Post -Headers $headers -Body $body -TimeoutSec 30
    
    Write-Host "✅ API 调用成功！" -ForegroundColor Green
    Write-Host "模型: $($response.model)" -ForegroundColor Cyan
    Write-Host "响应内容:" -ForegroundColor Yellow
    Write-Host $response.choices[0].message.content -ForegroundColor White
    
    Write-Host "`n📊 使用统计:" -ForegroundColor Magenta
    Write-Host "  提示 tokens: $($response.usage.prompt_tokens)" -ForegroundColor Gray
    Write-Host "  完成 tokens: $($response.usage.completion_tokens)" -ForegroundColor Gray
    Write-Host "  总 tokens: $($response.usage.total_tokens)" -ForegroundColor Gray
    
    return $true
} catch {
    Write-Host "❌ API 调用失败: $($_.Exception.Message)" -ForegroundColor Red
    
    # 尝试读取错误响应
    try {
        $errorResponse = $_.ErrorDetails.Message
        if ($errorResponse) {
            Write-Host "错误详情: $errorResponse" -ForegroundColor Red
        }
    } catch {}
    
    return $false
}