# 类型类 TypeClasses

> 属性/关系/动作的元数据扩展定义。每个类型类有分类（Kind）、参数类型、前端组件映射、枚举值、JSON Schema 和绑定实例。内置 11 个分类。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/config/type-classes` |
| 路由 name | `typeClasses` |
| 路由 meta.title | `类型类` |
| 侧边栏 icon/label | `tag` / `类型类` |
| 分组 | 配置管理 |

## 页面

**主文件**: `frontend/src/views/config/TypeClasses.vue`
**CSS 前缀**: `tc-`
**子组件目录**: `frontend/src/components/typeclass/`

### 页面结构
- **PageHeader**: 统计（总量/活跃/废弃）+ 适用类型筛选 + 废弃筛选 + 搜索 + 新建
- **左导航**: 分类列表（彩色圆点+中英文名+数量徽标）+ "全部"选项 + "枚举管理"入口。分类 CRUD 通过右键菜单+弹框。
- **主列表**: 批量操作栏 + 可滚动表格（checkbox/适用类型标签/分类/名称/参数预览/状态徽标）
- **右侧抽屉**（双栏，可拖拽，可最大化）:
  - **左栏 3 tab**:
    - **详情**: 基本信息（适用类型 checkbox/分类/名称模板）+ 配置参数（param_type/frontend_component/options/schema JSON 编辑器）+ 高级（source_type/sort/protected/multi-bind/array）+ 状态
    - **显示**: 图标+颜色选择器
    - **引用**: 统计卡片 + 可筛选绑定列表
  - **右栏 — 参数预览**: JSON Schema 自动生成表单，实时预览 + 原始 JSON
- **分类弹框**: 编码/中文名/图标/颜色/适用类型/排序/描述
- **枚举字典弹框**: 左枚举名列表 + 右可编辑表格

### 跨模块使用
- `BoundTypeClassList.vue` 被 LinkTypes 和 ObjectTypes 引用

## 后端 API

### TypeClassController — `/api/type-classes`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/type-classes` | 过滤列表（categoryCode/applicableType/deprecated/q）|
| GET | `/api/type-classes/{id}` | 详情 |
| GET | `/api/type-classes/category-stats` | 按分类统计 |
| POST | `/api/type-classes` | 新建（ID `type-class-{UUID}`）|
| PUT | `/api/type-classes/{id}` | 更新（system_protected 字段受保护）|
| DELETE | `/api/type-classes/{id}` | 删除（system_protected 防护，级联绑定）|
| POST | `/api/type-classes/{id}/deprecate` | 切换废弃状态 |

### 关联控制器

| Controller | 路径 | 主要方法 |
|---|---|---|
| `TypeClassCategoryController` | `/api/tc-category` | list, get, create, update, delete |
| `TypeClassBindController` | `/api/tc-bind` | list, byCarrier, stats, create, updateValue, delete |
| `TcRenderController` | `/api/tc-render` | resolve, categoryMap |

### Mappers
- `TypeClassMapper` — CRUD + 同名校验 + 分类统计 + 废弃切换
- `TypeClassCategoryMapper` — 分类 CRUD + 类型类计数
- `TypeClassBindMapper` — 绑定实例 CRUD（按元数据/属性/链接/类查询）
- `TcEnumMapper` — 枚举字典 CRUD + 排序

## API 客户端

```js
export const typeClassApi = {
  list, get, stats, create, update, remove, deprecate
}
export const tcCategoryApi = { list, get, create, update, remove }
export const tcBindApi = { list, stats, byCarrier, create, update, remove }
export const tcRenderApi = { resolve, categoryMap }
export const tcEnumApi = { names, list, create, update, remove, reorder }
```

## 数据库

### ont_type_class（类型类主表，V8 重构）
| 列 | 说明 |
|---|---|
| `id` | `type-class-{UUID}` PK |
| `category_code` | FK→ont_type_class_category_dict |
| `name_prefix` | NOT NULL（固定前缀）|
| `name_template` | 模板表达式 |
| `name_cn_base` | NOT NULL（基础中文名）|
| `source_type` | 默认 `platform_built` |
| `allow_apply_types` | JSON 白名单（property/relation/action）|
| `allow_multi_bind` | 0/1 |
| `is_array_value` | 0/1 |
| `system_protected` | 0/1（受保护不可删）|
| `param_type` | boolean/rid/enum/text/numeric/json |
| `frontend_component` | switch/single_select/multi_select/text_input/number_input/json_editor/rid_selector |
| `param_options_json` | 结构化枚举配置 |
| `param_validator_json` | 校验规则 |
| `param_json` | 多字段参数 JSON Schema |
| `icon` / `color` | 显示 |
| `sort_weight` | 排序权重 |
| UNIQUE(category_code, name_prefix) |

### 关联表
- `ont_type_class_category_dict` — 分类字典（11 个种子分类）
- `ont_type_class_bind` — 绑定实例（type_class_meta_id, applicable_type, value, bind_deprecated...）
- `ont_dic_type_class` — 统一枚举字典（6 个枚举名: applicable_type, property_owner_type, source_type, param_type, frontend_component, env）

## 共享组件
- `PageHeader` / `IconPickerField` / `ColorPickerField` / `CodeEditor`
- `ParamSchemaForm` / `ParamPreview`（`@/components/typeclass/`）
- `BoundTypeClassList` — 被 LinkTypes/ObjectTypes 引用
