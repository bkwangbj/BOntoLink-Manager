-- 外部数据源(HTTP 接口类) — 与 sys_data_source(数据库类) 并列的另一种数据源形式
-- 密钥/密码/Token 类字段目前与 sys_data_source 保持一致按明文存储, 待统一加密方案后一并改造 (TODO)

CREATE TABLE ont_ext_data_source (
  id               VARCHAR(47) PRIMARY KEY,
  category_code    VARCHAR(64),
  ds_code          VARCHAR(64) NOT NULL,
  ds_name          VARCHAR(128) NOT NULL,
  ds_type          VARCHAR(32) NOT NULL DEFAULT 'http_rest',
  read_write_type  INTEGER NOT NULL DEFAULT 1,
  base_url         VARCHAR(512),
  default_method   VARCHAR(16) DEFAULT 'POST',
  content_type     VARCHAR(64) DEFAULT 'application/json',
  connect_timeout  INTEGER DEFAULT 5000,
  read_timeout     INTEGER DEFAULT 10000,
  retry_count      INTEGER DEFAULT 1,
  retry_interval   INTEGER DEFAULT 1000,
  ssl_verify       INTEGER DEFAULT 1,
  log_enable       INTEGER DEFAULT 1,
  header_enable    INTEGER DEFAULT 0,
  global_header    TEXT,
  auth_type        VARCHAR(32) DEFAULT 'none',
  auth_config      TEXT,
  status           INTEGER NOT NULL DEFAULT 1,
  remark           VARCHAR(512),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_ext_ds_code ON ont_ext_data_source (category_code, ds_code);
CREATE INDEX idx_ext_ds_type ON ont_ext_data_source (ds_type);
CREATE INDEX idx_ext_ds_status ON ont_ext_data_source (status);

CREATE TABLE ont_ext_api_group (
  id          VARCHAR(47) PRIMARY KEY,
  ds_id       VARCHAR(47) NOT NULL,
  group_name  VARCHAR(128) NOT NULL,
  parent_id   VARCHAR(47) DEFAULT '0',
  sort        INTEGER DEFAULT 0,
  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ext_group_ds ON ont_ext_api_group (ds_id);

CREATE TABLE ont_ext_api_interface (
  id               VARCHAR(47) PRIMARY KEY,
  ds_id            VARCHAR(47) NOT NULL,
  group_id         VARCHAR(47) DEFAULT '0',
  api_code         VARCHAR(64) NOT NULL,
  api_name         VARCHAR(128) NOT NULL,
  method           VARCHAR(16) DEFAULT 'POST',
  api_path         VARCHAR(256),
  api_status       VARCHAR(16) DEFAULT 'debug',
  read_write_type  INTEGER DEFAULT 1,
  description      VARCHAR(512),
  request_params   TEXT,
  response_params  TEXT,
  override_auth    INTEGER DEFAULT 0,
  auth_type        VARCHAR(32),
  auth_config      TEXT,
  header_inherit   INTEGER DEFAULT 1,
  content_type     VARCHAR(64),
  timeout          INTEGER,
  status           INTEGER NOT NULL DEFAULT 1,
  sort             INTEGER DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_ext_api_code ON ont_ext_api_interface (ds_id, api_code);
CREATE INDEX idx_ext_api_group ON ont_ext_api_interface (group_id);
CREATE INDEX idx_ext_api_status ON ont_ext_api_interface (status);

CREATE TABLE ont_ext_api_call_log (
  id             VARCHAR(47) PRIMARY KEY,
  trace_id       VARCHAR(64),
  ds_id          VARCHAR(47) NOT NULL,
  interface_id   VARCHAR(47),
  call_type      VARCHAR(32),
  caller         VARCHAR(128),
  full_url       VARCHAR(1024),
  request_header TEXT,
  request_body   TEXT,
  call_status    INTEGER,
  http_status    INTEGER,
  cost_time      INTEGER,
  response_size  INTEGER,
  response_body  TEXT,
  error_msg      VARCHAR(512),
  call_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ext_log_ds_time ON ont_ext_api_call_log (ds_id, call_time);
CREATE INDEX idx_ext_log_api_time ON ont_ext_api_call_log (interface_id, call_time);
CREATE INDEX idx_ext_log_status ON ont_ext_api_call_log (call_status);
