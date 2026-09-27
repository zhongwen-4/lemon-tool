# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — **界面骨架已按 SukiSU 摆好，下一步接真实数据**。
  三页口径用户已在 2026-09-28 定死：主页 = 「点此开始检测」+「应用版本」+「提交 BUG」+ **扫描结果**；
  检查历史 = **本项目自己的扫描记录**（本项目没有 `/data/adb/ksu/log` 这个数据源）；
  设置 = 关于 + 检查更新。取色走 SukiSU 的**非莫奈**那一支（不再用莫奈取色）。
  主页/设置页的精简与配色已落地、本机 8 条出包前置（debug/release Kotlin、两个 AAR 元数据、
  两个清单合并、R8、资源优化）全过；版本 **0.6.0/code7**、commit `c14ecbb` 已推，
  **CI run 28 两个 job 全绿（`core tests (host)` + `apk (arm64-v8a)`，已出包）**。
  用户接下来要的是：`然后实现一下检查模块和检查历史的功能`。

## Next

- **实现「检查模块」**：扫描链路（SAF 选 zip → `nativeScanJson` → `parseReport` → `ScanState`）已经全通，
  只差显示 —— 把 `ScanState` 接到主页：扫描中的进度态、结果卡（模块名/版本/作者/文件数 +
  高/中/低/信息计数 + `verdict`）、逐条 findings（按等级配色）、`notes` 与 `truncated`、失败态。
- **实现「检查历史」**：用本项目自己的扫描记录（`filesDir` 里一份 JSONL，条数上限约 200）喂
  SukiSU 的 SU 日志列表骨架（`SulogScreenMiuix` 的形状不动，`SulogScreenState.entries` 填映射后的记录）。
- 真机看三页观感：英雄卡实际高度与溢出图标的裁切、底栏胶囊拖动切页的手感、MiuiX 行组件在卡片里的间距。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 若要毛玻璃：**这条路现在被 minSdk 卡死** —— `miuix-blur-android:0.9.4` 的 manifest 硬要求
  minSdk 33，而本项目是 24（2026-09-27 已把该依赖删掉）。要做只能整体抬 minSdk，或自己实现模糊。

## 已落地（倒序；细节都在 work 包里）

- 2026-09-28：**主页 / 设置页精简收尾 + 去掉莫奈取色** —— 取色改走 `MiuixKernelSUTheme` 的非莫奈那一支；
  主页只留「点此开始检测 / 应用版本 / 提交 BUG」（状态卡的另两支与 SELinux + Seccomp 那张卡全删），
  设置页只留「检查更新 / 关于」，顺带删掉 `strings.xml` 里 79 条已无引用的文案。版本 0.6.0/code7，
  commit `c14ecbb`。三条知识：`android/theme-non-monet-sukisu.md`（新增）、
  `tooling/github-push-and-local-proxy.md`（改名并**推翻**昨天的结论：真凶是本机 7890 系统代理，
  PowerShell 走它、git 不走）、`tooling/file-edit-anchors-and-newlines.md`（补记切片端点差一位的第二次失手）。
- 2026-09-28：**界面整包换成 SukiSU 的骨架**（UI 口径）—— 主页 / 关于 / 设置照搬结构与尺寸、
  检查历史换成 SU 日志列表、三页数据全是空状态，`History.kt` 删掉，`ScanUi.kt` 重写（1041 → 358 行）。
  版本 0.5.0/code6，commit `8517d3f`。两个坑进了知识库：here-string 丢换行把 `package` 粘进注释
  （`knowledge/tooling/here-string-package-line.md`）、推 GitHub 连不上（即为上面那条，当时结论有误）。
- 2026-09-27：**修掉 CI 出包失败**（run `36281282791`）—— 升 MiuiX 0.9.4 时 compileSdk 留在 36，
  `apk` job 卡死在 `checkReleaseAarMetadata`；改成 `compileSdk 37` + `compileSdkMinor 2`
  （SDK 平台从 37 起按小版本发布，用 `android-37.2`；targetSdk 不动），并删掉硬要求 minSdk 33 的
  `miuix-blur` 依赖。commit `6af075b`，**CI 复跑（run `36282815228`）两个 job 全绿、出包成功**；
  详见 `knowledge/build/compile-sdk-and-aar-metadata.md`。
- 2026-09-27：移植 SukiSU 的界面代码 —— 只搬组件与骨架、只做 Miuix 一套、图标继续用
  `material-icons-extended`；三页按它的版式重画；GPL-3.0 义务已履行（每个搬进来的文件写出处与改动日期，
  它的启动图标一个字没搬）。commit `a05fdbb`。
- 2026-09-27 之前：工具链升到 **AGP 9.4.1 / Kotlin 2.4.20 / Gradle 9.7.1 / JDK 21 /
  compose-bom 2026.09.00 / MiuiX 0.9.4**（与 SukiSU 完全同一套）。所以原来那条
  「要不要升 MiuiX 0.9.4 换取 preference 行组件」**已经做完**；本机与 CI 的 Gradle 分叉也一并消失。
- 2026-09-25：GPL-3.0 声明 + 根目录 `LICENSE`；CI 发布签名与权限修复；三 tab 底栏 + 检查历史。
- 更早的（核心、CI、莫奈取色、图标、主题、误报治理）见 `work/module-risk-scanner/index.md`。

## Open questions

- 用户提「做个虚拟环境直接刷写模块看执行了哪些命令」：动态沙箱**进不了 APK**（已在本机实测假 PATH
  dry-run 并论证，见 `knowledge/detection/dry-run-sandbox.md`）。剩下的选择题：要不要在 APK 里做**静态**
  执行轨迹预览（按钩子分组列命令、零执行）？要不要另开一个 work 包做桌面/CI 侧 sandbox？
- 更新检查现在只查 `api.github.com`，**国内网络常常不通**（界面只能报「检查失败」）。要不要加镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`）？
- 设置页那个「检查更新」开关现在是本页自持（`rememberSaveable`，默认开），**没有接真正的自动检查逻辑**；
  要不要接上（进主页时后台检查 + 有新版就显示 UpdateCard）？
- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 与 Kotlin 跑测试」已经够用。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
