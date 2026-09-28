package com.lemon.mrs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemon.mrs.ui.component.FloatingBottomBar
import com.lemon.mrs.ui.component.FloatingBottomBarItem
import com.lemon.mrs.ui.component.bottombar.LocalMainPagerState
import com.lemon.mrs.ui.component.bottombar.rememberMainPagerState
import com.lemon.mrs.ui.screen.about.AboutScreen
import com.lemon.mrs.ui.screen.home.HomeActions
import com.lemon.mrs.ui.screen.home.HomePagerMiuix
import com.lemon.mrs.ui.screen.home.HomeUiState
import com.lemon.mrs.ui.screen.settings.SettingPagerMiuix
import com.lemon.mrs.ui.screen.sulog.SulogActions
import com.lemon.mrs.ui.screen.sulog.SulogScreenMiuix
import com.lemon.mrs.ui.screen.sulog.SulogScreenState
import com.lemon.mrs.ui.theme.LocalEnableBlur
import com.lemon.mrs.ui.theme.LocalEnableFloatingBottomBar
import com.lemon.mrs.ui.theme.LocalEnableFloatingBottomBarBlur
import com.lemon.mrs.ui.util.DisplaySettings
import com.lemon.mrs.ui.util.rememberBlurBackdrop
import com.lemon.mrs.ui.util.sulog.ScanEntryFields
import com.lemon.mrs.ui.util.sulog.SulogEntry
import com.lemon.mrs.ui.util.sulog.SulogEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private const val TAB_HOME = 0
private const val TAB_HISTORY = 1
private const val TAB_SETTINGS = 2
private const val TAB_COUNT = 3

/**
 * 扫描结果的数据形状：跟 C++ 核心吐出来的 JSON 一一对应，[parseReport] 负责解析。
 *
 * 界面这一轮整包换成 SukiSU 的骨架、三页的数据都先空着（用户要求），
 * 但扫描这一路（选包 -> nativeScanJson -> parseReport）留着没动：主页大卡片的动作
 * 已经接到选包上，只是结果暂时不显示。往后把 [ScanState] 接到新界面上即可。
 */
sealed interface ScanState {
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
    var aboutOpen by remember { mutableStateOf(false) }
    // 显示开关（磨砂玻璃 / 液态玻璃 / 预测性返回手势）住在主壳这一层：设置页读写它，
    // 关于页的返回要不要跟手也看它 —— 所以在这里建一份，两个分支共用同一个实例。
    val context = LocalContext.current
    val display = remember(context) { DisplaySettings(context) }
    // 主壳一直留在组合里，关于页是**盖在上面**的一层，不是把主壳换掉：
    // 上游是 push 一个导航条目盖住主屏（主屏并没有被销毁），本项目没有导航库，就用同一个 Box 叠一层。
    // 2026-09-28 之前这里是 if/else 替换，于是从关于页回来会重建主壳——底栏跳回主页、扫描结果也丢了。
    Box(modifier = Modifier.fillMaxSize()) {
        ScannerShell(scan = scan, onOpenAbout = { aboutOpen = true }, display = display)

        if (aboutOpen) {
            // 系统返回手势的进度（0..1）：由 AboutScreen 的 PredictiveBackHandler 喂上来（上游是
            // navigation3 的路由栈在做同一件事）。页面跟着手指往右滑，松手完成才真的关、取消就弹回。
            var aboutBackProgress by remember { mutableFloatStateOf(0f) }
            val aboutSlide by animateFloatAsState(
                targetValue = aboutBackProgress,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "aboutBackSlide",
            )
            // 关于页也有自己的取色与动态背景，同样得在主题里（原先它落在 MiuixTheme 的默认浅色上）。
            MiuixAppTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = size.width * aboutSlide }
                        .background(colorScheme.surface)
                        // 点按吞在自己这一层，别漏到底下的底栏上（上游的悬浮底栏也是这么干的）。
                        .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    AboutScreen(
                        onBack = { aboutOpen = false },
                        enablePredictiveBack = display.enablePredictiveBack,
                        onBackProgress = { aboutBackProgress = it },
                    )
                }
            }
        }
    }
}

/**
 * 本项目的 Miuix 主题：SukiSU 的**非莫奈**那一支（见 `knowledge/android/theme-non-monet-sukisu.md`）。
 *
 * 上游把主题套在整个 App 上，所以关于页也在主题里；本项目原先只在主壳 ScannerShell 里套，
 * 关于页于是落到 `MiuixTheme.colorScheme` 的默认值（`lightColorScheme()`）—— 深色模式下
 * 关于页底色发白、动态背景取的 `surface` 也不对。2026-09-28 起两个分支共用这一个入口。
 */
@Composable
private fun MiuixAppTheme(content: @Composable () -> Unit) {
    // 不取莫奈色，跟 SukiSU 默认那套一致：跟随系统明暗 + 它的默认调色板（TonalSpot）与色规（Spec2025）、
    // 不指定主色。这样主卡与各处取色走的是 SukiSU 非莫奈那一支（硬编码的绿卡 + primary 蓝）。
    MiuixTheme(
        controller = remember {
            ThemeController(
                colorSchemeMode = ColorSchemeMode.System,
                colorSpec = ThemeColorSpec.Spec2025,
                paletteStyle = ThemePaletteStyle.TonalSpot,
            )
        }
    ) { content() }
}

@Composable
private fun ScannerShell(
    scan: (String) -> String,
    onOpenAbout: () -> Unit,
    display: DisplaySettings,
) {
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    // 检查历史里显示的「扫的是哪个文件」——用 SAF 给的显示名，比缓存路径可读。
    var targetName by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 主页的数据还是空壳（本项目没有内核 / KernelSU 那套信息）。
    val homeState = remember { HomeUiState() }

    // 检查历史 = 本项目自己的扫描记录：一行一条 JSON 存在 filesDir，进页面时读一次。
    // 记录在扫描成功后就写盘，这里只负责读出来并映射成 SU 日志列表能渲染的条目。
    var history by remember { mutableStateOf<List<ScanRecord>>(emptyList()) }
    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) { ScanHistory.load(context) }
    }
    val sulogState = remember(history) {
        SulogScreenState(entries = history.map(::scanRecordToSulogEntry))
    }

    fun runScan(path: String) {
        state = ScanState.Scanning
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { parseReport(scan(path)) }
            }
            state = outcome.fold(
                onSuccess = { report ->
                    val finishedAt = System.currentTimeMillis()
                    val record = ScanHistory.of(report, targetName, finishedAt)
                    history = withContext(Dispatchers.IO) { ScanHistory.append(context, record) }
                    ScanState.Done(report, finishedAt)
                },
                onFailure = { ScanState.Failed(it.message ?: "扫描失败") },
            )
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val path = runCatching { MainActivity.copyToCache(context, uri) }.getOrNull()
        if (path == null) {
            state = ScanState.Failed("无法读取所选文件")
            return@rememberLauncherForActivityResult
        }
        targetName = MainActivity.displayName(context, uri) ?: uri.lastPathSegment ?: "所选文件"
        runScan(path)
    }

    val homeActions = remember(picker) {
        HomeActions(
            onInstallClick = { picker.launch(arrayOf("*/*")) },
            onOpenUrl = { url -> openLink(context, url) },
        )
    }
    val sulogActions = remember(context) {
        SulogActions(
            // 清空是不可逆的：界面那边先弹确认，确认后才走到这里。
            onCleanFile = {
                ScanHistory.clear(context)
                history = emptyList()
            },
        )
    }

    // 手指滑 pager 时把底栏高亮同步过去（版式照 SukiSU 的 MainScreen）。
    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    // 取色口径见 MiuixAppTheme。
    MiuixAppTheme {
        val enableBlur = display.enableBlur
        val enableGlass = display.enableFloatingBottomBar
        // 磨砂玻璃：顶栏底下那层要模糊的内容（设备不支持 RenderEffect 时拿到 null，顶栏退化成纯色）。
        val blurBackdrop = rememberBlurBackdrop(enableBlur)
        // 液态玻璃：底栏胶囊要折射「下面的内容」，所以主内容这一层得先录进一个 backdrop
        // （上游 MainScreen 的写法：blurBackdrop 挂外层 Box、backdrop 挂 Pager）。
        val surfaceColor = colorScheme.surface
        val backdrop = rememberLayerBackdrop {
            drawRect(surfaceColor)
            drawContent()
        }
        CompositionLocalProvider(
            LocalMainPagerState provides mainPagerState,
            LocalEnableBlur provides enableBlur,
            LocalEnableFloatingBottomBar provides enableGlass,
            LocalEnableFloatingBottomBarBlur provides display.enableFloatingBottomBarBlur,
        ) {
            Scaffold(
                bottomBar = {
                    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        FloatingBottomBar(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    start = 28.dp,
                                    end = 28.dp,
                                    bottom = if (navInset != 0.dp) 8.dp + navInset else 28.dp,
                                ),
                            selectedIndex = mainPagerState.selectedPage,
                            onSelected = { mainPagerState.animateToPage(it) },
                            backdrop = backdrop,
                            tabsCount = TAB_COUNT,
                            // 液态玻璃关掉时退回上游的纯色那一支（isBlurEnabled = false）。
                            isBlurEnabled = enableGlass,
                        ) { activateTab ->
                            BottomTab(
                                selected = mainPagerState.selectedPage == TAB_HOME,
                                icon = Icons.Rounded.Cottage,
                                label = "主页",
                                onClick = { activateTab(TAB_HOME) },
                            )
                            BottomTab(
                                selected = mainPagerState.selectedPage == TAB_HISTORY,
                                icon = Icons.Rounded.History,
                                label = "检查历史",
                                onClick = { activateTab(TAB_HISTORY) },
                            )
                            BottomTab(
                                selected = mainPagerState.selectedPage == TAB_SETTINGS,
                                icon = Icons.Rounded.Settings,
                                label = "设置",
                                onClick = { activateTab(TAB_SETTINGS) },
                            )
                        }
                    }
                },
            ) { innerPadding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier)
                        .then(
                            if (enableGlass && display.enableFloatingBottomBarBlur) {
                                Modifier.layerBackdrop(backdrop)
                            } else {
                                Modifier
                            },
                        ),
                    overscrollEffect = null,
                ) { page ->
                    val bottomInnerPadding = innerPadding.calculateBottomPadding()
                    when (page) {
                        TAB_HOME -> HomePagerMiuix(
                            state = homeState,
                            actions = homeActions,
                            bottomInnerPadding = bottomInnerPadding,
                            scanState = state,
                        )

                        TAB_HISTORY -> SulogScreenMiuix(
                            state = sulogState,
                            actions = sulogActions,
                            bottomInnerPadding = bottomInnerPadding,
                        )

                        else -> SettingPagerMiuix(
                            onOpenAbout = onOpenAbout,
                            display = display,
                            bottomInnerPadding = bottomInnerPadding,
                        )
                    }
                }
            }
        }
    }
}

private fun openLink(context: Context, url: String) {
    if (url.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

/** 底栏的一根：图标在上、文字在下（跟 SukiSU 的 BottomBarMiuix 一致）。 */
@Composable
private fun RowScope.BottomTab(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    FloatingBottomBarItem(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 76.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Text(
            text = label,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}

/**
 * 每一页自己的壳：顶栏 + 一条 LazyColumn。
 * 版式照 SukiSU 的 HomePagerMiuix / SettingPagerMiuix——页面自己拿 Scaffold 与 TopAppBar，
 * 列表左右留 12dp，列表底部给悬浮底栏留出 [bottomInnerPadding]。
 */
@Composable
internal fun PageScaffold(
    title: String,
    bottomInnerPadding: Dp,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            content()
            item { Spacer(Modifier.height(bottomInnerPadding + 12.dp)) }
        }
    }
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

/**
 * 一条检查记录 -> 一个 SU 日志列表能渲染的条目。
 * 列表骨架是移植件（SulogListMiuix.kt），形状不动，映射全在这一层做：
 * 标题放模块名、描述放扫描对象、三个标签放高·中·低危计数、尾部状态放结论。
 */
private fun scanRecordToSulogEntry(record: ScanRecord): SulogEntry {
    val title = record.moduleName.ifBlank { record.moduleId }
    val time = formatScanTime(record.finishedAt)
    return SulogEntry(
        key = record.id,
        eventType = SulogEventType.ScanReport,
        rawLine = record.verdict,
        timestampText = time,
        fields = linkedMapOf(
            ScanEntryFields.MODULE to title,
            ScanEntryFields.TARGET to record.target,
            ScanEntryFields.HIGH to record.high.toString(),
            ScanEntryFields.MEDIUM to record.medium.toString(),
            ScanEntryFields.LOW to record.low.toString(),
            ScanEntryFields.VERDICT to record.verdict,
            "版本" to record.version,
            "包名" to record.moduleId,
            "作者" to record.author,
            "文件数" to record.fileCount.toString(),
            "信息" to record.info.toString(),
            "发现" to record.findings.size.toString(),
            "时间" to time,
        ).filterValues { it.isNotBlank() },
    )
}

private fun formatScanTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))