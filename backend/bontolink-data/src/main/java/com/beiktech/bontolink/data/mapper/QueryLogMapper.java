package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 查询日志 Mapper
 */
@Mapper
public interface QueryLogMapper {

    /**
     * 插入日志
     */
    @Insert("INSERT INTO sys_query_log (id, user_id, query_text, matched_entity_id, match_score, user_clicked, session_id, query_time) " +
            "VALUES (#{id}, #{user_id}, #{query_text}, #{matched_entity_id}, #{match_score}, #{user_clicked}, #{session_id}, datetime('now','localtime'))")
    int insertLog(Map<String, Object> data);

    /**
     * 更新点击状态
     */
    @Update("UPDATE sys_query_log SET user_clicked = 1 WHERE id = #{id}")
    int updateClicked(@Param("id") String id);

    /**
     * 查询指定日期的点击记录
     */
    @Select("SELECT * FROM sys_query_log WHERE DATE(query_time) = #{date} AND user_clicked = 1")
    List<Map<String, Object>> findClickedQueries(@Param("date") String date);

    /**
     * 查询高频查询词（Top N）
     */
    @Select("SELECT query_text, COUNT(*) as count FROM sys_query_log " +
            "WHERE query_time >= datetime('now', '-7 days') " +
            "GROUP BY query_text ORDER BY count DESC LIMIT #{limit}")
    List<Map<String, Object>> findTopQueries(@Param("limit") int limit);

    /**
     * 清理过期日志
     */
    @Delete("DELETE FROM sys_query_log WHERE query_time < datetime('now', #{days} || ' days')")
    int deleteOlderThan(@Param("days") int days);

    /**
     * 统计日志数量
     */
    @Select("SELECT COUNT(*) FROM sys_query_log")
    int count();

    /**
     * 统计昨日查询量
     */
    @Select("SELECT COUNT(*) FROM sys_query_log WHERE DATE(query_time) = DATE('now', '-1 day')")
    int countYesterday();
}
