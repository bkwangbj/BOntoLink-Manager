package com.beiktech.bontolink.ontology.service.expansion;

import com.alibaba.fastjson2.JSON;
import com.beiktech.bontolink.base.text.ChineseSegmenter;
import com.beiktech.bontolink.data.mapper.SynonymDictMapper;
import com.beiktech.bontolink.ontology.model.ExpandedText;
import com.beiktech.bontolink.ontology.model.SynonymEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 同义词扩展器
 */
@Slf4j
@Component
public class SynonymExpander {

    @Autowired
    private SynonymDictMapper synonymMapper;

    @Autowired
    private ChineseSegmenter segmenter;

    /**
     * 扩展文本中的同义词
     */
    public ExpandedText expand(String text, String domain) {
        ExpandedText result = new ExpandedText();
        result.setOriginalText(text);

        // 1. 分词
        List<String> tokens = segmenter.segment(text);
        result.setTokens(tokens);

        // 2. 过滤停用词
        List<String> filteredTokens = filterStopWords(tokens);

        // 3. 对每个词查找同义词
        Map<String, List<SynonymEntry>> synonymMap = new LinkedHashMap<>();

        for (String token : filteredTokens) {
            List<SynonymEntry> synonyms = findSynonyms(token, domain);
            if (!synonyms.isEmpty()) {
                synonymMap.put(token, synonyms);
            }
        }

        result.setSynonymMap(synonymMap);

        // 4. 生成扩充文本
        String expandedText = result.getFullExpandedText();
        result.setExpandedText(expandedText);

        log.debug("同义词扩展: {} → {} (新增 {} 个同义词)",
            text, expandedText, synonymMap.values().stream().mapToInt(List::size).sum());

        return result;
    }

    /**
     * 查找单个词的同义词
     */
    @Cacheable(value = "synonyms", key = "#word + '_' + (#domain != null ? #domain : 'ALL')")
    private List<SynonymEntry> findSynonyms(String word, String domain) {
        List<Map<String, Object>> records;

        if (domain != null) {
            // 优先查找指定领域
            records = synonymMapper.findByWordAndDomain(word, domain);
            if (records.isEmpty()) {
                // 降级到通用领域
                records = synonymMapper.findByWordAndDomain(word, "GENERAL");
            }
        } else {
            // 查询所有领域
            records = synonymMapper.findByWord(word);
        }

        List<SynonymEntry> entries = new ArrayList<>();
        for (Map<String, Object> record : records) {
            String synonymsJson = (String) record.get("synonyms");
            if (synonymsJson != null) {
                List<String> synonymList = JSON.parseArray(synonymsJson, String.class);
                Double confidence = ((Number) record.get("confidence")).doubleValue();

                for (String synonym : synonymList) {
                    entries.add(new SynonymEntry(word, synonym, confidence));
                }
            }
        }

        return entries;
    }

    /**
     * 过滤停用词
     */
    private List<String> filterStopWords(List<String> tokens) {
        Set<String> stopWords = getStopWords();
        return tokens.stream()
            .filter(t -> !stopWords.contains(t))
            .filter(t -> t.length() > 1)  // 过滤单字
            .collect(Collectors.toList());
    }

    @Cacheable(value = "stopwords", key = "'all'")
    private Set<String> getStopWords() {
        List<String> words = synonymMapper.listAllStopWords();
        return new HashSet<>(words);
    }
}
