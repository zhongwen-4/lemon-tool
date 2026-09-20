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
- 2026-09-20 核心对外收敛成一个入口 `mrs::scan_path()`：用 `stat` + `S_ISREG` 判文件还是目录
  （原来 CLI 用 `fopen` 判定，**Linux 上 fopen 打开目录会成功**，导致目录被当 zip 解析，
  CI 的 `core` job 因此红过一次；见 `knowledge/build/posix-vs-win32-portability.md`）。
- 2026-09-20 **CI 首次全绿**：run `35475236439`（commit `646931e`）两个 job 都过，产出
  `mrs-apk`：`app-release.apk` **1127142 字节 ≈ 1.07 MiB**，内含
  `lib/arm64-v8a/libmrs_jni.so`（252 KB），远低于 12 MiB 预算。

- 2026-09-20 **界面整体重写（莫奈取色）**，`ScanUi.kt` 从 300 行长到 510 行：
  - 取色：`MiuixTheme(controller = ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem))`。
    坑：**不用 Monet 时默认卡片就是白底压白底**（静态主题里 `surfaceContainer = Color.White`），
    所以「比纯白背景深一点」这个诉求本身就要求走 Monet，莫奈下卡片底色才和页面底色分得开。
  - 顶部固定栏：**没用 `SmallTopAppBar`**（它的 `title` 是 `String`，塞不进图标+两行文字），
    自己写 Row 丢进 `Scaffold(topBar = { })`：应用图标 40dp + 3dp 间距 + 软件名(title3) +
    下方小字版本号(footnote2，从 PackageManager 读 versionName)。
  - 上部 40%：`Modifier.weight(0.4f)` 的单个 Card 面板，里面放 72dp 模块图标
    （`MiuixIcons.Layers`，扫描中换成转圈）、模块名/版本/作者/id，以及**唯一的操作按钮**
    （选 zip / 重新选择）。下部 60% 放检测项，两侧按权重切分屏幕高度，互不挤压。
  - 检测项：`LazyColumn` + 计数概览卡。**高危红框（`colorScheme.error`）、中危黄框
    （自定琥珀色，暗色下换亮一档）、低危与信息透明框**；点击整卡展开，展开区显示
    「说明」+ `文件(<文件名>)第 x 行` + 完整路径（等宽字体）。
  - 展开状态用 `ScannerScreen` 里的 `Set<Int>` 提升保管，**没有放在 item 内部 `remember`**
    ——LazyColumn 会回收 item，内部 remember 一滚就没。
- 2026-09-20 补上应用图标（**之前 `AndroidManifest.xml` 根本没有 `android:icon`，桌面是默认机器人**）：
  `drawable/ic_app_mark.xml`（矢量，头部直接用）+ 自适应图标
  （`ic_launcher_foreground.xml` + `mipmap-anydpi-v26/ic_launcher.xml`）+
  `mipmap-anydpi/ic_launcher.xml`（API 24-25 回退）。
- 2026-09-20 依赖加 `top.yukonga.miuix.kmp:miuix-icons-android:0.8.8`（图标不在 miuix-android 里，
  见 `knowledge/android/miuix-0.8.8.md`）。
- 2026-09-20 **CI 全绿**：run `35477558871`（commit `f381080`）两个 job 都过，
  `app-release.apk` **1273442 字节 ≈ 1.21 MiB**（上一版 1132958 ≈ 1.08 MiB，**+140 KB**）。
  体积增量已核实去向：图标构件只留下用到的 3 个（dex 里搜未引用图标名为 0 命中），
  涨的主要是新界面代码产生的 dex。
## 未验证 / 风险

- APK 构建路径本机跑不了（没有 NDK、也没有 gradle），只能靠 CI 验证。
- 真机没连过（`adb devices` 为空），报告在设备上的实际显示效果没验证过。
- ~~本机连不上 GitHub~~ 已解决：本机 FlClash 代理 `http://127.0.0.1:7890` 可用，
  `git -c http.proxy=http://127.0.0.1:7890 push lemon-tool main` 稳定成功。
  读 CI 日志的办法见 `knowledge/tooling/github-actions-logs-via-api.md`。
- **Kotlin/Compose 这条构建链本机一次都没编过**：本机没有 Kotlin 编译器也没有 gradle，
  MiuiX 的 API 用法是靠拉 sources jar 逐个核对签名得来的（已核对：Text/Button/Card/Scaffold/
  SmallTopAppBar/SmallTitle/CircularProgressIndicator/MiuixTheme 及所用样式与色名）。第一次真
  编译会发生在 CI。
- AGP 8.11.1 + Gradle 8.13 + Kotlin 2.3.20 + MiuiX/Compose 这条链**已由 CI 实测通过**
  （2026-09-20），不再算风险项。

- **界面渲染效果一次都没被看过**：本机编不了 Kotlin/Compose，CI 只保证「能编译、能出 APK」，
  布局是否好看、黄框对比度够不够、顶部 40% 面板会不会太空，都要等真机截图才能判断。
- 莫奈取色在 **Android 11 及以下会回落到 MiuiX 默认紫**（`platformDynamicColors` 在 API < 31
  直接给 `monetSystemColors()`），不是壁纸色；minSdk 24，真机得是 Android 12+ 才看得到莫奈效果。

## 已知缺陷

- ~~误报严重~~ **2026-09-20 已修**：规则拆成「特征表（最高中危）+ 升级表（组合/路径敏感才高危）」，
  并加了 `verdict` 定性结论。善意夹具断言写死「高危 0 · 中危 0」，恶意夹具断言 9 条升级规则全中。
  模型细节见 `knowledge/detection/rule-design.md`。
  残留风险：目前调参只靠自造的两个夹具，**还没有真实模块样本**，路径分档与升级阈值可能需要再调。
- **条目数没有上限**：`core/src/zip.cpp`/`scan.cpp` 对单个条目有 8 MB 上限，但没限制
  zip 条目数量；构造一个几十万条目的 zip 可以撑爆内存。需要加条目数上限并给报告截断。

## 下一步

1. ~~重构检测规则模型~~ 已完成（2026-09-20）。
2. 给 zip 条目数设上限。
3. 用真实模块样本压误报（现在是自造夹具，覆盖面有限）。
4. ~~推到 GitHub 跑 workflow~~ 已完成（见上）。
5. 装到真机，验证「选 zip → 出报告」整条链路 —— **等用户测试反馈**。

## 进度

- 2026-09-20 建档；同日定下 Android + C++ + 体积优先。
- 2026-09-20 核心实现完成并在宿主端通过全部断言；APK 外壳与 CI 已就绪，待 CI 验证。
- 2026-09-20 界面改为 MiuiX(Compose)，compileSdk 跟到 36；同日发现规则误报缺陷。
- 2026-09-20 修掉 Linux 上「目录被当 zip」的路径判定 bug；CI 两个 job 全绿，APK 1.07 MiB。
- 2026-09-20 重做规则模型（特征表 + 升级表）修掉误报：善意样本「高危 0 · 中危 0」，
  恶意样本 6 高危；报告与界面都加了定性结论。

## Read now

- `knowledge/build/android-toolchain.md` — 宿主构建命令与本机工具链现状
- `knowledge/build/msvc-utf8-source.md`、`knowledge/build/posix-vs-win32-portability.md`
  — 动 C++ 源码前扫一眼，省一次编译失败、省一次「CI 红而本机绿」
- `knowledge/android/miuix-0.8.8.md` — 动界面（MiuiX/Compose）前必读
- `knowledge/detection/rule-design.md` — 动检测规则前必读（误报是核心指标）

## Read if

- 要加检测规则 → 先读 `knowledge/detection/rule-design.md`，再看 `core/src/rules.cpp`
  的特征表格式（`id/sev/needle/detail`）与 `core/src/escalate.cpp` 的升级表
- 脚本断言「CI 全过、本机全红」→ 读 `knowledge/tooling/powershell-chinese-encoding-traps.md`
- 需要 Android 模块规范细节（module.prop、脚本钩子、system 覆盖方式）→
  先建 `knowledge/android/` 下的条目再读
- 要改界面（MiuiX/Compose）→ 先读 `knowledge/tooling/impeccable-on-android-project.md`
  （impeccable 在本项目的调法）与 `knowledge/android/miuix-0.8.8.md`（组件签名与硬约束）
