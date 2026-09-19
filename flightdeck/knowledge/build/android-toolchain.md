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
