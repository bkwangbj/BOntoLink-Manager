package com.beiktech.bontolink.service.semantic;

import com.alibaba.fastjson2.JSON;
import com.beiktech.bontolink.data.mapper.SemanticQueryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 语义查询 Service
 */
@Slf4j
@Service
public class SemanticQueryService {

    @Autowired
    private SemanticQueryMapper semanticQueryMapper;

    /**
     * 查询扩展
     * 输入用户自然语言，返回扩展后的关键词和同义词
     */
    public Map<String, Object> expandQuery(String query) {
        Map<String, Object> result = new HashMap<>();
        result.put("originalQuery", query);

        // 1. 分词（简单按空格分割，实际可用分词器）
        String[] words = query.trim().split("\\s+");
        List<String> keywords = new ArrayList<>(Arrays.asList(words));
        result.put("keywords", keywords);

        // 2. 查询同义词
        Set<String> expandedTerms = new HashSet<>(keywords);
        for (String word : keywords) {
            // 精确匹配同义词
            String synonymsJson = semanticQueryMapper.findSynonyms(word);
            if (synonymsJson != null) {
                List<String> syns = JSON.parseArray(synonymsJson, String.class);
                expandedTerms.addAll(syns);
            }

            // 模糊匹配同义词词典
            List<Map<String, Object>> fuzzyMatches = semanticQueryMapper.findSynonymsByKeyword(word);
            for (Map<String, Object> match : fuzzyMatches) {
                String matchSynsJson = (String) match.get("synonyms");
                if (matchSynsJson != null) {
                    List<String> matchSyns = JSON.parseArray(matchSynsJson, String.class);
                    expandedTerms.addAll(matchSyns);
                }
            }

            // 查询领域术语常用表达
            String commonTermsJson = semanticQueryMapper.findCommonTerms(word);
            if (commonTermsJson != null) {
                List<String> commonTerms = JSON.parseArray(commonTermsJson, String.class);
                expandedTerms.addAll(commonTerms);
            }
        }

        result.put("expandedTerms", new ArrayList<>(expandedTerms));
        result.put("expansionCount", expandedTerms.size() - keywords.size());

        return result;
    }

    /**
     * 智能本体匹配
     * 根据用户查询，使用同义词扩展后匹配最相关的本体类
     */
    public List<Map<String, Object>> matchOntology(String query, int limit) {
        // 1. 先扩展查询
        Map<String, Object> expansion = expandQuery(query);
        @SuppressWarnings("unchecked")
        List<String> expandedTerms = (List<String>) expansion.get("expandedTerms");

        // 2. 用扩展后的关键词搜索本体
        List<Map<String, Object>> matches = new ArrayList<>();
        for (String keyword : expandedTerms) {
            List<String> synonyms = new ArrayList<>(expandedTerms);
            synonyms.remove(keyword);

            List<Map<String, Object>> results = semanticQueryMapper.searchWithSynonyms(
                keyword, synonyms, limit
            );
            matches.addAll(results);
        }

        // 3. 去重并按评分排序
        Map<String, Map<String, Object>> uniqueMatches = new LinkedHashMap<>();
        for (Map<String, Object> match : matches) {
            String id = (String) match.get("id");
            if (!uniqueMatches.containsKey(id)) {
                uniqueMatches.put(id, match);
            } else {
                // 合并评分（取最高分）
                Map<String, Object> existing = uniqueMatches.get(id);
                Object existingScore = existing.get("match_score");
                Object newScore = match.get("match_score");
                if (existingScore != null && newScore != null) {
                    int existingVal = ((Number) existingScore).intValue();
                    int newVal = ((Number) newScore).intValue();
                    if (newVal > existingVal) {
                        existing.put("match_score", newScore);
                    }
                }
            }
        }

        return uniqueMatches.values().stream()
            .sorted((a, b) -> {
                Object scoreA = a.get("match_score");
                Object scoreB = b.get("match_score");
                if (scoreA == null) return 1;
                if (scoreB == null) return -1;
                return Integer.compare(
                    ((Number) scoreB).intValue(),
                    ((Number) scoreA).intValue()
                );
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 通过 RID 查询本体
     */
    public Map<String, Object> findByRid(String rid) {
        return semanticQueryMapper.findByRid(rid);
    }

    /**
     * 批量 RID 查询
     */
    public List<Map<String, Object>> findBatchByRid(List<String> rids) {
        if (rids == null || rids.isEmpty()) {
            return Collections.emptyList();
        }
        return semanticQueryMapper.findBatchByRid(rids);
    }

    /**
     * 查询父类列表
     */
    public List<Map<String, Object>> findParents(String classId) {
        return semanticQueryMapper.findParents(classId);
    }

    /**
     * 查询子类列表
     */
    public List<Map<String, Object>> findChildren(String classId) {
        return semanticQueryMapper.findChildren(classId);
    }

    /**
     * 获取完整层级树
     */
    public Map<String, Object> getHierarchyTree(String classId) {
        Map<String, Object> tree = new HashMap<>();

        // 当前节点
        Map<String, Object> current = semanticQueryMapper.findByRid(classId);
        if (current == null) {
            // 如果不是 RID，尝试作为 ID 查询
            current = new HashMap<>();
            current.put("id", classId);
        }
        tree.put("current", current);

        // 祖先路径（到根节点）
        List<Map<String, Object>> ancestors = semanticQueryMapper.findAllAncestors(classId);
        tree.put("ancestors", ancestors);
        tree.put("ancestorCount", ancestors.size());

        // 后代树（到叶子节点）
        List<Map<String, Object>> descendants = semanticQueryMapper.findAllDescendants(classId);
        tree.put("descendants", descendants);
        tree.put("descendantCount", descendants.size());

        // 直接父类
        List<Map<String, Object>> parents = semanticQueryMapper.findParents(classId);
        tree.put("parents", parents);

        // 直接子类
        List<Map<String, Object>> children = semanticQueryMapper.findChildren(classId);
        tree.put("children", children);

        return tree;
    }
}
