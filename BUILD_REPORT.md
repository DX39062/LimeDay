# 青柠日记 2.1.0 构建报告

构建时间：2026-07-16（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.1.0-arm64-v8a.apk`
- 本地构建副本：`dist/LimeDay-2.1.0-arm64-v8a.apk`
- 文件大小：8,422,611 bytes
- SHA-256：`21E54AC78589B864D1FA7CB9E1BCB97C8AC6D4E166643FBA69565709B6D0A26D`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.1.0`（versionCode `2100`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：Microsoft OpenJDK 17.0.18 ARM64

versionCode `2100` 高于已发布 Flutter 测试包在模拟器中的 `2003`，可直接覆盖升级。

## 功能与架构

- Kotlin + Jetpack Compose + Material 3 原生 UI。
- 每日待办主页只显示复盘二级入口，不直接显示复盘字段或总结。
- 独立复盘页包含结构化复盘、心情和 LLM 总结。
- WebDAV 支持 HTTPS 配置、PROPFIND 测试、MKCOL、GET、PUT、冲突合并与 WorkManager 后台同步。
- Room v4 使用 UUID、revision、deviceId 和软删除记录。
- Room v1/v2 与 Flutter/Drift v3 均提供非破坏性迁移。
- WebDAV 密码与 LLM API Key 使用 Android Keystore AES/GCM 保存。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：12 项通过，0 失败。

- 每日进度计算：2 项。
- OpenAI、Anthropic、Gemini 响应解析：3 项。
- 同步 JSON 往返、冲突合并、软删除、按日期去重：4 项。
- HTTPS WebDAV PROPFIND、首次同步 404、MKCOL/PUT 与凭据排除：3 项。

### ARM64 设备测试

`connectedDebugAndroidTest`：API 36 ARM64 模拟器上 4 项通过，0 失败。

- Room v1 → Room v4 数据迁移。
- Room v2 → Room v4 总结与同步元数据迁移。
- Flutter/Drift v3 → Room v4 数据迁移。
- 待办主页 → 独立复盘页 → 智能总结区域导航层级。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告仅包括依赖/Gradle 更新提示、KAPT 性能建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## 模拟器验收

### API 36 ARM64

- ABI：`arm64-v8a`。
- 冷启动：675ms；强制停止后再次冷启动 732ms。
- 新增测试待办成功。
- 标记完成后进度显示 `1 / 1` 与 `100%`。
- 复盘输入停止后自动保存，强制停止并重启后显示“继续复盘”。
- 主页无复盘字段；复盘页底部存在“智能总结”“配置模型”和“生成总结”。
- AndroidRuntime 与 SQLiteLog 无错误。

### API 26 ARM64

- 安装成功，冷启动 330ms。
- `MainActivity` 为前台可见窗口。
- AndroidRuntime 与 SQLiteLog 无错误。

## APK 检查

- `aapt2 dump badging`：版本、minSdk、targetSdk 与 application ID 正确。
- APK 中唯一原生库为 `lib/arm64-v8a/libandroidx.graphics.path.so`。
- `apksigner verify --verbose --print-certs`：v2 签名通过。
- `unzip -t`：压缩内容完整，无错误。
- 源码凭据扫描：未发现硬编码 API Key、WebDAV 密码或私钥。

## 仓库分支

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：已推送的 Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.1 实现。
