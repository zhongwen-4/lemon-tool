// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/SulogHelper.kt
// 改动清单：docs/sukisu-port-changes.md#sulogmodels
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
     * ScanReport 专用：详情弹窗要渲染成列表的结构化明细（发现逐条 + 提示 + 截断位），
     * 由 ScanUi.kt 的 scanRecordToSulogEntry 填好。null 表示这类条目没有附加明细。
     */
    val scanDetail: ScanDetail? = null,
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
 * ScanReport 的详情正文。2026-09-30 用户要求「详情改成列表」之后这里不再是拼好的文本，
 * 而是结构化数据 —— 界面按「一条发现一张 SU 日志式的卡」渲染（SulogListMiuix.kt）。
 */
data class ScanDetail(
    val findings: List<ScanDetailFinding>,
    val notes: List<String>,
    val truncated: Boolean,
) {
    val isEmpty: Boolean get() = findings.isEmpty() && notes.isEmpty() && !truncated
}

/**
 * 一条发现。[severity] 是核心给的英文小写（high / medium / low / info），[severityLabel] 是同一套中文，
 * 界面拿它当标签文字、按 severity 取标签颜色。
 */
data class ScanDetailFinding(
    val position: Int,
    val total: Int,
    val severity: String,
    val severityLabel: String,
    val rule: String,
    val file: String,
    val line: Int,
    val detail: String,
)

/**
 * ScanReport 事件用到的 fields 键名。
 * 写入方是 ScanUi.kt 的 scanRecordToSulogEntry，读取方是 SulogListMiuix.kt 的
 * 标题 / 描述 / 标签 / 结论四个取值函数 —— 放这里共用，免得两边字符串写岔。
 * 键名直接用中文：检查条目的详情列表按这些键取概览那几行（高·中·低危计数也在内）。
 */
object ScanEntryFields {
    const val MODULE = "模块"
    const val TARGET = "目标"
    const val HIGH = "高危"
    const val MEDIUM = "中危"
    const val LOW = "低危"
    const val VERDICT = "结论"
    const val INFO = "信息"
    const val VERSION = "版本"
    const val PACKAGE = "包名"
    const val AUTHOR = "作者"
    const val FILES = "文件数"
    const val FINDINGS = "发现"
    const val TIME = "时间"
}