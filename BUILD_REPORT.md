# 青柠日记 2.7.2 构建报告

构建时间：2026-07-18（Asia/Shanghai）

## 交付物

- 仓库 APK：`releases/LimeDay-2.7.2-arm64-v8a.apk`
- Gradle 输出：`app/build/outputs/apk/release/app-arm64-v8a-release.apk`
- 文件大小：8,801,539 bytes
- SHA-256：`889513FA82C804865DDD9F84396EA054C15B5658D6E0D72340F1FA0138D664AE`
- 架构：仅 `arm64-v8a`
- 签名：Android Debug RSA 2048，APK Signature Scheme v2

该 APK 可直接侧载用于验收和试用。正式提交应用商店前必须使用长期保存的正式发布证书重新签名。

## 版本与平台

- Application ID：`com.limeday.app`
- Version：`2.7.2`（versionCode `2702`）
- minSdk：26
- targetSdk / compileSdk：36
- Android Build Tools：36.0.0
- Gradle：8.13
- Android Gradle Plugin：8.13.2
- Kotlin：2.3.20
- JDK：17
- Room schema：7（未改变）
- WebDAV JSON：v2（未改变）

## 2.7.2 改动

- 搜索焦点逻辑区分首次未聚焦与真实失焦：首次点击不会闪烁关闭；空查询在真实失焦后折叠，非空查询保留；输入法完成只隐藏键盘，关闭动作清空并恢复原智能视图。
- 快速新增栏在加号左侧新增仅显示手绘图形的分组选择；浮层含当前项、所有有效分组和“管理分组”，新增时把所选 `groupId` 写入 Todo，连续录入保留选择，失效选择回退“日常”。
- 加入 16 个固定分组图标和图标/颜色编辑网格；旧空键、`folder`、`leaf` 占位键按稳定顺序幂等规范化，不改变分组 ID 或 Todo 归属。
- OpenAI、Anthropic、Gemini、OpenRouter、DeepSeek、Kimi、通义千问、智谱 GLM、SiliconFlow、MiniMax、豆包、xAI、Mistral、Groq、Ollama 与自定义服务均使用独立手绘标识，并覆盖预设、卡片、编辑器、总结选择与范围总结来源。
- 标准齿轮改为不规则调节线图形；静态合同阻止 Material Icons、`android.R.drawable` 和 Unicode 功能箭头进入应用 UI。
- 启动图标重构为米白手绘日记页、左侧装订、三行待办、小对勾、未完成圆圈和右上青柠切片；同步更新 adaptive、普通、圆形和 Android 13 monochrome 资源。

## 自动化验证

### JVM 单元测试

`testDebugUnitTest`：54 项通过，0 失败，0 errors。

- 新增搜索焦点状态机、快速分组接线、16 图标稳定分配、模型预设唯一图标映射、未知映射 fallback、启动图标资源和禁止系统图标合同。
- 继续覆盖重复日期、智能总结、四类 LLM 协议、模型缓存、WebDAV v1/v2、同步合并、每日进度、复盘映射和撤销批次。

### Android 测试

- `assembleDebugAndroidTest`：通过，33 项仪器测试方法及测试 APK 编译成功。
- 新增快速分组选择并传入正确 `groupId`、搜索首次展开不闪退/非空失焦/空查询折叠/关闭恢复，以及旧分组图标只规范化一次并保留 Todo 引用。
- 本机构建环境没有 `adb` 设备运行器，也没有连接 Android 设备或 AVD，因此没有执行 `connectedDebugAndroidTest`；这里只报告编译成功，不宣称真机执行通过。

### Lint

- `lintDebug`：0 errors，15 warnings。
- 警告为预测返回属性在 API 33 以下被忽略、依赖/Gradle 更新提示、KAPT 迁移建议，以及按需求仅支持 ARM64 导致的 ChromeOS x86_64 提示。

## APK 检查

- `assembleRelease` 与 release vital lint：通过。
- `aapt dump badging`：Application ID、2.7.2 / 2702、minSdk 26、targetSdk 36 正确。
- APK 中仅包含 `lib/arm64-v8a/libandroidx.graphics.path.so`，没有其他 ABI。
- `apksigner verify --verbose --print-certs`：v2 签名通过，签名者为开发交付用 Android Debug 证书。
- `unzip -t`：压缩内容完整，无错误。
- DEX 凭据模式扫描：未发现 GitHub token、常见 LLM/API Key、AWS Key 或私钥头。
- 合并 Manifest 的 `icon` / `roundIcon` 指向新版 mipmap，APK 资源表包含新版 foreground 与 monochrome。

## 分支与发布

- `main`：Kotlin/Compose 2.7.2 实现。
- 目标注释标签与公开 GitHub Release：`v2.7.2`。
- 发布完成后从 GitHub 重新下载 APK，与仓库交付物逐字节比较并在本节记录结果。

## 构建缓存清理

- 发布 APK、测试证据与报告落盘后停止 Gradle daemon。
- 删除项目 `.gradle`、`app/build` 和 `.kotlin` 会话缓存；保留项目 `.toolchain/android-sdk` 与用户级 `~/.gradle` 作为可复用工具链。
