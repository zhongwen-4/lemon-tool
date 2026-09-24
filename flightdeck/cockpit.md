# Cockpit — lemon_tool

Focus:

做 root 管理器（Magisk / KernelSU / APatch）模块的风险检测工具：Android 端 APK，
C++ 核心 + MiuiX(Compose) 界面，只检查未安装的模块包。

## In flight

- `work/module-risk-scanner/` — 界面按 SukiSU 的观感改造：**悬浮底栏**（MiuiX `FloatingNavigationBar`，无毛玻璃）+ 横向 pager 滑动 + `material-icons-extended` 图标；版本 0.2.1/code3，**本机 Kotlin 编译已通过**，等 CI 出包与真机看观感

## Next

- 真机验证「选 zip → 出报告」整条链路（等用户测试）。
- 给 zip 条目数加上限（防内存被打爆）。
- 用真实模块样本压误报（现在的夹具是自造的，覆盖面有限）。
- 等真机截图判断新界面：状态卡与检测项卡的比例、胶囊与描边的对比度是否够。
- 2026-09-21：顶栏图标已按他要求加回；版本号 0.1.0 → **0.2.0**；「关于」页加了**更新检查**
  （Release `v0.2.0` 已发布，APK 1.24 MiB）。代价是加了 INTERNET 权限，「不联网」的措辞已改准。
- 2026-09-21：`uninstall.sh` 误报按他选的「甲」修掉（升级层不对卸载脚本生效 + 报告加提示），本机断言全过。

- 2026-09-24：底栏已按他选的「丁」加图标（`TextOnly` → `IconAndText`，主页图标用 `Scan`，关于用
  `Info`）。改动只能由 CI 验证，**等真机看观感**——图标 26dp、未选中态 alpha 0.4 会不会太淡。

- 2026-09-24：IDE 的 `JvmTarget` 未解析已修 —— 根因是本机只有 JDK 25，wrapper 锁的 Gradle 8.13 读不了
  Java 25 的 class 文件（Groovy 3.0.22 / major 69）；wrapper 改指 **9.2.0**，本机跑通配置与任务图。
  CI 仍走 `gradle-version: '8.13'` + JDK 17，两处**有意分叉**（要合得先在 CI 验 Gradle 9）。

- 2026-09-24 新约定：更新只提版本号、不动包名（记入 `briefing.md`）。但排查发现 **CI 每 run 现生成
  签名密钥**，不同 run 的 APK 装不上去——「升级」目前只能卸载重装。要真能更新得先固定密钥。

- 2026-09-24：已给 10 套候选图标库（官网 + 许可 + 接入方式，见 `knowledge/android/icon-libraries.md`）
  与 MiuiX 155 个图标的可搜索预览（`build/miuix-icons-preview.html`，不进仓库）；**等他挑一套 + 一个图标**，
  之后把那个 SVG 转成 `ImageVector` 换掉 `Scan`。

- 2026-09-25：用户要「整个 UI 抄 SukiSU Ultra 的代码、图标用 Tabler Icons」。核实后：它的代码是
  **GPL-3.0**，启动图标另有一份禁止提取/禁止其他 app 使用/衍生项目必须替换的单独许可，而本仓库
  **没有 LICENSE** → 照搬代码不可行；它的 `ui/` 有 **281 个 .kt / 2.0 MB**（每个界面写两遍 Material +
  Miuix），与本 App 的「扫描 + 关于」两页完全不对等；它底栏实际用的是 `material-icons-extended` 的
  `Icons.Rounded.*`（想要的房子＝`Cottage`）；**MiuiX 0.8.8 没有 blur/Backdrop**，要浮动模糊底栏得升 0.9.x。
  **已交回五个问题，等答复；答复前不动代码**（详见 `work/module-risk-scanner/index.md` 第 10 条）。

- 2026-09-25：UI 改造落地（用户定稿：抄 SukiSU 的布局观感、悬浮底栏、不要毛玻璃、图标用
  `material-icons-extended`）。底栏 → MiuiX `FloatingNavigationBar`（IconAndText），内容 → `HorizontalPager`，
  主页图标 `Icons.Rounded.Cottage`、关于 `Icons.Rounded.Info`，版本 **0.2.1 / code3**。
  **本机现在能编 Kotlin**（配好 JVM 代理后 `compileDebug/ReleaseKotlin` 全过），打 APK 仍靠 CI。
## Open questions


- 用户提「做个虚拟环境直接刷写模块看执行了哪些命令」：动态沙箱**进不了 APK**（已在本机实测假 PATH
  dry-run 并论证，见 `knowledge/detection/dry-run-sandbox.md`）。剩下的选择题：要不要在 APK 里做**静态**
  执行轨迹预览（按钩子分组列命令、零执行）？要不要另开一个 work 包做桌面/CI 侧 sandbox？
- 更新检查现在只查 `api.github.com`，**国内网络常常不通**（界面只能报检查失败）。要不要加镜像回退
  （比如 jsDelivr 读仓库里的 `version.json`）？
- 本机要不要也装 NDK + gradle（约 1.6 GB）？现状「CI 编 APK、本机编 C++ 跑测试」已经够用，
  暂不急。
- 要不要兼顾 32 位设备（armeabi-v7a）？现在只出 arm64-v8a。
