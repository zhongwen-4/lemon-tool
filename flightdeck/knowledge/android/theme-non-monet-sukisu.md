# 本项目的界面取色：走 SukiSU 的非莫奈那一支（2026-09-28 定）

SUMMARY: 用户明确「不要莫奈取色了，用 sukisu 的配色」。SukiSU 的配色在它的
`ui/theme/MiuixTheme.kt` 的 `MiuixKernelSUTheme` 里：**非莫奈 = `ColorSchemeMode.System` +
`keyColor = null` + `ThemeColorSpec.Spec2025` + `ThemePaletteStyle.TonalSpot`**（它的默认设置值）；
莫奈那一支才会传 `ColorSchemeMode.MonetXxx`、并用 `dynamicLight/DarkColorScheme(context).primary` 当 keyColor。
本项目现在就是前者，顺带得到一个好用的副作用：`MiuixTheme.isDynamicColor` 变成 false ——
上游那些 `if (isDynamicColor) 取 primary else 硬编码绿/红` 的分支因此全部走硬编码那一路，
这正是「SukiSU 非莫奈」的观感来源。
READ WHEN: before 动主题/取色（`MiuixTheme(controller = ThemeController(...))`）、
或发现界面颜色跟 SukiSU 截图的非莫奈模式对不上时。
RECHECK WHEN: MiuiX 再升版本（`ThemeColorSpec` / `ThemePaletteStyle` 枚举变了），
或用户改口要回莫奈之后。

---

## 怎么识别「莫奈」与「非莫奈」（0.9.4）

`ThemeController(colorSchemeMode, keyColor, isDark, paletteStyle, colorSpec)`：

- **莫奈**：`MonetSystem` / `MonetLight` / `MonetDark` + `keyColor = 系统 dynamic color 的 primary`
  → 跟着壁纸变色，`isDynamicColor == true`。
- **非莫奈（本项目）**：`System` / `Light` / `Dark` + **不传 keyColor（null）**
  → 固定调色板，`isDynamicColor == false`。

`isDynamicColor` 是 MiuiX 主题里的布尔，界面代码用它区分两支 —— 见
`ui/component/miuix/WarningCard.kt` 与 `ui/screen/home/HomeMiuix.kt` 里残留的 `if (isDynamicColor)`。

## 本项目的落地位置

`ScanUi.kt` 的 `ScannerShell`：

```kotlin
MiuixTheme(controller = remember {
    ThemeController(
        colorSchemeMode = ColorSchemeMode.System,
        colorSpec = ThemeColorSpec.Spec2025,
        paletteStyle = ThemePaletteStyle.TonalSpot,
    )
})
```

**不传 keyColor**，就是非莫奈。平台主题（`Theme.Mrs`，管系统栏与开屏）与这里互不相干，
见 `no-theme-means-black-system-bars.md`。

## 顺带的教训

- 「换成某人的配色」= 去找他主题入口函数**默认走哪一支**，别自己挑一个 `ThemePaletteStyle`。
  上游默认的 paletteStyle / colorSpec 来自它 SettingsRepository 的默认值（TonalSpot / SPEC_2025）。
- 只删界面里 `isDynamicColor` 的分支没用：那两支共用**同一个 controller**，
  真正的开关在 `ThemeController` 的参数上。
