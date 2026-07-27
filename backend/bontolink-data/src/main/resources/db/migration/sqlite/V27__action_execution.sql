-- =====================================================================
-- V23  动作执行日志 (Action Execution) — SQLite
-- 执行引擎每次运行(含试运行 dry_run)记录一条:输入参数 + 解析结果 + 状态。
-- =====================================================================
CREATE TABLE ont_action_execution (
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
CREATE INDEX idx_action_exec_action ON ont_action_execution(action_id);
CREATE INDEX idx_action_exec_time   ON ont_action_execution(execute_time);
