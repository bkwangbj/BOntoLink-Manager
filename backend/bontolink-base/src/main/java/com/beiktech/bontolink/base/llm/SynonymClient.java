package com.beiktech.bontolink.base.llm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 同义词生成客户端（基于通用 LLM 客户端）
 */
@Slf4j
@Component
public class SynonymClient {

    @Value("${bontolink.llm.provider:}")
    private String provider;

    @Value("${bontolink.llm.api-key:}")
    private String apiKey;

    @Value("${bontolink.llm.base-url:}")
    private String baseUrl;

    @Value("${bontolink.llm.model:}")
    private String model;

    @Value("${bontolink.llm.temperature:0.3}")
    private double temperature;

    @Value("${bontolink.llm.max-tokens:256}")
    private int maxTokens;

    @Value("${bontolink.llm.system-prompt:你是一个专业的水利行业本体专家。给定一个概念，输出5-8个中文同义词或近义词，严格返回JSON数组格式，不含任何解释或多余文字。示例：[\"水坝\",\"堤坝\",\"拦河坝\"]}")
    private String systemPrompt;

    @Value("${bontolink.llm.merge-system-prompt:false}")
    private boolean mergeSystemPrompt;

    @Value("${bontolink.llm.timeout-seconds:120}")
    private int timeoutSeconds;

    private volatile UniversalLLMClient client;

    /**
     * 为给定词和描述生成同义词列表。
     *
     * @param word 核心词
     * @param desc 补充描述（可为空）
     * @return 同义词列表，调用失败时返回空列表
     */
    public List<String> generateSynonyms(String word, String desc) {
        // 检查配置
        if (provider == null || provider.isBlank() || model == null || model.isBlank()) {
            log.warn("未配置 LLM API (provider={}, model={})，跳过同义词生成", provider, model);
            return List.of();
        }

        // 构造用户提示词
        String userPrompt = "概念：" + word;
        if (desc != null && !desc.isBlank()) {
            userPrompt += "\n描述：" + desc;
        }

        try {
            String response = getClient().chat(userPrompt, systemPrompt);
            return parseSynonyms(response);
        } catch (Exception e) {
            log.error("同义词生成失败: word={}, provider={}, model={}, error={}",
                    word, provider, model, e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析同义词数组（从响应中提取 JSON 数组）
     */
    private List<String> parseSynonyms(String content) {
        try {
            // 提取 JSON 数组（content 可能包含多余空白或 markdown 代码块）
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start < 0 || end <= start) {
                log.warn("LLM 响应格式异常，无法解析数组: {}", content);
                return List.of();
            }

            JSONArray arr = JSON.parseArray(content.substring(start, end + 1));
            List<String> result = new ArrayList<>(arr.size());
            for (int i = 0; i < arr.size(); i++) {
                String s = arr.getString(i);
                if (s != null && !s.isBlank()) {
                    result.add(s.trim());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("同义词响应解析失败: content={}, error={}", content, e.getMessage());
            return List.of();
        }
    }

    /**
     * 获取或创建 LLM 客户端
     */
    private UniversalLLMClient getClient() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    LLMConfig config = new LLMConfig();
                    config.setProvider(provider);
                    config.setApiKey(apiKey);
                    config.setBaseUrl(baseUrl);
                    config.setModel(model);
                    config.setTemperature(temperature);
                    config.setMaxTokens(maxTokens);
                    config.setSystemPrompt(systemPrompt);
                    config.setMergeSystemPrompt(mergeSystemPrompt);
                    config.setTimeoutSeconds(timeoutSeconds);

                    client = new UniversalLLMClient(config);
                }
            }
        }
        return client;
    }
}
