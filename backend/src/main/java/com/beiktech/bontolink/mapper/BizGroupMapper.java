package com.beiktech.bontolink.mapper;

import com.beiktech.bontolink.entity.BizGroup;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface BizGroupMapper {

    @Select("SELECT * FROM ont_biz_group WHERE parent_id = #{parentId} ORDER BY g_sort")
    List<BizGroup> listByParent(@Param("parentId") String parentId);

    @Insert("""
        INSERT INTO ont_biz_group(id, parent_id, category_code, g_name, g_sort, icon, color, description)
        VALUES(#{id}, #{parentId}, #{categoryCode}, #{gName}, #{gSort}, #{icon}, #{color}, #{description})
    """)
    int insert(BizGroup g);

    @Update("""
        UPDATE ont_biz_group SET
            g_name=#{gName}, g_sort=#{gSort}, icon=#{icon}, color=#{color}, description=#{description}
        WHERE id=#{id}
    """)
    int update(BizGroup g);

    @Delete("DELETE FROM ont_biz_group WHERE id = #{id}")
    int delete(@Param("id") String id);

    @Select("""
        SELECT c.* FROM ont_biz_group_class gc
        JOIN ont_class c ON c.id = gc.class_id
        WHERE gc.group_id = #{groupId}
        ORDER BY gc.g_sort
    """)
    List<Map<String, Object>> listGroupClasses(@Param("groupId") String groupId);

    @Select("SELECT id FROM ont_biz_group WHERE category_code = #{code} LIMIT 1")
    String findGroupIdByCategoryCode(@Param("code") String code);

    @Select("SELECT COALESCE(MAX(g_sort), 0) FROM ont_biz_group_class WHERE category_code = #{code}")
    int maxSortByCategoryCode(@Param("code") String code);

    @Select("SELECT class_id FROM ont_biz_group_class WHERE category_code = #{code} ORDER BY g_sort")
    java.util.List<String> listClassIdsByCategoryCode(@Param("code") String code);

    @Select("SELECT COUNT(*) FROM ont_biz_group_class WHERE category_code = #{code} AND class_id = #{classId}")
    int existsMember(@Param("code") String code, @Param("classId") String classId);

    @Insert("""
        INSERT INTO ont_biz_group_class(id, group_id, class_id, category_code, g_sort)
        VALUES(#{id}, #{groupId}, #{classId}, #{categoryCode}, #{gSort})
    """)
    int insertMember(@Param("id") String id,
                     @Param("groupId") String groupId,
                     @Param("classId") String classId,
                     @Param("categoryCode") String categoryCode,
                     @Param("gSort") int gSort);

    @Delete("DELETE FROM ont_biz_group_class WHERE category_code = #{code} AND class_id = #{classId}")
    int deleteMember(@Param("code") String code, @Param("classId") String classId);

    @Update("UPDATE ont_biz_group_class SET g_sort = #{sort} WHERE category_code = #{code} AND class_id = #{classId}")
    int updateMemberSort(@Param("code") String code, @Param("classId") String classId, @Param("sort") int sort);
}
