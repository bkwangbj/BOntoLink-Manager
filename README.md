# BOntoLink 动态本体管理系统

按《BOntoLink 博智联02：动态本体管理系统 --01正式版》与《BOntoLink-UI 设计规范总览》
两份需求文档实现的完整工程骨架。

## 技术栈

| 层 | 选型 |
|----|----|
| 前端 | Vue 3 + Vite + Vue Router + Pinia + Axios（无 UI 框架，自建 `bl-*` 组件） |
| 后端 | Java 21 + Spring Boot 3.3 + MyBatis Annotation + Spring JDBC |
| 数据库 | 默认 **SQLite**（嵌入式，启动即用）；同步支持 **PostgreSQL**（切 profile 即可） |

## 目录

```
BOntoLink02/
├── backend/                Spring Boot + Maven
│   └── src/main/
│       ├── java/com/beiktech/bontolink/
│       │   ├── BontoLinkApplication.java
│       │   ├── config/      数据库初始化 / CORS
│       │   ├── common/      统一响应 / 异常
│       │   ├── entity/      Biz* / OntClass
│       │   ├── mapper/      MyBatis Mapper（注解 SQL）
│       │   ├── service/     业务服务
│       │   └── controller/  REST 控制器
│       └── resources/
│           ├── application.yml             默认 sqlite profile
│           ├── application-sqlite.yml
│           ├── application-postgresql.yml
│           └── db/
│               ├── schema-sqlite.sql       SQLite DDL
│               ├── schema-postgresql.sql   PG DDL（字段一致）
│               └── data.sql                按 PDF 命名空间规则录入的 mock 数据
└── frontend/               Vue 3 + Vite
    └── src/
        ├── assets/styles/  tokens.css（颜色/字体/间距/圆角/阴影）+ base.css（bl-* 组件）
        ├── components/     AppTopBar / AppSidebar / NavItem / AiFab / PageHeader
        ├── lib/bl.js       BL.toast / BL.confirm / BL.icon（90+ 描边图标）
        ├── api/index.js    统一 axios + R<T> 解包 + 错误提示
        ├── stores/         Pinia（侧栏折叠 / 主题 / 强调色 / 当前领域）
        ├── router/         路由表（所有模块）
        └── views/          页面：workspace / resources / tools / config
```

## 设计规范落地

| 规范要点 | 实现位置 |
|---------|----------|
| 五项核心原则（业务优先 / 就地编辑 / 一致路径 / 键盘可达 / 可恢复） | 行业分类管理页面三分栏 + 抽屉表单 + 右键菜单 + ⌘K 全局搜索 + BL.confirm 删除校验 |
| 三层颜色系统（主色 / 4 语义色 / 中性 + 8 业务对象色） | `tokens.css` `--bl-primary` / `--bl-success/warning/danger/ai` / `--bl-obj-1..8` |
| 浅色 / 深色 / 跟随系统 三主题 | `tokens.css` `:root[data-theme="dark"]` + 顶栏「设置」抽屉 |
| 6 档字号 / 8 倍间距 / 4 档圆角 / 3 档阴影 | `tokens.css` 全部以 CSS 变量定义；业务页面严格使用变量，无硬编码 |
| 列表页"五件套"（搜索 / 筛选 / 行点击 / 行右键 / 批量条） | 行业分类管理 + ObjectTypes / LinkTypes 等列表页 |
| 抽屉式创建 + 行右键 + Tag 状态切换 | Category.vue 抽屉表单 / TreeNode 右键菜单 / .bl-tag 状态切换 |
| 文案规范（中文主显，hover 英文 tooltip） | NavItem 悬停显示 EN，侧栏底部双语并列 |
| AI FAB 右下角 + AI 助手专属紫色 | `components/AiFab.vue` + `--bl-ai` |

## 命名空间 / RID 规则

完全按 PDF "1.2 本体空间命名规则" + "1.9 rid 全局唯一资源标识" 落库：

- 前缀：`w_root` / `w_common` / `w_wtr` / `w_wtr_hyd` …
- URI：`http://watf.com/ont/v1/water/hydrology#`
- 层级标识：`watf.water.hydrology`
- RID：`ri.ont.biz.category.<uuid>`、`ri.ont.class.<uuid>` 等

## 启动

### 后端

```bash
cd backend
mvn spring-boot:run         # 默认 SQLite，端口 8088，上下文 /bontolink
# 切 PG：
mvn spring-boot:run -Dspring-boot.run.profiles=postgresql
```

启动会自动执行 `db/schema-*.sql` + `db/data.sql`，写入：
- 6 个行业、10 个水利下属领域、2 个水文分组
- 22 个命名空间、5 个版本
- 4 个对象类型、2 个关系、2 个动作、2 个接口、5 条属性

### 前端

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173 ，已配置 /api 代理到后端
npm run build               # 输出 dist/
```

## 主要 REST 端点

| 方法 | 路径 | 用途 |
|----|----|----|
| GET  | `/api/health` | 健康检查 |
| GET  | `/api/category/tree` | 行业 → 领域 → 分组 完整树 |
| GET  | `/api/category/{id}/stats` | 选中节点的下级/对象/关系/动作/属性统计 |
| POST `/api/category` / PUT `/api/category/{id}` / DELETE | 节点 CRUD（有下级节点禁止删） |
| GET  | `/api/namespace` | 命名空间列表（含 URI + 层级标识） |
| GET  | `/api/namespace/{code}/versions` | 版本列表 |
| POST | `/api/namespace/versions` | 创建版本（snapshot/OWL 字段就位） |
| GET  | `/api/resource/discover/stats` | 总览数据卡片 |
| GET  | `/api/resource/{classes\|links\|actions\|interfaces}` | 资源列表 |
| GET  | `/api/resource/graph` | 图谱节点 + 边 |

## 数据库切换

应用通过 `bontolink.db.dialect` + `bontolink.db.schema-init` + `bontolink.db.data-init` 三个键
解耦：

- `application-sqlite.yml` → `schema-sqlite.sql` + `data.sql`
- `application-postgresql.yml` → `schema-postgresql.sql` + `data.sql`

两份 schema 字段一一对应（仅类型从 `VARCHAR(N)` → `TEXT`、`DATETIME` → `TIMESTAMP`），
业务 SQL（`data.sql`、Mapper 注解 SQL）使用标准 ANSI 子集，无方言差异。
