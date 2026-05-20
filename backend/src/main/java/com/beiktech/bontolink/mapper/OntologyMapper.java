package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface OntologyMapper {

    // 资源列表（轻量行，前端列表展示）
    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, icon, color, status FROM ont_class ORDER BY create_time DESC")
    List<Map<String, Object>> listClasses();

    @Select("SELECT id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, status FROM ont_class_link ORDER BY create_time DESC")
    List<Map<String, Object>> listLinks();

    @Select("SELECT id, rid, api_name, class_id, action_kind, display_name, status FROM ont_class_action ORDER BY create_time DESC")
    List<Map<String, Object>> listActions();

    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, description, icon, color, status FROM ont_interface ORDER BY create_time DESC")
    List<Map<String, Object>> listInterfaces();

    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, description, icon, color, status FROM ont_interface WHERE id = #{id}")
    Map<String, Object> findInterfaceById(@Param("id") String id);

    @Select("SELECT id, rid, api_name, data_type, display_name, rdfs_label, is_required FROM ont_interface_property WHERE interface_id = #{interfaceId} ORDER BY id")
    List<Map<String, Object>> listInterfaceProperties(@Param("interfaceId") String interfaceId);

    @Select("SELECT id, api_name, display_name, color, icon FROM ont_class WHERE id IN (SELECT class_id FROM ont_biz_group_class WHERE category_code IS NOT NULL)")
    List<Map<String, Object>> listAllClassesLight();

    @Select("SELECT id, class_id, api_name, data_type, display_name, is_primary, is_required FROM ont_class_property WHERE class_id = #{classId} ORDER BY id")
    List<Map<String, Object>> listProperties(@Param("classId") String classId);

    // 类计数（按命名空间 / 按分类编码）
    @Select("SELECT COUNT(1) FROM ont_class")
    int countClasses();

    @Select("SELECT COUNT(1) FROM ont_class_link")
    int countLinks();

    @Select("SELECT COUNT(1) FROM ont_class_action")
    int countActions();

    @Select("SELECT COUNT(1) FROM ont_interface")
    int countInterfaces();

    @Select("SELECT COUNT(1) FROM ont_class_property")
    int countProperties();

    @Select("SELECT COUNT(1) FROM ont_class WHERE category_code = #{code}")
    int countClassesByCategory(@Param("code") String code);

    @Select("SELECT COUNT(1) FROM ont_class WHERE ns_code = #{nsCode}")
    int countClassesByNs(@Param("nsCode") String nsCode);

    /* —— 按 category_code 列表聚合：返回 0 表示无匹配 —— */

    @Select("<script>SELECT COUNT(*) FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    int countClassesByCodes(@Param("codes") java.util.Collection<String> codes);

    @Select("<script>SELECT COUNT(*) FROM ont_class_property WHERE class_id IN " +
            "(SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)" +
            "</script>")
    int countPropertiesByCodes(@Param("codes") java.util.Collection<String> codes);

    @Select("<script>SELECT COUNT(*) FROM ont_class_link WHERE source_class_id IN " +
            "(SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)" +
            "</script>")
    int countLinksByCodes(@Param("codes") java.util.Collection<String> codes);

    @Select("<script>SELECT COUNT(*) FROM ont_class_action WHERE class_id IN " +
            "(SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)" +
            "</script>")
    int countActionsByCodes(@Param("codes") java.util.Collection<String> codes);

    /* —— 按 ns_code 列表聚合接口 —— */

    @Select("<script>SELECT COUNT(*) FROM ont_interface WHERE ns_code IN " +
            "<foreach collection='nsCodes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    int countInterfacesByNsCodes(@Param("nsCodes") java.util.Collection<String> nsCodes);

    /* —— 通过 class_id 列表聚合（分组节点的统一路径） —— */

    @Select("<script>SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    java.util.List<String> findClassIdsByCategoryCodes(@Param("codes") java.util.Collection<String> codes);

    /** 通过分组的 category_code（grp_xxx）从关联表查到 class_id */
    @Select("<script>SELECT DISTINCT class_id FROM ont_biz_group_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    java.util.List<String> findClassIdsByGroupCategoryCodes(@Param("codes") java.util.Collection<String> codes);

    @Select("<script>SELECT COUNT(*) FROM ont_class_link WHERE source_class_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    int countLinksByClassIds(@Param("ids") java.util.Collection<String> ids);

    @Select("<script>SELECT COUNT(*) FROM ont_class_action WHERE class_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    int countActionsByClassIds(@Param("ids") java.util.Collection<String> ids);

    @Select("<script>SELECT COUNT(*) FROM ont_class_property WHERE class_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    int countPropertiesByClassIds(@Param("ids") java.util.Collection<String> ids);

    /** 取一组 class_id 对应的轻量信息（用于卡片 chip 展示） */
    @Select("<script>SELECT id, api_name, display_name, color, icon FROM ont_class WHERE id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            " ORDER BY create_time</script>")
    java.util.List<java.util.Map<String, Object>> findClassesByIds(@Param("ids") java.util.Collection<String> ids);

    /** 取 source/target 都在给定 class_id 集合内的关系（用于分组节点图谱） */
    @Select("<script>SELECT id, api_name, display_name, source_class_id, target_class_id, cardinality " +
            " FROM ont_class_link " +
            " WHERE source_class_id IN <foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "   AND target_class_id IN <foreach collection='ids' item='j' open='(' separator=',' close=')'>#{j}</foreach>" +
            "</script>")
    java.util.List<java.util.Map<String, Object>> findLinksWithin(@Param("ids") java.util.Collection<String> ids);
}
