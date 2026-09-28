# 关于页的动态背景（OS3 渐变）：要什么依赖、什么时候才画得出来

SUMMARY: 用户 2026-09-28 要「把 SukiSU 关于页的背景色抄过来」——那份背景是 `BgEffectBackground` 里的
**AGSL RuntimeShader 渐变**（不是毛玻璃），底层要 `android.graphics.RuntimeShader`（**API 33**）；
本项目用 **`miuix-shader-android:0.9.4`** 封装（包名 `top.yukonga.miuix.kmp.shader`，**minSdk 24**，
有 `RuntimeShader(String)` / `asBrush()` / `isRuntimeShaderSupported()`），**`miuix-ui` 不会把它带进来，
要自己加依赖行**。上游门控是 `isRuntimeShaderSupported() && enableBlur && SDK >= 35`；本项目没有毛玻璃
那一支，只留 `isRuntimeShaderSupported() && SDK >= 35` —— 即 **Android 15+ 才画，13/14 及更低退化成窗口底色**。
它和 `miuix-blur-android` 名字像、门槛完全不同：后者 manifest 硬要求 minSdk 33（已删），前者 minSdk 24（可用）。
READ WHEN: before 动关于页的背景、移植 SukiSU 的 `ui/component/miuix/effect/*`，或要判断「某个效果在这个设备
/这个版本上画不画得出来」时。
RECHECK WHEN: 上游改了 effect 的门控，或本项目抬了 minSdk、决定做毛玻璃之后。

---

## 为什么之前「没有背景色」（两层原因，得一起改）

1. 抄的时候把参数写死了 —— `AboutMiuix.kt` 里是
   `BgEffectBackground(dynamicBackground = false, …, effectBackground = false, …)`。
2. `BgEffectBackground` 第一行就是
   `if (!isRuntimeShaderSupported()) { Box(modifier, content); return }`，
   而且 `effectBackground = false` 时 `BgEffectModifier` 只画 `surface` 就收工 —— 于是背景整条没有。

现在的口径（2026-09-28，commit `08c4cb4`）：

```kotlin
val effectBackground = remember {
    isRuntimeShaderSupported() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
}
BgEffectBackground(
    dynamicBackground = effectBackground,
    modifier = Modifier.fillMaxSize(),
    bgModifier = Modifier,          // 上游这里传 layerBackdrop（毛玻璃的壳），本项目不搬
    isFullSize = true,
    effectBackground = effectBackground,
    alpha = { 1f - scrollProgress },
) { … }
```

低版本（< 33）由平台窗口底色兜着（`Theme.Mrs` 的 `@color/window_surface`：浅色 `#FFFBFE`、
深色 `#141218`），所以「退化」不是全黑。另外这条背景还依赖页面在 MiuixTheme 里（取 `surface` 与
`isInDarkTheme()`），见 `miuix-theme-scope.md`。

## 搬进来的东西（都在 `ui/component/miuix/effect/`）

| 本项目 | 上游 | 改动 |
| --- | --- | --- |
| `BgEffectBackground.kt` | 同名 | 包名；`isInDarkTheme()` / `shouldShowSplitPane()` 换本项目实现；RuntimeShader 换坐标 |
| `BgEffectConfig.kt` | 同名 | 只改包名（四套调色板一字未动） |
| `BgEffectPainter.kt` | 同名 | 包名 + `kmp.blur` → `kmp.shader` |
| `BgEffectModifier.kt` | 同名 | 只改包名 |
| `OS3BgFrag.kt` | 同名 | 只改包名（AGSL 正文一字未动） |
| `DeviceType.kt`、`ui/util/WindowSize.kt` | 同名 | 只改包名（手机 / 平板两套配色靠 `shouldShowSplitPane()`） |

正文是否真的一字未动，用这条比（剥掉注释行再比，比的是「行为」）：

```powershell
function Norm($p) { $l = [System.IO.File]::ReadAllLines($p) | Where-Object {
  $_.Trim() -ne "" -and -not $_.TrimStart().StartsWith("//") }
  ($l -join "`n").Replace("com.sukisu.ultra","com.lemon.mrs").Replace("kmp.blur","kmp.shader").Trim() }
```

## miuix-shader 怎么验（2026-09-28 实测）

- 解 aar：`tar -xf <hash>\miuix-shader.aar -C <dir>` —— 里面有 `AndroidManifest.xml`
  （`minSdkVersion 24`）、`classes.jar`、`META-INF/com/android/build/gradle/aar-metadata.properties`
  （`minCompileSdk 37`）。
- 看 API：`javap -p -classpath classes.jar top.yukonga.miuix.kmp.shader.RuntimeShader_androidKt`
  （加 `-c` 能看出 `isRuntimeShaderSupported()` 就是 `SDK_INT >= 33`）；同目录还有
  `RuntimeShader`（interface，`setFloatUniform` 一堆重载）/ `AndroidRuntimeShader`。
- 加依赖后本机先跑 `:app:checkDebugAarMetadata` 与 `:app:processDebugMainManifest`，别等 CI。
