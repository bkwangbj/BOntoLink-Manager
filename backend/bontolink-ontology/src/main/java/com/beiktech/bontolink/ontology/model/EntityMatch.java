package com.beiktech.bontolink.ontology.model;

import lombok.Data;

/**
 * 实体匹配结果
 */
@Data
public class EntityMatch {
    private String entityId;
    private String entityName;
    private Double score;
    private String matchType;  // VECTOR/KEYWORD/RULE/FUSED
    private String matchReason;

    // 详细分数
    private Double vectorScore;
    private Double keywordScore;
    private Double ruleScore;

    public EntityMatch() {}

    public EntityMatch(String entityId, String entityName, Double score, String matchType) {
        this.entityId = entityId;
        this.entityName = entityName;
        this.score = score;
        this.matchType = matchType;
    }
}
