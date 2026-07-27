package com.beiktech.bontolink.base.embedding;

import java.util.List;

/**
 * Embedding 服务接口
 */
public interface EmbeddingService {

    /**
     * 单个文本向量化
     * @param text 文本
     * @return 向量（768维）
     */
    float[] embed(String text);

    /**
     * 批量向量化
     * @param texts 文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     */
    int getDimension();
}
