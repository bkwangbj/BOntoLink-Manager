-- V35: 水利行业完整示范数据 —— 全模块覆盖
-- 业务场景: 黄河流域水文水资源综合管理
-- 模块覆盖:
--   1. 枚举类型 (4种模式) + 枚举项 (含多级) + 编码规则 + 同步配置
--   2. 值类型 (String/Integer/Decimal/Enum 多种约束) + 使用配置
--   3. 共享属性 (data/object/annotation/struct 四种 + OWL特性)
--   4. 结构类型 + 结构项
--   5. 接口属性 (3个接口各补属性)
--   6. 对象类型 (多领域, 含父子继承/union类表达式/is_thing/is_common)
--   7. 对象属性 (data/object/annotation/struct, 含主键/必填/多值/范围约束)
--   8. 接口-类绑定
--   9. 链接类型 (1:1/1:N/N:1/N:N) + 映射 + 知识图谱边
--  10. 类分组 (equivalent/disjoint) + 析取联合
--  11. 属性等价 + 属性互不相交
--  12. 数据集 (主+补充)
--  13. 属性格式化 (number/date/currency/text)
--  14. 类型类绑定 (geo/hubble/business/hierarchy)
--  15. 分组-类关系
-- 幂等: 全部 ON CONFLICT DO NOTHING，可重复执行

-- ============================================================
-- 1. 枚举类型 ont_enum_types
--    覆盖: general_single / general_multi / biz_single / biz_multi
-- ============================================================
INSERT INTO ont_enum_types (id, rid, api_name, category_code, enum_type, max_level, top_code, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('enum-types-v35-001','ri.ont.enum.library.wtr_station_type','wtr_station_type','dom_water_hydrology',   'general_single',1,NULL,'active','水文测站类型','水文测站功能类型: 水位/流量/雨量/蒸发/泥沙/水质/综合','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-002','ri.ont.enum.library.wtr_quality_grade','wtr_quality_grade','dom_water_environment','general_single',1,NULL,'active','水质等级','GB3838地表水水质分类: I~V类及劣V类','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-003','ri.ont.enum.library.wtr_alert_level','wtr_alert_level','dom_water_floodcontrol', 'general_single',1,NULL,'active','洪水预警级别','防洪预警四级: 蓝/黄/橙/红','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-004','ri.ont.enum.library.wtr_project_status','wtr_project_status','dom_water_engineering','biz_single',1,NULL,'active','工程状态','水利工程全生命周期状态','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-005','ri.ont.enum.library.wtr_pipe_material','wtr_pipe_material','dom_water_watersupply','general_single',1,NULL,'active','管材类型','供水管网管材分类标准','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-006','ri.ont.enum.library.wtr_obs_element','wtr_obs_element','dom_water_hydrology',    'biz_multi',2,NULL,'active','水文观测要素','水文站监测要素二级分类','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-007','ri.ont.enum.library.wtr_eco_bio_type','wtr_eco_bio_type','dom_water_ecology',    'biz_multi',2,NULL,'active','水生生物种类','水生态调查生物分类','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-types-v35-008','ri.ont.enum.library.wtr_basin_code','wtr_basin_code','dom_water_resource',       'general_multi',2,NULL,'inactive','流域分区编码','已停用 — 请使用 SSLY 枚举替代','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 1a. 枚举项 ont_enum_items
-- ============================================================
-- wtr_station_type (7项, 1级)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('enum-item-v35-st-01','enum-types-v35-001','ST01','water_level_station','水位站',NULL,1,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-st-02','enum-types-v35-001','ST02','flow_station','流量站',NULL,1,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-st-03','enum-types-v35-001','ST03','rainfall_station','雨量站',NULL,1,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-st-04','enum-types-v35-001','ST04','evaporation_station','蒸发站',NULL,1,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-st-05','enum-types-v35-001','ST05','sediment_station','泥沙站',NULL,1,5,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-st-06','enum-types-v35-001','ST06','quality_station','水质站',NULL,1,6,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-st-07','enum-types-v35-001','ST07','composite_station','综合站',NULL,1,7,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- wtr_quality_grade (6项, 1级)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('enum-item-v35-wq-01','enum-types-v35-002','I',  'grade_i',  'I类',  NULL,1,1,'active',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-wq-02','enum-types-v35-002','II', 'grade_ii', 'II类', NULL,1,2,'active',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-wq-03','enum-types-v35-002','III','grade_iii','III类',NULL,1,3,'active',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-wq-04','enum-types-v35-002','IV', 'grade_iv', 'IV类', NULL,1,4,'active',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-wq-05','enum-types-v35-002','V',  'grade_v',  'V类',  NULL,1,5,'active',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-wq-06','enum-types-v35-002','VI', 'grade_vi', '劣V类',NULL,1,6,'active',1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- wtr_alert_level (4项, 1级)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('enum-item-v35-al-01','enum-types-v35-003','AL04','alert_blue',  '蓝色预警',NULL,1,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-al-02','enum-types-v35-003','AL03','alert_yellow','黄色预警',NULL,1,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-al-03','enum-types-v35-003','AL02','alert_orange','橙色预警',NULL,1,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-al-04','enum-types-v35-003','AL01','alert_red',   '红色预警',NULL,1,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- wtr_project_status (5项, 1级)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('enum-item-v35-ps-01','enum-types-v35-004','PS1','planning',  '规划中',NULL,1,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-ps-02','enum-types-v35-004','PS2','under_const','在建',  NULL,1,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-ps-03','enum-types-v35-004','PS3','operating',  '运行',  NULL,1,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-ps-04','enum-types-v35-004','PS4','suspended',  '停用',  NULL,1,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-ps-05','enum-types-v35-004','PS5','scrapped',   '报废',  NULL,1,5,'inactive',0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- wtr_pipe_material (6项, 1级)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('enum-item-v35-pm-01','enum-types-v35-005','PM1','ductile_iron','球墨铸铁管',NULL,1,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-pm-02','enum-types-v35-005','PM2','steel_pipe',  '钢管',      NULL,1,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-pm-03','enum-types-v35-005','PM3','pe_pipe',     'PE管',      NULL,1,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-pm-04','enum-types-v35-005','PM4','concrete_pipe','混凝土管', NULL,1,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-pm-05','enum-types-v35-005','PM5','pvc_pipe',    'PVC管',     NULL,1,5,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-pm-06','enum-types-v35-005','PM6','composite',   '复合管',    NULL,1,6,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- wtr_obs_element (2级, 5父+13子)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  -- 一级父节点
  ('enum-item-v35-oe-01','enum-types-v35-006','HM',   'hydro_met',  '水文气象',NULL,1,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-02','enum-types-v35-006','WQ',   'water_qual', '水质',    NULL,1,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-03','enum-types-v35-006','WE',   'water_eco',  '水生态',  NULL,1,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-04','enum-types-v35-006','SD',   'sediment',   '泥沙',    NULL,1,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-05','enum-types-v35-006','IC',   'ice',        '冰凌',    NULL,1,5,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水文气象子项
  ('enum-item-v35-oe-11','enum-types-v35-006','HM01','wl',          '水位',   'HM',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-12','enum-types-v35-006','HM02','discharge',   '流量',   'HM',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-13','enum-types-v35-006','HM03','rainfall',    '降水量', 'HM',2,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-14','enum-types-v35-006','HM04','evaporation', '蒸发量', 'HM',2,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-15','enum-types-v35-006','HM05','temperature', '气温',   'HM',2,5,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水质子项
  ('enum-item-v35-oe-21','enum-types-v35-006','WQ01','do',          '溶解氧', 'WQ',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-22','enum-types-v35-006','WQ02','ph',          'pH值',   'WQ',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-23','enum-types-v35-006','WQ03','turbidity',   '浊度',   'WQ',2,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-24','enum-types-v35-006','WQ04','conductivity','电导率', 'WQ',2,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 泥沙子项
  ('enum-item-v35-oe-41','enum-types-v35-006','SD01','concentration','含沙量','SD',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-42','enum-types-v35-006','SD02','transport',   '输沙量', 'SD',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 冰凌子项
  ('enum-item-v35-oe-51','enum-types-v35-006','IC01','ice_thickness','冰厚',  'IC',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-oe-52','enum-types-v35-006','IC02','ice_cover',   '封冰率', 'IC',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 1b. 枚举编码规则 ont_enum_level_code_rule (多级枚举)
-- ============================================================
INSERT INTO ont_enum_level_code_rule (id, enum_id, code_name, rule_level, code_separator, code_len, total_len, fill_char, fill_pos, create_time, update_time) VALUES
  -- wtr_obs_element 两级: 父2位, 子4位
  ('enum-lcr-v35-oe-1','enum-types-v35-006','大类','1','',2,2,'0',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-lcr-v35-oe-2','enum-types-v35-006','小类','2','',2,4,'0',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- wtr_eco_bio_type 两级: 父2位, 子4位
  ('enum-lcr-v35-bt-1','enum-types-v35-007','界门纲','1','',2,2,'0',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-lcr-v35-bt-2','enum-types-v35-007','种群','2','',2,4,'0',1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 1c. 枚举同步配置 ont_enum_sync_config (3种 sync_source_type 演示)
-- ============================================================
INSERT INTO ont_enum_sync_config (id, enum_id, data_source_id, table_alias, table_name, field_code, field_name, field_sort, field_status, filter_sql, sync_mode, sync_strategy, sync_source_type, custom_sql, field_parent, create_time, update_time) VALUES
  -- table 模式: 测站类型从数据库表同步
  ('enum-sync-config-v35-001','enum-types-v35-001','datasource-00000000-mysql-001','水文测站类型表','t_station_type','type_code','type_name','sort_no','is_active',NULL,'level_diff','once','table',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- custom_sql 模式: 观测要素从带层级的自定义 SQL 同步
  ('enum-sync-config-v35-002','enum-types-v35-006','datasource-00000000-mysql-001','观测要素分类','t_element_dict','elem_code','elem_name','sort_no','status','elem_code IS NOT NULL','full_overwrite','daily','custom_sql','SELECT elem_code, parent_code, elem_name, sort_no, status FROM t_element_dict WHERE dict_type=''OBS_ELEMENT'' ORDER BY sort_no','parent_code','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (enum_id) DO NOTHING;

-- ============================================================
-- 2. 值类型使用配置 ont_valuetypes_usage_config
-- ============================================================
INSERT INTO ont_valuetypes_usage_config (id, max_select_level, allow_non_leaf, display_format, is_system_default, create_time, update_time) VALUES
  ('vt-usage-config-v35-01',1,0,'code_label',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('vt-usage-config-v35-02',2,1,'label',     0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('vt-usage-config-v35-03',0,0,'code',      0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 2a. 值类型 ont_value_types
--     覆盖: Enum / String-Regex / String-Length / Decimal / Integer
-- ============================================================
INSERT INTO ont_value_types (id, rid, api_name, category_code, base_type, constraint_type, constraint_config, enum_id, default_usage_config_id, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  -- Enum 类型
  ('value-types-v35-001','ri.ont.value.types.wtr_station_type','wtr_station_type_vt','dom_water_hydrology',   'String','Enum',NULL,'enum-types-v35-001','vt-usage-config-default',1,'测站类型','水文测站类型枚举值类型','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-002','ri.ont.value.types.wtr_quality_grade','wtr_quality_grade_vt','dom_water_environment','String','Enum',NULL,'enum-types-v35-002','vt-usage-config-v35-03',1,'水质等级','GB3838水质等级枚举值类型','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-003','ri.ont.value.types.wtr_alert_level','wtr_alert_level_vt','dom_water_floodcontrol', 'String','Enum',NULL,'enum-types-v35-003','vt-usage-config-default',1,'预警级别','洪水预警级别枚举值类型','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-004','ri.ont.value.types.wtr_project_status','wtr_project_status_vt','dom_water_engineering','String','Enum',NULL,'enum-types-v35-004','vt-usage-config-default',1,'工程状态','水利工程状态枚举值类型','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-005','ri.ont.value.types.wtr_pipe_material','wtr_pipe_material_vt','dom_water_watersupply','String','Enum',NULL,'enum-types-v35-005','vt-usage-config-default',1,'管材类型','供水管材分类枚举值类型','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-006','ri.ont.value.types.wtr_obs_element','wtr_obs_element_vt','dom_water_hydrology',    'String','Enum',NULL,'enum-types-v35-006','vt-usage-config-v35-02',1,'观测要素','水文观测要素二级枚举值类型','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- String-Regex 类型
  ('value-types-v35-010','ri.ont.value.types.station_code','station_code_vt','dom_water_hydrology',          'String','Regex','{"pattern":"^[A-Z]{2}[0-9]{8}$"}',NULL,NULL,1,'测站编码','国标水文测站编码 2位大写字母+8位数字','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-011','ri.ont.value.types.river_code','river_code_vt','dom_water_hydrology',              'String','Regex','{"pattern":"^[0-9]{8}$"}',NULL,NULL,1,'河道编码','8位数字河道编码','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- String-Length 类型
  ('value-types-v35-012','ri.ont.value.types.short_text','short_text_vt',NULL,                               'String','Length','{"min":1,"max":50}',NULL,NULL,1,'短文本','不超过50字的简短文本','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-013','ri.ont.value.types.long_text','long_text_vt',NULL,                                 'String','Length','{"min":0,"max":2000}',NULL,NULL,1,'长文本','不超过2000字的详细说明','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- Decimal 类型 (带范围)
  ('value-types-v35-020','ri.ont.value.types.water_level_m','water_level_vt','dom_water_hydrology',          'Decimal','Length','{"min":-50,"max":10000}',NULL,NULL,1,'水位(m)','黄海高程基准水位，单位:米','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-021','ri.ont.value.types.flow_rate_m3s','flow_rate_vt','dom_water_hydrology',            'Decimal','Length','{"min":0}',NULL,NULL,1,'流量(m³/s)','瞬时过水流量，单位:立方米/秒','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-022','ri.ont.value.types.rainfall_mm','rainfall_vt','dom_water_hydrology',               'Decimal','Length','{"min":0,"max":3000}',NULL,NULL,1,'降雨量(mm)','时段降雨量，单位:毫米','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-023','ri.ont.value.types.ph_value','ph_value_vt','dom_water_environment',                'Decimal','Length','{"min":0,"max":14}',NULL,NULL,1,'pH值','水体酸碱度 0-14','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-024','ri.ont.value.types.dissolved_oxygen','dissolved_oxygen_vt','dom_water_environment', 'Decimal','Length','{"min":0,"max":20}',NULL,NULL,1,'溶解氧(mg/L)','水体溶解氧浓度，单位:mg/L','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-025','ri.ont.value.types.area_km2','area_km2_vt','dom_water_soilconservation',           'Decimal','Length','{"min":0}',NULL,NULL,1,'面积(km²)','地理面积，单位:平方公里','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- Integer 类型
  ('value-types-v35-030','ri.ont.value.types.positive_int','positive_int_vt',NULL,                           'Integer','Length','{"min":1}',NULL,NULL,1,'正整数','大于0的整数','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-031','ri.ont.value.types.year_int','year_vt',NULL,                                       'Integer','Length','{"min":1800,"max":2100}',NULL,NULL,1,'年份','公历年份 1800-2100','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- Boolean 类型
  ('value-types-v35-040','ri.ont.value.types.bool_flag','bool_flag_vt',NULL,                                 'Boolean','RID','{}',NULL,NULL,1,'布尔标记','true/false 二值标记','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- DateTime 类型
  ('value-types-v35-050','ri.ont.value.types.datetime_std','datetime_vt',NULL,                               'DateTime','RID','{}',NULL,NULL,1,'日期时间','标准 ISO8601 日期时间','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('value-types-v35-051','ri.ont.value.types.date_only','date_vt',NULL,                                      'DateTime','RID','{"format":"yyyy-MM-dd"}',NULL,NULL,1,'日期','仅包含日期部分','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (api_name) DO NOTHING; -- (2级, 5父+10子)
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('enum-item-v35-bt-01','enum-types-v35-007','PP','phytoplankton','浮游植物',NULL,1,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-02','enum-types-v35-007','ZP','zooplankton', '浮游动物',NULL,1,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-03','enum-types-v35-007','MB','macroinverts','底栖生物',NULL,1,3,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-04','enum-types-v35-007','FI','fish',        '鱼类',    NULL,1,4,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-05','enum-types-v35-007','AQ','aquatic_plant','水生植物',NULL,1,5,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 子项
  ('enum-item-v35-bt-11','enum-types-v35-007','PP01','cyanobacteria','蓝藻',  'PP',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-12','enum-types-v35-007','PP02','diatom',      '硅藻',  'PP',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-21','enum-types-v35-007','ZP01','rotifer',     '轮虫',  'ZP',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-31','enum-types-v35-007','MB01','chironomid',  '摇蚊幼虫','MB',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-32','enum-types-v35-007','MB02','oligochaete', '颤蚓',  'MB',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-41','enum-types-v35-007','FI01','carp',        '鲤鱼',  'FI',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-42','enum-types-v35-007','FI02','grass_carp',  '草鱼',  'FI',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-51','enum-types-v35-007','AQ01','reed',        '芦苇',  'AQ',2,1,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-item-v35-bt-52','enum-types-v35-007','AQ02','lotus',       '荷花',  'AQ',2,2,'active',0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 1b. 枚举编码规则 ont_enum_level_code_rule
-- ============================================================
INSERT INTO ont_enum_level_code_rule (id, enum_id, code_name, rule_level, code_separator, code_len, total_len, fill_char, fill_pos, create_time, update_time) VALUES
  ('enum-lcr-v35-oe-1','enum-types-v35-006','大类','1','',2,2,'0',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-lcr-v35-oe-2','enum-types-v35-006','小类','2','',2,4,'0',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-lcr-v35-bt-1','enum-types-v35-007','界门纲','1','',2,2,'0',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-lcr-v35-bt-2','enum-types-v35-007','种群','2','',2,4,'0',1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 1c. 枚举同步配置 ont_enum_sync_config
-- ============================================================
INSERT INTO ont_enum_sync_config (id, enum_id, data_source_id, table_alias, table_name, field_code, field_name, field_sort, field_status, filter_sql, sync_mode, sync_strategy, sync_source_type, custom_sql, field_parent, create_time, update_time) VALUES
  ('enum-sync-config-v35-001','enum-types-v35-001','datasource-00000000-mysql-001','水文测站类型表','t_station_type','type_code','type_name','sort_no','is_active',NULL,'level_diff','once','table',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('enum-sync-config-v35-002','enum-types-v35-006','datasource-00000000-mysql-001','观测要素分类','t_element_dict','elem_code','elem_name','sort_no','status','elem_code IS NOT NULL','full_overwrite','daily','custom_sql','SELECT elem_code,parent_code,elem_name,sort_no,status FROM t_element_dict WHERE dict_type=''OBS_ELEMENT'' ORDER BY sort_no','parent_code','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (enum_id) DO NOTHING;

-- ============================================================
-- 2. 值类型使用配置 ont_valuetypes_usage_config
-- ============================================================
INSERT INTO ont_valuetypes_usage_config (id, max_select_level, allow_non_leaf, display_format, is_system_default, create_time, update_time) VALUES
  ('vt-usage-config-v35-01',1,0,'code_label',0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('vt-usage-config-v35-02',2,1,'label',     0,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('vt-usage-config-v35-03',0,0,'code',      0,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. 共享属性 ont_shared_properties
--    prop_type: data / object / annotation / struct
--    OWL特性演示: functional / transitive / asymmetric / irreflexive
-- ============================================================
INSERT INTO ont_shared_properties (id, rid, category_code, prop_code, prop_type, data_type, value_type, is_key, is_required, is_multi_valued_prop, is_range_constraint_prop, xsd_min_length, xsd_max_length, xsd_pattern, xsd_min_inclusive, xsd_max_inclusive, owl_functional, owl_inverse_functional, owl_transitive, owl_symmetric, owl_asymmetric, owl_reflexive, owl_irreflexive, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  -- data: 测站编码 (is_key, owl_functional, regex约束)
  ('shared-properties-v35-001','ri.ont.shared.props.station_code','dom_water_hydrology','station_code','data','xsd:string','value-types-v35-010',1,1,0,0,10,10,'^[A-Z]{2}[0-9]{8}$',NULL,NULL,1,0,0,0,0,0,0,1,'测站编码','国标水文测站唯一编码','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 河道编码 (owl_functional)
  ('shared-properties-v35-002','ri.ont.shared.props.river_code','dom_water_hydrology','river_code','data','xsd:string','value-types-v35-011',0,0,0,0,8,8,'^[0-9]{8}$',NULL,NULL,1,0,0,0,0,0,0,1,'河道编码','8位数字河道标识编码','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 水位 (范围约束, owl_functional)
  ('shared-properties-v35-003','ri.ont.shared.props.water_level','dom_water_hydrology','water_level','data','xsd:decimal','value-types-v35-020',0,0,0,1,NULL,NULL,NULL,'-50','10000',1,0,0,0,0,0,0,1,'水位(m)','黄海基面水位','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 流量 (范围>=0, owl_functional)
  ('shared-properties-v35-004','ri.ont.shared.props.flow_rate','dom_water_hydrology','flow_rate','data','xsd:decimal','value-types-v35-021',0,0,0,1,NULL,NULL,NULL,'0',NULL,1,0,0,0,0,0,0,1,'流量(m³/s)','过水流量','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 降雨量
  ('shared-properties-v35-005','ri.ont.shared.props.rainfall','dom_water_hydrology','rainfall','data','xsd:decimal','value-types-v35-022',0,0,0,1,NULL,NULL,NULL,'0','3000',1,0,0,0,0,0,0,1,'降雨量(mm)','时段降雨量','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 水温 (范围约束)
  ('shared-properties-v35-006','ri.ont.shared.props.water_temp','dom_water_environment','water_temp','data','xsd:decimal',NULL,0,0,0,1,NULL,NULL,NULL,'-10','50',1,0,0,0,0,0,0,1,'水温(℃)','水体温度','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: pH值
  ('shared-properties-v35-007','ri.ont.shared.props.ph_value','dom_water_environment','ph_value','data','xsd:decimal','value-types-v35-023',0,0,0,1,NULL,NULL,NULL,'0','14',1,0,0,0,0,0,0,1,'pH值','水体酸碱度','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 溶解氧
  ('shared-properties-v35-008','ri.ont.shared.props.dissolved_oxygen','dom_water_environment','dissolved_oxygen','data','xsd:decimal','value-types-v35-024',0,0,0,1,NULL,NULL,NULL,'0','20',1,0,0,0,0,0,0,1,'溶解氧(mg/L)','水体溶解氧浓度','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 观测时间 (is_required, owl_functional)
  ('shared-properties-v35-009','ri.ont.shared.props.obs_time','dom_water_hydrology','obs_time','data','xsd:dateTime','value-types-v35-050',0,1,0,0,NULL,NULL,NULL,NULL,NULL,1,0,0,0,0,0,0,1,'观测时间','监测发生时刻','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 工程状态枚举
  ('shared-properties-v35-010','ri.ont.shared.props.project_status','dom_water_engineering','project_status','data','xsd:string','value-types-v35-004',0,0,0,0,NULL,NULL,NULL,NULL,NULL,1,0,0,0,0,0,0,1,'工程状态','水利工程全生命周期状态','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 设计容量 (is_multi_valued_prop=1 演示多值)
  ('shared-properties-v35-011','ri.ont.shared.props.design_capacity','dom_water_engineering','design_capacity','data','xsd:decimal',NULL,0,0,1,0,NULL,NULL,NULL,'0',NULL,0,0,0,0,0,0,0,1,'设计容量','工程设计库容（多值序列）','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- data: 地理面积
  ('shared-properties-v35-012','ri.ont.shared.props.area_km2','dom_water_soilconservation','area_km2','data','xsd:decimal','value-types-v35-025',0,0,0,1,NULL,NULL,NULL,'0',NULL,1,0,0,0,0,0,0,1,'面积(km²)','地物覆盖面积','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- annotation: 数据质量备注
  ('shared-properties-v35-013','ri.ont.shared.props.data_quality_note',NULL,'data_quality_note','annotation',NULL,NULL,0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,'数据质量备注','数据来源与质量评级说明','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- object: 上级流域 (owl_transitive+asymmetric+irreflexive — 层级包含关系)
  ('shared-properties-v35-014','ri.ont.shared.props.parent_basin',NULL,'parent_basin','object',NULL,NULL,0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,1,0,1,0,1,1,'上级流域','所属上级流域单元','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- object: 所在河流 (owl_functional)
  ('shared-properties-v35-015','ri.ont.shared.props.located_river','dom_water_hydrology','located_river','object',NULL,NULL,0,0,0,0,NULL,NULL,NULL,NULL,NULL,1,0,0,0,0,0,0,1,'所在河流','测站/监测点所在河流','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- struct: 位置信息 (引用 Location 结构)
  ('shared-properties-v35-016','ri.ont.shared.props.location_struct','dom_water_hydrology','location_struct','struct',NULL,NULL,0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,'位置信息','经纬高综合位置结构','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (prop_code) DO NOTHING;

-- ============================================================
-- 4. 结构类型 ont_struct_types + 结构项 ont_struct_items
-- ============================================================
INSERT INTO ont_struct_types (id, struct_code, category_code, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('struct-types-v35-001','WaterStationInfo','dom_water_hydrology',1,'测站基本信息','测站编码+经纬度+高程','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('struct-types-v35-002','WaterQualityIndicator','dom_water_environment',1,'水质指标','pH+溶解氧+水温三要素','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('struct-types-v35-003','WaterFlowMeasure','dom_water_hydrology',1,'水流量测','水位+流量+观测时间','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (struct_code) DO NOTHING;

INSERT INTO ont_struct_items (id, struct_id, sort_no, prop_id) VALUES
  ('struct-items-v35-0101','struct-types-v35-001',1,'shared-properties-v35-001'),
  ('struct-items-v35-0102','struct-types-v35-001',2,'shared-properties-00000009'),
  ('struct-items-v35-0103','struct-types-v35-001',3,'shared-properties-00000010'),
  ('struct-items-v35-0104','struct-types-v35-001',4,'shared-properties-00000011'),
  ('struct-items-v35-0201','struct-types-v35-002',1,'shared-properties-v35-007'),
  ('struct-items-v35-0202','struct-types-v35-002',2,'shared-properties-v35-008'),
  ('struct-items-v35-0203','struct-types-v35-002',3,'shared-properties-v35-006'),
  ('struct-items-v35-0301','struct-types-v35-003',1,'shared-properties-v35-003'),
  ('struct-items-v35-0302','struct-types-v35-003',2,'shared-properties-v35-004'),
  ('struct-items-v35-0303','struct-types-v35-003',3,'shared-properties-v35-009')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 5. 接口属性 ont_interface_property (if-1/if-2/if-3)
-- ============================================================
-- monitorable_entity (if-1)
INSERT INTO ont_interface_property (id, rid, interface_id, api_name, prop_code, data_type, value_type, category_code, display_name, rdfs_label, is_required, status, create_time, update_time) VALUES
  ('interface-pro-v35-101','ri.ont.if-1.monitor_time',   'if-1','monitor_time',   'monitorTime',   'xsd:dateTime','value-types-v35-050',NULL,'监测时间',   'monitor_time',   1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-102','ri.ont.if-1.monitor_interval','if-1','monitor_interval','monitorInterval','xsd:integer','value-types-v35-030',NULL,'监测频次(min)','monitor_interval',0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-103','ri.ont.if-1.is_real_time',    'if-1','is_real_time',    'isRealTime',    'xsd:boolean','value-types-v35-040',NULL,'是否实时',    'is_real_time',    0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-104','ri.ont.if-1.device_id',       'if-1','device_id',       'deviceId',      'xsd:string', 'value-types-v35-012',NULL,'设备编号',    'device_id',       0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- geo_entity (if-2)
INSERT INTO ont_interface_property (id, rid, interface_id, api_name, prop_code, data_type, value_type, category_code, display_name, rdfs_label, is_required, status, create_time, update_time) VALUES
  ('interface-pro-v35-201','ri.ont.if-2.longitude',    'if-2','longitude',    'longitude',   'xsd:decimal',NULL,NULL,'经度',    'longitude',   1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-202','ri.ont.if-2.latitude',     'if-2','latitude',     'latitude',    'xsd:decimal',NULL,NULL,'纬度',    'latitude',    1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-203','ri.ont.if-2.altitude',     'if-2','altitude',     'altitude',    'xsd:decimal',NULL,NULL,'高程(m)', 'altitude',    0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-204','ri.ont.if-2.coord_system', 'if-2','coord_system', 'coordSystem', 'xsd:string', 'value-types-v35-012',NULL,'坐标系', 'coord_system', 0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-205','ri.ont.if-2.geom_type',    'if-2','geom_type',    'geomType',    'xsd:string', 'value-types-v35-012',NULL,'几何类型','geom_type',   0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- hydrology_measurable (if-3)
INSERT INTO ont_interface_property (id, rid, interface_id, api_name, prop_code, data_type, value_type, category_code, display_name, rdfs_label, is_required, status, create_time, update_time) VALUES
  ('interface-pro-v35-301','ri.ont.if-3.station_code',  'if-3','station_code',  'stationCode',  'xsd:string', 'value-types-v35-010','dom_water_hydrology','测站编码','station_code',  1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-302','ri.ont.if-3.obs_element',   'if-3','obs_element',   'obsElement',   'xsd:string', 'value-types-v35-006','dom_water_hydrology','观测要素','obs_element',   1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-303','ri.ont.if-3.obs_time',      'if-3','obs_time',      'obsTime',      'xsd:dateTime','value-types-v35-050','dom_water_hydrology','观测时间','obs_time',      1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-304','ri.ont.if-3.water_level_obs','if-3','water_level_obs','waterLevelObs','xsd:decimal','value-types-v35-020','dom_water_hydrology','水位(m)','water_level_obs',0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('interface-pro-v35-305','ri.ont.if-3.flow_rate_obs', 'if-3','flow_rate_obs', 'flowRateObs',  'xsd:decimal','value-types-v35-021','dom_water_hydrology','流量(m³/s)','flow_rate_obs',0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- 接口属性格式化
INSERT INTO ont_property_format (format_id, src_type, property_id, property_scope, format_enabled, format_type, decimal_places, use_thousand_sep, negative_mode, date_pattern, locale, create_time, update_time) VALUES
  ('property-format-v35-ip101',1,'interface-pro-v35-101','interface',1,'date',  0,0,3,'yyyy-MM-dd HH:mm:ss','zh-CN','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('property-format-v35-ip201',1,'interface-pro-v35-201','interface',1,'number',6,0,3,'yyyy-MM-dd',         'zh-CN','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('property-format-v35-ip202',1,'interface-pro-v35-202','interface',1,'number',6,0,3,'yyyy-MM-dd',         'zh-CN','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('property-format-v35-ip304',1,'interface-pro-v35-304','interface',1,'number',2,1,3,'yyyy-MM-dd',         'zh-CN','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('property-format-v35-ip305',1,'interface-pro-v35-305','interface',1,'number',3,1,3,'yyyy-MM-dd',         'zh-CN','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (format_id) DO NOTHING;

-- ============================================================
-- 6. 对象类型 ont_class
--    演示: 普通类 / 父子继承 / union类表达式 / is_thing / is_common
-- ============================================================
INSERT INTO ont_class (id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, rdfs_defined_by, description, icon, color, status, is_thing, is_nothing, is_common, parent_class_id, class_expr_type, class_expr_content, create_time, update_time) VALUES
  -- 水利工程领域: DamSafetyPoint (Reservoir 子类)
  ('class-v35-01','ri.ont.class.v35-01','DamSafetyPoint','w_wtr_eng','dom_water_engineering','大坝安全监测点','DamSafetyPoint','大坝坝体、坝基及库岸的位移/渗压/应力监测点位','水利公共本体库','大坝结构健康监测','station','#F53F3F',1,0,0,0,'class-00000000-0000-0000-0000-000000000004',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 防洪减灾: FloodForecastPoint (FloodGauge 子类)
  ('class-v35-02','ri.ont.class.v35-02','FloodForecastPoint','w_wtr_fld','dom_water_floodcontrol','洪水预报点','FloodForecastPoint','流域洪水演算节点，用于洪水预报模型驱动和预警发布','水利公共本体库','洪水预报控制断面','station','#FF7D00',1,0,0,0,'class-wfld-01',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 农田灌溉: IrrigationSchedule (新类)
  ('class-v35-03','ri.ont.class.v35-03','IrrigationSchedule','w_wtr_irr','dom_water_irrigation','灌溉计划','IrrigationSchedule','灌区年/季/旬灌溉用水计划，包含灌溉制度与配水方案','水利公共本体库','灌溉用水计划','calendar','#FADB14',1,0,0,0,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水生态: EcologySurveyPoint (新类)
  ('class-v35-04','ri.ont.class.v35-04','EcologySurveyPoint','w_wtr_wec','dom_water_ecology','生态调查点','EcologySurveyPoint','河湖水生态定期采样与生物调查的固定监测点位','水利公共本体库','水生态监测站位','leaf','#13C2C2',1,0,0,0,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水文: SedimentSample (HydrologyStation 子类视角的测次数据)
  ('class-v35-05','ri.ont.class.v35-05','SedimentSample','w_wtr_hyd','dom_water_hydrology','泥沙样品','SedimentSample','水文站采集的泥沙颗粒分析样品及含沙量测定记录','水利公共本体库','悬移质/推移质泥沙测次','droplet','#00B42A',1,0,0,0,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水资源: WaterWithdrawalRecord (新类)
  ('class-v35-06','ri.ont.class.v35-06','WaterWithdrawalRecord','w_wtr_wr','dom_water_resource','取水记录','WaterWithdrawalRecord','取水许可证持证单位的实际取水量计量与登记记录','水利公共本体库','实际取水量台账','list','#EB2F96',1,0,0,0,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水土保持: ErosionEvent (新类)
  ('class-v35-07','ri.ont.class.v35-07','ErosionEvent','w_wtr_sc','dom_water_soilconservation','侵蚀事件','ErosionEvent','坡面降雨侵蚀或沟道切割侵蚀的单次发生事件记录','水利公共本体库','土壤侵蚀发生事件','shield','#722ED1',1,0,0,0,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水利工程: DrainageOutfall (新类, 排涝)
  ('class-v35-08','ri.ont.class.v35-08','DrainageOutfall','w_wtr_eng','dom_water_engineering','排涝出口','DrainageOutfall','城市排涝泵站或重力自流排水的出口建筑物','水利公共本体库','城市排涝出口设施','factory','#165DFF',1,0,0,0,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水文: BasinBoundary (union类表达式 — 流域边界为河流+水库+渠道的联合)
  ('class-v35-09','ri.ont.class.v35-09','BasinBoundary','w_wtr_hyd','dom_water_hydrology','流域边界单元','BasinBoundary','由河流、水库、渠道等水体构成的流域空间边界单元','水利公共本体库','流域地理边界','grid','#FF7D00',1,0,0,0,NULL,'union','{"unionIds":["class-00000000-0000-0000-0000-000000000002","class-00000000-0000-0000-0000-000000000004","class-weng-05"]}','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 公共: WaterConservancyUnit (is_common=1, is_thing=1 — 水利管理单位通用类)
  ('class-v35-10','ri.ont.class.v35-10','WaterConservancyUnit','w_common',NULL,'水利管理单位','WaterConservancyUnit','负责水利工程运营管理、水资源调配的业务管理单位','水利公共本体库','水利管理机构','team','#86909C',1,1,0,1,NULL,NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 7. 对象类属性 ont_class_property
--    覆盖 prop_type: data / object / annotation / struct
--    覆盖 OWL 特性: functional / transitive / asymmetric / irreflexive
-- ============================================================
INSERT INTO ont_class_property
  (id, rid, class_id, category_code, api_name, prop_code, prop_type,
   data_type, value_type, display_name, rdfs_label, rdfs_comment,
   class_ds_id, physical_table, physical_column,
   is_primary, is_required, is_key, is_derived, is_multi_valued_prop,
   is_range_constraint_prop, range_class_id,
   xsd_min_inclusive, xsd_max_inclusive, xsd_pattern,
   xsd_min_length, xsd_max_length,
   owl_functional, owl_transitive, owl_asymmetric, owl_irreflexive,
   sort, status, create_time, update_time) VALUES
  -- class-v35-01 DamSafetyPoint: data属性
  ('class-prop-v35-0101',NULL,'class-v35-01','dom_water_engineering',
   'DamSafetyPoint.dam_id','dam_id','data',
   'xsd:string','vt-string-station-code-v35',
   '大坝编号','dam_id','大坝唯一标识编码',
   NULL,'t_dam_safety_point','dam_id',1,1,1,0,0,0,NULL,NULL,NULL,'^DA[0-9]{6}$',6,12,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0102',NULL,'class-v35-01','dom_water_engineering',
   'DamSafetyPoint.seepage_flow','seepage_flow','data',
   'xsd:decimal','vt-decimal-flow-rate-v35',
   '渗流量','seepage_flow','大坝测压管实测渗流量(L/s)',
   NULL,'t_dam_safety_point','seepage_flow',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0103',NULL,'class-v35-01','dom_water_engineering',
   'DamSafetyPoint.obs_time','obs_time','data',
   'xsd:dateTime','vt-datetime-v35',
   '观测时间','obs_time','本次安全观测记录时间',
   NULL,'t_dam_safety_point','obs_time',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,1,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- DamSafetyPoint: object属性 (所属管理单位, owl_functional)
  ('class-prop-v35-0104',NULL,'class-v35-01','dom_water_engineering',
   'DamSafetyPoint.managed_by','managed_by','object',
   NULL,NULL,
   '管理单位','managed_by','该大坝监测点的归属水利管理单位',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-v35-10',NULL,NULL,NULL,NULL,NULL,1,0,0,0,3,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- DamSafetyPoint: annotation属性
  ('class-prop-v35-0105',NULL,'class-v35-01','dom_water_engineering',
   'DamSafetyPoint.safety_remark','safety_remark','annotation',
   'xsd:string',NULL,
   '安全备注','safety_remark','人工填写的安全观测补充说明',
   NULL,NULL,NULL,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,4,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- DamSafetyPoint: struct属性
  ('class-prop-v35-0106',NULL,'class-v35-01','dom_water_engineering',
   'DamSafetyPoint.location','location','struct',
   NULL,'vt-struct-location-v35',
   '位置信息','location','大坝监测点的地理坐标结构体',
   NULL,NULL,NULL,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,5,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-02 FloodForecastPoint: data属性
  ('class-prop-v35-0201',NULL,'class-v35-02','dom_water_floodcontrol',
   'FloodForecastPoint.gauge_code','gauge_code','data',
   'xsd:string','vt-string-river-code-v35',
   '水尺编码','gauge_code','洪水预报水尺站唯一编码',
   NULL,'t_flood_forecast_point','gauge_code',1,1,1,0,0,0,NULL,NULL,NULL,'^[A-Z]{2}[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0202',NULL,'class-v35-02','dom_water_floodcontrol',
   'FloodForecastPoint.warning_level','warning_level','data',
   'xsd:decimal','vt-decimal-water-level-v35',
   '警戒水位','warning_level','该站点的洪水警戒水位阈值(m)',
   NULL,'t_flood_forecast_point','warning_level',0,0,0,0,0,0,NULL,'-20',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- FloodForecastPoint: object属性 (所属流域边界, owl_transitive+asymmetric+irreflexive)
  ('class-prop-v35-0203',NULL,'class-v35-02','dom_water_floodcontrol',
   'FloodForecastPoint.in_basin','in_basin','object',
   NULL,NULL,
   '所属流域','in_basin','该预报点归属的流域边界单元',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-v35-09',NULL,NULL,NULL,NULL,NULL,0,1,1,1,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-03 IrrigationSchedule: data属性
  ('class-prop-v35-0301',NULL,'class-v35-03','dom_water_irrigation',
   'IrrigationSchedule.schedule_code','schedule_code','data',
   'xsd:string','vt-string-station-code-v35',
   '方案编码','schedule_code','灌溉配水方案唯一编码',
   NULL,'t_irrigation_schedule','schedule_code',1,1,1,0,0,0,NULL,NULL,NULL,NULL,4,20,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0302',NULL,'class-v35-03','dom_water_irrigation',
   'IrrigationSchedule.planned_volume','planned_volume','data',
   'xsd:decimal','vt-decimal-flow-rate-v35',
   '计划水量','planned_volume','本方案计划配水总量(万m³)',
   NULL,'t_irrigation_schedule','planned_volume',0,1,0,0,0,0,NULL,'0','99999',NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0303',NULL,'class-v35-03','dom_water_irrigation',
   'IrrigationSchedule.status_flag','status_flag','data',
   'xsd:string','vt-enum-project-status-v35',
   '执行状态','status_flag','方案当前执行状态',
   NULL,'t_irrigation_schedule','status_flag',0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-04 EcologySurveyPoint: data属性
  ('class-prop-v35-0401',NULL,'class-v35-04','dom_water_ecology',
   'EcologySurveyPoint.site_code','site_code','data',
   'xsd:string','vt-string-station-code-v35',
   '站位编码','site_code','生态调查固定站位编码',
   NULL,'t_ecology_survey','site_code',1,1,1,0,0,0,NULL,NULL,NULL,'^EC[0-9]{6}$',6,10,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0402',NULL,'class-v35-04','dom_water_ecology',
   'EcologySurveyPoint.bio_type','bio_type','data',
   'xsd:string','vt-enum-eco-bio-v35',
   '生物类型','bio_type','本次调查的生物分类(多选)',
   NULL,'t_ecology_survey','bio_type',0,0,0,0,1,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-05 SedimentSample: data属性
  ('class-prop-v35-0501',NULL,'class-v35-05','dom_water_hydrology',
   'SedimentSample.sample_no','sample_no','data',
   'xsd:string','vt-string-station-code-v35',
   '样品编号','sample_no','泥沙样品唯一采集编号',
   NULL,'t_sediment_sample','sample_no',1,1,1,0,0,0,NULL,NULL,NULL,'^SD[0-9]{8}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0502',NULL,'class-v35-05','dom_water_hydrology',
   'SedimentSample.suspended_conc','suspended_conc','data',
   'xsd:decimal','vt-decimal-flow-rate-v35',
   '悬移质含沙量','suspended_conc','悬移质含沙量(kg/m³)',
   NULL,'t_sediment_sample','suspended_conc',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0503',NULL,'class-v35-05','dom_water_hydrology',
   'SedimentSample.is_valid','is_valid','data',
   'xsd:boolean','vt-boolean-v35',
   '样品有效性','is_valid','样品是否满足质量控制要求',
   NULL,'t_sediment_sample','is_valid',0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-06 WaterWithdrawalRecord: data属性
  ('class-prop-v35-0601',NULL,'class-v35-06','dom_water_resource',
   'WaterWithdrawalRecord.permit_no','permit_no','data',
   'xsd:string','vt-string-station-code-v35',
   '取水许可证号','permit_no','取水许可证编号',
   NULL,'t_water_withdrawal','permit_no',1,1,1,0,0,0,NULL,NULL,NULL,NULL,8,30,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0602',NULL,'class-v35-06','dom_water_resource',
   'WaterWithdrawalRecord.actual_volume','actual_volume','data',
   'xsd:decimal','vt-decimal-area-v35',
   '实际取水量','actual_volume','本期实际取水量(万m³)',
   NULL,'t_water_withdrawal','actual_volume',0,1,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0603',NULL,'class-v35-06','dom_water_resource',
   'WaterWithdrawalRecord.record_year','record_year','data',
   'xsd:integer','vt-integer-year-v35',
   '统计年份','record_year','取水量统计所属年份',
   NULL,'t_water_withdrawal','record_year',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-07 ErosionEvent: data属性
  ('class-prop-v35-0701',NULL,'class-v35-07','dom_water_soilconservation',
   'ErosionEvent.event_code','event_code','data',
   'xsd:string','vt-string-station-code-v35',
   '侵蚀事件编码','event_code','侵蚀事件唯一标识',
   NULL,'t_erosion_event','event_code',1,1,1,0,0,0,NULL,NULL,NULL,'^ER[0-9]{8}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0702',NULL,'class-v35-07','dom_water_soilconservation',
   'ErosionEvent.rainfall_amount','rainfall_amount','data',
   'xsd:decimal','vt-decimal-rainfall-v35',
   '降雨量','rainfall_amount','引发侵蚀的降雨总量(mm)',
   NULL,'t_erosion_event','rainfall_amount',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- ErosionEvent: annotation属性
  ('class-prop-v35-0703',NULL,'class-v35-07','dom_water_soilconservation',
   'ErosionEvent.field_note','field_note','annotation',
   'xsd:string',NULL,
   '现场记录','field_note','野外调查人员对该侵蚀事件的定性描述',
   NULL,NULL,NULL,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-08 DrainageOutfall: data属性
  ('class-prop-v35-0801',NULL,'class-v35-08','dom_water_engineering',
   'DrainageOutfall.outfall_code','outfall_code','data',
   'xsd:string','vt-string-station-code-v35',
   '出口编码','outfall_code','排涝出口设施唯一编码',
   NULL,'t_drainage_outfall','outfall_code',1,1,1,0,0,0,NULL,NULL,NULL,'^DO[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0802',NULL,'class-v35-08','dom_water_engineering',
   'DrainageOutfall.design_capacity','design_capacity','data',
   'xsd:decimal','vt-decimal-area-v35',
   '设计排涝能力','design_capacity','设计排涝流量(m³/s)',
   NULL,'t_drainage_outfall','design_capacity',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0803',NULL,'class-v35-08','dom_water_engineering',
   'DrainageOutfall.pipe_material','pipe_material','data',
   'xsd:string','vt-enum-pipe-material-v35',
   '管材类型','pipe_material','排水管道主体材质类型',
   NULL,'t_drainage_outfall','pipe_material',0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-09 BasinBoundary: data属性
  ('class-prop-v35-0901',NULL,'class-v35-09','dom_water_hydrology',
   'BasinBoundary.basin_code','basin_code','data',
   'xsd:string','vt-enum-basin-code-v35',
   '流域编码','basin_code','流域分类编码(可多选)',
   NULL,'t_basin_boundary','basin_code',1,1,1,0,1,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-0902',NULL,'class-v35-09','dom_water_hydrology',
   'BasinBoundary.area_km2','area_km2','data',
   'xsd:decimal','vt-decimal-area-v35',
   '流域面积','area_km2','流域控制面积(km²)',
   NULL,'t_basin_boundary','area_km2',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- BasinBoundary: struct属性
  ('class-prop-v35-0903',NULL,'class-v35-09','dom_water_hydrology',
   'BasinBoundary.outlet_location','outlet_location','struct',
   NULL,'vt-struct-location-v35',
   '出口位置','outlet_location','流域出口断面的地理坐标结构体',
   NULL,NULL,NULL,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- class-v35-10 WaterConservancyUnit: data属性
  ('class-prop-v35-1001',NULL,'class-v35-10','w_common',
   'WaterConservancyUnit.unit_code','unit_code','data',
   'xsd:string','vt-string-station-code-v35',
   '单位编码','unit_code','水利管理单位统一社会信用代码/内部编码',
   NULL,'t_wcu','unit_code',1,1,1,0,0,0,NULL,NULL,NULL,NULL,6,40,1,0,0,0,0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-prop-v35-1002',NULL,'class-v35-10','w_common',
   'WaterConservancyUnit.unit_name','unit_name','data',
   'xsd:string','vt-string-short-text-v35',
   '单位名称','unit_name','水利管理单位法定名称',
   NULL,'t_wcu','unit_name',0,1,0,0,0,0,NULL,NULL,NULL,NULL,2,100,0,0,0,0,1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- WaterConservancyUnit: object属性 (object属性 owl_functional)
  ('class-prop-v35-1003',NULL,'class-v35-10','w_common',
   'WaterConservancyUnit.parent_unit','parent_unit','object',
   NULL,NULL,
   '上级单位','parent_unit','水利管理单位的上级主管单位',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-v35-10',NULL,NULL,NULL,NULL,NULL,1,0,0,0,2,1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. 接口与对象类绑定 ont_interface_class
-- ============================================================
INSERT INTO ont_interface_class (id, interface_id, class_id, category_code, status, create_time, update_time) VALUES
  -- if-1 (可监测实体) — DamSafetyPoint, FloodForecastPoint, WaterWithdrawalRecord
  ('if-class-v35-0101','if-1','class-v35-01','dom_water_engineering',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0201','if-1','class-v35-02','dom_water_floodcontrol',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0601','if-1','class-v35-06','dom_water_resource',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- if-2 (地理实体) — DamSafetyPoint, FloodForecastPoint, EcologySurveyPoint, DrainageOutfall, BasinBoundary
  ('if-class-v35-0102','if-2','class-v35-01','dom_water_engineering',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0202','if-2','class-v35-02','dom_water_floodcontrol',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0401','if-2','class-v35-04','dom_water_ecology',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0801','if-2','class-v35-08','dom_water_engineering',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0901','if-2','class-v35-09','dom_water_hydrology',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- if-3 (水文可测量) — FloodForecastPoint, SedimentSample
  ('if-class-v35-0203','if-3','class-v35-02','dom_water_floodcontrol',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('if-class-v35-0501','if-3','class-v35-05','dom_water_hydrology',1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 9. 链接类型 + 映射 + 类链接
--    基数覆盖: one:one / one:many / many:one / many:many
--    含 is_data_source_rel=1 类型
-- ============================================================
INSERT INTO ont_link_types
  (id, link_type_id, rid, status,
   l_object_type_id, r_object_type_id,
   l_cardinality, r_cardinality,
   l_display_name, l_plural_name, r_display_name, r_plural_name,
   l_visibility, r_visibility, l_api_name, r_api_name,
   l_enabled, r_enabled, is_data_source_rel, rel_data_table,
   rdfs_label, rdfs_comment, category_code, created_at, updated_at) VALUES
  -- 1:1 — DamSafetyPoint ↔ WaterConservancyUnit (管理单位)
  ('link-types-v35-01','link-types-v35-01','ri.ont.link.v35-01','active',
   'class-v35-01','class-v35-10','one','one',
   '归属管理单位','归属管理单位','管理大坝监测点','管理大坝监测点',
   1,1,'managed_by_unit','manages_dam_point',1,1,0,NULL,
   '大坝监测点-管理单位(1:1)','每个大坝监测点由唯一一个管理单位负责，每个单位亦一一对应其直管点位',
   'dom_water_engineering','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 1:N — WaterConservancyUnit → IrrigationSchedule (制定)
  ('link-types-v35-02','link-types-v35-02','ri.ont.link.v35-02','active',
   'class-v35-10','class-v35-03','one','many',
   '制定灌溉方案','制定灌溉方案','方案制定单位','方案制定单位',
   1,1,'formulates_schedule','formulated_by_unit',1,1,0,NULL,
   '管理单位-灌溉方案(1:N)','一个管理单位可制定多个灌溉配水方案',
   'dom_water_irrigation','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- N:1 — SedimentSample → FloodForecastPoint (采样于)
  ('link-types-v35-03','link-types-v35-03','ri.ont.link.v35-03','active',
   'class-v35-05','class-v35-02','many','one',
   '采样站点','采样站点','包含泥沙样品','包含泥沙样品',
   1,1,'sampled_at_point','contains_sediment_sample',1,1,0,NULL,
   '泥沙样品-预报站(N:1)','多个泥沙样品可采自同一洪水预报点位',
   'dom_water_hydrology','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- N:N — EcologySurveyPoint ↔ BasinBoundary
  ('link-types-v35-04','link-types-v35-04','ri.ont.link.v35-04','active',
   'class-v35-04','class-v35-09','many','many',
   '位于流域','位于流域','包含生态调查点','包含生态调查点',
   1,1,'located_in_basin','contains_ecology_point',1,1,0,NULL,
   '生态调查点-流域边界(N:N)','一个调查点可跨多个流域边界，一个流域可包含多个调查点',
   'dom_water_ecology','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- is_data_source_rel=1 — WaterWithdrawalRecord → DamSafetyPoint (实测关联)
  ('link-types-v35-05','link-types-v35-05','ri.ont.link.v35-05','active',
   'class-v35-06','class-v35-01','one','many',
   '监测大坝','监测大坝','取水记录来源','取水记录来源',
   1,1,'monitors_dam_point','sourced_from_withdrawal',1,1,1,'t_rel_withdrawal_dam',
   '取水记录-大坝监测点(数据源关联)','通过物理关联表追踪取水记录对应的大坝安全监测点',
   'dom_water_resource','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- ErosionEvent → EcologySurveyPoint (影响调查点, 1:N)
  ('link-types-v35-06','link-types-v35-06','ri.ont.link.v35-06','active',
   'class-v35-07','class-v35-04','one','many',
   '影响生态站位','影响生态站位','记录侵蚀事件','记录侵蚀事件',
   1,1,'affects_ecology_point','impacted_by_erosion',1,1,0,NULL,
   '侵蚀事件-生态调查点(1:N)','一次侵蚀事件可影响下游多个生态调查点',
   'dom_water_soilconservation','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- DrainageOutfall → BasinBoundary (N:1)
  ('link-types-v35-07','link-types-v35-07','ri.ont.link.v35-07','active',
   'class-v35-08','class-v35-09','many','one',
   '排入流域','排入流域','包含排涝出口','包含排涝出口',
   1,1,'drains_into_basin','contains_drainage_outfall',1,1,0,NULL,
   '排涝出口-流域边界(N:1)','多个排涝出口排入同一流域边界单元',
   'dom_water_engineering','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ont_link_mappings (关联字段映射)
INSERT INTO ont_link_mappings (mapping_id, link_id, side, seq, object_field, join_table_column, created_at, updated_at) VALUES
  ('link-map-v35-0101','link-types-v35-01','l',1,'id','dam_point_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0102','link-types-v35-01','r',1,'id','unit_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0201','link-types-v35-02','l',1,'id','unit_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0202','link-types-v35-02','r',1,'id','schedule_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0301','link-types-v35-03','l',1,'id','sample_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0302','link-types-v35-03','r',1,'id','forecast_point_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0501','link-types-v35-05','l',1,'id','withdrawal_id','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('link-map-v35-0502','link-types-v35-05','r',1,'id','dam_point_id','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (mapping_id) DO NOTHING;

-- ont_class_link (对象类关联 — 直连关联类型)
INSERT INTO ont_class_link
  (id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, rdfs_label, rdfs_comment, status, create_time, update_time) VALUES
  ('class-link-v35-01','ri.ont.classlink.v35-01','IrrigationSchedule.covers_area',
   'class-v35-03','class-v35-09','many:one',
   '覆盖流域','covers_area','灌溉方案所覆盖的流域边界单元',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-link-v35-02','ri.ont.classlink.v35-02','EcologySurveyPoint.adjacent_outfall',
   'class-v35-04','class-v35-08','many:many',
   '邻近排涝出口','adjacent_outfall','生态调查点与地理相邻的排涝出口设施',1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-link-v35-03','ri.ont.classlink.v35-03','WaterConservancyUnit.manages_basin',
   'class-v35-10','class-v35-09','one:many',
   '管辖流域','manages_basin','管理单位负责管辖的流域边界单元',1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 10. 等价/不相交类组 ont_class_group
--     group_type: equivalent / disjoint
-- ============================================================
INSERT INTO ont_class_group
  (id, class_id, ref_class_id, group_type, rdfs_comment, rdfs_see_also, rdfs_defined_by, status, create_time, update_time) VALUES
  -- equivalent: BasinBoundary ≡ 流域边界(V20 HydrologicalBasin 如有则引用; 此处自引演示)
  ('class-group-v35-01','class-v35-09','class-v35-02','equivalent',
   '流域边界单元在概念上与洪水预报站所属流域具有等价关系(演示)',NULL,NULL,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- disjoint: ErosionEvent 与 IrrigationSchedule 不相交
  ('class-group-v35-02','class-v35-07','class-v35-03','disjoint',
   '侵蚀事件实例不能同时是灌溉配水方案实例',NULL,NULL,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- disjoint: SedimentSample 与 WaterWithdrawalRecord 不相交
  ('class-group-v35-03','class-v35-05','class-v35-06','disjoint',
   '泥沙样品与取水记录属于不相交的数据实体',NULL,NULL,1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 11. 不相交联合 ont_class_disjoint_union
--     BasinBoundary 由 HydrologyStation / Reservoir / Canal 不相交联合
-- ============================================================
INSERT INTO ont_class_disjoint_union
  (id, parent_class_id, sub_class_id, status, rdfs_comment, rdfs_see_also, rdfs_defined_by, create_time, update_time) VALUES
  ('class-du-v35-01','class-v35-09','class-00000000-0000-0000-0000-000000000002',1,
   'BasinBoundary 由 HydrologyStation/Reservoir/Canal 不相交联合而成',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-du-v35-02','class-v35-09','class-00000000-0000-0000-0000-000000000004',1,
   'BasinBoundary 由 HydrologyStation/Reservoir/Canal 不相交联合而成',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('class-du-v35-03','class-v35-09','class-weng-05',1,
   'BasinBoundary 由 HydrologyStation/Reservoir/Canal 不相交联合而成',NULL,NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 12. 属性等价 ont_property_equivalent
-- ============================================================
INSERT INTO ont_property_equivalent
  (id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment, create_time, update_time) VALUES
  -- DamSafetyPoint.obs_time ≡ SedimentSample.obs_time (共享属性视角)
  ('prop-equiv-v35-01','class-v35-01','class-prop-v35-0103','class-v35-05','class-prop-v35-0502',1,
   '大坝安全观测时间与泥沙采样时间在语义上具有等价关系','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- FloodForecastPoint.warning_level ≡ DamSafetyPoint.seepage_flow (观测告警阈值概念等价演示)
  ('prop-equiv-v35-02','class-v35-02','class-prop-v35-0202','class-v35-01','class-prop-v35-0102',1,
   '预报站警戒水位与大坝渗流量均属安全阈值类属性的等价演示','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 13. 属性不相交 ont_property_disjoint
-- ============================================================
INSERT INTO ont_property_disjoint
  (id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment, create_time, update_time) VALUES
  -- ErosionEvent.field_note 与 IrrigationSchedule.status_flag 概念不相交
  ('prop-disj-v35-01','class-v35-07','class-prop-v35-0703','class-v35-03','class-prop-v35-0303',1,
   '现场记录(定性描述)属性与方案状态(枚举)属性在语义上不相交','2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- WaterConservancyUnit.unit_code 与 WaterConservancyUnit.unit_name 值域不相交
  ('prop-disj-v35-02','class-v35-10','class-prop-v35-1001','class-v35-10','class-prop-v35-1002',1,
   '单位编码(代码值)与单位名称(自然语言文本)属性值域不相交','2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 14. 物理数据集映射 ont_class_ds
--     rel_type: main / supplementary
-- ============================================================
INSERT INTO ont_class_ds
  (id, class_id, ds_code, physical_table, table_label, rel_type, alias,
   pk_keys, join_on_keys, join_type, physical_fields, sort, status, create_time, update_time) VALUES
  -- DamSafetyPoint — main表
  ('class-ds-v35-01','class-v35-01','datasource-00000000-mysql-001',
   't_dam_safety_point','大坝安全监测点','main','dsp',
   '["id"]',NULL,NULL,
   '[{"field":"dam_id","label":"大坝编号"},{"field":"seepage_flow","label":"渗流量"},{"field":"obs_time","label":"观测时间"}]',
   0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- DamSafetyPoint — supplementary扩展表
  ('class-ds-v35-02','class-v35-01','datasource-00000000-mysql-001',
   't_dam_safety_ext','大坝安全扩展信息','supplementary','dsp_ext',
   NULL,'["dsp.id = dsp_ext.dam_point_id"]','LEFT',
   '[{"field":"crack_width","label":"裂缝宽度(mm)"},{"field":"settlement_mm","label":"沉降量(mm)"}]',
   1,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- FloodForecastPoint — main表
  ('class-ds-v35-03','class-v35-02','datasource-00000000-mysql-001',
   't_flood_forecast_point','洪水预报站','main','ffp',
   '["id"]',NULL,NULL,
   '[{"field":"gauge_code","label":"水尺编码"},{"field":"warning_level","label":"警戒水位"}]',
   0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- WaterWithdrawalRecord — main表
  ('class-ds-v35-04','class-v35-06','datasource-00000000-mysql-001',
   't_water_withdrawal','取水记录台账','main','wwr',
   '["id"]',NULL,NULL,
   '[{"field":"permit_no","label":"取水许可证号"},{"field":"actual_volume","label":"实际取水量"},{"field":"record_year","label":"统计年份"}]',
   0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- BasinBoundary — main表
  ('class-ds-v35-05','class-v35-09','datasource-00000000-mysql-001',
   't_basin_boundary','流域边界单元','main','bb',
   '["id"]',NULL,NULL,
   '[{"field":"basin_code","label":"流域编码"},{"field":"area_km2","label":"流域面积(km²)"}]',
   0,1,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 15. 属性格式化配置 ont_property_format
--     覆盖 format_type: number / date / currency / text
--     src_type=1 对象类属性 / src_type=2 共享属性
-- ============================================================
INSERT INTO ont_property_format
  (format_id, src_type, property_id, property_scope, format_enabled,
   format_type, decimal_places, use_thousand_sep, negative_mode,
   currency_symbol, accounting_align, date_pattern, time_pattern,
   locale, fraction_type, special_type, custom_format,
   text_force, text_max_length, text_regex,
   percent_auto_multiply, create_time, update_time, create_user) VALUES
  -- number: DamSafetyPoint.seepage_flow — 保留3位小数 + 千分位
  ('prop-fmt-v35-01',1,'class-prop-v35-0102','class',1,
   'number',3,1,'parentheses',NULL,0,NULL,NULL,'zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 10:00:00','2026-08-03 10:00:00','admin'),
  -- number: WaterWithdrawalRecord.actual_volume — 2位小数
  ('prop-fmt-v35-02',1,'class-prop-v35-0602','class',1,
   'number',2,1,'minus',NULL,0,NULL,NULL,'zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 10:00:00','2026-08-03 10:00:00','admin'),
  -- currency: IrrigationSchedule.planned_volume — 万元格式
  ('prop-fmt-v35-03',1,'class-prop-v35-0302','class',1,
   'currency',2,1,'parentheses','¥',1,NULL,NULL,'zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 10:00:00','2026-08-03 10:00:00','admin'),
  -- date: DamSafetyPoint.obs_time — yyyy-MM-dd HH:mm
  ('prop-fmt-v35-04',1,'class-prop-v35-0103','class',1,
   'date',NULL,0,NULL,NULL,0,'yyyy-MM-dd','HH:mm','zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 10:00:00','2026-08-03 10:00:00','admin'),
  -- text: WaterConservancyUnit.unit_name — 最大100字限制
  ('prop-fmt-v35-05',1,'class-prop-v35-1002','class',1,
   'text',NULL,0,NULL,NULL,0,NULL,NULL,'zh_CN',NULL,NULL,NULL,1,100,NULL,0,
   '2026-08-03 10:00:00','2026-08-03 10:00:00','admin'),
  -- number: BasinBoundary.area_km2 — 共享属性格式(src_type=2)
  ('prop-fmt-v35-06',2,'shared-properties-00000011','class',1,
   'number',2,1,'minus',NULL,0,NULL,NULL,'zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 10:00:00','2026-08-03 10:00:00','admin')
ON CONFLICT (format_id) DO NOTHING;

-- ============================================================
-- 16. 类型类绑定 ont_type_class_bind
--     applicable_type: property / relation / action
--     type_class_meta_id 参考: type-class-geo-altitude / type-class-hubble-icon
--                              type-class-hierarchy-parent / type-class-geo-latitude
-- ============================================================
INSERT INTO ont_type_class_bind
  (id, env, type_class_meta_id, applicable_type, property_owner_type, property_owner_id,
   property_id, link_type_id, action_type_id, suffix_custom, value,
   bind_deprecated, remark, create_user, update_user, created_at, updated_at) VALUES
  -- geo: DamSafetyPoint 经纬度 (通过 location struct 属性 — 绑定 altitude)
  ('bind-v35-01','prod','type-class-geo-altitude','property','object','class-v35-01',
   'class-prop-v35-0106',NULL,NULL,NULL,'120.5',0,'大坝监测点海拔高度绑定','admin',NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- geo: FloodForecastPoint latitude 绑定
  ('bind-v35-02','prod','type-class-geo-altitude','property','object','class-v35-02',
   'class-prop-v35-0202',NULL,NULL,NULL,'50.0',0,'预报站警戒水位高程绑定','admin',NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- hubble: WaterConservancyUnit 图标 URL 绑定
  ('bind-v35-03','prod','type-class-hubble-icon','property','object','class-v35-10',
   'class-prop-v35-1001',NULL,NULL,NULL,'icons/water-unit.svg',0,'管理单位图标 URL','admin',NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- hierarchy: WaterConservancyUnit 上下级关系绑定
  ('bind-v35-04','prod','type-class-hierarchy-parent','relation','object',NULL,
   NULL,'link-types-v35-02',NULL,NULL,NULL,0,'管理单位-灌溉方案层级关系','admin',NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- business: ErosionEvent 业务标签绑定
  ('bind-v35-05','prod','type-class-hubble-icon','property','object','class-v35-07',
   'class-prop-v35-0701',NULL,NULL,NULL,'icons/erosion.svg',0,'侵蚀事件图标','admin',NULL,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 17. 业务分组-对象类关联 ont_biz_group_class
-- ============================================================
INSERT INTO ont_biz_group_class (id, group_id, ref_id, group_type, category_code, g_sort, create_time, update_time) VALUES
  -- 水利工程组 (假设 group_id='biz-group-engineering' 对应已有分组)
  ('bgc-v35-01','biz-group-engineering','class-v35-01','class','dom_water_engineering',10,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('bgc-v35-02','biz-group-engineering','class-v35-08','class','dom_water_engineering',20,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 水文水资源组
  ('bgc-v35-03','biz-group-hydrology','class-v35-02','class','dom_water_floodcontrol',10,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('bgc-v35-04','biz-group-hydrology','class-v35-05','class','dom_water_hydrology',20,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('bgc-v35-05','biz-group-hydrology','class-v35-09','class','dom_water_hydrology',30,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 灌区水资源管理组
  ('bgc-v35-06','biz-group-irrigation','class-v35-03','class','dom_water_irrigation',10,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('bgc-v35-07','biz-group-irrigation','class-v35-06','class','dom_water_resource',20,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 生态水土保持组
  ('bgc-v35-08','biz-group-ecology','class-v35-04','class','dom_water_ecology',10,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  ('bgc-v35-09','biz-group-ecology','class-v35-07','class','dom_water_soilconservation',20,'2026-08-03 10:00:00','2026-08-03 10:00:00'),
  -- 公共管理组
  ('bgc-v35-10','biz-group-common','class-v35-10','class','w_common',10,'2026-08-03 10:00:00','2026-08-03 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- END V35__comprehensive_demo_seed.sql
-- 覆盖模块汇总:
--   枚举类型: general_single(3) / general_multi(1) / biz_single(1) / biz_multi(2)
--   值类型: Enum(6) / String-Regex(2) / String-Length(1) / Decimal(6) / Integer(2) / Boolean(1) / DateTime(2)
--   值类型使用配置: 3 种 (max_select_level=0/1/2)
--   共享属性: data(12) / annotation(1) / object(2, owl_transitive+functional) / struct(1) = 16
--   结构类型: 3 种 + 10 条结构项
--   接口属性: if-1/2/3 各 3-5 条, 含5条属性格式
--   对象类: 10 种 (含 union表达式/is_common/is_thing/parent_class)
--   对象类属性: 20+ 条, prop_type 全覆盖 (data/object/annotation/struct)
--   接口-类绑定: 10 条
--   链接类型: 7 条, 基数全覆盖 (1:1/1:N/N:1/N:N) + is_data_source_rel=1
--   链接映射: 8 条 (l/r 双侧)
--   类-类直连: 3 条
--   类等价/不相交组: 3 条
--   不相交联合成员: 3 条
--   属性等价: 2 条
--   属性不相交: 2 条
--   物理数据集映射: 5 条 (main + supplementary)
--   属性格式: 6 条 (number/currency/date/text, src_type 1 和 2)
--   类型类绑定: 5 条 (geo/hubble/hierarchy)
--   业务分组-类关联: 10 条
-- ============================================================
