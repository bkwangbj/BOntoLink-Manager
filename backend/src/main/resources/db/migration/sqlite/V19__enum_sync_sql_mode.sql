-- 枚举同步配置: 新增 SQL 模式字段
ALTER TABLE ont_enum_sync_config ADD COLUMN sync_source_type TEXT NOT NULL DEFAULT 'table'; -- table | sql
ALTER TABLE ont_enum_sync_config ADD COLUMN custom_sql TEXT;

-- 枚举项: 新增锁定字段 (is_sync_locked=1 时同步不覆盖/不删除)
ALTER TABLE ont_enum_items ADD COLUMN is_sync_locked INTEGER NOT NULL DEFAULT 0;
