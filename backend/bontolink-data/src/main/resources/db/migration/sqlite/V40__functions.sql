-- =====================================================================
-- V40  函数 (Functions) 模块  —  SQLite 方言
-- 依据《本体管理系统-函数Functions.pdf》第 4 章数据表建全部表。
-- 标注「扩展」的字段/表为文档之外的必要补充, 原因见各处注释。
-- =====================================================================

-- ---------- 4.1 版本库主表 ont_version_repo ----------
CREATE TABLE IF NOT EXISTS ont_version_repo (
  id              TEXT PRIMARY KEY,                   -- "ont_version_repo-" + UUID
  rid             TEXT NOT NULL,                      -- ri.ont.version_repo.{32位}
  industry_dir    TEXT NOT NULL,                      -- 一级: 行业目录 (水利/交通/能源…)
  category_dir    TEXT NOT NULL,                      -- 二级: 领域目录 (水文监测函数集…)
  version_no      TEXT NOT NULL,                      -- 语义化版本号, 同行业同领域唯一
  repo_branch     TEXT NOT NULL DEFAULT '',           -- 代码仓库分支
  repo_commit_id  TEXT NOT NULL DEFAULT '',           -- 代码仓库 commit 哈希
  repo_url        TEXT,                               -- 仓库地址(可空)
  version_status  INTEGER NOT NULL DEFAULT 1,         -- 1草稿中/2发布中/3已发布/4已回滚
  is_default      INTEGER NOT NULL DEFAULT 0,         -- 同行业同领域仅一条为 1
  release_note    TEXT,                               -- 版本说明
  publish_user    TEXT,                               -- 发布人
  publish_time    TEXT,                               -- 发布时间
  is_deleted      INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_repo_rid ON ont_version_repo(rid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dir_version ON ont_version_repo(industry_dir, category_dir, version_no);

-- ---------- 4.2 函数主表 ont_function ----------
CREATE TABLE IF NOT EXISTS ont_function (
  id                TEXT PRIMARY KEY,                 -- "ont_function-" + UUID
  rid               TEXT NOT NULL,                    -- ri.ont.function.{32位}, 永久不变
  version_no        TEXT NOT NULL,                    -- 所属版本号, 与版本库对应
  api_name          TEXT NOT NULL,                    -- 代码内方法名(小驼峰), 同文件唯一
  function_label    TEXT NOT NULL,                    -- 中文业务名称
  function_type     INTEGER NOT NULL DEFAULT 1,       -- 1常规/2动作/3聚合/4衍生/5时序
  language          INTEGER NOT NULL DEFAULT 2,       -- 1PYTHON/2TYPESCRIPT
  industry_dir      TEXT NOT NULL,                    -- 行业目录, 与版本库对齐
  category_dir      TEXT NOT NULL,                    -- 领域目录, 与版本库对齐
  class_name        TEXT,                             -- 代码所属类名
  full_access_path  TEXT NOT NULL,                    -- /行业/领域/类名/方法名, 同版本唯一
  code_file_path    TEXT NOT NULL,                    -- 仓库内相对文件路径
  code_md5          TEXT,                             -- 源码 MD5 指纹
  code_content      TEXT,                             -- 扩展: 模板/源码内容(IDE 落地前供代码预览卡片使用)
  file_line_start   INTEGER,                          -- 源码起始行号
  file_line_end     INTEGER,                          -- 源码结束行号
  status            INTEGER NOT NULL DEFAULT 1,       -- 1草稿/2已发布/3已停用/4已废弃
  visibility        INTEGER NOT NULL DEFAULT 1,       -- 1全平台/2本部门/3指定角色/4私有
  rdfs_label        TEXT,                             -- 本体标准属性: 语义化标签
  rdfs_comment      TEXT,                             -- 本体标准属性: 功能详细描述(函数说明)
  rdfs_see_also     TEXT,                             -- 本体标准属性: 参考资料
  rdfs_defined_by   TEXT,                             -- 本体标准属性: 定义来源
  create_user       TEXT,                             -- 创建人账号
  publish_time      TEXT,                             -- 扩展: 发布时间(详情页基础信息要展示, 文档主表漏列)
  is_deleted        INTEGER NOT NULL DEFAULT 0,
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_function_rid  ON ont_function(rid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_path_version  ON ont_function(full_access_path, version_no);
CREATE INDEX IF NOT EXISTS idx_function_version    ON ont_function(version_no);
CREATE INDEX IF NOT EXISTS idx_function_dir        ON ont_function(industry_dir, category_dir);

-- ---------- 4.3 函数参数表 ont_function_param ----------
CREATE TABLE IF NOT EXISTS ont_function_param (
  id              TEXT PRIMARY KEY,                   -- "ont_function_param-" + UUID
  function_id     TEXT NOT NULL,                      -- FK -> ont_function.id
  param_name      TEXT NOT NULL,                      -- 参数变量名
  param_type      TEXT NOT NULL,                      -- 基础类型/枚举/本体类/自定义结构体
  param_direction INTEGER NOT NULL DEFAULT 1,         -- 1INPUT 入参 / 2OUTPUT 返回值
  is_required     INTEGER NOT NULL DEFAULT 1,         -- 仅入参有效
  default_value   TEXT,                               -- 默认值
  value_range     TEXT,                               -- 取值范围 / 可选项
  param_desc      TEXT,                               -- 参数业务含义(平台元数据, 可编辑)
  object_class_id TEXT,                               -- 扩展: 绑定的本体对象类 id, 支撑类型链接跳转对象详情页
  sort_num        INTEGER NOT NULL DEFAULT 0,
  is_deleted      INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_fn_param_function ON ont_function_param(function_id);

-- ---------- 4.4 运行配置表 ont_function_runtime_config (1:1) ----------
CREATE TABLE IF NOT EXISTS ont_function_runtime_config (
  id                TEXT PRIMARY KEY,                 -- "ont_function_runtime_config-" + UUID
  function_id       TEXT NOT NULL,                    -- FK -> ont_function.id, 一对一
  timeout           INTEGER NOT NULL DEFAULT 30,      -- 执行超时(秒) 1~3600
  retry_count       INTEGER NOT NULL DEFAULT 2,       -- 失败重试次数 0~5
  retry_interval    INTEGER NOT NULL DEFAULT 1,       -- 重试间隔(秒) 0~60
  memory_quota      INTEGER NOT NULL DEFAULT 512,     -- 内存配额(MB) 128~4096
  concurrency_limit INTEGER NOT NULL DEFAULT 100,     -- 并发限制(个) 1~1000
  enable_cache      INTEGER NOT NULL DEFAULT 1,       -- 结果缓存 0关/1开
  cache_ttl         INTEGER NOT NULL DEFAULT 3600,    -- 缓存有效期(秒) 60~86400
  is_deleted        INTEGER NOT NULL DEFAULT 0,
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fn_runtime_function ON ont_function_runtime_config(function_id);

-- ---------- 4.5 环境变量表 ont_function_env_var (1:N) ----------
CREATE TABLE IF NOT EXISTS ont_function_env_var (
  id          TEXT PRIMARY KEY,                       -- "ont_function_env_var-" + UUID
  function_id TEXT NOT NULL,                          -- FK -> ont_function.id
  var_name    TEXT NOT NULL,                          -- 变量名, 大写下划线
  var_value   TEXT NOT NULL DEFAULT '',               -- 变量值(敏感信息加密存储)
  var_type    INTEGER NOT NULL DEFAULT 1,             -- 1字符串/2数字型/3布尔型/4枚举型
  value_range TEXT,                                   -- 数字型=范围; 枚举型=逗号分隔可选值
  var_desc    TEXT,                                   -- 变量说明
  is_encrypt  INTEGER NOT NULL DEFAULT 0,             -- 0明文/1加密
  sort_num    INTEGER NOT NULL DEFAULT 0,
  is_deleted  INTEGER NOT NULL DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_fn_env_function ON ont_function_env_var(function_id);

-- ---------- 扩展: 调用统计 ont_function_call_stat ----------
-- 文档未定义此表, 但列表页「近7天/总调用」与详情页「可观测性 Tab」(4 指标卡 /
-- 趋势图 / 调用方 TOP3) 需要真实数据源, 否则只能前端写死。
-- 粒度: 一行 = 某函数 × 某天 × 某调用方。
CREATE TABLE IF NOT EXISTS ont_function_call_stat (
  id            TEXT PRIMARY KEY,                     -- "fn_call_stat-" + UUID
  function_id   TEXT NOT NULL,                        -- FK -> ont_function.id
  stat_date     TEXT NOT NULL,                        -- yyyy-MM-dd
  caller_app    TEXT,                                 -- 调用方应用名
  call_count    INTEGER NOT NULL DEFAULT 0,           -- 调用次数
  success_count INTEGER NOT NULL DEFAULT 0,           -- 成功次数
  error_count   INTEGER NOT NULL DEFAULT 0,           -- 错误次数
  avg_cost_ms   INTEGER NOT NULL DEFAULT 0,           -- 平均耗时(毫秒)
  create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_fn_stat_function ON ont_function_call_stat(function_id, stat_date);
