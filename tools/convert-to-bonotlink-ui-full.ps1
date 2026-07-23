# BOntLink-Manager → bonotlink-ui 本体转换工具 (完整版)
# 生成完全兼容 jjdata-ontology.json 格式的本体定义

param(
    [string]$OutputPath = "f:/aiproject/bonotlink-ui/app/backend/src/main/resources/ontology",
    [string]$BaseUrl = "http://localhost:8088/bontolink/api",
    [string]$DomainFilter = "",
    [string]$OutputFile = "water-service-ontology.json"
)

$ErrorActionPreference = "Stop"

Write-Host "==> BOntLink-Manager → bonotlink-ui 完整格式转换" -ForegroundColor Cyan
Write-Host "    输出: $OutputPath/$OutputFile" -ForegroundColor Gray
Write-Host ""

# 检查后端
try {
    Invoke-RestMethod -Uri "$BaseUrl/health" -TimeoutSec 3 -ErrorAction Stop | Out-Null
    Write-Host "[√] 后端运行中" -ForegroundColor Green
} catch {
    Write-Host "[×] 后端未运行" -ForegroundColor Red
    exit 1
}

# ========== 1. 加载对象类型 ==========
Write-Host "==> 加载对象类型..." -ForegroundColor Cyan
$classesResp = Invoke-RestMethod -Uri "$BaseUrl/class-meta/classes"
$allClasses = $classesResp.data | Where-Object { $_.status -eq 1 }
Write-Host "    $($allClasses.Count) 个启用类" -ForegroundColor Gray

# ========== 2. 加载命名空间 ==========
Write-Host "==> 加载命名空间..." -ForegroundColor Cyan
$nsResp = Invoke-RestMethod -Uri "$BaseUrl/namespaces" -ErrorAction SilentlyContinue
$nsMap = @{}  # ns_code -> namespace
if ($nsResp.data) {
    foreach ($ns in $nsResp.data) {
        $nsMap[$ns.ns_code] = $ns
    }
    Write-Host "    $($nsMap.Count) 个命名空间" -ForegroundColor Gray
}

# ========== 3. 加载数据源映射 ==========
$dsResp = Invoke-RestMethod -Uri "$BaseUrl/datasources"
$dsMap = @{}
foreach ($ds in $dsResp.data) {
    $dsMap[$ds.ds_code] = $ds
}

# ========== 4. 加载枚举(作为字典) ==========
Write-Host "==> 加载枚举类型..." -ForegroundColor Cyan
$enumsResp = Invoke-RestMethod -Uri "$BaseUrl/enum-types"
$enumTypes = $enumsResp.data | Where-Object { $_.status -eq "active" }
$dictMap = @{}  # enum_id -> entries

foreach ($et in $enumTypes) {
    $itemsUrl = "$BaseUrl/enum-types/$($et.id)/items"
    $items = (Invoke-RestMethod -Uri $itemsUrl -ErrorAction SilentlyContinue).data

    $entries = @()
    foreach ($item in $items) {
        $entries += @{
            code = $item.code
            label = $item.label ?? $item.code
        }
    }

    $dictMap[$et.id] = @{
        name = $et.display_name ?? $et.rdfs_label ?? $et.api_name
        entries = $entries
    }
}
Write-Host "    $($dictMap.Count) 个字典" -ForegroundColor Gray

# ========== 5. 转换实体 ==========
Write-Host "==> 转换实体..." -ForegroundColor Cyan
$entities = @()
$classIdMap = @{}  # class_id -> entity_name

foreach ($cls in $allClasses) {
    # 获取属性
    $propsUrl = "$BaseUrl/class-meta/classes/$($cls.id)/properties"
    $propsResp = Invoke-RestMethod -Uri $propsUrl -ErrorAction SilentlyContinue
    $props = $propsResp.data | Where-Object { $_.status -eq 1 }

    # 获取数据集(物理表)
    $detailUrl = "$BaseUrl/resources/object-types/$($cls.id)"
    $detail = (Invoke-RestMethod -Uri $detailUrl -ErrorAction SilentlyContinue).data

    if (-not $detail.classDatasources -or $detail.classDatasources.Count -eq 0) {
        Write-Host "    跳过(无物理表): $($cls.display_name)" -ForegroundColor Yellow
        continue
    }

    $primaryTable = $detail.classDatasources[0].physical_table
    $primaryDs = $detail.classDatasources[0].ds_code

    # 字段转换
    $fields = @()
    foreach ($prop in $props) {
        # XSD类型 → SQL类型
        $sqlType = switch -Wildcard ($prop.data_type) {
            "xsd:string"   { "VARCHAR" }
            "xsd:integer"  { "INTEGER" }
            "xsd:long"     { "BIGINT" }
            "xsd:decimal"  { "DECIMAL" }
            "xsd:float"    { "FLOAT" }
            "xsd:double"   { "DOUBLE" }
            "xsd:boolean"  { "BOOLEAN" }
            "xsd:dateTime" { "TIMESTAMP" }
            "xsd:date"     { "DATE" }
            "xsd:time"     { "TIME" }
            default        { "VARCHAR" }
        }

        $field = @{
            name = $prop.physical_column ?? $prop.api_name
            label = $prop.display_name ?? $prop.rdfs_label ?? $prop.api_name
            type = $sqlType
            owlProperty = $prop.api_name  # 保留 OWL 属性名
        }

        # 可选字段
        if ($prop.rdfs_comment) {
            $field.remark = $prop.rdfs_comment
        }

        # 标记指标字段
        if ($prop.prop_type -eq "data" -and $sqlType -in @("INTEGER", "BIGINT", "DECIMAL", "FLOAT", "DOUBLE")) {
            $field.metric = $true
        }

        # 枚举字段: 添加 dickey + dictValues
        if ($prop.range_enum_id -and $dictMap.ContainsKey($prop.range_enum_id)) {
            $field.dickey = $dictMap[$prop.range_enum_id].name
            $field.dictValues = $dictMap[$prop.range_enum_id].entries
        }

        $fields += $field
    }

    $entityName = $cls.display_name ?? $cls.rdfs_label ?? $cls.api_name
    $classIdMap[$cls.id] = $entityName

    # 构建实体定义
    $entity = @{
        name = $entityName
        table = $primaryTable
        dataSource = $primaryDs
        description = $cls.rdfs_comment ?? ""
        fields = $fields
        owlClass = $cls.api_name  # OWL 类名
        owlPrefix = $cls.api_name.Substring(0,1).ToLower() + $cls.api_name.Substring(1)
    }

    # 添加命名空间信息(如果存在)
    if ($cls.ns_code -and $nsMap.ContainsKey($cls.ns_code)) {
        $ns = $nsMap[$cls.ns_code]
        $entity.namespace = @{
            code = $ns.ns_code
            name = $ns.ns_name
            uri = $ns.ns_uri
        }
    }

    $entities += $entity
}
Write-Host "    转换 $($entities.Count) 个实体" -ForegroundColor Gray

# ========== 5. 转换关系 ==========
Write-Host "==> 转换关系..." -ForegroundColor Cyan
$relationships = @()

$linksResp = Invoke-RestMethod -Uri "$BaseUrl/link-types"
foreach ($link in $linksResp.data) {
    if ($link.status -ne "active") { continue }

    $mappingsUrl = "$BaseUrl/link-types/$($link.id)/mappings"
    $mappings = (Invoke-RestMethod -Uri $mappingsUrl -ErrorAction SilentlyContinue).data

    foreach ($m in $mappings) {
        # 跳过无效映射(缺少源或目标类ID)
        if (-not $m.class_from_id -or -not $m.class_to_id) { continue }

        $fromEntity = $classIdMap[$m.class_from_id]
        $toEntity = $classIdMap[$m.class_to_id]
        if (-not $fromEntity -or -not $toEntity) { continue }

        # 判断 JOIN 类型
        # SAME_DB_STRONG: 同数据源 + 强关联(is_identifying=true)
        # SAME_DB_WEAK: 同数据源 + 弱关联
        # CROSS_DB_WEAK: 跨数据源
        $fromDs = ($entities | Where-Object { $_.name -eq $fromEntity }).dataSource
        $toDs = ($entities | Where-Object { $_.name -eq $toEntity }).dataSource

        $joinType = if ($fromDs -ne $toDs) {
            "CROSS_DB_WEAK"
        } elseif ($link.is_identifying) {
            "SAME_DB_STRONG"
        } else {
            "SAME_DB_WEAK"
        }

        $relationships += @{
            name = "$fromEntity→$toEntity"
            from = @{
                entity = $fromEntity
                field = $m.field_from ?? ""
            }
            to = @{
                entity = $toEntity
                field = $m.field_to ?? ""
            }
            joinType = $joinType
            description = $link.rdfs_comment ?? "$fromEntity 通过 $($m.field_from) 关联 $toEntity 的 $($m.field_to)"
        }
    }
}
Write-Host "    转换 $($relationships.Count) 个关系" -ForegroundColor Gray

# ========== 6. 转换字典(全局) ==========
Write-Host "==> 转换字典..." -ForegroundColor Cyan
$dictionaries = @()

foreach ($et in $enumTypes) {
    if ($dictMap.ContainsKey($et.id)) {
        $dictionaries += @{
            name = $dictMap[$et.id].name
            entries = $dictMap[$et.id].entries
        }
    }
}
Write-Host "    转换 $($dictionaries.Count) 个全局字典" -ForegroundColor Gray

# ========== 7. 生成 promptHints ==========
$promptHints = @(
    "本本体由 BOntLink-Manager 自动转换生成,来源: $BaseUrl"
    "实体物理表映射基于 ont_class_ds.physical_table 配置"
    "关系连接基于 link_type_mapping 的字段映射"
    "字典数据来源于 ont_enum_type + ont_enum_item"
)

# 添加数据源说明
$dsList = $entities | Select-Object -ExpandProperty dataSource -Unique
foreach ($ds in $dsList) {
    $promptHints += "数据源 $ds 包含: " + (($entities | Where-Object { $_.dataSource -eq $ds }).name -join ", ")
}

# ========== 8. 组装最终 JSON ==========
$ontology = [ordered]@{
    promptHints = $promptHints
}

# 添加命名空间定义(顶层)
if ($nsMap.Count -gt 0) {
    $namespaces = @()
    foreach ($ns in $nsMap.Values) {
        $namespaces += @{
            code = $ns.ns_code
            name = $ns.ns_name
            uri = $ns.ns_uri
            label = $ns.rdfs_label
            comment = $ns.rdfs_comment
        }
    }
    $ontology.namespaces = $namespaces
}

$ontology.entities = $entities
$ontology.relationships = $relationships
$ontology.dictionaries = $dictionaries

# ========== 9. 输出文件 ==========
if (-not (Test-Path $OutputPath)) {
    New-Item -ItemType Directory -Path $OutputPath -Force | Out-Null
}
$outputFullPath = Join-Path $OutputPath $OutputFile
$json = $ontology | ConvertTo-Json -Depth 15
$json | Out-File -FilePath $outputFullPath -Encoding UTF8

Write-Host ""
Write-Host "==> 转换完成!" -ForegroundColor Green
Write-Host "    输出: $outputFullPath" -ForegroundColor Gray
Write-Host "    大小: $([Math]::Round((Get-Item $outputFullPath).Length / 1KB, 2)) KB" -ForegroundColor Gray
Write-Host ""
Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  1. 重启 bonotlink-ui 后端: cd f:/aiproject/bonotlink-ui/app/backend && mvn spring-boot:run" -ForegroundColor Gray
Write-Host "  2. OntologyManager 会自动加载 classpath:ontology/*.json" -ForegroundColor Gray
Write-Host "  3. 测试自然语言查询: POST /api/query/stream?question=查询..." -ForegroundColor Gray
