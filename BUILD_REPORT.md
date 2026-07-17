# 青柠日记 2.3.0 构建报告

构建时间：2026-07-17（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.3.0-arm64-v8a.apk`
- 本地构建副本：`dist/LimeDay-2.3.0-arm64-v8a.apk`
- 文件大小：8,472,315 bytes
- SHA-256：`43D6C92A4847CB6A4AFC077FA8CB755BA83CCB665FBD41932100EB772B060551`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2 验证通过

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.3.0`（versionCode `2300`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.3.0 功能

- 待办列表取消常驻编辑图标，点击事项正文进入编辑。
- 左滑只露出“移到回收站”按钮，不再因滑动距离直接删除。
- 编辑弹窗中的删除操作增加二次确认。
- 设置页新增回收站二级页，按删除时间展示全部软删除待办并支持恢复到原日期。
- WebDAV 配置、连接测试和同步操作移入独立二级页面。
- 复盘页仅保留“解决了什么问题？”和“随便写写”两个输入区域，不再显示心情评分。
- 旧版亮点、收获和明日重点带原标签无损合并到“随便写写”，同步 JSON v1 保持兼容。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：18 项通过，0 失败。

- 每日进度计算：2 项。
- 简化复盘与旧数据映射：3 项。
- 设置默认值：1 项。
- OpenAI、Anthropic、Gemini 响应解析：3 项。
- 同步/备份 JSON、冲突合并、软删除、格式拒绝与凭据排除：6 项。
- HTTPS WebDAV 与首次同步流程：3 项。

### Android 测试

- `assembleDebugAndroidTest`：通过，测试 APK 编译成功。
- 测试集覆盖滑动不直接删除、编辑删除确认、回收站删除/恢复、复盘双字段及 WebDAV 二级导航。
- 当前构建环境没有连接 Android 设备或 AVD，因此本次没有执行 `connectedDebugAndroidTest`。
- 数据库 v1/v2/v3 迁移测试继续保留在测试集中。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告为依赖/Gradle 更新提示、KAPT 性能建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `aapt dump badging`：Application ID、2.3.0 / 2300、minSdk 26、targetSdk 36 正确。
- APK 中仅包含 `arm64-v8a` 原生库。
- `apksigner verify --verbose --print-certs`：v2 签名通过。
- `unzip -t`：压缩内容完整，无错误。
- 源码凭据扫描：未发现硬编码 API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.3.0 实现。
