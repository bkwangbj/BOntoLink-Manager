-- =============================================================
-- 类型类参数 Schema 化 — PostgreSQL
-- ont_type_class 增加 param_json:完整参数 JSON Schema(元数据层)
--   与 SQLite 方言对齐,统一用 TEXT 存 JSON。
-- =============================================================
ALTER TABLE ont_type_class ADD COLUMN IF NOT EXISTS param_json TEXT;
