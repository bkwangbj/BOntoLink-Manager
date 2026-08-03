-- V33: 城乡供水管网与运维场景 —— 对象批次 + 三类基数链接 + 函数动作种子
-- 通用数据（SQLite 和 PostgreSQL 共用）
--
-- 业务场景: 城乡供水管网 + 运维管理
--  A. 10 个新对象类型（class-demo-01 ~ 10，分布在 供水/工程/水环境 领域）
--  B. 12 条链接关系: 一对多 ×4 / 多对一 ×4 / 多对多 ×4
--     其中 supplier-pipes 演示 is_data_source_rel=1 物理中间表关联（pipe_supplier_rel）
--  C. 10 个函数动作（m_type=3 / action_type=30，每对象一个，
--     含函数规则 ont_class_action_rule + 函数定义 + 入参映射）
-- 图谱镜像: ont_class_link 同步 12 条，供总览统计与知识图谱渲染
-- 幂等: 全部 ON CONFLICT DO NOTHING，可重复执行

-- ============================================================
-- 1. 新对象类型 ont_class
-- ============================================================
INSERT INTO ont_class (id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, rdfs_defined_by, description, icon, color, status, is_thing, is_nothing, is_common, create_time, update_time) VALUES
  ('class-demo-01','ri.ont.class.demo-01','WaterPlant','w_wtr_sup','dom_water_watersupply','自来水厂','WaterPlant','城乡供水源头设施，负责取水净化、制水与出厂加压供水','水利公共本体库','制水与供水枢纽工程','factory','#165DFF',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-02','ri.ont.class.demo-02','PipeSegment','w_wtr_sup','dom_water_watersupply','输水管段','PipeSegment','相邻两节点之间的供水管段（含管径、材质、埋深、敷设年份）','水利公共本体库','供水管网基础单元','wave','#00B42A',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-03','ri.ont.class.demo-03','SupplyZone','w_wtr_sup','dom_water_watersupply','供水片区','SupplyZone','分区计量与服务的供水片区，通常由一条干管与若干支管覆盖','水利公共本体库','分区计量服务单元','grid','#FF7D00',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-04','ri.ont.class.demo-04','PipelineValve','w_wtr_sup','dom_water_watersupply','管网阀门','PipelineValve','管网中控制、检修用阀门（闸阀/蝶阀/三通）','水利公共本体库','管网控制设施','sliders','#722ED1',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-05','ri.ont.class.demo-05','FlowMeter','w_wtr_sup','dom_water_watersupply','流量计','FlowMeter','分区计量与用户计量的流量计量设备','水利公共本体库','供水计量设备','sliders','#13C2C2',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-06','ri.ont.class.demo-06','MeterReading','w_wtr_sup','dom_water_watersupply','抄表记录','MeterReading','流量计/水表的抄表读数记录（远传或人工）','水利公共本体库','计量抄表数据','list','#FADB14',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-07','ri.ont.class.demo-07','MaintenanceTask','w_wtr_eng','dom_water_engineering','维修工单','MaintenanceTask','管网爆管、渗漏等故障的维修任务单','水利公共本体库','管网运维工单','shield','#F53F3F',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-08','ri.ont.class.demo-08','MaintenanceCrew','w_wtr_eng','dom_water_engineering','维修班组','MaintenanceCrew','执行管网维修任务的班组，含抢修队伍与日常巡护组','水利公共本体库','管网抢修力量','team','#00B42A',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-09','ri.ont.class.demo-09','MaterialSupplier','w_wtr_sup','dom_water_watersupply','管材供应商','MaterialSupplier','为管网提供管材、阀门等物资的供应商','水利公共本体库','物资采购供应方','shop','#EB2F96',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-demo-10','ri.ont.class.demo-10','WaterQualitySample','w_wtr_wae','dom_water_environment','水质水样','WaterQualitySample','出厂水、管网末梢水的采样检测记录','水利公共本体库','供水水质监测样本','droplet','#722ED1',1,1,0,0,'2026-08-02 12:00:00','2026-08-02 12:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 2. 链接类型 ont_link_types
--    一对多: l=one, r=many        （waterplant-pipes / waterplant-tasks / zone-readings / zone-valves）
--    多对一: l=many, r=one        （meter-pipe / task-pipe / sample-zone / crew-plant）
--    多对多: l=many, r=many       （valve-pipes / crew-tasks / supplier-pipes / crew-pipes）
-- ============================================================
INSERT INTO ont_link_types (id, link_type_id, rid, status, l_object_type_id, r_object_type_id, l_cardinality, r_cardinality, l_display_name, l_plural_name, r_display_name, r_plural_name, l_visibility, r_visibility, l_api_name, r_api_name, l_enabled, r_enabled, is_data_source_rel, rel_data_table, rdfs_label, rdfs_comment, category_code, created_at, updated_at, created_by, updated_by) VALUES
  -- 一对多 (1:N)
  ('link-types-demo-01','waterplant-pipes','ri.ont.link.types.waterplant-pipes','active','class-demo-01','class-demo-02','one','many','供水管段',NULL,'所属水厂','水厂管段','prominent','normal','supplyPipes','belongsToPlant',1,1,0,NULL,'水厂-管段 供水','一座自来水厂铺设并管辖多段供水管段；每段管段归属唯一水厂','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-02','waterplant-tasks','ri.ont.link.types.waterplant-tasks','active','class-demo-01','class-demo-07','one','many','维修工单',NULL,'所属水厂','水厂工单','normal','normal','issuesTasks','occursAtPlant',1,1,0,NULL,'水厂-维修工单 管辖','一座水厂管辖并下发多张维修工单；每张工单归属唯一水厂','dom_water_engineering','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-03','zone-readings','ri.ont.link.types.zone-readings','active','class-demo-03','class-demo-06','one','many','抄表记录',NULL,'所属片区','片区抄表','normal','normal','hasReadings','belongsToZone',1,1,0,NULL,'片区-抄表记录 产生','一个供水片区产生多条抄表记录；每条记录归属唯一片区','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-04','zone-valves','ri.ont.link.types.zone-valves','active','class-demo-03','class-demo-04','one','many','辖内阀门',NULL,'所属片区','片区阀门','normal','normal','hasValves','locatedInZone',1,1,0,NULL,'片区-阀门 管辖','一个供水片区辖内有多台阀门；每台阀门归属唯一片区','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  -- 多对一 (N:1)
  ('link-types-demo-05','meter-pipe','ri.ont.link.types.meter-pipe','active','class-demo-05','class-demo-02','many','one','安装管段','管段流量计','安装流量计',NULL,'normal','normal','installedOnPipe','hasFlowMeters',1,1,0,NULL,'流量计-管段 安装','多台流量计可安装在同一条管段上；每台流量计对应唯一安装管段','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-06','task-pipe','ri.ont.link.types.task-pipe','active','class-demo-07','class-demo-02','many','one','维修管段','管段工单','维修工单',NULL,'normal','normal','targetsPipe','hasTasks',1,1,0,NULL,'维修工单-管段 目标','多张维修工单针对同一条管段；每张工单维修唯一目标管段','dom_water_engineering','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-07','sample-zone','ri.ont.link.types.sample-zone','active','class-demo-10','class-demo-03','many','one','采样片区','片区水样','水质水样',NULL,'normal','normal','sampledFromZone','hasSamples',1,1,0,NULL,'水质水样-片区 采样','多个水质水样采自同一供水片区；每个水样对应唯一采样片区','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-08','crew-plant','ri.ont.link.types.crew-plant','active','class-demo-08','class-demo-01','many','one','隶属水厂','水厂班组','维修班组',NULL,'normal','normal','affiliatedToPlant','hasCrews',1,1,0,NULL,'维修班组-水厂 隶属','多个维修班组挂靠同一座水厂；每个班组隶属唯一水厂','dom_water_engineering','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  -- 多对多 (N:N)
  ('link-types-demo-09','valve-pipes','ri.ont.link.types.valve-pipes','active','class-demo-04','class-demo-02','many','many','连通管段','阀门管段','连接阀门','管段阀门','normal','normal','connectsPipes','connectedValves',1,1,0,NULL,'阀门-管段 连通','一台阀门（三通/检修阀）连通多段管段；一段管段上可安装多台阀门','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-10','crew-tasks','ri.ont.link.types.crew-tasks','active','class-demo-08','class-demo-07','many','many','参与工单','班组工单','参与班组','工单班组','normal','normal','handlesTasks','handledByCrews',1,1,0,NULL,'班组-工单 参与','一个维修班组参与多张工单；一张工单可由多个班组协同完成','dom_water_engineering','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  -- 多对多 + 物理中间表（is_data_source_rel=1），中间表 pipe_supplier_rel 由外部数据源提供
  ('link-types-demo-11','supplier-pipes','ri.ont.link.types.supplier-pipes','active','class-demo-09','class-demo-02','many','many','供应管段','供应管材','管材供应商','采购供应商','normal','normal','suppliesPipes','sourcedFrom',1,1,1,'pipe_supplier_rel','供应商-管段 供应','一个供应商向多个管段供货；一段管段的管材可来自多个供应商，经物理中间表 pipe_supplier_rel 关联','dom_water_watersupply','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin'),
  ('link-types-demo-12','crew-pipes','ri.ont.link.types.crew-pipes','active','class-demo-08','class-demo-02','many','many','维护管段','班组管段','维护班组','管段班组','normal','normal','maintainsPipes','maintainedByCrews',1,1,0,NULL,'班组-管段 维护','一个维修班组负责维护多段管段；一段管段由多个班组轮巡维护','dom_water_engineering','2026-08-02 12:00:00','2026-08-02 12:00:00','admin','admin')
ON CONFLICT (link_type_id) DO NOTHING;

-- ============================================================
-- 3. 字段映射 ont_link_mappings
--    每链接左右各一条; supplier-pipes 填中间表列名 join_table_column
-- ============================================================
INSERT INTO ont_link_mappings (mapping_id, link_id, side, seq, object_field, join_table_column, created_at, updated_at) VALUES
  -- waterplant-pipes
  ('link-mappings-demo-0101','link-types-demo-01','left',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0102','link-types-demo-01','right',1,'plant_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- waterplant-tasks
  ('link-mappings-demo-0201','link-types-demo-02','left',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0202','link-types-demo-02','right',1,'plant_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- zone-readings
  ('link-mappings-demo-0301','link-types-demo-03','left',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0302','link-types-demo-03','right',1,'zone_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- zone-valves
  ('link-mappings-demo-0401','link-types-demo-04','left',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0402','link-types-demo-04','right',1,'zone_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- meter-pipe
  ('link-mappings-demo-0501','link-types-demo-05','left',1,'pipe_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0502','link-types-demo-05','right',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- task-pipe
  ('link-mappings-demo-0601','link-types-demo-06','left',1,'pipe_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0602','link-types-demo-06','right',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- sample-zone
  ('link-mappings-demo-0701','link-types-demo-07','left',1,'zone_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0702','link-types-demo-07','right',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- crew-plant
  ('link-mappings-demo-0801','link-types-demo-08','left',1,'plant_id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0802','link-types-demo-08','right',1,'id',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- valve-pipes
  ('link-mappings-demo-0901','link-types-demo-09','left',1,'pipe_ids',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-0902','link-types-demo-09','right',1,'valve_ids',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- crew-tasks
  ('link-mappings-demo-1001','link-types-demo-10','left',1,'task_ids',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-1002','link-types-demo-10','right',1,'crew_ids',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- supplier-pipes (物理中间表列)
  ('link-mappings-demo-1101','link-types-demo-11','left',1,'supplier_id','supplier_id','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-1102','link-types-demo-11','right',1,'pipe_id','pipe_id','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- crew-pipes
  ('link-mappings-demo-1201','link-types-demo-12','left',1,'pipe_ids',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('link-mappings-demo-1202','link-types-demo-12','right',1,'crew_ids',NULL,'2026-08-02 12:00:00','2026-08-02 12:00:00')
ON CONFLICT (mapping_id) DO NOTHING;

-- ============================================================
-- 4. 图谱镜像 ont_class_link（总览统计 / 知识图谱边）
--    与 ont_link_types 一一对应，cardinality 使用四值枚举
--    注: PG 方言 api_name 无 UNIQUE 约束, 冲突目标统一用主键 id
-- ============================================================
INSERT INTO ont_class_link (id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, rdfs_label, rdfs_comment, status, create_time, update_time) VALUES
  -- 一对多
  ('class-link-demo-01','ri.ont.class.link.plantSupplyPipes','plantSupplyPipes','class-demo-01','class-demo-02','one_to_many','水厂-管段 供水','水厂-管段 供水','一座自来水厂铺设并管辖多段供水管段；每段管段归属唯一水厂',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-02','ri.ont.class.link.plantIssuesTasks','plantIssuesTasks','class-demo-01','class-demo-07','one_to_many','水厂-维修工单 管辖','水厂-维修工单 管辖','一座水厂管辖并下发多张维修工单；每张工单归属唯一水厂',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-03','ri.ont.class.link.zoneHasReadings','zoneHasReadings','class-demo-03','class-demo-06','one_to_many','片区-抄表记录 产生','片区-抄表记录 产生','一个供水片区产生多条抄表记录；每条记录归属唯一片区',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-04','ri.ont.class.link.zoneHasValves','zoneHasValves','class-demo-03','class-demo-04','one_to_many','片区-阀门 管辖','片区-阀门 管辖','一个供水片区辖内有多台阀门；每台阀门归属唯一片区',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- 多对一
  ('class-link-demo-05','ri.ont.class.link.meterInstalledOnPipe','meterInstalledOnPipe','class-demo-05','class-demo-02','many_to_one','流量计-管段 安装','流量计-管段 安装','多台流量计可安装在同一条管段上；每台流量计对应唯一安装管段',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-06','ri.ont.class.link.taskTargetsPipe','taskTargetsPipe','class-demo-07','class-demo-02','many_to_one','维修工单-管段 目标','维修工单-管段 目标','多张维修工单针对同一条管段；每张工单维修唯一目标管段',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-07','ri.ont.class.link.sampleFromZone','sampleFromZone','class-demo-10','class-demo-03','many_to_one','水质水样-片区 采样','水质水样-片区 采样','多个水质水样采自同一供水片区；每个水样对应唯一采样片区',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-08','ri.ont.class.link.crewBelongsToPlant','crewBelongsToPlant','class-demo-08','class-demo-01','many_to_one','维修班组-水厂 隶属','维修班组-水厂 隶属','多个维修班组挂靠同一座水厂；每个班组隶属唯一水厂',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  -- 多对多
  ('class-link-demo-09','ri.ont.class.link.valveConnectsPipes','valveConnectsPipes','class-demo-04','class-demo-02','many_to_many','阀门-管段 连通','阀门-管段 连通','一台阀门（三通/检修阀）连通多段管段；一段管段上可安装多台阀门',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-10','ri.ont.class.link.crewHandlesTasks','crewHandlesTasks','class-demo-08','class-demo-07','many_to_many','班组-工单 参与','班组-工单 参与','一个维修班组参与多张工单；一张工单可由多个班组协同完成',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-11','ri.ont.class.link.supplierProvidesPipes','supplierProvidesPipes','class-demo-09','class-demo-02','many_to_many','供应商-管段 供应','供应商-管段 供应','一个供应商向多个管段供货；一段管段的管材可来自多个供应商',1,'2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class-link-demo-12','ri.ont.class.link.crewMaintainsPipes','crewMaintainsPipes','class-demo-08','class-demo-02','many_to_many','班组-管段 维护','班组-管段 维护','一个维修班组负责维护多段管段；一段管段由多个班组轮巡维护',1,'2026-08-02 12:00:00','2026-08-02 12:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 5. 函数动作主表 ont_class_action（m_type=3 函数 / action_type=30 函数执行）
--    对应 A 部分 10 个对象，各挂一个函数执行动作
-- ============================================================
INSERT INTO ont_class_action
  (id, rid, api_name, m_type, action_type, object_class_id, function_code, category_code,
   show_on_detail, show_on_batch, button_text, compile_status, form_enabled,
   submit_criteria_enabled, status, current_version, is_deleted, icon, color,
   rdfs_label, rdfs_comment, rdfs_defined_by, create_time, update_time) VALUES
  ('class_action-demo-fn-01','ri.ont.action.plant_water_balance','plant_water_balance',3,30,'class-demo-01','fn.water.plantBalance','dom_water_watersupply',1,0,'水量核算',1,1,0,1,'v1',0,'code','#722ED1','水厂水量平衡核算','核算进厂水量与出厂供水量平衡率, 识别产销差(漏损+未计费用水)','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-02','ri.ont.action.pipe_flow_capacity','pipe_flow_capacity',3,30,'class-demo-02','fn.water.pipeFlowCapacity','dom_water_watersupply',1,0,'能力校验',1,1,0,1,'v1',0,'code','#722ED1','管段输水能力校验','按管径/材质/埋深校验当前输水流量是否超过设计输水能力','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-03','ri.ont.action.zone_night_flow','zone_night_flow',3,30,'class-demo-03','fn.water.zoneNightFlow','dom_water_watersupply',1,0,'夜间流量',1,1,0,1,'v1',0,'code','#722ED1','片区夜间最小流量分析','通过夜间最小流量与片区面积识别渗漏与异常用水(DMA 分区计量分析)','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-04','ri.ont.action.valve_op_risk','valve_op_risk',3,30,'class-demo-04','fn.water.valveOpRisk','dom_water_watersupply',1,0,'风险评估',1,1,0,1,'v1',0,'code','#722ED1','阀门操作风险评估','评估阀门启闭对下游水压/水质影响及操作风险等级','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-05','ri.ont.action.meter_calibration','meter_calibration',3,30,'class-demo-05','fn.water.meterCalibration','dom_water_watersupply',1,0,'校准核查',1,1,0,1,'v1',0,'code','#722ED1','流量计计量校准核查','对比同期上下游表计偏差并结合投用年限, 标记失准流量计','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-06','ri.ont.action.reading_anomaly','reading_anomaly',3,30,'class-demo-06','fn.water.readingAnomaly','dom_water_watersupply',1,0,'异常检测',1,1,0,1,'v1',0,'code','#722ED1','抄表异常检测','检测零读数/突增突降/倒走等抄表异常记录','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-07','ri.ont.action.task_cost_estimate','task_cost_estimate',3,30,'class-demo-07','fn.eng.taskCostEstimate','dom_water_engineering',1,0,'成本估算',1,1,0,1,'v1',0,'code','#722ED1','维修工单成本估算','按管材/工时/机械费用估算单张工单维修成本','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-08','ri.ont.action.crew_workload','crew_workload',3,30,'class-demo-08','fn.eng.crewWorkload','dom_water_engineering',1,0,'工作量统计',1,1,0,1,'v1',0,'code','#722ED1','班组工作量统计','汇总班组工单数与抢修时长, 计算人均工时与响应达标率','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-09','ri.ont.action.supplier_credit','supplier_credit',3,30,'class-demo-09','fn.water.supplierCredit','dom_water_watersupply',1,0,'信用评估',1,1,0,1,'v1',0,'code','#722ED1','供应商信用评估','结合供货及时率/退换货率/质保单合格率评估供应商信用','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('class_action-demo-fn-10','ri.ont.action.sample_assess','sample_assess',3,30,'class-demo-10','fn.env.sampleAssess','dom_water_environment',1,0,'达标评估',1,1,0,1,'v1',0,'code','#722ED1','水质达标评估','对照 GB5749 生活饮用水卫生标准限值评估水样达标情况','水利公共本体库','2026-08-02 12:00:00','2026-08-02 12:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 6. 函数规则 ont_class_action_rule（rule_type=2 副作用规则, rule_config.kind=function）
--    入参配置与 8 函数参数映射一一对应
-- ============================================================
INSERT INTO ont_class_action_rule
  (id, action_id, action_type, rule_type, rule_name, sort, status, rule_config, create_time, update_time) VALUES
  ('action-rule-demo-fn-01','class_action-demo-fn-01',30,2,'水厂水量平衡核算',0,1,
   '{"kind":"function","func_code":"fn.water.plantBalance","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"进厂水量","param_type":"number","required":1,"value_source":1,"value_content":"inlet_flow"},{"name":"出厂供水量","param_type":"number","required":1,"value_source":1,"value_content":"outlet_flow"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-02','class_action-demo-fn-02',30,2,'管段输水能力校验',0,1,
   '{"kind":"function","func_code":"fn.water.pipeFlowCapacity","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"管径","param_type":"number","required":1,"value_source":1,"value_content":"diameter_mm"},{"name":"输水流量","param_type":"number","required":1,"value_source":1,"value_content":"flow_rate"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-03','class_action-demo-fn-03',30,2,'片区夜间最小流量分析',0,1,
   '{"kind":"function","func_code":"fn.water.zoneNightFlow","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"夜间最小流量","param_type":"number","required":1,"value_source":1,"value_content":"min_night_flow"},{"name":"片区面积","param_type":"number","required":1,"value_source":1,"value_content":"zone_area"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-04','class_action-demo-fn-04',30,2,'阀门操作风险评估',0,1,
   '{"kind":"function","func_code":"fn.water.valveOpRisk","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"阀门类型","param_type":"string","required":1,"value_source":1,"value_content":"valve_type"},{"name":"启闭方式","param_type":"string","required":1,"value_source":1,"value_content":"op_mode"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-05','class_action-demo-fn-05',30,2,'流量计计量校准核查',0,1,
   '{"kind":"function","func_code":"fn.water.meterCalibration","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"偏差率","param_type":"number","required":1,"value_source":1,"value_content":"diff_rate"},{"name":"投用年限","param_type":"number","required":1,"value_source":1,"value_content":"meter_age"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-06','class_action-demo-fn-06',30,2,'抄表异常检测',0,1,
   '{"kind":"function","func_code":"fn.water.readingAnomaly","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"本期读数","param_type":"number","required":1,"value_source":1,"value_content":"reading_value"},{"name":"上期读数","param_type":"number","required":1,"value_source":1,"value_content":"prev_value"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-07','class_action-demo-fn-07',30,2,'维修工单成本估算',0,1,
   '{"kind":"function","func_code":"fn.eng.taskCostEstimate","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"维修长度","param_type":"number","required":1,"value_source":1,"value_content":"pipe_length"},{"name":"管材单价","param_type":"number","required":1,"value_source":1,"value_content":"material_unit"},{"name":"维修工时","param_type":"number","required":1,"value_source":1,"value_content":"work_hours"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-08','class_action-demo-fn-08',30,2,'班组工作量统计',0,1,
   '{"kind":"function","func_code":"fn.eng.crewWorkload","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"工单数","param_type":"number","required":1,"value_source":1,"value_content":"task_count"},{"name":"总工时","param_type":"number","required":1,"value_source":1,"value_content":"total_hours"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-09','class_action-demo-fn-09',30,2,'供应商信用评估',0,1,
   '{"kind":"function","func_code":"fn.water.supplierCredit","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"供货及时率","param_type":"number","required":1,"value_source":1,"value_content":"ontime_rate"},{"name":"退换货率","param_type":"number","required":1,"value_source":1,"value_content":"return_rate"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00'),
  ('action-rule-demo-fn-10','class_action-demo-fn-10',30,2,'水质达标评估',0,1,
   '{"kind":"function","func_code":"fn.env.sampleAssess","func_version":"v1","func_autoupgrade":1,"func_params":[{"name":"酸碱度","param_type":"number","required":1,"value_source":1,"value_content":"ph"},{"name":"浊度","param_type":"number","required":1,"value_source":1,"value_content":"turbidity"},{"name":"余氯","param_type":"number","required":1,"value_source":1,"value_content":"residual_chlorine"}],"func_exec_identity":"caller","func_error_strategy":"rollback","func_timeout":30,"func_retry":0,"func_concurrent":0,"func_return_attachment":1,"func_exceptions":[]}',
   '2026-08-02 12:00:00','2026-08-02 12:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 7. 函数定义 ont_action_function_rule_config
--    runtime/entry 说明: 外部函数运行时声明, 实际注册在函数平台
-- ============================================================
INSERT INTO ont_action_function_rule_config
  (id, action_id, rule_id, function_code, function_name, version, runtime, entry, timeout_ms, config) VALUES
  ('action-func-demo-01','class_action-demo-fn-01','action-rule-demo-fn-01','fn.water.plantBalance','水厂水量平衡核算','v1','python3','water/plant_balance.py',30000,'{"unit":"m3","report_channel":"dash"}'),
  ('action-func-demo-02','class_action-demo-fn-02','action-rule-demo-fn-02','fn.water.pipeFlowCapacity','管段输水能力校验','v1','python3','water/pipe_capacity.py',30000,'{"unit":"m3/h","formula":"Hazen-Williams"}'),
  ('action-func-demo-03','class_action-demo-fn-03','action-rule-demo-fn-03','fn.water.zoneNightFlow','片区夜间最小流量分析','v1','python3','water/zone_night_flow.py',60000,'{"method":"DMA-min-flow","window":"02:00-04:00"}'),
  ('action-func-demo-04','class_action-demo-fn-04','action-rule-demo-fn-04','fn.water.valveOpRisk','阀门操作风险评估','v1','python3','water/valve_op_risk.py',30000,'{"levels":["低","中","高"]}'),
  ('action-func-demo-05','class_action-demo-fn-05','action-rule-demo-fn-05','fn.water.meterCalibration','流量计计量校准核查','v1','python3','water/meter_calibration.py',30000,'{"threshold":0.05,"age_limit":8}'),
  ('action-func-demo-06','class_action-demo-fn-06','action-rule-demo-fn-06','fn.water.readingAnomaly','抄表异常检测','v1','python3','water/reading_anomaly.py',30000,'{"rules":["zero","spike","backflow"]}'),
  ('action-func-demo-07','class_action-demo-fn-07','action-rule-demo-fn-07','fn.eng.taskCostEstimate','维修工单成本估算','v1','python3','eng/task_cost_estimate.py',30000,'{"currency":"CNY"}'),
  ('action-func-demo-08','class_action-demo-fn-08','action-rule-demo-fn-08','fn.eng.crewWorkload','班组工作量统计','v1','python3','eng/crew_workload.py',60000,'{"metric":"person-hour"}'),
  ('action-func-demo-09','class_action-demo-fn-09','action-rule-demo-fn-09','fn.water.supplierCredit','供应商信用评估','v1','python3','water/supplier_credit.py',30000,'{"score_range":[0,100]}'),
  ('action-func-demo-10','class_action-demo-fn-10','action-rule-demo-fn-10','fn.env.sampleAssess','水质达标评估','v1','python3','env/sample_assess.py',30000,'{"standard":"GB5749-2022"}')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 8. 函数入参映射 ont_action_function_param_mapping
--    direction: in=入参(函数输入) / out=出参(函数返回值)
--    value_source: 1来自参数 2静态值 3当前用户 4系统时间 5对象参数属性
-- ============================================================
INSERT INTO ont_action_function_param_mapping
  (id, function_config_id, param_code, param_name, value_source, value_content, direction, sort) VALUES
  -- fn-01 水厂水量平衡
  ('action-fnparam-demo-0101','action-func-demo-01','inlet_flow','进厂水量',1,'inlet_flow','in',1),
  ('action-fnparam-demo-0102','action-func-demo-01','outlet_flow','出厂供水量',1,'outlet_flow','in',2),
  ('action-fnparam-demo-0103','action-func-demo-01','balance_rate','平衡率',1,'balance_rate','out',3),
  -- fn-02 管段输水能力
  ('action-fnparam-demo-0201','action-func-demo-02','diameter_mm','管径(mm)',1,'diameter_mm','in',1),
  ('action-fnparam-demo-0202','action-func-demo-02','flow_rate','输水流量',1,'flow_rate','in',2),
  ('action-fnparam-demo-0203','action-func-demo-02','utilization_ratio','负荷率',1,'utilization_ratio','out',3),
  -- fn-03 片区夜间流量
  ('action-fnparam-demo-0301','action-func-demo-03','min_night_flow','夜间最小流量',1,'min_night_flow','in',1),
  ('action-fnparam-demo-0302','action-func-demo-03','zone_area','片区面积',1,'zone_area','in',2),
  ('action-fnparam-demo-0303','action-func-demo-03','leak_score','渗漏评分',1,'leak_score','out',3),
  -- fn-04 阀门操作风险
  ('action-fnparam-demo-0401','action-func-demo-04','valve_type','阀门类型',1,'valve_type','in',1),
  ('action-fnparam-demo-0402','action-func-demo-04','op_mode','启闭方式',1,'op_mode','in',2),
  ('action-fnparam-demo-0403','action-func-demo-04','risk_level','风险等级',1,'risk_level','out',3),
  -- fn-05 流量计校准
  ('action-fnparam-demo-0501','action-func-demo-05','diff_rate','偏差率',1,'diff_rate','in',1),
  ('action-fnparam-demo-0502','action-func-demo-05','meter_age','投用年限',1,'meter_age','in',2),
  ('action-fnparam-demo-0503','action-func-demo-05','need_calibrate','需校准',1,'need_calibrate','out',3),
  -- fn-06 抄表异常
  ('action-fnparam-demo-0601','action-func-demo-06','reading_value','本期读数',1,'reading_value','in',1),
  ('action-fnparam-demo-0602','action-func-demo-06','prev_value','上期读数',1,'prev_value','in',2),
  ('action-fnparam-demo-0603','action-func-demo-06','anomaly_flag','异常标记',1,'anomaly_flag','out',3),
  -- fn-07 维修成本
  ('action-fnparam-demo-0701','action-func-demo-07','pipe_length','维修长度',1,'pipe_length','in',1),
  ('action-fnparam-demo-0702','action-func-demo-07','material_unit','管材单价',1,'material_unit','in',2),
  ('action-fnparam-demo-0703','action-func-demo-07','work_hours','维修工时',1,'work_hours','in',3),
  ('action-fnparam-demo-0704','action-func-demo-07','total_cost','估算成本',1,'total_cost','out',4),
  -- fn-08 班组工作量
  ('action-fnparam-demo-0801','action-func-demo-08','task_count','工单数',1,'task_count','in',1),
  ('action-fnparam-demo-0802','action-func-demo-08','total_hours','总工时',1,'total_hours','in',2),
  ('action-fnparam-demo-0803','action-func-demo-08','avg_hours','人均工时',1,'avg_hours','out',3),
  -- fn-09 供应商信用
  ('action-fnparam-demo-0901','action-func-demo-09','ontime_rate','供货及时率',1,'ontime_rate','in',1),
  ('action-fnparam-demo-0902','action-func-demo-09','return_rate','退换货率',1,'return_rate','in',2),
  ('action-fnparam-demo-0903','action-func-demo-09','credit_score','信用分',1,'credit_score','out',3),
  -- fn-10 水质达标
  ('action-fnparam-demo-1001','action-func-demo-10','ph','酸碱度',1,'ph','in',1),
  ('action-fnparam-demo-1002','action-func-demo-10','turbidity','浊度',1,'turbidity','in',2),
  ('action-fnparam-demo-1003','action-func-demo-10','residual_chlorine','余氯',1,'residual_chlorine','in',3),
  ('action-fnparam-demo-1004','action-func-demo-10','pass_flag','达标判定',1,'pass_flag','out',4)
ON CONFLICT (id) DO NOTHING;
