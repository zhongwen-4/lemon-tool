# 磨砂玻璃与液态玻璃：依赖、设备门槛、上游怎么接线

SUMMARY: 两个效果都在 **`miuix-blur-android:0.9.4`** 这一个坐标里（**`miuix-ui` 不会把它带进来**）：
「磨砂」= `textureBlur` / `rememberLayerBackdrop` / `layerBackdrop`；
「液态玻璃」= `ui/component/liquid/` 那四个文件（`lens` 折射 + `innerShadow` 内阴影 + `vibrancy` 饱和度 +
`highlight.*` 倾斜高光 + `sensor.rememberDeviceTilt`），外加 `ui/component/miuix/animation/InteractiveHighlight.kt`
（按下去的径向高光，依赖本仓库已有的 `modifier/inspectDragGestures`）。
该 aar 的 manifest **硬写 `minSdkVersion 33`**（本项目 24）→ 靠 app manifest 里
`<uses-sdk tools:overrideLibrary="top.yukonga.miuix.kmp.blur"/>` 放行（上游 SukiSU 是 minSdk 26，也是这么绕的）；
运行时门槛分三档：**API 31 才有 `RenderEffect`（磨砂）、API 33 才有 AGSL `RuntimeShader`（液态玻璃的折射）**，
更低的设备**开关打开也没效果** —— 这是上游口径，不是 bug。
接线照上游 `MainScreen`：`rememberBlurBackdrop(enableBlur)`（拿不到就是 null，顶栏退化成纯色）→
`rememberLayerBackdrop { drawRect(surfaceColor); drawContent() }` → 主内容那一层挂
`Modifier.layerBackdrop(...)`（上游是外层 Box 挂 blurBackdrop、Pager 挂 backdrop），
底栏再把 `backdrop` 交给 `FloatingBottomBar(backdrop = …, isBlurEnabled = …)`。
**不挂 `layerBackdrop` 的话胶囊没有可折射的内容**，只剩底色 —— 这是「效果没出来」最常见的原因。
本项目的两个开关在 `ui/util/DisplaySettings.kt`（SharedPreferences + Compose 状态，默认都开），
设置页两行 `SwitchPreference` 读写它，主壳通过 `LocalEnableBlur` / `LocalEnableFloatingBottomBar` /
`LocalEnableFloatingBottomBarBlur` 往下给。
**注意 `rememberLayerBackdrop` / `rememberCombinedBackdrop` 是纯 Compose 构造**（javap 核过：
方法体里没有 `android.graphics` 引用），低版本调用不会崩；真正碰高版本 API 的是
`drawBackdrop` / `textureBlur` / `lens`，它们都在运行时门控后面。

READ WHEN: before 动「磨砂玻璃 / 液态玻璃」、往界面里加 blur / backdrop 效果，或用户问「这个效果为什么我看不到」时。
RECHECK WHEN: 升级 miuix-blur（组件签名与门槛可能变），或上游 SukiSU 换了效果实现时。