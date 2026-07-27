# BOntoLink REST API 参考

> 本文档按业务模块组织，每个模块列出关键端点（2-3 个）和响应结构示例。
> 完整端点清单见 [`_idx/`](_idx/README.md) 各模块索引。

---

## A. 核心资源管理

### 1. 对象类型

**前端路由**: [`/resources/object-types`](/#/resources/object-types)  
**后端 Controller**: `ClassMetaController` `ResourceController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resource/classes` | 对象类型列表（支持 categoryCode 过滤） |
| GET | `/api/resource/classes/{id}` | 对象类型详情（含属性/关系/动作/接口/数据源） |
| POST | `/api/class-meta/classes` | 向导一站式创建对象类 |
| PUT | `/api/class-meta/classes/{id}/status` | 切换启用/禁用 |
| GET | `/api/class-meta/classes` | 简要列表（选择器用） |
| POST | `/api/class-meta/classes/{classId}/properties` | 新增属性 |
| PUT | `/api/class-meta/properties/{propId}` | 更新属性 |
| POST | `/api/class-meta/classes/{classId}/properties/reorder` | 拖拽排序 |
| GET | `/api/class-meta/class-group` | 等价类/不相交类关系 |
| GET | `/api/class-meta/property-equivalent` | 等价属性关系 |
| GET | `/api/class-meta/property-disjoint` | 互斥属性关系 |
| GET | `/api/class-meta/disjoint-union` | 互斥并集成员 |
| DELETE | `/api/class-meta/classes/{id}` | 删除（被引用时返回阻断原因） |

**响应示例 (列表)**:
```json
{"code":0,"data":[{"id":"class-...","api_name":"HydrologyStation","display_name":"水文测站","rdfs_comment":"面向水文行业的测站台账","status":1,"category_code":"dom_water_hydrology","ns_code":"w_wtr_hyd"}]}
```

**响应示例 (详情)**:
```json
{"code":0,"data":{"id":"class-...","api_name":"HydrologyStation","display_name":"水文测站","properties":[...],"links":[...],"actions":[...],"interfaces":[...],"datasources":[...]}}
```

---

### 2. 链接类型

**前端路由**: [`/resources/link-types`](/#/resources/link-types)  
**后端 Controller**: `LinkTypeController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/link-types` | 链接类型列表 |
| GET | `/api/link-types/{id}` | 详情（含 mappings + type_classes） |
| POST | `/api/link-types` | 创建（可同时带 mappings） |
| POST | `/api/link-types/{id}/status` | 切换启用/实验/废弃状态 |
| POST | `/api/link-types/batch-delete` | 批量删除 |

**响应示例**:
```json
{"code":0,"data":[{"id":"link-...","api_name":"HasMonitor","display_name":"监测", "source_class_id":"class-...","target_class_id":"class-...","status":"active"}]}
```

---

### 3. 值类型

**前端路由**: [`/resources/value-types`](/#/resources/value-types)  
**后端 Controller**: `ValueTypeController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/value-types` | 值类型列表 |
| POST | `/api/value-types` | 新建 |
| DELETE | `/api/value-types/{id}` | 删除 |
| GET | `/api/value-types/usage-configs` | 默认使用配置列表 |
| POST | `/api/value-types/sync-from-enums` | 枚举同步为值类型 |

**响应示例**:
```json
{"code":0,"data":[{"id":"vt-...","display_name":"字符串","data_type":"string","group_id":"group-vt-basic"}]}
```

---

### 4. 枚举类型

**前端路由**: [`/resources/enum-types`](/#/resources/enum-types)  
**后端 Controller**: `EnumTypeController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/enum-types` | 枚举类型列表 |
| GET | `/api/enum-types/{id}` | 详情（含 items + levelRules） |
| POST | `/api/enum-types` | 新建 |
| POST | `/api/enum-types/{enumId}/items` | 添加枚举项 |
| POST | `/api/enum-types/{enumId}/level-rules` | 保存层次编码规则 |
| POST | `/api/enum-types/{enumId}/sync-run` | 执行外部数据库同步 |

**响应示例**:
```json
{"code":0,"data":[{"id":"enum-...","display_name":"测站类型","status":"active","items":[{"code":"A","label":"水文站"},{"code":"B","label":"水位站"}]}]}
```

---

### 5. 共享属性 & 结构属性

**前端路由**: [`/resources/shared-props`](/#/resources/shared-props)  
**后端 Controller**: `SharedPropertyController` `StructTypeController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/shared-properties` | 共享属性列表（含 ref_count） |
| POST | `/api/shared-properties` | 创建 |
| POST | `/api/shared-properties/batch-delete` | 批量删除（回显被引用阻断项） |
| GET | `/api/shared-properties/{id}/references` | 引用列表 |
| GET | `/api/struct-types` | 结构类型列表 |
| POST | `/api/struct-types` | 创建（可同时带 items） |

**响应示例**:
```json
{"code":0,"data":[{"id":"sp-...","display_name":"创建时间","prop_type":"DATE","ref_count":15}]}
```

---

### 6. 接口

**前端路由**: [`/resources/interfaces`](/#/resources/interfaces)  
**后端 Controller**: `InterfaceController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/interface` | 接口列表 |
| GET | `/api/interface/{id}` | 详情（含 properties） |
| POST | `/api/interface` | 新建 |
| POST | `/api/interface/{id}/properties` | 添加属性 |
| POST | `/api/interface/{id}/implementers` | 绑定实现类 |
| POST | `/api/interface/{id}/toggle` | 切换启用/禁用 |

**响应示例**:
```json
{"code":0,"data":[{"id":"if-...","api_name":"Measurable","display_name":"可监测","status":1,"properties":[...]}]}
```

---

### 7. 动作类型

**前端路由**: [`/resources/action-types`](/#/resources/action-types)  
**后端 Controller**: `ResourceController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resource/actions` | 动作类型列表（资源总览返回） |

---

## B. 数据源与物理表

### 8. 数据源

**前端路由**: [`/resources/datasources`](/#/resources/datasources)  
**后端 Controller**: `DataSourceController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/datasource` | 数据源列表 |
| GET | `/api/datasource/{id}` | 详情 |
| POST | `/api/datasource` | 新建 |
| POST | `/api/datasource/{id}/test` | 测试连接 |
| GET | `/api/datasource/{id}/pool` | 连接池运行时指标（MXBean） |
| POST | `/api/datasource/{id}/pool/refresh` | 热刷新连接池 |
| POST | `/api/datasource/{id}/pool/resize` | 动态调整最大连接数 |

**响应示例**:
```json
{"code":0,"data":[{"id":"ds-...","name":"水利主库","db_type":"mysql","host":"127.0.0.1","status":"ACTIVE"}]}
```

### 9. 物理表

**后端 Controller**: `PhysicalTableController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/physical-tables` | 物理表列表（可选 dsId 过滤） |
| POST | `/api/physical-tables/sync` | 同步指定数据源的表结构 |
| PUT | `/api/physical-tables/{id}/name` | 修改中文名 |

---

## C. 分类与命名空间

### 10. 行业分类

**前端路由**: [`/config/category`](/#/config/category)  
**后端 Controller**: `CategoryController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/category/tree` | 完整行业分类树 |
| GET | `/api/category/{id}` | 节点详情 |
| GET | `/api/category/{id}/stats` | 节点下的资源统计 |
| GET | `/api/category/{id}/graph` | 关联图谱 |
| POST | `/api/category` | 新建节点 |
| POST | `/api/category/{id}/members` | 向分组添加对象类 |

**响应示例**:
```json
{"code":0,"data":[{"id":"category-10000000-0000-0000-0000-000000000001","category_code":"ind_water_resource","rdfs_label":"水利行业","children":[...]}]}
```

### 11. 命名空间

**后端 Controller**: `NamespaceController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/namespace` | 命名空间列表 |
| POST | `/api/namespace` | 新建 |
| GET | `/api/namespace/{code}/versions` | 版本列表 |
| POST | `/api/namespace/versions` | 创建版本快照 |

### 12. 分组

**后端 Controller**: `GroupController` `GroupRefController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/group` | 按父节点查子分组 |
| GET | `/api/group/all` | 全部分组（扁平列表） |
| GET | `/api/group/by-domain` | 按领域返回分组 |
| POST | `/api/group` | 新建分组 |
| GET | `/api/group/{id}/classes` | 分组下绑定的对象类型 |
| POST | `/api/group-refs` | 新建资源-分组绑定 |

---

## D. 类型类系统

### 13. 类型类

**前端路由**: [`/config/type-classes`](/#/config/type-classes)  
**后端 Controller**: `TypeClassController` `TypeClassCategoryController` `TypeClassBindController` `TcRenderController` `TcEnumController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/type-classes` | 类型类列表（支持 categoryCode/applicableType/deprecated/q 过滤） |
| GET | `/api/type-classes/{id}` | 单个详情 |
| POST | `/api/type-classes` | 新建 |
| POST | `/api/type-classes/{id}/deprecate` | 切换弃用 |
| GET | `/api/type-classes/category-stats` | 按大类聚合统计 |
| GET | `/api/tc-category` | 类型类大类(Kind)字典列表 |
| GET | `/api/tc-bind/by-carrier` | 按载体反查已绑定类型类 |
| GET | `/api/tc-render/resolve` | 解析载体的类型类绑定（渲染友好结构） |
| GET | `/api/tc-enum` | 枚举可选项 |

**响应示例**:
```json
{"code":0,"data":[{"id":"tc-...","display_name":"百分率","category_code":"measure","applicable_type":"PROPERTY","param_schema":{"type":"object","properties":{"min":{"type":"number"}}}}]}
```

---

## E. 实例探索

### 14. 实例探索

**前端路由**: [`/workspace/instances`](/#/workspace/instances)  
**后端 Controller**: `InstanceController` `ExploreDesignController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/instance/object-types` | 对象类型统计（含模拟实例数/属性数/链接数） |
| GET | `/api/instance/list` | 实例列表（分页+筛选+排序） |
| GET | `/api/instance/detail` | 单条实例详情（含 columns + links） |
| GET | `/api/instance/columns` | 列定义（系统列+属性列） |
| GET | `/api/instance/chart-data` | 图表数据（长格式，多指标/分组/排序） |
| GET | `/api/instance/aggregate` | 按 groupBy 分组聚合 |
| GET | `/api/instance/matrix` | 二维交叉聚合（热力图用） |
| GET | `/api/instance/stat` | 整体聚合统计（指标卡片） |
| GET | `/api/instance/links` | 相邻对象类型列表 |
| GET | `/api/instance/geo-points` | 逐点经纬度（散点地图用） |
| GET | `/api/instance/search` | 简版全局搜索 |
| GET | `/api/instance/search-results` | 完整搜索结果页 |
| GET | `/api/explore-design` | 对象类型下的命名设计列表 |
| PUT | `/api/explore-design/default` | Upsert 默认看板 |

**响应示例 (列表)**:
```json
{"code":0,"data":{"total":156,"page":1,"size":20,"records":[{"id":"inst_001","companyName":"XX市供水公司","supplyVolume":1250000.5,"createTime":"2026-01-15 10:30:00"}]}}
```

---

## F. 全局搜索与智能查询

### 15. 全局搜索

**前端**: `GlobalSearchModal.vue`（Cmd+K 弹框）  
**后端 Controller**: `SearchController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/search/global?q=xxx&type=all&smart=false` | 全局搜索 |

**参数说明**:
- `q`: 搜索关键词
- `type`: `all`(综合) / `object` / `link` / `prop` / `ds` / `other`
- `smart`: `true` 启用智能模式（分词+同义词扩展+多关键词评分）

**普通模式响应**:
```json
{"code":0,"data":{"objects":[...],"links":[...],"__total":23,"__query":"供水"}}
```

**智能模式响应**:
```json
{"code":0,"data":{"originalQuery":"水利单位用水量","extractedKeywords":["水利","单位","用水量"],"expandedKeywords":["水利","水务","水资源","单位","用水量","供水量"],"ontologies":[{"display_name":"水利服务业企业","matchScore":3,"matchedKeywords":["水利","单位","用水量"]}],"totalMatches":5,"returnedCount":2}}
```

---

## G. 配置与工具

### 16. 图标库

**后端 Controller**: `IconLibController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/icon-lib` | 全量图标（groups + icons） |
| POST | `/api/icon-lib/groups` | 新建图标分组 |
| POST | `/api/icon-lib/groups/{groupId}/icons` | 上传 SVG 图标 |

### 17. 字典管理

**前端路由**: [`/config/dict-manager`](/#/config/dict-manager)  
**后端 Controller**: `DictController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dict` | 字典定义列表 |
| POST | `/api/dict` | 新建字典 |
| GET | `/api/dict/{dictId}/items` | 字典条目列表 |
| GET | `/api/dict/{dictId}/items/tree` | 条目树形结构 |
| GET | `/api/dict/code/{code}` | 按 code 公开查询（长效缓存） |

**响应示例**:
```json
{"code":0,"data":[{"id":"dict-...","dict_code":"sys_ds_type","dict_name":"数据源类型","items":[{"item_code":"mysql","item_label":"MySQL"}]}]}
```

### 18. 属性格式化

**后端 Controller**: `PropertyFormatController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/property-format/property/{propertyId}` | 查询单条格式化配置 |
| PUT | `/api/property-format/property/{propertyId}` | Upsert 配置 |
| GET | `/api/property-format/by-properties` | 批量查询 |
| PUT | `/api/property-format/batch` | 批量 upsert |

### 19. 图谱

**前端路由**: [`/workspace/graph`](/#/workspace/graph)  
**后端 Controller**: `GraphController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/graph/industry-tree` | 行业层级图谱（industry/domain/subdomain/group） |
| GET | `/api/graph/ontology` | 对象本体图谱（sub/eq/dis/union/link 五类关系） |

### 20. 资源总览

**前端路由**: [`/workspace/discover`](/#/workspace/discover)  
**后端 Controller**: `ResourceController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resource/discover/overview` | 综合统计（按行业/领域，15 项资源计数） |
| GET | `/api/resource/graph` | 行业/领域/分组/对象/接口完整图谱（节点+边） |

### 21. 调试 SQL

**后端 Controller**: `DebugSqlController`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/debug/query` | 执行 SELECT SQL（仅开发环境） |
| GET | `/api/debug/tables` | 列出所有表 |
| GET | `/api/debug/schema/{tableName}` | 查看表结构 |

### 22. 系统健康

**后端 Controller**: `HealthController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 服务存活探活 |

---

## H. 语义扩充

### 23. 同义词词典

**后端 Controller**: `SynonymDictController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/synonym-dict` | 同义词列表（可选 domain 过滤） |
| POST | `/api/synonym-dict` | 创建 |
| POST | `/api/synonym-dict/batch-import` | 批量导入 |

### 24. 领域术语

**后端 Controller**: `DomainTermController`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/domain-term` | 领域术语列表 |
| POST | `/api/domain-term` | 创建 |

---

## 附录

### Controller 清单

| # | Controller | 模块 | 端点约数 |
|---|-----------|------|:-------:|
| 1 | `InstanceController` | 实例探索 | 12 |
| 2 | `ClassMetaController` | 类元数据 | 26 |
| 3 | `EnumTypeController` | 枚举类型 | 22 |
| 4 | `TypeClassController` | 类型类 | 7 |
| 5 | `InterfaceController` | 接口 | 13 |
| 6 | `CategoryController` | 行业分类 | 13 |
| 7 | `DictController` | 字典管理 | 13 |
| 8 | `DataSourceController` | 数据源 | 13 |
| 9 | `ResourceController` | 资源总览/对象类型 | 9 |
| 10 | `IconLibController` | 图标库 | 9 |
| 11 | `ValueTypeController` | 值类型 | 9 |
| 12 | `LinkTypeController` | 链接类型 | 8 |
| 13 | `NamespaceController` | 命名空间 | 8 |
| 14 | `SharedPropertyController` | 共享属性 | 7 |
| 15 | `StructTypeController` | 结构属性 | 7 |
| 16 | `SynonymDictController` | 同义词词典 | 7 |
| 17 | `TypeClassBindController` | 类型类绑定 | 6 |
| 18 | `TcEnumController` | 类型类枚举 | 6 |
| 19 | `ExploreDesignController` | 探索看板设计 | 6 |
| 20 | `GroupRefController` | 分组关联 | 6 |
| 21 | `GroupController` | 分组 | 7 |
| 22 | `TcCategoryController` | 类型类大类 | 5 |
| 23 | `PropertyFormatController` | 属性格式化 | 5 |
| 24 | `DomainTermController` | 领域术语 | 5 |
| 25 | `PhysicalTableController` | 物理表 | 4 |
| 26 | `SearchController` | 全局搜索 | 1 |
| 27 | `GraphController` | 图谱 | 2 |
| 28 | `TcRenderController` | 类型类渲染 | 2 |
| 29 | `DebugSqlController` | 调试 SQL | 3 |
| 30 | `OntologyMatchController` | 本体匹配(8089) | 3 |
| 31 | `HealthController` | 健康检查 | 1 |
| 32 | `SemanticQueryController` | 语义搜索 | 7 |

**总端点数**: ~205+（bontolink-admin 8088）+ 3（bontolink-ontology 8089）

---

## 页面路由汇总

| 路由 | 页面 | 核心后端模块 |
|------|------|-------------|
| `/` | → discover | ResourceController |
| `/workspace/discover` | 总览 | ResourceController |
| `/workspace/graph` | 图谱 | GraphController |
| `/workspace/instances` | 实例探索 | InstanceController + ExploreDesignController |
| `/workspace/maker` | 可视化制作 | 纯前端 |
| `/resources/object-types` | 对象类型 | ClassMetaController + ResourceController |
| `/resources/link-types` | 链接类型 | LinkTypeController |
| `/resources/action-types` | 动作类型 | ResourceController |
| `/resources/value-types` | 值类型 | ValueTypeController |
| `/resources/enum-types` | 枚举类型 | EnumTypeController |
| `/resources/shared-props` | 共享属性 | SharedPropertyController + StructTypeController |
| `/resources/functions` | 函数 | 占位页 |
| `/resources/interfaces` | 接口 | InterfaceController |
| `/resources/datasources` | 数据源 | DataSourceController + PhysicalTableController |
| `/tools/ai` | AI 助手 | 纯前端 |
| `/tools/import-export` | 导入导出 | 纯前端 |
| `/config/category` | 行业分类 | CategoryController |
| `/config/type-classes` | 类型类 | TypeClassController 系列(5个) |
| `/config/security` | 权限安全 | 纯前端 |
| `/config/dict-manager` | 字典管理 | DictController |
| `semantic-test.html` | 测试页 | SearchController + SynonymDictController |

---

**更新**: 2026-07-24 — 重写为模块摘要格式，覆盖全部 32 个 Controller
