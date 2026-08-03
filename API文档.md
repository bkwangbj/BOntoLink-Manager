# BOntoLink 语义工具 API 文档

> 版本：2026-07-31 · 基础路径 `http://localhost:8088/bontolink/api`  
> 响应格式：`{ "code": 0, "msg": "ok", "data": {...} }`（code=0 成功，非0失败）

---

## 目录

- [一、语义工具](#一语义工具)
  - [语义扩展](#1-post-apitoolsemantic-expand--语义扩展)
  - [最小关系子图](#2-post-apitoolsubgraph--最小关系子图)
- [二、Jena / OWL 本体](#二jena--owl-本体)
  - [状态与元数据](#状态与元数据)
  - [类与属性查询](#类与属性查询)
  - [SPARQL 执行](#sparql-执行)
  - [实体目录](#实体目录)
- [三、向量库](#三向量库)
- [四、数据库](#四数据库)
- [五、本体模型管理（ontology 模块）](#五本体模型管理ontology-模块)
- [六、统一分发入口](#六统一分发入口)

---

## 一、语义工具

### 1. `POST /api/tool/semantic-expand` — 语义扩展

向量搜索命中实体 → 从 Jena 拉取每个实体的完整本体结构（类定义、父子类、属性、枚举成员）。未命中时自动降级到 Jena 关键词检索。

**请求 Body**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| question | string | ✓ | 自然语言问题 |
| nsCode | string | | 命名空间过滤，如 `w_wtr_stat` |
| topK | int | | 向量搜索条数，0 使用配置默认值 |
| threshold | double | | 相似度阈值，0 使用配置默认值 |

**响应 data**

```json
{
  "query": "水库供水类型",
  "vectorAvailable": true,
  "matched": true,
  "entities": [
    {
      "entityType": "class",
      "entityId": "class-xxxxxxxx",
      "similarity": 0.87,
      "sourceText": "水库 供水 蓄水 ...",
      "nsCode": "w_wtr",
      "apiName": "Reservoir",
      "label": "水库",
      "ontology": {
        "success": true,
        "uri": "http://...",
        "label": "水库",
        "superClasses": ["WaterProject"],
        "subClasses": [],
        "properties": [{ "localName": "hasType", "label": "类型", "type": "ObjectProperty" }]
      }
    }
  ],
  "fallback": null,
  "success": true
}
```

> `matched=false` 时 `fallback.source` 为 `jena_keyword`（Jena 降级结果）或 `none`（无结果）。  
> `enum_item` 类型实体 `ontology` 字段为 `null`，`ontologyNote` 说明原因。

---

### 2. `POST /api/tool/subgraph` — 最小关系子图

向量搜索命中实体 → 从 Jena 提取去重后的类/属性/关系边，供外部调用方构建 SPARQL 生成上下文。**枚举类型会附加完整的枚举项列表**。

**请求 Body**：与语义扩展相同（question / nsCode / topK / threshold）。

**构建逻辑**

1. 向量搜索 → 命中实体集
2. 按类型分流：`class/enum` → `classDetail`；`class_prop/property` → `propertyDetail`
3. **枚举类型特殊处理**：检测到 `enum` 类型时，从 Jena 本体查询该枚举的所有 Individual（枚举项），带缓存机制
4. `class_prop` 命中时，补充其 `domains` 中的父类（`classDetail`）
5. 扩展命中类挂载的所有属性（1-hop）
6. 检测命中类集合内的 `ObjectProperty` 边 → `edges`
7. 附加 `vocab()` 命名空间前缀

**响应 data**

```json
{
  "success": true,
  "prefixes": {
    "": "http://bontolink.beiktech.com/ontology#",
    "owl": "http://www.w3.org/2002/07/owl#"
  },
  "classes": [
    {
      "localName": "Enum_AdministrativeRegion",
      "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion",
      "label": "行政区划",
      "comment": "中国行政区划枚举",
      "superClasses": [],
      "subClasses": [],
      "enumItems": [
        {
          "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion_110000",
          "localName": "Enum_AdministrativeRegion_110000",
          "label": "北京市",
          "code": "110000",
          "apiName": "beijing"
        },
        {
          "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion_120000",
          "localName": "Enum_AdministrativeRegion_120000",
          "label": "天津市",
          "code": "120000",
          "apiName": "tianjin"
        }
      ]
    },
    {
      "localName": "t1901_s02_1",
      "uri": "http://watf.com/ont/v1/water/statistics#t1901_s02_1",
      "label": "单位财务情况",
      "comment": null,
      "superClasses": [],
      "subClasses": []
    }
  ],
  "properties": [
    {
      "localName": "t1901_s02_1.q12",
      "uri": "http://...",
      "label": "本年收入合计：事业收入",
      "type": "DatatypeProperty",
      "domains": ["t1901_s02_1"],
      "ranges": [],
      "inverseOf": null
    }
  ],
  "edges": [
    { "from": "ClassA", "property": "hasProp", "to": "ClassB", "label": "ClassA --[属性]--> ClassB" }
  ],
  "matchedEntities": [
    { "entityType": "enum", "entityId": "enum-xxx", "apiName": "AdministrativeRegion", "similarity": 0.89, "nsCode": "w_wtr" },
    { "entityType": "class_prop", "entityId": "...", "apiName": "t1901_s02_1.q12", "similarity": 413.3, "nsCode": "w_wtr_stat" }
  ],
  "vectorAvailable": true
}
```

**枚举项说明**

- **枚举类识别**：`classes` 数组中，`localName` 以 `Enum_` 开头的是枚举类
- **enumItems 字段**：仅枚举类包含此字段，普通类没有
- **枚举项结构**：
  - `uri`: 完整 URI（格式：`{namespace}#Enum_{apiName}_{code}`）
  - `localName`: 本地名（格式：`Enum_{apiName}_{code}`）
  - `label`: 中文标签
  - `code`: 枚举编码（例如：`110000`）
  - `apiName`: API 名称（例如：`beijing`）
- **数据来源**：枚举项从 Jena 本体查询，是枚举类的 Individual 实例
- **缓存机制**：首次查询从本体加载，后续直接返回缓存，提升性能

> `prefixes` 直接用于 SPARQL `PREFIX` 块；`edges` 仅包含命中类集合内存在的 ObjectProperty 连接。

---

## 二、Jena / OWL 本体

### 状态与元数据

#### `GET /api/tool/status` — 三大数据源状态

返回 Jena / 向量库 / 数据库的可用性状态聚合。无参数。

#### `GET /api/tool/jena/meta/vocab` — 命名空间前缀

返回本体命名空间映射、本体声明、imports、规模统计。无参数。

#### `GET /api/tool/jena/meta/kinds` — 实体类型统计

返回各类型（class / objectProperty / datatypeProperty / individual 等）的数量。无参数。

---

### 类与属性查询

#### `GET /api/tool/jena/classes` — 全量 OWL 类列表

无参数。返回本体中所有非匿名 OWL 类。

**响应 data**：`{ "success": true, "classes": [{ "uri", "localName", "label", "superClasses" }] }`

#### `GET /api/tool/jena/class/{localName}` — OWL 类详情

| 参数 | 类型 | 说明 |
|---|---|---|
| localName | path | 类的本地名，如 `Reservoir` |

**响应 data**

```json
{
  "success": true,
  "uri": "http://...",
  "localName": "Reservoir",
  "label": "水库",
  "superClasses": ["WaterProject"],
  "subClasses": [],
  "properties": [{ "localName": "hasType", "label": "类型", "type": "ObjectProperty" }]
}
```

#### `GET /api/tool/jena/properties` — 全量 OWL 属性列表

无参数。返回所有属性的 uri / localName / label / type / domains / ranges。

#### `GET /api/tool/jena/property/{localName}` — OWL 属性详情

| 参数 | 类型 | 说明 |
|---|---|---|
| localName | path | 属性本地名 |

**响应 data**

```json
{
  "success": true,
  "uri": "...",
  "localName": "hasType",
  "label": "类型",
  "type": "ObjectProperty",
  "domains": ["Reservoir"],
  "ranges": ["DamType"],
  "superProperties": [],
  "subProperties": [],
  "inverseOf": null,
  "characteristics": { "functional": false, "symmetric": false, "transitive": false }
}
```

#### `GET /api/tool/jena/enum-individuals/{enumApiName}` — 查询枚举类的所有枚举项

查询指定枚举类的所有 Individual（枚举项），用于前端下拉选择、数据验证等场景。

| 参数 | 类型 | 说明 |
|---|---|---|
| enumApiName | path | 枚举类的 api_name（不带 `Enum_` 前缀），如 `AdministrativeRegion` |

**响应 data**

```json
{
  "success": true,
  "enumClass": "AdministrativeRegion",
  "count": 34,
  "items": [
    {
      "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion_110000",
      "localName": "Enum_AdministrativeRegion_110000",
      "label": "北京市",
      "code": "110000",
      "apiName": "beijing"
    },
    {
      "uri": "http://bontolink.beiktech.com/ontology#Enum_AdministrativeRegion_120000",
      "localName": "Enum_AdministrativeRegion_120000",
      "label": "天津市",
      "code": "120000",
      "apiName": "tianjin"
    }
  ]
}
```

**说明**

- 枚举类在本体中以 `Enum_{api_name}` 形式存在
- 枚举项是枚举类的 Individual 实例
- 返回的 `items` 包含每个枚举项的完整信息（uri, localName, label, code, apiName）
- 该接口主要供内部使用，前端建议使用 `/api/tool/subgraph` 接口

#### `GET /api/tool/jena/hierarchy` — 类层级树

| 参数 | 类型 | 默认 | 说明 |
|---|---|---|---|
| root | query | null | 根节点 localName，空=全部顶层类 |
| depth | query | 0 | 展开深度，0=全部（上限 8） |

#### `GET /api/tool/jena/members` — 类成员/枚举值浏览

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| class | query | ✓ | | 类 localName（如 `Enum_DWLX`） |
| keyword | query | | null | 关键字过滤 |
| page | query | | 1 | |
| size | query | | 50 | |

#### `GET /api/tool/jena/resource` — 资源三元组浏览（仿 Fuseki）

| 参数 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| uri | query | ✓ | | 资源 IRI（URL 编码） |
| page | query | | 1 | 入站三元组分页 |
| size | query | | 50 | |

返回该 IRI 的出站三元组（按谓词分组）+ 入站三元组（分页）。

---

### SPARQL 执行

#### `POST /api/tool/jena/sparql` — 执行 SPARQL

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sparql | string | ✓ | SPARQL 查询语句（SELECT / ASK / CONSTRUCT） |
| limit | int | | 附加 LIMIT，0 不附加 |
| format | string | | 结果格式（默认 JSON） |

#### `GET /api/tool/jena/query/templates` — 预设 SPARQL 模板

返回可直接执行的常用查询模板列表。无参数。

---

### 实体目录

#### `GET /api/tool/jena/entities` — 通用实体检索

| 参数 | 类型 | 说明 |
|---|---|---|
| kind | query | `class` / `objectProperty` / `datatypeProperty` / `annotationProperty` / `individual` / `all` |
| ns | query | 命名空间 URI 前缀过滤 |
| keyword | query | 关键字（localName / label 匹配） |
| page | query | 默认 1 |
| size | query | 默认 50 |

---

## 三、向量库

#### `POST /api/tool/vector/search` — 向量相似度检索

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| text | string | ✓ | 检索文本 |
| topK | int | | 返回条数，0 使用配置默认 |
| threshold | double | | 相似度阈值，0 使用配置默认 |
| nsCode | string | | 命名空间过滤 |

**响应 data**

```json
{
  "success": true,
  "matches": [
    { "entity_id": "class-...", "entity_type": "class", "similarity": 0.87, "source_text": "...", "ns_code": "w_wtr" }
  ]
}
```

#### `GET /api/tool/vector/list` — 向量库分页浏览

| 参数 | 类型 | 说明 |
|---|---|---|
| nsCode | query | 命名空间过滤 |
| page | query | 默认 1 |
| size | query | 默认 50 |

#### `GET /api/tool/vector/detail/{classId}` — 单条向量详情

`classId` 为 path 参数（如 `class-xxxxxxxx-xxxx-...`）。

---

## 四、数据库

#### `POST /api/tool/db/query` — 执行 SQL（只读）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| sql | string | ✓ | 只支持 SELECT 语句 |

#### `GET /api/tool/db/tables` — 列出所有表

无参数。返回关系库中所有表名。

#### `GET /api/tool/db/schema/{table}` — 表结构

`table` 为 path 参数。返回列名、类型等 DDL 信息。

---

## 五、本体模型管理（ontology 模块）

> 响应格式：`{ "code": int, "msg": string, "data": any }`（此模块与 tool 模块格式略有不同）

#### `GET /api/ontology/health` — OntModel 健康状态

```json
{ "status": "running", "modelVersion": 3, "dbVersion": 3, "rebuildCount": 1, "modelReady": true }
```

#### `GET /api/ontology/stats` — OntModel 详细统计

无参数。返回语句数、类数、属性数等全量统计。

#### `POST /api/ontology/rebuild` — 强制重建 OntModel

无 body。从数据库重新构建 OntModel（同步操作，数据量大时耗时数秒）。

```json
{ "success": true, "statementCount": 12450, "version": 4 }
```

#### `POST /api/ontology/match` — 本体实体匹配

| 字段 | 类型 | 默认 | 说明 |
|---|---|---|---|
| query | string | | 查询文本，必填 |
| topK | int | 10 | 返回条数 |
| threshold | double | 0.6 | 相似度阈值 |

```json
{
  "query": "水库供水量",
  "expandedQuery": "水库 供水量 蓄水量 ...",
  "matches": [{ /* EntityMatch */ }],
  "totalMatches": 5
}
```

#### `POST /api/ontology/preview-expansion` — 预览同义词扩充效果

| 字段 | 类型 | 说明 |
|---|---|---|
| query | string | 查询文本，必填 |

```json
{
  "original": "水费",
  "expanded": "水费 水价 供水费用 水资源费",
  "synonyms": { "水费": ["水价", "供水费用"] },
  "stats": { "termCount": 1, "expandedCount": 3 }
}
```

#### `GET /api/ontology/classes` — OWL 类列表（ontology 模块）

返回 `[{ uri, localName, label, comment, superClasses }]`，label 优先中文。

#### `GET /api/ontology/class/{localName}` — OWL 类详情（ontology 模块）

同 `/api/tool/jena/class/{localName}`，额外包含 `properties`（domain 为此类的属性列表）。

#### `GET /api/ontology/properties` — OWL 属性列表（ontology 模块）

返回 `[{ uri, localName, label, type, domains, ranges }]`。

#### `GET /api/ontology/enums` — 枚举类型列表

返回所有 `Enum_` 前缀的类，`localName` 字段已去掉前缀。  
字段：`uri / localName / label / comment / enumType?`

#### `GET /api/ontology/enum/{enumName}/items` — 枚举项列表

`enumName` 不含 `Enum_` 前缀（如 `DWLX`）。

```json
{
  "enumName": "DWLX",
  "items": [{ "uri": "...", "label": "水库", "code": "201", "apiName": "Reservoir" }],
  "itemCount": 15
}
```

#### `GET /api/ontology/value-types` — 值类型列表

返回所有 `ValueType_` 前缀资源，字段：`uri / localName / label / baseType / constraintType / enumUri? / enumName?`

---

## 六、统一分发入口

#### `POST /api/tool/query` — 统一查询分发

通过 `source` 字段路由到三大数据源。

**示例**

```json
// Jena SPARQL
{ "source": "jena", "sparql": "SELECT ?c WHERE { ?c a owl:Class } LIMIT 10" }

// 向量搜索
{ "source": "vector", "text": "水库", "topK": 10, "threshold": 0.6, "nsCode": "w_wtr" }

// 数据库查询
{ "source": "db", "sql": "SELECT * FROM ont_class LIMIT 10" }

// 数据库表结构
{ "source": "db", "table": "ont_class" }

// 数据库表列表
{ "source": "db" }
```

---

## 附录：实体类型说明

| entityType | 对应表 | 说明 |
|---|---|---|
| `class` | `ont_class` | OWL 对象类型 |
| `class_prop` | `ont_class_property` | 类属性，apiName 格式为 `类名.属性名` |
| `shared_property` | `ont_shared_properties` | 共享属性 |
| `enum` | `ont_enum_types` | 枚举类型，在本体中以 `Enum_{api_name}` OntClass 存在 |
| `enum_item` | `ont_enum_items` | 枚举值（不做 Jena 类查询，取父枚举 apiName），在本体中以 Individual 存在 |
| `link_type` | `ont_link_types` | 链接类型 |
| `biz_category` | `ont_biz_category` | 业务分类 |

---

## 更新日志

### 2026-07-31

**枚举项查询增强**

1. **新增接口**：`GET /api/tool/jena/enum-individuals/{enumApiName}` — 查询枚举类的所有 Individual
   - 从 Jena 本体查询枚举项，而非直接查数据库
   - 返回包含 uri, localName, label, code, apiName 的完整信息

2. **`/api/tool/subgraph` 增强**
   - 碰到枚举类型时，自动从本体查询并返回所有枚举项
   - 在 `classes` 数组中，枚举类会额外包含 `enumItems` 字段，格式：`[{ code, label }, ...]`（精简版，仅保留编码和标签）
   - 实现缓存机制，提升重复查询性能
   - `SemanticExpandService` 新增缓存清理方法：`clearEnumItemsCache()` 和 `clearEnumItemsCache(enumApiName)`

3. **对象属性数据类型**
   - `properties` 数组中新增 `dataType` 字段，返回 XSD 数据类型：`xsd:string`、`xsd:integer`、`xsd:decimal`、`xsd:dateTime` 等
   - 数据类型在本体构建时从 `ont_class_property.data_type` 字段读取并存储到本体中

4. **性能优化**
   - 本体版本号缓存：`OntologyModelManager.getDbVersion()` 实现 5 秒 TTL 缓存，减少重复数据库查询
   - Milvus 重试增强：针对首次请求的 gRPC 连接错误（INTERNAL/end-of-stream）自动重试，含 500ms 延迟

5. **本体结构说明**
   - 枚举类在本体中以 `Enum_{api_name}` 形式的 OntClass 存在
   - 枚举项是枚举类的 Individual 实例，命名格式：`Enum_{api_name}_{code}`
   - 枚举项包含属性：`ont:code`（编码）、`rdfs:label`（中文标签）


