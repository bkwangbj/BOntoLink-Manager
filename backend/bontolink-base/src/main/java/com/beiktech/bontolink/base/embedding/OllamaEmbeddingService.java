package com.beiktech.bontolink.base.embedding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Ollama Embedding 服务（参考 watf-ai AiEmbedding.getQwen3Embedding 实现）
 * 适用于内网 Ollama 部署的模型，如 bge-m3
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "bontolink.embedding.provider", havingValue = "ollama")
public class OllamaEmbeddingService implements EmbeddingService {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    @Value("${bontolink.ollama.base-url:http://localhost:11434/api/embeddings}")
    private String baseUrl;

    @Value("${bontolink.ollama.model:bge-m3}")
    private String model;

    @Value("${bontolink.ollama.dimension:1024}")
    private int dimension;

    @Value("${bontolink.ollama.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${bontolink.ollama.batch-concurrency:4}")
    private int batchConcurrency;

    // @Value 字段注入完成后才能初始化，用 @PostConstruct
    private OkHttpClient httpClient;
    private ExecutorService embedPool;

    @PostConstruct
    void init() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                // 复用连接，最多 5 条，空闲 5 分钟后回收
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .build();
        // Ollama 不支持真正批量，多词条时并行调用提升吞吐（方案A 每个实体多条向量）
        embedPool = Executors.newFixedThreadPool(Math.max(1, batchConcurrency), r -> {
            Thread t = new Thread(r, "ollama-embed");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[dimension];
        }
        List<float[]> result = embedBatch(List.of(text));
        return result.isEmpty() ? new float[dimension] : result.get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        if (texts.size() == 1) {
            return List.of(embedSingle(texts.get(0)));
        }
        // Ollama 不支持真正批量，多词条并行调用（结果顺序与入参一致）
        List<Future<float[]>> futures = new ArrayList<>(texts.size());
        for (String text : texts) {
            futures.add(embedPool.submit(() -> embedSingle(text)));
        }
        List<float[]> result = new ArrayList<>(texts.size());
        for (Future<float[]> f : futures) {
            try {
                result.add(f.get());
            } catch (Exception e) {
                log.error("Ollama 并发 embedding 失败", e);
                result.add(new float[dimension]);
            }
        }
        return result;
    }

    /**
     * 参考 AiEmbedding.getQwen3Embedding：使用 prompt + options.embedding=true 格式
     */
    private float[] embedSingle(String text) {
        JSONObject body = new JSONObject();
        body.put("prompt", text);
        body.put("model", model);
        JSONObject options = new JSONObject();
        options.put("embedding", true);
        body.put("options", options);

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create(body.toJSONString(), JSON_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseStr = response.body().string();
            JSONObject json = JSONObject.parseObject(responseStr);

            if (json.get("error") != null) {
                log.error("Ollama embedding 失败: model={}, error={}", model, responseStr);
                return new float[dimension];
            }

            JSONArray arr = json.getJSONArray("embedding");
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = arr.getFloat(i);
            }
            return vec;
        } catch (Exception e) {
            log.error("Ollama embedding 异常: model={}, error={}", model, e.getMessage(), e);
            return new float[dimension];
        }
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
