# 链接类型 LinkTypes

> 对象类型之间的关联/关系建模。支持双向关系、基数约束（1:1/1:N/N:1/N:N）、复合主键映射、数据源中间表、类型类绑定。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/link-types` |
| 路由 name | `linkTypes` |
| 路由 meta.title | `链接` |
| 侧边栏 icon/label | `link` / `链接` |
| 分组 | 资源管理 |

## 页面

**主文件**: `frontend/src/views/resources/LinkTypes.vue` (CSS 前缀 `lk-`)
**编辑器**: `frontend/src/views/resources/linktype/LinkTypeEditor.vue` (CSS 前缀 `lke-`)

### 页面结构
- **Header**: 统计（总量/正式/实验/废弃）+ 状态筛选 + 基数筛选 + 搜索
- **左侧**: `CategoryTreeFilter` (store-key: "link-types")
- **主列表**: 表格列 — checkbox, 图标, 源实体, 链接名, 基数标签, 目标实体, 数据源开关, 双向开关, 状态标签, 映射数
- **右侧抽屉** (`LinkTypeEditor`, z-index 1000, 可拖拽宽度, localStorage 持久化):
  - 页签: 基础信息 / 链接关系图 / 类型类
  - 编辑/只读模式切换
  - **对称双栏布局**: 源端(上) + 目标端(下):
    - 对象类型选择器
    - 基数分段控制（one/many）
    - 启用开关
    - 关键映射表（复合主键支持，增删行）
  - 数据源行: `is_data_source_rel` 开关 + 物理表选择器
  - 显示配置: display_name, plural_name, visibility, api_name
  - 类型类绑定
- **批量操作**: 批量删除

### 跨模块复用
- ObjectTypes.vue 的 TabLinkGraph / TabObjectGraph 复用了 LinkTypeEditor（`showTabs=false`）

### 子组件
- `BoundTypeClassList` — 类型类绑定列表 (`@/components/typeclass/BoundTypeClassList.vue`)
- `MiniSwitch` — 内联布尔开关

## 后端 API

### LinkTypeController — `/api/link-types`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/link-types` | 列表（JOIN ont_class 取源/目标端显示名） |
| GET | `/api/link-types/{id}` | 详情（含 mappings + type_classes） |
| GET | `/api/link-types/{id}/mappings` | 字段映射列表 |
| POST | `/api/link-types` | 新建（自动生成 id/rid/基数/状态默认值） |
| PUT | `/api/link-types/{id}` | 更新（mappings 整体覆盖） |
| DELETE | `/api/link-types/{id}` | 删除（级联 mappings + type_class_bind） |
| POST | `/api/link-types/batch-delete` | 批量删除 |
| POST | `/api/link-types/{id}/status` | 切换状态（experimental/active/deprecated） |

### Mapper: LinkTypeMapper
- `listAll()` — JOIN ont_class (l/r) 取源/目标显示名 + icon + color + 子查询统计
- `findById()` / `existsByCode()` / `insert()` / `update()` / `delete()`
- `listMappings(linkId)` / `deleteMappingsByLink()` / `insertMapping()`
- `listTypeClasses(linkId)` / `deleteTypeClassesByLink()` (仅 `applicable_type='relation'`)

## API 客户端

```js
export const linkTypeApi = {
  list, get, mappings, create, update, remove, batchRemove, setStatus
}
```

## 数据库

### ont_link_types（主表）
| 列 | 说明 |
|---|---|
| `id` | `link-types-{UUID}` PK |
| `link_type_id` | 业务编码（UNIQUE，如 `aircraft-flight-operate`）|
| `l_object_type_id` / `r_object_type_id` | 源/目标对象类型 FK→ont_class |
| `l_cardinality` / `r_cardinality` | one/many |
| `l_display_name` / `r_display_name` | 显示名 |
| `l_plural_name` / `r_plural_name` | 复数名（many 时启用）|
| `l_api_name` / `r_api_name` | camelCase API 名 |
| `l_visibility` / `r_visibility` | normal/prominent/hidden |
| `l_enabled` / `r_enabled` | 0/1 |
| `is_data_source_rel` | 0=字段映射 / 1=中间表 |
| `rel_data_table` | 中间表物理表名 |
| `category_code` | 所属行业 |
| `status` | experimental/active/deprecated |

### ont_link_mappings（字段映射）
| 列 | 说明 |
|---|---|
| `mapping_id` | `link-mappings-{UUID}` PK |
| `link_id` | FK→ont_link_types |
| `side` | left/right |
| `seq` | 复合键序号（1-based） |
| `object_field` | 对象属性名 |
| `join_table_column` | 中间表列名（数据源关系时用） |

### 关联表
- `ont_type_class_bind`（where `applicable_type='relation'`）— 类型类绑定

## 共享组件
- `CategoryTreeFilter` — 分类树
- `PageHeader` — 页面头部
- `Pager` — 分页
- `BoundTypeClassList` — 类型类绑定列表
