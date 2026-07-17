# 青柠日记 2.2.0 构建报告

构建时间：2026-07-17（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.2.0-arm64-v8a.apk`
- 本地构建副本：`dist/LimeDay-2.2.0-arm64-v8a.apk`
- 文件大小：8,472,315 bytes
- SHA-256：`E04DAA04C83A4F0F1166305CB0155E6B8C8AF76B3C0E11E6629B82B76630C4D5`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.2.0`（versionCode `2200`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.2.0 功能

- 重绘 Adaptive Icon 前景并保留 Android 13 主题图标，品牌主体适配常见系统遮罩。
- 设置页新增跟随系统、浅色和深色外观模式。
- 设置页新增独立的每日待办与复盘提醒，可启用、关闭和修改时间。
- 设置页新增 JSON 数据导入导出、版本、隐私说明和开源许可。
- 复盘页显示当日全部待办，可完成、取消完成、编辑和删除。
- 主页与复盘页统一为左滑删除，并提供 5 秒撤销；编辑弹窗保留无障碍删除入口。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：15 项通过，0 失败。

- 每日进度计算：2 项。
- 设置默认值：1 项。
- OpenAI、Anthropic、Gemini 响应解析：3 项。
- 同步/备份 JSON、冲突合并、软删除、格式拒绝与凭据排除：6 项。
- HTTPS WebDAV 与首次同步流程：3 项。

### Android 测试

- `assembleDebugAndroidTest`：通过，测试 APK 编译成功。
- 新增复盘待办区域、设置导航和左滑删除触发测试。
- 当前构建环境没有连接 Android 设备或 AVD，因此本次没有执行 `connectedDebugAndroidTest`。
- 数据库 v1/v2/v3 迁移测试仍保留在测试集中。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告为依赖/Gradle 更新提示、KAPT 性能建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `aapt dump badging`：Application ID、2.2.0 / 2200、minSdk 26、targetSdk 36 正确。
- APK 中唯一原生库为 `lib/arm64-v8a/libandroidx.graphics.path.so`。
- `apksigner verify --verbose --print-certs`：v2 签名通过。
- `unzip -t`：压缩内容完整，无错误。
- 源码凭据扫描：未发现硬编码 API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.2.0 实现。
