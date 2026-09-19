# module-risk-scanner

状态：进行中。已定：Android 设备端、C++ 核心 + MiuiX(Compose) 界面的 APK、只检查未安装的模块包。

## 已定决策

- 2026-09-20 运行位置：在 Android 设备上跑。
- 2026-09-20 技术栈：C++ 核心（CMake）+ Kotlin/Compose 外层，UI 用 MiuiX 组件库。
- 2026-09-20 界面：`top.yukonga.miuix.kmp:miuix-android:0.8.8`（见
  `knowledge/android/miuix-0.8.8.md`）。**它硬要求 compileSdk 36**，compileSdk/targetSdk 已跟到 36。
- 2026-09-20 交付形态：APK，由 GitHub Actions 编译（`.github/workflows/android.yml`）。
- 2026-09-20 扫描范围：只检查**未安装**的模块包（用户选一个 zip）。不碰 `/data/adb/modules`，
  不需要 root，不联网。
- 2026-09-20 体积策略（如实记录代价）：核心侧仍然抠——`-Oz`、`-fno-exceptions`/`-fno-rtti`、
  `--gc-sections`、`-Wl,--exclude-libs,ALL`、单 ABI arm64-v8a、minSdk 24、release 开 R8 +
  shrinkResources。但引入 MiuiX/Compose 后，**原来的「尽量小」不再可能**：Compose 运行时本身就
  是几 MB 量级。CI 的体积守卫已从 4 MiB 放宽到 12 MiB。LTO 还没开（待评估收益）。

## 已完成

- `core/`：自研 DEFLATE 解压（零第三方依赖）+ zip 解析 + module.prop 解析 + 规则引擎 +
  报告输出（终端文本 / JSON）。含 20 条命令与混淆特征规则、URL/IP 提取、base64 长串检测、
  ELF 架构识别、setuid 位检测、`system/` 覆盖统计、开机与安装钩子清单。
- `core/tests/`：夹具 + `run_tests.ps1` 断言脚本。宿主 MSVC 下全部通过：deflate 与 stored
  两种压缩方式、目录扫描、JSON 输出、以及 `rm -rf /data/...` 不误报「删除根路径」的回归。
- `app/`：APK 外壳 —— 用 SAF 选 zip（不需要任何权限）→ 拷到缓存 → JNI 扫描 → 显示报告。
- `.github/workflows/android.yml`：两个 job。`core` 在 ubuntu 上 g++ 编核心并跑同一套断言；
  `apk` 装 NDK/CMake、生成临时签名、`assembleRelease`、体积守卫、上传产物。
- `app/` 的界面：MiuiX 组件写的主界面（`SmallTopAppBar` + `Card` + `Button` +
  `SmallTitle` + `Text`），SAF 选 zip → 后台线程扫描 → 用 JSON 结果渲染模块信息、
  高/中/低危计数、逐条发现（按等级配色）、提示与截断说明。
- `app/src/main/cpp/jni_bridge.cpp` 改为只暴露 `nativeScanJson`，返回核心的 JSON。

## 未验证 / 风险

- APK 构建路径本机跑不了（没有 NDK、也没有 gradle），只能靠 CI 验证。NDK
  `27.0.12077973` 与 AGP 8.6.1 / Gradle 8.9 的组合是首次尝试，CI 报错就按提示调版本。
- 真机没连过（`adb devices` 为空），报告在设备上的实际显示效果没验证过。
- **本机连不上 GitHub**：2026-09-20 试 `git fetch` 报 `Failed to connect to github.com
  port 443 after 21072 ms`。远程 `lemon-tool`（github.com/zhongwen-4/lemon-tool.git）已配置，
  远端停在 `af1133b`，本地领先 2 个提交。CI 要跑起来得先能推上去（多半需要开代理）。
- **Kotlin/Compose 这条构建链本机一次都没编过**：本机没有 Kotlin 编译器也没有 gradle，
  MiuiX 的 API 用法是靠拉 sources jar 逐个核对签名得来的（已核对：Text/Button/Card/Scaffold/
  SmallTopAppBar/SmallTitle/CircularProgressIndicator/MiuixTheme 及所用样式与色名）。第一次真
  编译会发生在 CI。
- **AGP 8.11.1 + Gradle 8.13 + Kotlin 2.3.20 这个组合没实测过**。AGP 对最低 Gradle 有硬性要求，
  CI 若报 `Minimum supported Gradle version is X`，按提示把 `gradle-version` 改到 X 即可。

## 下一步

1. 推到 GitHub 跑 workflow，确认 APK 能出且小于 4 MiB。
2. 装到真机，验证「选 zip → 出报告」整条链路。
3. 拿真实模块样本调规则、压误报。

## 进度

- 2026-09-20 建档；同日定下 Android + C++ + 体积优先。
- 2026-09-20 核心实现完成并在宿主端通过全部断言；APK 外壳与 CI 已就绪，待 CI 验证。

## Read now

- `knowledge/build/android-toolchain.md` — 宿主构建命令与本机工具链现状
- `knowledge/build/msvc-utf8-source.md` — 动 C++ 源码前扫一眼，省一次编译失败
- `knowledge/android/miuix-0.8.8.md` — 动界面（MiuiX/Compose）前必读

## Read if

- 要加检测规则 → 看 `core/src/rules.cpp` 的规则表格式（needle + 可选 tail 边界字符）
- 需要 Android 模块规范细节（module.prop、脚本钩子、system 覆盖方式）→
  先建 `knowledge/android/` 下的条目再读
