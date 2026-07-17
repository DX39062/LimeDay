# 青柠日记 2.4.0 构建报告

构建时间：2026-07-17（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.4.0-arm64-v8a.apk`
- 本地构建副本：`dist/LimeDay-2.4.0-arm64-v8a.apk`
- 文件大小：8,521,471 bytes
- SHA-256：`74161C8B71355B033185A38032066D24D1DCE04BB829A0A67A61E6730C0E08DF`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.4.0`（versionCode `2400`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.4.0 功能

- 待办左滑改为最多 148dp 的固定操作区，支持右滑、点击事项正文或点击其他待办收回，同一列表最多展开一条。
- 移入回收站后显示约 5 秒撤销入口；新增、删除、撤销和列表重排采用淡入、淡出与位移动画。
- 待办新增高/普通/低优先级、移动日期和复制操作，优先级不自动改变列表排序。
- 待办的添加、完成、更多、优先级、日期、复制、回收站和恢复图标改为 Compose Canvas 自绘。
- 完成勾选、标题颜色、完成进度与页面导航增加 150 至 220 毫秒的克制动效。
- “解决了什么问题？”与“随便写写”默认输入高度分别增至约 4 行和 8 行。
- 智能总结主按钮改为右对齐紧凑样式，同时保持至少 48dp 的触控高度。
- Room 升级到 schema 5；WebDAV JSON v1 增加可选 `priority` 字段，旧数据缺失该字段时按普通优先级读取。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：20 项通过，0 失败。

- 每日进度计算：2 项。
- 简化复盘与旧数据映射：3 项。
- 设置默认值：1 项。
- OpenAI、Anthropic、Gemini 响应解析：3 项。
- 同步/备份 JSON、优先级兼容、冲突合并、软删除、格式拒绝与凭据排除：8 项。
- HTTPS WebDAV 与首次同步流程：3 项。

### Android 测试

- `assembleDebugAndroidTest`：通过，测试 APK 编译成功。
- 测试集覆盖可收回左滑、删除二次操作、优先级更多选项、回收站删除/恢复、移动/复制语义、复盘双字段、WebDAV 二级导航，以及 Room v1/v2/v3/v4 到 v5 的迁移。
- 当前构建环境没有连接 Android 设备或 AVD，因此没有执行 `connectedDebugAndroidTest`。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告为依赖/Gradle 更新提示、KAPT 性能建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `aapt dump badging`：Application ID、2.4.0 / 2400、minSdk 26、targetSdk 36 正确。
- APK 中仅包含 `arm64-v8a` 原生库。
- `apksigner verify --verbose --print-certs`：v2 签名通过。
- `unzip -t`：压缩内容完整，无错误。
- 生产源码凭据扫描：未发现硬编码 API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.4.0 实现。
