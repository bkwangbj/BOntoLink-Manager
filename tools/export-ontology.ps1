# BOntoLink02 本体导出工具
# 用途: 将"水利服务业"领域的完整本体配置导出为 JSON,供 bonotlink-ui 项目使用
# 用法: .\export-ontology.ps1 [-OutputPath "e:/aiproject/bonotlink-ui/ontology"]

param(
    [string]$OutputPath = "e:/aiproject/bonotlink-ui/ontology",
    [string]$BaseUrl = "http://localhost:8088/bontolink/api",
    [string]$Domain = "水利服务业"  # 可选:导出特定领域,空=全部
)

$ErrorActionPreference = "Stop"

Write-Host "==> BOntoLink02 本体导出工具" -ForegroundColor Cyan
Write-Host "    输出目录: $OutputPath" -ForegroundColor Gray
Write-Host "    API 地址: $BaseUrl" -ForegroundColor Gray
Write-Host ""

# 创建输出目录
if (-not (Test-Path $OutputPath)) {
    New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null
    Write-Host "[√] 已创建输出目录" -ForegroundColor Green
}

# 检查后端是否运行
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/health" -Method GET -TimeoutSec 3 -ErrorAction Stop
    Write-Host "[√] 后端服务运行中" -ForegroundColor Green
} catch {
    Write-Host "[×] 后端未运行,请先启动: cd backend && mvnw spring-boot:run" -ForegroundColor Red
    exit 1
}

# 1. 导出图谱数据(nodes + edges)
Write-Host ""
Write-Host "==> 导出图谱数据..." -ForegroundColor Cyan
try {
    $graph = Invoke-RestMethod -Uri "$BaseUrl/graph/ontology" -Method GET
    $graphFile = Join-Path $OutputPath "graph-ontology.json"
    $graph | ConvertTo-Json -Depth 10 | Out-File -FilePath $graphFile -Encoding UTF8
    Write-Host "[√] 已导出: graph-ontology.json" -ForegroundColor Green
    Write-Host "    节点数: $($graph.data.nodes.Count)" -ForegroundColor Gray
    Write-Host "    边数:   $($graph.data.edges.Count)" -ForegroundColor Gray
} catch {
    Write-Host "[×] 图谱导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. 导出行业分类树
Write-Host ""
Write-Host "==> 导出行业分类树..." -ForegroundColor Cyan
try {
    $category = Invoke-RestMethod -Uri "$BaseUrl/category/tree" -Method GET
    $catFile = Join-Path $OutputPath "category-tree.json"
    $category | ConvertTo-Json -Depth 10 | Out-File -FilePath $catFile -Encoding UTF8
    Write-Host "[√] 已导出: category-tree.json" -ForegroundColor Green
} catch {
    Write-Host "[×] 分类树导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. 导出对象类型列表(含属性)
Write-Host ""
Write-Host "==> 导出对象类型..." -ForegroundColor Cyan
try {
    $classes = Invoke-RestMethod -Uri "$BaseUrl/class-meta/classes" -Method GET
    $classFile = Join-Path $OutputPath "object-types.json"
    $classes | ConvertTo-Json -Depth 10 | Out-File -FilePath $classFile -Encoding UTF8
    Write-Host "[√] 已导出: object-types.json ($($classes.data.Count) 个类)" -ForegroundColor Green
} catch {
    Write-Host "[×] 对象类型导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. 导出值类型
Write-Host ""
Write-Host "==> 导出值类型..." -ForegroundColor Cyan
try {
    $valueTypes = Invoke-RestMethod -Uri "$BaseUrl/value-types" -Method GET
    $vtFile = Join-Path $OutputPath "value-types.json"
    $valueTypes | ConvertTo-Json -Depth 10 | Out-File -FilePath $vtFile -Encoding UTF8
    Write-Host "[√] 已导出: value-types.json ($($valueTypes.data.Count) 个)" -ForegroundColor Green
} catch {
    Write-Host "[×] 值类型导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. 导出枚举类型
Write-Host ""
Write-Host "==> 导出枚举类型..." -ForegroundColor Cyan
try {
    $enumTypes = Invoke-RestMethod -Uri "$BaseUrl/enum-types" -Method GET
    $etFile = Join-Path $OutputPath "enum-types.json"
    $enumTypes | ConvertTo-Json -Depth 10 | Out-File -FilePath $etFile -Encoding UTF8
    Write-Host "[√] 已导出: enum-types.json ($($enumTypes.data.Count) 个)" -ForegroundColor Green
} catch {
    Write-Host "[×] 枚举类型导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 6. 导出链接类型
Write-Host ""
Write-Host "==> 导出链接类型..." -ForegroundColor Cyan
try {
    $linkTypes = Invoke-RestMethod -Uri "$BaseUrl/link-types" -Method GET
    $ltFile = Join-Path $OutputPath "link-types.json"
    $linkTypes | ConvertTo-Json -Depth 10 | Out-File -FilePath $ltFile -Encoding UTF8
    Write-Host "[√] 已导出: link-types.json ($($linkTypes.data.Count) 个)" -ForegroundColor Green
} catch {
    Write-Host "[×] 链接类型导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 7. 导出共享属性
Write-Host ""
Write-Host "==> 导出共享属性..." -ForegroundColor Cyan
try {
    $sharedProps = Invoke-RestMethod -Uri "$BaseUrl/shared-properties" -Method GET
    $spFile = Join-Path $OutputPath "shared-properties.json"
    $sharedProps | ConvertTo-Json -Depth 10 | Out-File -FilePath $spFile -Encoding UTF8
    Write-Host "[√] 已导出: shared-properties.json ($($sharedProps.data.Count) 个)" -ForegroundColor Green
} catch {
    Write-Host "[×] 共享属性导出失败: $($_.Exception.Message)" -ForegroundColor Red
}

# 生成导出元数据
$metadata = @{
    exportTime = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    domain = $Domain
    version = "1.0.0"
    source = "BOntoLink02"
    files = @(
        "graph-ontology.json",
        "category-tree.json",
        "object-types.json",
        "value-types.json",
        "enum-types.json",
        "link-types.json",
        "shared-properties.json"
    )
}
$metaFile = Join-Path $OutputPath "export-metadata.json"
$metadata | ConvertTo-Json -Depth 5 | Out-File -FilePath $metaFile -Encoding UTF8

Write-Host ""
Write-Host "==> 导出完成!" -ForegroundColor Green
Write-Host "    输出目录: $OutputPath" -ForegroundColor Gray
Write-Host "    元数据:   export-metadata.json" -ForegroundColor Gray
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  1. 将导出文件复制到 bonotlink-ui 项目" -ForegroundColor Gray
Write-Host "  2. 在 bonotlink-ui 中使用 import 功能加载这些配置" -ForegroundColor Gray
