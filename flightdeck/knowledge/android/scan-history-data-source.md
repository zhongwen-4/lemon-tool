# 检查历史的数据源：本项目自己的扫描记录（不是 SU 日志）

SUMMARY: 界面骨架是照 SukiSU 的 **SU 日志列表**搬的，但本项目是「扫未安装的模块 zip」，
**没有 `/data/adb/ksu/log` 这个数据源**（也不需要 root）。所以「检查历史」= 自己每次检查留下的一条记录：
一行一条 JSON 存在 `filesDir/scan_history.jsonl`，新在前、上限 200 条、超出丢最旧。
**骨架形状不动**（那是移植件，改它就要重写 GPL 出处与改动说明），映射全放在边界上：
`ScanUi.kt` 的 `scanRecordToSulogEntry` 把一条记录变成 `SulogEntry`，键名在 `ScanEntryFields` 里两边共用。
**条目详情是整页的列表**（2026-09-30 二稿）：条目卡退回上一版那套行式，
点一条卡进「另一个列表」= 整页详情（概览 / 每条发现 / 每条提示各一张 SU 日志式的卡），不是一段等宽正文。
READ WHEN: before 动「检查历史」这一页、或要给别的功能找本地持久化方案时。
RECHECK WHEN: 记录里要加字段（记得同时改 `ScanEntryFields` 与 `ScanHistory.parse`），
或用户要给历史加搜索 / 筛选 / 单条删除。

---

## 为什么不用现成的 HistoryStore

2026-09-28 之前有一版 `History.kt`（`HistoryEntry` / `HistoryStore` / `HistoryScreen`），
在「整包换成 SukiSU 骨架」那一轮被删掉了。现在不再造第二套列表 UI：**UI 用移植件、数据用自己的**。
好处是列表的观感与它完全一致（条目卡、详情弹窗、标签配色都是它那套），
代价是 `SulogEntry` 的字段语义本来是为日志行设计的，要「翻译」：

| 记录里的东西 | 放到 SulogEntry 的哪 |
| --- | --- |
| 模块名（空则包名） | `fields[ScanEntryFields.MODULE]` → 条目卡标题 |
| 扫描对象（SAF 显示名） | `fields[ScanEntryFields.TARGET]` → 条目卡副标题 |
| 高 / 中 / 低危计数 | `fields[ScanEntryFields.HIGH/MEDIUM/LOW]` → 三个 StatusTag |
| 结论（verdict） | `fields[ScanEntryFields.VERDICT]` → 条目卡尾部状态 |
| 完成时间 | `timestampText`，另存一份 `fields["时间"]` 给详情弹窗 |
| `id` | `key`（列表项的稳定 key） |
| 事件类型 | `SulogEventType.ScanReport`（为本项目新加的一支） |

其余字段（版本 / 包名 / 作者 / 文件数 / 信息 / 发现条数）只进详情弹窗，不参与列表渲染。

## 两个必须知道的点

- **键名要用中文**：详情弹窗（`sulogEntryDetailText`）是把 `fields` 原样按「key: value」逐行打出来的，
  用 `module_name` 这种内部名会直接暴露给用户。
- **读写两边共用一套常量**：`ScanEntryFields`（在 `ui/util/sulog/SulogModels.kt`）是唯一的键名来源。
  写入方 `ScanUi.kt`、读取方 `SulogListMiuix.kt` 都引它 —— 手写两份字符串必然会写岔，
  而且症状很隐蔽（标题变默认文案「检测结果」，不报错）。

## 存储本身的约定

- `ScanHistory.load/append/clear` 全部包在 `runCatching` 里：**读坏 / 写不进去都当空历史**，不崩界面。
- 上限 200 条，`append` 是「读全量 → 前插 → take(200) → 整份重写」，所以会丢最旧的。
  文件很小（一条几百字节），不值得为它上数据库。
- 清空是**不可逆**的：界面先弹确认（`SulogClearConfirmDialog`），确认后才走 `SulogActions.onCleanFile`。

## 2026-09-29：结果明细全部进历史（主页只留一张结论卡）

用户口径变了：**主页只放「应用版本 / 提交 BUG / 开始检查」**，检查完那张卡按最高风险整卡着色
（高危红 / 中危黄 / 其余绿，照上游主页「工作中」那张卡的版式），**逐条发现卡与详细结论卡不再出现在主页**。
于是 `SulogEntry` 多了一个本项目自加的字段：

```kotlin
val scanDetail: ScanDetail? = null   // ScanReport 专用：详情列表的结构化数据（发现逐条 + 提示 + 截断位）
```

- 填它的是 `ScanUi.kt` 的 `scanRecordDetail(record)`：2026-09-29 那版是**一段等宽的「key: value」正文**，
  2026-09-30 用户要「详情改为列表、同样是 SU 日志的样式」之后改成**结构化数据**（见下一节）。
- 读它的是 `SulogListMiuix.kt` 的 `SulogDetailScreen`（2026-09-30 二稿还是 `SulogDetailDialog` 弹窗）：ScanReport 走 `ScanDetailList`（列表），
  其它事件类型仍是上游那段等宽正文（`sulogEntryDetailText`），两者都留在 `SelectionContainer` 里可选中复制。
- 给移植件加字段的原则照旧：**字段加在数据形状上、渲染留在边界**，骨架的版式不动。
  其它事件类型（RootExecve 等）不填 extraDetail → 行为与以前完全一致。
- 主页那张结论卡的**标题直接写「高危模块 / 中危模块 / 低危模块」**（一档发现都没有时也按最低那档写；
  2026-09-29 用户口径是「只写这三档」），四档计数只列非零项（`severityCountsText`），零发现时写
  「未发现风险项」；点它仍然 = 「重新选包检测」（明细去「检查历史」里看）。
- 2026-09-29 又定两处外观（都是用户口径）：**条目卡右侧写「点击查看详情」**（结论挪进详情弹窗的 fields），
  卡片主体把 **「名称 / 路径 / 时间 / 标签」四样显式写成四行**（`ScanEntryRows`）—— 名称 = 模块名、
  路径 = **文件名**（SAF 只给 `content://` URI，拿不到 `/sdcard/...` 那种真路径，用户原话「第二行写文件名」）、
  时间 = 检查时间、标签 = 高危 / 中危 / 低危三枚 chips。上游那套不写标签的版式原样保留成 `SulogEntryRows`，
  留给将来接别的日志类型用。四个标签都是两个字，宽度天然一致，所以标签列**没设固定宽度**
  （理由见 `android/compose-labeled-rows-alignment.md`）。
  那张卡的版式坑（三层 `fillMaxSize` 导致文字重叠）单独记在 `android/compose-box-corner-layout.md`。

## 2026-09-30：条目详情改成列表（每项一张 SU 日志式的卡）

用户原话「把检查历史卡片的详情改为列表，同样是 su 日志的样式」。做法：

- 数据形状换成**结构化**的：`SulogEntry.scanDetail: ScanDetail?`，`ScanDetail` =
  `findings: List<ScanDetailFinding>` + `notes: List<String>` + `truncated: Boolean`（`isEmpty` 便于判空）；
  `ScanDetailFinding` 带 `position/total/severity/severityLabel/rule/file/line/detail`。
  `ScanUi.kt` 只填字段、不拼文本；两者都在 `ui/util/sulog/SulogModels.kt`。
- 渲染在 `SulogListMiuix.kt` 的 `ScanDetailList`：一张 `DetailCard`（= Miuix `Card` +
  `insideMargin = PaddingValues(16.dp)`，与列表条目卡同一种）**一项**：
  1. `ScanOverviewCard` —— 「名称 / 路径 / 时间 / 版本 / 包名 / 作者 / 文件数 / 发现」这些**复用
     `ScanEntryLine`**（列表卡那套「标签 + 值」行），再来一行标签 chips 与一句结论；
  2. 每条发现一张 `ScanFindingCard` —— 标题 = 规则名、右侧 = `1/12`（复用 `SulogEntryStatusText`）、
     文件行、说明行（长文本用 `ScanEntryWrappedLine`，**不能用带 marquee 的 `ScanEntryLine`**）、等级 chip；
  3. 每条提示 / 截断各一张 `ScanNoteCard`。
- **颜色口径**：详情里计数与等级 chip 走主页结论卡那套红 / 黄 / 绿（`ScanSeverityTag`，
  信息档走主题次级容器色），一档都没有时写「未发现风险项」；列表卡的 chips 仍是上游那套
  主题三色（`sulogEntrySummaryTags`）—— 详情这一处是刻意不同的，用户若不认再改回去。
- 概览卡把原先 fields 正文里的东西**一样不少**地搬了过来（键名走 `ScanEntryFields`，
  2026-09-30 补了 `VERSION / PACKAGE / AUTHOR / FILES / FINDINGS / TIME` 六个常量）。

## 2026-09-30 二稿：条目卡退回上一版，详情从弹窗改成整页

用户口径「检查历史的列表改回去，单个卡片点进去进入另一个列表」。先问清「改回去」指哪一段才动手 ——
0.11.5 那四行是用户自己点名要的，猜错就是白跑一轮 CI。两件：

- **条目卡退回上一版（0.11.4）的版式**：不再按事件类型分叉，一律走 `SulogEntryRows`（上游那套
  「标题 / 描述 / 时间 + 一行标签 chips」，右侧仍是「点击查看详情」），`ScanEntryRows` 整段删掉。
  ⚠ 删它时**别顺手删 `ScanEntryLine` / `ScanEntryLabel`** —— 这两个「标签 + 值」的小函数详情卡还在用
  （概览卡与发现卡都靠它们渲染），跟要删的四行卡是两码事。
- **详情由弹窗改成整页** `SulogDetailScreen`（配方见 `android/overlay-page-and-predictive-back.md` 的第二处整页）：
  骨架照关于页（`Scaffold` + `SmallTopAppBar` 返回箭头 + 一列可滚的卡，内容仍是上一节那套 `ScanDetailList`），
  弹窗里那个「确定」按钮不再需要；返回手势与关于页同一套二选一（开关开 = 跟手往右滑、关 = 普通返回）。
- **坐标上的坑**：这一页不能做在 pager 的页里 —— Miuix 的悬浮底栏是 `Scaffold(bottomBar = …)` 画的、排在
  内容之后，盖在 pager 页里只会盖住内容、底栏仍浮在上面还能被点到。所以状态 `detailEntry` 与叠层都提到
  主壳 `ScanUi.ScannerShell`；`MiuixTheme` 只是 `CompositionLocalProvider`、不插布局节点（实拉 0.9.4 sources jar 核过）。
- 余下那条「OverlayDialog 底色 / Card 内边距」的实测事实对**清空确认弹窗**仍然有效，留在下一节。

## 2026-10-01：详情最上面那张提示卡（白底红字）

用户要「每个检查详情最上面写一行红色的卡片：该结果仅供参考」，紧接着又定颜色口径：**卡片是白的、字是红的**。

- 落法：`SulogDetailScreen` 的内容包一层 `Column`，第一张就是这张提示卡 —— 它用 `DetailCard`
  （**默认卡色**，浅色下白、深色下自动变深，所以它和下面那几列卡是同一个底），里面一个 `Text`，
  `color = Color(0xFFF72727)`（与主页结论卡、详情等级 chip 同一个红）。
- ⚠ **别用上游的 `WarningCard`**（本项目已经移植了它）：那个组件默认 `level = WarningLevel.Error`，
  是**红底红字**（浅色 0xFFF8E2E2 底 / 深色 0xFF310808 底）。用户明确否掉了这个观感 ——
  "红色的卡片"指的是字是红的，不是底是红的。文案 `scan_detail_disclaimer`。
- 2026-10-01 追加：这张卡的文字**居中**（`Modifier.fillMaxWidth()` + `textAlign = TextAlign.Center`）—— 用户口径，用不着自己换行。

## 弹窗里的两个实测事实（清空确认弹窗仍在用）

- `MiuiX` 的 `OverlayDialog` 底色就是 `MiuixTheme.colorScheme.background`（`DialogDefaults.backgroundColor()`），
  与页面底色同一个值 —— 所以弹窗里直接放默认 `Card`（底色 `surfaceContainer`）**有对比、不会糊成一片**，
  和列表页看到的是同一种关系。`DialogDefaults.insideMargin = DpSize(24.dp, 24.dp)`。
- Miuix 的 `CardDefaults.InsideMargin` 是 **`PaddingValues(0.dp)`**，卡片想有内边距必须**显式传**
  `insideMargin`（列表条目卡与这里的 `DetailCard` 都传 16.dp）；不传的话内容会贴着卡片边缘。
- 这两条是**实拉 sources jar 看来的**，不是猜的：`repo1.maven.org/maven2/top/yukonga/miuix/kmp/miuix-ui-android/0.9.4/`
  下的 `miuix-ui-android-0.9.4-sources.jar`，用
  `[System.IO.Compression.ZipFile]::OpenRead(...)` 读 `commonMain/.../basic/Card.kt`、
  `overlay/OverlayDialog.kt`、`layout/DialogContentLayout.kt`。查组件签名的三条路见 `android/miuix-0.9.4.md`。