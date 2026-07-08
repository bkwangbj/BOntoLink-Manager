-- ============================================================
-- V18: 全量系统字典种子 — 整合所有散落的枚举/字典数据
-- ============================================================

-- 0. 通用状态
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-act-status', 'sys_act_status', '启用状态', '通用启用/禁用 status=1/0', 1);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no, color) VALUES
  ('di-act-1', 'dict-act-status', '1', '启用', 1, '#00b42a'),
  ('di-act-0', 'dict-act-status', '0', '禁用', 2, '#f53f3f');

-- 是否
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-yes-no', 'sys_yes_no', '是否', '通用是否', 2);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-yn-1', 'dict-yes-no', '1', '是', 1),
  ('di-yn-0', 'dict-yes-no', '0', '否', 2);

-- 连接类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-join-type', 'sys_join_type', '连接类型', 'SQL JOIN 类型', 3);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-join-1', 'dict-join-type', 'LEFT',  '左连接', 1),
  ('di-join-2', 'dict-join-type', 'INNER', '内连接', 2),
  ('di-join-3', 'dict-join-type', 'RIGHT', '右连接', 3),
  ('di-join-4', 'dict-join-type', 'FULL',  '全连接', 4);

-- 1. 文本状态（ont_enum_types / ont_enum_items.status）
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-text-status', 'sys_text_status', '文本状态', 'active/inactive', 10);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-ts-1', 'dict-text-status', 'active', '启用', 1),
  ('di-ts-2', 'dict-text-status', 'inactive', '停用', 2);

-- 2. 行业分类类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-cat-type', 'sys_category_type', '行业分类类型', 'BizCategory.categoryType: 1=行业 2=领域 3=分组', 11);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-cat-1', 'dict-cat-type', '1', '行业', 1),
  ('di-cat-2', 'dict-cat-type', '2', '领域', 2),
  ('di-cat-3', 'dict-cat-type', '3', '分组', 3);

-- 3. 枚举类型模式
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-enum-type', 'sys_enum_type', '枚举类型模式', 'ont_enum_types.enum_type', 12);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-et-1', 'dict-enum-type', 'general_single', '通用单选', 1),
  ('di-et-2', 'dict-enum-type', 'general_multi', '通用多级', 2),
  ('di-et-3', 'dict-enum-type', 'biz_single', '业务单选', 3),
  ('di-et-4', 'dict-enum-type', 'biz_multi', '业务多级', 4);

-- 4. 枚举同步模式
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-sync-mode', 'sys_sync_mode', '同步模式', 'ont_enum_sync_config.sync_mode', 13);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-sm-1', 'dict-sync-mode', 'level_diff', '层级差异', 1),
  ('di-sm-2', 'dict-sync-mode', 'full_overwrite', '全量覆盖', 2),
  ('di-sm-3', 'dict-sync-mode', 'incremental', '增量追加', 3);

-- 5. 同步策略
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-sync-strategy', 'sys_sync_strategy', '同步策略', 'ont_enum_sync_config.sync_strategy', 14);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-ss-1', 'dict-sync-strategy', 'once', '一次性', 1),
  ('di-ss-2', 'dict-sync-strategy', 'daily', '每日', 2),
  ('di-ss-3', 'dict-sync-strategy', 'weekly', '每周', 3),
  ('di-ss-4', 'dict-sync-strategy', 'monthly', '每月', 4);

-- 6. 同步类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-sync-type', 'sys_sync_type', '同步触发类型', 'ont_enum_sync_log.sync_type', 15);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-syt-1', 'dict-sync-type', 'manual', '手动', 1),
  ('di-syt-2', 'dict-sync-type', 'auto', '自动', 2);

-- 7. 同步状态
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-sync-run-status', 'sys_sync_run_status', '同步运行状态', 'ont_enum_sync_log.sync_status', 16);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no, color) VALUES
  ('di-srs-1', 'dict-sync-run-status', 'running', '运行中', 1, '#165DFF'),
  ('di-srs-2', 'dict-sync-run-status', 'success', '成功', 2, '#00b42a'),
  ('di-srs-3', 'dict-sync-run-status', 'failed', '失败', 3, '#f53f3f');

-- 8. 属性类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-prop-type', 'sys_prop_type', '属性类型', 'ont_class_property.prop_type', 20);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-pt-1', 'dict-prop-type', 'data', '数据属性', 1),
  ('di-pt-2', 'dict-prop-type', 'object', '对象属性', 2),
  ('di-pt-3', 'dict-prop-type', 'annotation', '注释属性', 3),
  ('di-pt-4', 'dict-prop-type', 'struct', '结构属性', 4);

-- 9. 属性作用域
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-prop-scope', 'sys_prop_scope', '属性作用域', 'ont_class_property.property_scope', 21);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-ps-1', 'dict-prop-scope', 'class', '类', 1),
  ('di-ps-2', 'dict-prop-scope', 'interface', '接口', 2);

-- 10. 数据类型（XSD）
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-data-type', 'sys_data_type', '数据类型', 'XSD 数据类型', 22);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-dt-1', 'dict-data-type', 'xsd:string', '字符串', 1),
  ('di-dt-2', 'dict-data-type', 'xsd:decimal', '小数', 2),
  ('di-dt-3', 'dict-data-type', 'xsd:integer', '整数', 3),
  ('di-dt-4', 'dict-data-type', 'xsd:boolean', '布尔', 4),
  ('di-dt-5', 'dict-data-type', 'xsd:dateTime', '日期时间', 5),
  ('di-dt-6', 'dict-data-type', 'xsd:date', '日期', 6),
  ('di-dt-7', 'dict-data-type', 'xsd:anyURI', 'URI', 7);

-- 11. 基数类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-cardinality', 'sys_cardinality', '基数类型', 'ont_class_link.cardinality', 23);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-card-1', 'dict-cardinality', 'many_to_many', '多对多', 1),
  ('di-card-2', 'dict-cardinality', 'many_to_one', '多对一', 2),
  ('di-card-3', 'dict-cardinality', 'one_to_many', '一对多', 3),
  ('di-card-4', 'dict-cardinality', 'one_to_one', '一对一', 4);

-- 12. 数据集关系类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-rel-type', 'sys_rel_type', '数据集关系类型', 'ont_class_ds.rel_type: 1=主 2=补充', 24);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-rt-1', 'dict-rel-type', '1', '主数据集', 1),
  ('di-rt-2', 'dict-rel-type', '2', '补充数据集', 2);

-- 13. 分组类型（等价/不相交）
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-group-type', 'sys_group_type', '分组类型', 'ont_class_group.group_type', 25);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-gt-1', 'dict-group-type', 'equivalent', '等价类', 1),
  ('di-gt-2', 'dict-group-type', 'disjoint', '不相交类', 2);

-- 14. 动作类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-action-kind', 'sys_action_kind', '动作类型', 'ont_class_action.action_kind', 26);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-ak-1', 'dict-action-kind', 'create', '创建', 1),
  ('di-ak-2', 'dict-action-kind', 'update', '更新', 2);

-- 15. 类表达式类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-expr-type', 'sys_expr_type', '类表达式类型', 'ont_class.class_expr_type', 27);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-expr-0', 'dict-expr-type', '', '普通业务类', 1),
  ('di-expr-1', 'dict-expr-type', 'union', '并集', 2),
  ('di-expr-2', 'dict-expr-type', 'intersection', '交集', 3),
  ('di-expr-3', 'dict-expr-type', 'complement', '补集', 4),
  ('di-expr-4', 'dict-expr-type', 'enumeration', '枚举', 5);

-- 16. 链接状态
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-link-status', 'sys_link_status', '链接状态', 'ont_link_types.status', 30);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-ls-1', 'dict-link-status', 'experimental', '实验', 1),
  ('di-ls-2', 'dict-link-status', 'active', '启用', 2),
  ('di-ls-3', 'dict-link-status', 'deprecated', '弃用', 3);

-- 17. 链接基数
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-link-card', 'sys_link_cardinality', '链接基数', 'ont_link_types.l/r_cardinality', 31);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-lc-1', 'dict-link-card', 'one', '单一', 1),
  ('di-lc-2', 'dict-link-card', 'many', '多个', 2);

-- 18. 链接可见性
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-link-vis', 'sys_link_visibility', '链接可见性', 'ont_link_types.l/r_visibility', 32);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-lv-1', 'dict-link-vis', 'normal', '普通', 1),
  ('di-lv-2', 'dict-link-vis', 'prominent', '突出', 2),
  ('di-lv-3', 'dict-link-vis', 'hidden', '隐藏', 3);

-- 19. 映射侧
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-link-side', 'sys_link_side', '映射侧', 'ont_link_mappings.side', 33);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-lsi-1', 'dict-link-side', 'left', '左侧', 1),
  ('di-lsi-2', 'dict-link-side', 'right', '右侧', 2);

-- 20. 数据源类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-ds-type', 'sys_ds_type', '数据源类型', 'sys_data_source.ds_type', 40);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-ds-1', 'dict-ds-type', 'mysql', 'MySQL', 1),
  ('di-ds-2', 'dict-ds-type', 'postgresql', 'PostgreSQL', 2),
  ('di-ds-3', 'dict-ds-type', 'oracle', 'Oracle', 3),
  ('di-ds-4', 'dict-ds-type', 'mongodb', 'MongoDB', 4),
  ('di-ds-5', 'dict-ds-type', 'dm', '达梦 DM', 5),
  ('di-ds-6', 'dict-ds-type', 'kingbase', '人大金仓', 6),
  ('di-ds-7', 'dict-ds-type', 'oscar', '神舟通用', 7),
  ('di-ds-8', 'dict-ds-type', 'gbase', '南大通用', 8),
  ('di-ds-9', 'dict-ds-type', 'polardb', '阿里云 PolarDB', 9),
  ('di-ds-10', 'dict-ds-type', 'tdsql', '腾讯云 TDSQL', 10),
  ('di-ds-11', 'dict-ds-type', 'gaussdb', '华为 GaussDB', 11),
  ('di-ds-12', 'dict-ds-type', 'oceanbase', 'OceanBase', 12);

-- 21. 连接状态
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-conn-status', 'sys_conn_status', '连接状态', 'sys_data_source.connect_status', 41);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no, color) VALUES
  ('di-cs-1', 'dict-conn-status', 'online', '在线', 1, '#00b42a'),
  ('di-cs-2', 'dict-conn-status', 'offline', '离线', 2, '#f53f3f'),
  ('di-cs-3', 'dict-conn-status', 'risk', '风险', 3, '#ff7d00');

-- 22. 格式化类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-fmt-type', 'sys_format_type', '格式化类型', 'ont_property_format.format_type', 50);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-fmt-1', 'dict-fmt-type', 'general', '常规', 1),
  ('di-fmt-2', 'dict-fmt-type', 'number', '数值', 2),
  ('di-fmt-3', 'dict-fmt-type', 'currency', '货币', 3),
  ('di-fmt-4', 'dict-fmt-type', 'accounting', '会计', 4),
  ('di-fmt-5', 'dict-fmt-type', 'date', '日期', 5),
  ('di-fmt-6', 'dict-fmt-type', 'time', '时间', 6),
  ('di-fmt-7', 'dict-fmt-type', 'percent', '百分比', 7),
  ('di-fmt-8', 'dict-fmt-type', 'fraction', '分数', 8),
  ('di-fmt-9', 'dict-fmt-type', 'scientific', '科学计数', 9),
  ('di-fmt-10', 'dict-fmt-type', 'text', '文本', 10),
  ('di-fmt-11', 'dict-fmt-type', 'special', '特殊', 11),
  ('di-fmt-12', 'dict-fmt-type', 'custom', '自定义', 12);

-- 23. 负数显示模式
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-neg-mode', 'sys_negative_mode', '负数显示模式', 'ont_property_format.negative_mode', 51);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-nm-0', 'dict-neg-mode', '0', '红括号', 1),
  ('di-nm-1', 'dict-neg-mode', '1', '黑括号', 2),
  ('di-nm-2', 'dict-neg-mode', '2', '红无符号', 3),
  ('di-nm-3', 'dict-neg-mode', '3', '黑负号', 4),
  ('di-nm-4', 'dict-neg-mode', '4', '红负号', 5);

-- 24. 特殊格式类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-special-type', 'sys_special_type', '特殊格式类型', 'ont_property_format.special_type', 52);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-sp-1', 'dict-special-type', 'zipcode', '邮政编码', 1),
  ('di-sp-2', 'dict-special-type', 'lowerChinese', '小写中文', 2),
  ('di-sp-3', 'dict-special-type', 'upperChinese', '大写中文', 3),
  ('di-sp-4', 'dict-special-type', 'rmbUpper', '人民币大写', 4),
  ('di-sp-5', 'dict-special-type', 'wanUnit', '万单位', 5),
  ('di-sp-6', 'dict-special-type', 'plusMinus', '正负号', 6);

-- 25. 值类型显示格式
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-display-fmt', 'sys_display_format', '值类型显示格式', 'ont_valuetypes_usage_config.display_format', 60);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-df-1', 'dict-display-fmt', 'label', '标签', 1),
  ('di-df-2', 'dict-display-fmt', 'code', '编码', 2),
  ('di-df-3', 'dict-display-fmt', 'code_label', '编码+标签', 3),
  ('di-df-4', 'dict-display-fmt', 'full_label', '全路径标签', 4);

-- 26. 资源分组类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-res-grp-type', 'sys_res_group_type', '资源分组类型', 'ont_biz_group_class.group_type', 70);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-rgt-1', 'dict-res-grp-type', 'object_types', '对象类型', 1),
  ('di-rgt-2', 'dict-res-grp-type', 'link_types', '链接类型', 2),
  ('di-rgt-3', 'dict-res-grp-type', 'action_types', '动作类型', 3),
  ('di-rgt-4', 'dict-res-grp-type', 'value_types', '值类型', 4),
  ('di-rgt-5', 'dict-res-grp-type', 'shared_props', '共享属性', 5),
  ('di-rgt-6', 'dict-res-grp-type', 'functions', '函数', 6),
  ('di-rgt-7', 'dict-res-grp-type', 'interface', '接口', 7),
  ('di-rgt-8', 'dict-res-grp-type', 'datasources', '数据源', 8),
  ('di-rgt-9', 'dict-res-grp-type', 'enum_types', '枚举类型', 9);

-- 27. 图标来源
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-icon-src', 'sys_icon_source', '图标来源', 'icon_lib_group.source', 80);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-is-1', 'dict-icon-src', 'seed', '内置', 1),
  ('di-is-2', 'dict-icon-src', 'manual', '手动', 2);

-- 28. 适用类型（类型类）
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-applicable', 'sys_applicable_type', '适用类型', '类型类 applicable_type', 90);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-app-1', 'dict-applicable', 'property', '属性', 1),
  ('di-app-2', 'dict-applicable', 'relation', '关系', 2),
  ('di-app-3', 'dict-applicable', 'action', '操作', 3);

-- 29. 来源类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-source-type', 'sys_source_type', '来源类型', '类型类 source_type', 91);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-st-1', 'dict-source-type', 'platform_built', '平台内置', 1),
  ('di-st-2', 'dict-source-type', 'user_custom', '用户自定义', 2);

-- 30. 属性所属类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-prop-owner', 'sys_prop_owner_type', '属性所属类型', '类型类 property_owner_type', 92);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-po-1', 'dict-prop-owner', 'object', '对象类型', 1),
  ('di-po-2', 'dict-prop-owner', 'interface', '接口类型', 2);

-- 31. 参数类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-param-type', 'sys_param_type', '参数类型', '类型类 param_type', 93);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-pm-1', 'dict-param-type', 'boolean', '布尔', 1),
  ('di-pm-2', 'dict-param-type', 'rid', 'RID', 2),
  ('di-pm-3', 'dict-param-type', 'enum', '枚举', 3),
  ('di-pm-4', 'dict-param-type', 'text', '文本', 4),
  ('di-pm-5', 'dict-param-type', 'numeric', '数值', 5),
  ('di-pm-6', 'dict-param-type', 'json', 'JSON', 6);

-- 32. 前端组件类型
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-fc-type', 'sys_frontend_component', '前端组件类型', '类型类 frontend_component', 94);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-fc-1', 'dict-fc-type', 'switch', '开关', 1),
  ('di-fc-2', 'dict-fc-type', 'single_select', '单选下拉', 2),
  ('di-fc-3', 'dict-fc-type', 'multi_select', '多选下拉', 3),
  ('di-fc-4', 'dict-fc-type', 'text_input', '文本输入', 4),
  ('di-fc-5', 'dict-fc-type', 'number_input', '数字输入', 5),
  ('di-fc-6', 'dict-fc-type', 'json_editor', 'JSON 编辑器', 6),
  ('di-fc-7', 'dict-fc-type', 'rid_selector', 'RID 选择器', 7);

-- 33. 环境
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-env', 'sys_env', '环境', '部署/运行环境', 95);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-env-1', 'dict-env', 'dev', '开发', 1),
  ('di-env-2', 'dict-env', 'test', '测试', 2),
  ('di-env-3', 'dict-env', 'prod', '生产', 3);

-- 34. 一级大类（类型类 category_dict）
INSERT OR IGNORE INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, sort_no) VALUES ('dict-tc-category', 'sys_tc_category', '类型类一级大类', '类型类 category_dict', 96);
INSERT OR IGNORE INTO ont_dict_item(id, dict_id, item_code, item_value, sort_no) VALUES
  ('di-tcc-1', 'dict-tc-category', 'hubble', '可视化展示类', 1),
  ('di-tcc-2', 'dict-tc-category', 'analyzer', '索引分析类', 2),
  ('di-tcc-3', 'dict-tc-category', 'business', '业务语义类', 3),
  ('di-tcc-4', 'dict-tc-category', 'custom', '自定义类型类', 4),
  ('di-tcc-5', 'dict-tc-category', 'geo', '地理坐标类', 5),
  ('di-tcc-6', 'dict-tc-category', 'choropleth_map_config_id', '等值热力地图类', 6),
  ('di-tcc-7', 'dict-tc-category', 'vertex', '知识图谱类', 7),
  ('di-tcc-8', 'dict-tc-category', 'timeseries', '时间序列类', 8),
  ('di-tcc-9', 'dict-tc-category', 'hierarchy', '层级导航类', 9),
  ('di-tcc-10', 'dict-tc-category', 'hubble-oe', '对象操作展示类', 10),
  ('di-tcc-11', 'dict-tc-category', 'actions', '通用操作能力类', 11);
