#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""gen_v39_rest.py — 生成 V39 剩余 SQL: 类属性/链接类型/分组类绑定
追加到 V39__ontomgmt_platform_ontology.sql
"""
import sys

OUT = r'f:\aiProject\BOnotLink-Manager\backend\bontolink-data\src\main\resources\db\migration\common\V39__ontomgmt_platform_ontology.sql'
TS = '2026-08-04 12:00:00'

def q(v):
    if v is None:
        return 'NULL'
    return "'" + str(v).replace("'", "''") + "'"

# ============================================================
# SECTION 1: ont_class_property
# ============================================================
# cols: id, class_id, category_code, api_name, prop_code, prop_type, data_type,
#       display_name, rdfs_label, rdfs_comment, physical_table, physical_column,
#       is_primary, is_required, is_key, sort, status, create_time, update_time

CP_COLS = ('id', 'class_id', 'category_code', 'api_name', 'prop_code',
           'prop_type', 'data_type', 'display_name', 'rdfs_label',
           'physical_table', 'physical_column',
           'is_primary', 'is_required', 'is_key', 'sort',
           'status', 'create_time', 'update_time')

def cp_row(pid, cid, cat, api, ptype, dtype, disp, tbl, col,
           pk=0, req=0, key=0, sort=0):
    rid_val = f'ri.ont.prop.{pid}'
    vals = [q(pid), q(cid), q(cat), q(api), q(api),
            q(ptype), q(dtype), q(disp), q(disp),
            q(tbl), q(col), pk, req, key, sort, 1, q(TS), q(TS)]
    return '  (' + ','.join(str(v) for v in vals) + ')'

# 所有类属性行
cp_rows = []

# --- OmBizNamespace (class-om-ns-01) ---
ns01 = 'class-om-ns-01'
cp_rows += [
    cp_row('cp-ns01-01', ns01, 'dom_om_bizclass', 'ns_code',    'string', 'ns_code', '命名空间编码', 'ont_biz_namespace', 'ns_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-ns01-02', ns01, 'dom_om_bizclass', 'ns_name',    'string', 'ns_name', '命名空间名称', 'ont_biz_namespace', 'ns_name', req=1, sort=2),
    cp_row('cp-ns01-03', ns01, 'dom_om_bizclass', 'ns_uri',     'string', 'ns_uri',  '命名空间URI',  'ont_biz_namespace', 'ns_uri', sort=3),
    cp_row('cp-ns01-04', ns01, 'dom_om_bizclass', 'hierarchyPath','string','hierarchy_path','层级路径','ont_biz_namespace','hierarchy_path', sort=4),
    cp_row('cp-ns01-05', ns01, 'dom_om_bizclass', 'currVersion','string', 'curr_version','当前版本','ont_biz_namespace','curr_version', sort=5),
    cp_row('cp-ns01-06', ns01, 'dom_om_bizclass', 'status',     'integer','status',  '状态',         'ont_biz_namespace', 'status', req=1, sort=6),
]

# --- OmBizCategory (class-om-ns-03) ---
ns03 = 'class-om-ns-03'
cp_rows += [
    cp_row('cp-ns03-01', ns03, 'dom_om_bizclass', 'categoryCode','string','category_code','分类编码','ont_biz_category','category_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-ns03-02', ns03, 'dom_om_bizclass', 'categoryType','integer','category_type','分类层级','ont_biz_category','category_type', req=1, sort=2),
    cp_row('cp-ns03-03', ns03, 'dom_om_bizclass', 'parentId',   'string', 'parent_id',  '父分类ID',   'ont_biz_category', 'parent_id', sort=3),
    cp_row('cp-ns03-04', ns03, 'dom_om_bizclass', 'nsCode',     'string', 'ns_code',    '命名空间编码','ont_biz_category','ns_code', sort=4),
    cp_row('cp-ns03-05', ns03, 'dom_om_bizclass', 'sort',       'integer','sort',        '排序',        'ont_biz_category','sort', sort=5),
    cp_row('cp-ns03-06', ns03, 'dom_om_bizclass', 'status',     'integer','status',      '状态',        'ont_biz_category','status', req=1, sort=6),
]

# --- OmBizGroup (class-om-ns-04) ---
ns04 = 'class-om-ns-04'
cp_rows += [
    cp_row('cp-ns04-01', ns04, 'dom_om_bizclass', 'gName',      'string', 'g_name',     '分组名称',    'ont_biz_group','g_name', req=1, sort=1),
    cp_row('cp-ns04-02', ns04, 'dom_om_bizclass', 'categoryCode','string','category_code','所属分类',   'ont_biz_group','category_code', sort=2),
    cp_row('cp-ns04-03', ns04, 'dom_om_bizclass', 'domainCode', 'string', 'domain_code','领域编码',    'ont_biz_group','domain_code', sort=3),
    cp_row('cp-ns04-04', ns04, 'dom_om_bizclass', 'gSort',      'integer','g_sort',      '排序',        'ont_biz_group','g_sort', sort=4),
    cp_row('cp-ns04-05', ns04, 'dom_om_bizclass', 'icon',       'string', 'icon',        '图标',        'ont_biz_group','icon', sort=5),
    cp_row('cp-ns04-06', ns04, 'dom_om_bizclass', 'color',      'string', 'color',       '颜色',        'ont_biz_group','color', sort=6),
]

# --- OmClass (class-om-co-01) — 核心类，8个属性 ---
co01 = 'class-om-co-01'
cp_rows += [
    cp_row('cp-co01-01', co01, 'dom_om_core', 'apiName',    'string', 'api_name',    'API名称',    'ont_class','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-co01-02', co01, 'dom_om_core', 'displayName','string', 'display_name','显示名称',   'ont_class','display_name', req=1, sort=2),
    cp_row('cp-co01-03', co01, 'dom_om_core', 'categoryCode','string','category_code','领域分类',  'ont_class','category_code', sort=3),
    cp_row('cp-co01-04', co01, 'dom_om_core', 'nsCode',     'string', 'ns_code',     '命名空间',   'ont_class','ns_code', sort=4),
    cp_row('cp-co01-05', co01, 'dom_om_core', 'icon',       'string', 'icon',        '图标名',     'ont_class','icon', sort=5),
    cp_row('cp-co01-06', co01, 'dom_om_core', 'color',      'string', 'color',       '主题色',     'ont_class','color', sort=6),
    cp_row('cp-co01-07', co01, 'dom_om_core', 'status',     'integer','status',      '状态',       'ont_class','status', req=1, sort=7),
    cp_row('cp-co01-08', co01, 'dom_om_core', 'isCommon',   'boolean','is_common',   '是否通用类', 'ont_class','is_common', sort=8),
]

# --- OmClassProperty (class-om-co-02) — 8个属性 ---
co02 = 'class-om-co-02'
cp_rows += [
    cp_row('cp-co02-01', co02, 'dom_om_core', 'apiName',    'string', 'api_name',     'API名称',    'ont_class_property','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-co02-02', co02, 'dom_om_core', 'propType',   'string', 'prop_type',    '属性类型',   'ont_class_property','prop_type', req=1, sort=2),
    cp_row('cp-co02-03', co02, 'dom_om_core', 'dataType',   'string', 'data_type',    '数据类型',   'ont_class_property','data_type', sort=3),
    cp_row('cp-co02-04', co02, 'dom_om_core', 'displayName','string', 'display_name', '显示名称',   'ont_class_property','display_name', sort=4),
    cp_row('cp-co02-05', co02, 'dom_om_core', 'isRequired', 'boolean','is_required',  '是否必填',   'ont_class_property','is_required', sort=5),
    cp_row('cp-co02-06', co02, 'dom_om_core', 'isKey',      'boolean','is_key',       '是否关键属性','ont_class_property','is_key', sort=6),
    cp_row('cp-co02-07', co02, 'dom_om_core', 'physicalTable','string','physical_table','物理表名',  'ont_class_property','physical_table', sort=7),
    cp_row('cp-co02-08', co02, 'dom_om_core', 'physicalColumn','string','physical_column','物理列名','ont_class_property','physical_column', sort=8),
]

# --- OmClassHierarchy (class-om-co-04) ---
co04 = 'class-om-co-04'
cp_rows += [
    cp_row('cp-co04-01', co04, 'dom_om_core', 'parentClassId','string','parent_class_id','父类ID',   'ont_class_hierarchy','parent_class_id', req=1, sort=1),
    cp_row('cp-co04-02', co04, 'dom_om_core', 'subClassId',  'string','sub_class_id',  '子类ID',    'ont_class_hierarchy','sub_class_id', req=1, sort=2),
    cp_row('cp-co04-03', co04, 'dom_om_core', 'inheritType', 'string','inherit_type',  '继承类型',  'ont_class_hierarchy','inherit_type', sort=3),
]

# --- OmLinkType (class-om-co-12) — 6个属性 ---
co12 = 'class-om-co-12'
cp_rows += [
    cp_row('cp-co12-01', co12, 'dom_om_core', 'linkTypeId',      'string','link_type_id',     '链接类型标识',  'ont_link_types','link_type_id', pk=1, req=1, key=1, sort=1),
    cp_row('cp-co12-02', co12, 'dom_om_core', 'lObjectTypeId',   'string','l_object_type_id', '源端对象类',    'ont_link_types','l_object_type_id', req=1, sort=2),
    cp_row('cp-co12-03', co12, 'dom_om_core', 'rObjectTypeId',   'string','r_object_type_id', '目标端对象类',  'ont_link_types','r_object_type_id', req=1, sort=3),
    cp_row('cp-co12-04', co12, 'dom_om_core', 'lCardinality',    'string','l_cardinality',    '源端基数',      'ont_link_types','l_cardinality', sort=4),
    cp_row('cp-co12-05', co12, 'dom_om_core', 'rCardinality',    'string','r_cardinality',    '目标端基数',    'ont_link_types','r_cardinality', sort=5),
    cp_row('cp-co12-06', co12, 'dom_om_core', 'status',          'string','status',            '状态',          'ont_link_types','status', req=1, sort=6),
]

# --- OmLinkMapping (class-om-co-13) ---
co13 = 'class-om-co-13'
cp_rows += [
    cp_row('cp-co13-01', co13, 'dom_om_core', 'sourceClassId','string','source_class_id','源类ID',   'ont_class_link','source_class_id', req=1, sort=1),
    cp_row('cp-co13-02', co13, 'dom_om_core', 'targetClassId','string','target_class_id','目标类ID', 'ont_class_link','target_class_id', req=1, sort=2),
    cp_row('cp-co13-03', co13, 'dom_om_core', 'cardinality',  'string','cardinality',    '基数关系', 'ont_class_link','cardinality', sort=3),
    cp_row('cp-co13-04', co13, 'dom_om_core', 'displayName',  'string','display_name',   '显示名称', 'ont_class_link','display_name', sort=4),
]

# --- OmValueType (class-om-cf-01) ---
cf01 = 'class-om-cf-01'
cp_rows += [
    cp_row('cp-cf01-01', cf01, 'dom_om_config', 'apiName',    'string', 'api_name',    'API名称',   'ont_value_types','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf01-02', cf01, 'dom_om_config', 'displayName','string', 'display_name','显示名称',  'ont_value_types','display_name', req=1, sort=2),
    cp_row('cp-cf01-03', cf01, 'dom_om_config', 'baseType',   'string', 'base_type',   '基础类型',  'ont_value_types','base_type', sort=3),
    cp_row('cp-cf01-04', cf01, 'dom_om_config', 'formatType', 'string', 'format_type', '格式类型',  'ont_value_types','format_type', sort=4),
    cp_row('cp-cf01-05', cf01, 'dom_om_config', 'status',     'integer','status',      '状态',      'ont_value_types','status', req=1, sort=5),
]

# --- OmEnumType (class-om-cf-03) ---
cf03 = 'class-om-cf-03'
cp_rows += [
    cp_row('cp-cf03-01', cf03, 'dom_om_config', 'apiName',    'string', 'api_name',    'API名称',   'ont_enum_types','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf03-02', cf03, 'dom_om_config', 'displayName','string', 'display_name','显示名称',  'ont_enum_types','display_name', req=1, sort=2),
    cp_row('cp-cf03-03', cf03, 'dom_om_config', 'categoryCode','string','category_code','所属分类', 'ont_enum_types','category_code', sort=3),
    cp_row('cp-cf03-04', cf03, 'dom_om_config', 'status',     'string', 'status',      '状态',      'ont_enum_types','status', req=1, sort=4),
    cp_row('cp-cf03-05', cf03, 'dom_om_config', 'syncEnabled','boolean','sync_enabled', '启用同步',  'ont_enum_types','sync_enabled', sort=5),
]

# --- OmEnumItem (class-om-cf-04) ---
cf04 = 'class-om-cf-04'
cp_rows += [
    cp_row('cp-cf04-01', cf04, 'dom_om_config', 'code',       'string', 'code',        '枚举编码',  'ont_enum_items','code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf04-02', cf04, 'dom_om_config', 'label',      'string', 'label',       '枚举标签',  'ont_enum_items','label', req=1, sort=2),
    cp_row('cp-cf04-03', cf04, 'dom_om_config', 'sort',       'integer','sort',        '排序',      'ont_enum_items','sort', sort=3),
    cp_row('cp-cf04-04', cf04, 'dom_om_config', 'parentCode', 'string', 'parent_code', '父级编码',  'ont_enum_items','parent_code', sort=4),
    cp_row('cp-cf04-05', cf04, 'dom_om_config', 'status',     'integer','status',      '状态',      'ont_enum_items','status', sort=5),
]

# --- OmSharedProperty (class-om-cf-08) ---
cf08 = 'class-om-cf-08'
cp_rows += [
    cp_row('cp-cf08-01', cf08, 'dom_om_config', 'propCode',   'string', 'prop_code',   '属性编码',  'ont_shared_properties','prop_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf08-02', cf08, 'dom_om_config', 'displayName','string', 'display_name','显示名称',  'ont_shared_properties','display_name', req=1, sort=2),
    cp_row('cp-cf08-03', cf08, 'dom_om_config', 'propType',   'string', 'prop_type',   '属性类型',  'ont_shared_properties','prop_type', sort=3),
    cp_row('cp-cf08-04', cf08, 'dom_om_config', 'dataType',   'string', 'data_type',   '数据类型',  'ont_shared_properties','data_type', sort=4),
    cp_row('cp-cf08-05', cf08, 'dom_om_config', 'isRequired', 'boolean','is_required', '是否必填',  'ont_shared_properties','is_required', sort=5),
    cp_row('cp-cf08-06', cf08, 'dom_om_config', 'status',     'integer','status',      '状态',      'ont_shared_properties','status', req=1, sort=6),
]

# --- OmStructType (class-om-cf-09) ---
cf09 = 'class-om-cf-09'
cp_rows += [
    cp_row('cp-cf09-01', cf09, 'dom_om_config', 'structCode',  'string','struct_code',  '结构编码',  'ont_struct_types','struct_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf09-02', cf09, 'dom_om_config', 'displayName', 'string','display_name', '显示名称',  'ont_struct_types','display_name', req=1, sort=2),
    cp_row('cp-cf09-03', cf09, 'dom_om_config', 'nsCode',      'string','ns_code',      '命名空间',  'ont_struct_types','ns_code', sort=3),
    cp_row('cp-cf09-04', cf09, 'dom_om_config', 'status',      'integer','status',      '状态',      'ont_struct_types','status', sort=4),
]

# --- OmStructItem (class-om-cf-10) ---
cf10 = 'class-om-cf-10'
cp_rows += [
    cp_row('cp-cf10-01', cf10, 'dom_om_config', 'propCode',   'string', 'prop_code',   '字段编码',  'ont_struct_items','prop_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf10-02', cf10, 'dom_om_config', 'displayName','string', 'display_name','字段名称',  'ont_struct_items','display_name', req=1, sort=2),
    cp_row('cp-cf10-03', cf10, 'dom_om_config', 'dataType',   'string', 'data_type',   '数据类型',  'ont_struct_items','data_type', sort=3),
    cp_row('cp-cf10-04', cf10, 'dom_om_config', 'sort',       'integer','sort',        '排序',      'ont_struct_items','sort', sort=4),
    cp_row('cp-cf10-05', cf10, 'dom_om_config', 'isRequired', 'boolean','is_required', '是否必填',  'ont_struct_items','is_required', sort=5),
]

# --- OmTypeClass (class-om-cf-11) ---
cf11 = 'class-om-cf-11'
cp_rows += [
    cp_row('cp-cf11-01', cf11, 'dom_om_config', 'apiName',    'string', 'api_name',    'API名称',   'ont_type_class','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-cf11-02', cf11, 'dom_om_config', 'displayName','string', 'display_name','显示名称',  'ont_type_class','display_name', req=1, sort=2),
    cp_row('cp-cf11-03', cf11, 'dom_om_config', 'categoryCode','string','category_code','所属分类', 'ont_type_class','category_code', sort=3),
    cp_row('cp-cf11-04', cf11, 'dom_om_config', 'status',     'integer','status',      '状态',      'ont_type_class','status', req=1, sort=4),
]

# --- OmClassAction (class-om-ac-01) — 6个属性 ---
ac01 = 'class-om-ac-01'
cp_rows += [
    cp_row('cp-ac01-01', ac01, 'dom_om_action', 'apiName',    'string', 'api_name',    'API名称',   'ont_class_action','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-ac01-02', ac01, 'dom_om_action', 'displayName','string', 'display_name','显示名称',  'ont_class_action','display_name', req=1, sort=2),
    cp_row('cp-ac01-03', ac01, 'dom_om_action', 'actionKind', 'string', 'action_kind', '动作类型',  'ont_class_action','action_kind', sort=3),
    cp_row('cp-ac01-04', ac01, 'dom_om_action', 'classId',    'string', 'class_id',    '所属对象类','ont_class_action','class_id', req=1, sort=4),
    cp_row('cp-ac01-05', ac01, 'dom_om_action', 'status',     'integer','status',      '状态',      'ont_class_action','status', req=1, sort=5),
    cp_row('cp-ac01-06', ac01, 'dom_om_action', 'formEnabled','boolean','form_enabled','启用表单',  'ont_class_action','form_enabled', sort=6),
]

# --- OmActionFormParam (class-om-ac-06) ---
ac06 = 'class-om-ac-06'
cp_rows += [
    cp_row('cp-ac06-01', ac06, 'dom_om_action', 'paramCode',  'string', 'param_code',  '参数编码',  'ont_action_form_param','param_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-ac06-02', ac06, 'dom_om_action', 'displayName','string', 'display_name','参数名称',  'ont_action_form_param','display_name', req=1, sort=2),
    cp_row('cp-ac06-03', ac06, 'dom_om_action', 'paramType',  'string', 'param_type',  '参数类型',  'ont_action_form_param','param_type', sort=3),
    cp_row('cp-ac06-04', ac06, 'dom_om_action', 'isRequired', 'boolean','is_required', '是否必填',  'ont_action_form_param','is_required', sort=4),
    cp_row('cp-ac06-05', ac06, 'dom_om_action', 'sort',       'integer','sort',        '排序',      'ont_action_form_param','sort', sort=5),
]

# --- OmActionRulePropMap (class-om-ac-05) ---
ac05 = 'class-om-ac-05'
cp_rows += [
    cp_row('cp-ac05-01', ac05, 'dom_om_action', 'ruleId',     'string', 'rule_id',     '规则ID',    'ont_action_rule_property_mapping','rule_id', req=1, sort=1),
    cp_row('cp-ac05-02', ac05, 'dom_om_action', 'targetProp', 'string', 'target_prop', '目标属性',  'ont_action_rule_property_mapping','target_prop', req=1, sort=2),
    cp_row('cp-ac05-03', ac05, 'dom_om_action', 'sourceType', 'string', 'source_type', '来源类型',  'ont_action_rule_property_mapping','source_type', sort=3),
    cp_row('cp-ac05-04', ac05, 'dom_om_action', 'sourceValue','string', 'source_value','来源值',    'ont_action_rule_property_mapping','source_value', sort=4),
]

# --- OmInterface (class-om-if-01) ---
if01 = 'class-om-if-01'
cp_rows += [
    cp_row('cp-if01-01', if01, 'dom_om_iface', 'apiName',    'string', 'api_name',    'API名称',   'ont_interface','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-if01-02', if01, 'dom_om_iface', 'displayName','string', 'display_name','显示名称',  'ont_interface','display_name', req=1, sort=2),
    cp_row('cp-if01-03', if01, 'dom_om_iface', 'categoryCode','string','category_code','所属分类', 'ont_interface','category_code', sort=3),
    cp_row('cp-if01-04', if01, 'dom_om_iface', 'status',     'integer','status',      '状态',      'ont_interface','status', req=1, sort=4),
]

# --- OmExtDataSource (class-om-if-04) — 6个属性 ---
if04 = 'class-om-if-04'
cp_rows += [
    cp_row('cp-if04-01', if04, 'dom_om_iface', 'dsCode',     'string', 'ds_code',     '数据源编码','ont_ext_data_source','ds_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-if04-02', if04, 'dom_om_iface', 'dsName',     'string', 'ds_name',     '数据源名称','ont_ext_data_source','ds_name', req=1, sort=2),
    cp_row('cp-if04-03', if04, 'dom_om_iface', 'dsType',     'string', 'ds_type',     '数据源类型','ont_ext_data_source','ds_type', sort=3),
    cp_row('cp-if04-04', if04, 'dom_om_iface', 'baseUrl',    'string', 'base_url',    '基础URL',   'ont_ext_data_source','base_url', sort=4),
    cp_row('cp-if04-05', if04, 'dom_om_iface', 'authType',   'string', 'auth_type',   '鉴权类型',  'ont_ext_data_source','auth_type', sort=5),
    cp_row('cp-if04-06', if04, 'dom_om_iface', 'status',     'integer','status',      '状态',      'ont_ext_data_source','status', req=1, sort=6),
]

# --- OmExtApiGroup (class-om-if-05) ---
if05 = 'class-om-if-05'
cp_rows += [
    cp_row('cp-if05-01', if05, 'dom_om_iface', 'groupName',  'string', 'group_name',  '分组名称',  'ont_ext_api_group','group_name', req=1, sort=1),
    cp_row('cp-if05-02', if05, 'dom_om_iface', 'groupCode',  'string', 'group_code',  '分组编码',  'ont_ext_api_group','group_code', sort=2),
    cp_row('cp-if05-03', if05, 'dom_om_iface', 'sort',       'integer','sort',        '排序',      'ont_ext_api_group','sort', sort=3),
    cp_row('cp-if05-04', if05, 'dom_om_iface', 'status',     'integer','status',      '状态',      'ont_ext_api_group','status', sort=4),
]

# --- OmExtApiInterface (class-om-if-06) ---
if06 = 'class-om-if-06'
cp_rows += [
    cp_row('cp-if06-01', if06, 'dom_om_iface', 'apiName',    'string', 'api_name',    'API名称',   'ont_ext_api_interface','api_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-if06-02', if06, 'dom_om_iface', 'method',     'string', 'method',      'HTTP方法',  'ont_ext_api_interface','method', sort=2),
    cp_row('cp-if06-03', if06, 'dom_om_iface', 'path',       'string', 'path',        '接口路径',  'ont_ext_api_interface','path', sort=3),
    cp_row('cp-if06-04', if06, 'dom_om_iface', 'description','string', 'description', '接口描述',  'ont_ext_api_interface','description', sort=4),
    cp_row('cp-if06-05', if06, 'dom_om_iface', 'status',     'integer','status',      '状态',      'ont_ext_api_interface','status', sort=5),
]

# --- OmIconGroup (class-om-sy-01) ---
sy01 = 'class-om-sy-01'
cp_rows += [
    cp_row('cp-sy01-01', sy01, 'dom_om_system', 'groupCode', 'string', 'group_code',  '分组编码',  'icon_lib_group','group_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-sy01-02', sy01, 'dom_om_system', 'groupName', 'string', 'group_name',  '分组名称',  'icon_lib_group','group_name', req=1, sort=2),
    cp_row('cp-sy01-03', sy01, 'dom_om_system', 'sort',      'integer','sort',        '排序',      'icon_lib_group','sort', sort=3),
]

# --- OmIconItem (class-om-sy-02) ---
sy02 = 'class-om-sy-02'
cp_rows += [
    cp_row('cp-sy02-01', sy02, 'dom_om_system', 'iconName',  'string', 'icon_name',   '图标名称',  'icon_lib_icon','icon_name', pk=1, req=1, key=1, sort=1),
    cp_row('cp-sy02-02', sy02, 'dom_om_system', 'svgContent','string', 'svg_content', 'SVG内容',   'icon_lib_icon','svg_content', sort=2),
    cp_row('cp-sy02-03', sy02, 'dom_om_system', 'groupId',   'string', 'group_id',    '所属分组',  'icon_lib_icon','group_id', sort=3),
    cp_row('cp-sy02-04', sy02, 'dom_om_system', 'status',    'integer','status',      '状态',      'icon_lib_icon','status', sort=4),
]

# --- OmDictDef (class-om-sy-03) ---
sy03 = 'class-om-sy-03'
cp_rows += [
    cp_row('cp-sy03-01', sy03, 'dom_om_system', 'dictCode',  'string', 'dict_code',   '字典编码',  'ont_dict_def','dict_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-sy03-02', sy03, 'dom_om_system', 'dictName',  'string', 'dict_name',   '字典名称',  'ont_dict_def','dict_name', req=1, sort=2),
    cp_row('cp-sy03-03', sy03, 'dom_om_system', 'status',    'integer','status',      '状态',      'ont_dict_def','status', sort=3),
]

# --- OmDomainTerm (class-om-sy-05) ---
sy05 = 'class-om-sy-05'
cp_rows += [
    cp_row('cp-sy05-01', sy05, 'dom_om_system', 'termCode',  'string', 'term_code',   '术语编码',  'ont_domain_term','term_code', pk=1, req=1, key=1, sort=1),
    cp_row('cp-sy05-02', sy05, 'dom_om_system', 'termName',  'string', 'term_name',   '术语名称',  'ont_domain_term','term_name', req=1, sort=2),
    cp_row('cp-sy05-03', sy05, 'dom_om_system', 'definition','string', 'definition',  '定义',      'ont_domain_term','definition', sort=3),
    cp_row('cp-sy05-04', sy05, 'dom_om_system', 'categoryCode','string','category_code','所属分类','ont_domain_term','category_code', sort=4),
]

# ============================================================
# SECTION 2: ont_link_types
# ============================================================
# cols: id, link_type_id, status, l_object_type_id, r_object_type_id,
#       l_cardinality, r_cardinality, l_display_name, r_display_name,
#       l_api_name, r_api_name, rdfs_label, rdfs_comment, category_code,
#       created_at, updated_at

LT_COLS = ('id','link_type_id','status',
           'l_object_type_id','r_object_type_id',
           'l_cardinality','r_cardinality',
           'l_display_name','r_display_name',
           'l_api_name','r_api_name',
           'rdfs_label','rdfs_comment','category_code',
           'created_at','updated_at')

def lt_row(n, ltid, lid, rid_cls, lcard, rcard, lname, rname, lapi, rapi, label, comment, cat):
    oid = f'link-types-v39-{n:03d}'
    vals = [q(oid), q(ltid), q('active'),
            q(lid), q(rid_cls), q(lcard), q(rcard),
            q(lname), q(rname), q(lapi), q(rapi),
            q(label), q(comment), q(cat),
            q(TS), q(TS)]
    return '  (' + ','.join(str(v) for v in vals) + ')'

lt_rows = [
    lt_row(1,  'om-class-has-property',   'class-om-co-01','class-om-co-02','one','many',
           '对象类','属性','classProperties','belongsToClass',
           '对象类拥有属性','OmClass与OmClassProperty的从属关系','dom_om_core'),
    lt_row(2,  'om-class-has-action',     'class-om-co-01','class-om-ac-01','one','many',
           '对象类','动作','boundActions','hostClass',
           '对象类绑定动作','OmClass与OmClassAction的从属关系','dom_om_action'),
    lt_row(3,  'om-class-left-of-link',   'class-om-co-01','class-om-co-12','one','many',
           '源端类','源端链接类型','outgoingLinkTypes','leftClass',
           '类作为链接类型源端','OmClass作为OmLinkType的l_object_type','dom_om_core'),
    lt_row(4,  'om-class-right-of-link',  'class-om-co-01','class-om-co-12','one','many',
           '目标端类','目标端链接类型','incomingLinkTypes','rightClass',
           '类作为链接类型目标端','OmClass作为OmLinkType的r_object_type','dom_om_core'),
    lt_row(5,  'om-category-has-group',   'class-om-ns-03','class-om-ns-04','one','many',
           '行业分类','分组','groups','parentCategory',
           '分类包含分组','OmBizCategory与OmBizGroup的从属关系','dom_om_bizclass'),
    lt_row(6,  'om-enum-has-item',        'class-om-cf-03','class-om-cf-04','one','many',
           '枚举类型','枚举项','enumItems','enumType',
           '枚举类型包含枚举项','OmEnumType与OmEnumItem的从属关系','dom_om_config'),
    lt_row(7,  'om-sharedprop-uses-struct','class-om-cf-08','class-om-cf-09','many','one',
           '共享属性','结构类型','structType','usedByProperties',
           '共享属性引用结构类型','OmSharedProperty引用OmStructType','dom_om_config'),
    lt_row(8,  'om-struct-has-item',      'class-om-cf-09','class-om-cf-10','one','many',
           '结构类型','结构字段','structItems','structType',
           '结构类型包含结构字段','OmStructType与OmStructItem的从属关系','dom_om_config'),
    lt_row(9,  'om-extds-has-apigroup',   'class-om-if-04','class-om-if-05','one','many',
           '数据源','API分组','apiGroups','dataSource',
           '数据源包含API分组','OmExtDataSource与OmExtApiGroup的从属关系','dom_om_iface'),
    lt_row(10, 'om-apigroup-has-api',     'class-om-if-05','class-om-if-06','one','many',
           'API分组','API接口','apiInterfaces','apiGroup',
           'API分组包含API接口','OmExtApiGroup与OmExtApiInterface的从属关系','dom_om_iface'),
    lt_row(11, 'om-extds-has-phytable',   'class-om-if-04','class-om-if-08','one','many',
           '数据源','物理表','physicalTables','dataSource',
           '数据源包含物理表','OmExtDataSource与OmPhysicalTable的从属关系','dom_om_iface'),
    lt_row(12, 'om-interface-bound-class','class-om-if-01','class-om-co-01','many','many',
           '接口','实现类','implementingClasses','implementedInterfaces',
           '接口与对象类的实现绑定','OmInterface与OmClass的多对多实现关系','dom_om_iface'),
    lt_row(13, 'om-action-has-rule',      'class-om-ac-01','class-om-ac-02','one','many',
           '动作','触发规则','triggerRules','parentAction',
           '动作拥有触发规则','OmClassAction与OmClassActionRule的从属关系','dom_om_action'),
    lt_row(14, 'om-action-has-formparam', 'class-om-ac-01','class-om-ac-06','one','many',
           '动作','表单参数','formParams','parentAction',
           '动作拥有表单参数','OmClassAction与OmActionFormParam的从属关系','dom_om_action'),
    lt_row(15, 'om-ns-contains-class',    'class-om-ns-01','class-om-co-01','one','many',
           '命名空间','对象类','containedClasses','namespace',
           '命名空间包含对象类','OmBizNamespace与OmClass的归属关系','dom_om_bizclass'),
]

# ============================================================
# SECTION 3: ont_biz_group_class
# ============================================================
# cols: id, group_id, ref_id, group_type, category_code, g_sort,
#       create_time, update_time

def gc_row(n, grp_id, ref_id, cat, sort):
    gid = f'bgc-om-{n:04d}'
    vals = [q(gid), q(grp_id), q(ref_id), q('object_types'), q(cat), sort, q(TS), q(TS)]
    return '  (' + ','.join(str(v) for v in vals) + ')'

gc_rows = []
n = 1

# grp-om-01: 命名空间
for i, cid in enumerate(['class-om-ns-01','class-om-ns-02'], 1):
    gc_rows.append(gc_row(n, 'grp-om-01', cid, 'dom_om_bizclass', i)); n+=1

# grp-om-02: 行业分类
for i, cid in enumerate(['class-om-ns-03','class-om-ns-04','class-om-ns-05'], 1):
    gc_rows.append(gc_row(n, 'grp-om-02', cid, 'dom_om_bizclass', i)); n+=1

# grp-om-03: 对象类
for i, cid in enumerate([
    'class-om-co-01','class-om-co-04','class-om-co-05',
    'class-om-co-06','class-om-co-09','class-om-co-10','class-om-co-11'], 1):
    gc_rows.append(gc_row(n, 'grp-om-03', cid, 'dom_om_core', i)); n+=1

# grp-om-04: 类属性
for i, cid in enumerate([
    'class-om-co-02','class-om-co-03','class-om-co-07','class-om-co-08'], 1):
    gc_rows.append(gc_row(n, 'grp-om-04', cid, 'dom_om_core', i)); n+=1

# grp-om-05: 链接类型
for i, cid in enumerate(['class-om-co-12','class-om-co-13'], 1):
    gc_rows.append(gc_row(n, 'grp-om-05', cid, 'dom_om_core', i)); n+=1

# grp-om-06: 值类型
for i, cid in enumerate(['class-om-cf-01','class-om-cf-02'], 1):
    gc_rows.append(gc_row(n, 'grp-om-06', cid, 'dom_om_config', i)); n+=1

# grp-om-07: 枚举类型
for i, cid in enumerate([
    'class-om-cf-03','class-om-cf-04','class-om-cf-05',
    'class-om-cf-06','class-om-cf-07'], 1):
    gc_rows.append(gc_row(n, 'grp-om-07', cid, 'dom_om_config', i)); n+=1

# grp-om-08: 共享属性
for i, cid in enumerate(['class-om-cf-08','class-om-cf-09','class-om-cf-10'], 1):
    gc_rows.append(gc_row(n, 'grp-om-08', cid, 'dom_om_config', i)); n+=1

# grp-om-09: 类型类
for i, cid in enumerate([
    'class-om-cf-11','class-om-cf-12','class-om-cf-13','class-om-cf-14'], 1):
    gc_rows.append(gc_row(n, 'grp-om-09', cid, 'dom_om_config', i)); n+=1

# grp-om-10: 动作定义
for i, cid in enumerate([
    'class-om-ac-01','class-om-ac-02','class-om-ac-03','class-om-ac-04'], 1):
    gc_rows.append(gc_row(n, 'grp-om-10', cid, 'dom_om_action', i)); n+=1

# grp-om-11: 表单配置
for i, cid in enumerate([
    'class-om-ac-05','class-om-ac-06','class-om-ac-07','class-om-ac-08',
    'class-om-ac-09','class-om-ac-10','class-om-ac-11',
    'class-om-ac-12','class-om-ac-13','class-om-ac-14','class-om-ac-15'], 1):
    gc_rows.append(gc_row(n, 'grp-om-11', cid, 'dom_om_action', i)); n+=1

# grp-om-12: 规则映射
for i, cid in enumerate([
    'class-om-ac-16','class-om-ac-17','class-om-ac-18',
    'class-om-ac-22','class-om-ac-23'], 1):
    gc_rows.append(gc_row(n, 'grp-om-12', cid, 'dom_om_action', i)); n+=1

# grp-om-13: 提交与通知
for i, cid in enumerate([
    'class-om-ac-19','class-om-ac-20','class-om-ac-21',
    'class-om-ac-24','class-om-ac-25','class-om-ac-26'], 1):
    gc_rows.append(gc_row(n, 'grp-om-13', cid, 'dom_om_action', i)); n+=1

# grp-om-14: 接口定义
for i, cid in enumerate(['class-om-if-01','class-om-if-02','class-om-if-03'], 1):
    gc_rows.append(gc_row(n, 'grp-om-14', cid, 'dom_om_iface', i)); n+=1

# grp-om-15: 外部数据源
for i, cid in enumerate([
    'class-om-if-04','class-om-if-05','class-om-if-06','class-om-if-07'], 1):
    gc_rows.append(gc_row(n, 'grp-om-15', cid, 'dom_om_iface', i)); n+=1

# grp-om-16: 物理表映射
gc_rows.append(gc_row(n, 'grp-om-16', 'class-om-if-08', 'dom_om_iface', 1)); n+=1

# grp-om-17: 图标库
for i, cid in enumerate(['class-om-sy-01','class-om-sy-02'], 1):
    gc_rows.append(gc_row(n, 'grp-om-17', cid, 'dom_om_system', i)); n+=1

# grp-om-18: 字典管理
for i, cid in enumerate(['class-om-sy-03','class-om-sy-04','class-om-sy-05'], 1):
    gc_rows.append(gc_row(n, 'grp-om-18', cid, 'dom_om_system', i)); n+=1

# grp-om-19: 版本与探索
for i, cid in enumerate(['class-om-sy-06','class-om-sy-07'], 1):
    gc_rows.append(gc_row(n, 'grp-om-19', cid, 'dom_om_system', i)); n+=1

# ============================================================
# BUILD SQL OUTPUT
# ============================================================
lines = []
lines.append('\n-- ============================================================')
lines.append('-- 6. 类属性 ont_class_property')
lines.append('-- ============================================================')
lines.append('INSERT INTO ont_class_property')
lines.append('  (' + ','.join(CP_COLS) + ') VALUES')
lines.append(',\n'.join(cp_rows))
lines.append('ON CONFLICT (id) DO NOTHING;\n')

lines.append('-- ============================================================')
lines.append('-- 7. 链接类型 ont_link_types')
lines.append('-- ============================================================')
lines.append('INSERT INTO ont_link_types')
lines.append('  (' + ','.join(LT_COLS) + ') VALUES')
lines.append(',\n'.join(lt_rows))
lines.append('ON CONFLICT (link_type_id) DO NOTHING;\n')

lines.append('-- ============================================================')
lines.append('-- 8. 分组类绑定 ont_biz_group_class')
lines.append('-- ============================================================')
lines.append('INSERT INTO ont_biz_group_class')
lines.append('  (id,group_id,ref_id,group_type,category_code,g_sort,create_time,update_time) VALUES')
lines.append(',\n'.join(gc_rows))
lines.append('ON CONFLICT (id) DO NOTHING;')

sql_text = '\n'.join(lines) + '\n'

with open(OUT, 'a', encoding='utf-8') as f:
    f.write(sql_text)

print(f'Done. Appended {len(cp_rows)} class-property rows, '
      f'{len(lt_rows)} link-type rows, {len(gc_rows)} group-class rows.')
print(f'Output: {OUT}')

