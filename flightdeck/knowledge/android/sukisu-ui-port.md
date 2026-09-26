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
READ WHEN: before 继续移植 SukiSU（或 KernelSU 系）的界面代码、或要给本项目加新界面时。
RECHECK WHEN: 上游大改目录结构，或本项目决定启用毛玻璃之后。

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
