package com.lemon.mrs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val report: ScanReport) : ScanState
    data class Failed(val message: String) : ScanState
}

data class Finding(
    val severity: String,
    val rule: String,
    val file: String,
    val line: Int,
    val detail: String,
)

data class ScanReport(
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

@Composable
fun ScannerScreen(scan: (String) -> String) {
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        state = ScanState.Scanning
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val path = MainActivity.copyToCache(context, uri)
                    parseReport(scan(path))
                }
            }
            state = outcome.fold(
                onSuccess = { ScanState.Done(it) },
                onFailure = { ScanState.Failed(it.message ?: "扫描失败") },
            )
        }
    }

    MiuixTheme {
        Scaffold(
            topBar = { SmallTopAppBar(title = "模块风险检测") },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card {
                    Text(
                        text = "只检查未安装的模块包，不改动设备，不需要 root。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("选择模块 zip 检查", style = MiuixTheme.textStyles.button)
                    }
                }

                when (val current = state) {
                    ScanState.Idle -> Unit
                    ScanState.Scanning -> Card {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(size = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("扫描中…", style = MiuixTheme.textStyles.body2)
                        }
                    }
                    is ScanState.Failed -> Card {
                        Text(
                            text = current.message,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.error,
                        )
                    }
                    is ScanState.Done -> ReportBody(current.report)
                }
            }
        }
    }
}

@Composable
private fun ReportBody(report: ScanReport) {
    Card {
        Text(
            text = report.moduleName.ifBlank { "(模块未提供名称)" },
            style = MiuixTheme.textStyles.title4,
        )
        if (report.moduleId.isNotBlank()) {
            Text(
                text = report.moduleId,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        val versionLine = listOfNotNull(
            report.version.takeIf { it.isNotBlank() }?.let { "版本 $it" },
            report.author.takeIf { it.isNotBlank() }?.let { "作者 $it" },
            "文件数 ${report.fileCount}",
        ).joinToString(" · ")
        Spacer(Modifier.height(6.dp))
        Text(
            text = versionLine,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            CountLabel("高危", report.high, MiuixTheme.colorScheme.error)
            CountLabel("中危", report.medium, MiuixTheme.colorScheme.primary)
            CountLabel("低危", report.low, MiuixTheme.colorScheme.onSurfaceVariantSummary)
            CountLabel("信息", report.info, MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        if (report.verdict.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = report.verdict,
                style = MiuixTheme.textStyles.footnote1,
                color = when {
                    report.high > 0 -> MiuixTheme.colorScheme.error
                    report.medium > 0 -> MiuixTheme.colorScheme.primary
                    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
            )
        }
        if (report.truncated) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "部分条目过大，只扫描了前一段内容",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }

    if (report.findings.isEmpty()) {
        Card {
            Text("未发现风险特征。", style = MiuixTheme.textStyles.body2)
        }
        return
    }

    SmallTitle("发现 ${report.findings.size} 项")
    Card {
        report.findings.forEachIndexed { index, finding ->
            if (index > 0) Spacer(Modifier.height(14.dp))
            Text(
                text = "[${severityLabel(finding.severity)}] ${finding.rule}",
                style = MiuixTheme.textStyles.footnote1,
                color = severityColor(finding.severity),
            )
            if (finding.file.isNotBlank()) {
                val location = if (finding.line > 0) "${finding.file}:${finding.line}" else finding.file
                Text(
                    text = location,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Text(text = finding.detail, style = MiuixTheme.textStyles.footnote1)
        }
    }

    if (report.notes.isNotEmpty()) {
        SmallTitle("提示")
        Card {
            report.notes.forEach { note ->
                Text(
                    text = "· $note",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun CountLabel(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$value", style = MiuixTheme.textStyles.title3, color = color)
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun severityColor(severity: String) = when (severity) {
    "high" -> MiuixTheme.colorScheme.error
    "medium" -> MiuixTheme.colorScheme.primary
    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

private fun severityLabel(severity: String) = when (severity) {
    "high" -> "高危"
    "medium" -> "中危"
    "low" -> "低危"
    else -> "信息"
}

private fun parseReport(json: String): ScanReport {
    val root = JSONObject(json)
    root.optString("error").takeIf { it.isNotBlank() }?.let { throw IllegalStateException(it) }
    val module = root.optJSONObject("module") ?: JSONObject()
    val counts = root.optJSONObject("counts") ?: JSONObject()
    val findings = root.optJSONArray("findings") ?: JSONArray()
    val notes = root.optJSONArray("notes") ?: JSONArray()
    return ScanReport(
        moduleName = module.optString("name"),
        moduleId = module.optString("id"),
        version = module.optString("version"),
        author = module.optString("author"),
        fileCount = root.optInt("fileCount"),
        high = counts.optInt("high"),
        medium = counts.optInt("medium"),
        low = counts.optInt("low"),
        info = counts.optInt("info"),
        verdict = root.optString("verdict"),
        truncated = root.optBoolean("truncated"),
        findings = (0 until findings.length()).map { index ->
            val item = findings.getJSONObject(index)
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
}
