-- =============================================================
-- 类型类实现(2.x)最小闭环种子:timeseries_measure + component_subtype
-- 多字段 param_json Schema 驱动前端动态表单;两方言共用(纯 INSERT/TEXT)。
-- =============================================================

-- 时序 · 序列度量字段(曲线颜色/线型/Y轴归属,3 字段;可多绑)
INSERT INTO ont_type_class(
  id, category_code, name_prefix, name_cn_base, allow_apply_types, allow_multi_bind,
  system_protected, param_type, frontend_component, param_json, demo_value, description, support_version_min, sort_weight
) VALUES (
  'type-class-ts-measure', 'timeseries', 'timeseries_measure', '序列度量字段', '["property"]', 1,
  0, 'json', 'json_editor',
  '{"default_color":{"type":"string","default":"","description":"曲线默认十六进制颜色值;为空时系统自动分配配色"},"line_type":{"type":"enum","default":"solid","enum":["solid","dashed","dotted"],"description":"曲线展示线型:solid(实线)/dashed(虚线)/dotted(点线),用于区分主辅指标层级"},"axis_position":{"type":"enum","default":"left","enum":["left","right"],"description":"度量字段归属 Y 轴:left(左轴)/right(右轴),解决多指标量级差异过大的同图展示问题"}}',
  '{"default_color":"#3b82f6","line_type":"solid","axis_position":"left"}',
  '标记字段为时序的数值度量指标(水位/流量/雨量/温度/压力等);系统识别后自动作为 Y 轴数据源生成折线/柱状图;一条时序实体可多个属性绑定,图表支持多曲线叠加。',
  '7.10', 20
);

-- 知识图谱 · 组件子类型(比对象类型更细的图谱分组着色)
INSERT INTO ont_type_class(
  id, category_code, name_prefix, name_cn_base, allow_apply_types, allow_multi_bind,
  system_protected, param_type, frontend_component, param_json, demo_value, description, support_version_min, sort_weight
) VALUES (
  'type-class-vertex-subtype', 'vertex', 'component_subtype', '组件子类型', '["property"]', 0,
  0, 'json', 'json_editor',
  '{"subtype_code":{"type":"string","required":true,"description":"子类型编码"},"group_color":{"type":"string","required":true,"description":"分组节点颜色(#hex)"},"group_name":{"type":"string","required":true,"description":"分组显示名称"}}',
  '{"subtype_code":"centrifugal_pump","group_color":"#3b82f6","group_name":"离心泵"}',
  '提供比「对象类型」更细粒度的图谱分组能力,同一对象类型下按子类型着色/筛选/聚合,提升图谱分类辨识度。',
  '7.10', 20
);
