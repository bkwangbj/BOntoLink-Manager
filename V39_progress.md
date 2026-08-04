# V39 本体管理平台元本体 — 任务进度

## 背景

用户要求：为 BOntoLink 项目本身建一套"元本体"——用项目自己的本体系统来描述系统的全部数据表，放在一个新的**通用行业 / 本体领域**下，方便分组浏览和语义检索。

---

## 目标文件

```
backend/bontolink-data/src/main/resources/db/migration/common/V39__ontomgmt_platform_ontology.sql
```

幂等（`ON CONFLICT DO NOTHING`），可重复执行。

---

## 已完成（425行，全部9张表）

| 表 | 内容 | 数量 |
|---|---|---|
| `ont_biz_namespace` | 4个命名空间：om_root / om_core / om_action / om_sys | 4 |
| `ont_biz_category` | 行业 `ind_ontomgmt` + 6个领域 | 7 |
| `ont_biz_group` | 19个分组（每领域2-4个） | 19 |
| `ont_enum_types` | 10个枚举类型（状态/属性类型/枚举分类/基数/数据类型等） | 10 |
| `ont_enum_items` | 各枚举类型的候选项 | 36 |
| `ont_class` | 73个对象类，完整覆盖所有项目数据表 | 73 |
| `ont_class_property` | 重点类的关键属性（18个类，每类3-8个属性） | 129 |
| `ont_link_types` | 核心跨类关系（类→属性/动作/链接类型，分类→分组，枚举→枚举项等） | 15 |
| `ont_biz_group_class` | 73个对象类全部分配到对应分组 | 73 |

**SQLite 语法验证：PASS（2026-08-04）**

---

## 待完成

~~无~~  **V39 全部完成。**

可选扩展：
- 接口定义 `ont_interface` + 属性（核心类实现 `INameable`、`IStatusable` 等）

---

| 领域 | 数量 | 覆盖的表 |
|---|---|---|
| 业务分类体系 | 5 | ont_biz_namespace/version, ont_biz_category, ont_biz_group, ont_biz_group_class |
| 本体核心建模 | 13 | ont_class, ont_class_property, ont_class_link, ont_class_hierarchy, disjoint_union, expansion, group, ds, ont_link_types, link_mappings, property_format/disjoint/equivalent |
| 配置管理 | 14 | ont_value_types, usage_config, ont_enum_types/items/level_code_rule/sync_config/log, ont_shared_properties, ont_struct_types/items, ont_type_class/bind/cat_dict, ont_dic_type_class |
| 动作与规则 | 26 | ont_class_action/rule, action_rule_condition, link_rule_config, rule_property_mapping, form_param/display/section/global/override_block/item, display_boolean/number/string/object, func_param_mapping/rule_config/exception_map, submit_standard/condition_node, notification_rule, webhook_input/rule, override_condition_group/item, action_execution |
| 接口与数据源 | 8 | ont_interface/property/class, ont_ext_data_source/api_group/api_interface/call_log, ont_physical_table |
| 系统支撑 | 7 | icon_lib_group/icon, ont_dict_def/item, ont_domain_term, ont_ontology_version, ont_explore_design |

---

## 待完成

### 1. 类属性 `ont_class_property`
每个核心类补充5-8个关键属性（API名称、显示名、数据类型、是否必填、是否主键等）。

重点类（属性最多）：
- `OmClass` → api_name, category_code, ns_code, display_name, icon, color, status, is_common
- `OmClassProperty` → api_name, prop_type, data_type, value_type, is_required, is_key, physical_table, physical_column
- `OmLinkType` → link_type_id, l_object_type_id, r_object_type_id, l_cardinality, r_cardinality, status
- `OmClassAction` → api_name, action_type, action_kind, display_name, status, form_enabled
- `OmExtDataSource` → ds_code, ds_name, ds_type, base_url, auth_type, status
- 其余类每类3-5个属性

预计行数：~400行

### 2. 链接类型 `ont_link_types`
核心关系，例如：
- OmClass → OmClassProperty（拥有属性）
- OmClass → OmClassAction（绑定动作）
- OmClass → OmLinkType（关联链接类型）
- OmInterface → OmClass（接口实现）
- OmExtDataSource → OmPhysicalTable（包含物理表）
- OmBizCategory → OmBizGroup（分组归属）
- OmEnumType → OmEnumItem（包含枚举项）
- OmSharedProperty → OmStructType（引用结构类型）

预计：~15条链接类型，30行

### 3. 接口定义 `ont_interface` + 属性
可选：为核心实体类定义接口（如 `INameable`、`IStatusable`、`ICategorized`）。

### 4. 分组-类绑定 `ont_biz_group_class`
将73个类分配到对应分组。约73行。

---

## 辅助工具脚本

- `tools/gen_v39_classes.py` — 已执行，生成73个对象类

---

## 继续工作的指令（新对话）

在新对话中，直接说：

> 继续 V39 任务。文件在 `backend/bontolink-data/src/main/resources/db/migration/common/V39__ontomgmt_platform_ontology.sql`，已有 187 行（命名空间/分类/分组/枚举/73个对象类）。
> 下一步：用 Python 脚本写入类属性（ont_class_property）、链接类型（ont_link_types）、分组类绑定（ont_biz_group_class）。
> 辅助脚本写到 `tools/` 目录，用 `/d/soft/Python/Python311/python` 执行。
> bash heredoc 会因单引号/中文冲突失败，必须用 Python 脚本文件方式写入。

---

## ID前缀约定

| 类型 | 前缀示例 |
|---|---|
| 命名空间 | `ns-om-001` |
| 分类 | `cat-om-ind` / `cat-om-d1` |
| 分组 | `grp-om-01` |
| 枚举类型 | `enum-om-001` |
| 枚举项 | `ei-om-sf-1` |
| 对象类 | `class-om-ns-01` / `class-om-co-01` |
| 类属性 | `cp-om-co01-01`（类前缀+序号） |
| 链接类型 | `lt-om-001` / `link-types-v39-001` |
