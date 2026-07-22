# BOntLink-Manager → bonotlink-ui 本体转换工具
# 用途: 将 BOnotLink-Manager 的本体配置转换为 bonotlink-ui 的 OntologyDefinition 格式
# 用法: .\convert-to-bonotlink-ui.ps1

param(
    [string]$OutputPath = "f:/aiproject/bonotlink-ui/app/backend/src/main/resources/ontology",
    [string]$BaseUrl = "http://localhost:8088/bontolink/api",
    [string]$Domain = "",
    [string]$OutputFile = "water-service-ontology.json"
)

$ErrorActionPreference = "Stop"

Write-Host "==> BOntLink-Manager → bonotlink-ui 本体转换" -ForegroundColor Cyan
Write-Host "    API: $BaseUrl" -ForegroundColor Gray
Write-Host "    输出: $OutputPath/$OutputFile" -ForegroundColor Gray
Write-Host ""

# 检查后端
try {
    Invoke-RestMethod -Uri "$BaseUrl/health" -TimeoutSec 3 -ErrorAction Stop | Out-Null
    Write-Host "[√] 后端运行中" -ForegroundColor Green
} catch {
    Write-Host "[×] 后端未运行,请先启动: cd backend && mvnw spring-boot:run" -ForegroundColor Red
    exit 1
}

# 加载对象类型
Write-Host "==> 加载对象类型..." -ForegroundColor Cyan
$classesResp = Invoke-RestMethod -Uri "$BaseUrl/class-meta/classes" -Method GET
$allClasses = $classesResp.data | Where-Object { $_.status -eq 1 }
Write-Host "    $($allClasses.Count) 个启用类" -ForegroundColor Gray

# 加载数据源
$dsResp = Invoke-RestMethod -Uri "$BaseUrl/datasources" -Method GET
$dsMap = @{}
foreach ($ds in $dsResp.data) { $dsMap[$ds.ds_code] = $ds }

# 转换实体
Write-Host "==> 转换实体..." -ForegroundColor Cyan
$entities = @()
$classIdMap = @{}

foreach ($cls in $allClasses) {
    # 获取属性
    $propsUrl = "$BaseUrl/class-meta/classes/$($cls.id)/properties"
    $propsResp = Invoke-RestMethod -Uri $propsUrl -ErrorAction SilentlyContinue
    $props = $propsResp.data | Where-Object { $_.status -eq 1 }

    # 获取数据集
    $detailUrl = "$BaseUrl/resources/object-types/$($cls.id)"
    $detail = (Invoke-RestMethod -Uri $detailUrl -ErrorAction SilentlyContinue).data
    $primaryTable = $detail.classDatasources[0].physical_table
    $primaryDs = $detail.classDatasources[0].ds_code

    # 字段映射
    $fields = @()
    foreach ($prop in $props) {
        $sqlType = switch -Wildcard ($prop.data_type) {
            "xsd:string"   { "VARCHAR" }
            "xsd:integer"  { "INTEGER" }
            "xsd:decimal"  { "DECIMAL" }
            "xsd:boolean"  { "BOOLEAN" }
            "xsd:dateTime" { "TIMESTAMP" }
            "xsd:date"     { "DATE" }
            default        { "VARCHAR" }
        }

        $field = @{
            name = $prop.physical_column ?? $prop.api_name
            label = $prop.display_name ?? $prop.rdfs_label ?? $prop.api_name
            type = $sqlType
        }

        if ($prop.rdfs_comment) { $field.remark = $prop.rdfs_comment }
        if ($prop.prop_type -eq "data" -and $sqlType -in @("INTEGER", "DECIMAL")) {
            $field.metric = $true
        }

        $fields += $field
    }

    $entityName = $cls.display_name ?? $cls.rdfs_label ?? $cls.api_name
    $classIdMap[$cls.id] = $entityName

    $entities += @{
        name = $entityName
        table = $primaryTable
        dataSource = $primaryDs
        description = $cls.rdfs_comment ?? ""
        fields = $fields
    }
}
Write-Host "    转换 $($entities.Count) 个实体" -ForegroundColor Gray

# 转换关系
Write-Host "==> 转换关系..." -ForegroundColor Cyan
$relationships = @()

$linksResp = Invoke-RestMethod -Uri "$BaseUrl/link-types"
foreach ($link in $linksResp.data) {
    if ($link.status -ne "active") { continue }

    $mappingsUrl = "$BaseUrl/link-types/$($link.id)/mappings"
    $mappings = (Invoke-RestMethod -Uri $mappingsUrl -ErrorAction SilentlyContinue).data

    foreach ($m in $mappings) {
        $fromEntity = $classIdMap[$m.class_from_id]
        $toEntity = $classIdMap[$m.class_to_id]
        if (-not $fromEntity -or -not $toEntity) { continue }

        $relationships += @{
            from = @{
                entity = $fromEntity
                field = $m.field_from ?? ""
            }
            to = @{
                entity = $toEntity
                field = $m.field_to ?? ""
            }
            joinType = if ($link.is_identifying) { "INNER" } else { "LEFT" }
            description = $link.rdfs_comment ?? ""
        }
    }
}
Write-Host "    转换 $($relationships.Count) 个关系" -ForegroundColor Gray

# 转换字典
Write-Host "==> 转换字典..." -ForegroundColor Cyan
$dictionaries = @()

$enumsResp = Invoke-RestMethod -Uri "$BaseUrl/enum-types"
foreach ($et in $enumsResp.data) {
    if ($et.status -ne "active") { continue }

    $itemsUrl = "$BaseUrl/enum-types/$($et.id)/items"
    $items = (Invoke-RestMethod -Uri $itemsUrl -ErrorAction SilentlyContinue).data

    $entries = @()
    foreach ($item in $items) {
        $entries += @{
            code = $item.code_value
            label = $item.display_label ?? $item.code_value
        }
    }

    $dictionaries += @{
        name = $et.display_name ?? $et.rdfs_label ?? $et.api_name
        entries = $entries
    }
}
Write-Host "    转换 $($dictionaries.Count) 个字典" -ForegroundColor Gray

# 生成提示词
$promptHints = @(
    "本本体由 BOntLink-Manager 自动转换生成。"
    "实体物理表映射基于 ont_class_ds + physical_table 配置。"
    "关系连接基于 link_type_mapping 的字段映射。"
)

# 组装 OntologyDefinition
$ontology = @{
    entities = $entities
    relationships = $relationships
    dictionaries = $dictionaries
    promptHints = $promptHints
}

# 输出 JSON
if (-not (Test-Path $OutputPath)) {
    New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null
}
$outputFullPath = Join-Path $OutputPath $OutputFile
$ontology | ConvertTo-Json -Depth 10 | Out-File -FilePath $outputFullPath -Encoding UTF8

Write-Host ""
Write-Host "==> 转换完成!" -ForegroundColor Green
Write-Host "    输出: $outputFullPath" -ForegroundColor Gray
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  1. 重启 bonotlink-ui 后端以加载新本体" -ForegroundColor Gray
Write-Host "  2. OntologyManager 会自动扫描 classpath:ontology/*.json" -ForegroundColor Gray
