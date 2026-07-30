package com.beiktech.bontolink.base.llm;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

/**
 * 通用 LLM 客户端（参考 watf-ai AiLLM 实现，使用 OkHttp）
 * 兼容所有 OpenAI Chat Completion API 格式的模型
 */
@Slf4j
public class UniversalLLMClient {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final LLMConfig config;

    public UniversalLLMClient(LLMConfig config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid LLM config: provider, model, baseUrl are required");
        }
        this.config = config;
        log.info("LLM 客户端已初始化: provider={}, model={}, url={}",
                config.getProvider(), config.getModel(), config.getEffectiveBaseUrl());
    }

    /**
     * 同步调用（使用配置中的 system prompt）
     */
    public String chat(String userPrompt) {
        return chat(userPrompt, config.getSystemPrompt());
    }

    /**
     * 同步调用（自定义 system prompt）
     */
    public String chat(String userPrompt, String systemPrompt) {
        RequestBody body = buildRequestBody(systemPrompt, userPrompt);
        Request request = new Request.Builder()
                .url(config.getEffectiveBaseUrl())
                .post(body)
                .addHeader("Authorization", "Bearer " + nullSafe(config.getApiKey()))
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
                .build();

        log.debug("LLM 请求: provider={}, model={}, body={}", config.getProvider(), config.getModel(), body);

        try (Response response = client.newCall(request).execute()) {
            String str = response.body().string();
            log.debug("LLM 响应: {}", str);
            return parseContent(str);
        } catch (Exception e) {
            log.error("LLM 调用失败: provider={}, model={}, error={}",
                    config.getProvider(), config.getModel(), e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    /**
     * 构造 OpenAI 兼容请求体（参考 AiLLM.buildOpenAiStrHttpBody）
     */
    private RequestBody buildRequestBody(String systemPrompt, String userPrompt) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("model", config.getModel());
        requestJson.put("stream", false);
        requestJson.put("temperature", config.getTemperature());
        requestJson.put("max_tokens", config.getMaxTokens());

        JSONArray messages = new JSONArray();

        // system 消息
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JSONObject system = new JSONObject();
            system.put("role", "system");
            system.put("content", systemPrompt);
            messages.add(system);
        }

        // user 消息
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", userPrompt);
        messages.add(user);

        requestJson.put("messages", messages);
        return RequestBody.create(requestJson.toJSONString(), JSON_TYPE);
    }

    /**
     * 解析响应 choices[0].message.content（参考 AiLLM.getOpenAiStr）
     */
    private String parseContent(String responseStr) {
        JSONObject obj = JSONObject.parseObject(responseStr);
        if (obj.containsKey("error")) {
            String msg = obj.getJSONObject("error").getString("message");
            throw new RuntimeException("LLM API error: " + msg);
        }
        if (obj.containsKey("code")) {
            throw new RuntimeException("LLM API error: " + responseStr);
        }
        return obj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public LLMConfig getConfig() {
        return config;
    }
}
