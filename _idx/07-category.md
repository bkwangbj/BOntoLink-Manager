# 行业分类 Category

> 行业/领域/分组三层树形分类管理（行业→领域→分组，最多 3 层），命名空间绑定、版本管理、分组拖拽重排。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/config/category` |
| 路由 name | `category` |
| 路由 meta.title | `行业分类` |
| 侧边栏 icon/label | `folder` / `行业分类` |
| 分组 | 配置管理 |

## 页面

**主文件**: `frontend/src/views/config/Category.vue`（~3100 行，大文件）
**CSS 前缀**: `cat-`

### 页面结构
- **PageHeader**: 搜索（中文/拼音/首字母）+ 导出 + 新建下拉（行业/领域/分组）
- **左栏（树）**: 树/列表切换，`TreeNode` 递归组件，展开折叠+统计徽标+右键菜单。领域节点有版本列表。快速添加悬浮菜单。
- **中栏**:
  - 无选中 → 全局概览：统计卡片（行业/领域/类/链接/动作/接口/属性）+ 行业柱状图 + 行业列表卡片
  - 有选中 → 面包屑+标题+统计卡片+行内创建+子节点网格卡片 / GroupGraph
- **右栏（详情抽屉，动态 tab）**:
  - industry/domain → 基础信息/命名空间（domain 独有：NS绑定/URI/版本/JSON元数据）/显示/其他
  - group → 基础信息/成员（拖拽排序+增删） / 显示/其他
- **新建/编辑抽屉**: 类型/父节点/编码/中文名/命名空间/图标/颜色/描述

### 子组件
- `TreeNode` — 递归树节点
- `GroupGraph` — 分组关联图
- `FieldRow` — 表单行（本地）
- `IconPickerField` / `ColorPickerField` / `CodeEditor`
- 图标选择全屏弹框：业务库（自定义 SVG 分组，支持上传/批量删）+ 内置图标库（18 分类组）

## 后端 API

### CategoryController — `/api/category`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/category/tree` | 完整分类树（嵌套层次）|
| GET | `/api/category/{id}` | 单节点详情 |
| GET | `/api/category/{id}/stats` | 节点下资源统计 |
| GET | `/api/category/{id}/graph` | 关联图数据（nodes+edges）|
| GET | `/api/category/picker` | 选择器数据 |
| GET | `/api/category/stats-batch?ids=a,b,c` | 批量统计 |
| GET | `/api/category/resolve-domain?code=` | 通过任意编码解析父领域 |
| POST | `/api/category` | 创建节点 |
| PUT | `/api/category/{id}` | 更新节点 |
| DELETE | `/api/category/{id}` | 删除节点（级联校验）|
| POST | `/api/category/{id}/members` | 添加成员到分组 |
| DELETE | `/api/category/{id}/members/{classId}` | 移除成员 |
| PUT | `/api/category/{id}/members/reorder` | 成员排序 |

### Service: CategoryService
- 组装树、计算统计、图数据、领域编码解析

### Mapper: BizCategoryMapper
- `listAll`, `findById`, `findByCode`, `countByParent`, `countDomainChildren`, `insert`, `update`, `delete`

## API 客户端

```js
export const categoryApi = {
  tree, get, resolveDomain, stats, statsBatch, graph, picker, // 查询
  create, update, remove,                                      // CRUD
  addMember, removeMember, reorderMembers                      // 成员管理
}
// 共 13 个方法
```

## 数据库

### ont_biz_category（分类节点）
| 列 | 说明 |
|---|---|
| `id` | `category-{UUID}` 或 `group-{UUID}` PK |
| `parent_id` | 自关联，'0'=顶级 |
| `category_code` | UNIQUE，小写+下划线 |
| `category_type` | 1=行业/2=领域/3=分组 |
| `ns_code` | 绑定命名空间 |
| `sort` | 排序号 |
| `icon` / `color` | 显示 |
| `status` | 0=禁用/1=启用 |
| `rdfs_label/comment/seeAlso/definedBy` | RDF 语义字段 |
| 索引: parent, type, ns |

### ont_biz_namespace（命名空间）
| 列 | 说明 |
|---|---|
| `id` | `namespace-{UUID}` PK |
| `ns_code` | UNIQUE |
| `ns_name` / `ns_uri` | 名称/URI |
| `curr_version` | 默认 '1.0' |
| `hierarchy_path` | 层次路径 |
| `status` | 0/1 |

附加表: `ont_biz_namespace_version` — 版本历史

### 种子数据
- 6 个行业: 水利/交通/应急/环境/林业/农业
- 10 个领域（水利下: 水土保持/水资源/水文等）
- 2 个分组（水文站/水文观测）
- 24 个命名空间

## 共享组件
- `PageHeader` / `CodeEditor`
- `IconPickerField` / `ColorPickerField`
