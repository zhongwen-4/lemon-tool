# 检查历史的数据源：本项目自己的扫描记录（不是 SU 日志）

SUMMARY: 界面骨架是照 SukiSU 的 **SU 日志列表**搬的，但本项目是「扫未安装的模块 zip」，
**没有 `/data/adb/ksu/log` 这个数据源**（也不需要 root）。所以「检查历史」= 自己每次检查留下的一条记录：
一行一条 JSON 存在 `filesDir/scan_history.jsonl`，新在前、上限 200 条、超出丢最旧。
**骨架形状不动**（那是移植件，改它就要重写 GPL 出处与改动说明），映射全放在边界上：
`ScanUi.kt` 的 `scanRecordToSulogEntry` 把一条记录变成 `SulogEntry`，键名在 `ScanEntryFields` 里两边共用。
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
val extraDetail: String? = null   // ScanReport 专用：详情弹窗在 fields 之后原样渲染的正文
```

- 填它的是 `ScanUi.kt` 的 `scanRecordDetail(record)`：**写成与 fields 同一套「key: value」日志格式**
  （用户 2026-09-29：「每个检查都按 SU 日志那种写法来」）—— 发现逐条一段
  （`发现 1/12` / `等级:` / `规则:` / `文件: path:line` / `说明:`），截断写 `截断: …`，
  提示写 `提示 1: …`；等级标签复用 `ScanEntryFields.HIGH/MEDIUM/LOW/INFO`（都在同一个映射里，不再引新文案）。
  全是等宽文本、可选中复制 —— 与 `sulogEntryDetailText` 那几行字段排在一起是同一个观感。
- 读它的是 `SulogListMiuix.kt` 的 `SulogDetailDialog`：挂在同一个 `SelectionContainer` 里、
  fields 之后另起一段 Text。
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
