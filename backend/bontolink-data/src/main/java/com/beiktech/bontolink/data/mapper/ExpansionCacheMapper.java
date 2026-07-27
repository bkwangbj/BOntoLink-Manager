package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 扩充缓存 Mapper
 */
@Mapper
public interface ExpansionCacheMapper {

    /**
     * 根据 class_id 查询
     */
    @Select("SELECT * FROM ont_class_expansion WHERE class_id = #{classId}")
    Map<String, Object> findByClassId(@Param("classId") String classId);

    /**
     * 插入或更新
     */
    @Insert("INSERT INTO ont_class_expansion (class_id, original_text, expanded_text, expansion_detail, token_count, expansion_version, last_update) " +
            "VALUES (#{class_id}, #{original_text}, #{expanded_text}, #{expansion_detail}, #{token_count}, #{expansion_version}, datetime('now','localtime')) " +
            "ON CONFLICT(class_id) DO UPDATE SET " +
            "original_text = #{original_text}, expanded_text = #{expanded_text}, expansion_detail = #{expansion_detail}, " +
            "token_count = #{token_count}, expansion_version = expansion_version + 1, last_update = datetime('now','localtime')")
    int insertOrUpdate(Map<String, Object> data);

    /**
     * 删除
     */
    @Delete("DELETE FROM ont_class_expansion WHERE class_id = #{classId}")
    int deleteByClassId(@Param("classId") String classId);

    /**
     * 清空所有缓存
     */
    @Delete("DELETE FROM ont_class_expansion")
    int deleteAll();

    /**
     * 统计缓存数量
     */
    @Select("SELECT COUNT(*) FROM ont_class_expansion")
    int count();

    /**
     * 查询最近更新的缓存
     */
    @Select("SELECT * FROM ont_class_expansion ORDER BY last_update DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentUpdated(@Param("limit") int limit);
}
