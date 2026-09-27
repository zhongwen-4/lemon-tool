# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — **界面整包换成 SukiSU Ultra 的骨架、数据全部留空**。
  用户 2026-09-28 改了口径：`你直接把现有的ui删掉，然后完全照搬sukisu的，功能不变`，
  随后细化成「主页只留组件骨架、数据清空」「检查历史用它的 SU 日志列表、其他组件不要」
  「其他的也要骨架、数据先空着」。三页（主页 / 关于 / 设置）照搬结构尺寸，检查历史整份删掉换 SU 日志列表
  （`History.kt` 已删）；`ScanUi.kt` 1041 → 358 行，只剩外壳 + `PageScaffold` + 扫描管线；
  版本 **0.5.0/code6**，commit `8517d3f` 已推。本机出包前置环节全过（debug/release Kotlin、
  两个 AAR 元数据检查、两个清单合并、R8、资源优化），**CI 在跑（run `36354483464`）**。
  **下一步就是给三页接真实数据**。

## Next

- **三页接数据**：主页 `HomeUiState`（内核/管理器版本这些本项目没有，要么去掉要么换成本项目的信息）；
  检查历史 = SU 日志，但本项目**没有 `/data/adb/ksu/log` 这个数据源**，得决定读什么
  （自己的扫描记录？自己的日志文件？）；设置页那一堆 root 管理器开关（SuSFS / KPM / LKM / ADB root /
  su compat / 卸载内核模块）与本项目无关，多半只留「关于 / 清理缓存 / 更新检查」几行。
- 主页大卡片的动作已经接到「选 zip」，但**扫描结果暂时不显示**；接回来只需把 `ScanState` 接到新界面上。
- 真机看三页观感：英雄卡实际高度与溢出图标的裁切、底栏胶囊拖动切页的手感、
  MiuiX 行组件在卡片里的间距。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 若要毛玻璃：**这条路现在被 minSdk 卡死** —— `miuix-blur-android:0.9.4` 的 manifest 硬要求
  minSdk 33，而本项目是 24（2026-09-27 已把该依赖删掉）。要做只能整体抬 minSdk，或自己实现模糊。

## 已落地（倒序；细节都在 work 包里）

- 2026-09-28：**界面整包换成 SukiSU 的骨架**（UI 口径）—— 主页 / 关于 / 设置照搬结构与尺寸、
  检查历史换成 SU 日志列表、三页数据全是空状态，`History.kt` 删掉，`ScanUi.kt` 重写。
  版本 0.5.0/code6，commit `8517d3f`。两个坑进了知识库：here-string 丢换行把 `package` 粘进注释
  （`knowledge/tooling/here-string-package-line.md`）、github.com 域名解到被墙 IP 时用本地 CONNECT 隧道推
  （`knowledge/tooling/github-push-when-ip-blocked.md`）。
- 2026-09-27：**修掉 CI 出包失败**（run `36281282791`）—— 升 MiuiX 0.9.4 时 compileSdk 留在 36，
  `apk` job 卡死在 `checkReleaseAarMetadata`；改成 `compileSdk 37` + `compileSdkMinor 2`
  （SDK 平台从 37 起按小版本发布，用 `android-37.2`；targetSdk 不动），并删掉硬要求 minSdk 33 的
  `miuix-blur` 依赖。commit `6af075b`，**CI 复跑（run `36282815228`）两个 job 全绿、出包成功**；
  详见 `knowledge/build/compile-sdk-and-aar-metadata.md`。
- 2026-09-27：移植 SukiSU 的界面代码 —— 只搬组件与骨架、只做 Miuix 一套、图标继续用
  `material-icons-extended`；三页按它的版式重画；GPL-3.0 义务已履行（每个搬进来的文件写出处与改动日期，
  它的启动图标一个字没搬）。commit `a05fdbb`。
- 2026-09-27 之前：工具链升到 **AGP 9.4.1 / Kotlin 2.4.20 / Gradle 9.7.1 / JDK 21 /
  compose-bom 2026.09.00 / MiuiX 0.9.4**（与 SukiSU 完全同一套）。所以 cockpit 里原来那条
  「要不要升 MiuiX 0.9.4 换取 preference 行组件」**已经做完**；本机与 CI 的 Gradle 分叉
  （本地 9.2.0 / CI 8.13）也一并消失，两边都是 9.7.1 + JDK 21。
- 2026-09-25：GPL-3.0 声明 + 根目录 `LICENSE`；CI 发布签名与权限修复；三 tab 底栏 + 检查历史。
- 更早的（核心、CI、莫奈取色、图标、主题、误报治理）见 `work/module-risk-scanner/index.md`。

## Open questions

- **三页的数据从哪来**：主页要显示什么（本项目没有内核版本/KernelSU 状态）、
  检查历史是显示扫描记录还是别的东西、设置页那一堆 root 管理器开关留哪些？
  这三件是「往后我要用」的填空，等你给方向。
- 用户提「做个虚拟环境直接刷写模块看执行了哪些命令」：动态沙箱**进不了 APK**（已在本机实测假 PATH
  dry-run 并论证，见 `knowledge/detection/dry-run-sandbox.md`）。剩下的选择题：要不要在 APK 里做**静态**
  执行轨迹预览（按钩子分组列命令、零执行）？要不要另开一个 work 包做桌面/CI 侧 sandbox？
- 更新检查现在只查 `api.github.com`，**国内网络常常不通**（界面只能报检查失败）。要不要加镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`）？
- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 与 Kotlin 跑测试」已经够用。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。