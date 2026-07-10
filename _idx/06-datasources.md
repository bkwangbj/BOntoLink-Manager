# 数据源 Datasources

> 外部数据库连接管理 — CRUD、连通性测试、连接池监控、物理表发现。支持 12 种数据库类型（MySQL/PostgreSQL/Oracle/MongoDB/DM/Kingbase/Oscar/GBase/PolarDB/TDSQL/GaussDB/OceanBase）。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/datasources` |
| 路由 name | `datasources` |
| 路由 meta.title | `数据源` |
| 侧边栏 icon/label | `database` / `数据源` |
| 分组 | 资源管理 |

## 页面

**主文件**: `frontend/src/views/resources/Datasources.vue`
**CSS 前缀**: `ds-`

### 页面结构
- **PageHeader**: 统计条（总量/正常/风险）+ 列表/分组切换 + 行业筛选 + 状态筛选 + 搜索 + 新建按钮
- **左侧**: `CategoryTreeFilter`
- **主列表**: 表格 — DB名/行业/引用数/状态/操作。支持批量选择/删除/启用/禁用。分页。
- **右侧抽屉** (z-index 1000, 可拖拽宽度, localStorage 持久化):
  - Tab1 **数据源**: 基本信息（名称/编码/行业/类型）+ 连接配置（JDBC驱动/URL/凭证 或 Mongo URL）+ 扩展（备注/最大连接/状态 radio）
  - Tab2 **监控**: 2×2 网格面板 — 全局状态(负载条+KV) / 基础配置(min idle/max conn) / 实时状态(活跃/等待) / 响应时间。池刷新/动态调整控制。
  - Tab3 **物理表**: 从数据源同步，可编辑表列表（名称/类型/列数/操作），分页

## 后端 API

### DataSourceController — `/api/datasource`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/datasource` | 列表 |
| GET | `/api/datasource/{id}` | 详情 |
| GET | `/api/datasource/overview` | 统计（总量/正常/风险 + 按类型分布）|
| GET | `/api/datasource/drivers` | JDBC 驱动目录（按类型分组）|
| GET | `/api/datasource/{id}/monitor` | 实时探测（连通性/响应时间/连接池指标）|
| GET | `/api/datasource/{id}/pool` | 连接池 MXBean 指标 |
| POST | `/api/datasource` | 新建 |
| PUT | `/api/datasource/{id}` | 更新 |
| DELETE | `/api/datasource/{id}` | 删除（级联销毁池+物理表）|
| POST | `/api/datasource/{id}/toggle` | 启用/禁用切换 |
| POST | `/api/datasource/{id}/test` | 测试连接（DriverManager 直连）|
| POST | `/api/datasource/{id}/pool/refresh` | 热刷新连接池 |
| POST | `/api/datasource/{id}/pool/resize` | 动态调整 maxPoolSize |

### Service: DataSourceService
- CRUD 编排 + 连接测试 + 池管理（`DynamicDataSourceRegistry`）

### Mapper: SysDataSourceMapper
- `listAll`, `findById`, `groupByType`, `insert`, `update`, `updateStatus`, `updateMonitor`, `updateMaxConn`, `delete`

## API 客户端

```js
export const datasourceApi = {
  list, get, overview, drivers, monitor, pool,   // 查询
  create, update, remove, toggle,                // CRUD
  test, refreshPool, resizePool                   // 运维
}
// 共 13 个方法
```

## 数据库

### sys_data_source
| 列 | 说明 |
|---|---|
| `id` | `datasource-{UUID}` PK |
| `category_code` | 所属业务领域 |
| `ds_code` / `ds_name` | 编码/名称 |
| `ds_type` | mysql/postgresql/oracle/mongodb/... |
| `jdbc_driver` / `jdbc_url` / `username` / `password` | JDBC 连接配置 |
| `mongo_url` | MongoDB 专用 |
| `status` | 1=启用/0=禁用 |
| `connect_status` | online/offline/risk |
| `active_conn` / `max_conn` | 连接池指标 |
| `response_ms` | 响应时间 |
| `collection_cnt` | 表数量（MongoDB）|
| `ref_count` | 引用次数 |
| 索引: `idx_ds_category`, `idx_ds_type` |

## 共享组件
- `CategoryTreeFilter` / `PageHeader` / `FieldRow` / `Pager`
