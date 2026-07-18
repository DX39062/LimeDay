# 青柠日记 2.7.0 构建报告

构建时间：2026-07-18（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.7.0-arm64-v8a.apk`
- Gradle 输出：`app/build/outputs/apk/release/app-arm64-v8a-release.apk`
- 文件大小：8,767,987 bytes
- SHA-256：`F965ED914F62CC61EBDFF3F78A64FD5AF0C48853DB8098EF07B58BB984123471`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.7.0`（versionCode `2700`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17

## 2.7.0 功能

- 应用内二级页使用提交后返回拦截：拖动阶段不改变 NavController 路由，取消保持当前页，提交后只返回一次；底部总结/设置返回待办，待办根页交给系统。
- 待办新增独立截止日期/时间、时区、单项提醒、每天/工作日/每周/每月/自定义重复，完成重复事项时幂等生成下一实例。
- 新增一级分组、固定收件箱、分组排序/重命名/软删除，以及可添加、修改、勾选、排序和删除的步骤。
- 待办页新增当天、逾期、计划中智能视图，以及覆盖标题、备注和步骤的本地搜索；列表按分组折叠。
- 智能总结总开关新安装默认关闭；关闭后复盘和总结页不显示生成入口，但保留历史只读，ViewModel 同样阻断网络生成。
- 2.6 升级用户若已保存模型服务且从未明确选择开关，则首次迁移保持智能总结启用。
- 回收站新增多选、全选、恢复所选、永久删除所选和清空；永久删除先确认，物理移除正文与步骤并保留最小同步墓碑。
- Room schema 升到 7；WebDAV/导出写出 JSON v2，包含待办扩展字段、分组、步骤和墓碑；v2 缺失时可读取 v1 并迁移。
- 顶层 Scaffold 不再重复消费状态栏 inset，减少待办标题上方空白；应用图标、页头装饰、可见图标和开关使用克制的儿童涂鸦语言。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：45 项通过，0 失败。

- 覆盖重复日期计算、智能总结开关默认/升级判定、四类 LLM 协议与模型缓存。
- 覆盖 WebDAV v2、v1 回退迁移、分组/步骤序列化和永久删除墓碑版本合并。
- 覆盖每日进度、复盘映射、撤销批次和 WebDAV HTTPS 请求流程。

### Android 测试

- `assembleDebugAndroidTest`：通过，29 项测试代码及测试 APK 编译成功。
- 覆盖 Room v1/v2/v3/v4/v5/v6 到 v7 的迁移、分组/步骤/截止/重复、搜索/逾期、回收站永久删除、智能总结隐藏、智能视图、分组折叠与提交后返回入口。
- 本机构建时没有连接 Android 设备或 AVD，因此没有执行 `connectedDebugAndroidTest`；上述仪器测试只完成编译验证，未伪报为设备执行通过。

### Lint

- `lintDebug`：0 errors，14 warnings。
- 警告为依赖/Gradle 更新提示、KAPT 迁移建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `assembleRelease` 与 release vital lint：通过。
- `aapt dump badging`：Application ID、2.7.0 / 2700、minSdk 26、targetSdk 36 正确。
- APK 中仅包含 `arm64-v8a` 原生库。
- `apksigner verify --verbose --print-certs`：v2 签名通过，签名者为开发交付用 Android Debug 证书。
- `unzip -t`：压缩内容完整，无错误。
- 生产源码凭据扫描：未发现硬编码 GitHub token、LLM API Key、WebDAV 密码或私钥。

## 分支与发布

- `legacy/android-kotlin-1.1`：旧 Kotlin 1.1 实现。
- `legacy/flutter-2.0`：Flutter 2.0 实现。
- `main`：当前 Kotlin/Compose 2.7.0 实现。
- 目标标签与公开 GitHub Release：`v2.7.0`。

## 构建缓存清理

- 发布 APK、测试结果与报告落盘后停止 Gradle daemon 与 ADB。
- 删除项目 `.gradle`、`app/build` 和 `.kotlin` 会话缓存；保留项目 `.toolchain/android-sdk` 与用户级 `~/.gradle` 作为可复用工具链。
