# 青柠日记

青柠日记是一款原生 Android 每日待办与复盘应用。每日待办是主页核心，复盘通过独立二级页面进入；每日总结位于复盘流程，周、月、季度和自定义范围总结位于独立“总结”主页面。

完整产品与技术规格见 [SPEC.md](SPEC.md)。

## 下载

- [LimeDay 2.7.0 ARM64 APK](releases/LimeDay-2.7.0-arm64-v8a.apk)
- SHA-256：`F965ED914F62CC61EBDFF3F78A64FD5AF0C48853DB8098EF07B58BB984123471`

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose + ViewModel + StateFlow
- Room/SQLite，本地优先并支持软删除与冲突合并
- OkHttp，支持 WebDAV 与 OpenAI Chat、OpenAI Responses、Anthropic、Gemini 四类 BYOK 模型接口
- WorkManager，执行有网络约束的后台 WebDAV 同步
- Android Keystore AES/GCM，保存 WebDAV 密码和 LLM API Key

## 当前能力

- 日期切换、每日待办 CRUD、优先级、移动日期、复制和完成进度
- 独立计划日与截止日期/时间、单项提醒、每天/工作日/每周/每月/自定义重复
- 一级待办分组、内置收件箱、可排序步骤，以及标题/备注/步骤本地搜索
- 当天、逾期、计划中智能视图和按分组折叠显示
- 可点击日期卡片与月份快速跳转月历，使用待办状态圆点提示
- 双字段每日复盘、旧复盘无损合并、当日待办操作和 500ms 自动保存
- 可收回的待办左滑操作、6 秒批量删除撤销；回收站支持多选、全选、批量恢复、永久删除和清空
- 待办增删、完成、进度和页面切换动效，以及自绘待办操作图标
- “待办、总结、设置”三级底部主导航，待办和总结相关设置集中管理
- 涂鸦风应用图标、待办页顶部装饰、自绘开关，以及全套 Compose Canvas 自绘导航与操作图标
- 跟随系统/浅色/深色外观模式和每日待办、复盘提醒
- JSON 数据导入导出、版本信息和隐私说明
- CC Switch 风格的多模型服务卡片管理、16 个服务预设、同厂商多配置、固定编辑入口、默认切换、复制、排序和连接检查
- OpenAI Chat、OpenAI Responses、Anthropic Messages、Gemini Native 协议与按协议获取模型
- 首次保存前手动获取模型、24 小时加密缓存、手动模型兜底及 Ollama/LAN HTTP 显式风险开关
- 默认关闭的智能总结总开关；每日总结快捷/自由指令、收藏和最近记录，以及单次供应商与模型覆盖
- 独立总结页，支持本周、本月、本季度、自定义范围、超长记录分段综合、默认折叠历史和软删除
- WebDAV 连接测试、首次建目录、双向合并和后台同步
- WebDAV 独立二级设置页
- Room v1/v2、Flutter/Drift v3、Room v4/v5/v6 到 Room v7 的非破坏迁移
- 浅色/深色模式、边到边布局和 TalkBack 语义

## 开发环境

- JDK 17
- Android SDK 36
- Android Build Tools 36.0.0
- ARM64 Android 8.0（API 26）及以上设备

```shell
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
./gradlew lintDebug
./gradlew assembleRelease
```

release 构建仅生成 `arm64-v8a` APK：

```text
app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

当前仓库交付包使用 debug key 签名，仅用于开发测试。提交应用商店前必须配置长期保管的正式发布密钥。

## WebDAV

从设置页进入 WebDAV 二级页面，填写 HTTPS WebDAV 根地址、用户名、密码和远端目录。应用写入 `<目录>/limeday-sync-v2.json`；仅在 v2 不存在时回退读取旧 `limeday-sync-v1.json`，迁移后只写 v2。

同步过程先下载远端快照，再按 `updatedAt`、`revision`、`deviceId` 合并，事务写入本地后上传合并结果。v2 同步待办扩展字段、分组、步骤和永久删除墓碑；密码、供应商端点、模型缓存和 API Key 不进入数据库或同步文件。
