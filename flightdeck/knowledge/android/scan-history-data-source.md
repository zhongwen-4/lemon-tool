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
