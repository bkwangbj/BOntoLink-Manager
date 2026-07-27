package com.beiktech.bontolink.base.embedding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock Embedding 服务（用于开发测试）
 *
 * 生产环境需替换为：
 * 1. OpenAIEmbeddingService (调用 OpenAI API)
 * 2. LocalEmbeddingService (本地模型如 BGE-M3)
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "bontolink.embedding.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 768;
    private final Random random = new Random(42); // 固定种子保证可重复

    @Override
    public float[] embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[DIMENSION];
        }

        // 基于文本哈希生成伪向量（保证相同文本向量相同）
        int hash = text.hashCode();
        Random r = new Random(hash);

        float[] vector = new float[DIMENSION];
        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = (float) r.nextGaussian();
        }

        // 归一化
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        for (int i = 0; i < DIMENSION; i++) {
            vector[i] /= norm;
        }

        log.debug("Mock embedding for text: {} (length={})", text.substring(0, Math.min(20, text.length())), text.length());
        return vector;
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> result = new ArrayList<>();
        for (String text : texts) {
            result.add(embed(text));
        }
        return result;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
}
