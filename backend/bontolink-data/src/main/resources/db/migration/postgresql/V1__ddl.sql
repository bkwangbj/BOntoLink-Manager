-- PostgreSQL DDL — 由 dbnew/tools/export_schemas.py 从真实 PG 库导出
-- 库: swc_wx_dev (schema: bonto_link_manager)

-- Table: icon_lib_group
CREATE TABLE IF NOT EXISTS "icon_lib_group" (
    "id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64),
    "name" VARCHAR(128) NOT NULL,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "source" TEXT,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: icon_lib_icon
CREATE TABLE IF NOT EXISTS "icon_lib_icon" (
    "id" VARCHAR(64) NOT NULL,
    "group_id" VARCHAR(64) NOT NULL,
    "name" VARCHAR(128) NOT NULL,
    "view_box" VARCHAR(255) NOT NULL DEFAULT '0 0 1024 1024'::character varying,
    "content" TEXT NOT NULL,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_action_execution
CREATE TABLE IF NOT EXISTS "ont_action_execution" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "action_api_name" VARCHAR(128),
    "object_class_id" VARCHAR(64),
    "op_type" SMALLINT,
    "input_params" TEXT,
    "resolved_result" TEXT,
    "status" VARCHAR(32) NOT NULL DEFAULT 'success'::character varying,
    "message" TEXT,
    "dry_run" SMALLINT NOT NULL DEFAULT 1,
    "executed_by" VARCHAR(64),
    "execute_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_display_boolean
CREATE TABLE IF NOT EXISTS "ont_action_form_display_boolean" (
    "id" VARCHAR(64) NOT NULL,
    "display_id" VARCHAR(64) NOT NULL,
    "true_label" VARCHAR(64),
    "false_label" VARCHAR(64),
    "default_value" SMALLINT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_display_number
CREATE TABLE IF NOT EXISTS "ont_action_form_display_number" (
    "id" VARCHAR(64) NOT NULL,
    "display_id" VARCHAR(64) NOT NULL,
    "min_value" DOUBLE PRECISION,
    "max_value" DOUBLE PRECISION,
    "step" DOUBLE PRECISION,
    "precision" INTEGER,
    "unit" VARCHAR(64),
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_display_object
CREATE TABLE IF NOT EXISTS "ont_action_form_display_object" (
    "id" VARCHAR(64) NOT NULL,
    "display_id" VARCHAR(64) NOT NULL,
    "object_class_id" VARCHAR(64),
    "picker_type" VARCHAR(64),
    "filter_config" TEXT,
    "allow_create" SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_display_string
CREATE TABLE IF NOT EXISTS "ont_action_form_display_string" (
    "id" VARCHAR(64) NOT NULL,
    "display_id" VARCHAR(64) NOT NULL,
    "input_type" VARCHAR(32),
    "max_length" INTEGER,
    "regex" TEXT,
    "options" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_global_config
CREATE TABLE IF NOT EXISTS "ont_action_form_global_config" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "layout" VARCHAR(32),
    "label_position" VARCHAR(16),
    "label_width" INTEGER,
    "submit_text" VARCHAR(64),
    "cancel_text" VARCHAR(64),
    "theme" VARCHAR(64),
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_override_block
CREATE TABLE IF NOT EXISTS "ont_action_form_override_block" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "block_name" VARCHAR(255),
    "target_param_code" VARCHAR(128),
    "sort" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_override_item
CREATE TABLE IF NOT EXISTS "ont_action_form_override_item" (
    "id" VARCHAR(64) NOT NULL,
    "block_id" VARCHAR(64) NOT NULL,
    "override_type" VARCHAR(32),
    "target_property" VARCHAR(128),
    "override_value" TEXT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_param
CREATE TABLE IF NOT EXISTS "ont_action_form_param" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "section_id" VARCHAR(64),
    "param_code" VARCHAR(128) NOT NULL,
    "param_name" VARCHAR(255),
    "param_type" VARCHAR(32),
    "data_type" VARCHAR(64),
    "is_required" SMALLINT NOT NULL DEFAULT 0,
    "is_multi" SMALLINT NOT NULL DEFAULT 0,
    "default_value" TEXT,
    "placeholder" VARCHAR(255),
    "sort" INTEGER NOT NULL DEFAULT 0,
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_param_display
CREATE TABLE IF NOT EXISTS "ont_action_form_param_display" (
    "id" VARCHAR(64) NOT NULL,
    "param_id" VARCHAR(64) NOT NULL,
    "display_type" VARCHAR(64),
    "label" VARCHAR(255),
    "help_text" TEXT,
    "visible" SMALLINT NOT NULL DEFAULT 1,
    "editable" SMALLINT NOT NULL DEFAULT 1,
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_form_section
CREATE TABLE IF NOT EXISTS "ont_action_form_section" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "section_name" VARCHAR(255),
    "title" VARCHAR(255),
    "description" TEXT,
    "collapsed" SMALLINT NOT NULL DEFAULT 0,
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_function_exception_map
CREATE TABLE IF NOT EXISTS "ont_action_function_exception_map" (
    "id" VARCHAR(64) NOT NULL,
    "function_config_id" VARCHAR(64) NOT NULL,
    "exception_code" VARCHAR(64),
    "exception_message" TEXT,
    "handle_strategy" VARCHAR(32),
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_function_param_mapping
CREATE TABLE IF NOT EXISTS "ont_action_function_param_mapping" (
    "id" VARCHAR(64) NOT NULL,
    "function_config_id" VARCHAR(64) NOT NULL,
    "param_code" VARCHAR(128),
    "param_name" VARCHAR(255),
    "value_source" SMALLINT,
    "value_content" TEXT,
    "direction" VARCHAR(16),
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_function_rule_config
CREATE TABLE IF NOT EXISTS "ont_action_function_rule_config" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "rule_id" VARCHAR(64),
    "function_code" VARCHAR(128),
    "function_name" VARCHAR(255),
    "version" VARCHAR(64),
    "runtime" VARCHAR(64),
    "entry" VARCHAR(255),
    "timeout_ms" INTEGER,
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_link_prop_mapping
CREATE TABLE IF NOT EXISTS "ont_action_link_prop_mapping" (
    "id" VARCHAR(64) NOT NULL,
    "link_rule_id" VARCHAR(64) NOT NULL,
    "property_code" VARCHAR(128),
    "property_name" VARCHAR(255),
    "value_source" SMALLINT,
    "value_content" TEXT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_link_rule_config
CREATE TABLE IF NOT EXISTS "ont_action_link_rule_config" (
    "id" VARCHAR(64) NOT NULL,
    "rule_id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64),
    "link_type_id" VARCHAR(64),
    "link_type_code" VARCHAR(128),
    "operation" VARCHAR(32),
    "source_param_code" VARCHAR(128),
    "target_param_code" VARCHAR(128),
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_notification_rule_config
CREATE TABLE IF NOT EXISTS "ont_action_notification_rule_config" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "rule_id" VARCHAR(64),
    "channel" VARCHAR(32),
    "template_code" VARCHAR(128),
    "title" VARCHAR(255),
    "content" TEXT,
    "recipients" TEXT,
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_override_condition_group
CREATE TABLE IF NOT EXISTS "ont_action_override_condition_group" (
    "id" VARCHAR(64) NOT NULL,
    "block_id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64),
    "logic_op" VARCHAR(16),
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_override_condition_item
CREATE TABLE IF NOT EXISTS "ont_action_override_condition_item" (
    "id" VARCHAR(64) NOT NULL,
    "group_id" VARCHAR(64) NOT NULL,
    "left_code" VARCHAR(128),
    "operator" VARCHAR(32),
    "right_value" TEXT,
    "value_source" SMALLINT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_rule_condition
CREATE TABLE IF NOT EXISTS "ont_action_rule_condition" (
    "id" VARCHAR(64) NOT NULL,
    "rule_id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64),
    "node_type" VARCHAR(32),
    "logic_op" VARCHAR(16),
    "left_source" SMALLINT,
    "left_code" VARCHAR(128),
    "operator" VARCHAR(32),
    "right_source" SMALLINT,
    "right_value" TEXT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_rule_property_mapping
CREATE TABLE IF NOT EXISTS "ont_action_rule_property_mapping" (
    "id" VARCHAR(64) NOT NULL,
    "rule_id" VARCHAR(64) NOT NULL,
    "property_code" VARCHAR(128),
    "property_name" VARCHAR(255),
    "prop_operator" VARCHAR(32),
    "value_source" SMALLINT,
    "value_content" TEXT,
    "is_primary_key" SMALLINT NOT NULL DEFAULT 0,
    "is_required" SMALLINT NOT NULL DEFAULT 0,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "param_name" TEXT,
    "default_type" TEXT,
    "default_source" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_submit_condition_node
CREATE TABLE IF NOT EXISTS "ont_action_submit_condition_node" (
    "id" VARCHAR(64) NOT NULL,
    "standard_id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64),
    "node_type" VARCHAR(32),
    "logic_op" VARCHAR(16),
    "left_code" VARCHAR(128),
    "operator" VARCHAR(32),
    "right_value" TEXT,
    "value_source" SMALLINT,
    "error_message" TEXT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_submit_standard_config
CREATE TABLE IF NOT EXISTS "ont_action_submit_standard_config" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "enabled" SMALLINT NOT NULL DEFAULT 0,
    "validate_mode" VARCHAR(16),
    "error_message" TEXT,
    "config" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_action_webhook_input_mapping
CREATE TABLE IF NOT EXISTS "ont_action_webhook_input_mapping" (
    "id" VARCHAR(64) NOT NULL,
    "webhook_config_id" VARCHAR(64) NOT NULL,
    "param_code" VARCHAR(128),
    "value_source" SMALLINT,
    "value_content" TEXT,
    "position" VARCHAR(16),
    "sort" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_action_webhook_rule_config
CREATE TABLE IF NOT EXISTS "ont_action_webhook_rule_config" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "rule_id" VARCHAR(64),
    "url" TEXT,
    "method" VARCHAR(16),
    "headers" TEXT,
    "body_template" TEXT,
    "auth_type" VARCHAR(32),
    "auth_config" TEXT,
    "timeout_ms" INTEGER,
    "retry_count" INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_biz_category
CREATE TABLE IF NOT EXISTS "ont_biz_category" (
    "id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64) NOT NULL DEFAULT '0'::character varying,
    "rid" TEXT,
    "category_code" VARCHAR(64) NOT NULL,
    "category_type" INTEGER NOT NULL DEFAULT 1,
    "ns_code" VARCHAR(64),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "icon" VARCHAR(255),
    "color" VARCHAR(255),
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "description" VARCHAR(255),
    "metadata" TEXT,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_biz_group
CREATE TABLE IF NOT EXISTS "ont_biz_group" (
    "id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64),
    "category_code" VARCHAR(64),
    "g_name" VARCHAR(128) NOT NULL,
    "g_sort" INTEGER NOT NULL DEFAULT 0,
    "icon" VARCHAR(255),
    "color" VARCHAR(255),
    "description" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "domain_code" VARCHAR(64),
    PRIMARY KEY ("id")
);

-- Table: ont_biz_group_class
CREATE TABLE IF NOT EXISTS "ont_biz_group_class" (
    "id" VARCHAR(64) NOT NULL,
    "group_id" VARCHAR(64) NOT NULL,
    "ref_id" VARCHAR(64) NOT NULL,
    "group_type" VARCHAR(64) NOT NULL DEFAULT 'object_types'::character varying,
    "category_code" VARCHAR(64),
    "g_sort" INTEGER NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_biz_namespace
CREATE TABLE IF NOT EXISTS "ont_biz_namespace" (
    "id" VARCHAR(64) NOT NULL,
    "ns_code" VARCHAR(64) NOT NULL,
    "ns_name" VARCHAR(128) NOT NULL,
    "ns_uri" VARCHAR(255),
    "hierarchy_path" TEXT,
    "curr_version" VARCHAR(255) NOT NULL DEFAULT '1.0'::character varying,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "metadata" TEXT,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_biz_namespace_version
CREATE TABLE IF NOT EXISTS "ont_biz_namespace_version" (
    "id" VARCHAR(64) NOT NULL,
    "ns_code" VARCHAR(64) NOT NULL,
    "version" VARCHAR(20) NOT NULL,
    "uri" VARCHAR(255) NOT NULL,
    "snapshot_data" TEXT,
    "owl_content" TEXT,
    "publish_time" TIMESTAMP,
    "is_current" SMALLINT NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class
CREATE TABLE IF NOT EXISTS "ont_class" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "api_name" VARCHAR(128) NOT NULL,
    "ns_code" VARCHAR(64),
    "category_code" VARCHAR(64),
    "display_name" VARCHAR(128),
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "description" VARCHAR(255),
    "icon" VARCHAR(255),
    "color" VARCHAR(255),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "metadata" TEXT,
    "parent_class_id" VARCHAR(64),
    "class_expr_type" VARCHAR(64),
    "class_expr_content" TEXT,
    "is_thing" SMALLINT NOT NULL DEFAULT 0,
    "is_nothing" SMALLINT NOT NULL DEFAULT 0,
    "is_common" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_action
CREATE TABLE IF NOT EXISTS "ont_class_action" (
    "id" VARCHAR(64) NOT NULL,
    "rid" VARCHAR(255),
    "api_name" VARCHAR(128) NOT NULL,
    "m_type" SMALLINT,
    "action_type" SMALLINT,
    "object_class_id" VARCHAR(64),
    "link_type_id" VARCHAR(64),
    "function_code" VARCHAR(128),
    "category_code" VARCHAR(128),
    "show_on_detail" SMALLINT NOT NULL DEFAULT 0,
    "show_on_batch" SMALLINT NOT NULL DEFAULT 0,
    "button_text" VARCHAR(128),
    "compile_status" SMALLINT NOT NULL DEFAULT 0,
    "save_path" VARCHAR(255),
    "form_enabled" SMALLINT NOT NULL DEFAULT 0,
    "submit_criteria_enabled" SMALLINT NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 0,
    "current_version" VARCHAR(64),
    "is_deleted" SMALLINT NOT NULL DEFAULT 0,
    "icon" VARCHAR(64),
    "color" VARCHAR(32),
    "metadata" TEXT,
    "rdfs_label" VARCHAR(255) NOT NULL,
    "rdfs_comment" TEXT,
    "rdfs_see_also" TEXT,
    "rdfs_defined_by" TEXT,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_action_rule
CREATE TABLE IF NOT EXISTS "ont_class_action_rule" (
    "id" VARCHAR(64) NOT NULL,
    "action_id" VARCHAR(64) NOT NULL,
    "action_type" SMALLINT,
    "rule_type" SMALLINT,
    "rule_name" VARCHAR(255),
    "target_param_code" VARCHAR(128),
    "link_type_code" VARCHAR(128),
    "sort" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rule_config" TEXT,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_disjoint_union
CREATE TABLE IF NOT EXISTS "ont_class_disjoint_union" (
    "id" VARCHAR(64) NOT NULL,
    "parent_class_id" VARCHAR(64) NOT NULL,
    "sub_class_id" VARCHAR(64) NOT NULL,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_ds
CREATE TABLE IF NOT EXISTS "ont_class_ds" (
    "id" VARCHAR(64) NOT NULL,
    "class_id" VARCHAR(64) NOT NULL,
    "ds_code" VARCHAR(64),
    "physical_table" VARCHAR(128),
    "table_label" TEXT,
    "rel_type" VARCHAR(64) NOT NULL DEFAULT '1'::character varying,
    "alias" TEXT,
    "pk_keys" TEXT,
    "join_on_keys" TEXT,
    "join_type" VARCHAR(64) DEFAULT 'LEFT'::character varying,
    "physical_fields" TEXT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_expansion
CREATE TABLE IF NOT EXISTS "ont_class_expansion" (
    "class_id" VARCHAR(64) NOT NULL,
    "original_text" TEXT NOT NULL,
    "expanded_text" TEXT NOT NULL,
    "expansion_detail" TEXT,
    "embedding_vector" BYTEA,
    "token_count" INTEGER,
    "expansion_version" INTEGER DEFAULT 1,
    "last_update" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("class_id")
);

-- Table: ont_class_group
CREATE TABLE IF NOT EXISTS "ont_class_group" (
    "id" VARCHAR(64) NOT NULL,
    "class_id" VARCHAR(64) NOT NULL,
    "ref_class_id" VARCHAR(64) NOT NULL,
    "group_type" VARCHAR(64) NOT NULL,
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_hierarchy
CREATE TABLE IF NOT EXISTS "ont_class_hierarchy" (
    "id" VARCHAR(64) NOT NULL,
    "child_class_id" VARCHAR(64) NOT NULL,
    "parent_class_id" VARCHAR(64) NOT NULL,
    "hierarchy_level" INTEGER,
    "relationship_type" VARCHAR(50) DEFAULT 'IS_A'::character varying,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_link
CREATE TABLE IF NOT EXISTS "ont_class_link" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "api_name" VARCHAR(128) NOT NULL,
    "source_class_id" VARCHAR(64) NOT NULL,
    "target_class_id" VARCHAR(64) NOT NULL,
    "cardinality" TEXT DEFAULT 'many_to_many'::text,
    "display_name" VARCHAR(128),
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_class_property
CREATE TABLE IF NOT EXISTS "ont_class_property" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "class_id" VARCHAR(64) NOT NULL,
    "category_code" VARCHAR(64),
    "api_name" VARCHAR(128) NOT NULL,
    "prop_code" VARCHAR(64),
    "prop_type" VARCHAR(64) DEFAULT 'data'::character varying,
    "data_type" VARCHAR(64),
    "value_type" VARCHAR(64),
    "display_name" VARCHAR(128),
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "class_ds_id" VARCHAR(64),
    "physical_table" VARCHAR(128),
    "physical_column" TEXT,
    "is_primary" SMALLINT NOT NULL DEFAULT 0,
    "is_required" SMALLINT NOT NULL DEFAULT 0,
    "is_key" SMALLINT NOT NULL DEFAULT 0,
    "is_derived" SMALLINT NOT NULL DEFAULT 0,
    "is_multi_valued_prop" SMALLINT NOT NULL DEFAULT 0,
    "is_range_constraint_prop" SMALLINT NOT NULL DEFAULT 0,
    "range_class_id" VARCHAR(64),
    "sub_property_of" TEXT,
    "xsd_min_length" TEXT,
    "xsd_max_length" TEXT,
    "xsd_length" TEXT,
    "xsd_pattern" TEXT,
    "xsd_min_inclusive" TEXT,
    "xsd_max_inclusive" TEXT,
    "owl_functional" SMALLINT NOT NULL DEFAULT 0,
    "owl_inverse_functional" SMALLINT NOT NULL DEFAULT 0,
    "owl_transitive" SMALLINT NOT NULL DEFAULT 0,
    "owl_symmetric" SMALLINT NOT NULL DEFAULT 0,
    "owl_asymmetric" SMALLINT NOT NULL DEFAULT 0,
    "owl_reflexive" SMALLINT NOT NULL DEFAULT 0,
    "owl_irreflexive" SMALLINT NOT NULL DEFAULT 0,
    "metadata" TEXT,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_dic_type_class
CREATE TABLE IF NOT EXISTS "ont_dic_type_class" (
    "id" VARCHAR(64) NOT NULL,
    "enum_name" VARCHAR(64) NOT NULL,
    "code" VARCHAR(64) NOT NULL,
    "name" VARCHAR(128) NOT NULL,
    "sort_no" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_dict_def
CREATE TABLE IF NOT EXISTS "ont_dict_def" (
    "id" VARCHAR(64) NOT NULL,
    "dict_code" VARCHAR(64) NOT NULL,
    "dict_name" VARCHAR(255) NOT NULL,
    "rdfs_comment" VARCHAR(255),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "sort_no" INTEGER NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_dict_item
CREATE TABLE IF NOT EXISTS "ont_dict_item" (
    "id" VARCHAR(64) NOT NULL,
    "dict_id" VARCHAR(64) NOT NULL,
    "parent_id" VARCHAR(64),
    "item_code" VARCHAR(64) NOT NULL,
    "item_value" VARCHAR(255) NOT NULL,
    "sort_no" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "color" VARCHAR(255),
    "ext_data" TEXT,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_domain_term
CREATE TABLE IF NOT EXISTS "ont_domain_term" (
    "id" VARCHAR(64) NOT NULL,
    "standard_term" VARCHAR(255) NOT NULL,
    "common_terms" TEXT NOT NULL,
    "domain" VARCHAR(50) NOT NULL,
    "term_type" VARCHAR(50),
    "similarity" NUMERIC DEFAULT 0.9,
    "context" TEXT,
    "usage_count" INTEGER DEFAULT 0,
    "source" VARCHAR(50) DEFAULT 'MANUAL'::character varying,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_enum_items
CREATE TABLE IF NOT EXISTS "ont_enum_items" (
    "id" VARCHAR(64) NOT NULL,
    "enum_id" VARCHAR(64) NOT NULL,
    "code" VARCHAR(64) NOT NULL,
    "api_name" VARCHAR(128),
    "label" VARCHAR(255) NOT NULL,
    "parent_code" VARCHAR(64),
    "level" INTEGER NOT NULL DEFAULT 1,
    "sort_num" INTEGER NOT NULL DEFAULT 0,
    "status" TEXT NOT NULL DEFAULT 'active'::text,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "is_sync_locked" SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY ("id")
);

-- Table: ont_enum_level_code_rule
CREATE TABLE IF NOT EXISTS "ont_enum_level_code_rule" (
    "id" VARCHAR(64) NOT NULL,
    "enum_id" VARCHAR(64) NOT NULL,
    "code_name" TEXT NOT NULL,
    "rule_level" INTEGER NOT NULL,
    "code_separator" TEXT,
    "code_len" INTEGER NOT NULL,
    "total_len" INTEGER NOT NULL,
    "fill_char" SMALLINT DEFAULT 0,
    "fill_pos" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_enum_sync_config
CREATE TABLE IF NOT EXISTS "ont_enum_sync_config" (
    "id" VARCHAR(64) NOT NULL,
    "enum_id" VARCHAR(64) NOT NULL,
    "data_source_id" VARCHAR(64),
    "table_alias" TEXT,
    "table_name" TEXT,
    "field_code" VARCHAR(64),
    "field_name" TEXT,
    "field_sort" TEXT,
    "field_status" TEXT,
    "filter_sql" TEXT,
    "sync_mode" TEXT DEFAULT 'level_diff'::text,
    "sync_strategy" TEXT DEFAULT 'once'::text,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "field_parent" TEXT,
    "sync_source_type" VARCHAR(64) NOT NULL DEFAULT 'table'::character varying,
    "custom_sql" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_enum_sync_log
CREATE TABLE IF NOT EXISTS "ont_enum_sync_log" (
    "id" VARCHAR(64) NOT NULL,
    "enum_id" VARCHAR(64) NOT NULL,
    "sync_type" VARCHAR(64) NOT NULL DEFAULT 'manual'::character varying,
    "add_count" INTEGER NOT NULL DEFAULT 0,
    "update_count" INTEGER NOT NULL DEFAULT 0,
    "del_count" INTEGER NOT NULL DEFAULT 0,
    "fail_count" INTEGER NOT NULL DEFAULT 0,
    "sync_status" VARCHAR(64) NOT NULL DEFAULT 'running'::character varying,
    "error_msg" TEXT,
    "oper_user" TEXT,
    "sync_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_enum_types
CREATE TABLE IF NOT EXISTS "ont_enum_types" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "api_name" VARCHAR(128) NOT NULL,
    "category_code" VARCHAR(64),
    "enum_type" VARCHAR(64) NOT NULL DEFAULT 'general_single'::character varying,
    "max_level" INTEGER NOT NULL DEFAULT 1,
    "top_code" VARCHAR(64),
    "status" TEXT NOT NULL DEFAULT 'active'::text,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_explore_design
CREATE TABLE IF NOT EXISTS "ont_explore_design" (
    "id" VARCHAR(64) NOT NULL,
    "class_id" VARCHAR(64) NOT NULL,
    "name" VARCHAR(128) NOT NULL,
    "kind" TEXT NOT NULL DEFAULT 'query'::text,
    "config" TEXT,
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_ext_api_call_log
CREATE TABLE IF NOT EXISTS "ont_ext_api_call_log" (
    "id" VARCHAR(47) NOT NULL,
    "trace_id" VARCHAR(64),
    "ds_id" VARCHAR(47) NOT NULL,
    "interface_id" VARCHAR(47),
    "call_type" VARCHAR(32),
    "caller" VARCHAR(128),
    "full_url" VARCHAR(1024),
    "request_header" TEXT,
    "request_body" TEXT,
    "call_status" INTEGER,
    "http_status" INTEGER,
    "cost_time" INTEGER,
    "response_size" INTEGER,
    "response_body" TEXT,
    "error_msg" VARCHAR(512),
    "call_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_ext_api_group
CREATE TABLE IF NOT EXISTS "ont_ext_api_group" (
    "id" VARCHAR(47) NOT NULL,
    "ds_id" VARCHAR(47) NOT NULL,
    "group_name" VARCHAR(128) NOT NULL,
    "parent_id" VARCHAR(47) DEFAULT '0'::character varying,
    "sort" INTEGER DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_ext_api_interface
CREATE TABLE IF NOT EXISTS "ont_ext_api_interface" (
    "id" VARCHAR(47) NOT NULL,
    "ds_id" VARCHAR(47) NOT NULL,
    "group_id" VARCHAR(47) DEFAULT '0'::character varying,
    "api_code" VARCHAR(64) NOT NULL,
    "api_name" VARCHAR(128) NOT NULL,
    "method" VARCHAR(16) DEFAULT 'POST'::character varying,
    "api_path" VARCHAR(256),
    "api_status" VARCHAR(16) DEFAULT 'debug'::character varying,
    "read_write_type" INTEGER DEFAULT 1,
    "description" VARCHAR(512),
    "request_params" TEXT,
    "response_params" TEXT,
    "override_auth" INTEGER DEFAULT 0,
    "auth_type" VARCHAR(32),
    "auth_config" TEXT,
    "header_inherit" INTEGER DEFAULT 1,
    "content_type" VARCHAR(64),
    "timeout" INTEGER,
    "status" INTEGER NOT NULL DEFAULT 1,
    "sort" INTEGER DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_ext_data_source
CREATE TABLE IF NOT EXISTS "ont_ext_data_source" (
    "id" VARCHAR(47) NOT NULL,
    "category_code" VARCHAR(64),
    "ds_code" VARCHAR(64) NOT NULL,
    "ds_name" VARCHAR(128) NOT NULL,
    "ds_type" VARCHAR(32) NOT NULL DEFAULT 'http_rest'::character varying,
    "read_write_type" INTEGER NOT NULL DEFAULT 1,
    "base_url" VARCHAR(512),
    "default_method" VARCHAR(16) DEFAULT 'POST'::character varying,
    "content_type" VARCHAR(64) DEFAULT 'application/json'::character varying,
    "connect_timeout" INTEGER DEFAULT 5000,
    "read_timeout" INTEGER DEFAULT 10000,
    "retry_count" INTEGER DEFAULT 1,
    "retry_interval" INTEGER DEFAULT 1000,
    "ssl_verify" INTEGER DEFAULT 1,
    "log_enable" INTEGER DEFAULT 1,
    "header_enable" INTEGER DEFAULT 0,
    "global_header" TEXT,
    "auth_type" VARCHAR(32) DEFAULT 'none'::character varying,
    "auth_config" TEXT,
    "status" INTEGER NOT NULL DEFAULT 1,
    "remark" VARCHAR(512),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_function
CREATE TABLE IF NOT EXISTS "ont_function" (
    "id" VARCHAR(64) NOT NULL,
    "rid" VARCHAR(128) NOT NULL,
    "version_no" VARCHAR(32) NOT NULL,
    "api_name" VARCHAR(128) NOT NULL,
    "function_label" VARCHAR(128) NOT NULL,
    "function_type" SMALLINT NOT NULL DEFAULT 1,
    "language" SMALLINT NOT NULL DEFAULT 2,
    "industry_dir" VARCHAR(64) NOT NULL,
    "category_dir" VARCHAR(128) NOT NULL,
    "class_name" VARCHAR(128),
    "full_access_path" VARCHAR(512) NOT NULL,
    "code_file_path" VARCHAR(256) NOT NULL,
    "code_md5" VARCHAR(32),
    "code_content" TEXT,
    "file_line_start" INTEGER,
    "file_line_end" INTEGER,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "visibility" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_label" VARCHAR(128),
    "rdfs_comment" TEXT,
    "rdfs_see_also" VARCHAR(256),
    "rdfs_defined_by" VARCHAR(256),
    "create_user" VARCHAR(64),
    "publish_time" TIMESTAMP,
    "is_deleted" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_function_call_stat
CREATE TABLE IF NOT EXISTS "ont_function_call_stat" (
    "id" VARCHAR(64) NOT NULL,
    "function_id" VARCHAR(64) NOT NULL,
    "stat_date" VARCHAR(10) NOT NULL,
    "caller_app" VARCHAR(128),
    "call_count" INTEGER NOT NULL DEFAULT 0,
    "success_count" INTEGER NOT NULL DEFAULT 0,
    "error_count" INTEGER NOT NULL DEFAULT 0,
    "avg_cost_ms" INTEGER NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_function_env_var
CREATE TABLE IF NOT EXISTS "ont_function_env_var" (
    "id" VARCHAR(64) NOT NULL,
    "function_id" VARCHAR(64) NOT NULL,
    "var_name" VARCHAR(128) NOT NULL,
    "var_value" VARCHAR(512) NOT NULL DEFAULT ''::character varying,
    "var_type" SMALLINT NOT NULL DEFAULT 1,
    "value_range" VARCHAR(512),
    "var_desc" VARCHAR(256),
    "is_encrypt" SMALLINT NOT NULL DEFAULT 0,
    "sort_num" INTEGER NOT NULL DEFAULT 0,
    "is_deleted" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_function_param
CREATE TABLE IF NOT EXISTS "ont_function_param" (
    "id" VARCHAR(64) NOT NULL,
    "function_id" VARCHAR(64) NOT NULL,
    "param_name" VARCHAR(128) NOT NULL,
    "param_type" VARCHAR(128) NOT NULL,
    "param_direction" SMALLINT NOT NULL DEFAULT 1,
    "is_required" SMALLINT NOT NULL DEFAULT 1,
    "default_value" VARCHAR(256),
    "value_range" VARCHAR(512),
    "param_desc" VARCHAR(512),
    "object_class_id" VARCHAR(64),
    "sort_num" INTEGER NOT NULL DEFAULT 0,
    "is_deleted" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_function_runtime_config
CREATE TABLE IF NOT EXISTS "ont_function_runtime_config" (
    "id" VARCHAR(64) NOT NULL,
    "function_id" VARCHAR(64) NOT NULL,
    "timeout" INTEGER NOT NULL DEFAULT 30,
    "retry_count" INTEGER NOT NULL DEFAULT 2,
    "retry_interval" INTEGER NOT NULL DEFAULT 1,
    "memory_quota" INTEGER NOT NULL DEFAULT 512,
    "concurrency_limit" INTEGER NOT NULL DEFAULT 100,
    "enable_cache" SMALLINT NOT NULL DEFAULT 1,
    "cache_ttl" INTEGER NOT NULL DEFAULT 3600,
    "is_deleted" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_interface
CREATE TABLE IF NOT EXISTS "ont_interface" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "api_name" VARCHAR(128) NOT NULL,
    "interface_code" VARCHAR(64),
    "ns_code" VARCHAR(64),
    "category_code" VARCHAR(64),
    "display_name" VARCHAR(128),
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "description" VARCHAR(255),
    "icon" VARCHAR(255),
    "color" VARCHAR(255),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "metadata" TEXT,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_interface_class
CREATE TABLE IF NOT EXISTS "ont_interface_class" (
    "id" VARCHAR(64) NOT NULL,
    "interface_id" VARCHAR(64) NOT NULL,
    "class_id" VARCHAR(64) NOT NULL,
    "category_code" VARCHAR(64),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_interface_property
CREATE TABLE IF NOT EXISTS "ont_interface_property" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "interface_id" VARCHAR(64) NOT NULL,
    "api_name" VARCHAR(128) NOT NULL,
    "prop_code" VARCHAR(64),
    "data_type" VARCHAR(64),
    "value_type" VARCHAR(64),
    "category_code" VARCHAR(64),
    "display_name" VARCHAR(128),
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "is_required" SMALLINT NOT NULL DEFAULT 0,
    "metadata" TEXT,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_link_mappings
CREATE TABLE IF NOT EXISTS "ont_link_mappings" (
    "mapping_id" VARCHAR(64) NOT NULL,
    "link_id" VARCHAR(64) NOT NULL,
    "side" VARCHAR(64) NOT NULL,
    "seq" INTEGER NOT NULL DEFAULT 1,
    "object_field" VARCHAR(128) NOT NULL,
    "join_table_column" VARCHAR(128),
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("mapping_id")
);

-- Table: ont_link_types
CREATE TABLE IF NOT EXISTS "ont_link_types" (
    "id" VARCHAR(64) NOT NULL,
    "link_type_id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "status" TEXT NOT NULL DEFAULT 'experimental'::text,
    "l_object_type_id" VARCHAR(64) NOT NULL,
    "r_object_type_id" VARCHAR(64) NOT NULL,
    "l_cardinality" VARCHAR(64) NOT NULL DEFAULT 'one'::character varying,
    "r_cardinality" VARCHAR(64) NOT NULL DEFAULT 'one'::character varying,
    "l_display_name" VARCHAR(128),
    "l_plural_name" VARCHAR(128),
    "r_display_name" VARCHAR(128),
    "r_plural_name" VARCHAR(128),
    "l_visibility" VARCHAR(64) NOT NULL DEFAULT 'normal'::character varying,
    "r_visibility" VARCHAR(64) NOT NULL DEFAULT 'normal'::character varying,
    "l_api_name" VARCHAR(128),
    "r_api_name" VARCHAR(128),
    "l_enabled" SMALLINT NOT NULL DEFAULT 1,
    "r_enabled" SMALLINT NOT NULL DEFAULT 1,
    "is_data_source_rel" SMALLINT NOT NULL DEFAULT 0,
    "rel_data_table" TEXT,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "category_code" VARCHAR(64),
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "created_by" VARCHAR(255),
    "updated_by" VARCHAR(255),
    PRIMARY KEY ("id")
);

-- Table: ont_ontology_version
CREATE TABLE IF NOT EXISTS "ont_ontology_version" (
    "id" VARCHAR(64) NOT NULL,
    "version" INTEGER NOT NULL DEFAULT 0,
    "updated_by" VARCHAR(255),
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_physical_table
CREATE TABLE IF NOT EXISTS "ont_physical_table" (
    "id" VARCHAR(64) NOT NULL,
    "ds_id" VARCHAR(64) NOT NULL,
    "physical_table" VARCHAR(128) NOT NULL,
    "display_name" VARCHAR(128),
    "table_type" VARCHAR(64) NOT NULL DEFAULT 'table'::character varying,
    "columns_json" TEXT,
    "column_count" INTEGER NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "sync_time" TIMESTAMP,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_property_disjoint
CREATE TABLE IF NOT EXISTS "ont_property_disjoint" (
    "id" VARCHAR(64) NOT NULL,
    "class_id1" TEXT NOT NULL,
    "prop_id1" TEXT NOT NULL,
    "class_id2" TEXT NOT NULL,
    "prop_id2" TEXT NOT NULL,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_comment" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_property_equivalent
CREATE TABLE IF NOT EXISTS "ont_property_equivalent" (
    "id" VARCHAR(64) NOT NULL,
    "class_id1" TEXT NOT NULL,
    "prop_id1" TEXT NOT NULL,
    "class_id2" TEXT NOT NULL,
    "prop_id2" TEXT NOT NULL,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_comment" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_property_format
CREATE TABLE IF NOT EXISTS "ont_property_format" (
    "format_id" VARCHAR(64) NOT NULL,
    "src_type" VARCHAR(64) NOT NULL DEFAULT '1'::character varying,
    "property_id" VARCHAR(64) NOT NULL,
    "property_scope" VARCHAR(64) NOT NULL DEFAULT 'class'::character varying,
    "format_enabled" SMALLINT NOT NULL DEFAULT 0,
    "format_type" VARCHAR(64) NOT NULL DEFAULT 'general'::character varying,
    "decimal_places" TEXT DEFAULT '2'::text,
    "use_thousand_sep" SMALLINT DEFAULT 0,
    "negative_mode" TEXT DEFAULT '3'::text,
    "currency_symbol" TEXT DEFAULT '¥'::text,
    "accounting_align" SMALLINT DEFAULT 1,
    "date_pattern" TEXT DEFAULT 'yyyy-MM-dd'::text,
    "time_pattern" TEXT DEFAULT 'HH:mm:ss'::text,
    "locale" TEXT DEFAULT 'zh-CN'::text,
    "fraction_type" VARCHAR(64) DEFAULT '# ?/?'::character varying,
    "special_type" VARCHAR(64) DEFAULT 'zipcode'::character varying,
    "custom_format" TEXT DEFAULT 'G/通用格式'::text,
    "text_force" SMALLINT DEFAULT 0,
    "text_max_length" TEXT,
    "text_regex" TEXT,
    "percent_auto_multiply" SMALLINT DEFAULT 1,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "create_user" VARCHAR(255),
    PRIMARY KEY ("format_id")
);

-- Table: ont_shared_properties
CREATE TABLE IF NOT EXISTS "ont_shared_properties" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT,
    "category_code" VARCHAR(64),
    "prop_code" VARCHAR(64) NOT NULL,
    "prop_type" VARCHAR(64) NOT NULL DEFAULT 'data'::character varying,
    "is_key" SMALLINT NOT NULL DEFAULT 0,
    "data_type" VARCHAR(64),
    "value_type" VARCHAR(64),
    "is_required" SMALLINT NOT NULL DEFAULT 0,
    "is_multi_valued_prop" SMALLINT NOT NULL DEFAULT 0,
    "is_range_constraint_prop" SMALLINT NOT NULL DEFAULT 0,
    "xsd_min_length" TEXT,
    "xsd_max_length" TEXT,
    "xsd_length" TEXT,
    "xsd_pattern" TEXT,
    "xsd_min_inclusive" TEXT,
    "xsd_max_inclusive" TEXT,
    "owl_functional" SMALLINT NOT NULL DEFAULT 0,
    "owl_inverse_functional" SMALLINT NOT NULL DEFAULT 0,
    "owl_transitive" SMALLINT NOT NULL DEFAULT 0,
    "owl_symmetric" SMALLINT NOT NULL DEFAULT 0,
    "owl_asymmetric" SMALLINT NOT NULL DEFAULT 0,
    "owl_reflexive" SMALLINT NOT NULL DEFAULT 0,
    "owl_irreflexive" SMALLINT NOT NULL DEFAULT 0,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "metadata" TEXT,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_struct_items
CREATE TABLE IF NOT EXISTS "ont_struct_items" (
    "id" VARCHAR(64) NOT NULL,
    "struct_id" VARCHAR(64) NOT NULL,
    "sort_no" INTEGER NOT NULL DEFAULT 0,
    "prop_id" VARCHAR(64) NOT NULL,
    PRIMARY KEY ("id")
);

-- Table: ont_struct_types
CREATE TABLE IF NOT EXISTS "ont_struct_types" (
    "id" VARCHAR(64) NOT NULL,
    "struct_code" VARCHAR(64) NOT NULL,
    "category_code" VARCHAR(64),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_type_class
CREATE TABLE IF NOT EXISTS "ont_type_class" (
    "id" VARCHAR(64) NOT NULL,
    "category_code" VARCHAR(64) NOT NULL,
    "icon" VARCHAR(255),
    "color" VARCHAR(255),
    "name_prefix" VARCHAR(64) NOT NULL,
    "name_template" TEXT,
    "name_cn_base" VARCHAR(128) NOT NULL,
    "source_type" VARCHAR(64) NOT NULL DEFAULT 'platform_built'::character varying,
    "group_tag" TEXT,
    "allow_apply_types" TEXT NOT NULL DEFAULT '[]'::text,
    "allow_multi_bind" SMALLINT NOT NULL DEFAULT 0,
    "is_array_value" SMALLINT NOT NULL DEFAULT 0,
    "system_protected" SMALLINT NOT NULL DEFAULT 0,
    "param_type" VARCHAR(64) NOT NULL DEFAULT 'text'::character varying,
    "frontend_component" VARCHAR(64) NOT NULL DEFAULT 'text_input'::character varying,
    "param_options_json" TEXT,
    "param_validator_json" TEXT,
    "param_desc" VARCHAR(255),
    "demo_value" TEXT,
    "depend_on_meta_ids" TEXT NOT NULL DEFAULT '[]'::text,
    "description" VARCHAR(255),
    "replacement_meta_id" VARCHAR(64),
    "is_deprecated" SMALLINT NOT NULL DEFAULT 0,
    "deprecated_reason" VARCHAR(255),
    "support_version_min" VARCHAR(255),
    "current_version_no" INTEGER NOT NULL DEFAULT 1,
    "sort_weight" INTEGER NOT NULL DEFAULT 999,
    "create_user" VARCHAR(255),
    "update_user" VARCHAR(255),
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "param_json" TEXT,
    PRIMARY KEY ("id")
);

-- Table: ont_type_class_bind
CREATE TABLE IF NOT EXISTS "ont_type_class_bind" (
    "id" VARCHAR(64) NOT NULL,
    "env" VARCHAR(64) NOT NULL DEFAULT 'prod'::character varying,
    "type_class_meta_id" VARCHAR(64) NOT NULL,
    "applicable_type" VARCHAR(64) NOT NULL,
    "property_owner_type" VARCHAR(64),
    "property_owner_id" VARCHAR(64),
    "property_id" VARCHAR(64),
    "link_type_id" VARCHAR(64),
    "action_type_id" VARCHAR(64),
    "suffix_custom" TEXT,
    "value" TEXT,
    "bind_deprecated" SMALLINT NOT NULL DEFAULT 0,
    "remark" VARCHAR(255),
    "create_user" VARCHAR(255),
    "update_user" VARCHAR(255),
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_type_class_category_dict
CREATE TABLE IF NOT EXISTS "ont_type_class_category_dict" (
    "category_code" VARCHAR(64) NOT NULL,
    "icon" VARCHAR(255),
    "color" VARCHAR(255),
    "category_name_cn" VARCHAR(128) NOT NULL,
    "global_allow_apply_types" TEXT NOT NULL DEFAULT '[]'::text,
    "source_type" VARCHAR(64) NOT NULL DEFAULT 'platform_built'::character varying,
    "sort_weight" INTEGER NOT NULL DEFAULT 999,
    "description" VARCHAR(255),
    "created_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("category_code")
);

-- Table: ont_value_types
CREATE TABLE IF NOT EXISTS "ont_value_types" (
    "id" VARCHAR(64) NOT NULL,
    "rid" TEXT NOT NULL,
    "api_name" VARCHAR(128) NOT NULL,
    "category_code" VARCHAR(64),
    "base_type" VARCHAR(64) NOT NULL,
    "constraint_type" VARCHAR(64) NOT NULL,
    "constraint_config" TEXT,
    "enum_id" VARCHAR(64),
    "default_usage_config_id" VARCHAR(64),
    "status" SMALLINT NOT NULL DEFAULT 1,
    "rdfs_label" VARCHAR(255),
    "rdfs_comment" VARCHAR(255),
    "rdfs_see_also" VARCHAR(255),
    "rdfs_defined_by" VARCHAR(255),
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_valuetypes_usage_config
CREATE TABLE IF NOT EXISTS "ont_valuetypes_usage_config" (
    "id" VARCHAR(64) NOT NULL,
    "max_select_level" INTEGER NOT NULL DEFAULT 0,
    "allow_non_leaf" SMALLINT NOT NULL DEFAULT 0,
    "display_format" TEXT NOT NULL DEFAULT 'label'::text,
    "is_system_default" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: ont_version_repo
CREATE TABLE IF NOT EXISTS "ont_version_repo" (
    "id" VARCHAR(64) NOT NULL,
    "rid" VARCHAR(128) NOT NULL,
    "industry_dir" VARCHAR(64) NOT NULL,
    "category_dir" VARCHAR(128) NOT NULL,
    "version_no" VARCHAR(32) NOT NULL,
    "repo_branch" VARCHAR(64) NOT NULL DEFAULT ''::character varying,
    "repo_commit_id" VARCHAR(64) NOT NULL DEFAULT ''::character varying,
    "repo_url" VARCHAR(256),
    "version_status" SMALLINT NOT NULL DEFAULT 1,
    "is_default" SMALLINT NOT NULL DEFAULT 0,
    "release_note" VARCHAR(1024),
    "publish_user" VARCHAR(64),
    "publish_time" TIMESTAMP,
    "is_deleted" SMALLINT NOT NULL DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: sys_data_source
CREATE TABLE IF NOT EXISTS "sys_data_source" (
    "id" VARCHAR(64) NOT NULL,
    "category_code" VARCHAR(64),
    "ds_code" VARCHAR(64) NOT NULL,
    "ds_name" VARCHAR(128) NOT NULL,
    "ds_type" VARCHAR(64) NOT NULL,
    "jdbc_driver" TEXT,
    "jdbc_url" TEXT,
    "username" TEXT,
    "password" TEXT,
    "mongo_url" TEXT,
    "status" SMALLINT NOT NULL DEFAULT 1,
    "remark" VARCHAR(255),
    "ref_count" INTEGER NOT NULL DEFAULT 0,
    "connect_status" TEXT DEFAULT 'online'::text,
    "active_conn" SMALLINT DEFAULT 0,
    "max_conn" TEXT DEFAULT '100'::text,
    "response_ms" SMALLINT DEFAULT 0,
    "collection_cnt" SMALLINT DEFAULT 0,
    "create_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: sys_doc
CREATE TABLE IF NOT EXISTS "sys_doc" (
    "id" TEXT NOT NULL,
    "doc_key" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "category" TEXT NOT NULL DEFAULT 'general'::text,
    "content" TEXT NOT NULL DEFAULT ''::text,
    "sort" INTEGER NOT NULL DEFAULT 0,
    "status" INTEGER NOT NULL DEFAULT 1,
    "created_at" TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD HH24:MI:SS'::text),
    "updated_at" TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD HH24:MI:SS'::text),
    PRIMARY KEY ("id")
);

-- Table: sys_query_log
CREATE TABLE IF NOT EXISTS "sys_query_log" (
    "id" VARCHAR(64) NOT NULL,
    "user_id" VARCHAR(64),
    "query_text" TEXT NOT NULL,
    "matched_entity_id" VARCHAR(64),
    "match_score" NUMERIC,
    "user_clicked" INTEGER DEFAULT 0,
    "session_id" VARCHAR(128),
    "query_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: sys_stopwords
CREATE TABLE IF NOT EXISTS "sys_stopwords" (
    "id" VARCHAR(64) NOT NULL,
    "word" VARCHAR(100) NOT NULL,
    "category" VARCHAR(50) DEFAULT 'COMMON'::character varying,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: sys_synonym_candidate
CREATE TABLE IF NOT EXISTS "sys_synonym_candidate" (
    "id" VARCHAR(64) NOT NULL,
    "word" VARCHAR(255) NOT NULL,
    "synonym" VARCHAR(255) NOT NULL,
    "confidence" NUMERIC,
    "evidence_count" INTEGER DEFAULT 0,
    "status" VARCHAR(20) DEFAULT 'PENDING'::character varying,
    "source" VARCHAR(50) DEFAULT 'AUTO_LEARN'::character varying,
    "reviewer" VARCHAR(64),
    "review_time" TIMESTAMP,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id")
);

-- Table: sys_synonym_dict
CREATE TABLE IF NOT EXISTS "sys_synonym_dict" (
    "id" VARCHAR(64) NOT NULL,
    "word" VARCHAR(255) NOT NULL,
    "synonyms" TEXT NOT NULL,
    "domain" VARCHAR(50),
    "confidence" NUMERIC DEFAULT 0.9,
    "source" VARCHAR(50) DEFAULT 'MANUAL'::character varying,
    "usage_count" INTEGER DEFAULT 0,
    "create_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "update_time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    "entity_type" TEXT,
    "entity_id" TEXT,
    PRIMARY KEY ("id")
);
