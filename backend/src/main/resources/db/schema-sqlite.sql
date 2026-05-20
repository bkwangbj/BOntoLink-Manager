-- =============================================================
-- BOntoLink 动态本体管理系统 — SQLite Schema
-- 与 PostgreSQL 字段保持一致；类型映射：
--   varchar(N) -> TEXT；datetime -> TEXT(ISO8601)；tinyint -> INTEGER
-- =============================================================

-- 行业/领域分类表
CREATE TABLE IF NOT EXISTS ont_biz_category (
  id              TEXT PRIMARY KEY,                 -- "category-" + UUID
  parent_id       TEXT NOT NULL DEFAULT '0',        -- 自关联上级；'0' 顶级
  rid             TEXT,                             -- ri.ont.biz.category.<uuid>
  category_code   TEXT NOT NULL UNIQUE,             -- 小写+下划线 全局唯一
  category_type   INTEGER NOT NULL DEFAULT 1,       -- 1=行业 2=领域 3=分组
  ns_code         TEXT,                             -- 绑定的命名空间编码
  status          INTEGER NOT NULL DEFAULT 1,       -- 0=禁用 1=启用
  sort            INTEGER NOT NULL DEFAULT 0,
  icon            TEXT,
  color           TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  description     TEXT,
  metadata        TEXT,                             -- JSON
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_biz_category_parent ON ont_biz_category(parent_id);
CREATE INDEX IF NOT EXISTS idx_biz_category_type   ON ont_biz_category(category_type);
CREATE INDEX IF NOT EXISTS idx_biz_category_ns     ON ont_biz_category(ns_code);

-- 命名空间表
CREATE TABLE IF NOT EXISTS ont_biz_namespace (
  id              TEXT PRIMARY KEY,                 -- "namespace-" + UUID
  ns_code         TEXT NOT NULL UNIQUE,             -- w_wtr / w_common 等
  ns_name         TEXT NOT NULL,                    -- 中文名
  ns_uri          TEXT,                             -- http://watf.com/ont/...#
  hierarchy_path  TEXT,                             -- watf.water.hydrology
  curr_version    TEXT NOT NULL DEFAULT '1.0',
  status          INTEGER NOT NULL DEFAULT 1,
  metadata        TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_biz_ns_code ON ont_biz_namespace(ns_code);

-- 命名空间版本表
CREATE TABLE IF NOT EXISTS ont_biz_namespace_version (
  id              TEXT PRIMARY KEY,                 -- "namespace-v-" + UUID
  ns_code         TEXT NOT NULL,
  version         TEXT NOT NULL,
  uri             TEXT NOT NULL,
  snapshot_data   TEXT,
  owl_content     TEXT,
  publish_time    TEXT,
  is_current      INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_biz_ns_v_code ON ont_biz_namespace_version(ns_code);

-- 分组表
CREATE TABLE IF NOT EXISTS ont_biz_group (
  id              TEXT PRIMARY KEY,                 -- "group-" + UUID
  parent_id       TEXT,                             -- 所属领域分类 ID
  category_code   TEXT,
  g_name          TEXT NOT NULL,
  g_sort          INTEGER NOT NULL DEFAULT 0,
  icon            TEXT,
  color           TEXT,
  description     TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 分组-对象关联表
CREATE TABLE IF NOT EXISTS ont_biz_group_class (
  id              TEXT PRIMARY KEY,
  group_id        TEXT NOT NULL,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  g_sort          INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_group_class_g ON ont_biz_group_class(group_id);

-- 本体业务类 / 接口 / 关系 / 动作（最小集，供其他模块占位）
CREATE TABLE IF NOT EXISTS ont_class (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  api_name        TEXT NOT NULL UNIQUE,
  ns_code         TEXT,
  category_code   TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  description     TEXT,
  icon            TEXT,
  color           TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  metadata        TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS ont_class_property (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  class_id        TEXT NOT NULL,
  api_name        TEXT NOT NULL,
  data_type       TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  is_primary      INTEGER NOT NULL DEFAULT 0,
  is_required     INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS ont_class_link (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  api_name        TEXT NOT NULL UNIQUE,
  source_class_id TEXT NOT NULL,
  target_class_id TEXT NOT NULL,
  cardinality     TEXT DEFAULT 'many_to_many',
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS ont_class_action (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  class_id        TEXT,
  api_name        TEXT NOT NULL UNIQUE,
  action_kind     TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS ont_interface (
  id              TEXT PRIMARY KEY,            -- "interface-" + UUID
  rid             TEXT,
  api_name        TEXT NOT NULL UNIQUE,        -- snake_case 全局唯一
  interface_code  TEXT,                        -- 接口编码（类内/领域内唯一）
  ns_code         TEXT,
  category_code   TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  description     TEXT,
  icon            TEXT,
  color           TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  metadata        TEXT,                        -- JSON 业务元数据/查询约束
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 接口属性
CREATE TABLE IF NOT EXISTS ont_interface_property (
  id              TEXT PRIMARY KEY,            -- "interface-pro-" + UUID
  rid             TEXT,
  interface_id    TEXT NOT NULL,
  api_name        TEXT NOT NULL,               -- snake_case
  prop_code       TEXT,                        -- 属性代码
  data_type       TEXT,                        -- XSD 数据类型
  category_code   TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  is_required     INTEGER NOT NULL DEFAULT 0,
  metadata        TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_iface_prop_iface ON ont_interface_property(interface_id);

-- 类实现接口关系
CREATE TABLE IF NOT EXISTS ont_interface_class (
  id              TEXT PRIMARY KEY,            -- "interface-class-" + UUID
  interface_id    TEXT NOT NULL,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_iface_cls_if ON ont_interface_class(interface_id);
CREATE INDEX IF NOT EXISTS idx_iface_cls_c  ON ont_interface_class(class_id);

-- 数据源（领域级虚拟数据源）
CREATE TABLE IF NOT EXISTS sys_data_source (
  id              TEXT PRIMARY KEY,            -- "datasource-" + UUID
  category_code   TEXT,                        -- 所属业务领域
  ds_code         TEXT NOT NULL,               -- 同领域内唯一
  ds_name         TEXT NOT NULL,               -- 中文名
  ds_type         TEXT NOT NULL,               -- mysql/postgresql/oracle/mongodb/dm/kingbase/...
  jdbc_driver     TEXT,                        -- JDBC 驱动类
  jdbc_url        TEXT,                        -- 连接 URL
  username        TEXT,
  password        TEXT,                        -- 加密密文
  mongo_url       TEXT,                        -- MongoDB 专属
  status          INTEGER NOT NULL DEFAULT 1,  -- 0 禁用 / 1 启用
  remark          TEXT,
  -- 监控相关字段（实际生产由 Druid/agent 推送，此处用于演示）
  ref_count       INTEGER NOT NULL DEFAULT 0,  -- 被本体类引用次数
  connect_status  TEXT DEFAULT 'online',       -- online / offline / risk
  active_conn     INTEGER DEFAULT 0,
  max_conn        INTEGER DEFAULT 100,
  response_ms     INTEGER DEFAULT 0,
  collection_cnt  INTEGER DEFAULT 0,           -- Mongo 集合数
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_ds_category ON sys_data_source(category_code);
CREATE INDEX IF NOT EXISTS idx_ds_type     ON sys_data_source(ds_type);
