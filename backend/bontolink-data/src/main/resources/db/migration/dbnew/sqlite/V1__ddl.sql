-- =====================================================================
-- SQLite 合并 DDL
-- 来源: sqlite/V1 + V21~V31 所有结构变更
-- 规则: 使用最终版本; ALTER 已内联; 约束直接写正确值
-- =====================================================================

-- ----------------------------------------------------------------
-- 1. 行业分类 / 命名空间
-- ----------------------------------------------------------------
CREATE TABLE ont_biz_category (
  id              TEXT PRIMARY KEY,
  parent_id       TEXT NOT NULL DEFAULT '0',
  rid             TEXT,
  category_code   TEXT NOT NULL UNIQUE,
  category_type   INTEGER NOT NULL DEFAULT 1,
  ns_code         TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  sort            INTEGER NOT NULL DEFAULT 0,
  icon            TEXT,
  color           TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  description     TEXT,
  metadata        TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_biz_namespace (
  id              TEXT PRIMARY KEY,
  ns_code         TEXT NOT NULL UNIQUE,
  ns_name         TEXT NOT NULL,
  ns_uri          TEXT,
  hierarchy_path  TEXT,
  curr_version    TEXT NOT NULL DEFAULT '1.0',
  status          INTEGER NOT NULL DEFAULT 1,
  metadata        TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_biz_namespace_version (
  id              TEXT PRIMARY KEY,
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
  id              TEXT PRIMARY KEY,
  parent_id       TEXT,
  category_code   TEXT,
  g_name          TEXT NOT NULL,
  g_sort          INTEGER NOT NULL DEFAULT 0,
  icon            TEXT,
  color           TEXT,
  description     TEXT,
  domain_code     TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 2. 核心本体: 类 / 接口
-- ----------------------------------------------------------------
CREATE TABLE ont_class (
  id                 TEXT PRIMARY KEY,
  rid                TEXT,
  api_name           TEXT NOT NULL UNIQUE,
  ns_code            TEXT,
  category_code      TEXT,
  display_name       TEXT,
  rdfs_label         TEXT,
  rdfs_comment       TEXT,
  rdfs_see_also      TEXT,
  rdfs_defined_by    TEXT,
  description        TEXT,
  icon               TEXT,
  color              TEXT,
  status             INTEGER NOT NULL DEFAULT 1,
  metadata           TEXT,
  parent_class_id    TEXT,
  class_expr_type    TEXT,
  class_expr_content TEXT,
  is_thing           INTEGER NOT NULL DEFAULT 0,
  is_nothing         INTEGER NOT NULL DEFAULT 0,
  is_common          INTEGER NOT NULL DEFAULT 0,
  create_time        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time        TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_interface (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  api_name        TEXT NOT NULL UNIQUE,
  interface_code  TEXT,
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
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 3. 数据源
-- ----------------------------------------------------------------
CREATE TABLE sys_data_source (
  id              TEXT PRIMARY KEY,
  category_code   TEXT,
  ds_code         TEXT NOT NULL UNIQUE,
  ds_name         TEXT NOT NULL,
  ds_type         TEXT NOT NULL,
  jdbc_driver     TEXT,
  jdbc_url        TEXT,
  username        TEXT,
  password        TEXT,
  mongo_url       TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  remark          TEXT,
  ref_count       INTEGER NOT NULL DEFAULT 0,
  connect_status  TEXT DEFAULT 'online',
  active_conn     INTEGER DEFAULT 0,
  max_conn        INTEGER DEFAULT 100,
  response_ms     INTEGER DEFAULT 0,
  collection_cnt  INTEGER DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 4. 字典
-- ----------------------------------------------------------------
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

CREATE TABLE ont_dict_item (
  id          TEXT PRIMARY KEY,
  dict_id     TEXT NOT NULL REFERENCES ont_dict_def(id) ON DELETE CASCADE,
  parent_id   TEXT REFERENCES ont_dict_item(id),
  item_code   TEXT NOT NULL,
  item_value  TEXT NOT NULL,
  sort_no     INTEGER NOT NULL DEFAULT 0,
  status      INTEGER NOT NULL DEFAULT 1,
  color       TEXT,
  ext_data    TEXT,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(dict_id, item_code)
);

-- ----------------------------------------------------------------
-- 5. 图标库
-- ----------------------------------------------------------------
CREATE TABLE icon_lib_group (
  id          TEXT PRIMARY KEY,
  parent_id   TEXT,
  name        TEXT NOT NULL,
  sort        INTEGER NOT NULL DEFAULT 0,
  source      TEXT,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE icon_lib_icon (
  id          TEXT PRIMARY KEY,
  group_id    TEXT NOT NULL,
  name        TEXT NOT NULL,
  view_box    TEXT NOT NULL DEFAULT '0 0 1024 1024',
  content     TEXT NOT NULL,
  sort        INTEGER NOT NULL DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 6. 值类型 / 枚举
-- ----------------------------------------------------------------
CREATE TABLE ont_valuetypes_usage_config (
  id                TEXT PRIMARY KEY,
  max_select_level  INTEGER NOT NULL DEFAULT 0,
  allow_non_leaf    INTEGER NOT NULL DEFAULT 0,
  display_format    TEXT NOT NULL DEFAULT 'label',
  is_system_default INTEGER NOT NULL DEFAULT 0,
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_value_types (
  id                      TEXT PRIMARY KEY,
  rid                     TEXT NOT NULL UNIQUE,
  api_name                TEXT NOT NULL UNIQUE,
  category_code           TEXT,
  base_type               TEXT NOT NULL,
  constraint_type         TEXT NOT NULL,
  constraint_config       TEXT,
  enum_id                 TEXT,
  default_usage_config_id TEXT,
  status                  INTEGER NOT NULL DEFAULT 1,
  rdfs_label              TEXT,
  rdfs_comment            TEXT,
  rdfs_see_also           TEXT,
  rdfs_defined_by         TEXT,
  create_time             TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time             TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_types (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  api_name        TEXT NOT NULL UNIQUE,
  category_code   TEXT,
  enum_type       TEXT NOT NULL DEFAULT 'general_single',
  max_level       INTEGER NOT NULL DEFAULT 1,
  top_code        TEXT,
  status          TEXT NOT NULL DEFAULT 'active',
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_items (
  id              TEXT PRIMARY KEY,
  enum_id         TEXT NOT NULL,
  code            TEXT NOT NULL,
  api_name        TEXT,
  label           TEXT NOT NULL,
  parent_code     TEXT,
  level           INTEGER NOT NULL DEFAULT 1,
  sort_num        INTEGER NOT NULL DEFAULT 0,
  status          TEXT NOT NULL DEFAULT 'active',
  is_sync_locked  INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_level_code_rule (
  id             TEXT PRIMARY KEY,
  enum_id        TEXT NOT NULL,
  code_name      TEXT NOT NULL,
  rule_level     INTEGER NOT NULL,
  code_separator TEXT DEFAULT '',
  code_len       INTEGER NOT NULL,
  total_len      INTEGER NOT NULL,
  fill_char      TEXT DEFAULT '0',
  fill_pos       INTEGER DEFAULT 0,
  create_time    TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_sync_config (
  id               TEXT PRIMARY KEY,
  enum_id          TEXT NOT NULL UNIQUE,
  data_source_id   TEXT,
  table_alias      TEXT,
  table_name       TEXT,
  field_code       TEXT,
  field_name       TEXT,
  field_sort       TEXT,
  field_status     TEXT,
  field_parent     TEXT,
  filter_sql       TEXT,
  sync_mode        TEXT DEFAULT 'level_diff',
  sync_strategy    TEXT DEFAULT 'once',
  sync_source_type TEXT NOT NULL DEFAULT 'table',
  custom_sql       TEXT,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_enum_sync_log (
  id           TEXT PRIMARY KEY,
  enum_id      TEXT NOT NULL,
  sync_type    TEXT NOT NULL DEFAULT 'manual',
  add_count    INTEGER NOT NULL DEFAULT 0,
  update_count INTEGER NOT NULL DEFAULT 0,
  del_count    INTEGER NOT NULL DEFAULT 0,
  fail_count   INTEGER NOT NULL DEFAULT 0,
  sync_status  TEXT NOT NULL DEFAULT 'running',
  error_msg    TEXT,
  oper_user    TEXT,
  sync_time    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 7. 共享属性 / 结构类型
-- ----------------------------------------------------------------
CREATE TABLE ont_struct_types (
  id              TEXT PRIMARY KEY,
  struct_code     TEXT NOT NULL UNIQUE,
  category_code   TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_struct_items (
  id         TEXT PRIMARY KEY,
  struct_id  TEXT NOT NULL,
  sort_no    INTEGER NOT NULL DEFAULT 0,
  prop_id    TEXT NOT NULL
);

CREATE TABLE ont_shared_properties (
  id                         TEXT PRIMARY KEY,
  rid                        TEXT,
  category_code              TEXT,
  prop_code                  TEXT NOT NULL UNIQUE,
  prop_type                  TEXT NOT NULL DEFAULT 'data',
  is_key                     INTEGER NOT NULL DEFAULT 0,
  data_type                  TEXT,
  value_type                 TEXT,
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
  status                     INTEGER NOT NULL DEFAULT 1,
  metadata                   TEXT,
  rdfs_label                 TEXT,
  rdfs_comment               TEXT,
  rdfs_see_also              TEXT,
  rdfs_defined_by            TEXT,
  create_time                TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time                TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
-- ================================================================
-- SQLite 合并 DDL — Part 2
-- 动作模块 / 语义扩展 / 外部数据源 / 执行日志
-- ================================================================

-- ----------------------------------------------------------------
-- 15. 动作类型模块（来源 sqlite/V26，含V37新列）
-- ----------------------------------------------------------------
DROP TABLE IF EXISTS ont_class_action;
CREATE TABLE ont_class_action (
  id                        TEXT PRIMARY KEY,
  rid                       TEXT,
  api_name                  TEXT NOT NULL UNIQUE,
  m_type                    INTEGER,
  action_type               INTEGER,
  action_kind               TEXT,
  object_class_id           TEXT,
  class_id                  TEXT,
  link_type_id              TEXT,
  function_code             TEXT,
  category_code             TEXT,
  show_on_detail            INTEGER NOT NULL DEFAULT 0,
  show_on_batch             INTEGER NOT NULL DEFAULT 0,
  button_text               TEXT,
  display_name              TEXT,
  compile_status            INTEGER NOT NULL DEFAULT 0,
  save_path                 TEXT,
  form_enabled              INTEGER NOT NULL DEFAULT 0,
  submit_criteria_enabled   INTEGER NOT NULL DEFAULT 0,
  status                    INTEGER NOT NULL DEFAULT 0,
  current_version           TEXT,
  is_deleted                INTEGER NOT NULL DEFAULT 0,
  icon                      TEXT,
  color                     TEXT,
  metadata                  TEXT,
  rdfs_label                TEXT,
  rdfs_comment              TEXT,
  rdfs_see_also             TEXT,
  rdfs_defined_by           TEXT,
  create_time               TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time               TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_action_object_class ON ont_class_action(object_class_id);
CREATE INDEX idx_action_link_type    ON ont_class_action(link_type_id);
CREATE INDEX idx_action_category     ON ont_class_action(category_code);

CREATE TABLE ont_class_action_rule (
  id                TEXT PRIMARY KEY,
  action_id         TEXT NOT NULL,
  action_type       INTEGER,
  rule_type         INTEGER,
  rule_name         TEXT,
  target_param_code TEXT,
  link_type_code    TEXT,
  sort              INTEGER NOT NULL DEFAULT 0,
  status            INTEGER NOT NULL DEFAULT 1,
  rule_config       TEXT,
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_action_rule_action ON ont_class_action_rule(action_id);

CREATE TABLE ont_action_rule_property_mapping (
  id             TEXT PRIMARY KEY,
  rule_id        TEXT NOT NULL,
  property_code  TEXT,
  property_name  TEXT,
  prop_operator  TEXT,
  value_source   INTEGER,
  value_content  TEXT,
  param_name     TEXT,
  default_type   TEXT,
  default_source TEXT,
  is_primary_key INTEGER NOT NULL DEFAULT 0,
  is_required    INTEGER NOT NULL DEFAULT 0,
  sort           INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_rule_prop_mapping_rule ON ont_action_rule_property_mapping(rule_id);

CREATE TABLE ont_action_rule_condition (
  id            TEXT PRIMARY KEY,
  rule_id       TEXT NOT NULL,
  parent_id     TEXT,
  node_type     TEXT,
  logic_op      TEXT,
  left_source   INTEGER,
  left_code     TEXT,
  operator      TEXT,
  right_source  INTEGER,
  right_value   TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_rule_condition_rule ON ont_action_rule_condition(rule_id);

CREATE TABLE ont_action_link_rule_config (
  id                TEXT PRIMARY KEY,
  rule_id           TEXT NOT NULL,
  action_id         TEXT,
  link_type_id      TEXT,
  link_type_code    TEXT,
  operation         TEXT,
  source_param_code TEXT,
  target_param_code TEXT,
  config            TEXT
);
CREATE INDEX idx_link_rule_config_rule ON ont_action_link_rule_config(rule_id);

CREATE TABLE ont_action_link_prop_mapping (
  id            TEXT PRIMARY KEY,
  link_rule_id  TEXT NOT NULL,
  property_code TEXT,
  property_name TEXT,
  value_source  INTEGER,
  value_content TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_link_prop_mapping_rule ON ont_action_link_prop_mapping(link_rule_id);

CREATE TABLE ont_action_form_section (
  id           TEXT PRIMARY KEY,
  action_id    TEXT NOT NULL,
  section_name TEXT,
  title        TEXT,
  description  TEXT,
  collapsed    INTEGER NOT NULL DEFAULT 0,
  sort         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_form_section_action ON ont_action_form_section(action_id);

CREATE TABLE ont_action_form_param (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,
  section_id    TEXT,
  param_code    TEXT NOT NULL,
  param_name    TEXT,
  param_type    TEXT,
  data_type     TEXT,
  is_required   INTEGER NOT NULL DEFAULT 0,
  is_multi      INTEGER NOT NULL DEFAULT 0,
  default_value TEXT,
  placeholder   TEXT,
  sort          INTEGER NOT NULL DEFAULT 0,
  config        TEXT
);
CREATE INDEX idx_form_param_action  ON ont_action_form_param(action_id);
CREATE INDEX idx_form_param_section ON ont_action_form_param(section_id);

CREATE TABLE ont_action_form_param_display (
  id           TEXT PRIMARY KEY,
  param_id     TEXT NOT NULL,
  display_type TEXT,
  label        TEXT,
  help_text    TEXT,
  visible      INTEGER NOT NULL DEFAULT 1,
  editable     INTEGER NOT NULL DEFAULT 1,
  config       TEXT
);
CREATE INDEX idx_param_display_param ON ont_action_form_param_display(param_id);

CREATE TABLE ont_action_form_display_object (
  id              TEXT PRIMARY KEY,
  display_id      TEXT NOT NULL,
  object_class_id TEXT,
  picker_type     TEXT,
  filter_config   TEXT,
  allow_create    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_display_object_display ON ont_action_form_display_object(display_id);

CREATE TABLE ont_action_form_display_string (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  input_type TEXT,
  max_length INTEGER,
  min_length INTEGER,
  pattern    TEXT,
  options    TEXT
);
CREATE INDEX idx_display_string_display ON ont_action_form_display_string(display_id);

CREATE TABLE ont_action_form_display_number (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  input_type TEXT,
  min_value  REAL,
  max_value  REAL,
  step       REAL,
  precision  INTEGER
);
CREATE INDEX idx_display_number_display ON ont_action_form_display_number(display_id);

CREATE TABLE ont_action_form_display_datetime (
  id          TEXT PRIMARY KEY,
  display_id  TEXT NOT NULL,
  picker_type TEXT,
  format      TEXT,
  range_mode  INTEGER
);
CREATE INDEX idx_display_datetime_display ON ont_action_form_display_datetime(display_id);

CREATE TABLE ont_action_form_display_boolean (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  style      TEXT
);
CREATE INDEX idx_display_boolean_display ON ont_action_form_display_boolean(display_id);

CREATE TABLE ont_action_form_display_file (
  id             TEXT PRIMARY KEY,
  display_id     TEXT NOT NULL,
  accept         TEXT,
  max_size       INTEGER,
  max_count      INTEGER,
  upload_url     TEXT,
  download_url   TEXT
);
CREATE INDEX idx_display_file_display ON ont_action_form_display_file(display_id);

CREATE TABLE ont_action_form_param_rule (
  id              TEXT PRIMARY KEY,
  param_id        TEXT NOT NULL,
  rule_type       TEXT,
  trigger_event   TEXT,
  condition_logic TEXT,
  action_type     TEXT,
  action_config   TEXT,
  sort            INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_param_rule_param ON ont_action_form_param_rule(param_id);

CREATE TABLE ont_action_submit_standard (
  id          TEXT PRIMARY KEY,
  action_id   TEXT NOT NULL,
  name        TEXT,
  description TEXT,
  sort        INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_submit_standard_action ON ont_action_submit_standard(action_id);

CREATE TABLE ont_action_submit_condition_node (
  id            TEXT PRIMARY KEY,
  standard_id   TEXT NOT NULL,
  parent_id     TEXT,
  node_type     TEXT,
  logic_op      TEXT,
  left_source   INTEGER,
  left_code     TEXT,
  operator      TEXT,
  right_value   TEXT,
  value_source  INTEGER,
  error_message TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_submit_condition_standard ON ont_action_submit_condition_node(standard_id);

CREATE TABLE ont_action_form_global_config (
  id             TEXT PRIMARY KEY,
  action_id      TEXT NOT NULL UNIQUE,
  layout         TEXT,
  label_position TEXT,
  label_width    INTEGER,
  submit_text    TEXT,
  cancel_text    TEXT,
  theme          TEXT,
  config         TEXT
);

-- ----------------------------------------------------------------
-- 16. 动作执行日志（来源 sqlite/V27）
-- ----------------------------------------------------------------
CREATE TABLE ont_action_execution (
  id               TEXT PRIMARY KEY,
  action_id        TEXT NOT NULL,
  action_api_name  TEXT,
  object_class_id  TEXT,
  op_type          INTEGER,
  input_params     TEXT,
  resolved_result  TEXT,
  status           TEXT NOT NULL DEFAULT 'success',
  message          TEXT,
  dry_run          INTEGER NOT NULL DEFAULT 1,
  executed_by      TEXT,
  execute_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_action_exec_action ON ont_action_execution(action_id);
CREATE INDEX idx_action_exec_time   ON ont_action_execution(execute_time);

-- ----------------------------------------------------------------
-- 16b. 动作函数规则 / 入参映射（来源 sqlite/V26）
-- ----------------------------------------------------------------
CREATE TABLE ont_action_function_rule_config (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,
  rule_id       TEXT,
  function_code TEXT,
  function_name TEXT,
  version       TEXT,
  runtime       TEXT,
  entry         TEXT,
  timeout_ms    INTEGER,
  config        TEXT
);
CREATE INDEX idx_function_config_action ON ont_action_function_rule_config(action_id);

CREATE TABLE ont_action_function_param_mapping (
  id                 TEXT PRIMARY KEY,
  function_config_id TEXT NOT NULL,
  param_code         TEXT,
  param_name         TEXT,
  value_source       INTEGER,
  value_content      TEXT,
  direction          TEXT,
  sort               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_function_param_config ON ont_action_function_param_mapping(function_config_id);

-- ----------------------------------------------------------------
-- 17. 语义扩展系统（来源 sqlite/V22 + V28/V30 修正）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_synonym_dict (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL,
    synonyms TEXT NOT NULL,
    domain TEXT,
    confidence REAL DEFAULT 0.9,
    source TEXT DEFAULT 'MANUAL',
    usage_count INTEGER DEFAULT 0,
    entity_type TEXT,
    entity_id TEXT,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime')),
    UNIQUE(entity_type, entity_id, word)
);
CREATE INDEX idx_synonym_word ON sys_synonym_dict(word);
CREATE INDEX idx_synonym_domain ON sys_synonym_dict(domain);
CREATE INDEX idx_synonym_confidence ON sys_synonym_dict(confidence DESC);
CREATE INDEX idx_synonym_entity ON sys_synonym_dict(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS ont_domain_term (
    id TEXT PRIMARY KEY,
    standard_term TEXT NOT NULL,
    common_terms TEXT NOT NULL,
    domain TEXT NOT NULL,
    term_type TEXT,
    similarity REAL DEFAULT 0.9,
    context TEXT,
    usage_count INTEGER DEFAULT 0,
    source TEXT DEFAULT 'MANUAL',
    create_time TEXT DEFAULT (datetime('now','localtime')),
    update_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_domain_term_standard ON ont_domain_term(standard_term);
CREATE INDEX idx_domain_term_domain ON ont_domain_term(domain);
CREATE INDEX idx_domain_term_type ON ont_domain_term(term_type);

CREATE TABLE IF NOT EXISTS ont_class_hierarchy (
    id TEXT PRIMARY KEY,
    child_class_id TEXT NOT NULL,
    parent_class_id TEXT NOT NULL,
    hierarchy_level INTEGER,
    relationship_type TEXT DEFAULT 'IS_A',
    create_time TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (child_class_id) REFERENCES ont_class(id),
    FOREIGN KEY (parent_class_id) REFERENCES ont_class(id),
    UNIQUE(child_class_id, parent_class_id)
);
CREATE INDEX idx_hierarchy_child ON ont_class_hierarchy(child_class_id);
CREATE INDEX idx_hierarchy_parent ON ont_class_hierarchy(parent_class_id);

CREATE TABLE IF NOT EXISTS sys_stopwords (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL UNIQUE,
    category TEXT DEFAULT 'COMMON',
    create_time TEXT DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_stopword_category ON sys_stopwords(category);

CREATE TABLE IF NOT EXISTS ont_class_expansion (
    class_id TEXT PRIMARY KEY,
    original_text TEXT NOT NULL,
    expanded_text TEXT NOT NULL,
    expansion_detail TEXT,
    embedding_vector BLOB,
    token_count INTEGER,
    expansion_version INTEGER DEFAULT 1,
    last_update TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (class_id) REFERENCES ont_class(id)
);
CREATE INDEX idx_expansion_update ON ont_class_expansion(last_update);

CREATE TABLE IF NOT EXISTS sys_query_log (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    query_text TEXT NOT NULL,
    matched_entity_id TEXT,
    match_score REAL,
    user_clicked INTEGER DEFAULT 0,
    session_id TEXT,
    query_time TEXT DEFAULT (datetime('now','localtime')),
    FOREIGN KEY (matched_entity_id) REFERENCES ont_class(id)
);
CREATE INDEX idx_query_log_time ON sys_query_log(query_time);
CREATE INDEX idx_query_log_entity ON sys_query_log(matched_entity_id);
CREATE INDEX idx_query_log_clicked ON sys_query_log(user_clicked);

CREATE TABLE IF NOT EXISTS sys_synonym_candidate (
    id TEXT PRIMARY KEY,
    word TEXT NOT NULL,
    synonym TEXT NOT NULL,
    confidence REAL,
    evidence_count INTEGER DEFAULT 0,
    status TEXT DEFAULT 'PENDING',
    source TEXT DEFAULT 'AUTO_LEARN',
    reviewer TEXT,
    review_time TEXT,
    create_time TEXT DEFAULT (datetime('now','localtime')),
    UNIQUE(word, synonym)
);
CREATE INDEX idx_candidate_status ON sys_synonym_candidate(status);
CREATE INDEX idx_candidate_confidence ON sys_synonym_candidate(confidence DESC);

-- ----------------------------------------------------------------
-- 18. 外部数据源（来源 sqlite/V31）
-- ----------------------------------------------------------------
CREATE TABLE ont_ext_data_source (
  id               TEXT PRIMARY KEY,
  category_code    TEXT,
  ds_code          TEXT NOT NULL,
  ds_name          TEXT NOT NULL,
  ds_type          TEXT NOT NULL DEFAULT 'http_rest',
  read_write_type  INTEGER NOT NULL DEFAULT 1,
  base_url         TEXT,
  default_method   TEXT DEFAULT 'POST',
  content_type     TEXT DEFAULT 'application/json',
  connect_timeout  INTEGER DEFAULT 5000,
  read_timeout     INTEGER DEFAULT 10000,
  retry_count      INTEGER DEFAULT 1,
  retry_interval   INTEGER DEFAULT 1000,
  ssl_verify       INTEGER DEFAULT 1,
  log_enable       INTEGER DEFAULT 1,
  header_enable    INTEGER DEFAULT 0,
  global_header    TEXT,
  auth_type        TEXT DEFAULT 'none',
  auth_config      TEXT,
  status           INTEGER NOT NULL DEFAULT 1,
  remark           TEXT,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX uk_ext_ds_code ON ont_ext_data_source (category_code, ds_code);
CREATE INDEX idx_ext_ds_type ON ont_ext_data_source (ds_type);
CREATE INDEX idx_ext_ds_status ON ont_ext_data_source (status);

CREATE TABLE ont_ext_api_group (
  id          TEXT PRIMARY KEY,
  ds_id       TEXT NOT NULL,
  group_name  TEXT NOT NULL,
  parent_id   TEXT DEFAULT '0',
  sort        INTEGER DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_ext_group_ds ON ont_ext_api_group (ds_id);

CREATE TABLE ont_ext_api_interface (
  id               TEXT PRIMARY KEY,
  ds_id            TEXT NOT NULL,
  group_id         TEXT DEFAULT '0',
  api_code         TEXT NOT NULL,
  api_name         TEXT NOT NULL,
  method           TEXT DEFAULT 'POST',
  api_path         TEXT,
  api_status       TEXT DEFAULT 'debug',
  read_write_type  INTEGER DEFAULT 1,
  description      TEXT,
  request_params   TEXT,
  response_params  TEXT,
  override_auth    INTEGER DEFAULT 0,
  auth_type        TEXT,
  auth_config      TEXT,
  header_inherit   INTEGER DEFAULT 1,
  content_type     TEXT,
  timeout          INTEGER,
  status           INTEGER NOT NULL DEFAULT 1,
  sort             INTEGER DEFAULT 0,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE UNIQUE INDEX uk_ext_api_code ON ont_ext_api_interface (ds_id, api_code);
CREATE INDEX idx_ext_api_group ON ont_ext_api_interface (group_id);
CREATE INDEX idx_ext_api_status ON ont_ext_api_interface (status);

CREATE TABLE ont_ext_api_call_log (
  id             TEXT PRIMARY KEY,
  trace_id       TEXT,
  ds_id          TEXT NOT NULL,
  interface_id   TEXT,
  call_type      TEXT,
  caller         TEXT,
  full_url       TEXT,
  request_header TEXT,
  request_body   TEXT,
  call_status    INTEGER,
  http_status    INTEGER,
  cost_time      INTEGER,
  response_size  INTEGER,
  response_body  TEXT,
  error_msg      TEXT,
  call_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_ext_log_ds_time ON ont_ext_api_call_log (ds_id, call_time);
CREATE INDEX idx_ext_log_api_time ON ont_ext_api_call_log (interface_id, call_time);
CREATE INDEX idx_ext_log_status ON ont_ext_api_call_log (call_status);

-- ----------------------------------------------------------------
-- 8. 属性格式化
-- ----------------------------------------------------------------
CREATE TABLE ont_property_format (
  format_id        TEXT PRIMARY KEY,
  src_type         INTEGER NOT NULL DEFAULT 1,
  property_id      TEXT NOT NULL UNIQUE,
  property_scope   TEXT NOT NULL DEFAULT 'class',
  format_enabled   INTEGER NOT NULL DEFAULT 0,
  format_type      TEXT NOT NULL DEFAULT 'general',
  decimal_places   INTEGER DEFAULT 2,
  use_thousand_sep INTEGER DEFAULT 0,
  negative_mode    INTEGER DEFAULT 3,
  currency_symbol  TEXT DEFAULT '¥',
  accounting_align INTEGER DEFAULT 1,
  date_pattern     TEXT DEFAULT 'yyyy-MM-dd',
  time_pattern     TEXT DEFAULT 'HH:mm:ss',
  locale           TEXT DEFAULT 'zh-CN',
  fraction_type    TEXT DEFAULT '# ?/?',
  special_type     TEXT DEFAULT 'zipcode',
  custom_format    TEXT DEFAULT 'G/通用格式',
  text_force       INTEGER DEFAULT 0,
  text_max_length  INTEGER,
  text_regex       TEXT,
  percent_auto_multiply INTEGER DEFAULT 1,
  create_user      TEXT,
  create_time      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 9. 类型类
-- ----------------------------------------------------------------
CREATE TABLE ont_dic_type_class (
  id          TEXT PRIMARY KEY,
  enum_name   TEXT NOT NULL,
  code        TEXT NOT NULL,
  name        TEXT NOT NULL,
  sort_no     INTEGER NOT NULL DEFAULT 0,
  status      INTEGER NOT NULL DEFAULT 1,
  created_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(enum_name, code)
);

CREATE TABLE ont_type_class_category_dict (
  category_code           TEXT PRIMARY KEY,
  icon                    TEXT,
  color                   TEXT,
  category_name_cn        TEXT NOT NULL,
  global_allow_apply_types TEXT NOT NULL DEFAULT '[]',
  source_type             TEXT NOT NULL DEFAULT 'platform_built',
  sort_weight             INTEGER NOT NULL DEFAULT 999,
  description             TEXT,
  created_at              TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at              TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_type_class (
  id                  TEXT PRIMARY KEY,
  category_code       TEXT NOT NULL,
  icon                TEXT,
  color               TEXT,
  name_prefix         TEXT NOT NULL,
  name_template       TEXT,
  name_cn_base        TEXT NOT NULL,
  source_type         TEXT NOT NULL DEFAULT 'platform_built',
  group_tag           TEXT,
  allow_apply_types   TEXT NOT NULL DEFAULT '[]',
  allow_multi_bind    INTEGER NOT NULL DEFAULT 0,
  is_array_value      INTEGER NOT NULL DEFAULT 0,
  system_protected    INTEGER NOT NULL DEFAULT 0,
  param_type          TEXT NOT NULL DEFAULT 'text',
  frontend_component  TEXT NOT NULL DEFAULT 'text_input',
  param_options_json  TEXT,
  param_validator_json TEXT,
  param_desc          TEXT,
  demo_value          TEXT,
  depend_on_meta_ids  TEXT NOT NULL DEFAULT '[]',
  description         TEXT,
  replacement_meta_id TEXT,
  is_deprecated       INTEGER NOT NULL DEFAULT 0,
  deprecated_reason   TEXT,
  support_version_min TEXT,
  current_version_no  INTEGER NOT NULL DEFAULT 1,
  sort_weight         INTEGER NOT NULL DEFAULT 999,
  param_json          TEXT,
  create_user         TEXT,
  update_user         TEXT,
  created_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(category_code, name_prefix)
);

CREATE TABLE ont_type_class_bind (
  id                  TEXT PRIMARY KEY,
  env                 TEXT NOT NULL DEFAULT 'prod',
  type_class_meta_id  TEXT NOT NULL,
  applicable_type     TEXT NOT NULL,
  property_owner_type TEXT,
  property_owner_id   TEXT,
  property_id         TEXT,
  link_type_id        TEXT,
  action_type_id      TEXT,
  suffix_custom       TEXT,
  value               TEXT,
  bind_deprecated     INTEGER NOT NULL DEFAULT 0,
  remark              TEXT,
  create_user         TEXT,
  update_user         TEXT,
  created_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at          TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 10. 类属性 / 类链接 / 类分组 / 类数据集
-- ----------------------------------------------------------------
CREATE TABLE ont_class_property (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  api_name        TEXT NOT NULL,
  prop_code       TEXT,
  prop_type       TEXT DEFAULT 'data',
  data_type       TEXT,
  value_type      TEXT,
  display_name    TEXT,
  rdfs_label      TEXT,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  class_ds_id     TEXT,
  physical_table  TEXT,
  physical_column TEXT,
  is_primary      INTEGER NOT NULL DEFAULT 0,
  is_required     INTEGER NOT NULL DEFAULT 0,
  is_key          INTEGER NOT NULL DEFAULT 0,
  is_derived      INTEGER NOT NULL DEFAULT 0,
  is_multi_valued_prop      INTEGER NOT NULL DEFAULT 0,
  is_range_constraint_prop  INTEGER NOT NULL DEFAULT 0,
  range_class_id  TEXT,
  sub_property_of TEXT,
  xsd_min_length  INTEGER,
  xsd_max_length  INTEGER,
  xsd_length      INTEGER,
  xsd_pattern     TEXT,
  xsd_min_inclusive TEXT,
  xsd_max_inclusive TEXT,
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

CREATE TABLE ont_class_group (
  id              TEXT PRIMARY KEY,
  class_id        TEXT NOT NULL,
  ref_class_id    TEXT NOT NULL,
  group_type      TEXT NOT NULL,
  rdfs_comment    TEXT,
  rdfs_see_also   TEXT,
  rdfs_defined_by TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE (group_type, class_id, ref_class_id)
);

CREATE TABLE ont_class_disjoint_union (
  id              TEXT PRIMARY KEY,
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
  id              TEXT PRIMARY KEY,
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
  id              TEXT PRIMARY KEY,
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
  id              TEXT PRIMARY KEY,
  class_id        TEXT NOT NULL,
  ds_code         TEXT,
  physical_table  TEXT,
  table_label     TEXT,
  rel_type        TEXT NOT NULL DEFAULT '1',
  alias           TEXT,
  pk_keys         TEXT,
  join_on_keys    TEXT,
  join_type       TEXT DEFAULT 'LEFT',
  physical_fields TEXT,
  sort            INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_biz_group_class (
  id              TEXT PRIMARY KEY,
  group_id        TEXT NOT NULL,
  ref_id          TEXT NOT NULL,
  group_type      TEXT NOT NULL DEFAULT 'object_types',
  category_code   TEXT,
  g_sort          INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 11. 接口属性 / 接口-类绑定
-- ----------------------------------------------------------------
CREATE TABLE ont_interface_property (
  id              TEXT PRIMARY KEY,
  rid             TEXT,
  interface_id    TEXT NOT NULL,
  api_name        TEXT NOT NULL,
  prop_code       TEXT,
  data_type       TEXT,
  value_type      TEXT,
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

CREATE TABLE ont_interface_class (
  id              TEXT PRIMARY KEY,
  interface_id    TEXT NOT NULL,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 12. 链接类型 / 映射
-- ----------------------------------------------------------------
CREATE TABLE ont_link_types (
  id                  TEXT PRIMARY KEY,
  link_type_id        TEXT NOT NULL UNIQUE,
  rid                 TEXT,
  status              TEXT NOT NULL DEFAULT 'experimental',
  l_object_type_id    TEXT NOT NULL,
  r_object_type_id    TEXT NOT NULL,
  l_cardinality       TEXT NOT NULL DEFAULT 'one',
  r_cardinality       TEXT NOT NULL DEFAULT 'one',
  l_display_name      TEXT,
  l_plural_name       TEXT,
  r_display_name      TEXT,
  r_plural_name       TEXT,
  l_visibility        TEXT NOT NULL DEFAULT 'normal',
  r_visibility        TEXT NOT NULL DEFAULT 'normal',
  l_api_name          TEXT,
  r_api_name          TEXT,
  l_enabled           INTEGER NOT NULL DEFAULT 1,
  r_enabled           INTEGER NOT NULL DEFAULT 1,
  is_data_source_rel  INTEGER NOT NULL DEFAULT 0,
  rel_data_table      TEXT,
  rdfs_label          TEXT,
  rdfs_comment        TEXT,
  category_code       TEXT,
  created_by          TEXT,
  updated_by          TEXT,
  created_at          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at          TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE ont_link_mappings (
  mapping_id        TEXT PRIMARY KEY,
  link_id           TEXT NOT NULL,
  side              TEXT NOT NULL,
  seq               INTEGER NOT NULL DEFAULT 1,
  object_field      TEXT NOT NULL,
  join_table_column TEXT,
  created_at        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at        TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------
-- 13. 物理表 / 探索设计
-- ----------------------------------------------------------------
CREATE TABLE ont_physical_table (
  id              TEXT PRIMARY KEY,
  ds_id           TEXT NOT NULL,
  physical_table  TEXT NOT NULL,
  display_name    TEXT,
  table_type      TEXT NOT NULL DEFAULT 'table',
  columns_json    TEXT,
  column_count    INTEGER NOT NULL DEFAULT 0,
  status          INTEGER NOT NULL DEFAULT 1,
  sync_time       TEXT,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(ds_id, physical_table)
);

-- ont_explore_design: 直接使用正确约束 UNIQUE(class_id, name, kind)，合并 V21 变更
CREATE TABLE ont_explore_design (
  id           TEXT PRIMARY KEY,
  class_id     TEXT NOT NULL,
  name         TEXT NOT NULL DEFAULT '',
  kind         TEXT NOT NULL DEFAULT 'query',
  config       TEXT,
  created_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(class_id, name, kind)
);

-- ----------------------------------------------------------------
-- 14. 本体版本（来源 sqlite/V24）
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ont_ontology_version (
  id         TEXT PRIMARY KEY,
  version    INTEGER NOT NULL DEFAULT 0,
  updated_by TEXT,
  updated_at TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
INSERT OR IGNORE INTO ont_ontology_version(id, version) VALUES ('ontology-model', 0);
