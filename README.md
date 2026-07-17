# 青柠日记

青柠日记是一款原生 Android 每日待办与复盘应用。每日待办是主页核心，复盘通过独立二级页面进入，LLM 总结也只在复盘流程中提供。

完整产品与技术规格见 [SPEC.md](SPEC.md)。

## 下载

- [LimeDay 2.2.0 ARM64 APK](releases/LimeDay-2.2.0-arm64-v8a.apk)
- SHA-256：`E04DAA04C83A4F0F1166305CB0155E6B8C8AF76B3C0E11E6629B82B76630C4D5`

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Navigation Compose + ViewModel + StateFlow
- Room/SQLite，本地优先并支持软删除与冲突合并
- OkHttp，支持 WebDAV 与三类 BYOK 模型接口
- WorkManager，执行有网络约束的后台 WebDAV 同步
- Android Keystore AES/GCM，保存 WebDAV 密码和 LLM API Key

## 当前能力

- 日期切换、每日待办 CRUD、完成进度
- 独立每日复盘页面、当日待办操作和 500ms 自动保存
- 待办左滑删除、5 秒撤销及无障碍删除入口
- 跟随系统/浅色/深色外观模式和每日待办、复盘提醒
- JSON 数据导入导出、版本信息和隐私说明
- OpenAI 兼容、Anthropic、Google Gemini 总结
- WebDAV 连接测试、首次建目录、双向合并和后台同步
- Room v1/v2 与 Flutter/Drift v3 到 Room v4 的非破坏迁移
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

在设置页填写 HTTPS WebDAV 根地址、用户名、密码和远端目录。应用使用 `<目录>/limeday-sync-v1.json` 保存平台无关的完整数据快照。

同步过程先下载远端快照，再按 `updatedAt`、`revision`、`deviceId` 合并，事务写入本地后上传合并结果。密码和 API Key 不进入数据库或同步文件。
