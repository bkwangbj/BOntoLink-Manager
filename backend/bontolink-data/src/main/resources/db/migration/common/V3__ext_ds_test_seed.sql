-- ============================================================
-- V3  测试用外部数据源: 本体管理系统后台服务接口  —  通用(SQLite/PostgreSQL)
--
-- 用户需求: 新建一个外部接口(HTTP)数据源, 以本体管理系统自身的后台服务接口为例,
--           主要用于测试; 至少 3 个查询类接口。
--
-- 数据源: 本体管理后台 (http://localhost:8088/bontolink, 即本系统后端)
-- 接口:   4 个只读查询接口 ——
--          healthy 健康检查 / categories 分类树 / datasources 数据源列表 / classes 对象类型
--
-- 幂等: ON CONFLICT (id) DO NOTHING; 固定 ID 与编码, 可重复执行。
--       冲突列取各表主键(id), SQLite 3.24+ 与 PostgreSQL 9.5+ 均支持。
-- 历史: 旧版 common/V45__ext_ds_test_seed.sql 已随 V3~V4x 合并删除, 内容迁移至此 V3。
-- ============================================================

-- 1. 外部数据源本体
INSERT INTO ont_ext_data_source
  (id, category_code, ds_code, ds_name, ds_type, read_write_type, base_url,
   default_method, content_type, connect_timeout, read_timeout, retry_count, retry_interval,
   ssl_verify, log_enable, header_enable, global_header, auth_type, auth_config,
   status, remark, create_time, update_time)
VALUES
  ('ext_ds-demo-bontolink', 'dom_data_management', 'DS_BONTOLINK', '本体管理后台服务', 'http_rest', 1,
   'http://localhost:8088/bontolink',
   'GET', 'application/json', 5000, 10000, 1, 1000,
   1, 1, 0, NULL, 'none', NULL,
   1, '测试用外部数据源——指向本体管理系统自身的后台服务接口, 演示外部接口类数据源的接入与调试。', '2026-08-07 12:00:00', '2026-08-07 12:00:00')
ON CONFLICT (id) DO NOTHING;

-- 2. 接口分组(便于前端按组展示)
INSERT INTO ont_ext_api_group (id, ds_id, group_name, parent_id, sort, create_time) VALUES
  ('ext_group-demo-basics',  'ext_ds-demo-bontolink', '基础查询', '0', 1, '2026-08-07 12:00:00'),
  ('ext_group-demo-ontology', 'ext_ds-demo-bontolink', '本体资源', '0', 2, '2026-08-07 12:00:00')
ON CONFLICT (id) DO NOTHING;

-- 3. 接口定义(4 个查询类接口)
INSERT INTO ont_ext_api_interface
  (id, ds_id, group_id, api_code, api_name, method, api_path, api_status, read_write_type,
   description, request_params, response_params, override_auth, auth_type, auth_config,
   header_inherit, content_type, timeout, status, sort, create_time, update_time)
VALUES
  ('ext_api-demo-health',     'ext_ds-demo-bontolink', 'ext_group-demo-basics',
   'healthy', '健康检查', 'GET', '/api/health', 'done', 1,
   '探测本体管理后台是否在线, 返回服务状态与时间。',
   '[{"key":"_","in":"none","type":"","desc":"无参"}]', '{}', 0, NULL, NULL,
   1, 'application/json', 5, 1, 1, '2026-08-07 12:00:00', '2026-08-07 12:00:00'),
  ('ext_api-demo-categories', 'ext_ds-demo-bontolink', 'ext_group-demo-ontology',
   'categories', '查询行业分类树', 'GET', '/api/category/tree', 'done', 1,
   '查询本体管理系统行业-领域-分组三级分类树。',
   '[]', '{}', 0, NULL, NULL,
   1, 'application/json', 10, 1, 2, '2026-08-07 12:00:00', '2026-08-07 12:00:00'),
  ('ext_api-demo-datasources', 'ext_ds-demo-bontolink', 'ext_group-demo-ontology',
   'datasources', '查询数据源列表', 'GET', '/api/ext-datasource', 'done', 1,
   '查询当前系统全部外部接口数据源列表。',
   '[]', '{}', 0, NULL, NULL,
   1, 'application/json', 10, 1, 3, '2026-08-07 12:00:00', '2026-08-07 12:00:00'),
  ('ext_api-demo-classes', 'ext_ds-demo-bontolink', 'ext_group-demo-ontology',
   'classes', '查询对象类型列表', 'GET', '/api/class-meta/classes', 'done', 1,
   '查询本体管理系统已建模的对象类型列表。',
   '[]', '{}', 0, NULL, NULL,
   1, 'application/json', 10, 1, 4, '2026-08-07 12:00:00', '2026-08-07 12:00:00')
ON CONFLICT (id) DO NOTHING;
