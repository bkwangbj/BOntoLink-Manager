-- ============================================================
-- V3: 给常用对象类型补充一批"利于筛选"的属性
--   目的: 让实例探索的搜索栏能同时堆叠多个筛选条件(枚举/数值/日期/布尔混搭)
--   说明: 实例值由后端 InstanceMockService.genValue 按属性名/类型自动模拟,
--         含 状态/等级/地区 的字符串/枚举会落到小基数值集(前端自动变多选筛选)。
--   方言: 纯 INSERT,sqlite / postgresql 通用,放 common/。
-- ============================================================

-- 水文测站 (class ...001) —— 已有: 编码/名称/经度/纬度/水位/所属河流
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, is_primary, is_required, sort) VALUES
('cp-enr-hs-1','ri.ont.class.property.cp-enr-hs-1','class-00000000-0000-0000-0000-000000000001','runState','enum','运行状态',0,0,11),
('cp-enr-hs-2','ri.ont.class.property.cp-enr-hs-2','class-00000000-0000-0000-0000-000000000001','stationGrade','enum','测站等级',0,0,12),
('cp-enr-hs-3','ri.ont.class.property.cp-enr-hs-3','class-00000000-0000-0000-0000-000000000001','region','string','所属地区',0,0,13),
('cp-enr-hs-4','ri.ont.class.property.cp-enr-hs-4','class-00000000-0000-0000-0000-000000000001','buildYear','int','建站年份',0,0,14),
('cp-enr-hs-5','ri.ont.class.property.cp-enr-hs-5','class-00000000-0000-0000-0000-000000000001','commissionDate','date','启用日期',0,0,15),
('cp-enr-hs-6','ri.ont.class.property.cp-enr-hs-6','class-00000000-0000-0000-0000-000000000001','isOnline','boolean','是否在线',0,0,16),
('cp-enr-hs-7','ri.ont.class.property.cp-enr-hs-7','class-00000000-0000-0000-0000-000000000001','annualRainfall','decimal','年均降雨量',0,0,17);

-- 河流 (class ...002) —— 已有: 河流名称
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, is_primary, is_required, sort) VALUES
('cp-enr-rv-1','ri.ont.class.property.cp-enr-rv-1','class-00000000-0000-0000-0000-000000000002','riverGrade','enum','河流等级',0,0,11),
('cp-enr-rv-2','ri.ont.class.property.cp-enr-rv-2','class-00000000-0000-0000-0000-000000000002','runState','enum','治理状态',0,0,12),
('cp-enr-rv-3','ri.ont.class.property.cp-enr-rv-3','class-00000000-0000-0000-0000-000000000002','region','string','流经地区',0,0,13),
('cp-enr-rv-4','ri.ont.class.property.cp-enr-rv-4','class-00000000-0000-0000-0000-000000000002','waterSystem','string','所属水系',0,0,14),
('cp-enr-rv-5','ri.ont.class.property.cp-enr-rv-5','class-00000000-0000-0000-0000-000000000002','riverLength','decimal','河流长度',0,0,15),
('cp-enr-rv-6','ri.ont.class.property.cp-enr-rv-6','class-00000000-0000-0000-0000-000000000002','annualFlow','decimal','多年平均流量',0,0,16),
('cp-enr-rv-7','ri.ont.class.property.cp-enr-rv-7','class-00000000-0000-0000-0000-000000000002','sourceCity','string','发源城市',0,0,17),
('cp-enr-rv-8','ri.ont.class.property.cp-enr-rv-8','class-00000000-0000-0000-0000-000000000002','isPerennial','boolean','是否常年河',0,0,18);

-- 水库 (class ...004) —— 已有较少,补足
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, is_primary, is_required, sort) VALUES
('cp-enr-rs-1','ri.ont.class.property.cp-enr-rs-1','class-00000000-0000-0000-0000-000000000004','reservoirCode','string','水库编码',1,1,1),
('cp-enr-rs-2','ri.ont.class.property.cp-enr-rs-2','class-00000000-0000-0000-0000-000000000004','reservoirName','string','水库名称',0,1,2),
('cp-enr-rs-3','ri.ont.class.property.cp-enr-rs-3','class-00000000-0000-0000-0000-000000000004','reservoirGrade','enum','工程等级',0,0,3),
('cp-enr-rs-4','ri.ont.class.property.cp-enr-rs-4','class-00000000-0000-0000-0000-000000000004','runState','enum','运行状态',0,0,4),
('cp-enr-rs-5','ri.ont.class.property.cp-enr-rs-5','class-00000000-0000-0000-0000-000000000004','region','string','所属地区',0,0,5),
('cp-enr-rs-6','ri.ont.class.property.cp-enr-rs-6','class-00000000-0000-0000-0000-000000000004','totalCapacity','decimal','总库容',0,0,6),
('cp-enr-rs-7','ri.ont.class.property.cp-enr-rs-7','class-00000000-0000-0000-0000-000000000004','damHeight','decimal','坝高',0,0,7),
('cp-enr-rs-8','ri.ont.class.property.cp-enr-rs-8','class-00000000-0000-0000-0000-000000000004','buildYear','int','建成年份',0,0,8),
('cp-enr-rs-9','ri.ont.class.property.cp-enr-rs-9','class-00000000-0000-0000-0000-000000000004','isInOperation','boolean','是否运行',0,0,9);

-- 污染源 (class-wwae-01) —— 已有: 污染源类型
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, is_primary, is_required, sort) VALUES
('cp-enr-ps-1','ri.ont.class.property.cp-enr-ps-1','class-wwae-01','sourceCode','string','污染源编码',1,1,1),
('cp-enr-ps-2','ri.ont.class.property.cp-enr-ps-2','class-wwae-01','runState','enum','监管状态',0,0,3),
('cp-enr-ps-3','ri.ont.class.property.cp-enr-ps-3','class-wwae-01','riskGrade','enum','风险等级',0,0,4),
('cp-enr-ps-4','ri.ont.class.property.cp-enr-ps-4','class-wwae-01','region','string','所在地区',0,0,5),
('cp-enr-ps-5','ri.ont.class.property.cp-enr-ps-5','class-wwae-01','dischargeFlow','decimal','日排放流量',0,0,6),
('cp-enr-ps-6','ri.ont.class.property.cp-enr-ps-6','class-wwae-01','registerDate','date','登记日期',0,0,7),
('cp-enr-ps-7','ri.ont.class.property.cp-enr-ps-7','class-wwae-01','isLicensed','boolean','是否持证',0,0,8);

-- 水电站 (class-weng-01) —— 已有: 装机容量
INSERT INTO ont_class_property(id, rid, class_id, api_name, data_type, display_name, is_primary, is_required, sort) VALUES
('cp-enr-hp-1','ri.ont.class.property.cp-enr-hp-1','class-weng-01','plantCode','string','电站编码',1,1,1),
('cp-enr-hp-2','ri.ont.class.property.cp-enr-hp-2','class-weng-01','plantName','string','电站名称',0,1,2),
('cp-enr-hp-3','ri.ont.class.property.cp-enr-hp-3','class-weng-01','runState','enum','运行状态',0,0,4),
('cp-enr-hp-4','ri.ont.class.property.cp-enr-hp-4','class-weng-01','plantGrade','enum','电站等级',0,0,5),
('cp-enr-hp-5','ri.ont.class.property.cp-enr-hp-5','class-weng-01','region','string','所属地区',0,0,6),
('cp-enr-hp-6','ri.ont.class.property.cp-enr-hp-6','class-weng-01','annualOutput','decimal','年发电量',0,0,7),
('cp-enr-hp-7','ri.ont.class.property.cp-enr-hp-7','class-weng-01','commissionDate','date','投产日期',0,0,8),
('cp-enr-hp-8','ri.ont.class.property.cp-enr-hp-8','class-weng-01','isGridConnected','boolean','是否并网',0,0,9);
