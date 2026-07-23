-- SQLite V1: 完整 schema（从 bontolink.db 提取）

CREATE TABLE ont_biz_category (
  id              TEXT PRIMARY KEY,                 -- "category-" + UUID；type=3 分组用 "group-" 前缀,与 ont_biz_group 共用同一 id
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

CREATE TABLE ont_biz_namespace (
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

CREATE TABLE ont_biz_namespace_version (
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

CREATE TABLE ont_biz_group (
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
, domain_code TEXT);

CREATE TABLE ont_class (
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

CREATE TABLE ont_interface (
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

CREATE TABLE sys_data_source (
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

CREATE TABLE ont_dict_def (
    id            TEXT PRIMARY KEY,
    dict_code     TEXT NOT NULL UNIQUE,
    dict_name     TEXT NOT NULL,
    rdfs_comment  TEXT,
    status        INTEGER NOT NULL DEFAULT 1,
    sort_no       INTEGER NOT NULL DEFAULT 0,
    create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    update_time   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE icon_lib_group (
  id          TEXT PRIMARY KEY,                              -- "ig-" + UUID
  parent_id   TEXT,                                          -- NULL 顶级；2 级时存父节点 id
  name        TEXT NOT NULL,
  sort        INTEGER NOT NULL DEFAULT 0,
  source      TEXT,                                          -- seed | manual
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE icon_lib_icon (
  id          TEXT PRIMARY KEY,                              -- "ii-" + UUID
  group_id    TEXT NOT NULL,                                 -- 关联 icon_lib_group.id (叶子节点)
  name        TEXT NOT NULL,                                 -- 文件名(去 .svg)
  view_box    TEXT NOT NULL DEFAULT '0 0 1024 1024',
  content     TEXT NOT NULL,                                 -- 内部 SVG 片段(已替换为 currentColor)
  sort        INTEGER NOT NULL DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_value_types (
  id                       TEXT PRIMARY KEY,             -- "value-types-" + UUID
  rid                      TEXT NOT NULL UNIQUE,
  api_name                 TEXT NOT NULL UNIQUE,
  category_code            TEXT,
  base_type                TEXT NOT NULL,                -- String / Integer / Decimal / Boolean / DateTime
  constraint_type          TEXT NOT NULL,                -- RID / UUID / Length / Regex / Enum
  constraint_config        TEXT,                         -- JSON (非 Enum 类型使用)
  enum_id                  TEXT,                         -- 关联 ont_enum_types.id (仅 Enum)
  default_usage_config_id  TEXT,
  status                   INTEGER NOT NULL DEFAULT 1,
  rdfs_label               TEXT,
  rdfs_comment             TEXT,
  rdfs_see_also            TEXT,
  rdfs_defined_by          TEXT,
  create_time              TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time              TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_valuetypes_usage_config (
  id                 TEXT PRIMARY KEY,                   -- "vt-usage-config-" + UUID
  max_select_level   INTEGER NOT NULL DEFAULT 0,         -- 0 = 不限制
  allow_non_leaf     INTEGER NOT NULL DEFAULT 0,
  display_format     TEXT NOT NULL DEFAULT 'label',      -- label / code / code_label / full_label
  is_system_default  INTEGER NOT NULL DEFAULT 0,
  create_time        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time        TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_types (
  id            TEXT PRIMARY KEY,                       -- "enum-types-" + UUID
  rid           TEXT,
  api_name      TEXT NOT NULL UNIQUE,
  category_code TEXT,
  enum_type     TEXT NOT NULL DEFAULT 'general_single', -- general_single / general_multi / biz_single / biz_multi
  max_level     INTEGER NOT NULL DEFAULT 1,
  top_code      TEXT,
  status        TEXT NOT NULL DEFAULT 'active',
  rdfs_label    TEXT,
  rdfs_comment  TEXT,
  rdfs_see_also TEXT,
  rdfs_defined_by TEXT,
  create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_dic_type_class (
  id          TEXT PRIMARY KEY,                    -- "tcdic-" + UUID
  enum_name   TEXT NOT NULL,                       -- 枚举名(ont_dic_tc_*)
  code        TEXT NOT NULL,                       -- CD 编码
  name        TEXT NOT NULL,                       -- NM 中文说明
  sort_no     INTEGER NOT NULL DEFAULT 0,          -- ST 排序
  status      INTEGER NOT NULL DEFAULT 1,          -- STATUS 1启用/0禁用
  created_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(enum_name, code)
);

CREATE TABLE ont_type_class_category_dict (
  category_code           TEXT PRIMARY KEY,        -- 大类唯一英文编码 hubble/vertex/...
  icon                    TEXT,                    -- 图标名
  color                   TEXT,                    -- 颜色(#hex)
  category_name_cn        TEXT NOT NULL,           -- 大类中文名
  global_allow_apply_types TEXT NOT NULL DEFAULT '[]',  -- 全局可挂载载体白名单 JSON
  source_type             TEXT NOT NULL DEFAULT 'platform_built',
  sort_weight             INTEGER NOT NULL DEFAULT 999,
  description             TEXT,
  created_at              TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at              TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_struct_types (
  id              TEXT PRIMARY KEY,                       -- "struct-types-" + UUID
  struct_code     TEXT NOT NULL UNIQUE,                   -- 编码 (例: FullName / Address)
  category_code   TEXT,                                   -- 所属领域 (与 ont_biz_category.category_code 对齐)
  status          INTEGER NOT NULL DEFAULT 1,             -- 0 禁用 1 启用
  rdfs_label      TEXT,                                   -- 中文名 (例: 姓名 / 地址)
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_shared_properties (
  id                         TEXT PRIMARY KEY,                       -- "shared-properties-" + UUID
  rid                        TEXT,                                   -- 全局 RID
  category_code              TEXT,                                   -- 所属领域 ont_biz_category.category_code
  prop_code                  TEXT NOT NULL UNIQUE,                   -- 属性编码,英文小写+下划线
  prop_type                  TEXT NOT NULL DEFAULT 'data',           -- data / object / annotation / struct
  is_key                     INTEGER NOT NULL DEFAULT 0,             -- HasKey
  data_type                  TEXT,                                   -- XSD 类型: xsd:string 等
  value_type                 TEXT,                                   -- 关联值类型 id
  is_required                INTEGER NOT NULL DEFAULT 0,
  is_multi_valued_prop       INTEGER NOT NULL DEFAULT 0,
  is_range_constraint_prop   INTEGER NOT NULL DEFAULT 0,
  xsd_min_length             INTEGER,
  xsd_max_length             INTEGER,
  xsd_length                 INTEGER,
  xsd_pattern                TEXT,
  xsd_min_inclusive          TEXT,
  xsd_max_inclusive          TEXT,
  owl_functional             INTEGER NOT NULL DEFAULT 0,
  owl_inverse_functional     INTEGER NOT NULL DEFAULT 0,
  owl_transitive             INTEGER NOT NULL DEFAULT 0,
  owl_symmetric              INTEGER NOT NULL DEFAULT 0,
  owl_asymmetric             INTEGER NOT NULL DEFAULT 0,
  owl_reflexive              INTEGER NOT NULL DEFAULT 0,
  owl_irreflexive            INTEGER NOT NULL DEFAULT 0,
  status                     INTEGER NOT NULL DEFAULT 1,             -- 0 禁用 1 启用
  metadata                   TEXT,                                   -- JSON: 业务元数据公理
  rdfs_label                 TEXT,
  rdfs_comment               TEXT,
  rdfs_see_also              TEXT,
  rdfs_defined_by            TEXT,
  create_time                TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time                TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_property_format (
  format_id        TEXT PRIMARY KEY,                   -- "property-format-" + UUID
  src_type         INTEGER NOT NULL DEFAULT 1,         -- 1=属性, 2=共享属性
  property_id      TEXT NOT NULL UNIQUE,               -- 关联属性 ID (非外键, 由 src_type 决定具体来源表)
  property_scope   TEXT NOT NULL DEFAULT 'class',      -- class | interface (src_type=1 时区分类属性 / 接口属性)
  format_enabled   INTEGER NOT NULL DEFAULT 0,
  format_type      TEXT NOT NULL DEFAULT 'general',    -- general/number/currency/accounting/date/time/percent/fraction/scientific/text/special/custom
  decimal_places   INTEGER DEFAULT 2,
  use_thousand_sep INTEGER DEFAULT 0,
  negative_mode    INTEGER DEFAULT 3,                  -- 0=红括号 1=黑括号 2=红无符号 3=黑负号 4=红负号
  currency_symbol  TEXT DEFAULT '¥',
  accounting_align INTEGER DEFAULT 1,
  date_pattern     TEXT DEFAULT 'yyyy-MM-dd',
  time_pattern     TEXT DEFAULT 'HH:mm:ss',
  locale           TEXT DEFAULT 'zh-CN',
  fraction_type    TEXT DEFAULT '# ?/?',
  special_type     TEXT DEFAULT 'zipcode',             -- zipcode/lowerChinese/upperChinese/rmbUpper/wanUnit/plusMinus
  custom_format    TEXT DEFAULT 'G/通用格式',
  text_force       INTEGER DEFAULT 0,
  text_max_length  INTEGER,
  text_regex       TEXT,
  percent_auto_multiply INTEGER DEFAULT 1,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  create_user      TEXT
);

CREATE TABLE ont_enum_level_code_rule (
  id             TEXT PRIMARY KEY,                     -- "enum_level_code_rule-" + UUID
  enum_id        TEXT NOT NULL,
  code_name      TEXT NOT NULL,
  rule_level     INTEGER NOT NULL,
  code_separator TEXT DEFAULT '',
  code_len       INTEGER NOT NULL,
  total_len      INTEGER NOT NULL,
  fill_char      TEXT DEFAULT '0',
  fill_pos       INTEGER DEFAULT 0,                    -- 0 = 前补 / 1 = 后补
  create_time    TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_sync_config (
  id             TEXT PRIMARY KEY,                       -- "enum-sync-config-" + UUID
  enum_id        TEXT NOT NULL UNIQUE,                   -- 关联 ont_enum_types.id
  data_source_id TEXT,                                   -- 数据源 (MySQL / Oracle / HTTP 等)
  table_alias    TEXT,                                   -- 业务自定义备注名称
  table_name     TEXT,                                   -- 数据表真实名称
  field_code     TEXT,                                   -- 编码字段
  field_name     TEXT,                                   -- 名称字段
  field_sort     TEXT,                                   -- 排序字段
  field_status   TEXT,                                   -- 状态字段
  filter_sql     TEXT,                                   -- 顶级筛选 SQL 表达式
  sync_mode      TEXT DEFAULT 'level_diff',              -- level_diff / full_overwrite / incremental
  sync_strategy  TEXT DEFAULT 'once',                    -- once / daily / weekly / monthly
  create_time    TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
, field_parent TEXT, sync_source_type TEXT NOT NULL DEFAULT 'table', custom_sql TEXT);

CREATE TABLE ont_enum_sync_log (
  id           TEXT PRIMARY KEY,                         -- "enum-sync-log-" + UUID
  enum_id      TEXT NOT NULL,                            -- 关联 ont_enum_types.id
  sync_type    TEXT NOT NULL DEFAULT 'manual',           -- manual / auto
  add_count    INTEGER NOT NULL DEFAULT 0,
  update_count INTEGER NOT NULL DEFAULT 0,
  del_count    INTEGER NOT NULL DEFAULT 0,
  fail_count   INTEGER NOT NULL DEFAULT 0,
  sync_status  TEXT NOT NULL DEFAULT 'running',          -- running / success / failed
  error_msg    TEXT,
  oper_user    TEXT,
  sync_time    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_items (
  id           TEXT PRIMARY KEY,                        -- "enum-item-" + UUID
  enum_id      TEXT NOT NULL,                          -- 关联 ont_enum_types.id
  code         TEXT NOT NULL,                          -- 完整编码 (例: 110101)
  api_name     TEXT,                                   -- 英文 (例: dongcheng)
  label        TEXT NOT NULL,                          -- 中文名
  parent_code  TEXT,                                   -- 父级编码 (建树用)
  level        INTEGER NOT NULL DEFAULT 1,
  sort_num     INTEGER NOT NULL DEFAULT 0,
  status       TEXT NOT NULL DEFAULT 'active',
  create_time  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
, is_sync_locked INTEGER NOT NULL DEFAULT 0);

CREATE TABLE ont_biz_group_class (
  id              TEXT PRIMARY KEY,
  group_id        TEXT NOT NULL,
  ref_id          TEXT NOT NULL,                 -- 关联资源 ID (类型由 group_type 决定)
  group_type      TEXT NOT NULL DEFAULT 'object_types',
  category_code   TEXT,
  g_sort          INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_class_property (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  api_name        TEXT NOT NULL,                 -- 类内唯一
  prop_code       TEXT,                          -- 属性编码 (camelCase)
  prop_type       TEXT DEFAULT 'data',           -- data / object / annotation / struct
  data_type       TEXT,                          -- XSD 数据类型 (对象属性为空)
  value_type      TEXT,                          -- 值类型 RID (基于 data_type 派生)
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

CREATE TABLE ont_class_link (
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

CREATE TABLE ont_class_action (
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

CREATE TABLE ont_interface_property (
  id              TEXT PRIMARY KEY,            -- "interface-pro-" + UUID
  rid             TEXT,
  interface_id    TEXT NOT NULL,
  api_name        TEXT NOT NULL,               -- snake_case
  prop_code       TEXT,                        -- 属性代码
  data_type       TEXT,                        -- XSD 数据类型
  value_type      TEXT,                        -- 值类型 RID (基于 data_type 派生可选值)
  category_code   TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  -- 配置状态: 0=Required(NOT NULL 约束) / 1=Optional(允许 NULL) / 2=Multiple(单独关系表存多值)
  is_required     INTEGER NOT NULL DEFAULT 0,
  metadata        TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_interface_class (
  id              TEXT PRIMARY KEY,            -- "interface-class-" + UUID
  interface_id    TEXT NOT NULL,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_class_group (
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

CREATE TABLE ont_class_disjoint_union (
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

CREATE TABLE ont_property_equivalent (
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

CREATE TABLE ont_property_disjoint (
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

CREATE TABLE ont_class_ds (
  id              TEXT PRIMARY KEY,              -- class-ds-<uuid>
  class_id        TEXT NOT NULL,
  ds_code         TEXT,
  physical_table  TEXT,
  table_label     TEXT,                          -- 表中文别名 (例: 测站主表)
  rel_type        INTEGER NOT NULL DEFAULT 1,    -- 1=主数据集 2=补充数据集
  alias           TEXT,                          -- main / s1 / s2 ...
  pk_keys         TEXT,                          -- 主表主键(逗号分隔)
  join_on_keys    TEXT,                          -- 补充表关联键(逗号分隔)
  join_type       TEXT DEFAULT 'LEFT',
  physical_fields TEXT,                          -- JSON 数组: [{name,data_type,is_pk,is_fk}], 包含未映射字段
  sort            INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_struct_items (
  id         TEXT PRIMARY KEY,                            -- "struct-items-" + UUID
  struct_id  TEXT NOT NULL,                               -- 关联 ont_struct_types.id
  sort_no    INTEGER NOT NULL DEFAULT 0,                  -- 序号
  prop_id    TEXT NOT NULL                                -- 关联 ont_shared_properties.id
);

CREATE TABLE ont_link_types (
  id                  TEXT PRIMARY KEY,                       -- "link-types-" + UUID
  link_type_id        TEXT NOT NULL UNIQUE,                   -- 业务唯一标识 (如 aircraft-flight-operate)
  rid                 TEXT,                                   -- 系统资源 ID, 自动生成
  status              TEXT NOT NULL DEFAULT 'experimental',   -- experimental / active / deprecated
  l_object_type_id    TEXT NOT NULL,                          -- 源端对象类型 (ont_class.id)
  r_object_type_id    TEXT NOT NULL,                          -- 目标端对象类型 (ont_class.id)
  l_cardinality       TEXT NOT NULL DEFAULT 'one',            -- one / many
  r_cardinality       TEXT NOT NULL DEFAULT 'one',            -- one / many
  l_display_name      TEXT,                                   -- 源端显示名 (如"执飞航班")
  l_plural_name       TEXT,                                   -- 源端复数名 (基数=many 时必填)
  r_display_name      TEXT,                                   -- 目标端显示名 (如"执飞机型")
  r_plural_name       TEXT,                                   -- 目标端复数名 (基数=many 时必填)
  l_visibility        TEXT NOT NULL DEFAULT 'normal',         -- normal / prominent / hidden
  r_visibility        TEXT NOT NULL DEFAULT 'normal',
  l_api_name          TEXT,                                   -- 源端 API 名 (camelCase, 如 operatedFlights)
  r_api_name          TEXT,                                   -- 目标端 API 名 (camelCase)
  l_enabled           INTEGER NOT NULL DEFAULT 1,             -- BOOLEAN: 1=启用 / 0=禁用
  r_enabled           INTEGER NOT NULL DEFAULT 1,
  is_data_source_rel  INTEGER NOT NULL DEFAULT 0,             -- 0=常规字段关联 / 1=物理中间表关联
  rel_data_table      TEXT,                                   -- 关联物理表名 (is_data_source_rel=1 时必填)
  rdfs_label          TEXT,                                   -- 通用名称 (列表显示)
  rdfs_comment        TEXT,                                   -- 备注
  category_code       TEXT,                                   -- 所属领域 (与 ont_biz_category.category_code 对齐)
  created_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  created_by          TEXT,
  updated_by          TEXT
);

CREATE TABLE ont_link_mappings (
  mapping_id        TEXT PRIMARY KEY,                         -- "link-mappings-" + UUID
  link_id           TEXT NOT NULL,                            -- 关联 ont_link_types.id
  side              TEXT NOT NULL,                            -- left / right
  seq               INTEGER NOT NULL DEFAULT 1,               -- 复合字段顺序号 (从 1 起)
  object_field      TEXT NOT NULL,                            -- 对象属性字段名
  join_table_column TEXT,                                     -- 中间表列名 (仅 is_data_source_rel=1 时填)
  created_at        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at        TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_type_class (
  id                  TEXT PRIMARY KEY,            -- "type-class-" + UUID
  category_code       TEXT NOT NULL,               -- 关联大类字典
  icon                TEXT,
  color               TEXT,
  name_prefix         TEXT NOT NULL,               -- 固定前缀(普通=完整名,模板=公共前缀)
  name_template       TEXT,                        -- 模板表达式 如 event_intent.<intent_>
  name_cn_base        TEXT NOT NULL,               -- 基础中文名
  source_type         TEXT NOT NULL DEFAULT 'platform_built',
  group_tag           TEXT,                        -- 业务分组标签
  allow_apply_types   TEXT NOT NULL DEFAULT '[]',  -- 允许挂载载体白名单 JSON(不超大类范围)
  allow_multi_bind    INTEGER NOT NULL DEFAULT 0,  -- 同一载体可否重复绑定
  is_array_value      INTEGER NOT NULL DEFAULT 0,  -- 参数是否多值数组
  system_protected    INTEGER NOT NULL DEFAULT 0,  -- 系统保护(禁删/禁改核心)
  param_type          TEXT NOT NULL DEFAULT 'text',     -- boolean/rid/enum/text/numeric/json
  frontend_component  TEXT NOT NULL DEFAULT 'text_input',
  param_options_json  TEXT,                        -- 结构化枚举配置 JSON
  param_validator_json TEXT,                       -- 校验规则 JSON
  param_desc          TEXT,                        -- 参数填写说明
  demo_value          TEXT,                        -- 示例值
  depend_on_meta_ids  TEXT NOT NULL DEFAULT '[]',  -- 前置依赖类型类 ID JSON
  description         TEXT,                        -- 完整业务说明
  replacement_meta_id TEXT,                        -- 弃用后替代类型类 ID
  is_deprecated       INTEGER NOT NULL DEFAULT 0,  -- 全局弃用
  deprecated_reason   TEXT,
  support_version_min TEXT,                        -- 支持的最低版本(如 7.20)
  current_version_no  INTEGER NOT NULL DEFAULT 1,
  sort_weight         INTEGER NOT NULL DEFAULT 999,
  create_user         TEXT,
  update_user         TEXT,
  created_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')), param_json TEXT,
  UNIQUE(category_code, name_prefix)
);

CREATE TABLE ont_type_class_bind (
  id                  TEXT PRIMARY KEY,            -- "bind-" + UUID
  env                 TEXT NOT NULL DEFAULT 'prod',
  type_class_meta_id  TEXT NOT NULL,               -- 关联 ont_type_class.id
  applicable_type     TEXT NOT NULL,               -- property/relation/action
  property_owner_type TEXT,                        -- object/interface(仅属性)
  property_owner_id   TEXT,                        -- 对象/接口 RID(仅属性)
  property_id         TEXT,                        -- 具体属性标识(仅属性)
  link_type_id        TEXT,                        -- 关系 RID(仅关系)
  action_type_id      TEXT,                        -- 操作 RID(仅操作)
  suffix_custom       TEXT,                        -- 模板类型类自定义后缀
  value               TEXT,                        -- 绑定配置参数(多值 JSON)
  bind_deprecated     INTEGER NOT NULL DEFAULT 0,  -- 单条绑定临时弃用
  remark              TEXT,
  create_user         TEXT,
  update_user         TEXT,
  created_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at          TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_dict_item (
    id            TEXT PRIMARY KEY,
    dict_id       TEXT NOT NULL REFERENCES ont_dict_def(id) ON DELETE CASCADE,
    parent_id     TEXT REFERENCES ont_dict_item(id),
    item_code     TEXT NOT NULL,
    item_value    TEXT NOT NULL,
    sort_no       INTEGER NOT NULL DEFAULT 0,
    status        INTEGER NOT NULL DEFAULT 1,
    color         TEXT,
    ext_data      TEXT,
    create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    update_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    UNIQUE(dict_id, item_code)
);

CREATE TABLE ont_explore_design (
  id           TEXT PRIMARY KEY,                 -- "design-" + UUID
  class_id     TEXT NOT NULL,                    -- 关联对象类型 ont_class.id
  name         TEXT NOT NULL DEFAULT '',         -- 空=默认看板, 非空=命名设计
  kind         TEXT NOT NULL DEFAULT 'query',    -- query / list
  config       TEXT,                             -- 整个设计对象 JSON(含 charts/kw/pills/sort/viewMode 等)
  created_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(class_id, name)
);

CREATE TABLE ont_physical_table (
  id              TEXT PRIMARY KEY,                 -- "phys-" + UUID
  ds_id           TEXT NOT NULL,                    -- 关联 sys_data_source.id
  physical_table  TEXT NOT NULL,                    -- 物理表/视图名
  display_name    TEXT,                             -- 中文名(用户可改, 同步时不覆盖)
  table_type      TEXT NOT NULL DEFAULT 'table',    -- table / view
  columns_json    TEXT,                             -- 字段清单 JSON: [{name,type}]
  column_count    INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  sync_time       TEXT,                             -- 最近一次同步时间
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(ds_id, physical_table)
);

-- V9: type class param_json
ALTER TABLE ont_type_class ADD COLUMN IF NOT EXISTS param_json TEXT;
