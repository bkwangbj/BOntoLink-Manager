-- =====================================================================
-- V41  函数 (Functions) 模块种子数据  —  SQLite / PostgreSQL 共用
-- 数据取自《本体管理系统-函数Functions.pdf》5.1 列表页与 5.3 详情页截图。
-- 只用标准 INSERT + 字面量, 不用任何方言函数, 保证两方言通跑。
-- getHydrologyStationThresholds 额外造了 v1.1.0 历史版本, 用于验证
-- uk_path_version 约束与详情页的版本切换。
-- =====================================================================

-- ---------- 版本库 ----------
INSERT INTO ont_version_repo
  (id, rid, industry_dir, category_dir, version_no, repo_branch, repo_commit_id, repo_url,
   version_status, is_default, release_note, publish_user, publish_time, is_deleted, create_time, update_time)
VALUES
  ('ont_version_repo-7a2d9f1c-8b3e-4d5a-9c6e-7f8a9b0c1d2e', 'ri.ont.version_repo.7a2d9f1c8b3e4d5a9c6e7f8a9b0c1d2e',
   '水利', '水文监测函数集', 'v1.1.2', 'release/1.1.x', '7a2d9f1c8b3e4d5a9c6e7f8a9b0c1d2e3f4a5b6c',
   'https://git.internal/hydro/hydro-function-lib', 3, 1, '新增干旱站阈值计算逻辑', 'chenbing',
   '2026-08-01 14:20:00', 0, '2026-07-15 09:53:00', '2026-08-01 14:20:00'),
  ('ont_version_repo-6b1c8e0d-7a2f-4c3b-8d5e-6f7a8b9c0d1e', 'ri.ont.version_repo.6b1c8e0d7a2f4c3b8d5e6f7a8b9c0d1e',
   '水利', '水文监测函数集', 'v1.1.0', 'release/1.1.x', '6b1c8e0d7a2f4c3b8d5e6f7a8b9c0d1e2f3a4b5c',
   'https://git.internal/hydro/hydro-function-lib', 3, 0, '阈值计算首个稳定版', 'chenbing',
   '2026-06-20 10:05:00', 0, '2026-06-10 09:00:00', '2026-06-20 10:05:00'),
  ('ont_version_repo-5c2d9f1e-8b3a-4d4c-9e6f-7a8b9c0d1e2f', 'ri.ont.version_repo.5c2d9f1e8b3a4d4c9e6f7a8b9c0d1e2f',
   '水利', '水文监测函数集', 'v1.0.3', 'release/1.0.x', '5c2d9f1e8b3a4d4c9e6f7a8b9c0d1e2f3a4b5c6d',
   'https://git.internal/hydro/hydro-function-lib', 3, 0, '水位异常检测算法调参', 'liuyang',
   '2026-05-18 16:40:00', 0, '2026-05-06 11:20:00', '2026-05-18 16:40:00'),
  ('ont_version_repo-4d3e0a2f-9c4b-4e5d-0f7a-8b9c0d1e2f3a', 'ri.ont.version_repo.4d3e0a2f9c4b4e5d0f7a8b9c0d1e2f3a',
   '水利', '水土保持函数集', 'v1.2.0', 'feature/soil-loss', '4d3e0a2f9c4b4e5d0f7a8b9c0d1e2f3a4b5c6d7e',
   'https://git.internal/hydro/soil-function-lib', 1, 1, '土壤流失量模型草稿', 'zhaomin',
   NULL, 0, '2026-07-28 09:10:00', '2026-08-03 17:02:00'),
  ('ont_version_repo-3e4f1b3a-0d5c-4f6e-1a8b-9c0d1e2f3a4b', 'ri.ont.version_repo.3e4f1b3a0d5c4f6e1a8b9c0d1e2f3a4b',
   '水利', '水利工程函数集', 'v1.0.0', 'main', '3e4f1b3a0d5c4f6e1a8b9c0d1e2f3a4b5c6d7e8f',
   'https://git.internal/hydro/project-function-lib', 3, 1, '工程运行年限派生属性上线', 'chenbing',
   '2026-07-23 15:30:00', 0, '2026-07-12 14:00:00', '2026-07-23 15:30:00'),
  ('ont_version_repo-2f5a2c4b-1e6d-4a7f-2b9c-0d1e2f3a4b5c', 'ri.ont.version_repo.2f5a2c4b1e6d4a7f2b9c0d1e2f3a4b5c',
   '水利', '水利工程函数集', 'v0.0.1', 'feature/reservoir-create', '2f5a2c4b1e6d4a7f2b9c0d1e2f3a4b5c6d7e8f9a',
   'https://git.internal/hydro/project-function-lib', 1, 0, '水库建档动作函数初稿', 'wanghao',
   NULL, 0, '2026-08-04 09:16:00', '2026-08-04 09:16:00');

-- ---------- 函数主表 ----------
-- f1 水文站阈值计算 (v1.1.2 最新)
INSERT INTO ont_function
  (id, rid, version_no, api_name, function_label, function_type, language,
   industry_dir, category_dir, class_name, full_access_path, code_file_path, code_md5, code_content,
   file_line_start, file_line_end, status, visibility, rdfs_label, rdfs_comment, rdfs_see_also,
   rdfs_defined_by, create_user, publish_time, is_deleted, create_time, update_time)
VALUES
  ('ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', 'ri.ont.function.1686f24c254240819a3fa844fbe86a31',
   'v1.1.2', 'getHydrologyStationThresholds', '水文站阈值计算', 1, 2,
   '水利', '水文监测函数集', 'ThresholdCalculator',
   '/水利/水文监测函数集/ThresholdCalculator/getHydrologyStationThresholds',
   'thresholds/calc.ts', 'a844fbe86a3f1686f24c254240819a3f',
'@Function()
public getHydrologyStationThresholds(station: HydroStation): ThresholdsResult {
  const stationId = station.stationId;
  console.log("获取水文站阈值, 站点ID: ", stationId);

  // 调用通用工具函数计算站龄
  const age = this.helperFunctions.calculateStationAge(station);

  if (age === undefined) {
    // 注意: 此处刻意不用模板字符串拼接, 否则会被 Flyway 当成占位符解析而报错
    throw new Error("无法计算阈值: 水文站 " + stationId + " 无投运日期");
  }

  console.log("水文站站龄: ", age);

  // 根据站龄确定等级与基准阈值
  let levelGroup: string;
  let baseThresholds: ThresholdsResult;

  if (age >= 20) {
    levelGroup = "legacy";
    baseThresholds = { waterLevel: 82.5, flow: 1350, levelGroup };
  } else if (age >= 5) {
    levelGroup = "normal";
    baseThresholds = { waterLevel: 85.0, flow: 1500, levelGroup };
  } else {
    levelGroup = "new";
    baseThresholds = { waterLevel: 86.2, flow: 1620, levelGroup };
  }

  return baseThresholds;
}',
   36, 62, 2, 1, '水文站阈值计算',
   '根据水文站类型与预警等级, 返回对应水位、流量警戒阈值, 用于监测预警场景下的阈值判定。',
   '水利监测本体规范 V2.0', '水利公共本体库', 'chenbing', '2026-08-01 14:20:00', 0,
   '2026-07-15 09:53:00', '2026-08-04 09:53:00'),

-- f6 水文站阈值计算 (v1.1.0 历史版本, 同 full_access_path)
  ('ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30', 'ri.ont.function.1686f24c254240819a3fa844fbe86a30',
   'v1.1.0', 'getHydrologyStationThresholds', '水文站阈值计算', 1, 2,
   '水利', '水文监测函数集', 'ThresholdCalculator',
   '/水利/水文监测函数集/ThresholdCalculator/getHydrologyStationThresholds',
   'thresholds/calc.ts', 'b955acf97b4a2797f35d365351920b4a',
'@Function()
public getHydrologyStationThresholds(station: HydroStation): ThresholdsResult {
  // v1.1.0: 尚未引入站龄分组, 固定返回基准阈值
  return { waterLevel: 85.0, flow: 1500, levelGroup: "normal" };
}',
   36, 41, 2, 1, '水文站阈值计算',
   '阈值计算首个稳定版: 固定基准阈值, 不区分站龄分组。', '水利监测本体规范 V2.0', '水利公共本体库',
   'chenbing', '2026-06-20 10:05:00', 0, '2026-06-10 09:00:00', '2026-06-20 10:05:00'),

-- f2 土壤流失计算
  ('ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d', 'ri.ont.function.2a7b3c1d5e4f4a6b8c9d0e1f2a3b4c5d',
   'v1.2.0', 'calculateSoilLossAmount', '土壤流失计算', 1, 2,
   '水利', '水土保持函数集', 'SoilLossCalculator',
   '/水利/水土保持函数集/SoilLossCalculator/calculateSoilLossAmount',
   'soilloss/calc.ts', 'c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5',
'@Function()
public calculateSoilLossAmount(plot: SoilMonitorPlot, rainfall: number): SoilLossResult {
  // TODO: 接入 USLE 模型系数表
  return { lossAmount: 0, level: "unknown" };
}',
   12, 18, 1, 1, '土壤流失计算',
   '基于监测小区坡度、植被覆盖度与降雨量估算年土壤流失量, 输出流失等级。', NULL, '水利公共本体库',
   'zhaomin', NULL, 0, '2026-07-28 09:10:00', '2026-08-03 17:02:00'),

-- f3 创建水库档案 (动作函数, 私有, 草稿)
  ('ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e', 'ri.ont.function.3b8c4d2e6f5a4b7c9d0e1f2a3b4c5d6e',
   'v0.0.1', 'createReservoirRecord', '创建水库档案', 2, 2,
   '水利', '水利工程函数集', 'ReservoirEditor',
   '/水利/水利工程函数集/ReservoirEditor/createReservoirRecord',
   'reservoir/create.ts', 'd1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6',
'@OntologyEditFunction()
public createReservoirRecord(payload: ReservoirCreateInput): HydroReservoir {
  // TODO: 校验库容与坝高约束后写入本体
  throw new Error("not implemented");
}',
   8, 13, 1, 4, '创建水库档案',
   '按录入的水库基础信息创建水库对象实例, 并建立与所属流域的关联关系。', NULL, NULL,
   'wanghao', NULL, 0, '2026-08-04 09:16:00', '2026-08-04 09:16:00'),

-- f4 水位异常检测 (时序函数, Python, 已停用)
  ('ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f', 'ri.ont.function.4c9d5e3f7a6b4c8d0e1f2a3b4c5d6e7f',
   'v1.0.3', 'detectWaterLevelAnomaly', '水位异常检测', 5, 1,
   '水利', '水文监测函数集', 'AnomalyDetector',
   '/水利/水文监测函数集/AnomalyDetector/detectWaterLevelAnomaly',
   'anomaly/detect.py', 'e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7',
'@Function()
def detect_water_level_anomaly(series, window_size=24, threshold=3.0, mode="zscore"):
    """基于滑动窗口 Z-Score 检测水位序列异常点。"""
    result = []
    # TODO: 迁移到 v2 检测算法后下线本函数
    return {"anomalies": result, "mode": mode}',
   21, 34, 3, 1, '水位异常检测',
   '对水位时序数据做滑动窗口异常检测, 输出异常点列表与置信度, 已由 v2 算法替代, 现已停用。',
   NULL, '水利公共本体库', 'liuyang', '2026-05-18 16:40:00', 0, '2026-05-06 11:20:00', '2026-07-25 10:11:00'),

-- f5 工程运行年限 (衍生函数)
  ('ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', 'ri.ont.function.5d0e6f4a8b7c4d9e1f2a3b4c5d6e7f8a',
   'v1.0.0', 'getProjectRunningYears', '工程运行年限', 4, 2,
   '水利', '水利工程函数集', 'ProjectDerived',
   '/水利/水利工程函数集/ProjectDerived/getProjectRunningYears',
   'derived/years.ts', 'f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8',
'@Function()
public getProjectRunningYears(project: HydroProject): number {
  const start = project.commissionDate;
  if (!start) return 0;
  return new Date().getFullYear() - new Date(start).getFullYear();
}',
   5, 11, 2, 1, '工程运行年限',
   '按工程投运日期派生运行年限, 供工程台账列表与老化评估看板直接引用。', NULL, '水利公共本体库',
   'chenbing', '2026-07-23 15:30:00', 0, '2026-07-12 14:00:00', '2026-07-23 15:30:00');

-- ---------- 函数参数 ----------
INSERT INTO ont_function_param
  (id, function_id, param_name, param_type, param_direction, is_required,
   default_value, value_range, param_desc, object_class_id, sort_num, is_deleted, create_time, update_time)
VALUES
  -- f1
  ('ont_function_param-5d8a1c2e-7f9b-4a3d-b6e8-2c3d4e5f6a7b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   'station', '[Hydro] Station', 1, 1, NULL, '水文站本体对象集合',
   '水文站对象, 包含站点基础属性与实时监测数据', NULL, 1, 0, '2026-08-01 14:18:30', '2026-08-01 14:18:30'),
  ('ont_function_param-6e9b2d3f-8a0c-4b4e-c7f9-3d4e5f6a7b8c', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   'result', 'ThresholdsResult', 2, 0, NULL, NULL,
   '包含水位、流量阈值及站龄分组的结果对象', NULL, 1, 0, '2026-08-01 14:18:30', '2026-08-01 14:18:30'),
  -- f6 (历史版本)
  ('ont_function_param-7f0c3e4a-9b1d-4c5f-d8a0-4e5f6a7b8c9d', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30',
   'station', '[Hydro] Station', 1, 1, NULL, '水文站本体对象集合',
   '水文站对象', NULL, 1, 0, '2026-06-10 09:00:00', '2026-06-10 09:00:00'),
  ('ont_function_param-8a1d4f5b-0c2e-4d6a-e9b1-5f6a7b8c9d0e', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30',
   'result', 'ThresholdsResult', 2, 0, NULL, NULL, '基准阈值结果对象', NULL, 1, 0,
   '2026-06-10 09:00:00', '2026-06-10 09:00:00'),
  -- f2
  ('ont_function_param-9b2e5a6c-1d3f-4e7b-f0c2-6a7b8c9d0e1f', 'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d',
   'plot', '[Soil] MonitorPlot', 1, 1, NULL, '水土保持监测小区对象集合',
   '监测小区对象, 含坡度、坡长与植被覆盖度', NULL, 1, 0, '2026-07-28 09:10:00', '2026-07-28 09:10:00'),
  ('ont_function_param-0c3f6b7d-2e4a-4f8c-a1d3-7b8c9d0e1f2a', 'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d',
   'rainfall', 'number', 1, 1, '0', '0 ~ 5000', '统计周期内累计降雨量(mm)', NULL, 2, 0,
   '2026-07-28 09:10:00', '2026-07-28 09:10:00'),
  ('ont_function_param-1d4a7c8e-3f5b-4a9d-b2e4-8c9d0e1f2a3b', 'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d',
   'result', 'SoilLossResult', 2, 0, NULL, NULL, '年土壤流失量与流失等级', NULL, 1, 0,
   '2026-07-28 09:10:00', '2026-07-28 09:10:00'),
  -- f3
  ('ont_function_param-2e5b8d9f-4a6c-4b0e-c3f5-9d0e1f2a3b4c', 'ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e',
   'payload', 'ReservoirCreateInput', 1, 1, NULL, NULL, '水库建档录入信息', NULL, 1, 0,
   '2026-08-04 09:16:00', '2026-08-04 09:16:00'),
  ('ont_function_param-3f6c9e0a-5b7d-4c1f-d4a6-0e1f2a3b4c5d', 'ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e',
   'reservoir', '[Hydro] Reservoir', 2, 0, NULL, NULL, '新建成功后的水库对象', NULL, 1, 0,
   '2026-08-04 09:16:00', '2026-08-04 09:16:00'),
  -- f4 (4 个入参, 用于验证列表「超过 3 个显示 …」规则)
  ('ont_function_param-4a7d0f1b-6c8e-4d2a-e5b7-1f2a3b4c5d6e', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'series', '[Hydro] WaterLevelSeries', 1, 1, NULL, '水位时序对象集合', '待检测的水位时序数据', NULL, 1, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00'),
  ('ont_function_param-5b8e1a2c-7d9f-4e3b-f6c8-2a3b4c5d6e7f', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'windowSize', 'number', 1, 0, '24', '1 ~ 168', '滑动窗口长度(小时)', NULL, 2, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00'),
  ('ont_function_param-6c9f2b3d-8e0a-4f4c-a7d9-3b4c5d6e7f8a', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'threshold', 'number', 1, 0, '3.0', '1.0 ~ 6.0', 'Z-Score 判定阈值', NULL, 3, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00'),
  ('ont_function_param-7d0a3c4e-9f1b-4a5d-b8e0-4c5d6e7f8a9b', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'mode', 'string', 1, 0, 'zscore', 'zscore,iqr,mad', '检测算法模式', NULL, 4, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00'),
  ('ont_function_param-8e1b4d5f-0a2c-4b6e-c9f1-5d6e7f8a9b0c', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'report', 'AnomalyReport', 2, 0, NULL, NULL, '异常点列表与检测模式', NULL, 1, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00'),
  -- f5
  ('ont_function_param-9f2c5e6a-1b3d-4c7f-d0a2-6e7f8a9b0c1d', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a',
   'project', '[Hydro] Project', 1, 1, NULL, '水利工程本体对象集合', '水利工程对象, 需含投运日期', NULL, 1, 0,
   '2026-07-12 14:00:00', '2026-07-12 14:00:00'),
  ('ont_function_param-0a3d6f7b-2c4e-4d8a-e1b3-7f8a9b0c1d2e', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a',
   'years', 'number', 2, 0, NULL, NULL, '工程已运行年限(年)', NULL, 1, 0,
   '2026-07-12 14:00:00', '2026-07-12 14:00:00');

-- ---------- 运行配置 ----------
INSERT INTO ont_function_runtime_config
  (id, function_id, timeout, retry_count, retry_interval, memory_quota, concurrency_limit,
   enable_cache, cache_ttl, is_deleted, create_time, update_time)
VALUES
  ('ont_function_runtime_config-8e3b4c5d-1a2f-4e7c-9d6b-5e6f7a8b9c0d', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   30, 1, 1, 512, 100, 1, 3600, 0, '2026-07-15 09:53:00', '2026-08-01 14:20:00'),
  ('ont_function_runtime_config-9f4c5d6e-2b3a-4f8d-0e7c-6f7a8b9c0d1e', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30',
   30, 2, 1, 512, 100, 1, 3600, 0, '2026-06-10 09:00:00', '2026-06-20 10:05:00'),
  ('ont_function_runtime_config-0a5d6e7f-3c4b-4a9e-1f8d-7a8b9c0d1e2f', 'ont_function-2a7b3c1d-5e4f-4a6b-8c9d-0e1f2a3b4c5d',
   60, 2, 2, 1024, 50, 0, 3600, 0, '2026-07-28 09:10:00', '2026-08-03 17:02:00'),
  ('ont_function_runtime_config-1b6e7f8a-4d5c-4b0f-2a9e-8b9c0d1e2f3a', 'ont_function-3b8c4d2e-6f5a-4b7c-9d0e-1f2a3b4c5d6e',
   30, 0, 1, 512, 20, 0, 3600, 0, '2026-08-04 09:16:00', '2026-08-04 09:16:00'),
  ('ont_function_runtime_config-2c7f8a9b-5e6d-4c1a-3b0f-9c0d1e2f3a4b', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   120, 3, 5, 2048, 10, 1, 7200, 0, '2026-05-06 11:20:00', '2026-07-25 10:11:00'),
  ('ont_function_runtime_config-3d8a9b0c-6f7e-4d2b-4c1a-0d1e2f3a4b5c', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a',
   15, 1, 1, 256, 200, 1, 86400, 0, '2026-07-12 14:00:00', '2026-07-23 15:30:00');

-- ---------- 环境变量 ----------
INSERT INTO ont_function_env_var
  (id, function_id, var_name, var_value, var_type, value_range, var_desc, is_encrypt, sort_num,
   is_deleted, create_time, update_time)
VALUES
  ('ont_function_env_var-2f6c7d8e-3a5b-4c9d-8e1f-6b7c8d9e0a1b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   'DEFAULT_LEVEL', 'normal', 4, 'normal,high,low', '默认预警等级, 控制阈值计算基准', 0, 1, 0,
   '2026-07-15 09:53:00', '2026-08-01 14:20:00'),
  ('ont_function_env_var-3a7d8e9f-4b6c-4d0e-9f2a-7c8d9e0a1b2c', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   'ENABLE_CACHE', 'true', 3, NULL, '是否开启结果缓存', 0, 2, 0,
   '2026-07-15 09:53:00', '2026-08-01 14:20:00'),
  ('ont_function_env_var-4b8e9f0a-5c7d-4e1f-0a3b-8d9e0a1b2c3d', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31',
   'CACHE_TTL', '3600', 2, '60~86400', '缓存有效期, 单位秒', 0, 3, 0,
   '2026-07-15 09:53:00', '2026-08-01 14:20:00'),
  ('ont_function_env_var-5c9f0a1b-6d8e-4f2a-1b4c-9e0a1b2c3d4e', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'MODEL_ENDPOINT', 'http://ai-gateway.internal/anomaly/v2', 1, NULL, '异常检测模型服务地址', 0, 1, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00'),
  ('ont_function_env_var-6d0a1b2c-7e9f-4a3b-2c5d-0a1b2c3d4e5f', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f',
   'MODEL_TOKEN', '****', 1, NULL, '模型服务访问令牌', 1, 2, 0,
   '2026-05-06 11:20:00', '2026-05-06 11:20:00');

-- ---------- 调用统计 (近 8 天 × 调用方) ----------
INSERT INTO ont_function_call_stat
  (id, function_id, stat_date, caller_app, call_count, success_count, error_count, avg_cost_ms, create_time)
VALUES
  -- f1 水文监测大屏
  ('fn_call_stat-f1-0728-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-28', '水文监测大屏', 96, 96, 0, 22, '2026-07-29 00:05:00'),
  ('fn_call_stat-f1-0729-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-29', '水文监测大屏', 104, 103, 1, 23, '2026-07-30 00:05:00'),
  ('fn_call_stat-f1-0730-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-30', '水文监测大屏', 88, 88, 0, 21, '2026-07-31 00:05:00'),
  ('fn_call_stat-f1-0731-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-31', '水文监测大屏', 112, 111, 1, 24, '2026-08-01 00:05:00'),
  ('fn_call_stat-f1-0801-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-01', '水文监测大屏', 121, 121, 0, 23, '2026-08-02 00:05:00'),
  ('fn_call_stat-f1-0802-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-02', '水文监测大屏', 76, 76, 0, 20, '2026-08-03 00:05:00'),
  ('fn_call_stat-f1-0803-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-03', '水文监测大屏', 93, 92, 1, 22, '2026-08-04 00:05:00'),
  ('fn_call_stat-f1-0804-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-04', '水文监测大屏', 61, 61, 0, 21, '2026-08-04 12:00:00'),
  -- f1 预警服务平台
  ('fn_call_stat-f1-0728-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-28', '预警服务平台', 72, 71, 1, 26, '2026-07-29 00:05:00'),
  ('fn_call_stat-f1-0729-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-29', '预警服务平台', 81, 81, 0, 25, '2026-07-30 00:05:00'),
  ('fn_call_stat-f1-0730-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-30', '预警服务平台', 69, 68, 1, 27, '2026-07-31 00:05:00'),
  ('fn_call_stat-f1-0731-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-31', '预警服务平台', 90, 90, 0, 24, '2026-08-01 00:05:00'),
  ('fn_call_stat-f1-0801-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-01', '预警服务平台', 95, 94, 1, 25, '2026-08-02 00:05:00'),
  ('fn_call_stat-f1-0802-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-02', '预警服务平台', 58, 58, 0, 23, '2026-08-03 00:05:00'),
  ('fn_call_stat-f1-0803-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-03', '预警服务平台', 77, 76, 1, 26, '2026-08-04 00:05:00'),
  ('fn_call_stat-f1-0804-b', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-04', '预警服务平台', 44, 44, 0, 24, '2026-08-04 12:00:00'),
  -- f1 数据服务网关
  ('fn_call_stat-f1-0729-c', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-29', '数据服务网关', 40, 40, 0, 19, '2026-07-30 00:05:00'),
  ('fn_call_stat-f1-0731-c', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-07-31', '数据服务网关', 52, 51, 1, 20, '2026-08-01 00:05:00'),
  ('fn_call_stat-f1-0802-c', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-02', '数据服务网关', 36, 36, 0, 18, '2026-08-03 00:05:00'),
  ('fn_call_stat-f1-0804-c', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a31', '2026-08-04', '数据服务网关', 29, 29, 0, 19, '2026-08-04 12:00:00'),
  -- f1 历史版本 v1.1.0 的旧调用量
  ('fn_call_stat-f6-0620-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30', '2026-06-20', '水文监测大屏', 810, 806, 4, 28, '2026-06-21 00:05:00'),
  ('fn_call_stat-f6-0621-a', 'ont_function-1686f24c-2542-4081-9a3f-a844fbe86a30', '2026-06-21', '水文监测大屏', 764, 761, 3, 27, '2026-06-22 00:05:00'),
  -- f5 工程运行年限 (高频派生)
  ('fn_call_stat-f5-0729-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-07-29', '工程台账系统', 1120, 1120, 0, 6, '2026-07-30 00:05:00'),
  ('fn_call_stat-f5-0730-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-07-30', '工程台账系统', 1042, 1041, 1, 6, '2026-07-31 00:05:00'),
  ('fn_call_stat-f5-0731-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-07-31', '工程台账系统', 1288, 1288, 0, 7, '2026-08-01 00:05:00'),
  ('fn_call_stat-f5-0801-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-08-01', '工程台账系统', 1355, 1353, 2, 7, '2026-08-02 00:05:00'),
  ('fn_call_stat-f5-0802-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-08-02', '工程台账系统', 902, 902, 0, 6, '2026-08-03 00:05:00'),
  ('fn_call_stat-f5-0803-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-08-03', '工程台账系统', 1174, 1172, 2, 6, '2026-08-04 00:05:00'),
  ('fn_call_stat-f5-0804-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-08-04', '老化评估看板', 840, 840, 0, 5, '2026-08-04 12:00:00'),
  ('fn_call_stat-f5-0715-a', 'ont_function-5d0e6f4a-8b7c-4d9e-1f2a-3b4c5d6e7f8a', '2026-07-15', '工程台账系统', 1531, 1528, 3, 7, '2026-07-16 00:05:00'),
  -- f4 水位异常检测 (已停用, 仅历史调用)
  ('fn_call_stat-f4-0720-a', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f', '2026-07-20', '预警服务平台', 186, 179, 7, 412, '2026-07-21 00:05:00'),
  ('fn_call_stat-f4-0722-a', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f', '2026-07-22', '预警服务平台', 204, 198, 6, 388, '2026-07-23 00:05:00'),
  ('fn_call_stat-f4-0724-a', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f', '2026-07-24', '水文监测大屏', 152, 149, 3, 401, '2026-07-25 00:05:00'),
  ('fn_call_stat-f4-0730-a', 'ont_function-4c9d5e3f-7a6b-4c8d-0e1f-2a3b4c5d6e7f', '2026-07-30', '预警服务平台', 118, 112, 6, 395, '2026-07-31 00:05:00');
