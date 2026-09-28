// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/SulogHelper.kt
// 改动（改动日期：2026-09-28）：
//   ① 只留数据形状 SulogFile / SulogEventType / SulogEventFilter / SulogEntry 与过滤器默认值。
//      上游同文件里的 listSulogFiles / readSulogFile / parseSulogLines / parseSulogLine /
//      cleanSulogFile / deleteSulogFile 都要读 /data/adb/ksu/log，本项目没有这个数据源，不搬。
//   ② 2026-09-28 补一个 ScanReport：本项目的历史是「自己每次检查模块留下的记录」，
//      没有 SU 日志，所以给这个列表加一个自己的事件类型，让条目卡能显示模块名 / 对象路径 /
//      高·中·低危计数 / 结论（映射在 ScanUi.kt 的 scanRecordToSulogEntry，展示在 SulogListMiuix.kt）。
package com.lemon.mrs.ui.util.sulog

data class SulogFile(
    val name: String,
    val path: String,
)

enum class SulogEventType {
    RootExecve,
    SuCompat,
    IoctlGrantRoot,
    DaemonEvent,
    Dropped,
    ScanReport,
    Unknown,
}

enum class SulogEventFilter(val eventType: SulogEventType?) {
    RootExecve(SulogEventType.RootExecve),
    SuCompat(SulogEventType.SuCompat),
    IoctlGrantRoot(SulogEventType.IoctlGrantRoot),
    DaemonEvent(SulogEventType.DaemonEvent),
}

fun defaultSulogEventFilters(): Set<SulogEventFilter> = SulogEventFilter.entries.toSet()

data class SulogEntry(
    val key: String,
    val eventType: SulogEventType,
    val rawLine: String,
    val timestampText: String?,
    val fields: Map<String, String>,
    /**
     * ScanReport 专用：详情弹窗在 fields 之后原样渲染的那段明细正文（发现逐条 + 提示），
     * 由 ScanUi.kt 的 scanRecordToSulogEntry 填好。null 表示这类条目没有附加明细。
     */
    val extraDetail: String? = null,
) {
    val searchableText: String by lazy {
        buildString {
            append(rawLine)
            append('\n')
            timestampText?.let {
                append(it)
                append('\n')
            }
            fields.values.forEach {
                append(it)
                append(' ')
            }
        }.lowercase()
    }
}

/**
 * ScanReport 事件用到的 fields 键名。
 * 写入方是 ScanUi.kt 的 scanRecordToSulogEntry，读取方是 SulogListMiuix.kt 的
 * 标题 / 描述 / 标签 / 结论四个取值函数 —— 放这里共用，免得两边字符串写岔。
 * 注意：详情弹窗会把 fields 原样按「key: value」逐行打出来，所以键名直接用中文。
 */
object ScanEntryFields {
    const val MODULE = "模块"
    const val TARGET = "目标"
    const val HIGH = "高危"
    const val MEDIUM = "中危"
    const val LOW = "低危"
    const val VERDICT = "结论"
    const val INFO = "信息"
}