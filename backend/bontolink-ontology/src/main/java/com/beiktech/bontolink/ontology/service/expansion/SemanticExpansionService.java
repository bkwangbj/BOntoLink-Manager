package com.beiktech.bontolink.ontology.service.expansion;

import com.beiktech.bontolink.ontology.model.ExpandedText;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 语义扩充服务（统一编排）
 */
@Slf4j
@Service
public class SemanticExpansionService {

    @Autowired
    private SynonymExpander synonymExpander;

    /**
     * 扩充查询文本
     */
    public ExpandedText expandQuery(String query) {
        return expandQuery(query, null);
    }

    /**
     * 扩充查询文本（指定领域）
     */
    public ExpandedText expandQuery(String query, String domain) {
        if (query == null || query.trim().isEmpty()) {
            ExpandedText empty = new ExpandedText();
            empty.setOriginalText("");
            empty.setExpandedText("");
            return empty;
        }

        log.debug("开始语义扩充: query={}, domain={}", query, domain);

        // 当前只实现同义词扩展
        // 后续可添加：领域术语映射、概念层次扩展
        ExpandedText result = synonymExpander.expand(query, domain);

        log.info("语义扩充完成: {} → {} (新增{}个词)",
            query,
            result.getExpandedText(),
            result.getStats().get("totalSynonyms"));

        return result;
    }
}
