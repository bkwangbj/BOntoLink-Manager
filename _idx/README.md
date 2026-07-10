# BOntoLink02 模块索引

> 预索引各模块结构，帮助 AI 快速定位"什么在哪里"，避免盲目探索。
>
> 每文件约 3K tokens，读完所有索引 ≈ 一次 API 调用的开销。

---

## 模块清单

| # | 模块 | 路由 | 索引文件 | 页面 | 数据表数 |
|---|---|---|---|---|---|
| 01 | [对象类型](01-object-types.md) | `/resources/object-types` | [→](01-object-types.md) | `ObjectTypes.vue` | 8 |
| 02 | [链接类型](02-link-types.md) | `/resources/link-types` | [→](02-link-types.md) | `LinkTypes.vue` + `LinkTypeEditor.vue` | 2+ |
| 03 | [值类型 + 枚举类型](03-value-types.md) | `/resources/value-types` `/resources/enum-types` | [→](03-value-types.md) | `ValueTypes.vue` `EnumTypes.vue` | 7 |
| 04 | [共享属性](04-shared-properties.md) | `/resources/shared-props` | [→](04-shared-properties.md) | `SharedProperties.vue` + `sharedproperty/*` | 3+ |
| 05 | [接口](05-interfaces.md) | `/resources/interfaces` | [→](05-interfaces.md) | `Interfaces.vue` | 3 |
| 06 | [数据源](06-datasources.md) | `/resources/datasources` | [→](06-datasources.md) | `Datasources.vue` | 1+ |
| 07 | [行业分类](07-category.md) | `/config/category` | [→](07-category.md) | `Category.vue` | 3 |
| 08 | [类型类](08-type-classes.md) | `/config/type-classes` | [→](08-type-classes.md) | `TypeClasses.vue` | 2+ |

---

## 技术栈速查

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 (`<script setup>`) + Vite 5 + 原生 CSS |
| 后端 | Java 21 + Spring Boot + MyBatis（注解式） |
| 数据库 | SQLite（默认）/ PostgreSQL 可选 |
| 迁移 | Flyway（`db/migration/{vendor}/Vn__*.sql` + `common/Vn__*.sql`）|
| HTTP | 前端 `http` 拦截器 → `/api/*` → 后端 `/bontolink/api/*` |
| 响应 | `R<T> = { code, msg, data }` |
| 状态管理 | 基本不用 Pinia，大多数状态在组件本地 |

## 关键约定

- **CSS 前缀**: 每个模块独立 namespace（`ot-`/`lk-`/`vt-`/`et-`/`sp-`/`if-`/`ds-`/`tc-`/`cat-`）
- **ID 格式**: `{prefix}-{UUID}`（如 `class-xxx`, `link-types-xxx`）
- **数据表**: `ont_xxx_yyy`（lowercase + underscore），主键 `id TEXT PRIMARY KEY`
- **时间戳**: `created_at` / `updated_at` TEXT NOT NULL DEFAULT `datetime('now','localtime')`
- **状态**: `status INTEGER`（1=启用/0=禁用）或 TEXT（`active/inactive/deprecated`）
- **RID**: `ri.ont.{module}.{code}`

## 共享组件（不要重写）

| 组件 | 位置 | 被哪些模块使用 |
|---|---|---|
| `CategoryTreeFilter` | `@/components/CategoryTreeFilter.vue` | ObjectTypes, LinkTypes, ValueTypes, EnumTypes, SharedProps, Interfaces, Datasources |
| `ValueTypePickerModal` | `@/components/ValueTypePickerModal.vue` | SharedProps, Interfaces |
| `EnumPickerModal` | `@/components/EnumPickerModal.vue` | ValueTypes |
| `SharedPropertyPickerModal` | `@/components/SharedPropertyPickerModal.vue` | SharedProps(Struct) |
| `PropertyFormatModal` | `@/components/PropertyFormatModal.vue` | SharedProps, Interfaces |
| `PageHeader` | `@/components/PageHeader.vue` | 所有模块 |
| `FieldRow` | `@/views/config/category/FieldRow.vue` | 详情抽屉表单 |
| `Pager` | `@/components/Pager.vue` | 需分页列表 |
| `CodeEditor` | `@/components/CodeEditor.vue` | Interfaces, SharedProps |
| `IconPickerField` / `ColorPickerField` | `@/components/` | Interfaces, ObjectTypes |
| `BoundTypeClassList` | `@/components/typeclass/BoundTypeClassList.vue` | LinkTypes, ObjectTypes |
| `DraggableHandles` / `useDraggableModal` | composable | 大弹框拖动/缩放 |
| `usePagination` | composable | 分页逻辑 |

## 常见操作模式

- **三栏资源页**: `PageHeader` + `CategoryTreeFilter`(左) + 列表(中) + 抽屉(右, z-index 1000)
- **详情抽屉**: 左侧 tab 栏 + 右侧内容区，可拖拽宽度（localStorage 持久化），无蒙层
- **批量操作栏**: [已选 N 项] [批量删除(红)] [批量启用(绿)] [批量禁用(灰)] [取消选择]
- **大模态弹框**: z-index ≥ 1200（必须高于抽屉 1010）
- **章节标题**: `<div class="sec">基础信息</div>` — 12px 灰 + 3px 蓝左边条

## 模块间依赖

```
枚举类型 ← 值类型 ← 共享属性/对象类型属性/接口属性
                    ← 对象类型 → 链接类型
                    ← 接口 → 对象类型(实现)
          行业分类 → 所有模块(按行业筛选)
          类型类 → 对象类型/链接类型(绑定)
          数据源 → 枚举类型(同步)/对象类型(物理表)
```
