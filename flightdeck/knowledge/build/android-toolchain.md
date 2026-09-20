# 本机 C++ / Android 构建环境清单

SUMMARY: 本机已有 Android SDK（D:\Android\Sdk）和 VS BuildTools 的 MSVC 14.44 + cmake + ninja
（都不在 PATH，必须用绝对路径），但**没装 NDK**——Android 交叉编译前必须先补上。
READ WHEN: when 要编译 C++、要配 Android/NDK 构建，或构建时报找不到 cmake / ninja / cl.exe / clang 时。
RECHECK WHEN: 装完 NDK、或 VS / SDK 升级之后。

---

## 可用的宿主工具链（本机已有，无需下载）

- MSVC 14.44.35207：`C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Tools\MSVC\14.44.35207\bin\Hostx64\x64\cl.exe`
- cmake：`C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe`
- ninja：`...\Microsoft\CMake\Ninja\ninja.exe`（与 cmake 同级的 Ninja 目录）
- 另有 VS 18 BuildTools：`C:\Program Files (x86)\Microsoft Visual Studio\18\BuildTools`（`vswhere -latest` 指向它）

坑：这些可执行文件**都不在 PATH**，直接敲 `cmake` / `ninja` / `cl` 会 CommandNotFound
（`C:\Program Files\Microsoft Visual Studio\2022` 和 `\18` 这两个目录是空的，别被误导）。
要么用绝对路径，要么先跑 `vcvars64.bat` 再调。

## Android 侧

- SDK：`D:\Android\Sdk`，已装 cmdline-tools(latest)、build-tools 35/36、platforms android-36、licenses 已接受。
- **NDK 未安装**：`sdkmanager --list` 有 `ndk;<版本>` 与 `cmake;<版本>` 可装。
- sdkmanager：`D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`（java 25 已在 PATH）。
- adb：`D:\platform-tools\adb.exe` 和 `D:\Android\Sdk\platform-tools\adb.exe` 各一份；探测时无设备连接。

坑：`sdkmanager --list` 首次联网查仓库要 1–2 分钟才返回，不是卡死；它还会把 batch 脚本内容
回显一大串到 stdout，过滤时要认准真正的包列表行。

## 其它现成工具

- Git Bash：`D:\Git\bin\bash.exe`（PATH 里没有 bash，要显式调）
- 7-Zip：`D:\7z\7z.exe`
- Python 3.11：`C:\Users\admin\AppData\Local\Programs\Python\Python311\python.exe`

## 宿主构建命令（已验证可用）

PowerShell 里一行搞定（vcvars + cmake + ninja 都要绝对路径）：

```powershell
$bt = 'C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools'
$cmd = "call `"$bt\VC\Auxiliary\Build\vcvars64.bat`" >nul && " +
       "`"$bt\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe`" " +
       "-S D:\lemon_tool\core -B D:\lemon_tool\build\host -G Ninja " +
       "-DCMAKE_MAKE_PROGRAM=`"$bt\Common7\IDE\CommonExtensions\Microsoft\CMake\Ninja\ninja.exe`" " +
       "-DCMAKE_BUILD_TYPE=Release && " +
       "`"$bt\Common7\IDE\CommonExtensions\Microsoft\CMake\CMake\bin\cmake.exe`" --build D:\lemon_tool\build\host"
cmd /c $cmd
```

测试：`powershell -NoProfile -File D:\lemon_tool\core\tests\run_tests.ps1`。
看中文报告前先 `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8`，否则终端里是乱码。

Android 侧的 APK 构建**不在本机做**：GitHub Actions 负责（`.github/workflows/android.yml`）。

---

## Gradle wrapper 与 IDE 脚本 classpath（2026-09-21）

- 仓库原来**没有 wrapper**（`gradlew` / `gradle/wrapper/` 全无），IDE 只能借用它自己找到的 Gradle。
  一旦那个 Gradle 不是 8.13 档，`app/build.gradle` 第 1 行 `import org.jetbrains.kotlin.gradle.dsl.JvmTarget`
  就报 `unable to resolve class ... @ line 1, column 8.` —— 这是**脚本编译期 classpath 里没有 KGP**，
  不是代码错（CI 一直绿）。
- 已补 wrapper 锁 **8.13**，与 `.github/workflows/android.yml` 的 `gradle-version: '8.13'` 对齐；
  同时加 `.gitattributes`：`gradlew` 强制 LF（否则 Linux/CI 上 `bad interpreter`）、`gradlew.bat` CRLF、
  `gradle-wrapper.jar` 标 binary。
- 生成方式（离线，不下载）：临时目录先 `touch settings.gradle`（Gradle 9 在空目录直接报
  `does not contain a Gradle build`），再用本机缓存 dist 的 Gradle 跑
  `gradle wrapper --gradle-distribution-url .../gradle-8.13-bin.zip --no-validate-url`。
  **`--no-validate-url` 必须加**：默认的 URL 校验是一次联网 HEAD，超时会让整个 wrapper 任务失败。
- 本机实测（缓存 dist 只有 9.2.0 / 9.5.0 / 9.5.1）：`gradle projects --offline --no-daemon`
  → BUILD SUCCESSFUL，脚本编译与 AGP 加载全过；说明那行 import 在正常 classpath 下没问题。
- 本机到 Gradle 官方分发站**不稳定**：`services.gradle.org` 实测 000 / 21s 超时（偶发 307），
  `downloads.gradle.org` 307 跳 github；国产镜像没一条通（腾讯 000、阿里 404）。
  因此 wrapper 里写了 `networkTimeout=60000` + `validateDistributionUrl=false`。
  首次跑 wrapper 拉不动 8.13 时，把 `distributionUrl` 换成能通的镜像或本地包即可。
- CI **不受影响**：workflow 走的是 `gradle assembleRelease`（setup-gradle 装的那个），不经过 `./gradlew`。