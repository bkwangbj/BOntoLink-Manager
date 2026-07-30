package com.beiktech.bontolink.base.llm;

import lombok.Data;

/**
 * LLM 配置
 */
@Data
public class LLMConfig {
    private String provider;           // 提供商：deepseek / qwen / kimi / openai / siliconflow / custom
    private String apiKey;             // API Key
    private String baseUrl;            // 基础 URL（可选，未设置时使用提供商默认值）
    private String model;              // 模型名称
    private Double temperature = 0.3;
    private Integer maxTokens = 256;
    private String systemPrompt;
    private Boolean mergeSystemPrompt = false;
    private Integer timeoutSeconds = 120;

    /**
     * 获取最终的 baseUrl（优先使用配置值，否则使用提供商默认值）
     */
    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        LLMProvider providerEnum = LLMProvider.fromCode(provider);
        return providerEnum.getDefaultBaseUrl();
    }

    /**
     * 验证配置是否完整
     */
    public boolean isValid() {
        return provider != null && !provider.isBlank()
                && model != null && !model.isBlank()
                && getEffectiveBaseUrl() != null;
    }
}
