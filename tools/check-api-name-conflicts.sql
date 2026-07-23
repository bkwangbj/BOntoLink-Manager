-- ==========================================
-- BOntoLink API_NAME 全局冲突检测脚本
-- ==========================================
-- 用途: 检测所有表中的 api_name 是否存在重复
-- 使用: sqlite3 bontolink.db < check-api-name-conflicts.sql
-- ==========================================

.mode column
.headers on
.width 50 30 20 10

-- 0. 汇总统计
SELECT '============ API_NAME 全局统计 ============' AS info;
SELECT
  'ont_class' AS table_name,
  COUNT(*) AS total_count,
  COUNT(DISTINCT api_name) AS unique_count,
  COUNT(*) - COUNT(DISTINCT api_name) AS duplicate_count
FROM ont_class WHERE api_name IS NOT NULL
UNION ALL
SELECT
  'ont_shared_properties',
  COUNT(*),
  COUNT(DISTINCT prop_code),
  COUNT(*) - COUNT(DISTINCT prop_code)
FROM ont_shared_properties WHERE prop_code IS NOT NULL
UNION ALL
SELECT
  'ont_value_types',
  COUNT(*),
  COUNT(DISTINCT api_name),
  COUNT(*) - COUNT(DISTINCT api_name)
FROM ont_value_types WHERE api_name IS NOT NULL
UNION ALL
SELECT
  'ont_enum_types',
  COUNT(*),
  COUNT(DISTINCT api_name),
  COUNT(*) - COUNT(DISTINCT api_name)
FROM ont_enum_types WHERE api_name IS NOT NULL
UNION ALL
SELECT
  'ont_enum_items',
  COUNT(*),
  COUNT(DISTINCT api_name),
  COUNT(*) - COUNT(DISTINCT api_name)
FROM ont_enum_items WHERE api_name IS NOT NULL
UNION ALL
SELECT
  'ont_interface',
  COUNT(*),
  COUNT(DISTINCT api_name),
  COUNT(*) - COUNT(DISTINCT api_name)
FROM ont_interface WHERE api_name IS NOT NULL
UNION ALL
SELECT
  'ont_link_types',
  COUNT(*),
  COUNT(DISTINCT link_type_code),
  COUNT(*) - COUNT(DISTINCT link_type_code)
FROM ont_link_types WHERE link_type_code IS NOT NULL
UNION ALL
SELECT
  'ont_struct_types',
  COUNT(*),
  COUNT(DISTINCT struct_code),
  COUNT(*) - COUNT(DISTINCT struct_code)
FROM ont_struct_types WHERE struct_code IS NOT NULL;

-- 1. 对象类型内部重复检测
SELECT '';
SELECT '============ 1. 对象类型(ont_class)内部重复 ============' AS info;
SELECT
  api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(COALESCE(display_name, rdfs_label), ' | ') AS names
FROM ont_class
WHERE api_name IS NOT NULL
GROUP BY api_name
HAVING COUNT(*) > 1;

-- 2. 共享属性内部重复检测
SELECT '';
SELECT '============ 2. 共享属性(ont_shared_properties)内部重复 ============' AS info;
SELECT
  prop_code AS api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(COALESCE(rdfs_label, prop_code), ' | ') AS names
FROM ont_shared_properties
WHERE prop_code IS NOT NULL
GROUP BY prop_code
HAVING COUNT(*) > 1;

-- 3. 值类型内部重复检测
SELECT '';
SELECT '============ 3. 值类型(ont_value_types)内部重复 ============' AS info;
SELECT
  api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(COALESCE(rdfs_label, api_name), ' | ') AS names
FROM ont_value_types
WHERE api_name IS NOT NULL
GROUP BY api_name
HAVING COUNT(*) > 1;

-- 4. 枚举类型内部重复检测
SELECT '';
SELECT '============ 4. 枚举类型(ont_enum_types)内部重复 ============' AS info;
SELECT
  api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(COALESCE(rdfs_label, api_name), ' | ') AS names
FROM ont_enum_types
WHERE api_name IS NOT NULL
GROUP BY api_name
HAVING COUNT(*) > 1;

-- 5. 枚举项内部重复检测（重点：同一枚举下的 api_name）
SELECT '';
SELECT '============ 5. 枚举项(ont_enum_items)内部重复 ============' AS info;
SELECT
  ei.api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(ei.id, ', ') AS ids,
  GROUP_CONCAT(ei.label || '(' || et.rdfs_label || ')', ' | ') AS names
FROM ont_enum_items ei
LEFT JOIN ont_enum_types et ON ei.enum_id = et.id
WHERE ei.api_name IS NOT NULL
GROUP BY ei.api_name
HAVING COUNT(*) > 1;

-- 6. 接口内部重复检测
SELECT '';
SELECT '============ 6. 接口(ont_interface)内部重复 ============' AS info;
SELECT
  api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(COALESCE(display_name, rdfs_label), ' | ') AS names
FROM ont_interface
WHERE api_name IS NOT NULL
GROUP BY api_name
HAVING COUNT(*) > 1;

-- 7. 链接类型内部重复检测
SELECT '';
SELECT '============ 7. 链接类型(ont_link_types)内部重复 ============' AS info;
SELECT
  link_type_code AS api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(link_name, ' | ') AS names
FROM ont_link_types
WHERE link_type_code IS NOT NULL
GROUP BY link_type_code
HAVING COUNT(*) > 1;

-- 8. 结构类型内部重复检测
SELECT '';
SELECT '============ 8. 结构类型(ont_struct_types)内部重复 ============' AS info;
SELECT
  struct_code AS api_name,
  COUNT(*) AS count,
  GROUP_CONCAT(id, ', ') AS ids,
  GROUP_CONCAT(COALESCE(rdfs_label, struct_code), ' | ') AS names
FROM ont_struct_types
WHERE struct_code IS NOT NULL
GROUP BY struct_code
HAVING COUNT(*) > 1;

-- 9. 跨表全局冲突检测（核心）
-- 注意: 类属性/接口属性/枚举项不参与全局检测（它们是局部唯一，允许跨容器重复）
SELECT '';
SELECT '============ 9. 跨表全局冲突检测 ============' AS info;
SELECT '提示: 类属性/接口属性/枚举项不参与此检测（局部唯一设计）' AS note;
WITH all_api_names AS (
  SELECT api_name, 'ont_class' AS source, id, display_name AS label FROM ont_class WHERE api_name IS NOT NULL
  UNION ALL
  SELECT prop_code, 'ont_shared_properties', id, rdfs_label FROM ont_shared_properties WHERE prop_code IS NOT NULL
  UNION ALL
  SELECT api_name, 'ont_value_types', id, rdfs_label FROM ont_value_types WHERE api_name IS NOT NULL
  UNION ALL
  SELECT api_name, 'ont_enum_types', id, rdfs_label FROM ont_enum_types WHERE api_name IS NOT NULL
  -- 🔸 ont_enum_items 已排除（枚举内唯一，不同枚举可重复）
  UNION ALL
  SELECT api_name, 'ont_interface', id, display_name FROM ont_interface WHERE api_name IS NOT NULL
  UNION ALL
  SELECT link_type_code, 'ont_link_types', id, link_name FROM ont_link_types WHERE link_type_code IS NOT NULL
  UNION ALL
  SELECT struct_code, 'ont_struct_types', id, rdfs_label FROM ont_struct_types WHERE struct_code IS NOT NULL
  -- 🔸 ont_class_property 已排除（类内唯一，不同类可重复）
  -- 🔸 ont_interface_property 已排除（接口内唯一，不同接口可重复）
)
SELECT
  api_name,
  COUNT(*) AS conflict_count,
  COUNT(DISTINCT source) AS affected_tables,
  GROUP_CONCAT(DISTINCT source, ' + ') AS conflict_sources,
  GROUP_CONCAT(id || '(' || source || ')', ' | ') AS detail_list
FROM all_api_names
GROUP BY api_name
HAVING COUNT(*) > 1 OR COUNT(DISTINCT source) > 1
ORDER BY conflict_count DESC, api_name;

-- 10. 命名规范检查（非法字符）
SELECT '';
SELECT '============ 10. 命名规范检查（包含非法字符）============' AS info;
SELECT
  'ont_class' AS table_name,
  api_name,
  id,
  'Contains uppercase/space/special chars' AS issue
FROM ont_class
WHERE api_name IS NOT NULL
  AND (
    api_name GLOB '*[A-Z]*'  -- 包含大写（注意：对象类型允许PascalCase,这里标记出来人工判断）
    OR api_name GLOB '*[ -.+]*'  -- 包含空格或特殊符号
    OR LENGTH(api_name) > 64  -- 超长
  )
UNION ALL
SELECT
  'ont_shared_properties',
  prop_code,
  id,
  CASE
    WHEN prop_code GLOB '*[A-Z]*' THEN 'Contains uppercase'
    WHEN prop_code GLOB '*[ -.+]*' THEN 'Contains special chars'
    WHEN LENGTH(prop_code) > 64 THEN 'Too long (>64)'
  END
FROM ont_shared_properties
WHERE prop_code IS NOT NULL
  AND (
    prop_code GLOB '*[A-Z]*'
    OR prop_code GLOB '*[ -.+]*'
    OR LENGTH(prop_code) > 64
  )
UNION ALL
SELECT
  'ont_value_types',
  api_name,
  id,
  CASE
    WHEN api_name GLOB '*[ -.+]*' THEN 'Contains special chars'
    WHEN LENGTH(api_name) > 64 THEN 'Too long (>64)'
  END
FROM ont_value_types
WHERE api_name IS NOT NULL
  AND (
    api_name GLOB '*[ -.+]*'
    OR LENGTH(api_name) > 64
  )
UNION ALL
SELECT
  'ont_enum_types',
  api_name,
  id,
  CASE
    WHEN api_name GLOB '*[ -.+]*' THEN 'Contains special chars'
    WHEN LENGTH(api_name) > 64 THEN 'Too long (>64)'
  END
FROM ont_enum_types
WHERE api_name IS NOT NULL
  AND (
    api_name GLOB '*[ -.+]*'
    OR LENGTH(api_name) > 64
  );

SELECT '';
SELECT '============ 检测完成 ============' AS info;
SELECT 'If no rows returned in sections 1-10, all checks passed!' AS result;
