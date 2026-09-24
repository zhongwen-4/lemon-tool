# 参考 SukiSU Ultra 的界面之前要知道的事（许可 / 规模 / 它底栏到底怎么写）

SUMMARY: 要「抄 SukiSU Ultra 的 UI」之前先看这三条：①它的**代码是 GPL-3.0**，而本仓库**没有 LICENSE**
（默认保留全部权利），照搬代码等于把整个项目（含 `core/` C++ 核心）拖成 GPL-3.0 并须公开源码；
它的**启动图标另有一份《SukiSU Ultra 图标有限使用许可证》**（严格非商业、禁止提取、禁止用作别的 app
的图标/素材，衍生项目必须删除或替换）。②规模：它的 `manager/.../ui/` 就有 **281 个 .kt、约 2.0 MB**，
每个界面写两遍（`XxxMaterial.kt` + `XxxMiuix.kt`，由 `UiMode.kt` 切），另加自研 Expressive 组件、
liquid 玻璃、markdown 渲染、WebUI、模板编辑器。③它的底栏用的是 **`material-icons-extended` 的
`Icons.Rounded.Cottage / Security / Extension / Settings`**（用户想要的「房子」＝`Icons.Rounded.Cottage`，
Apache-2.0 可放心用），切换是横向 pager，可选浮动毛玻璃底栏——**而 MiuiX 0.8.8 没有 blur API，做不了**。
结论：**抄观感、不抄代码**。

READ WHEN: before 要照搬或参考别的 App（SukiSU / KernelSU 这类 root 管理器）的界面、底栏、主题时。

RECHECK WHEN: SukiSU 换许可证、或本仓库开始声明自己的 LICENSE 之后。

---

## 事实（2026-09-25 核实，走 api.github.com）

- 仓库 `SukiSU-Ultra/SukiSU-Ultra`：Kotlin，默认分支 `main`，**license = GPL-3.0**。
  对照 `tiann/KernelSU`、`KernelSU-Next/KernelSU-Next` 也都是 **GPL-3.0**（三者同源）。
- `LICENSE_icon_SC`（还有英文版）＝《SukiSU Ultra 图标有限使用许可证 v1.0》，只管带动漫角色的那几个
  启动图标（文件名匹配 `ic_launcher(?!.*alt.*).*`）。要点：**严格非商业**；不得从项目里提取后单独分发、
  不得用作其他 app 的图标 / 图标包 / 主题 / Logo / 素材；**fork、镜像、衍生项目必须删除或替换**
  （保留署名不构成授权）；「适用于源码的许可证不会自动适用于图标」。
- 工具链（`manager/gradle/libs.versions.toml`）：AGP **9.4.1**、Kotlin **2.4.20**、
  compose-bom 2026.09.00、material3 **1.5.0-alpha28**、**miuix 0.9.4**、materialKolor 5.0.1、
  libsu 6.0.0、`lsplugin-apksign` 插件。比本项目（MiuiX 0.8.8）整整新一个代际。

## 它的底栏怎么写的（`ui/component/bottombar/BottomBarMiuix.kt`）

- 四个目的地：`Home` / `SuperUser` / `Module` / `Setting`；图标来自 **`material-icons-extended`**
  （`Icons.Rounded.*`），**既不是 Tabler 也不是 Miuix 图标**。
- 两种形态：普通 MiuiX `NavigationBar`，或可选**浮动底栏** `FloatingBottomBar` + `BlurredBar`
  （`top.yukonga.miuix.kmp.blur.Backdrop` + `LayerBackdrop`）；带 `Badge` / `BadgedBox` 数字角标。
- 切页是**横向 pager**（`LocalMainPagerState`，可以左右滑），不是单纯换 tab。
- ⚠ **MiuiX 0.8.8 里没有 blur / Backdrop**：拉 `miuix-android-0.8.8-sources.jar` 检索
  `blur|Backdrop|Liquid|Glass` **零命中**（同版本里 `FloatingToolbar`、`Badge`、`BadgedBox` 是有的）。
  想要那种悬浮模糊底栏必须上 0.9.x，而 0.9.4 的工具链要求与本项目现有锁版本差一截。

## 图标风格冲突（挑图标前必须知道）

- MiuiX 自带图标是**实心** `ImageVector`：例 `miuix-icon/extended/Scan.kt` 里 `stroke` 出现 **0** 次、
  `fill` **6** 次，viewport 1042.8，外裹一层 `scaleY = -1` 的翻转 `group`；
  文件头 `SPDX-License-Identifier: Apache-2.0`（compose-miuix-ui contributors）。
- Tabler / Lucide / Feather 这一系是**描边**：`fill="none" stroke="currentColor" stroke-width="2"`
  + round cap/join，viewBox 24。
- 所以同一根底栏里混「实心 MiuiX 图标 + 描边 Tabler 图标」会明显不一致；要混就得**整栏换成同一套**。