package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/** 类型类绑定实例 Mapper(ont_type_class_bind)。 */
@Mapper
public interface TypeClassBindMapper {

    /** 某类型类的全部绑定明细 */
    @Select("SELECT * FROM ont_type_class_bind WHERE type_class_meta_id = #{metaId} ORDER BY created_at DESC")
    List<Map<String, Object>> listByMeta(@Param("metaId") String metaId);

    /** 某类型类的绑定统计(总/属性/关系/操作) */
    @Select("""
        SELECT
          COUNT(*) AS total,
          SUM(CASE WHEN applicable_type = 'property' THEN 1 ELSE 0 END) AS property_count,
          SUM(CASE WHEN applicable_type = 'relation' THEN 1 ELSE 0 END) AS relation_count,
          SUM(CASE WHEN applicable_type = 'action'   THEN 1 ELSE 0 END) AS action_count
        FROM ont_type_class_bind WHERE type_class_meta_id = #{metaId}
    """)
    Map<String, Object> statsByMeta(@Param("metaId") String metaId);

    @Select("SELECT * FROM ont_type_class_bind WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /** 按属性载体反查绑定(联类型类元数据,供属性详情「已绑定类型类」区展示 + 参数编辑) */
    @Select("""
        SELECT b.*, m.category_code, m.name_prefix, m.name_template, m.name_cn_base, m.icon, m.color,
               m.param_type, m.frontend_component, m.param_options_json, m.param_json, m.allow_multi_bind,
               m.param_desc, m.demo_value, m.description, m.is_deprecated AS meta_deprecated
        FROM ont_type_class_bind b JOIN ont_type_class m ON b.type_class_meta_id = m.id
        WHERE b.applicable_type = 'property' AND b.property_owner_id = #{ownerId} AND b.property_id = #{propertyId}
        ORDER BY b.created_at
    """)
    List<Map<String, Object>> listByProperty(@Param("ownerId") String ownerId, @Param("propertyId") String propertyId);

    /** 按关系(链接)载体反查绑定 */
    @Select("""
        SELECT b.*, m.category_code, m.name_prefix, m.name_template, m.name_cn_base, m.icon, m.color,
               m.param_type, m.frontend_component, m.param_options_json, m.param_json, m.allow_multi_bind,
               m.param_desc, m.demo_value, m.description, m.is_deprecated AS meta_deprecated
        FROM ont_type_class_bind b JOIN ont_type_class m ON b.type_class_meta_id = m.id
        WHERE b.applicable_type = 'relation' AND b.link_type_id = #{linkTypeId}
        ORDER BY b.created_at
    """)
    List<Map<String, Object>> listByLink(@Param("linkTypeId") String linkTypeId);

    /** 只更新绑定参数值 */
    @Update("UPDATE ont_type_class_bind SET value = #{value}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateValue(@Param("id") String id, @Param("value") String value, @Param("updatedAt") String updatedAt);

    /** 渲染解析:某对象类型下全部属性的类型类绑定(联属性名 + 类型类元数据),供图表/图谱真实渲染 */
    @Select("""
        SELECT b.id, b.property_id, b.value, b.bind_deprecated,
               p.api_name AS prop_api_name, p.display_name AS prop_display_name, p.data_type AS prop_data_type,
               m.category_code, m.name_prefix, m.name_template, m.name_cn_base
        FROM ont_type_class_bind b
        JOIN ont_type_class m ON b.type_class_meta_id = m.id
        LEFT JOIN ont_class_property p ON p.id = b.property_id
        WHERE b.applicable_type = 'property' AND b.property_owner_id = #{classId}
        ORDER BY p.api_name, m.category_code, m.name_prefix
    """)
    List<Map<String, Object>> listByOwnerClass(@Param("classId") String classId);

    /** 渲染解析(批量):某大类下全部对象类型属性绑定(图谱 vertex 一次拉取,按对象类型分组) */
    @Select("""
        SELECT b.property_owner_id, b.property_id, b.value,
               p.api_name AS prop_api_name, p.display_name AS prop_display_name,
               m.category_code, m.name_prefix, m.name_template, m.name_cn_base
        FROM ont_type_class_bind b
        JOIN ont_type_class m ON b.type_class_meta_id = m.id
        LEFT JOIN ont_class_property p ON p.id = b.property_id
        WHERE b.applicable_type = 'property' AND m.category_code = #{categoryCode}
        ORDER BY b.property_owner_id, p.api_name
    """)
    List<Map<String, Object>> listByCategory(@Param("categoryCode") String categoryCode);

    @Insert("""
        INSERT INTO ont_type_class_bind(id, env, type_class_meta_id, applicable_type,
            property_owner_type, property_owner_id, property_id, link_type_id, action_type_id,
            suffix_custom, value, bind_deprecated, remark, create_user, update_user)
        VALUES (#{id}, #{env}, #{type_class_meta_id}, #{applicable_type},
            #{property_owner_type}, #{property_owner_id}, #{property_id}, #{link_type_id}, #{action_type_id},
            #{suffix_custom}, #{value}, #{bind_deprecated}, #{remark}, #{create_user}, #{update_user})
    """)
    int insert(Map<String, Object> row);

    @Delete("DELETE FROM ont_type_class_bind WHERE id = #{id}")
    int delete(@Param("id") String id);

    @Delete("DELETE FROM ont_type_class_bind WHERE type_class_meta_id = #{metaId}")
    int deleteByMeta(@Param("metaId") String metaId);
}
