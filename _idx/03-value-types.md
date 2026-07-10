# 值类型 ValueTypes

> 属性的语义包装器，封装元数据约束（长度/正则/RID/UUID/枚举），提供安全的属性值表达。配置层模块。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/value-types` |
| 路由 name | `valueTypes` |
| 路由 meta.title | `值类型` |
| 侧边栏 icon/label | `list` / `值类型` |
| 分组 | 配置管理 |

## 页面

**主文件**: `frontend/src/views/config/ValueTypes.vue`
**CSS 前缀**: `vt-`

### 页面结构
- **顶部**: 状态分段按钮（全部/启用/禁用）、业务领域筛选、搜索、新建按钮
- **左侧**: `CategoryTreeFilter`
- **主列表**: 表格 — 名称/API/领域/数据类型/约束/枚举/说明/RID/状态/操作
- **右侧编辑抽屉** (z-index 1000, 可拖拽宽度):
  - Tab1 **元数据**: 名称/描述/API(创建后不可改)/基础类型(保存后不可改)/领域/分组/状态
  - Tab2 **约束配置**: 5 种约束卡片（RID/UUID/Length/Regex/Enum），其中 Enum 时包含枚举选择面板 + 使用配置
  - Tab3 **预览验证**: 测试值输入 + 实时校验 + 配置概要

### 子组件
- `TestValueTreeNode` — 递归树形选择（预览多级枚举）
- `EnumPickerModal` — 枚举选择面板弹框

## 后端 API

### ValueTypeController — `/api/value-types`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/value-types` | 所有值类型列表（LEFT JOIN ont_enum_types） |
| GET | `/api/value-types/{id}` | 详情 |
| POST | `/api/value-types` | 新建（ID 前缀 `value-types-`） |
| PUT | `/api/value-types/{id}` | 更新 |
| DELETE | `/api/value-types/{id}` | 删除（同时清理分组引用） |
| GET | `/api/value-types/usage-configs` | 所有使用配置 |
| POST | `/api/value-types/usage-configs` | 新建使用配置 |
| PUT | `/api/value-types/usage-configs/{id}` | 更新使用配置 |

### Mapper: ValueTypeMapper
- `ont_value_types`: `listAll`, `findById`, `insert`, `update`, `delete`
- `ont_valuetypes_usage_config`: `findUsageConfig`, `listUsageConfigs`, `insertUsageConfig`, `updateUsageConfig`

## API 客户端

```js
export const valueTypeApi = {
  list, get, create, update, remove,
  listUsageConfigs, createUsageConfig, updateUsageConfig
}
```

## 数据库

### ont_value_types
| 列 | 说明 |
|---|---|
| `id` | `value-types-{UUID}` PK |
| `base_type` | String/Integer/Decimal/Boolean/DateTime |
| `constraint_type` | RID/UUID/Length/Regex/Enum |
| `constraint_config` | JSON 约束参数 |
| `enum_id` | FK→ont_enum_types（constraint_type=Enum 时）|
| `default_usage_config_id` | FK→ont_valuetypes_usage_config |

### ont_valuetypes_usage_config
| 列 | 说明 |
|---|---|
| `max_select_level` | 最大选择层级 |
| `allow_non_leaf` | 是否允许非叶子节点 |
| `display_format` | label/code/code_label/full_label |
| `is_system_default` | 是否为系统默认 |

## 共享组件
- `CategoryTreeFilter` / `PageHeader` / `FieldRow` / `Pager`
- `EnumPickerModal` — 枚举选择面板

---

# 枚举类型 EnumTypes

> 统一枚举库管理，支持单/多级枚举、层次编码规则、数据库同步（从外部数据源拉取枚举项）。是值类型"Enum"约束的数据来源。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/enum-types` |
| 路由 name | `enumTypes` |
| 路由 meta.title | `枚举类型` |
| 侧边栏 icon/label | `layers` / `枚举类型` |
| 分组 | 配置管理 |

## 页面

**主文件**: `frontend/src/views/config/EnumTypes.vue`
**CSS 前缀**: `et-`

### 页面结构
- **顶部**: 状态筛选、搜索、新建按钮
- **左侧**: `CategoryTreeFilter`
- **主列表**: 表格 — 名称/API-NAME/枚举类型/最大层级/项数/说明/状态/操作
- **详情抽屉** (z-index 999, 可拖拽+最大化):
  - Tab1 **详情**: 基本信息 + 编码管理（层次配置/示例）
  - Tab2 **数据**: 树视图/表格视图双模式，枚举项行内编辑
  - Tab3 **同步规则**: 数据源→表→字段 三级联动，执行同步/查看日志
  - Tab4 **引用**: 被引用列表（对象属性/接口属性）
- **新建/编辑抽屉** (z-index 1000): 元数据表单

### 子组件
- `ItemTreeNode` — 数据 Tab 树视图递归组件
- `FilterableSelect` — 可筛选下拉框

## 后端 API

### EnumTypeController — `/api/enum-types`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/enum-types` | 所有枚举类型列表（含 item_count） |
| GET | `/api/enum-types/{id}` | 详情（含 items + levelRules） |
| POST | `/api/enum-types` | 新建（enum_type 默认 general_single） |
| PUT | `/api/enum-types/{id}` | 更新 |
| DELETE | `/api/enum-types/{id}` | 删除（级联） |
| GET/POST | `/{enumId}/items` | 枚举项列表/新建 |
| PUT/DELETE | `/items/{id}` | 枚举项更新/删除 |
| GET/POST | `/{enumId}/level-rules` | 层次编码规则列表/整体保存 |
| GET/POST | `/{enumId}/sync-config` | 同步配置 |
| GET | `/{enumId}/sync-logs` | 同步日志 |
| POST | `/{enumId}/sync-run` | 执行同步 |
| POST | `/{enumId}/sync-test` | 测试连接 |
| GET | `/{enumId}/references` | 被引用查询 |

### Mapper: EnumTypeMapper
覆盖 5 张表: `ont_enum_types`, `ont_enum_items`, `ont_enum_level_code_rule`, `ont_enum_sync_config`, `ont_enum_sync_log`

### Service: EnumSyncService
- `run(enumId, syncType, operUser)` — 3 种同步模式:
  - `full_overwrite` 全量覆盖
  - `level_diff` 逐级对比（以业务系统为准）
  - `incremental` 增量更新
- 依赖: `DataSourceService`, `DataSourceConnector`

## API 客户端

```js
export const enumTypeApi = {
  // 分组(stub): listGroups, createGroup, updateGroup, removeGroup
  list, get, create, update, remove,                    // 类型CRUD
  listItems, addItem, updateItem, removeItem,           // 枚举项
  listLevelRules, saveLevelRules,                       // 编码规则
  getSyncConfig, saveSyncConfig, listSyncLogs, runSync, testSync,  // 同步
  listReferences                                        // 引用
}
```

## 数据库

| 表名 | 关键列 | 用途 |
|---|---|---|
| `ont_enum_types` | `enum_type`(general_single/multi/biz_single/multi), `max_level` | 枚举类型主表 |
| `ont_enum_items` | `code`, `label`, `parent_code`, `level`, `sort_num`, `status` | 枚举项 |
| `ont_enum_level_code_rule` | `code_name`, `rule_level`, `code_len`, `total_len`, `code_separator`, `fill_char`, `fill_pos`(0=前补/1=后补) | 层次编码规则 |
| `ont_enum_sync_config` | `data_source_id`, `table_name`, `field_code/name/sort/status/parent`, `filter_sql`, `sync_mode`, `sync_strategy` | 同步配置 |
| `ont_enum_sync_log` | `add/update/del/fail_count`, `sync_status`, `error_msg`, `oper_user` | 同步日志 |

## 共享组件
- `CategoryTreeFilter` / `PageHeader` / `FieldRow` / `Pager`
- `FilterableSelect`

## 模块间关系
- 值类型(ValueTypes) 依赖枚举类型: `constraint_type='Enum'` → `enum_id` 引用 `ont_enum_types.id`
- 引用链: 枚举类型 ← 值类型 ← 对象类属性 / 接口属性
