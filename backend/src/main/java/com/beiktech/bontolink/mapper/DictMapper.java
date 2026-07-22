package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 字典管理 Mapper，覆盖 ont_dict_def + ont_dict_item 两张表。
 */
@Mapper
public interface DictMapper {

    // ==================== 字典定义 CRUD ====================

    @Select("SELECT * FROM ont_dict_def ORDER BY sort_no, create_time")
    List<Map<String, Object>> listDefs();

    @Select("SELECT * FROM ont_dict_def WHERE id = #{id}")
    Map<String, Object> getDef(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_dict_def(id, dict_code, dict_name, rdfs_comment, status, sort_no)
        VALUES (#{id}, #{dictCode,jdbcType=VARCHAR}, #{dictName,jdbcType=VARCHAR},
                #{rdfsComment,jdbcType=VARCHAR}, #{status,jdbcType=INTEGER}, #{sortNo,jdbcType=INTEGER})
    """)
    int insertDef(Map<String, Object> body);

    @Update("""
        UPDATE ont_dict_def SET
            dict_code=#{dictCode,jdbcType=VARCHAR}, dict_name=#{dictName,jdbcType=VARCHAR},
            rdfs_comment=#{rdfsComment,jdbcType=VARCHAR}, status=#{status,jdbcType=INTEGER},
            sort_no=#{sortNo,jdbcType=INTEGER},
            update_time=CURRENT_TIMESTAMP
        WHERE id=#{id}
    """)
    int updateDef(Map<String, Object> body);

    @Delete("DELETE FROM ont_dict_def WHERE id = #{id}")
    int deleteDef(@Param("id") String id);

    // ==================== 字典条目 CRUD ====================

    @Select("SELECT * FROM ont_dict_item WHERE dict_id = #{dictId} ORDER BY sort_no")
    List<Map<String, Object>> listItems(@Param("dictId") String dictId);

    @Select("SELECT * FROM ont_dict_item WHERE id = #{id}")
    Map<String, Object> getItem(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_dict_item(id, dict_id, parent_id, item_code, item_value, sort_no, status, color, ext_data)
        VALUES (#{id}, #{dictId,jdbcType=VARCHAR}, #{parentId,jdbcType=VARCHAR},
                #{itemCode,jdbcType=VARCHAR}, #{itemValue,jdbcType=VARCHAR},
                #{sortNo,jdbcType=INTEGER}, #{status,jdbcType=INTEGER},
                #{color,jdbcType=VARCHAR}, #{extData,jdbcType=VARCHAR})
    """)
    int insertItem(Map<String, Object> body);

    @Update("""
        UPDATE ont_dict_item SET
            parent_id=#{parentId,jdbcType=VARCHAR}, item_code=#{itemCode,jdbcType=VARCHAR},
            item_value=#{itemValue,jdbcType=VARCHAR}, sort_no=#{sortNo,jdbcType=INTEGER},
            status=#{status,jdbcType=INTEGER}, color=#{color,jdbcType=VARCHAR},
            ext_data=#{extData,jdbcType=VARCHAR},
            update_time=CURRENT_TIMESTAMP
        WHERE id=#{id}
    """)
    int updateItem(Map<String, Object> body);

    @Delete("DELETE FROM ont_dict_item WHERE id = #{id}")
    int deleteItem(@Param("id") String id);

    /** 级联删除某字典下的所有条目 */
    @Delete("DELETE FROM ont_dict_item WHERE dict_id = #{dictId}")
    int deleteItemsByDict(@Param("dictId") String dictId);

    // ==================== 树形查询 ====================

    @Select("SELECT * FROM ont_dict_item WHERE parent_id = #{parentId} ORDER BY sort_no")
    List<Map<String, Object>> listItemsByParent(@Param("parentId") String parentId);

    // ==================== 公开查询（供其他模块调用） ====================

    @Select("""
        SELECT i.item_code, i.item_value, i.color, i.ext_data
        FROM ont_dict_item i
        JOIN ont_dict_def d ON d.id = i.dict_id
        WHERE d.dict_code = #{code} AND i.status = 1 AND d.status = 1
        ORDER BY i.sort_no
    """)
    List<Map<String, Object>> getItemsByCode(@Param("code") String code);

    /** 查某字典下所有条目（用于后台组装树） */
    @Select("SELECT * FROM ont_dict_item WHERE dict_id = #{dictId} ORDER BY sort_no")
    List<Map<String, Object>> listAllItems(@Param("dictId") String dictId);
}
