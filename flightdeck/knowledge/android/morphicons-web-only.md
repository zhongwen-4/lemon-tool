# 网页端形变库 morphicons 进不了本 App

SUMMARY: morphicons（npm 包 `morphicons`，站点 morphicons.com）是 **web 端的 SVG 形变动画库**：
React / Vue / Svelte / React Native / vanilla DOM 五种绑定，靠 DOM 或 `react-native-svg` 驱动，
**没有 Android / Compose / Kotlin 产物**；它也不是图标集——形变的源与目标都是 Lucide / Tabler /
Heroicons(outline) / Iconoir 这类**描边图标**的数据（`lucide` 的 IconNode，或裸 `d` 字符串），
实心图标（Material Symbols、Phosphor fill）能解析但形变途中读不对。算法全在 TS 里：
归一化成三次贝塞尔 → 弧长重采样 → 对应点 → Procrustes 对齐 → 极坐标插值 → 弹簧。
要在 Compose 里求同样观感只有两条路：拿它的**底层图标**（Lucide）自己转 `ImageVector`，
或者复刻那套算法。别指望往 APK 里引依赖。
READ WHEN: when 用户要求「用某个网页端的动效 / 图标库（morphicons 等）改本 App 界面」，或评估
往 APK 里引入 JS 生态的图标、动画库时。
RECHECK WHEN: 该库出了 Kotlin/Compose 绑定，或本项目改用 WebView / RN 外壳之后。

---

出处是站点自述：`https://www.morphicons.com/llms.txt`（完整文档 `llms-full.txt`）。它的绑定只有
`morphicons/react`、`/vue`、`/svelte`、`/react-native`、`/dom`、`/adapters`，peer 依赖是
`react` / `vue` / `svelte` / `react-native-svg`——全是 JS 生态。它把「动画」和「图标」分得很清：
**图标永远是外来数据**，库只负责两套路径之间的形变，所以「换成 morphicons 图标」这个说法本身
就不成立（真正要换的是 Lucide 那套图标，morphicons 只是形变引擎）。