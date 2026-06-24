-- 枚举同步配置: 新增"上级编码字段"映射, 用于从源表显式父级列建立层级关系
ALTER TABLE ont_enum_sync_config ADD COLUMN field_parent TEXT;
