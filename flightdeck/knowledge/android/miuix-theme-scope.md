# ⚠ 本项目的 MiuixTheme 只套在主壳上：越过 ScannerShell 的页面会落到默认浅色

SUMMARY: `MiuixTheme(...)` 原先只写在 `ScanUi.kt` 的 `ScannerShell` 里，而 `MiuixTheme.colorScheme`
的 CompositionLocal 默认值是 **`lightColorScheme()`**（miuix 源码：`internal val LocalColors =
staticCompositionLocalOf { lightColorScheme() }`）——所以从 `ScannerScreen` 直接切出去的页面
（关于页：`if (aboutOpen) AboutScreen(...) else ScannerShell(...)`）**根本不在主题里**：深色模式下
底色发白、`surface` 也不是 SukiSU 那套 TonalSpot。2026-09-28 把主题抽成 `MiuixAppTheme` 两个分支共用
（上游是整包套主题，所以它没这个问题）。**以后再加「不经过 ScannerShell 的页面 / 全屏弹层」，一律包进 `MiuixAppTheme`。**
READ WHEN: before 给本项目加「不经过 ScannerShell 的页面或全屏弹层」，或发现某页/某组件的颜色跟别的页对不上、
深色下发白时。
RECHECK WHEN: 改成在 `MainActivity` 的 `setContent` 里整包套主题，或引入真正的导航库之后。

---

## 症状与根因

- 症状：关于页在深色模式下是浅色底，其它页正常。
- 根因：`AboutScreenMiuix` 用 `MiuixTheme.colorScheme.*` 取色，但渲染时没有 provider，落到默认值。
- 修法（`ScanUi.kt`）：把那段 `MiuixTheme(controller = remember { ThemeController(…非莫奈…) })`
  抽成 `@Composable private fun MiuixAppTheme(content: @Composable () -> Unit)`；
  `ScannerScreen` 的 about 分支包一层，`ScannerShell` 里那句换成 `MiuixAppTheme { … }`。
  两个分支各在自己这一层，**不会嵌套**（嵌套也不会崩，只是白套一层）。

## 顺带两条

- 关于页的 OS3 动态背景从主题里取 `MiuixTheme.colorScheme.surface` 与 `isInDarkTheme()`，
  所以「页面不在主题里」会同时让背景色与渐变配色选错（见 `about-bg-effect-shader.md`）。
- 窗口底色（平台主题 `Theme.Mrs` 的 `@color/window_surface`）与 Miuix 的 `surface` 是两个来源，
  别拿一个去解释另一个（见 `no-theme-means-black-system-bars.md`）。
