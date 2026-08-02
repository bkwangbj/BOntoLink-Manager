-- 外部数据源(HTTP 接口类) — 与 sys_data_source(数据库类) 并列的另一种数据源形式
-- 密钥/密码/Token 类字段目前与 sys_data_source 保持一致按明文存储, 待统一加密方案后一并改造 (TODO)

CREATE TABLE ont_ext_data_source (
  id               TEXT PRIMARY KEY,               -- "ext_ds-" + UUID
  category_code    TEXT,                           -- 所属业务领域
  ds_code          TEXT NOT NULL,                  -- 虚拟数据源编码, 同领域内唯一
  ds_name          TEXT NOT NULL,                  -- 数据源名称
  ds_type          TEXT NOT NULL DEFAULT 'http_rest', -- http_rest / webhook / graphql
  read_write_type  INTEGER NOT NULL DEFAULT 1,     -- 1 只读 / 2 读写
  base_url         TEXT,                           -- 基础地址, 所有接口路径基于此拼接
  default_method   TEXT DEFAULT 'POST',            -- 全局默认 HTTP 方法
  content_type     TEXT DEFAULT 'application/json',-- 默认请求体格式
  connect_timeout  INTEGER DEFAULT 5000,           -- 连接超时(ms)
  read_timeout     INTEGER DEFAULT 10000,          -- 读取超时(ms)
  retry_count      INTEGER DEFAULT 1,              -- 重试次数, 最大 3
  retry_interval   INTEGER DEFAULT 1000,           -- 重试间隔(ms)
  ssl_verify       INTEGER DEFAULT 1,              -- SSL 证书校验 0 关 / 1 开
  log_enable       INTEGER DEFAULT 1,              -- 请求日志记录 0 关 / 1 开
  header_enable    INTEGER DEFAULT 0,              -- 是否启用全局请求头
  global_header    TEXT,                           -- 全局请求头 JSON
  auth_type        TEXT DEFAULT 'none',            -- none/apikey/basic/bearer/oauth2_client/oauth2_code/cas/jwt/digest
  auth_config      TEXT,                           -- 鉴权参数 JSON
  status           INTEGER NOT NULL DEFAULT 1,     -- 0 禁用 / 1 启用
  remark           TEXT,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX uk_ext_ds_code ON ont_ext_data_source (category_code, ds_code);
CREATE INDEX idx_ext_ds_type ON ont_ext_data_source (ds_type);
CREATE INDEX idx_ext_ds_status ON ont_ext_data_source (status);

-- 接口分组(树形)
CREATE TABLE ont_ext_api_group (
  id          TEXT PRIMARY KEY,                    -- "ext_group-" + UUID
  ds_id       TEXT NOT NULL,                       -- 关联 ont_ext_data_source.id
  group_name  TEXT NOT NULL,
  parent_id   TEXT DEFAULT '0',                    -- 顶级为 '0'
  sort        INTEGER DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_ext_group_ds ON ont_ext_api_group (ds_id);

-- 接口定义
CREATE TABLE ont_ext_api_interface (
  id               TEXT PRIMARY KEY,               -- "ext_api-" + UUID
  ds_id            TEXT NOT NULL,
  group_id         TEXT DEFAULT '0',               -- 未分组为 '0'
  api_code         TEXT NOT NULL,                  -- 数据源内唯一, 供函数/动作引用
  api_name         TEXT NOT NULL,
  method           TEXT DEFAULT 'POST',
  api_path         TEXT,                           -- 相对路径, 与 base_url 拼接
  api_status       TEXT DEFAULT 'debug',           -- debug 调试 / done 完成
  read_write_type  INTEGER DEFAULT 1,              -- 1 只读 / 2 读写
  description      TEXT,
  request_params   TEXT,                           -- Header/Query/Path/Body 全量定义 JSON
  response_params  TEXT,                           -- 成功/失败多状态响应定义 JSON
  override_auth    INTEGER DEFAULT 0,              -- 0 继承全局 / 1 单独配置
  auth_type        TEXT,
  auth_config      TEXT,
  header_inherit   INTEGER DEFAULT 1,              -- 是否继承数据源全局 Header
  content_type     TEXT,                           -- 为空继承数据源
  timeout          INTEGER,                        -- 单位秒, 为空继承数据源
  status           INTEGER NOT NULL DEFAULT 1,
  sort             INTEGER DEFAULT 0,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX uk_ext_api_code ON ont_ext_api_interface (ds_id, api_code);
CREATE INDEX idx_ext_api_group ON ont_ext_api_interface (group_id);
CREATE INDEX idx_ext_api_status ON ont_ext_api_interface (status);

-- 调用日志
CREATE TABLE ont_ext_api_call_log (
  id             TEXT PRIMARY KEY,                 -- "ext_log-" + UUID
  trace_id       TEXT,                             -- 全链路追踪标识
  ds_id          TEXT NOT NULL,
  interface_id   TEXT,
  call_type      TEXT,                             -- function / action / debug
  caller         TEXT,                             -- 调用来源标识
  full_url       TEXT,
  request_header TEXT,                             -- 敏感字段脱敏后存储
  request_body   TEXT,
  call_status    INTEGER,                          -- 1 成功 / 2 失败 / 3 超时 / 4 鉴权失败
  http_status    INTEGER,
  cost_time      INTEGER,                          -- ms
  response_size  INTEGER,                          -- byte
  response_body  TEXT,
  error_msg      TEXT,
  call_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_ext_log_ds_time ON ont_ext_api_call_log (ds_id, call_time);
CREATE INDEX idx_ext_log_api_time ON ont_ext_api_call_log (interface_id, call_time);
CREATE INDEX idx_ext_log_status ON ont_ext_api_call_log (call_status);
