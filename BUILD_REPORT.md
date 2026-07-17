# 青柠日记 2.6.0 构建报告

构建时间：2026-07-18（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.6.0-arm64-v8a.apk`
- Gradle 输出：`app/build/outputs/apk/release/app-arm64-v8a-release.apk`
- 文件大小：8,653,303 bytes
- SHA-256：`7E646F7138E145ACC7338893CB4D852373D62A1B4A8D17F881F2F8292B10BE2E`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.6.0`（versionCode `2600`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.6.0 功能

- 设置成为第三个底部一级页面，待办提醒、复盘提醒、回收站、模型服务、提示词、外观、数据、WebDAV 和关于集中分组管理。
- 模型服务保留多配置加密存储，同一厂商可保存多份配置；卡片主体与固定自绘铅笔均可编辑，操作区支持自动换行。
- 新建模型服务在首次保存前提供显式“手动获取模型”，仅在用户点击时联网；失败仍可手填模型并保存，未保存配置不会留下孤立缓存。
- 总结历史逐条默认折叠，可独立展开且不持久化展开状态。
- 待办日期卡片可打开当前所选月份的快速月历，支持立即跳转、回到今天和待办完成状态圆点。
- 每日待办与复盘共用 6 秒删除撤销；连续删除合并成一个批次，一次撤销恢复整批。
- 应用图标改为便签、对勾与青柠切片组成的涂鸦图形；底部导航、页面操作、下拉箭头和待办页顶部装饰均为 Compose Canvas 或自有矢量绘制。
- 保持 Room schema 6、WebDAV JSON v1、四类 LLM 协议、16 个服务预设与最长 93 天范围总结兼容。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：37 项通过，0 失败。

- 每日进度、月份状态、删除撤销提示、复盘旧数据映射和设置默认值：9 项。
- 四协议响应解析、模型列表解析、地址候选、HTTP 安全开关、模型缓存、同厂商多配置与模拟网络请求：15 项。
- 同步/备份 JSON、范围总结兼容与冲突合并：10 项。
- HTTPS WebDAV 与首次同步流程：3 项。

### Android 测试

- `assembleDebugAndroidTest`：通过，22 项测试代码及测试 APK 编译成功。
- 覆盖 Room v1/v2/v3/v4/v5 到 v6 的迁移、回收站、待办手势与操作、三级主导航、快速月历、模型服务固定编辑/首次取模、默认折叠历史和 WebDAV 二级页。
- SDK 36 与 platform-tools 安装在临时目录，但没有连接 Android 设备或 AVD，因此没有执行 `connectedDebugAndroidTest`；迁移与 Compose 仪器测试只完成了编译验证。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告为依赖/Gradle 更新提示、KAPT 迁移建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `assembleRelease` 与 release vital lint：通过。
- `aapt dump badging`：Application ID、2.6.0 / 2600、minSdk 26、targetSdk 36 正确。
- APK 中仅包含 `arm64-v8a` 原生库。
- `apksigner verify --verbose --print-certs`：v2 签名通过，签名者为开发交付用 Android Debug 证书。
- `unzip -t`：压缩内容完整，无错误。
- 生产源码凭据扫描：未发现硬编码 API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.6.0 实现。
- 标签与公开 GitHub Release：`v2.6.0`。

## 构建缓存清理

- 发布 APK、测试结果与报告落盘后停止 Gradle daemon。
- 删除项目 `.gradle`、`app/build` 和本次专用的 `/private/tmp/android-sdk`；不清理可复用于其他项目的用户级 `~/.gradle` 缓存。
- 清理对象约 484 MB，后续本机构建需重新配置可用 Android SDK 36。
