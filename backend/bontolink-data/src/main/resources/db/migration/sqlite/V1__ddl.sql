-- SQLite DDL — 由 dbnew/tools/export_schemas.py 从本地 SQLite 库导出
-- 库: bontolink.db

-- Table: icon_lib_group
CREATE TABLE IF NOT EXISTS icon_lib_group (
  id          TEXT PRIMARY KEY,                              -- "ig-" + UUID
  parent_id   TEXT,                                          -- NULL 顶级；2 级时存父节点 id
  name        TEXT NOT NULL,
  sort        INTEGER NOT NULL DEFAULT 0,
  source      TEXT,                                          -- seed | manual
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: icon_lib_icon
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

-- Table: ont_action_execution
CREATE TABLE IF NOT EXISTS ont_action_execution (
  id               TEXT PRIMARY KEY,                 -- "action_exec-" + UUID
  action_id        TEXT NOT NULL,                    -- FK -> ont_class_action.id
  action_api_name  TEXT,
  object_class_id  TEXT,
  op_type          INTEGER,                          -- 动作细分类型 (=action_type)
  input_params     TEXT,                             -- JSON: 表单输入
  resolved_result  TEXT,                             -- JSON: 解析出的实例记录 + 副作用
  status           TEXT NOT NULL DEFAULT 'success',  -- success / validation_failed / failed
  message          TEXT,
  dry_run          INTEGER NOT NULL DEFAULT 1,       -- 1=试运行 0=正式(当前均为模拟)
  executed_by      TEXT,
  execute_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_action_form_display_boolean
CREATE TABLE IF NOT EXISTS ont_action_form_display_boolean (
  id            TEXT PRIMARY KEY,
  display_id    TEXT NOT NULL,
  true_label    TEXT,
  false_label   TEXT,
  default_value INTEGER
);

-- Table: ont_action_form_display_number
CREATE TABLE IF NOT EXISTS ont_action_form_display_number (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  min_value  REAL,
  max_value  REAL,
  step       REAL,
  precision  INTEGER,
  unit       TEXT
);

-- Table: ont_action_form_display_object
CREATE TABLE IF NOT EXISTS ont_action_form_display_object (
  id              TEXT PRIMARY KEY,
  display_id      TEXT NOT NULL,                               -- FK -> ont_action_form_param_display.id
  object_class_id TEXT,
  picker_type     TEXT,                                        -- 选择器类型
  filter_config   TEXT,                                        -- JSON
  allow_create    INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_form_display_string
CREATE TABLE IF NOT EXISTS ont_action_form_display_string (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  input_type TEXT,                                             -- text/textarea/select/radio
  max_length INTEGER,
  regex      TEXT,
  options    TEXT                                              -- JSON
);

-- Table: ont_action_form_global_config
CREATE TABLE IF NOT EXISTS ont_action_form_global_config (
  id             TEXT PRIMARY KEY,
  action_id      TEXT NOT NULL UNIQUE,                         -- FK -> ont_class_action.id
  layout         TEXT,                                         -- one-column/two-column
  label_position TEXT,                                         -- top/left
  label_width    INTEGER,
  submit_text    TEXT,
  cancel_text    TEXT,
  theme          TEXT,
  config         TEXT                                          -- JSON
);

-- Table: ont_action_form_override_block
CREATE TABLE IF NOT EXISTS ont_action_form_override_block (
  id                TEXT PRIMARY KEY,
  action_id         TEXT NOT NULL,                             -- FK -> ont_class_action.id
  block_name        TEXT,
  target_param_code TEXT,
  sort              INTEGER NOT NULL DEFAULT 0,
  status            INTEGER NOT NULL DEFAULT 1
);

-- Table: ont_action_form_override_item
CREATE TABLE IF NOT EXISTS ont_action_form_override_item (
  id              TEXT PRIMARY KEY,
  block_id        TEXT NOT NULL,                               -- FK -> ont_action_form_override_block.id
  override_type   TEXT,                                        -- visible/required/value/options/readonly...
  target_property TEXT,
  override_value  TEXT,
  sort            INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_form_param
CREATE TABLE IF NOT EXISTS ont_action_form_param (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,                                 -- FK -> ont_class_action.id
  section_id    TEXT,                                          -- FK -> ont_action_form_section.id
  param_code    TEXT NOT NULL,
  param_name    TEXT,
  param_type    TEXT,                                          -- object/string/number/boolean/...
  data_type     TEXT,
  is_required   INTEGER NOT NULL DEFAULT 0,
  is_multi      INTEGER NOT NULL DEFAULT 0,
  default_value TEXT,
  placeholder   TEXT,
  sort          INTEGER NOT NULL DEFAULT 0,
  config        TEXT                                           -- JSON
);

-- Table: ont_action_form_param_display
CREATE TABLE IF NOT EXISTS ont_action_form_param_display (
  id           TEXT PRIMARY KEY,
  param_id     TEXT NOT NULL,                                  -- FK -> ont_action_form_param.id
  display_type TEXT,                                           -- 控件类型
  label        TEXT,
  help_text    TEXT,
  visible      INTEGER NOT NULL DEFAULT 1,
  editable     INTEGER NOT NULL DEFAULT 1,
  config       TEXT                                            -- JSON
);

-- Table: ont_action_form_section
CREATE TABLE IF NOT EXISTS ont_action_form_section (
  id           TEXT PRIMARY KEY,
  action_id    TEXT NOT NULL,                                  -- FK -> ont_class_action.id
  section_name TEXT,
  title        TEXT,
  description  TEXT,
  collapsed    INTEGER NOT NULL DEFAULT 0,
  sort         INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_function_exception_map
CREATE TABLE IF NOT EXISTS ont_action_function_exception_map (
  id                 TEXT PRIMARY KEY,
  function_config_id TEXT NOT NULL,                            -- FK -> ont_action_function_rule_config.id
  exception_code     TEXT,
  exception_message  TEXT,
  handle_strategy    TEXT,                                     -- ignore/rollback/retry
  sort               INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_function_param_mapping
CREATE TABLE IF NOT EXISTS ont_action_function_param_mapping (
  id                 TEXT PRIMARY KEY,
  function_config_id TEXT NOT NULL,                            -- FK -> ont_action_function_rule_config.id
  param_code         TEXT,
  param_name         TEXT,
  value_source       INTEGER,
  value_content      TEXT,
  direction          TEXT,                                     -- in/out
  sort               INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_function_rule_config
CREATE TABLE IF NOT EXISTS ont_action_function_rule_config (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,                                 -- FK -> ont_class_action.id
  rule_id       TEXT,
  function_code TEXT,
  function_name TEXT,
  version       TEXT,
  runtime       TEXT,
  entry         TEXT,
  timeout_ms    INTEGER,
  config        TEXT                                           -- JSON
);

-- Table: ont_action_link_prop_mapping
CREATE TABLE IF NOT EXISTS ont_action_link_prop_mapping (
  id            TEXT PRIMARY KEY,
  link_rule_id  TEXT NOT NULL,                                 -- FK -> ont_action_link_rule_config.id
  property_code TEXT,
  property_name TEXT,
  value_source  INTEGER,
  value_content TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_link_rule_config
CREATE TABLE IF NOT EXISTS ont_action_link_rule_config (
  id                TEXT PRIMARY KEY,
  rule_id           TEXT NOT NULL,                             -- FK -> ont_class_action_rule.id
  action_id         TEXT,
  link_type_id      TEXT,
  link_type_code    TEXT,
  operation         TEXT,                                      -- create/delete
  source_param_code TEXT,
  target_param_code TEXT,
  config            TEXT                                       -- JSON
);

-- Table: ont_action_notification_rule_config
CREATE TABLE IF NOT EXISTS ont_action_notification_rule_config (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,                                 -- FK -> ont_class_action.id
  rule_id       TEXT,
  channel       TEXT,                                          -- email/sms/site/webhook
  template_code TEXT,
  title         TEXT,
  content       TEXT,
  recipients    TEXT,                                          -- JSON
  config        TEXT                                           -- JSON
);

-- Table: ont_action_override_condition_group
CREATE TABLE IF NOT EXISTS ont_action_override_condition_group (
  id        TEXT PRIMARY KEY,
  block_id  TEXT NOT NULL,                                     -- FK -> ont_action_form_override_block.id
  parent_id TEXT,                                              -- 条件组树
  logic_op  TEXT,                                              -- and/or
  sort      INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_override_condition_item
CREATE TABLE IF NOT EXISTS ont_action_override_condition_item (
  id           TEXT PRIMARY KEY,
  group_id     TEXT NOT NULL,                                  -- FK -> ont_action_override_condition_group.id
  left_code    TEXT,
  operator     TEXT,
  right_value  TEXT,
  value_source INTEGER,
  sort         INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_rule_condition
CREATE TABLE IF NOT EXISTS ont_action_rule_condition (
  id            TEXT PRIMARY KEY,
  rule_id       TEXT NOT NULL,                                 -- FK -> ont_class_action_rule.id
  parent_id     TEXT,                                          -- 条件树父节点
  node_type     TEXT,                                          -- group/item
  logic_op      TEXT,                                          -- and/or
  left_source   INTEGER,                                       -- 左值来源(同 value_source)
  left_code     TEXT,
  operator      TEXT,                                          -- eq/ne/gt/lt/ge/le/contains/in/empty...
  right_source  INTEGER,
  right_value   TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_rule_property_mapping
CREATE TABLE IF NOT EXISTS ont_action_rule_property_mapping (
  id             TEXT PRIMARY KEY,
  rule_id        TEXT NOT NULL,                                -- FK -> ont_class_action_rule.id
  property_code  TEXT,
  property_name  TEXT,
  prop_operator  TEXT,                                         -- set/add/sub/append/clear
  value_source   INTEGER,                                      -- 1表单参数/2静态/3当前用户/4系统时间/5关联对象属性
  value_content  TEXT,
  is_primary_key INTEGER NOT NULL DEFAULT 0,
  is_required    INTEGER NOT NULL DEFAULT 0,
  sort           INTEGER NOT NULL DEFAULT 0
, param_name TEXT, default_type TEXT, default_source TEXT);

-- Table: ont_action_submit_condition_node
CREATE TABLE IF NOT EXISTS ont_action_submit_condition_node (
  id            TEXT PRIMARY KEY,
  standard_id   TEXT NOT NULL,                                 -- FK -> ont_action_submit_standard_config.id
  parent_id     TEXT,                                          -- 条件树父节点
  node_type     TEXT,                                          -- group/condition
  logic_op      TEXT,                                          -- and/or
  left_code     TEXT,
  operator      TEXT,
  right_value   TEXT,
  value_source  INTEGER,
  error_message TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_submit_standard_config
CREATE TABLE IF NOT EXISTS ont_action_submit_standard_config (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,                                 -- FK -> ont_class_action.id
  enabled       INTEGER NOT NULL DEFAULT 0,
  validate_mode TEXT,                                          -- all/any
  error_message TEXT,
  config        TEXT                                           -- JSON
);

-- Table: ont_action_webhook_input_mapping
CREATE TABLE IF NOT EXISTS ont_action_webhook_input_mapping (
  id                TEXT PRIMARY KEY,
  webhook_config_id TEXT NOT NULL,                             -- FK -> ont_action_webhook_rule_config.id
  param_code        TEXT,
  value_source      INTEGER,
  value_content     TEXT,
  position          TEXT,                                      -- header/query/body
  sort              INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_action_webhook_rule_config
CREATE TABLE IF NOT EXISTS ont_action_webhook_rule_config (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,                                 -- FK -> ont_class_action.id
  rule_id       TEXT,
  url           TEXT,
  method        TEXT,                                          -- POST/GET/PUT
  headers       TEXT,                                          -- JSON
  body_template TEXT,
  auth_type     TEXT,
  auth_config   TEXT,                                          -- JSON
  timeout_ms    INTEGER,
  retry_count   INTEGER NOT NULL DEFAULT 0
);

-- Table: ont_biz_category
CREATE TABLE IF NOT EXISTS ont_biz_category (
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

-- Table: ont_biz_group
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
, domain_code TEXT);

-- Table: ont_biz_group_class
CREATE TABLE IF NOT EXISTS ont_biz_group_class (
  id              TEXT PRIMARY KEY,
  group_id        TEXT NOT NULL,
  ref_id          TEXT NOT NULL,                 -- 关联资源 ID (类型由 group_type 决定)
  group_type      TEXT NOT NULL DEFAULT 'object_types',
  category_code   TEXT,
  g_sort          INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_biz_namespace
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

-- Table: ont_biz_namespace_version
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

-- Table: ont_class
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

-- Table: ont_class_action
CREATE TABLE IF NOT EXISTS ont_class_action (
  id                        TEXT PRIMARY KEY,                 -- "class_action-" + UUID
  rid                       TEXT,                             -- ri.ont.action.{api_name}
  api_name                  TEXT NOT NULL UNIQUE,             -- 英文小写下划线, 动作编码
  m_type                    INTEGER,                          -- 大类 1对象/2链接/3函数/4Webhook/5接口/6通知
  action_type               INTEGER,                          -- 细分 11创建/12修改/13Upsert/14删除对象 21创建/22删除链接 30函数 40Webhook 51-54接口 60通知
  object_class_id           TEXT,                             -- FK -> ont_class.id
  link_type_id              TEXT,                             -- FK -> ont_link_types.id
  function_code             TEXT,                             -- 关联函数编码
  category_code             TEXT,                             -- 行业分类(左树过滤)
  show_on_detail            INTEGER NOT NULL DEFAULT 0,       -- 详情页展示
  show_on_batch             INTEGER NOT NULL DEFAULT 0,       -- 批量场景展示
  button_text               TEXT,                             -- 按钮文案
  compile_status            INTEGER NOT NULL DEFAULT 0,       -- 0未编译/1通过/2失败
  save_path                 TEXT,                             -- 保存位置
  form_enabled              INTEGER NOT NULL DEFAULT 0,       -- 是否启用表单
  submit_criteria_enabled   INTEGER NOT NULL DEFAULT 0,       -- 是否启用提交标准
  status                    INTEGER NOT NULL DEFAULT 0,       -- 0草稿/1已发布/2已停用
  current_version           TEXT,                             -- 当前版本
  is_deleted                INTEGER NOT NULL DEFAULT 0,       -- 逻辑删除
  icon                      TEXT,
  color                     TEXT,
  metadata                  TEXT,                             -- JSON
  rdfs_label                TEXT NOT NULL,                    -- 动作名称(非空)
  rdfs_comment              TEXT,
  rdfs_see_also             TEXT,
  rdfs_defined_by           TEXT,
  create_time               TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time               TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_class_action_rule
CREATE TABLE IF NOT EXISTS ont_class_action_rule (
  id                TEXT PRIMARY KEY,                          -- "action_rule-" + UUID
  action_id         TEXT NOT NULL,                             -- FK -> ont_class_action.id
  action_type       INTEGER,                                   -- 规则所属动作细分类型
  rule_type         INTEGER,                                   -- 规则类型(编辑类/副作用类)
  rule_name         TEXT,
  target_param_code TEXT,                                      -- 作用目标参数编码
  link_type_code    TEXT,
  sort              INTEGER NOT NULL DEFAULT 0,
  status            INTEGER NOT NULL DEFAULT 1,
  rule_config       TEXT,                                      -- JSON
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_class_disjoint_union
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

-- Table: ont_class_ds
CREATE TABLE IF NOT EXISTS ont_class_ds (
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

-- Table: ont_class_expansion
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

-- Table: ont_class_group
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

-- Table: ont_class_hierarchy
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

-- Table: ont_class_link
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

-- Table: ont_class_property
CREATE TABLE IF NOT EXISTS ont_class_property (
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

-- Table: ont_dic_type_class
CREATE TABLE IF NOT EXISTS ont_dic_type_class (
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

-- Table: ont_dict_def
CREATE TABLE IF NOT EXISTS ont_dict_def (

    id            TEXT PRIMARY KEY,

    dict_code     TEXT NOT NULL UNIQUE,

    dict_name     TEXT NOT NULL,

    rdfs_comment  TEXT,

    status        INTEGER NOT NULL DEFAULT 1,

    sort_no       INTEGER NOT NULL DEFAULT 0,

    create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime')),

    update_time   TEXT NOT NULL DEFAULT (datetime('now','localtime'))

);

-- Table: ont_dict_item
CREATE TABLE IF NOT EXISTS ont_dict_item (

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

-- Table: ont_domain_term
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

-- Table: ont_enum_items
CREATE TABLE IF NOT EXISTS ont_enum_items (
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

-- Table: ont_enum_level_code_rule
CREATE TABLE IF NOT EXISTS ont_enum_level_code_rule (
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

-- Table: ont_enum_sync_config
CREATE TABLE IF NOT EXISTS ont_enum_sync_config (
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

-- Table: ont_enum_sync_log
CREATE TABLE IF NOT EXISTS ont_enum_sync_log (
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

-- Table: ont_enum_types
CREATE TABLE IF NOT EXISTS ont_enum_types (
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

-- Table: ont_explore_design
CREATE TABLE IF NOT EXISTS ont_explore_design (

  id           TEXT PRIMARY KEY,

  class_id     TEXT NOT NULL,

  name         TEXT NOT NULL DEFAULT '',

  kind         TEXT NOT NULL DEFAULT 'query',

  config       TEXT,

  created_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),

  updated_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),

  UNIQUE(class_id, name, kind)

);

-- Table: ont_ext_api_call_log
CREATE TABLE IF NOT EXISTS ont_ext_api_call_log (

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

-- Table: ont_ext_api_group
CREATE TABLE IF NOT EXISTS ont_ext_api_group (

  id          TEXT PRIMARY KEY,                    -- "ext_group-" + UUID

  ds_id       TEXT NOT NULL,                       -- 关联 ont_ext_data_source.id

  group_name  TEXT NOT NULL,

  parent_id   TEXT DEFAULT '0',                    -- 顶级为 '0'

  sort        INTEGER DEFAULT 0,

  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))

);

-- Table: ont_ext_api_interface
CREATE TABLE IF NOT EXISTS ont_ext_api_interface (

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

-- Table: ont_ext_data_source
CREATE TABLE IF NOT EXISTS ont_ext_data_source (

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

-- Table: ont_function
CREATE TABLE IF NOT EXISTS ont_function (
  id                TEXT PRIMARY KEY,                 -- "ont_function-" + UUID
  rid               TEXT NOT NULL,                    -- ri.ont.function.{32位}, 永久不变
  version_no        TEXT NOT NULL,                    -- 所属版本号, 与版本库对应
  api_name          TEXT NOT NULL,                    -- 代码内方法名(小驼峰), 同文件唯一
  function_label    TEXT NOT NULL,                    -- 中文业务名称
  function_type     INTEGER NOT NULL DEFAULT 1,       -- 1常规/2动作/3聚合/4衍生/5时序
  language          INTEGER NOT NULL DEFAULT 2,       -- 1PYTHON/2TYPESCRIPT
  industry_dir      TEXT NOT NULL,                    -- 行业目录, 与版本库对齐
  category_dir      TEXT NOT NULL,                    -- 领域目录, 与版本库对齐
  class_name        TEXT,                             -- 代码所属类名
  full_access_path  TEXT NOT NULL,                    -- /行业/领域/类名/方法名, 同版本唯一
  code_file_path    TEXT NOT NULL,                    -- 仓库内相对文件路径
  code_md5          TEXT,                             -- 源码 MD5 指纹
  code_content      TEXT,                             -- 扩展: 模板/源码内容(IDE 落地前供代码预览卡片使用)
  file_line_start   INTEGER,                          -- 源码起始行号
  file_line_end     INTEGER,                          -- 源码结束行号
  status            INTEGER NOT NULL DEFAULT 1,       -- 1草稿/2已发布/3已停用/4已废弃
  visibility        INTEGER NOT NULL DEFAULT 1,       -- 1全平台/2本部门/3指定角色/4私有
  rdfs_label        TEXT,                             -- 本体标准属性: 语义化标签
  rdfs_comment      TEXT,                             -- 本体标准属性: 功能详细描述(函数说明)
  rdfs_see_also     TEXT,                             -- 本体标准属性: 参考资料
  rdfs_defined_by   TEXT,                             -- 本体标准属性: 定义来源
  create_user       TEXT,                             -- 创建人账号
  publish_time      TEXT,                             -- 扩展: 发布时间(详情页基础信息要展示, 文档主表漏列)
  is_deleted        INTEGER NOT NULL DEFAULT 0,
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_function_call_stat
CREATE TABLE IF NOT EXISTS ont_function_call_stat (
  id            TEXT PRIMARY KEY,                     -- "fn_call_stat-" + UUID
  function_id   TEXT NOT NULL,                        -- FK -> ont_function.id
  stat_date     TEXT NOT NULL,                        -- yyyy-MM-dd
  caller_app    TEXT,                                 -- 调用方应用名
  call_count    INTEGER NOT NULL DEFAULT 0,           -- 调用次数
  success_count INTEGER NOT NULL DEFAULT 0,           -- 成功次数
  error_count   INTEGER NOT NULL DEFAULT 0,           -- 错误次数
  avg_cost_ms   INTEGER NOT NULL DEFAULT 0,           -- 平均耗时(毫秒)
  create_time   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_function_env_var
CREATE TABLE IF NOT EXISTS ont_function_env_var (
  id          TEXT PRIMARY KEY,                       -- "ont_function_env_var-" + UUID
  function_id TEXT NOT NULL,                          -- FK -> ont_function.id
  var_name    TEXT NOT NULL,                          -- 变量名, 大写下划线
  var_value   TEXT NOT NULL DEFAULT '',               -- 变量值(敏感信息加密存储)
  var_type    INTEGER NOT NULL DEFAULT 1,             -- 1字符串/2数字型/3布尔型/4枚举型
  value_range TEXT,                                   -- 数字型=范围; 枚举型=逗号分隔可选值
  var_desc    TEXT,                                   -- 变量说明
  is_encrypt  INTEGER NOT NULL DEFAULT 0,             -- 0明文/1加密
  sort_num    INTEGER NOT NULL DEFAULT 0,
  is_deleted  INTEGER NOT NULL DEFAULT 0,
  create_time TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_function_param
CREATE TABLE IF NOT EXISTS ont_function_param (
  id              TEXT PRIMARY KEY,                   -- "ont_function_param-" + UUID
  function_id     TEXT NOT NULL,                      -- FK -> ont_function.id
  param_name      TEXT NOT NULL,                      -- 参数变量名
  param_type      TEXT NOT NULL,                      -- 基础类型/枚举/本体类/自定义结构体
  param_direction INTEGER NOT NULL DEFAULT 1,         -- 1INPUT 入参 / 2OUTPUT 返回值
  is_required     INTEGER NOT NULL DEFAULT 1,         -- 仅入参有效
  default_value   TEXT,                               -- 默认值
  value_range     TEXT,                               -- 取值范围 / 可选项
  param_desc      TEXT,                               -- 参数业务含义(平台元数据, 可编辑)
  object_class_id TEXT,                               -- 扩展: 绑定的本体对象类 id, 支撑类型链接跳转对象详情页
  sort_num        INTEGER NOT NULL DEFAULT 0,
  is_deleted      INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_function_runtime_config
CREATE TABLE IF NOT EXISTS ont_function_runtime_config (
  id                TEXT PRIMARY KEY,                 -- "ont_function_runtime_config-" + UUID
  function_id       TEXT NOT NULL,                    -- FK -> ont_function.id, 一对一
  timeout           INTEGER NOT NULL DEFAULT 30,      -- 执行超时(秒) 1~3600
  retry_count       INTEGER NOT NULL DEFAULT 2,       -- 失败重试次数 0~5
  retry_interval    INTEGER NOT NULL DEFAULT 1,       -- 重试间隔(秒) 0~60
  memory_quota      INTEGER NOT NULL DEFAULT 512,     -- 内存配额(MB) 128~4096
  concurrency_limit INTEGER NOT NULL DEFAULT 100,     -- 并发限制(个) 1~1000
  enable_cache      INTEGER NOT NULL DEFAULT 1,       -- 结果缓存 0关/1开
  cache_ttl         INTEGER NOT NULL DEFAULT 3600,    -- 缓存有效期(秒) 60~86400
  is_deleted        INTEGER NOT NULL DEFAULT 0,
  create_time       TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_interface
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

-- Table: ont_interface_class
CREATE TABLE IF NOT EXISTS ont_interface_class (
  id              TEXT PRIMARY KEY,            -- "interface-class-" + UUID
  interface_id    TEXT NOT NULL,
  class_id        TEXT NOT NULL,
  category_code   TEXT,
  status          INTEGER NOT NULL DEFAULT 1,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_interface_property
CREATE TABLE IF NOT EXISTS ont_interface_property (
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

-- Table: ont_link_mappings
CREATE TABLE IF NOT EXISTS ont_link_mappings (
  mapping_id        TEXT PRIMARY KEY,                         -- "link-mappings-" + UUID
  link_id           TEXT NOT NULL,                            -- 关联 ont_link_types.id
  side              TEXT NOT NULL,                            -- left / right
  seq               INTEGER NOT NULL DEFAULT 1,               -- 复合字段顺序号 (从 1 起)
  object_field      TEXT NOT NULL,                            -- 对象属性字段名
  join_table_column TEXT,                                     -- 中间表列名 (仅 is_data_source_rel=1 时填)
  created_at        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at        TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_link_types
CREATE TABLE IF NOT EXISTS ont_link_types (
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

-- Table: ont_ontology_version
CREATE TABLE IF NOT EXISTS ont_ontology_version (

    id          TEXT PRIMARY KEY,

    version     INTEGER NOT NULL DEFAULT 0,

    updated_by  TEXT,

    updated_at  TEXT NOT NULL DEFAULT (datetime('now','localtime'))

);

-- Table: ont_physical_table
CREATE TABLE IF NOT EXISTS ont_physical_table (

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

-- Table: ont_property_disjoint
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

-- Table: ont_property_equivalent
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

-- Table: ont_property_format
CREATE TABLE IF NOT EXISTS ont_property_format (
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

-- Table: ont_shared_properties
CREATE TABLE IF NOT EXISTS ont_shared_properties (
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

-- Table: ont_struct_items
CREATE TABLE IF NOT EXISTS ont_struct_items (
  id         TEXT PRIMARY KEY,                            -- "struct-items-" + UUID
  struct_id  TEXT NOT NULL,                               -- 关联 ont_struct_types.id
  sort_no    INTEGER NOT NULL DEFAULT 0,                  -- 序号
  prop_id    TEXT NOT NULL                                -- 关联 ont_shared_properties.id
);

-- Table: ont_struct_types
CREATE TABLE IF NOT EXISTS ont_struct_types (
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

-- Table: ont_type_class
CREATE TABLE IF NOT EXISTS ont_type_class (
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

-- Table: ont_type_class_bind
CREATE TABLE IF NOT EXISTS ont_type_class_bind (
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

-- Table: ont_type_class_category_dict
CREATE TABLE IF NOT EXISTS ont_type_class_category_dict (
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

-- Table: ont_value_types
CREATE TABLE IF NOT EXISTS ont_value_types (
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

-- Table: ont_valuetypes_usage_config
CREATE TABLE IF NOT EXISTS ont_valuetypes_usage_config (
  id                 TEXT PRIMARY KEY,                   -- "vt-usage-config-" + UUID
  max_select_level   INTEGER NOT NULL DEFAULT 0,         -- 0 = 不限制
  allow_non_leaf     INTEGER NOT NULL DEFAULT 0,
  display_format     TEXT NOT NULL DEFAULT 'label',      -- label / code / code_label / full_label
  is_system_default  INTEGER NOT NULL DEFAULT 0,
  create_time        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time        TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: ont_version_repo
CREATE TABLE IF NOT EXISTS ont_version_repo (
  id              TEXT PRIMARY KEY,                   -- "ont_version_repo-" + UUID
  rid             TEXT NOT NULL,                      -- ri.ont.version_repo.{32位}
  industry_dir    TEXT NOT NULL,                      -- 一级: 行业目录 (水利/交通/能源…)
  category_dir    TEXT NOT NULL,                      -- 二级: 领域目录 (水文监测函数集…)
  version_no      TEXT NOT NULL,                      -- 语义化版本号, 同行业同领域唯一
  repo_branch     TEXT NOT NULL DEFAULT '',           -- 代码仓库分支
  repo_commit_id  TEXT NOT NULL DEFAULT '',           -- 代码仓库 commit 哈希
  repo_url        TEXT,                               -- 仓库地址(可空)
  version_status  INTEGER NOT NULL DEFAULT 1,         -- 1草稿中/2发布中/3已发布/4已回滚
  is_default      INTEGER NOT NULL DEFAULT 0,         -- 同行业同领域仅一条为 1
  release_note    TEXT,                               -- 版本说明
  publish_user    TEXT,                               -- 发布人
  publish_time    TEXT,                               -- 发布时间
  is_deleted      INTEGER NOT NULL DEFAULT 0,
  create_time     TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  update_time     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- Table: sys_data_source
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

-- Table: sys_query_log
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

-- Table: sys_stopwords
CREATE TABLE IF NOT EXISTS sys_stopwords (

    id TEXT PRIMARY KEY,

    word TEXT NOT NULL UNIQUE,

    category TEXT DEFAULT 'COMMON',

    create_time TEXT DEFAULT (datetime('now','localtime'))

);

-- Table: sys_synonym_candidate
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

-- Table: sys_synonym_dict
CREATE TABLE IF NOT EXISTS sys_synonym_dict (

    id TEXT PRIMARY KEY,

    word TEXT NOT NULL,

    synonyms TEXT NOT NULL,

    domain TEXT,

    confidence REAL DEFAULT 0.9,

    source TEXT DEFAULT 'MANUAL',

    usage_count INTEGER DEFAULT 0,

    create_time TEXT DEFAULT (datetime('now','localtime')),

    update_time TEXT DEFAULT (datetime('now','localtime')),

    entity_type TEXT,

    entity_id TEXT,

    UNIQUE(entity_type, entity_id, word)

);
CREATE TABLE IF NOT EXISTS sys_doc (
    "id" TEXT NOT NULL,
    "doc_key" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "category" TEXT NOT NULL DEFAULT 'general',
    "content" TEXT NOT NULL DEFAULT '',
    "sort" INTEGER NOT NULL DEFAULT 0,
    "status" INTEGER NOT NULL DEFAULT 1,
    "created_at" TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    "updated_at" TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    PRIMARY KEY ("id")
);
