package com.beiktech.bontolink.data.mapper;

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

    // 资源列表（含聚合计数，供 aggregate=true 场景使用；比分两步查少一次 RTT）
    @Select("SELECT c.id, c.rid, c.api_name, c.ns_code, c.category_code," +
            "  c.display_name, c.rdfs_label, c.icon, c.color, c.status," +
            "  COALESCE(p.cnt,  0) AS prop_total," +
            "  COALESCE(pn.cnt, 0) AS prop_normal," +
            "  COALESCE(lk.cnt, 0) AS link_count," +
            "  COALESCE(ac.cnt, 0) AS action_count," +
            "  COALESCE(ic.cnt, 0) AS interface_count" +
            " FROM ont_class c" +
            " LEFT JOIN (SELECT class_id, COUNT(1) AS cnt FROM ont_class_property GROUP BY class_id) p ON p.class_id = c.id" +
            " LEFT JOIN (SELECT class_id, COUNT(1) AS cnt FROM ont_class_property WHERE is_primary = 0 GROUP BY class_id) pn ON pn.class_id = c.id" +
            " LEFT JOIN (SELECT cid, COUNT(1) AS cnt FROM (" +
            "              SELECT source_class_id AS cid FROM ont_class_link" +
            "              UNION ALL SELECT target_class_id FROM ont_class_link) t GROUP BY cid) lk ON lk.cid = c.id" +
            " LEFT JOIN (SELECT object_class_id, COUNT(1) AS cnt FROM ont_class_action GROUP BY object_class_id) ac ON ac.object_class_id = c.id" +
            " LEFT JOIN (SELECT class_id, COUNT(1) AS cnt FROM ont_interface_class GROUP BY class_id) ic ON ic.class_id = c.id" +
            " ORDER BY c.create_time DESC")
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

    /**
     * 查全部链接类型（用于 OntModel 构建）
     * l_api_name = 以左端实体为主语时的 ObjectProperty 名
     * r_api_name = 以右端实体为主语时的 ObjectProperty 名
     * 两者不一定互为 owl:inverseOf（主语视角不同，语义不对称）
     */
    @Select("SELECT lt.id, lt.link_type_id, lt.status," +
            " lt.l_object_type_id, lt.r_object_type_id," +
            " lt.l_cardinality, lt.r_cardinality," +
            " lt.l_display_name, lt.l_plural_name, lt.l_api_name, lt.l_enabled," +
            " lt.r_display_name, lt.r_plural_name, lt.r_api_name, lt.r_enabled," +
            " lt.rdfs_label, lt.rdfs_comment, lt.category_code," +
            " lc.api_name AS l_class_api, lc.ns_code AS l_ns_code," +
            " rc.api_name AS r_class_api, rc.ns_code AS r_ns_code" +
            " FROM ont_link_types lt" +
            " LEFT JOIN ont_class lc ON lc.id = lt.l_object_type_id" +
            " LEFT JOIN ont_class rc ON rc.id = lt.r_object_type_id" +
            " WHERE lt.status != 'deprecated'" +
            " ORDER BY lt.created_at")
    List<Map<String, Object>> listLinkTypes();

    /** 查全部类动作（action_type 区分动作类型；别名兼容图谱旧键 class_id/display_name） */
    @Select("SELECT id, rid, api_name, object_class_id AS class_id, action_type, rdfs_label AS display_name, status FROM ont_class_action WHERE is_deleted = 0 ORDER BY create_time DESC")
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

    /** 查某类的属性列表（图谱/详情页用，JOIN 值类型和枚举以获取枚举信息） */
    @Select("SELECT cp.id, cp.class_id, cp.api_name, cp.prop_code, cp.prop_type, cp.data_type, cp.value_type, " +
            "       cp.display_name, cp.rdfs_label, cp.rdfs_comment, " +
            "       cp.is_primary, cp.is_required, cp.is_key, cp.is_derived, " +
            "       cp.is_multi_valued_prop, cp.is_range_constraint_prop, cp.range_class_id, " +
            "       cp.class_ds_id, cp.physical_table, cp.physical_column, " +
            "       cp.sort, cp.metadata, " +
            "       vt.constraint_type AS vt_constraint_type, vt.base_type AS vt_base_type, " +
            "       vt.enum_id AS enum_id, " +
            "       et.api_name AS enum_api_name, et.rdfs_label AS enum_label " +
            "  FROM ont_class_property cp " +
            "  LEFT JOIN ont_value_types vt ON vt.id = cp.value_type " +
            "  LEFT JOIN ont_enum_types et ON et.id = vt.enum_id " +
            " WHERE cp.class_id = #{classId} ORDER BY cp.sort, cp.id")
    List<Map<String, Object>> listProperties(@Param("classId") String classId);

    /** 查全部枚举类型（用于 OntModel 构建） */
    @Select("SELECT id, rid, api_name, enum_type, max_level, rdfs_label, rdfs_comment, status FROM ont_enum_types WHERE status = 'active'")
    List<Map<String, Object>> listEnumTypes();

    /** 查指定枚举的所有枚举项（用于 OntModel 构建） */
    @Select("SELECT id, enum_id, code, api_name, label, parent_code, level FROM ont_enum_items WHERE enum_id = #{enumId} AND status = 'active' ORDER BY level, sort_num")
    List<Map<String, Object>> listEnumItems(@Param("enumId") String enumId);

    /** 查全部值类型（用于 OntModel 构建，JOIN 枚举以获取枚举信息） */
    @Select("SELECT v.id, v.rid, v.api_name, v.base_type, v.constraint_type, v.constraint_config, " +
            "v.enum_id, v.rdfs_label, v.rdfs_comment, v.status, " +
            "e.api_name AS enum_api_name, e.rdfs_label AS enum_label " +
            "FROM ont_value_types v " +
            "LEFT JOIN ont_enum_types e ON e.id = v.enum_id " +
            "WHERE v.status = 1")
    List<Map<String, Object>> listValueTypes();

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
    @Select("<script>SELECT COUNT(*) FROM ont_class_action WHERE object_class_id IN " +
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

    /** 通过分组节点的 id（与 ont_biz_group.id 复用）直查关联的 class_id */
    @Select("<script>SELECT DISTINCT ref_id FROM ont_biz_group_class WHERE group_type = 'object_types' AND group_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    java.util.List<String> findClassIdsByGroupIds(@Param("ids") java.util.Collection<String> ids);

    /** 按 class_id 集合统计出向关系数 */
    @Select("<script>SELECT COUNT(*) FROM ont_class_link WHERE source_class_id IN " +
            "<foreach collection='ids' item='i' open='(' separator=',' close=')'>#{i}</foreach>" +
            "</script>")
    int countLinksByClassIds(@Param("ids") java.util.Collection<String> ids);

    /** 按 class_id 集合统计动作数 */
    @Select("<script>SELECT COUNT(*) FROM ont_class_action WHERE object_class_id IN " +
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

    /** 全表 ont_class_ds 精简信息（列表聚合用） */
    @Select("SELECT class_id, ds_code, physical_table FROM ont_class_ds ORDER BY rel_type, sort")
    List<Map<String, Object>> batchListAllClassDsBrief();

    /** 全表 ont_class_ds 含数据源元信息（OntModel 构建用，JOIN sys_data_source） */
    @Select("SELECT cd.class_id, cd.ds_code, cd.physical_table, cd.table_label, " +
            "       cd.rel_type, cd.alias, cd.pk_keys, cd.join_on_keys, cd.join_type, " +
            "       cd.physical_fields, cd.sort, " +
            "       ds.ds_name, ds.ds_type, ds.jdbc_url " +
            "  FROM ont_class_ds cd " +
            "  LEFT JOIN sys_data_source ds ON ds.ds_code = cd.ds_code " +
            " ORDER BY cd.class_id, cd.rel_type, cd.sort")
    List<Map<String, Object>> listAllClassDatasources();

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
    @Select("SELECT COUNT(*) FROM ont_class_action WHERE object_class_id = #{id} AND is_deleted = 0")
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
    @Select("SELECT id, rid, api_name, action_type, rdfs_label, rdfs_label AS display_name, status FROM ont_class_action WHERE object_class_id = #{id} AND is_deleted = 0 ORDER BY create_time")
    List<Map<String, Object>> listActionsOfClass(@Param("id") String id);

    /** 类相关的关系（出向 + 入向） */
    @Select("SELECT id, rid, api_name, source_class_id, target_class_id, cardinality, display_name, rdfs_label, status FROM ont_class_link WHERE source_class_id = #{id} OR target_class_id = #{id} ORDER BY create_time")
    List<Map<String, Object>> listLinksOfClass(@Param("id") String id);

    /** 类同领域的数据源（按 category_code 简单关联） */
    @Select("SELECT id, ds_code, ds_name, ds_type, status FROM sys_data_source WHERE category_code = #{categoryCode} ORDER BY create_time")
    List<Map<String, Object>> listDatasourcesByCategory(@Param("categoryCode") String categoryCode);
}
