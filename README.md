# 青柠日记

青柠日记是一款离线优先的 Android 每日计划与复盘应用。当前 2.0 版本使用 Flutter 重构，产品与技术要求见 [SPEC.md](SPEC.md)。

## 当前能力

- 按日期管理待办，支持编辑、完成和软删除。
- 自动保存每日复盘与心情评分。
- 日历标记存在内容的日期。
- 支持 OpenAI 兼容、Anthropic 和 Gemini 三类自带密钥接口。
- 支持浅色、深色、系统动态配色和手机/平板自适应布局。
- 可从旧 Kotlin/Room 1.1 版本无损迁移数据和模型配置。

## 技术栈

- Flutter 3.44 / Dart 3.12
- Material 3 + Riverpod
- Drift + SQLite
- Dio
- Android Keystore 安全存储

当前交付目标仅为 Android。Windows、macOS、WebDAV 和 OneDrive 同步属于后续阶段。

## 开发环境

- JDK 17
- Android SDK 36 / Build Tools 36
- Android minSdk 26

本仓库可使用项目本地 `.toolchain`，该目录不会提交到 Git：

```shell
export PATH="$PWD/.toolchain/flutter/bin:$PATH"
export ANDROID_HOME="$PWD/.toolchain/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PUB_CACHE="$PWD/.toolchain/pub-cache"
export HOME="$PWD/.home"
```

安装依赖并生成 Drift 代码：

```shell
flutter pub get
dart run build_runner build
```

## 验证与构建

```shell
flutter analyze
flutter test
flutter build apk --release
```

Release APK 输出到：

```text
build/app/outputs/flutter-apk/app-release.apk
```

当前 Release 构建为了内部测试使用 Android debug key 签名。正式上架前必须配置长期保存的发布密钥。

## 数据与隐私

待办、复盘和总结保存在应用私有 SQLite 数据库中。API Key 使用 Android Keystore 支持的安全存储，不进入数据库、日志和同步数据。调用智能总结前，应用会提示当日记录将发送给所选模型服务商。

旧版实现保存在 Git 分支 `legacy/android-kotlin-1.1`。
