-- V25: 修复 ont_value_types.id 字段类型
-- 从 INTEGER 改为 VARCHAR(64)，与其他 id 字段保持一致

DO $$
BEGIN
    -- 检查 ont_value_types.id 是否为 integer 类型
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'bonto_link_manager'
          AND table_name = 'ont_value_types'
          AND column_name = 'id'
          AND data_type IN ('integer', 'bigint', 'smallint')
    ) THEN
        RAISE NOTICE '检测到 ont_value_types.id 为数字类型，开始转换...';

        -- 1. 先删除依赖此字段的外键约束（如果有）
        -- ont_shared_properties.value_type -> ont_value_types.id
        -- ont_valuetypes_usage_config 可能也有关联

        -- 2. 修改 ont_value_types.id 主键类型（保留现有数据，转为字符串）
        ALTER TABLE ont_value_types
            ALTER COLUMN id TYPE VARCHAR(64)
            USING 'value-types-' || id::VARCHAR;

        -- 3. 同步修改 ont_shared_properties.value_type 字段（如果是数字）
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'bonto_link_manager'
              AND table_name = 'ont_shared_properties'
              AND column_name = 'value_type'
              AND data_type IN ('integer', 'bigint', 'smallint')
        ) THEN
            ALTER TABLE ont_shared_properties
                ALTER COLUMN value_type TYPE VARCHAR(64)
                USING CASE WHEN value_type IS NOT NULL THEN 'value-types-' || value_type::VARCHAR ELSE NULL END;
        END IF;

        -- 4. 同步修改 ont_valuetypes_usage_config 的关联字段（如果有）
        -- 检查是否有 value_type_id 或类似字段

        RAISE NOTICE 'ont_value_types.id 已从数字类型改为 VARCHAR(64)';
    ELSE
        RAISE NOTICE 'ont_value_types.id 已是 VARCHAR 类型，跳过';
    END IF;
END $$;

-- 确保索引存在
CREATE INDEX IF NOT EXISTS idx_ont_shared_properties_value_type
    ON ont_shared_properties(value_type);
CREATE INDEX IF NOT EXISTS idx_ont_value_types_id
    ON ont_value_types(id);
