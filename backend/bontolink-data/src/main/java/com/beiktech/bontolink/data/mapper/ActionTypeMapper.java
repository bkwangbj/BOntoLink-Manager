package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 动作类型 (Action Types) Mapper
 * <p>
 * 主表 ont_class_action 的 CRUD。列表 JOIN ont_class(关联主体对象类) 与
 * ont_link_types(链接类动作) 取展示名, 并子查询规则数。删除时级联清理直接子表。
 */
@Mapper
public interface ActionTypeMapper {

    /** 列表 (JOIN 对象类/链接类型展示名 + 规则数按5维度统计) */
    @Select("""
        SELECT a.*,
               oc.display_name AS object_class_name, oc.api_name AS object_class_api,
               oc.icon AS object_class_icon, oc.color AS object_class_color,
               lt.rdfs_label AS link_type_name, lt.link_type_id AS link_type_code,
               (SELECT COUNT(1) FROM ont_class_action_rule r WHERE r.action_id = a.id) AS rule_count,
               (SELECT COUNT(1) FROM ont_class_action_rule r WHERE r.action_id = a.id AND (r.rule_config LIKE '%"kind":"create_object"%' OR r.rule_config LIKE '%"kind":"modify_object"%' OR r.rule_config LIKE '%"kind":"delete_object"%')) AS rule_object,
               (SELECT COUNT(1) FROM ont_class_action_rule r WHERE r.action_id = a.id AND (r.rule_config LIKE '%"kind":"create_link"%' OR r.rule_config LIKE '%"kind":"delete_link"%')) AS rule_link,
               (SELECT COUNT(1) FROM ont_class_action_rule r WHERE r.action_id = a.id AND r.rule_config LIKE '%"kind":"notification"%') AS rule_notify,
               (SELECT COUNT(1) FROM ont_class_action_rule r WHERE r.action_id = a.id AND r.rule_config LIKE '%"kind":"function"%') AS rule_function,
               (SELECT COUNT(1) FROM ont_class_action_rule r WHERE r.action_id = a.id AND r.rule_config LIKE '%"kind":"webhook"%') AS rule_webhook
        FROM ont_class_action a
        LEFT JOIN ont_class oc ON oc.id = a.object_class_id
        LEFT JOIN ont_link_types lt ON lt.id = a.link_type_id
        WHERE a.is_deleted = 0
        ORDER BY a.update_time DESC
    """)
    List<Map<String, Object>> listAll();

    /** 按 id 查单条 (JOIN 对象类/链接类型展示名) */
    @Select("""
        SELECT a.*,
               oc.display_name AS object_class_name, oc.api_name AS object_class_api,
               oc.icon AS object_class_icon, oc.color AS object_class_color,
               lt.rdfs_label AS link_type_name, lt.link_type_id AS link_type_code
        FROM ont_class_action a
        LEFT JOIN ont_class oc ON oc.id = a.object_class_id
        LEFT JOIN ont_link_types lt ON lt.id = a.link_type_id
        WHERE a.id = #{id}
    """)
    Map<String, Object> findById(@Param("id") String id);

    /** 检查动作编码 (api_name) 是否已存在 */
    @Select("SELECT 1 FROM ont_class_action WHERE api_name = #{code} AND is_deleted = 0 LIMIT 1")
    Integer existsByCode(@Param("code") String code);

    /** 新增动作 */
    @Insert("""
        INSERT INTO ont_class_action(
            id, rid, api_name, m_type, action_type, object_class_id, link_type_id,
            function_code, category_code, show_on_detail, show_on_batch, button_text,
            compile_status, save_path, form_enabled, submit_criteria_enabled, status,
            current_version, is_deleted, icon, color, metadata,
            rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by)
        VALUES (
            #{id}, #{rid}, #{api_name}, #{m_type}, #{action_type}, #{object_class_id}, #{link_type_id},
            #{function_code}, #{category_code}, #{show_on_detail}, #{show_on_batch}, #{button_text},
            #{compile_status}, #{save_path}, #{form_enabled}, #{submit_criteria_enabled}, #{status},
            #{current_version}, #{is_deleted}, #{icon}, #{color}, #{metadata},
            #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})
    """)
    int insert(Map<String, Object> row);

    /** 更新动作 (同步 update_time) */
    @Update("""
        UPDATE ont_class_action SET
          rid = #{rid},
          api_name = #{api_name},
          m_type = #{m_type}, action_type = #{action_type},
          object_class_id = #{object_class_id}, link_type_id = #{link_type_id},
          function_code = #{function_code}, category_code = #{category_code},
          show_on_detail = #{show_on_detail}, show_on_batch = #{show_on_batch},
          button_text = #{button_text}, compile_status = #{compile_status},
          save_path = #{save_path}, form_enabled = #{form_enabled},
          submit_criteria_enabled = #{submit_criteria_enabled}, status = #{status},
          current_version = #{current_version}, icon = #{icon}, color = #{color},
          metadata = #{metadata}, rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
          rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
          update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    /** 仅更新状态 (草稿/已发布/已停用) */
    @Update("UPDATE ont_class_action SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") Object status);

    /* —— 表单分组 / 参数 (新建向导 参数映射 步骤持久化) —— */
    @Insert("""
        INSERT INTO ont_action_form_section(id, action_id, section_name, title, description, collapsed, sort)
        VALUES (#{id}, #{action_id}, #{section_name}, #{title}, #{description}, #{collapsed}, #{sort})
    """)
    int insertFormSection(Map<String, Object> row);

    @Insert("""
        INSERT INTO ont_action_form_param(
            id, action_id, section_id, param_code, param_name, param_type, data_type,
            is_required, is_multi, default_value, placeholder, sort, config)
        VALUES (
            #{id}, #{action_id}, #{section_id}, #{param_code}, #{param_name}, #{param_type}, #{data_type},
            #{is_required}, #{is_multi}, #{default_value}, #{placeholder}, #{sort}, #{config})
    """)
    int insertFormParam(Map<String, Object> row);

    @Select("SELECT * FROM ont_action_form_section WHERE action_id = #{id} ORDER BY sort")
    List<Map<String, Object>> listFormSections(@Param("id") String id);

    @Select("SELECT * FROM ont_action_form_param WHERE action_id = #{id} ORDER BY sort")
    List<Map<String, Object>> listFormParams(@Param("id") String id);

    /* —— 规则 + 规则属性映射 (编辑详情「规则」页) —— */
    @Insert("""
        INSERT INTO ont_class_action_rule(
            id, action_id, action_type, rule_type, rule_name, target_param_code, link_type_code, sort, status, rule_config)
        VALUES (
            #{id}, #{action_id}, #{action_type}, #{rule_type}, #{rule_name}, #{target_param_code}, #{link_type_code}, #{sort}, #{status}, #{rule_config})
    """)
    int insertRule(Map<String, Object> row);

    @Select("SELECT * FROM ont_class_action_rule WHERE action_id = #{id} ORDER BY rule_type, sort")
    List<Map<String, Object>> listRules(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_action_rule_property_mapping(
            id, rule_id, property_code, property_name, prop_operator, value_source, value_content, is_primary_key, is_required, sort)
        VALUES (
            #{id}, #{rule_id}, #{property_code}, #{property_name}, #{prop_operator}, #{value_source}, #{value_content}, #{is_primary_key}, #{is_required}, #{sort})
    """)
    int insertRulePropMapping(Map<String, Object> row);

    @Select("SELECT * FROM ont_action_rule_property_mapping WHERE rule_id = #{ruleId} ORDER BY sort")
    List<Map<String, Object>> listRulePropMappings(@Param("ruleId") String ruleId);

    @Delete("DELETE FROM ont_action_rule_property_mapping WHERE rule_id IN (SELECT id FROM ont_class_action_rule WHERE action_id = #{id})")
    int deleteRulePropMappingsByAction(@Param("id") String id);

    /* —— 提交标准 (编辑详情「安全与提交」页) —— */
    @Insert("""
        INSERT INTO ont_action_submit_standard_config(id, action_id, enabled, validate_mode, error_message, config)
        VALUES (#{id}, #{action_id}, #{enabled}, #{validate_mode}, #{error_message}, #{config})
    """)
    int insertSubmitStandard(Map<String, Object> row);

    @Select("SELECT * FROM ont_action_submit_standard_config WHERE action_id = #{id} LIMIT 1")
    Map<String, Object> getSubmitStandard(@Param("id") String id);

    /* —— 提交条件节点树 —— */
    @Insert("""
        INSERT INTO ont_action_submit_condition_node(
            id, standard_id, parent_id, node_type, logic_op, left_code, operator, right_value, value_source, error_message, sort)
        VALUES (
            #{id}, #{standard_id}, #{parent_id}, #{node_type}, #{logic_op}, #{left_code}, #{operator}, #{right_value}, #{value_source}, #{error_message}, #{sort})
    """)
    int insertSubmitConditionNode(Map<String, Object> row);

    @Select("SELECT * FROM ont_action_submit_condition_node WHERE standard_id = #{standardId} ORDER BY sort")
    List<Map<String, Object>> listSubmitConditionNodes(@Param("standardId") String standardId);

    @Delete("DELETE FROM ont_action_submit_condition_node WHERE standard_id IN (SELECT id FROM ont_action_submit_standard_config WHERE action_id = #{id})")
    int deleteSubmitConditionNodesByAction(@Param("id") String id);

    /* —— 执行日志 (执行引擎) —— */
    @Insert("""
        INSERT INTO ont_action_execution(
            id, action_id, action_api_name, object_class_id, op_type,
            input_params, resolved_result, status, message, dry_run, executed_by)
        VALUES (
            #{id}, #{action_id}, #{action_api_name}, #{object_class_id}, #{op_type},
            #{input_params}, #{resolved_result}, #{status}, #{message}, #{dry_run}, #{executed_by})
    """)
    int insertExecution(Map<String, Object> row);

    @Select("SELECT * FROM ont_action_execution WHERE action_id = #{id} ORDER BY execute_time DESC LIMIT 50")
    List<Map<String, Object>> listExecutions(@Param("id") String id);

    @Delete("DELETE FROM ont_action_execution WHERE action_id = #{id}")
    int deleteExecutionsByAction(@Param("id") String id);

    /** 删除主记录 */
    @Delete("DELETE FROM ont_class_action WHERE id = #{id}")
    int delete(@Param("id") String id);

    /* —— 级联删除直接子表 (以 action_id 关联) —— */
    @Delete("DELETE FROM ont_class_action_rule WHERE action_id = #{id}")
    int deleteRulesByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_form_section WHERE action_id = #{id}")
    int deleteFormSectionsByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_form_param WHERE action_id = #{id}")
    int deleteFormParamsByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_form_override_block WHERE action_id = #{id}")
    int deleteOverrideBlocksByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_function_rule_config WHERE action_id = #{id}")
    int deleteFunctionConfigsByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_notification_rule_config WHERE action_id = #{id}")
    int deleteNotificationConfigsByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_webhook_rule_config WHERE action_id = #{id}")
    int deleteWebhookConfigsByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_submit_standard_config WHERE action_id = #{id}")
    int deleteSubmitStandardsByAction(@Param("id") String id);

    @Delete("DELETE FROM ont_action_form_global_config WHERE action_id = #{id}")
    int deleteGlobalConfigByAction(@Param("id") String id);
}
