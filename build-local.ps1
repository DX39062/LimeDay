$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdk = Get-ChildItem "$root\.toolchain\jdk" -Directory | Select-Object -First 1
if (-not $jdk) { throw "未找到 .toolchain/jdk 中的 JDK，请先安装本地工具链。" }

$env:JAVA_HOME = $jdk.FullName
$env:ANDROID_HOME = "$root\.toolchain\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = "$root\.toolchain\gradle-home"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

& "$root\gradlew.bat" testDebugUnitTest assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = "$root\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "构建结束但未找到 APK：$apk" }
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
