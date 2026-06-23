-- 物理表/视图元数据(从数据源同步落库, 对象类型新建向导"选择主表"用)
CREATE TABLE IF NOT EXISTS ont_physical_table (
  id              VARCHAR(46)  PRIMARY KEY,
  ds_id           VARCHAR(46)  NOT NULL,
  physical_table  VARCHAR(128) NOT NULL,
  display_name    VARCHAR(255),
  table_type      VARCHAR(16)  NOT NULL DEFAULT 'table',
  columns_json    TEXT,
  column_count    INTEGER      NOT NULL DEFAULT 0,
  status          SMALLINT     NOT NULL DEFAULT 1,
  sync_time       TIMESTAMP,
  create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(ds_id, physical_table)
);
CREATE INDEX IF NOT EXISTS idx_phys_table_ds ON ont_physical_table(ds_id);
