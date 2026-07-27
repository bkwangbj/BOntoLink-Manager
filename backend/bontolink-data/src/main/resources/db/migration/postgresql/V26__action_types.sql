-- =====================================================================
-- V22  动作类型 (Action Types) 模块  —  PostgreSQL 方言
-- 与 sqlite/V22 同结构,pg 语法(VARCHAR / SMALLINT / TIMESTAMP)。
-- =====================================================================

-- ---------- 3.1 主表:重建 ont_class_action ----------
DROP TABLE IF EXISTS ont_class_action;

CREATE TABLE ont_class_action (
  id                        VARCHAR(64) PRIMARY KEY,          -- "class_action-" + UUID
  rid                       VARCHAR(255),
  api_name                  VARCHAR(128) NOT NULL UNIQUE,
  m_type                    SMALLINT,                         -- 大类 1对象/2链接/3函数/4Webhook/5接口/6通知
  action_type               SMALLINT,                         -- 细分 11-60
  object_class_id           VARCHAR(64),                      -- FK -> ont_class.id
  link_type_id              VARCHAR(64),                      -- FK -> ont_link_types.id
  function_code             VARCHAR(128),
  category_code             VARCHAR(128),
  show_on_detail            SMALLINT NOT NULL DEFAULT 0,
  show_on_batch             SMALLINT NOT NULL DEFAULT 0,
  button_text               VARCHAR(128),
  compile_status            SMALLINT NOT NULL DEFAULT 0,      -- 0未编译/1通过/2失败
  save_path                 VARCHAR(255),
  form_enabled              SMALLINT NOT NULL DEFAULT 0,
  submit_criteria_enabled   SMALLINT NOT NULL DEFAULT 0,
  status                    SMALLINT NOT NULL DEFAULT 0,      -- 0草稿/1已发布/2已停用
  current_version           VARCHAR(64),
  is_deleted                SMALLINT NOT NULL DEFAULT 0,
  icon                      VARCHAR(64),
  color                     VARCHAR(32),
  metadata                  TEXT,
  rdfs_label                VARCHAR(255) NOT NULL,
  rdfs_comment              TEXT,
  rdfs_see_also             TEXT,
  rdfs_defined_by           TEXT,
  create_time               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_action_object_class ON ont_class_action(object_class_id);
CREATE INDEX IF NOT EXISTS idx_action_link_type    ON ont_class_action(link_type_id);
CREATE INDEX IF NOT EXISTS idx_action_category     ON ont_class_action(category_code);

-- ---------- 3.2 规则 ----------
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

-- ---------- 3.3 规则-属性映射 ----------
CREATE TABLE ont_action_rule_property_mapping (
  id             VARCHAR(64) PRIMARY KEY,
  rule_id        VARCHAR(64) NOT NULL,
  property_code  VARCHAR(128),
  property_name  VARCHAR(255),
  prop_operator  VARCHAR(32),
  value_source   SMALLINT,
  value_content  TEXT,
  is_primary_key SMALLINT NOT NULL DEFAULT 0,
  is_required    SMALLINT NOT NULL DEFAULT 0,
  sort           INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_rule_prop_mapping_rule ON ont_action_rule_property_mapping(rule_id);

-- ---------- 3.4 规则条件 ----------
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

-- ---------- 3.5.1 链接规则配置 ----------
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

-- ---------- 3.5.2 链接属性映射 ----------
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

-- ---------- 3.6.1 表单分组 ----------
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

-- ---------- 3.6.2 表单参数 ----------
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

-- ---------- 3.7.1 参数显示配置 ----------
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

-- ---------- 3.7.2 对象型显示扩展 ----------
CREATE TABLE ont_action_form_display_object (
  id              VARCHAR(64) PRIMARY KEY,
  display_id      VARCHAR(64) NOT NULL,
  object_class_id VARCHAR(64),
  picker_type     VARCHAR(64),
  filter_config   TEXT,
  allow_create    SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_display_object_display ON ont_action_form_display_object(display_id);

-- ---------- 3.7.3 字符串型显示扩展 ----------
CREATE TABLE ont_action_form_display_string (
  id         VARCHAR(64) PRIMARY KEY,
  display_id VARCHAR(64) NOT NULL,
  input_type VARCHAR(32),
  max_length INTEGER,
  regex      TEXT,
  options    TEXT
);
CREATE INDEX IF NOT EXISTS idx_display_string_display ON ont_action_form_display_string(display_id);

-- ---------- 3.7.4 数值型显示扩展 ----------
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

-- ---------- 3.7.5 布尔型显示扩展 ----------
CREATE TABLE ont_action_form_display_boolean (
  id            VARCHAR(64) PRIMARY KEY,
  display_id    VARCHAR(64) NOT NULL,
  true_label    VARCHAR(64),
  false_label   VARCHAR(64),
  default_value SMALLINT
);
CREATE INDEX IF NOT EXISTS idx_display_boolean_display ON ont_action_form_display_boolean(display_id);

-- ---------- 3.8.1 覆盖块 ----------
CREATE TABLE ont_action_form_override_block (
  id                VARCHAR(64) PRIMARY KEY,
  action_id         VARCHAR(64) NOT NULL,
  block_name        VARCHAR(255),
  target_param_code VARCHAR(128),
  sort              INTEGER NOT NULL DEFAULT 0,
  status            SMALLINT NOT NULL DEFAULT 1
);
CREATE INDEX IF NOT EXISTS idx_override_block_action ON ont_action_form_override_block(action_id);

-- ---------- 3.8.2 覆盖项 ----------
CREATE TABLE ont_action_form_override_item (
  id              VARCHAR(64) PRIMARY KEY,
  block_id        VARCHAR(64) NOT NULL,
  override_type   VARCHAR(32),
  target_property VARCHAR(128),
  override_value  TEXT,
  sort            INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_override_item_block ON ont_action_form_override_item(block_id);

-- ---------- 3.8.3 覆盖条件组 ----------
CREATE TABLE ont_action_override_condition_group (
  id        VARCHAR(64) PRIMARY KEY,
  block_id  VARCHAR(64) NOT NULL,
  parent_id VARCHAR(64),
  logic_op  VARCHAR(16),
  sort      INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_override_cond_group_block ON ont_action_override_condition_group(block_id);

-- ---------- 3.8.4 覆盖条件项 ----------
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

-- ---------- 3.9.1 函数规则配置 ----------
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

-- ---------- 3.9.2 函数参数映射 ----------
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

-- ---------- 3.9.3 函数异常映射 ----------
CREATE TABLE ont_action_function_exception_map (
  id                 VARCHAR(64) PRIMARY KEY,
  function_config_id VARCHAR(64) NOT NULL,
  exception_code     VARCHAR(64),
  exception_message  TEXT,
  handle_strategy    VARCHAR(32),
  sort               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_function_exc_config ON ont_action_function_exception_map(function_config_id);

-- ---------- 3.10.1 通知规则配置 ----------
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

-- ---------- 3.11.1 Webhook 规则配置 ----------
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

-- ---------- 3.11.2 Webhook 入参映射 ----------
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

-- ---------- 3.12.1 提交标准配置 ----------
CREATE TABLE ont_action_submit_standard_config (
  id            VARCHAR(64) PRIMARY KEY,
  action_id     VARCHAR(64) NOT NULL,
  enabled       SMALLINT NOT NULL DEFAULT 0,
  validate_mode VARCHAR(16),
  error_message TEXT,
  config        TEXT
);
CREATE INDEX IF NOT EXISTS idx_submit_standard_action ON ont_action_submit_standard_config(action_id);

-- ---------- 3.12.2 提交条件节点(树) ----------
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

-- ---------- 3.13 表单全局配置 ----------
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

-- =====================================================================
-- 示例种子:2 个演示动作(挂在 class-wsc-01)
-- =====================================================================
INSERT INTO ont_class_action
  (id, rid, api_name, m_type, action_type, object_class_id, category_code,
   show_on_detail, show_on_batch, button_text, compile_status, form_enabled,
   status, current_version, icon, color, rdfs_label, rdfs_comment, create_time, update_time)
VALUES
  ('class_action-demo-create-01', 'ri.ont.action.create_soil_erosion_plot',
   'create_soil_erosion_plot', 1, 11, 'class-wsc-01', 'dom_water_soilconservation',
   0, 0, '新建监测样地', 1, 1,
   1, 'v1', 'plus', '#00b42a', '新建水土流失监测样地', '创建一条水土流失监测样地对象实例',
   '2026-07-21 12:10:00', '2026-07-21 12:10:00'),
  ('class_action-demo-modify-01', 'ri.ont.action.update_plot_status',
   'update_plot_status', 1, 12, 'class-wsc-01', 'dom_water_soilconservation',
   1, 0, '修改状态', 0, 1,
   0, 'v1', 'edit', '#165DFF', '修改样地监测状态', '修改监测样地的监测状态字段',
   '2026-07-21 12:12:00', '2026-07-21 12:12:00');
