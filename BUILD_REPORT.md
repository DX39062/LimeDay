# 青柠日记 1.1.0 构建报告

构建时间：2026-07-16（Asia/Hong_Kong）

## 交付物

- APK：`dist/LimeDay-1.1.0.apk`
- 文件大小：19,052,014 bytes
- SHA-256：`F96E29F9049FF18992D1BA1E48F15391CD0A2EEA0070DFF6EA7F4107ED52DA2E`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 是可直接侧载安装的开发签名版本，适合当前验收与试用。正式上架应用商店前应由发布方使用长期保存的正式证书重新签名。

## 构建环境

- Microsoft OpenJDK 17.0.19 LTS
- Gradle 8.13
- Android Gradle Plugin 8.13.2
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0（AGP 同时安装并使用其默认 35.0.0）
- Kotlin 2.3.20

工具链安装在项目内 `.toolchain`，不依赖系统级 JDK 或 Android SDK。

## APK 元数据

- Application ID：`com.limeday.app`
- Version：`1.1.0`（versionCode 2）
- minSdk：26
- targetSdk / compileSdk：36
- 权限：`android.permission.INTERNET`；其余动态接收器权限由 AndroidX 自动生成
- 应用备份与设备迁移：关闭

## 验证结果

- `testDebugUnitTest`：5 项通过，0 失败
- `lintDebug`：0 错误；11 个非阻断警告均为依赖/工具更新建议或 KAPT→KSP 性能建议
- `assembleDebug`：成功
- APK 签名验证：成功
- 源码凭据扫描：未发现形似 API Key 的字面量
- 自适应、圆形和单色主题图标资源：均已打包

LLM 自动化测试覆盖 OpenAI 兼容、Anthropic 和 Gemini 三类响应解析。由于项目不内置用户 API Key，构建环境未向真实厂商发送付费请求；真实接口连接由用户在应用中配置后执行。
