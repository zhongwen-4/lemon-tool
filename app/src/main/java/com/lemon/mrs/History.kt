package com.lemon.mrs

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 一次扫描的存档：概览够列表页显示，[reportJson] 留着回看那次的完整报告。
 * 报告本身就是核心吐出的 JSON，不再另做一套格式。
 */
data class HistoryEntry(
    val finishedAt: Long,
    val moduleName: String,
    val moduleId: String,
    val version: String,
    val fileCount: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val info: Int,
    val verdict: String,
    val reportJson: String,
)

/** 存档文件放在 filesDir（不是 cacheDir），系统清缓存不会把历史一起清掉。 */
private const val HISTORY_FILE = "history.json"

internal const val MAX_ENTRIES = 50

fun historyEntryOf(report: ScanReport, reportJson: String, finishedAt: Long): HistoryEntry = HistoryEntry(
    finishedAt = finishedAt,
    moduleName = report.moduleName,
    moduleId = report.moduleId,
    version = report.version,
    fileCount = report.fileCount,
    high = report.high,
    medium = report.medium,
    low = report.low,
    info = report.info,
    verdict = report.verdict,
    reportJson = reportJson,
)

object HistoryStore {

    /** 读不出来（文件损坏、格式变了）就当没有，不要因此让界面崩掉。 */
    fun load(context: Context): List<HistoryEntry> = runCatching {
        val file = File(context.filesDir, HISTORY_FILE)
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText())
        (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let(::fromJson) }
    }.getOrElse { emptyList() }

    fun append(context: Context, entry: HistoryEntry): List<HistoryEntry> {
        val next = (listOf(entry) + load(context)).take(MAX_ENTRIES)
        runCatching {
            val array = JSONArray()
            next.forEach { array.put(toJson(it)) }
            File(context.filesDir, HISTORY_FILE).writeText(array.toString())
        }
        return next
    }

    fun clear(context: Context): List<HistoryEntry> {
        runCatching { File(context.filesDir, HISTORY_FILE).takeIf { it.exists() }?.delete() }
        return emptyList()
    }

    private fun toJson(entry: HistoryEntry): JSONObject = JSONObject().apply {
        put("finishedAt", entry.finishedAt)
        put("moduleName", entry.moduleName)
        put("moduleId", entry.moduleId)
        put("version", entry.version)
        put("fileCount", entry.fileCount)
        put("high", entry.high)
        put("medium", entry.medium)
        put("low", entry.low)
        put("info", entry.info)
        put("verdict", entry.verdict)
        put("report", entry.reportJson)
    }

    private fun fromJson(json: JSONObject): HistoryEntry? {
        val report = json.optString("report")
        if (report.isBlank()) return null
        return HistoryEntry(
            finishedAt = json.optLong("finishedAt"),
            moduleName = json.optString("moduleName"),
            moduleId = json.optString("moduleId"),
            version = json.optString("version"),
            fileCount = json.optInt("fileCount"),
            high = json.optInt("high"),
            medium = json.optInt("medium"),
            low = json.optInt("low"),
            info = json.optInt("info"),
            verdict = json.optString("verdict"),
            reportJson = report,
        )
    }
}

@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    onOpen: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (entries.isEmpty()) {
            item { HistorySectionTitle("检查历史") }
            item { EmptyHistoryCard() }
        } else {
            item { HistorySectionTitle("扫描概览") }
            item { SummaryCard(entries, onClear) }
            item { HistorySectionTitle("历史记录") }
            item { HistoryListCard(entries, onOpen) }
        }
    }
}

@Composable
private fun HistorySectionTitle(text: String) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
    )
}

@Composable
private fun SummaryCard(entries: List<HistoryEntry>, onClear: () -> Unit) {
    val risk = entries.count { it.high > 0 }
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors(), cornerRadius = 16.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Text(text = "扫描概览", style = MiuixTheme.textStyles.title3)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "共 ${entries.size} 次，其中 $risk 次出现高危",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(14.dp))
            ClearRow(onClear = onClear)
        }
    }
}

@Composable
private fun ClearRow(onClear: () -> Unit) {
    Text(
        text = "清空历史",
        style = MiuixTheme.textStyles.button,
        color = MiuixTheme.colorScheme.error,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClear)
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun HistoryListCard(entries: List<HistoryEntry>, onOpen: (HistoryEntry) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = panelColors(),
        cornerRadius = 16.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
                HistoryRow(entry = entry, onOpen = { onOpen(entry) })
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.moduleName.ifBlank { "未命名模块" },
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = historySubtitle(entry),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = riskTail(entry),
            style = MiuixTheme.textStyles.footnote1,
            color = riskColor(entry),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors(), cornerRadius = 16.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "还没有检查记录",
                style = MiuixTheme.textStyles.title3,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "每次扫描完成都会自动存一条，方便回头对照。",
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val HISTORY_TIME = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

internal fun historySubtitle(entry: HistoryEntry): String {
    val time = HISTORY_TIME.format(Date(entry.finishedAt))
    val version = entry.version.takeIf { it.isNotBlank() }?.let { " · v$it" } ?: ""
    return "$time$version"
}

internal fun riskTail(entry: HistoryEntry): String =
    if (entry.high == 0 && entry.medium == 0) "未见异常" else "高 ${entry.high} · 中 ${entry.medium}"

@Composable
internal fun riskColor(entry: HistoryEntry): Color = when {
    entry.high > 0 -> MiuixTheme.colorScheme.error
    entry.medium > 0 -> MiuixTheme.colorScheme.primary
    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}
