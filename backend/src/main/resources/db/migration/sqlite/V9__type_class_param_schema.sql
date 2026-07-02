-- =============================================================
-- 类型类参数 Schema 化 — SQLite
-- ont_type_class 增加 param_json:完整参数 JSON Schema(元数据层)
--   多字段类型类(如 timeseries_measure = default_color+line_type+axis_position、
--   event_type = type_style_map 样式映射)据此驱动前端动态表单渲染;
--   实例参数值仍存 ont_type_class_bind.value。
-- =============================================================
ALTER TABLE ont_type_class ADD COLUMN param_json TEXT;
