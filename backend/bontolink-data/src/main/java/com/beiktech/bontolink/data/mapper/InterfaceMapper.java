package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 接口 Mapper — 覆盖 ont_interface / ont_interface_property / ont_interface_class 三张表,
 * 提供接口主表 CRUD、接口属性管理及对象类实现关系的增删查功能.
 */
@Mapper
public interface InterfaceMapper {

    /* ============ 接口主表 ============ */

    /** 查询所有接口, 附带子查询统计属性数量 prop_count 和已实现对象类数量 impl_count */
    @Select("""
        SELECT i.*,
               (SELECT COUNT(*) FROM ont_interface_property p WHERE p.interface_id = i.id) AS prop_count,
               (SELECT COUNT(*) FROM ont_interface_class ic WHERE ic.interface_id = i.id AND ic.status = 1) AS impl_count
        FROM ont_interface i
        ORDER BY i.create_time DESC
    """)
    List<Map<String, Object>> listAll();

    /** 按 id 查询单条接口 */
    @Select("SELECT * FROM ont_interface WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /** 新增接口 */
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

    /** 更新接口启用/禁用状态 */
    @Update("UPDATE ont_interface SET status=#{status} WHERE id=#{id}")
    int updateStatus(@Param("id") String id, @Param("status") int status);

    /** 删除接口主记录 */
    @Delete("DELETE FROM ont_interface WHERE id = #{id}")
    int delete(@Param("id") String id);

    /** 级联删除接口的全部属性 */
    @Delete("DELETE FROM ont_interface_property WHERE interface_id = #{id}")
    int deletePropertiesByInterface(@Param("id") String id);

    /** 级联删除接口的全部实现关系记录 */
    @Delete("DELETE FROM ont_interface_class WHERE interface_id = #{id}")
    int deleteImplByInterface(@Param("id") String id);

    /* ============ 接口属性 ============ */

    /** 查询指定接口的所有属性, 按创建时间排序 */
    @Select("SELECT * FROM ont_interface_property WHERE interface_id = #{interfaceId} ORDER BY create_time")
    List<Map<String, Object>> listProperties(@Param("interfaceId") String interfaceId);

    /** 新增接口属性 */
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

    /** 更新接口属性 */
    @Update("""
        UPDATE ont_interface_property SET
            prop_code=#{prop_code}, data_type=#{data_type}, value_type=#{value_type}, display_name=#{display_name},
            rdfs_label=#{rdfs_label}, rdfs_comment=#{rdfs_comment},
            is_required=#{is_required}, metadata=#{metadata}, status=#{status}
        WHERE id=#{id}
    """)
    int updateProperty(Map<String, Object> row);

    /** 删除单条接口属性 */
    @Delete("DELETE FROM ont_interface_property WHERE id = #{id}")
    int deleteProperty(@Param("id") String id);

    /* ============ 类实现接口 ============ */

    /** 查询实现指定接口的所有对象类, JOIN ont_class 取类的基础字段 */
    @Select("""
        SELECT ic.id AS rel_id, ic.status AS rel_status, ic.category_code AS rel_category_code,
               c.id, c.api_name, c.display_name, c.rdfs_label, c.ns_code, c.icon, c.color
        FROM ont_interface_class ic
        JOIN ont_class c ON c.id = ic.class_id
        WHERE ic.interface_id = #{interfaceId}
        ORDER BY ic.create_time
    """)
    List<Map<String, Object>> listImplementers(@Param("interfaceId") String interfaceId);

    /** 检查某对象类是否已实现指定接口 (防重) */
    @Select("SELECT COUNT(*) FROM ont_interface_class WHERE interface_id = #{interfaceId} AND class_id = #{classId}")
    int existsImpl(@Param("interfaceId") String interfaceId, @Param("classId") String classId);

    /** 添加对象类实现接口的关系记录 */
    @Insert("""
        INSERT INTO ont_interface_class(id, interface_id, class_id, category_code, status)
        VALUES (#{id}, #{interfaceId}, #{classId}, #{categoryCode}, 1)
    """)
    int addImpl(@Param("id") String id,
                @Param("interfaceId") String interfaceId,
                @Param("classId") String classId,
                @Param("categoryCode") String categoryCode);

    /** 移除对象类与接口的实现关系 */
    @Delete("DELETE FROM ont_interface_class WHERE interface_id = #{interfaceId} AND class_id = #{classId}")
    int removeImpl(@Param("interfaceId") String interfaceId, @Param("classId") String classId);
}
