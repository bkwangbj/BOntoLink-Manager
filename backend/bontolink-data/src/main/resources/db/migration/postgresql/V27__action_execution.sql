-- =====================================================================
-- V23  动作执行日志 (Action Execution) — PostgreSQL
-- =====================================================================
CREATE TABLE ont_action_execution (
  id               VARCHAR(64) PRIMARY KEY,
  action_id        VARCHAR(64) NOT NULL,
  action_api_name  VARCHAR(128),
  object_class_id  VARCHAR(64),
  op_type          SMALLINT,
  input_params     TEXT,
  resolved_result  TEXT,
  status           VARCHAR(32) NOT NULL DEFAULT 'success',
  message          TEXT,
  dry_run          SMALLINT NOT NULL DEFAULT 1,
  executed_by      VARCHAR(64),
  execute_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_action_exec_action ON ont_action_execution(action_id);
CREATE INDEX IF NOT EXISTS idx_action_exec_time   ON ont_action_execution(execute_time);
