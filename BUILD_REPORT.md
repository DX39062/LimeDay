# 青柠日记 2.0.0 构建报告

构建日期：2026-07-16（Asia/Shanghai）

## 交付物

| APK | 适用设备 | 大小 | SHA-256 |
|---|---|---:|---|
| `dist/LimeDay-2.0.0.apk` | 通用包 | 60,794,365 bytes | `CC726092697EDD6FB1399D0DFB1E441F1442FFE9A4BC388F3A338A01977CFA79` |
| `dist/LimeDay-2.0.0-arm64-v8a.apk` | 现代 ARM64 设备，推荐 | 21,281,902 bytes | `3D5B7152FEC76D39C43F1A45BCDB1B0FB917604FD33721C9F5531EF34EBFFD04` |
| `dist/LimeDay-2.0.0-armeabi-v7a.apk` | 旧 32 位 ARM 设备 | 18,756,428 bytes | `8C0AC0CDC029A010BD5A6BA62D12092201678D0C9E22D322FA783A01C07F0978` |
| `dist/LimeDay-2.0.0-x86_64.apk` | x86_64 模拟器/设备 | 22,717,187 bytes | `A0C50D18954226C8C035BA22799A81A220931BDF2BC6F0197BEF28E1ADB9C752` |

通用包包含 `arm64-v8a`、`armeabi-v7a` 和 `x86_64`。无法判断设备架构时使用通用包。

## 应用元数据

- Application ID：`com.limeday.app`
- 应用名称：青柠日记
- Version：`2.0.0`（versionCode 3）
- minSdk：26
- targetSdk / compileSdk：36
- 权限：`android.permission.INTERNET`，以及 AndroidX 自动生成的内部动态接收器权限
- 应用备份和设备迁移：关闭
- 明文 HTTP：关闭

## 构建环境

- Flutter 3.44.6 Stable
- Dart 3.12.2
- JDK 17.0.18 LTS
- Gradle 9.1.0
- Android Gradle Plugin 9.0.1
- Kotlin 2.3.20
- Android SDK Platform 34、35、36
- Android Build Tools 36.0.0
- Android NDK 28.2.13676358

Flutter、Android SDK 和 Pub 缓存位于仓库忽略的 `.toolchain` 目录。

## 验证结果

- `flutter analyze`：0 个问题。
- `flutter test`：10 项通过，0 失败。
- Room v1 到 Drift v3 迁移测试：通过。
- Room v2 到 Drift v3 迁移测试：通过。
- Todo 日期隔离、revision 和软删除测试：通过。
- DailyReview 单日 upsert 测试：通过。
- OpenAI 兼容、Anthropic 和 Gemini 响应解析测试：通过。
- 手机与平板截图回归测试：通过。
- `flutter build apk --release`：成功。
- `flutter build apk --release --split-per-abi`：成功。
- 通用包与 ARM64 包 APK Signature Scheme v2：验证通过。
- 源码凭据扫描：未发现硬编码 API Key。
- API 36 ARM64 模拟器：安装成功，冷启动成功，首帧约 724ms。
- API 36 核心流程：新增待办、标记完成、进度更新、复盘自动保存通过。
- API 36 持久化：强制停止后重启，待办完成状态和复盘内容完整保留。
- API 36 最近 300 行日志：未发现 `FATAL EXCEPTION` 或应用崩溃。
- API 26 ARM64 模拟器：安装成功，冷启动成功，首帧约 484ms。
- API 26 前台窗口和应用进程：正常；运行截图无布局遮挡。

构建期间 `dynamic_color` 插件产生 Flutter Built-in Kotlin 未来兼容性警告，不影响当前构建。后续升级 Flutter 或插件时应重新检查。

## 签名与发布限制

当前 Release APK 使用 Android Debug RSA 2048 密钥签名，适合内部安装和验收。正式上架前必须切换到长期保存的发布密钥，并生成 AAB。

本次使用 Android API 26 和 API 36 ARM64 模拟器完成 APK 安装与冷启动验证。API 36 额外完成待办和复盘持久化冒烟流程。尚未在实体手机上验证厂商 ROM、输入法和硬件 Keystore 差异。

模型自动化测试只验证请求适配和响应解析。仓库不包含用户 API Key，因此没有向真实付费模型服务发送请求。
