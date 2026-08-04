"""生成 V39 对象类 + 类属性 + 链接类型 + 分组绑定"""
import sys, os

TARGET = r'f:/aiProject/BOnotLink-Manager/backend/bontolink-data/src/main/resources/db/migration/common/V39__ontomgmt_platform_ontology.sql'
T = '2026-08-04 12:00:00'

# ---- 对象类 (id, api_name, category_code, ns_code, display_name, icon, color, label, comment) ----
CLASSES = [
  # 业务分类体系
  ('class-om-ns-01','OmNamespace',             'dom_om_bizclass','om_root',  '命名空间',         'key',          '#165DFF','命名空间',         '本体命名空间定义，对应 ont_biz_namespace'),
  ('class-om-ns-02','OmNamespaceVersion',       'dom_om_bizclass','om_root',  '命名空间版本',     'history',      '#165DFF','命名空间版本',     '命名空间发布版本快照，对应 ont_biz_namespace_version'),
  ('class-om-ns-03','OmBizCategory',            'dom_om_bizclass','om_root',  '业务分类',         'list',         '#00B42A','业务分类',         '行业/领域/分组三级分类节点，对应 ont_biz_category'),
  ('class-om-ns-04','OmBizGroup',               'dom_om_bizclass','om_root',  '业务分组',         'folder',       '#00B42A','业务分组',         '对象类业务分组，对应 ont_biz_group'),
  ('class-om-ns-05','OmBizGroupClass',          'dom_om_bizclass','om_root',  '分组类绑定',       'link',         '#86909C','分组类关联',       '业务分组与对象类的绑定，对应 ont_biz_group_class'),
  # 本体核心建模
  ('class-om-co-01','OmClass',                  'dom_om_core',   'om_core',   '对象类',           'cube',         '#00B42A','对象类',           '本体对象类定义，对应 ont_class'),
  ('class-om-co-02','OmClassProperty',          'dom_om_core',   'om_core',   '类属性',           'sliders',      '#165DFF','类属性',           '对象类属性定义，对应 ont_class_property'),
  ('class-om-co-03','OmClassLink',              'dom_om_core',   'om_core',   '类链接',           'link',         '#FF7D00','类链接',           '对象类与链接类型绑定，对应 ont_class_link'),
  ('class-om-co-04','OmClassHierarchy',         'dom_om_core',   'om_core',   '类层级',           'git-merge',    '#13C2C2','类层级',           '对象类父子继承层级，对应 ont_class_hierarchy'),
  ('class-om-co-05','OmClassDisjointUnion',     'dom_om_core',   'om_core',   '类不相交联合',     'x-circle',     '#F53F3F','不相交联合',       'OWL类不相交联合表达，对应 ont_class_disjoint_union'),
  ('class-om-co-06','OmClassExpansion',         'dom_om_core',   'om_core',   '类扩展',           'plus-circle',  '#722ED1','类扩展',           '对象类扩展属性配置，对应 ont_class_expansion'),
  ('class-om-co-07','OmClassGroup',             'dom_om_core',   'om_core',   '类分组关联',       'layers',       '#86909C','类分组',           '类与业务分组关联，对应 ont_class_group'),
  ('class-om-co-08','OmClassDs',               'dom_om_core',    'om_core',   '类数据源绑定',     'database',     '#165DFF','类数据源',         '对象类与物理数据源绑定，对应 ont_class_ds'),
  ('class-om-co-09','OmLinkType',               'dom_om_core',   'om_core',   '链接类型',         'link',         '#FF7D00','链接类型',         '对象类间关联关系定义，对应 ont_link_types'),
  ('class-om-co-10','OmLinkMapping',            'dom_om_core',   'om_core',   '链接映射',         'shuffle',      '#FF7D00','链接映射',         '链接类型端点属性映射，对应 ont_link_mappings'),
  ('class-om-co-11','OmPropertyFormat',         'dom_om_core',   'om_core',   '属性格式',         'sliders',      '#86909C','属性格式',         '属性值格式化配置，对应 ont_property_format'),
  ('class-om-co-12','OmPropertyDisjoint',       'dom_om_core',   'om_core',   '属性不相交',       'minus-circle', '#F53F3F','属性不相交',       'OWL属性不相交约束，对应 ont_property_disjoint'),
  ('class-om-co-13','OmPropertyEquivalent',     'dom_om_core',   'om_core',   '属性等价',         'check-circle', '#13C2C2','属性等价',         'OWL属性等价声明，对应 ont_property_equivalent'),
  # 配置管理
  ('class-om-cf-01','OmValueType',              'dom_om_config', 'om_core',   '值类型',           'layers',       '#FF7D00','值类型',           '数据属性值域类型定义，对应 ont_value_types'),
  ('class-om-cf-02','OmValueTypeUsage',         'dom_om_config', 'om_core',   '值类型使用配置',   'settings',     '#FF7D00','值类型使用配置',   '值类型场景显示配置，对应 ont_valuetypes_usage_config'),
  ('class-om-cf-03','OmEnumType',               'dom_om_config', 'om_core',   '枚举类型',         'list',         '#722ED1','枚举类型',         '可选值枚举类型，对应 ont_enum_types'),
  ('class-om-cf-04','OmEnumItem',               'dom_om_config', 'om_core',   '枚举项',           'tag',          '#722ED1','枚举项',           '枚举类型可选值条目，对应 ont_enum_items'),
  ('class-om-cf-05','OmEnumLevelCodeRule',      'dom_om_config', 'om_core',   '枚举级别编码规则', 'hash',         '#722ED1','枚举级别编码',     '多级枚举编码生成规则，对应 ont_enum_level_code_rule'),
  ('class-om-cf-06','OmEnumSyncConfig',         'dom_om_config', 'om_core',   '枚举同步配置',     'refresh-cw',   '#722ED1','枚举同步配置',     '枚举数据外部同步规则，对应 ont_enum_sync_config'),
  ('class-om-cf-07','OmEnumSyncLog',            'dom_om_config', 'om_core',   '枚举同步日志',     'file-text',    '#86909C','枚举同步日志',     '枚举同步执行日志，对应 ont_enum_sync_log'),
  ('class-om-cf-08','OmSharedProperty',         'dom_om_config', 'om_core',   '共享属性',         'key',          '#13C2C2','共享属性',         '跨类复用的公共属性，对应 ont_shared_properties'),
  ('class-om-cf-09','OmStructType',             'dom_om_config', 'om_core',   '结构类型',         'layout',       '#13C2C2','结构类型',         '结构属性的结构体类型，对应 ont_struct_types'),
  ('class-om-cf-10','OmStructItem',             'dom_om_config', 'om_core',   '结构项',           'menu',         '#13C2C2','结构项',           '结构类型字段定义，对应 ont_struct_items'),
  ('class-om-cf-11','OmTypeClass',              'dom_om_config', 'om_core',   '类型类',           'book',         '#165DFF','类型类',           '字典编码类型类，对应 ont_type_class'),
  ('class-om-cf-12','OmTypeClassBind',          'dom_om_config', 'om_core',   '类型类绑定',       'link',         '#165DFF','类型类绑定',       '类型类与分类绑定，对应 ont_type_class_bind'),
  ('class-om-cf-13','OmTypeClassCatDict',       'dom_om_config', 'om_core',   '类型类分类字典',   'book-open',    '#165DFF','类型类分类字典',   '类型类与分类字典映射，对应 ont_type_class_category_dict'),
  ('class-om-cf-14','OmDicTypeClass',           'dom_om_config', 'om_core',   '字典型类型类',     'book',         '#86909C','字典型类型类',     '基于字典的类型类，对应 ont_dic_type_class'),
  # 动作与规则
  ('class-om-ac-01','OmClassAction',            'dom_om_action', 'om_action', '类动作',           'zap',          '#722ED1','类动作',           '对象类绑定的动作定义，对应 ont_class_action'),
  ('class-om-ac-02','OmClassActionRule',        'dom_om_action', 'om_action', '动作规则',         'shield',       '#722ED1','动作规则',         '动作触发规则条件，对应 ont_class_action_rule'),
  ('class-om-ac-03','OmActionRuleCond',         'dom_om_action', 'om_action', '规则条件',         'filter',       '#722ED1','规则条件',         '动作规则条件表达式，对应 ont_action_rule_condition'),
  ('class-om-ac-04','OmActionLinkRule',         'dom_om_action', 'om_action', '链接动作规则',     'link',         '#722ED1','链接动作规则',     '链接类型动作规则，对应 ont_action_link_rule_config'),
  ('class-om-ac-05','OmActionRulePropMap',      'dom_om_action', 'om_action', '规则属性映射',     'shuffle',      '#EB2F96','规则属性映射',     '动作规则属性映射，对应 ont_action_rule_property_mapping'),
  ('class-om-ac-06','OmActionFormParam',        'dom_om_action', 'om_action', '表单参数',         'layout',       '#EB2F96','表单参数',         '动作表单参数定义，对应 ont_action_form_param'),
  ('class-om-ac-07','OmActionFormParamDisp',    'dom_om_action', 'om_action', '表单参数显示',     'eye',          '#EB2F96','表单参数显示',     '表单参数显示配置，对应 ont_action_form_param_display'),
  ('class-om-ac-08','OmActionFormSection',      'dom_om_action', 'om_action', '表单分区',         'columns',      '#EB2F96','表单分区',         '表单分区布局，对应 ont_action_form_section'),
  ('class-om-ac-09','OmActionFormGlobal',       'dom_om_action', 'om_action', '表单全局配置',     'settings',     '#EB2F96','表单全局配置',     '表单全局参数，对应 ont_action_form_global_config'),
  ('class-om-ac-10','OmActionFormOverrideBlock','dom_om_action', 'om_action', '表单覆盖块',       'box',          '#EB2F96','表单覆盖块',       '表单覆盖配置块，对应 ont_action_form_override_block'),
  ('class-om-ac-11','OmActionFormOverrideItem', 'dom_om_action', 'om_action', '表单覆盖项',       'edit-2',       '#EB2F96','表单覆盖项',       '表单覆盖具体项，对应 ont_action_form_override_item'),
  ('class-om-ac-12','OmActionFormDispBool',     'dom_om_action', 'om_action', '布尔显示配置',     'toggle-left',  '#EB2F96','布尔显示配置',     '布尔属性表单显示，对应 ont_action_form_display_boolean'),
  ('class-om-ac-13','OmActionFormDispNum',      'dom_om_action', 'om_action', '数字显示配置',     'hash',         '#EB2F96','数字显示配置',     '数字属性表单显示，对应 ont_action_form_display_number'),
  ('class-om-ac-14','OmActionFormDispStr',      'dom_om_action', 'om_action', '字符串显示配置',   'type',         '#EB2F96','字符串显示配置',   '字符串属性表单显示，对应 ont_action_form_display_string'),
  ('class-om-ac-15','OmActionFormDispObj',      'dom_om_action', 'om_action', '对象显示配置',     'cube',         '#EB2F96','对象显示配置',     '对象属性表单显示，对应 ont_action_form_display_object'),
  ('class-om-ac-16','OmActionFuncParamMap',     'dom_om_action', 'om_action', '函数参数映射',     'cpu',          '#165DFF','函数参数映射',     '动作函数参数映射，对应 ont_action_function_param_mapping'),
  ('class-om-ac-17','OmActionFuncRuleConf',     'dom_om_action', 'om_action', '函数规则配置',     'code',         '#165DFF','函数规则配置',     '函数规则配置，对应 ont_action_function_rule_config'),
  ('class-om-ac-18','OmActionFuncExcMap',       'dom_om_action', 'om_action', '函数异常映射',     'alert-circle', '#F53F3F','函数异常映射',     '函数异常处理映射，对应 ont_action_function_exception_map'),
  ('class-om-ac-19','OmActionSubmitConf',       'dom_om_action', 'om_action', '提交标准配置',     'check-circle', '#00B42A','提交标准配置',     '动作提交标准配置，对应 ont_action_submit_standard_config'),
  ('class-om-ac-20','OmActionSubmitCondNode',   'dom_om_action', 'om_action', '提交条件节点',     'git-branch',   '#00B42A','提交条件节点',     '提交流程条件分支，对应 ont_action_submit_condition_node'),
  ('class-om-ac-21','OmActionNotifyRule',       'dom_om_action', 'om_action', '通知规则配置',     'bell',         '#00B42A','通知规则',         '动作通知规则，对应 ont_action_notification_rule_config'),
  ('class-om-ac-22','OmActionWebhookMap',       'dom_om_action', 'om_action', 'Webhook输入映射',  'send',         '#165DFF','Webhook映射',      'Webhook输入参数映射，对应 ont_action_webhook_input_mapping'),
  ('class-om-ac-23','OmActionWebhookRule',      'dom_om_action', 'om_action', 'Webhook规则',      'globe',        '#165DFF','Webhook规则',      'Webhook调用规则，对应 ont_action_webhook_rule_config'),
  ('class-om-ac-24','OmActionOverrideCondGrp',  'dom_om_action', 'om_action', '覆盖条件组',       'layers',       '#722ED1','覆盖条件组',       '动作覆盖条件分组，对应 ont_action_override_condition_group'),
  ('class-om-ac-25','OmActionOverrideCondItem', 'dom_om_action', 'om_action', '覆盖条件项',       'filter',       '#722ED1','覆盖条件项',       '覆盖条件具体项，对应 ont_action_override_condition_item'),
  ('class-om-ac-26','OmActionExecution',        'dom_om_action', 'om_action', '动作执行记录',     'activity',     '#86909C','执行记录',         '动作执行历史，对应 ont_action_execution'),
  # 接口与数据源
  ('class-om-if-01','OmInterface',              'dom_om_iface',  'om_core',   '接口',             'link',         '#EB2F96','接口',             '本体接口定义，对应 ont_interface'),
  ('class-om-if-02','OmInterfaceProperty',      'dom_om_iface',  'om_core',   '接口属性',         'sliders',      '#EB2F96','接口属性',         '接口属性声明，对应 ont_interface_property'),
  ('class-om-if-03','OmInterfaceClass',         'dom_om_iface',  'om_core',   '接口类绑定',       'check-square', '#EB2F96','接口类绑定',       '接口与对象类绑定，对应 ont_interface_class'),
  ('class-om-if-04','OmExtDataSource',          'dom_om_iface',  'om_core',   '外部数据源',       'database',     '#722ED1','外部数据源',       '外部系统数据源，对应 ont_ext_data_source'),
  ('class-om-if-05','OmExtApiGroup',            'dom_om_iface',  'om_core',   'API分组',          'folder',       '#722ED1','API分组',          '数据源API接口分组，对应 ont_ext_api_group'),
  ('class-om-if-06','OmExtApiInterface',        'dom_om_iface',  'om_core',   'API接口',          'globe',        '#722ED1','API接口',          '具体API接口定义，对应 ont_ext_api_interface'),
  ('class-om-if-07','OmExtApiCallLog',          'dom_om_iface',  'om_core',   'API调用日志',      'file-text',    '#86909C','API调用日志',      'API调用执行日志，对应 ont_ext_api_call_log'),
  ('class-om-if-08','OmPhysicalTable',          'dom_om_iface',  'om_core',   '物理表',           'table',        '#165DFF','物理表',           '数据源物理表结构，对应 ont_physical_table'),
  # 系统支撑
  ('class-om-sy-01','OmIconGroup',              'dom_om_system', 'om_sys',    '图标分组',         'folder',       '#13C2C2','图标分组',         '图标库分组，对应 icon_lib_group'),
  ('class-om-sy-02','OmIconItem',               'dom_om_system', 'om_sys',    '图标',             'image',        '#13C2C2','图标',             'SVG图标定义，对应 icon_lib_icon'),
  ('class-om-sy-03','OmDictDef',                'dom_om_system', 'om_sys',    '字典定义',         'book',         '#FF7D00','字典定义',         '系统字典类型，对应 ont_dict_def'),
  ('class-om-sy-04','OmDictItem',               'dom_om_system', 'om_sys',    '字典项',           'tag',          '#FF7D00','字典项',           '字典条目，对应 ont_dict_item'),
  ('class-om-sy-05','OmDomainTerm',             'dom_om_system', 'om_sys',    '领域术语',         'book-open',    '#FF7D00','领域术语',         '业务术语词条，对应 ont_domain_term'),
  ('class-om-sy-06','OmOntologyVersion',        'dom_om_system', 'om_sys',    '本体版本',         'git-commit',   '#86909C','本体版本',         'OWL模型版本号，对应 ont_ontology_version'),
  ('class-om-sy-07','OmExploreDesign',          'dom_om_system', 'om_sys',    '探索设计',         'compass',      '#86909C','探索设计',         '知识图谱探索视图设计，对应 ont_explore_design'),
]

def q(s):
    return "'" + s.replace("'", "''") + "'"

lines = []
for c in CLASSES:
    cid, api, cat, ns, dn, icon, color, label, comment = c
    rid = f"ri.ont.class.{cid}"
    lines.append(
        f"  ({q(api)},{q(cat)},'{{}}',NULL,{q(color)},{q(T)},{q(comment)},{q(dn)},{q(icon)},{q(cid)},0,0,0,NULL,{q(ns)},NULL,{q(comment)},NULL,{q(label)},NULL,{q(rid)},1,{q(T)})"
    )

sql = "\n-- ============================================================\n"
sql += "-- 5. 对象类 (65 类，覆盖所有项目数据表)\n"
sql += "-- ============================================================\n"
sql += "INSERT INTO ont_class (api_name,category_code,class_expr_content,class_expr_type,color,create_time,description,display_name,icon,id,is_common,is_nothing,is_thing,metadata,ns_code,parent_class_id,rdfs_comment,rdfs_defined_by,rdfs_label,rdfs_see_also,rid,status,update_time) VALUES\n"
sql += ",\n".join(lines) + "\nON CONFLICT (api_name) DO NOTHING;\n"

with open(TARGET, 'a', encoding='utf-8') as f:
    f.write(sql)
print(f"classes written: {len(CLASSES)}")
