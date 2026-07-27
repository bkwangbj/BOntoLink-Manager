package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 领域术语映射 Mapper
 */
@Mapper
public interface DomainTermMapper {

    /**
     * 查询所有术语
     */
    @Select("SELECT * FROM ont_domain_term ORDER BY domain, similarity DESC")
    List<Map<String, Object>> listAll();

    /**
     * 按标准术语查询
     */
    @Select("SELECT * FROM ont_domain_term WHERE standard_term = #{standardTerm}")
    List<Map<String, Object>> findByStandardTerm(@Param("standardTerm") String standardTerm);

    /**
     * 按标准术语和领域查询
     */
    @Select("SELECT * FROM ont_domain_term WHERE standard_term = #{standardTerm} AND domain = #{domain}")
    List<Map<String, Object>> findByStandardTermAndDomain(
        @Param("standardTerm") String standardTerm,
        @Param("domain") String domain
    );

    /**
     * 搜索通用术语（模糊匹配）
     */
    @Select("SELECT * FROM ont_domain_term WHERE common_terms LIKE CONCAT('%', #{keyword}, '%')")
    List<Map<String, Object>> searchCommonTerms(@Param("keyword") String keyword);

    /**
     * 根据 ID 查询
     */
    @Select("SELECT * FROM ont_domain_term WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") String id);

    /**
     * 插入
     */
    @Insert("INSERT INTO ont_domain_term (id, standard_term, common_terms, domain, term_type, similarity, context, usage_count, source, create_time, update_time) " +
            "VALUES (#{id}, #{standard_term}, #{common_terms}, #{domain}, #{term_type}, #{similarity}, #{context}, #{usage_count}, #{source}, datetime('now','localtime'), datetime('now','localtime'))")
    int insert(Map<String, Object> data);

    /**
     * 更新
     */
    @Update("UPDATE ont_domain_term SET standard_term = #{standard_term}, common_terms = #{common_terms}, " +
            "domain = #{domain}, term_type = #{term_type}, similarity = #{similarity}, context = #{context}, " +
            "update_time = datetime('now','localtime') WHERE id = #{id}")
    int update(Map<String, Object> data);

    /**
     * 删除
     */
    @Delete("DELETE FROM ont_domain_term WHERE id = #{id}")
    int deleteById(@Param("id") String id);

    /**
     * 增加使用次数
     */
    @Update("UPDATE ont_domain_term SET usage_count = usage_count + 1 WHERE id = #{id}")
    int incrementUsageCount(@Param("id") String id);
}
