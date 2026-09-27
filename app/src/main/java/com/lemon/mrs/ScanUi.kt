package com.lemon.mrs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
    var aboutOpen by remember { mutableStateOf(false) }
    if (aboutOpen) {
        AboutScreen(onBack = { aboutOpen = false })
    } else {
        ScannerShell(scan = scan, onOpenAbout = { aboutOpen = true })
    }
}

@Composable
private fun ScannerShell(scan: (String) -> String, onOpenAbout: () -> Unit) {
    var state by remember { mutableStateOf<ScanState>(ScanState.Idle) }
    var lastPath by remember { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val mainPagerState = rememberMainPagerState(pagerState)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 三页的骨架状态：全是空数据，往后接真实数据就换这里。
    val homeState = remember { HomeUiState() }
    val sulogState = remember { SulogScreenState() }

    fun runScan(path: String) {
        lastPath = path
        state = ScanState.Scanning
        scope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { parseReport(scan(path)) }
            }
            state = outcome.fold(
                onSuccess = { report -> ScanState.Done(report, System.currentTimeMillis()) },
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

    val homeActions = remember(picker) {
        HomeActions(
            onInstallClick = { picker.launch(arrayOf("*/*")) },
            onOpenUrl = { url -> openLink(context, url) },
        )
    }
    val sulogActions = remember { SulogActions() }

    // 手指滑 pager 时把底栏高亮同步过去（版式照 SukiSU 的 MainScreen）。
    LaunchedEffect(pagerState.currentPage) {
        mainPagerState.syncPage()
    }

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
    ) {
        CompositionLocalProvider(LocalMainPagerState provides mainPagerState) {
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
                            tabsCount = TAB_COUNT,
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
                    modifier = Modifier.fillMaxSize(),
                    overscrollEffect = null,
                ) { page ->
                    val bottomInnerPadding = innerPadding.calculateBottomPadding()
                    when (page) {
                        TAB_HOME -> HomePagerMiuix(
                            state = homeState,
                            actions = homeActions,
                            bottomInnerPadding = bottomInnerPadding,
                        )

                        TAB_HISTORY -> SulogScreenMiuix(
                            state = sulogState,
                            actions = sulogActions,
                            bottomInnerPadding = bottomInnerPadding,
                        )

                        else -> SettingPagerMiuix(
                            onOpenAbout = onOpenAbout,
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