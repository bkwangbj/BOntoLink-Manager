package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 同义词候选 Mapper
 */
@Mapper
public interface SynonymCandidateMapper {

    /**
     * 查询待审核的候选
     */
    @Select("SELECT * FROM sys_synonym_candidate WHERE status = 'PENDING' ORDER BY confidence DESC, evidence_count DESC")
    List<Map<String, Object>> findPending();

    /**
     * 查询所有候选（分页）
     */
    @Select("SELECT * FROM sys_synonym_candidate ORDER BY create_time DESC")
    List<Map<String, Object>> listAll();

    /**
     * 根据 ID 查询
     */
    @Select("SELECT * FROM sys_synonym_candidate WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /**
     * 插入
     */
    @Insert("INSERT INTO sys_synonym_candidate (id, word, synonym, confidence, evidence_count, status, source, create_time) " +
            "VALUES (#{id}, #{word}, #{synonym}, #{confidence}, #{evidence_count}, #{status}, #{source}, datetime('now','localtime'))")
    int insert(Map<String, Object> data);

    /**
     * 更新状态
     */
    @Update("UPDATE sys_synonym_candidate SET status = #{status}, reviewer = #{reviewer}, review_time = datetime('now','localtime') WHERE id = #{id}")
    int updateStatus(@Param("id") String id, @Param("status") String status, @Param("reviewer") String reviewer);

    /**
     * 删除
     */
    @Delete("DELETE FROM sys_synonym_candidate WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 检查是否已存在
     */
    @Select("SELECT COUNT(*) FROM sys_synonym_candidate WHERE word = #{word} AND synonym = #{synonym} AND status = 'PENDING'")
    int existsPending(@Param("word") String word, @Param("synonym") String synonym);

    /**
     * 统计待审核数量
     */
    @Select("SELECT COUNT(*) FROM sys_synonym_candidate WHERE status = 'PENDING'")
    int countPending();
}
