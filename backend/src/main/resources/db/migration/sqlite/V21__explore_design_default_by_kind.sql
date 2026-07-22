-- 默认看板/默认列表按 kind 区分:UNIQUE(class_id,name) → UNIQUE(class_id,name,kind)
-- 这样同一对象类型可同时存在「图表默认板(kind=query)」与「列表默认(kind=list)」两条 name='' 记录。
-- SQLite 不支持直接改约束,重建表迁移数据。
ALTER TABLE ont_explore_design RENAME TO ont_explore_design_v6;
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
INSERT INTO ont_explore_design (id, class_id, name, kind, config, created_at, updated_at)
  SELECT id, class_id, name, kind, config, created_at, updated_at FROM ont_explore_design_v6;
DROP TABLE ont_explore_design_v6;
CREATE INDEX IF NOT EXISTS idx_explore_design_class ON ont_explore_design(class_id);
