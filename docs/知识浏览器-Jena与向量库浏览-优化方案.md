# 知识浏览器 · 通用本体与向量库浏览方案（v2 · 仿 Fuseki）

> 版本：v2.0 草案 · 2026-07-29（由 v1 定制版升级为通用版）
> 前端入口：`frontend/src/views/tools/KnowledgeBrowser.vue`
> 后端模块：`bontolink-admin` 的 `com.beiktech.bontolink.tool`（`/api/tool/*`）

---

## 0. 版本说明与设计目标

**v2 演进**：从 v1"针对 `Enum_` 前缀的定制枚举浏览器"，升级为"**仿 Fuseki 的通用本体浏览器**"。核心转变——

- **以 URI 为中心**：类、属性、个体、枚举值、匿名节点都通过统一的"资源页"浏览，点任意引用可无限下钻。
- **元数据驱动**：类/属性/个体/命名空间全部从模型**自动发现**，**不硬编码 `Enum_` 前缀、不硬编码命名空间**。换一个本体照样能用。

**设计目标**

| 目标 | 含义 |
|---|---|
| **通用** | 元数据驱动，任何 OWL/RDF 本体（任意命名空间/结构/规模）皆可直接浏览 |
| **可扩展** | 实体类型（kind）、前端渲染模板、SPARQL 查询模板均可插拔注册，新增类型零侵入 |
| **只读** | 仅浏览，全部 `GET` + SPARQL（SELECT/CONSTRUCT/ASK/DESCRIBE），无任何写操作 |
| **仿 Fuseki** | 借鉴资源浏览器 / 数据集元数据 / YASGUI 查询工作台三大范式，并做增强 |

---

## 1. 设计哲学

### 1.1 仿 Fuseki 的四大范式

1. **资源中心浏览（Resource-centric）**：任意资源以"**出站三元组**（作为主语：谓词→宾语列表）+ **入站三元组**（作为宾语：主语→谓词列表）"呈现，点击任意宾语/主语即跳转到该资源页。这是 Fuseki resource explorer / Pubby 的通用范式，**天然覆盖本体全部内容**（不限于类/属性/枚举）。
2. **数据集元数据**：命名空间、前缀、imports、OWL 版本、三元组统计一览。
3. **目录浏览**：按实体类型（类 / 对象属性 / 数据属性 / 注释属性 / 个体）分页检索。
4. **通用 SPARQL 工作台**：编辑器 + 查询模板 + 多格式结果（表格 / JSON / CSV / Turtle）。

### 1.2 通用性三原则

- **元数据驱动**：类/属性/个体/枚举/命名空间均由后端从 `OntModel` 自动枚举，前端按 `rdf:type` 动态选择渲染，不写死任何项目特定规则。
- **渲染模板可插拔**：前端维护"类型 → 视图组件"注册表，未知类型自动回退到"通用三元组视图"。
- **类型（kind）可扩展**：后端实体类型为可配置集合，新增 `skos:Concept`、`sh:Shape` 等仅需注册一个 kind。

---

## 2. 现状与本体数据全景（实测 2026-07-29）

已实现：类列表+详情、属性列表+详情、SPARQL 查询、向量相似度检索、状态卡片。

| 维度 | 数量 | 说明 |
|---|---|---|
| 类（owl:Class） | 73 | 含 20 个枚举类（`Enum_*`） |
| DatatypeProperty | 272 | 含 70 个 FunctionalProperty |
| ObjectProperty | 20 | |
| 显式 owl:NamedIndividual | 0 | 实例以 `rdf:type=枚举类` 表达，未走 OWL 推理 |
| **枚举个体**（rdf:type=枚举类） | **3000+** | `Enum_slfwy-addvcd` 3468、`Enum_DWLX` 33、`Enum_DJZC` 26… |
| 命名空间 | 1 个主空间 `…/ontology#` | 主本体资源无 imports/版本声明 |

**枚举个体结构（规整）**：`ontology#code`(编码) + `rdfs:label`(中文名@zh) + `rdf:type`(所属枚举类)。IRI 如 `…#Enum_DWLX_505`。

> v2 通用方案中，"枚举"不再是特例：枚举类 = 拥有众多个体的类，枚举值 = 个体，被"成员浏览 / 资源页"自然覆盖。

---

## 3. 总体架构（元数据驱动三层）

```
┌─────────────────────────────────────────────────────────┐
│ 前端 KnowledgeBrowser.vue（路由式资源页 ?uri=）            │
│  左：数据集导航（命名空间/目录/类树）  右：资源页 + SPARQL 工作台 │
└──────────────┬──────────────────────────────────────────┘
               │ /api/tool/jena/*（只读）
┌──────────────┴──────────────────────────────────────────┐
│ 目录层  GET entities / hierarchy / members   → 实体目录、类树、个体 │
│ 资源层  GET resource?uri=                    → 出站+入站三元组(核心) │
│ 查询层  POST sparql / GET query-templates    → 通用查询+模板        │
│ 元数据  GET meta/vocab / meta/kinds          → 命名空间/统计        │
└─────────────────────────────────────────────────────────┘
```

关键：**资源层是核心**。目录层只负责"找到入口"，点进去后统一走资源页下钻浏览。

---

## 4. 后端接口设计（通用，挂 `/api/tool`，全部只读）

实现位置：`JenaToolService` / `VectorToolService` 新增方法，`ToolQueryController` 新增端点。

### 4.1 元数据
| 端点 | 说明 | service 方法 |
|---|---|---|
| `GET /jena/meta/vocab` | 命名空间/前缀/imports/OWL 版本/三元组统计 | `vocab()` |
| `GET /jena/meta/kinds` | 实体类型清单及数量（class/objectProperty/datatypeProperty/annotationProperty/individual…） | `kinds()` |

```json
// GET /jena/meta/vocab
{ "success": true, "prefixes": [ {"prefix":"bl","ns":"http://bontolink.beiktech.com/ontology#"} ],
  "imports": [], "owlVersion": null, "statements": 12429, "classes": 73, "properties": 292 }
```

### 4.2 目录层
| 端点 | 说明 | service 方法 |
|---|---|---|
| `GET /jena/entities?kind=&ns=&keyword=&page=&size=` | 通用实体分页检索（kind 可扩展） | `entities(kind, ns, keyword, page, size)` |
| `GET /jena/hierarchy?root=&depth=` | 类层级树（subClassOf，可指定根/深度） | `hierarchy(root, depth)` |
| `GET /jena/members?class=&keyword=&page=&size=` | 某类的个体（实例/枚举值通用） | `members(classUri, keyword, page, size)` |

```json
// GET /jena/entities?kind=individual&ns=&keyword=水利&page=1&size=50
{ "success": true, "kind": "individual",
  "items": [ {"uri":"...#Enum_DWLX_201","localName":"Enum_DWLX_201","label":"流域管理机构","type":"...#Enum_DWLX"} ],
  "total": 3000, "page": 1, "size": 50 }
```

### 4.3 资源层（核心，仿 Fuseki）
| 端点 | 说明 | service 方法 |
|---|---|---|
| `GET /jena/resource?uri=&page=&size=` | 通用资源页：出站/入站三元组 + 类型 + 标签 | `resource(uri, page, size)` |

```json
// GET /jena/resource?uri=...%23Enum_DWLX_201
{ "success": true, "uri": "...#Enum_DWLX_201", "localName": "Enum_DWLX_201",
  "label": "流域管理机构", "types": ["...#Enum_DWLX"],
  "outgoing": [ {"predicate":"...#code","predicateLabel":"编码","objects":[{"value":"201","isLiteral":true}]},
                {"predicate":"rdfs:label","predicateLabel":"label","objects":[{"value":"流域管理机构","isLiteral":true,"lang":"zh"}]} ],
  "incoming": [ {"subject":"...#SomeClass","predicate":"...#hasUnitType"} ],
  "outgoingTotal": 3, "incomingTotal": 1, "page": 1, "size": 100 }
```
- 对象标记 `isLiteral` / `lang` / `datatype` / `isResource`，前端据此渲染为"字面量文本"或"可点击资源链接"。
- 入站/出站均分页，避免大型资源（如被 3468 次引用的枚举类）一次拉爆。

### 4.4 查询层（仿 YASGUI）
| 端点 | 说明 | service 方法 |
|---|---|---|
| `POST /jena/sparql` | 已有，**增强** `format=json|csv|turtle|xml` | `sparql(...)` 扩展 |
| `GET /jena/query/templates` | 预设查询模板（通用 + 由元数据自动生成） | `queryTemplates()` |

### 4.5 向量库
| 端点 | 说明 | service 方法 |
|---|---|---|
| `GET /vector/list?nsCode=&page=&size=` | 分页浏览全部向量 | `list(nsCode, page, size)` |
| `GET /vector/{id}` | 单条向量详情 | `detail(id)` |

> 字段以 `class_embeddings` 实际表结构为准；方言自适应与优雅降级沿用现有做法。

---

## 5. 前端设计（`KnowledgeBrowser.vue`）

### 5.1 整体结构
```
KnowledgeBrowser.vue
├─ 顶部：状态卡片（Jena / 向量库 / 数据库）
├─ 两大页签：「本体浏览器」「向量库」
└─ 本体浏览器
   ├─ 左：数据集导航
   │   ├─ 命名空间 + 元数据（meta/vocab）
   │   ├─ 实体目录（按 kind 切换：类/对象属性/数据属性/注释属性/个体）
   │   └─ 类层级树（subClassOf）
   └─ 右：内容区（页签）
       ├─ 资源页（默认，按 type 自适应渲染，可下钻）
       └─ SPARQL 工作台（编辑器 + 模板 + 多格式结果）
```
- **资源页路由**：内部用 `?uri=` 状态（或路由 query）记录当前资源，支持前进/后退与深链接。
- 任何"目录项 / 树节点 / 三元组宾语"点击都导航到对应资源页，**无限下钻**。

### 5.2 渲染模板注册表（可扩展核心）
前端按资源的 `rdf:type` 选择渲染模板，未注册类型回退通用视图：
```js
const renderers = {
  'owl#Class':            ClassView,        // 层级 + 关联属性 + 成员入口
  'owl#ObjectProperty':   PropertyView,     // domain/range/特征/逆/等价
  'owl#DatatypeProperty': PropertyView,
  'owl#NamedIndividual':  IndividualView,   // 属性-值对
  'enum':                 EnumView,         // 编码/中文名（个体的高密度表格皮肤）
  'default':              GenericResourceView // 纯出站/入站三元组表（兜底）
}
```
- **新增类型 = 注册一个模板**，无需改主框架；未知本体自动落到 `GenericResourceView`。
- "枚举"只是个体的一种**皮肤**（高密度编码表），逻辑与个体浏览共用。

### 5.3 SPARQL 工作台（仿 YASGUI，增强）
- 模板下拉（通用模板 + 后端按元数据生成的"查某类个体""查某属性用例"）。
- 结果格式切换：**表格 / JSON / CSV / 三元组（Turtle）**，CSV 一键导出。
- 查询历史（localStorage）、常用前缀自动注入。

---

## 6. 通用性与可扩展性设计（核心）

| 维度 | 机制 | 效果 |
|---|---|---|
| **换本体即用** | 类/属性/个体/命名空间全部元数据发现，无硬编码 | 任意 OWL/RDF 本体接入即浏览 |
| **新增实体类型** | 后端 kind 集合可配置；`entities?kind=` 动态分发 | 加 `skos:Concept` 等零侵入 |
| **新增渲染** | 前端 `renderers` 注册表，未知类型回退通用视图 | 类型皮肤可插拔 |
| **新增查询** | 模板从元数据自动生成 + 手工预设 | 模板可扩展 |
| **大模型适配** | 目录/资源/成员全分页，结果上限 | 大规模本体不卡顿 |
| **多命名空间** | `meta/vocab` + `entities?ns=` 过滤 | 支持导入多词表本体 |

---

## 7. 仿 Fuseki 能力对照

| Fuseki 能力 | 本方案对应 | 增强点 |
|---|---|---|
| Resource explorer（出站/入站三元组） | `GET /jena/resource?uri=` 资源页 | 按 type 自适应渲染 + 枚举高密度皮肤 |
| 数据集元数据/统计 | `GET /jena/meta/vocab` | 融合进左侧导航 |
| YASGUI 查询编辑器 | SPARQL 工作台 | 模板自动生成 + CSV 导出 + 历史 |
| 命名空间前缀管理 | `meta/vocab` 前缀清单 | 编辑器自动注入 |
| graph 列表 | （单模型，可扩展 `?graph=` 参数） | 预留多图扩展位 |
| — | 向量库浏览 + 向量↔本体联动 | Fuseki 没有的增强 |

---

## 8. 性能与安全

- **全分页**：目录、资源出入站、成员、向量一律服务端分页（默认 `size=50`）。
- **轻量缓存**：类树（73）、命名空间、kind 统计可一次性返回并前端缓存。
- **结果上限**：SPARQL / 检索沿用 `limit`。
- **只读**：全部 `GET` + 受限 SPARQL；资源页只读，不提供编辑。
- **鉴权**：诊断工具，生产环境加鉴权（控制器注释已提示）。

---

## 9. 分阶段实施计划

| 里程碑 | 内容 | 优先级 | 价值 |
|---|---|---|---|
| **M1** | **通用资源页**（`resource?uri=` 出站/入站/下钻）+ **成员浏览**（含枚举 3000+） | P0 | 打通"所有内容"浏览 |
| **M2** | 元数据（vocab/kinds）+ 目录（entities 分页检索）+ 类树 | P1 | 找到入口、导航 |
| **M3** | SPARQL 工作台增强（模板/多格式/导出/历史） | P1 | 仿 Fuseki 查询体验 |
| **M4** | 渲染模板细化 + 向量库浏览 + 向量↔本体联动 | P2 | 完整与闭环 |

> 建议先做 **M1**：通用资源页 + 成员浏览，一举覆盖"类/属性/个体/枚举/匿名资源"的全部只读浏览，3000+ 枚举值自然纳入。

---

## 10. 待确认问题

1. **资源页下钻深度**：是否限制入站三元组数量（大型资源如被 3468 次引用的枚举类）？建议分页即可，不设硬上限。
2. **枚举皮肤识别**：`enum` 渲染模板用"类拥有众多个体（阈值如 >10）"自动判定，还是仍按 `Enum_` 前缀提示？（建议：自动判定，更通用）
3. **SPARQL 结果格式**：除 表格/JSON/CSV/Turtle 外是否需要 RDF/XML？
4. **多图支持**：当前单 OntModel，是否预留 `?graph=` 扩展位（建议：预留但不实现）？
5. **是否按 M1 先落地**（通用资源页 + 成员浏览）？
