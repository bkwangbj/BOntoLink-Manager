-- =====================================================================
-- PostgreSQL 合并 DDL
-- 来源: postgresql/V1 + V21~V32 所有结构变更
-- 规则: CREATE TABLE 使用最终版本; ALTER 变更已内联; 约束直接写正确值
-- =====================================================================

-- ----------------------------------------------------------------
-- 1. 行业分类 / 命名空间
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_biz_category (
  id               VARCHAR(64) PRIMARY KEY,
  parent_id        VARCHAR(64) NOT NULL DEFAULT '0',
  rid              TEXT,
  category_code    VARCHAR(64) NOT NULL,
  category_type    INTEGER NOT NULL DEFAULT 1,
  ns_code          VARCHAR(64),
  status           SMALLINT NOT NULL DEFAULT 1,
  sort             INTEGER NOT NULL DEFAULT 0,
  icon             VARCHAR(255),
  color            VARCHAR(255),
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  description      VARCHAR(255),
  metadata         TEXT,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_namespace (
  id               VARCHAR(64) PRIMARY KEY,
  ns_code          VARCHAR(64) NOT NULL,
  ns_name          VARCHAR(128) NOT NULL,
  ns_uri           VARCHAR(255),
  hierarchy_path   TEXT,
  curr_version     VARCHAR(255) NOT NULL DEFAULT '1.0',
  status           SMALLINT NOT NULL DEFAULT 1,
  metadata         TEXT,
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_namespace_version (
  id               VARCHAR(64) PRIMARY KEY,
  ns_code          VARCHAR(64) NOT NULL,
  version          VARCHAR(20) NOT NULL,
  uri              VARCHAR(255) NOT NULL,
  snapshot_data    TEXT,
  owl_content      TEXT,
  publish_time     TIMESTAMP,
  is_current       SMALLINT NOT NULL DEFAULT 0,
  status           SMALLINT NOT NULL DEFAULT 1,
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_group (
  id               VARCHAR(64) PRIMARY KEY,
  parent_id        VARCHAR(64),
  category_code    VARCHAR(64),
  g_name           VARCHAR(128) NOT NULL,
  g_sort           INTEGER NOT NULL DEFAULT 0,
  icon             VARCHAR(255),
  color            VARCHAR(255),
  description      VARCHAR(255),
  domain_code      VARCHAR(64),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 2. 核心本体: 类 / 接口
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_class (
  id               VARCHAR(64) PRIMARY KEY,
  rid              TEXT,
  api_name         VARCHAR(128) NOT NULL,
  ns_code          VARCHAR(64),
  category_code    VARCHAR(64),
  display_name     VARCHAR(128),
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  description      VARCHAR(255),
  icon             VARCHAR(255),
  color            VARCHAR(255),
  status           SMALLINT NOT NULL DEFAULT 1,
  metadata         TEXT,
  parent_class_id  VARCHAR(64),
  class_expr_type  VARCHAR(64),
  class_expr_content TEXT,
  is_thing         SMALLINT NOT NULL DEFAULT 0,
  is_nothing       SMALLINT NOT NULL DEFAULT 0,
  is_common        SMALLINT NOT NULL DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface (
  id               VARCHAR(64) PRIMARY KEY,
  rid              TEXT,
  api_name         VARCHAR(128) NOT NULL,
  interface_code   VARCHAR(64),
  ns_code          VARCHAR(64),
  category_code    VARCHAR(64),
  display_name     VARCHAR(128),
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  description      VARCHAR(255),
  icon             VARCHAR(255),
  color            VARCHAR(255),
  status           SMALLINT NOT NULL DEFAULT 1,
  metadata         TEXT,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 3. 数据源
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_data_source (
  id               VARCHAR(64) PRIMARY KEY,
  category_code    VARCHAR(64),
  ds_code          VARCHAR(64) NOT NULL,
  ds_name          VARCHAR(128) NOT NULL,
  ds_type          VARCHAR(64) NOT NULL,
  jdbc_driver      TEXT,
  jdbc_url         TEXT,
  username         TEXT,
  password         TEXT,
  mongo_url        TEXT,
  status           SMALLINT NOT NULL DEFAULT 1,
  remark           VARCHAR(255),
  ref_count        INTEGER NOT NULL DEFAULT 0,
  connect_status   TEXT DEFAULT 'online',
  active_conn      SMALLINT DEFAULT 0,
  max_conn         TEXT DEFAULT '100',
  response_ms      SMALLINT DEFAULT 0,
  collection_cnt   SMALLINT DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 4. 字典
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_dict_def (
  id               VARCHAR(64) PRIMARY KEY,
  dict_code        VARCHAR(64) NOT NULL,
  dict_name        VARCHAR(255) NOT NULL,
  rdfs_comment     VARCHAR(255),
  status           SMALLINT NOT NULL DEFAULT 1,
  sort_no          INTEGER NOT NULL DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_dict_item (
  id               VARCHAR(64) PRIMARY KEY,
  dict_id          VARCHAR(64) NOT NULL,
  parent_id        VARCHAR(64),
  item_code        VARCHAR(64) NOT NULL,
  item_value       VARCHAR(255) NOT NULL,
  sort_no          INTEGER NOT NULL DEFAULT 0,
  status           SMALLINT NOT NULL DEFAULT 1,
  color            VARCHAR(255),
  ext_data         TEXT,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 5. 图标库
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS icon_lib_group (
  id               VARCHAR(64) PRIMARY KEY,
  parent_id        VARCHAR(64),
  name             VARCHAR(128) NOT NULL,
  sort             INTEGER NOT NULL DEFAULT 0,
  source           TEXT,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS icon_lib_icon (
  id               VARCHAR(64) PRIMARY KEY,
  group_id         VARCHAR(64) NOT NULL,
  name             VARCHAR(128) NOT NULL,
  view_box         VARCHAR(255) NOT NULL DEFAULT '0 0 1024 1024',
  content          TEXT NOT NULL,
  sort             INTEGER NOT NULL DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 6. 值类型 / 枚举
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_valuetypes_usage_config (
  id                  VARCHAR(64) PRIMARY KEY,
  max_select_level    INTEGER NOT NULL DEFAULT 0,
  allow_non_leaf      SMALLINT NOT NULL DEFAULT 0,
  display_format      TEXT NOT NULL DEFAULT 'label',
  is_system_default   SMALLINT NOT NULL DEFAULT 0,
  create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_value_types (
  id                      VARCHAR(64) PRIMARY KEY,
  rid                     TEXT NOT NULL,
  api_name                VARCHAR(128) NOT NULL,
  category_code           VARCHAR(64),
  base_type               VARCHAR(64) NOT NULL,
  constraint_type         VARCHAR(64) NOT NULL,
  constraint_config       TEXT,
  enum_id                 VARCHAR(64),
  default_usage_config_id VARCHAR(64),
  status                  SMALLINT NOT NULL DEFAULT 1,
  rdfs_label              VARCHAR(255),
  rdfs_comment            VARCHAR(255),
  rdfs_see_also           VARCHAR(255),
  rdfs_defined_by         VARCHAR(255),
  create_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_types (
  id               VARCHAR(64) PRIMARY KEY,
  rid              TEXT,
  api_name         VARCHAR(128) NOT NULL,
  category_code    VARCHAR(64),
  enum_type        VARCHAR(64) NOT NULL DEFAULT 'general_single',
  max_level        INTEGER NOT NULL DEFAULT 1,
  top_code         VARCHAR(64),
  status           TEXT NOT NULL DEFAULT 'active',
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_items (
  id               VARCHAR(64) PRIMARY KEY,
  enum_id          VARCHAR(64) NOT NULL,
  code             VARCHAR(64) NOT NULL,
  api_name         VARCHAR(128),
  label            VARCHAR(255) NOT NULL,
  parent_code      VARCHAR(64),
  level            INTEGER NOT NULL DEFAULT 1,
  sort_num         INTEGER NOT NULL DEFAULT 0,
  status           TEXT NOT NULL DEFAULT 'active',
  is_sync_locked   SMALLINT NOT NULL DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_level_code_rule (
  id               VARCHAR(64) PRIMARY KEY,
  enum_id          VARCHAR(64) NOT NULL,
  code_name        TEXT NOT NULL,
  rule_level       INTEGER NOT NULL,
  code_separator   TEXT,
  code_len         INTEGER NOT NULL,
  total_len        INTEGER NOT NULL,
  fill_char        SMALLINT DEFAULT 0,
  fill_pos         SMALLINT DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_sync_config (
  id               VARCHAR(64) PRIMARY KEY,
  enum_id          VARCHAR(64) NOT NULL,
  data_source_id   VARCHAR(64),
  table_alias      TEXT,
  table_name       TEXT,
  field_code       VARCHAR(64),
  field_name       TEXT,
  field_sort       TEXT,
  field_status     TEXT,
  field_parent     TEXT,
  filter_sql       TEXT,
  sync_mode        TEXT DEFAULT 'level_diff',
  sync_strategy    TEXT DEFAULT 'once',
  sync_source_type VARCHAR(64) NOT NULL DEFAULT 'table',
  custom_sql       TEXT,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_sync_log (
  id               VARCHAR(64) PRIMARY KEY,
  enum_id          VARCHAR(64) NOT NULL,
  sync_type        VARCHAR(64) NOT NULL DEFAULT 'manual',
  add_count        INTEGER NOT NULL DEFAULT 0,
  update_count     INTEGER NOT NULL DEFAULT 0,
  del_count        INTEGER NOT NULL DEFAULT 0,
  fail_count       INTEGER NOT NULL DEFAULT 0,
  sync_status      VARCHAR(64) NOT NULL DEFAULT 'running',
  error_msg        TEXT,
  oper_user        TEXT,
  sync_time        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 7. 共享属性 / 结构类型
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_struct_types (
  id               VARCHAR(64) PRIMARY KEY,
  struct_code      VARCHAR(64) NOT NULL,
  category_code    VARCHAR(64),
  status           SMALLINT NOT NULL DEFAULT 1,
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_struct_items (
  id         VARCHAR(64) PRIMARY KEY,
  struct_id  VARCHAR(64) NOT NULL,
  sort_no    INTEGER NOT NULL DEFAULT 0,
  prop_id    VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS ont_shared_properties (
  id                       VARCHAR(64) PRIMARY KEY,
  rid                      TEXT,
  category_code            VARCHAR(64),
  prop_code                VARCHAR(64) NOT NULL,
  prop_type                VARCHAR(64) NOT NULL DEFAULT 'data',
  is_key                   SMALLINT NOT NULL DEFAULT 0,
  data_type                VARCHAR(64),
  value_type               VARCHAR(255),
  is_required              SMALLINT NOT NULL DEFAULT 0,
  is_multi_valued_prop     SMALLINT NOT NULL DEFAULT 0,
  is_range_constraint_prop SMALLINT NOT NULL DEFAULT 0,
  xsd_min_length           TEXT,
  xsd_max_length           TEXT,
  xsd_length               TEXT,
  xsd_pattern              TEXT,
  xsd_min_inclusive        TEXT,
  xsd_max_inclusive        TEXT,
  owl_functional           SMALLINT NOT NULL DEFAULT 0,
  owl_inverse_functional   SMALLINT NOT NULL DEFAULT 0,
  owl_transitive           SMALLINT NOT NULL DEFAULT 0,
  owl_symmetric            SMALLINT NOT NULL DEFAULT 0,
  owl_asymmetric           SMALLINT NOT NULL DEFAULT 0,
  owl_reflexive            SMALLINT NOT NULL DEFAULT 0,
  owl_irreflexive          SMALLINT NOT NULL DEFAULT 0,
  status                   SMALLINT NOT NULL DEFAULT 1,
  metadata                 TEXT,
  rdfs_label               VARCHAR(255),
  rdfs_comment             VARCHAR(255),
  rdfs_see_also            VARCHAR(255),
  rdfs_defined_by          VARCHAR(255),
  create_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 8. 属性格式化
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_property_format (
  format_id              VARCHAR(64) PRIMARY KEY,
  src_type               VARCHAR(64) NOT NULL DEFAULT '1',
  property_id            VARCHAR(64) NOT NULL,
  property_scope         VARCHAR(64) NOT NULL DEFAULT 'class',
  format_enabled         SMALLINT NOT NULL DEFAULT 0,
  format_type            VARCHAR(64) NOT NULL DEFAULT 'general',
  decimal_places         TEXT DEFAULT '2',
  use_thousand_sep       SMALLINT DEFAULT 0,
  negative_mode          TEXT DEFAULT '3',
  currency_symbol        TEXT DEFAULT '¥',
  accounting_align       SMALLINT DEFAULT 1,
  date_pattern           TEXT DEFAULT 'yyyy-MM-dd',
  time_pattern           TEXT DEFAULT 'HH:mm:ss',
  locale                 TEXT DEFAULT 'zh-CN',
  fraction_type          VARCHAR(64) DEFAULT '# ?/?',
  special_type           VARCHAR(64) DEFAULT 'zipcode',
  custom_format          TEXT DEFAULT 'G/通用格式',
  text_force             SMALLINT DEFAULT 0,
  text_max_length        TEXT,
  text_regex             TEXT,
  percent_auto_multiply  SMALLINT DEFAULT 1,
  create_user            VARCHAR(255),
  create_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 9. 类型类
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_dic_type_class (
  id          VARCHAR(64) PRIMARY KEY,
  enum_name   VARCHAR(64) NOT NULL,
  code        VARCHAR(64) NOT NULL,
  name        VARCHAR(128) NOT NULL,
  sort_no     INTEGER NOT NULL DEFAULT 0,
  status      SMALLINT NOT NULL DEFAULT 1,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_type_class_category_dict (
  category_code            VARCHAR(64) PRIMARY KEY,
  icon                     VARCHAR(255),
  color                    VARCHAR(255),
  category_name_cn         VARCHAR(128) NOT NULL,
  global_allow_apply_types TEXT NOT NULL DEFAULT '[]',
  source_type              VARCHAR(64) NOT NULL DEFAULT 'platform_built',
  sort_weight              INTEGER NOT NULL DEFAULT 999,
  description              VARCHAR(255),
  created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_type_class (
  id                   VARCHAR(64) PRIMARY KEY,
  category_code        VARCHAR(64) NOT NULL,
  icon                 VARCHAR(255),
  color                VARCHAR(255),
  name_prefix          VARCHAR(64) NOT NULL,
  name_template        TEXT,
  name_cn_base         VARCHAR(128) NOT NULL,
  source_type          VARCHAR(64) NOT NULL DEFAULT 'platform_built',
  group_tag            TEXT,
  allow_apply_types    TEXT NOT NULL DEFAULT '[]',
  allow_multi_bind     SMALLINT NOT NULL DEFAULT 0,
  is_array_value       SMALLINT NOT NULL DEFAULT 0,
  system_protected     SMALLINT NOT NULL DEFAULT 0,
  param_type           VARCHAR(64) NOT NULL DEFAULT 'text',
  frontend_component   VARCHAR(64) NOT NULL DEFAULT 'text_input',
  param_options_json   TEXT,
  param_validator_json TEXT,
  param_desc           VARCHAR(255),
  demo_value           TEXT,
  depend_on_meta_ids   TEXT NOT NULL DEFAULT '[]',
  description          VARCHAR(255),
  replacement_meta_id  VARCHAR(64),
  is_deprecated        SMALLINT NOT NULL DEFAULT 0,
  deprecated_reason    VARCHAR(255),
  support_version_min  VARCHAR(255),
  current_version_no   INTEGER NOT NULL DEFAULT 1,
  sort_weight          INTEGER NOT NULL DEFAULT 999,
  param_json           TEXT,
  create_user          VARCHAR(255),
  update_user          VARCHAR(255),
  created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_type_class_bind (
  id                  VARCHAR(64) PRIMARY KEY,
  env                 VARCHAR(64) NOT NULL DEFAULT 'prod',
  type_class_meta_id  VARCHAR(64) NOT NULL,
  applicable_type     VARCHAR(64) NOT NULL,
  property_owner_type VARCHAR(64),
  property_owner_id   VARCHAR(64),
  property_id         VARCHAR(64),
  link_type_id        VARCHAR(64),
  action_type_id      VARCHAR(64),
  suffix_custom       TEXT,
  value               TEXT,
  bind_deprecated     SMALLINT NOT NULL DEFAULT 0,
  remark              VARCHAR(255),
  create_user         VARCHAR(255),
  update_user         VARCHAR(255),
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 10. 类属性 / 类链接 / 类分组 / 类数据集
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_class_property (
  id                       VARCHAR(64) PRIMARY KEY,
  rid                      TEXT,
  class_id                 VARCHAR(64) NOT NULL,
  category_code            VARCHAR(64),
  api_name                 VARCHAR(128) NOT NULL,
  prop_code                VARCHAR(64),
  prop_type                VARCHAR(64) DEFAULT 'data',
  data_type                VARCHAR(64),
  value_type               VARCHAR(64),
  display_name             VARCHAR(128),
  rdfs_label               VARCHAR(255),
  rdfs_comment             VARCHAR(255),
  rdfs_see_also            VARCHAR(255),
  rdfs_defined_by          VARCHAR(255),
  class_ds_id              VARCHAR(64),
  physical_table           VARCHAR(128),
  physical_column          TEXT,
  is_primary               SMALLINT NOT NULL DEFAULT 0,
  is_required              SMALLINT NOT NULL DEFAULT 0,
  is_key                   SMALLINT NOT NULL DEFAULT 0,
  is_derived               SMALLINT NOT NULL DEFAULT 0,
  is_multi_valued_prop     SMALLINT NOT NULL DEFAULT 0,
  is_range_constraint_prop SMALLINT NOT NULL DEFAULT 0,
  range_class_id           VARCHAR(64),
  sub_property_of          TEXT,
  xsd_min_length           TEXT,
  xsd_max_length           TEXT,
  xsd_length               TEXT,
  xsd_pattern              TEXT,
  xsd_min_inclusive        TEXT,
  xsd_max_inclusive        TEXT,
  owl_functional           SMALLINT NOT NULL DEFAULT 0,
  owl_inverse_functional   SMALLINT NOT NULL DEFAULT 0,
  owl_transitive           SMALLINT NOT NULL DEFAULT 0,
  owl_symmetric            SMALLINT NOT NULL DEFAULT 0,
  owl_asymmetric           SMALLINT NOT NULL DEFAULT 0,
  owl_reflexive            SMALLINT NOT NULL DEFAULT 0,
  owl_irreflexive          SMALLINT NOT NULL DEFAULT 0,
  metadata                 TEXT,
  sort                     INTEGER NOT NULL DEFAULT 0,
  status                   SMALLINT NOT NULL DEFAULT 1,
  create_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_link (
  id               VARCHAR(64) PRIMARY KEY,
  rid              TEXT,
  api_name         VARCHAR(128) NOT NULL,
  source_class_id  VARCHAR(64) NOT NULL,
  target_class_id  VARCHAR(64) NOT NULL,
  cardinality      TEXT DEFAULT 'many_to_many',
  display_name     VARCHAR(128),
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  status           SMALLINT NOT NULL DEFAULT 1,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_group (
  id               VARCHAR(64) PRIMARY KEY,
  class_id         VARCHAR(64) NOT NULL,
  ref_class_id     VARCHAR(64) NOT NULL,
  group_type       VARCHAR(64) NOT NULL,
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  status           SMALLINT NOT NULL DEFAULT 1,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_disjoint_union (
  id               VARCHAR(64) PRIMARY KEY,
  parent_class_id  VARCHAR(64) NOT NULL,
  sub_class_id     VARCHAR(64) NOT NULL,
  status           SMALLINT NOT NULL DEFAULT 1,
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_property_equivalent (
  id               VARCHAR(64) PRIMARY KEY,
  class_id1        TEXT NOT NULL,
  prop_id1         TEXT NOT NULL,
  class_id2        TEXT NOT NULL,
  prop_id2         TEXT NOT NULL,
  status           SMALLINT NOT NULL DEFAULT 1,
  rdfs_comment     VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_property_disjoint (
  id               VARCHAR(64) PRIMARY KEY,
  class_id1        TEXT NOT NULL,
  prop_id1         TEXT NOT NULL,
  class_id2        TEXT NOT NULL,
  prop_id2         TEXT NOT NULL,
  status           SMALLINT NOT NULL DEFAULT 1,
  rdfs_comment     VARCHAR(255),
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_ds (
  id               VARCHAR(64) PRIMARY KEY,
  class_id         VARCHAR(64) NOT NULL,
  ds_code          VARCHAR(64),
  physical_table   VARCHAR(128),
  table_label      TEXT,
  rel_type         VARCHAR(64) NOT NULL DEFAULT '1',
  alias            TEXT,
  pk_keys          TEXT,
  join_on_keys     TEXT,
  join_type        VARCHAR(64) DEFAULT 'LEFT',
  physical_fields  TEXT,
  sort             INTEGER NOT NULL DEFAULT 0,
  status           SMALLINT NOT NULL DEFAULT 1,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_group_class (
  id               VARCHAR(64) PRIMARY KEY,
  group_id         VARCHAR(64) NOT NULL,
  ref_id           VARCHAR(64) NOT NULL,
  group_type       VARCHAR(64) NOT NULL DEFAULT 'object_types',
  category_code    VARCHAR(64),
  g_sort           INTEGER NOT NULL DEFAULT 0,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 11. 接口属性 / 接口-类绑定
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_interface_property (
  id               VARCHAR(64) PRIMARY KEY,
  rid              TEXT,
  interface_id     VARCHAR(64) NOT NULL,
  api_name         VARCHAR(128) NOT NULL,
  prop_code        VARCHAR(64),
  data_type        VARCHAR(64),
  value_type       VARCHAR(64),
  category_code    VARCHAR(64),
  display_name     VARCHAR(128),
  rdfs_label       VARCHAR(255),
  rdfs_comment     VARCHAR(255),
  rdfs_see_also    VARCHAR(255),
  rdfs_defined_by  VARCHAR(255),
  is_required      SMALLINT NOT NULL DEFAULT 0,
  metadata         TEXT,
  status           SMALLINT NOT NULL DEFAULT 1,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface_class (
  id               VARCHAR(64) PRIMARY KEY,
  interface_id     VARCHAR(64) NOT NULL,
  class_id         VARCHAR(64) NOT NULL,
  category_code    VARCHAR(64),
  status           SMALLINT NOT NULL DEFAULT 1,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 12. 链接类型 / 映射
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_link_types (
  id                  VARCHAR(64) PRIMARY KEY,
  link_type_id        VARCHAR(64) NOT NULL,
  rid                 TEXT,
  status              TEXT NOT NULL DEFAULT 'experimental',
  l_object_type_id    VARCHAR(64) NOT NULL,
  r_object_type_id    VARCHAR(64) NOT NULL,
  l_cardinality       VARCHAR(64) NOT NULL DEFAULT 'one',
  r_cardinality       VARCHAR(64) NOT NULL DEFAULT 'one',
  l_display_name      VARCHAR(128),
  l_plural_name       VARCHAR(128),
  r_display_name      VARCHAR(128),
  r_plural_name       VARCHAR(128),
  l_visibility        VARCHAR(64) NOT NULL DEFAULT 'normal',
  r_visibility        VARCHAR(64) NOT NULL DEFAULT 'normal',
  l_api_name          VARCHAR(128),
  r_api_name          VARCHAR(128),
  l_enabled           SMALLINT NOT NULL DEFAULT 1,
  r_enabled           SMALLINT NOT NULL DEFAULT 1,
  is_data_source_rel  SMALLINT NOT NULL DEFAULT 0,
  rel_data_table      TEXT,
  rdfs_label          VARCHAR(255),
  rdfs_comment        VARCHAR(255),
  category_code       VARCHAR(64),
  created_by          VARCHAR(255),
  updated_by          VARCHAR(255),
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_link_mappings (
  mapping_id        VARCHAR(64) PRIMARY KEY,
  link_id           VARCHAR(64) NOT NULL,
  side              VARCHAR(64) NOT NULL,
  seq               INTEGER NOT NULL DEFAULT 1,
  object_field      VARCHAR(128) NOT NULL,
  join_table_column VARCHAR(128),
  created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------------------------------------------
-- 13. 物理表 / 探索设计
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_physical_table (
  id               VARCHAR(64) PRIMARY KEY,
  ds_id            VARCHAR(64) NOT NULL,
  physical_table   VARCHAR(128) NOT NULL,
  display_name     VARCHAR(128),
  table_type       VARCHAR(64) NOT NULL DEFAULT 'table',
  columns_json     TEXT,
  column_count     INTEGER NOT NULL DEFAULT 0,
  status           SMALLINT NOT NULL DEFAULT 1,
  sync_time        TIMESTAMP,
  create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ont_explore_design: 直接使用正确约束 UNIQUE(class_id, name, kind)，合并 V21 变更
CREATE TABLE IF NOT EXISTS ont_explore_design (
  id               VARCHAR(64) PRIMARY KEY,
  class_id         VARCHAR(64) NOT NULL,
  name             VARCHAR(128) NOT NULL,
  kind             TEXT NOT NULL DEFAULT 'query',
  config           TEXT,
  created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ont_explore_design_class_name_kind_key UNIQUE (class_id, name, kind)
);

-- ----------------------------------------------------------------
-- 14. 本体版本
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_ontology_version (
  id          VARCHAR(64) PRIMARY KEY,
  version     INTEGER NOT NULL DEFAULT 0,
  updated_by  VARCHAR(255),
  updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- ================================================================
-- PostgreSQL 合并 DDL — Part 2
-- 第 15～18 节（动作模块 / 语义扩展 / 外部数据源 / 全局索引）
-- ================================================================

-- ----------------------------------------------------------------
-- 15. 动作类型模块（来源 V26，ont_action_rule_property_mapping 含 V37 新列）
-- ----------------------------------------------------------------
CREATE TABLE ont_class_action (
  id                      VARCHAR(64) PRIMARY KEY,
  rid                     VARCHAR(255),
  api_name                VARCHAR(128) NOT NULL UNIQUE,
  m_type                  SMALLINT,
  action_type             SMALLINT,
  action_kind             VARCHAR(64),
  object_class_id         VARCHAR(64),
  class_id                VARCHAR(64),
  link_type_id            VARCHAR(64),
  function_code           VARCHAR(128),
  category_code           VARCHAR(128),
  show_on_detail          SMALLINT NOT NULL DEFAULT 0,
  show_on_batch           SMALLINT NOT NULL DEFAULT 0,
  button_text             VARCHAR(128),
  display_name            VARCHAR(128),
  compile_status          SMALLINT NOT NULL DEFAULT 0,
  save_path               VARCHAR(255),
  form_enabled            SMALLINT NOT NULL DEFAULT 0,
  submit_criteria_enabled SMALLINT NOT NULL DEFAULT 0,
  status                  SMALLINT NOT NULL DEFAULT 0,
  current_version         VARCHAR(64),
  is_deleted              SMALLINT NOT NULL DEFAULT 0,
  icon                    VARCHAR(64),
  color                   VARCHAR(32),
  metadata                TEXT,
  rdfs_label              VARCHAR(255),
  rdfs_comment            TEXT,
  rdfs_see_also           TEXT,
  rdfs_defined_by         TEXT,
  create_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_action_object_class ON ont_class_action(object_class_id);
CREATE INDEX IF NOT EXISTS idx_action_link_type    ON ont_class_action(link_type_id);
CREATE INDEX IF NOT EXISTS idx_action_category     ON ont_class_action(category_code);

CREATE TABLE ont_class_action_rule (
  id                VARCHAR(64) PRIMARY KEY,
  action_id         VARCHAR(64) NOT NULL,
  action_type       SMALLINT,
  rule_type         SMALLINT,
  rule_name         VARCHAR(255),
  target_param_code VARCHAR(128),
  link_type_code    VARCHAR(128),
  sort              INTEGER NOT NULL DEFAULT 0,
  status            SMALLINT NOT NULL DEFAULT 1,
  rule_config       TEXT,
  create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_action_rule_action ON ont_class_action_rule(action_id);

CREATE TABLE ont_action_rule_property_mapping (
  id             VARCHAR(64) PRIMARY KEY,
  rule_id        VARCHAR(64) NOT NULL,
  property_code  VARCHAR(128),
  property_name  VARCHAR(255),
  prop_operator  VARCHAR(32),
  value_source   SMALLINT,
  value_content  TEXT,
  param_name     TEXT,
  default_type   TEXT,
  default_source TEXT,
  is_primary_key SMALLINT NOT NULL DEFAULT 0,
  is_required    SMALLINT NOT NULL DEFAULT 0,
  sort           INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_rule_prop_mapping_rule ON ont_action_rule_property_mapping(rule_id);

CREATE TABLE ont_action_rule_condition (
  id           VARCHAR(64) PRIMARY KEY,
  rule_id      VARCHAR(64) NOT NULL,
  parent_id    VARCHAR(64),
  node_type    VARCHAR(32),
  logic_op     VARCHAR(16),
  left_source  SMALLINT,
  left_code    VARCHAR(128),
  operator     VARCHAR(32),
  right_source SMALLINT,
  right_value  TEXT,
  sort         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_rule_condition_rule ON ont_action_rule_condition(rule_id);

CREATE TABLE ont_action_link_rule_config (
  id                VARCHAR(64) PRIMARY KEY,
  rule_id           VARCHAR(64) NOT NULL,
  action_id         VARCHAR(64),
  link_type_id      VARCHAR(64),
  link_type_code    VARCHAR(128),
  operation         VARCHAR(32),
  source_param_code VARCHAR(128),
  target_param_code VARCHAR(128),
  config            TEXT
);
CREATE INDEX IF NOT EXISTS idx_link_rule_config_rule ON ont_action_link_rule_config(rule_id);

CREATE TABLE ont_action_link_prop_mapping (
  id            VARCHAR(64) PRIMARY KEY,
  link_rule_id  VARCHAR(64) NOT NULL,
  property_code VARCHAR(128),
  property_name VARCHAR(255),
  value_source  SMALLINT,
  value_content TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_link_prop_mapping_rule ON ont_action_link_prop_mapping(link_rule_id);

CREATE TABLE ont_action_form_section (
  id           VARCHAR(64) PRIMARY KEY,
  action_id    VARCHAR(64) NOT NULL,
  section_name VARCHAR(255),
  title        VARCHAR(255),
  description  TEXT,
  collapsed    SMALLINT NOT NULL DEFAULT 0,
  sort         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_form_section_action ON ont_action_form_section(action_id);

CREATE TABLE ont_action_form_param (
  id            VARCHAR(64) PRIMARY KEY,
  action_id     VARCHAR(64) NOT NULL,
  section_id    VARCHAR(64),
  param_code    VARCHAR(128) NOT NULL,
  param_name    VARCHAR(255),
  param_type    VARCHAR(32),
  data_type     VARCHAR(64),
  is_required   SMALLINT NOT NULL DEFAULT 0,
  is_multi      SMALLINT NOT NULL DEFAULT 0,
  default_value TEXT,
  placeholder   VARCHAR(255),
  sort          INTEGER NOT NULL DEFAULT 0,
  config        TEXT
);
CREATE INDEX IF NOT EXISTS idx_form_param_action  ON ont_action_form_param(action_id);
CREATE INDEX IF NOT EXISTS idx_form_param_section ON ont_action_form_param(section_id);

CREATE TABLE ont_action_form_param_display (
  id           VARCHAR(64) PRIMARY KEY,
  param_id     VARCHAR(64) NOT NULL,
  display_type VARCHAR(64),
  label        VARCHAR(255),
  help_text    TEXT,
  visible      SMALLINT NOT NULL DEFAULT 1,
  editable     SMALLINT NOT NULL DEFAULT 1,
  config       TEXT
);
CREATE INDEX IF NOT EXISTS idx_param_display_param ON ont_action_form_param_display(param_id);

CREATE TABLE ont_action_form_display_object (
  id              VARCHAR(64) PRIMARY KEY,
  display_id      VARCHAR(64) NOT NULL,
  object_class_id VARCHAR(64),
  picker_type     VARCHAR(64),
  filter_config   TEXT,
  allow_create    SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_display_object_display ON ont_action_form_display_object(display_id);

CREATE TABLE ont_action_form_display_string (
  id         VARCHAR(64) PRIMARY KEY,
  display_id VARCHAR(64) NOT NULL,
  input_type VARCHAR(32),
  max_length INTEGER,
  regex      TEXT,
  options    TEXT
);
CREATE INDEX IF NOT EXISTS idx_display_string_display ON ont_action_form_display_string(display_id);

CREATE TABLE ont_action_form_display_number (
  id           VARCHAR(64) PRIMARY KEY,
  display_id   VARCHAR(64) NOT NULL,
  min_value    DOUBLE PRECISION,
  max_value    DOUBLE PRECISION,
  step         DOUBLE PRECISION,
  "precision"  INTEGER,
  unit         VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_display_number_display ON ont_action_form_display_number(display_id);

CREATE TABLE ont_action_form_display_boolean (
  id            VARCHAR(64) PRIMARY KEY,
  display_id    VARCHAR(64) NOT NULL,
  true_label    VARCHAR(64),
  false_label   VARCHAR(64),
  default_value SMALLINT
);
CREATE INDEX IF NOT EXISTS idx_display_boolean_display ON ont_action_form_display_boolean(display_id);

CREATE TABLE ont_action_form_override_block (
  id                VARCHAR(64) PRIMARY KEY,
  action_id         VARCHAR(64) NOT NULL,
  block_name        VARCHAR(255),
  target_param_code VARCHAR(128),
  sort              INTEGER NOT NULL DEFAULT 0,
  status            SMALLINT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_override_block_action ON ont_action_form_override_block(action_id);

CREATE TABLE ont_action_form_override_item (
  id              VARCHAR(64) PRIMARY KEY,
  block_id        VARCHAR(64) NOT NULL,
  override_type   VARCHAR(32),
  target_property VARCHAR(128),
  override_value  TEXT,
  sort            INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_override_item_block ON ont_action_form_override_item(block_id);

CREATE TABLE ont_action_override_condition_group (
  id        VARCHAR(64) PRIMARY KEY,
  block_id  VARCHAR(64) NOT NULL,
  parent_id VARCHAR(64),
  logic_op  VARCHAR(16),
  sort      INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_override_cond_group_block ON ont_action_override_condition_group(block_id);

CREATE TABLE ont_action_override_condition_item (
  id           VARCHAR(64) PRIMARY KEY,
  group_id     VARCHAR(64) NOT NULL,
  left_code    VARCHAR(128),
  operator     VARCHAR(32),
  right_value  TEXT,
  value_source SMALLINT,
  sort         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_override_cond_item_group ON ont_action_override_condition_item(group_id);

CREATE TABLE ont_action_function_rule_config (
  id            VARCHAR(64) PRIMARY KEY,
  action_id     VARCHAR(64) NOT NULL,
  rule_id       VARCHAR(64),
  function_code VARCHAR(128),
  function_name VARCHAR(255),
  version       VARCHAR(64),
  runtime       VARCHAR(64),
  entry         VARCHAR(255),
  timeout_ms    INTEGER,
  config        TEXT
);
CREATE INDEX IF NOT EXISTS idx_function_config_action ON ont_action_function_rule_config(action_id);

CREATE TABLE ont_action_function_param_mapping (
  id                 VARCHAR(64) PRIMARY KEY,
  function_config_id VARCHAR(64) NOT NULL,
  param_code         VARCHAR(128),
  param_name         VARCHAR(255),
  value_source       SMALLINT,
  value_content      TEXT,
  direction          VARCHAR(16),
  sort               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_function_param_config ON ont_action_function_param_mapping(function_config_id);

CREATE TABLE ont_action_function_exception_map (
  id                 VARCHAR(64) PRIMARY KEY,
  function_config_id VARCHAR(64) NOT NULL,
  exception_code     VARCHAR(64),
  exception_message  TEXT,
  handle_strategy    VARCHAR(32),
  sort               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_function_exc_config ON ont_action_function_exception_map(function_config_id);

CREATE TABLE ont_action_notification_rule_config (
  id            VARCHAR(64) PRIMARY KEY,
  action_id     VARCHAR(64) NOT NULL,
  rule_id       VARCHAR(64),
  channel       VARCHAR(32),
  template_code VARCHAR(128),
  title         VARCHAR(255),
  content       TEXT,
  recipients    TEXT,
  config        TEXT
);
CREATE INDEX IF NOT EXISTS idx_notification_config_action ON ont_action_notification_rule_config(action_id);

CREATE TABLE ont_action_webhook_rule_config (
  id            VARCHAR(64) PRIMARY KEY,
  action_id     VARCHAR(64) NOT NULL,
  rule_id       VARCHAR(64),
  url           TEXT,
  method        VARCHAR(16),
  headers       TEXT,
  body_template TEXT,
  auth_type     VARCHAR(32),
  auth_config   TEXT,
  timeout_ms    INTEGER,
  retry_count   INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_webhook_config_action ON ont_action_webhook_rule_config(action_id);

CREATE TABLE ont_action_webhook_input_mapping (
  id                VARCHAR(64) PRIMARY KEY,
  webhook_config_id VARCHAR(64) NOT NULL,
  param_code        VARCHAR(128),
  value_source      SMALLINT,
  value_content     TEXT,
  "position"        VARCHAR(16),
  sort              INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_webhook_input_config ON ont_action_webhook_input_mapping(webhook_config_id);

CREATE TABLE ont_action_submit_standard_config (
  id            VARCHAR(64) PRIMARY KEY,
  action_id     VARCHAR(64) NOT NULL,
  enabled       SMALLINT NOT NULL DEFAULT 0,
  validate_mode VARCHAR(16),
  error_message TEXT,
  config        TEXT
);
CREATE INDEX IF NOT EXISTS idx_submit_standard_action ON ont_action_submit_standard_config(action_id);

CREATE TABLE ont_action_submit_condition_node (
  id            VARCHAR(64) PRIMARY KEY,
  standard_id   VARCHAR(64) NOT NULL,
  parent_id     VARCHAR(64),
  node_type     VARCHAR(32),
  logic_op      VARCHAR(16),
  left_code     VARCHAR(128),
  operator      VARCHAR(32),
  right_value   TEXT,
  value_source  SMALLINT,
  error_message TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_submit_condition_standard ON ont_action_submit_condition_node(standard_id);

CREATE TABLE ont_action_form_global_config (
  id             VARCHAR(64) PRIMARY KEY,
  action_id      VARCHAR(64) NOT NULL UNIQUE,
  layout         VARCHAR(32),
  label_position VARCHAR(16),
  label_width    INTEGER,
  submit_text    VARCHAR(64),
  cancel_text    VARCHAR(64),
  theme          VARCHAR(64),
  config         TEXT
);

-- 动作执行日志（来源 V27）
CREATE TABLE ont_action_execution (
  id              VARCHAR(64) PRIMARY KEY,
  action_id       VARCHAR(64) NOT NULL,
  action_api_name VARCHAR(128),
  object_class_id VARCHAR(64),
  op_type         SMALLINT,
  input_params    TEXT,
  resolved_result TEXT,
  status          VARCHAR(32) NOT NULL DEFAULT 'success',
  message         TEXT,
  dry_run         SMALLINT NOT NULL DEFAULT 1,
  executed_by     VARCHAR(64),
  execute_time    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_action_exec_action ON ont_action_execution(action_id);
CREATE INDEX IF NOT EXISTS idx_action_exec_time   ON ont_action_execution(execute_time);

-- ----------------------------------------------------------------
-- 16. 语义扩展（来源 V22 + V28 列内联 + V30 约束）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_synonym_dict (
  id           VARCHAR(64) PRIMARY KEY,
  word         VARCHAR(255) NOT NULL,
  synonyms     TEXT NOT NULL,
  domain       VARCHAR(50),
  confidence   NUMERIC(3,2) DEFAULT 0.9,
  source       VARCHAR(50) DEFAULT 'MANUAL',
  usage_count  INTEGER DEFAULT 0,
  entity_type  TEXT,
  entity_id    TEXT,
  create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_synonym_entity_word UNIQUE (entity_type, entity_id, word)
);
CREATE INDEX IF NOT EXISTS idx_synonym_word       ON sys_synonym_dict(word);
CREATE INDEX IF NOT EXISTS idx_synonym_domain     ON sys_synonym_dict(domain);
CREATE INDEX IF NOT EXISTS idx_synonym_confidence ON sys_synonym_dict(confidence DESC);
CREATE INDEX IF NOT EXISTS idx_synonym_entity     ON sys_synonym_dict(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS ont_domain_term (
  id            VARCHAR(64) PRIMARY KEY,
  standard_term VARCHAR(255) NOT NULL,
  common_terms  TEXT NOT NULL,
  domain        VARCHAR(50) NOT NULL,
  term_type     VARCHAR(50),
  similarity    NUMERIC(3,2) DEFAULT 0.9,
  context       TEXT,
  usage_count   INTEGER DEFAULT 0,
  source        VARCHAR(50) DEFAULT 'MANUAL',
  create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_domain_term_standard ON ont_domain_term(standard_term);
CREATE INDEX IF NOT EXISTS idx_domain_term_domain   ON ont_domain_term(domain);
CREATE INDEX IF NOT EXISTS idx_domain_term_type     ON ont_domain_term(term_type);

CREATE TABLE IF NOT EXISTS ont_class_hierarchy (
  id                VARCHAR(64) PRIMARY KEY,
  child_class_id    VARCHAR(64) NOT NULL,
  parent_class_id   VARCHAR(64) NOT NULL,
  hierarchy_level   INTEGER,
  relationship_type VARCHAR(50) DEFAULT 'IS_A',
  create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (child_class_id)  REFERENCES ont_class(id),
  FOREIGN KEY (parent_class_id) REFERENCES ont_class(id),
  UNIQUE(child_class_id, parent_class_id)
);
CREATE INDEX IF NOT EXISTS idx_hierarchy_child  ON ont_class_hierarchy(child_class_id);
CREATE INDEX IF NOT EXISTS idx_hierarchy_parent ON ont_class_hierarchy(parent_class_id);

CREATE TABLE IF NOT EXISTS sys_stopwords (
  id          VARCHAR(64) PRIMARY KEY,
  word        VARCHAR(100) NOT NULL UNIQUE,
  category    VARCHAR(50) DEFAULT 'COMMON',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_stopword_category ON sys_stopwords(category);

CREATE TABLE IF NOT EXISTS ont_class_expansion (
  class_id          VARCHAR(64) PRIMARY KEY,
  original_text     TEXT NOT NULL,
  expanded_text     TEXT NOT NULL,
  expansion_detail  TEXT,
  embedding_vector  BYTEA,
  token_count       INTEGER,
  expansion_version INTEGER DEFAULT 1,
  last_update       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (class_id) REFERENCES ont_class(id)
);
CREATE INDEX IF NOT EXISTS idx_expansion_update ON ont_class_expansion(last_update);

CREATE TABLE IF NOT EXISTS sys_query_log (
  id                VARCHAR(64) PRIMARY KEY,
  user_id           VARCHAR(64),
  query_text        TEXT NOT NULL,
  matched_entity_id VARCHAR(64),
  match_score       NUMERIC(5,4),
  user_clicked      INTEGER DEFAULT 0,
  session_id        VARCHAR(128),
  query_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (matched_entity_id) REFERENCES ont_class(id)
);
CREATE INDEX IF NOT EXISTS idx_query_log_time    ON sys_query_log(query_time);
CREATE INDEX IF NOT EXISTS idx_query_log_entity  ON sys_query_log(matched_entity_id);
CREATE INDEX IF NOT EXISTS idx_query_log_clicked ON sys_query_log(user_clicked);

CREATE TABLE IF NOT EXISTS sys_synonym_candidate (
  id             VARCHAR(64) PRIMARY KEY,
  word           VARCHAR(255) NOT NULL,
  synonym        VARCHAR(255) NOT NULL,
  confidence     NUMERIC(3,2),
  evidence_count INTEGER DEFAULT 0,
  status         VARCHAR(20) DEFAULT 'PENDING',
  source         VARCHAR(50) DEFAULT 'AUTO_LEARN',
  reviewer       VARCHAR(64),
  review_time    TIMESTAMP,
  create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(word, synonym)
);
CREATE INDEX IF NOT EXISTS idx_candidate_status     ON sys_synonym_candidate(status);
CREATE INDEX IF NOT EXISTS idx_candidate_confidence ON sys_synonym_candidate(confidence DESC);

-- ----------------------------------------------------------------
-- 17. 外部数据源（来源 V31）
-- ----------------------------------------------------------------
CREATE TABLE ont_ext_data_source (
  id              VARCHAR(47) PRIMARY KEY,
  category_code   VARCHAR(64),
  ds_code         VARCHAR(64) NOT NULL,
  ds_name         VARCHAR(128) NOT NULL,
  ds_type         VARCHAR(32) NOT NULL DEFAULT 'http_rest',
  read_write_type INTEGER NOT NULL DEFAULT 1,
  base_url        VARCHAR(512),
  default_method  VARCHAR(16) DEFAULT 'POST',
  content_type    VARCHAR(64) DEFAULT 'application/json',
  connect_timeout INTEGER DEFAULT 5000,
  read_timeout    INTEGER DEFAULT 10000,
  retry_count     INTEGER DEFAULT 1,
  retry_interval  INTEGER DEFAULT 1000,
  ssl_verify      INTEGER DEFAULT 1,
  log_enable      INTEGER DEFAULT 1,
  header_enable   INTEGER DEFAULT 0,
  global_header   TEXT,
  auth_type       VARCHAR(32) DEFAULT 'none',
  auth_config     TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  remark          VARCHAR(512),
  create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_ext_ds_code    ON ont_ext_data_source (category_code, ds_code);
CREATE INDEX        idx_ext_ds_type   ON ont_ext_data_source (ds_type);
CREATE INDEX        idx_ext_ds_status ON ont_ext_data_source (status);

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
  id              VARCHAR(47) PRIMARY KEY,
  ds_id           VARCHAR(47) NOT NULL,
  group_id        VARCHAR(47) DEFAULT '0',
  api_code        VARCHAR(64) NOT NULL,
  api_name        VARCHAR(128) NOT NULL,
  method          VARCHAR(16) DEFAULT 'POST',
  api_path        VARCHAR(256),
  api_status      VARCHAR(16) DEFAULT 'debug',
  read_write_type INTEGER DEFAULT 1,
  description     VARCHAR(512),
  request_params  TEXT,
  response_params TEXT,
  override_auth   INTEGER DEFAULT 0,
  auth_type       VARCHAR(32),
  auth_config     TEXT,
  header_inherit  INTEGER DEFAULT 1,
  content_type    VARCHAR(64),
  timeout         INTEGER,
  status          INTEGER NOT NULL DEFAULT 1,
  sort            INTEGER DEFAULT 0,
  create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_ext_api_code   ON ont_ext_api_interface (ds_id, api_code);
CREATE INDEX        idx_ext_api_group  ON ont_ext_api_interface (group_id);
CREATE INDEX        idx_ext_api_status ON ont_ext_api_interface (status);

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
CREATE INDEX idx_ext_log_ds_time  ON ont_ext_api_call_log (ds_id, call_time);
CREATE INDEX idx_ext_log_api_time ON ont_ext_api_call_log (interface_id, call_time);
CREATE INDEX idx_ext_log_status   ON ont_ext_api_call_log (call_status);

-- ----------------------------------------------------------------
-- 18. 全局索引（来源 V1 末尾 + V32）
-- ----------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_ont_biz_category_category_code          ON ont_biz_category(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_biz_group_parent_id                 ON ont_biz_group(parent_id);
CREATE INDEX IF NOT EXISTS idx_ont_biz_group_class_group_id            ON ont_biz_group_class(group_id);
CREATE INDEX IF NOT EXISTS idx_ont_biz_group_class_ref_id              ON ont_biz_group_class(ref_id);
CREATE INDEX IF NOT EXISTS idx_ont_biz_namespace_version_ns_code       ON ont_biz_namespace_version(ns_code);
CREATE INDEX IF NOT EXISTS idx_ont_class_category_code                 ON ont_class(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_class_link_source_class_id          ON ont_class_link(source_class_id);
CREATE INDEX IF NOT EXISTS idx_ont_class_link_target_class_id          ON ont_class_link(target_class_id);
CREATE INDEX IF NOT EXISTS idx_ont_class_property_class_id             ON ont_class_property(class_id);
CREATE INDEX IF NOT EXISTS idx_ont_dic_type_class_enum_name            ON ont_dic_type_class(enum_name);
CREATE INDEX IF NOT EXISTS idx_ont_dict_item_dict_id                   ON ont_dict_item(dict_id);
CREATE INDEX IF NOT EXISTS idx_ont_enum_items_enum_id                  ON ont_enum_items(enum_id);
CREATE INDEX IF NOT EXISTS idx_ont_enum_sync_log_enum_id               ON ont_enum_sync_log(enum_id);
CREATE INDEX IF NOT EXISTS idx_ont_enum_types_category_code            ON ont_enum_types(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_explore_design_class_id             ON ont_explore_design(class_id);
CREATE INDEX IF NOT EXISTS idx_ont_interface_property_interface_id     ON ont_interface_property(interface_id);
CREATE INDEX IF NOT EXISTS idx_ont_physical_table_ds_id                ON ont_physical_table(ds_id);
CREATE INDEX IF NOT EXISTS idx_ont_type_class_category_code            ON ont_type_class(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_type_class_bind_type_class_meta_id  ON ont_type_class_bind(type_class_meta_id);
CREATE INDEX IF NOT EXISTS idx_ont_type_class_bind_applicable_type     ON ont_type_class_bind(applicable_type);
CREATE INDEX IF NOT EXISTS idx_ont_value_types_category_code           ON ont_value_types(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_value_types_enum_id                 ON ont_value_types(enum_id);

-- 唯一索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_biz_category_u_category_code             ON ont_biz_category(category_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_biz_namespace_u_ns_code                  ON ont_biz_namespace(ns_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_class_u_api_name                         ON ont_class(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_dict_def_u_dict_code                     ON ont_dict_def(dict_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_interface_u_api_name                     ON ont_interface(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_link_types_u_link_type_id                ON ont_link_types(link_type_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_type_class_category_dict_u_category_code ON ont_type_class_category_dict(category_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_value_types_u_api_name                   ON ont_value_types(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_value_types_u_rid                        ON ont_value_types(rid);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_data_source_u_ds_code                    ON sys_data_source(ds_code);
-- V32: 演示种子幂等插入所需约束
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_enum_types_u_api_name        ON ont_enum_types(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_struct_types_u_struct_code    ON ont_struct_types(struct_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_shared_properties_u_prop_code ON ont_shared_properties(prop_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_enum_sync_config_u_enum_id    ON ont_enum_sync_config(enum_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_class_link_u_api_name         ON ont_class_link(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_class_action_u_api_name       ON ont_class_action(api_name);
