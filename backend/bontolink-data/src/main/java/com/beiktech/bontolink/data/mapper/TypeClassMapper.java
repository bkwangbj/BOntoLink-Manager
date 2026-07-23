package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 类型类标准定义(ont_type_class)Mapper。
 * 升级版:定义与绑定分离,本表仅存"标准定义/元数据"。
 */
@Mapper
public interface TypeClassMapper {

    @Select("SELECT * FROM ont_type_class ORDER BY sort_weight, category_code, name_prefix")
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_type_class WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /** 大类下统计(总数/弃用数) */
    @Select("SELECT category_code, COUNT(*) AS total, SUM(is_deprecated) AS deprecated_count FROM ont_type_class GROUP BY category_code")
    List<Map<String, Object>> categoryStats();

    /** 同一大类下 name_prefix 是否重复(排除自身) */
    @Select("SELECT COUNT(*) FROM ont_type_class WHERE category_code = #{categoryCode} AND name_prefix = #{namePrefix} AND id <> #{excludeId}")
    int countSameName(@Param("categoryCode") String categoryCode, @Param("namePrefix") String namePrefix, @Param("excludeId") String excludeId);

    @Insert("""
        INSERT INTO ont_type_class(
            id, category_code, icon, color, name_prefix, name_template, name_cn_base,
            source_type, group_tag, allow_apply_types, allow_multi_bind, is_array_value,
            system_protected, param_type, frontend_component, param_options_json, param_validator_json, param_json,
            param_desc, demo_value, depend_on_meta_ids, description, replacement_meta_id,
            is_deprecated, deprecated_reason, support_version_min, current_version_no, sort_weight,
            create_user, update_user
        ) VALUES (
            #{id}, #{category_code}, #{icon}, #{color}, #{name_prefix}, #{name_template}, #{name_cn_base},
            #{source_type}, #{group_tag}, #{allow_apply_types}, #{allow_multi_bind}, #{is_array_value},
            #{system_protected}, #{param_type}, #{frontend_component}, #{param_options_json}, #{param_validator_json}, #{param_json},
            #{param_desc}, #{demo_value}, #{depend_on_meta_ids}, #{description}, #{replacement_meta_id},
            #{is_deprecated}, #{deprecated_reason}, #{support_version_min}, #{current_version_no}, #{sort_weight},
            #{create_user}, #{update_user}
        )
    """)
    int insert(Map<String, Object> row);

    @Update("""
        UPDATE ont_type_class SET
            category_code = #{category_code}, icon = #{icon}, color = #{color},
            name_prefix = #{name_prefix}, name_template = #{name_template}, name_cn_base = #{name_cn_base},
            source_type = #{source_type}, group_tag = #{group_tag}, allow_apply_types = #{allow_apply_types},
            allow_multi_bind = #{allow_multi_bind}, is_array_value = #{is_array_value}, system_protected = #{system_protected},
            param_type = #{param_type}, frontend_component = #{frontend_component},
            param_options_json = #{param_options_json}, param_validator_json = #{param_validator_json}, param_json = #{param_json},
            param_desc = #{param_desc}, demo_value = #{demo_value}, depend_on_meta_ids = #{depend_on_meta_ids},
            description = #{description}, replacement_meta_id = #{replacement_meta_id},
            is_deprecated = #{is_deprecated}, deprecated_reason = #{deprecated_reason},
            support_version_min = #{support_version_min}, current_version_no = #{current_version_no},
            sort_weight = #{sort_weight}, update_user = #{update_user},
            updated_at = #{updated_at}
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    @Update("UPDATE ont_type_class SET is_deprecated = #{deprecated}, deprecated_reason = #{reason}, updated_at = #{updatedAt} WHERE id = #{id}")
    int setDeprecated(@Param("id") String id, @Param("deprecated") int deprecated,
                      @Param("reason") String reason, @Param("updatedAt") String updatedAt);

    @Delete("DELETE FROM ont_type_class WHERE id = #{id}")
    int delete(@Param("id") String id);
}
