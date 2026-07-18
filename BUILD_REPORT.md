# 青柠日记 2.7.1 构建报告

构建时间：2026-07-18（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.7.1-arm64-v8a.apk`
- Gradle 输出：`app/build/outputs/apk/release/app-arm64-v8a-release.apk`
- 文件大小：8,784,403 bytes
- SHA-256：`3829708CD0A646779B0DC878C8116A21735D42FB1F3B3FDF84E2D6433DADBA3D`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.7.1`（versionCode `2701`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.7.1 修复

- `MainActivity` 设置 `android:enableOnBackInvokedCallback="false"`，彻底关闭系统预测返回预览；应用路由仍只在返回提交后执行一次。
- 模型服务页新增始终可见的页头“新增”和列表底部“新增模型服务”，点击后先选择预设再编辑；新服务使用独立 UUID，首次保存前保留手动获取模型。
- 待办三点菜单把详情改为清单/调节图形，把移动日期改为日历加右箭头，与复制、删除和优先级图形区分。
- 完成进度压缩为 64–72dp，只在当天且没有搜索时显示；逾期、计划中与搜索不再误显所选日期进度。
- 搜索默认折叠为 48dp 手绘按钮，与分组按钮同排；展开后搜索标题、备注和步骤，清空失焦自动折叠，关闭清空查询。
- 固定默认分组改名为“日常”；2.7.0 的固定“收件箱”仅幂等改名一次，不改 ID、Todo 引用、Room schema 或 WebDAV 格式。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：48 项通过，0 失败。

- 覆盖重复日期计算、智能总结开关默认/升级判定、四类 LLM 协议与模型缓存。
- 覆盖 WebDAV v2、v1 回退迁移、分组/步骤序列化和永久删除墓碑版本合并。
- 覆盖每日进度、复盘映射、撤销批次、WebDAV HTTPS 请求，以及 2.7.1 Manifest、图标、进度和搜索合同。

### Android 测试

- `assembleDebugAndroidTest`：通过，31 项测试代码及测试 APK 编译成功。
- 覆盖 Room v1/v2/v3/v4/v5/v6 到 v7 的迁移、默认分组幂等改名、已有模型服务时新增、分组/步骤/截止/重复、紧凑搜索/进度和回收站。
- 本机构建时没有连接 Android 设备或 AVD，因此没有执行 `connectedDebugAndroidTest`；上述仪器测试只完成编译验证，未伪报为设备执行通过。

### Lint

- `lintDebug`：0 errors，15 warnings。
- 警告为预测返回属性在 API 33 以下被忽略、依赖/Gradle 更新提示、KAPT 迁移建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `assembleRelease` 与 release vital lint：通过。
- `aapt dump badging`：Application ID、2.7.1 / 2701、minSdk 26、targetSdk 36 正确。
- 合并后的 release Manifest 保留 `android:enableOnBackInvokedCallback="false"`。
- APK 中仅包含 `arm64-v8a` 原生库。
- `apksigner verify --verbose --print-certs`：v2 签名通过，签名者为开发交付用 Android Debug 证书。
- `unzip -t`：压缩内容完整，无错误。
- 生产源码凭据扫描：未发现硬编码 GitHub token、LLM API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.7.1 实现。
- 目标标签与公开 GitHub Release：`v2.7.1`。

## 构建缓存清理

- 发布 APK、测试结果与报告落盘后停止 Gradle daemon 与 ADB。
- 删除项目 `.gradle`、`app/build` 和 `.kotlin` 会话缓存；保留项目 `.toolchain/android-sdk` 与用户级 `~/.gradle` 作为可复用工具链。
