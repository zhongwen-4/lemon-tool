# 本机 C++ / Android 构建环境清单

SUMMARY: 本机已有 Android SDK（D:\Android\Sdk）和 VS BuildTools 的 MSVC 14.44 + cmake + ninja
（都不在 PATH，必须用绝对路径），但**没装 NDK**——Android 交叉编译前必须先补上。
本机 **JDK 只有 Temurin 25**（没有 17/21），这是 IDE / Gradle 那串报错的根源，见文末。
**本机现在能编 Kotlin**（2026-09-25 起，靠用户级 gradle.properties 里的代理配置；打 APK 仍要 CI）。
READ WHEN: when 要编译 C++、要配 Android/NDK 构建，或构建时报找不到 cmake / ninja / cl.exe / clang，
或 IDE 报 `app/build.gradle` 里的类解析不了、gradle 报 `Unsupported class file major version` 时。
RECHECK WHEN: 装完 NDK、本机补装 JDK 17/21、或 VS / SDK 升级之后。

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
- JDK：**只有 Temurin 25**（`C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot`，在 PATH，
  `JAVA_HOME` 为空）。没有 17 / 21 —— 这一条是 IDE 与 Gradle 报错的根源，见文末。

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

## Gradle wrapper 与 IDE 的脚本 classpath（2026-09-21 立，2026-09-24 纠正）

- 症状：IDE 报 `app/build.gradle` 第 1 行
  `unable to resolve class org.jetbrains.kotlin.gradle.dsl.JvmTarget @ line 1, column 8.`（来源标 Gradle）。
- **真正的根因（2026-09-24 定位）：本机只有 Temurin JDK 25。** Gradle 8.13 自带的 Groovy 3.0.22
  **读不了 Java 25 的 class 文件（major version 69）**，构建脚本在语义分析阶段就崩，IDE 拿不到脚本
  classpath，于是把 KGP 的类型报成「未解析」。复现（原文）：

  ```
  gradlew.bat projects --offline --no-daemon
  → BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
    Unsupported class file major version 69
  ```

  所以那行 import 和 wrapper 本身都没错，**CI 一直绿**（CI 的 actions/setup-java 装的是 JDK 17）。
- **2026-09-21 那次的判断是错的**：当时以为「补上 wrapper 就完事」，本机验证用的却是缓存的
  Gradle **9.2.0**（9.2.0 的 Groovy 读得了 Java 25），所以显得全过；而 IDE 走 wrapper 的 8.13，照旧崩。
- **修法（2026-09-24）**：wrapper 的 `distributionUrl` 改指 **`gradle-9.2.0-bin.zip`**（本机已有该 dist，
  同一 URL → 哈希相同 → 直接复用、不联网）。实测 Gradle 9.2.0 + AGP 8.11.1 + KGP 2.3.20 在 Java 25 上：
  `projects` 与 `:app:assembleRelease --dry-run` 都 BUILD SUCCESSFUL（`buildCMakeRelWithDebInfo
  [arm64-v8a]`、`checkKotlinGradlePluginConfigurationErrors` 等任务都正常注册）。
- **代价（有意留的分叉）**：`.github/workflows/android.yml` 仍用 `gradle-version: '8.13'` + JDK 17，
  CI 那条 APK 链路是已验证的，不动。于是 **wrapper(9.2.0) ≠ CI(8.13)**。本机本来就编不了 APK（没 NDK），
  实际影响很小；要合一，得先在 CI 上验一遍 Gradle 9 的 `assembleRelease`。
- 备选修法（不改版本，但要在本机装东西）：装一个 JDK 17/21 让 Gradle 用它 —— 写进
  `~/.gradle/gradle.properties` 的 `org.gradle.java.home`，**别写进仓库的 `gradle.properties`**，
  那会连 CI 一起改坏。好处是 wrapper 与 CI 保持同版本。
- 其它结论不变：`.gitattributes` 里 `gradlew` 强制 LF（否则 CI 上 `bad interpreter`）、`gradlew.bat`
  CRLF、`gradle-wrapper.jar` 标 binary；wrapper 里保留 `networkTimeout=60000` +
  `validateDistributionUrl=false`（本机到 `services.gradle.org` 不稳：000 / 超时 / 偶发 307，
  国产镜像也都不通）。
- 生成 wrapper 的老办法（离线）：临时目录先 `touch settings.gradle`（Gradle 9 在空目录报
  `does not contain a Gradle build`），再用缓存 dist 的 Gradle 跑
  `gradle wrapper --gradle-distribution-url ... --no-validate-url`（**`--no-validate-url` 必须加**：
  默认的 URL 校验是一次联网 HEAD，超时会打死整个任务）。## 本机能编 Kotlin 了（2026-09-25）

- 症状：`.\gradlew.bat :app:compileDebugKotlin` 报 `Could not resolve top.yukonga.miuix.kmp:miuix-icons-android:0.8.8`
  → `Connect to repo.maven.apache.org:443 [repo.maven.apache.org/198.18.0.66] failed: Connection timed out`。
- 根因：**JVM 不读 Windows 的 WinINET 代理设置**。本机系统代理是 `127.0.0.1:7890`
  （注册表 `HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings` 里 `ProxyEnable=1`、
  `ProxyServer=127.0.0.1:7890`）。PowerShell 的 `Invoke-*` 走它，所以能上 `dl.google.com`；
  **Gradle（JVM）和 `curl.exe` 都不走**，直连还会把 maven 域名解析成 `198.18.0.66`（假地址）→ 超时。
- 修法：在**用户级** `C:\Users\admin\.gradle\gradle.properties`（**仓库外，不影响 CI**）写：

  ```
  systemProp.http.proxyHost=127.0.0.1
  systemProp.http.proxyPort=7890
  systemProp.https.proxyHost=127.0.0.1
  systemProp.https.proxyPort=7890
  systemProp.http.nonProxyHosts=localhost|127.*|[::1]
  ```

- 结果：`:app:compileDebugKotlin` 与 `:app:compileReleaseKotlin` 都 **BUILD SUCCESSFUL**
  （首次拉依赖约 26 s，之后几秒）。**本机从此能验证 Kotlin 改动的语法与类型**，不必推 CI 等结果。
- 仍然做不到的：`assembleRelease` 需要 NDK `27.0.12077973`（本机没装），**打 APK 依旧是 CI 的活**。
- 代理端口变了就改这个文件；删掉它即恢复原状。
