package com.beiktech.bontolink.ontology.model;

import lombok.Data;

/**
 * 同义词条目
 */
@Data
public class SynonymEntry {
    private String originalWord;
    private String synonym;
    private Double confidence;
    private String domain;

    public SynonymEntry() {}

    public SynonymEntry(String originalWord, String synonym, Double confidence) {
        this.originalWord = originalWord;
        this.synonym = synonym;
        this.confidence = confidence;
    }
}
