package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ValueTypeMapper {

    @Select("""
        SELECT v.*, e.rdfs_label AS enum_label, e.api_name AS enum_api_name, e.enum_type AS enum_type_kind
        FROM ont_value_types v
        LEFT JOIN ont_enum_types e ON e.id = v.enum_id
        ORDER BY v.create_time DESC
    """)
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_value_types WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_value_types(id, rid, api_name, category_code, base_type, constraint_type,
            constraint_config, enum_id, default_usage_config_id, status,
            rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by)
        VALUES (#{id}, #{rid}, #{api_name}, #{category_code}, #{base_type}, #{constraint_type},
            #{constraint_config}, #{enum_id}, #{default_usage_config_id}, #{status},
            #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})
    """)
    int insert(Map<String, Object> row);

    @Update("""
        UPDATE ont_value_types SET
          rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
          rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
          constraint_type = #{constraint_type}, constraint_config = #{constraint_config},
          enum_id = #{enum_id}, default_usage_config_id = #{default_usage_config_id},
          category_code = #{category_code}, status = #{status},
          update_time = datetime('now','localtime')
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    @Delete("DELETE FROM ont_value_types WHERE id = #{id}")
    int delete(@Param("id") String id);

    /* usage config */
    @Select("SELECT * FROM ont_valuetypes_usage_config WHERE id = #{id}")
    Map<String, Object> findUsageConfig(@Param("id") String id);

    @Select("SELECT * FROM ont_valuetypes_usage_config ORDER BY is_system_default DESC, create_time")
    List<Map<String, Object>> listUsageConfigs();

    @Insert("INSERT INTO ont_valuetypes_usage_config(id, max_select_level, allow_non_leaf, display_format, is_system_default) " +
            "VALUES(#{id}, #{max_select_level}, #{allow_non_leaf}, #{display_format}, #{is_system_default})")
    int insertUsageConfig(Map<String, Object> row);

    @Update("UPDATE ont_valuetypes_usage_config SET max_select_level = #{max_select_level}, " +
            "allow_non_leaf = #{allow_non_leaf}, display_format = #{display_format}, " +
            "is_system_default = #{is_system_default}, update_time = datetime('now','localtime') WHERE id = #{id}")
    int updateUsageConfig(Map<String, Object> row);
}
