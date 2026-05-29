-- ============================================================
-- BOntoLink Mock Data (SQLite & PostgreSQL 兼容)
-- 行业 -> 领域 -> 子领域 -> 分组
-- 命名空间严格按 PDF "本体空间命名规则" 章节构建
-- 写入前先 DELETE，保证可重入
-- ============================================================

DELETE FROM ont_interface_class;
DELETE FROM sys_data_source;
DELETE FROM ont_interface_property;
DELETE FROM ont_class_action;
DELETE FROM ont_class_link;
DELETE FROM ont_class_property;
DELETE FROM ont_class;
DELETE FROM ont_interface;
DELETE FROM ont_biz_group_class;
DELETE FROM ont_biz_group;
DELETE FROM ont_biz_namespace_version;
DELETE FROM ont_biz_namespace;
DELETE FROM ont_biz_category;

-- ===== 命名空间 =====
INSERT INTO ont_biz_namespace(id, ns_code, ns_name, ns_uri, hierarchy_path, curr_version, status, rdfs_label, rdfs_comment) VALUES
('namespace-00000000-0000-0000-0000-000000000001','w_root','本体根目录','http://watf.com/ont/v1#','watf.root','1.0',1,'本体根目录','BOntoLink 根命名空间'),
('namespace-00000000-0000-0000-0000-000000000002','w_common','公共基础层','http://watf.com/ont/v1/common#','watf.common','1.0',1,'公共基础层','跨领域共享基础概念'),
('namespace-00000000-0000-0000-0000-000000000003','w_wtr','水利领域','http://watf.com/ont/v1/water#','watf.water','1.0',1,'水利领域','水利总命名空间'),
('namespace-00000000-0000-0000-0000-000000000004','w_wtr_sc','水土保持','http://watf.com/ont/v1/water/soilconservation#','watf.water.soilconservation','1.0',1,'水土保持',NULL),
('namespace-00000000-0000-0000-0000-000000000005','w_wtr_wr','水资源','http://watf.com/ont/v1/water/waterresource#','watf.water.waterresource','1.0',1,'水资源',NULL),
('namespace-00000000-0000-0000-0000-000000000006','w_wtr_hyd','水文','http://watf.com/ont/v1/water/hydrology#','watf.water.hydrology','1.0',1,'水文',NULL),
('namespace-00000000-0000-0000-0000-000000000007','w_wtr_wae','水环境','http://watf.com/ont/v1/water/waterenvironment#','watf.water.waterenvironment','1.0',1,'水环境',NULL),
('namespace-00000000-0000-0000-0000-000000000008','w_wtr_wec','水生态','http://watf.com/ont/v1/water/waterecology#','watf.water.waterecology','1.0',1,'水生态',NULL),
('namespace-00000000-0000-0000-0000-000000000009','w_wtr_eng','水利工程','http://watf.com/ont/v1/water/engineering#','watf.water.engineering','1.0',1,'水利工程',NULL),
('namespace-00000000-0000-0000-0000-000000000010','w_wtr_fld','防洪减灾','http://watf.com/ont/v1/water/floodcontrol#','watf.water.floodcontrol','1.0',1,'防洪减灾',NULL),
('namespace-00000000-0000-0000-0000-000000000011','w_wtr_irr','农田灌溉','http://watf.com/ont/v1/water/irrigation#','watf.water.irrigation','1.0',1,'农田灌溉',NULL),
('namespace-00000000-0000-0000-0000-000000000012','w_wtr_sup','城乡供水','http://watf.com/ont/v1/water/watersupply#','watf.water.watersupply','1.0',1,'城乡供水',NULL),
('namespace-00000000-0000-0000-0000-000000000013','w_wtr_drn','排涝排水','http://watf.com/ont/v1/water/drainage#','watf.water.drainage','1.0',1,'排涝排水',NULL),
('namespace-00000000-0000-0000-0000-000000000014','w_wtr_saf','水利安全','http://watf.com/ont/v1/water/watersafety#','watf.water.watersafety','1.0',1,'水利安全',NULL),
('namespace-00000000-0000-0000-0000-000000000015','w_wtr_mon','水文监测','http://watf.com/ont/v1/water/monitor#','watf.water.monitor','1.0',1,'水文监测',NULL),
('namespace-00000000-0000-0000-0000-000000000016','w_wtr_reg','水利监管','http://watf.com/ont/v1/water/regulation#','watf.water.regulation','1.0',1,'水利监管',NULL),
('namespace-00000000-0000-0000-0000-000000000017','w_wtr_stat','水利统计','http://watf.com/ont/v1/water/statistics#','watf.water.statistics','1.0',1,'水利统计',NULL),
('namespace-00000000-0000-0000-0000-000000000020','w_tfc','交通总领域','http://watf.com/ont/v1/traffic#','watf.traffic','1.0',1,'交通总领域',NULL),
('namespace-00000000-0000-0000-0000-000000000021','w_eme','应急总领域','http://watf.com/ont/v1/emergency#','watf.emergency','1.0',1,'应急总领域',NULL),
('namespace-00000000-0000-0000-0000-000000000022','w_env','环保总领域','http://watf.com/ont/v1/environment#','watf.environment','1.0',1,'环保总领域',NULL),
('namespace-00000000-0000-0000-0000-000000000023','w_for','林业总领域','http://watf.com/ont/v1/forestry#','watf.forestry','1.0',1,'林业总领域',NULL),
('namespace-00000000-0000-0000-0000-000000000024','w_agr','农业农村总领域','http://watf.com/ont/v1/agriculture#','watf.agriculture','1.0',1,'农业农村总领域',NULL);

-- ===== 命名空间版本 =====
INSERT INTO ont_biz_namespace_version(id, ns_code, version, uri, is_current, status, publish_time, rdfs_label, rdfs_comment) VALUES
('namespace-v-00000000-0000-0000-0000-000000000001','w_wtr','1.0','http://watf.com/ont/v1/water#',1,1,'2026-04-01 10:00:00','水利领域初版','基础水利本体定义'),
('namespace-v-00000000-0000-0000-0000-000000000002','w_wtr','0.9','http://watf.com/ont/v1/water#',0,1,'2026-02-15 10:00:00','水利领域预览版','早期预览'),
('namespace-v-00000000-0000-0000-0000-000000000003','w_wtr_sc','1.0','http://watf.com/ont/v1/water/soilconservation#',1,1,'2026-05-08 14:30:00','水土保持初版','新增水土保持 5 个实体类、12 个数据属性'),
('namespace-v-00000000-0000-0000-0000-000000000004','w_wtr_hyd','1.0','http://watf.com/ont/v1/water/hydrology#',1,1,'2026-04-20 09:00:00','水文初版',NULL),
('namespace-v-00000000-0000-0000-0000-000000000005','w_tfc','1.0','http://watf.com/ont/v1/traffic#',1,1,'2026-03-10 10:00:00','交通领域初版',NULL);

-- ===== 行业 / 领域 / 分组 分类 =====
-- 行业 (category_type=1)
INSERT INTO ont_biz_category(id, parent_id, rid, category_code, category_type, ns_code, status, sort, icon, color, rdfs_label, rdfs_comment, description) VALUES
('category-10000000-0000-0000-0000-000000000001','0','ri.ont.biz.category.10000000-0000-0000-0000-000000000001','ind_water_resource',1,'w_wtr',1,1,'industry','#165DFF','水利行业','面向水利全行业本体管理','水利行业总入口'),
('category-10000000-0000-0000-0000-000000000002','0','ri.ont.biz.category.10000000-0000-0000-0000-000000000002','ind_transportation',1,'w_tfc',1,2,'industry','#00B42A','交通行业',NULL,'交通行业总入口'),
('category-10000000-0000-0000-0000-000000000003','0','ri.ont.biz.category.10000000-0000-0000-0000-000000000003','ind_emergency',1,'w_eme',1,3,'industry','#FF7D00','应急行业',NULL,NULL),
('category-10000000-0000-0000-0000-000000000004','0','ri.ont.biz.category.10000000-0000-0000-0000-000000000004','ind_environment',1,'w_env',1,4,'industry','#722ED1','环保行业',NULL,NULL),
('category-10000000-0000-0000-0000-000000000005','0','ri.ont.biz.category.10000000-0000-0000-0000-000000000005','ind_forestry',1,'w_for',1,5,'industry','#13C2C2','林业行业',NULL,NULL),
('category-10000000-0000-0000-0000-000000000006','0','ri.ont.biz.category.10000000-0000-0000-0000-000000000006','ind_agriculture',1,'w_agr',1,6,'industry','#EB2F96','农业农村',NULL,NULL);

-- 领域 (category_type=2) 挂在水利行业下
INSERT INTO ont_biz_category(id, parent_id, rid, category_code, category_type, ns_code, status, sort, icon, color, rdfs_label, description) VALUES
('category-20000000-0000-0000-0000-000000000001','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000001','dom_water_soilconservation',2,'w_wtr_sc',1,1,'folder','#165DFF','水土保持',NULL),
('category-20000000-0000-0000-0000-000000000002','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000002','dom_water_resource',2,'w_wtr_wr',1,2,'folder','#00B42A','水资源',NULL),
('category-20000000-0000-0000-0000-000000000003','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000003','dom_water_hydrology',2,'w_wtr_hyd',1,3,'folder','#FF7D00','水文',NULL),
('category-20000000-0000-0000-0000-000000000004','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000004','dom_water_environment',2,'w_wtr_wae',1,4,'folder','#722ED1','水环境',NULL),
('category-20000000-0000-0000-0000-000000000005','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000005','dom_water_ecology',2,'w_wtr_wec',1,5,'folder','#13C2C2','水生态',NULL),
('category-20000000-0000-0000-0000-000000000006','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000006','dom_water_engineering',2,'w_wtr_eng',1,6,'folder','#EB2F96','水利工程',NULL),
('category-20000000-0000-0000-0000-000000000007','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000007','dom_water_floodcontrol',2,'w_wtr_fld',1,7,'folder','#F53F3F','防洪减灾',NULL),
('category-20000000-0000-0000-0000-000000000008','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000008','dom_water_irrigation',2,'w_wtr_irr',1,8,'folder','#FADB14','农田灌溉',NULL),
('category-20000000-0000-0000-0000-000000000009','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000009','dom_water_watersupply',2,'w_wtr_sup',1,9,'folder','#165DFF','城乡供水',NULL),
('category-20000000-0000-0000-0000-000000000010','category-10000000-0000-0000-0000-000000000001','ri.ont.biz.category.20000000-0000-0000-0000-000000000010','dom_water_statistics',2,'w_wtr_stat',1,10,'folder','#722ED1','水利统计',NULL);

-- 分组 (category_type=3) 挂在 "水文" 领域下
INSERT INTO ont_biz_category(id, parent_id, rid, category_code, category_type, ns_code, status, sort, icon, color, rdfs_label) VALUES
('category-30000000-0000-0000-0000-000000000001','category-20000000-0000-0000-0000-000000000003','ri.ont.biz.category.30000000-0000-0000-0000-000000000001','grp_hydrology_station',3,'w_wtr_hyd',1,1,'database','#165DFF','测站与设备'),
('category-30000000-0000-0000-0000-000000000002','category-20000000-0000-0000-0000-000000000003','ri.ont.biz.category.30000000-0000-0000-0000-000000000002','grp_hydrology_observation',3,'w_wtr_hyd',1,2,'database','#00B42A','观测数据');

-- ===== 分组（ont_biz_group） — 系统全局共享 =====
INSERT INTO ont_biz_group(id, parent_id, category_code, g_name, g_sort, icon, color, description) VALUES
-- 对象类型分组 (供 ObjectTypes 左侧树)
('group-10000000-0000-0000-0000-000000000001','category-20000000-0000-0000-0000-000000000003','grp_hydrology_station','测站与设备',1,'database','#165DFF','水文测站、设备相关对象类型集合'),
('group-10000000-0000-0000-0000-000000000002','category-20000000-0000-0000-0000-000000000003','grp_hydrology_observation','观测数据',2,'database','#00B42A','观测数据相关对象类型集合'),
-- 值类型分组 (供 ValueTypes 左侧树)
('group-vt-basic',  NULL, NULL, '基础类型',     1, 'layers',  '#165DFF', '系统基础值类型 (字符串/数字/布尔/日期)'),
('group-vt-enum',   NULL, NULL, '枚举类型',     2, 'list',    '#00B42A', '基于国标/业务枚举的值类型'),
('group-vt-id',     NULL, NULL, '标识符类型',   3, 'key',     '#722ED1', 'RID / UUID / 编码类值类型'),
-- 接口分组 (供 Interfaces 左侧树)
('group-if-common',  NULL, NULL, '公共接口',    1, 'link',    '#165DFF', '通用基础接口 (可监测 / 地理实体 等)'),
('group-if-water',   NULL, NULL, '水利领域接口', 2, 'droplet', '#00B42A', '水文 / 工程 / 水务 等领域接口'),
('group-if-deprecated', NULL, NULL, '废弃 / 停用', 9, 'archive', '#86909C', '已停用或仅供过渡的接口'),
-- 数据源分组 (供 Datasources 左侧树)
('group-ds-main',    NULL, NULL, '主业务库',    1, 'database', '#165DFF', '生产主业务数据库'),
('group-ds-obs',     NULL, NULL, '观测数据库',  2, 'cloud',    '#00B42A', '观测/监测数据存储'),
('group-ds-3rdparty',NULL, NULL, '第三方接口',  3, 'link',     '#FF7D00', '外部系统对接数据源');

-- ===== 对象类 =====
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, description, icon, color, status) VALUES
('class-00000000-0000-0000-0000-000000000001','ri.ont.class.00000000-0000-0000-0000-000000000001','HydrologyStation','w_wtr_hyd','dom_water_hydrology','水文测站','HydrologyStation','水文领域观测站点实体','面向水文行业的测站台账','station','#165DFF',1),
('class-00000000-0000-0000-0000-000000000002','ri.ont.class.00000000-0000-0000-0000-000000000002','River','w_wtr_hyd','dom_water_hydrology','河流','River','地表水河道实体',NULL,'wave','#00B42A',1),
('class-00000000-0000-0000-0000-000000000003','ri.ont.class.00000000-0000-0000-0000-000000000003','WaterQuality','w_wtr_wae','dom_water_environment','水质指标','WaterQuality','水环境质量监测',NULL,'droplet','#722ED1',1),
('class-00000000-0000-0000-0000-000000000004','ri.ont.class.00000000-0000-0000-0000-000000000004','Reservoir','w_wtr_eng','dom_water_engineering','水库','Reservoir','水库工程实体',NULL,'dam','#FF7D00',1);

-- 对象-分组关联 (统一表 ont_biz_group_class with group_type='object_types')
INSERT INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort) VALUES
-- 测站与设备 (grp_hydrology_station)
('gc-1','group-10000000-0000-0000-0000-000000000001','class-00000000-0000-0000-0000-000000000001','object_types','grp_hydrology_station',1), -- HydrologyStation
('gc-2','group-10000000-0000-0000-0000-000000000001','class-whyd-01','object_types','grp_hydrology_station',2),                              -- RainfallStation
('gc-3','group-10000000-0000-0000-0000-000000000001','class-whyd-03','object_types','grp_hydrology_station',3),                              -- EvaporationStation
-- 观测数据 (grp_hydrology_observation)
('gc-4','group-10000000-0000-0000-0000-000000000002','class-00000000-0000-0000-0000-000000000002','object_types','grp_hydrology_observation',1), -- River
('gc-5','group-10000000-0000-0000-0000-000000000002','class-00000000-0000-0000-0000-000000000003','object_types','grp_hydrology_observation',2), -- WaterQuality
('gc-6','group-10000000-0000-0000-0000-000000000002','class-whyd-02','object_types','grp_hydrology_observation',3);                              -- RunoffSeries

-- 属性
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, rdfs_label, is_primary, is_required) VALUES
('cp-1','ri.ont.class.property.cp-1','class-00000000-0000-0000-0000-000000000001','stationCode','string','测站编码','stationCode',1,1),
('cp-2','ri.ont.class.property.cp-2','class-00000000-0000-0000-0000-000000000001','stationName','string','测站名称','stationName',0,1),
('cp-3','ri.ont.class.property.cp-3','class-00000000-0000-0000-0000-000000000001','longitude','decimal','经度','longitude',0,0),
('cp-4','ri.ont.class.property.cp-4','class-00000000-0000-0000-0000-000000000001','latitude','decimal','纬度','latitude',0,0),
('cp-5','ri.ont.class.property.cp-5','class-00000000-0000-0000-0000-000000000002','riverName','string','河流名称','riverName',1,1);

-- 关系
INSERT INTO ont_class_link(id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, rdfs_label) VALUES
('cl-1','ri.ont.class.link.cl-1','locatedInRiver','class-00000000-0000-0000-0000-000000000001','class-00000000-0000-0000-0000-000000000002','many_to_one','测站所属河流','locatedInRiver'),
('cl-2','ri.ont.class.link.cl-2','hasWaterQuality','class-00000000-0000-0000-0000-000000000002','class-00000000-0000-0000-0000-000000000003','one_to_many','河流水质','hasWaterQuality');

-- 动作
INSERT INTO ont_class_action(id, rid, class_id, api_name, action_kind, display_name, rdfs_label) VALUES
('ca-1','ri.ont.class.action.ca-1','class-00000000-0000-0000-0000-000000000001','registerStation','create','登记新测站','registerStation'),
('ca-2','ri.ont.class.action.ca-2','class-00000000-0000-0000-0000-000000000001','decommissionStation','update','测站退役','decommissionStation');

-- 接口
INSERT INTO ont_interface(id, rid, api_name, interface_code, ns_code, category_code, display_name, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by, description, icon, color, status, metadata) VALUES
('if-1','ri.ont.interface.if-1','monitorable_entity','iface_monitorable','w_common',NULL,'可监测实体接口','MonitorableEntity','统一可监测实体的公共行为','https://watf.com/standards/monitorable','WATF/v1/common','所有具备监测能力的实体应实现此接口','station','#165DFF',1,'{"query":"按时间区间筛选","unit_normalization":true}'),
('if-2','ri.ont.interface.if-2','geo_entity','iface_geo','w_common',NULL,'地理实体接口','GeoEntity','具备地理空间属性的实体','OGC GeoSPARQL','WATF/v1/common','提供经纬度、高程、空间几何能力','wave','#00B42A',1,'{"crs":"WGS84","precision":6}'),
('if-3','ri.ont.interface.if-3','hydrology_measurable','iface_wtr_meas','w_wtr_hyd','dom_water_hydrology','水文可测量接口','HydrologyMeasurable','水文领域统一测量行为',NULL,'WATF/v1/water/hydrology','返回水文要素观测值的契约','droplet','#FF7D00',1,NULL),
('if-4','ri.ont.interface.if-4','riverine_entity','iface_river','w_wtr_hyd','dom_water_hydrology','河流实体接口','RiverineEntity','与河道相关的实体',NULL,NULL,'适配河流上下游、支流关系等','wave','#722ED1',1,NULL),
('if-5','ri.ont.interface.if-5','reservoir_entity','iface_reservoir','w_wtr_eng','dom_water_engineering','水库相关接口','ReservoirEntity','与水库工程相关的实体',NULL,NULL,'水库基础工程实体契约','dam','#EB2F96',1,NULL),
('if-6','ri.ont.interface.if-6','irrigatable_entity','iface_irr','w_wtr_irr','dom_water_irrigation','可灌溉接口','IrrigatableEntity','农田灌溉对象接口',NULL,NULL,'可被灌溉对象的契约','wheat','#FADB14',1,NULL),
('if-7','ri.ont.interface.if-7','flood_alertable','iface_flood_alert','w_wtr_fld','dom_water_floodcontrol','洪水预警接口','FloodAlertable','支持洪水预警与触发',NULL,'WATF/v1/water/floodcontrol','水位/流量阈值告警','shield','#F53F3F',1,'{"alertLevels":["黄","橙","红"]}'),
('if-8','ri.ont.interface.if-8','water_supply_metering','iface_sup_meter','w_wtr_sup','dom_water_watersupply','供水计量接口','WaterSupplyMetering','供水计量数据上报',NULL,NULL,'水表/管网计量统一规范','sliders','#13C2C2',1,'{"period":"month"}'),
('if-9','ri.ont.interface.if-9','pollution_sourced','iface_pollution','w_wtr_wae','dom_water_environment','污染源识别接口','PollutionSourced','污染源识别与归因',NULL,NULL,'污染源属性、排放口契约','factory','#F53F3F',0,NULL);

-- 接口属性（按 PDF 字段定义，prop_code + xsd:* data_type）
INSERT INTO ont_interface_property(id, rid, interface_id, api_name, prop_code, data_type, category_code, display_name, rdfs_label, rdfs_comment, is_required, metadata) VALUES
('interface-pro-001','ri.ont.interface.property.ip-1','if-1','sample_at','sampleAt','xsd:dateTime',NULL,'采样时间','sampleAt','观测/采样的发生时间，UTC ISO8601',1,'{"timezone":"UTC"}'),
('interface-pro-002','ri.ont.interface.property.ip-2','if-1','value','value','xsd:decimal',NULL,'观测值','value','观测得到的数值',1,'{"unit":"varies","precision":4}'),
('interface-pro-003','ri.ont.interface.property.ip-3','if-2','longitude','longitude','xsd:decimal',NULL,'经度','longitude','WGS84 经度',1,'{"range":[-180,180]}'),
('interface-pro-004','ri.ont.interface.property.ip-4','if-2','latitude','latitude','xsd:decimal',NULL,'纬度','latitude','WGS84 纬度',1,'{"range":[-90,90]}'),
('interface-pro-005','ri.ont.interface.property.ip-5','if-2','elevation','elevation','xsd:decimal',NULL,'高程','elevation','海拔高度（米）',0,'{"unit":"m"}'),
('interface-pro-006','ri.ont.interface.property.ip-6','if-3','unit','unit','xsd:string','dom_water_hydrology','计量单位','unit','计量单位（mm/L/...）',1,NULL),
('interface-pro-007','ri.ont.interface.property.ip-7','if-3','precision','precision','xsd:decimal','dom_water_hydrology','精度','precision','测量精度',0,NULL),
('interface-pro-008','ri.ont.interface.property.ip-8','if-7','warning_level','warningLevel','xsd:string','dom_water_floodcontrol','警戒等级','warningLevel','黄/橙/红 三级警戒',1,'{"enum":["黄","橙","红"]}'),
('interface-pro-009','ri.ont.interface.property.ip-9','if-7','threshold','threshold','xsd:decimal','dom_water_floodcontrol','阈值','threshold','触发预警的水位/流量',1,'{"unit":"m"}'),
('interface-pro-010','ri.ont.interface.property.ip-10','if-8','meter_code','meterCode','xsd:string','dom_water_watersupply','水表编号','meterCode','远传水表统一编号',1,NULL),
('interface-pro-011','ri.ont.interface.property.ip-11','if-8','reading','reading','xsd:decimal','dom_water_watersupply','本期读数','reading','本期累计读数',1,'{"unit":"m3","precision":2}');

-- 类实现接口关系
INSERT INTO ont_interface_class(id, interface_id, class_id, category_code, status) VALUES
('interface-class-001','if-1','class-00000000-0000-0000-0000-000000000001','dom_water_hydrology',1),    -- 水文测站 实现 可监测实体
('interface-class-002','if-1','class-whyd-01','dom_water_hydrology',1),                                  -- 雨量站 实现 可监测实体
('interface-class-003','if-1','class-whyd-03','dom_water_hydrology',1),                                  -- 蒸发站 实现 可监测实体
('interface-class-004','if-1','class-00000000-0000-0000-0000-000000000003','dom_water_environment',1),   -- 水质指标 实现 可监测实体
('interface-class-005','if-2','class-00000000-0000-0000-0000-000000000001','dom_water_hydrology',1),    -- 水文测站 实现 地理实体
('interface-class-006','if-2','class-00000000-0000-0000-0000-000000000002','dom_water_hydrology',1),    -- 河流 实现 地理实体
('interface-class-007','if-2','class-00000000-0000-0000-0000-000000000004','dom_water_engineering',1), -- 水库 实现 地理实体
('interface-class-008','if-3','class-whyd-01','dom_water_hydrology',1),                                  -- 雨量站 实现 水文可测量
('interface-class-009','if-3','class-whyd-02','dom_water_hydrology',1),                                  -- 径流序列 实现 水文可测量
('interface-class-010','if-4','class-00000000-0000-0000-0000-000000000002','dom_water_hydrology',1),    -- 河流 实现 河流实体接口
('interface-class-011','if-5','class-00000000-0000-0000-0000-000000000004','dom_water_engineering',1), -- 水库 实现 水库接口
('interface-class-012','if-7','class-wfld-01','dom_water_floodcontrol',1),                               -- 洪水位 实现 洪水预警
('interface-class-013','if-8','class-wsup-03','dom_water_watersupply',1);                                -- 远传水表 实现 供水计量

-- ============================================================
-- 数据源 sys_data_source（覆盖 12 种数据库类型 + 真实状态分布）
-- ============================================================
INSERT INTO sys_data_source(id, category_code, ds_code, ds_name, ds_type, jdbc_driver, jdbc_url, username, password, status, remark, ref_count, connect_status, active_conn, max_conn, response_ms, collection_cnt) VALUES
('datasource-00000000-mysql-001','dom_water_hydrology','water_main_db','水利主业务库','mysql','com.mysql.cj.jdbc.Driver','jdbc:mysql://127.0.0.1:3306/water_biz','water_user','***',1,'生产环境水利核心业务库',12,'online',8,20,23,0),
('datasource-00000000-mysql-002','dom_water_irrigation','irrigation_mysql','灌区调度库','mysql','com.mysql.cj.jdbc.Driver','jdbc:mysql://10.0.0.21:3306/irrigation','irr_user','***',1,'灌区水量调度库',6,'online',4,80,31,0),
('datasource-0000-postgresql-001','dom_water_environment','water_env_pg','水环境分析库','postgresql','org.postgresql.Driver','jdbc:postgresql://127.0.0.1:5432/water_env','pg_user','***',1,'水环境监测分析与建模',5,'online',3,50,18,0),
('datasource-0000-postgresql-002','dom_water_ecology','eco_pg','水生态评估库','postgresql','org.postgresql.Driver','jdbc:postgresql://10.0.0.30:5432/water_eco','eco_user','***',1,'水生态评估专用库',2,'online',2,40,22,0),
('datasource-000000-oracle-001','dom_water_engineering','engineering_ora','水利工程档案库','oracle','oracle.jdbc.OracleDriver','jdbc:oracle:thin:@10.0.0.50:1521:WTRENG','eng_user','***',1,'工程档案历史数据',8,'online',5,120,45,0),
('datasource-00000-mongodb-001','dom_water_hydrology','water_mongo','水文观测时序库','mongodb',NULL,NULL,'mongo_user','***',1,'水文海量时序观测数据',14,'online',6,256,12,38),
('datasource-0000000000-dm-001','dom_water_floodcontrol','flood_dm','防洪预警库 (达梦)','dm','dm.jdbc.driver.DmDriver','jdbc:dm://10.0.0.61:5236/FLOOD','flood_user','***',1,'国产化防洪预警数据库',3,'online',2,80,29,0),
('datasource-0000000-kingbase-01','dom_water_watersupply','supply_kingbase','供水营业库 (人大金仓)','kingbase','com.kingbase8.Driver','jdbc:kingbase8://10.0.0.62:54321/water_sup','sup_user','***',1,'供水营业系统库',4,'risk',9,100,180,0),
('datasource-000000000-oscar-001','dom_water_statistics','stat_oscar','水利统计库 (神舟通用)','oscar','com.oscar.jdbc.Driver','jdbc:oscar://10.0.0.63:2003/STATDB','stat_user','***',1,'水利综合统计库',2,'online',1,50,33,0),
('datasource-000000000-gbase-001','dom_water_soilconservation','sc_gbase','水土保持库 (南大通用)','gbase','com.gbase.jdbc.Driver','jdbc:gbase://10.0.0.64:5258/sc_db','sc_user','***',1,'水土流失监测数据',2,'online',1,40,27,0),
('datasource-00000000-polardb-001','dom_water_resource','wr_polardb','水资源云库 (阿里云 PolarDB)','polardb','com.mysql.cj.jdbc.Driver','jdbc:mysql://polardb.aliyun:3306/water_res','polar_user','***',1,'水资源 PolarDB MySQL 兼容版',7,'online',5,200,15,0),
('datasource-00000000-tdsql-001','dom_water_environment','tdsql_env','环境云库 (腾讯云 TDSQL)','tdsql','com.mysql.cj.jdbc.Driver','jdbc:mysql://tdsql.qcloud:3306/water_env','td_user','***',1,'TDSQL 多副本环境库',3,'online',2,150,20,0),
('datasource-00000000-gaussdb-01','dom_water_regulation','gauss_reg','监管云库 (华为云 GaussDB)','gaussdb','com.huawei.gaussdb.jdbc.Driver','jdbc:gaussdb://gauss.huaweicloud:5432/reg','gauss_user','***',0,'迁移评估中，暂时禁用',0,'offline',0,100,0,0),
('datasource-00000000-oceanbase-1','dom_water_drainage','ob_drainage','排涝云库 (OceanBase)','oceanbase','com.oceanbase.jdbc.Driver','jdbc:oceanbase://ob.svc:2881/drainage','ob_user','***',1,'城市排涝海量库',1,'risk',24,256,210,0);

-- ============================================================
-- 各领域差异化扩充（让 stats 真实展现领域间数据量差异）
-- 命名约定：class-w<sc/wr/...>-NN
-- ============================================================

-- 水土保持 (sc) 6 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wsc-01','ri.ont.class.wsc-01','SoilErosionPlot','w_wtr_sc','dom_water_soilconservation','水土流失监测样地','folder','#165DFF',1),
('class-wsc-02','ri.ont.class.wsc-02','VegetationCoverage','w_wtr_sc','dom_water_soilconservation','植被覆盖度','leaf','#00B42A',1),
('class-wsc-03','ri.ont.class.wsc-03','TerracedField','w_wtr_sc','dom_water_soilconservation','梯田','wheat','#FF7D00',1),
('class-wsc-04','ri.ont.class.wsc-04','CheckDam','w_wtr_sc','dom_water_soilconservation','拦沙坝','dam','#722ED1',1),
('class-wsc-05','ri.ont.class.wsc-05','SlopeUnit','w_wtr_sc','dom_water_soilconservation','坡面单元','tree','#13C2C2',1),
('class-wsc-06','ri.ont.class.wsc-06','WindbreakBelt','w_wtr_sc','dom_water_soilconservation','防风林带','tree','#EB2F96',1);

-- 水资源 (wr) 9 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wwr-01','ri.ont.class.wwr-01','WaterIntake','w_wtr_wr','dom_water_resource','取水口','droplet','#165DFF',1),
('class-wwr-02','ri.ont.class.wwr-02','WaterAllocationPlan','w_wtr_wr','dom_water_resource','水量分配方案','sliders','#00B42A',1),
('class-wwr-03','ri.ont.class.wwr-03','GroundwaterWell','w_wtr_wr','dom_water_resource','地下水监测井','station','#FF7D00',1),
('class-wwr-04','ri.ont.class.wwr-04','SurfaceWaterBody','w_wtr_wr','dom_water_resource','地表水体','wave','#722ED1',1),
('class-wwr-05','ri.ont.class.wwr-05','WaterQuota','w_wtr_wr','dom_water_resource','用水定额','grid','#13C2C2',1),
('class-wwr-06','ri.ont.class.wwr-06','WaterPermit','w_wtr_wr','dom_water_resource','取水许可证','shield','#EB2F96',1),
('class-wwr-07','ri.ont.class.wwr-07','WaterPriceTier','w_wtr_wr','dom_water_resource','水价阶梯','list','#FADB14',1),
('class-wwr-08','ri.ont.class.wwr-08','WaterRightTrade','w_wtr_wr','dom_water_resource','水权交易','share','#F53F3F',1),
('class-wwr-09','ri.ont.class.wwr-09','BasinUnit','w_wtr_wr','dom_water_resource','流域单元','folder','#165DFF',1);

-- 水文 (hyd) 已有 2，再加 3 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-whyd-01','ri.ont.class.whyd-01','RainfallStation','w_wtr_hyd','dom_water_hydrology','雨量站','droplet','#165DFF',1),
('class-whyd-02','ri.ont.class.whyd-02','RunoffSeries','w_wtr_hyd','dom_water_hydrology','径流序列','wave','#00B42A',1),
('class-whyd-03','ri.ont.class.whyd-03','EvaporationStation','w_wtr_hyd','dom_water_hydrology','蒸发站','station','#FF7D00',1);

-- 水环境 (wae) 已有 1，再加 3 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wwae-01','ri.ont.class.wwae-01','PollutionSource','w_wtr_wae','dom_water_environment','污染源','factory','#F53F3F',1),
('class-wwae-02','ri.ont.class.wwae-02','WaterFunctionZone','w_wtr_wae','dom_water_environment','水功能区','grid','#722ED1',1),
('class-wwae-03','ri.ont.class.wwae-03','SewageOutlet','w_wtr_wae','dom_water_environment','排污口','droplet','#FF7D00',1);

-- 水生态 (wec) 3 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wwec-01','ri.ont.class.wwec-01','AquaticSpecies','w_wtr_wec','dom_water_ecology','水生生物种','leaf','#00B42A',1),
('class-wwec-02','ri.ont.class.wwec-02','RiparianZone','w_wtr_wec','dom_water_ecology','河岸带','tree','#13C2C2',1),
('class-wwec-03','ri.ont.class.wwec-03','EcoFlowSegment','w_wtr_wec','dom_water_ecology','生态流量河段','wave','#165DFF',1);

-- 水利工程 (eng) 已有 1，再加 7 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-weng-01','ri.ont.class.weng-01','HydropowerStation','w_wtr_eng','dom_water_engineering','水电站','zap','#FF7D00',1),
('class-weng-02','ri.ont.class.weng-02','PumpStation','w_wtr_eng','dom_water_engineering','泵站','factory','#165DFF',1),
('class-weng-03','ri.ont.class.weng-03','Sluice','w_wtr_eng','dom_water_engineering','水闸','dam','#722ED1',1),
('class-weng-04','ri.ont.class.weng-04','Embankment','w_wtr_eng','dom_water_engineering','堤防','grid','#00B42A',1),
('class-weng-05','ri.ont.class.weng-05','Canal','w_wtr_eng','dom_water_engineering','渠道','wave','#13C2C2',1),
('class-weng-06','ri.ont.class.weng-06','Tunnel','w_wtr_eng','dom_water_engineering','输水隧洞','folder','#EB2F96',1),
('class-weng-07','ri.ont.class.weng-07','InspectionRecord','w_wtr_eng','dom_water_engineering','工程巡查记录','list','#FADB14',1);

-- 防洪减灾 (fld) 2 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wfld-01','ri.ont.class.wfld-01','FloodGauge','w_wtr_fld','dom_water_floodcontrol','洪水位','station','#F53F3F',1),
('class-wfld-02','ri.ont.class.wfld-02','EvacuationArea','w_wtr_fld','dom_water_floodcontrol','转移安置区','shield','#FF7D00',1);

-- 农田灌溉 (irr) 4 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wirr-01','ri.ont.class.wirr-01','IrrigationDistrict','w_wtr_irr','dom_water_irrigation','灌区','wheat','#FADB14',1),
('class-wirr-02','ri.ont.class.wirr-02','IrrigationCanal','w_wtr_irr','dom_water_irrigation','灌溉渠系','wave','#00B42A',1),
('class-wirr-03','ri.ont.class.wirr-03','SoilMoisture','w_wtr_irr','dom_water_irrigation','土壤墒情','droplet','#165DFF',1),
('class-wirr-04','ri.ont.class.wirr-04','CropWaterRight','w_wtr_irr','dom_water_irrigation','作物用水','leaf','#13C2C2',1);

-- 城乡供水 (sup) 5 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wsup-01','ri.ont.class.wsup-01','WaterTreatmentPlant','w_wtr_sup','dom_water_watersupply','自来水厂','factory','#165DFF',1),
('class-wsup-02','ri.ont.class.wsup-02','SupplyPipeline','w_wtr_sup','dom_water_watersupply','供水管网','grid','#722ED1',1),
('class-wsup-03','ri.ont.class.wsup-03','WaterMeter','w_wtr_sup','dom_water_watersupply','远传水表','sliders','#00B42A',1),
('class-wsup-04','ri.ont.class.wsup-04','RuralWaterStation','w_wtr_sup','dom_water_watersupply','农村供水点','station','#FF7D00',1),
('class-wsup-05','ri.ont.class.wsup-05','WaterTankFacility','w_wtr_sup','dom_water_watersupply','水箱设施','dam','#EB2F96',1);

-- 水利统计 (stat) 2 个
INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, icon, color, status) VALUES
('class-wstat-01','ri.ont.class.wstat-01','InfraMonthlyReport','w_wtr_stat','dom_water_statistics','基建月报','list','#86909C',1),
('class-wstat-02','ri.ont.class.wstat-02','ComprehensiveReport','w_wtr_stat','dom_water_statistics','综合统计','grid','#1D2129',1);

-- ============================================================
-- 关系（覆盖多个领域；分布让统计有差异）
-- ============================================================
INSERT INTO ont_class_link(id, rid, api_name, source_class_id, target_class_id, cardinality, display_name) VALUES
('cl-wsc-1','ri.ont.class.link.cl-wsc-1','protectsSlope',  'class-wsc-04','class-wsc-05','many_to_many','拦沙坝保护坡面'),
('cl-wsc-2','ri.ont.class.link.cl-wsc-2','vegetationOnSlope','class-wsc-02','class-wsc-05','many_to_many','植被覆盖坡面'),
('cl-wwr-1','ri.ont.class.link.cl-wwr-1','withinBasin',    'class-wwr-01','class-wwr-09','many_to_one','取水口属于流域'),
('cl-wwr-2','ri.ont.class.link.cl-wwr-2','permitForIntake','class-wwr-06','class-wwr-01','one_to_one', '许可针对取水口'),
('cl-wwr-3','ri.ont.class.link.cl-wwr-3','quotaAppliesToBasin','class-wwr-05','class-wwr-09','one_to_many','定额作用于流域'),
('cl-wwr-4','ri.ont.class.link.cl-wwr-4','tradeBetweenPermits','class-wwr-08','class-wwr-06','many_to_many','许可间交易'),
('cl-whyd-1','ri.ont.class.link.cl-whyd-1','rainGaugeNearStation','class-whyd-01','class-00000000-0000-0000-0000-000000000001','many_to_one','雨量站靠近水文站'),
('cl-whyd-2','ri.ont.class.link.cl-whyd-2','runoffOfRiver','class-whyd-02','class-00000000-0000-0000-0000-000000000002','many_to_one','径流序列属于河流'),
('cl-wwae-1','ri.ont.class.link.cl-wwae-1','dischargesTo','class-wwae-01','class-wwae-03','one_to_many','污染源排向排污口'),
('cl-wwae-2','ri.ont.class.link.cl-wwae-2','outletAffectsRiver','class-wwae-03','class-00000000-0000-0000-0000-000000000002','many_to_one','排污口影响河流'),
('cl-weng-1','ri.ont.class.link.cl-weng-1','poweredByReservoir','class-weng-01','class-00000000-0000-0000-0000-000000000004','many_to_one','水电站依水库'),
('cl-weng-2','ri.ont.class.link.cl-weng-2','feedsIrrigation','class-weng-05','class-wirr-02','one_to_many','渠道供水灌溉'),
('cl-weng-3','ri.ont.class.link.cl-weng-3','sluiceOnEmbankment','class-weng-03','class-weng-04','many_to_one','水闸在堤防上'),
('cl-wirr-1','ri.ont.class.link.cl-wirr-1','districtServesCrop','class-wirr-01','class-wirr-04','one_to_many','灌区服务作物'),
('cl-wirr-2','ri.ont.class.link.cl-wirr-2','soilInDistrict','class-wirr-03','class-wirr-01','many_to_one','墒情属于灌区'),
('cl-wsup-1','ri.ont.class.link.cl-wsup-1','plantSuppliesPipe','class-wsup-01','class-wsup-02','one_to_many','水厂供给管网'),
('cl-wsup-2','ri.ont.class.link.cl-wsup-2','meterOnPipe','class-wsup-03','class-wsup-02','many_to_one','水表挂管网'),
('cl-wfld-1','ri.ont.class.link.cl-wfld-1','gaugeWatchesReservoir','class-wfld-01','class-00000000-0000-0000-0000-000000000004','many_to_one','洪水位监视水库');

-- ============================================================
-- 动作（按所属对象 → 自然落入对应领域）
-- ============================================================
INSERT INTO ont_class_action(id, rid, class_id, api_name, action_kind, display_name) VALUES
('ca-wsc-1','ri.ont.class.action.ca-wsc-1','class-wsc-01','recordErosion',  'update','记录侵蚀'),
('ca-wsc-2','ri.ont.class.action.ca-wsc-2','class-wsc-04','inspectCheckDam','update','拦沙坝巡查'),
('ca-wwr-1','ri.ont.class.action.ca-wwr-1','class-wwr-01','registerIntake', 'create','新增取水口'),
('ca-wwr-2','ri.ont.class.action.ca-wwr-2','class-wwr-06','issuePermit',    'create','签发许可'),
('ca-wwr-3','ri.ont.class.action.ca-wwr-3','class-wwr-08','executeTrade',   'update','完成水权交易'),
('ca-whyd-1','ri.ont.class.action.ca-whyd-1','class-whyd-01','reportRainfall','update','上报降雨'),
('ca-wwae-1','ri.ont.class.action.ca-wwae-1','class-wwae-03','sampleOutlet','update','排污口采样'),
('ca-weng-1','ri.ont.class.action.ca-weng-1','class-weng-01','generatePower','update','发电出力'),
('ca-weng-2','ri.ont.class.action.ca-weng-2','class-weng-02','startPumping','update','开启泵站'),
('ca-weng-3','ri.ont.class.action.ca-weng-3','class-weng-03','operateSluice','update','操作闸门'),
('ca-wirr-1','ri.ont.class.action.ca-wirr-1','class-wirr-01','scheduleIrrigation','update','安排灌溉'),
('ca-wsup-1','ri.ont.class.action.ca-wsup-1','class-wsup-03','readMeter','update','抄表'),
('ca-wfld-1','ri.ont.class.action.ca-wfld-1','class-wfld-02','evacuate','update','启动转移');

-- ============================================================
-- 属性（每个新对象 2-3 条）
-- ============================================================
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, is_primary, is_required) VALUES
('cp-wsc-1','ri.ont.class.property.cp-wsc-1','class-wsc-01','plotCode','string','样地编码',1,1),
('cp-wsc-2','ri.ont.class.property.cp-wsc-2','class-wsc-01','area','decimal','面积',0,1),
('cp-wsc-3','ri.ont.class.property.cp-wsc-3','class-wsc-02','coverageRate','decimal','覆盖度',0,1),
('cp-wsc-4','ri.ont.class.property.cp-wsc-4','class-wsc-03','elevation','decimal','海拔',0,0),
('cp-wsc-5','ri.ont.class.property.cp-wsc-5','class-wsc-04','height','decimal','坝高',0,1),
('cp-wwr-1','ri.ont.class.property.cp-wwr-1','class-wwr-01','intakeCode','string','取水口编码',1,1),
('cp-wwr-2','ri.ont.class.property.cp-wwr-2','class-wwr-01','maxFlow','decimal','最大取水流量',0,1),
('cp-wwr-3','ri.ont.class.property.cp-wwr-3','class-wwr-03','depth','decimal','井深',0,1),
('cp-wwr-4','ri.ont.class.property.cp-wwr-4','class-wwr-06','permitNo','string','许可证号',1,1),
('cp-wwr-5','ri.ont.class.property.cp-wwr-5','class-wwr-06','validUntil','date','有效期至',0,1),
('cp-wwr-6','ri.ont.class.property.cp-wwr-6','class-wwr-09','basinName','string','流域名称',1,1),
('cp-whyd-1','ri.ont.class.property.cp-whyd-1','class-whyd-01','dailyRain','decimal','日降雨量',0,1),
('cp-whyd-2','ri.ont.class.property.cp-whyd-2','class-whyd-02','meanFlow','decimal','平均流量',0,1),
('cp-wwae-1','ri.ont.class.property.cp-wwae-1','class-wwae-01','sourceType','string','污染源类型',0,1),
('cp-wwae-2','ri.ont.class.property.cp-wwae-2','class-wwae-03','outletNo','string','排污口编号',1,1),
('cp-wwec-1','ri.ont.class.property.cp-wwec-1','class-wwec-01','speciesName','string','物种名称',1,1),
('cp-wwec-2','ri.ont.class.property.cp-wwec-2','class-wwec-03','ecoFlowMin','decimal','最小生态流量',0,1),
('cp-weng-1','ri.ont.class.property.cp-weng-1','class-weng-01','installedPower','decimal','装机容量',0,1),
('cp-weng-2','ri.ont.class.property.cp-weng-2','class-weng-02','liftHead','decimal','扬程',0,1),
('cp-weng-3','ri.ont.class.property.cp-weng-3','class-weng-03','gateCount','int','闸门数',0,1),
('cp-weng-4','ri.ont.class.property.cp-weng-4','class-weng-04','length','decimal','长度',0,1),
('cp-weng-5','ri.ont.class.property.cp-weng-5','class-weng-05','crossSection','decimal','断面面积',0,0),
('cp-weng-6','ri.ont.class.property.cp-weng-6','class-weng-07','inspector','string','巡查人',0,1),
('cp-wirr-1','ri.ont.class.property.cp-wirr-1','class-wirr-01','districtCode','string','灌区编码',1,1),
('cp-wirr-2','ri.ont.class.property.cp-wirr-2','class-wirr-01','irrigatedArea','decimal','灌溉面积',0,1),
('cp-wirr-3','ri.ont.class.property.cp-wirr-3','class-wirr-03','moisturePct','decimal','土壤含水率',0,1),
('cp-wsup-1','ri.ont.class.property.cp-wsup-1','class-wsup-01','dailyCapacity','decimal','日供水能力',0,1),
('cp-wsup-2','ri.ont.class.property.cp-wsup-2','class-wsup-03','readingValue','decimal','读数',0,1),
('cp-wsup-3','ri.ont.class.property.cp-wsup-3','class-wsup-04','population','int','服务人口',0,1),
('cp-wfld-1','ri.ont.class.property.cp-wfld-1','class-wfld-01','warningLevel','string','警戒水位',0,1),
('cp-wstat-1','ri.ont.class.property.cp-wstat-1','class-wstat-01','reportMonth','string','报告月份',1,1),
('cp-wstat-2','ri.ont.class.property.cp-wstat-2','class-wstat-02','reportPeriod','string','报告期',1,1);

-- ============================================================
-- 对象类型补充元数据 mock：等价 / 不相交 / 互斥并集 / 等价属性 / 互斥属性
-- ============================================================
DELETE FROM ont_class_group;
DELETE FROM ont_class_disjoint_union;
DELETE FROM ont_property_equivalent;
DELETE FROM ont_property_disjoint;
DELETE FROM ont_class_ds;

-- 把核心几条对象做一些补全（类表达式 / 公共类 / 父类 / 注释属性）：用 UPDATE 避免与既有 INSERT 冲突
UPDATE ont_class SET class_expr_type = NULL, class_expr_content = '{}', is_common = 1
  WHERE id = 'class-00000000-0000-0000-0000-000000000001';
UPDATE ont_class SET class_expr_type = 'union',
  class_expr_content = '{"unionIds":["class-whyd-01","class-whyd-02"]}'
  WHERE id = 'class-whyd-03';
UPDATE ont_class SET parent_class_id = 'class-00000000-0000-0000-0000-000000000001'
  WHERE id = 'class-whyd-01';
UPDATE ont_class SET parent_class_id = 'class-00000000-0000-0000-0000-000000000001'
  WHERE id = 'class-whyd-02';
UPDATE ont_class SET parent_class_id = 'class-00000000-0000-0000-0000-000000000004'
  WHERE id = 'class-weng-01';
UPDATE ont_class SET rdfs_see_also = '领域本体规范 V3.0', rdfs_defined_by = '水利公共本体库'
  WHERE id = 'class-00000000-0000-0000-0000-000000000001';

-- 等价类: HydrologyStation ≡ RainfallStation
INSERT INTO ont_class_group(id, class_id, ref_class_id, group_type, status, rdfs_comment) VALUES
('class-group-eq-01',
 CASE WHEN 'class-00000000-0000-0000-0000-000000000001' < 'class-whyd-01' THEN 'class-00000000-0000-0000-0000-000000000001' ELSE 'class-whyd-01' END,
 CASE WHEN 'class-00000000-0000-0000-0000-000000000001' < 'class-whyd-01' THEN 'class-whyd-01' ELSE 'class-00000000-0000-0000-0000-000000000001' END,
 'equivalent', 1, '水文测站与雨量监测点在测量行为上等价');

-- 不相交类: River vs Reservoir
INSERT INTO ont_class_group(id, class_id, ref_class_id, group_type, status, rdfs_comment) VALUES
('class-group-dj-01',
 CASE WHEN 'class-00000000-0000-0000-0000-000000000002' < 'class-00000000-0000-0000-0000-000000000004' THEN 'class-00000000-0000-0000-0000-000000000002' ELSE 'class-00000000-0000-0000-0000-000000000004' END,
 CASE WHEN 'class-00000000-0000-0000-0000-000000000002' < 'class-00000000-0000-0000-0000-000000000004' THEN 'class-00000000-0000-0000-0000-000000000004' ELSE 'class-00000000-0000-0000-0000-000000000002' END,
 'disjoint', 1, '河流与水库为不相交的水体类型');

-- 互斥并集: HydrologyStation = {RainfallStation, RunoffSeries, EvaporationStation}
INSERT INTO ont_class_disjoint_union(id, parent_class_id, sub_class_id, status, rdfs_comment) VALUES
('class-disjoint-union-h-01','class-00000000-0000-0000-0000-000000000001','class-whyd-01',1,'雨量监测点'),
('class-disjoint-union-h-02','class-00000000-0000-0000-0000-000000000001','class-whyd-02',1,'径流过程'),
('class-disjoint-union-h-03','class-00000000-0000-0000-0000-000000000001','class-whyd-03',1,'蒸发监测点');

-- 等价属性: HydrologyStation.stationName ↔ HydrologyStation.stationCode 是反例 → 跨类对齐示例（同类型等价）
INSERT INTO ont_property_equivalent(id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment) VALUES
('prop-equivalent-01',
 'class-00000000-0000-0000-0000-000000000001','cp-1',
 'class-whyd-01','cp-whyd-1',
 1,'两类的「编码」型属性语义一致');

-- 互斥属性: HydrologyStation.longitude vs HydrologyStation.latitude (示例：演示同类互斥)
INSERT INTO ont_property_disjoint(id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment) VALUES
('prop-disjoint-01',
 'class-00000000-0000-0000-0000-000000000001','cp-3',
 'class-00000000-0000-0000-0000-000000000001','cp-4',
 1,'示例：经度与纬度作为独立位置维度，互斥演示');

-- 对象-数据集关联 (主表 + 2 个补充表),包含完整物理字段清单 (用于关系图画布展示未映射字段)
INSERT INTO ont_class_ds(id, class_id, ds_code, physical_table, table_label, rel_type, alias, pk_keys, join_on_keys, join_type, physical_fields, sort, status) VALUES
('class-ds-01','class-00000000-0000-0000-0000-000000000001','water_main_db','t_hydrology_station','测站主表',1,'main','station_code',NULL,'LEFT',
 '[{"name":"station_code","data_type":"string","is_pk":1,"is_fk":0},{"name":"station_name","data_type":"string","is_pk":0,"is_fk":0},{"name":"lng","data_type":"decimal","is_pk":0,"is_fk":0},{"name":"lat","data_type":"decimal","is_pk":0,"is_fk":0},{"name":"region_code","data_type":"string","is_pk":0,"is_fk":1},{"name":"river_id","data_type":"string","is_pk":0,"is_fk":1},{"name":"build_year","data_type":"integer","is_pk":0,"is_fk":0},{"name":"is_active","data_type":"boolean","is_pk":0,"is_fk":0}]',
 1,1),
('class-ds-02','class-00000000-0000-0000-0000-000000000001','water_mongo','c_station_observation','测站观测补表',2,'s1',NULL,'station_code','LEFT',
 '[{"name":"station_code","data_type":"string","is_pk":0,"is_fk":1},{"name":"observe_time","data_type":"datetime","is_pk":0,"is_fk":0},{"name":"water_level","data_type":"decimal","is_pk":0,"is_fk":0},{"name":"flow_rate","data_type":"decimal","is_pk":0,"is_fk":0},{"name":"temperature","data_type":"decimal","is_pk":0,"is_fk":0}]',
 2,1),
('class-ds-03','class-00000000-0000-0000-0000-000000000001','water_main_db','t_station_river_link','测站-河流关系表',2,'s2',NULL,'station_code','INNER',
 '[{"name":"station_code","data_type":"string","is_pk":0,"is_fk":1},{"name":"river_name","data_type":"string","is_pk":0,"is_fk":0},{"name":"river_section","data_type":"string","is_pk":0,"is_fk":0},{"name":"distance_to_estuary","data_type":"decimal","is_pk":0,"is_fk":0}]',
 3,1);

-- 补全部分类属性的扩展字段（prop_type / 物理映射 / OWL 特性 / XSD 约束）
UPDATE ont_class_property SET prop_type='data', prop_code='station_code', is_key=1, is_required=1,
  physical_table='t_hydrology_station', physical_column='station_code',
  xsd_length=8, owl_inverse_functional=1 WHERE id='cp-1';
UPDATE ont_class_property SET prop_type='data', prop_code='station_name', is_required=1,
  physical_table='t_hydrology_station', physical_column='station_name',
  xsd_min_length=2, xsd_max_length=64 WHERE id='cp-2';
UPDATE ont_class_property SET prop_type='data', prop_code='longitude',
  physical_table='t_hydrology_station', physical_column='lng',
  xsd_min_inclusive='-180', xsd_max_inclusive='180', owl_functional=1 WHERE id='cp-3';
UPDATE ont_class_property SET prop_type='data', prop_code='latitude',
  physical_table='t_hydrology_station', physical_column='lat',
  xsd_min_inclusive='-90', xsd_max_inclusive='90', owl_functional=1 WHERE id='cp-4';
-- cp-5 仍归属 River 类型,保持原映射 (用于跨类关系)
UPDATE ont_class_property SET prop_type='data', prop_code='river_name', is_key=1, is_required=1,
  physical_table='t_river', physical_column='name' WHERE id='cp-5';
-- 补齐 cp-1..cp-4 的 class_ds_id (HydrologyStation 主表)
UPDATE ont_class_property SET class_ds_id='class-ds-01' WHERE id IN ('cp-1','cp-2','cp-3','cp-4');

-- 给 HydrologyStation 再补两条属性,使关系图覆盖到 s1 / s2 补充表
INSERT INTO ont_class_property(id, rid, class_id, api_name, prop_code, prop_type, data_type, display_name, rdfs_label,
    is_required, is_key, class_ds_id, physical_table, physical_column, sort) VALUES
('cp-6','ri.ont.class.property.cp-6','class-00000000-0000-0000-0000-000000000001',
  'waterLevel','water_level','data','decimal','水位','waterLevel',
  0, 0, 'class-ds-02', 'c_station_observation', 'water_level', 6),
('cp-7','ri.ont.class.property.cp-7','class-00000000-0000-0000-0000-000000000001',
  'affiliatedRiverName','river_name','data','string','所属河流','affiliatedRiverName',
  0, 0, 'class-ds-03', 't_station_river_link', 'river_name', 7);

-- ============================================================
-- 枚举类型 + 值类型 + 属性格式化 mock 数据
-- ============================================================
DELETE FROM ont_enum_items;
DELETE FROM ont_enum_types;
-- ont_enum_group 已废弃 (DROP TABLE 在 schema-sqlite.sql);此处不再 DELETE
DELETE FROM ont_enum_level_code_rule;
DELETE FROM ont_value_types;
DELETE FROM ont_valuetypes_usage_config;
DELETE FROM ont_property_format;
-- 清理旧的枚举分组关联 (若存在)
DELETE FROM ont_biz_group_class WHERE group_type = 'enum_types';

-- 业务分组 (枚举专用,挂在 ont_biz_group): 国标标准 / 水利行业 / 监测分类
INSERT OR IGNORE INTO ont_biz_group(id, parent_id, category_code, g_name, g_sort, icon, color, description) VALUES
('group-enum-gb',           NULL,             NULL,                   '国标标准', 1, 'book',  '#165DFF', '国家标准枚举分组'),
('group-enum-water',         NULL,             NULL,                   '水利行业', 2, 'droplet','#00B42A', '水利行业相关枚举'),
('group-enum-water-monitor', 'group-enum-water','dom_water_hydrology',  '监测分类', 1, 'sliders','#FF7D00', '水文监测相关枚举');

-- 枚举类型 (4 条) — 不再含 group_id,分组关系迁移至 ont_biz_group_class with group_type='enum_types'
INSERT INTO ont_enum_types(id, rid, api_name, category_code, enum_type, max_level, top_code, status, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by) VALUES
('enum-types-area-district','ri.ont.enum.library.area_district','area_district',NULL,'general_multi',5,NULL,'active',
 '行政区划 (国标 GB/T 2260)','中华人民共和国行政区划代码国家标准，覆盖省、市、县、乡镇、村五级层级编码','http://www.mca.gov.cn/article/sj/xzqh/2024/','GB/T 2260-2024'),
('enum-types-gender','ri.ont.enum.library.gender','gender',NULL,'general_single',1,NULL,'active',
 '性别 (国标 GB/T 2261.1)','一级通用：男 / 女 / 未知',NULL,'GB/T 2261.1-2003'),
('enum-types-eng-kind','ri.ont.enum.library.eng_kind','eng_kind','dom_water_engineering','biz_multi',2,NULL,'active',
 '水利工程类型','按主体功能划分:水库 / 水电站 / 泵站 / 闸坝 / 堤防 / 渠道',NULL,'水利公共本体库'),
('enum-types-job','ri.ont.enum.library.job','job',NULL,'biz_single',1,NULL,'inactive',
 '岗位类型','业务一级:管理 / 技术 / 调度 / 维护(已停用)',NULL,'人事公共本体库');

-- 枚举类型 → 分组绑定 (统一表 ont_biz_group_class with group_type='enum_types')
INSERT OR IGNORE INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort) VALUES
('gref-enum-ad',      'group-enum-gb',           'enum-types-area-district', 'enum_types', NULL, 1),
('gref-enum-gender',  'group-enum-gb',           'enum-types-gender',        'enum_types', NULL, 2),
('gref-enum-engkind', 'group-enum-water-monitor','enum-types-eng-kind',      'enum_types', 'dom_water_engineering', 1),
('gref-enum-job',     'group-enum-water',        'enum-types-job',           'enum_types', NULL, 1);

-- 行政区划：示例 5 条(北京市 -> 市辖区 -> 东城区 -> 东华门街道 -> 多福巷社区)
INSERT INTO ont_enum_items(id, enum_id, code, api_name, label, parent_code, level, sort_num, status) VALUES
('enum-item-ad-110000','enum-types-area-district','110000','beijing','北京市',NULL,1,1,'active'),
('enum-item-ad-110100','enum-types-area-district','110100','shixiaqu','市辖区','110000',2,1,'active'),
('enum-item-ad-110101','enum-types-area-district','110101','dongcheng','东城区','110100',3,1,'active'),
('enum-item-ad-110102','enum-types-area-district','110102','xicheng','西城区','110100',3,2,'active'),
('enum-item-ad-110101001','enum-types-area-district','110101001','donghuamen','东华门街道','110101',4,1,'active'),
('enum-item-ad-110101001001','enum-types-area-district','110101001001','duofuxiang','多福巷社区','110101001',5,1,'active');

-- 性别
INSERT INTO ont_enum_items(id, enum_id, code, api_name, label, parent_code, level, sort_num, status) VALUES
('enum-item-g-male',  'enum-types-gender','1','male','男',  NULL,1,1,'active'),
('enum-item-g-female','enum-types-gender','2','female','女',NULL,1,2,'active'),
('enum-item-g-unkn',  'enum-types-gender','9','unknown','未知',NULL,1,3,'active');

-- 水利工程类型
INSERT INTO ont_enum_items(id, enum_id, code, api_name, label, parent_code, level, sort_num, status) VALUES
('enum-item-eng-01','enum-types-eng-kind','01','reservoir','水库',  NULL,1,1,'active'),
('enum-item-eng-02','enum-types-eng-kind','02','hydropower','水电站',NULL,1,2,'active'),
('enum-item-eng-03','enum-types-eng-kind','03','pump_station','泵站',NULL,1,3,'active'),
('enum-item-eng-04','enum-types-eng-kind','04','sluice','闸坝',     NULL,1,4,'active'),
('enum-item-eng-05','enum-types-eng-kind','05','levee','堤防',      NULL,1,5,'active'),
('enum-item-eng-06','enum-types-eng-kind','06','channel','渠道',    NULL,1,6,'active'),
('enum-item-eng-0101','enum-types-eng-kind','0101','large_reservoir','大型水库','01',2,1,'active'),
('enum-item-eng-0102','enum-types-eng-kind','0102','medium_reservoir','中型水库','01',2,2,'active'),
('enum-item-eng-0103','enum-types-eng-kind','0103','small_reservoir','小型水库','01',2,3,'active');

-- 行政区划层次编码规则(5 层)
INSERT INTO ont_enum_level_code_rule(id, enum_id, code_name, rule_level, code_separator, code_len, total_len, fill_char, fill_pos) VALUES
('enum_level_code_rule-ad-1','enum-types-area-district','省(区)',1,'',2,6,'0',0),
('enum_level_code_rule-ad-2','enum-types-area-district','地(市、州)',2,'',2,6,'0',0),
('enum_level_code_rule-ad-3','enum-types-area-district','区(县)',3,'',2,6,'0',0),
('enum_level_code_rule-ad-4','enum-types-area-district','乡(镇)',4,'',3,9,'0',1),
('enum_level_code_rule-ad-5','enum-types-area-district','村(社区)',5,'',3,12,'0',1);

-- 行政区划同步配置 (一条示例,关联 MySQL 数据源)
INSERT INTO ont_enum_sync_config(id, enum_id, data_source_id, table_alias, table_name,
    field_code, field_name, field_sort, field_status, filter_sql, sync_mode, sync_strategy) VALUES
('enum-sync-config-ad','enum-types-area-district','ds-mysql-main','行政区划表','t_area',
 'area_code','area_name','area_code','status','parent_code IS NULL','level_diff','once');

-- 同步日志样例 (2 条)
INSERT INTO ont_enum_sync_log(id, enum_id, sync_type, add_count, update_count, del_count, fail_count, sync_status, error_msg, oper_user, sync_time) VALUES
('enum-sync-log-ad-1','enum-types-area-district','manual', 32,  0, 0, 0, 'success', NULL, 'admin', datetime('now','localtime','-2 days')),
('enum-sync-log-ad-2','enum-types-area-district','auto',    3,  1, 0, 0, 'success', NULL, NULL,    datetime('now','localtime','-12 hours'));

-- 默认使用配置 (1 条系统默认)
INSERT INTO ont_valuetypes_usage_config(id, max_select_level, allow_non_leaf, display_format, is_system_default) VALUES
('vt-usage-config-default',0,0,'label',1);

-- 值类型 (5 条覆盖各 base_type/constraint_type)
INSERT INTO ont_value_types(id, rid, api_name, category_code, base_type, constraint_type, constraint_config, enum_id, default_usage_config_id, status, rdfs_label, rdfs_comment) VALUES
('value-types-text',     'ri.ont.value.types.text',     'text',     NULL,'String','Length','{"min":0,"max":255}',NULL,NULL,1,'文本','基础字符串文本，长度 0-255'),
('value-types-mobile',   'ri.ont.value.types.mobile',   'mobile',   NULL,'String','Regex', '{"pattern":"^1[3-9]\\\\d{9}$"}',NULL,NULL,1,'手机号','中国大陆 11 位手机号'),
('value-types-rid',      'ri.ont.value.types.rid',      'rid_text', NULL,'String','RID',   '{}',NULL,NULL,1,'RID','平台全局资源标识'),
('value-types-area',     'ri.ont.value.types.area',     'area_code',NULL,'String','Enum',  NULL,'enum-types-area-district','vt-usage-config-default',1,'行政区划','基于国标行政区划的层级编码'),
('value-types-gender',   'ri.ont.value.types.gender',   'gender_code',NULL,'String','Enum',NULL,'enum-types-gender','vt-usage-config-default',1,'性别','基于国标性别枚举');

-- ===== 值类型 → 分组绑定 (group_type='value_types') =====
INSERT OR IGNORE INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort) VALUES
('gref-vt-text',   'group-vt-basic', 'value-types-text',   'value_types', NULL, 1),
('gref-vt-mobile', 'group-vt-basic', 'value-types-mobile', 'value_types', NULL, 2),
('gref-vt-rid',    'group-vt-id',    'value-types-rid',    'value_types', NULL, 1),
('gref-vt-area',   'group-vt-enum',  'value-types-area',   'value_types', NULL, 1),
('gref-vt-gender', 'group-vt-enum',  'value-types-gender', 'value_types', NULL, 2);

-- ===== 接口 → 分组绑定 (group_type='interface') =====
INSERT OR IGNORE INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort) VALUES
('gref-if-1','group-if-common',     'if-1','interface',NULL,1),
('gref-if-2','group-if-common',     'if-2','interface',NULL,2),
('gref-if-3','group-if-water',      'if-3','interface',NULL,1),
('gref-if-4','group-if-water',      'if-4','interface',NULL,2),
('gref-if-5','group-if-water',      'if-5','interface',NULL,3),
('gref-if-6','group-if-water',      'if-6','interface',NULL,4),
('gref-if-7','group-if-water',      'if-7','interface',NULL,5),
('gref-if-8','group-if-water',      'if-8','interface',NULL,6),
('gref-if-9','group-if-deprecated', 'if-9','interface',NULL,1);

-- ===== 数据源 → 分组绑定 (group_type='datasources') =====
INSERT OR IGNORE INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort) VALUES
('gref-ds-001','group-ds-main','datasource-00000000-mysql-001','datasources',NULL,1),
('gref-ds-002','group-ds-main','datasource-00000000-mysql-002','datasources',NULL,2),
('gref-ds-003','group-ds-main','datasource-0000-postgresql-001','datasources',NULL,3),
('gref-ds-004','group-ds-main','datasource-0000-postgresql-002','datasources',NULL,4),
('gref-ds-005','group-ds-main','datasource-000000-oracle-001','datasources',NULL,5),
('gref-ds-006','group-ds-obs', 'datasource-00000-mongodb-001','datasources',NULL,1),
('gref-ds-007','group-ds-main','datasource-0000000000-dm-001','datasources',NULL,6);

-- 给部分现有属性挂上格式化配置（覆盖多种分类的示例; src_type=1 表示普通属性）
INSERT INTO ont_property_format(
    format_id, src_type, property_id, property_scope, format_enabled, format_type,
    decimal_places, use_thousand_sep, negative_mode, currency_symbol, accounting_align,
    date_pattern, time_pattern, locale, fraction_type, special_type, custom_format,
    text_force, text_max_length, text_regex, percent_auto_multiply
) VALUES
-- 经度 (decimal) → 数值 6 位小数, 无千分位
('property-format-cp-3', 1, 'cp-3','class',1,'number',
 6, 0, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 纬度 (decimal) → 数值 6 位小数
('property-format-cp-4', 1, 'cp-4','class',1,'number',
 6, 0, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 测站编码 (string) → 特殊: 邮政编码风格 (强制保留前导零)
('property-format-cp-1', 1, 'cp-1','class',1,'special',
 0, 0, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 接口属性: 采样时间 (xsd:dateTime) → 日期 yyyy-MM-dd HH:mm:ss
('property-format-ip-1', 1, 'interface-pro-001','interface',1,'date',
 0, 0, 3, '¥', 1,
 'yyyy-MM-dd HH:mm:ss','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 接口属性: 观测值 (xsd:decimal) → 数值 4 位小数,千位分隔
('property-format-ip-2', 1, 'interface-pro-002','interface',1,'number',
 4, 1, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 接口属性: 经度 (xsd:decimal) → 数值 6 位
('property-format-ip-3', 1, 'interface-pro-003','interface',1,'number',
 6, 0, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 接口属性: 阈值 (xsd:decimal) → 数值 2 位,千位分隔
('property-format-ip-9', 1, 'interface-pro-009','interface',1,'number',
 2, 1, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 接口属性: 本期读数 (xsd:decimal) → 货币 2 位,千分位
('property-format-ip-11', 1, 'interface-pro-011','interface',1,'currency',
 2, 1, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 0, NULL, NULL, 1),
-- 接口属性: 水表编号 (xsd:string) → 文本,正则校验
('property-format-ip-10', 1, 'interface-pro-010','interface',1,'text',
 0, 0, 3, '¥', 1,
 'yyyy-MM-dd','HH:mm:ss','zh-CN','# ?/?','zipcode','G/通用格式',
 1, 32, '^M[0-9]{6,12}$', 1);

-- 给部分属性绑定 value_type
UPDATE ont_class_property SET value_type='value-types-text' WHERE id IN ('cp-2','cp-5');
UPDATE ont_class_property SET value_type='value-types-area' WHERE id='cp-1';

-- =============================================================
-- 共享属性 (ont_shared_properties) — 跨类复用属性库
-- 分组通过 ont_biz_group + ont_biz_group_class(group_type='shared_props') 关联
-- =============================================================

-- 共享属性分组 (3 组)
INSERT OR IGNORE INTO ont_biz_group(id, parent_id, category_code, g_name, g_sort, icon, color, description) VALUES
('group-sp-time',     NULL, NULL, '时间属性',     1, 'calendar', '#165DFF', '开始/结束/创建/更新时间等通用时间属性'),
('group-sp-identity', NULL, NULL, '标识属性',     2, 'key',      '#722ED1', '名称/编码/状态等基础标识属性'),
('group-sp-geo',      NULL, NULL, '地理属性',     3, 'mapPin',   '#00B42A', '经度/纬度/坐标系等地理属性');

-- 共享属性数据 (12 条) — 覆盖时间 / 标识 / 地理 / 数值 等典型场景
INSERT INTO ont_shared_properties(id, rid, category_code, prop_code, prop_type, data_type, value_type,
  is_required, is_multi_valued_prop, is_range_constraint_prop,
  xsd_min_length, xsd_max_length, xsd_pattern, xsd_min_inclusive, xsd_max_inclusive,
  owl_functional, status, rdfs_label, rdfs_comment) VALUES
('shared-properties-00000001','ri.ont.shared.props.start_time',  NULL, 'start_time',  'data', 'xsd:dateTime', NULL,
  0, 0, 0, NULL, NULL, NULL, NULL, NULL, 1, 1, '开始时间', '业务对象的开始时间, 通用于实体/事件/工程等'),
('shared-properties-00000002','ri.ont.shared.props.end_time',    NULL, 'end_time',    'data', 'xsd:dateTime', NULL,
  0, 0, 0, NULL, NULL, NULL, NULL, NULL, 1, 1, '结束时间', '业务对象的结束时间, 与 start_time 配对使用'),
('shared-properties-00000003','ri.ont.shared.props.create_time', NULL, 'create_time', 'data', 'xsd:dateTime', NULL,
  1, 0, 0, NULL, NULL, NULL, NULL, NULL, 1, 1, '创建时间', '记录入库时间, 由系统自动生成'),
('shared-properties-00000004','ri.ont.shared.props.update_time', NULL, 'update_time', 'data', 'xsd:dateTime', NULL,
  1, 0, 0, NULL, NULL, NULL, NULL, NULL, 1, 1, '更新时间', '最近一次更新时间, 由系统自动维护'),
('shared-properties-00000005','ri.ont.shared.props.name',        NULL, 'name',        'data', 'xsd:string',   'value-types-text',
  1, 0, 0, 1, 128, NULL, NULL, NULL, 1, 1, '名称', '业务对象的中文名称, 长度 1-128 字符'),
('shared-properties-00000006','ri.ont.shared.props.code',        NULL, 'code',        'data', 'xsd:string',   'value-types-text',
  1, 0, 0, 2, 64, '^[A-Za-z0-9_\-]+$', NULL, NULL, 1, 1, '编码', '业务对象的唯一编码, 字母数字下划线连字符'),
('shared-properties-00000007','ri.ont.shared.props.status',      NULL, 'status',      'data', 'xsd:string',   NULL,
  1, 0, 1, NULL, NULL, NULL, NULL, NULL, 1, 1, '状态', '业务对象状态, 枚举: active / inactive / deprecated'),
('shared-properties-00000008','ri.ont.shared.props.description', NULL, 'description', 'data', 'xsd:string',   NULL,
  0, 0, 0, NULL, 2000, NULL, NULL, NULL, 0, 1, '说明', '业务对象的详细说明, 不超过 2000 字符'),
('shared-properties-00000009','ri.ont.shared.props.longitude',  'dom_water_hydrology', 'longitude',  'data', 'xsd:decimal', NULL,
  0, 0, 1, NULL, NULL, NULL, '-180', '180', 1, 1, '经度', '地理坐标-经度, WGS84 坐标系, 范围 [-180, 180]'),
('shared-properties-00000010','ri.ont.shared.props.latitude',   'dom_water_hydrology', 'latitude',   'data', 'xsd:decimal', NULL,
  0, 0, 1, NULL, NULL, NULL, '-90', '90', 1, 1, '纬度', '地理坐标-纬度, WGS84 坐标系, 范围 [-90, 90]'),
('shared-properties-00000011','ri.ont.shared.props.altitude',   'dom_water_hydrology', 'altitude',   'data', 'xsd:decimal', NULL,
  0, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, 1, '高程', '地理坐标-高程 (单位: 米), 基于 1985 国家高程基准'),
('shared-properties-00000012','ri.ont.shared.props.creator',    NULL, 'creator',     'data', 'xsd:string',   NULL,
  0, 0, 0, NULL, 64, NULL, NULL, NULL, 1, 1, '创建人', '记录创建人账号或姓名');

-- 共享属性 ↔ 分组 关联 (ont_biz_group_class, group_type='shared_props')
INSERT OR IGNORE INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort) VALUES
-- 时间属性组
('sp-gc-01','group-sp-time',     'shared-properties-00000001', 'shared_props', NULL, 1),
('sp-gc-02','group-sp-time',     'shared-properties-00000002', 'shared_props', NULL, 2),
('sp-gc-03','group-sp-time',     'shared-properties-00000003', 'shared_props', NULL, 3),
('sp-gc-04','group-sp-time',     'shared-properties-00000004', 'shared_props', NULL, 4),
-- 标识属性组
('sp-gc-05','group-sp-identity', 'shared-properties-00000005', 'shared_props', NULL, 1),
('sp-gc-06','group-sp-identity', 'shared-properties-00000006', 'shared_props', NULL, 2),
('sp-gc-07','group-sp-identity', 'shared-properties-00000007', 'shared_props', NULL, 3),
('sp-gc-08','group-sp-identity', 'shared-properties-00000008', 'shared_props', NULL, 4),
('sp-gc-12','group-sp-identity', 'shared-properties-00000012', 'shared_props', NULL, 5),
-- 地理属性组
('sp-gc-09','group-sp-geo',      'shared-properties-00000009', 'shared_props', 'dom_water_hydrology', 1),
('sp-gc-10','group-sp-geo',      'shared-properties-00000010', 'shared_props', 'dom_water_hydrology', 2),
('sp-gc-11','group-sp-geo',      'shared-properties-00000011', 'shared_props', 'dom_water_hydrology', 3);
