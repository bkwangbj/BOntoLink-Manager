package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 对象类型补充元数据 mapper：
 *  - ont_class_group  (等价 / 不相交)
 *  - ont_class_disjoint_union (互斥并集)
 *  - ont_property_equivalent / ont_property_disjoint (等价 / 互斥属性)
 *  - ont_class_property CRUD（带 OWL 特性 / XSD 约束 / 物理映射）
 *  - ont_class_ds (类→数据集映射)
 */
@Mapper
public interface ClassMetaMapper {

    /* ============ ont_class_group (equivalent / disjoint) ============ */

    /** 按关系类型(equivalent/disjoint)查当前类涉及的分组；CASE WHEN 自动将"另一端"类 id 归一到 other_class_id */
    @Select("""
        SELECT g.id, g.class_id, g.ref_class_id, g.group_type, g.status,
               g.rdfs_comment, g.rdfs_see_also, g.rdfs_defined_by,
               CASE WHEN g.class_id = #{currentId} THEN g.ref_class_id ELSE g.class_id END AS other_class_id,
               c.api_name AS other_api_name,
               c.display_name AS other_display_name,
               c.rdfs_label AS other_rdfs_label,
               c.rdfs_comment AS other_rdfs_comment,
               c.icon AS other_icon,
               c.color AS other_color,
               c.status AS other_status,
               c.category_code AS other_category_code
        FROM ont_class_group g
        LEFT JOIN ont_class c ON c.id = CASE WHEN g.class_id = #{currentId} THEN g.ref_class_id ELSE g.class_id END
        WHERE g.group_type = #{type}
          AND (g.class_id = #{currentId} OR g.ref_class_id = #{currentId})
          AND NOT (g.class_id = #{currentId} AND g.ref_class_id = #{currentId})
        ORDER BY g.create_time DESC
    """)
    List<Map<String, Object>> listGroupByType(@Param("currentId") String currentId, @Param("type") String type);

    /** 检查同类型的类对关系是否已存在（防重；minId/maxId 为排序后的 id，保证无向去重） */
    @Select("SELECT COUNT(*) FROM ont_class_group WHERE group_type = #{type} AND class_id = #{minId} AND ref_class_id = #{maxId}")
    int existsGroupPair(@Param("type") String type, @Param("minId") String minId, @Param("maxId") String maxId);

    /** 新增一条类分组关系（equivalent / disjoint） */
    @Insert("INSERT INTO ont_class_group(id, class_id, ref_class_id, group_type, status, rdfs_comment, rdfs_see_also, rdfs_defined_by) " +
            "VALUES (#{id}, #{class_id}, #{ref_class_id}, #{group_type}, #{status}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})")
    int insertGroup(Map<String, Object> row);

    /** 更新类分组关系的状态和 RDFS 注解字段 */
    @Update("UPDATE ont_class_group SET status = #{status}, rdfs_comment = #{rdfs_comment}, " +
            "rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by} WHERE id = #{id}")
    int updateGroup(Map<String, Object> row);

    /** 删除一条类分组关系 */
    @Delete("DELETE FROM ont_class_group WHERE id = #{id}")
    int deleteGroup(@Param("id") String id);

    /* ============ ont_class_disjoint_union ============ */

    /** 查某父类下的所有互斥并集子类（JOIN ont_class 获取子类展示信息） */
    @Select("""
        SELECT du.id, du.parent_class_id, du.sub_class_id, du.status,
               du.rdfs_comment, du.rdfs_see_also, du.rdfs_defined_by,
               c.api_name, c.display_name, c.rdfs_label, c.rdfs_comment AS sub_rdfs_comment,
               c.icon, c.color, c.category_code, c.status AS class_status
        FROM ont_class_disjoint_union du
        LEFT JOIN ont_class c ON c.id = du.sub_class_id
        WHERE du.parent_class_id = #{parentClassId}
        ORDER BY du.create_time DESC
    """)
    List<Map<String, Object>> listDisjointUnion(@Param("parentClassId") String parentClassId);

    /** 当前类作为子类，处于哪些互斥并集中（用于反查） */
    @Select("""
        SELECT du.parent_class_id, c.api_name, c.display_name, c.rdfs_label
        FROM ont_class_disjoint_union du
        LEFT JOIN ont_class c ON c.id = du.parent_class_id
        WHERE du.sub_class_id = #{subClassId}
    """)
    List<Map<String, Object>> listDisjointUnionParents(@Param("subClassId") String subClassId);

    /** 新增一条互斥并集关系（parent_class_id 为并集容器，sub_class_id 为成员类） */
    @Insert("INSERT INTO ont_class_disjoint_union(id, parent_class_id, sub_class_id, status, rdfs_comment, rdfs_see_also, rdfs_defined_by) " +
            "VALUES (#{id}, #{parent_class_id}, #{sub_class_id}, #{status}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})")
    int insertDisjointUnion(Map<String, Object> row);

    /** 更新互斥并集关系的状态和备注 */
    @Update("UPDATE ont_class_disjoint_union SET status = #{status}, rdfs_comment = #{rdfs_comment} WHERE id = #{id}")
    int updateDisjointUnion(Map<String, Object> row);

    /** 删除一条互斥并集关系 */
    @Delete("DELETE FROM ont_class_disjoint_union WHERE id = #{id}")
    int deleteDisjointUnion(@Param("id") String id);

    /* ============ ont_property_equivalent / ont_property_disjoint ============ */

    /** 按属性 id 查其等价属性关系（四表 JOIN：关系表 + 两端属性 + 两端类，双向匹配） */
    @Select("""
        SELECT pe.id, pe.class_id1, pe.prop_id1, pe.class_id2, pe.prop_id2, pe.status, pe.rdfs_comment,
               p1.api_name AS api1, p1.display_name AS name1, p1.prop_type AS type1, p1.data_type AS dt1,
               p2.api_name AS api2, p2.display_name AS name2, p2.prop_type AS type2, p2.data_type AS dt2,
               c1.api_name AS class1_api, c1.display_name AS class1_name,
               c2.api_name AS class2_api, c2.display_name AS class2_name
        FROM ont_property_equivalent pe
        LEFT JOIN ont_class_property p1 ON p1.id = pe.prop_id1
        LEFT JOIN ont_class_property p2 ON p2.id = pe.prop_id2
        LEFT JOIN ont_class c1 ON c1.id = pe.class_id1
        LEFT JOIN ont_class c2 ON c2.id = pe.class_id2
        WHERE pe.prop_id1 = #{propId} OR pe.prop_id2 = #{propId}
    """)
    List<Map<String, Object>> listPropertyEquivalents(@Param("propId") String propId);

    /** 按类 id 查该类所有属性的等价关系（用于类详情页汇总展示） */
    @Select("""
        SELECT pe.id, pe.class_id1, pe.prop_id1, pe.class_id2, pe.prop_id2, pe.status, pe.rdfs_comment,
               p1.api_name AS api1, p1.display_name AS name1, p1.prop_type AS type1, p1.data_type AS dt1,
               p2.api_name AS api2, p2.display_name AS name2, p2.prop_type AS type2, p2.data_type AS dt2,
               c1.api_name AS class1_api, c1.display_name AS class1_name,
               c2.api_name AS class2_api, c2.display_name AS class2_name
        FROM ont_property_equivalent pe
        LEFT JOIN ont_class_property p1 ON p1.id = pe.prop_id1
        LEFT JOIN ont_class_property p2 ON p2.id = pe.prop_id2
        LEFT JOIN ont_class c1 ON c1.id = pe.class_id1
        LEFT JOIN ont_class c2 ON c2.id = pe.class_id2
        WHERE pe.class_id1 = #{classId} OR pe.class_id2 = #{classId}
    """)
    List<Map<String, Object>> listPropertyEquivalentsByClass(@Param("classId") String classId);

    /** 新增一条属性等价关系 */
    @Insert("INSERT INTO ont_property_equivalent(id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment) " +
            "VALUES (#{id}, #{class_id1}, #{prop_id1}, #{class_id2}, #{prop_id2}, #{status}, #{rdfs_comment})")
    int insertPropertyEquivalent(Map<String, Object> row);

    /** 更新属性等价关系的状态和备注 */
    @Update("UPDATE ont_property_equivalent SET status = #{status}, rdfs_comment = #{rdfs_comment} WHERE id = #{id}")
    int updatePropertyEquivalent(Map<String, Object> row);

    /** 删除一条属性等价关系 */
    @Delete("DELETE FROM ont_property_equivalent WHERE id = #{id}")
    int deletePropertyEquivalent(@Param("id") String id);

    /** 按类 id 查该类所有属性的互斥关系（四表 JOIN，结构同 listPropertyEquivalentsByClass） */
    @Select("""
        SELECT pd.id, pd.class_id1, pd.prop_id1, pd.class_id2, pd.prop_id2, pd.status, pd.rdfs_comment,
               p1.api_name AS api1, p1.display_name AS name1, p1.prop_type AS type1, p1.data_type AS dt1,
               p2.api_name AS api2, p2.display_name AS name2, p2.prop_type AS type2, p2.data_type AS dt2,
               c1.api_name AS class1_api, c1.display_name AS class1_name,
               c2.api_name AS class2_api, c2.display_name AS class2_name
        FROM ont_property_disjoint pd
        LEFT JOIN ont_class_property p1 ON p1.id = pd.prop_id1
        LEFT JOIN ont_class_property p2 ON p2.id = pd.prop_id2
        LEFT JOIN ont_class c1 ON c1.id = pd.class_id1
        LEFT JOIN ont_class c2 ON c2.id = pd.class_id2
        WHERE pd.class_id1 = #{classId} OR pd.class_id2 = #{classId}
    """)
    List<Map<String, Object>> listPropertyDisjointByClass(@Param("classId") String classId);

    /** 新增一条属性互斥关系 */
    @Insert("INSERT INTO ont_property_disjoint(id, class_id1, prop_id1, class_id2, prop_id2, status, rdfs_comment) " +
            "VALUES (#{id}, #{class_id1}, #{prop_id1}, #{class_id2}, #{prop_id2}, #{status}, #{rdfs_comment})")
    int insertPropertyDisjoint(Map<String, Object> row);

    /** 更新属性互斥关系的状态和备注 */
    @Update("UPDATE ont_property_disjoint SET status = #{status}, rdfs_comment = #{rdfs_comment} WHERE id = #{id}")
    int updatePropertyDisjoint(Map<String, Object> row);

    /** 删除一条属性互斥关系 */
    @Delete("DELETE FROM ont_property_disjoint WHERE id = #{id}")
    int deletePropertyDisjoint(@Param("id") String id);

    /* ============ ont_class_property CRUD (扩展版) ============ */

    /** 查某类的完整属性列表（含全部 OWL 特性、XSD 约束及物理映射字段） */
    @Select("""
        SELECT id, class_id, category_code, api_name, prop_code, prop_type, data_type, value_type,
               display_name, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by,
               class_ds_id, physical_table, physical_column,
               is_primary, is_required, is_key, is_derived, is_multi_valued_prop, is_range_constraint_prop,
               range_class_id, sub_property_of,
               xsd_min_length, xsd_max_length, xsd_length, xsd_pattern, xsd_min_inclusive, xsd_max_inclusive,
               owl_functional, owl_inverse_functional, owl_transitive, owl_symmetric,
               owl_asymmetric, owl_reflexive, owl_irreflexive,
               metadata, sort, status, rid, create_time, update_time
        FROM ont_class_property
        WHERE class_id = #{classId}
        ORDER BY sort, create_time
    """)
    List<Map<String, Object>> listClassPropertiesFull(@Param("classId") String classId);

    /** 新增一条类属性（含 OWL/XSD 约束及物理映射字段） */
    @Insert("""
        INSERT INTO ont_class_property(
            id, rid, class_id, category_code, api_name, prop_code, prop_type, data_type, value_type,
            display_name, rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by,
            class_ds_id, physical_table, physical_column,
            is_primary, is_required, is_key, is_derived, is_multi_valued_prop, is_range_constraint_prop,
            range_class_id, sub_property_of,
            xsd_min_length, xsd_max_length, xsd_length, xsd_pattern, xsd_min_inclusive, xsd_max_inclusive,
            owl_functional, owl_inverse_functional, owl_transitive, owl_symmetric,
            owl_asymmetric, owl_reflexive, owl_irreflexive,
            metadata, sort, status
        ) VALUES (
            #{id}, #{rid}, #{class_id}, #{category_code}, #{api_name}, #{prop_code}, #{prop_type}, #{data_type}, #{value_type},
            #{display_name}, #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by},
            #{class_ds_id}, #{physical_table}, #{physical_column},
            #{is_primary}, #{is_required}, #{is_key}, #{is_derived}, #{is_multi_valued_prop}, #{is_range_constraint_prop},
            #{range_class_id}, #{sub_property_of},
            #{xsd_min_length}, #{xsd_max_length}, #{xsd_length}, #{xsd_pattern}, #{xsd_min_inclusive}, #{xsd_max_inclusive},
            #{owl_functional}, #{owl_inverse_functional}, #{owl_transitive}, #{owl_symmetric},
            #{owl_asymmetric}, #{owl_reflexive}, #{owl_irreflexive},
            #{metadata}, #{sort}, #{status}
        )
    """)
    int insertClassProperty(Map<String, Object> row);

    /** 全量更新一条类属性（含 OWL/XSD/物理映射；同时刷新 update_time） */
    @Update("""
        UPDATE ont_class_property SET
            prop_code = #{prop_code}, prop_type = #{prop_type}, data_type = #{data_type}, value_type = #{value_type},
            display_name = #{display_name}, rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
            rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
            class_ds_id = #{class_ds_id}, physical_table = #{physical_table}, physical_column = #{physical_column},
            is_primary = #{is_primary}, is_required = #{is_required}, is_key = #{is_key},
            is_derived = #{is_derived}, is_multi_valued_prop = #{is_multi_valued_prop},
            is_range_constraint_prop = #{is_range_constraint_prop},
            range_class_id = #{range_class_id}, sub_property_of = #{sub_property_of},
            xsd_min_length = #{xsd_min_length}, xsd_max_length = #{xsd_max_length}, xsd_length = #{xsd_length},
            xsd_pattern = #{xsd_pattern}, xsd_min_inclusive = #{xsd_min_inclusive}, xsd_max_inclusive = #{xsd_max_inclusive},
            owl_functional = #{owl_functional}, owl_inverse_functional = #{owl_inverse_functional},
            owl_transitive = #{owl_transitive}, owl_symmetric = #{owl_symmetric},
            owl_asymmetric = #{owl_asymmetric}, owl_reflexive = #{owl_reflexive}, owl_irreflexive = #{owl_irreflexive},
            metadata = #{metadata}, sort = #{sort}, status = #{status},
            update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    int updateClassProperty(Map<String, Object> row);

    /** 删除一条类属性 */
    @Delete("DELETE FROM ont_class_property WHERE id = #{id}")
    int deleteClassProperty(@Param("id") String id);

    /** 仅更新单条属性的 sort 值 (用于拖拽排序) */
    @Update("UPDATE ont_class_property SET sort = #{sort}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateClassPropertySort(@Param("id") String id, @Param("sort") int sort);

    /* ============ ont_class extended fields (类表达式 / 其他) ============ */

    /** 全量更新对象类（含 RDFS/OWL 表达式/分类/命名空间等所有可编辑字段） */
    @Update("""
        UPDATE ont_class SET
            display_name = #{display_name}, rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
            rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
            description = #{description}, icon = #{icon}, color = #{color}, status = #{status},
            metadata = #{metadata}, parent_class_id = #{parent_class_id}, category_code = #{category_code},
            ns_code = #{ns_code}, class_expr_type = #{class_expr_type}, class_expr_content = #{class_expr_content},
            is_thing = #{is_thing}, is_nothing = #{is_nothing}, is_common = #{is_common},
            update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    int updateClassFull(Map<String, Object> row);

    /** 新建对象类 */
    @Insert("""
        INSERT INTO ont_class(id, rid, api_name, ns_code, category_code, display_name, rdfs_label, rdfs_comment,
                              rdfs_see_also, rdfs_defined_by, description, icon, color, status, metadata,
                              parent_class_id, class_expr_type, class_expr_content, is_thing, is_nothing, is_common)
        VALUES (#{id}, #{rid}, #{api_name}, #{ns_code}, #{category_code}, #{display_name}, #{rdfs_label}, #{rdfs_comment},
                #{rdfs_see_also}, #{rdfs_defined_by}, #{description}, #{icon}, #{color}, #{status}, #{metadata},
                #{parent_class_id}, #{class_expr_type}, #{class_expr_content}, #{is_thing}, #{is_nothing}, #{is_common})
    """)
    int insertClass(Map<String, Object> row);

    /** api_name 是否已存在 */
    @Select("SELECT COUNT(1) FROM ont_class WHERE api_name = #{apiName}")
    int existsApiName(@Param("apiName") String apiName);

    /** 按 category_code 取分类的 ns_code + parent_id (用于新建对象时自动带入命名空间) */
    @Select("SELECT ns_code, parent_id FROM ont_biz_category WHERE category_code = #{code}")
    Map<String, Object> findCategoryByCode(@Param("code") String code);

    /** 按分类 id 取 ns_code + parent_id (沿 parent_id 向上回溯命名空间) */
    @Select("SELECT ns_code, parent_id FROM ont_biz_category WHERE id = #{id}")
    Map<String, Object> findCategoryById(@Param("id") String id);

    /* ============ 对象类状态切换 / 删除 ============ */

    /** 仅切换类状态 (启用 1 / 禁用 0)，不触碰其他字段 */
    @Update("UPDATE ont_class SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateClassStatus(@Param("id") String id, @Param("status") int status);

    /** 按 id 取对象类的显示名称（删除确认弹框用） */
    @Select("SELECT display_name FROM ont_class WHERE id = #{id}")
    String findClassName(@Param("id") String id);

    /* —— 删除前引用检查 (被其他资源引用则禁止删除) —— */
    /** 统计以该类为父类的子类数量 */
    @Select("SELECT COUNT(1) FROM ont_class WHERE parent_class_id = #{id}")
    int countChildClasses(@Param("id") String id);

    /** 统计引用该类的链接类型数（左端或右端） */
    @Select("SELECT COUNT(1) FROM ont_link_types WHERE l_object_type_id = #{id} OR r_object_type_id = #{id}")
    int countLinkTypeRefs(@Param("id") String id);

    /** 统计引用该类的类关系数（出向或入向） */
    @Select("SELECT COUNT(1) FROM ont_class_link WHERE source_class_id = #{id} OR target_class_id = #{id}")
    int countClassLinkRefs(@Param("id") String id);

    /* —— 级联清理类自有子数据 (删除对象类时一并清除) —— */
    /** 级联删除该类的全部属性 */
    @Delete("DELETE FROM ont_class_property WHERE class_id = #{id}")
    int deletePropertiesByClass(@Param("id") String id);

    /** 级联删除该类的全部数据集绑定 */
    @Delete("DELETE FROM ont_class_ds WHERE class_id = #{id}")
    int deleteClassDsByClass(@Param("id") String id);

    /** 级联删除该类的全部动作 */
    @Delete("DELETE FROM ont_class_action WHERE class_id = #{id}")
    int deleteActionsByClass(@Param("id") String id);

    /** 级联删除该类参与的全部等价/不相交分组关系（双端匹配） */
    @Delete("DELETE FROM ont_class_group WHERE class_id = #{id} OR ref_class_id = #{id}")
    int deleteClassGroupByClass(@Param("id") String id);

    /** 级联删除该类作为父类或子类的全部互斥并集关系 */
    @Delete("DELETE FROM ont_class_disjoint_union WHERE parent_class_id = #{id} OR sub_class_id = #{id}")
    int deleteDisjointUnionByClass(@Param("id") String id);

    /** 级联删除该类所有属性的等价关系 */
    @Delete("DELETE FROM ont_property_equivalent WHERE class_id1 = #{id} OR class_id2 = #{id}")
    int deletePropEquivByClass(@Param("id") String id);

    /** 级联删除该类所有属性的互斥关系 */
    @Delete("DELETE FROM ont_property_disjoint WHERE class_id1 = #{id} OR class_id2 = #{id}")
    int deletePropDisjointByClass(@Param("id") String id);

    /** 级联解除该类实现的所有接口关联 */
    @Delete("DELETE FROM ont_interface_class WHERE class_id = #{id}")
    int deleteInterfaceClassByClass(@Param("id") String id);

    /** 级联删除该类在业务分组表中的引用记录（group_type = object_types） */
    @Delete("DELETE FROM ont_biz_group_class WHERE ref_id = #{id} AND group_type = 'object_types'")
    int deleteGroupRefByClass(@Param("id") String id);

    /** 删除对象类本身（级联清理完成后最后执行） */
    @Delete("DELETE FROM ont_class WHERE id = #{id}")
    int deleteClass(@Param("id") String id);

    /** 写入 类→数据集(主表/附表) 绑定 */
    @Insert("""
        INSERT INTO ont_class_ds(
            id, class_id, ds_code, physical_table, table_label, rel_type, alias,
            pk_keys, join_on_keys, join_type, physical_fields, sort, status
        ) VALUES (
            #{id}, #{class_id}, #{ds_code}, #{physical_table}, #{table_label}, #{rel_type}, #{alias},
            #{pk_keys}, #{join_on_keys}, #{join_type}, #{physical_fields}, #{sort}, #{status}
        )
    """)
    int insertClassDs(Map<String, Object> row);

    /** 更新一条 类→数据集 绑定 (表名/别名/类型/关联键/连接方式/排序/状态; 不动 physical_fields) */
    @Update("""
        UPDATE ont_class_ds SET
            table_label = #{table_label}, alias = #{alias}, rel_type = #{rel_type},
            pk_keys = #{pk_keys}, join_on_keys = #{join_on_keys}, join_type = #{join_type},
            sort = #{sort}, status = #{status}
        WHERE id = #{id}
    """)
    int updateClassDs(Map<String, Object> row);

    /** 删除一条 类→数据集 绑定 */
    @Delete("DELETE FROM ont_class_ds WHERE id = #{id}")
    int deleteClassDs(@Param("id") String id);

    /** 解除引用某 class_ds 的属性映射 (删除绑定前调用, 避免关系图残留脏连线) */
    @Update("UPDATE ont_class_property SET class_ds_id = NULL, physical_table = NULL, physical_column = NULL WHERE class_ds_id = #{dsId}")
    int clearPropMappingsByDs(@Param("dsId") String dsId);

    /** 检索可作为等价/不相交/互斥并集等候选的对象类（按行业/领域过滤） */
    @Select("""
        SELECT id, api_name, display_name, rdfs_label, rdfs_comment, category_code, icon, color, status
        FROM ont_class
        WHERE status = 1
        ORDER BY create_time DESC
    """)
    List<Map<String, Object>> listAllClassesForPicker();
}
