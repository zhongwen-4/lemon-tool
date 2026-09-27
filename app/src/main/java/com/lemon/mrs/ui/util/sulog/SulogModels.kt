// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/SulogHelper.kt
// 改动（改动日期：2026-09-28）：
//   ① 只留数据形状 SulogFile / SulogEventType / SulogEventFilter / SulogEntry 与过滤器默认值。
//      上游同文件里的 listSulogFiles / readSulogFile / parseSulogLines / parseSulogLine /
//      cleanSulogFile / deleteSulogFile 都要读 /data/adb/ksu/log，本项目没有这个数据源，不搬。
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