# LimeDay 2.5 模型服务与范围总结设计

## 目标

2.5 将原来的单个 LLM 配置升级为本地加密的多供应商管理，并增加独立范围总结页。交互借鉴 CC Switch 的供应商卡片、预设填充、主动获取模型和明确启用状态，但不复制其代码、图标或面向 CLI 的代理能力。

## 供应商能力

- 协议：OpenAI Chat Completions、OpenAI Responses、Anthropic Messages、Gemini Native。
- 首批预设：OpenAI、Anthropic、Gemini、OpenRouter、DeepSeek、Kimi、通义千问、智谱 GLM、SiliconFlow、MiniMax、豆包、xAI、Mistral、Groq、Ollama、自定义 OpenAI 兼容。
- 管理：添加、编辑、复制、删除、启用、排序、连接测试、获取模型。
- 选择：一个全局默认供应商；每日或范围生成可以临时覆盖供应商和模型。
- 模型发现：由用户主动刷新，缓存 24 小时。失败时保留手动模型输入和保存能力。
- 本地接口：HTTP 默认拒绝；用户为单个供应商显式开启后才允许 Ollama 或局域网地址。

Azure OpenAI、AWS Bedrock 和 Vertex AI 需要专门的认证字段、签名流程或资源路径，2.5 不用不完整的“兼容模式”冒充支持。

## 本地安全边界

以下内容使用 Android Keystore AES/GCM 加密后保存在 SharedPreferences：

- 供应商名称、协议、Base URL、模型、API Key 和本地 HTTP 开关。
- 默认供应商 ID、列表顺序和模型缓存。
- 收藏指令和最近 10 条指令。

这些内容不得进入 Room、WebDAV、JSON 导出、日志或崩溃文本。RangeSummary 只保存生成时的供应商 ID、显示名和模型，不保存端点或凭据。

## 模型发现

| 协议 | 请求 | 认证 | 解析 |
|---|---|---|---|
| OpenAI Chat/Responses | `GET {base}/models` | `Authorization: Bearer` | `data[].id` |
| Anthropic Messages | `GET {base}/models` | `x-api-key` 与 `anthropic-version` | `data[].id` |
| Gemini Native | `GET {base}/models` | `x-goog-api-key` | `models[].name`，去除 `models/` 前缀 |

预设可给出精确模型列表地址。自动推导会识别 Base URL 已包含版本段的情况，避免重复拼接 `/v1`。网络、认证、404 或解析错误均为非阻塞错误。

没有版本段的兼容地址按 `/v1/models`、`/models` 顺序尝试；非标准 Anthropic 兼容子路径还会尝试剥离子路径后的候选地址。只有 404/405 会继续下一候选，认证及其他错误立即反馈给用户。

## 一次性指令

每日和范围总结使用同一套一次性指令机制，不构建多轮聊天：

- 总结今日进展
- 给出明日建议
- 分析未完成事项
- 压缩成三句话
- 用户自由输入

内置项不占收藏配额。用户可收藏自由指令；最近记录去重后保留 10 条。

## 范围总结

总结页提供本周、本月、本季度和自定义范围。默认只发送原始待办和复盘，用户可选择加入已有每日总结。范围不得超过 93 天。

数据按日期组织。输入超过单请求阈值时，在日期边界拆成多个片段，分别生成中间摘要，再把中间摘要发送给同一供应商完成最终综合。只有最终成功内容写入 RangeSummary。

每次生成创建独立历史记录。历史按生成时间倒序展示，可软删除；同范围重新生成不会覆盖旧版本。

## 数据与同步

Room schema 6 新增 `range_summaries` 表。5 到 6 迁移只创建新表和索引。

WebDAV 文件名和 `formatVersion: 1` 保持不变，在顶层新增可选 `rangeSummaries` 数组。缺少该字段按空列表处理，合并时 RangeSummary 以 UUID 为身份并使用现有 `updatedAt`、`revision`、`deviceId` 顺序决胜。

## 发布验收

- 单元测试覆盖四类响应解析、模型列表解析、地址推导、同步序列化和合并。
- 迁移测试覆盖 Room 5 到 6，并继续验证 1/2/3/4 旧库升级。
- Lint、单元测试和 release ARM64 APK 构建通过。
- APK 内不出现测试 API Key、WebDAV 密码或供应商凭据。
- 发布版本为 `2.5.0`，Git 标签为 `v2.5.0`，GitHub Release 附带 ARM64 APK 和 SHA-256。
