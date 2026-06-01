package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 结构属性 (Struct types) Mapper
 * <p>
 * 把一组共享属性组合为业务上有意义的"结构"(姓名 / 地址 / 时间段 / 金额 ...)。
 * struct_types 是结构的元信息,struct_items 是结构内每个共享属性的引用 + 顺序。
 */
@Mapper
public interface StructTypeMapper {

    /** 列表(含子条目数量) */
    @Select("""
        SELECT s.*,
               (SELECT COUNT(1) FROM ont_struct_items i WHERE i.struct_id = s.id) AS item_count
        FROM ont_struct_types s
        ORDER BY s.update_time DESC
    """)
    List<Map<String, Object>> listAll();

    @Select("SELECT * FROM ont_struct_types WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Select("SELECT 1 FROM ont_struct_types WHERE struct_code = #{code} LIMIT 1")
    Integer existsByCode(@Param("code") String code);

    /** 条目列表 (按 sort_no 排序, JOIN 共享属性的标签 + 数据类型) */
    @Select("""
        SELECT i.*,
               sp.prop_code      AS prop_code,
               sp.rdfs_label     AS prop_label,
               sp.data_type      AS prop_data_type,
               sp.value_type     AS prop_value_type,
               sp.is_required    AS prop_is_required
        FROM ont_struct_items i
        LEFT JOIN ont_shared_properties sp ON sp.id = i.prop_id
        WHERE i.struct_id = #{structId}
        ORDER BY i.sort_no, i.id
    """)
    List<Map<String, Object>> listItems(@Param("structId") String structId);

    @Insert("""
        INSERT INTO ont_struct_types(id, struct_code, category_code, status,
            rdfs_label, rdfs_comment, rdfs_see_also, rdfs_defined_by)
        VALUES (#{id}, #{struct_code}, #{category_code}, #{status},
            #{rdfs_label}, #{rdfs_comment}, #{rdfs_see_also}, #{rdfs_defined_by})
    """)
    int insert(Map<String, Object> row);

    @Update("""
        UPDATE ont_struct_types SET
          category_code = #{category_code}, status = #{status},
          rdfs_label = #{rdfs_label}, rdfs_comment = #{rdfs_comment},
          rdfs_see_also = #{rdfs_see_also}, rdfs_defined_by = #{rdfs_defined_by},
          update_time = datetime('now','localtime')
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    @Delete("DELETE FROM ont_struct_types WHERE id = #{id}")
    int delete(@Param("id") String id);

    /* —— 条目操作 —— */
    @Delete("DELETE FROM ont_struct_items WHERE struct_id = #{structId}")
    int deleteItemsByStruct(@Param("structId") String structId);

    @Insert("""
        INSERT INTO ont_struct_items(id, struct_id, sort_no, prop_id)
        VALUES (#{id}, #{struct_id}, #{sort_no}, #{prop_id})
    """)
    int insertItem(Map<String, Object> row);

    @Delete("DELETE FROM ont_struct_items WHERE id = #{id}")
    int deleteItem(@Param("id") String id);
}
