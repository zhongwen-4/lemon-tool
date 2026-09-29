# module-risk-scanner

状态：进行中。已定：Android 设备端、C++ 核心 + MiuiX(Compose) 界面的 APK、只检查未安装的模块包。

## 已定决策

- 2026-09-20 运行位置：在 Android 设备上跑。
- 2026-09-20 技术栈：C++ 核心（CMake）+ Kotlin/Compose 外层，UI 用 MiuiX 组件库。
- 2026-09-20 界面：`top.yukonga.miuix.kmp:miuix-android:0.8.8`（见
  `knowledge/android/miuix-0.8.8.md`）。**它硬要求 compileSdk 36**（当时 compileSdk/targetSdk 都跟到 36）。
  2026-09-27 升 MiuiX 0.9.4 之后被
  `checkReleaseAarMetadata` 逼到 **compileSdk 37（配 `compileSdkMinor 2`）**，**targetSdk 仍保持 36**
  （targetSdk 才决定运行时行为）；详见 `knowledge/build/compile-sdk-and-aar-metadata.md`。
- 2026-09-20 交付形态：APK，由 GitHub Actions 编译（`.github/workflows/android.yml`）。
- 2026-09-20 扫描范围：只检查**未安装**的模块包（用户选一个 zip）。不碰 `/data/adb/modules`，
  不需要 root，不联网。
- 2026-09-20 体积策略（如实记录代价）：核心侧仍然抠——`-Oz`、`-fno-exceptions`/`-fno-rtti`、
  `--gc-sections`、`-Wl,--exclude-libs,ALL`、单 ABI arm64-v8a、minSdk 24、release 开 R8 +
  shrinkResources。但引入 MiuiX/Compose 后，**原来的「尽量小」不再可能**：Compose 运行时本身就
  是几 MB 量级。CI 的体积守卫已从 4 MiB 放宽到 12 MiB。LTO 还没开（待评估收益）。

## 已完成

- `core/`：自研 DEFLATE 解压（零第三方依赖）+ zip 解析 + module.prop 解析 + 规则引擎 +
  报告输出（终端文本 / JSON）。含 20 条命令与混淆特征规则、URL/IP 提取、base64 长串检测、
  ELF 架构识别、setuid 位检测、`system/` 覆盖统计、开机与安装钩子清单。
- `core/tests/`：夹具 + `run_tests.ps1` 断言脚本。宿主 MSVC 下全部通过：deflate 与 stored
  两种压缩方式、目录扫描、JSON 输出、以及 `rm -rf /data/...` 不误报「删除根路径」的回归。
- `app/`：APK 外壳 —— 用 SAF 选 zip（不需要任何权限）→ 拷到缓存 → JNI 扫描 → 显示报告。
- `.github/workflows/android.yml`：两个 job。`core` 在 ubuntu 上 g++ 编核心并跑同一套断言；
  `apk` 装 NDK/CMake、生成临时签名、`assembleRelease`、体积守卫、上传产物。
- `app/` 的界面：MiuiX 组件写的主界面（`SmallTopAppBar` + `Card` + `Button` +
  `SmallTitle` + `Text`），SAF 选 zip → 后台线程扫描 → 用 JSON 结果渲染模块信息、
  高/中/低危计数、逐条发现（按等级配色）、提示与截断说明。
- `app/src/main/cpp/jni_bridge.cpp` 改为只暴露 `nativeScanJson`，返回核心的 JSON。
- 2026-09-20 核心对外收敛成一个入口 `mrs::scan_path()`：用 `stat` + `S_ISREG` 判文件还是目录
  （原来 CLI 用 `fopen` 判定，**Linux 上 fopen 打开目录会成功**，导致目录被当 zip 解析，
  CI 的 `core` job 因此红过一次；见 `knowledge/build/posix-vs-win32-portability.md`）。
- 2026-09-20 **CI 首次全绿**：run `35475236439`（commit `646931e`）两个 job 都过，产出
  `mrs-apk`：`app-release.apk` **1127142 字节 ≈ 1.07 MiB**，内含
  `lib/arm64-v8a/libmrs_jni.so`（252 KB），远低于 12 MiB 预算。

- 2026-09-20 **界面整体重写（莫奈取色）**，`ScanUi.kt` 从 300 行长到 510 行：
  - 取色：`MiuixTheme(controller = ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem))`。
    坑：**不用 Monet 时默认卡片就是白底压白底**（静态主题里 `surfaceContainer = Color.White`），
    所以「比纯白背景深一点」这个诉求本身就要求走 Monet，莫奈下卡片底色才和页面底色分得开。
  - 顶部固定栏：**没用 `SmallTopAppBar`**（它的 `title` 是 `String`，塞不进图标+两行文字），
    自己写 Row 丢进 `Scaffold(topBar = { })`：应用图标 40dp + 3dp 间距 + 软件名(title3) +
    下方小字版本号(footnote2，从 PackageManager 读 versionName)。
  - 上部 40%：`Modifier.weight(0.4f)` 的单个 Card 面板，里面放 72dp 模块图标
    （`MiuixIcons.Layers`，扫描中换成转圈）、模块名/版本/作者/id，以及**唯一的操作按钮**
    （选 zip / 重新选择）。下部 60% 放检测项，两侧按权重切分屏幕高度，互不挤压。
  - 检测项：`LazyColumn` + 计数概览卡。**高危红框（`colorScheme.error`）、中危黄框
    （自定琥珀色，暗色下换亮一档）、低危与信息透明框**；点击整卡展开，展开区显示
    「说明」+ `文件(<文件名>)第 x 行` + 完整路径（等宽字体）。
  - 展开状态用 `ScannerScreen` 里的 `Set<Int>` 提升保管，**没有放在 item 内部 `remember`**
    ——LazyColumn 会回收 item，内部 remember 一滚就没。
- 2026-09-20 补上应用图标（**之前 `AndroidManifest.xml` 根本没有 `android:icon`，桌面是默认机器人**）：
  `drawable/ic_app_mark.xml`（矢量，头部直接用）+ 自适应图标
  （`ic_launcher_foreground.xml` + `mipmap-anydpi-v26/ic_launcher.xml`）+
  `mipmap-anydpi/ic_launcher.xml`（API 24-25 回退）。
- 2026-09-20 依赖加 `top.yukonga.miuix.kmp:miuix-icons-android:0.8.8`（图标不在 miuix-android 里，
  见 `knowledge/android/miuix-0.8.8.md`）。
- 2026-09-20 **CI 全绿**：run `35477558871`（commit `f381080`）两个 job 都过，
  `app-release.apk` **1273442 字节 ≈ 1.21 MiB**（上一版 1132958 ≈ 1.08 MiB，**+140 KB**）。
  体积增量已核实去向：图标构件只留下用到的 3 个（dex 里搜未引用图标名为 0 命中），
  涨的主要是新界面代码产生的 dex。
- 2026-09-20 **按用户给的参考图（TrustAttestor 截图）重做版式**，第二次整体重写 `ScanUi.kt`：
  - 结构从「顶部 40% 模块面板 + 下部列表」换成参考图的三段式：**顶部固定标题栏（底部带分割线）
    → 状态卡 → 检测项卡列表 → 底部导航（主页/关于）**。按屏幕比例切分的 `HERO_WEIGHT` 已删除。
  - 状态卡：居中状态圆环图标（异常红/琥珀、正常绿、待机用 `MiuixIcons.Layers`、扫描中换转圈）
    → 结论标题（`title3`）→ 核心 `verdict` 原文 → 全宽圆角按钮（52dp 高、26dp 圆角，比
    `ButtonDefaults` 默认的 40dp/16dp 更胖，贴合参考图）→ 底部 caption（模块名 + 上次检测时间 + 文件数）。
    有结果时是双按钮：「重新检测」（重扫缓存里同一个 zip，不弹选择器）+「选择其他模块」。
  - 检测项卡：左侧等级圆图标、中间标题(`body1`)与一句话副标题、右侧等级胶囊（`StatusPill`：
    12% 透明底 + 45% 边框 + 同色字）与展开箭头；展开后是**两张白底内嵌卡**——用
    `colorScheme.surface` 而非外层卡片的 `surfaceContainerHigh`（浅色下内层更白、深色下内层更深，
    两层始终分得开），标题各配 `MiuixIcons.Lock`（说明）与 `MiuixIcons.Info`（详情）。
  - 高危/中危保留彩色描边（`Modifier.border`，20dp 圆角），低危与信息仍是透明框。
  - 底部导航用 MiuiX 的 `NavigationBar(mode = NavigationBarDisplayMode.TextOnly)`。
    坑：`NavigationBarItem` 的 `icon: ImageVector` 是**必填**参数，TextOnly 模式下不渲染但躲不掉，
    只能随便给一个矢量（**2026-09-24 已改成 `IconAndText`，主页图标同时换成 `Scan`**）。
  - 新增 `ic_state_alert.xml` / `ic_state_ok.xml`：24dp 视口、统一用 `#FF000000` 描一个圆环 + 感叹号/对勾，
    靠 `Icon(tint = ...)` 的 ColorFilter 整体染色，所以同一个矢量能出红、琥珀、绿三种颜色。
  - 四档配色定了：高红、中黄、低绿（描边仍透明）、信息灰。
- 2026-09-20 **CI 全绿**：run `35478921424`（commit `70b70ba`），`app-release.apk`
  **1290986 字节 ≈ 1.23 MiB**（上一版 1273442 ≈ 1.21 MiB，**+17 KB**）。
- 2026-09-20 **CI 全绿**：run `35480514245`（commit `7265af4`），`core tests (host)` 与 `apk (arm64-v8a)` 都过；
  artifact `mrs-apk` 的 zip 从上一版的量级掉到 **999 KB**（APK 实际字节这次没读 job 日志）。
- 2026-09-21 **三件事一起做**（用户指令）：
  1. 顶栏图标加回（原样）。
  2. 版本号 0.1.0 → **0.2.0**（versionCode 1 → 2）。
  3. 「关于」页新增**更新检查**：`releases/latest` 的 `tag_name` 跟已安装 versionName 逐段比数字，
     只引 `HttpURLConnection` + `org.json`（不加依赖、不加体积）。同时给 CI 接上**tag 发版**：
     推 `v*` 时 `gh release create` 把 APK 挂上去（无第三方 action）。详见
     `knowledge/build/release-and-update-check.md`。
  - 代价：**加了 `INTERNET` 权限**，「不联网」这个卖点必须改成「扫描全程离线、只有检查更新联网」，
    「关于」页的措辞已同步改掉（原文是假陈述）。
  - 首个 Release：`v0.2.0`（tag run `35539547477`），APK **1.24 MiB**，`releases/latest` 实测返回正常。
  - 真机验证仍待用户：装 0.2.0 点「检查更新」应显示「已经是最新版本（v0.2.0）」。


- 2026-09-27 **CI 出包失败（run `36281282791`）已修**：升 MiuiX 0.9.4 时把 compileSdk 留在 36，
  `apk` job 在 `:app:checkReleaseAarMetadata` 直接失败（20 条 issue 全是「要求 compileSdk ≥ 37」）。
  改成 `compileSdk 37` + `compileSdkMinor 2`（SDK 平台从 37 起按小版本发布，只有 `android-37.2`），
  `targetSdk` 不动；本机接着又撞出第二道门 —— `miuix-blur-android:0.9.4` 硬要求 minSdk 33，与本项目
  minSdk 24 冲突，代码里没人用它，直接删依赖。CI 里平台换 `platforms;android-37.2` +
  `build-tools;37.0.0`。commit `6af075b`。知识：
  `knowledge/build/compile-sdk-and-aar-metadata.md`。
  **复跑结果**：CI run `36282815228`（commit `bac6019`）两个 job 全绿，`apk` 出包成功，artifact `mrs-apk` **1.10 MiB**（远低于 12 MiB 预算）。
- **新版式一次都没被看过**（2026-09-27）：本机只能编 Kotlin，渲染要么等 CI 出的 APK、要么等真机。
  两个我照上游抄但没验证过的地方：英雄卡靠 `Row(height(IntrinsicSize.Min))` 撑高度、
  右上角 110dp 大图标靠 `offset` 溢出被卡片裁掉——上游是这么写的，但与我们的文案长度不同，
  实际高度/裁切效果要真机看。底栏胶囊也是同样情况（拖动切页、按压缩放都没上手试过）。

## 未验证 / 风险

- APK 构建路径本机跑不了（没有 NDK；Gradle 能跑但打不了包），只能靠 CI 验证。
  **2026-09-27 补正**：本机除 native 编译与打包签名以外的环节都能预先跑出来（AAR 元数据、清单合并、
  R8、资源裁剪），只要跑对任务 —— `compile*Kotlin` 过**覆盖不到**前两道门。见
  `knowledge/build/android-toolchain.md` 的「本机验证能走到哪一步」。
- 真机没连过（`adb devices` 为空），报告在设备上的实际显示效果没验证过。
- ~~本机连不上 GitHub~~ 已解决：本机 FlClash 代理 `http://127.0.0.1:7890` 可用，
  `git -c http.proxy=http://127.0.0.1:7890 push lemon-tool main` 稳定成功。
  读 CI 日志的办法见 `knowledge/tooling/github-actions-logs-via-api.md`。
  2026-09-20 补充：**别把代理当默认前提**。当天 FlClash 还在跑，但 `127.0.0.1:7890` 已经没人监听
  （`Test-NetConnection`/`TcpClient.Connect` 直接拒连、系统代理 `ProxyEnable=0`），带代理推送报
  `Failed to connect to github.com port 443 via 127.0.0.1`；而同一时刻**直连是通的**
  （`curl https://github.com` 与 `https://api.github.com/rate_limit` 都 200），
  改用 `git -c http.proxy= -c https.proxy= push lemon-tool main` 一次成功。
  所以推送/查 API 失败时先花两秒测直连，别只剩「重试代理」一条路。
- **Kotlin/Compose 这条构建链本机一次都没编过**：本机没有 Kotlin 编译器（2026-09-24 起 wrapper 9.2.0 能在本机跑到配置阶段，编 APK 仍缺 NDK），
  MiuiX 的 API 用法是靠拉 sources jar 逐个核对签名得来的（已核对：Text/Button/Card/Scaffold/
  SmallTopAppBar/SmallTitle/CircularProgressIndicator/MiuixTheme 及所用样式与色名）。第一次真
  编译会发生在 CI。
- AGP 8.11.1 + Gradle 8.13 + Kotlin 2.3.20 + MiuiX/Compose 这条链**已由 CI 实测通过**
  （2026-09-20），不再算风险项。

- **界面渲染效果一次都没被看过**：本机编不了 Kotlin/Compose，CI 只保证「能编译、能出 APK」，
  布局是否好看、黄框对比度够不够、顶部 40% 面板会不会太空，都要等真机截图才能判断。
- 莫奈取色在 **Android 11 及以下会回落到 MiuiX 默认紫**（`platformDynamicColors` 在 API < 31
  直接给 `monetSystemColors()`），不是壁纸色；minSdk 24，真机得是 Android 12+ 才看得到莫奈效果。

- 「关于」页的内容与措辞是**我自己补的**（参考图有 主页/关于 两个 tab，但用户没说过要什么），
  未经确认。
- 顶栏图标 2026-09-20 删过一次、**2026-09-21 按用户要求原样加回**（`ic_app_mark` 40dp +
  `HEADER_ICON_GAP = 3.dp`，他自己定的数字）。回推：他说的「最上面黑色的东西」**不是**这个图标，
  大概率是最上面那条系统状态栏——也就是当天一并修掉的那个 `android:theme` 缺失（见下一条）。
  所以 `ic_app_mark` 在 40dp 下就是「黄底 + 近黑盾牌」这个观感问题仍然存在，他没提就当没意见。
- 2026-09-20 一起挖出并修掉的根因：这个 app 的 manifest **从来没写过 `android:theme`**，平台按默认的
  **深色** `Theme.DeviceDefault` 走，状态栏、导航栏、启动底色全跟着它变黑（与 MiuixTheme 的莫奈取色无关）。
  补了 `values/themes.xml` + `values-night/themes.xml` + `@color/window_surface`，只 override 窗口底色与
  系统栏；机制见 `knowledge/android/no-theme-means-black-system-bars.md`。
- **这次是「两头都堵」的改法**：用户那句话我看不到截图，分不清他指顶栏那个深色图标还是最上面那条系统
  状态栏，所以两个都改了。他回话后只留下他要的那一个方向。

## 已知缺陷

- ~~误报严重~~ **2026-09-20 已修**：规则拆成「特征表（最高中危）+ 升级表（组合/路径敏感才高危）」，
  并加了 `verdict` 定性结论。善意夹具断言写死「高危 0 · 中危 0」，恶意夹具断言 9 条升级规则全中。
  模型细节见 `knowledge/detection/rule-design.md`。
  残留风险：目前调参只靠自造的两个夹具，**还没有真实模块样本**，路径分档与升级阈值可能需要再调。
- **条目数没有上限**：`core/src/zip.cpp`/`scan.cpp` 对单个条目有 8 MB 上限，但没限制
  zip 条目数量；构造一个几十万条目的 zip 可以撑爆内存。需要加条目数上限并给报告截断。
- ~~`uninstall.sh` 的自清理被判中危~~ **2026-09-21 已修（用户选了「甲」）**：卸载脚本仍照扫、仍进
  文件与脚本清单，特征表命中照记，但**升级表不对它生效**，`obf.blob` / `obf.eval-dynamic` 这两条
  内联升级也一并压掉（记录最高只到低危），报告里加一条提示说明「只在卸载时执行、不计入风险统计」。
  同一个良性样本现在拿到 **高危 0 · 中危 0 · 低危 4** + 提示。规则细节见
  `knowledge/detection/rule-design.md` 的「例外」一节。

## 下一步

1. ~~重构检测规则模型~~ 已完成（2026-09-20）。
2. 给 zip 条目数设上限。
3. 用真实模块样本压误报（现在是自造夹具，覆盖面有限）。
4. ~~推到 GitHub 跑 workflow~~ 已完成（见上）。
4b. ~~卸载脚本不参与升级判定~~ 已完成（2026-09-21，用户选「甲」）。
5. 装到真机，验证「选 zip → 出报告」整条链路 —— **等用户测试反馈**。
6. 候选（2026-09-20 用户提「做个虚拟环境刷写模块看执行了哪些命令」后评估）：APK 内做**静态
   「执行轨迹预览」**——按钩子分组、按顺序列出会执行的命令，零执行、体积几乎不涨。
7. 候选（不进 APK）：桌面/CI 侧 dry-run 沙箱，方法与本机实测见
   `knowledge/detection/dry-run-sandbox.md`。

8. ~~底栏换成 morphicons 图标~~ 已完成（2026-09-24，用户选「丁」）：morphicons 只有 web 端（见
   `knowledge/android/morphicons-web-only.md`），照字面做不了，也没必要——实际做的是
   `TextOnly` → `IconAndText`，主页图标从占位的 `Tasks` 换成 `Scan`。

9. 固定 CI 的签名密钥（keystore 存成 repo secret，或本地固定一份）——否则版本号提了也覆盖安装不了，
   用户每次都得卸载重装。见 `knowledge/build/release-and-update-check.md` 的「坑」。

10. ~~等用户挑「主页」图标~~ → ~~照 SukiSU 的布局重做 UI~~ **已完成**（2026-09-25）：保留现有图标，
    按用户要求复刻 SukiSU 的页面结构与文字层级，不复制其 GPL-3.0 源码。主页、历史、设置三页
    使用分区标题、分组卡片、偏好行和横向 pager；本轮进一步收敛卡片圆角与边距，并让顶栏标题随页面切换。
    本机 `:app:compileDebugKotlin --offline` 已通过，等待 CI 出包和真机确认观感。
11. ~~等用户定：要不要升 MiuiX 0.9.4~~ **已完成**（2026-09-27 升到 0.9.4，连带 AGP 9.4.1 / Gradle 9.7.1 / JDK 21，见「已定决策」）。起因是「照抄 SukiSU 的设置页/列表 UI」——他们的行组件
    （`ArrowPreference`/`SwitchPreference`/`OverlayDropdownPreference`）在 0.9.x 的 `preference` 包里，
    0.8.8 完全没有。实测代价：0.9.4 拉 Compose 1.12.0 → `checkDebugAarMetadata` 报
    **AGP 必须 ≥ 9.1.0**（我们是 8.11.1），要升就连带升 AGP 9 / Gradle 9 / Compose 1.12，并改 CI 的
    `gradle-version` 与 JDK。不升则用手写的 `SettingRow`（已落地）。详见
    `knowledge/android/miuix-0.8.8.md` 的「升到 0.9.x 的代价」。
12. ~~三页接真实数据~~ **已完成**（2026-09-28，见「进度」最后一条）：检查模块与检查历史都接上了。
13. **真机验收**（下一件事）：装 0.7.0 上真机，走一遍「点此开始检测 → 选 zip → 看结论卡与发现列表 →
    切到检查历史看记录 → 点条目看详情 → 清空」，重点看这几处观感与手感：结论卡的长文本换行、
    发现卡的等级配色在深色下是否可读、条目卡在大字号下的裁切、清空确认弹窗的按钮宽度。
14. 检查历史还差的周边（用户没要，先记着）：搜索 / 按等级筛选 / 单条删除 / 导出报告。
15. 记录里现在只存 findings 的摘要字段，**没有存 notes 以外的原文上下文**；如果以后要「点开看原文件那一行」，
    得连文件内容一起存（体积会涨，要重新掂量）。
16. ~~「预测性返回手势」的开关只作用于关于页~~ **已完成**（2026-09-30：改成应用级，见「进度」第九轮）。

## 进度

- 2026-09-20 建档；同日定下 Android + C++ + 体积优先。
- 2026-09-20 核心实现完成并在宿主端通过全部断言；APK 外壳与 CI 已就绪，待 CI 验证。
- 2026-09-20 界面改为 MiuiX(Compose)，compileSdk 跟到 36；同日发现规则误报缺陷。
- 2026-09-20 修掉 Linux 上「目录被当 zip」的路径判定 bug；CI 两个 job 全绿，APK 1.07 MiB。
- 2026-09-20 重做规则模型（特征表 + 升级表）修掉误报：善意样本「高危 0 · 中危 0」，
  恶意样本 6 高危；报告与界面都加了定性结论。
- 2026-09-20 复现第二个误报：良性模块的 `uninstall.sh` 自清理被判 `cmd.rm-rf-adb`（中危）；
  根因是升级表不看文件名（2026-09-21 按用户选的「甲」修掉，见下）。
- 2026-09-20 界面第一轮真机反馈：删掉顶栏深色图标、补平台主题清掉黑色系统栏与黑开屏（见「未验证/风险」）。
- 2026-09-21 按用户要求把顶栏图标加回、版本号提到 0.2.0，并落地「更新检查」（GitHub Release 为数据源）
  + CI tag 发版通道；首个 Release `v0.2.0` 已发布。
- 2026-09-21 用户对 `uninstall.sh` 误报选了「甲」，核心已改并在本机跑通全部断言（good 夹具新增
  `uninstall.sh`，evil 夹具的卸载脚本里塞了升级项用于断言「压掉」；顺带被本机的 BOM 陷阱咬了一次）。

- 2026-09-20 用户质疑「能不能做虚拟环境直接刷写模块看它执行了哪些命令」：本机搭了**假 PATH 沙箱**
  （每条命令一个只记录不执行的 stub + 真 shell 解释器）实测跑通，`evil/customize.sh` 出 14 条真实顺序
  的命令轨迹；结论是这套东西**进不了 APK**（非 root 跑不动关键命令、要 rooted AVD、恶意模块反沙箱、
  且「刷写」本身就是它们等的触发条件）。方法、陷阱与结论记在 `knowledge/detection/dry-run-sandbox.md`。

- 2026-09-21 用户报 IDE 里 `app/build.gradle:1` 的 `JvmTarget` 未解析：根因是仓库一直没有 Gradle wrapper，IDE 只能借用未知 Gradle；
  已补 wrapper（锁 8.13，对齐 CI）+ `.gitattributes`（`gradlew` 强制 LF），本机用缓存 9.2.0 跑 `gradle projects --offline` 验证脚本编译与 AGP 加载全过。
- 2026-09-24 收到「底栏换 morphicons 图标」的需求，调研后驳回照字面实现：morphicons 是 web 端的
  JS 形变库、自身不带图标（底层是 Lucide / Tabler / Heroicons 的描边集），而本 App 的
  `NavigationBarItem.icon` 只吃 Compose `ImageVector`。已列四个替代方案待他选。顺带把 MiuiX
  `NavigationBar` 的真实签名（`icon` 必填、四种 display mode、两套 Defaults）补进
  `knowledge/android/miuix-0.8.8.md`。过程里被本机 PowerShell 的别名优先规则咬掉两个文件
  （`rd` 就是 `Remove-Item`，已 `git restore` 还原，见
  `knowledge/tooling/powershell-aliases-and-cmdlets.md`）。

- 2026-09-24 底栏按用户选的「丁」加图标：`NavigationBar` 的 mode 从 `TextOnly` 改成 `IconAndText`
  （图标 + 文字），主页图标从占位的 `Tasks` 换成 `Scan`——155 个 MiuiX 图标里没有 Home/House，
  `Scan` 是其中最贴题的一个。本机编不了 Kotlin，这次改动同样只能由 CI 验证。

- 2026-09-24 修掉 IDE 报的 `JvmTarget` 未解析（与 2026-09-21 报的是同一条）：根因是**本机只有 JDK 25**，
  而 wrapper 锁的 Gradle 8.13 自带 Groovy 3.0.22 **读不了 Java 25 的 class 文件（major 69）**，构建脚本
  在语义分析阶段就崩，IDE 于是把 KGP 的类型报成未解析。wrapper 改指 **9.2.0**（本机已有该 dist，不联网），
  本机实测 `projects` 与 `:app:assembleRelease --dry-run` 全过（AGP 8.11.1 在 Gradle 9.2.0 上配得起来）。
  CI 保持 8.13 + JDK 17 不动 —— wrapper 与 CI 由此**有意分叉**。详见
  `knowledge/build/android-toolchain.md`。

- 2026-09-24 用户立约定「更新只提版本号、不动包名」，已记入 `flightdeck/briefing.md` 第 8 条。
  顺着这条排查，发现它目前**还做不到**：CI 每 run 用 `keytool -genkeypair` 现生成一把签名密钥，
  不同 run 的 APK 签名不同，覆盖安装必失败（只能卸载重装）。已列为下一步第 9 条。

- 2026-09-24 用户要亲自挑「主页」图标，于是从 `miuix-icons-android:0.8.8` 的 sources jar 里解析出
  全部 155 个 `Regular` 图标的真实路径，生成了可搜索的 HTML 预览 + PNG 联络表（`build/miuix-icons-preview.*`，
  `build/` 不进仓库）。核对过 155 个都是 `NonZero` 填充、且必须套 `group` 的翻转变换才不上下颠倒。
  **等他指定图标**。

- 2026-09-24 用户改主意：不从 MiuiX 里挑，要「几套图标库 + 官网」。查证了 10 套的官网与许可证
  （按 GitHub `homepage` / `license` 字段，不靠印象），纠正两点：**Remix Icon 已不是 Apache-2.0**
  （改成自定义 "Remix Icon License v1.0"）；**只有 Google Material icons 有官方 Android 构件**
  （`material-icons-extended` 到 1.7.8 冻结）。结论记 `knowledge/android/icon-libraries.md`。

- 2026-09-25 preflight 后收到「整个 UI 抄 SukiSU Ultra 的代码、图标用 Tabler Icons」。**先核实再答，没动代码**：
  ① 许可：SukiSU / KernelSU / KernelSU-Next 三者代码都是 GPL-3.0，SukiSU 的启动图标另有
  《SukiSU Ultra 图标有限使用许可证》（严格非商业、禁止提取、禁止用作别的 app 的图标或素材、衍生项目必须
  删除或替换），而本仓库**没有 LICENSE** → 照搬代码不可行；② 规模：它 `manager/.../ui/` 281 个 .kt、
  约 2.0 MB，每个界面写两遍（`XxxMaterial.kt` + `XxxMiuix.kt`），另加自研 Expressive 组件、liquid 玻璃、
  markdown 渲染、WebUI；③ 它底栏用的是 `material-icons-extended` 的 `Icons.Rounded.*`，切页是横向 pager，
  可选浮动毛玻璃底栏——而 **MiuiX 0.8.8 没有 blur / Backdrop**（拉 sources jar 检索零命中），
  要那种效果得升 0.9.x，会牵动整条工具链。另确认 **Tabler 的 SVG 能取到**（走 `api.github.com`
  的 `contents` 端点；raw.githubusercontent 与 curl.exe 在本机都不通）。已把五个问题交回用户，等他答复。

- 2026-09-25 用户定稿后做 UI 改造（悬浮底栏 + pager + material 图标），**本机编译验证通过**：
  底栏从撑满整宽的 `NavigationBar` 换成 MiuiX 自带的 **`FloatingNavigationBar`**（圆角浮岛、居中、
  无毛玻璃——0.8.8 本来也没有 blur API），`selected` 改绑 `pagerState.currentPage`、点击走
  `animateScrollToPage`；内容区换成 **`HorizontalPager`**，「主页 / 关于」两个 tab 可以左右滑。
  新增依赖 `androidx.compose.material:material-icons-extended:1.7.8`，图标用
  `Icons.Rounded.Cottage`（主页）与 `Icons.Rounded.Info`（关于）；版本 0.2.0/code2 → **0.2.1/code3**。
  顺带把**本机的 Kotlin 编译打通了**：JVM 不读 WinINET 代理 → Gradle 下不到依赖、报
  `repo.maven.apache.org ... Connection timed out`；在用户级 `~/.gradle/gradle.properties` 里配
  `systemProp.*` 代理后，`compileDebugKotlin` 与 `compileReleaseKotlin` 都 BUILD SUCCESSFUL
  （打 APK 仍需 CI，本机没 NDK）。所以这一轮改动是**本机验证过**的，不再是「只能等 CI」。
  过程中被 here-string 末尾换行的坑咬了一次（两个 import 粘成一行），见
  `knowledge/tooling/file-edit-anchors-and-newlines.md`。
- 2026-09-25 用户要「照抄 SukiSU 的主页与设置页 + 底栏加检查历史 + 列表 UI 照抄，直接复制代码改文字」。
  照字面做不到（GPL-3.0 且本仓库无 LICENSE；它的行组件在 MiuiX 0.9.x，0.8.8 没有；主页/设置的数据源
  ——root 状态、内核版本、SuSFS/KPM/WebUI 那些——我们这边根本不存在），于是**按它的结构自己写等价实现**，
  本机 debug + release 编译全过：
  ① 底栏从两个 tab 加到三个：主页 `Icons.Rounded.Cottage` / 检查历史 `History` / 设置 `Settings`；
  ② 新增 `History.kt`：`HistoryEntry` + `HistoryStore`（存 `filesDir/history.json`，最新在前、最多 50 条，
  读不出来就当空、不崩）+ `HistoryScreen`（概览卡 + 一张分组卡片里一行一条：图标 + 模块名 +
  时间/版本 + 风险尾值 + 箭头，可清空；点一条把那次报告装回主页）；
  ③ 主页新增 `ModuleInfoCard`（模块名/版本/作者/模块 ID/文件数，一行一项）；
  ④ 「关于」页改成「设置」页：分区标题 + 分组卡片 + 行项（更新 / 存储：清理扫描缓存、清空检查历史 /
  关于：版本、包名）+ 原有的说明与免责；缓存清理接 `MainActivity.clearCachedModule`；
  ⑤ 扫码成功后自动往历史里追加一条。
  版本 0.2.1/code3 → **0.3.0/code4**。又踩了一次 here-string 吃掉末尾换行的坑（import 粘行、`}` 粘行），
  见 `knowledge/tooling/file-edit-anchors-and-newlines.md`。

- 2026-09-25 用户明确要求保留现有图标、按 SukiSU 观感调整 UI，且本轮不调用 impeccable。已修改
  `ScanUi.kt` 与 `History.kt`：顶栏标题随 pager 页面显示上下文；主页增加「模块检查 / 模块信息 /
  检测结果 / 扫描备注」分区，历史页增加「扫描概览 / 历史记录」分区；卡片圆角从 20dp 收到 16dp，
  按钮从 52dp/26dp 收到 48dp/24dp，内容边距从 16dp 收到 12dp。图标资源、引用和版本号未改。
  `:app:compileDebugKotlin --offline` 编译通过。

- 2026-09-25 检查 Android 编译工作流发现三个发布问题并已修复：APK job 增加 `needs: core`；tag 构建
  校验 `vX.Y.Z` 与 `versionName` 一致；固定签名从 Actions secrets 解码且仅 tag 步骤读取，tag 未配置时
  拒绝发布，非 tag 始终用临时签名；已有 Release 改为 `gh release upload --clobber`，支持重跑 tag；
  构建 job 与 tag-only 发布 job 的仓库写权限分离。详见 `knowledge/build/release-and-update-check.md`。

- 2026-09-25 用户决定将本项目声明为 GPL-3.0，根目录新增完整 `LICENSE`。更新 SukiSU 参考知识：源码许可相容，
  但引入其代码仍需保留上游版权/许可证声明并履行对应源码义务；启动图标继续受单独许可限制。

- 2026-09-27 **移植 SukiSU Ultra 的界面代码**（用户指令：移植它的 UI，图标用我们自己的；后又追加「三个页面的版式也照它重画」）。
  三个前置确认（用户答复）：① 范围＝**只搬组件与骨架**，不搬它 `screen/` 里 134 个页面、`webui/`、`kernelFlash/`、
  模板编辑器与双套 Material 实现；② 图标**继续用 `material-icons-extended`**（也正是它底栏用的那套）；
  ③ **只做 Miuix 一套**，不要 UiMode 双实现。许可这条已不再是障碍：本项目 GPL-3.0 与它相容，
  搬进来的文件都写了「上游路径 + 改了什么 + 改动日期 2026-09-27」（GPL-3.0 §5a）；**它的启动图标单独许可，一个字没搬**。
  落地（commit `a05fdbb`）：
  ① 新增 `ui/component/FloatingBottomBar.kt`——它的悬浮底栏，**砍掉毛玻璃那一路**（`isBlurEnabled`/`backdrop` 参数、
      Backdrop/liquid/设备倾斜高光全部去掉），只留纯色路径；深色判断改用 surface 亮度。
      **副产品**：上游那个 `InteractiveHighlight` 只在毛玻璃一路里用，所以整条链一起不用了，
      顺带避开 `android.graphics.RuntimeShader`（要 API 33）与本项目 minSdk 24 的冲突。
  ② 新增 `ui/component/miuix/animation/DampedDragAnimation.kt` 与 `ui/component/miuix/modifier/DragGestureInspector.kt`
     ——**逐字搬**，只改包名（与 `inspectDragGestures` 的 import）。
  ③ 新增 `ui/component/bottombar/MainPagerState.kt`——取它 `BottomBar.kt` 里的 `MainPagerState`/`rememberMainPagerState`
     + `LocalMainPagerState`；翻页动画从 miuix 的 `springAnimateToPage` 换成 `animateScrollToPage`。
     底栏现在支持**左右拖动**切页，手指滑 pager 也会同步高亮（`syncPage`）。
  ④ 新增 `ui/component/Rows.kt`——`InfoRow`（只读行）与 `ActionRow`（可点、尾部带箭头），
     底座是 MiuiX 自带的 `BasicComponent` / `ArrowPreference`（它设置页就是这么写的），
     替掉了我们自己手写的 `SettingRow`/`GroupCard`/`RowDivider`。
  ⑤ **三个页面按它的版式重画**：新增 `PageScaffold`（一页一个 `Scaffold(topBar = TopAppBar(…))` +
     一条 `LazyColumn`：左右 12dp、`overScrollVertical()`、`scrollEndHaptic()`、`nestedScroll(scrollBehavior…)`、
     `contentPadding = innerPadding`，底部给悬浮底栏留位）；主页与历史页的「英雄卡」照它的 `StatusCard` 写法
     （整卡换底色 + 右下角 110dp 图标 `offset(27.dp,31.dp)` 溢出 + 左上角 22.sp 大字 + 左下角操作）。
     `ScannerScreen` 的顶栏从共享的 `AppHeader` 换成每页自己的 `TopAppBar`（它就是这么做的）。
     图标全部沿用原样（Cottage / History / Settings / Info / SystemUpdate / Article / FolderZip）。
  版本 0.3.0/code4 → **0.4.0/code5**。踩到的坑记进 `knowledge/tooling/file-edit-anchors-and-newlines.md`
  （按行号切片重拼时边界算错，把 `HistoryEntry`/`HistoryStore` 整段切掉了，靠 `git show HEAD:` 取回）。
  **本机 `:app:compileDebugKotlin --offline` 与 `:app:compileReleaseKotlin` 都过**；打 APK 与观感仍只靠 CI + 真机。
- 2026-09-28 **界面整包换成 SukiSU 的骨架**（用户第二次改口径：`你直接把现有的ui删掉，然后完全照搬sukisu的，功能不变`，
  随后细化成 `主页只留组件骨架、数据清空` / `检查历史用它的 SU 日志列表、其他组件不要` / `其他的也要骨架、数据先空着`）。
  搬法：上游源码整包解到 `%TEMP%\suku_0928`，逐文件「改包名 → 删 import → 砍不可用依赖 → 留同名空实现 →
  `values-zh-rCN` 批量抽字符串」。**主页 / 关于 / 设置三页照搬结构尺寸**（设置页 572 行整包脚本搬运），
  **检查历史整份删掉换 SU 日志列表**（`History.kt` 已删，只留条目卡 + 详情弹窗），三页数据全是空状态。
  `ScanUi.kt` 1041 → 358 行，只剩外壳 + `PageScaffold` + 扫描管线。
  版本 0.4.0/code5 → **0.5.0/code6**。踩到两个坑并已进知识库：
  ① here-string 丢换行把 `package` 粘进注释，假象像「Kotlin 增量缓存坏了」
  （`knowledge/tooling/here-string-package-line.md`）；
  ② `git push` 连不上 github.com —— DNS 解到了被墙的 IP，本地起 CONNECT 隧道绕过去
  （结论当天稍后即被推翻：真凶是本机 7890 系统代理，见 `knowledge/tooling/github-push-and-local-proxy.md`）。
  本机 `:app:compileDebugKotlin` / `compileReleaseKotlin` / 两个 `check*AarMetadata` / 两个 `process*MainManifest` /
  `minifyReleaseWithR8` / `optimizeReleaseResources` 全过；**三页观感与真机链路仍只靠 CI + 真机**。
- 2026-09-28 **主页/设置页精简收尾 + 去掉莫奈取色**（用户口径：`不要莫奈取色了，用sukisu的配色`，
  设置页 `就留个关于和检查更新`，主页 `不支持` → `点此开始检测`、信息区只留「应用版本」、
  两个状态（SELinux / Seccomp）删掉、`支持开发/了解xxx` → `提交BUG`）。
  - 取色改走上游 `MiuixKernelSUTheme` 的**非莫奈那一支**（`ColorSchemeMode.System` + 不指定 keyColor +
    `Spec2025` + `TonalSpot`）；`MiuixTheme.isDynamicColor` 因此变 false，上游那些 `if (isDynamicColor)`
    分支全部走硬编码色 —— 这就是「SukiSU 非莫奈」的观感。见 `knowledge/android/theme-non-monet-sukisu.md`。
  - `HomeMiuix.kt`：`StatusCard` 三分支只剩一支、改名 `CheckEntryCard`（另两支是 KernelSU 内核取数）；
    `InfoCard` 只留一行「应用版本」（`getManagerVersion` 从 PackageManager 读，不依赖 root）；
    `SupportLinks` 换成一行「提交 BUG」→ 本项目 issues。
  - `SettingsMiuix.kt`：只留「检查更新」+「关于」两行。
  - `strings.xml`：新增 6 条；删掉 79 条已无引用的（被删组件与旧设置页那批根管理器开关）。
  - 版本 0.5.0/code6 → **0.6.0/code7**，commit `c14ecbb`（已推）。
  - 本机 8 条出包前置全过（debug/release Kotlin、两个 AAR 元数据、两个清单合并、R8、资源优化）。
  - **推送的真凶找到了**：本机 `HKCU\Software\Microsoft\Windows\CurrentVersion\Internet Settings` 里
    `ProxyEnable=1`、`ProxyServer=127.0.0.1:7890`（真有进程在听）。**PowerShell 的 `Invoke-*` 走这个
    WinINET 代理，git 用的 libcurl 不走** —— 所以同一台机器上 API 一直通、push 一直不通。
    `git -c http.proxy=http://127.0.0.1:7890 -c https.proxy=http://127.0.0.1:7890 push` 3 秒推完。
    上一轮那条「DNS 解到被墙 IP、本地开 CONNECT 隧道」的结论**已被推翻**；知识文件更名为
    `knowledge/tooling/github-push-and-local-proxy.md`（隧道降级成「没有代理时」的备选，并记下两个坑：
    CONNECT 行里端口后面还跟着 HTTP 版本会让 `int.Parse` 炸、github 的候选 IP 会漂）。

- 2026-09-28 **实现检查模块与检查历史**（用户口径：`开工，检查历史上面的su日志改成检查历史`）。
  - 检查模块 = 把已经打通的扫描链路接到界面：主页新增 `ScanSummarySection`
    （检测中 `CircularProgressIndicator(progress = null)` 转圈 / 失败 `WarningCard` / 成功出结论卡），
    结论卡按最高等级换底色，含 verdict 大字、模块名、版本·作者·文件数、高/中/低/信息计数标签、
    发现条数与 notes；发现逐条一卡（等级标签 + 规则名 + 文件:行 + 说明），
    走 LazyColumn 的**独立 item**（几百条也是懒加载，不撑爆首屏）。`ScanState` 由 private 改公开。
  - 检查历史 = **本项目自己的扫描记录**（本项目没有 `/data/adb/ksu/log`）：新增 `ScanHistory.kt`，
    一行一条 JSON 存 `filesDir/scan_history.jsonl`，新的在前、上限 200、超出丢最旧，读写全包在
    `runCatching` 里（读坏当空历史）。扫描成功后写一条并刷新内存列表；进页面读一次。
    骨架仍用移植来的 SU 日志列表：映射在 `ScanUi.kt` 的 `scanRecordToSulogEntry`，
    键名走 `ScanEntryFields` 两边共用（防写岔）。顶栏标题由「SU 日志」改成「检查历史」，
    空历史给提示卡，列表底部加一行「清空检查历史」（先弹确认再删，清空不可逆）。
    MainActivity 加 `displayName()`，从 SAF 取显示名当历史里的「目标」。
  - 版本 0.6.0/code7 → **0.7.0/code8**，commit `3ffed5b`。
  - 本机 8 条出包前置全过。三条知识：新增 `android/scan-history-data-source.md`（数据源选择 + 映射表 +
    两个必须知道的点）、`android/miuix-0.9.4.md` 补「查组件签名的三条路」、
    `tooling/github-push-and-local-proxy.md` 补「代理会被随时开关，先看端口有没有在听」。
  - 推送这一天折腾了三回，最后落到最省事的一条：**代理关掉后 DNS 又把 `github.com` 解到被墙 IP**，
    这时不用起隧道，`git -c http.curloptResolve=github.com:443:<当下通的IP> push` 一句就够
    （实测 4 秒推完）。前提是**推之前先 TCP 测一遍候选 IP**——它们隔十几分钟通断就会反过来。
    三条路的取舍已全部写进 `tooling/github-push-and-local-proxy.md`。

- 2026-09-28 **关于页：接系统返回键 + 搬上游 OS3 动态背景**。用户口径：「关于页的返回逻辑有问题，
  并且关于页不是有背景色吗，也抄过来」。
  - 返回键：上游归 navigation3 的路由栈管，本项目没有导航库 → `AboutScreen.kt` 补 `BackHandler`，
    否则在关于页按返回会直接退出 App。
  - 背景色：上游的 `BgEffectBackground` 是 **AGSL RuntimeShader 渐变**（不是毛玻璃）。抄的时候
    两个参数被写死成 `false`，而 `isRuntimeShaderSupported()` 为假时它整条 `return` → 背景完全没有。
    这次把 effect 包整包搬进来（`BgEffectBackground` / `BgEffectConfig` / `BgEffectPainter` /
    `BgEffectModifier` / `OS3BgFrag` / `DeviceType`）+ `ui/util/WindowSize.kt`；RuntimeShader 改从
    **`miuix-shader-android:0.9.4`** 取（minSdk 24；上游走 `miuix-blur`，那个坐标被 minSdk 33 卡死），
    门控保留上游口径 `isRuntimeShaderSupported() && SDK >= 35` → **只有 Android 15+ 画渐变，
    低版本退化成窗口底色**（不是升级了才有的效果，是上游本身这么门控）。
  - 顺带修一个真 bug：`MiuixTheme` 原先只套在 `ScannerShell` 里，关于页落到 `lightColorScheme()`
    默认值上（深色模式底色发白、拿到的 `surface` 也不对）→ 抽成 `MiuixAppTheme`，两个分支共用。
  - 版本 0.7.0/code8 → **0.8.0/code9**，commit `08c4cb4`；CI run `36365506193` 两个 job 全绿（已出包）；
    本机 8 条出包前置全过。
  - 两条新知识：`android/about-bg-effect-shader.md`、`android/miuix-theme-scope.md`；更新
    `android/sukisu-ui-port.md`（第三批 + 返回键那个例外）、`android/miuix-0.9.4.md`（miuix-shader 门槛）。

- 2026-09-29 **磨砂玻璃 + 液态玻璃落地；关于页返回手势/背景补齐；CI 签名改成固定钥匙**。
  用户口径：「关于页的系统返回手势有问题（返回直接退到桌面）/ 关于页的背景也没实现 / 模块检查 / 实现液态玻璃」+「磨砂玻璃你也没实现啊」，
  外加一句关键线索「我从最新的 action 里面下的」。
  - **先破案再动手**：三条症状（返回退桌面、背景没实现、模块检查没实现）**正好是 0.6.0/code7 的功能集**
    （BackHandler 与背景是 0.8.0 加的、模块检查是 0.7.0 加的）→ 不是功能缺失，是**装的不是新包**。
    查 CI：分支构建原来每次现生成一把**随机钥匙**签名，覆盖安装必然失败（只能先卸载），
    手机上留着的还是老版本。修法：仓库里放一把固定的公开 CI 钥匙 `ci-signing/mrs-ci.jks`，
    分支构建与 tag 走同一条取钥匙的路；产物改名成 `mrs-<version>-c<code>.apk`。
    → 新知识 `build/ci-signing-stable-key.md`。
  - **磨砂玻璃 + 液态玻璃**：接 `miuix-blur-android:0.9.4`（manifest 硬写 minSdk 33 → app manifest 里
    `tools:overrideLibrary` 放行，上游同款做法），整份换回上游的 `ui/util/BlurExt.kt` 与
    `ui/component/FloatingBottomBar.kt`，新搬 `ui/component/liquid/`（CombinedBackdrop / InnerShadow /
    Lens / Vibrancy）与 `ui/component/miuix/animation/InteractiveHighlight.kt`；
    主壳照上游 `MainScreen` 接线（`rememberBlurBackdrop` + `rememberLayerBackdrop` + 两层 `layerBackdrop`，
    底栏收 `backdrop`）；两个开关落进 `ui/util/DisplaySettings.kt`（SharedPreferences，默认都开），
    设置页补两行 `SwitchPreference`（模糊 / 液态玻璃）。门槛：磨砂要 API 31、液态玻璃折射要 API 33。
    → 新知识 `android/miuix-blur-and-liquid-glass.md`（同时**更正** `build/compile-sdk-and-aar-metadata.md`
    里「库的 minSdk 别用 overrideLibrary 硬过」那条，以及 `android/miuix-0.9.4.md` 里「miuix-blur 已删除」）。
  - **关于页**：返回手势从 `BackHandler` 换成 `PredictiveBackHandler` + 外层 `graphicsLayer` 跟手滑出
    （上游是 navigation3 路由栈在做同一件事）；背景门槛从上游的 `SDK >= 35` 降到 `isRuntimeShaderSupported()`
    （API 33，Android 13/14 也能看），更低版本由 `BgEffectBackground` 补一层**静态渐变**兜底
    （原来那里直接退化成纯 Box、什么都没有）。→ 新知识 `android/overlay-page-and-predictive-back.md`。
  - 版本 0.8.0/code9 → **0.9.0/code10**，commit `9cc8a86`；CI run `36490628242` 两个 job 全绿，
    产物 `mrs-apk-0.9.0-c10-9cc8a86`（1.37 MB）。本机 8 条出包前置全过。
  - **待用户在真机确认**：装之前**必须先卸载旧版**（签名换了），装完在主页看「应用版本」是不是 0.9.0；
    磨砂/液态玻璃在 Android 12 以下看不到属正常（API 门槛）。

  - **2026-09-29 追加（用户：把预测性返回手势加个开关）**：设置页补上游同名行
    `settings_enable_predictive_back`（文案照抄上游 zh-CN，图标 `Icons.AutoMirrored.Rounded.MenuOpen`），
    状态进 `DisplaySettings.enablePredictiveBack`（KEY `enable_predictive_back`），默认**开**。
    **没照上游那套机制**：上游是应用级的 —— `KernelSUApplication.onCreate` 在 API 34+ 用隐藏 API
    `ApplicationInfo#setEnableOnBackInvokedCallback`（配 `org.lsposed.hiddenapibypass`）翻平台标志、重启才生效、
    只影响平台自己的返回动画（那一行也因此只在 Android 14+ 出现）。本项目没引那层依赖，预测性返回也只有
    关于页一处 → 开关直接管那一处：开 = `PredictiveBackHandler`（跟手往右滑出）、关 = `BackHandler`
    （照常回上一页、页面不动画），`if/else` 二选一（**两条不能同时挂**，后注册的会赢）。
    同时 manifest 补 `android:enableOnBackInvokedCallback="true"`（配 `tools:targetApi="33"`）——
    **API 33/34 不加它收不到进度回调**，跟手动画根本出不来；API 35+ 平台默认开着、该属性被忽略。
    `display` 从 `ScannerShell` 提到 `ScannerScreen`（原先建在里面，关于页那一层读不到）。
    版本 **0.10.0/code11**，本机 8 条出包前置全过。
  - **待用户在真机确认（0.10.0）**：装之前先卸载旧版（签名换过，覆盖装不上）；主页核对「应用版本」是 0.10.0；
    设置页应有**四行**开关（模糊 / 液态玻璃 / 预测性返回手势 / 检查更新），关掉预测性返回后关于页返回不再跟手。
  - **2026-09-29 第三轮（用户：结果全进检查历史、主页只留三样、检测卡按风险红黄绿）**：
    ① 主页 `HomeMiuix.kt` 只剩三张卡：检查卡 / 应用版本 / 提交 BUG —— 逐条发现卡
    （`scanFindingsSection`、`ScanFindingCard`）与那张详细结论卡从主页删掉；
    ② 检查卡 `CheckCard` 跟 `ScanState` 走：待机 = 原来的「点此开始检测」行卡，检测中 = 转圈卡，
    失败 = `WarningCard`，出结果 = **整卡按最高风险着色**（高危红 `0xFFF8E2E2` / 暗 `0xFF310808`、
    中危黄 `0xFFFFF0DB` / 暗 `0xFF3E2F1B`、其余绿 `0xFFDFFAE4` / 暗 `0xFF1A3825`，绿那档取自上游），
    版式照上游主页「工作中」那张卡：`Row(height(IntrinsicSize.Min))` 撑高 + 左上大字结论与一行计数 +
    左下模块名 + 右下 110dp 大图标（`offset(27.dp, 31.dp)`，被卡片裁掉一角）；点它仍然 = 重新选包检测；
    ③ 结果明细全部进「检查历史」：`SulogEntry.extraDetail`（本项目自加字段）+ `ScanUi.kt` 的
    `scanRecordDetail`（发现逐条 + 提示 + 截断说明）+ `SulogDetailDialog` 里另起一段等宽文本。
    版本 **0.11.0/code12**，本机 8 条出包前置全过。
    → `knowledge/android/scan-history-data-source.md` 补了一节（结果明细的落点）。
  - **待用户在真机确认（0.11.0）**：主页应只剩三张卡；扫一个模块看结论卡是否按风险变红 / 黄 / 绿
    （大字 + 计数 + 左下模块名 + 右下大图标裁得好看不好看 —— 上游那套版式本项目还没在真机看过）；
    切到检查历史点条目，详情里应有「【发现明细】」逐条与「【提示】」。
  - **2026-09-29 第四轮（用户真机反馈：文字重合 / 历史按 SU 日志写法 / 卡片标题写等级）**：
    ① 结论卡**文字重叠**是我上一轮的锅：三块内容都写成 `Box(fillMaxSize)`，被塞进同一格互相压住。
    改成装饰图标层 `matchParentSize()`（不参与定尺寸）、内容层一个 `Column` 自己定高，模块名给
    图标留 96dp + 省略号。坑单独记成 `knowledge/android/compose-box-corner-layout.md`。
    ② 结论卡**标题**按最高等级直接写「高危 / 中危 / 低危」；只有一档都没有才写「未发现风险」。
    ③ 检查历史的条目详情改成 **SU 日志那套「key: value」逐行写法**（发现逐条：等级 / 规则 / 文件 / 说明；
    截断与提示也各占一行），列表条目卡本来就是移植来的 SU 日志卡，不动。
    版本 **0.11.1/code13**，本机 8 条出包前置全过。
  - **2026-09-29 第五轮（用户：结论卡标题只写「xx模块」）**：标题三条文案拆成 `scan_result_high` /
    `scan_result_medium` / `scan_result_low`（高危模块 / 中危模块 / 低危模块）；**一档发现都没有时也按
    最低那档写**（用户口径是「只写这三档」），`scan_no_findings` 仍留给那一行计数用。
    版本 **0.11.2/code14**，本机 8 条出包前置全过，commit `0becacd` 已推。
  - **2026-09-29 第六轮（用户：你改一下）**：结论卡标题保持三档，把零发现时那一行计数从「未发现风险」
    改成「未发现风险项」（标题「低危模块」+ 计数「未发现风险」看着重复），两处注释跟着改。
    版本 **0.11.3/code15**，本机 8 条出包前置全过；commit `811bca3` 后来与 0.11.4 一起推上去了。
  - **2026-09-29 第七轮（用户：右侧写点击查看详情）**：检查历史每条右侧的状态由「结论」改成
    「点击查看详情」（新增 `scan_entry_view_detail` 文案），结论本身挪进详情弹窗的 fields，
    映射层与文件头注释跟着改。版本 **0.11.4/code16**，本机 8 条出包前置全过，commit `488a6f2` 已推。
  - **2026-09-29 第八轮（用户：写，第二行写文件名）**：条目卡改成把「名称 / 路径 / 时间 / 标签」四样
    **显式写成四行**（`ScanEntryRows`；新增 `scan_entry_name/path/time/tags` 四条文案），第二行的「路径」
    放**文件名**（用户定的；SAF 拿不到真路径）；上游那套不写标签的版式原样保留成 `SulogEntryRows`。
    版本 **0.11.5/code17**，本机 8 条出包前置全过；commit `44c170d` **没推上去**（两次直连都失败，按规矩停手）。
    新知识 `android/compose-labeled-rows-alignment.md`（标签列别设固定宽度）。
  - **2026-09-30 第九轮（用户：预测性返回手势的开关应该控制所有的预测性返回手势）**：这是真 bug ——
    之前那个开关只切了关于页的 `PredictiveBackHandler` / `BackHandler` 两条路，系统那套返回动画与别处
    一概不归它管。照上游补成**应用级**：新增 `MrsApplication`（`onCreate` 里 API 34+ 用 `HiddenApiBypass`
    放行后反射调 `ApplicationInfo#setEnableOnBackInvokedCallback`，按 `DisplaySettings.enablePredictiveBack`
    翻平台标志）、新增依赖 `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`（只出 `.aar`，别猜 `.jar`）、
    manifest 挂 `android:name=".MrsApplication"`、设置页翻动开关时同步翻标志并 `recreate()`
    （上游 `ColorPaletteScreen` 同款）。版本 **0.11.6/code18**，本机 8 条出包前置全过
    （新依赖先联网灌进本机 Gradle 缓存，否则 `--offline` 会 FAILED）。commit `36e930a` 已推，CI run `36636011139` 两个 job 全绿（产物 `mrs-apk-0.11.6-c18-45abca4`）。新知识
    `android/predictive-back-app-level-flag.md`。
  - **待用户在真机确认（0.11.6）**：Android 14+ 上关掉「预测性返回手势」后，系统的返回动画（返回桌面时
    窗口跟手缩看）应该一起没了，开启时关于页仍是跟手滑出。注意翻开关会 `recreate()`，主页「刚扫完」的
    结论卡会被清回待机（结果已经在检查历史里）—— 照上游的代价，不是新 bug。API < 34 上平台标志不存在，
    开关只管 app 内那一处。
  - **待用户在真机确认（0.11.3）**：结论卡上大字等级、计数、模块名三行不重叠；模块名过长时是省略号且不进图标；
    卡片标题应是「高危模块 / 中危模块 / 低危模块」（一档发现都没有时也会写「低危模块」）；
    检查历史点条目，详情是「发现 1/12 / 等级: / 规则: / 文件: / 说明:」这样的逐行日志。

## Read now

- `knowledge/android/miuix-blur-and-liquid-glass.md` — 动磨砂/液态玻璃、加 blur/backdrop、或问「效果为什么看不到」时读
- `knowledge/android/overlay-page-and-predictive-back.md` — 加「盖在主壳上的整页」、或用户反馈「返回退到桌面 / 返回后结果丢了」时读
- `knowledge/build/ci-signing-stable-key.md` — 用户说「装了你给的最新包还是旧界面」时先读
- `knowledge/build/android-toolchain.md` — 宿主构建命令与本机工具链现状
- `knowledge/build/compile-sdk-and-aar-metadata.md` — 升 Compose/MiuiX 依赖、或 CI 报 AAR 元数据 / minSdk 门槛时读
- `knowledge/build/msvc-utf8-source.md`、`knowledge/build/posix-vs-win32-portability.md`
  — 动 C++ 源码前扫一眼，省一次编译失败、省一次「CI 红而本机绿」
- `knowledge/android/miuix-0.9.4.md` — **动界面（MiuiX 0.9.4）前必读**：组件签名是实拉 sources jar 核对过的
- `knowledge/android/compose-box-corner-layout.md` — 做「四角 + 溢出大图标」那种卡片版式、或卡片里文字重合时读
- `knowledge/android/scan-history-data-source.md` — 要动「检查历史」这一页、或给功能找本地持久化方案时读
- `knowledge/android/predictive-back-app-level-flag.md` — 动「预测性返回手势」开关、给别处返回接预测性返回、或要在本项目里反射调隐藏 API 时读
- `knowledge/android/sukisu-ui-port.md` — 要**继续移植 SukiSU 的界面**、或给本项目加新界面时必读
- `knowledge/android/about-bg-effect-shader.md` — 动关于页背景、移植 `ui/component/miuix/effect/*`、或判断某个效果这个版本画不画得出来时读
- `knowledge/android/miuix-theme-scope.md` — 加「不经过主壳的页面/弹层」，或某页深色下发白时读
- `knowledge/android/miuix-0.8.8.md` — 0.8.8 时期的历史记录（日常改界面看 0.9.4 那份）
- `knowledge/detection/rule-design.md` — 动检测规则前必读（误报是核心指标）
- `knowledge/android/sukisu-ultra-as-ui-reference.md` — 要照搬/参考别的 App 的界面（尤其底栏、图标混用）前必读
- `knowledge/tooling/which-hosts-are-reachable.md` — 要拉外网内容（图标 SVG / 源码 / Maven jar）时读
- `knowledge/tooling/file-edit-anchors-and-newlines.md` — 用脚本改文件（尤其批量替换）之前扫一眼
- `knowledge/tooling/here-string-package-line.md` — 用 here-string / 脚本往仓库里写 .kt 之前必读
  （丢了换行会把 `package` 粘进注释，症状像编译器缓存坏了）
- `knowledge/tooling/github-push-and-local-proxy.md` — **git push 连不上 github.com 时先读这条**（多半是本机 7890 系统代理）
- `knowledge/build/release-and-update-check.md` — 动版本号、发版、或改「检查更新」前必读
- `knowledge/detection/dry-run-sandbox.md` — 要评估动态/半动态分析（执行轨迹）时读，
  含「假 PATH 沙箱」的坑与「为什么进不了 APK」的结论

## Read if

- 要加检测规则 → 先读 `knowledge/detection/rule-design.md`，再看 `core/src/rules.cpp`
  的特征表格式（`id/sev/needle/detail`）与 `core/src/escalate.cpp` 的升级表
- 脚本断言「CI 全过、本机全红」→ 读 `knowledge/tooling/powershell-chinese-encoding-traps.md`
- 需要 Android 模块规范细节（module.prop、脚本钩子、system 覆盖方式）→
  先建 `knowledge/android/` 下的条目再读
- `git push` 连接被重置 / 连不上 github.com → 读 `knowledge/tooling/github-push-and-local-proxy.md`
- 要改界面（MiuiX/Compose）→ 先读 `knowledge/tooling/impeccable-on-android-project.md`
  （impeccable 在本项目的调法）与 `knowledge/android/miuix-0.9.4.md`（组件签名与硬约束）
