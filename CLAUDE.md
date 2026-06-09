# BOntoLink02 — 项目协作指南

> 动态本体管理系统(水利行业) · Vue 3 + Spring Boot + SQLite
>
> **Claude 进入本项目时自动读此文件。修改时保持精简(<300 行),冗长内容放到独立文档并在这里引用。**

---

## 项目身份

- **名称**: BOntoLink02 / 动态本体管理系统
- **业务**: 面向水利行业的本体(Ontology)建模与元数据管理
- **核心模块**: 对象类型 / 链接类型 / 属性 / 值类型 / 枚举类型 / 共享属性 / 结构属性 / 接口 / 数据源 / 类型类

---

## 技术栈

### 后端
- **Java 21** + **Spring Boot**
- **MyBatis** (注解式 SQL,`mapUnderscoreToCamelCase: true`)
- **SQLite** (单机文件 `backend/bontolink.db`)
- **Maven** 构建
- **响应包装**: `R<T> = { code, msg, data }`,失败 `R.error(400, "...")`

### 前端
- **Vue 3** (`<script setup>` 风格)
- **Vite** 5.x
- **原生 CSS**(**无** Element / Antd / TailwindCSS,所有组件自研)
- **vue-router** hash 模式
- **Pinia** 状态(少用,大多在组件本地)
- **HTTP** 自定义 `http` 拦截器(`baseURL: /api`,自动解包 `R<T>`)

### 环境
- **Windows Server 2022** + **PowerShell 7**
- 前端 dev: `localhost:5173` (vite),代理 `/api → localhost:8088/bontolink`
- 后端: `localhost:8088`,context-path `/bontolink`

---

## 常用命令(直接复用)

### 前端
```powershell
cd c:\beiktech-jyx\BOntoLink02\frontend
npx vite build              # 生产构建(每次大改后必跑验证)
# dev server 由用户自己跑, Claude 不要 npm run dev
```

### 后端
```powershell
cd c:\beiktech-jyx\BOntoLink02\backend
mvn -DskipTests clean compile           # 只编译, 验证 Java 代码
mvn -q -DskipTests spring-boot:run      # 启动 (用 run_in_background)
```

### 数据库迁移(Flyway)/ 重置
- **schema/种子已迁到 Flyway**:`backend/src/main/resources/db/migration/`
  - `sqlite/V1__baseline_schema.sql`、`postgresql/V1__baseline_schema.sql`、`common/V2__baseline_seed.sql`(两方言共用种子)
- **增量改动 = 新建 `V3__说明.sql`**(方言相关放 `sqlite/`/`postgresql/`,通用放 `common/`),启动自动只跑未执行过的,记录在 `flyway_schema_history` 表。**不要改已发布的 Vn 文件**。
- **`bontolink.db` 不入库**(已 gitignore),由 Flyway 从迁移脚本重建。
- **重置**:
```powershell
Get-Process java | Stop-Process -Force -ErrorAction SilentlyContinue
Remove-Item c:\beiktech-jyx\BOntoLink02\backend\bontolink.db* -Force
# 再启动后端,Flyway 自动建表 + 灌种子(日志: "Successfully applied N migrations")
```

### 等待后端就绪(smoke test 用)
```bash
until curl -sf http://localhost:8088/bontolink/api/health > /dev/null; do sleep 2; done
curl -s http://localhost:8088/bontolink/api/xxx | head -c 600
```

---

## 必读关键约定

### CSS 类名前缀(每模块独立 namespace)

| 模块 | 前缀 |
|---|---|
| 对象类型 ObjectTypes | `ot-` |
| 链接 LinkTypes | `lk-` / `lke-`(编辑器) |
| 值类型 ValueTypes | `vt-` |
| 枚举类型 EnumTypes | `et-` |
| 共享属性 SharedProperties | `sp-` |
| 接口 Interfaces | `if-` |
| 数据源 Datasources | `ds-` |
| 类型类 TypeClasses | `tc-` |
| 行业分类 Category | `cat-` |
| 选择器模态 | `vtp-` / `spp-` / `ep-` |

### 数据库命名

- **表名**: `ont_xxx_yyy` (lowercase + underscore)
- **主键**: `id TEXT PRIMARY KEY` 格式 `"{prefix}-" + UUID`(如 `class-...`,`link-types-...`,`shared-properties-...`)
- **字段**: `snake_case` (`category_code`,`prop_type`,`created_at`)
- **时间戳**: `TEXT NOT NULL DEFAULT (datetime('now','localtime'))`
- **状态**: `status INTEGER` (1=启用/0=禁用) 或 `'active'/'inactive'/'deprecated'`(EnumTypes / LinkTypes)
- **RID**: `rid TEXT` 格式 `"ri.ont.{module}.{code}"`

### 后端 Mapper / Controller 模板

```java
// Mapper
@Mapper
public interface XxxMapper {
    @Select("SELECT * FROM ont_xxx ORDER BY update_time DESC")
    List<Map<String, Object>> listAll();
    // 用 Map<String, Object>, 不写 Entity 类
}

// Controller
@RestController
@RequestMapping("/api/xxx")
public class XxxController {
    @Autowired private XxxMapper mapper;
    @GetMapping public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }
    @PostMapping public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String id = "xxx-" + UUID.randomUUID();
        body.put("id", id);
        mapper.insert(body);
        return R.ok(mapper.findById(id));
    }
    // ... standard CRUD + batch-delete
}
```

### 前端 API 命名

`xxxApi` 在 `frontend/src/api/index.js`,标准 7 个方法:
```js
list / get(id) / create(data) / update(id, data) / remove(id) / batchRemove(ids) / [stats/refs/etc.]
```

### 字段适配(Jackson 兼容)

后端字段命名怪规则:**`gName` / `gSort` 会被 Jackson 序列化为小写** `gname` / `gsort`。前端始终用适配器:
```js
const label = g.gname || g.gName || g.g_name
const code = r.category_code || r.categoryCode || ''
```

### 图标 / Toast / Confirm

- 图标: `v-html="BL.icon('name', size, color?)"`(从 `src/lib/bl.js` ICONS 表)
- 反馈: `BL.success('...') / BL.warning / BL.error / BL.info`
- 确认: `const ok = await BL.confirm({ title, content, danger, okText })`
- 输入: `const v = await BL.prompt({ title, label, validate })`

---

## 视觉 / 交互基线(摘要)

详见 [UI规范.md](UI规范.md)。这里只列**最关键**几条:

### 三栏式资源页布局
```
PageHeader (标题 + 统计 + 筛选 + 搜索 + 新建按钮)
└── 主体: CategoryTreeFilter(左) + 列表(中) + 抽屉(右,fixed 浮层)
```

### 批量操作栏(全局统一 outline 配色)
```
[已选 N 项] [🗑️ 批量删除] [✓ 启用] [⏻ 禁用] [取消选择]
```
- 删除红:`background: #fff; border: 1px solid #f53f3f; color: #f53f3f`
- 启用绿:`#00b42a`
- 禁用灰:`#86909c`

### 详情抽屉头部
```
[36×36 大图标(按业务染色)] 标题 - 编码  [🔒/✏️ 只读/编辑] [□ 最大化] [× 关闭]
```
- z-index: 详情抽屉 1010,大模态弹框 ≥ 1200(必须高于抽屉)
- 不带蒙层(允许背景点击切换)
- 左边缘 5px 拖拽手柄,localStorage 持久化宽度

### 章节标题
```html
<div class="sec">基础信息</div>  <!-- 12px 灰色 + 3px 蓝色左边条 -->
```

---

## 已知模块清单

| 路由 | 主文件 | 数据表 |
|---|---|---|
| `/resources/object-types` | `views/resources/ObjectTypes.vue` | `ont_class + ont_class_property + ont_class_link` |
| `/resources/link-types` | `views/resources/LinkTypes.vue` + `linktype/LinkTypeEditor.vue` | `ont_link_types + ont_link_mappings` |
| `/resources/value-types` | `views/config/ValueTypes.vue` | `ont_value_types + ont_valuetypes_usage_config` |
| `/resources/enum-types` | `views/config/EnumTypes.vue` | `ont_enum_types + ont_enum_items + ont_enum_level_code_rule` |
| `/resources/shared-props` | `views/resources/SharedProperties.vue` + `sharedproperty/*` | `ont_shared_properties + ont_struct_types + ont_struct_items` |
| `/resources/interfaces` | `views/resources/Interfaces.vue` | `ont_interface + ont_interface_property + ont_interface_class` |
| `/resources/datasources` | `views/resources/Datasources.vue` | `sys_data_source` |
| `/config/category` | `views/config/Category.vue` | `ont_biz_category + ont_biz_namespace` |
| `/config/type-classes` | `views/config/TypeClasses.vue` | `ont_type_class` |

**共享组件**(必须复用,不要重写):
- `CategoryTreeFilter` — 6 模块共用的行业分类树
- `ValueTypePickerModal` / `EnumPickerModal` / `SharedPropertyPickerModal` — 三大资源选择面板
- `PropertyFormatModal` — 属性格式化弹框(`src_type=1` 普通属性 / `2` 共享属性)
- `useDraggableModal` + `DraggableHandles` — 大弹框拖动/最大化/缩放 composable
- `PageHeader` / `FieldRow` / `LeftGroupTree`(legacy)

---

## 高频踩坑(避免重复)

| 现象 | 根因 | 修复 |
|---|---|---|
| sticky 表格出现空白列 | `table-layout: auto` 实际列宽 ≠ sticky `left:` 偏移 | 用 `table-layout: fixed` 或移除 sticky |
| sticky 表头横滚被覆盖 | 角落 sticky 列 z-index 不够 | 角落格 `z-index: 4`(普通 sticky 3) |
| 抽屉里弹模态被遮挡 | 模态 z-index ≤ 抽屉(1010) | 大弹框 z-index ≥ 1200 |
| 高度链 `height: 100%` 算 0 | 父级用 `min-height: 100%` | 父级 `display: flex` + `flex: 1` |
| `watch immediate` 报 TDZ | 引用了下方 `const` 声明的 ref | 把 ref 提前到 watch 之前 |
| `BL.icon('xxx')` 渲染六边形 | ICONS 表里没有该名 → fallback cube | 在 `bl.js` ICONS 表新增 |
| 模板 `@event="(n) => ref = n"` 不生效 | ref 是 const 不可在模板表达式重新指向 | 改用 `function setX(n) { x.value = n }` |
| `Datasources` 用 `r.categoryCode` 其他用 `r.category_code` | Jackson 配置不一致 | 组件内适配 `r.category_code \|\| r.categoryCode` |
| 改 router 后页面空白 | HMR 缓存旧 lazy import | 浏览器硬刷新(Ctrl+Shift+R) |

---

## 权限白名单(.claude/settings.json)

为减少授权弹窗,以下命令族已纳入白名单(`./.claude/settings.json`)。**新增条目原则**:

- **可以加白名单**:只读 / 编译 / 测试 / 端口查询 / 杀本机 java 进程 / 项目本地数据库重置
- **不要加白名单**:`git push *` / `git reset --hard *` / `rm -rf *` / `npm install *` / `mvn deploy *` / `Remove-Item -Recurse *`

已放通的命令类(63 条):
- 文件读取:`grep / awk / sed -n / find / ls / cat / head / tail / wc / echo / printf / test / until`
- HTTP smoke test:`curl:*`
- Node 一次性脚本:`node -e:*`
- Git 只读:`git status / diff / log / show / branch / ls-files / rev-parse / config / remote -v`
- Maven 构建:`mvn ... compile / clean compile / spring-boot:run / test`
- 前端构建:`npx vite build / preview`
- PowerShell 只读:`Get-Process / Get-ChildItem / Get-Content / Test-Path / Select-String / Get-Command / Get-NetTCPConnection`
- 项目专用:`Stop-Process java*`,`Invoke-WebRequest *localhost*`

**修改前先想**:这条命令的最坏后果是什么?会写入数据 / 修改远程 / 删除文件吗?是 → 不放白名单。否 → 安全放。

---

## 禁忌(不要做)

- ❌ 引入 Element/Antd/Tailwind 等 UI 库 — 项目坚持原生 CSS
- ❌ 用 `fetch` — 统一走 `http` 拦截器
- ❌ 直接修改 `bl.js` 既有 icon path — 加新名即可
- ❌ 一个 module 自己拍脑袋造新视觉风格 — 先查 UI规范 + 其他模块
- ❌ 没读文件就 Edit — 必须先 Read
- ❌ `git push --force` 主分支 — 默认拒绝,除非用户明确说
- ❌ 跑 `npm run dev` / vite dev — dev server 由用户自己管
- ❌ 批量"美化代码" / "重构" — 用户没要求不要做
- ❌ 加注释解释代码做什么 — 只在 "为什么这么做不直观" 时加

---

## 协作偏好(用户风格)

- **中文回复**,简短直接,不要 disclaimer 式开场白
- **优先沟通**:截图 + 一句话需求 → Claude 探查 + 改 + 验证
- **风格一致 > 个性化**:每个新模块对齐既有模块
- **build verify 是底线**:大改后必 `vite build`,失败必修
- **不要过度澄清**:能从代码看出来的别问
- **F12 console 报错**:看到截图直接定位,不要猜
- **大任务先 plan**:>200 行 / 多文件改动,先给方案

---

## 相关文档

| 文件 | 内容 |
|---|---|
| [UI规范.md](UI规范.md) | 完整视觉/交互/组件规范(19 章) |
| [ClaudeCode使用指南.md](ClaudeCode使用指南.md) | 通用 Claude Code 协作方法论 |
| [help.md](help.md) | 个人协作复盘 + 业界经验 + 行动项 |
| `frontend/src/api/index.js` | 所有 API 客户端定义 |
| `backend/src/main/resources/db/migration/sqlite/V1__baseline_schema.sql` | SQLite 表结构(Flyway baseline) |
| `backend/src/main/resources/db/migration/common/V2__baseline_seed.sql` | 种子数据(两方言共用) |

---

## 新增模块快速 Checklist

1. **Schema/种子** (Flyway 迁移 `db/migration/`): 表/索引改 `sqlite/V1`(或新建 `V3__*.sql`),种子写 `common/V2`(或新 `Vn`)
2. **Mapper** (`backend/.../mapper/XxxMapper.java`): `@Mapper interface`,SQL 注解式
3. **Controller** (`backend/.../controller/XxxController.java`): `/api/xxx` REST CRUD
4. **API** (`frontend/src/api/index.js`): `xxxApi` 标准 7 方法
5. **页面** (`frontend/src/views/xxx/Xxx.vue`): 三栏布局 + CategoryTreeFilter + 抽屉
6. **路由** (`frontend/src/router/index.js`): 加 path / name / meta.title
7. **导航** (`frontend/src/components/AppSidebar.vue`): 加 sidebar 项 + icon
8. **验证**: `mvn clean compile` + `npx vite build` + 删 DB 重启 + curl smoke

---

## 文档版本

| 版本 | 日期 | 内容 |
|---|---|---|
| v1.0 | 2026-06-01 | 初版,基于 BOntoLink02 项目积累 |
