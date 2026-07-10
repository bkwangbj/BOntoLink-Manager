# 共享属性 SharedProperties

> 跨对象类型复用的属性定义，一次修改全局生效。包含普通共享属性 + 结构属性（Struct Types，多个共享属性组合的复合结构）。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/shared-props` |
| 路由 name | `sharedProps` |
| 路由 meta.title | `共享属性` |
| 侧边栏 icon/label | `share` / `共享属性` |
| 分组 | 资源管理 |

## 页面

**主文件**: `frontend/src/views/resources/SharedProperties.vue` (CSS 前缀 `sp-`)
**子组件目录**: `frontend/src/views/resources/sharedproperty/`

### 页面结构
- **Header**: 标题 + 列表/分组视图切换 + 搜索
- **左侧**: `CategoryTreeFilter`
- **主面板**: 双 Tab（普通属性 / 结构属性）
- **右侧详情抽屉** (`SharedPropertyDetailDrawer`, z-index 1000):
  - 5 个页签: 基础信息 / OWL 特性 / 约束 / 引用 / 注释
  - 只读/编辑模式切换
  - 宽度 420-960px 可拖拽 + localStorage 持久化

### 子组件
- `SharedPropertyDetailDrawer.vue` — 详情抽屉
- `NewSharedPropertyWizard.vue` — 新建向导弹框（z-index 1100，3 步骤：元数据→配置→保存位置）
- `StructTypesView.vue` — 结构属性子视图（左右双栏：左结构列表 + 右条目表，拖拽重排）
- `MiniSwitch.vue` — 启/禁用开关

### 使用的 Picker Modal
- `ValueTypePickerModal` — 选择值类型
- `SharedPropertyPickerModal` — 选择共享属性添加到结构
- `PropertyFormatModal` — 属性格式化规则配置（`property-scope='shared'`）

## 后端 API

### SharedPropertyController — `/api/shared-properties`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/shared-properties` | 列表（含 ref_count, 值类型名, 格式化状态）|
| GET | `/api/shared-properties/{id}` | 详情 |
| POST | `/api/shared-properties` | 新建（校验 prop_code 唯一，可选分组绑定）|
| PUT | `/api/shared-properties/{id}` | 更新（prop_code 不可变）|
| DELETE | `/api/shared-properties/{id}` | 删除（ref_count>0 拒绝）|
| POST | `/api/shared-properties/batch-delete` | 批量删除（引用>0 跳过）|
| GET | `/api/shared-properties/{id}/references` | 引用列表（反查 ont_class_property）|

### StructTypeController — `/api/struct-types`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/struct-types` | 列表（含 item_count）|
| GET | `/api/struct-types/{id}` | 详情 + 内嵌 items |
| GET | `/api/struct-types/{id}/items` | 仅 items 列表 |
| POST | `/api/struct-types` | 新建（可同时带 items）|
| PUT | `/api/struct-types/{id}` | 更新（items 整体覆盖）|
| DELETE | `/api/struct-types/{id}` | 删除（级联 items）|
| POST | `/api/struct-types/batch-delete` | 批量删除 |

### Mapper

**SharedPropertyMapper**: `listAll`(带 ref_count/value_type/format), `findById`, `existsByCode`, `insert`(26+列), `update`, `delete`, `listReferences`, `deleteGroupRefs/insertGroupRef`

**StructTypeMapper**: `listAll`(item_count), `findById`, `existsByCode`, `listItems`(JOIN ont_shared_properties), `insert/update/delete` items

## API 客户端

```js
export const sharedPropertyApi = {
  list, get, create, update, remove, batchRemove, references
}
export const structTypeApi = {
  list, get, items, create, update, remove, batchRemove
}
```

## 数据库

### ont_shared_properties（共享属性主表）
| 列 | 说明 |
|---|---|
| `id` | `shared-properties-{UUID}` PK |
| `prop_code` | 唯一编码 |
| `prop_type` | data/object/annotation/struct |
| `data_type` | XSD 类型 |
| `value_type` | 值类型引用 |
| `is_key / is_required / is_multi_valued_prop / is_range_constraint_prop` | 布尔约束 |
| `owl_*` (7个) | OWL 特性字段 |
| `xsd_*` (6个) | XSD 约束字段 |
| `rdfs_*` (4个) | RDFS 语义字段 |
| `metadata` | JSON |
| 索引: `idx_sp_category`, `idx_sp_code`, `idx_sp_status` |

### ont_struct_types（结构属性元数据）
| 列 | 说明 |
|---|---|
| `id` | `struct-types-{UUID}` PK |
| `struct_code` | 唯一编码 |
| `rdfs_label / rdfs_comment / rdfs_see_also / rdfs_defined_by` | 语义字段 |

### ont_struct_items（结构条目 — M:N 关联）
| 列 | 说明 |
|---|---|
| `id` | `struct-items-{UUID}` PK |
| `struct_id` | FK→ont_struct_types |
| `sort_no` | 排序 |
| `prop_id` | FK→ont_shared_properties |
| 索引: `idx_struct_items_struct`, `idx_struct_items_prop` |

### 分组关联
`ont_biz_group_class` (group_type='shared_props') — 共享属性分组绑定

## 共享组件
- `CategoryTreeFilter` / `PageHeader` / `FieldRow` / `CodeEditor`
- `ValueTypePickerModal` / `SharedPropertyPickerModal` / `PropertyFormatModal`
