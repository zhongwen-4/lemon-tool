# 没写 android:theme 的 app：状态栏、导航栏、启动底色全是黑的

SUMMARY: manifest 里不写 `android:theme`，平台就按默认的**深色** `Theme.DeviceDefault` 走，浅色界面上会多出
黑色的状态栏/导航栏和黑开屏；补一个只管窗口与系统栏的主题即可（界面配色仍归 MiuixTheme 的莫奈取色）。
READ WHEN: when 出现「界面顶上/底下有一条黑」「启动先黑一下」、或要给 Compose/Miuix 的 app 配平台主题时。

---

## 机制

- 没有 `android:theme` 时，用的是平台默认的 `Theme.DeviceDefault`（**深色**），`statusBarColor`、
  `navigationBarColor`、`windowBackground` 全跟着它 —— 跟 Compose / Miuix 的取色**毫无关系**，
  所以界面是浅色莫奈、顶上却是一条黑。
- Android 12+ 的启动画面背景默认取 `windowBackground`，所以黑开屏是同一个根因，不用单独写
  `windowSplashScreenBackground`。
- Android 15+ 且 targetSdk ≥ 35 时已强制 edge-to-edge：`statusBarColor` 被忽略（系统栏透明、内容画到栏下），
  这条主要影响 **Android 14 及以下**与开屏那一瞬。

## 修法（2026-09-20 在 lemon_tool 上落地过）

- `res/values/themes.xml` 与 `res/values-night/themes.xml` 各一个 `Theme.Mrs`，
  parent 用 `@android:style/Theme.DeviceDefault(.Light).NoActionBar`；
  只 override `windowBackground`/`statusBarColor`/`navigationBarColor` +
  `windowLightStatusBar`/`windowLightNavigationBar`，颜色走 `@color/window_surface`（values 浅 / values-night 深）。
- 然后用 `android:theme="@style/Theme.Mrs"` 挂在 `<application>` 上。
- 只管窗口与系统栏，别在这里配界面颜色：MiuixTheme(Monet) 才是界面配色的唯一来源，两边混着来必然对不齐。
- 想更严丝合缝也可以改用 `enableEdgeToEdge()` + 运行时把系统栏设成 MiuixTheme 的 surface，
  但那要写 Kotlin 且 `statusBarColor` 在新系统上已废弃 —— 先用静态主题够用。
