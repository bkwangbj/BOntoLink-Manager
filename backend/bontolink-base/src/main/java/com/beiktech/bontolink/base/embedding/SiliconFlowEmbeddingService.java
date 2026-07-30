package com.beiktech.bontolink.base.embedding;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "bontolink.embedding.provider", havingValue = "siliconflow")
public class SiliconFlowEmbeddingService implements EmbeddingService {

    @Value("${bontolink.siliconflow.api-key}")
    private String apiKey;

    @Value("${bontolink.siliconflow.base-url:https://api.siliconflow.cn/v1/embeddings}")
    private String baseUrl;

    @Value("${bontolink.siliconflow.model:BAAI/bge-base-zh-v1.5}")
    private String model;

    @Value("${bontolink.siliconflow.dimension:1024}")
    private int dimension;

    @Value("${bontolink.siliconflow.timeout-seconds:30}")
    private int timeoutSeconds;

    private volatile RestClient restClient;

    private RestClient getClient() {
        if (restClient == null) {
            synchronized (this) {
                if (restClient == null) {
                    restClient = RestClient.builder()
                            .baseUrl(baseUrl)
                            .defaultHeader("Authorization", "Bearer " + apiKey)
                            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .build();
                }
            }
        }
        return restClient;
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
        try {
            String body = JSON.toJSONString(Map.of("model", model, "input", texts));
            String response = getClient().post()
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return parseEmbeddings(response, texts.size());
        } catch (Exception e) {
            log.error("SiliconFlow embedding 失败: model={} texts={} err={}",
                    model, texts.size(), e.getMessage(), e);
            // 返回零向量
            List<float[]> fallback = new ArrayList<>();
            for (int i = 0; i < texts.size(); i++) {
                fallback.add(new float[dimension]);
            }
            return fallback;
        }
    }

    private List<float[]> parseEmbeddings(String response, int expectedCount) {
        List<float[]> result = new ArrayList<>(expectedCount);
        try {
            JSONObject root = JSON.parseObject(response);
            JSONArray data = root.getJSONArray("data");
            for (int i = 0; i < data.size(); i++) {
                JSONArray embArr = data.getJSONObject(i).getJSONArray("embedding");
                float[] vec = new float[embArr.size()];
                for (int j = 0; j < embArr.size(); j++) {
                    vec[j] = embArr.getFloat(j);
                }
                result.add(vec);
            }
        } catch (Exception e) {
            log.error("Failed to parse SiliconFlow embedding response: {}", e.getMessage());
        }
        return result;
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
