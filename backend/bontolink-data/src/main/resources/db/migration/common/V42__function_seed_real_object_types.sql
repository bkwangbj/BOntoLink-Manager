-- =====================================================================
-- V42  函数种子数据对齐真实本体对象类  —  SQLite / PostgreSQL 共用
--
-- V41 的参数类型照文档截图写了 [Hydro] Station 这类示意名, 库里 ont_class 并没有
-- 这些类, 导致详情页「类型」链接点了跳不到对象详情。这里改成真实存在的对象类
-- (命名空间 + api_name), 由后端按裸类名反查 ont_class 补 object_class_id,
-- 因此这里刻意不写死 class id, 避免换库后出现悬空外键。
--
-- 映射:
--   [Hydro] Station           → [w_wtr_hyd] HydrologyStation  水文测站
--   [Soil]  MonitorPlot       → [w_wtr_sc]  SoilErosionPlot   水土流失监测样地
--   [Hydro] Reservoir         → [w_wtr_eng] Reservoir         水库
--   [Hydro] WaterLevelSeries  → [w_wtr_hyd] RunoffSeries      径流序列
--   [Hydro] Project           → [w_wtr_eng] HydropowerStation 水电站
-- =====================================================================

UPDATE ont_function_param SET param_type = '[w_wtr_hyd] HydrologyStation'
 WHERE param_type = '[Hydro] Station';

UPDATE ont_function_param SET param_type = '[w_wtr_sc] SoilErosionPlot'
 WHERE param_type = '[Soil] MonitorPlot';

UPDATE ont_function_param SET param_type = '[w_wtr_eng] Reservoir'
 WHERE param_type = '[Hydro] Reservoir';

UPDATE ont_function_param SET param_type = '[w_wtr_hyd] RunoffSeries'
 WHERE param_type = '[Hydro] WaterLevelSeries';

UPDATE ont_function_param SET param_type = '[w_wtr_eng] HydropowerStation'
 WHERE param_type = '[Hydro] Project';

-- 代码正文里的类型标识同步改名, 避免签名与代码对不上 (REPLACE 两方言通用)
UPDATE ont_function SET code_content = REPLACE(code_content, 'HydroStation', 'HydrologyStation')
 WHERE code_content LIKE '%HydroStation%';

UPDATE ont_function SET code_content = REPLACE(code_content, 'SoilMonitorPlot', 'SoilErosionPlot')
 WHERE code_content LIKE '%SoilMonitorPlot%';

UPDATE ont_function SET code_content = REPLACE(code_content, 'HydroReservoir', 'Reservoir')
 WHERE code_content LIKE '%HydroReservoir%';

UPDATE ont_function SET code_content = REPLACE(code_content, 'HydroProject', 'HydropowerStation')
 WHERE code_content LIKE '%HydroProject%';
