# 接口 Interfaces

> 纯抽象契约模块。定义一组共享属性契约，供对象类型（ObjectTypes）去实现（implements）。类似 OOP 中的 interface。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/interfaces` |
| 路由 name | `interfaces` |
| 路由 meta.title | `接口` |
| 侧边栏 icon/label | `station` / `接口` |
| 分组 | 资源管理 |

## 页面

**主文件**: `frontend/src/views/resources/Interfaces.vue`
**CSS 前缀**: `if-`

### 页面结构
- **Header**: 状态 tab（全部/启用/实验）+ 列表/分组视图切换 + 行业/领域筛选 + 搜索 + 新建按钮
- **左侧**: `CategoryTreeFilter`
- **主列表**: 表格列 — checkbox, 接口名(图标+显示名+API名), 所属行业, 描述, 属性数, 实现数, 领域, 状态, 操作
  - 支持排序/分页/分组视图（按行业折叠）
  - 批量操作: 批量删除 + 批量启用 + 批量禁用
- **右侧详情抽屉** (z-index 1000, 可拖拽宽度, 可最大化):
  - Tab1 **概览**: 接口编码/API名(创建后不可改)/rdfs:label/显示名/状态 radio + 分类归属 + 语义定义 + 规则配置(CodeEditor)
  - Tab2 **显示**: 颜色选择(ColorPickerField) + 图标选择(IconPickerField) + RID
  - Tab3 **属性**: 行内编辑表格（编码/名称/XSD类型/值类型引用/配置状态/格式化/注释）, 支持增删改+批量格式化(PropertyFormatModal)
  - Tab4 **实现**: 已绑定对象类列表 + 解绑 + 模态弹框选择绑定

## 后端 API

### InterfaceController — `/api/interface`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/interface` | 所有接口列表 |
| GET | `/api/interface/{id}` | 接口详情 |
| POST | `/api/interface` | 新建（service 生成 id`interface-{UUID}`, rid`ri.ont.interface.{UUID}`）|
| PUT | `/api/interface/{id}` | 更新基础信息（api_name 不可改）|
| POST | `/api/interface/{id}/toggle` | 切换启用/禁用（active↔inactive）|
| DELETE | `/api/interface/{id}` | 删除（级联属性+实现类绑定）|
| GET | `/api/interface/{id}/properties` | 接口属性列表 |
| POST | `/api/interface/{id}/properties` | 添加属性 |
| PUT | `/api/interface/properties/{propId}` | 更新接口属性 |
| DELETE | `/api/interface/properties/{propId}` | 删除接口属性 |
| GET | `/api/interface/{id}/implementers` | 实现该接口的对象类列表 |
| POST | `/api/interface/{id}/implementers` | 绑定实现类 |
| DELETE | `/api/interface/{id}/implementers/{classId}` | 解绑实现类 |

### Service: InterfaceService
- `save()`, `delete()` — `@Transactional`
- `addImpl` — 幂等（已关联跳过）

### Mapper: InterfaceMapper
- **ont_interface**: `listAll`(含 prop_count/impl_count 子查询), `findById`, `insert`, `update`(禁止改 api_name), `updateStatus`, `delete`, 级联删除
- **ont_interface_property**: `listProperties`, `insertProperty`, `updateProperty`, `deleteProperty`
- **ont_interface_class**: `listImplementers`(JOIN ont_class), `existsImpl`, `addImpl`, `removeImpl`

## API 客户端

```js
export const interfaceApi = {
  list, get, create, update, toggle, remove,                    // CRUD+toggle
  properties, addProperty, updateProperty, removeProperty,       // 属性管理(4)
  implementers, addImpl, removeImpl                               // 实现类管理(3)
}
// 共 13 个方法
```

## 数据库

### ont_interface（接口主表）
| 列 | 说明 |
|---|---|
| `id` | `interface-{UUID}` PK |
| `api_name` | UNIQUE（创建后不可改）|
| `interface_code / ns_code / category_code` | 编码/命名空间/行业 |
| `display_name / rdfs_label / rdfs_comment / rdfs_see_also / rdfs_defined_by` | 语义字段 |
| `description` | 说明 |
| `icon / color` | 显示配置 |
| `status` | 1=启用/0=禁用 |
| `metadata` | JSON |

### ont_interface_property（接口属性）
| 列 | 说明 |
|---|---|
| `id` | `interface-pro-{UUID}` PK |
| `interface_id` | FK |
| `api_name / prop_code / data_type / value_type / category_code` | 属性定义 |
| `is_required` | 0=可选/1=必填/2=多值 |
| `metadata` | JSON |
| 索引: `idx_iface_prop_iface` |

### ont_interface_class（类实现关系）
| 列 | 说明 |
|---|---|
| `id` | `interface-class-{UUID}` PK |
| `interface_id` / `class_id` | FK |
| 索引: `idx_iface_cls_if`, `idx_iface_cls_c` |

## 共享组件
- `CategoryTreeFilter` / `PageHeader` / `FieldRow`
- `CodeEditor` — JSON 元数据编辑
- `IconPickerField` / `ColorPickerField`
- `PropertyFormatModal` — 属性格式化（单条/批量）
