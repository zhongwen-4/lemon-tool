package com.lemon.mrs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType

private const val HERO_WEIGHT = 0.4f

private val HERO_ICON_SIZE = 72.dp
private val HEADER_ICON_SIZE = 40.dp
private val HEADER_ICON_GAP = 3.dp
private val PANEL_BORDER = 1.5.dp
private val PANEL_SHAPE = RoundedCornerShape(CardDefaults.CornerRadius)

private val WARNING_ON_LIGHT = Color(0xFFBF6A00)
private val WARNING_ON_DARK = Color(0xFFFFC24B)

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
    var expanded by remember { mutableStateOf(emptySet<Int>()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        state = ScanState.Scanning
        expanded = emptySet()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { parseReport(scan(MainActivity.copyToCache(context, uri))) }
            }
            state = outcome.fold(
                onSuccess = { ScanState.Done(it) },
                onFailure = { ScanState.Failed(it.message ?: "扫描失败") },
            )
        }
    }

    MiuixTheme(controller = remember { ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem) }) {
        Scaffold(topBar = { AppHeader() }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                ModulePanel(
                    state = state,
                    onPick = { picker.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(HERO_WEIGHT).fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                FindingList(
                    state = state,
                    expanded = expanded,
                    onToggle = { index ->
                        expanded = if (index in expanded) expanded - index else expanded + index
                    },
                    modifier = Modifier.weight(1f - HERO_WEIGHT).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun AppHeader() {
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_app_mark),
            contentDescription = null,
            modifier = Modifier.size(HEADER_ICON_SIZE),
            tint = Color.Unspecified,
        )
        Spacer(Modifier.width(HEADER_ICON_GAP))
        Column {
            Text(text = stringResource(R.string.app_name), style = MiuixTheme.textStyles.title3)
            Text(
                text = "版本 $version",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun ModulePanel(state: ScanState, onPick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = panelColors()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.height(HERO_ICON_SIZE), contentAlignment = Alignment.Center) {
                if (state == ScanState.Scanning) {
                    CircularProgressIndicator(size = HERO_ICON_SIZE * 0.55f)
                } else {
                    Icon(
                        imageVector = MiuixIcons.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(HERO_ICON_SIZE),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            when (state) {
                ScanState.Idle -> {
                    Text(text = "尚未选择模块", style = MiuixTheme.textStyles.title3)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "只检查未安装的模块 zip，不解包安装、不改动设备",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }

                ScanState.Scanning -> Text(text = "正在检查…", style = MiuixTheme.textStyles.title3)

                is ScanState.Done -> {
                    Text(
                        text = state.report.moduleName.ifBlank { "未知模块" },
                        style = MiuixTheme.textStyles.title3,
                    )
                    val subtitle = moduleSubtitle(state.report)
                    if (subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                is ScanState.Failed -> {
                    Text(
                        text = "检查失败",
                        style = MiuixTheme.textStyles.title3,
                        color = MiuixTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = state.message,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state != ScanState.Scanning,
            ) {
                Text(
                    text = if (state is ScanState.Done) "重新选择模块" else "选择模块 zip",
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

@Composable
private fun FindingList(
    state: ScanState,
    expanded: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val report = (state as? ScanState.Done)?.report
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 4.dp),
    ) {
        if (report == null) {
            item {
                HintCard(
                    text = (state as? ScanState.Failed)?.message
                        ?: "选择模块 zip 后，检测项会按高危、中危、低危列在这里。",
                )
            }
            return@LazyColumn
        }
        item { SummaryCard(report) }
        if (report.findings.isEmpty()) {
            item { HintCard(text = "未发现风险特征。") }
        }
        itemsIndexed(report.findings) { index, finding ->
            FindingCard(
                finding = finding,
                expanded = index in expanded,
                onToggle = { onToggle(index) },
            )
        }
        if (report.notes.isNotEmpty()) {
            item { NotesCard(report.notes) }
        }
    }
}

@Composable
private fun SummaryCard(report: ScanReport) {
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CountLabel("高危", report.high, severityTextColor("high"))
                CountLabel("中危", report.medium, severityTextColor("medium"))
                CountLabel("低危", report.low, severityTextColor("low"))
                CountLabel("信息", report.info, severityTextColor("info"))
            }
            if (report.verdict.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = report.verdict,
                    style = MiuixTheme.textStyles.footnote1,
                    color = if (report.high > 0) {
                        MiuixTheme.colorScheme.error
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                )
            }
            if (report.truncated) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "部分条目过大，只扫描了前一段内容。",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun CountLabel(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), style = MiuixTheme.textStyles.title3, color = color)
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun FindingCard(finding: Finding, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(PANEL_BORDER, severityBorder(finding.severity), PANEL_SHAPE),
        colors = panelColors(),
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onToggle,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = finding.rule, style = MiuixTheme.textStyles.footnote1)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = severityLabel(finding.severity),
                        style = MiuixTheme.textStyles.footnote2,
                        color = severityTextColor(finding.severity),
                    )
                }
                Icon(
                    imageVector = if (expanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = finding.detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(text = locationLine(finding), style = MiuixTheme.textStyles.footnote2)
                if (finding.file.isNotBlank()) {
                    Text(
                        text = finding.file,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors()) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun NotesCard(notes: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            notes.forEach { note ->
                Text(
                    text = "· $note",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

@Composable
private fun panelColors(): CardColors =
    CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHigh)

@Composable
private fun warningColor(): Color =
    if (MiuixTheme.colorScheme.surface.luminance() > 0.5f) WARNING_ON_LIGHT else WARNING_ON_DARK

@Composable
private fun severityBorder(severity: String): Color = when (severity) {
    "high" -> MiuixTheme.colorScheme.error
    "medium" -> warningColor()
    else -> Color.Transparent
}

@Composable
private fun severityTextColor(severity: String): Color = when (severity) {
    "high" -> MiuixTheme.colorScheme.error
    "medium" -> warningColor()
    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

private fun severityLabel(severity: String) = when (severity) {
    "high" -> "高危"
    "medium" -> "中危"
    "low" -> "低危"
    else -> "信息"
}

private fun moduleSubtitle(report: ScanReport): String = listOfNotNull(
    report.version.takeIf { it.isNotBlank() }?.let { "v$it" },
    report.author.takeIf { it.isNotBlank() },
    report.moduleId.takeIf { it.isNotBlank() },
).joinToString(" · ")

private fun locationLine(finding: Finding): String {
    if (finding.file.isBlank()) return "未定位到具体文件"
    val name = finding.file.substringAfterLast('/').ifBlank { finding.file }
    return if (finding.line > 0) "文件($name)第 ${finding.line} 行" else "文件($name)"
}

@Suppress("DEPRECATION")
private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
        .getOrNull()
        .orEmpty()

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