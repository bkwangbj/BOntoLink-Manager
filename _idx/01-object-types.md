# 对象类型 ObjectTypes

> 本体核心实体（OWL Class），管理类定义、属性、层次关系、等价/不相交约束、物理表绑定、接口实现、动作等。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/object-types` |
| 路由 name | `objectTypes` |
| 路由 meta.title | `对象类型` |
| 侧边栏 icon/label | `cube` / `对象类型` |
| 分组 | 资源管理 |

## 页面

**主文件**: `frontend/src/views/resources/ObjectTypes.vue`
**子组件目录**: `frontend/src/views/resources/objecttype/`
**CSS 前缀**: `ot-`

### 页面结构
- **Header** (48px): 标题、分组/列表切换、行业/领域/状态筛选、搜索、新建按钮
- **左侧**: `CategoryTreeFilter` — 行业分类树过滤
- **主面板**: 3 种视图（`list` 表格 / `card` 卡片 / `graph` 占位）
- **右侧抽屉** (z-index 1000, 可拖拽宽度, localStorage 持久化):
  - 左侧 Tab 导航（4 组）:
    - 基础信息(蓝): 概览 `TabOverview` / 属性 `TabProps`
    - 关系(橙): 链接关系 `TabLinkGraph` / 对象图谱 `TabObjectGraph`
    - 规则约束(紫): 等价类/不相交类/互斥并集/等价属性/不相交属性/接口
    - 业务应用(绿): 动作/函数/被引用/数据源/使用情况/时序图表/事件时间轴
  - 支持 2 级嵌套抽屉 (z-index 1010, 最多 8 tab 栈)

### 子组件
- `TabOverview.vue` — 基本信息编辑（基础/显示/类表达式/其他）
- `TabProps.vue` — 属性行内编辑（OWL特性+XSD约束+物理映射）
- `TabClassGroup.vue` — 等价/不相交类关系管理
- `TabLinkGraph.vue` — Canvas 链接关系图
- `TabObjectGraph.vue` — 7 维对象图谱
- `TabDisjointUnion.vue` — 互斥并集管理
- `TabPropertyRelation.vue` — 等价/不相交属性管理
- `NewObjectTypeWizard.vue` — 分步创建向导
- `TimeseriesChart.vue` / `EventTimeline.vue` — 时序/事件视图

## 后端 API

### ClassMetaController — `/api/class-meta`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/class-meta/classes` | 所有类列表（供 picker 用） |
| POST | `/api/class-meta/classes` | 创建类（走 ObjectTypeCreateService 向导） |
| PUT | `/api/class-meta/classes/{id}` | 更新类基本信息 |
| PUT | `/api/class-meta/classes/{id}/status` | 切换启用/禁用 |
| DELETE | `/api/class-meta/classes/{id}` | 删除类（级联+引用检查） |
| GET | `/api/class-meta/class-group` | 等价/不相交组列表 |
| POST/PUT/DELETE | `/api/class-meta/class-group` | 分组 CRUD |
| GET/POST/PUT/DELETE | `/api/class-meta/disjoint-union` | 互斥并集 CRUD |
| GET/POST/PUT/DELETE | `/api/class-meta/property-equivalent` | 属性等价关系 CRUD |
| GET/POST/PUT/DELETE | `/api/class-meta/property-disjoint` | 属性不相交关系 CRUD |
| GET | `/api/class-meta/classes/{classId}/properties` | 类的属性列表 |
| POST | `/api/class-meta/classes/{classId}/properties` | 添加属性 |
| PUT | `/api/class-meta/properties/{propId}` | 更新属性 |
| DELETE | `/api/class-meta/properties/{propId}` | 删除属性 |
| POST | `/api/class-meta/classes/{classId}/properties/reorder` | 拖拽排序 |
| PUT/DELETE | `/api/class-meta/datasources/{id}` | 更新/删除数据源绑定 |

### ResourceController — `/api/resource`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/resource/classes` | 类列表（可选 aggregate=true 加统计） |
| GET | `/api/resource/classes/{id}` | 类详情（嵌套属性/链接/动作/接口/数据源） |
| GET | `/api/resource/classes/{id}/properties` | 类的属性列表 |
| GET | `/api/resource/discover/stats` | 全局简单统计 |
| GET | `/api/resource/discover/overview` | 15 项概览统计（行业/领域过滤） |

### Mapper: ClassMetaMapper
- `ont_class` CRUD + 状态切换 + 编码唯一校验 + 引用检查
- `ont_class_property` CRUD + 拖拽排序
- `ont_class_group` / `ont_class_disjoint_union` CRUD + 去重检查
- `ont_property_equivalent` / `ont_property_disjoint` CRUD
- `ont_class_ds` CRUD + 级联清空字段映射

### Service: ObjectTypeCreateService
- `create(body)` — `@Transactional` 向导创建（类 + 数据源 + 属性字段映射）

## API 客户端

- `resourceApi`: `classes, classDetail, properties, discoverStats, discoverOverview`
  - → GET `/api/resource/classes`, `/api/resource/classes/{id}`, ...
- `classMetaApi`: `candidates, createClass, updateClass, setClassStatus, removeClass`
  - → POST/PUT/DELETE `/api/class-meta/classes`
- 子资源 CRUD: `listGroup / addGroup / ...`, `listProps / addProp / ...`
- 数据源: `updateClassDs / removeClassDs`

## 数据库

### ont_class（主表）
| 列 | 说明 |
|---|---|
| `id` | `class-{UUID}` PK |
| `api_name` | 唯一标识（UNIQUE） |
| `ns_code` / `category_code` | 命名空间 / 行业分类 |
| `parent_class_id` | 父类 |
| `class_expr_type/content` | 类表达式 |
| `icon` / `color` / `status` | 显示 + 状态 |
| `is_thing` / `is_nothing` / `is_common` | 特殊类型标记 |

### ont_class_property（属性）
关键列: `class_id`, `api_name`, `prop_type`, `data_type`(XSD), `value_type`, `class_ds_id`, `physical_table/column`, `is_primary/required/key/derived`, `range_class_id`, `sub_property_of`, `sort`, `status`
+ 7 个 `owl_*` 特性 + 6 个 `xsd_*` 约束

### 关联表
- `ont_class_group` — 等价/不相交类关系
- `ont_class_disjoint_union` — 互斥并集
- `ont_property_equivalent` / `ont_property_disjoint` — 属性等价/不相交
- `ont_class_ds` — 类-物理表绑定（rel_type: 1=主表/2=附表）
- `ont_class_link` — 简单类链接

## 共享组件
- `CategoryTreeFilter` — 行业分类树
- `StatusTag` — 状态标签
- `FieldRow` — 表单行布局
- `PropertyFormatModal` — 属性格式化弹框
- `LinkTypeEditor` — 链接属性编辑器（showTabs=false 模式）
