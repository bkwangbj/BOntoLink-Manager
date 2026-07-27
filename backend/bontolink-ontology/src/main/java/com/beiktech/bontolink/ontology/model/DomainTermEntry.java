package com.beiktech.bontolink.ontology.model;

import lombok.Data;

/**
 * 领域术语条目
 */
@Data
public class DomainTermEntry {
    private String standardTerm;
    private String commonTerm;
    private Double similarity;
    private String termType;
    private String context;

    public DomainTermEntry() {}

    public DomainTermEntry(String standardTerm, String commonTerm, Double similarity) {
        this.standardTerm = standardTerm;
        this.commonTerm = commonTerm;
        this.similarity = similarity;
    }
}
