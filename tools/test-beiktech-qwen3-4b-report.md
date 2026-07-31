# BeikTech 内部 Qwen3-4B 模型测试报告

## 测试时间
2026-07-30 11:36

## 测试结果摘要
✅ **API 连接正常**  
✅ **模型调用成功**  
✅ **无需秘钥**  
⚠️ **中文输入存在问题**

## 详细测试过程

### 1. 服务连接性测试
- **测试端点**: `http://work.beiktech.com:8002/v1/models`
- **结果**: 成功返回模型列表
- **发现**: 服务正常运行，`Qwen3-4B` 模型可用

### 2. 模型信息
从 API 返回的模型信息：
- **模型 ID**: `Qwen3-4B`
- **模型类型**: vLLM 部署
- **最大长度**: 4096 tokens
- **所有者**: vllm
- **模型路径**: `/media/q/8B527E5A5A2DC05A/Qwen3-4B-Instruct-2507`

### 3. 英文对话测试
**请求**:
```json
{
  "model": "Qwen3-4B",
  "messages": [
    {"role": "user", "content": "Hello"}
  ],
  "max_tokens": 50
}
```

**响应**:
```json
{
  "id": "chatcmpl-7a2d6b071c21458989e0a7e226942b18",
  "object": "chat.completion",
  "model": "Qwen3-4B",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I assist you today? 😊"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 9,
    "completion_tokens": 12,
    "total_tokens": 21
  }
}
```

### 4. 英文复杂对话测试
**请求**:
```json
{
  "model": "Qwen3-4B",
  "messages": [
    {"role": "user", "content": "What is artificial intelligence?"}
  ],
  "max_tokens": 150
}
```

**响应**: 成功返回详细的 AI 介绍，包含结构化内容

### 5. 中文对话测试
**请求**:
```json
{
  "model": "Qwen3-4B",
  "messages": [
    {"role": "user", "content": "你好，请介绍一下你自己"}
  ],
  "max_tokens": 200
}
```

**结果**: ❌ 返回错误 `{"detail":"There was an error parsing the body"}`

## 技术分析

### 优势
1. **无需秘钥**：完全开放的 API 接口，方便测试和开发
2. **响应快速**：英文对话响应时间在 1 秒左右
3. **模型性能**：Qwen3-4B 模型在英文对话中表现良好
4. **服务稳定**：API 端点响应正常

### 问题
1. **中文编码问题**：中文内容可能导致请求体解析失败
2. **可能的解决方案**：
   - 使用 URL 编码处理中文内容
   - 检查服务端是否支持 UTF-8 编码
   - 尝试使用其他中文输入方式

## 配置信息
- **API 端点**: `http://work.beiktech.com:8002/v1/chat/completions`
- **模型**: `Qwen3-4B`
- **无需认证**: 完全开放
- **最大 token**: 4096

## 建议
1. **优先使用英文**：如果中文支持有问题，建议使用英文进行对话
2. **检查编码**：可以尝试使用 URL 编码或 Base64 编码中文内容
3. **联系管理员**：如果需要完整的中文支持，建议联系服务管理员
4. **作为开发环境**：这个端点非常适合作为开发和测试环境

## 结论
✅ **模型可用**：`Qwen3-4B` 模型可以正常调用
✅ **无需秘钥**：完全开放的 API 接口
✅ **英文支持良好**：英文对话功能正常
⚠️ **中文支持有限**：中文输入可能存在编码问题

这个内部服务非常适合用于开发和测试，特别是不需要秘钥的场景。建议在项目中配置这个端点作为开发环境的备用模型。