# 移植自 SukiSU Ultra 的文件：改动清单

本文件集中登记**所有移植自 SukiSU Ultra 的文件**的改动说明（GPL-3.0 §5a 要求的「显著改动声明」）。
每个移植文件的头部只留「出处（上游路径）」+ 一行指向本文件对应小节的链接，改动明细全部在这里。

- 上游：SukiSU Ultra 的 `manager/` 模块，GPL-3.0；本仓库根目录 `LICENSE` 同为 GPL-3.0。
- 新增一个移植文件时：本文件补一节（小节标题 = **文件名去掉扩展名**，就是链接锚点），
  并在该文件头部写一行 `改动清单：docs/sukisu-port-changes.md#<锚点>`。
- 锚点写法：标题小写。例：`## SulogListMiuix` -> `#suloglistmiuix`。
- 只改包名这类一句话改动也登记，图的是「一眼看全」。

---

## 目录

- [MrsApplication](#mrsapplication) — `app/src/main/java/com/lemon/mrs/MrsApplication.kt`
- [WarningLevel](#warninglevel) — `app/src/main/java/com/lemon/mrs/ui/component/WarningLevel.kt`
- [Theme](#theme) — `app/src/main/java/com/lemon/mrs/ui/theme/Theme.kt`
- [UninstallDialog](#uninstalldialog) — `app/src/main/java/com/lemon/mrs/ui/component/uninstalldialog/UninstallDialog.kt`
- [SulogUiState](#suloguistate) — `app/src/main/java/com/lemon/mrs/ui/screen/sulog/SulogUiState.kt`
- [BlurExt](#blurext) — `app/src/main/java/com/lemon/mrs/ui/util/BlurExt.kt`
- [SulogListMiuix](#suloglistmiuix) — `app/src/main/java/com/lemon/mrs/ui/screen/sulog/SulogListMiuix.kt`
- [WindowSize](#windowsize) — `app/src/main/java/com/lemon/mrs/ui/util/WindowSize.kt`
- [StatusTagMiuix](#statustagmiuix) — `app/src/main/java/com/lemon/mrs/ui/component/statustag/StatusTagMiuix.kt`
- [StatusTag](#statustag) — `app/src/main/java/com/lemon/mrs/ui/component/statustag/StatusTag.kt`
- [LocaleHelper](#localehelper) — `app/src/main/java/com/lemon/mrs/ui/util/LocaleHelper.kt`
- [SulogModels](#sulogmodels) — `app/src/main/java/com/lemon/mrs/ui/util/sulog/SulogModels.kt`
- [LatestVersionInfo](#latestversioninfo) — `app/src/main/java/com/lemon/mrs/ui/util/module/LatestVersionInfo.kt`
- [KsuIsValid](#ksuisvalid) — `app/src/main/java/com/lemon/mrs/ui/component/KsuIsValid.kt`
- [HomeMiuix](#homemiuix) — `app/src/main/java/com/lemon/mrs/ui/screen/home/HomeMiuix.kt`
- [SettingsMiuix](#settingsmiuix) — `app/src/main/java/com/lemon/mrs/ui/screen/settings/SettingsMiuix.kt`
- [HomeUtils](#homeutils) — `app/src/main/java/com/lemon/mrs/ui/screen/home/HomeUtils.kt`
- [FloatingBottomBar](#floatingbottombar) — `app/src/main/java/com/lemon/mrs/ui/component/FloatingBottomBar.kt`
- [AboutUiState](#aboutuistate) — `app/src/main/java/com/lemon/mrs/ui/screen/about/AboutUiState.kt`
- [SettingsUiState](#settingsuistate) — `app/src/main/java/com/lemon/mrs/ui/screen/settings/SettingsUiState.kt`
- [MainPagerState](#mainpagerstate) — `app/src/main/java/com/lemon/mrs/ui/component/bottombar/MainPagerState.kt`
- [HomeUiState](#homeuistate) — `app/src/main/java/com/lemon/mrs/ui/screen/home/HomeUiState.kt`
- [AboutMiuix](#aboutmiuix) — `app/src/main/java/com/lemon/mrs/ui/screen/about/AboutMiuix.kt`
- [AboutUtils](#aboututils) — `app/src/main/java/com/lemon/mrs/ui/screen/about/AboutUtils.kt`
- [ConfirmDialog](#confirmdialog) — `app/src/main/java/com/lemon/mrs/ui/component/dialog/ConfirmDialog.kt`
- [AboutScreen](#aboutscreen) — `app/src/main/java/com/lemon/mrs/ui/screen/about/AboutScreen.kt`
- [LoadingDialog](#loadingdialog) — `app/src/main/java/com/lemon/mrs/ui/component/dialog/LoadingDialog.kt`
- [InteractiveHighlight](#interactivehighlight) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/animation/InteractiveHighlight.kt`
- [DampedDragAnimation](#dampeddraganimation) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/animation/DampedDragAnimation.kt`
- [RebootListPopupMiuix](#rebootlistpopupmiuix) — `app/src/main/java/com/lemon/mrs/ui/component/rebootlistpopup/RebootListPopupMiuix.kt`
- [WarningCard](#warningcard) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/WarningCard.kt`
- [CombinedBackdrop](#combinedbackdrop) — `app/src/main/java/com/lemon/mrs/ui/component/liquid/CombinedBackdrop.kt`
- [SendLogDialog](#sendlogdialog) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/SendLogDialog.kt`
- [InnerShadow](#innershadow) — `app/src/main/java/com/lemon/mrs/ui/component/liquid/InnerShadow.kt`
- [Lens](#lens) — `app/src/main/java/com/lemon/mrs/ui/component/liquid/Lens.kt`
- [Vibrancy](#vibrancy) — `app/src/main/java/com/lemon/mrs/ui/component/liquid/Vibrancy.kt`
- [DragGestureInspector](#draggestureinspector) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/modifier/DragGestureInspector.kt`
- [BgEffectConfig](#bgeffectconfig) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectConfig.kt`
- [BgEffectBackground](#bgeffectbackground) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectBackground.kt`
- [BgEffectModifier](#bgeffectmodifier) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectModifier.kt`
- [BgEffectPainter](#bgeffectpainter) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectPainter.kt`
- [OS3BgFrag](#os3bgfrag) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/OS3BgFrag.kt`
- [DeviceType](#devicetype) — `app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/DeviceType.kt`

---

## MrsApplication

- 本文件：`app/src/main/java/com/lemon/mrs/MrsApplication.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/KernelSUApplication.kt`

改动（改动日期：2026-09-30）：
  ① 只搬「预测性返回的平台标志」这一件事。上游这个类还管 OkHttp 缓存、SuperUserViewModel 预热、
     webroot、TMPDIR 等，本项目一样都不需要（没有内核那一侧的东西）。
  ② 上游在 onCreate 里按存下来的开关翻标志；本项目同一个口径，开关值从 DisplaySettings 读。
  ③ 上游的类名叫 KernelSUApplication，本项目叫 MrsApplication。

---

## WarningLevel

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/WarningLevel.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/WarningLevel.kt`

改动：只改包名（改动日期：2026-09-28）

---

## Theme

- 本文件：`app/src/main/java/com/lemon/mrs/ui/theme/Theme.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/theme/Theme.kt`

改动（改动日期：2026-09-28）：只取本项目真用得到的三个 CompositionLocal——LocalEnableBlur /
  LocalEnableFloatingBottomBar / LocalEnableFloatingBottomBarBlur（上游还带 LocalColorMode、
  LocalEnableNavigationBadge、LocalModuleDescriptionMaxLines 等，本项目没有那些开关）；
  isInDarkTheme 仍按「跟随系统」实现（上游读它自己的 LocalColorMode）。

---

## UninstallDialog

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/uninstalldialog/UninstallDialog.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/uninstalldialog/UninstallDialog.kt`

改动（改动日期：2026-09-28）：上游这个是 LKM 模式下「卸载内核模块」的确认弹窗，本项目没有这个功能，
  只保留同名同签名的空壳，设置页的调用点与上游一致。

---

## SulogUiState

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/sulog/SulogUiState.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogUiState.kt`

改动（改动日期：2026-09-28）：
  ① 删掉 SulogActions.onBack（本项目的日志列表是 Tab 页，没有返回栈）与 SulogFileSelector
     （日志文件下拉不要），其余字段与上游一字不差。
  ② 每个字段都给了空默认值，SulogScreenState() 就是一个「数据空着」的骨架状态；
     每个 action 都给了空实现，接真实数据时再传进来。

---

## BlurExt

- 本文件：`app/src/main/java/com/lemon/mrs/ui/util/BlurExt.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/util/BlurExt.kt`

改动（改动日期：2026-09-28）：① 只改包名。
       ② 之前这里是不搬毛玻璃的占位实现（rememberBlurBackdrop 恒为 null、BlurredBar 退化纯色）；
       现在接上 miuix-blur 0.9.4 换回上游原文。miuix-blur 的 manifest 硬写 minSdk 33，
       靠 app 的 AndroidManifest 里 tools:overrideLibrary 放行，低版本由 isRenderEffectSupported() 门控。

---

## SulogListMiuix

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/sulog/SulogListMiuix.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogMiuix.kt`、`manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogScreen.kt`

改动（改动日期：2026-09-28）：
  ① 只搬「SU 日志列表」这一块：列表段 sulogEntriesSection、条目卡 SulogEntryCard、
     条目详情弹窗 SulogDetailDialog、空/错提示卡 SulogMessageCard，以及取条目标题 /
     描述 / 标签 / 详情文本 / 返回值的几个小函数。
  ② 上游那一页的顶栏（返回、清空日志、按类型筛选）、搜索框 SearchBox / SearchPager /
     SearchBarFake、日志文件下拉 OverlayDropdownPreference、下拉刷新 PullToRefresh、
     SulogStatusSection 状态提示卡，以及一切毛玻璃（rememberBlurBackdrop / BlurredBar /
     layerBackdrop）都不要——用户要求「其他的组件不要」。
  ③ 页面外壳改成项目自己的 PageScaffold（顶栏 + 一条 LazyColumn + 给悬浮底栏留白），
     跟主页 / 设置页同一套。
  ④ 列表渲染 state.entries：搜索与筛选这一路没搬，SulogScreenState.visibleEntries
     在本项目里没有生产者。往后接真实日志时填 entries 即可。
  ⑤ 条目卡尾部那个箭头：上游用 MiuixIcons.Basic.ArrowRight，但本项目锁的
     miuix-icons 0.9.4 只有 top.yukonga.miuix.kmp.icon.extended 一个包（没有 basic），
     这里改用同语义的 MiuixIcons.ChevronForward。
  ⑥ 2026-09-28 接上「检查历史」：本项目的历史是扫描记录、不是 SU 日志，所以
     ① 顶栏标题换成「检查历史」（R.string.scan_history，不再叫「SU 日志」）；
     ② 条目类型加了 SulogEventType.ScanReport 一支，四个取值小函数各补一个分支
        （模块名 / 扫描对象 / 高·中·低计数 / 点击查看详情）；sulogEntrySummaryTags 因此改成 @Composable
        （标签要读 string 资源）；
     ③ 空历史给一张提示卡；列表非空时底部加一行「清空检查历史」——点了先弹确认，
        确认后才走 actions.onCleanFile（清空是不可逆的，不做静默删除）。
  ⑦ 2026-09-29 用户口径：「检查模块的结果全部放进检查历史」——条目详情里除了 fields 那几行，
     再加一段正文（发现逐条 + 提示，ScanUi.kt 的 scanRecordDetail 生成、走 SulogEntry.scanDetail ——
     ⑩ 之后那段正文改成列表渲染：extraDetail 这个字符串字段已换成结构化的 scanDetail），
     主页那边只留一张按风险着色的结论卡。
  ⑧ 2026-09-29：检查条目的右侧状态由「结论」改成「点击查看详情」——结论本身挪进详情弹窗，
     卡片右侧只当点击提示（用户要求「右侧写点击查看详情」）。
  ⑨ 2026-09-29：检查条目的卡片改成把「名称 / 路径 / 时间 / 标签」四样显式写出来（见 ScanEntryRows），
     第二行的「路径」放文件名；上游那套不写标签的版式原样保留成 SulogEntryRows，给将来别的日志类型用。
  ⑩ 2026-09-30 用户要「把检查历史卡片的详情改为列表，同样是 SU 日志的样式」：详情弹窗里
     检查条目那段等宽的「键: 值」正文换成**列表**（ScanDetailList）—— 概览一张 SU 日志式的卡、
     每条发现一张卡（规则 / 文件 / 说明 / 等级标签）、每条提示一张卡；数据走 SulogEntry.scanDetail
     （结构化的，不再是一段拼好的文本）。上游那种日志条目的详情仍是一段等宽正文，照上游不动。
  ⑫ 2026-10-01 用户要「每个检查详情最上面写一行红色的卡片：该结果仅供参考」——详情整页的内容最上面
     加一张提示卡：**白底**（复用 `DetailCard` 的默认卡色，与其它卡同一套）+ **红字**
     （0xFFF72727，与主页结论卡、详情等级 chip 同一个红），文案 `scan_detail_disclaimer`。
     用户当天追加口径「卡片是白的，字是红的」—— 上游 `WarningCard` 那种红底红字他不要，所以没用它。
     卡片下面才是概览 / 每条发现 / 每条提示那一列卡。
  ⑬ 2026-10-01 用户口径：那张提示卡的文字**居中**（fillMaxWidth() + TextAlign.Center）。
  ⑪ 2026-09-30 用户口径：「检查历史的列表改回去，单个卡片点进去进入另一个列表」——
     ① 条目卡退回上一版（0.11.4）的版式：不管什么类型都走 SulogEntryRows（上游那套不写标签的
        标题 / 描述 / 时间 / 标签 chips），0.11.5 那套显式四行「名称 / 路径 / 时间 / 标签」整段删掉
        （ScanEntryRows 已删；详情的概览卡 / 发现卡里那几行「标签 + 值」仍走 ScanEntryLine / ScanEntryLabel）；
     ② 条目详情由弹窗改成**整页** SulogDetailScreen（骨架照关于页：SmallTopAppBar + 返回箭头 + 可滚内容），
        由 ScanUi.ScannerShell 盖在主壳上、跟手往右滑，返回手势与关于页同一套口径（受「预测性返回手势」开关管）。

---

## WindowSize

- 本文件：`app/src/main/java/com/lemon/mrs/ui/util/WindowSize.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/util/WindowSize.kt`

改动：只改包名（改动日期：2026-09-28）。上游背景效果用它决定手机 / 平板两套配色。

---

## StatusTagMiuix

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/statustag/StatusTagMiuix.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/statustag/StatusTagMiuix.kt`

改动：只改包名（改动日期：2026-09-28）

---

## StatusTag

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/statustag/StatusTag.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/statustag/StatusTag.kt`

改动：上游按 LocalUiMode 在 Miuix / Material 两套间切换；本项目只做 Miuix 一套，直接调 StatusTagMiuix。
改动日期：2026-09-28

---

## LocaleHelper

- 本文件：`app/src/main/java/com/lemon/mrs/ui/util/LocaleHelper.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/util/LocaleHelper.kt`

改动（改动日期：2026-09-28）：
  ① 只留设置页用到的那三个东西：SYSTEM / SUPPORTED_TAGS / displayName。
     上游的 persistLanguage / loadLanguage / applyLanguage / 重启 Activity 那一套不搬。
  ② SUPPORTED_TAGS 先给空表：本项目现在只有默认资源 + 简体中文，还没做语言切换，
     所以设置页「语言」那行下拉是空的（数据先空着，往后接）。

---

## SulogModels

- 本文件：`app/src/main/java/com/lemon/mrs/ui/util/sulog/SulogModels.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/util/SulogHelper.kt`

改动（改动日期：2026-09-28）：
  ① 只留数据形状 SulogFile / SulogEventType / SulogEventFilter / SulogEntry 与过滤器默认值。
     上游同文件里的 listSulogFiles / readSulogFile / parseSulogLines / parseSulogLine /
     cleanSulogFile / deleteSulogFile 都要读 /data/adb/ksu/log，本项目没有这个数据源，不搬。
  ② 2026-09-28 补一个 ScanReport：本项目的历史是「自己每次检查模块留下的记录」，
     没有 SU 日志，所以给这个列表加一个自己的事件类型，让条目卡能显示模块名 / 对象路径 /
     高·中·低危计数 / 结论（映射在 ScanUi.kt 的 scanRecordToSulogEntry，展示在 SulogListMiuix.kt）。
  ③ 2026-09-30 用户要「详情改成列表、同样是 SU 日志的样式」：明细正文由一段拼好的文本改成
     结构化数据 ScanDetail（发现逐条 + 提示 + 截断位），SulogEntry.extraDetail 随之换成 scanDetail。

---

## LatestVersionInfo

- 本文件：`app/src/main/java/com/lemon/mrs/ui/util/module/LatestVersionInfo.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/util/module/LatestVersionInfo.kt`

改动：只改包名（改动日期：2026-09-28）

---

## KsuIsValid

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/KsuIsValid.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/KsuValidCheck.kt`

改动（改动日期：2026-09-28）：上游这里要问内核（Natives.isManager / Natives.version）才决定是否渲染，
  本项目没有这套内核取数，所以直接渲染 content。留着同名同签名，设置页的调用点就能跟上游一字不差。

---

## HomeMiuix

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/home/HomeMiuix.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeMiuix.kt`

改动（改动日期：2026-09-28）：
  ① 只改包名与 import；Scaffold / TopAppBar / LazyColumn / UpdateCard 的结构、尺寸、配色与上游一字不差。
  ② 数据先空着：HomeUiState 全默认（无内核信息、无更新信息），相应的卡片渲染出来是空的。
  ③ 去内核依赖：Natives.isFullFeatured() 的底边距判断改成直接用 bottomInnerPadding；
     Natives / KernelVersion 的取数、Preview 相关的 import 与预览块删除。
  ④ 毛玻璃接上了（2026-09-28 更新）：LayerBackdrop 用 miuix-blur 的真类型，
     backdrop 由 ui/util/BlurExt.kt 提供，顶栏跟着滚动内容一起磨砂。
  ⑤ 按用户口径精简主页，组件骨架保留、只改内容：
     - StatusCard（上游的「工作中 / 内核支持 / 不支持」三分支）改名 CheckEntryCard 且只留一支：
       标题「不支持」换成「点此开始检测」，动作仍是选模块 zip；另两支是 KernelSU 内核取数，本项目没有数据源。
     - InfoCard 只留一行「应用版本」（版本号从本机 PackageManager 取），上游那一堆内核/设备信息
       与 SELinux + Seccomp 两张信息卡全删。
     - SupportLinks（支持开发 / 了解 KernelSU）换成一行「提交 BUG」，指向本项目 issues。
  ⑥ 2026-09-28 接上「检查模块」：主页加一张结论卡与逐条发现卡（扫描在 ScanUi.kt 里跑，
     这里只负责把 ScanState 画出来）。
  ⑦ 2026-09-29 用户口径变了：**结果全部进「检查历史」**，主页只留一张检查卡 ——
     待机是「点此开始检测」，出结果后按最高风险整卡着色（高危红 / 中危黄 / 其余绿；
     版式照上游主页那张「工作中」卡：左上大字结论 + 一行计数、左下模块名、右下 110dp 大图标），
     逐条发现卡与那张详细结论卡都从主页删掉（明细在检查历史的详情里看）。

---

## SettingsMiuix

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/settings/SettingsMiuix.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/settings/SettingsMiuix.kt`

改动（改动日期：2026-09-28）：
  ① 上游那一整页 root 管理器的开关与子页面（SuSFS / KPM / LKM / ADB root / su compat / 卸载内核模块 /
     主题 / Profile 模板 / 工具 / 界面模式 / 语言）全部删掉：本项目只检查模块包，没有这些数据源。
     只留两行——「检查更新」（SwitchPreference）与「关于」（ArrowPreference）。
  ② 「检查更新」的开关状态本页自持（rememberSaveable，默认开）。用户要求数据先空着，
     等接上真实的自动检查逻辑，再把这行换成本页外的状态。
  ③ 顶栏 + 一条 LazyColumn 的外壳与上游一致；毛玻璃已接上（ui/util/BlurExt.kt 换成上游原文）。
  ④ 2026-09-28 用户要「磨砂玻璃 / 液态玻璃」两个效果，补两行 SwitchPreference（上游同名开关），
     状态放进 DisplaySettings（SharedPreferences），主壳读它决定顶栏毛玻璃与底栏液态玻璃。
  ⑤ 2026-09-29「预测性返回手势」开关（上游同名行），2026-09-30 按用户口径改成**应用级**：
     翻动时除了存开关，还要照上游 `ColorPaletteScreen` 那套翻平台的预测性返回标志并 `recreate()`，
     这样它管的是所有预测性返回手势，不只是关于页那一处。
  ⑥ 2026-10-01 用户要「风险检查完直接跳到该模块的检查历史的详情页」并「加个开关」：
     补一行「检查完自动查看详情」，状态同样进 DisplaySettings（默认开）；主壳扫描成功后读它
     决定要不要把详情整页直接盖上来。

---

## HomeUtils

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/home/HomeUtils.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeUtils.kt`

改动（改动日期：2026-09-28）：
  ① 只留数据形状 ManagerVersion / SystemInfo 与 getManagerVersion（版本号从 PackageManager 读，
     不依赖 root）。上游同文件里的 getZygiskImplementation / rememberSusfsInfo / rememberHookTypeLabel
     全是内核取数，本项目没有数据源，不搬。
  ② SystemInfo 各字段补了默认值，这样主页骨架可以先建成「数据空着」的状态。

---

## FloatingBottomBar

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/FloatingBottomBar.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/FloatingBottomBar.kt`

改动（改动日期：2026-09-28）：① 只改包名（com.sukisu.ultra -> com.lemon.mrs）与 isInDarkTheme 的 import。
       ② 之前那一版把毛玻璃 / 液态玻璃那一路整个删了（当时 miuix-blur 的 minSdk 33 卡住），
       现在依赖接上了，整份换回上游原文——drawBackdrop / lens / vibrancy / innerShadow / 倾斜高光全在。

---

## AboutUiState

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/about/AboutUiState.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutUiState.kt`

改动：只改包名。
改动日期：2026-09-28

---

## SettingsUiState

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/settings/SettingsUiState.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/settings/SettingsUiState.kt`

改动（改动日期：2026-09-28）：
  ① 上游的 colorStyle / colorSpec 用的是 com.materialkolor 的 PaletteStyle / ColorSpec 枚举名，
     本项目没引 materialkolor，这里改成同名字符串（PaletteStyle.TonalSpot.name / ColorSpec.SpecVersion.SPEC_2025.name）。
  ② SettingsScreenActions 的每个回调都补了空实现，这样设置页可以先用 SettingsUiState() +
     SettingsScreenActions() 建成「组件骨架、数据空着」的样子。

---

## MainPagerState

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/bottombar/MainPagerState.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/bottombar/BottomBar.kt`

改动：① 去掉 UiMode、角标、毛玻璃那些部分；② 翻页动画改用 Compose 自带的 animateScrollToPage
      （上游用的是 miuix 的 springAnimateToPage）；③ LocalMainPagerState 从 MainActivity 挪到这里。
      其余与上游一致。

改动日期：2026-09-27。

---

## HomeUiState

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/home/HomeUiState.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeUiState.kt`

改动：只改包名/import，并给每个字段补默认值——主页这一轮只留组件骨架、数据先空着，
      所以 HomeUiState() 就能建出一个空状态；派生属性（showGkiWarning / hasUpdate 等）与上游一字不差，
      往后接真实内核信息时直接赋值即可。改动日期：2026-09-28。

---

## AboutMiuix

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/about/AboutMiuix.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutMiuix.kt`

改动（改动日期：2026-09-28）：
  ① 只改包名与 import；版式、滚动视差、Logo 淡出/缩放、LazyColumn 结构、Card +
     ArrowPreference 链接行全部与上游一致。
  ② 关于页自己这一层不套毛玻璃（2026-09-28 仍是这样）：上游在 enableBlur 为真时给 Logo、
     应用名和链接卡片套 textureBlur（还带 logoBlend / blendColors 两套混色表），
     这些分支连同 BlurColors / BlendColorEntry / BlurBlendMode / rememberLayerBackdrop
     / layerBackdrop 的引用一并删除——纯色路径与上游非毛玻璃时完全一样。
     （顶栏 / 底栏的毛玻璃与液态玻璃在别处，见 ui/util/BlurExt.kt 与 ui/component/FloatingBottomBar.kt。）
  ③ 动态背景的门槛：上游是 `isRuntimeShaderSupported() && enableBlur && SDK >= 35`（Android 15+ 才画）；
     2026-09-28 用户反馈「关于页的背景也没实现」，门槛降到 RuntimeShader 自己的要求（API 33），
     Android 13 / 14 也画；更低版本由 BgEffectBackground 的静态渐变兜底。
     `bgModifier` 保持 `Modifier`（上游那里传的是 layerBackdrop，本项目不搬）。

---

## AboutUtils

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/about/AboutUtils.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutUtils.kt`

改动：只改包名。
改动日期：2026-09-28

---

## ConfirmDialog

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/dialog/ConfirmDialog.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/dialog/Dialog.kt`

改动：上游的 rememberConfirmDialog 是一整套带 markdown 渲染的确认弹窗（Dialog.kt + DialogMiuix.kt
      + MarkdownContent，共约 17 KB）。本次只照搬页面骨架、数据先空着，所以这里先给一个同签名的
      空实现：调用点（HomeMiuix 的 UpdateCard）保持与上游一致，弹窗以后接。
改动日期：2026-09-28

---

## AboutScreen

- 本文件：`app/src/main/java/com/lemon/mrs/ui/screen/about/AboutScreen.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutScreen.kt`

改动（改动日期：2026-09-28；2026-09-29 追加 ⑤）：
  ① 上游用 navigation3 的 Navigator.push/pop 进这条路由；本项目没有那套导航，改成上层传 onBack。
  ② 上游的 BuildConfig.VERSION_NAME、R.string.about_source_code 文案（含它自己的图标许可说明）
     换成我们的：版本号从 PackageManager 读，链接指向本仓库。
  ③ 其余（state/actions 的构造方式、extractLinks 的用法）与上游一致。
  ④ 系统返回手势：上游由 navigation3 的路由栈接管（返回时整页跟着手指走）。本项目没有导航库，
     这里用 PredictiveBackHandler 自己接——进度通过 onBackProgress 喂给上层，让关于页跟着手势
     往右滑出去，手势取消就弹回原位，松手完成才真的关（API < 34 上它等价于普通返回键，同样回调 onBack）。
     0.8.0 里用的是 BackHandler：手势一样能回上一页，只是页面不会跟着手指走。
  ⑤ 2026-09-29 用户要「把预测性返回手势加个开关」：开关值（DisplaySettings.enablePredictiveBack）由上层传进来，
     开 = ④ 的 PredictiveBackHandler（跟手滑出），关 = BackHandler（照常回上一页，页面不动画）。
     **两条路只能注册一条**：同一层两个返回处理都挂上时后注册的那个会赢，跟手动画会失效。

---

## LoadingDialog

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/dialog/LoadingDialog.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/dialog/Dialog.kt`

改动（改动日期：2026-09-28）：上游的 loading 弹窗是整套 Dialog 框架（DialogHandleBase / OverlayDialog 等）里的一环，
  本项目暂时只搬骨架、不搬弹窗框架。这里保留同名的句柄类型与 rememberLoadingDialog，
  句柄照常能 withLoading / showLoading，只是当前不弹任何东西；往后接真弹窗时换掉实现即可。

---

## InteractiveHighlight

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/animation/InteractiveHighlight.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/animation/InteractiveHighlight.kt`

改动（改动日期：2026-09-28）：只改包名（com.sukisu.ultra -> com.lemon.mrs），逻辑一字未动。

---

## DampedDragAnimation

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/animation/DampedDragAnimation.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/animation/DampedDragAnimation.kt`

改动：只有包名与 inspectDragGestures 的导入路径（com.sukisu.ultra → com.lemon.mrs）。代码与上游逐字相同。

改动日期：2026-09-27。

---

## RebootListPopupMiuix

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/rebootlistpopup/RebootListPopupMiuix.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/rebootlistpopup/RebootListPopupMiuix.kt`

改动：上游那个是「重启 / 重启到恢复模式…」的内核动作弹窗（依赖 root 与内核接口）。
      本次只照搬页面骨架、数据先空着，所以先留一个同签名的空壳：顶栏上占位，后面接。
改动日期：2026-09-28

---

## WarningCard

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/WarningCard.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/WarningCard.kt`

改动：只改包名（改动日期：2026-09-28）

---

## CombinedBackdrop

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/liquid/CombinedBackdrop.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/liquid/CombinedBackdrop.kt`

改动（改动日期：2026-09-28）：只改包名（com.sukisu.ultra -> com.lemon.mrs），逻辑一字未动。

---

## SendLogDialog

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/SendLogDialog.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/SendLogDialog.kt`

改动（改动日期：2026-09-28）：上游这里要收集日志文件、弹列表让你选发给谁，本项目还没有这套东西，
  只保留同名同签名的空壳，设置页的调用点与上游一致。

---

## InnerShadow

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/liquid/InnerShadow.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/liquid/InnerShadow.kt`

改动（改动日期：2026-09-28）：只改包名（com.sukisu.ultra -> com.lemon.mrs），逻辑一字未动。

---

## Lens

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/liquid/Lens.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/liquid/Lens.kt`

改动（改动日期：2026-09-28）：只改包名（com.sukisu.ultra -> com.lemon.mrs），逻辑一字未动。

---

## Vibrancy

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/liquid/Vibrancy.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/liquid/Vibrancy.kt`

改动（改动日期：2026-09-28）：只改包名（com.sukisu.ultra -> com.lemon.mrs），逻辑一字未动。

---

## DragGestureInspector

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/modifier/DragGestureInspector.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/modifier/DragGestureInspector.kt`

改动：只有包名（com.sukisu.ultra → com.lemon.mrs）。代码与上游逐字相同。

改动日期：2026-09-27。

---

## BgEffectConfig

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectConfig.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/BgEffectConfig.kt`

改动：只改包名（改动日期：2026-09-28）。四套调色板与数值一字未动。

---

## BgEffectBackground

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectBackground.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/BgEffectBackground.kt`

改动（改动日期：2026-09-28）：
  ① 只改包名；RuntimeShader 从 `top.yukonga.miuix.kmp.shader` 取（上游走 miuix-blur，本项目没这个坐标，
     理由见 BgEffectPainter.kt 的文件头）。
  ② `isInDarkTheme()` / `shouldShowSplitPane()` 换成本项目的实现（同名同义，上游那两个在
     `ui/theme/Theme.kt` 与 `ui/util/WindowSize.kt`）。
  ③ 运行时不支持 RuntimeShader（SDK < 33）时，上游是**直接退化成纯 Box**什么都不画；
     2026-09-28 改为补一层静态渐变（同一套 BgEffectConfig 调色板），低版本也看得到背景。

---

## BgEffectModifier

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectModifier.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/BgEffectModifier.kt`

改动：只改包名（改动日期：2026-09-28）。ModifierNodeElement / DrawModifierNode 的写法、
      60fps 限流、isFullSize 的 0.8f / 0.5f 高度系数一字未动。

---

## BgEffectPainter

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/BgEffectPainter.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/BgEffectPainter.kt`

改动（改动日期：2026-09-28）：① 只改包名；② 上游从 `top.yukonga.miuix.kmp.blur` 取 RuntimeShader/asBrush，
     本项目没有 miuix-blur（它的 manifest 硬要求 minSdk 33），改从 `top.yukonga.miuix.kmp.shader` 取
     ——同一套 API（`miuix-shader-android:0.9.4`，minSdk 24，内部就是包 android.graphics.RuntimeShader）。
     其余（uniform 名字、缓存字段、updateXxx 的顺序与短路逻辑）一字未动。

---

## OS3BgFrag

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/OS3BgFrag.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/OS3BgFrag.kt`

改动：只改包名（改动日期：2026-09-28）。AGSL 着色器正文一字未动。

---

## DeviceType

- 本文件：`app/src/main/java/com/lemon/mrs/ui/component/miuix/effect/DeviceType.kt`
- 上游：`manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/DeviceType.kt`

改动：只改包名（改动日期：2026-09-28）。

