-- =============================================================
-- BOntoLink — PostgreSQL Schema (与 SQLite 字段完全对应)
-- =============================================================

CREATE TABLE IF NOT EXISTS ont_biz_category (
  id              VARCHAR(45)  PRIMARY KEY,          -- "category-" + UUID；type=3 分组用 "group-" 前缀,与 ont_biz_group 共用同一 id
  parent_id       VARCHAR(45)  NOT NULL DEFAULT '0',
  rid             VARCHAR(128),
  category_code   VARCHAR(64)  NOT NULL UNIQUE,
  category_type   SMALLINT     NOT NULL DEFAULT 1,
  ns_code         VARCHAR(24),
  status          SMALLINT     NOT NULL DEFAULT 1,
  sort            INTEGER      NOT NULL DEFAULT 0,
  icon            VARCHAR(64),
  color           VARCHAR(20),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  rdfs_see_also   VARCHAR(512),
  rdfs_defined_by VARCHAR(255),
  description     TEXT,
  metadata        TEXT,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_biz_category_parent ON ont_biz_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_biz_category_type   ON ont_biz_category(category_type);
CREATE INDEX IF NOT EXISTS idx_biz_category_ns     ON ont_biz_category(ns_code);

CREATE TABLE IF NOT EXISTS ont_biz_namespace (
  id              VARCHAR(46)  PRIMARY KEY,
  ns_code         VARCHAR(24)  NOT NULL UNIQUE,
  ns_name         VARCHAR(64)  NOT NULL,
  ns_uri          VARCHAR(255),
  hierarchy_path  VARCHAR(255),
  curr_version    VARCHAR(20)  NOT NULL DEFAULT '1.0',
  status          SMALLINT     NOT NULL DEFAULT 1,
  metadata        TEXT,
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_namespace_version (
  id              VARCHAR(48)  PRIMARY KEY,
  ns_code         VARCHAR(24)  NOT NULL,
  version         VARCHAR(20)  NOT NULL,
  uri             VARCHAR(255) NOT NULL,
  snapshot_data   TEXT,
  owl_content     TEXT,
  publish_time    TIMESTAMP,
  is_current      SMALLINT     NOT NULL DEFAULT 0,
  status          SMALLINT     NOT NULL DEFAULT 1,
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  rdfs_see_also   VARCHAR(512),
  rdfs_defined_by VARCHAR(255),
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_biz_ns_v_code ON ont_biz_namespace_version(ns_code);

CREATE TABLE IF NOT EXISTS ont_biz_group (
  id              VARCHAR(42)  PRIMARY KEY,
  parent_id       VARCHAR(45),
  category_code   VARCHAR(64),
  g_name          VARCHAR(64)  NOT NULL,
  g_sort          INTEGER      NOT NULL DEFAULT 0,
  icon            VARCHAR(64),
  color           VARCHAR(20),
  description     TEXT,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_group_class (
  id              VARCHAR(50)  PRIMARY KEY,
  group_id        VARCHAR(42)  NOT NULL,
  class_id        VARCHAR(42)  NOT NULL,
  category_code   VARCHAR(64),
  g_sort          INTEGER      NOT NULL DEFAULT 0,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class (
  id              VARCHAR(42)  PRIMARY KEY,
  rid             VARCHAR(128),
  api_name        VARCHAR(128) NOT NULL UNIQUE,
  ns_code         VARCHAR(24),
  category_code   VARCHAR(64),
  display_name    VARCHAR(128),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  description     TEXT,
  icon            VARCHAR(64),
  color           VARCHAR(20),
  status          SMALLINT     NOT NULL DEFAULT 1,
  metadata        TEXT,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_property (
  id              VARCHAR(50)  PRIMARY KEY,
  rid             VARCHAR(128),
  class_id        VARCHAR(42)  NOT NULL,
  api_name        VARCHAR(128) NOT NULL,
  data_type       VARCHAR(32),
  display_name    VARCHAR(128),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  is_primary      SMALLINT     NOT NULL DEFAULT 0,
  is_required     SMALLINT     NOT NULL DEFAULT 0,
  status          SMALLINT     NOT NULL DEFAULT 1,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_link (
  id              VARCHAR(50)  PRIMARY KEY,
  rid             VARCHAR(128),
  api_name        VARCHAR(128) NOT NULL UNIQUE,
  source_class_id VARCHAR(42)  NOT NULL,
  target_class_id VARCHAR(42)  NOT NULL,
  cardinality     VARCHAR(20)  DEFAULT 'many_to_many',
  display_name    VARCHAR(128),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  status          SMALLINT     NOT NULL DEFAULT 1,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_action (
  id              VARCHAR(50)  PRIMARY KEY,
  rid             VARCHAR(128),
  class_id        VARCHAR(42),
  api_name        VARCHAR(128) NOT NULL UNIQUE,
  action_kind     VARCHAR(32),
  display_name    VARCHAR(128),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  status          SMALLINT     NOT NULL DEFAULT 1,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface (
  id              VARCHAR(46)  PRIMARY KEY,
  rid             VARCHAR(128),
  api_name        VARCHAR(128) NOT NULL UNIQUE,
  interface_code  VARCHAR(128),
  ns_code         VARCHAR(24),
  category_code   VARCHAR(64),
  display_name    VARCHAR(128),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  rdfs_see_also   VARCHAR(512),
  rdfs_defined_by VARCHAR(255),
  description     TEXT,
  icon            VARCHAR(64),
  color           VARCHAR(20),
  status          SMALLINT     NOT NULL DEFAULT 1,
  metadata        TEXT,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface_property (
  id              VARCHAR(50)  PRIMARY KEY,
  rid             VARCHAR(128),
  interface_id    VARCHAR(46)  NOT NULL,
  api_name        VARCHAR(128) NOT NULL,
  prop_code       VARCHAR(128),
  data_type       VARCHAR(64),
  category_code   VARCHAR(64),
  display_name    VARCHAR(128),
  rdfs_label      VARCHAR(255),
  rdfs_comment    TEXT,
  rdfs_see_also   VARCHAR(512),
  rdfs_defined_by VARCHAR(255),
  is_required     SMALLINT     NOT NULL DEFAULT 0,
  metadata        TEXT,
  status          SMALLINT     NOT NULL DEFAULT 1,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface_class (
  id              VARCHAR(51)  PRIMARY KEY,
  interface_id    VARCHAR(46)  NOT NULL,
  class_id        VARCHAR(46)  NOT NULL,
  category_code   VARCHAR(64),
  status          SMALLINT     NOT NULL DEFAULT 1,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_data_source (
  id              VARCHAR(47)  PRIMARY KEY,
  category_code   VARCHAR(64),
  ds_code         VARCHAR(64)  NOT NULL,
  ds_name         VARCHAR(128) NOT NULL,
  ds_type         VARCHAR(32)  NOT NULL,
  jdbc_driver     VARCHAR(255),
  jdbc_url        VARCHAR(512),
  username        VARCHAR(128),
  password        VARCHAR(255),
  mongo_url       VARCHAR(512),
  status          SMALLINT     NOT NULL DEFAULT 1,
  remark          VARCHAR(512),
  ref_count       INTEGER      NOT NULL DEFAULT 0,
  connect_status  VARCHAR(20)  DEFAULT 'online',
  active_conn     INTEGER      DEFAULT 0,
  max_conn        INTEGER      DEFAULT 100,
  response_ms     INTEGER      DEFAULT 0,
  collection_cnt  INTEGER      DEFAULT 0,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ds_category ON sys_data_source(category_code);
CREATE INDEX IF NOT EXISTS idx_ds_type     ON sys_data_source(ds_type);
