package com.beiktech.bontolink.base.embedding;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
        List<float[]> result = new ArrayList<>(texts.size());
        // Ollama 不支持批量，逐条调用
        for (String text : texts) {
            float[] vec = embedSingle(text);
            result.add(vec);
        }
        return result;
    }

    /**
     * 参考 AiEmbedding.getQwen3Embedding：使用 prompt + options.embedding=true 格式
     */
    private float[] embedSingle(String text) {
        // 构造请求体（Ollama 格式）
        JSONObject body = new JSONObject();
        body.put("prompt", text);
        body.put("model", model);
        JSONObject options = new JSONObject();
        options.put("embedding", true);
        body.put("options", options);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(baseUrl)
                .post(RequestBody.create(body.toJSONString(), JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
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
