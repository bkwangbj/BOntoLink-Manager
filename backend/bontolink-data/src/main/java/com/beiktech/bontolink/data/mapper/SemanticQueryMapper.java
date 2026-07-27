package com.beiktech.bontolink.data.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 语义查询 Mapper
 */
@Mapper
public interface SemanticQueryMapper {

    /**
     * 通过 RID 查询本体类
     */
    @Select("SELECT * FROM ont_class WHERE rid = #{rid}")
    Map<String, Object> findByRid(String rid);

    /**
     * 批量 RID 查询
     */
    @Select("<script>" +
            "SELECT * FROM ont_class WHERE rid IN " +
            "<foreach collection='rids' item='rid' open='(' separator=',' close=')'>" +
            "#{rid}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> findBatchByRid(@Param("rids") List<String> rids);

    /**
     * 查询父类列表
     */
    @Select("SELECT c.* FROM ont_class c " +
            "INNER JOIN ont_class_hierarchy h ON c.id = h.parent_class_id " +
            "WHERE h.child_class_id = #{classId} " +
            "ORDER BY h.hierarchy_level")
    List<Map<String, Object>> findParents(String classId);

    /**
     * 查询子类列表
     */
    @Select("SELECT c.* FROM ont_class c " +
            "INNER JOIN ont_class_hierarchy h ON c.id = h.child_class_id " +
            "WHERE h.parent_class_id = #{classId} " +
            "ORDER BY h.hierarchy_level")
    List<Map<String, Object>> findChildren(String classId);

    /**
     * 递归查询所有祖先类（到根节点）
     */
    @Select("<script>" +
            "WITH RECURSIVE ancestors AS (" +
            "  SELECT parent_class_id, child_class_id, hierarchy_level, 1 as depth " +
            "  FROM ont_class_hierarchy WHERE child_class_id = #{classId} " +
            "  UNION ALL " +
            "  SELECT h.parent_class_id, h.child_class_id, h.hierarchy_level, a.depth + 1 " +
            "  FROM ont_class_hierarchy h " +
            "  INNER JOIN ancestors a ON h.child_class_id = a.parent_class_id " +
            "  WHERE a.depth &lt; 10 " +
            ") " +
            "SELECT DISTINCT c.* FROM ont_class c " +
            "INNER JOIN ancestors a ON c.id = a.parent_class_id " +
            "ORDER BY c.display_name" +
            "</script>")
    List<Map<String, Object>> findAllAncestors(String classId);

    /**
     * 递归查询所有后代类（到叶子节点）
     */
    @Select("<script>" +
            "WITH RECURSIVE descendants AS (" +
            "  SELECT parent_class_id, child_class_id, hierarchy_level, 1 as depth " +
            "  FROM ont_class_hierarchy WHERE parent_class_id = #{classId} " +
            "  UNION ALL " +
            "  SELECT h.parent_class_id, h.child_class_id, h.hierarchy_level, d.depth + 1 " +
            "  FROM ont_class_hierarchy h " +
            "  INNER JOIN descendants d ON h.parent_class_id = d.child_class_id " +
            "  WHERE d.depth &lt; 10 " +
            ") " +
            "SELECT DISTINCT c.* FROM ont_class c " +
            "INNER JOIN descendants d ON c.id = d.child_class_id " +
            "ORDER BY c.display_name" +
            "</script>")
    List<Map<String, Object>> findAllDescendants(String classId);

    /**
     * 根据关键词模糊搜索本体类（支持同义词扩展）
     */
    @Select("<script>" +
            "SELECT DISTINCT c.*, " +
            "  CASE " +
            "    WHEN c.display_name LIKE CONCAT('%', #{keyword}, '%') THEN 10 " +
            "    WHEN c.api_name LIKE CONCAT('%', #{keyword}, '%') THEN 8 " +
            "    WHEN c.rdfs_label LIKE CONCAT('%', #{keyword}, '%') THEN 6 " +
            "    WHEN c.rdfs_comment LIKE CONCAT('%', #{keyword}, '%') THEN 4 " +
            "    ELSE 1 " +
            "  END as match_score " +
            "FROM ont_class c " +
            "WHERE c.display_name LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR c.api_name LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR c.rdfs_label LIKE CONCAT('%', #{keyword}, '%') " +
            "  OR c.rdfs_comment LIKE CONCAT('%', #{keyword}, '%') " +
            "<if test='synonyms != null and synonyms.size() > 0'>" +
            "  <foreach collection='synonyms' item='syn' separator=' '>" +
            "    OR c.display_name LIKE CONCAT('%', #{syn}, '%') " +
            "    OR c.rdfs_label LIKE CONCAT('%', #{syn}, '%') " +
            "  </foreach>" +
            "</if>" +
            "ORDER BY match_score DESC, c.display_name " +
            "LIMIT #{limit}" +
            "</script>")
    List<Map<String, Object>> searchWithSynonyms(@Param("keyword") String keyword,
                                                  @Param("synonyms") List<String> synonyms,
                                                  @Param("limit") int limit);

    /**
     * 查询同义词（用于关键词扩展）
     */
    @Select("SELECT synonyms FROM sys_synonym_dict WHERE word = #{word}")
    String findSynonyms(String word);

    /**
     * 查询领域术语的常用表达（用于关键词扩展）
     */
    @Select("SELECT common_terms FROM ont_domain_term WHERE standard_term = #{term}")
    String findCommonTerms(String term);

    /**
     * 模糊匹配同义词词典
     */
    @Select("SELECT word, synonyms, domain, confidence FROM sys_synonym_dict " +
            "WHERE word LIKE CONCAT('%', #{keyword}, '%') " +
            "ORDER BY confidence DESC, domain " +
            "LIMIT 10")
    List<Map<String, Object>> findSynonymsByKeyword(String keyword);
}
