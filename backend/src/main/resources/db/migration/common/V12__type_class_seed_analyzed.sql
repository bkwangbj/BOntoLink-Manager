-- =============================================================
-- 分词器(analyzer)· analyzed(全文检索分词)。配置型:analyzer_name 数组
--   (第一位核心分词器,后续附加过滤器,数组顺序即分词管道执行顺序)。
--   参数编辑走前端专用「编辑分词参数」弹窗(常用/全量 + 核心分词器 + 附加能力 + 实时预览)。
-- =============================================================
INSERT INTO ont_type_class(
  id, category_code, name_prefix, name_cn_base, allow_apply_types, allow_multi_bind,
  system_protected, param_type, frontend_component, param_json, demo_value, description, support_version_min, sort_weight
) VALUES (
  'type-class-analyzer-analyzed', 'analyzer', 'analyzed', '全文检索分词', '["property"]', 0,
  0, 'json', 'json_editor',
  '{"analyzer_name":{"type":"array","description":"分词管道:第一位为核心分词器(单选必填),后续为附加过滤器(可多选);数组顺序即管道执行顺序"}}',
  '{"analyzer_name":["ik_max_word","pinyin","stop"]}',
  '为属性配置全文检索分词管道(核心分词器 + N 个附加过滤器);业务侧声明语义,引擎侧翻译为 Elasticsearch 原生规则;keyword 精确匹配模式下不可叠加附加能力。',
  '7.0', 35
);
