package com.beiktech.bontolink.ontology.model;

import lombok.Data;
import java.util.*;

/**
 * 扩充文本结果
 */
@Data
public class ExpandedText {
    private String originalText;
    private List<String> tokens = new ArrayList<>();
    private Map<String, List<SynonymEntry>> synonymMap = new LinkedHashMap<>();
    private List<DomainTermEntry> domainTerms = new ArrayList<>();
    private List<String> hierarchyTerms = new ArrayList<>();
    private Map<String, List<String>> fieldTerms = new LinkedHashMap<>();
    private String expandedText;

    /**
     * 添加同义词
     */
    public void addSynonyms(Map<String, List<SynonymEntry>> synonyms) {
        this.synonymMap.putAll(synonyms);
    }

    /**
     * 添加领域术语
     */
    public void addDomainTerms(List<DomainTermEntry> terms) {
        this.domainTerms.addAll(terms);
    }

    /**
     * 添加层次术语
     */
    public void addHierarchyTerms(List<String> terms) {
        this.hierarchyTerms.addAll(terms);
    }

    /**
     * 添加字段术语
     */
    public void addFieldTerms(Map<String, List<String>> terms) {
        this.fieldTerms.putAll(terms);
    }

    /**
     * 生成完整扩充文本
     */
    public String getFullExpandedText() {
        if (expandedText != null) {
            return expandedText;
        }

        StringBuilder sb = new StringBuilder(originalText);

        // 添加同义词
        synonymMap.values().forEach(entries ->
            entries.forEach(entry -> sb.append(" ").append(entry.getSynonym()))
        );

        // 添加领域术语
        domainTerms.forEach(term -> sb.append(" ").append(term.getCommonTerm()));

        // 添加层次术语
        hierarchyTerms.forEach(term -> sb.append(" ").append(term));

        // 添加字段术语
        fieldTerms.values().forEach(terms ->
            terms.forEach(term -> sb.append(" ").append(term))
        );

        expandedText = sb.toString().trim();
        return expandedText;
    }

    /**
     * 统计信息
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalSynonyms", synonymMap.values().stream().mapToInt(List::size).sum());
        stats.put("totalDomainTerms", domainTerms.size());
        stats.put("totalHierarchyTerms", hierarchyTerms.size());
        stats.put("totalFieldTerms", fieldTerms.values().stream().mapToInt(List::size).sum());
        return stats;
    }
}

