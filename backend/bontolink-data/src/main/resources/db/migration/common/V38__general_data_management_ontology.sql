-- V38: 通用与数据管理领域本体种子数据
-- 行业: 通用行业 (ind_general)
-- 领域: 通用公共 / 数据管理 / 数据治理 / 元数据管理
-- 幂等: ON CONFLICT DO NOTHING，可重复执行

-- ============================================================
-- 1. 命名空间
-- ============================================================
INSERT INTO ont_biz_namespace (id, ns_code, ns_name, ns_uri, hierarchy_path, curr_version, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('namespace-38-gen-0001','w_gen',   '通用根',       'http://ont.platform.com/v1/general#',          'platform.general',                  '1.0',1,'通用根',       'BOntoLink 通用领域根命名空间',        '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('namespace-38-gen-0002','w_gen_cm','通用公共层',   'http://ont.platform.com/v1/general/common#',   'platform.general.common',           '1.0',1,'通用公共层',   '跨领域通用基础概念',                  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('namespace-38-gen-0003','w_gen_dm','数据管理',     'http://ont.platform.com/v1/general/datamgmt#', 'platform.general.datamgmt',         '1.0',1,'数据管理',     '数据源/表/字段/模型等核心数据资产',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('namespace-38-gen-0004','w_gen_dg','数据治理',     'http://ont.platform.com/v1/general/datago#',   'platform.general.datago',           '1.0',1,'数据治理',     '数据域/策略/质量/标准治理概念',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('namespace-38-gen-0005','w_gen_me','元数据管理',   'http://ont.platform.com/v1/general/metadata#', 'platform.general.metadata',         '1.0',1,'元数据管理',   '元数据注册/本体定义/数据字典',        '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (ns_code) DO NOTHING;

-- ============================================================
-- 2. 行业 / 领域 / 分组
-- ============================================================
INSERT INTO ont_biz_category (id, parent_id, rid, category_code, category_type, ns_code, status, sort, icon, color, rdfs_label, description, create_time, update_time) VALUES
  -- 行业
  ('category-38-gen-0001','0','ri.ont.biz.category.38-gen-0001','ind_general',        1,'w_gen',   1,10,'industry','#165DFF','通用行业',   '跨行业通用本体，面向数据管理与治理场景',     '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- 领域
  ('category-38-gen-0002','category-38-gen-0001','ri.ont.biz.category.38-gen-0002','dom_general_common',  2,'w_gen_cm',1,1,'folder','#00B42A','通用公共',   '跨领域复用的基础通用类型',                  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('category-38-gen-0003','category-38-gen-0001','ri.ont.biz.category.38-gen-0003','dom_data_management', 2,'w_gen_dm',1,2,'folder','#165DFF','数据管理',   '数据资产、数据源、数据表、数据模型管理',    '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('category-38-gen-0004','category-38-gen-0001','ri.ont.biz.category.38-gen-0004','dom_data_governance', 2,'w_gen_dg',1,3,'folder','#722ED1','数据治理',   '数据域划分、质量规则、策略与标准管理',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('category-38-gen-0005','category-38-gen-0001','ri.ont.biz.category.38-gen-0005','dom_metadata_mgmt',   2,'w_gen_me',1,4,'folder','#FF7D00','元数据管理', '元数据注册、本体定义、数据字典管理',        '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (category_code) DO NOTHING;

-- 分组
INSERT INTO ont_biz_group (id, parent_id, category_code, g_name, g_sort, icon, color, description, domain_code, create_time, update_time) VALUES
  ('group-38-gen-0001','category-38-gen-0003','grp_dm_source', '数据源与存储', 1,'database','#165DFF','数据源定义、连接与存储类型',   'dom_data_management','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('group-38-gen-0002','category-38-gen-0003','grp_dm_model',  '数据模型',     2,'layers',  '#00B42A','数据模型、数据集、数据表等',     'dom_data_management','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('group-38-gen-0003','category-38-gen-0004','grp_dg_rule',   '治理规则',     1,'shield',  '#722ED1','数据质量规则与治理策略',         'dom_data_governance','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('group-38-gen-0004','category-38-gen-0005','grp_meta_reg',  '元数据注册',   1,'list',    '#FF7D00','元数据注册表与本体定义',         'dom_metadata_mgmt',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('group-38-gen-0005','category-38-gen-0002','grp_gen_base',  '通用基础',     1,'cube',    '#13C2C2','组织、人员等跨领域通用类',       'dom_general_common', '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 3. 枚举类型
-- ============================================================
INSERT INTO ont_enum_types (id, rid, api_name, category_code, enum_type, max_level, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('enum-types-v38-001','ri.ont.enum.library.dm_ds_type',        'dm_ds_type',        'dom_data_management','general_single',1,'active','数据源类型',   '关系型/文档型/时序等存储类型',     '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-002','ri.ont.enum.library.dm_field_type',     'dm_field_type',     'dom_data_management','general_single',1,'active','字段数据类型', '数据库字段基础数据类型',           '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-003','ri.ont.enum.library.dm_asset_status',   'dm_asset_status',   'dom_data_management','general_single',1,'active','数据资产状态', '数据资产全生命周期状态',           '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-004','ri.ont.enum.library.dm_sensitivity',    'dm_sensitivity',    'dom_data_governance','general_single',1,'active','数据敏感级别', '数据安全分级: 公开/内部/机密/绝密','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-005','ri.ont.enum.library.dm_quality_level',  'dm_quality_level',  'dom_data_governance','general_single',1,'active','数据质量等级', '优秀/良好/中等/较差/未知',         '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-006','ri.ont.enum.library.dm_gov_status',     'dm_gov_status',     'dom_data_governance','biz_single',   1,'active','治理状态',     '数据资产治理审批流状态',           '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-007','ri.ont.enum.library.dm_model_type',     'dm_model_type',     'dom_metadata_mgmt', 'general_single',1,'active','数据模型类型', '概念/逻辑/物理/本体模型',          '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('enum-types-v38-008','ri.ont.enum.library.dm_sync_mode',      'dm_sync_mode',      'dom_data_management','general_single',1,'active','同步模式',     '数据同步策略: 全量/增量/CDC/手动', '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- 枚举项: dm_ds_type
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-dst-01','enum-types-v38-001','MYSQL',    'mysql',      'MySQL',       NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-02','enum-types-v38-001','PGSQL',    'postgresql', 'PostgreSQL',  NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-03','enum-types-v38-001','ORACLE',   'oracle',     'Oracle',      NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-04','enum-types-v38-001','SQLSRV',   'sqlserver',  'SQL Server',  NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-05','enum-types-v38-001','DM8',      'dm',         '达梦 DM8',    NULL,1,5,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-06','enum-types-v38-001','KINGBASE', 'kingbase',   '金仓 KingBase',NULL,1,6,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-07','enum-types-v38-001','MONGO',    'mongodb',    'MongoDB',     NULL,1,7,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-08','enum-types-v38-001','ES',       'elasticsearch','Elasticsearch',NULL,1,8,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-09','enum-types-v38-001','HIVE',     'hive',       'Hive',        NULL,1,9,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-dst-10','enum-types-v38-001','SQLITE',   'sqlite',     'SQLite',      NULL,1,10,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_field_type
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-fld-01','enum-types-v38-002','STR',  'string',   'STRING',   NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-02','enum-types-v38-002','INT',  'integer',  'INTEGER',  NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-03','enum-types-v38-002','BIG',  'bigint',   'BIGINT',   NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-04','enum-types-v38-002','DEC',  'decimal',  'DECIMAL',  NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-05','enum-types-v38-002','FLT',  'float',    'FLOAT',    NULL,1,5,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-06','enum-types-v38-002','BOOL', 'boolean',  'BOOLEAN',  NULL,1,6,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-07','enum-types-v38-002','DT',   'date',     'DATE',     NULL,1,7,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-08','enum-types-v38-002','DTM',  'datetime', 'DATETIME', NULL,1,8,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-09','enum-types-v38-002','JSON', 'json',     'JSON',     NULL,1,9,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-10','enum-types-v38-002','BLOB', 'blob',     'BLOB/BINARY',NULL,1,10,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-fld-11','enum-types-v38-002','TXT',  'text',     'TEXT',     NULL,1,11,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_asset_status
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-ast-01','enum-types-v38-003','DRAFT',    'draft',      '草稿',   NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ast-02','enum-types-v38-003','REVIEW',   'reviewing',  '审核中', NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ast-03','enum-types-v38-003','ACTIVE',   'active',     '已发布', NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ast-04','enum-types-v38-003','ARCHIVED', 'archived',   '已归档', NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ast-05','enum-types-v38-003','DEPR',     'deprecated', '已废弃', NULL,1,5,'inactive',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_sensitivity
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-sen-01','enum-types-v38-004','S1','public',      '公开',   NULL,1,1,'active',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-sen-02','enum-types-v38-004','S2','internal',    '内部',   NULL,1,2,'active',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-sen-03','enum-types-v38-004','S3','confidential','机密',   NULL,1,3,'active',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-sen-04','enum-types-v38-004','S4','secret',      '绝密',   NULL,1,4,'active',1,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_quality_level
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-ql-01','enum-types-v38-005','QE','excellent','优秀',NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ql-02','enum-types-v38-005','QG','good',     '良好',NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ql-03','enum-types-v38-005','QM','medium',   '中等',NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ql-04','enum-types-v38-005','QP','poor',     '较差',NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-ql-05','enum-types-v38-005','QU','unknown',  '未知',NULL,1,5,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_gov_status
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-gov-01','enum-types-v38-006','GR','registered', '已注册', NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-gov-02','enum-types-v38-006','GV','reviewing',  '审核中', NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-gov-03','enum-types-v38-006','GA','approved',   '已审批', NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-gov-04','enum-types-v38-006','GP','published',  '已发布', NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-gov-05','enum-types-v38-006','GD','deprecated', '已废弃', NULL,1,5,'inactive',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_model_type
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-mt-01','enum-types-v38-007','MC','conceptual','概念模型',NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-mt-02','enum-types-v38-007','ML','logical',   '逻辑模型',NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-mt-03','enum-types-v38-007','MP','physical',  '物理模型',NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-mt-04','enum-types-v38-007','MO','ontology',  '本体模型',NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- 枚举项: dm_sync_mode
INSERT INTO ont_enum_items (id, enum_id, code, api_name, label, parent_code, level, sort_num, status, is_sync_locked, create_time, update_time) VALUES
  ('ei-v38-sm-01','enum-types-v38-008','FULL', 'full',        '全量同步',NULL,1,1,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-sm-02','enum-types-v38-008','INCR', 'incremental', '增量同步',NULL,1,2,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-sm-03','enum-types-v38-008','CDC',  'cdc',         'CDC变更捕获',NULL,1,3,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ei-v38-sm-04','enum-types-v38-008','MAN',  'manual',      '手动同步',NULL,1,4,'active',0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 4. 值类型使用配置 + 值类型
-- ============================================================
INSERT INTO ont_valuetypes_usage_config (id, max_select_level, allow_non_leaf, display_format, is_system_default, create_time, update_time) VALUES
  ('vt-usage-v38-001',0,0,'label',      0,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('vt-usage-v38-002',0,0,'code_label', 0,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO ont_value_types (id, rid, api_name, category_code, base_type, constraint_type, constraint_config, enum_id, default_usage_config_id, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('value-types-v38-001','ri.ont.vt.dm_vt_ds_type',      'dm_vt_ds_type',      'dom_data_management','String', 'Enum',  NULL,                                                              'enum-types-v38-001','vt-usage-v38-001',1,'数据源类型',  '存储/数据库类型枚举',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('value-types-v38-002','ri.ont.vt.dm_vt_field_type',   'dm_vt_field_type',   'dom_data_management','String', 'Enum',  NULL,                                                              'enum-types-v38-002','vt-usage-v38-001',1,'字段数据类型','数据库字段类型枚举',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('value-types-v38-003','ri.ont.vt.dm_vt_asset_status', 'dm_vt_asset_status', 'dom_data_management','String', 'Enum',  NULL,                                                              'enum-types-v38-003','vt-usage-v38-001',1,'资产状态',    '数据资产生命周期状态','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('value-types-v38-004','ri.ont.vt.dm_vt_sensitivity',  'dm_vt_sensitivity',  'dom_data_governance','String', 'Enum',  NULL,                                                              'enum-types-v38-004','vt-usage-v38-001',1,'敏感级别',    '数据安全分级枚举',    '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('value-types-v38-005','ri.ont.vt.dm_vt_quality_score','dm_vt_quality_score','dom_data_governance','Decimal','Length','{"minInclusive":"0","maxInclusive":"100"}',                       NULL,NULL,1,               '质量评分',    '0~100 综合质量评分',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('value-types-v38-006','ri.ont.vt.dm_vt_version_str',  'dm_vt_version_str',  'dom_metadata_mgmt', 'String', 'Regex', '{"pattern":"^\\d+\\.\\d+(\\.\\d+)?$","example":"1.0.0"}',         NULL,NULL,1,               '版本号',      '语义化版本 x.y[.z]',  '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- ============================================================
-- 5. 共享属性
-- ============================================================
INSERT INTO ont_shared_properties (id, rid, category_code, prop_code, prop_type, is_key, data_type, value_type, is_required, is_multi_valued_prop, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('sp-v38-001','ri.ont.sp.dm_version_no',    'dom_general_common', 'dm_version_no',    'data',      0,'xsd:string', 'value-types-v38-006',0,0,1,'版本号',      '语义化版本号 x.y.z',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-002','ri.ont.sp.dm_owner_name',    'dom_data_governance','dm_owner_name',    'data',      0,'xsd:string', NULL,                 0,0,1,'数据责任人',  '资产主要负责人姓名',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-003','ri.ont.sp.dm_sensitivity',   'dom_data_governance','dm_sensitivity',   'data',      0,'xsd:string', 'value-types-v38-004',1,0,1,'敏感级别',    '数据安全分级',         '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-004','ri.ont.sp.dm_quality_score', 'dom_data_governance','dm_quality_score', 'data',      0,'xsd:decimal','value-types-v38-005',0,0,1,'质量评分',    '0~100 综合质量评分',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-005','ri.ont.sp.dm_source_system', 'dom_data_management','dm_source_system', 'data',      0,'xsd:string', NULL,                 0,0,1,'来源系统',    '数据来源系统标识',     '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-006','ri.ont.sp.dm_gov_status',    'dom_data_governance','dm_gov_status',    'data',      0,'xsd:string', NULL,                 0,0,1,'治理状态',    '数据治理审批流状态',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-007','ri.ont.sp.dm_created_by',    'dom_general_common', 'dm_created_by',    'annotation',0,'xsd:string', NULL,                 0,0,1,'创建人',      '记录创建人标识',       '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-008','ri.ont.sp.dm_approved_by',   'dom_data_governance','dm_approved_by',   'annotation',0,'xsd:string', NULL,                 0,0,1,'审批人',      '最终审批通过人',       '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-009','ri.ont.sp.dm_publish_time',  'dom_general_common', 'dm_publish_time',  'data',      0,'xsd:dateTime',NULL,                0,0,1,'发布时间',    '资产正式对外发布时间', '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('sp-v38-010','ri.ont.sp.dm_retention_days','dom_data_management','dm_retention_days','data',      0,'xsd:integer',NULL,                 0,0,1,'保留周期(天)','数据保留最长天数',     '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (prop_code) DO NOTHING;

-- ============================================================
-- 6. 接口
-- ============================================================
INSERT INTO ont_interface (id, rid, api_name, interface_code, ns_code, category_code, display_name, status, icon, color, description, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  ('if-v38-001','ri.ont.interface.if-v38-001','versioned_asset',  'iface_versioned', 'w_gen_cm','dom_general_common', '可版本化资产',1,'git',    '#165DFF','具有版本管理能力的数据资产契约','VersionedAsset',  '统一版本化资产公共行为','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('if-v38-002','ri.ont.interface.if-v38-002','governed_asset',   'iface_governed',  'w_gen_dg','dom_data_governance','受治理资产',  1,'shield', '#722ED1','纳入数据治理体系的资产契约',   'GovernedAsset',   '治理状态/责任人/敏感级别','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('if-v38-003','ri.ont.interface.if-v38-003','auditable_entity', 'iface_auditable', 'w_gen_cm','dom_general_common', '可审计实体',  1,'eye',    '#00B42A','记录创建/修改/审批操作日志',   'AuditableEntity', '支持操作审计追踪',      '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- 接口属性
INSERT INTO ont_interface_property (id, rid, interface_id, api_name, prop_code, data_type, value_type, category_code, display_name, is_required, status, rdfs_label, rdfs_comment, create_time, update_time) VALUES
  -- versioned_asset
  ('ifp-v38-001',NULL,'if-v38-001','version_no',        'version_no',   'xsd:string', 'value-types-v38-006','dom_general_common','版本号',    1,1,'版本号',    '语义化版本号 x.y.z',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifp-v38-002',NULL,'if-v38-001','version_status',    'version_status','xsd:string', 'value-types-v38-003','dom_general_common','版本状态',  1,1,'版本状态',  '当前版本资产状态',     '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifp-v38-003',NULL,'if-v38-001','version_date',      'version_date', 'xsd:dateTime',NULL,                 'dom_general_common','发版日期',  0,1,'发版日期',  '版本发布时间',         '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- governed_asset
  ('ifp-v38-004',NULL,'if-v38-002','gov_status',        'gov_status',   'xsd:string', NULL,                 'dom_data_governance','治理状态', 1,1,'治理状态',  '数据治理审批流状态',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifp-v38-005',NULL,'if-v38-002','owner_name',        'owner_name',   'xsd:string', NULL,                 'dom_data_governance','数据责任人',1,1,'数据责任人','资产主要负责人',       '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifp-v38-006',NULL,'if-v38-002','sensitivity_level', 'sensitivity',  'xsd:string', 'value-types-v38-004','dom_data_governance','敏感级别', 1,1,'敏感级别',  '数据安全分级',         '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- auditable_entity
  ('ifp-v38-007',NULL,'if-v38-003','created_by_user',   'created_by',   'xsd:string', NULL,                 'dom_general_common','创建人',    1,1,'创建人',    '记录创建人',           '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifp-v38-008',NULL,'if-v38-003','updated_by_user',   'updated_by',   'xsd:string', NULL,                 'dom_general_common','修改人',    0,1,'修改人',    '最近修改人',           '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifp-v38-009',NULL,'if-v38-003','audit_remark',      'audit_remark', 'xsd:string', NULL,                 'dom_general_common','审计备注',  0,1,'审计备注',  '操作说明/审计日志',    '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 7. 对象类型 (ont_class)
-- ============================================================
INSERT INTO ont_class (id, rid, api_name, ns_code, category_code, display_name, icon, color, is_common, is_thing, is_nothing, status, rdfs_label, rdfs_comment, description, create_time, update_time) VALUES
  -- 通用公共 (is_common=1)
  ('class-v38-cm-001','ri.ont.class.v38-cm-001','GenOrganization',    'w_gen_cm','dom_general_common', '组织机构',    'building','#165DFF',1,0,0,1,'GenOrganization',    '组织/机构/单位通用类',    '企业、部门、机构等组织实体',        '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-cm-002','ri.ont.class.v38-cm-002','GenPerson',          'w_gen_cm','dom_general_common', '人员',        'user',    '#00B42A', 1,0,0,1,'GenPerson',          '人员/个人通用类',         '系统内涉及的人员实体',              '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-cm-003','ri.ont.class.v38-cm-003','DataTag',            'w_gen_cm','dom_general_common', '数据标签',    'tag',     '#13C2C2', 1,0,0,1,'DataTag',            '通用数据标签/标记',       '用于分类和检索的标签实体',          '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- 数据管理
  ('class-v38-dm-001','ri.ont.class.v38-dm-001','DataAsset',          'w_gen_dm','dom_data_management','数据资产',    'database','#165DFF', 1,0,0,1,'DataAsset',          '数据资产根抽象类',        '所有数据管理资产的公共父类',        '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dm-002','ri.ont.class.v38-dm-002','DataSourceDef',      'w_gen_dm','dom_data_management','数据源定义',  'server',  '#165DFF', 0,0,0,1,'DataSourceDef',      '数据源连接与配置定义',    '数据库/API/文件等来源的定义实体',   '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dm-003','ri.ont.class.v38-dm-003','DataTableDef',       'w_gen_dm','dom_data_management','数据表定义',  'table',   '#00B42A', 0,0,0,1,'DataTableDef',       '物理/逻辑数据表定义',     '数据源中物理表或视图的元数据定义',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dm-004','ri.ont.class.v38-dm-004','DataFieldDef',       'w_gen_dm','dom_data_management','数据字段定义','columns', '#FF7D00', 0,0,0,1,'DataFieldDef',       '数据表字段元数据定义',    '单个字段的类型、约束与业务描述',    '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dm-005','ri.ont.class.v38-dm-005','DataModelDef',       'w_gen_dm','dom_data_management','数据模型定义','layers',  '#722ED1', 0,0,0,1,'DataModelDef',       '数据模型(概念/逻辑/物理)','聚合多张数据表的模型定义实体',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dm-006','ri.ont.class.v38-dm-006','DataSetDef',         'w_gen_dm','dom_data_management','数据集定义',  'grid',    '#EB2F96', 0,0,0,1,'DataSetDef',         '面向主题的数据集合',      '按业务主题组织的逻辑数据集定义',    '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- 数据治理
  ('class-v38-dg-001','ri.ont.class.v38-dg-001','DataDomainDef',      'w_gen_dg','dom_data_governance','数据域',      'folder',  '#722ED1', 0,0,0,1,'DataDomainDef',      '数据域/主题域定义',       '按业务主题划分的数据管理范围',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dg-002','ri.ont.class.v38-dg-002','DataPolicyDef',      'w_gen_dg','dom_data_governance','数据策略',    'shield',  '#F53F3F', 0,0,0,1,'DataPolicyDef',      '数据管理策略定义',        '访问控制、保留等数据治理策略',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dg-003','ri.ont.class.v38-dg-003','DataQualityRule',    'w_gen_dg','dom_data_governance','数据质量规则','check',   '#00B42A', 0,0,0,1,'DataQualityRule',    '数据质量检核规则',        '字段级/表级/跨表质量校验规则',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-dg-004','ri.ont.class.v38-dg-004','DataStandardDef',    'w_gen_dg','dom_data_governance','数据标准',    'book',    '#FADB14', 0,0,0,1,'DataStandardDef',    '数据标准规范定义',        '字段命名/编码/值域等标准规范',      '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- 元数据管理
  ('class-v38-me-001','ri.ont.class.v38-me-001','MetadataRegistryItem','w_gen_me','dom_metadata_mgmt', '元数据注册项','list',    '#FF7D00', 0,0,0,1,'MetadataRegistryItem','元数据注册表条目',        '各类元数据的统一注册入口',          '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-me-002','ri.ont.class.v38-me-002','OntologyDef',        'w_gen_me','dom_metadata_mgmt', '本体定义',    'share',   '#165DFF', 0,0,0,1,'OntologyDef',        '本体模型顶层定义',        '描述领域知识概念体系的本体实体',    '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('class-v38-me-003','ri.ont.class.v38-me-003','DataDictionaryDef',  'w_gen_me','dom_metadata_mgmt', '数据字典',    'book',    '#13C2C2', 0,0,0,1,'DataDictionaryDef',  '业务数据字典定义',        '编码/值映射等业务字典资产',         '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (api_name) DO NOTHING;

-- 父子继承: DataSourceDef / DataTableDef / DataModelDef / DataSetDef → DataAsset
UPDATE ont_class SET parent_class_id='class-v38-dm-001'
  WHERE id IN ('class-v38-dm-002','class-v38-dm-003','class-v38-dm-005','class-v38-dm-006')
  AND parent_class_id IS NULL;

-- ============================================================
-- 8. 对象属性 (ont_class_property) — 核心类关键字段
-- ============================================================
INSERT INTO ont_class_property (id, class_id, category_code, api_name, prop_code, prop_type, data_type, value_type, display_name, class_ds_id, physical_table, physical_column, is_primary, is_required, is_key, sort, status, create_time, update_time) VALUES
  -- DataSourceDef
  ('cp-v38-ds-001','class-v38-dm-002','dom_data_management','source_name',  'source_name', 'data','xsd:string', NULL,                 '数据源名称', NULL, NULL, NULL, 1,1,0,1,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-ds-002','class-v38-dm-002','dom_data_management','source_code',  'source_code', 'data','xsd:string', NULL,                 '数据源编码', NULL, NULL, NULL, 0,1,1,2,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-ds-003','class-v38-dm-002','dom_data_management','source_type',  'source_type', 'data','xsd:string', 'value-types-v38-001','数据库类型', NULL, NULL, NULL, 0,1,0,3,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-ds-004','class-v38-dm-002','dom_data_management','jdbc_url',     'jdbc_url',    'data','xsd:string', NULL,                 '连接地址',   NULL, NULL, NULL, 0,0,0,4,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-ds-005','class-v38-dm-002','dom_data_management','source_status','source_status','data','xsd:string','value-types-v38-003','资产状态',   NULL, NULL, NULL, 0,1,0,5,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- DataTableDef
  ('cp-v38-tb-001','class-v38-dm-003','dom_data_management','table_name',   'table_name',  'data','xsd:string', NULL,                 '表名',       NULL, NULL, NULL, 1,1,1,1,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-tb-002','class-v38-dm-003','dom_data_management','table_label',  'table_label', 'data','xsd:string', NULL,                 '中文名',     NULL, NULL, NULL, 0,0,0,2,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-tb-003','class-v38-dm-003','dom_data_management','row_count',    'row_count',   'data','xsd:integer',NULL,                 '数据量(行)', NULL, NULL, NULL, 0,0,0,3,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-tb-004','class-v38-dm-003','dom_data_management','column_count', 'column_count','data','xsd:integer',NULL,                 '字段数',     NULL, NULL, NULL, 0,0,0,4,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-tb-005','class-v38-dm-003','dom_data_management','table_status', 'table_status','data','xsd:string', 'value-types-v38-003','资产状态',   NULL, NULL, NULL, 0,1,0,5,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- DataFieldDef
  ('cp-v38-fd-001','class-v38-dm-004','dom_data_management','field_name',   'field_name',  'data','xsd:string', NULL,                 '字段名',     NULL, NULL, NULL, 1,1,1,1,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-fd-002','class-v38-dm-004','dom_data_management','field_label',  'field_label', 'data','xsd:string', NULL,                 '字段中文名', NULL, NULL, NULL, 0,0,0,2,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-fd-003','class-v38-dm-004','dom_data_management','field_type',   'field_type',  'data','xsd:string', 'value-types-v38-002','字段类型',   NULL, NULL, NULL, 0,1,0,3,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-fd-004','class-v38-dm-004','dom_data_management','is_nullable',  'is_nullable', 'data','xsd:boolean',NULL,                 '允许为空',   NULL, NULL, NULL, 0,0,0,4,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-fd-005','class-v38-dm-004','dom_data_management','is_pk',        'is_pk',       'data','xsd:boolean',NULL,                 '是否主键',   NULL, NULL, NULL, 0,0,0,5,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-fd-006','class-v38-dm-004','dom_data_management','field_desc',   'field_desc',  'annotation','xsd:string',NULL,            '字段说明',   NULL, NULL, NULL, 0,0,0,6,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- OntologyDef
  ('cp-v38-on-001','class-v38-me-002','dom_metadata_mgmt', 'ont_name',      'ont_name',    'data','xsd:string', NULL,                 '本体名称',   NULL, NULL, NULL, 1,1,0,1,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-on-002','class-v38-me-002','dom_metadata_mgmt', 'ont_code',      'ont_code',    'data','xsd:string', NULL,                 '本体编码',   NULL, NULL, NULL, 0,1,1,2,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-on-003','class-v38-me-002','dom_metadata_mgmt', 'model_type',    'model_type',  'data','xsd:string', 'value-types-v38-007',NULL,         NULL, NULL, NULL, 0,1,0,3,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-on-004','class-v38-me-002','dom_metadata_mgmt', 'ont_version',   'ont_version', 'data','xsd:string', 'value-types-v38-006','版本号',     NULL, NULL, NULL, 0,0,0,4,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- GenOrganization
  ('cp-v38-or-001','class-v38-cm-001','dom_general_common','org_name',      'org_name',    'data','xsd:string', NULL,                 '机构名称',   NULL, NULL, NULL, 1,1,0,1,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-or-002','class-v38-cm-001','dom_general_common','org_code',      'org_code',    'data','xsd:string', NULL,                 '机构编码',   NULL, NULL, NULL, 0,1,1,2,1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('cp-v38-or-003','class-v38-cm-001','dom_general_common','org_type',      'org_type',    'data','xsd:string', NULL,                 '机构类型',   NULL, NULL, NULL, 0,0,0,3,1,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 9. 链接类型 (ont_link_types)
-- ============================================================
INSERT INTO ont_link_types (id, link_type_id, rid, status, l_object_type_id, r_object_type_id, l_cardinality, r_cardinality, l_display_name, l_plural_name, r_display_name, r_plural_name, l_api_name, r_api_name, l_enabled, r_enabled, rdfs_label, rdfs_comment, category_code, created_at, updated_at) VALUES
  ('lt-v38-001','dm-source-has-table',   'ri.ont.lt.dm-source-has-table',   'active','class-v38-dm-002','class-v38-dm-003','one','many','包含数据表','包含数据表','所属数据源','所属数据源','tables',       'dataSource',   1,1,'数据源-包含-数据表',  '数据源与其下属数据表的归属关系','dom_data_management','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-002','dm-table-has-field',    'ri.ont.lt.dm-table-has-field',    'active','class-v38-dm-003','class-v38-dm-004','one','many','包含字段',  '包含字段',  '所属数据表','所属数据表','fields',        'dataTable',    1,1,'数据表-包含-字段',    '数据表与其字段的组成关系',      'dom_data_management','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-003','dm-model-includes-table','ri.ont.lt.dm-model-includes-table','active','class-v38-dm-005','class-v38-dm-003','many','many','包含数据表','包含数据表','所属模型',  '所属模型',  'modelTables',  'dataModels',   1,1,'数据模型-包含-数据表','数据模型与数据表多对多组合',    'dom_data_management','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-004','dm-table-in-domain',    'ri.ont.lt.dm-table-in-domain',    'active','class-v38-dm-003','class-v38-dg-001','many','one', '归属数据域','归属数据域','包含数据表','包含数据表','dataDomain',   'domainTables', 1,1,'数据表-归属-数据域',  '数据表与所属业务数据域关系',    'dom_data_governance','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-005','dm-field-uses-dict',    'ri.ont.lt.dm-field-uses-dict',    'active','class-v38-dm-004','class-v38-me-003','many','one', '引用字典',  '引用字典',  '被引用字段','被引用字段','dictionary',   'referencedBy', 1,1,'字段-引用-数据字典',  '字段值域与业务数据字典映射关系','dom_metadata_mgmt',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-006','dm-asset-governed-by-policy','ri.ont.lt.dm-asset-governed-by-policy','active','class-v38-dm-001','class-v38-dg-002','many','many','适用策略','适用策略','管控资产','管控资产','governPolicies','governedAssets',1,1,'数据资产-受控于-策略','数据资产与数据策略多对多约束',  'dom_data_governance','2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-007','dm-ontology-has-registry','ri.ont.lt.dm-ontology-has-registry','active','class-v38-me-002','class-v38-me-001','one','many','包含元数据项','包含元数据项','所属本体','所属本体','registryItems','ontologyDef',  1,1,'本体-包含-元数据注册项','本体与其注册元数据条目关系',  'dom_metadata_mgmt',  '2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('lt-v38-008','dm-org-owns-asset',     'ri.ont.lt.dm-org-owns-asset',     'active','class-v38-cm-001','class-v38-dm-001','many','many','拥有数据资产','拥有数据资产','归属机构','归属机构','ownedAssets',  'ownerOrgs',    1,1,'机构-拥有-数据资产',  '组织机构对数据资产的所有权关系','dom_general_common', '2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (link_type_id) DO NOTHING;

-- ============================================================
-- 10. 接口-类绑定 (ont_interface_class)
-- ============================================================
INSERT INTO ont_interface_class (id, interface_id, class_id, category_code, status, create_time, update_time) VALUES
  ('ifc-v38-001','if-v38-001','class-v38-dm-001','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-002','if-v38-002','class-v38-dm-001','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-003','if-v38-001','class-v38-dm-002','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-004','if-v38-002','class-v38-dm-002','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-005','if-v38-002','class-v38-dm-003','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-006','if-v38-003','class-v38-dm-003','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-007','if-v38-001','class-v38-me-002','dom_metadata_mgmt', 1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-008','if-v38-002','class-v38-me-002','dom_metadata_mgmt', 1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-009','if-v38-003','class-v38-me-002','dom_metadata_mgmt', 1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('ifc-v38-010','if-v38-003','class-v38-cm-001','dom_general_common',1,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 11. 分组-类关联 (ont_biz_group_class)
-- ============================================================
INSERT INTO ont_biz_group_class (id, group_id, ref_id, group_type, category_code, g_sort, create_time, update_time) VALUES
  -- grp_dm_source: 数据源与存储
  ('bgc-v38-001','group-38-gen-0001','class-v38-dm-002','object_types','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-002','group-38-gen-0001','class-v38-dm-003','object_types','dom_data_management',2,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- grp_dm_model: 数据模型
  ('bgc-v38-003','group-38-gen-0002','class-v38-dm-005','object_types','dom_data_management',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-004','group-38-gen-0002','class-v38-dm-006','object_types','dom_data_management',2,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-005','group-38-gen-0002','class-v38-dm-004','object_types','dom_data_management',3,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- grp_dg_rule: 治理规则
  ('bgc-v38-006','group-38-gen-0003','class-v38-dg-001','object_types','dom_data_governance',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-007','group-38-gen-0003','class-v38-dg-002','object_types','dom_data_governance',2,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-008','group-38-gen-0003','class-v38-dg-003','object_types','dom_data_governance',3,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-009','group-38-gen-0003','class-v38-dg-004','object_types','dom_data_governance',4,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- grp_meta_reg: 元数据注册
  ('bgc-v38-010','group-38-gen-0004','class-v38-me-001','object_types','dom_metadata_mgmt', 1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-011','group-38-gen-0004','class-v38-me-002','object_types','dom_metadata_mgmt', 2,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-012','group-38-gen-0004','class-v38-me-003','object_types','dom_metadata_mgmt', 3,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  -- grp_gen_base: 通用基础
  ('bgc-v38-013','group-38-gen-0005','class-v38-dm-001','object_types','dom_general_common',1,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-014','group-38-gen-0005','class-v38-cm-001','object_types','dom_general_common',2,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-015','group-38-gen-0005','class-v38-cm-002','object_types','dom_general_common',3,'2026-08-04 09:00:00','2026-08-04 09:00:00'),
  ('bgc-v38-016','group-38-gen-0005','class-v38-cm-003','object_types','dom_general_common',4,'2026-08-04 09:00:00','2026-08-04 09:00:00')
ON CONFLICT (id) DO NOTHING;
