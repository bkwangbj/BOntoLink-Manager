# 函数 Functions

> 面向本体的业务逻辑单元管理 — 函数元数据、代码派生签名、运行配置、环境变量、调用统计。
> 依据《本体管理系统-函数Functions.pdf》实现;函数**代码本身**由「本体驱动的函数在线编排系统」(IDE,规划中)维护,本模块只管元数据与配置。

## 路由 & 导航

| 项目 | 值 |
|---|---|
| 路由 path | `/resources/functions` |
| 路由 name | `functions` |
| 路由 meta.title | `函数` |
| 侧边栏 icon/label | `branch` / `函数` |
| 分组 | 资源管理 |
| 深链 | `?openId=<函数id>` 直接打开详情抽屉 |

## 页面

**主文件**: `frontend/src/views/resources/Functions.vue`
**子组件目录**: `frontend/src/views/resources/function/`
**CSS 前缀**: `fn-`(列表)/ `fdt-`(目录树)/ `fnw-`(向导)/ `fdw-`(详情抽屉)/ `ftp-` `fdp-` `ffp-`(三个选择弹窗)/ `fci-`(combobox)/ `fcp-`(代码预览)

### 页面结构
- **PageHeader**: 统计条(总数/已发布/草稿/已停用)+ 关键词搜索(仅匹配函数名)+ 类型多选 + 语言单选 + 状态多选 + 可见性单选 + 查询 + `+` 新增
- **左侧**: `FunctionDirTree.vue` — 函数专属「行业领域分组」树(全部函数 → 行业 → 领域),数据源 `/api/functions/dirs`,**不走** `ont_biz_category`,因为函数目录是向导里可自由输入、不存在即自动创建的自由文本
- **主列表**: 13 列表格,前两列(多选框 / 函数名称)sticky 冻结,`table-layout: fixed`;全列表头可排序,默认最近更新倒序;分页 10/20/50(默认 10);底部批量删除 / 发布 / 停用
- **新增向导** (`FunctionCreateWizard.vue`, z-index 1200): 三步 —— 选择语言(进入第 2 步后锁定)→ 基础信息 → 定义签名
- **详情抽屉** (`FunctionDetailWorkspace.vue`, z-index 1010, 可拖宽 + 最大化 + localStorage 记忆宽度, 无蒙层):
  顶部信息栏 + 只读提示条(32px)+ 左导航 140px + 底部操作栏(仅编辑模式)
  - **概览**: 基础信息(左右两栏)/ 代码预览 / 输入参数 / 输出类型
  - **配置**: 运行配置(两列网格 7 项 + 超限红框角标)/ 环境变量(四类型差异化控件)/ 权限配置
  - **可观测性**: 4 指标卡 / echarts 趋势图(近 7/30/90 天)/ 调用方 TOP3

### 子组件

| 文件 | 作用 |
|---|---|
| `FunctionDirTree.vue` | 行业领域两级分组树 |
| `FunctionCreateWizard.vue` | 三步新增向导 |
| `FunctionDetailWorkspace.vue` | 详情抽屉工作台 |
| `TypePickerModal.vue` | 类型选择弹窗(基础类型 + 本体对象,参数/返回值共用) |
| `DirPickerModal.vue` | 目录树选择弹窗 |
| `FilePickerModal.vue` | 已有代码文件选择弹窗 |
| `ComboInput.vue` | 可输入下拉(行业/领域「输入不存在自动创建」) |
| `CodePreview.vue` | 行号 + 轻量正则语法高亮的只读代码块(**不用 Monaco**,IDE 落地后那边才用) |
| `FunctionTrendChart.vue` | 调用趋势折线图(echarts 按需引入) |
| `codeTemplate.js` | TS / Python 初始模板代码生成 + 类型名工具 |

## 数据表

| 表 | 说明 | 关键约束 |
|---|---|---|
| `ont_version_repo` | 版本库主表(行业+领域维度的版本序列) | `uk_dir_version(industry_dir, category_dir, version_no)` |
| `ont_function` | 函数主表,一条 = 某版本下的一个函数 | `uk_path_version(full_access_path, version_no)`、`idx_function_version`、`idx_function_dir` |
| `ont_function_param` | 入参 / 返回值 | `param_direction` 1=INPUT 2=OUTPUT |
| `ont_function_runtime_config` | 运行配置,1:1 | `uk_fn_runtime_function(function_id)` |
| `ont_function_env_var` | 环境变量,1:N | — |
| `ont_function_call_stat` | **超文档扩展**:按天 × 调用方的调用统计 | `idx_fn_stat_function(function_id, stat_date)` |

**迁移**: `sqlite/V40__functions.sql`、`postgresql/V40__functions.sql`、`common/V41__function_seed.sql`(5 个水利函数 + 1 个历史版本)、`common/V42__function_seed_real_object_types.sql`

**ID 前缀**: `ont_function-` / `ont_function_param-` / `ont_function_runtime_config-` / `ont_function_env_var-` / `ont_version_repo-` / `fn_call_stat-`
**RID**: `ri.ont.function.{32位}` / `ri.ont.version_repo.{32位}`

### 相对文档的字段增补(SQL 注释里标了「扩展」)
- `ont_function.code_content` — IDE 落地前供代码预览卡片使用
- `ont_function.publish_time` — 详情页要展示发布时间,文档主表漏列
- `ont_function_param.object_class_id` — 类型链接跳对象详情需要 id
- `ont_function_call_stat` 整表 — 支撑列表「近7天/总调用」与可观测性 Tab

### 枚举
- `function_type`: 1常规 / 2动作 / 3聚合 / 4衍生 / 5时序
- `language`: 1 Python / 2 TypeScript
- `status`: 1草稿 / 2已发布 / 3已停用 / 4已废弃
- `visibility`: 1全平台 / 2本部门 / 3指定角色 / 4私有
- `var_type`: 1字符串 / 2数字型 / 3布尔型 / 4枚举型
- `version_status`: 1草稿中 / 2发布中 / 3已发布 / 4已回滚

## 后端 API

### FunctionController — `/api/functions`

| 方法 | URL | 说明 |
|---|---|---|
| GET | `/api/functions` | 列表。**默认每个 `full_access_path` 只返回最新版本**;`?allVersions=true` 返回全部版本记录 |
| GET | `/api/functions/{id}` | 详情(含 params / in_params / out_params / runtime_config / env_vars / versions) |
| POST | `/api/functions` | 创建(校验小驼峰 + 同文件 API 唯一 + 路径版本唯一;自动建版本库、默认运行配置、算 `code_md5`) |
| PUT | `/api/functions/{id}` | 更新(请求未携带的键沿用旧值,避免 partial PUT 清空) |
| DELETE | `/api/functions/{id}` | 删除(级联参数/配置/环境变量/统计) |
| POST | `/api/functions/batch-delete` | 批量删除 |
| POST | `/api/functions/{id}/status` | 状态切换(1~4) |
| GET | `/api/functions/dirs` | 行业领域分组树(按列表可见口径计数) |
| GET | `/api/functions/files` | 已有代码文件清单 |
| GET | `/api/functions/check-api-name` | 实时校验 `{ name_valid, duplicated, file_exists, message }` |
| GET | `/api/functions/{id}/versions` | 同访问路径的全部版本 |
| GET | `/api/functions/{id}/stats?days=30` | 调用统计聚合 `{ total_calls, success_rate, avg_cost_ms, error_count, trend[], callers[] }` |
| PUT | `/api/functions/{id}/runtime-config` | 保存运行配置(服务端做取值范围校验) |
| PUT | `/api/functions/{id}/env-vars` | 环境变量整体覆盖 |
| PUT | `/api/functions/{id}/param-desc` | 只改参数说明(名称/类型是代码派生,只读) |

### VersionRepoController — `/api/version-repos`
`list` / `get` / `dir-options`(行业→领域两级选项) / `create` / `update`

### Mapper: FunctionMapper
主表 CRUD + `listVersionsByPath` / `existsByPathVersion` / `findIdByFileAndApiName` / `listDirCounts` / `listCodeFiles` / `listClassApiNames` + 参数 / 运行配置 / 环境变量 / 调用统计 / 版本库

## API 客户端

```js
export const functionApi = {
  list, get, create, update, remove, batchRemove, setStatus,
  dirs, files, checkApiName, versions, stats,
  saveRuntime, saveEnvVars, saveParamDesc,
}
export const versionRepoApi = { list, get, dirOptions, create, update }
```

## 关键实现约定(改这个模块前必读)

1. **方言中立**:入参出参类型串拼接、近 7 天调用量等聚合**一律在 Java 侧算**,SQL 里不用 `group_concat` / `string_agg` / 日期函数,避免 SQLite 与 PostgreSQL 分叉。
2. **最新版本口径**:列表、目录树计数都按 `latestPerPath()` 收敛;版本号比较用 `compareVersion()` 语义化解析(`v1.10.0 > v1.9.3`)。
3. **字段权责**(文档二):代码派生字段(api_name / 版本 / 类型 / RID / 类名 / 文件路径 / 发布时间 / 代码 / 参数名与类型)全程只读;平台元数据(中文名 / 说明 / 业务分类 / 可见性 / 参数说明 / 运行配置 / 环境变量)编辑模式可改。
4. **类型链接**:参数 `object_class_id` 为空时,后端按 `param_type` 的裸类名(`[ns] Station` → `Station`)反查 `ont_class.api_name` 补 id,让详情页类型链接可跳转对象详情。
5. **种子 SQL 禁忌**:代码类种子里不能出现 JS 模板字符串 `${}` 字面量(注释里也不行),Flyway 会当占位符解析并让后端启动直接失败。

## 未落地(规划中)

- **P5–P9 在线编排 IDE**:Monaco 编辑器、资源导入、依赖管理、运行、DAP 断点调试、终端。详情抽屉与代码预览卡片的「在代码仓库中打开 / 编辑」目前是占位提示。
- **对象类别详情页的「关联函数」Tab**(文档 2.4 上游入口):函数侧的深链 `?openId=` 已就绪,ObjectTypes 侧尚未加该 Tab。
- **全量调用方明细页**:可观测性 Tab 的「查看更多」目前是占位。
