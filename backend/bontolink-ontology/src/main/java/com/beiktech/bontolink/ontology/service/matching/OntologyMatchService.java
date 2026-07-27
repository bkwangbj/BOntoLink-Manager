package com.beiktech.bontolink.ontology.service.matching;

import com.beiktech.bontolink.data.mapper.OntologyMapper;
import com.beiktech.bontolink.ontology.model.EntityMatch;
import com.beiktech.bontolink.ontology.model.ExpandedText;
import com.beiktech.bontolink.ontology.model.SynonymEntry;
import com.beiktech.bontolink.ontology.service.OntologyModelManager;
import com.beiktech.bontolink.ontology.service.expansion.SemanticExpansionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 本体匹配服务
 * <p>
 * 使用 OntologyModelManager 管理 Jena OntModel 生命周期：
 * - 查询自动触发版本检查，落后时异步重建
 * - 支持关键词匹配 + OWL 推理增强匹配
 */
@Slf4j
@Service
public class OntologyMatchService {

    private final SemanticExpansionService expansionService;
    private final OntologyMapper ontologyMapper;
    private final OntologyModelManager modelManager;

    public OntologyMatchService(SemanticExpansionService expansionService,
                                OntologyMapper ontologyMapper,
                                OntologyModelManager modelManager) {
        this.expansionService = expansionService;
        this.ontologyMapper = ontologyMapper;
        this.modelManager = modelManager;
    }

    /**
     * 匹配实体
     * 先做语义扩充 → 再从 OntModel 推理匹配 → 最后关键词兜底
     */
    public List<EntityMatch> match(String query, Integer topK, Double threshold) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int limit = topK != null ? topK : 10;
        double minScore = threshold != null ? threshold : 0.6;

        log.info("开始匹配: query={}, topK={}, threshold={}", query, limit, minScore);

        // 1. 语义扩充
        ExpandedText expanded = expansionService.expandQuery(query);
        String expandedQuery = expanded.getExpandedText();

        // 2. 从 OntModel 推理匹配（基于 OWL 层次 + 推理器）
        List<EntityMatch> matches = ontologyMatch(expandedQuery, limit);

        // 3. 如果 OntModel 结果不足，用关键词兜底
        if (matches.size() < limit) {
            List<EntityMatch> keywordResults = keywordMatch(expandedQuery, limit);
            // 合并，去重
            Set<String> existingIds = matches.stream()
                    .map(EntityMatch::getEntityId)
                    .collect(Collectors.toSet());
            for (EntityMatch km : keywordResults) {
                if (!existingIds.contains(km.getEntityId())) {
                    matches.add(km);
                }
            }
        }

        // 4. 过滤低分
        matches = matches.stream()
            .filter(m -> m.getScore() >= minScore)
            .collect(Collectors.toList());

        // 5. 添加匹配原因
        for (EntityMatch match : matches) {
            match.setMatchReason(generateMatchReason(match, expanded));
        }

        log.info("匹配完成，返回 {} 个结果 (OntModel: {})", matches.size(),
                matches.stream().filter(m -> "ONTOLOGY".equals(m.getMatchType())).count());

        return matches;
    }

    /**
     * 基于 OntModel 的本体推理匹配
     * 利用 OWL 层次结构（子类/父类）和推理器增强召回
     */
    private List<EntityMatch> ontologyMatch(String query, int limit) {
        List<EntityMatch> results = new ArrayList<>();

        try {
            OntModel model = modelManager.getModel();
            if (model == null) {
                log.warn("OntModel 未就绪，跳过推理匹配");
                return results;
            }

            String[] keywords = query.toLowerCase().split("\\s+");

            // 遍历 OntModel 中的所有类
            ExtendedIterator<OntClass> classes = model.listClasses();
            while (classes.hasNext() && results.size() < limit * 2) {
                OntClass ontClass = classes.next();

                // 跳过 OWL 内置类
                if (ontClass.isAnon()) continue;
                String uri = ontClass.getURI();
                if (uri == null || uri.contains("/owl#") || uri.contains("/rdf#") || uri.contains("/rdfs#")) continue;

                String localName = uri.contains("#") ? uri.substring(uri.indexOf('#') + 1) : uri;
                String label = ontClass.getLabel("zh");
                if (label == null) label = localName;

                // 计算分数
                double score = calculateOntModelScore(ontClass, localName, label, keywords);

                if (score > 0) {
                    EntityMatch match = new EntityMatch();
                    match.setEntityId(localName);
                    match.setEntityName(label);
                    match.setScore(score);
                    match.setMatchType("ONTOLOGY");

                    // 如果有推理器加持，加分
                    match.setRuleScore(score * 0.3);
                    match.setScore(Math.min(1.0, match.getScore() + match.getRuleScore()));

                    results.add(match);
                }
            }
            classes.close();

        } catch (Exception e) {
            log.warn("OntModel 匹配失败，降级到关键词匹配: {}", e.getMessage());
        }

        return results.stream()
                .sorted(Comparator.comparingDouble(EntityMatch::getScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 基于 OntModel 计算匹配分数
     * 考虑：标签匹配 + 子类/父类推理 + 属性关联
     */
    private double calculateOntModelScore(OntClass ontClass, String localName,
                                           String label, String[] keywords) {
        double score = 0.0;
        int totalKeywords = keywords.length;
        if (totalKeywords == 0) return 0.0;

        for (String keyword : keywords) {
            if (keyword.trim().isEmpty()) continue;

            // 类名匹配
            if (localName.equalsIgnoreCase(keyword)) {
                score += 1.0;
                continue;
            }
            if (localName.toLowerCase().contains(keyword)) {
                score += 0.6;
                continue;
            }

            // 标签匹配
            if (label != null && label.toLowerCase().contains(keyword)) {
                score += 0.5;
                continue;
            }

            // 父类匹配（推理器已推理出层次）
            if (ontClass.hasSuperClass()) {
                ExtendedIterator<OntClass> supers = ontClass.listSuperClasses();
                while (supers.hasNext()) {
                    OntClass superCls = supers.next();
                    String superUri = superCls.getURI();
                    if (superUri != null && superUri.toLowerCase().contains(keyword)) {
                        score += 0.3;
                        break;
                    }
                }
                supers.close();
            }

            // 子类匹配
            if (ontClass.hasSubClass()) {
                ExtendedIterator<OntClass> subs = ontClass.listSubClasses();
                while (subs.hasNext()) {
                    OntClass subCls = subs.next();
                    String subUri = subCls.getURI();
                    if (subUri != null && subUri.toLowerCase().contains(keyword)) {
                        score += 0.2;
                        break;
                    }
                }
                subs.close();
            }
        }

        return totalKeywords > 0 ? score / totalKeywords : 0.0;
    }

    /**
     * 关键词匹配（基于 SQL LIKE 兜底）
     */
    private List<EntityMatch> keywordMatch(String query, int limit) {
        List<Map<String, Object>> entities = ontologyMapper.listClasses();
        List<EntityMatch> matches = new ArrayList<>();

        String[] keywords = query.split("\\s+");

        for (Map<String, Object> entity : entities) {
            String id = (String) entity.get("id");
            String name = (String) entity.get("display_name");
            String description = (String) entity.get("description");

            if (name == null) continue;

            double score = calculateScore(name, description, keywords);

            if (score > 0) {
                EntityMatch match = new EntityMatch();
                match.setEntityId(id);
                match.setEntityName(name);
                match.setScore(score);
                match.setMatchType("KEYWORD");
                match.setKeywordScore(score);
                matches.add(match);
            }
        }

        return matches.stream()
            .sorted(Comparator.comparingDouble(EntityMatch::getScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 计算关键词匹配分数
     */
    private double calculateScore(String name, String description, String[] keywords) {
        double score = 0.0;
        int totalKeywords = keywords.length;

        for (String keyword : keywords) {
            if (keyword.trim().isEmpty()) continue;

            if (name.equalsIgnoreCase(keyword)) {
                score += 1.0;
            } else if (name.toLowerCase().contains(keyword.toLowerCase())) {
                score += 0.5;
            } else if (description != null && description.toLowerCase().contains(keyword.toLowerCase())) {
                score += 0.2;
            }
        }

        return totalKeywords > 0 ? score / totalKeywords : 0.0;
    }

    /**
     * 生成匹配原因说明
     */
    private String generateMatchReason(EntityMatch match, ExpandedText expanded) {
        List<String> reasons = new ArrayList<>();

        if ("ONTOLOGY".equals(match.getMatchType())) {
            reasons.add("本体推理匹配");

            // 推理增强
            if (match.getRuleScore() != null && match.getRuleScore() > 0) {
                reasons.add("含层次推理");
            }
            return String.join(", ", reasons);
        }

        // 同义词匹配
        int synonymCount = 0;
        for (Map.Entry<String, List<SynonymEntry>> entry
                : expanded.getSynonymMap().entrySet()) {
            if (match.getEntityName() != null && match.getEntityName().contains(entry.getKey())) {
                synonymCount++;
            }
        }

        if (synonymCount > 0) {
            reasons.add("同义词匹配(" + synonymCount + "个)");
        }

        reasons.add("关键词匹配");

        return String.join(", ", reasons);
    }
}
