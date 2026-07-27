package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 同义词词典 Mapper
 */
@Mapper
public interface SynonymDictMapper {

    /**
     * 查询所有同义词
     */
    @Select("SELECT * FROM sys_synonym_dict ORDER BY domain, confidence DESC, update_time DESC")
    List<Map<String, Object>> listAll();

    /**
     * 按领域查询
     */
    @Select("SELECT * FROM sys_synonym_dict WHERE domain = #{domain} ORDER BY confidence DESC")
    List<Map<String, Object>> listByDomain(@Param("domain") String domain);

    /**
     * 按主词查询
     */
    @Select("SELECT * FROM sys_synonym_dict WHERE word = #{word}")
    List<Map<String, Object>> findByWord(@Param("word") String word);

    /**
     * 按主词查询同义词（返回 synonyms 字段）
     */
    @Select("SELECT synonyms FROM sys_synonym_dict WHERE word = #{word} LIMIT 1")
    String findSynonymsByWord(@Param("word") String word);

    /**
     * 按主词和领域查询
     */
    @Select("SELECT * FROM sys_synonym_dict WHERE word = #{word} AND domain = #{domain}")
    List<Map<String, Object>> findByWordAndDomain(@Param("word") String word, @Param("domain") String domain);

    /**
     * 根据 ID 查询
     */
    @Select("SELECT * FROM sys_synonym_dict WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /**
     * 插入
     */
    @Insert("INSERT INTO sys_synonym_dict (id, word, synonyms, domain, confidence, source, usage_count, create_time, update_time) " +
            "VALUES (#{id}, #{word}, #{synonyms}, #{domain}, #{confidence}, #{source}, #{usage_count}, datetime('now','localtime'), datetime('now','localtime'))")
    int insert(Map<String, Object> data);

    /**
     * 更新
     */
    @Update("UPDATE sys_synonym_dict SET word = #{word}, synonyms = #{synonyms}, domain = #{domain}, " +
            "confidence = #{confidence}, update_time = datetime('now','localtime') WHERE id = #{id}")
    int update(Map<String, Object> data);

    /**
     * 删除
     */
    @Delete("DELETE FROM sys_synonym_dict WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 增加使用次数
     */
    @Update("UPDATE sys_synonym_dict SET usage_count = usage_count + 1 WHERE id = #{id}")
    int incrementUsageCount(@Param("id") String id);

    /**
     * 查询停用词
     */
    @Select("SELECT word FROM sys_stopwords")
    List<String> listAllStopWords();
}
