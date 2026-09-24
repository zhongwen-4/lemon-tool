# MiuiX 0.8.8 接入要点（Compose）

SUMMARY: MiuiX 坐标 `top.yukonga.miuix.kmp:miuix-android:0.8.8`，**硬要求 compileSdk 36**
（AAR 里 `minCompileSdk=36`，低了直接构建失败）、minSdk 23；库本身用 Kotlin 2.3.20 编译，
消费端要用同版本 Kotlin 与 compose 编译器插件；组件在 `basic` / `theme` 两个包。
**莫奈取色是库内置的**（`MiuixTheme(controller = ThemeController(...))`），
**图标是另一个构件** `miuix-icons-android`，`basic.Card` 没有 `border` 参数。
READ WHEN: when 要改本 App 的界面、升级 MiuiX、改用莫奈取色、加图标，或构建时报
compileSdk / Kotlin 元数据版本不匹配时。
RECHECK WHEN: MiuiX 升到 0.9+，或 Kotlin / AGP 换大版本之后。

---

## 坐标与硬约束（实拉 AAR 核实）

- `top.yukonga.miuix.kmp:miuix-android:0.8.8`（Maven Central，AAR 约 1.6 MB）
- AAR 内 `META-INF/com/android/build/gradle/aar-metadata.properties`：
  `minCompileSdk=36`、`minAndroidGradlePluginVersion=1.0.0`、`coreLibraryDesugaringEnabled=false`
- AAR 的 `AndroidManifest.xml`：`minSdkVersion=23`
- POM 依赖：Compose Multiplatform foundation 1.10.3、kotlin-stdlib 2.3.20、
  navigationevent-compose 1.0.1、material3-window-size-class 1.9.0、shapes-android 1.2.0、
  material-color-utilities-android 4.1.1
- 必须 `android.useAndroidX=true`

## 组件位置与签名（0.8.8 核实）

- `top.yukonga.miuix.kmp.basic`：`Text`、`Button`、`Card`、`Scaffold`、`TopAppBar`、
  `SmallTopAppBar`、`SmallTitle`、`Surface`、`CircularProgressIndicator`、`LinearProgressIndicator`、
  `Divider`、`Switch`、`TextField`、`Snackbar`、`NavigationBar`…，以及 `Icon`、`CardDefaults`、`CardColors`
- `top.yukonga.miuix.kmp.theme`：`MiuixTheme { }`、`MiuixTheme.colorScheme`、`MiuixTheme.textStyles`、
  `ThemeController`、`ColorSchemeMode`
- 关键签名细节：
  - `Text(text, modifier, color, autoSize, fontSize, …, softWrap, maxLines, minLines, onTextLayout,
    style: TextStyle = LocalTextStyles.current.main)` —— **`style` 是最后一个参数**，必须命名传。
  - `Card(modifier, cornerRadius, insideMargin, colors, content: @Composable ColumnScope.() -> Unit)`，
    **没有 border 参数**
  - `Card(modifier, cornerRadius, insideMargin, colors, pressFeedbackType, showIndication,
    onClick, onLongPress, content)` —— 可点击重载，`pressFeedbackType = PressFeedbackType.Sink`
    给按压反馈，`PressFeedbackType` 在 `top.yukonga.miuix.kmp.utils`
  - `Button(onClick, modifier, enabled, cornerRadius, minWidth, minHeight, colors, insideMargin,
    interactionSource, indication, content: @Composable RowScope.() -> Unit)`
  - `Scaffold(modifier, topBar, bottomBar, floatingActionButton, floatingToolbar, snackbarHost,
    popupHost, containerColor = surface, contentWindowInsets, content: @Composable (PaddingValues) -> Unit)`
  - `SmallTopAppBar(title: **String**, modifier, color, titleColor, navigationIcon, actions,
    scrollBehavior, defaultWindowInsetsPadding, horizontalPadding)` —— **title 是 String 不是槽位**，
    要在标题栏里放图标+两行文字，只能不用它、自己画一个 Row 塞进 `Scaffold(topBar = { })`
  - `MiuixTheme(controller: ThemeController, textStyles, smoothRounding, content)` 与
    `MiuixTheme(colors, textStyles, smoothRounding, content)` 两个重载
  - `Icon(imageVector | painter | bitmap, contentDescription, modifier, tint)`；`tint = Color.Unspecified`
    表示不染色（画多色图标时必须传）；内部 `.defaultSizeFor(painter)` 只在 modifier 没定尺寸时生效，
    所以 `Modifier.size(40.dp)` 能覆盖默认 24.dp
  - `textStyles` 字段：main / paragraph / body1 / body2 / button / footnote1 / footnote2 /
    headline1 / headline2 / subtitle / title1 … title4
  - 常用色：primary / onPrimary / error / errorContainer / primaryContainer / secondaryContainer /
    surface / surfaceContainer / surfaceContainerHigh / surfaceContainerHighest / onSurface /
    onSurfaceContainer / onSurfaceContainerVariant / onSurfaceVariantSummary / onBackgroundVariant /
    outline / dividerLine

## 莫奈取色（库内置，别自己接 material-color-utilities）

```
val controller = remember { ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem) }
MiuixTheme(controller = controller) { ... }
```

- `ColorSchemeMode` 六个值：`System / Light / Dark / MonetSystem / MonetLight / MonetDark`
- `ThemeController` 还能传 `keyColor`（给了就**不再读系统壁纸色**，改成自己按种子色生成）、
  `colorSpec`（Spec2021/Spec2025）、`paletteStyle`（TonalSpot 等 9 种）、`isDark`
- `MonetSystem` 在 Android 上的实现（`DynamicColors.android.kt`）：
  API ≥ 33 读 `Settings.Secure` 的 `theme_customization_overlay_packages`（系统壁纸取色，
  含系统用的 paletteStyle）；API 31-32 走 `android.R.color.system_accent1_*` 角色；
  **API < 31 回落到 `monetSystemColors()`——即 MiuiX 自己的紫色种子**。minSdk 24 的 App
  在 Android 11 及以下看到的是紫，不是壁纸色，这是预期回落不是 bug
- `com.materialkolor:material-color-utilities` 由 MiuiX 传递引入，**不需要**自己加依赖

### 静态主题下 surfaceContainer 就是纯白（重要坑）

`lightColorScheme()` 默认值：`background = Color.White`、`surface = #F7F7F7`、
**`surfaceContainer = Color.White`**。而 `CardDefaults.defaultColors()` 的默认底色正是
`surfaceContainer` —— 所以在**不用 Monet** 的默认主题下，卡片是白底压白底，**完全看不出边界**。
用 Monet 后 `mapMd3RolesToMiuixColorsCommon()` 把 `surfaceContainer` 映射到 MD3 的
surfaceContainer（浅色约 tone 94，明显比 `surface` 的 tone 98 深），卡片才浮出来。
要更明确的“面板”感用 `surfaceContainerHigh`。

## 图标是独立构件（不在 miuix-android 里）

- 坐标：`top.yukonga.miuix.kmp:miuix-icons-android:0.8.8`（Maven Central，AAR 约 1.19 MB，
  `minSdkVersion=23`，**AAR 内没有 proguard 规则文件**，所以 R8 能自由裁掉没引用的图标）
- `miuix-android` 里只有空壳 `top.yukonga.miuix.kmp.icon.MiuixIcons`（四个嵌套 object：
  `Basic / Light / Regular / Heavy`），图标本体在 icons 构件的
  `top.yukonga.miuix.kmp.icon.extended` 包，做成**嵌套 object 的扩展属性**
- 用法：`import top.yukonga.miuix.kmp.icon.extended.Layers` + `import top.yukonga.miuix.kmp.icon.MiuixIcons`，
  然后写 `MiuixIcons.Layers`（等价于 `MiuixIcons.Regular.Layers`，还有 `.Light.` / `.Heavy.` 变体）。
  扩展属性**必须按名字 import**，否则 `MiuixIcons.Layers` 解析不到
- 0.8.8 共 155 个图标，命名是 MiuiX 自己的一套（`File`、`Folder`、`Layers`、`Scan`、`Search`、
  `Report`、`Info`、`Settings`、`ChevronForward`、`ExpandMore`/`ExpandLess`、`Th1`~`Th31` 等），
  **和 Material Icons 名字对不上**，别凭印象写，去
  `https://compose-miuix-ui.github.io/miuix/zh_CN/guide/icons` 查
- 体积实测：只引用 3 个图标时，**未引用的图标确实被 R8 裁掉了**（在 `classes.dex` 里搜
  `ZoomOut`/`WorldClock`/`MapAlbum` 等字符串为 0 命中，搜用到的 3 个为 1 命中）。整包 dex 从
  1488 KB 涨到 1751 KB 主要是新界面代码，不是图标

- **0.8.8 里没有 Home / House（房子）图标**：155 个里有 `Tasks`、`ListView`、`Scan`、`SearchDevice`、
  `Report`、`Info`、`Settings`、`Help`、`GridView`…，但**没有房子**——想做「主页」tab 只能从这些里挑，
  本 App 底栏用的是 `Scan`（应用本身就是扫描器，比原先占位的 `Tasks` 贴题）
- 要拿全量图标名别去翻文档页：拉 `miuix-icons-android-<版本>-sources.jar`，
  `commonMain/top/yukonga/miuix/kmp/icon/extended/*.kt` 的**文件名就是图标名**（0.8.8 = 155 个，
  一个文件一个图标）
- **图标源码结构**（想自己生成预览、或解析图标数据时用）：每个文件里有 **Light / Regular / Heavy 三档变体**，
  各是一段 `val MiuixIcons.<变体>.<名字>: ImageVector get() { ImageVector.Builder(...) }`；
  `MiuixIcons.<名字>` 只是 `MiuixIcons.Regular.<名字>` 的别名（**要预览就取 Regular**）。每段自带
  `viewportWidth/Height`（1000~1450 不等，**不是 24**）+ 一个
  `group(scaleX, scaleY, translationX, translationY)`（实测全是 `1 / -1 / tx / ty`，即
  `x' = x + tx, y' = -y + ty`——**不套这个变换就会画得上下颠倒**）+ 一个
  `addPath(pathData = listOf(PathNode…))`
- 用到的 `PathNode` 只有 `MoveTo / LineTo / HorizontalTo / VerticalTo / QuadTo / Close`（**没有弧线和三次
  贝塞尔**，全是绝对坐标，没有 `Relative*` 变体），且 155 个的 `pathFillType` **全是 `NonZero`**——
  所以转 SVG（`M/L/H/V/Q/Z` + `matrix(...)`）或 GDI+（`FillMode.Winding`）都很省事，不会踩填充规则的坑
- 2026-09-24 照这套做过一次全量预览：155 个图标的 HTML（可搜索、标了候选）与 PNG 联络表在
  `build/miuix-icons-preview.html` / `build/miuix-icons-preview.png`。`build/` 被 `.gitignore` 锚定忽略，
  属于本机可再生产物，不进仓库

## Card 没有 border —— 要框就自己画

`Card` 只接受 `colors: CardColors`（只有 `color` / `contentColor` 两个字段），没有描边参数。
要彩色边框用：

```
Modifier.border(宽度, 颜色, RoundedCornerShape(CardDefaults.CornerRadius))
```

- 形状**必须用 `RoundedCornerShape(CardDefaults.CornerRadius)`**，`BasicCard` 内部就是
  `.clip(RoundedCornerShape(cornerRadius))`，形状不一致会出现缝或角被切
- `Modifier.border` 画在后续 `background` 之上，且不被内部 `clip` 切掉，所以描边可见
- 透明框就传 `Color.Transparent`（要的只是“不给这级加颜色”，不是不画）

## 怎么核实 API —— 别凭记忆写

直接从 Maven Central 拉 sources jar 对照签名，比猜可靠得多：

```
https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-android/<版本>/miuix-android-<版本>-sources.jar
https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-android/<版本>/miuix-android-<版本>.aar
https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-icons-android/<版本>/miuix-icons-android-<版本>-sources.jar
```

sources jar 里 `commonMain/…/*.kt` 是真实签名，`androidMain/…` 是实际实现（Monet 的回落逻辑就在这里）；
AAR 里 `classes.jar` 可列包结构、`aar-metadata.properties` 给出 minCompileSdk。
**icons AAR 的 AndroidManifest.xml 是纯文本 XML（不是二进制）**，7z 解出来后直接读字节就能看 minSdk。

## 会顺带把 Compose 版本拉高（0.8.8 实查 .module 文件）

MiuiX 是 Compose Multiplatform 构件，android 变体暴露的是 `org.jetbrains.compose.*`：

- `androidApiElements-published`：`org.jetbrains.compose.foundation:foundation:1.10.3`、kotlin-stdlib 2.3.20
- 而 `org.jetbrains.compose.foundation:foundation:1.10.3` 的 android 变体又依赖
  `androidx.compose.foundation:foundation:**1.10.5**`

所以 app 里显式写的 `androidx.compose.ui:ui:1.9.5` 之类**会被依赖解析升到 1.10.x**，实际生效版本
以解析结果为准，不要以为声明写了 1.9.5 就真是 1.9.5。`androidx.compose.ui:ui:1.10.5` 的 AAR 元数据
是 `minCompileSdk=35`、`minAndroidGradlePluginVersion=8.6.0`，compileSdk 36 + AGP 8.11.1 都满足。

## NavigationBar / NavigationBarItem 签名（0.8.8，从 sources jar 核实）

- `NavigationBar(modifier, color = colorScheme.surface, showDivider = true,
  defaultWindowInsetsPadding = true, mode: NavigationBarDisplayMode = IconAndText,
  content: @Composable RowScope.() -> Unit)` —— 支持 2~5 个 item；自带分割线，以及
  navigationBars / captionBar 的内边距（不用自己垫）
- `NavigationBarItem` 的 `icon: ImageVector` 与 `label: String` **都是必填**；`icon` 即使
  `mode = TextOnly` 也躲不掉——它不渲染，但必须给一个矢量
- `NavigationBarDisplayMode` 四个值：`IconAndText` / `IconOnly` / `TextOnly` /
  **`IconWithSelectedLabel`**（图标常显，文字只在选中时出现）
- `NavigationBarDefaults`：`ItemHeight 64dp`、`IconSize 26dp`、`LabelFontSize 12sp`、
  `IconTopPadding 8dp`、`BottomPadding 8dp`、未选中 `UnselectedAlpha 0.4f`
- 另有浮岛样式 `FloatingNavigationBar` + `FloatingNavigationBarDefaults`，数据类
  `NavigationItem(label, icon)`
### 浮岛底栏 `FloatingNavigationBar`（0.8.8 就有，2026-09-25 核实）

```kotlin
@Composable fun FloatingNavigationBar(
    modifier: Modifier = Modifier,
    color: Color = MiuixTheme.colorScheme.surfaceContainer,
    cornerRadius: Dp = FloatingToolbarDefaults.CornerRadius,
    horizontalAlignment: Alignment.Horizontal = CenterHorizontally,
    horizontalOutSidePadding: Dp = FloatingNavigationBarDefaults.HorizontalOutSidePadding, // 36.dp
    shadowElevation: Dp = FloatingNavigationBarDefaults.ShadowElevation,                  // 1.dp
    showDivider: Boolean = false,
    defaultWindowInsetsPadding: Boolean = true,
    mode: FloatingNavigationBarDisplayMode = FloatingNavigationBarDisplayMode.IconOnly,
    content: @Composable () -> Unit,
)
@Composable fun FloatingNavigationBarItem(
    selected: Boolean, onClick: () -> Unit, icon: ImageVector, label: String,
    modifier: Modifier = Modifier, enabled: Boolean = true,
)
```

- **不用毛玻璃**：它就是「圆角 + 阴影 + 居中」的一颗浮岛，`miuix.kmp.blur` 那套（Backdrop /
  LayerBackdrop）**0.8.8 里根本没有**，0.9.x 才有。想要悬浮底栏不必升版本。
- `FloatingNavigationBarDisplayMode` 只有三个值：`IconAndText` / `IconOnly` / `TextOnly`
  （**默认是 `IconOnly`**，要图标加文字必须显式传 `IconAndText`）。
- 内部：`Column { Row(padding(bottom = …), spacedBy(ItemSpacing=12.dp), 居中) { content() } }`；
  自己处理 `navigationBars` 内边距与底部间距（无手势条时留 36.dp），所以直接丢进
  `Scaffold(bottomBar = {})` 即可，不必自己垫 insets。
- 视觉：**不画选中指示器**，选中＝`onSurfaceContainer` 实色 + 加粗，未选中＝同色 alpha 0.4；
  两种模式各自的默认值在 `FloatingNavigationBarDefaults`（IconSize 24.dp / LabelFontSize 12.sp /
  IconOnlySize 28.dp 等）。
- 与普通 `NavigationBar` 的差别：普通版自带分割线、占满整宽、默认 `IconAndText`；
  浮岛版不占满宽、默认 `IconOnly`、无分割线。
## 升到 0.9.x 的代价（2026-09-25 实测，当天决定不升）

- **0.9.x 换了坐标**：0.8.x 的单体 `top.yukonga.miuix.kmp:miuix-android` 在 Maven Central **停在 0.8.8**；
  0.9.x 拆成 `miuix-ui-android` / `miuix-nav-android` / `miuix-preference-android` / `miuix-blur-android` /
  `miuix-icons-android`（另有 `miuix-core` / `miuix-shader` / `miuix-squircle` 等内部件），当前最新 0.9.4。
  包名没变（还是 `top.yukonga.miuix.kmp.basic.*` / `.theme.*`），所以迁移主要是改依赖坐标。
- **0.9.4 要求 AGP ≥ 9.1.0**：把它装进本项目（AGP 8.11.1）后 `:app:checkDebugAarMetadata` 直接失败——
  `androidx.compose.animation:animation-core-android:1.12.0 requires Android Gradle plugin 9.1.0 or higher`
  （0.9.4 拉的是 **Compose 1.12.0**）。要升就得连带升 AGP 9 + Gradle 9 + Compose 1.12，CI 也得从
  `gradle-version: '8.13'` + JDK 17 换掉。SukiSU 自己是 AGP 9.4.1 / Kotlin 2.4.20，就是这么来的。
- **想要的东西 0.9.x 才有**：整个 `top.yukonga.miuix.kmp.preference` 包（`ArrowPreference`、
  `SwitchPreference`、`OverlayDropdownPreference`……「一行一项的设置列表」全靠它），以及
  `MiuixScrollBehavior`、`overScrollVertical`、`isDynamicColor`、`top.yukonga.miuix.kmp.blur.*`。
  0.8.8 里这些**一个都没有**（对 sources jar 检索 `preference/` 与
  `blur|Backdrop|Liquid|Glass` 均零命中；0.8.8 有的近亲是 `TabRow` / `SuperSwitch` / `NumberPicker` /
  `Slider` / `TextField` / `SearchBar` / `TopAppBar`）。
- 结论：要「照抄 SukiSU 的设置页 / 列表 UI」就得走这条升级；不升就只能自己手写等价的行组件
  （本项目已落地 `SettingRow`：图标 + 标题/副标题 + 尾部文字 + 可选点击）。
