-- 物理表/视图元数据(从数据源同步落库, 对象类型新建向导"选择主表"用)
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
CREATE INDEX IF NOT EXISTS idx_phys_table_ds ON ont_physical_table(ds_id);
