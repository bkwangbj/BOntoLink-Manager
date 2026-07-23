-- PG V1: 完整 schema（从 bontolink.db 提取）

CREATE TABLE IF NOT EXISTS ont_biz_category (
  id                             VARCHAR(64) PRIMARY KEY,
  parent_id                      VARCHAR(64) NOT NULL DEFAULT '0',
  rid                            TEXT,
  category_code                  VARCHAR(64) NOT NULL,
  category_type                  INTEGER NOT NULL DEFAULT 1,
  ns_code                        VARCHAR(64),
  status                         SMALLINT NOT NULL DEFAULT 1,
  sort                           INTEGER NOT NULL DEFAULT 0,
  icon                           VARCHAR(255),
  color                          VARCHAR(255),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  description                    VARCHAR(255),
  metadata                       TEXT,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_namespace (
  id                             VARCHAR(64) PRIMARY KEY,
  ns_code                        VARCHAR(64) NOT NULL,
  ns_name                        VARCHAR(128) NOT NULL,
  ns_uri                         VARCHAR(255),
  hierarchy_path                 TEXT,
  curr_version                   VARCHAR(255) NOT NULL DEFAULT '1.0',
  status                         SMALLINT NOT NULL DEFAULT 1,
  metadata                       TEXT,
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_namespace_version (
  id                             VARCHAR(64) PRIMARY KEY,
  ns_code                        VARCHAR(64) NOT NULL,
  version                        VARCHAR(20) NOT NULL,
  uri                            VARCHAR(255) NOT NULL,
  snapshot_data                  TEXT,
  owl_content                    TEXT,
  publish_time                   TIMESTAMP,
  is_current                     SMALLINT NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_biz_group (
  id                             VARCHAR(64) PRIMARY KEY,
  parent_id                      VARCHAR(64),
  category_code                  VARCHAR(64),
  g_name                         VARCHAR(128) NOT NULL,
  g_sort                         INTEGER NOT NULL DEFAULT 0,
  icon                           VARCHAR(255),
  color                          VARCHAR(255),
  description                    VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  domain_code                    VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS ont_class (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  api_name                       VARCHAR(128) NOT NULL,
  ns_code                        VARCHAR(64),
  category_code                  VARCHAR(64),
  display_name                   VARCHAR(128),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  description                    VARCHAR(255),
  icon                           VARCHAR(255),
  color                          VARCHAR(255),
  status                         SMALLINT NOT NULL DEFAULT 1,
  metadata                       TEXT,
  parent_class_id                VARCHAR(64),
  class_expr_type                VARCHAR(64),
  class_expr_content             TEXT,
  is_thing                       SMALLINT NOT NULL DEFAULT 0,
  is_nothing                     SMALLINT NOT NULL DEFAULT 0,
  is_common                      SMALLINT NOT NULL DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  api_name                       VARCHAR(128) NOT NULL,
  interface_code                 VARCHAR(64),
  ns_code                        VARCHAR(64),
  category_code                  VARCHAR(64),
  display_name                   VARCHAR(128),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  description                    VARCHAR(255),
  icon                           VARCHAR(255),
  color                          VARCHAR(255),
  status                         SMALLINT NOT NULL DEFAULT 1,
  metadata                       TEXT,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_data_source (
  id                             VARCHAR(64) PRIMARY KEY,
  category_code                  VARCHAR(64),
  ds_code                        VARCHAR(64) NOT NULL,
  ds_name                        VARCHAR(128) NOT NULL,
  ds_type                        VARCHAR(64) NOT NULL,
  jdbc_driver                    TEXT,
  jdbc_url                       TEXT,
  username                       TEXT,
  password                       TEXT,
  mongo_url                      TEXT,
  status                         SMALLINT NOT NULL DEFAULT 1,
  remark                         VARCHAR(255),
  ref_count                      INTEGER NOT NULL DEFAULT 0,
  connect_status                 TEXT DEFAULT 'online',
  active_conn                    SMALLINT DEFAULT 0,
  max_conn                       TEXT DEFAULT '100',
  response_ms                    SMALLINT DEFAULT 0,
  collection_cnt                 SMALLINT DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_dict_def (
  id                             VARCHAR(64) PRIMARY KEY,
  dict_code                      VARCHAR(64) NOT NULL,
  dict_name                      VARCHAR(255) NOT NULL,
  rdfs_comment                   VARCHAR(255),
  status                         SMALLINT NOT NULL DEFAULT 1,
  sort_no                        INTEGER NOT NULL DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS icon_lib_group (
  id                             VARCHAR(64) PRIMARY KEY,
  parent_id                      VARCHAR(64),
  name                           VARCHAR(128) NOT NULL,
  sort                           INTEGER NOT NULL DEFAULT 0,
  source                         TEXT,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS icon_lib_icon (
  id                             VARCHAR(64) PRIMARY KEY,
  group_id                       VARCHAR(64) NOT NULL,
  name                           VARCHAR(128) NOT NULL,
  view_box                       VARCHAR(255) NOT NULL DEFAULT '0 0 1024 1024',
  content                        TEXT NOT NULL,
  sort                           INTEGER NOT NULL DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_value_types (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT NOT NULL,
  api_name                       VARCHAR(128) NOT NULL,
  category_code                  VARCHAR(64),
  base_type                      VARCHAR(64) NOT NULL,
  constraint_type                VARCHAR(64) NOT NULL,
  constraint_config              TEXT,
  enum_id                        VARCHAR(64),
  default_usage_config_id        VARCHAR(64),
  status                         SMALLINT NOT NULL DEFAULT 1,
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_valuetypes_usage_config (
  id                             VARCHAR(64) PRIMARY KEY,
  max_select_level               INTEGER NOT NULL DEFAULT 0,
  allow_non_leaf                 SMALLINT NOT NULL DEFAULT 0,
  display_format                 TEXT NOT NULL DEFAULT 'label',
  is_system_default              SMALLINT NOT NULL DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_types (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  api_name                       VARCHAR(128) NOT NULL,
  category_code                  VARCHAR(64),
  enum_type                      VARCHAR(64) NOT NULL DEFAULT 'general_single',
  max_level                      INTEGER NOT NULL DEFAULT 1,
  top_code                       VARCHAR(64),
  status                         TEXT NOT NULL DEFAULT 'active',
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_dic_type_class (
  id                             VARCHAR(64) PRIMARY KEY,
  enum_name                      VARCHAR(64) NOT NULL,
  code                           VARCHAR(64) NOT NULL,
  name                           VARCHAR(128) NOT NULL,
  sort_no                        INTEGER NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_type_class_category_dict (
  category_code                  VARCHAR(64) PRIMARY KEY,
  icon                           VARCHAR(255),
  color                          VARCHAR(255),
  category_name_cn               VARCHAR(128) NOT NULL,
  global_allow_apply_types       TEXT NOT NULL DEFAULT '[]',
  source_type                    VARCHAR(64) NOT NULL DEFAULT 'platform_built',
  sort_weight                    INTEGER NOT NULL DEFAULT 999,
  description                    VARCHAR(255),
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_struct_types (
  id                             VARCHAR(64) PRIMARY KEY,
  struct_code                    VARCHAR(64) NOT NULL,
  category_code                  VARCHAR(64),
  status                         SMALLINT NOT NULL DEFAULT 1,
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_shared_properties (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  category_code                  VARCHAR(64),
  prop_code                      VARCHAR(64) NOT NULL,
  prop_type                      VARCHAR(64) NOT NULL DEFAULT 'data',
  is_key                         SMALLINT NOT NULL DEFAULT 0,
  data_type                      VARCHAR(64),
  value_type                     VARCHAR(64),
  is_required                    SMALLINT NOT NULL DEFAULT 0,
  is_multi_valued_prop           SMALLINT NOT NULL DEFAULT 0,
  is_range_constraint_prop       SMALLINT NOT NULL DEFAULT 0,
  xsd_min_length                 TEXT,
  xsd_max_length                 TEXT,
  xsd_length                     TEXT,
  xsd_pattern                    TEXT,
  xsd_min_inclusive              TEXT,
  xsd_max_inclusive              TEXT,
  owl_functional                 SMALLINT NOT NULL DEFAULT 0,
  owl_inverse_functional         SMALLINT NOT NULL DEFAULT 0,
  owl_transitive                 SMALLINT NOT NULL DEFAULT 0,
  owl_symmetric                  SMALLINT NOT NULL DEFAULT 0,
  owl_asymmetric                 SMALLINT NOT NULL DEFAULT 0,
  owl_reflexive                  SMALLINT NOT NULL DEFAULT 0,
  owl_irreflexive                SMALLINT NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  metadata                       TEXT,
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_property_format (
  format_id                      VARCHAR(64) PRIMARY KEY,
  src_type                       VARCHAR(64) NOT NULL DEFAULT '1',
  property_id                    VARCHAR(64) NOT NULL,
  property_scope                 VARCHAR(64) NOT NULL DEFAULT 'class',
  format_enabled                 SMALLINT NOT NULL DEFAULT 0,
  format_type                    VARCHAR(64) NOT NULL DEFAULT 'general',
  decimal_places                 TEXT DEFAULT '2',
  use_thousand_sep               SMALLINT DEFAULT 0,
  negative_mode                  TEXT DEFAULT '3',
  currency_symbol                TEXT DEFAULT '¥',
  accounting_align               SMALLINT DEFAULT 1,
  date_pattern                   TEXT DEFAULT 'yyyy-MM-dd',
  time_pattern                   TEXT DEFAULT 'HH:mm:ss',
  locale                         TEXT DEFAULT 'zh-CN',
  fraction_type                  VARCHAR(64) DEFAULT '# ?/?',
  special_type                   VARCHAR(64) DEFAULT 'zipcode',
  custom_format                  TEXT DEFAULT 'G/通用格式',
  text_force                     SMALLINT DEFAULT 0,
  text_max_length                TEXT,
  text_regex                     TEXT,
  percent_auto_multiply          SMALLINT DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_user                    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ont_enum_level_code_rule (
  id                             VARCHAR(64) PRIMARY KEY,
  enum_id                        VARCHAR(64) NOT NULL,
  code_name                      TEXT NOT NULL,
  rule_level                     INTEGER NOT NULL,
  code_separator                 TEXT,
  code_len                       INTEGER NOT NULL,
  total_len                      INTEGER NOT NULL,
  fill_char                      SMALLINT DEFAULT 0,
  fill_pos                       SMALLINT DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_sync_config (
  id                             VARCHAR(64) PRIMARY KEY,
  enum_id                        VARCHAR(64) NOT NULL,
  data_source_id                 VARCHAR(64),
  table_alias                    TEXT,
  table_name                     TEXT,
  field_code                     VARCHAR(64),
  field_name                     TEXT,
  field_sort                     TEXT,
  field_status                   TEXT,
  filter_sql                     TEXT,
  sync_mode                      TEXT DEFAULT 'level_diff',
  sync_strategy                  TEXT DEFAULT 'once',
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  field_parent                   TEXT,
  sync_source_type               VARCHAR(64) NOT NULL DEFAULT 'table',
  custom_sql                     TEXT
);

CREATE TABLE IF NOT EXISTS ont_enum_sync_log (
  id                             VARCHAR(64) PRIMARY KEY,
  enum_id                        VARCHAR(64) NOT NULL,
  sync_type                      VARCHAR(64) NOT NULL DEFAULT 'manual',
  add_count                      INTEGER NOT NULL DEFAULT 0,
  update_count                   INTEGER NOT NULL DEFAULT 0,
  del_count                      INTEGER NOT NULL DEFAULT 0,
  fail_count                     INTEGER NOT NULL DEFAULT 0,
  sync_status                    VARCHAR(64) NOT NULL DEFAULT 'running',
  error_msg                      TEXT,
  oper_user                      TEXT,
  sync_time                      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_enum_items (
  id                             VARCHAR(64) PRIMARY KEY,
  enum_id                        VARCHAR(64) NOT NULL,
  code                           VARCHAR(64) NOT NULL,
  api_name                       VARCHAR(128),
  label                          VARCHAR(255) NOT NULL,
  parent_code                    VARCHAR(64),
  level                          INTEGER NOT NULL DEFAULT 1,
  sort_num                       INTEGER NOT NULL DEFAULT 0,
  status                         TEXT NOT NULL DEFAULT 'active',
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_sync_locked                 SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ont_biz_group_class (
  id                             VARCHAR(64) PRIMARY KEY,
  group_id                       VARCHAR(64) NOT NULL,
  ref_id                         VARCHAR(64) NOT NULL,
  group_type                     VARCHAR(64) NOT NULL DEFAULT 'object_types',
  category_code                  VARCHAR(64),
  g_sort                         INTEGER NOT NULL DEFAULT 0,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_property (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  class_id                       VARCHAR(64) NOT NULL,
  category_code                  VARCHAR(64),
  api_name                       VARCHAR(128) NOT NULL,
  prop_code                      VARCHAR(64),
  prop_type                      VARCHAR(64) DEFAULT 'data',
  data_type                      VARCHAR(64),
  value_type                     VARCHAR(64),
  display_name                   VARCHAR(128),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  class_ds_id                    VARCHAR(64),
  physical_table                 VARCHAR(128),
  physical_column                TEXT,
  is_primary                     SMALLINT NOT NULL DEFAULT 0,
  is_required                    SMALLINT NOT NULL DEFAULT 0,
  is_key                         SMALLINT NOT NULL DEFAULT 0,
  is_derived                     SMALLINT NOT NULL DEFAULT 0,
  is_multi_valued_prop           SMALLINT NOT NULL DEFAULT 0,
  is_range_constraint_prop       SMALLINT NOT NULL DEFAULT 0,
  range_class_id                 VARCHAR(64),
  sub_property_of                TEXT,
  xsd_min_length                 TEXT,
  xsd_max_length                 TEXT,
  xsd_length                     TEXT,
  xsd_pattern                    TEXT,
  xsd_min_inclusive              TEXT,
  xsd_max_inclusive              TEXT,
  owl_functional                 SMALLINT NOT NULL DEFAULT 0,
  owl_inverse_functional         SMALLINT NOT NULL DEFAULT 0,
  owl_transitive                 SMALLINT NOT NULL DEFAULT 0,
  owl_symmetric                  SMALLINT NOT NULL DEFAULT 0,
  owl_asymmetric                 SMALLINT NOT NULL DEFAULT 0,
  owl_reflexive                  SMALLINT NOT NULL DEFAULT 0,
  owl_irreflexive                SMALLINT NOT NULL DEFAULT 0,
  metadata                       TEXT,
  sort                           INTEGER NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_link (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  api_name                       VARCHAR(128) NOT NULL,
  source_class_id                VARCHAR(64) NOT NULL,
  target_class_id                VARCHAR(64) NOT NULL,
  cardinality                    TEXT DEFAULT 'many_to_many',
  display_name                   VARCHAR(128),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_action (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  class_id                       VARCHAR(64),
  api_name                       VARCHAR(128) NOT NULL,
  action_kind                    TEXT,
  display_name                   VARCHAR(128),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface_property (
  id                             VARCHAR(64) PRIMARY KEY,
  rid                            TEXT,
  interface_id                   VARCHAR(64) NOT NULL,
  api_name                       VARCHAR(128) NOT NULL,
  prop_code                      VARCHAR(64),
  data_type                      VARCHAR(64),
  value_type                     VARCHAR(64),
  category_code                  VARCHAR(64),
  display_name                   VARCHAR(128),
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  is_required                    SMALLINT NOT NULL DEFAULT 0,
  metadata                       TEXT,
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_interface_class (
  id                             VARCHAR(64) PRIMARY KEY,
  interface_id                   VARCHAR(64) NOT NULL,
  class_id                       VARCHAR(64) NOT NULL,
  category_code                  VARCHAR(64),
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_group (
  id                             VARCHAR(64) PRIMARY KEY,
  class_id                       VARCHAR(64) NOT NULL,
  ref_class_id                   VARCHAR(64) NOT NULL,
  group_type                     VARCHAR(64) NOT NULL,
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_disjoint_union (
  id                             VARCHAR(64) PRIMARY KEY,
  parent_class_id                VARCHAR(64) NOT NULL,
  sub_class_id                   VARCHAR(64) NOT NULL,
  status                         SMALLINT NOT NULL DEFAULT 1,
  rdfs_comment                   VARCHAR(255),
  rdfs_see_also                  VARCHAR(255),
  rdfs_defined_by                VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_property_equivalent (
  id                             VARCHAR(64) PRIMARY KEY,
  class_id1                      TEXT NOT NULL,
  prop_id1                       TEXT NOT NULL,
  class_id2                      TEXT NOT NULL,
  prop_id2                       TEXT NOT NULL,
  status                         SMALLINT NOT NULL DEFAULT 1,
  rdfs_comment                   VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_property_disjoint (
  id                             VARCHAR(64) PRIMARY KEY,
  class_id1                      TEXT NOT NULL,
  prop_id1                       TEXT NOT NULL,
  class_id2                      TEXT NOT NULL,
  prop_id2                       TEXT NOT NULL,
  status                         SMALLINT NOT NULL DEFAULT 1,
  rdfs_comment                   VARCHAR(255),
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_class_ds (
  id                             VARCHAR(64) PRIMARY KEY,
  class_id                       VARCHAR(64) NOT NULL,
  ds_code                        VARCHAR(64),
  physical_table                 VARCHAR(128),
  table_label                    TEXT,
  rel_type                       VARCHAR(64) NOT NULL DEFAULT '1',
  alias                          TEXT,
  pk_keys                        TEXT,
  join_on_keys                   TEXT,
  join_type                      VARCHAR(64) DEFAULT 'LEFT',
  physical_fields                TEXT,
  sort                           INTEGER NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_struct_items (
  id                             VARCHAR(64) PRIMARY KEY,
  struct_id                      VARCHAR(64) NOT NULL,
  sort_no                        INTEGER NOT NULL DEFAULT 0,
  prop_id                        VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS ont_link_types (
  id                             VARCHAR(64) PRIMARY KEY,
  link_type_id                   VARCHAR(64) NOT NULL,
  rid                            TEXT,
  status                         TEXT NOT NULL DEFAULT 'experimental',
  l_object_type_id               VARCHAR(64) NOT NULL,
  r_object_type_id               VARCHAR(64) NOT NULL,
  l_cardinality                  VARCHAR(64) NOT NULL DEFAULT 'one',
  r_cardinality                  VARCHAR(64) NOT NULL DEFAULT 'one',
  l_display_name                 VARCHAR(128),
  l_plural_name                  VARCHAR(128),
  r_display_name                 VARCHAR(128),
  r_plural_name                  VARCHAR(128),
  l_visibility                   VARCHAR(64) NOT NULL DEFAULT 'normal',
  r_visibility                   VARCHAR(64) NOT NULL DEFAULT 'normal',
  l_api_name                     VARCHAR(128),
  r_api_name                     VARCHAR(128),
  l_enabled                      SMALLINT NOT NULL DEFAULT 1,
  r_enabled                      SMALLINT NOT NULL DEFAULT 1,
  is_data_source_rel             SMALLINT NOT NULL DEFAULT 0,
  rel_data_table                 TEXT,
  rdfs_label                     VARCHAR(255),
  rdfs_comment                   VARCHAR(255),
  category_code                  VARCHAR(64),
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_by                     VARCHAR(255),
  updated_by                     VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS ont_link_mappings (
  mapping_id                     VARCHAR(64) PRIMARY KEY,
  link_id                        VARCHAR(64) NOT NULL,
  side                           VARCHAR(64) NOT NULL,
  seq                            INTEGER NOT NULL DEFAULT 1,
  object_field                   VARCHAR(128) NOT NULL,
  join_table_column              VARCHAR(128),
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_type_class (
  id                             VARCHAR(64) PRIMARY KEY,
  category_code                  VARCHAR(64) NOT NULL,
  icon                           VARCHAR(255),
  color                          VARCHAR(255),
  name_prefix                    VARCHAR(64) NOT NULL,
  name_template                  TEXT,
  name_cn_base                   VARCHAR(128) NOT NULL,
  source_type                    VARCHAR(64) NOT NULL DEFAULT 'platform_built',
  group_tag                      TEXT,
  allow_apply_types              TEXT NOT NULL DEFAULT '[]',
  allow_multi_bind               SMALLINT NOT NULL DEFAULT 0,
  is_array_value                 SMALLINT NOT NULL DEFAULT 0,
  system_protected               SMALLINT NOT NULL DEFAULT 0,
  param_type                     VARCHAR(64) NOT NULL DEFAULT 'text',
  frontend_component             VARCHAR(64) NOT NULL DEFAULT 'text_input',
  param_options_json             TEXT,
  param_validator_json           TEXT,
  param_desc                     VARCHAR(255),
  demo_value                     TEXT,
  depend_on_meta_ids             TEXT NOT NULL DEFAULT '[]',
  description                    VARCHAR(255),
  replacement_meta_id            VARCHAR(64),
  is_deprecated                  SMALLINT NOT NULL DEFAULT 0,
  deprecated_reason              VARCHAR(255),
  support_version_min            VARCHAR(255),
  current_version_no             INTEGER NOT NULL DEFAULT 1,
  sort_weight                    INTEGER NOT NULL DEFAULT 999,
  create_user                    VARCHAR(255),
  update_user                    VARCHAR(255),
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  param_json                     TEXT
);

CREATE TABLE IF NOT EXISTS ont_type_class_bind (
  id                             VARCHAR(64) PRIMARY KEY,
  env                            VARCHAR(64) NOT NULL DEFAULT 'prod',
  type_class_meta_id             VARCHAR(64) NOT NULL,
  applicable_type                VARCHAR(64) NOT NULL,
  property_owner_type            VARCHAR(64),
  property_owner_id              VARCHAR(64),
  property_id                    VARCHAR(64),
  link_type_id                   VARCHAR(64),
  action_type_id                 VARCHAR(64),
  suffix_custom                  TEXT,
  value                          TEXT,
  bind_deprecated                SMALLINT NOT NULL DEFAULT 0,
  remark                         VARCHAR(255),
  create_user                    VARCHAR(255),
  update_user                    VARCHAR(255),
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_dict_item (
  id                             VARCHAR(64) PRIMARY KEY,
  dict_id                        VARCHAR(64) NOT NULL,
  parent_id                      VARCHAR(64),
  item_code                      VARCHAR(64) NOT NULL,
  item_value                     VARCHAR(255) NOT NULL,
  sort_no                        INTEGER NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  color                          VARCHAR(255),
  ext_data                       TEXT,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_explore_design (
  id                             VARCHAR(64) PRIMARY KEY,
  class_id                       VARCHAR(64) NOT NULL,
  name                           VARCHAR(128) NOT NULL,
  kind                           TEXT NOT NULL DEFAULT 'query',
  config                         TEXT,
  created_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at                     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ont_physical_table (
  id                             VARCHAR(64) PRIMARY KEY,
  ds_id                          VARCHAR(64) NOT NULL,
  physical_table                 VARCHAR(128) NOT NULL,
  display_name                   VARCHAR(128),
  table_type                     VARCHAR(64) NOT NULL DEFAULT 'table',
  columns_json                   TEXT,
  column_count                   INTEGER NOT NULL DEFAULT 0,
  status                         SMALLINT NOT NULL DEFAULT 1,
  sync_time                      TIMESTAMP,
  create_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time                    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE ont_type_class ADD COLUMN IF NOT EXISTS param_json TEXT;

CREATE INDEX IF NOT EXISTS idx_ont_biz_category_category_code ON ont_biz_category(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_biz_group_parent_id ON ont_biz_group(parent_id);
CREATE INDEX IF NOT EXISTS idx_ont_biz_group_class_group_id ON ont_biz_group_class(group_id);
CREATE INDEX IF NOT EXISTS idx_ont_biz_group_class_ref_id ON ont_biz_group_class(ref_id);
CREATE INDEX IF NOT EXISTS idx_ont_biz_namespace_version_ns_code ON ont_biz_namespace_version(ns_code);
CREATE INDEX IF NOT EXISTS idx_ont_class_category_code ON ont_class(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_class_link_source_class_id ON ont_class_link(source_class_id);
CREATE INDEX IF NOT EXISTS idx_ont_class_link_target_class_id ON ont_class_link(target_class_id);
CREATE INDEX IF NOT EXISTS idx_ont_class_property_class_id ON ont_class_property(class_id);
CREATE INDEX IF NOT EXISTS idx_ont_dic_type_class_enum_name ON ont_dic_type_class(enum_name);
CREATE INDEX IF NOT EXISTS idx_ont_dict_item_dict_id ON ont_dict_item(dict_id);
CREATE INDEX IF NOT EXISTS idx_ont_enum_items_enum_id ON ont_enum_items(enum_id);
CREATE INDEX IF NOT EXISTS idx_ont_enum_sync_log_enum_id ON ont_enum_sync_log(enum_id);
CREATE INDEX IF NOT EXISTS idx_ont_enum_types_category_code ON ont_enum_types(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_explore_design_class_id ON ont_explore_design(class_id);
CREATE INDEX IF NOT EXISTS idx_ont_interface_property_interface_id ON ont_interface_property(interface_id);
CREATE INDEX IF NOT EXISTS idx_ont_physical_table_ds_id ON ont_physical_table(ds_id);
CREATE INDEX IF NOT EXISTS idx_ont_type_class_category_code ON ont_type_class(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_type_class_bind_type_class_meta_id ON ont_type_class_bind(type_class_meta_id);
CREATE INDEX IF NOT EXISTS idx_ont_type_class_bind_applicable_type ON ont_type_class_bind(applicable_type);
CREATE INDEX IF NOT EXISTS idx_ont_value_types_category_code ON ont_value_types(category_code);
CREATE INDEX IF NOT EXISTS idx_ont_value_types_enum_id ON ont_value_types(enum_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_biz_category_u_category_code ON ont_biz_category(category_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_biz_namespace_u_ns_code ON ont_biz_namespace(ns_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_class_u_api_name ON ont_class(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_dict_def_u_dict_code ON ont_dict_def(dict_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_interface_u_api_name ON ont_interface(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_link_types_u_link_type_id ON ont_link_types(link_type_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_type_class_category_dict_u_category_code ON ont_type_class_category_dict(category_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_value_types_u_api_name ON ont_value_types(api_name);
CREATE UNIQUE INDEX IF NOT EXISTS idx_sys_data_source_u_ds_code ON sys_data_source(ds_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_ont_value_types_u_rid ON ont_value_types(rid);
