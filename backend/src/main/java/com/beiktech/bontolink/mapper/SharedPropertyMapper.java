package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 共享属性 (Shared properties) Mapper
 * <p>
 * "引用次数 (ref_count)" 通过 ont_class_property.prop_code 与 ont_shared_properties.prop_code
 * 的字符串匹配做反向追踪。
 */
@Mapper
public interface SharedPropertyMapper {

    /** 列表(含引用计数 + 值类型名称 + 格式化启用状态), 默认按更新时间倒序 */
    @Select("""
        SELECT sp.*,
               v.rdfs_label AS value_type_label,
               v.api_name   AS value_type_api,
               (SELECT COUNT(1) FROM ont_class_property cp WHERE cp.prop_code = sp.prop_code) AS ref_count,
               (SELECT pf.format_enabled FROM ont_property_format pf
                 WHERE pf.property_id = sp.id AND pf.src_type = 2 LIMIT 1) AS format_enabled
        FROM ont_shared_properties sp
        LEFT JOIN ont_value_types v ON v.id = sp.value_type
        ORDER BY sp.update_time DESC
    """)
    List<Map<String, Object>> listAll();

    @Select("""
        SELECT sp.*,
               v.rdfs_label AS value_type_label,
               v.api_name   AS value_type_api,
               (SELECT COUNT(1) FROM ont_class_property cp WHERE cp.prop_code = sp.prop_code) AS ref_count
        FROM ont_shared_properties sp
        LEFT JOIN ont_value_types v ON v.id = sp.value_type
        WHERE sp.id = #{id}
    """)
    Map<String, Object> findById(@Param("id") String id);

    @Select("SELECT 1 FROM ont_shared_properties WHERE prop_code = #{code} LIMIT 1")
    Integer existsByCode(@Param("code") String code);

    @Insert("""
        INSERT INTO ont_shared_properties(id, rid, category_code, prop_code, prop_type, is_key,
            data_type, value_type, is_required, is_multi_valued_prop, is_range_constraint_prop,
            xsd_min_length, xsd_max_length, xsd_length, xsd_pattern, xsd_min_inclusive, xsd_max_inclusive,
            owl_functional, owl_inverse_functional, owl_transitive, owl_symmetric, owl_asymmetric,
            owl_reflexive, owl_irreflexive, status, metadata,
            rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by)
        VALUES (#{id}, #{rid}, #{category_code}, #{prop_code}, #{prop_type}, #{is_key},
            #{data_type}, #{value_type}, #{is_required}, #{is_multi_valued_prop}, #{is_range_constraint_prop},
            #{xsd_min_length}, #{xsd_max_length}, #{xsd_length}, #{xsd_pattern}, #{xsd_min_inclusive}, #{xsd_max_inclusive},
            #{owl_functional}, #{owl_inverse_functional}, #{owl_transitive}, #{owl_symmetric}, #{owl_asymmetric},
            #{owl_reflexive}, #{owl_irreflexive}, #{status}, #{metadata},
            #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})
    """)
    int insert(Map<String, Object> row);

    @Update("""
        UPDATE ont_shared_properties SET
          category_code = #{category_code},
          prop_type = #{prop_type}, is_key = #{is_key},
          data_type = #{data_type}, value_type = #{value_type},
          is_required = #{is_required}, is_multi_valued_prop = #{is_multi_valued_prop},
          is_range_constraint_prop = #{is_range_constraint_prop},
          xsd_min_length = #{xsd_min_length}, xsd_max_length = #{xsd_max_length},
          xsd_length = #{xsd_length}, xsd_pattern = #{xsd_pattern},
          xsd_min_inclusive = #{xsd_min_inclusive}, xsd_max_inclusive = #{xsd_max_inclusive},
          owl_functional = #{owl_functional}, owl_inverse_functional = #{owl_inverse_functional},
          owl_transitive = #{owl_transitive}, owl_symmetric = #{owl_symmetric},
          owl_asymmetric = #{owl_asymmetric}, owl_reflexive = #{owl_reflexive},
          owl_irreflexive = #{owl_irreflexive},
          status = #{status}, metadata = #{metadata},
          rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
          rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
          update_time = datetime('now','localtime')
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    @Delete("DELETE FROM ont_shared_properties WHERE id = #{id}")
    int delete(@Param("id") String id);

    /** 引用列表: 哪些对象类属性引用了该共享属性 (按 prop_code 反查) */
    @Select("""
        SELECT cp.id, cp.api_name, cp.display_name, cp.prop_code,
               cp.class_id, c.display_name AS class_name, c.rdfs_label AS class_label,
               cp.category_code, cp.create_time
        FROM ont_class_property cp
        LEFT JOIN ont_class c ON c.id = cp.class_id
        WHERE cp.prop_code = #{propCode}
        ORDER BY cp.create_time DESC
    """)
    List<Map<String, Object>> listReferences(@Param("propCode") String propCode);

    /** 同步分组 (调用方先 delete 旧绑定再 insert 新绑定) */
    @Delete("DELETE FROM ont_biz_group_class WHERE ref_id = #{spId} AND group_type = 'shared_props'")
    int deleteGroupRefs(@Param("spId") String spId);

    @Insert("""
        INSERT INTO ont_biz_group_class(id, group_id, ref_id, group_type, category_code, g_sort)
        VALUES (#{id}, #{groupId}, #{refId}, 'shared_props', #{categoryCode}, #{gSort})
    """)
    int insertGroupRef(@Param("id") String id, @Param("groupId") String groupId,
                       @Param("refId") String refId, @Param("categoryCode") String categoryCode,
                       @Param("gSort") Integer gSort);
}
