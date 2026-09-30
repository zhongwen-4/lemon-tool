# MiuiX 0.9.4 接入要点与签名（本项目当前用的版本）

SUMMARY: 0.9.x **拆了坐标**：`miuix-ui-android` / `miuix-nav-android` / `miuix-preference-android` /
`miuix-blur-android` / `miuix-icons-android`（包名仍是 `top.yukonga.miuix.kmp.*`，所以迁移主要是改依赖行）。
本项目 2026-09-27 已升到 **0.9.4**，工具链是 AGP 9.4.1 / Kotlin 2.4.20 / Gradle 9.7.1 / JDK 21 /
compose-bom 2026.09.00 —— **与 SukiSU Ultra 完全同一套**，所以它的界面代码可以直接搬。
**硬门槛（2026-09-27 实测）**：0.9.4 全家桶要求 **`compileSdk ≥ 37`**（差一档就在
`checkReleaseAarMetadata` 直接失败，20 条 issue）；`miuix-blur-android:0.9.4` 的 manifest 硬要求
**`minSdk 33`**，与本项目 `minSdk 24` 冲突 —— **2026-09-29 已重新引入**，靠 app manifest 里的
`<uses-sdk tools:overrideLibrary="top.yukonga.miuix.kmp.blur"/>` 放行（上游也这么绕；细节见
`miuix-blur-and-liquid-glass.md`）。
它的兄弟 **`miuix-shader-android:0.9.4`** 门槛不同：**minSdk 24**，2026-09-28 起已启用
（给关于页的 OS3 渐变背景用，见 `about-bg-effect-shader.md`）。
详见 `knowledge/build/compile-sdk-and-aar-metadata.md`。
0.9.x 才有的东西：整个 `preference` 包（一行一项的设置列表全靠它）、`blur.*`（Backdrop 毛玻璃）、
`MiuixScrollBehavior`、`overScrollVertical`、`isDynamicColor`。**0.8.8 里这些一个都没有**。
坑：`WindowInsets.only` / `WindowInsets.add` 是**顶层扩展函数**，必须显式 import
（漏了 `add` 报 `Unresolved reference 'add' on receiver of type 'WindowInsets'`）。
READ WHEN: before 改本 App 的界面、用 MiuiX 组件、或移植别人的 MiuiX 界面时。
RECHECK WHEN: MiuiX 再升版本，或 compose-bom / AGP 换大版本之后。

---

## 现状（2026-09-27 核实）

- `app/build.gradle` 依赖：`miuix-ui-android` / `miuix-icons-android` / `miuix-nav-android` /
  `miuix-preference-android`，全是 **0.9.4**；compose-bom 2026.09.00；
  `material-icons-extended`（图标仍用它，Apache-2.0）。
  **`miuix-blur-android:0.9.4`**（2026-09-29 重新加回：磨砂玻璃与液态玻璃都靠它，用 `tools:overrideLibrary`
  绕开它的 minSdk 33，见 `miuix-blur-and-liquid-glass.md`）。2026-09-28 起新增
  **`miuix-shader-android:0.9.4`**（minSdk 24，包名 `top.yukonga.miuix.kmp.shader`，给关于页的
  OS3 渐变背景；**`miuix-ui` 不会带它进来，必须显式加依赖行**）。
- 本机 `./gradlew.bat :app:compileDebugKotlin --offline` 与 `compileReleaseKotlin` 都能过
  （0.9.4 的依赖已在 Gradle 缓存里）。**但这两条过不算验证过出包**：AAR 元数据与清单合并要另外跑，
  见 `knowledge/build/android-toolchain.md` 的「本机验证能走到哪一步」。打 APK 仍只靠 CI（本机没 NDK）。
- 0.8.8 时期的记录留在 `miuix-0.8.8.md`，只作历史参考。

## preference 包（0.9.4 新增，设置页/列表页就靠它）

一共 9 个：`ArrowPreference` / `CheckboxPreference` / `SwitchPreference` / `RadioButtonPreference` /
`SliderPreference` / `OverlayDropdownPreference` / `OverlaySpinnerPreference` /
`WindowDropdownPreference` / `WindowSpinnerPreference`。

```kotlin
ArrowPreference(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: BasicComponentColors = BasicComponentDefaults.titleColor(),
    summary: String? = null,
    summaryColor: BasicComponentColors = BasicComponentDefaults.summaryColor(),
    startAction: @Composable (() -> Unit)? = null,   // 开头图标放这里
    endActions: @Composable RowScope.() -> Unit = {}, // 尾部文字放这里，箭头由它自己补
    bottomAction: (@Composable () -> Unit)? = null,
    insideMargin: PaddingValues = BasicComponentDefaults.InsideMargin,
    onClick: (() -> Unit)? = null,
    holdDownState: Boolean = false,
    enabled: Boolean = true,
)
// SwitchPreference(checked, onCheckedChange, title, …) 其余同上，尾部队列里换成 Switch
```

- `BasicComponentDefaults.InsideMargin = PaddingValues(16.dp)`；
  `titleColor()` 默认 `onBackground`、`summaryColor()` 默认 `onSurfaceVariantSummary`
  （即**默认就是「卡片外的底色」**，上游把它们直接塞进 Card 里用，本项目照办）。
- 行组件的底座是 `basic.BasicComponent`，它有两个重载：一个 `title/summary/summaryColor/…` 的便捷版，
  一个是纯 `content: @Composable ColumnScope.() -> Unit` 的通用版。想要「没有箭头的只读行」就用它。

## 常用签名（实拉 0.9.4 sources jar 核对，别猜）

- `Card(modifier, cornerRadius = 16.dp, insideMargin = PaddingValues(0.dp), colors = CardDefaults.defaultColors(), content)`
  与可点重载 `Card(…, pressFeedbackType = PressFeedbackType.None, showIndication = false, holdDownState, onClick, onLongPress, content)`
  —— **没有 `border` 参数**，描边得自己在外面 `Modifier.border`。
- `CardDefaults.defaultColors(color = surfaceContainer, contentColor = onSurfaceContainer)`；`CornerRadius = 16.dp`、`InsideMargin = 0.dp`。
- `TopAppBar(title: String, modifier, color, titleColor, largeTitle, …, navigationIcon, actions, scrollBehavior: ScrollBehavior? = null, defaultWindowInsetsPadding = true, …)`
  —— `title` 是 **String 不是槽位**，要塞图标+两行只能自己画；`actions` 是
  `@Composable RowScope.() -> Unit`（顶栏右侧那一组，SmallTopAppBar 同款），右侧那块留白由
  `TopAppBarDefaults.ActionIconPadding = 16.dp` 管（上游 SukiSU 的清空按钮还在里面额外加了
  `Modifier.padding(end = 8.dp)`）。`bottomContent` 放在标题栏下方。
- `MiuixScrollBehavior(state, canScroll, snapAnimationSpec, flingAnimationSpec): ScrollBehavior`；
  配合 `LazyColumn` 的 `.nestedScroll(scrollBehavior.nestedScrollConnection)` + `.overScrollVertical()` + `.scrollEndHaptic()`。
- `Modifier.overScrollVertical(nestedScrollToParent = true, isEnabled = { true })`；
  `Modifier.scrollEndHaptic(hapticFeedbackType = HapticFeedbackType.TextHandleMove)`。
- `MiuixTheme.isDynamicColor`（布尔）、`MiuixTheme.colorScheme` 里新增 `surfaceContainer` 系列、
  `onSurfaceContainer`、`onSurfaceVariantSummary`、`dividerLine`、`errorContainer`、`secondaryContainer`。
- `MiuixTheme.textStyles` 仍是 main/paragraph/body1/body2/button/footnote1/footnote2/headline1/headline2/subtitle/title1…4。
- 主题取色：`ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem)`，另有 `ThemeColorSpec` / `ThemePaletteStyle`。
## 图标：0.9.4 只有 extended 一个包（2026-09-28 实测）

`miuix-icons-android:0.9.4` 的 classes.jar 里**只有** `top/yukonga/miuix/kmp/icon/extended/`（157 个 Kt 类），
**没有 `icon/basic` 包**。从上游（用的不是这一版）抄界面时，遇到 `MiuixIcons.Basic.ArrowRight` 这种就必须换：

- 0.9.4 的写法是 **`MiuixIcons.Back`**（import `top.yukonga.miuix.kmp.icon.extended.Back`）——
  `MiuixIcons.<名字>` 直接就是扩展属性，不经过 `.Extended`。
- **没有 `ArrowRight`**；可用的近义是 `ChevronForward` / `ChevronBackward` / `Forward`。
  （本项目 SU 日志条目卡尾部箭头因此改成 `MiuixIcons.ChevronForward`。）
- 核对办法：解 aar 里的 `classes.jar`，列 `icon/` 下的条目名（别猜）。

## 另外两条 0.9.4 实测

- `Card` 有两个重载：静态的 `(modifier, cornerRadius, insideMargin, colors, content)` 与可点的
  `(modifier, cornerRadius, insideMargin, colors, pressFeedbackType, showIndication, holdDownState,
  onClick, onLongPress, content)`——**可点那个同样有 `insideMargin`**，全用命名参数传即可。
- `top.yukonga.miuix.kmp.overlay.OverlayDialog` 在 0.9.4 里存在（同层还有 `OverlayListPopup` /
  `OverlayBottomSheet` / `OverlayCascadingListPopup`）。

## 查 MiuiX 组件签名的三条路（2026-09-28 实测，按优先级）

1. **上游 GitHub 仓库 `yukonga/miuix`**（最准最快，这次就是这么查到进度指示器的）：
   `api.github.com/repos/yukonga/miuix/git/trees/main?recursive=1` 列全树找 `.kt`，
   再用 `contents/<路径>?ref=main` + `Accept: application/vnd.github.raw` 取原文。
   源码在 `miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/...`（`miuix-preference` 等同理）。
2. **`javap` 解本地 aar 的 classes.jar**（只想确认某个组件/重载存不存在、看 JVM 签名）：
   缓存路径是 `C:\Users\admin\.gradle\caches\modules-2\files-2.1\top.yukonga.miuix.kmp\<构件>\0.9.4\<hash>\*.aar`，
   用 `[System.IO.Compression.ZipFile]` 取里面的 `classes.jar`，再
   `& "$JDK\bin\javap.exe" -classpath classes.jar top.yukonga.miuix.kmp.basic.ProgressIndicatorKt`。
   注意 javap 打出来的是**混淆后的 JVM 签名**（`LinearProgressIndicator--jt2gSs`，`Dp` 变 `float`、
   `Color` 变 `long`），只能看形状与参数个数，**参数名要靠第 1 / 3 条**。
3. **拉 `-sources.jar`（2026-10-01 推翻上一条旧结论：能用）**：`repo1.maven.org` 的**目录索引**
   仍然常超时，但**具体文件按全名直下是通的** ——
   `https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-ui-android/0.9.4/miuix-ui-android-0.9.4-sources.jar`
   （238 KB，本机 1 秒下完），解开就是 `commonMain/top/yukonga/miuix/kmp/basic/*.kt`，**带完整参数名与 KDoc**，
   核对签名最省事的就是它（第 1 条的 GitHub 上传源同理，只是要多一次列树请求）。

**这次的结果（进度指示器，0.9.4 实有）**：

```kotlin
CircularProgressIndicator(modifier, progress: Float? = null, colors, strokeWidth: Dp, size: Dp)
LinearProgressIndicator(modifier, progress: Float? = null, colors, height: Dp)
InfiniteProgressIndicator(modifier, color: Color = Color.Gray, size: Dp, strokeWidth: Dp, orbitingDotSize: Dp)
```

**`progress = null` 就是「不确定进度」的转圈动画**（内部走 `rememberInfiniteTransition`），
所以「扫描中」这种没有百分比的等待态直接传 `null` 即可，不用自己写 `restartable` 动画。
