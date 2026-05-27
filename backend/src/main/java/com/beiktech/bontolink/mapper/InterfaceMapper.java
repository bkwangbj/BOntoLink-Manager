package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface InterfaceMapper {

    /* ============ 接口主表 ============ */

    @Select("""
        SELECT i.*,
               (SELECT COUNT(*) FROM ont_interface_property p WHERE p.interface_id = i.id) AS prop_count,
               (SELECT COUNT(*) FROM ont_interface_class ic WHERE ic.interface_id = i.id AND ic.status = 1) AS impl_count
        FROM ont_interface i
        ORDER BY i.create_time DESC
    """)
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_interface WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_interface(
            id, rid, api_name, interface_code, ns_code, category_code,
            display_name, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by,
            description, icon, color, status, metadata
        ) VALUES (
            #{id}, #{rid}, #{api_name}, #{interface_code}, #{ns_code}, #{category_code},
            #{display_name}, #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by},
            #{description}, #{icon}, #{color}, #{status}, #{metadata}
        )
    """)
    int insert(Map<String, Object> row);

    /** 更新时禁止修改 api_name */
    @Update("""
        UPDATE ont_interface SET
            interface_code=#{interface_code}, ns_code=#{ns_code}, category_code=#{category_code},
            display_name=#{display_name}, rdfs_label=#{rdfs_label}, rdfs_comment=#{rdfs_comment},
            rdfs_see_also=#{rdfs_see_also}, rdfs_defined_by=#{rdfs_defined_by},
            description=#{description}, icon=#{icon}, color=#{color},
            status=#{status}, metadata=#{metadata}
        WHERE id=#{id}
    """)
    int update(Map<String, Object> row);

    @Update("UPDATE ont_interface SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") int status);

    @Delete("DELETE FROM ont_interface WHERE id = #{id}")
    int delete(@Param("id") String id);

    @Delete("DELETE FROM ont_interface_property WHERE interface_id = #{id}")
    int deletePropertiesByInterface(@Param("id") String id);

    @Delete("DELETE FROM ont_interface_class WHERE interface_id = #{id}")
    int deleteImplByInterface(@Param("id") String id);

    /* ============ 接口属性 ============ */

    @Select("SELECT * FROM ont_interface_property WHERE interface_id = #{interfaceId} ORDER BY create_time")
    List<Map<String, Object>> listProperties(@Param("interfaceId") String interfaceId);

    @Insert("""
        INSERT INTO ont_interface_property(
            id, rid, interface_id, api_name, prop_code, data_type, value_type, category_code,
            display_name, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by,
            is_required, metadata, status
        ) VALUES (
            #{id}, #{rid}, #{interface_id}, #{api_name}, #{prop_code}, #{data_type}, #{value_type}, #{category_code},
            #{display_name}, #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by},
            #{is_required}, #{metadata}, #{status}
        )
    """)
    int insertProperty(Map<String, Object> row);

    @Update("""
        UPDATE ont_interface_property SET
            prop_code=#{prop_code}, data_type=#{data_type}, value_type=#{value_type}, display_name=#{display_name},
            rdfs_label=#{rdfs_label}, rdfs_comment=#{rdfs_comment},
            is_required=#{is_required}, metadata=#{metadata}, status=#{status}
        WHERE id=#{id}
    """)
    int updateProperty(Map<String, Object> row);

    @Delete("DELETE FROM ont_interface_property WHERE id = #{id}")
    int deleteProperty(@Param("id") String id);

    /* ============ 类实现接口 ============ */

    @Select("""
        SELECT ic.id AS rel_id, ic.status AS rel_status, ic.category_code AS rel_category_code,
               c.id, c.api_name, c.display_name, c.rdfs_label, c.ns_code, c.icon, c.color
        FROM ont_interface_class ic
        JOIN ont_class c ON c.id = ic.class_id
        WHERE ic.interface_id = #{interfaceId}
        ORDER BY ic.create_time
    """)
    List<Map<String, Object>> listImplementers(@Param("interfaceId") String interfaceId);

    @Select("SELECT COUNT(*) FROM ont_interface_class WHERE interface_id = #{interfaceId} AND class_id = #{classId}")
    int existsImpl(@Param("interfaceId") String interfaceId, @Param("classId") String classId);

    @Insert("""
        INSERT INTO ont_interface_class(id, interface_id, class_id, category_code, status)
        VALUES (#{id}, #{interfaceId}, #{classId}, #{categoryCode}, 1)
    """)
    int addImpl(@Param("id") String id,
                @Param("interfaceId") String interfaceId,
                @Param("classId") String classId,
                @Param("categoryCode") String categoryCode);

    @Delete("DELETE FROM ont_interface_class WHERE interface_id = #{interfaceId} AND class_id = #{classId}")
    int removeImpl(@Param("interfaceId") String interfaceId, @Param("classId") String classId);
}
