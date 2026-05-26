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
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  description     TEXT,
  icon            TEXT,
  color           TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  metadata        TEXT,
  -- 类层级
  parent_class_id TEXT,
  -- OWL 复杂类表达式
  class_expr_type    TEXT,            -- NULL/0=普通; union/intersection/complement/enumeration
  class_expr_content TEXT,            -- JSON
  is_thing        INTEGER NOT NULL DEFAULT 0,
  is_nothing      INTEGER NOT NULL DEFAULT 0,
  is_common       INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_class_parent ON ont_class(parent_class_id);
CREATE INDEX IF NOT EXISTS idx_class_cat    ON ont_class(category_code);

-- 类属性（扩展版：含 OWL 特性 / XSD 约束 / 物理映射）
CREATE TABLE IF NOT EXISTS ont_class_property (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  api_name        TEXT NOT NULL,                 -- 类内唯一
  prop_code       TEXT,                          -- 属性编码 (camelCase)
  prop_type       TEXT DEFAULT 'data',           -- data / object / annotation
  data_type       TEXT,                          -- XSD 数据类型 (对象属性为空)
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  -- 物理映射
  class_ds_id     TEXT,                          -- 来源数据集 (main/s1/s2)
  physical_table  TEXT,
  physical_column TEXT,
  -- 主键 / 必填 / 派生
  is_primary      INTEGER NOT NULL DEFAULT 0,
  is_required     INTEGER NOT NULL DEFAULT 0,
  is_key          INTEGER NOT NULL DEFAULT 0,
  is_derived      INTEGER NOT NULL DEFAULT 0,
  is_multi_valued_prop      INTEGER NOT NULL DEFAULT 0,
  is_range_constraint_prop  INTEGER NOT NULL DEFAULT 0,
  -- 对象属性 / 子属性
  range_class_id  TEXT,                          -- 对象属性值域类 ID
  sub_property_of TEXT,                          -- 父属性 ID
  -- XSD 约束
  xsd_min_length  INTEGER,
  xsd_max_length  INTEGER,
  xsd_length      INTEGER,
  xsd_pattern     TEXT,
  xsd_min_inclusive TEXT,
  xsd_max_inclusive TEXT,
  -- OWL 特性
  owl_functional         INTEGER NOT NULL DEFAULT 0,
  owl_inverse_functional INTEGER NOT NULL DEFAULT 0,
  owl_transitive         INTEGER NOT NULL DEFAULT 0,
  owl_symmetric          INTEGER NOT NULL DEFAULT 0,
  owl_asymmetric         INTEGER NOT NULL DEFAULT 0,
  owl_reflexive          INTEGER NOT NULL DEFAULT 0,
  owl_irreflexive        INTEGER NOT NULL DEFAULT 0,
  metadata        TEXT,
  sort            INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_cprop_class ON ont_class_property(class_id);
CREATE INDEX IF NOT EXISTS idx_cprop_type  ON ont_class_property(prop_type);

-- 等价 / 不相交（ont_class_group）
CREATE TABLE IF NOT EXISTS ont_class_group (
  id              TEXT PRIMARY KEY,              -- class-group-<uuid>
  class_id        TEXT NOT NULL,                 -- 小 ID
  ref_class_id    TEXT NOT NULL,                 -- 大 ID
  group_type      TEXT NOT NULL,                 -- equivalent / disjoint
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE (group_type, class_id, ref_class_id)
);
CREATE INDEX IF NOT EXISTS idx_cgroup_cls ON ont_class_group(class_id);
CREATE INDEX IF NOT EXISTS idx_cgroup_ref ON ont_class_group(ref_class_id);

-- 互斥并集 (ont_class_disjoint_union)
CREATE TABLE IF NOT EXISTS ont_class_disjoint_union (
  id              TEXT PRIMARY KEY,              -- class-disjoint-union-<uuid>
  parent_class_id TEXT NOT NULL,
  sub_class_id    TEXT NOT NULL,
  status          INTEGER NOT NULL DEFAULT 1,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE (parent_class_id, sub_class_id)
);
CREATE INDEX IF NOT EXISTS idx_cdu_parent ON ont_class_disjoint_union(parent_class_id);

-- 等价属性
CREATE TABLE IF NOT EXISTS ont_property_equivalent (
  id              TEXT PRIMARY KEY,              -- prop-equivalent-<uuid>
  class_id1       TEXT NOT NULL,
  prop_id1        TEXT NOT NULL,
  class_id2       TEXT NOT NULL,
  prop_id2        TEXT NOT NULL,
  status          INTEGER NOT NULL DEFAULT 1,
  rdfs_comment    TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_peq_p1 ON ont_property_equivalent(prop_id1);
CREATE INDEX IF NOT EXISTS idx_peq_p2 ON ont_property_equivalent(prop_id2);

-- 互斥属性
CREATE TABLE IF NOT EXISTS ont_property_disjoint (
  id              TEXT PRIMARY KEY,              -- prop-disjoint-<uuid>
  class_id1       TEXT NOT NULL,
  prop_id1        TEXT NOT NULL,
  class_id2       TEXT NOT NULL,
  prop_id2        TEXT NOT NULL,
  status          INTEGER NOT NULL DEFAULT 1,
  rdfs_comment    TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_pdj_p1 ON ont_property_disjoint(prop_id1);
CREATE INDEX IF NOT EXISTS idx_pdj_p2 ON ont_property_disjoint(prop_id2);

-- 对象数据集关联（主表 + 补充数据集）
CREATE TABLE IF NOT EXISTS ont_class_ds (
  id              TEXT PRIMARY KEY,              -- class-ds-<uuid>
  class_id        TEXT NOT NULL,
  ds_code         TEXT,
  physical_table  TEXT,
  rel_type        INTEGER NOT NULL DEFAULT 1,    -- 1=主数据集 2=补充数据集
  alias           TEXT,                          -- main / s1 / s2 ...
  pk_keys         TEXT,                          -- 主表主键(逗号分隔)
  join_on_keys    TEXT,                          -- 补充表关联键(逗号分隔)
  join_type       TEXT DEFAULT 'LEFT',
  sort            INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_cds_class ON ont_class_ds(class_id);

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

-- =============================================================
-- 「我的图库」共享图标目录(最多 2 级)与上传图标
-- =============================================================
CREATE TABLE IF NOT EXISTS icon_lib_group (
  id          TEXT PRIMARY KEY,                              -- "ig-" + UUID
  parent_id   TEXT,                                          -- NULL 顶级；2 级时存父节点 id
  name        TEXT NOT NULL,
  sort        INTEGER NOT NULL DEFAULT 0,
  source      TEXT,                                          -- seed | manual
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_iglib_grp_parent ON icon_lib_group(parent_id);

CREATE TABLE IF NOT EXISTS icon_lib_icon (
  id          TEXT PRIMARY KEY,                              -- "ii-" + UUID
  group_id    TEXT NOT NULL,                                 -- 关联 icon_lib_group.id (叶子节点)
  name        TEXT NOT NULL,                                 -- 文件名(去 .svg)
  view_box    TEXT NOT NULL DEFAULT '0 0 1024 1024',
  content     TEXT NOT NULL,                                 -- 内部 SVG 片段(已替换为 currentColor)
  sort        INTEGER NOT NULL DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_iglib_ico_group ON icon_lib_icon(group_id);

-- =============================================================
-- 兼容旧数据库：以下 ALTER TABLE 在新建库时多余（首次启动时报"列已存在"），
-- 在已存在的旧库上才生效；初始化器把错误降级为日志，不会阻塞启动。
-- =============================================================
ALTER TABLE ont_class ADD COLUMN rdfs_see_also   TEXT;
ALTER TABLE ont_class ADD COLUMN rdfs_defined_by TEXT;
ALTER TABLE ont_class ADD COLUMN parent_class_id TEXT;
ALTER TABLE ont_class ADD COLUMN class_expr_type    TEXT;
ALTER TABLE ont_class ADD COLUMN class_expr_content TEXT;
ALTER TABLE ont_class ADD COLUMN is_thing   INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class ADD COLUMN is_nothing INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class ADD COLUMN is_common  INTEGER NOT NULL DEFAULT 0;

ALTER TABLE ont_class_property ADD COLUMN category_code   TEXT;
ALTER TABLE ont_class_property ADD COLUMN prop_code       TEXT;
ALTER TABLE ont_class_property ADD COLUMN prop_type       TEXT DEFAULT 'data';
ALTER TABLE ont_class_property ADD COLUMN rdfs_see_also   TEXT;
ALTER TABLE ont_class_property ADD COLUMN rdfs_defined_by TEXT;
ALTER TABLE ont_class_property ADD COLUMN class_ds_id     TEXT;
ALTER TABLE ont_class_property ADD COLUMN physical_table  TEXT;
ALTER TABLE ont_class_property ADD COLUMN physical_column TEXT;
ALTER TABLE ont_class_property ADD COLUMN is_key          INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN is_derived      INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN is_multi_valued_prop      INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN is_range_constraint_prop  INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN range_class_id  TEXT;
ALTER TABLE ont_class_property ADD COLUMN sub_property_of TEXT;
ALTER TABLE ont_class_property ADD COLUMN xsd_min_length  INTEGER;
ALTER TABLE ont_class_property ADD COLUMN xsd_max_length  INTEGER;
ALTER TABLE ont_class_property ADD COLUMN xsd_length      INTEGER;
ALTER TABLE ont_class_property ADD COLUMN xsd_pattern     TEXT;
ALTER TABLE ont_class_property ADD COLUMN xsd_min_inclusive TEXT;
ALTER TABLE ont_class_property ADD COLUMN xsd_max_inclusive TEXT;
ALTER TABLE ont_class_property ADD COLUMN owl_functional         INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN owl_inverse_functional INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN owl_transitive         INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN owl_symmetric          INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN owl_asymmetric         INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN owl_reflexive          INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN owl_irreflexive        INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ont_class_property ADD COLUMN metadata        TEXT;
ALTER TABLE ont_class_property ADD COLUMN sort            INTEGER NOT NULL DEFAULT 0;
