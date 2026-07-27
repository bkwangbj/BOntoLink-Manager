-- =====================================================================
-- V22  动作类型 (Action Types) 模块  —  SQLite 方言
-- 依据《动作类型模块设计需求》第 3 章数据模型建全部表。
-- 主表 ont_class_action 由薄版重建为完整版(旧字段 class_id/action_kind/
-- display_name 已被总览/图谱只读引用,配套 Java 查询在本次同步改列名)。
-- =====================================================================

-- ---------- 3.1 主表:重建 ont_class_action ----------
DROP TABLE IF EXISTS ont_class_action;

CREATE TABLE ont_class_action (
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
CREATE INDEX idx_action_object_class ON ont_class_action(object_class_id);
CREATE INDEX idx_action_link_type    ON ont_class_action(link_type_id);
CREATE INDEX idx_action_category     ON ont_class_action(category_code);

-- ---------- 3.2 规则 ont_class_action_rule ----------
CREATE TABLE ont_class_action_rule (
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
CREATE INDEX idx_action_rule_action ON ont_class_action_rule(action_id);

-- ---------- 3.3 规则-属性映射 ont_action_rule_property_mapping ----------
CREATE TABLE ont_action_rule_property_mapping (
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
);
CREATE INDEX idx_rule_prop_mapping_rule ON ont_action_rule_property_mapping(rule_id);

-- ---------- 3.4 规则条件 ont_action_rule_condition ----------
CREATE TABLE ont_action_rule_condition (
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
CREATE INDEX idx_rule_condition_rule ON ont_action_rule_condition(rule_id);

-- ---------- 3.5.1 链接规则配置 ont_action_link_rule_config ----------
CREATE TABLE ont_action_link_rule_config (
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
CREATE INDEX idx_link_rule_config_rule ON ont_action_link_rule_config(rule_id);

-- ---------- 3.5.2 链接属性映射 ont_action_link_prop_mapping ----------
CREATE TABLE ont_action_link_prop_mapping (
  id            TEXT PRIMARY KEY,
  link_rule_id  TEXT NOT NULL,                                 -- FK -> ont_action_link_rule_config.id
  property_code TEXT,
  property_name TEXT,
  value_source  INTEGER,
  value_content TEXT,
  sort          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_link_prop_mapping_rule ON ont_action_link_prop_mapping(link_rule_id);

-- ---------- 3.6.1 表单分组 ont_action_form_section ----------
CREATE TABLE ont_action_form_section (
  id           TEXT PRIMARY KEY,
  action_id    TEXT NOT NULL,                                  -- FK -> ont_class_action.id
  section_name TEXT,
  title        TEXT,
  description  TEXT,
  collapsed    INTEGER NOT NULL DEFAULT 0,
  sort         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_form_section_action ON ont_action_form_section(action_id);

-- ---------- 3.6.2 表单参数 ont_action_form_param ----------
CREATE TABLE ont_action_form_param (
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
CREATE INDEX idx_form_param_action  ON ont_action_form_param(action_id);
CREATE INDEX idx_form_param_section ON ont_action_form_param(section_id);

-- ---------- 3.7.1 参数显示配置 ont_action_form_param_display ----------
CREATE TABLE ont_action_form_param_display (
  id           TEXT PRIMARY KEY,
  param_id     TEXT NOT NULL,                                  -- FK -> ont_action_form_param.id
  display_type TEXT,                                           -- 控件类型
  label        TEXT,
  help_text    TEXT,
  visible      INTEGER NOT NULL DEFAULT 1,
  editable     INTEGER NOT NULL DEFAULT 1,
  config       TEXT                                            -- JSON
);
CREATE INDEX idx_param_display_param ON ont_action_form_param_display(param_id);

-- ---------- 3.7.2 对象型显示扩展 ont_action_form_display_object ----------
CREATE TABLE ont_action_form_display_object (
  id              TEXT PRIMARY KEY,
  display_id      TEXT NOT NULL,                               -- FK -> ont_action_form_param_display.id
  object_class_id TEXT,
  picker_type     TEXT,                                        -- 选择器类型
  filter_config   TEXT,                                        -- JSON
  allow_create    INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_display_object_display ON ont_action_form_display_object(display_id);

-- ---------- 3.7.3 字符串型显示扩展 ont_action_form_display_string ----------
CREATE TABLE ont_action_form_display_string (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  input_type TEXT,                                             -- text/textarea/select/radio
  max_length INTEGER,
  regex      TEXT,
  options    TEXT                                              -- JSON
);
CREATE INDEX idx_display_string_display ON ont_action_form_display_string(display_id);

-- ---------- 3.7.4 数值型显示扩展 ont_action_form_display_number ----------
CREATE TABLE ont_action_form_display_number (
  id         TEXT PRIMARY KEY,
  display_id TEXT NOT NULL,
  min_value  REAL,
  max_value  REAL,
  step       REAL,
  precision  INTEGER,
  unit       TEXT
);
CREATE INDEX idx_display_number_display ON ont_action_form_display_number(display_id);

-- ---------- 3.7.5 布尔型显示扩展 ont_action_form_display_boolean ----------
CREATE TABLE ont_action_form_display_boolean (
  id            TEXT PRIMARY KEY,
  display_id    TEXT NOT NULL,
  true_label    TEXT,
  false_label   TEXT,
  default_value INTEGER
);
CREATE INDEX idx_display_boolean_display ON ont_action_form_display_boolean(display_id);

-- ---------- 3.8.1 覆盖块 ont_action_form_override_block ----------
CREATE TABLE ont_action_form_override_block (
  id                TEXT PRIMARY KEY,
  action_id         TEXT NOT NULL,                             -- FK -> ont_class_action.id
  block_name        TEXT,
  target_param_code TEXT,
  sort              INTEGER NOT NULL DEFAULT 0,
  status            INTEGER NOT NULL DEFAULT 1
);
CREATE INDEX idx_override_block_action ON ont_action_form_override_block(action_id);

-- ---------- 3.8.2 覆盖项 ont_action_form_override_item ----------
CREATE TABLE ont_action_form_override_item (
  id              TEXT PRIMARY KEY,
  block_id        TEXT NOT NULL,                               -- FK -> ont_action_form_override_block.id
  override_type   TEXT,                                        -- visible/required/value/options/readonly...
  target_property TEXT,
  override_value  TEXT,
  sort            INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_override_item_block ON ont_action_form_override_item(block_id);

-- ---------- 3.8.3 覆盖条件组 ont_action_override_condition_group ----------
CREATE TABLE ont_action_override_condition_group (
  id        TEXT PRIMARY KEY,
  block_id  TEXT NOT NULL,                                     -- FK -> ont_action_form_override_block.id
  parent_id TEXT,                                              -- 条件组树
  logic_op  TEXT,                                              -- and/or
  sort      INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_override_cond_group_block ON ont_action_override_condition_group(block_id);

-- ---------- 3.8.4 覆盖条件项 ont_action_override_condition_item ----------
CREATE TABLE ont_action_override_condition_item (
  id           TEXT PRIMARY KEY,
  group_id     TEXT NOT NULL,                                  -- FK -> ont_action_override_condition_group.id
  left_code    TEXT,
  operator     TEXT,
  right_value  TEXT,
  value_source INTEGER,
  sort         INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_override_cond_item_group ON ont_action_override_condition_item(group_id);

-- ---------- 3.9.1 函数规则配置 ont_action_function_rule_config ----------
CREATE TABLE ont_action_function_rule_config (
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
CREATE INDEX idx_function_config_action ON ont_action_function_rule_config(action_id);

-- ---------- 3.9.2 函数参数映射 ont_action_function_param_mapping ----------
CREATE TABLE ont_action_function_param_mapping (
  id                 TEXT PRIMARY KEY,
  function_config_id TEXT NOT NULL,                            -- FK -> ont_action_function_rule_config.id
  param_code         TEXT,
  param_name         TEXT,
  value_source       INTEGER,
  value_content      TEXT,
  direction          TEXT,                                     -- in/out
  sort               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_function_param_config ON ont_action_function_param_mapping(function_config_id);

-- ---------- 3.9.3 函数异常映射 ont_action_function_exception_map ----------
CREATE TABLE ont_action_function_exception_map (
  id                 TEXT PRIMARY KEY,
  function_config_id TEXT NOT NULL,                            -- FK -> ont_action_function_rule_config.id
  exception_code     TEXT,
  exception_message  TEXT,
  handle_strategy    TEXT,                                     -- ignore/rollback/retry
  sort               INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_function_exc_config ON ont_action_function_exception_map(function_config_id);

-- ---------- 3.10.1 通知规则配置 ont_action_notification_rule_config ----------
CREATE TABLE ont_action_notification_rule_config (
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
CREATE INDEX idx_notification_config_action ON ont_action_notification_rule_config(action_id);

-- ---------- 3.11.1 Webhook 规则配置 ont_action_webhook_rule_config ----------
CREATE TABLE ont_action_webhook_rule_config (
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
CREATE INDEX idx_webhook_config_action ON ont_action_webhook_rule_config(action_id);

-- ---------- 3.11.2 Webhook 入参映射 ont_action_webhook_input_mapping ----------
CREATE TABLE ont_action_webhook_input_mapping (
  id                TEXT PRIMARY KEY,
  webhook_config_id TEXT NOT NULL,                             -- FK -> ont_action_webhook_rule_config.id
  param_code        TEXT,
  value_source      INTEGER,
  value_content     TEXT,
  position          TEXT,                                      -- header/query/body
  sort              INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX idx_webhook_input_config ON ont_action_webhook_input_mapping(webhook_config_id);

-- ---------- 3.12.1 提交标准配置 ont_action_submit_standard_config ----------
CREATE TABLE ont_action_submit_standard_config (
  id            TEXT PRIMARY KEY,
  action_id     TEXT NOT NULL,                                 -- FK -> ont_class_action.id
  enabled       INTEGER NOT NULL DEFAULT 0,
  validate_mode TEXT,                                          -- all/any
  error_message TEXT,
  config        TEXT                                           -- JSON
);
CREATE INDEX idx_submit_standard_action ON ont_action_submit_standard_config(action_id);

-- ---------- 3.12.2 提交条件节点(树) ont_action_submit_condition_node ----------
CREATE TABLE ont_action_submit_condition_node (
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
CREATE INDEX idx_submit_condition_standard ON ont_action_submit_condition_node(standard_id);

-- ---------- 3.13 表单全局配置 ont_action_form_global_config ----------
CREATE TABLE ont_action_form_global_config (
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

-- =====================================================================
-- 示例种子:2 个演示动作(挂在 class-wsc-01 水土流失监测样地)
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
