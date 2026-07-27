package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 概念层次 Mapper
 */
@Mapper
public interface HierarchyMapper {

    /**
     * 查询所有层次关系
     */
    @Select("SELECT * FROM ont_class_hierarchy ORDER BY hierarchy_level")
    List<Map<String, Object>> listAll();

    /**
     * 查询父类
     */
    @Select("SELECT h.*, c.display_name as parent_name FROM ont_class_hierarchy h " +
            "LEFT JOIN ont_class c ON h.parent_class_id = c.id " +
            "WHERE h.child_class_id = #{classId}")
    List<Map<String, Object>> findParents(@Param("classId") String classId);

    /**
     * 查询子类
     */
    @Select("SELECT h.*, c.display_name as child_name FROM ont_class_hierarchy h " +
            "LEFT JOIN ont_class c ON h.child_class_id = c.id " +
            "WHERE h.parent_class_id = #{classId}")
    List<Map<String, Object>> findChildren(@Param("classId") String classId);

    /**
     * 查询兄弟类（同父）
     */
    @Select("SELECT h2.child_class_id, c.display_name FROM ont_class_hierarchy h1 " +
            "JOIN ont_class_hierarchy h2 ON h1.parent_class_id = h2.parent_class_id " +
            "LEFT JOIN ont_class c ON h2.child_class_id = c.id " +
            "WHERE h1.child_class_id = #{classId} AND h2.child_class_id != #{classId}")
    List<Map<String, Object>> findSiblings(@Param("classId") String classId);

    /**
     * 插入
     */
    @Insert("INSERT INTO ont_class_hierarchy (id, child_class_id, parent_class_id, hierarchy_level, relationship_type, create_time) " +
            "VALUES (#{id}, #{child_class_id}, #{parent_class_id}, #{hierarchy_level}, #{relationship_type}, datetime('now','localtime'))")
    int insert(Map<String, Object> data);

    /**
     * 删除
     */
    @Delete("DELETE FROM ont_class_hierarchy WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 检查关系是否存在
     */
    @Select("SELECT COUNT(*) FROM ont_class_hierarchy WHERE child_class_id = #{childId} AND parent_class_id = #{parentId}")
    int existsRelation(@Param("childId") String childId, @Param("parentId") String parentId);
}
