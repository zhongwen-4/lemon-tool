# 盖在主壳上的整页：叠层不要换壳；系统返回手势要自己接

SUMMARY: 本项目没有导航库（上游 SukiSU 用 navigation3），所以「关于页」这种整页要在**同一个 Box 里叠一层**：
`Box { ScannerShell(...); if (aboutOpen) MiuixAppTheme { Box(...) { AboutScreen(...) } } }`。
以前写成 `if (aboutOpen) AboutScreen() else ScannerShell()`，从关于页回来时**主壳被重建** ——
底栏跳回主页、扫描结果也丢光。叠上去的那一层要 `.background(colorScheme.surface)` 挡住下面的内容、
`.pointerInput(Unit) { detectTapGestures { } }` 吞掉点按（上游的悬浮底栏也这么干），
并且**必须包进 `MiuixAppTheme`**（否则落到 `lightColorScheme()` 默认值上，深色模式发白，见 miuix-theme-scope）。
返回手势：`BackHandler` 能接住返回键，但**页面不会跟着手指走**；要跟手就用
`PredictiveBackHandler(enabled = true) { progress -> progress.collect { onBackProgress(it.progress) }… }`
（API < 34 上它等价于普通返回键，同样回调 onBack；手势取消会抛 `CancellationException`，把进度归零即可），
进度喂给外层 `Modifier.graphicsLayer { translationX = size.width * slide }` 做跟手滑出。
**同一层只留一套**：`AboutScreen.kt` 里已经用了 `PredictiveBackHandler`，就不要再叠 `BackHandler`
——后注册的那个会赢，跟手动画会失效。
另外：`rememberLayerBackdrop` 那种 `drawRect(...)`/`drawContent()` 的 lambda 是 **非 composable 的 DrawScope**，
里面读 `MiuixTheme.colorScheme`（`@Composable @ReadOnlyComposable`）会直接编译失败
（`@Composable invocations can only happen from …`）——先把颜色 hoist 成 `val surfaceColor = colorScheme.surface`。

READ WHEN: before 给本项目加「盖在主壳上的整页 / 全屏弹层」，或用户反馈「返回直接退到桌面」「返回后底栏跳回主页 / 结果丢了」时。 用户提「给预测性返回手势加开关」时也照这里的口径做。

## 开关（2026-09-29）

用户要「把预测性返回手势加个开关」。上游 SukiSU 确实有同名设置项 `settings_enable_predictive_back`
（zh-CN 文案「预测性返回手势 / 启用对预测性返回手势的支持」，图标 `Icons.AutoMirrored.Rounded.MenuOpen`），
但它是个**应用级**开关：`KernelSUApplication.onCreate` 在 API 34+ 用隐藏 API
`ApplicationInfo#setEnableOnBackInvokedCallback`（配 `org.lsposed.hiddenapibypass`）翻平台的预测性返回标志，
**重启才生效**，而且只影响平台自己的返回动画（那一行也因此只在 Android 14+ 出现）。

本项目没引那层隐藏 API 依赖，预测性返回也只有关于页这一处，所以开关直接管那一处：
开 = `PredictiveBackHandler`（页面跟手往右滑出）、关 = `BackHandler`（照常回上一页，页面不动画），
`if/else` **二选一**，两条路不能同时挂（见上）。默认 **开**：本项目 0.9.0 起这个效果就是开着的，
默认改关等于把用户已经看到的东西悄悄拿走（上游默认 false，那是它的口径）。

另外 manifest 补了 `android:enableOnBackInvokedCallback="true"`（配 `tools:targetApi="33"`）：
**API 33/34 不加它，`PredictiveBackHandler` 根本收不到进度回调**（跟手动画出不来，退化成普通返回）；
API 35+（targetSdk 33+）平台默认开着、这个属性被忽略。

配套代码：`DisplaySettings.enablePredictiveBack`（KEY `enable_predictive_back`）·
`SettingsMiuix.kt` 一行 SwitchPreference · `ScanUi.kt` 把 `display` 提到 `ScannerScreen`（原先建在 `ScannerShell` 里，
关于页那一层读不到）再往下传。
