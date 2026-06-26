package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 枚举类型 Mapper — 覆盖 ont_enum_types / ont_enum_items / ont_enum_level_code_rule
 * / ont_enum_sync_config / ont_enum_sync_log 五张表, 提供枚举主表 CRUD、枚举项管理、
 * 层次编码规则、数据库同步配置及同步日志功能.
 */
@Mapper
public interface EnumTypeMapper {

    /* ---- 分组(树) ---- 已迁移至 ont_biz_group_class with group_type='enum_types';
       此处仅保留兼容 stub:返回空数组,前端应改用 /api/group-refs?type=enum_types */
    default List<Map<String, Object>> listGroups() { return java.util.Collections.emptyList(); }
    default int insertGroup(Map<String, Object> row) { return 0; }
    default int updateGroup(Map<String, Object> row) { return 0; }
    default int deleteGroup(@Param("id") String id) { return 0; }

    /* ---- 枚举类型主表 (不再包含 group_id) ---- */
    /** 查询所有枚举类型, 附带子查询统计每个枚举的枚举项数量 item_count */
    @Select("""
        SELECT t.*,
               (SELECT COUNT(*) FROM ont_enum_items i WHERE i.enum_id = t.id) AS item_count
        FROM ont_enum_types t
        ORDER BY t.create_time DESC
    """)
    List<Map<String, Object>> listTypes();

    /** 按 id 查询单条枚举类型 */
    @Select("SELECT * FROM ont_enum_types WHERE id = #{id}")
    Map<String, Object> findType(@Param("id") String id);

    /** 新增枚举类型 */
    @Insert("""
        INSERT INTO ont_enum_types(id, rid, api_name, category_code, enum_type, max_level,
            top_code, status, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by)
        VALUES (#{id}, #{rid}, #{api_name}, #{category_code}, #{enum_type}, #{max_level},
            #{top_code}, #{status}, #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})
    """)
    int insertType(Map<String, Object> row);

    /** 更新枚举类型基础信息 */
    @Update("""
        UPDATE ont_enum_types SET enum_type=#{enum_type}, max_level=#{max_level},
            category_code=#{category_code},
            top_code=#{top_code}, status=#{status}, rdfs_label=#{rdfs_label}, rdfs_comment=#{rdfs_comment},
            rdfs_see_also=#{rdfs_see_also}, rdfs_defined_by=#{rdfs_defined_by},
            update_time = datetime('now','localtime')
        WHERE id=#{id}
    """)
    int updateType(Map<String, Object> row);

    /** 删除枚举类型主记录 */
    @Delete("DELETE FROM ont_enum_types WHERE id = #{id}")
    int deleteType(@Param("id") String id);

    /* ---- 枚举项 ---- */
    /** 查询指定枚举的所有枚举项, 按层级、排序号、编码排序 */
    @Select("""
        SELECT * FROM ont_enum_items WHERE enum_id = #{enumId}
        ORDER BY level, sort_num, code
    """)
    List<Map<String, Object>> listItems(@Param("enumId") String enumId);

    /** 新增枚举项 */
    @Insert("""
        INSERT INTO ont_enum_items(id, enum_id, code, api_name, label, parent_code, level, sort_num, status)
        VALUES (#{id}, #{enum_id}, #{code}, #{api_name}, #{label}, #{parent_code}, #{level}, #{sort_num}, #{status})
    """)
    int insertItem(Map<String, Object> row);

    /** 更新枚举项信息 */
    @Update("""
        UPDATE ont_enum_items SET label=#{label}, api_name=#{api_name}, parent_code=#{parent_code},
            level=#{level}, sort_num=#{sort_num}, status=#{status},
            update_time = datetime('now','localtime')
        WHERE id=#{id}
    """)
    int updateItem(Map<String, Object> row);

    /** 删除单条枚举项 */
    @Delete("DELETE FROM ont_enum_items WHERE id = #{id}")
    int deleteItem(@Param("id") String id);

    /* ---- 层次编码规则 ---- */
    /** 查询指定枚举的所有层次编码规则, 按层级升序 */
    @Select("SELECT * FROM ont_enum_level_code_rule WHERE enum_id = #{enumId} ORDER BY rule_level")
    List<Map<String, Object>> listLevelRules(@Param("enumId") String enumId);

    /** 新增层次编码规则 */
    @Insert("""
        INSERT INTO ont_enum_level_code_rule(id, enum_id, code_name, rule_level, code_separator,
            code_len, total_len, fill_char, fill_pos)
        VALUES (#{id}, #{enum_id}, #{code_name}, #{rule_level}, #{code_separator},
            #{code_len}, #{total_len}, #{fill_char}, #{fill_pos})
    """)
    int insertLevelRule(Map<String, Object> row);

    /** 删除指定枚举的全部层次编码规则 (更新前先清空再重新插入) */
    @Delete("DELETE FROM ont_enum_level_code_rule WHERE enum_id = #{enumId}")
    int deleteLevelRulesByEnum(@Param("enumId") String enumId);

    /* ---- 数据库同步配置 ---- */
    /** 查询指定枚举的数据库同步配置 */
    @Select("SELECT * FROM ont_enum_sync_config WHERE enum_id = #{enumId}")
    Map<String, Object> findSyncConfig(@Param("enumId") String enumId);

    /** 新增数据库同步配置 */
    @Insert("""
        INSERT INTO ont_enum_sync_config(id, enum_id, data_source_id, table_alias, table_name,
            field_code, field_name, field_sort, field_status, field_parent, filter_sql, sync_mode, sync_strategy)
        VALUES (#{id}, #{enum_id}, #{data_source_id}, #{table_alias}, #{table_name},
            #{field_code}, #{field_name}, #{field_sort}, #{field_status}, #{field_parent}, #{filter_sql},
            #{sync_mode}, #{sync_strategy})
    """)
    int insertSyncConfig(Map<String, Object> row);

    /** 更新数据库同步配置 */
    @Update("""
        UPDATE ont_enum_sync_config SET
          data_source_id=#{data_source_id}, table_alias=#{table_alias}, table_name=#{table_name},
          field_code=#{field_code}, field_name=#{field_name}, field_sort=#{field_sort},
          field_status=#{field_status}, field_parent=#{field_parent}, filter_sql=#{filter_sql},
          sync_mode=#{sync_mode}, sync_strategy=#{sync_strategy},
          update_time = datetime('now','localtime')
        WHERE id=#{id}
    """)
    int updateSyncConfig(Map<String, Object> row);

    /** 删除数据库同步配置 */
    @Delete("DELETE FROM ont_enum_sync_config WHERE id = #{id}")
    int deleteSyncConfig(@Param("id") String id);

    /* ---- 同步日志 ---- */
    /** 查询指定枚举的同步日志, 最新优先, 最多返回 200 条 */
    @Select("SELECT * FROM ont_enum_sync_log WHERE enum_id = #{enumId} ORDER BY sync_time DESC LIMIT 200")
    List<Map<String, Object>> listSyncLogs(@Param("enumId") String enumId);

    /** 写入一条同步执行日志 */
    @Insert("""
        INSERT INTO ont_enum_sync_log(id, enum_id, sync_type, add_count, update_count, del_count,
            fail_count, sync_status, error_msg, oper_user)
        VALUES (#{id}, #{enum_id}, #{sync_type}, #{add_count}, #{update_count}, #{del_count},
            #{fail_count}, #{sync_status}, #{error_msg}, #{oper_user})
    """)
    int insertSyncLog(Map<String, Object> row);

    /* ---- 被引用查询: 枚举 → 值类型 → 类属性 / 接口属性 ---- */
    /**
     * 查询枚举被哪些属性引用; UNION ALL 合并两路:
     * 1) 对象类属性 (ont_class_property → ont_value_types → ont_class)
     * 2) 接口属性 (ont_interface_property → ont_value_types → ont_interface)
     */
    @Select("""
        SELECT 'class_prop' AS ref_type,
               c.rdfs_label AS object_label, c.api_name AS object_api, c.id AS object_id,
               p.rdfs_label AS prop_label, p.api_name AS prop_api, p.id AS prop_id,
               v.rdfs_label AS value_type_label, v.api_name AS value_type_api
          FROM ont_class_property p
          JOIN ont_value_types v ON v.id = p.value_type
          LEFT JOIN ont_class c ON c.id = p.class_id
         WHERE v.enum_id = #{enumId}
        UNION ALL
        SELECT 'interface_prop' AS ref_type,
               i.rdfs_label AS object_label, i.api_name AS object_api, i.id AS object_id,
               p.rdfs_label AS prop_label, p.api_name AS prop_api, p.id AS prop_id,
               v.rdfs_label AS value_type_label, v.api_name AS value_type_api
          FROM ont_interface_property p
          JOIN ont_value_types v ON v.id = p.value_type
          LEFT JOIN ont_interface i ON i.id = p.interface_id
         WHERE v.enum_id = #{enumId}
    """)
    List<Map<String, Object>> listReferences(@Param("enumId") String enumId);
}
