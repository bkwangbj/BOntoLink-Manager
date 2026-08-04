-- V36: 防洪减灾领域完整演示数据 —— 全量覆盖所有模块，统一归属 dom_water_floodcontrol
-- 通用数据（SQLite 和 PostgreSQL 共用）
--
-- 与 V35 互补：V35 覆盖全行业多领域，本脚本聚焦「防洪减灾」单一领域，
-- 使每个模块在该领域下都有一整套闭环数据（枚举/值类型/共享属性/结构类型/
-- 接口属性/对象类/对象类属性/接口类绑定/链接/分组/格式/类型类绑定/业务分组）。
-- 引用既有基线: class-wfld-01(FloodGauge) / class-wfld-02(EvacuationArea) /
--              class-v35-02(FloodForecastPoint) / class-v35-10(WaterConservancyUnit) /
--              if-1(monitorable_entity) / if-2(geo_entity) / if-7(flood_alertable)
-- 幂等: ON CONFLICT DO NOTHING，可重复执行

-- ============================================================
-- 0. 防洪减灾业务分组 ont_biz_group
-- ============================================================
INSERT INTO ont_biz_group (id, g_name, g_sort, icon, color, category_code, domain_code, description, parent_id, create_time, update_time) VALUES
  ('group-flood-control','防洪减灾对象类型',1,'shield','#F53F3F','dom_water_floodcontrol','dom_water_floodcontrol','防洪减灾领域对象类型集合','category-20000000-0000-0000-0000-000000000007','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('group-flood-interface','防洪预警接口组',2,'link','#F53F3F',NULL,NULL,'防洪预警领域接口集合',NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 1. 枚举类型 ont_enum_types
--    覆盖 general_single / biz_single / general_multi
-- ============================================================
INSERT INTO ont_enum_types (id, rid, api_name, category_code, enum_type, max_level, top_code, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('enum-types-v36-001','ri.ont.enum.v36-001','wtr_flood_level','dom_water_floodcontrol','general_single',1,'FL',1,'洪水量级','按洪峰重现期划分的洪水等级','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-types-v36-002','ri.ont.enum.v36-002','wtr_levee_status','dom_water_floodcontrol','general_single',1,'LS',1,'堤防状况','堤防险情与运行状况等级','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-types-v36-003','ri.ont.enum.v36-003','wtr_evacuation_state','dom_water_floodcontrol','biz_single',1,'ES',1,'撤离状态','人员转移撤离的组织实施状态','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-types-v36-004','ri.ont.enum.v36-004','wtr_flood_cause','dom_water_floodcontrol','general_multi',1,'FC',1,'洪水成因','引发洪水的自然与人为成因(可多选)','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- 1a. 枚举项 ont_enum_items
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  -- 洪水量级 (FL01-FL04)
  ('enum-item-v36-fl-01','enum-types-v36-001','FL01','extraordinary','特大洪水',NULL,1,1,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fl-02','enum-types-v36-001','FL02','large','大洪水',NULL,1,2,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fl-03','enum-types-v36-001','FL03','medium','中洪水',NULL,1,3,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fl-04','enum-types-v36-001','FL04','small','小洪水',NULL,1,4,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 堤防状况 (LS1-LS5)
  ('enum-item-v36-ls-01','enum-types-v36-002','LS01','good','状况良好',NULL,1,1,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-ls-02','enum-types-v36-002','LS02','seepage','渗漏',NULL,1,2,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-ls-03','enum-types-v36-002','LS03','piping','管涌',NULL,1,3,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-ls-04','enum-types-v36-002','LS04','landslide','滑坡',NULL,1,4,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-ls-05','enum-types-v36-002','LS05','breach','溃决险情',NULL,1,5,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 撤离状态 (ES1-ES5)
  ('enum-item-v36-es-01','enum-types-v36-003','ES01','standby','待命',NULL,1,1,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-es-02','enum-types-v36-003','ES02','organizing','组织撤离',NULL,1,2,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-es-03','enum-types-v36-003','ES03','in_progress','撤离进行中',NULL,1,3,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-es-04','enum-types-v36-003','ES04','completed','撤离完成',NULL,1,4,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-es-05','enum-types-v36-003','ES05','cancelled','解除/取消',NULL,1,5,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 洪水成因 (FC01-FC05)
  ('enum-item-v36-fc-01','enum-types-v36-004','FC01','storm','暴雨洪水',NULL,1,1,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fc-02','enum-types-v36-004','FC02','snowmelt','融雪洪水',NULL,1,2,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fc-03','enum-types-v36-004','FC03','dam_fail','溃坝洪水',NULL,1,3,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fc-04','enum-types-v36-004','FC04','storm_surge','风暴潮洪水',NULL,1,4,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-item-v36-fc-05','enum-types-v36-004','FC05','upstream','上游来水',NULL,1,5,'active',0,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- 1b. 枚举编码规则 ont_enum_level_code_rule (general_multi 补 rule)
INSERT INTO ont_enum_level_code_rule (id, enum_id, code_name, rule_level, code_separator, code_len, total_len, fill_char, fill_pos, create_time, update_time) VALUES
  ('enum-lcr-v36-fl-1','enum-types-v36-001','洪量级','1','',2,2,'0',0,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('enum-lcr-v36-fc-1','enum-types-v36-004','成因大类','1','',2,2,'0',0,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- 1c. 枚举同步配置 ont_enum_sync_config (从防洪预警达梦库同步堤防状况)
INSERT INTO ont_enum_sync_config (id, enum_id, data_source_id, table_alias, table_name, field_code, field_name, field_sort, field_status, filter_sql, sync_mode, sync_strategy, sync_source_type, custom_sql, field_parent, create_time, update_time) VALUES
  ('enum-sync-config-v36-001','enum-types-v36-002','datasource-0000000000-dm-001','堤防状况字典','t_levee_dict','levee_code','levee_name','sort_no','is_active',NULL,'level_diff','once','table',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (enum_id) DO NOTHING;

-- ============================================================
-- 2. 值类型使用配置 + 值类型 ont_value_types
--    约束类型覆盖: Enum / String-Regex / String-Length / Decimal / Integer
-- ============================================================
INSERT INTO ont_valuetypes_usage_config (id, max_select_level, allow_non_leaf, display_format, is_system_default, create_time, update_time) VALUES
  ('vt-usage-config-v36-01',1,0,'code_label',0,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ont_value_types (id, rid, api_name, category_code, base_type, constraint_type, constraint_config, enum_id, default_usage_config_id, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  -- Enum 类
  ('value-types-v36-001','ri.ont.value.types.v36.flood_level','wtr_flood_level_vt','dom_water_floodcontrol','String','Enum',NULL,'enum-types-v36-001','vt-usage-config-default',1,'洪水量级','洪水等级枚举值类型','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('value-types-v36-002','ri.ont.value.types.v36.levee_status','wtr_levee_status_vt','dom_water_floodcontrol','String','Enum',NULL,'enum-types-v36-002','vt-usage-config-default',1,'堤防状况','堤防险情状态枚举值类型','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('value-types-v36-003','ri.ont.value.types.v36.evacuation_state','wtr_evacuation_state_vt','dom_water_floodcontrol','String','Enum',NULL,'enum-types-v36-003','vt-usage-config-v36-01',1,'撤离状态','人员撤离组织实施状态枚举值类型','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('value-types-v36-004','ri.ont.value.types.v36.flood_cause','wtr_flood_cause_vt','dom_water_floodcontrol','String','Enum',NULL,'enum-types-v36-004','vt-usage-config-default',1,'洪水成因','洪水成因多选枚举值类型','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- String-Regex
  ('value-types-v36-010','ri.ont.value.types.v36.material_code','wtr_material_code_vt','dom_water_floodcontrol','String','Regex','{"pattern":"^[A-Z]{2}[0-9]{4}$"}',NULL,NULL,1,'物资编码','防汛物资统一编码','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- String-Length
  ('value-types-v36-011','ri.ont.value.types.v36.short_note','wtr_short_note_vt',NULL,'String','Length','{"min":1,"max":40}',NULL,NULL,1,'短说明','不超过40字的简短说明','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- Decimal
  ('value-types-v36-020','ri.ont.value.types.v36.crest_elevation','crest_elevation_vt','dom_water_floodcontrol','Decimal','Length','{"min":-50,"max":10000}',NULL,NULL,1,'洪峰高程(m)','洪峰水位黄海高程','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('value-types-v36-021','ri.ont.value.types.v36.flow_velocity','flow_velocity_vt','dom_water_floodcontrol','Decimal','Length','{"min":0,"max":20}',NULL,NULL,1,'流速(m/s)','断面平均流速','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('value-types-v36-022','ri.ont.value.types.v36.storage_capacity','storage_capacity_vt','dom_water_floodcontrol','Decimal','Length','{"min":0,"max":999999}',NULL,NULL,1,'库容(万m³)','水库库容','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- Integer
  ('value-types-v36-030','ri.ont.value.types.v36.personnel_count','personnel_count_vt','dom_water_floodcontrol','Integer','Length','{"min":0,"max":99999}',NULL,NULL,1,'人员数量','转移/值守人员数量','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 3. 共享属性 ont_shared_properties
--    prop_type: data / object / annotation / struct
--    OWL特性: functional / transitive+asymmetric+irreflexive
-- ============================================================
INSERT INTO ont_shared_properties
  (id, rid, category_code, prop_code, prop_type, data_type, value_type,
   is_key, is_required, is_multi_valued_prop, is_range_constraint_prop,
   xsd_min_length, xsd_max_length, xsd_pattern, xsd_min_inclusive, xsd_max_inclusive,
   owl_functional, owl_inverse_functional, owl_transitive, owl_symmetric,
   owl_asymmetric, owl_reflexive, owl_irreflexive,
   status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  -- data: 洪水量级 (枚举)
  ('shared-properties-v36-01','ri.ont.sp.v36-01','dom_water_floodcontrol','flood_level','data',
   'xsd:string','wtr_flood_level_vt',0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,
   '洪水量级','本次洪水的量级等级','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- data: 洪峰时刻 (日期时间)
  ('shared-properties-v36-02','ri.ont.sp.v36-02','dom_water_floodcontrol','crest_time','data',
   'xsd:dateTime','datetime_vt',0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,
   '洪峰时刻','洪峰通过断面的时间','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- data: 洪峰高程 (小数)
  ('shared-properties-v36-03','ri.ont.sp.v36-03','dom_water_floodcontrol','crest_elevation','data',
   'xsd:decimal','crest_elevation_vt',0,0,0,0,NULL,NULL,NULL,'-50','10000',0,0,0,0,0,0,0,1,
   '洪峰高程','洪峰水位高程值(m)','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- data: 堤防状况 (枚举)
  ('shared-properties-v36-04','ri.ont.sp.v36-04','dom_water_floodcontrol','levee_status','data',
   'xsd:string','wtr_levee_status_vt',0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,
   '堤防状况','堤段当前运行与险情状况','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- data: 巡堤时间
  ('shared-properties-v36-05','ri.ont.sp.v36-05','dom_water_floodcontrol','patrol_time','data',
   'xsd:dateTime','datetime_vt',0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,
   '巡堤时间','本次巡堤查险的开展时间','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- data: 物资编码 (Regex, is_key)
  ('shared-properties-v36-06','ri.ont.sp.v36-06','dom_water_floodcontrol','material_code','data',
   'xsd:string','wtr_material_code_vt',1,1,0,0,NULL,NULL,'^[A-Z]{2}[0-9]{4}$',NULL,NULL,1,0,0,0,0,0,0,1,
   '物资编码','防汛物资唯一编码','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- object: 下游转移区 (传递+非对称+非自反)
  ('shared-properties-v36-07','ri.ont.sp.v36-07','dom_water_floodcontrol','downstream_area','object',
   NULL,NULL,0,0,0,1,NULL,NULL,NULL,NULL,NULL,0,0,1,0,1,0,1,1,
   '下游转移区','下游需要转移撤离的安置区域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- object: 主管部门 (函数型)
  ('shared-properties-v36-08','ri.ont.sp.v36-08','dom_water_floodcontrol','governed_unit','object',
   NULL,NULL,0,1,0,1,NULL,NULL,NULL,NULL,NULL,1,0,0,0,0,0,0,1,
   '主管部门','负责该防洪对象的水利管理单位','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- annotation: 险情描述
  ('shared-properties-v36-09','ri.ont.sp.v36-09','dom_water_floodcontrol','danger_note','annotation',
   'xsd:string',NULL,0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,
   '险情描述','巡堤/值班人员对险情的定性描述','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- data: 洪峰流速
  ('shared-properties-v36-11','ri.ont.sp.v36-11','dom_water_floodcontrol','flow_velocity','data',
   'xsd:decimal','flow_velocity_vt',0,0,0,0,NULL,NULL,NULL,'0','20',0,0,0,0,0,0,0,1,
   '洪峰流速','洪峰断面平均流速(m/s)','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- struct: 洪峰信息
  ('shared-properties-v36-10','ri.ont.sp.v36-10','dom_water_floodcontrol','flood_peak_info','struct',
   NULL,'vt-struct-flood-peak-v36',0,0,0,0,NULL,NULL,NULL,NULL,NULL,0,0,0,0,0,0,0,1,
   '洪峰信息','洪峰高程/时刻/流速结构体','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (prop_code) DO NOTHING;

-- ============================================================
-- 4. 结构类型 ont_struct_types + ont_struct_items
-- ============================================================
INSERT INTO ont_struct_types (id, struct_code, category_code, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('struct-v36-01','v36_flood_peak_info','dom_water_floodcontrol',1,'洪峰信息','洪峰高程/时刻/流速组合结构','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('struct-v36-02','v36_patrol_record','dom_water_floodcontrol',1,'巡堤记录','巡堤时间/堤防状况/险情描述组合结构','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (struct_code) DO NOTHING;

INSERT INTO ont_struct_items (id, struct_id, sort_no, prop_id) VALUES
  ('struct-item-v36-01','struct-v36-01',1,'shared-properties-v36-03'),
  ('struct-item-v36-02','struct-v36-01',2,'shared-properties-v36-02'),
  ('struct-item-v36-03','struct-v36-01',3,'shared-properties-v36-11'),
  ('struct-item-v36-04','struct-v36-02',1,'shared-properties-v36-05'),
  ('struct-item-v36-05','struct-v36-02',2,'shared-properties-v36-04'),
  ('struct-item-v36-06','struct-v36-02',3,'shared-properties-v36-09')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 5. 接口属性 ont_interface_property —— 为 if-7 防洪预警接口补齐契约
-- ============================================================
INSERT INTO ont_interface_property (id, rid, interface_id, api_name, prop_code, data_type, value_type, category_code, display_name, rdfs_label, is_required, status, create_time, update_time) VALUES
  ('if-prop-v36-001',NULL,'if-7','FloodAlertable.warning_threshold','warning_threshold','xsd:decimal','crest_elevation_vt','dom_water_floodcontrol','预警阈值','warning_threshold',1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-prop-v36-002',NULL,'if-7','FloodAlertable.alert_color','alert_color','xsd:string','wtr_alert_level_vt','dom_water_floodcontrol','预警色','alert_color',1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-prop-v36-003',NULL,'if-7','FloodAlertable.issue_time','issue_time','xsd:dateTime','datetime_vt','dom_water_floodcontrol','发布时刻','issue_time',1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-prop-v36-004',NULL,'if-7','FloodAlertable.affected_area','affected_area','xsd:string','wtr_short_note_vt','dom_water_floodcontrol','影响区域','affected_area',0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- 接口属性格式化
INSERT INTO ont_property_format (format_id, src_type, property_id, property_scope, format_enabled, format_type, decimal_places, use_thousand_sep, negative_mode, date_pattern, locale, create_time, update_time, create_user) VALUES
  ('prop-fmt-v36-if-01',2,'shared-properties-v36-03','interface',1,'number',2,1,'minus','','zh_CN','2026-08-03 11:00:00','2026-08-03 11:00:00','admin'),
  ('prop-fmt-v36-if-02',2,'shared-properties-v36-02','interface',1,'date',NULL,0,NULL,'yyyy-MM-dd HH:mm','zh_CN','2026-08-03 11:00:00','2026-08-03 11:00:00','admin')
ON CONFLICT (format_id) DO NOTHING;

-- ============================================================
-- 6. 对象类 ont_class —— 10 个，全部归属 dom_water_floodcontrol
--    parent_class_id 引用既有类; 含 intersection 类表达式演示
-- ============================================================
INSERT INTO ont_class (id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, rdfs_defined_by, description, icon, color, status, is_thing, is_nothing, is_common, parent_class_id, class_expr_type, class_expr_content, create_time, update_time) VALUES
  -- 汛期值守点 (FloodGauge 子类)
  ('class-v36-01','ri.ont.class.v36-01','FloodSeasonWatch','w_wtr_fld','dom_water_floodcontrol','汛期值守点','FloodSeasonWatch','汛期由专人在洪水预报站驻守的值守点位','防洪减灾域','汛期人工值守监测点','eye','#F53F3F',1,1,0,0,'class-wfld-01',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 洪水预警记录 (FloodForecastPoint 子类)
  ('class-v36-02','ri.ont.class.v36-02','FloodWarningRecord','w_wtr_fld','dom_water_floodcontrol','洪水预警记录','FloodWarningRecord','针对某一防洪对象发布的洪水预警信息单','防洪减灾域','洪水预警发布记录','bell','#FF7D00',1,1,0,0,'class-v35-02',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 堤防段 (FloodGauge 子类)
  ('class-v36-03','ri.ont.class.v36-03','LeveeReach','w_wtr_fld','dom_water_floodcontrol','堤防段','LeveeReach','沿河堤防按桩号划分的基本巡查与防守单元','防洪减灾域','堤防巡查防守段','barrier','#EB2F96',1,1,0,0,'class-wfld-01',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 巡堤查险任务
  ('class-v36-04','ri.ont.class.v36-04','LeveePatrolTask','w_wtr_fld','dom_water_floodcontrol','巡堤查险任务','LeveePatrolTask','在堤防段上组织的巡堤查险班组任务','防洪减灾域','巡堤查险作业任务','clipboard','#00B42A',1,1,0,0,NULL,NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 溢洪道
  ('class-v36-05','ri.ont.class.v36-05','Spillway','w_wtr_fld','dom_water_floodcontrol','溢洪道','Spillway','水库泄洪的溢洪道建筑物','防洪减灾域','水库溢洪泄水设施','dam','#165DFF',1,1,0,0,NULL,NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 山洪灾害防御点
  ('class-v36-06','ri.ont.class.v36-06','FlashFloodPoint','w_wtr_fld','dom_water_floodcontrol','山洪灾害防御点','FlashFloodPoint','山洪灾害易发区域设置的监测防御点','防洪减灾域','山洪预警防御点','bolt','#722ED1',1,1,0,0,'class-wfld-01',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 防汛应急预案 (intersection: 调度指令 ∩ 撤离指令 的复合概念)
  ('class-v36-07','ri.ont.class.v36-07','FloodEmergencyPlan','w_wtr_fld','dom_water_floodcontrol','防汛应急预案','FloodEmergencyPlan','综合水库调度与人员撤离的防汛应急预案','防洪减灾域','流域防汛预案','book','#13C2C2',1,1,0,0,NULL,'intersection','{"intersectionIds":["class-v36-08","class-v36-09"]}','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 转移撤离指令 (关联 EvacuationArea)
  ('class-v36-08','ri.ont.class.v36-08','EvacuationCommand','w_wtr_fld','dom_water_floodcontrol','转移撤离指令','EvacuationCommand','向下游安置区发布的人员转移撤离指令','防洪减灾域','人员转移撤离指令','arrow-left','#FADB14',1,1,0,0,NULL,NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 水库调度指令
  ('class-v36-09','ri.ont.class.v36-09','ReservoirOperation','w_wtr_fld','dom_water_floodcontrol','水库调度指令','ReservoirOperation','防洪调度下达的水库泄蓄指令','防洪减灾域','水库防洪调度指令','settings','#F53F3F',1,1,0,0,NULL,NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 防汛物资库 (is_common=1, is_thing=1)
  ('class-v36-10','ri.ont.class.v36-10','FloodMaterialStore','w_wtr_fld','dom_water_floodcontrol','防汛物资库','FloodMaterialStore','集中储备防汛抢险物资的仓库节点','防洪减灾域','防汛物资储备点','inbox','#86909C',1,1,0,1,NULL,NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 7. 对象类属性 ont_class_property
--    prop_type: data / object / annotation / struct 全覆盖
--    OWL特性: functional / transitive+asymmetric+irreflexive
--    注: 该表 api_name 无 UNIQUE 约束, 冲突键用 (id)
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
  -- FloodSeasonWatch: data 属性
  ('class-prop-v36-0101',NULL,'class-v36-01','dom_water_floodcontrol',
   'FloodSeasonWatch.watch_code','watch_code','data',
   'xsd:string','vt-string-station-code-v35',
   '值守点编码','watch_code','汛期值守点唯一编码',
   NULL,'t_flood_watch','watch_code',1,1,1,0,0,0,NULL,NULL,NULL,'^WT[0-9]{6}$',6,12,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0102',NULL,'class-v36-01','dom_water_floodcontrol',
   'FloodSeasonWatch.watch_start','watch_start','data',
   'xsd:dateTime','vt-datetime-v35',
   '值守开始','watch_start','汛期值守开始时间',
   NULL,'t_flood_watch','watch_start',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodSeasonWatch: object 属性 (值守堤段, owl_functional)
  ('class-prop-v36-0103',NULL,'class-v36-01','dom_water_floodcontrol',
   'FloodSeasonWatch.guarded_reach','guarded_reach','object',
   NULL,NULL,
   '值守堤段','guarded_reach','该值守点负责防守的堤防段',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-v36-03',NULL,NULL,NULL,NULL,NULL,1,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodWarningRecord: data 属性
  ('class-prop-v36-0201',NULL,'class-v36-02','dom_water_floodcontrol',
   'FloodWarningRecord.warning_no','warning_no','data',
   'xsd:string','vt-string-station-code-v35',
   '预警编号','warning_no','洪水预警唯一编号',
   NULL,'t_flood_warning','warning_no',1,1,1,0,0,0,NULL,NULL,NULL,'^WN[0-9]{8}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0202',NULL,'class-v36-02','dom_water_floodcontrol',
   'FloodWarningRecord.issue_time','issue_time','data',
   'xsd:dateTime','vt-datetime-v35',
   '发布时刻','issue_time','预警信息发布时间',
   NULL,'t_flood_warning','issue_time',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,1,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodWarningRecord: object 属性 (触发的值守点)
  ('class-prop-v36-0203',NULL,'class-v36-02','dom_water_floodcontrol',
   'FloodWarningRecord.triggered_by','triggered_by','object',
   NULL,NULL,
   '触发值守点','triggered_by','触发该预警的值守监测点',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-v36-01',NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- LeveeReach: data 属性
  ('class-prop-v36-0301',NULL,'class-v36-03','dom_water_floodcontrol',
   'LeveeReach.reach_code','reach_code','data',
   'xsd:string','vt-string-station-code-v35',
   '堤段桩号','reach_code','堤防段桩号编码',
   NULL,'t_levee_reach','reach_code',1,1,1,0,0,0,NULL,NULL,NULL,'^LV[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0302',NULL,'class-v36-03','dom_water_floodcontrol',
   'LeveeReach.reach_length','reach_length','data',
   'xsd:decimal','vt-decimal-area-v35',
   '堤段长度','reach_length','堤防段长度(m)',
   NULL,'t_levee_reach','reach_length',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0303',NULL,'class-v36-03','dom_water_floodcontrol',
   'LeveeReach.status','status','data',
   'xsd:string','wtr_levee_status_vt',
   '堤防状况','status','堤防段当前状况(枚举)',
   NULL,'t_levee_reach','status',0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- LeveePatrolTask: data 属性
  ('class-prop-v36-0401',NULL,'class-v36-04','dom_water_floodcontrol',
   'LeveePatrolTask.task_no','task_no','data',
   'xsd:string','vt-string-station-code-v35',
   '任务编号','task_no','巡堤查险任务编号',
   NULL,'t_levee_patrol','task_no',1,1,1,0,0,0,NULL,NULL,NULL,'^PT[0-9]{8}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0402',NULL,'class-v36-04','dom_water_floodcontrol',
   'LeveePatrolTask.patrol_time','patrol_time','data',
   'xsd:dateTime','vt-datetime-v35',
   '巡堤时间','patrol_time','本次巡堤开展时间',
   NULL,'t_levee_patrol','patrol_time',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- LeveePatrolTask: object 属性 (巡查人员所属单位)
  ('class-prop-v36-0403',NULL,'class-v36-04','dom_water_floodcontrol',
   'LeveePatrolTask.patrolled_by','patrolled_by','object',
   NULL,NULL,
   '组织单位','patrolled_by','组织巡堤任务的单位',
   NULL,NULL,NULL,0,1,0,0,0,1,'class-v35-10',NULL,NULL,NULL,NULL,NULL,1,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- Spillway: data 属性
  ('class-prop-v36-0501',NULL,'class-v36-05','dom_water_floodcontrol',
   'Spillway.spillway_code','spillway_code','data',
   'xsd:string','vt-string-station-code-v35',
   '溢洪道编码','spillway_code','溢洪道设施唯一编码',
   NULL,'t_spillway','spillway_code',1,1,1,0,0,0,NULL,NULL,NULL,'^SP[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0502',NULL,'class-v36-05','dom_water_floodcontrol',
   'Spillway.max_discharge','max_discharge','data',
   'xsd:decimal','vt-decimal-flow-rate-v35',
   '最大泄量','max_discharge','最大泄流能力(m³/s)',
   NULL,'t_spillway','max_discharge',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- Spillway: object 属性 (所属调度, owl_functional)
  ('class-prop-v36-0503',NULL,'class-v36-05','dom_water_floodcontrol',
   'Spillway.belongs_to','belongs_to','object',
   NULL,NULL,
   '所属调度','belongs_to','溢洪道对应的水库调度指令',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-v36-09',NULL,NULL,NULL,NULL,NULL,1,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FlashFloodPoint: data 属性
  ('class-prop-v36-0601',NULL,'class-v36-06','dom_water_floodcontrol',
   'FlashFloodPoint.point_code','point_code','data',
   'xsd:string','vt-string-station-code-v35',
   '防御点编码','point_code','山洪防御点唯一编码',
   NULL,'t_flash_flood','point_code',1,1,1,0,0,0,NULL,NULL,NULL,'^FF[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0602',NULL,'class-v36-06','dom_water_floodcontrol',
   'FlashFloodPoint.community','community','data',
   'xsd:string','vt-string-short-text-v35',
   '防御社区','community','受山洪威胁的社区/村组',
   NULL,'t_flash_flood','community',0,0,0,0,0,0,NULL,NULL,NULL,NULL,2,50,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodEmergencyPlan: data 属性
  ('class-prop-v36-0701',NULL,'class-v36-07','dom_water_floodcontrol',
   'FloodEmergencyPlan.plan_code','plan_code','data',
   'xsd:string','vt-string-station-code-v35',
   '预案编码','plan_code','防汛预案唯一编码',
   NULL,'t_flood_plan','plan_code',1,1,1,0,0,0,NULL,NULL,NULL,'^FP[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0702',NULL,'class-v36-07','dom_water_floodcontrol',
   'FloodEmergencyPlan.version','version','data',
   'xsd:integer','vt-integer-year-v35',
   '预案版本','version','预案版本号',
   NULL,'t_flood_plan','version',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodEmergencyPlan: object 属性 (覆盖转移区, transitive+asymmetric+irreflexive)
  ('class-prop-v36-0703',NULL,'class-v36-07','dom_water_floodcontrol',
   'FloodEmergencyPlan.covers_area','covers_area','object',
   NULL,NULL,
   '覆盖转移区','covers_area','预案覆盖的下游转移安置区',
   NULL,NULL,NULL,0,0,0,0,0,1,'class-wfld-02',NULL,NULL,NULL,NULL,NULL,0,1,1,1,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- EvacuationCommand: data 属性
  ('class-prop-v36-0801',NULL,'class-v36-08','dom_water_floodcontrol',
   'EvacuationCommand.order_time','order_time','data',
   'xsd:dateTime','vt-datetime-v35',
   '指令时间','order_time','下达撤离指令的时间',
   NULL,'t_evacuation_cmd','order_time',0,1,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0802',NULL,'class-v36-08','dom_water_floodcontrol',
   'EvacuationCommand.evacuated_num','evacuated_num','data',
   'xsd:integer','personnel_count_vt',
   '转移人数','evacuated_num','本次实际转移人数',
   NULL,'t_evacuation_cmd','evacuated_num',0,0,0,0,0,0,NULL,'0','99999',NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- EvacuationCommand: annotation 属性
  ('class-prop-v36-0803',NULL,'class-v36-08','dom_water_floodcontrol',
   'EvacuationCommand.order_note','order_note','annotation',
   'xsd:string',NULL,
   '指令备注','order_note','撤离指令补充说明',
   NULL,NULL,NULL,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- ReservoirOperation: data 属性
  ('class-prop-v36-0901',NULL,'class-v36-09','dom_water_floodcontrol',
   'ReservoirOperation.op_code','op_code','data',
   'xsd:string','vt-string-station-code-v35',
   '调度指令编码','op_code','调度指令唯一编码',
   NULL,'t_reservoir_op','op_code',1,1,1,0,0,0,NULL,NULL,NULL,'^RO[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-0902',NULL,'class-v36-09','dom_water_floodcontrol',
   'ReservoirOperation.discharge_flow','discharge_flow','data',
   'xsd:decimal','vt-decimal-flow-rate-v35',
   '泄洪流量','discharge_flow','本次调度要求的下泄流量(m³/s)',
   NULL,'t_reservoir_op','discharge_flow',0,1,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodMaterialStore: data 属性
  ('class-prop-v36-1001',NULL,'class-v36-10','dom_water_floodcontrol',
   'FloodMaterialStore.store_code','store_code','data',
   'xsd:string','vt-string-station-code-v35',
   '物资库编码','store_code','防汛物资库唯一编码',
   NULL,'t_flood_store','store_code',1,1,1,0,0,0,NULL,NULL,NULL,'^MS[0-9]{6}$',NULL,NULL,1,0,0,0,0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-prop-v36-1002',NULL,'class-v36-10','dom_water_floodcontrol',
   'FloodMaterialStore.stock_qty','stock_qty','data',
   'xsd:integer','personnel_count_vt',
   '库存数量','stock_qty','当前物资库存数量',
   NULL,'t_flood_store','stock_qty',0,0,0,0,0,0,NULL,'0',NULL,NULL,NULL,NULL,0,0,0,0,1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- FloodMaterialStore: struct 属性
  ('class-prop-v36-1003',NULL,'class-v36-10','dom_water_floodcontrol',
   'FloodMaterialStore.peak_info','peak_info','struct',
   NULL,'vt-struct-flood-peak-v36',
   '洪峰信息','peak_info','物资库关联记录的洪峰结构体',
   NULL,NULL,NULL,0,0,0,0,0,0,NULL,NULL,NULL,NULL,NULL,NULL,0,0,0,0,2,1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. 接口与对象类绑定 ont_interface_class
--    if-7(flood_alertable 防洪预警) / if-1(monitorable) / if-2(geo)
-- ============================================================
INSERT INTO ont_interface_class (id, interface_id, class_id, category_code, status, create_time, update_time) VALUES
  -- if-7 防洪预警接口
  ('if-class-v36-0101','if-7','class-v36-01','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0201','if-7','class-v36-02','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0301','if-7','class-v36-03','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0601','if-7','class-v36-06','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0901','if-7','class-v36-09','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- if-1 可监测实体
  ('if-class-v36-0102','if-1','class-v36-01','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0202','if-1','class-v36-02','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0902','if-1','class-v36-09','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- if-2 地理实体
  ('if-class-v36-0302','if-2','class-v36-03','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0501','if-2','class-v36-05','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('if-class-v36-0602','if-2','class-v36-06','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 9. 链接类型 + 映射 + 类链接
--    基数覆盖: one:one / one:many / many:one / many:many + 数据源关联
-- ============================================================
INSERT INTO ont_link_types
  (id, link_type_id, rid, status,
   l_object_type_id, r_object_type_id,
   l_cardinality, r_cardinality,
   l_display_name, l_plural_name, r_display_name, r_plural_name,
   l_visibility, r_visibility, l_api_name, r_api_name,
   l_enabled, r_enabled, is_data_source_rel, rel_data_table,
   rdfs_label, rdfs_comment, category_code, created_at, updated_at) VALUES
  -- 1:1 — Spillway ↔ ReservoirOperation
  ('link-types-v36-01','link-types-v36-01','ri.ont.link.v36-01','active',
   'class-v36-05','class-v36-09','one','one',
   '对应调度','对应调度','控制溢洪道','控制溢洪道',
   1,1,'corresponding_op','controls_spillway',1,1,0,NULL,
   '溢洪道-水库调度(1:1)','每座溢洪道对应唯一水库调度指令，反之亦然',
   'dom_water_floodcontrol','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 1:N — LeveeReach → LeveePatrolTask
  ('link-types-v36-02','link-types-v36-02','ri.ont.link.v36-02','active',
   'class-v36-03','class-v36-04','one','many',
   '组织巡堤','组织巡堤','巡查堤段','巡查堤段',
   1,1,'organizes_patrol','patrols_reach',1,1,0,NULL,
   '堤防段-巡堤任务(1:N)','一个堤防段可组织多次巡堤查险任务',
   'dom_water_floodcontrol','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- N:1 — FloodWarningRecord → LeveeReach
  ('link-types-v36-03','link-types-v36-03','ri.ont.link.v36-03','active',
   'class-v36-02','class-v36-03','many','one',
   '预警堤段','预警堤段','收到预警','收到预警',
   1,1,'warns_reach','receives_warning',1,1,0,NULL,
   '洪水预警-堤防段(N:1)','多个预警记录针对同一堤防段发布',
   'dom_water_floodcontrol','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- N:N — EvacuationCommand ↔ EvacuationArea
  ('link-types-v36-04','link-types-v36-04','ri.ont.link.v36-04','active',
   'class-v36-08','class-wfld-02','many','many',
   '撤离至安置区','撤离至安置区','执行撤离指令','执行撤离指令',
   1,1,'evacuates_to_area','executes_command',1,1,0,NULL,
   '撤离指令-安置区(N:N)','一个指令可涉及多个安置区，一个安置区可承接多指令',
   'dom_water_floodcontrol','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- is_data_source_rel=1 — FloodMaterialStore → FloodSeasonWatch
  ('link-types-v36-05','link-types-v36-05','ri.ont.link.v36-05','active',
   'class-v36-10','class-v36-01','one','many',
   '保障值守点','保障值守点','物资来源库','物资来源库',
   1,1,'supplies_watch_point','sourced_from_store',1,1,1,'t_rel_store_watch',
   '物资库-值守点(数据源关联)','通过物理关联表追踪物资库对值守点的物资保障',
   'dom_water_floodcontrol','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 1:N — FloodEmergencyPlan → EvacuationCommand
  ('link-types-v36-06','link-types-v36-06','ri.ont.link.v36-06','active',
   'class-v36-07','class-v36-08','one','many',
   '下达撤离指令','下达撤离指令','依据预案执行','依据预案执行',
   1,1,'issues_command','under_plan',1,1,0,NULL,
   '防汛预案-撤离指令(1:N)','一份预案可下达多次撤离指令',
   'dom_water_floodcontrol','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ont_link_mappings
INSERT INTO ont_link_mappings (mapping_id, link_id, side, seq, object_field, join_table_column, created_at, updated_at) VALUES
  ('link-map-v36-0101','link-types-v36-01','l',1,'id','spillway_id','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('link-map-v36-0102','link-types-v36-01','r',1,'id','op_id','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('link-map-v36-0201','link-types-v36-02','l',1,'id','reach_id','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('link-map-v36-0202','link-types-v36-02','r',1,'id','task_id','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('link-map-v36-0501','link-types-v36-05','l',1,'id','store_id','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('link-map-v36-0502','link-types-v36-05','r',1,'id','watch_point_id','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (mapping_id) DO NOTHING;

-- ont_class_link (直连关联)
INSERT INTO ont_class_link
  (id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, rdfs_label, rdfs_comment, status, create_time, update_time) VALUES
  ('class-link-v36-01','ri.ont.classlink.v36-01','FloodWarningRecord.alerts_watch',
   'class-v36-02','class-v36-01','many:one',
   '预警值守点','alerts_watch','预警记录所对应的值守监测点',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-link-v36-02','ri.ont.classlink.v36-02','FlashFloodPoint.adjacent_reach',
   'class-v36-06','class-v36-03','many:many',
   '邻近堤段','adjacent_reach','山洪防御点邻近的堤防段',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-link-v36-03','ri.ont.classlink.v36-03','ReservoirOperation.backed_plan',
   'class-v36-09','class-v36-07','one:one',
   '依据预案','backed_plan','水库调度指令依据的防汛预案',1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 10. 类等价/不相交组 ont_class_group
-- ============================================================
INSERT INTO ont_class_group
  (id, class_id, ref_class_id, group_type, rdfs_comment, rdfs_see_also, rdfs_defined_by, status, create_time, update_time) VALUES
  -- equivalent: 洪水预警记录在语义上等价于值守点异常告警
  ('class-group-v36-01','class-v36-02','class-v36-01','equivalent',
   '洪水预警记录与汛期值守点告警在语义上等价(演示)',NULL,NULL,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- disjoint: 溢洪道 与 堤防段 不相交
  ('class-group-v36-02','class-v36-05','class-v36-03','disjoint',
   '溢洪道实例不能同时是堤防段实例',NULL,NULL,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- disjoint: 转移撤离指令 与 水库调度指令 不相交 (尽管预案交集两者)
  ('class-group-v36-03','class-v36-08','class-v36-09','disjoint',
   '撤离指令与调度指令是两类不同的指令实体',NULL,NULL,1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 11. 不相交联合 ont_class_disjoint_union
--     FloodSeasonWatch = 洪水预警 ∪ 汛期值守 (由两类不相交联合)
-- ============================================================
INSERT INTO ont_class_disjoint_union
  (id, parent_class_id, sub_class_id, status, rdfs_comment, rdfs_see_also, rdfs_defined_by, create_time, update_time) VALUES
  ('class-du-v36-01','class-v36-02','class-v36-01',1,
   '洪水预警记录由值守点与山洪点等不相交联合(演示)',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-du-v36-02','class-v36-02','class-v36-06',1,
   '洪水预警记录由值守点与山洪点等不相交联合(演示)',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-du-v36-03','class-v36-09','class-v36-05',1,
   '水库调度指令由溢洪道等泄洪设施的不相交联合(演示)',NULL,NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 12. 属性等价 ont_property_equivalent
-- ============================================================
INSERT INTO ont_property_equivalent
  (id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment, create_time, update_time) VALUES
  ('prop-equiv-v36-01','class-v36-02','class-prop-v36-0202','class-v36-01','class-prop-v36-0102',1,
   '预警发布时刻与值守开始时间在语义上具有等价关系(演示)','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('prop-equiv-v36-02','class-v36-05','class-prop-v36-0502','class-v36-09','class-prop-v36-0902',1,
   '溢洪道最大泄量与水库存调度下泄流量均属泄流能力类属性等价','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 13. 属性不相交 ont_property_disjoint
-- ============================================================
INSERT INTO ont_property_disjoint
  (id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment, create_time, update_time) VALUES
  ('prop-disj-v36-01','class-v36-08','class-prop-v36-0803','class-v36-08','class-prop-v36-0802',1,
   '指令备注(定性文本)与转移人数(数值)属性值域不相交','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('prop-disj-v36-02','class-v36-03','class-prop-v36-0302','class-v36-03','class-prop-v36-0303',1,
   '堤段长度(数值)与堤防状况(枚举)属性值域不相交','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 14. 物理数据集映射 ont_class_ds (main + supplementary)
-- ============================================================
INSERT INTO ont_class_ds
  (id, class_id, ds_code, physical_table, table_label, rel_type, alias,
   pk_keys, join_on_keys, join_type, physical_fields, sort, status, create_time, update_time) VALUES
  ('class-ds-v36-01','class-v36-01','datasource-0000000000-dm-001',
   't_flood_watch','汛期值守点','main','fw',
   '["id"]',NULL,NULL,
   '[{"field":"watch_code","label":"值守点编码"},{"field":"watch_start","label":"值守开始"}]',
   0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 补充表: 值守点扩展
  ('class-ds-v36-02','class-v36-01','datasource-0000000000-dm-001',
   't_flood_watch_ext','值守点扩展','supplementary','fw_ext',
   NULL,'["fw.id = fw_ext.watch_point_id"]','LEFT',
   '[{"field":"guard_count","label":"值守人数"},{"field":"shift","label":"班次"}]',
   1,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-ds-v36-03','class-v36-02','datasource-0000000000-dm-001',
   't_flood_warning','洪水预警记录','main','fwarn',
   '["id"]',NULL,NULL,
   '[{"field":"warning_no","label":"预警编号"},{"field":"issue_time","label":"发布时刻"}]',
   0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-ds-v36-04','class-v36-03','datasource-0000000000-dm-001',
   't_levee_reach','堤防段','main','lr',
   '["id"]',NULL,NULL,
   '[{"field":"reach_code","label":"堤段桩号"},{"field":"reach_length","label":"堤段长度"}]',
   0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class-ds-v36-05','class-v36-09','datasource-0000000000-dm-001',
   't_reservoir_op','水库调度指令','main','ro',
   '["id"]',NULL,NULL,
   '[{"field":"op_code","label":"调度指令编码"},{"field":"discharge_flow","label":"泄洪流量"}]',
   0,1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 15. 属性格式化配置 ont_property_format
--     src_type=1 对象类属性 (number / currency / date / text)
-- ============================================================
INSERT INTO ont_property_format
  (format_id, src_type, property_id, property_scope, format_enabled,
   format_type, decimal_places, use_thousand_sep, negative_mode,
   currency_symbol, accounting_align, date_pattern, time_pattern,
   locale, fraction_type, special_type, custom_format,
   text_force, text_max_length, text_regex,
   percent_auto_multiply, create_time, update_time, create_user) VALUES
  -- number: 溢洪道最大泄量 — 2位小数
  ('prop-fmt-v36-01',1,'class-prop-v36-0502','class',1,
   'number',2,1,'minus',NULL,0,NULL,NULL,'zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 11:00:00','2026-08-03 11:00:00','admin'),
  -- currency: 物资库库存价值 — 用 stock_qty 演示金额格式
  ('prop-fmt-v36-02',1,'class-prop-v36-1002','class',1,
   'currency',0,1,'parentheses','¥',1,NULL,NULL,'zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 11:00:00','2026-08-03 11:00:00','admin'),
  -- date: 巡堤时间
  ('prop-fmt-v36-03',1,'class-prop-v36-0402','class',1,
   'date',NULL,0,NULL,NULL,0,'yyyy-MM-dd','HH:mm','zh_CN',NULL,NULL,NULL,0,NULL,NULL,0,
   '2026-08-03 11:00:00','2026-08-03 11:00:00','admin'),
  -- text: 防御社区 — 最大50字
  ('prop-fmt-v36-04',1,'class-prop-v36-0602','class',1,
   'text',NULL,0,NULL,NULL,0,NULL,NULL,'zh_CN',NULL,NULL,NULL,1,50,NULL,0,
   '2026-08-03 11:00:00','2026-08-03 11:00:00','admin')
ON CONFLICT (format_id) DO NOTHING;

-- ============================================================
-- 16. 类型类绑定 ont_type_class_bind (geo / hubble / hierarchy)
-- ============================================================
INSERT INTO ont_type_class_bind
  (id, env, type_class_meta_id, applicable_type, property_owner_type, property_owner_id,
   property_id, link_type_id, action_type_id, suffix_custom, value,
   bind_deprecated, remark, create_user, update_user, created_at, updated_at) VALUES
  ('bind-v36-01','prod','type-class-geo-altitude','property','object','class-v36-01',
   'class-prop-v36-0101',NULL,NULL,NULL,'45.0',0,'值守点海拔绑定','admin',NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bind-v36-02','prod','type-class-geo-altitude','property','object','class-v36-06',
   'class-prop-v36-0601',NULL,NULL,NULL,'120.8',0,'山洪点海拔绑定','admin',NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bind-v36-03','prod','type-class-hubble-icon','property','object','class-v36-10',
   'class-prop-v36-1001',NULL,NULL,NULL,'icons/flood-store.svg',0,'物资库图标','admin',NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bind-v36-04','prod','type-class-hierarchy-parent','relation','object',NULL,
   NULL,'link-types-v36-02',NULL,NULL,NULL,0,'堤防段-巡堤任务层级关系','admin',NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bind-v36-05','prod','type-class-hubble-icon','property','object','class-v36-05',
   'class-prop-v36-0501',NULL,NULL,NULL,'icons/spillway.svg',0,'溢洪道图标','admin',NULL,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 17. 业务分组-对象类关联 ont_biz_group_class
--     group_type 约定: object_types / enum_types / value_types / interface
-- ============================================================
INSERT INTO ont_biz_group_class (id, group_id, ref_id, group_type, category_code, g_sort, create_time, update_time) VALUES
  ('bgc-v36-01','group-flood-control','class-v36-01','object_types','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-02','group-flood-control','class-v36-02','object_types','dom_water_floodcontrol',2,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-03','group-flood-control','class-v36-03','object_types','dom_water_floodcontrol',3,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-04','group-flood-control','class-v36-04','object_types','dom_water_floodcontrol',4,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-05','group-flood-control','class-v36-05','object_types','dom_water_floodcontrol',5,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-06','group-flood-control','class-v36-06','object_types','dom_water_floodcontrol',6,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-07','group-flood-control','class-v36-07','object_types','dom_water_floodcontrol',7,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-08','group-flood-control','class-v36-08','object_types','dom_water_floodcontrol',8,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-09','group-flood-control','class-v36-09','object_types','dom_water_floodcontrol',9,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-10','group-flood-control','class-v36-10','object_types','dom_water_floodcontrol',10,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  -- 枚举/值类型/接口分组
  ('bgc-v36-11','group-enum-water','enum-types-v36-001','enum_types','dom_water_floodcontrol',1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-12','group-enum-water','enum-types-v36-002','enum_types','dom_water_floodcontrol',2,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-13','group-enum-water','enum-types-v36-003','enum_types','dom_water_floodcontrol',3,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-14','group-enum-water','enum-types-v36-004','enum_types','dom_water_floodcontrol',4,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-15','group-vt-enum','value-types-v36-001','value_types',NULL,1,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-16','group-vt-enum','value-types-v36-002','value_types',NULL,2,'2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('bgc-v36-17','group-flood-interface','if-7','interface',NULL,1,'2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 18. 函数动作主表 ont_class_action（m_type=3 函数 / action_type=30）
--     防洪减灾 10 个对象类各挂一个函数执行动作
-- ============================================================
INSERT INTO ont_class_action
  (id, rid, api_name, m_type, action_type, object_class_id, function_code, category_code,
   show_on_detail, show_on_batch, button_text, compile_status, form_enabled,
   submit_criteria_enabled, status, current_version, is_deleted, icon, color,
   rdfs_label, rdfs_comment, rdfs_defined_by, create_time, update_time) VALUES
  ('class_action-v36-fn-01','ri.ont.action.v36.watch_check','v36_watch_check',3,30,'class-v36-01','fn.flood.watchCheck','dom_water_floodcontrol',1,0,'值守核验',1,1,0,1,'v1',0,'eye','#F53F3F','汛期值守到位核验','校验值守点值守时段是否覆盖并核验到位记录','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-02','ri.ont.action.v36.warning_grade','v36_warning_grade',3,30,'class-v36-02','fn.flood.warningGrade','dom_water_floodcontrol',1,0,'预警定级',1,1,0,1,'v1',0,'bell','#FF7D00','洪水预警等级判定','按洪峰高程与阈值对比自动判定预警等级(蓝/黄/橙/红)','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-03','ri.ont.action.v36.levee_risk','v36_levee_risk',3,30,'class-v36-03','fn.flood.leveeRisk','dom_water_floodcontrol',1,0,'险情评估',1,1,0,1,'v1',0,'barrier','#EB2F96','堤防险情评估','结合堤段长度与当前状况评估溃决风险等级','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-04','ri.ont.action.v36.patrol_rate','v36_patrol_rate',3,30,'class-v36-04','fn.flood.patrolRate','dom_water_floodcontrol',1,0,'达标统计',1,1,0,1,'v1',0,'clipboard','#00B42A','巡堤查险达标率统计','汇总巡堤任务与时间计算响应达标率','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-05','ri.ont.action.v36.spill_capacity','v36_spill_capacity',3,30,'class-v36-05','fn.flood.spillCapacity','dom_water_floodcontrol',1,0,'能力校验',1,1,0,1,'v1',0,'dam','#165DFF','溢洪道泄洪能力校验','校验调度下泄流量是否超过溢洪道最大泄流能力','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-06','ri.ont.action.v36.flash_risk','v36_flash_risk',3,30,'class-v36-06','fn.flood.flashRisk','dom_water_floodcontrol',1,0,'风险分级',1,1,0,1,'v1',0,'bolt','#722ED1','山洪灾害风险分级','按洪水量级与防御社区规模划分山洪风险等级','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-07','ri.ont.action.v36.plan_score','v36_plan_score',3,30,'class-v36-07','fn.flood.planScore','dom_water_floodcontrol',1,0,'预案评分',1,1,0,1,'v1',0,'book','#13C2C2','防汛预案完备性评分','按预案版本与覆盖要素评估预案完备性','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-08','ri.ont.action.v36.evacuate_rate','v36_evacuate_rate',3,30,'class-v36-08','fn.flood.evacuateRate','dom_water_floodcontrol',1,0,'进度核算',1,1,0,1,'v1',0,'arrow-left','#FADB14','转移撤离进度核算','核算指令发布后实际转移人数与目标进度','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-09','ri.ont.action.v36.op_safe_check','v36_op_safe_check',3,30,'class-v36-09','fn.flood.opSafeCheck','dom_water_floodcontrol',1,0,'安全校验',1,1,0,1,'v1',0,'settings','#F53F3F','水库调度泄流安全校验','校验泄洪流量与库容是否处于安全区间','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('class_action-v36-fn-10','ri.ont.action.v36.store_gap','v36_store_gap',3,30,'class-v36-10','fn.flood.storeGap','dom_water_floodcontrol',1,0,'缺口分析',1,1,0,1,'v1',0,'inbox','#86909C','防汛物资缺口分析','对比库存数量与定额计算物资缺口','防洪减灾域','2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- 18b. 函数规则 ont_class_action_rule（rule_type=2 副作用规则, rule_config.kind=function）
INSERT INTO ont_class_action_rule
  (id, action_id, action_type, rule_type, rule_name, sort, status, rule_config, create_time, update_time) VALUES
  ('action-rule-v36-01','class_action-v36-fn-01',30,2,'汛期值守到位核验',0,1,
   '{"kind":"function","func_code":"fn.flood.watchCheck","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"值守点编码","param_type":"string","required":1,"value_source":1,"value_content":"watch_code"},{"name":"值守开始","param_type":"datetime","required":1,"value_source":1,"value_content":"watch_start"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-02','class_action-v36-fn-02',30,2,'洪水预警等级判定',0,1,
   '{"kind":"function","func_code":"fn.flood.warningGrade","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"洪峰高程","param_type":"number","required":1,"value_source":1,"value_content":"crest_elevation"},{"name":"预警阈值","param_type":"number","required":1,"value_source":1,"value_content":"warning_threshold"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-03','class_action-v36-fn-03',30,2,'堤防险情评估',0,1,
   '{"kind":"function","func_code":"fn.flood.leveeRisk","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"堤段长度","param_type":"number","required":1,"value_source":1,"value_content":"reach_length"},{"name":"堤防状况","param_type":"string","required":1,"value_source":1,"value_content":"status"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-04','class_action-v36-fn-04',30,2,'巡堤查险达标率统计',0,1,
   '{"kind":"function","func_code":"fn.flood.patrolRate","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"任务编号","param_type":"string","required":1,"value_source":1,"value_content":"task_no"},{"name":"巡堤时间","param_type":"datetime","required":1,"value_source":1,"value_content":"patrol_time"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-05','class_action-v36-fn-05',30,2,'溢洪道泄洪能力校验',0,1,
   '{"kind":"function","func_code":"fn.flood.spillCapacity","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"最大泄量","param_type":"number","required":1,"value_source":1,"value_content":"max_discharge"},{"name":"下泄流量","param_type":"number","required":1,"value_source":1,"value_content":"discharge_flow"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-06','class_action-v36-fn-06',30,2,'山洪灾害风险分级',0,1,
   '{"kind":"function","func_code":"fn.flood.flashRisk","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"洪水量级","param_type":"string","required":1,"value_source":1,"value_content":"flood_level"},{"name":"防御社区","param_type":"string","required":1,"value_source":1,"value_content":"community"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-07','class_action-v36-fn-07',30,2,'防汛预案完备性评分',0,1,
   '{"kind":"function","func_code":"fn.flood.planScore","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"预案编码","param_type":"string","required":1,"value_source":1,"value_content":"plan_code"},{"name":"预案版本","param_type":"integer","required":1,"value_source":1,"value_content":"version"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-08','class_action-v36-fn-08',30,2,'转移撤离进度核算',0,1,
   '{"kind":"function","func_code":"fn.flood.evacuateRate","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"转移人数","param_type":"integer","required":1,"value_source":1,"value_content":"evacuated_num"},{"name":"指令时间","param_type":"datetime","required":1,"value_source":1,"value_content":"order_time"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-09','class_action-v36-fn-09',30,2,'水库调度泄流安全校验',0,1,
   '{"kind":"function","func_code":"fn.flood.opSafeCheck","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"泄洪流量","param_type":"number","required":1,"value_source":1,"value_content":"discharge_flow"},{"name":"库容","param_type":"number","required":1,"value_source":1,"value_content":"storage_capacity"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00'),
  ('action-rule-v36-10','class_action-v36-fn-10',30,2,'防汛物资缺口分析',0,1,
   '{"kind":"function","func_code":"fn.flood.storeGap","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"库存数量","param_type":"integer","required":1,"value_source":1,"value_content":"stock_qty"},{"name":"物资编码","param_type":"string","required":1,"value_source":1,"value_content":"material_code"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-03 11:00:00','2026-08-03 11:00:00')
ON CONFLICT (id) DO NOTHING;

-- 18c. 动作表单参数 ont_action_form_param（与各函数 in 入参一一对应）
INSERT INTO ont_action_form_param
  (id, action_id, section_id, param_code, param_name, param_type, data_type,
   is_required, is_multi, default_value, placeholder, sort, config) VALUES
  -- 值守到位核验
  ('action-formparam-v36-0101','class_action-v36-fn-01',NULL,'watch_code','值守点编码','string','xsd:string',1,0,NULL,'请输入值守点编码',0,'{"value_source":1,"property_code":"watch_code"}'),
  ('action-formparam-v36-0102','class_action-v36-fn-01',NULL,'watch_start','值守开始','datetime','xsd:dateTime',1,0,NULL,'请选择值守开始时间',1,'{"value_source":1,"property_code":"watch_start"}'),
  -- 洪水预警定级
  ('action-formparam-v36-0201','class_action-v36-fn-02',NULL,'crest_elevation','洪峰高程','number','xsd:decimal',1,0,NULL,'请输入洪峰高程(m)',0,'{"value_source":1,"property_code":"crest_elevation"}'),
  ('action-formparam-v36-0202','class_action-v36-fn-02',NULL,'warning_threshold','预警阈值','number','xsd:decimal',1,0,NULL,'请输入预警阈值(m)',1,'{"value_source":1,"property_code":"warning_threshold"}'),
  -- 堤防险情评估
  ('action-formparam-v36-0301','class_action-v36-fn-03',NULL,'reach_length','堤段长度','number','xsd:decimal',1,0,NULL,'请输入堤段长度(m)',0,'{"value_source":1,"property_code":"reach_length"}'),
  ('action-formparam-v36-0302','class_action-v36-fn-03',NULL,'status','堤防状况','string','xsd:string',1,0,NULL,'请选择堤防状况',1,'{"value_source":1,"property_code":"status"}'),
  -- 巡堤达标统计
  ('action-formparam-v36-0401','class_action-v36-fn-04',NULL,'task_no','任务编号','string','xsd:string',1,0,NULL,'请输入任务编号',0,'{"value_source":1,"property_code":"task_no"}'),
  ('action-formparam-v36-0402','class_action-v36-fn-04',NULL,'patrol_time','巡堤时间','datetime','xsd:dateTime',1,0,NULL,'请选择巡堤时间',1,'{"value_source":1,"property_code":"patrol_time"}'),
  -- 溢洪道能力校验
  ('action-formparam-v36-0501','class_action-v36-fn-05',NULL,'max_discharge','最大泄量','number','xsd:decimal',1,0,NULL,'请输入最大泄量(m³/s)',0,'{"value_source":1,"property_code":"max_discharge"}'),
  ('action-formparam-v36-0502','class_action-v36-fn-05',NULL,'discharge_flow','下泄流量','number','xsd:decimal',1,0,NULL,'请输入下泄流量(m³/s)',1,'{"value_source":1,"property_code":"discharge_flow"}'),
  -- 山洪风险分级
  ('action-formparam-v36-0601','class_action-v36-fn-06',NULL,'flood_level','洪水量级','string','xsd:string',1,0,NULL,'请选择洪水量级',0,'{"value_source":1,"property_code":"flood_level"}'),
  ('action-formparam-v36-0602','class_action-v36-fn-06',NULL,'community','防御社区','string','xsd:string',1,0,NULL,'请输入防御社区',1,'{"value_source":1,"property_code":"community"}'),
  -- 预案评分
  ('action-formparam-v36-0701','class_action-v36-fn-07',NULL,'plan_code','预案编码','string','xsd:string',1,0,NULL,'请输入预案编码',0,'{"value_source":1,"property_code":"plan_code"}'),
  ('action-formparam-v36-0702','class_action-v36-fn-07',NULL,'version','预案版本','integer','xsd:integer',1,0,NULL,'请输入预案版本号',1,'{"value_source":1,"property_code":"version"}'),
  -- 撤离进度核算
  ('action-formparam-v36-0801','class_action-v36-fn-08',NULL,'evacuated_num','转移人数','integer','xsd:integer',1,0,NULL,'请输入实际转移人数',0,'{"value_source":1,"property_code":"evacuated_num"}'),
  ('action-formparam-v36-0802','class_action-v36-fn-08',NULL,'order_time','指令时间','datetime','xsd:dateTime',1,0,NULL,'请选择指令时间',1,'{"value_source":1,"property_code":"order_time"}'),
  -- 调度安全校验
  ('action-formparam-v36-0901','class_action-v36-fn-09',NULL,'discharge_flow','泄洪流量','number','xsd:decimal',1,0,NULL,'请输入泄洪流量(m³/s)',0,'{"value_source":1,"property_code":"discharge_flow"}'),
  ('action-formparam-v36-0902','class_action-v36-fn-09',NULL,'storage_capacity','库容','number','xsd:decimal',1,0,NULL,'请输入库容(万m³)',1,'{"value_source":1,"property_code":"storage_capacity"}'),
  -- 物资缺口分析
  ('action-formparam-v36-1001','class_action-v36-fn-10',NULL,'stock_qty','库存数量','integer','xsd:integer',1,0,NULL,'请输入当前库存数量',0,'{"value_source":1,"property_code":"stock_qty"}'),
  ('action-formparam-v36-1002','class_action-v36-fn-10',NULL,'material_code','物资编码','string','xsd:string',1,0,NULL,'请输入物资编码',1,'{"value_source":1,"property_code":"material_code"}')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- END V36__flood_control_demo_seed.sql
-- 覆盖模块汇总 (全部 dom_water_floodcontrol):
--   业务分组: 2 组 (防洪减灾对象类型 / 防洪预警接口组)
--   枚举: 4 种 (general_single×2 / biz_single×1 / general_multi×1) + 20 项
--   编码规则: 2 条; 同步配置: 1 条 (达梦防洪库)
--   值类型: 10 种 (Enum×4 / Regex×1 / Length×2 / Decimal×3 / Integer×1)
--   共享属性: 11 条 (data×7 / object×2 / annotation×1 / struct×1)
--   结构类型: 2 种 + 6 条结构项
--   接口属性: if-7 补齐 4 条 + 2 条格式
--   对象类: 10 种 (含 parent_class / intersection 表达式 / is_common)
--   对象类属性: 24 条 (data/object/annotation/struct 全覆盖 + OWL 特性)
--   接口-类绑定: 11 条 (if-7 / if-1 / if-2)
--   链接类型: 6 条 (1:1/1:N/N:1/N:N + is_data_source_rel=1)
--   链接映射: 6 条; 类-类直连: 3 条
--   类等价/不相交组: 3 条; 不相交联合: 3 条
--   属性等价: 2 条; 属性不相交: 2 条
--   物理数据集映射: 5 条 (main + supplementary)
--   属性格式: 4 条 (number/currency/date/text)
--   类型类绑定: 5 条 (geo/hubble/hierarchy)
--   业务分组-类关联: 17 条
--   函数动作: 10 条 (每个对象类一个) + 10 条规则 + 20 条表单参数
-- ============================================================
