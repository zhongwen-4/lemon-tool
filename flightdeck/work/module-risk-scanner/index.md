# module-risk-scanner

状态：进行中。已定：Android 设备端、C++ 核心 + MiuiX(Compose) 界面的 APK、只检查未安装的模块包。

## 已定决策

- 2026-09-20 运行位置：在 Android 设备上跑。
- 2026-09-20 技术栈：C++ 核心（CMake）+ Kotlin/Compose 外层，UI 用 MiuiX 组件库。
- 2026-09-20 界面：`top.yukonga.miuix.kmp:miuix-android:0.8.8`（见
  `knowledge/android/miuix-0.8.8.md`）。**它硬要求 compileSdk 36**，compileSdk/targetSdk 已跟到 36。
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


## 未验证 / 风险

- APK 构建路径本机跑不了（没有 NDK、也没有 gradle），只能靠 CI 验证。
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

10. ~~等用户挑「主页」图标~~ **方向已变**（2026-09-25）：用户改成「整个 UI 抄 SukiSU Ultra 的代码，
    图标用 Tabler Icons」。核实后有三条硬事实（`knowledge/android/sukisu-ultra-as-ui-reference.md`）：
    它的代码是 **GPL-3.0**、启动图标另有一份**禁止他用**的许可，而本仓库**没有 LICENSE** →
    照搬代码不可行；它的 `ui/` 就有 281 个 .kt / 2.0 MB，与本 App 两个页面完全不对等；
    它底栏实际用的是 `material-icons-extended` 的 `Icons.Rounded.Cottage/Security/Extension/Settings`
    （用户想要的「房子」就是 `Cottage`）。**已把问题清单交回用户，等他答复，答复前不动代码**：
    抄代码还是抄观感 / 底栏要不要扩成四个 tab / 用 `material-icons-extended` 还是坚持 Tabler /
    要不要浮动模糊底栏（MiuiX 0.8.8 无 blur） / 要不要连带升工具链。

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

## Read now

- `knowledge/build/android-toolchain.md` — 宿主构建命令与本机工具链现状
- `knowledge/build/msvc-utf8-source.md`、`knowledge/build/posix-vs-win32-portability.md`
  — 动 C++ 源码前扫一眼，省一次编译失败、省一次「CI 红而本机绿」
- `knowledge/android/miuix-0.8.8.md` — 动界面（MiuiX/Compose）前必读
- `knowledge/detection/rule-design.md` — 动检测规则前必读（误报是核心指标）
- `knowledge/android/sukisu-ultra-as-ui-reference.md` — 要照搬/参考别的 App 的界面（尤其底栏、图标混用）前必读
- `knowledge/tooling/which-hosts-are-reachable.md` — 要拉外网内容（图标 SVG / 源码 / Maven jar）时读
- `knowledge/build/release-and-update-check.md` — 动版本号、发版、或改「检查更新」前必读
- `knowledge/detection/dry-run-sandbox.md` — 要评估动态/半动态分析（执行轨迹）时读，
  含「假 PATH 沙箱」的坑与「为什么进不了 APK」的结论

## Read if

- 要加检测规则 → 先读 `knowledge/detection/rule-design.md`，再看 `core/src/rules.cpp`
  的特征表格式（`id/sev/needle/detail`）与 `core/src/escalate.cpp` 的升级表
- 脚本断言「CI 全过、本机全红」→ 读 `knowledge/tooling/powershell-chinese-encoding-traps.md`
- 需要 Android 模块规范细节（module.prop、脚本钩子、system 覆盖方式）→
  先建 `knowledge/android/` 下的条目再读
- 要改界面（MiuiX/Compose）→ 先读 `knowledge/tooling/impeccable-on-android-project.md`
  （impeccable 在本项目的调法）与 `knowledge/android/miuix-0.8.8.md`（组件签名与硬约束）
