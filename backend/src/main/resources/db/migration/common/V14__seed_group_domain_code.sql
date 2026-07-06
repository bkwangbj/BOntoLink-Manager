-- 为现有 ont_biz_group 数据设置 domain_code（所属领域）
-- 只有明确属于特定业务领域的分组才设置，跨领域系统级分组保持 NULL

UPDATE ont_biz_group SET domain_code = 'dom_water_hydrology'
WHERE id = 'group-10000000-0000-0000-0000-000000000001';

UPDATE ont_biz_group SET domain_code = 'dom_water_hydrology'
WHERE id = 'group-10000000-0000-0000-0000-000000000002';

UPDATE ont_biz_group SET domain_code = 'dom_water_hydrology'
WHERE id = 'group-enum-water-monitor';
