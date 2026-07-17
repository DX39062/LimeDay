# 青柠日记 2.5.0 构建报告

构建时间：2026-07-17（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.5.0-arm64-v8a.apk`
- Gradle 输出：`app/build/outputs/apk/release/app-arm64-v8a-release.apk`
- 文件大小：8,636,159 bytes
- SHA-256：`85022CD19828311C19F7E5C241B359DF47F1EE06AE313C9E846532AE3D7518A7`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.5.0`（versionCode `2500`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.5.0 功能

- 模型设置升级为独立二级页面，以卡片管理多个供应商并标记当前默认项。
- 支持添加、编辑、复制、删除、启用、上移、下移、连接检查和模型列表刷新。
- 内置 OpenAI、Anthropic、Gemini、OpenRouter、DeepSeek、Kimi、通义千问、智谱 GLM、SiliconFlow、MiniMax、豆包、xAI、Mistral、Groq、Ollama 和自定义 OpenAI 兼容预设。
- 支持 OpenAI Chat Completions、OpenAI Responses、Anthropic Messages 和 Gemini Native 四类协议。
- 模型发现支持版本化地址、兼容路径候选和精确地址覆盖；失败后仍可手填保存，成功列表加密缓存 24 小时。
- 供应商、API Key、模型缓存、收藏指令和最近指令继续使用 Android Keystore AES/GCM 加密；旧版单供应商配置自动迁移且保留原协议。
- 每日总结支持快捷或自由的一次性指令、收藏、最近 10 条记录，以及单次供应商和模型覆盖。
- 新增底部“总结”主页面，支持本周、本月、本季度和最长 93 天的自定义范围。
- 范围总结默认使用原始待办和复盘，可选加入已有每日总结；长输入按日期分段并分层综合。
- 每次范围生成保留独立历史，可软删除并通过 WebDAV/JSON 备份同步。
- Room 升级到 schema 6；WebDAV JSON v1 新增可选 `rangeSummaries`，旧文件缺失时按空列表兼容。
- 供应商卡片操作与协议标识使用 Compose Canvas 自绘图标。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：33 项通过，0 失败。

- 每日进度、复盘旧数据映射和设置默认值：6 项。
- 四协议响应解析、模型列表解析、地址候选、HTTP 安全开关、模型缓存与模拟网络请求：14 项。
- 同步/备份 JSON、范围总结兼容与冲突合并：10 项。
- HTTPS WebDAV 与首次同步流程：3 项。

### Android 测试

- `assembleDebugAndroidTest`：通过，18 项测试代码及测试 APK 编译成功。
- 覆盖 Room v1/v2/v3/v4/v5 到 v6 的迁移、回收站、待办手势与操作、复盘页面、模型供应商二级页、总结主页面和 WebDAV 二级页。
- 当前构建环境没有可用 platform-tools、Android 设备或 AVD，因此没有执行 `connectedDebugAndroidTest`；迁移与 Compose 仪器测试只完成了编译验证。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告为依赖/Gradle 更新提示、KAPT 迁移建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `assembleRelease` 与 release vital lint：通过。
- `aapt dump badging`：Application ID、2.5.0 / 2500、minSdk 26、targetSdk 36 正确。
- APK 中仅包含 `arm64-v8a` 原生库。
- `apksigner verify --verbose --print-certs`：v2 签名通过，签名者为开发交付用 Android Debug 证书。
- `unzip -t`：压缩内容完整，无错误。
- 生产源码凭据扫描：未发现硬编码 API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.5.0 实现。
- 计划标签与公开 GitHub Release：`v2.5.0`。
