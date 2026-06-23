-- 实例探索「看板/设计」持久化(替代原前端 localStorage)
-- name 为空('') = 该对象类型的默认看板(每 class 一条);非空 = 命名设计
CREATE TABLE IF NOT EXISTS ont_explore_design (
  id           TEXT PRIMARY KEY,                 -- "design-" + UUID
  class_id     TEXT NOT NULL,                    -- 关联对象类型 ont_class.id
  name         TEXT NOT NULL DEFAULT '',         -- 空=默认看板, 非空=命名设计
  kind         TEXT NOT NULL DEFAULT 'query',    -- query / list
  config       TEXT,                             -- 整个设计对象 JSON(含 charts/kw/pills/sort/viewMode 等)
  created_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  updated_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
  UNIQUE(class_id, name)
);
CREATE INDEX IF NOT EXISTS idx_explore_design_class ON ont_explore_design(class_id);
