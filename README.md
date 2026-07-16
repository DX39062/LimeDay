# 青柠日记

一款清新的 Android 每日待办、复盘与智能总结应用。产品和技术规格见 [SPEC.md](SPEC.md)。

## 开发环境

- Android Studio Meerkat（2024.3.1）或更高版本
- JDK 17
- Android SDK 36

在 Android Studio 中打开项目，等待 Gradle 同步后运行 `app`。命令行环境安装 JDK 17 后可执行：

```shell
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

当前工程面向 Android 16（API 36），最低支持 Android 8.0（API 26）。待办、复盘和总结通过 Room 保存在设备本地。

## 智能总结

设置页面支持三类自带密钥（BYOK）接口：

- OpenAI Chat Completions 兼容接口，可自定义 Base URL 和模型。
- Anthropic Messages API。
- Google Gemini GenerateContent API。

API Key 使用 Android Keystore 的 AES/GCM 密钥加密，仅保存在设备本机。应用没有内置密钥；生成总结时，当前日期的待办与复盘内容会发送给用户选择的模型服务商。

## 本地工具链构建

仓库根目录的 `build-local.ps1` 会使用项目内 `.toolchain` 目录中的 JDK 与 Android SDK：

```powershell
.\build-local.ps1
```

成功后 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

