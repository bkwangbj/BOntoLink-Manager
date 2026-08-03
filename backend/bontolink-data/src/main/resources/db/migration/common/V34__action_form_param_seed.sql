-- V34: 函数动作表单参数补充种子 —— ont_action_form_param
-- 通用数据（SQLite 和 PostgreSQL 共用）
--
-- 对应 V33 生成的 10 个函数动作，补齐"输入参数"表单字段（与各函数 in 入参一一对应），
-- 使动作在「试运行 / 参数映射」中能渲染出可填写的表单参数。
-- config 与前端保存格式一致: {"value_source":1,"property_code":"<param_code>"}
-- 幂等: ON CONFLICT DO NOTHING，可重复执行

INSERT INTO ont_action_form_param
  (id, action_id, section_id, param_code, param_name, param_type, data_type,
   is_required, is_multi, default_value, placeholder, sort, config) VALUES
  -- 水厂水量平衡核算 (class_action-demo-fn-01)
  ('action-formparam-demo-0101','class_action-demo-fn-01',NULL,'inlet_flow','进厂水量','number','xsd:decimal',1,0,NULL,'请输入进厂水量(m³)',0,'{"value_source":1,"property_code":"inlet_flow"}'),
  ('action-formparam-demo-0102','class_action-demo-fn-01',NULL,'outlet_flow','出厂供水量','number','xsd:decimal',1,0,NULL,'请输入出厂供水量(m³)',1,'{"value_source":1,"property_code":"outlet_flow"}'),
  -- 管段输水能力校验 (class_action-demo-fn-02)
  ('action-formparam-demo-0201','class_action-demo-fn-02',NULL,'diameter_mm','管径(mm)','number','xsd:decimal',1,0,NULL,'请输入管径(mm)',0,'{"value_source":1,"property_code":"diameter_mm"}'),
  ('action-formparam-demo-0202','class_action-demo-fn-02',NULL,'flow_rate','输水流量','number','xsd:decimal',1,0,NULL,'请输入输水流量(m³/h)',1,'{"value_source":1,"property_code":"flow_rate"}'),
  -- 片区夜间最小流量分析 (class_action-demo-fn-03)
  ('action-formparam-demo-0301','class_action-demo-fn-03',NULL,'min_night_flow','夜间最小流量','number','xsd:decimal',1,0,NULL,'请输入夜间最小流量(m³/h)',0,'{"value_source":1,"property_code":"min_night_flow"}'),
  ('action-formparam-demo-0302','class_action-demo-fn-03',NULL,'zone_area','片区面积','number','xsd:decimal',1,0,NULL,'请输入片区面积(km²)',1,'{"value_source":1,"property_code":"zone_area"}'),
  -- 阀门操作风险评估 (class_action-demo-fn-04)
  ('action-formparam-demo-0401','class_action-demo-fn-04',NULL,'valve_type','阀门类型','string','xsd:string',1,0,NULL,'闸阀 / 蝶阀 / 三通',0,'{"value_source":1,"property_code":"valve_type"}'),
  ('action-formparam-demo-0402','class_action-demo-fn-04',NULL,'op_mode','启闭方式','string','xsd:string',1,0,NULL,'全开 / 半开 / 关闭',1,'{"value_source":1,"property_code":"op_mode"}'),
  -- 流量计计量校准核查 (class_action-demo-fn-05)
  ('action-formparam-demo-0501','class_action-demo-fn-05',NULL,'diff_rate','偏差率','number','xsd:decimal',1,0,NULL,'请输入与上游表计偏差率(%)',0,'{"value_source":1,"property_code":"diff_rate"}'),
  ('action-formparam-demo-0502','class_action-demo-fn-05',NULL,'meter_age','投用年限','number','xsd:decimal',1,0,NULL,'请输入投用年限(年)',1,'{"value_source":1,"property_code":"meter_age"}'),
  -- 抄表异常检测 (class_action-demo-fn-06)
  ('action-formparam-demo-0601','class_action-demo-fn-06',NULL,'reading_value','本期读数','number','xsd:decimal',1,0,NULL,'请输入本期抄表读数',0,'{"value_source":1,"property_code":"reading_value"}'),
  ('action-formparam-demo-0602','class_action-demo-fn-06',NULL,'prev_value','上期读数','number','xsd:decimal',1,0,NULL,'请输入上期抄表读数',1,'{"value_source":1,"property_code":"prev_value"}'),
  -- 维修工单成本估算 (class_action-demo-fn-07)
  ('action-formparam-demo-0701','class_action-demo-fn-07',NULL,'pipe_length','维修长度','number','xsd:decimal',1,0,NULL,'请输入维修长度(m)',0,'{"value_source":1,"property_code":"pipe_length"}'),
  ('action-formparam-demo-0702','class_action-demo-fn-07',NULL,'material_unit','管材单价','number','xsd:decimal',1,0,NULL,'请输入管材单价(元/m)',1,'{"value_source":1,"property_code":"material_unit"}'),
  ('action-formparam-demo-0703','class_action-demo-fn-07',NULL,'work_hours','维修工时','number','xsd:decimal',1,0,NULL,'请输入维修工时(h)',2,'{"value_source":1,"property_code":"work_hours"}'),
  -- 班组工作量统计 (class_action-demo-fn-08)
  ('action-formparam-demo-0801','class_action-demo-fn-08',NULL,'task_count','工单数','number','xsd:integer',1,0,NULL,'请输入统计周期内工单数',0,'{"value_source":1,"property_code":"task_count"}'),
  ('action-formparam-demo-0802','class_action-demo-fn-08',NULL,'total_hours','总工时','number','xsd:decimal',1,0,NULL,'请输入班组总工时(h)',1,'{"value_source":1,"property_code":"total_hours"}'),
  -- 供应商信用评估 (class_action-demo-fn-09)
  ('action-formparam-demo-0901','class_action-demo-fn-09',NULL,'ontime_rate','供货及时率','number','xsd:decimal',1,0,NULL,'请输入供货及时率(%)',0,'{"value_source":1,"property_code":"ontime_rate"}'),
  ('action-formparam-demo-0902','class_action-demo-fn-09',NULL,'return_rate','退换货率','number','xsd:decimal',1,0,NULL,'请输入退换货率(%)',1,'{"value_source":1,"property_code":"return_rate"}'),
  -- 水质达标评估 (class_action-demo-fn-10)
  ('action-formparam-demo-1001','class_action-demo-fn-10',NULL,'ph','酸碱度','number','xsd:decimal',1,0,NULL,'请输入 pH 值(6.5-8.5)',0,'{"value_source":1,"property_code":"ph"}'),
  ('action-formparam-demo-1002','class_action-demo-fn-10',NULL,'turbidity','浊度','number','xsd:decimal',1,0,NULL,'请输入浊度(NTU)',1,'{"value_source":1,"property_code":"turbidity"}'),
  ('action-formparam-demo-1003','class_action-demo-fn-10',NULL,'residual_chlorine','余氯','number','xsd:decimal',1,0,NULL,'请输入余氯(mg/L)',2,'{"value_source":1,"property_code":"residual_chlorine"}')
ON CONFLICT (id) DO NOTHING;
