package com.beiktech.bontolink.base.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DeepSeekSynonymClient {

    private static final String SYSTEM_PROMPT =
            "你是一个专业的水利行业本体专家。给定一个概念，输出5-8个中文同义词或近义词，" +
            "严格返回JSON数组格式，不含任何解释或多余文字。示例：[\"水坝\",\"堤坝\",\"拦河坝\"]";

    @Value("${bontolink.deepseek.api-key:}")
    private String apiKey;

    @Value("${bontolink.deepseek.base-url:https://api.deepseek.com/v1/chat/completions}")
    private String baseUrl;

    @Value("${bontolink.deepseek.model:deepseek-chat}")
    private String model;

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

    /**
     * 为给定词和描述生成同义词列表。
     *
     * @param word 核心词
     * @param desc 补充描述（可为空）
     * @return 同义词列表，调用失败时返回空列表
     */
    public List<String> generateSynonyms(String word, String desc) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DeepSeek api-key 未配置，跳过同义词生成");
            return List.of();
        }

        String userPrompt = "概念：" + word;
        if (desc != null && !desc.isBlank()) {
            userPrompt += "\n描述：" + desc;
        }

        String body = JSON.toJSONString(Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3,
                "max_tokens", 256
        ));

        try {
            String response = getClient().post()
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseResponse(response);
        } catch (Exception e) {
            log.error("DeepSeek 调用失败: word={}, error={}", word, e.getMessage());
            return List.of();
        }
    }

    private List<String> parseResponse(String response) {
        try {
            JSONObject root = JSON.parseObject(response);
            String content = root.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            // 提取 JSON 数组（content 可能包含多余空白或 markdown 代码块）
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start < 0 || end <= start) {
                log.warn("DeepSeek 响应格式异常，无法解析数组: {}", content);
                return List.of();
            }
            JSONArray arr = JSON.parseArray(content.substring(start, end + 1));
            List<String> result = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                String s = arr.getString(i);
                if (s != null && !s.isBlank()) result.add(s.trim());
            }
            return result;
        } catch (Exception e) {
            log.warn("DeepSeek 响应解析失败: {}", e.getMessage());
            return List.of();
        }
    }
}
