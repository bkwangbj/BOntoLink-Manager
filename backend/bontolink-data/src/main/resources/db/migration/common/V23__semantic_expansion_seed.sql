-- 语义扩充系统种子数据
-- 通用数据（SQLite 和 PostgreSQL 共用）

-- 1. 同义词词典 - 水利领域
INSERT INTO sys_synonym_dict (id, word, synonyms, domain, confidence, source) VALUES
  ('syn-w-001', '水利', '["水务","水资源","水工程","水利工程"]', 'WATER', 1.0, 'MANUAL'),
  ('syn-w-002', '供水', '["给水","输水","配水"]', 'WATER', 0.95, 'MANUAL'),
  ('syn-w-003', '水费', '["水价","水资源费","供水费用"]', 'WATER', 0.92, 'MANUAL'),
  ('syn-w-004', '水量', '["供水量","水体积","用水量"]', 'WATER', 0.88, 'MANUAL'),
  ('syn-w-005', '河流', '["江河","水系"]', 'WATER', 0.90, 'MANUAL'),
  ('syn-w-006', '水库', '["蓄水库","水利枢纽"]', 'WATER', 0.92, 'MANUAL')
ON CONFLICT (id) DO NOTHING;

-- 2. 同义词词典 - 通用领域
INSERT INTO sys_synonym_dict (id, word, synonyms, domain, confidence, source) VALUES
  ('syn-g-001', '企业', '["公司","单位","机构","组织","法人"]', 'GENERAL', 0.95, 'MANUAL'),
  ('syn-g-002', '信息', '["数据","资料","情况","状况"]', 'GENERAL', 0.85, 'MANUAL'),
  ('syn-g-003', '查询', '["搜索","检索","查找","寻找"]', 'GENERAL', 0.90, 'MANUAL'),
  ('syn-g-004', '管理', '["运营","经营","维护"]', 'GENERAL', 0.87, 'MANUAL'),
  ('syn-g-005', '名称', '["名字","称呼","标识"]', 'GENERAL', 0.88, 'MANUAL')
ON CONFLICT (id) DO NOTHING;

-- 3. 同义词词典 - 财务领域
INSERT INTO sys_synonym_dict (id, word, synonyms, domain, confidence, source) VALUES
  ('syn-f-001', '财务', '["会计","资金","经费","收支","账务","财会"]', 'FINANCE', 0.98, 'MANUAL'),
  ('syn-f-002', '成本', '["费用","开支","支出","造价"]', 'FINANCE', 0.92, 'MANUAL'),
  ('syn-f-003', '收入', '["营收","营业额","销售额"]', 'FINANCE', 0.88, 'MANUAL'),
  ('syn-f-004', '利润', '["盈利","收益","净利"]', 'FINANCE', 0.90, 'MANUAL'),
  ('syn-f-005', '资产', '["财产","产权"]', 'FINANCE', 0.85, 'MANUAL')
ON CONFLICT (id) DO NOTHING;

-- 4. 领域术语映射 - 实体级
INSERT INTO ont_domain_term (id, standard_term, common_terms, domain, term_type, similarity, context) VALUES
  ('dt-e-001', '水利服务业企业', '["水务公司","供水企业","水利单位"]', 'WATER', 'ENTITY', 0.90, '指从事供水服务的企业'),
  ('dt-e-002', '水利单位基本情况', '["水利机构信息","水利单位档案","水利组织资料"]', 'WATER', 'ENTITY', 0.88, '水利单位的基础信息')
ON CONFLICT (id) DO NOTHING;

-- 5. 领域术语映射 - 字段级
INSERT INTO ont_domain_term (id, standard_term, common_terms, domain, term_type, similarity, context) VALUES
  ('dt-f-001', '供水成本', '["水费","水价","水资源费"]', 'WATER', 'FIELD', 0.92, '指供水的费用'),
  ('dt-f-002', '实际供水量', '["供水数量","供水总量","水量"]', 'WATER', 'FIELD', 0.85, '实际提供的水量'),
  ('dt-f-003', '水费实际收取率', '["水费收缴率","水费回收率","收费率"]', 'WATER', 'FIELD', 0.90, '水费的实际收取比例'),
  ('dt-f-004', '固定资产折旧费', '["折旧","固定资产摊销","资产折旧"]', 'FINANCE', 'FIELD', 0.88, '固定资产的折旧费用'),
  ('dt-f-005', '营业收入', '["收入","营收","销售额"]', 'FINANCE', 'FIELD', 0.85, '企业的营业收入')
ON CONFLICT (id) DO NOTHING;

-- 6. 领域术语映射 - 单位级
INSERT INTO ont_domain_term (id, standard_term, common_terms, domain, term_type, similarity, context) VALUES
  ('dt-u-001', '立方米', '["吨","方","m³"]', 'WATER', 'UNIT', 0.95, '1立方米水≈1吨'),
  ('dt-u-002', '元/立方米', '["元/吨","块钱一吨","元每吨"]', 'WATER', 'UNIT', 0.93, '每立方米的价格')
ON CONFLICT (id) DO NOTHING;

-- 7. 停用词
INSERT INTO sys_stopwords (id, word, category) VALUES
  ('sw-001', '的', 'COMMON'),
  ('sw-002', '了', 'COMMON'),
  ('sw-003', '和', 'COMMON'),
  ('sw-004', '是', 'COMMON'),
  ('sw-005', '在', 'COMMON'),
  ('sw-006', '有', 'COMMON'),
  ('sw-007', '个', 'COMMON'),
  ('sw-008', '等', 'COMMON'),
  ('sw-009', '查询', 'COMMON'),
  ('sw-010', '信息', 'COMMON'),
  ('sw-011', '数据', 'COMMON'),
  ('sw-012', '请', 'COMMON'),
  ('sw-013', '帮我', 'COMMON'),
  ('sw-014', '给我', 'COMMON'),
  ('sw-015', '看看', 'COMMON'),
  ('sw-016', '一下', 'COMMON'),
  ('sw-017', '关于', 'COMMON'),
  ('sw-018', '所有', 'COMMON'),
  ('sw-019', '全部', 'COMMON'),
  ('sw-020', '都', 'COMMON')
ON CONFLICT (id) DO NOTHING;
