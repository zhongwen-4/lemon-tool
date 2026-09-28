# 移植 SukiSU Ultra 的界面代码：怎么做、搬了什么、哪里能整条砍掉

SUMMARY: 本项目已是 **GPL-3.0**（根目录 LICENSE），与 SukiSU 源码许可相容，所以**可以真搬它的代码**，
但每个搬进来的文件必须写出处（上游路径）+ 说明改了什么 + **改动日期**（GPL-3.0 §5a 要求）；
带 Apache-2.0 出处的那份（`FloatingBottomBar.kt` 源自 compose-miuix-ui 示例）要连它的文件头一起保留。
**它的启动图标是单独的许可，一律不搬**（禁提取、禁给别的 app 用）。搬法是「只搬组件与骨架、不搬页面业务」：
`ui/component` 的通用件、`ui/theme` 的取色思路、页面壳（Scaffold + TopAppBar + LazyColumn）；
`screen/` 里 134 个页面、`webui/`、`kernelFlash/`、模板编辑器**不搬**（数据源在本项目根本不存在）。
**「不要毛玻璃」这条一决定，blur/liquid/InteractiveHighlight 整条链都不用搬**——上游那个
`InteractiveHighlight` 只在 `isBlurEnabled` 那一路里用，纯色路径不需要它，顺带避开
`android.graphics.RuntimeShader`（要 API 33）与本项目 minSdk 24 的冲突。
**两个例外 / 补充（2026-09-28）**：① 关于页的 OS3 渐变背景**不是毛玻璃**，它只在运行时门控
API 33/35，已经搬进来（见 `about-bg-effect-shader.md`）——「不搬 blur」与「搬那个背景」并不冲突；
② 上游页面的「返回」由 navigation3 的路由栈管，本项目没有导航库，**凡是从 `ScannerScreen` 切出去的子页
都得自己接 `BackHandler`**（不接的话在子页按返回直接退出 App；关于页就是这么修的）。
READ WHEN: before 继续移植 SukiSU（或 KernelSU 系）的界面代码、或要给本项目加新界面时。
RECHECK WHEN: 上游大改目录结构，或本项目决定启用毛玻璃、或引入导航库之后。

---

## 怎么拿到源码（本机通道实测）

```powershell
$h = @{ 'User-Agent' = 'codex'; 'Accept' = 'application/vnd.github.raw' }
Invoke-RestMethod -Uri "https://api.github.com/repos/SukiSU-Ultra/SukiSU-Ultra/contents/manager/app/src/main/java/com/sukisu/ultra/ui/<相对路径>?ref=main" -Headers $h
```
- 先走 `git/trees/main?recursive=1` 看规模与目录（一次拿全树，比逐个 contents 便宜得多），
  再按需拉单个文件。`raw.githubusercontent.com` 与 `curl` 在本机都不通，别走那两条。
- 路径前缀是 `manager/app/src/main/java/com/sukisu/ultra/ui/`；`ui/` 下有 281 个 .kt / 约 2.07 MB。

## 已经搬进来的（commit a05fdbb，2026-09-27）

| 本项目文件 | 上游 | 改了什么 |
| --- | --- | --- |
| `ui/component/FloatingBottomBar.kt` | `ui/component/FloatingBottomBar.kt` | 砍掉毛玻璃那一路与 `isBlurEnabled`/`backdrop` 参数；去掉只在毛玻璃路用的 `InteractiveHighlight`；深色判断改用 surface 亮度 |
| `ui/component/miuix/animation/DampedDragAnimation.kt` | 同名 | **逐字相同**，只改包名与 `inspectDragGestures` 的 import |
| `ui/component/miuix/modifier/DragGestureInspector.kt` | 同名 | **逐字相同**，只改包名 |
| `ui/component/bottombar/MainPagerState.kt` | `ui/component/bottombar/BottomBar.kt` 里的一段 | 只取 `MainPagerState`/`rememberMainPagerState` + `LocalMainPagerState`；翻页动画换成 `animateScrollToPage` |
| `ui/component/Rows.kt` | 无（本项目自写） | 把上游设置页重复的排版收成 `InfoRow`/`ActionRow`，底座还是 MiuiX 的 `BasicComponent`/`ArrowPreference` |

页面壳的做法（照它的 `HomePagerMiuix` / `SettingPagerMiuix`）：每页自己拿
`Scaffold(topBar = TopAppBar(…), contentWindowInsets = systemBars.add(displayCutout).only(Horizontal))`，
里面一条 `LazyColumn`：`padding(horizontal = 12.dp)` + `.overScrollVertical()` + `.scrollEndHaptic()`
+ `.nestedScroll(scrollBehavior.nestedScrollConnection)`，`contentPadding = innerPadding`，
内容尽量收进**一个 item 的 Column**（`verticalArrangement = spacedBy(12.dp)`），底部用
`Spacer(bottomInnerPadding + 12.dp)` 给悬浮底栏让位。

英雄卡（它的 `StatusCard`）的写法值得记住：`Card` 里套
`Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min))`，里面三个 `Box(Modifier.fillMaxSize()…)` 叠加——
右下角一枚 **110dp 大图标 `offset(27.dp, 31.dp)` 溢出**、左上角 `padding(16.dp, 14.dp)` 放 22.sp SemiBold 大字、
左下角 `padding(16.dp, 10.dp)` 放操作。`height(IntrinsicSize.Min)` 不能省，否则 `fillMaxSize` 在 LazyColumn
里拿到的是无界高度。

## 不搬的东西与理由

- `screen/**`（134 个 .kt / 1.3 MB）、`util/KsuCli.kt`、`viewmodel/**`：依赖 root 状态、内核版本、
  SuSFS/KPM/WebUI —— 本项目是「扫未安装的 zip」，没有这些数据源。
- `webui/`、`kernelFlash/`、`templateeditor/`、`markdown/`：功能不存在。
- 它每个界面写两遍（`XxxMaterial.kt` + `XxxMiuix.kt`，由 `UiMode.kt` 切）。**本项目只做 Miuix 一套**
  （用户 2026-09-27 定），所以搬的时候只取 `*Miuix.kt`。
## 第二批：整包换界面（commit 8517d3f，2026-09-28，版本 0.5.0/code6）

用户口径变了两回，最终是「**把现有 UI 删掉，完全照搬 SukiSU 的，功能不变**」，紧接着两步细化：
「主页只留组件骨架、数据清空」「检查历史改用它的 SU 日志列表，其他组件不要」「其他的也要骨架、数据先空着」。
所以这一批搬的是**骨架**，一行业务数据都没接。

| 本项目文件 | 上游 | 改了什么 |
| --- | --- | --- |
| `ui/screen/home/{HomeMiuix,HomeUiState,HomeUtils}.kt` | `screen/home/` 同名 | 结构尺寸一字不差；砍掉内核取数（`getZygiskImplementation` / `rememberSusfsInfo` / `rememberHookTypeLabel` 留同名空实现、`KernelVersion.isGKI()` 恒 false）；`HomeUiState` 每个字段补空默认值；删掉全部 `@Preview` |
| `ui/screen/about/{AboutMiuix,AboutScreen,AboutUiState,AboutUtils}.kt` | `screen/about/` 同名 | 同理；去掉 textureBlur / logoBlend |
| `ui/screen/sulog/{SulogListMiuix,SulogUiState}.kt` + `ui/util/sulog/SulogModels.kt` | `screen/sulog/` + `ui/util/SulogHelper.kt` | **只要列表**：条目卡 + 列表段 + 条目详情弹窗 + 取标题/描述/标签/返回值的几个函数；上游那页的顶栏（返回/清空/筛选）、SearchBox/SearchPager/SearchBarFake、日志文件下拉、PullToRefresh、状态提示卡全部不要。`SulogHelper` 只留数据形状（读 `/data/adb/ksu/log` 的函数不搬） |
| `ui/screen/settings/{SettingsMiuix,SettingsUiState}.kt` | `screen/settings/` 同名 | 脚本整包搬运（改包名 + 删一行 `layerBackdrop` 用法 + `selectedIndex` 恒 0），Card 分组与三种 Preference 一行没动 |
| `ui/component/{KsuIsValid,WarningLevel}.kt`、`ui/component/dialog/{ConfirmDialog,LoadingDialog}.kt`、`ui/component/miuix/{WarningCard,SendLogDialog}.kt`、`ui/component/statustag/*`、`ui/component/rebootlistpopup/RebootListPopupMiuix.kt`、`ui/theme/Theme.kt`、`ui/util/{BlurExt,LocaleHelper}.kt`、`ui/UiMode.kt` | 各家 | **同名同签名的空壳**：`KsuIsValid` 直接渲染 content、`UninstallDialog`/`SendLogDialog`/`RebootListPopup` 是 `= Unit`、`rememberLoadingDialog` 的 handle 照常能 withLoading 但不弹东西、`rememberBlurBackdrop` 恒 null、`UiMode` 只有 `Miuix` 一项、`LocaleHelper.SUPPORTED_TAGS` 是空表 |

**主界面换法**：`ScanUi.kt` 从 1041 行重写成 358 行——只留外壳（悬浮底栏 + HorizontalPager）、
`PageScaffold`（每页自己的 Scaffold+TopAppBar+一条 LazyColumn，给底栏留白）、扫描数据形状与
`parseReport`。老的 `History.kt`（`HistoryEntry`/`HistoryStore`/`HistoryScreen`）**整份删掉**。
扫描这一路（选包 → `nativeScanJson` → `parseReport`）保留，主页大卡片的动作已接到选包上，
但结果暂时不显示（用户要求数据先空着）。

**搬的时候的取舍（都是用户明说的）**：毛玻璃继续不做（`miuix-blur` 要 minSdk 33）；
只看 Miuix 一路；上游那一页里「打开另一个子页面」的 ArrowPreference 全部保留、
动作走 `SettingsScreenActions` 的空实现（主题/工具/KPM/SuSFS/卸载这些本项目还没有）。

## 第三批：关于页的两处修正（commit `08c4cb4`，2026-09-28，版本 0.8.0/code9）

用户口径：「关于页的返回逻辑有问题，并且关于页不是有背景色吗，也抄过来」。

| 本项目文件 | 上游 | 改了什么 |
| --- | --- | --- |
| `ui/screen/about/AboutScreen.kt` | 同名 | **补 `BackHandler(enabled = true) { onBack() }`**：上游的返回归 navigation3 的路由栈，本项目没有导航库，得自己接（不接就按返回退出 App） |
| `ui/screen/about/AboutMiuix.kt` | 同名 | `BgEffectBackground` 的两个参数由写死的 `false` 换成算出来的 `effectBackground`（口径与理由见 `about-bg-effect-shader.md`） |
| `ui/component/miuix/effect/`（`BgEffectBackground` / `BgEffectConfig` / `BgEffectPainter` / `BgEffectModifier` / `OS3BgFrag` / `DeviceType`）+ `ui/util/WindowSize.kt` | `ui/component/miuix/effect/*` + `ui/util/WindowSize.kt` | **新增**：整条 OS3 渐变背景；`kmp.blur` → `kmp.shader`（`app/build.gradle` 新增 `miuix-shader-android:0.9.4`） |
| `ScanUi.kt` | 无（本项目自写） | 主题抽成 `MiuixAppTheme`，about 分支也包上（见 `miuix-theme-scope.md`） |

要点：**「不搬毛玻璃」不等于「不搬 RuntimeShader」** —— 毛玻璃（`layerBackdrop` / `InteractiveHighlight`）
要的是 minSdk 33 的 **manifest** 门槛，所以那个坐标不能用；OS3 渐变背景只是**运行时**门控，
minSdk 24 照样编得过。
