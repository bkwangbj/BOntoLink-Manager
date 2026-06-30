package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 链接类型 (Link Types) Mapper
 * <p>
 * 支持 ont_link_types 主表 + ont_link_mappings 关联映射表的 CRUD 操作。
 * 列表查询 JOIN ont_class 两次, 一次性返回两端对象的中文/英文名供前端展示。
 */
@Mapper
public interface LinkTypeMapper {

    /** 列表 (JOIN 源/目标对象类的展示名 + 引用次数) */
    @Select("""
        SELECT lt.*,
               lc.display_name AS l_class_name, lc.api_name AS l_class_api,
               lc.icon AS l_class_icon, lc.color AS l_class_color,
               rc.display_name AS r_class_name, rc.api_name AS r_class_api,
               rc.icon AS r_class_icon, rc.color AS r_class_color,
               (SELECT COUNT(1) FROM ont_link_mappings m WHERE m.link_id = lt.id) AS mapping_count,
               (SELECT COUNT(1) FROM ont_type_class_bind tc WHERE tc.link_type_id = lt.id AND tc.applicable_type = 'relation') AS type_class_count
        FROM ont_link_types lt
        LEFT JOIN ont_class lc ON lc.id = lt.l_object_type_id
        LEFT JOIN ont_class rc ON rc.id = lt.r_object_type_id
        ORDER BY lt.updated_at DESC
    """)
    List<Map<String, Object>> listAll();

    /** 按 id 查单条 (JOIN 两端对象类名称) */
    @Select("""
        SELECT lt.*,
               lc.display_name AS l_class_name, lc.api_name AS l_class_api,
               rc.display_name AS r_class_name, rc.api_name AS r_class_api
        FROM ont_link_types lt
        LEFT JOIN ont_class lc ON lc.id = lt.l_object_type_id
        LEFT JOIN ont_class rc ON rc.id = lt.r_object_type_id
        WHERE lt.id = #{id}
    """)
    Map<String, Object> findById(@Param("id") String id);

    /** 检查链接类型编码是否已存在 */
    @Select("SELECT 1 FROM ont_link_types WHERE link_type_id = #{code} LIMIT 1")
    Integer existsByCode(@Param("code") String code);

    /** 新增链接类型 */
    @Insert("""
        INSERT INTO ont_link_types(id, link_type_id, rid, status,
            l_object_type_id, r_object_type_id, l_cardinality, r_cardinality,
            l_display_name, l_plural_name, r_display_name, r_plural_name,
            l_visibility, r_visibility, l_api_name, r_api_name,
            l_enabled, r_enabled, is_data_source_rel, rel_data_table,
            rdfs_label, rdfs_comment, category_code,
            created_by, updated_by)
        VALUES (#{id}, #{link_type_id}, #{rid}, #{status},
            #{l_object_type_id}, #{r_object_type_id}, #{l_cardinality}, #{r_cardinality},
            #{l_display_name}, #{l_plural_name}, #{r_display_name}, #{r_plural_name},
            #{l_visibility}, #{r_visibility}, #{l_api_name}, #{r_api_name},
            #{l_enabled}, #{r_enabled}, #{is_data_source_rel}, #{rel_data_table},
            #{rdfs_label}, #{rdfs_comment}, #{category_code},
            #{created_by}, #{updated_by})
    """)
    int insert(Map<String, Object> row);

    /** 更新链接类型 (同步更新 updated_at) */
    @Update("""
        UPDATE ont_link_types SET
          rid = #{rid},
          status = #{status},
          l_object_type_id = #{l_object_type_id}, r_object_type_id = #{r_object_type_id},
          l_cardinality = #{l_cardinality}, r_cardinality = #{r_cardinality},
          l_display_name = #{l_display_name}, l_plural_name = #{l_plural_name},
          r_display_name = #{r_display_name}, r_plural_name = #{r_plural_name},
          l_visibility = #{l_visibility}, r_visibility = #{r_visibility},
          l_api_name = #{l_api_name}, r_api_name = #{r_api_name},
          l_enabled = #{l_enabled}, r_enabled = #{r_enabled},
          is_data_source_rel = #{is_data_source_rel}, rel_data_table = #{rel_data_table},
          rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
          category_code = #{category_code},
          updated_at = datetime('now','localtime'),
          updated_by = #{updated_by}
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    /** 删除链接类型主记录 */
    @Delete("DELETE FROM ont_link_types WHERE id = #{id}")
    int delete(@Param("id") String id);

    /* —— 关联映射 —— */
    /** 查询某链接类型下所有字段映射 (按 side/seq 排序) */
    @Select("""
        SELECT * FROM ont_link_mappings
        WHERE link_id = #{linkId}
        ORDER BY side, seq
    """)
    List<Map<String, Object>> listMappings(@Param("linkId") String linkId);

    /** 删除某链接类型的全部字段映射 (保存前先清空再批量插入) */
    @Delete("DELETE FROM ont_link_mappings WHERE link_id = #{linkId}")
    int deleteMappingsByLink(@Param("linkId") String linkId);

    /** 新增一条字段映射 */
    @Insert("""
        INSERT INTO ont_link_mappings(mapping_id, link_id, side, seq, object_field, join_table_column)
        VALUES (#{mapping_id}, #{link_id}, #{side}, #{seq}, #{object_field}, #{join_table_column})
    """)
    int insertMapping(Map<String, Object> row);

    /* —— 关联类型类 (relation 类型) —— */
    /** 查询挂在某链接类型下的 relation 类型类绑定列表 (JOIN 元数据取大类/名称) */
    @Select("""
        SELECT b.*,
               m.category_code AS category,
               m.name_cn_base  AS name
        FROM ont_type_class_bind b
        LEFT JOIN ont_type_class m ON m.id = b.type_class_meta_id
        WHERE b.link_type_id = #{linkId} AND b.applicable_type = 'relation'
        ORDER BY m.category_code, m.name_cn_base
    """)
    List<Map<String, Object>> listTypeClasses(@Param("linkId") String linkId);

    /** 删除某链接类型下的全部 relation 类型类绑定 */
    @Delete("DELETE FROM ont_type_class_bind WHERE link_type_id = #{linkId} AND applicable_type = 'relation'")
    int deleteTypeClassesByLink(@Param("linkId") String linkId);
}
