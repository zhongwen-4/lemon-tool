// 本项目自写（非移植件）：检查记录的本地存储。
//
// 数据源说明：本项目是「扫未安装的模块 zip」，没有 SukiSU 那种 /data/adb/ksu/log，
// 所以「检查历史」就是自己每次检查留下的一条记录 —— 一行一条 JSON 存在 filesDir，
// 最多 MAX_RECORDS 条、新的在前、超出丢最旧。不联网、不需要任何权限。
package com.lemon.mrs

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** 一条检查记录：跟 [ScanReport] 对齐，另加扫描对象与完成时间。/ [id] 是列表项的稳定 key。 */
data class ScanRecord(
    val id: String,
    val target: String,
    val finishedAt: Long,
    val moduleName: String,
    val moduleId: String,
    val version: String,
    val author: String,
    val fileCount: Int,
    val high: Int,
    val medium: Int,
    val low: Int,
    val info: Int,
    val verdict: String,
    val truncated: Boolean,
    val findings: List<Finding>,
    val notes: List<String>,
)

object ScanHistory {

    private const val FILE_NAME = "scan_history.jsonl"
    private const val MAX_RECORDS = 200

    /** 把一次成功的扫描变成一条记录。 */
    fun of(report: ScanReport, target: String, finishedAt: Long): ScanRecord = ScanRecord(
        id = UUID.randomUUID().toString(),
        target = target,
        finishedAt = finishedAt,
        moduleName = report.moduleName,
        moduleId = report.moduleId,
        version = report.version,
        author = report.author,
        fileCount = report.fileCount,
        high = report.high,
        medium = report.medium,
        low = report.low,
        info = report.info,
        verdict = report.verdict,
        truncated = report.truncated,
        findings = report.findings,
        notes = report.notes,
    )

    /** 读出全部记录（新 -> 旧）。文件不在或读坏了都当空历史，不抛异常。 */
    fun load(context: Context): List<ScanRecord> = runCatching {
        val file = file(context)
        if (!file.isFile) {
            emptyList()
        } else {
            file.readLines()
                .asSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { parse(it) }
                .sortedByDescending { it.finishedAt }
                .toList()
        }
    }.getOrElse { emptyList() }

    /** 追加一条并返回新的完整列表（列表上限 MAX_RECORDS）。 */
    fun append(context: Context, record: ScanRecord): List<ScanRecord> {
        val updated = (listOf(record) + load(context)).take(MAX_RECORDS)
        runCatching { write(context, updated) }
        return updated
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun write(context: Context, records: List<ScanRecord>) {
        file(context).bufferedWriter(Charsets.UTF_8).use { writer ->
            records.forEach { record ->
                writer.write(record.toJson().toString())
                writer.newLine()
            }
        }
    }

    private fun ScanRecord.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("target", target)
        put("finishedAt", finishedAt)
        put("moduleName", moduleName)
        put("moduleId", moduleId)
        put("version", version)
        put("author", author)
        put("fileCount", fileCount)
        put("high", high)
        put("medium", medium)
        put("low", low)
        put("info", info)
        put("verdict", verdict)
        put("truncated", truncated)
        put(
            "findings",
            JSONArray().apply {
                findings.forEach { finding ->
                    put(
                        JSONObject().apply {
                            put("severity", finding.severity)
                            put("rule", finding.rule)
                            put("file", finding.file)
                            put("line", finding.line)
                            put("detail", finding.detail)
                        }
                    )
                }
            },
        )
        put("notes", JSONArray(notes))
    }

    private fun parse(line: String): ScanRecord? = runCatching {
        val json = JSONObject(line)
        val findings = json.optJSONArray("findings") ?: JSONArray()
        val notes = json.optJSONArray("notes") ?: JSONArray()
        ScanRecord(
            id = json.optString("id"),
            target = json.optString("target"),
            finishedAt = json.optLong("finishedAt"),
            moduleName = json.optString("moduleName"),
            moduleId = json.optString("moduleId"),
            version = json.optString("version"),
            author = json.optString("author"),
            fileCount = json.optInt("fileCount"),
            high = json.optInt("high"),
            medium = json.optInt("medium"),
            low = json.optInt("low"),
            info = json.optInt("info"),
            verdict = json.optString("verdict"),
            truncated = json.optBoolean("truncated"),
            findings = (0 until findings.length()).map { index ->
                val item = findings.optJSONObject(index) ?: JSONObject()
                Finding(
                    severity = item.optString("severity"),
                    rule = item.optString("rule"),
                    file = item.optString("file"),
                    line = item.optInt("line"),
                    detail = item.optString("detail"),
                )
            },
            notes = (0 until notes.length()).map { notes.optString(it) },
        )
    }.getOrNull()
}
