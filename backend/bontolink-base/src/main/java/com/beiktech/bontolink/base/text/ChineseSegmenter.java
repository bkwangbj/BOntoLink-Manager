package com.beiktech.bontolink.base.text;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 中文分词器（基于结巴分词）
 */
@Slf4j
@Component
public class ChineseSegmenter {

    private final JiebaSegmenter segmenter;

    public ChineseSegmenter() {
        this.segmenter = new JiebaSegmenter();
        log.info("中文分词器初始化完成");
    }

    /**
     * 分词（返回词列表）
     */
    @Cacheable(value = "segmentation", key = "#text")
    public List<String> segment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        return segmenter.process(text, JiebaSegmenter.SegMode.SEARCH)
            .stream()
            .map(token -> token.word)
            .collect(Collectors.toList());
    }

    /**
     * 分词（返回详细信息）
     */
    public List<SegToken> segmentDetail(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        return segmenter.process(text, JiebaSegmenter.SegMode.SEARCH);
    }

    /**
     * 标准化文本
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        // 去除多余空格
        text = text.replaceAll("\\s+", " ");

        // 转小写（仅英文）
        // text = text.toLowerCase();

        // 去除标点符号（可选）
        // text = text.replaceAll("[\\p{Punct}]", "");

        return text.trim();
    }
}
