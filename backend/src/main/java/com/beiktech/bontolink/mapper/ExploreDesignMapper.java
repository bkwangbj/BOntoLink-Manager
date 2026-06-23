package com.beiktech.bontolink.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 实例探索「看板/设计」持久化。
 * name='' 为该对象类型的默认看板(每 class 一条);name 非空为命名设计。
 */
@Mapper
public interface ExploreDesignMapper {

    /** 某对象类型下的命名设计(不含默认看板) */
    @Select("SELECT * FROM ont_explore_design WHERE class_id = #{classId} AND name <> '' ORDER BY updated_at DESC")
    List<Map<String, Object>> listNamedByClass(@Param("classId") String classId);

    /** 某对象类型的默认看板(name='') */
    @Select("SELECT * FROM ont_explore_design WHERE class_id = #{classId} AND name = '' LIMIT 1")
    Map<String, Object> getDefault(@Param("classId") String classId);

    @Select("SELECT * FROM ont_explore_design WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    @Insert("""
        INSERT INTO ont_explore_design(id, class_id, name, kind, config)
        VALUES (#{id}, #{classId}, #{name}, #{kind}, #{config})
    """)
    int insert(Map<String, Object> row);

    /** 覆盖更新 config(及 kind/name),刷新 updated_at */
    @Update("""
        UPDATE ont_explore_design SET
            name = #{name}, kind = #{kind}, config = #{config}, updated_at = #{updatedAt}
        WHERE id = #{id}
    """)
    int update(Map<String, Object> row);

    @Delete("DELETE FROM ont_explore_design WHERE id = #{id}")
    int deleteById(@Param("id") String id);
}
