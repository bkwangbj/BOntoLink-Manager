package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/** 类型类一级大类(Kind)字典 Mapper(ont_type_class_category_dict)。 */
@Mapper
public interface TypeClassCategoryMapper {

    /** 大类列表 + 该大类下类型类数量 */
    @Select("""
        SELECT c.*, (SELECT COUNT(*) FROM ont_type_class t WHERE t.category_code = c.category_code) AS tc_count
        FROM ont_type_class_category_dict c
        ORDER BY c.sort_weight, c.category_code
    """)
    List<Map<String, Object>> listWithCount();

    @Select("SELECT * FROM ont_type_class_category_dict WHERE category_code = #{code}")
    Map<String, Object> findByCode(@Param("code") String code);

    @Insert("""
        INSERT INTO ont_type_class_category_dict(category_code, icon, color, category_name_cn,
            global_allow_apply_types, source_type, sort_weight, description)
        VALUES (#{category_code}, #{icon}, #{color}, #{category_name_cn},
            #{global_allow_apply_types}, #{source_type}, #{sort_weight}, #{description})
    """)
    int insert(Map<String, Object> row);

    @Update("""
        UPDATE ont_type_class_category_dict SET
            icon = #{icon}, color = #{color}, category_name_cn = #{category_name_cn},
            global_allow_apply_types = #{global_allow_apply_types}, source_type = #{source_type},
            sort_weight = #{sort_weight}, description = #{description}, updated_at = #{updated_at}
        WHERE category_code = #{category_code}
    """)
    int update(Map<String, Object> row);

    @Delete("DELETE FROM ont_type_class_category_dict WHERE category_code = #{code}")
    int delete(@Param("code") String code);

    /** 该大类下是否仍有类型类(删除前校验) */
    @Select("SELECT COUNT(*) FROM ont_type_class WHERE category_code = #{code}")
    int countTypeClasses(@Param("code") String code);
}
