package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

/**
 * 本体图谱总览 mapper：聚合查询 ont_class / ont_class_link / ont_class_action /
 * ont_interface / ont_class_property / ont_class_group / ont_class_disjoint_union
 * 等核心表，用于图谱渲染、统计汇总及对象类型详情页。
 */
@Mapper
public interface OntologyMapper {

    // 资源列表（轻量行，前端列表展示；不含大字段 description/metadata）
    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, icon, color, status FROM ont_class ORDER BY create_time DESC")
    List<Map<String, Object>> listClasses();

    /* 图谱用: 类含 parent_class_id (用于父子边)、category_code (跨画布联动) */
    @Select("SELECT id, api_name, category_code, parent_class_id, display_name, rdfs_label, icon, color, status FROM ont_class")
    List<Map<String, Object>> listClassesForGraph();

    /* 等价 / 互斥 关系 (group_type = equivalent / disjoint) */
    @Select("SELECT id, class_id, ref_class_id, group_type FROM ont_class_group WHERE status = 1")
    List<Map<String, Object>> listClassGroups();

    /* 并集关系 (parent_class_id ⇔ sub_class_id) */
    @Select("SELECT id, parent_class_id, sub_class_id FROM ont_class_disjoint_union")
    List<Map<String, Object>> listClassDisjointUnions();

    /** 查全部类关系（用于图谱边列表） */
    @Select("SELECT id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, status FROM ont_class_link ORDER BY create_time DESC")
    List<Map<String, Object>> listLinks();

    /** 查全部类动作（action_kind 区分动作类型） */
    @Select("SELECT id, rid, api_name, class_id, action_kind, display_name, status FROM ont_class_action ORDER BY create_time DESC")
    List<Map<String, Object>> listActions();

    /** 查全部接口轻量列表 */
    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, description, icon, color, status FROM ont_interface ORDER BY create_time DESC")
    List<Map<String, Object>> listInterfaces();

    /** 按 id 查单个接口详情 */
    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, description, icon, color, status FROM ont_interface WHERE id = #{id}")
    Map<String, Object> findInterfaceById(@Param("id") String id);

    /** 查某接口的属性列表 */
    @Select("SELECT id, rid, api_name, data_type, display_name, rdfs_label, is_required FROM ont_interface_property WHERE interface_id = #{interfaceId} ORDER BY id")
    List<Map<String, Object>> listInterfaceProperties(@Param("interfaceId") String interfaceId);

    /** 查所有挂了分类的对象类轻量信息（通过 ont_biz_group_class 子查询过滤，用于选择器候选列表） */
    @Select("SELECT id, api_name, display_name, color, icon FROM ont_class WHERE id IN " +
            "(SELECT ref_id FROM ont_biz_group_class WHERE group_type = 'object_types' AND category_code IS NOT NULL)")
    List<Map<String, Object>> listAllClassesLight();

    /** 查某类的属性列表（图谱/详情页用，不含 XSD/OWL 约束字段，字段比 ClassMetaMapper.listClassPropertiesFull 精简） */
    @Select("SELECT id, class_id, api_name, prop_code, prop_type, data_type, value_type, " +
            "       display_name, rdfs_label, rdfs_comment, " +
            "       is_primary, is_required, is_key, is_derived, " +
            "       is_multi_valued_prop, is_range_constraint_prop, range_class_id, " +
            "       class_ds_id, physical_table, physical_column, " +
            "       sort, metadata " +
            "  FROM ont_class_property WHERE class_id = #{classId} ORDER BY sort, id")
    List<Map<String, Object>> listProperties(@Param("classId") String classId);

    // 全局计数（统计面板用）
    /** 查对象类总数 */
    @Select("SELECT COUNT(1) FROM ont_class")
    int countClasses();

    /** 查类关系总数 */
    @Select("SELECT COUNT(1) FROM ont_class_link")
    int countLinks();

    /** 查动作总数 */
    @Select("SELECT COUNT(1) FROM ont_class_action")
    int countActions();

    /** 查接口总数 */
    @Select("SELECT COUNT(1) FROM ont_interface")
    int countInterfaces();

    /** 查属性总数 */
    @Select("SELECT COUNT(1) FROM ont_class_property")
    int countProperties();

    /** 按行业分类编码统计对象类数 */
    @Select("SELECT COUNT(1) FROM ont_class WHERE category_code = #{code}")
    int countClassesByCategory(@Param("code") String code);

    /** 按命名空间编码统计对象类数 */
    @Select("SELECT COUNT(1) FROM ont_class WHERE ns_code = #{nsCode}")
    int countClassesByNs(@Param("nsCode") String nsCode);

    /* —— 按 category_code 列表聚合：返回 0 表示无匹配 —— */

    /** 按多个 category_code 统计对象类总数（MyBatis foreach 动态 IN） */
    @Select("<script>SELECT COUNT(*) FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    int countClassesByCodes(@Param("codes") java.util.Collection<String> codes);

    /** 按多个 category_code 统计属性总数（子查询先找类再计属性） */
    @Select("<script>SELECT COUNT(*) FROM ont_class_property WHERE class_id IN " +
            "(SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)" +
            "</script>")
    int countPropertiesByCodes(@Param("codes") java.util.Collection<String> codes);

    /** 按多个 category_code 统计关系总数（子查询过滤出向类） */
    @Select("<script>SELECT COUNT(*) FROM ont_class_link WHERE source_class_id IN " +
            "(SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)" +
            "</script>")
    int countLinksByCodes(@Param("codes") java.util.Collection<String> codes);

    /** 按多个 category_code 统计动作总数（子查询过滤类） */
    @Select("<script>SELECT COUNT(*) FROM ont_class_action WHERE class_id IN " +
            "(SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>)" +
            "</script>")
    int countActionsByCodes(@Param("codes") java.util.Collection<String> codes);

    /* —— 按 ns_code 列表聚合接口 —— */

    /** 按多个 ns_code 统计接口总数 */
    @Select("<script>SELECT COUNT(*) FROM ont_interface WHERE ns_code IN " +
            "<foreach collection='nsCodes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    int countInterfacesByNsCodes(@Param("nsCodes") java.util.Collection<String> nsCodes);

    /* —— 通过 class_id 列表聚合（分组节点的统一路径） —— */

    /** 按多个 category_code 取对应的 class_id 列表（图谱分组节点展开用） */
    @Select("<script>SELECT id FROM ont_class WHERE category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    java.util.List<String> findClassIdsByCategoryCodes(@Param("codes") java.util.Collection<String> codes);

    /** 通过分组的 category_code（grp_xxx）从关联表查到 class_id (ref_id) */
    @Select("<script>SELECT DISTINCT ref_id FROM ont_biz_group_class WHERE group_type = 'object_types' AND category_code IN " +
            "<foreach collection='codes' item='c' open='(' separator=',' close=')'>#{c}</foreach>" +
            "</script>")
    java.util.List<String> findClassIdsByGroupCategoryCodes(@Param("codes") java.util.Collection<String> codes);

    /** 按 class_id 集合统计出向关系数 */
    @Select("<script>SELECT COUNT(*) FROM ont_class_link WHERE source_class_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    int countLinksByClassIds(@Param("ids") java.util.Collection<String> ids);

    /** 按 class_id 集合统计动作数 */
    @Select("<script>SELECT COUNT(*) FROM ont_class_action WHERE class_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    int countActionsByClassIds(@Param("ids") java.util.Collection<String> ids);

    /** 按 class_id 集合统计属性数 */
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

    /* —— 对象类型列表/详情 聚合 —— */

    /** 单条对象类型详情（全字段；表中不存在的注释字段 rdfs_see_also / rdfs_defined_by 暂未建列） */
    @Select("SELECT id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment, " +
            " description, icon, color, status, metadata, create_time, update_time " +
            " FROM ont_class WHERE id = #{id}")
    Map<String, Object> findClassById(@Param("id") String id);

    /** 单类的属性总数 / 普通属性数（普通 = is_primary = 0） */
    @Select("SELECT COUNT(*) FROM ont_class_property WHERE class_id = #{id}")
    int countPropertiesOfClass(@Param("id") String id);

    /** 单类的普通属性数（is_primary = 0，排除主属性） */
    @Select("SELECT COUNT(*) FROM ont_class_property WHERE class_id = #{id} AND is_primary = 0")
    int countNormalPropertiesOfClass(@Param("id") String id);

    /** 出/入向关系数 */
    @Select("SELECT COUNT(*) FROM ont_class_link WHERE source_class_id = #{id} OR target_class_id = #{id}")
    int countLinksOfClass(@Param("id") String id);

    /** 同领域(category_code)的数据源数 — 暂用领域归属推算，待 ont_class_ds 落地后改成精确关联 */
    @Select("SELECT COUNT(*) FROM sys_data_source WHERE category_code = #{categoryCode}")
    int countDatasourcesByCategory(@Param("categoryCode") String categoryCode);

    /** 对象类型挂接的物理数据集 (含物理字段 JSON) — 用于关系图画布 */
    @Select("SELECT id, class_id, ds_code, physical_table, table_label, rel_type, alias, " +
            "       pk_keys, join_on_keys, join_type, physical_fields, sort, status " +
            "  FROM ont_class_ds WHERE class_id = #{id} ORDER BY rel_type, sort")
    List<Map<String, Object>> listClassDatasources(@Param("id") String id);

    /** 对象类型挂接的物理表精简信息 (仅 ds_code/physical_table) — 用于列表的数据源数/关联表列, 避免拉取大字段 physical_fields */
    @Select("SELECT ds_code, physical_table FROM ont_class_ds WHERE class_id = #{id} ORDER BY rel_type, sort")
    List<Map<String, Object>> listClassDsBrief(@Param("id") String id);

    /** 子类数（按 parent_class_id；当前 schema 暂无该列，返回 0 即可，留接口） */
    @Select("SELECT 0")
    int countChildClassesOfClass(@Param("id") String id);

    /** 类的动作数 */
    @Select("SELECT COUNT(*) FROM ont_class_action WHERE class_id = #{id}")
    int countActionsOfClass(@Param("id") String id);

    /** 类实现的接口数 */
    @Select("SELECT COUNT(*) FROM ont_interface_class WHERE class_id = #{id}")
    int countInterfacesOfClass(@Param("id") String id);

    /** 类实现的接口列表 */
    @Select("SELECT i.id, i.api_name, i.display_name, i.rdfs_label, i.icon, i.color, i.status " +
            " FROM ont_interface i " +
            " INNER JOIN ont_interface_class ic ON ic.interface_id = i.id " +
            " WHERE ic.class_id = #{id} ORDER BY i.create_time")
    List<Map<String, Object>> listInterfacesOfClass(@Param("id") String id);

    /** 类相关的动作列表 */
    @Select("SELECT id, rid, api_name, action_kind, display_name, rdfs_label, status FROM ont_class_action WHERE class_id = #{id} ORDER BY create_time")
    List<Map<String, Object>> listActionsOfClass(@Param("id") String id);

    /** 类相关的关系（出向 + 入向） */
    @Select("SELECT id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, rdfs_label, status FROM ont_class_link WHERE source_class_id = #{id} OR target_class_id = #{id} ORDER BY create_time")
    List<Map<String, Object>> listLinksOfClass(@Param("id") String id);

    /** 类同领域的数据源（按 category_code 简单关联） */
    @Select("SELECT id, ds_code, ds_name, ds_type, status FROM sys_data_source WHERE category_code = #{categoryCode} ORDER BY create_time")
    List<Map<String, Object>> listDatasourcesByCategory(@Param("categoryCode") String categoryCode);
}
