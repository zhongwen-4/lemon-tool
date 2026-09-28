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

READ WHEN: before 给本项目加「盖在主壳上的整页 / 全屏弹层」，或用户反馈「返回直接退到桌面」「返回后底栏跳回主页 / 结果丢了」时。