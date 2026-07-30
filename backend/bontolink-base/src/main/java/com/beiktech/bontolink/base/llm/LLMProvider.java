package com.beiktech.bontolink.base.llm;

/**
 * LLM 提供商枚举
 */
public enum LLMProvider {
    DEEPSEEK("deepseek", "https://api.deepseek.com/v1/chat/completions"),
    QWEN("qwen", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"),
    KIMI("kimi", "https://api.moonshot.cn/v1/chat/completions"),
    OPENAI("openai", "https://api.openai.com/v1/chat/completions"),
    SILICONFLOW("siliconflow", "https://api.siliconflow.cn/v1/chat/completions"),
    CUSTOM("custom", null); // 自定义 URL

    private final String code;
    private final String defaultBaseUrl;

    LLMProvider(String code, String defaultBaseUrl) {
        this.code = code;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }

    public static LLMProvider fromCode(String code) {
        for (LLMProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        return CUSTOM;
    }
}
