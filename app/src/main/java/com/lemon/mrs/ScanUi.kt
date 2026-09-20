package com.lemon.mrs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDisplayMode
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Layers
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAB_HOME = 0
private const val TAB_ABOUT = 1
private const val NORMAL_KEY = -1

private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val PILL_SHAPE = RoundedCornerShape(percent = 50)
private val CARD_BORDER = 1.dp
private val HEADER_ICON_SIZE = 40.dp
private val HEADER_ICON_GAP = 3.dp
private val GLYPH_LARGE = 52.dp
private val GLYPH_SMALL = 30.dp
private val BUTTON_RADIUS = 26.dp
private val BUTTON_HEIGHT = 52.dp

private val WARNING_ON_LIGHT = Color(0xFFBF6A00)
private val WARNING_ON_DARK = Color(0xFFFFC24B)
private val OK_ON_LIGHT = Color(0xFF2E7D32)
private val OK_ON_DARK = Color(0xFF7FD48C)

private sealed interface ScanState {
    data object Idle : ScanState
    data object Scanning : ScanState
    data class Done(val report: ScanReport, val finishedAt: Long) : ScanState
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
    var lastPath by remember { mutableStateOf<String?>(null) }
    var tab by remember { mutableStateOf(TAB_HOME) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun runScan(path: String) {
        lastPath = path
        state = ScanState.Scanning
        expanded = emptySet()
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { parseReport(scan(path)) }
            }
            state = outcome.fold(
                onSuccess = { ScanState.Done(it, System.currentTimeMillis()) },
                onFailure = { ScanState.Failed(it.message ?: "扫描失败") },
            )
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = runCatching { MainActivity.copyToCache(context, uri) }.getOrNull()
        if (path == null) {
            state = ScanState.Failed("无法读取所选文件")
        } else {
            runScan(path)
        }
    }

    MiuixTheme(controller = remember { ThemeController(colorSchemeMode = ColorSchemeMode.MonetSystem) }) {
        Scaffold(
            topBar = { AppHeader() },
            bottomBar = {
                NavigationBar(mode = NavigationBarDisplayMode.TextOnly) {
                    NavigationBarItem(
                        selected = tab == TAB_HOME,
                        onClick = { tab = TAB_HOME },
                        icon = MiuixIcons.Tasks,
                        label = "主页",
                    )
                    NavigationBarItem(
                        selected = tab == TAB_ABOUT,
                        onClick = { tab = TAB_ABOUT },
                        icon = MiuixIcons.Info,
                        label = "关于",
                    )
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (tab == TAB_HOME) {
                    HomeScreen(
                        state = state,
                        expanded = expanded,
                        onToggle = { key ->
                            expanded = if (key in expanded) expanded - key else expanded + key
                        },
                        onPick = { picker.launch(arrayOf("*/*")) },
                        onRescan = { lastPath?.let { runScan(it) } },
                    )
                } else {
                    AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                    text = "v$version",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        HorizontalDivider(color = MiuixTheme.colorScheme.dividerLine)
    }
}

@Composable
private fun HomeScreen(
    state: ScanState,
    expanded: Set<Int>,
    onToggle: (Int) -> Unit,
    onPick: () -> Unit,
    onRescan: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatusCard(state = state, onPick = onPick, onRescan = onRescan) }

        val report = (state as? ScanState.Done)?.report ?: return@LazyColumn
        if (report.findings.isEmpty()) {
            item {
                NormalCard(
                    report = report,
                    expanded = NORMAL_KEY in expanded,
                    onToggle = { onToggle(NORMAL_KEY) },
                )
            }
        } else {
            itemsIndexed(report.findings) { index, finding ->
                FindingCard(
                    finding = finding,
                    expanded = index in expanded,
                    onToggle = { onToggle(index) },
                )
            }
        }
        if (report.notes.isNotEmpty()) {
            item { NotesCard(report.notes) }
        }
    }
}

@Composable
private fun StatusCard(state: ScanState, onPick: () -> Unit, onRescan: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors(), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusGlyph(state)
            Spacer(Modifier.height(12.dp))
            Text(
                text = statusTitle(state),
                style = MiuixTheme.textStyles.title3,
                textAlign = TextAlign.Center,
            )
            val detail = statusDetail(state)
            if (detail.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = detail,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = if (state is ScanState.Done) onRescan else onPick,
                modifier = Modifier.fillMaxWidth(),
                enabled = state != ScanState.Scanning,
                cornerRadius = BUTTON_RADIUS,
                minHeight = BUTTON_HEIGHT,
            ) {
                Text(
                    text = if (state is ScanState.Done) "重新检测" else "选择模块 zip",
                    style = MiuixTheme.textStyles.button,
                )
            }
            if (state is ScanState.Done) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onPick,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = BUTTON_RADIUS,
                    minHeight = BUTTON_HEIGHT,
                ) {
                    Text(text = "选择其他模块", style = MiuixTheme.textStyles.button)
                }
            }
            val caption = statusCaption(state)
            if (caption.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = caption,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatusGlyph(state: ScanState) {
    when (state) {
        ScanState.Scanning -> Box(
            modifier = Modifier.height(GLYPH_LARGE),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(size = GLYPH_LARGE * 0.75f)
        }

        ScanState.Idle -> Icon(
            imageVector = MiuixIcons.Layers,
            contentDescription = null,
            modifier = Modifier.size(GLYPH_LARGE),
            tint = MiuixTheme.colorScheme.primary,
        )

        is ScanState.Failed -> StateIcon(R.drawable.ic_state_alert, MiuixTheme.colorScheme.error, GLYPH_LARGE)

        is ScanState.Done -> when {
            state.report.high > 0 -> StateIcon(R.drawable.ic_state_alert, MiuixTheme.colorScheme.error, GLYPH_LARGE)
            state.report.medium > 0 -> StateIcon(R.drawable.ic_state_alert, warningColor(), GLYPH_LARGE)
            else -> StateIcon(R.drawable.ic_state_ok, okColor(), GLYPH_LARGE)
        }
    }
}

@Composable
private fun StateIcon(resId: Int, tint: Color, size: Dp) {
    Icon(
        painter = painterResource(resId),
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = tint,
    )
}

@Composable
private fun FindingCard(finding: Finding, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(CARD_BORDER, severityBorder(finding.severity), CARD_SHAPE),
        colors = panelColors(),
        cornerRadius = 20.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onToggle,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeverityGlyph(finding.severity)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = finding.rule, style = MiuixTheme.textStyles.body1)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = severitySummary(finding.severity),
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(severityLabel(finding.severity), severityColor(finding.severity))
                Spacer(Modifier.width(4.dp))
                Chevron(expanded)
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                DetailCard(icon = MiuixIcons.Lock, title = "说明", body = finding.detail)
                Spacer(Modifier.height(10.dp))
                DetailCard(
                    icon = MiuixIcons.Info,
                    title = "详情",
                    body = locationLine(finding),
                    tail = finding.file,
                )
            }
        }
    }
}

@Composable
private fun NormalCard(report: ScanReport, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = panelColors(),
        cornerRadius = 20.dp,
        pressFeedbackType = PressFeedbackType.Sink,
        onClick = onToggle,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateIcon(R.drawable.ic_state_ok, okColor(), GLYPH_SMALL)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "未发现风险特征", style = MiuixTheme.textStyles.body1)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "模块的行为都在正常范围内",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill("正常", okColor())
                Spacer(Modifier.width(4.dp))
                Chevron(expanded)
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                DetailCard(
                    icon = MiuixIcons.Lock,
                    title = "说明",
                    body = "逐条匹配模块内脚本与配置里的特征，并按组合行为与路径敏感度升级判定",
                )
                Spacer(Modifier.height(10.dp))
                DetailCard(
                    icon = MiuixIcons.Info,
                    title = "详情",
                    body = "共解析 ${report.fileCount} 个文件，未命中任何风险特征",
                )
            }
        }
    }
}

@Composable
private fun SeverityGlyph(severity: String) {
    val color = severityColor(severity)
    if (severity == "high" || severity == "medium") {
        StateIcon(R.drawable.ic_state_alert, color, GLYPH_SMALL)
    } else {
        Icon(
            imageVector = MiuixIcons.Info,
            contentDescription = null,
            modifier = Modifier.size(GLYPH_SMALL - 4.dp),
            tint = color,
        )
    }
}

@Composable
private fun Chevron(expanded: Boolean) {
    Icon(
        imageVector = if (expanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun StatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(PILL_SHAPE)
            .background(color.copy(alpha = 0.12f))
            .border(CARD_BORDER, color.copy(alpha = 0.45f), PILL_SHAPE)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text = text, style = MiuixTheme.textStyles.footnote1, color = color)
    }
}

@Composable
private fun DetailCard(icon: ImageVector, title: String, body: String, tail: String = "") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surface),
        cornerRadius = 16.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(text = body, style = MiuixTheme.textStyles.body1)
            if (tail.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tail,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun NotesCard(notes: List<String>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = panelColors(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            notes.forEach { note ->
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
private fun AboutScreen() {
    val context = LocalContext.current
    val version = remember(context) { appVersion(context) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = panelColors(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(text = stringResource(R.string.app_name), style = MiuixTheme.textStyles.title3)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "版本 $version",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Text(
                        text = "包名 ${context.packageName}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = panelColors(), cornerRadius = 20.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                    Text(text = "这个工具做什么", style = MiuixTheme.textStyles.body1)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "选一个模块 zip，在设备本地解析它的脚本、配置与二进制，按「特征表 + 升级表」判定风险。不联网、不需要 root、不安装任何东西。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(text = "免责说明", style = MiuixTheme.textStyles.body1)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "判定是启发式的，只作参考，不能替代人工审阅模块源码。",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }
    }
}

@Composable
private fun panelColors(): CardColors =
    CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainerHigh)

@Composable
private fun isDarkSurface(): Boolean = MiuixTheme.colorScheme.surface.luminance() <= 0.5f

@Composable
private fun warningColor(): Color = if (isDarkSurface()) WARNING_ON_DARK else WARNING_ON_LIGHT

@Composable
private fun okColor(): Color = if (isDarkSurface()) OK_ON_DARK else OK_ON_LIGHT

@Composable
private fun severityColor(severity: String): Color = when (severity) {
    "high" -> MiuixTheme.colorScheme.error
    "medium" -> warningColor()
    "low" -> okColor()
    else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
}

@Composable
private fun severityBorder(severity: String): Color = when (severity) {
    "high" -> MiuixTheme.colorScheme.error
    "medium" -> warningColor()
    else -> Color.Transparent
}

private fun severityLabel(severity: String) = when (severity) {
    "high" -> "高危"
    "medium" -> "中危"
    "low" -> "低危"
    else -> "信息"
}

private fun severitySummary(severity: String) = when (severity) {
    "high" -> "高危行为，建议不要安装"
    "medium" -> "可疑行为，需要人工确认"
    "low" -> "轻微风险，通常可以忽略"
    else -> "只是行为记录，不构成风险"
}

private fun statusTitle(state: ScanState): String = when (state) {
    ScanState.Idle -> "尚未选择模块"
    ScanState.Scanning -> "正在检查…"
    is ScanState.Failed -> "检查失败"
    is ScanState.Done -> when {
        state.report.high > 0 -> "发现高危风险"
        state.report.medium > 0 -> "存在可疑行为"
        else -> "未发现明显风险"
    }
}

private fun statusDetail(state: ScanState): String = when (state) {
    ScanState.Idle -> "只检查未安装的模块包，不解包安装、不改动设备"
    ScanState.Scanning -> "正在解析模块包内容"
    is ScanState.Failed -> state.message
    is ScanState.Done -> state.report.verdict
}

private fun statusCaption(state: ScanState): String {
    if (state !is ScanState.Done) return ""
    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(state.finishedAt))
    val module = if (state.report.moduleName.isNotBlank()) state.report.moduleName else state.report.moduleId
    val head = if (module.isNotBlank()) "模块 $module · " else ""
    return head + "上次检测 $time · 解析 ${state.report.fileCount} 个文件"
}

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