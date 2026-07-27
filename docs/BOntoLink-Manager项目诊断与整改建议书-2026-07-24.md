# BOntoLink-Manager 项目诊断与整改建议书

> 生成日期：2026-07-24
> 整合来源：《项目不足排查报告-2026-07-24.md》（安全 / 工程化）、《功能设计问题报告-2026-07-24.md》（功能设计 / 交互 / 业务 / 需求符合度）
> 排查方式：全量只读审查，关键发现均经人工二次验证
> 适用范围：backend（四模块）/ backend-old / frontend / bk-analysis-maker-vue3 / 根目录工程化与文档

---

## 0. 文档说明

本报告是两轮排查的整合交付物，目标读者为技术负责人与开发团队。左侧两份专题报告保留作为分项证据底稿，本文件给出统一的问题地图与整改路线图。

**关键澄清（已与项目方确认）**
- 语义扩充模块（同义词/领域术语 CRUD + 匹配）属**新增在测功能**，需求文档尚未同步，并非"无需求依据的自建"。其当前前后不一致（后端有 API、前端无入口、ontology 有消费、PG 部署下表不存在）应理解为**测试阶段未完成项**，不列为设计缺陷。
- ontology 模块"按现状无法启动"已用配置证据坐实（激活 `sqlite` profile 却无对应数据源配置，且包扫描不覆盖 data 层）。

---

## 1. 项目概况

BOntoLink-Manager 是一套"动态本体管理系统"：以行业→领域→分组三级树管理对象类型/关系/动作/接口/属性/枚举/值类型/命名空间/数据源等本体资源，并提供图谱展示、资源发现、语义扩充、内嵌图表设计器等功能。后端为 Maven 四模块（data 数据层 / base 基础层 / admin 管理 API 8088 / ontology 本体与语义 API 8089），前端为 Vue3 + Vite 主前端，外加 bk-analysis-maker-vue3 图表组件库。

**诊断结论一句话**：系统已实现可观的功能骨架，但**安全地基缺失、演示数据污染生产功能、双服务架构收益为零、文档与实现严重脱节**四类问题叠加，当前状态不适合直接对外交付或上线。

---

## 2. 问题总览

| 维度 | 严重 | 中等 | 轻微 | 说明 |
|------|------|------|------|------|
| 安全 | 7 | 2 | — | 零认证 + 任意 SQL 执行 + XSS |
| 功能信任（mock 污染） | 7 | — | — | 假数据混入生产响应 |
| 架构 / 构建 | 3 | 4 | 4 | Flyway 引爆、ontology 死服务、仓库卫生 |
| 业务设计 / 数据模型 | — | 6 | 4 | 双链接模型断裂、删除级联漏洞 |
| 交互 / 信息架构 | — | 4 | 4 | 死链、全局筛选摆设、重复建设 |
| 需求符合度 | 12 | — | — | 对照 260521 正式版缺失项 |
| 工程化 / 文档 | 1 | 5 | 5 | 零 CI、README 失实、文档散乱 |
| **合计** | **30** | **21** | **17** | |

> 注：部分中等/轻微项在专题报告中交叉归类，此处按主维度去重计数。

---

## 3. 严重问题详解

### 3.1 安全缺口（最高优先级，建议 1-3 天内处理）

| # | 问题 | 位置与证据 |
|---|------|-----------|
| S1 | **无鉴权任意 SQL 执行**：`POST /api/debug/query` 直接执行请求体 SQL，仅靠 `startsWith("SELECT")` 弱校验（注释/换行可绕过）；`GET /api/debug/schema/{tableName}` 路径变量拼入 `PRAGMA` 构成注入 | `DebugSqlController.java:27-46, 67-76` |
| S2 | **用户自定义 SQL 直达外部数据源可写库**：`EnumSyncService.previewSql` 用 `Statement.executeQuery` 对任意已配置数据源执行用户 SQL，PG/MySQL 支持 stacked queries 可写库，接口无鉴权 | `EnumSyncService.java:201`、`EnumTypeController.java:231-244` |
| S3 | **全系统零认证 + CORS 全开放带凭据**：无 spring-security；`allowedOriginPatterns("*")+allowCredentials(true)`，任意站点可携 cookie 调全部接口 | `CorsConfig.java:12-16` |
| S4 | **真实开发库凭据明文入库**：`dev.beiktech.com:9523` + `postgres/postgres` 硬编码未用环境变量 | `application-postgresql.yml:4-6` |
| S5 | **数据源密码"加密"名存实亡**：schema 注释"加密密文"，代码零解密逻辑 | `V1__baseline_schema.sql:128`、`DataSourceConnector.java:73` |
| S6 | **前端存储/反射 XSS 三处**：`BL.icon()` 后端 SVG 未消毒供 200+ 处 v-html；`Instances.vue:74` 搜索词未转义；`BL.confirm()` 的 innerHTML 不转义 | `frontend/src/lib/bl.js:385, 423-431`、`Instances.vue:74,144` |
| S7 | **eval / new Function 执行库存字符串代码 10+ 处**：图表规则以 `eval(ruleList)` 执行，既是 XSS 面也阻断 CSP | `bk-analysis-maker-vue3/.../pie-chart/main.vue:179` 等 |

### 3.2 演示数据污染生产功能（信任危机）

这是功能设计层最大隐患：**mock 与真实数据在同一接口混合返回，用户无法区分**。

| # | 问题 | 证据 |
|---|------|------|
| S8 | **图谱接口编造语义关系**：`/api/graph/ontology` 真实边不足时用 `Random(42)` 随机生成 eq/dis/union/link 假边，目标"整图至少 60 条边" | `GraphController.java:124, 187-213` |
| S9 | **实例探索全量伪造**：`/api/instance/*` 走 `InstanceMockService` 随机生成，物理数据源绑定不读真实表 | `InstanceController.java:23` |
| S10 | **对象图谱 Tab 全 mock**：无论选哪个对象类型都渲染同一份"水文测站"维度 | `TabObjectGraph.vue:192-262` |
| S11 | **AI 助手三触点全摆设**：页面 setTimeout 假对话、悬浮球被注释隐藏、对象抽屉按钮只 toast"待联调" | `AiAssistant.vue:37-39`、`App.vue:13` |
| S12 | **新建向导内置 mock 物理表兜底**：无后端时静默用假表清单 | `NewObjectTypeWizard.vue:328` |
| S13 | **时序图表/事件时间轴为合成演示数据** | `TimeseriesChart.vue:6`、`EventTimeline.vue:7` |
| S14 | **语义扩充查询演示结果 mock** | `lib/analyzers.js:77` |

### 3.3 架构与构建引爆点

| # | 问题 | 证据 |
|---|------|------|
| S15 | **Flyway 迁移双向引爆**：`common/` 两个 V4 冲突、`common/V3` 与 `sqlite/V3` 冲突且后者未提交——当前工作区启动必崩，全新克隆缺 7 张语义表 | `bontolink-data/.../db/migration/` |
| S16 | **50MB SQLite 二进制被 git 跟踪**：`backend-old/` 两个 25MB .db 在库，`.gitignore` 无通用 `*.db` | `git ls-files` 已验证 |
| S17 | **ontology 模块按现状无法启动**：激活 sqlite profile 无对应数据源配置、包扫描不覆盖 data 层，却打包可执行 jar | `BontoLinkOntologyApplication.java:14` |
| S18 | **frontend/package-lock.json 被忽略未入库**，主前端依赖不可复现 | `.gitignore:25` |
| S19 | **README 与实际架构严重不符**：文档说单模块/8088 单服务，实际四模块双服务 + Flyway；三份文档三种说法 | `README.md` vs `backend/pom.xml`、`QUICK_START.md` |

---

## 4. 中等问题详解（精选）

### 4.1 后端
- **测试覆盖为零**：四模块均声明 starter-test 却无一测试类
- **参数校验整体缺失**：`@Valid/@NotBlank` 0 命中；普遍用 `Map<String,Object>` 接参
- **SQLite 与 PG schema 严重漂移**：SQLite 41 表 0 索引几乎无外键；PG 30+ 索引 → SQLite 下可重复、全表扫描
- **异常吞没与信息泄露**：多处 `catch{}` 空块；`e.getMessage()` 原样返回前端
- **`@Transactional` 标在 Controller 上**，破坏分层

### 4.2 前端
- **无 lint/test/typecheck 脚本**，两前端零测试
- **依赖过旧/废弃**：vite 5（EOL）、monaco 0.33、@antv/g6 v4、eslint 8
- **构建产物误提交**：bk-analysis-maker 的 `dist/`（3.4MB）入库
- **超长单文件**：Category.vue 3079 行、EnumTypes.vue 2475 行等 15+ 个 500+ 行
- **两套 UI 框架并存**：element-plus + tdesign，与 README 宣称"无 UI 框架"矛盾

### 4.3 业务设计 / 数据模型
- **双链接模型互不同步（核心）**：图谱/实例探索读旧表 `ont_class_link`（无 INSERT API，仅 20 行种子），CRUD 写新表 `ont_link_types`——界面创建的链接永不在图谱出现
- **"有下级禁止删除"可被 update 绕过**：无环检测、改 code 后子表引用悬空
- **两套删除入口行为矛盾**：group 端注释级联实不级联，category 端反而级联
- **级联删除漏表 / 先删后写无事务**：失败即数据丢失
- **四套标识并存**（id/rid/api_name/code）、JSON-in-TEXT 17 处、无外键无软删除

### 4.4 交互 / 信息架构
- **全局"领域"选择器是纯摆设**：`selectedDomains` 无页面读取，各页各自造筛选
- **死链 + 无 404**：总览"动作类型"跳不存在路由，全站无兜底
- **向导步骤条造假**：显示"步骤 1/2"，步骤 2 不存在
- **重复建设**：图谱三套、图表两套、属性画布双版本、全局搜索三行为；5 个死代码文件

### 4.5 工程化 / 文档
- **零 CI/CD**：无 workflow / Dockerfile / Jenkinsfile
- **tools/ 脚本硬编码他人机器路径**（`c:\beiktech-jyx\...`）
- **无 LICENSE**；二进制 `UI规范.docx` 入库无法 diff
- **文档散乱五处无索引**：根目录 10+ md、docs/、开发文档/（26 PDF 多版本）、UI术语规范/、_idx/

---

## 5. 轻微问题（择机处理）

- 硬编码 Windows 绝对路径入代码（`ApiNameConflictChecker.java:15`）
- 假接口占位（`sync-test` 固定返回 OK）
- 迁移跳号（缺 V17、V19 为 no-op），老库校验和与新文件不一致将 validate 失败
- 种子数据含内网拓扑（10.0.0.x、polardb.aliyun 等）
- 可观测性缺失：无 actuator / swagger / logback 滚动策略
- console.log 少量残留；localStorage 键名前缀 `bl.*`/`bontolink.*` 两套并存
- 无文案常量层（"确定"分散 66 处）
- 根目录临时文件堆积（日期+中文一次性文档、3 个导出 html）
- 无 TypeScript；第三方组件库源码混入业务仓

---

## 6. 需求符合度缺口（对照 260521 正式版）

| # | 缺失功能 | 需求出处 |
|---|----------|----------|
| R1 | OWL 语义层整体缺失（复杂类表达式、属性 OWL 特性） | 260521 P37-57 |
| R2 | 版本快照与回滚（字段存在从不写入） | 260521 P12/15 |
| R3 | OWL/RDF/PNG/PDF/JSON 导出链路 | 260521 P15 |
| R4 | 实例真实查询（需求定位为低技术用户搜索分析工具） | 实例探索 6.10 |
| R5 | 动作类型 CRUD（可绑定却无法创建） | 260521 P10/60 |
| R6 | 数据源连接池监控面板 | 260521 P25-26 |
| R7 | 属性格式化生效链路（全系统自动生效） | 属性格式化 5.27 |
| R8 | 函数模块（空路由占位） | 260521 P10/60 |
| R9 | 权限安全（Object Type 控制、可见性三级） | 260521 P10 |
| R10 | 导入导出（四按钮均无事件） | 260521 P10 |
| R11 | 停用词管理（表存在无 CRUD） | — |
| R12 | 总览 15 项统计与点击下钻（路由写错、参数丢失） | 总览 6.5 |

> 语义扩充（同义词/领域术语）经确认属在测新功能，不列入缺失项（见 0 节澄清）。

---

## 7. 统一整改路线图

按"风险 × 工作量 × 收益"排序，建议分四阶段推进。

### P0 — 安全与信任（必须，1 周内）
1. **堵 SQL 执行口**：删除 `DebugSqlController` 或加 `@Profile("dev")` + 鉴权；`sync-preview-sql` 改为只读预校验或加鉴权
2. **收敛 CORS**：`allowedOriginPatterns` 改为显式白名单；引入认证（spring-security / 网关）
3. **凭据外置与轮换**：`application-postgresql.yml` 改 `${ENV}` 占位；轮换 postgres 口令；落实数据源密码真的加密
4. **加 demo-mode 开关**：`bontolink.demo-mode` 全局控制全部 mock 注入，关闭时前端对演示数据加水印。半天可消除"系统说谎"风险
5. **前端 XSS 消毒**：`BL.icon/confirm/iconText` 加 DOMPurify 或转义；`Instances.vue` 搜索词转义

### P1 — 架构解耦与数据完整性（2-4 周）
6. **修 Flyway**：统一 V3/V4 版本号、提交缺失迁移文件、新旧库校验和对齐
7. **仓库瘦身**：`git rm --cached` 两个 25MB db + 归档删除 backend-old；补 `*.db` 忽略；frontend lock 入库
8. **合并双链接模型**：`ont_class_link` 与 `ont_link_types` 二选一，打通"建的链接进图谱"
9. **ontology 服务决策**：补数据源配置真正启用并接前端，或把 3 个端点并回 admin 下线 8089
10. **统一删除策略**：引用检查清单 + 级联补漏 + 事务包裹；消除级联漏表与先删后写

### P2 — 功能闭环与交互修复（1-2 月）
11. 修死链 + 404 路由 + 打通 `selectedDomains` 全局筛选（小改动全局受益）
12. 向导步骤条与实际流程对齐；补"创建后补挂物理表"入口
13. 假按钮要么接真实能力要么下线；打通语义扩充前端入口与 PG 迁移脚本
14. 需求缺口 R1-R12 按优先级排期（建议先 R5 动作类型 CRUD、R4 实例真实查询）

### P3 — 工程化地基（持续）
15. 引入单元测试 + 参数校验（`@Valid`）；补齐 CI/CD（build + test + 镜像）
16. 重写 README 匹配四模块双服务现状；归档一次性文档到 docs/；确认需求基准版本（260521）并归档旧版
17. 依赖升级（vite/eslint/monaco/g6）；死代码文件删除；UI 框架收敛一套

---

## 8. 关键证据索引

| 主题 | 底稿文件 | 章节 |
|------|----------|------|
| 安全 / 工程化 全量 | `docs/项目不足排查报告-2026-07-24.md` | 一、二、三、四 |
| 功能设计 / 交互 / 业务 | `docs/功能设计问题报告-2026-07-24.md` | 一~八 |
| 语义扩充修订说明 | 功能设计报告 | 第二节末、第六节第 2 点 |
| ontology 启动验证 | 本次会话已验证 | 0 节澄清 |

---

*本报告可与两份专题底稿配套使用。建议先执行 P0 第 4 项（demo-mode 开关），半天内即可消除最大的用户信任风险，且不依赖其他改动。*
