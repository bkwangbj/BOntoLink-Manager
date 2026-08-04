-- =====================================================================
-- V40  函数 (Functions) 模块  —  PostgreSQL 方言
-- 与 sqlite/V40__functions.sql 表结构一一对应, 差异仅在类型与时间默认值。
-- 时间列一律 TIMESTAMP + CURRENT_TIMESTAMP; stat_date 保持 VARCHAR(10),
-- 让「近7天」这类日期比较在 Java 侧按字符串算, 两方言零分叉。
-- =====================================================================

-- ---------- 4.1 版本库主表 ont_version_repo ----------
CREATE TABLE IF NOT EXISTS ont_version_repo (
  id              VARCHAR(49) PRIMARY KEY,
  rid             VARCHAR(128) NOT NULL,
  industry_dir    VARCHAR(64)  NOT NULL,
  category_dir    VARCHAR(128) NOT NULL,
  version_no      VARCHAR(32)  NOT NULL,
  repo_branch     VARCHAR(64)  NOT NULL DEFAULT '',
  repo_commit_id  VARCHAR(64)  NOT NULL DEFAULT '',
  repo_url        VARCHAR(256),
  version_status  SMALLINT     NOT NULL DEFAULT 1,
  is_default      SMALLINT     NOT NULL DEFAULT 0,
  release_note    VARCHAR(1024),
  publish_user    VARCHAR(64),
  publish_time    TIMESTAMP,
  is_deleted      SMALLINT     NOT NULL DEFAULT 0,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_repo_rid ON ont_version_repo(rid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_dir_version ON ont_version_repo(industry_dir, category_dir, version_no);

-- ---------- 4.2 函数主表 ont_function ----------
CREATE TABLE IF NOT EXISTS ont_function (
  id                VARCHAR(49)  PRIMARY KEY,
  rid               VARCHAR(128) NOT NULL,
  version_no        VARCHAR(32)  NOT NULL,
  api_name          VARCHAR(128) NOT NULL,
  function_label    VARCHAR(128) NOT NULL,
  function_type     SMALLINT     NOT NULL DEFAULT 1,
  language          SMALLINT     NOT NULL DEFAULT 2,
  industry_dir      VARCHAR(64)  NOT NULL,
  category_dir      VARCHAR(128) NOT NULL,
  class_name        VARCHAR(128),
  full_access_path  VARCHAR(512) NOT NULL,
  code_file_path    VARCHAR(256) NOT NULL,
  code_md5          VARCHAR(32),
  code_content      TEXT,                              -- 扩展: 见 sqlite 版注释
  file_line_start   INTEGER,
  file_line_end     INTEGER,
  status            SMALLINT     NOT NULL DEFAULT 1,
  visibility        SMALLINT     NOT NULL DEFAULT 1,
  rdfs_label        VARCHAR(128),
  rdfs_comment      TEXT,
  rdfs_see_also     VARCHAR(256),
  rdfs_defined_by   VARCHAR(256),
  create_user       VARCHAR(64),
  publish_time      TIMESTAMP,                         -- 扩展: 见 sqlite 版注释
  is_deleted        SMALLINT     NOT NULL DEFAULT 0,
  create_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_function_rid ON ont_function(rid);
CREATE UNIQUE INDEX IF NOT EXISTS uk_path_version ON ont_function(full_access_path, version_no);
CREATE INDEX IF NOT EXISTS idx_function_version   ON ont_function(version_no);
CREATE INDEX IF NOT EXISTS idx_function_dir       ON ont_function(industry_dir, category_dir);

-- ---------- 4.3 函数参数表 ont_function_param ----------
CREATE TABLE IF NOT EXISTS ont_function_param (
  id              VARCHAR(49)  PRIMARY KEY,
  function_id     VARCHAR(49)  NOT NULL,
  param_name      VARCHAR(128) NOT NULL,
  param_type      VARCHAR(128) NOT NULL,
  param_direction SMALLINT     NOT NULL DEFAULT 1,
  is_required     SMALLINT     NOT NULL DEFAULT 1,
  default_value   VARCHAR(256),
  value_range     VARCHAR(512),
  param_desc      VARCHAR(512),
  object_class_id VARCHAR(64),                         -- 扩展: 见 sqlite 版注释
  sort_num        INTEGER      NOT NULL DEFAULT 0,
  is_deleted      SMALLINT     NOT NULL DEFAULT 0,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fn_param_function ON ont_function_param(function_id);

-- ---------- 4.4 运行配置表 ont_function_runtime_config (1:1) ----------
CREATE TABLE IF NOT EXISTS ont_function_runtime_config (
  id                VARCHAR(49) PRIMARY KEY,
  function_id       VARCHAR(49) NOT NULL,
  timeout           INTEGER     NOT NULL DEFAULT 30,
  retry_count       INTEGER     NOT NULL DEFAULT 2,
  retry_interval    INTEGER     NOT NULL DEFAULT 1,
  memory_quota      INTEGER     NOT NULL DEFAULT 512,
  concurrency_limit INTEGER     NOT NULL DEFAULT 100,
  enable_cache      SMALLINT    NOT NULL DEFAULT 1,
  cache_ttl         INTEGER     NOT NULL DEFAULT 3600,
  is_deleted        SMALLINT    NOT NULL DEFAULT 0,
  create_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_fn_runtime_function ON ont_function_runtime_config(function_id);

-- ---------- 4.5 环境变量表 ont_function_env_var (1:N) ----------
CREATE TABLE IF NOT EXISTS ont_function_env_var (
  id          VARCHAR(49)  PRIMARY KEY,
  function_id VARCHAR(49)  NOT NULL,
  var_name    VARCHAR(128) NOT NULL,
  var_value   VARCHAR(512) NOT NULL DEFAULT '',
  var_type    SMALLINT     NOT NULL DEFAULT 1,
  value_range VARCHAR(512),
  var_desc    VARCHAR(256),
  is_encrypt  SMALLINT     NOT NULL DEFAULT 0,
  sort_num    INTEGER      NOT NULL DEFAULT 0,
  is_deleted  SMALLINT     NOT NULL DEFAULT 0,
  create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fn_env_function ON ont_function_env_var(function_id);

-- ---------- 扩展: 调用统计 ont_function_call_stat ----------
CREATE TABLE IF NOT EXISTS ont_function_call_stat (
  id            VARCHAR(49) PRIMARY KEY,
  function_id   VARCHAR(49) NOT NULL,
  stat_date     VARCHAR(10) NOT NULL,
  caller_app    VARCHAR(128),
  call_count    INTEGER     NOT NULL DEFAULT 0,
  success_count INTEGER     NOT NULL DEFAULT 0,
  error_count   INTEGER     NOT NULL DEFAULT 0,
  avg_cost_ms   INTEGER     NOT NULL DEFAULT 0,
  create_time   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_fn_stat_function ON ont_function_call_stat(function_id, stat_date);
