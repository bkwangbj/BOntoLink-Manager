# 硅基流动 Qwen/Qwen2.5-7B-Instruct 模型测试报告

## 测试时间
2026-07-30 11:30

## 测试结果摘要
✅ **API 连接正常**  
❌ **账户余额不足，无法调用模型**

## 详细测试过程

### 1. API 连接性测试
- **测试命令**: `curl -X GET "https://api.siliconflow.cn/v1/models"`
- **结果**: 成功返回模型列表
- **发现**: API key 有效，可以访问硅基流动 API

### 2. 模型可用性验证
从返回的模型列表中确认 `Qwen/Qwen2.5-7B-Instruct` 模型存在：
- 模型 ID: `Qwen/Qwen2.5-7B-Instruct`
- 所有者: 硅基流动平台

### 3. 模型调用测试
- **测试命令**: 
  ```bash
  curl -X POST "https://api.siliconflow.cn/v1/chat/completions" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer sk-gndwpiiwzjxjwvtqmdlcwbazbmebkzlxtgutenjcsetvaowb" \
    -d '{
      "model": "Qwen/Qwen2.5-7B-Instruct",
      "messages": [
        {
          "role": "system",
          "content": "You are a helpful assistant."
        },
        {
          "role": "user",
          "content": "你好，请介绍一下你自己"
        }
      ],
      "max_tokens": 200,
      "temperature": 0.7
    }'
  ```
- **返回结果**: 
  ```json
  {"code":30001,"message":"Sorry, your account balance is insufficient","data":null}
  ```
- **分析**: API 调用格式正确，但账户余额不足导致无法执行模型推理

## 配置信息
- **API Key**: `sk-gndwpiiwzjxjwvtqmdlcwbazbmebkzlxtgutenjcsetvaowb`
- **API 端点**: `https://api.siliconflow.cn/v1/chat/completions`
- **模型**: `Qwen/Qwen2.5-7B-Instruct`
- **配置文件**: `backend/bontolink-admin/src/main/resources/application.yml`

## 结论
1. **API 连接正常**：可以访问硅基流动平台 API
2. **模型可用**：`Qwen/Qwen2.5-7B-Instruct` 在平台模型列表中
3. **余额不足**：当前 API key 余额不足以支持模型调用
4. **配置正确**：API key 和端点配置正确

## 建议
1. 检查硅基流动账户余额
2. 如果需要使用该模型，需要充值或获取新的 API key
3. 可以考虑使用项目中已配置的 DeepSeek 模型作为替代

## 附录：可用模型列表（部分）
从 API 返回的模型列表中，与 Qwen 相关的模型包括：
- `Qwen/Qwen2.5-72B-Instruct-128K`
- `Qwen/Qwen2.5-72B-Instruct`
- `Qwen/Qwen2.5-32B-Instruct`
- `Qwen/Qwen2.5-14B-Instruct`
- `Qwen/Qwen2.5-7B-Instruct` ← 目标模型
- `Pro/Qwen/Qwen2.5-7B-Instruct` (Pro 版本)
- `LoRA/Qwen/Qwen2.5-7B-Instruct` (LoRA 版本)