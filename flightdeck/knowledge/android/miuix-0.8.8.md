# MiuiX 0.8.8 接入要点（Compose）

SUMMARY: MiuiX 坐标 `top.yukonga.miuix.kmp:miuix-android:0.8.8`，**硬要求 compileSdk 36**
（AAR 里 `minCompileSdk=36`，低了直接构建失败）、minSdk 23；库本身用 Kotlin 2.3.20 编译，
消费端要用同版本 Kotlin 与 compose 编译器插件；组件在 `basic` / `theme` 两个包。
READ WHEN: when 要改本 App 的界面、升级 MiuiX，或构建时报 compileSdk / Kotlin 元数据版本不匹配时。
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
  `Divider`、`Switch`、`TextField`、`Snackbar`、`NavigationBar`…
- `top.yukonga.miuix.kmp.theme`：`MiuixTheme { }`、`MiuixTheme.colorScheme`、`MiuixTheme.textStyles`
- 关键签名细节：
  - `Text(text, modifier, color, autoSize, fontSize, …, softWrap, maxLines, minLines, onTextLayout,
    style: TextStyle = LocalTextStyles.current.main)` —— **`style` 是最后一个参数**，必须命名传。
  - `Card(modifier, cornerRadius, insideMargin, colors, content: @Composable ColumnScope.() -> Unit)`
  - `Button(onClick, modifier, enabled, cornerRadius, minWidth, minHeight, colors, insideMargin,
    interactionSource, indication, content: @Composable RowScope.() -> Unit)`
  - `Scaffold(modifier, topBar, bottomBar, floatingActionButton, …, content: @Composable (PaddingValues) -> Unit)`
  - `SmallTopAppBar(title: String, modifier, color, titleColor, navigationIcon, actions, scrollBehavior, …)`
  - `MiuixTheme(colors, textStyles, smoothRounding, content)`
  - `textStyles` 字段：main / paragraph / body1 / body2 / button / footnote1 / footnote2 /
    headline1 / headline2 / subtitle / title1 … title4
  - 常用色：primary / onPrimary / error / primaryContainer / secondaryContainer / surface /
    surfaceContainer / onSurface / onSurfaceVariantSummary / onBackgroundVariant / outline / dividerLine

## 怎么核实 API —— 别凭记忆写

直接从 Maven Central 拉 sources jar 对照签名，比猜可靠得多：

```
https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-android/<版本>/miuix-android-<版本>-sources.jar
https://repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-android/<版本>/miuix-android-<版本>.aar
```

sources jar 里 `commonMain/…/*.kt` 是真实签名；AAR 里 `classes.jar` 可列包结构、
`aar-metadata.properties` 给出 minCompileSdk。本机 `repo1.maven.org` 可达（GitHub 不可达）。

## 会顺带把 Compose 版本拉高（0.8.8 实查 .module 文件）

MiuiX 是 Compose Multiplatform 构件，android 变体暴露的是 `org.jetbrains.compose.*`：

- `androidApiElements-published`：`org.jetbrains.compose.foundation:foundation:1.10.3`、kotlin-stdlib 2.3.20
- 而 `org.jetbrains.compose.foundation:foundation:1.10.3` 的 android 变体又依赖
  `androidx.compose.foundation:foundation:**1.10.5**`

所以 app 里显式写的 `androidx.compose.ui:ui:1.9.5` 之类**会被依赖解析升到 1.10.x**，实际生效版本
以解析结果为准，不要以为声明写了 1.9.5 就真是 1.9.5。`androidx.compose.ui:ui:1.10.5` 的 AAR 元数据
是 `minCompileSdk=35`、`minAndroidGradlePluginVersion=8.6.0`，compileSdk 36 + AGP 8.11.1 都满足。
