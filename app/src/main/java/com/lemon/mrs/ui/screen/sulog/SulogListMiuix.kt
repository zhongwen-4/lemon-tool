// 移植自 SukiSU Ultra（GPL-3.0）：
//   上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogMiuix.kt
//   上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/sulog/SulogScreen.kt
// 改动（改动日期：2026-09-28）：
//   ① 只搬「SU 日志列表」这一块：列表段 sulogEntriesSection、条目卡 SulogEntryCard、
//      条目详情弹窗 SulogDetailDialog、空/错提示卡 SulogMessageCard，以及取条目标题 /
//      描述 / 标签 / 详情文本 / 返回值的几个小函数。
//   ② 上游那一页的顶栏（返回、清空日志、按类型筛选）、搜索框 SearchBox / SearchPager /
//      SearchBarFake、日志文件下拉 OverlayDropdownPreference、下拉刷新 PullToRefresh、
//      SulogStatusSection 状态提示卡，以及一切毛玻璃（rememberBlurBackdrop / BlurredBar /
//      layerBackdrop）都不要——用户要求「其他的组件不要」。
//   ③ 页面外壳改成项目自己的 PageScaffold（顶栏 + 一条 LazyColumn + 给悬浮底栏留白），
//      跟主页 / 设置页同一套。
//   ④ 列表渲染 state.entries：搜索与筛选这一路没搬，SulogScreenState.visibleEntries
//      在本项目里没有生产者。往后接真实日志时填 entries 即可。
//   ⑤ 条目卡尾部那个箭头：上游用 MiuixIcons.Basic.ArrowRight，但本项目锁的
//      miuix-icons 0.9.4 只有 top.yukonga.miuix.kmp.icon.extended 一个包（没有 basic），
//      这里改用同语义的 MiuixIcons.ChevronForward。
//   ⑥ 2026-09-28 接上「检查历史」：本项目的历史是扫描记录、不是 SU 日志，所以
//      ① 顶栏标题换成「检查历史」（R.string.scan_history，不再叫「SU 日志」）；
//      ② 条目类型加了 SulogEventType.ScanReport 一支，四个取值小函数各补一个分支
//         （模块名 / 扫描对象 / 高·中·低计数 / 点击查看详情）；sulogEntrySummaryTags 因此改成 @Composable
//         （标签要读 string 资源）；
//      ③ 空历史给一张提示卡；列表非空时底部加一行「清空检查历史」——点了先弹确认，
//         确认后才走 actions.onCleanFile（清空是不可逆的，不做静默删除）。
//   ⑦ 2026-09-29 用户口径：「检查模块的结果全部放进检查历史」——条目详情里除了 fields 那几行，
//      再加一段正文（发现逐条 + 提示，ScanUi.kt 的 scanRecordDetail 生成、走 SulogEntry.scanDetail ——
//      ⑩ 之后那段正文改成列表渲染：extraDetail 这个字符串字段已换成结构化的 scanDetail），
//      主页那边只留一张按风险着色的结论卡。
//   ⑧ 2026-09-29：检查条目的右侧状态由「结论」改成「点击查看详情」——结论本身挪进详情弹窗，
//      卡片右侧只当点击提示（用户要求「右侧写点击查看详情」）。
//   ⑨ 2026-09-29：检查条目的卡片改成把「名称 / 路径 / 时间 / 标签」四样显式写出来（见 ScanEntryRows），
//      第二行的「路径」放文件名；上游那套不写标签的版式原样保留成 SulogEntryRows，给将来别的日志类型用。
//   ⑩ 2026-09-30 用户要「把检查历史卡片的详情改为列表，同样是 SU 日志的样式」：详情弹窗里
//      检查条目那段等宽的「键: 值」正文换成**列表**（ScanDetailList）—— 概览一张 SU 日志式的卡、
//      每条发现一张卡（规则 / 文件 / 说明 / 等级标签）、每条提示一张卡；数据走 SulogEntry.scanDetail
//      （结构化的，不再是一段拼好的文本）。上游那种日志条目的详情仍是一段等宽正文，照上游不动。
//   ⑫ 2026-10-01 用户要「每个检查详情最上面写一行红色的卡片：该结果仅供参考」——详情整页的内容最上面
//      加一张提示卡：**白底**（复用 `DetailCard` 的默认卡色，与其它卡同一套）+ **红字**
//      （0xFFF72727，与主页结论卡、详情等级 chip 同一个红），文案 `scan_detail_disclaimer`。
//      用户当天追加口径「卡片是白的，字是红的」—— 上游 `WarningCard` 那种红底红字他不要，所以没用它。
//      卡片下面才是概览 / 每条发现 / 每条提示那一列卡。
//   ⑪ 2026-09-30 用户口径：「检查历史的列表改回去，单个卡片点进去进入另一个列表」——
//      ① 条目卡退回上一版（0.11.4）的版式：不管什么类型都走 SulogEntryRows（上游那套不写标签的
//         标题 / 描述 / 时间 / 标签 chips），0.11.5 那套显式四行「名称 / 路径 / 时间 / 标签」整段删掉
//         （ScanEntryRows 已删；详情的概览卡 / 发现卡里那几行「标签 + 值」仍走 ScanEntryLine / ScanEntryLabel）；
//      ② 条目详情由弹窗改成**整页** SulogDetailScreen（骨架照关于页：SmallTopAppBar + 返回箭头 + 可滚内容），
//         由 ScanUi.ScannerShell 盖在主壳上、跟手往右滑，返回手势与关于页同一套口径（受「预测性返回手势」开关管）。
package com.lemon.mrs.ui.screen.sulog

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import com.lemon.mrs.PageScaffold
import com.lemon.mrs.R
import com.lemon.mrs.ui.component.statustag.StatusTag
import com.lemon.mrs.ui.theme.isInDarkTheme
import com.lemon.mrs.ui.util.sulog.ScanDetailFinding
import com.lemon.mrs.ui.util.sulog.ScanEntryFields
import com.lemon.mrs.ui.util.sulog.SulogEntry
import com.lemon.mrs.ui.util.sulog.SulogEventType
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/** 检查历史页：顶栏 + 一列检查记录，点一条开详情弹窗。数据从 [state] 来，默认是空的。 */
@Composable
fun SulogScreenMiuix(
    state: SulogScreenState,
    actions: SulogActions,
    bottomInnerPadding: Dp,
    onEntryClick: (SulogEntry) -> Unit,
) {
    var clearConfirming by remember { mutableStateOf(false) }
    SulogClearConfirmDialog(
        show = clearConfirming,
        onDismiss = { clearConfirming = false },
        onConfirm = {
            clearConfirming = false
            actions.onCleanFile()
        },
    )

    PageScaffold(title = stringResource(R.string.scan_history), bottomInnerPadding = bottomInnerPadding) {
        sulogEntriesSection(
            entries = state.entries,
            errorMessage = state.errorMessage,
            onEntryClick = onEntryClick,
            onClearClick = { clearConfirming = true },
        )
    }
}

private fun LazyListScope.sulogEntriesSection(
    entries: List<SulogEntry>,
    errorMessage: String?,
    onEntryClick: (SulogEntry) -> Unit,
    onClearClick: () -> Unit,
) {
    when {
        errorMessage != null -> item {
            SulogMessageCard(
                modifier = Modifier.fillParentMaxSize(),
                title = stringResource(R.string.sulog_failed_to_load),
                summary = errorMessage,
            )
        }

        entries.isEmpty() -> item {
            SulogMessageCard(
                modifier = Modifier.fillParentMaxSize(),
                title = stringResource(R.string.scan_history_empty),
                summary = stringResource(R.string.scan_history_empty_summary),
            )
        }

        else -> {
            itemsIndexed(entries, key = { index, entry -> "$index-${entry.key}" }) { _, entry ->
                SulogEntryCard(
                    entry = entry,
                    onClick = { onEntryClick(entry) },
                )
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    ArrowPreference(
                        title = stringResource(R.string.scan_history_clear),
                        summary = stringResource(R.string.scan_history_clear_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.DeleteSweep,
                                contentDescription = stringResource(R.string.scan_history_clear),
                                modifier = Modifier.padding(end = 6.dp),
                                tint = colorScheme.onBackground,
                            )
                        },
                        onClick = onClearClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SulogEntryCard(
    entry: SulogEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(16.dp),
    ) {
        // 条目卡正文：退回上一版的版式（用户 2026-09-30 定）—— 照上游 SU 日志那套，不写标签。
        SulogEntryRows(entry)
    }
}

/** 条目卡正文：照上游 SU 日志条目那一套 —— 标题 / 描述 / 时间 + 一行标签 chips，右侧状态 + 箭头。 */
@Composable
private fun SulogEntryRows(entry: SulogEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = sulogEntryTitle(entry),
                modifier = Modifier.basicMarquee(),
                fontWeight = FontWeight(550),
                color = colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            sulogEntryDescription(entry)?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            entry.timestampText?.let {
                Text(
                    text = it,
                    modifier = Modifier.basicMarquee(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val colors = listOf(
                    colorScheme.primary to colorScheme.onPrimary,
                    colorScheme.secondaryContainer to colorScheme.onSecondaryContainer,
                    colorScheme.tertiaryContainer to colorScheme.onTertiaryContainer,
                )
                sulogEntrySummaryTags(entry).forEachIndexed { index, tag ->
                    val (bg, fg) = colors.getOrElse(index) { colors.last() }
                    StatusTag(label = tag, backgroundColor = bg, contentColor = fg)
                }
            }
        }
        sulogEntryStatus(entry)?.let { SulogEntryStatusText(it) }
        SulogEntryChevron()
    }
}

/** 「名称 / 路径 / 时间 / 标签」这四个标签都是两个字，宽度天然一致，四行的值就左对齐了。 */
@Composable
private fun ScanEntryLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(end = 10.dp),
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary,
        maxLines = 1,
        softWrap = false,
    )
}

/** 一行「标签 + 值」。 */
@Composable
private fun ScanEntryLine(
    label: String,
    value: String,
    valueFontWeight: FontWeight? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ScanEntryLabel(label)
        Text(
            text = value,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(),
            fontSize = 13.sp,
            fontWeight = valueFontWeight,
            color = colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** 卡片右侧那句状态（检查条目现为「点击查看详情」）。 */
@Composable
private fun SulogEntryStatusText(text: String) {
    Text(
        text = text,
        color = colorScheme.onSurfaceVariantActions,
        fontSize = 12.sp,
        fontWeight = FontWeight(550),
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.padding(start = 16.dp),
    )
}

/** 卡片尾部那枚箭头（RTL 下翻个面）。 */
@Composable
private fun SulogEntryChevron() {
    val layoutDirection = LocalLayoutDirection.current
    Image(
        modifier = Modifier
            .graphicsLayer {
                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
            }
            .padding(start = 8.dp)
            .size(width = 10.dp, height = 16.dp),
        imageVector = MiuixIcons.ChevronForward,
        contentDescription = null,
        colorFilter = ColorFilter.tint(colorScheme.onSurfaceVariantActions),
    )
}

@Composable
private fun SulogMessageCard(
    modifier: Modifier,
    title: String,
    summary: String?,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight(550),
                color = colorScheme.onSurfaceVariantSummary,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 条目详情的**整页**（用户 2026-09-30 定：点一条卡「进另一个列表」，不再是弹窗）。
 * 版式照关于页那套整页：SmallTopAppBar（返回箭头）+ 一条可滚的内容；
 * 返回手势同一套口径（见 knowledge/android/overlay-page-and-predictive-back.md）：
 * 开关打开走 PredictiveBackHandler（页面跟着手指往右滑出），关掉走 BackHandler，两条只挂一条。
 */
@Composable
internal fun SulogDetailScreen(
    entry: SulogEntry,
    onBack: () -> Unit,
    enablePredictiveBack: Boolean = true,
    onBackProgress: (Float) -> Unit = {},
) {
    if (enablePredictiveBack) {
        PredictiveBackHandler(enabled = true) { progress ->
            try {
                progress.collect { event -> onBackProgress(event.progress) }
                onBackProgress(0f)
                onBack()
            } catch (e: CancellationException) {
                // 手势半路松手取消：页面弹回原位，不关。
                onBackProgress(0f)
            }
        }
    } else {
        // 普通返回：先把进度清零，免得页面留在「跟手滑到一半」的位置上。
        LaunchedEffect(Unit) { onBackProgress(0f) }
        BackHandler { onBack() }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = sulogEntryTitle(entry),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        val layoutDirection = LocalLayoutDirection.current
                        Icon(
                            modifier = Modifier.graphicsLayer {
                                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                            },
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.systemBars
            .add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        SelectionContainer(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .padding(bottom = 24.dp),
        ) {
            Column {
                // 检查详情最上面那行提示卡（用户 2026-10-01 定）：白底（跟其它卡同一套默认底色）+ 红字。
                DetailCard {
                    Text(
                        text = stringResource(R.string.scan_detail_disclaimer),
                        fontSize = 14.sp,
                        fontWeight = FontWeight(550),
                        color = Color(0xFFF72727),
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (entry.eventType == SulogEventType.ScanReport) {
                    // 检查条目的详情是**列表**（用户 2026-09-30 定）：概览一张、每条发现一张、
                    // 每条提示一张，都照 SU 日志条目卡的样式。
                    ScanDetailList(entry)
                } else {
                    // 上游 SU 日志条目的详情：一段等宽的「键: 值」正文（照上游不动）。
                    Text(
                        text = sulogEntryDetailText(entry),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

/** 检查条目的详情列表：概览卡 + 每条发现一张卡 + 每条提示一张卡（+ 截断那张）。 */
@Composable
private fun ScanDetailList(entry: SulogEntry) {
    val detail = entry.scanDetail
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScanOverviewCard(entry)
        detail?.findings?.forEach { finding -> ScanFindingCard(finding) }
        detail?.notes?.forEachIndexed { index, note ->
            ScanNoteCard(title = stringResource(R.string.scan_note_title, index + 1), body = note)
        }
        if (detail?.truncated == true) {
            ScanNoteCard(title = stringResource(R.string.scan_truncated), body = null)
        }
    }
}

/**
 * 概览卡：与列表条目卡同一套（标题 + 若干「标签 + 值」的行 + 一行标签 chips），底部补一句结论。
 * 详情改成列表之后，原先那段「键: 值」正文里的东西一样不少地落在这张卡上。
 */
@Composable
private fun ScanOverviewCard(entry: SulogEntry) {
    val fields = entry.fields
    DetailCard {
        ScanEntryLine(
            label = stringResource(R.string.scan_entry_name),
            value = sulogEntryTitle(entry),
            valueFontWeight = FontWeight(550),
        )
        fields[ScanEntryFields.TARGET]?.let {
            ScanEntryLine(label = stringResource(R.string.scan_entry_path), value = it)
        }
        entry.timestampText?.let {
            ScanEntryLine(label = stringResource(R.string.scan_entry_time), value = it)
        }
        fields[ScanEntryFields.VERSION]?.let {
            ScanEntryLine(label = ScanEntryFields.VERSION, value = it)
        }
        fields[ScanEntryFields.PACKAGE]?.let {
            ScanEntryLine(label = ScanEntryFields.PACKAGE, value = it)
        }
        fields[ScanEntryFields.AUTHOR]?.let {
            ScanEntryLine(label = ScanEntryFields.AUTHOR, value = it)
        }
        fields[ScanEntryFields.FILES]?.let {
            ScanEntryLine(label = ScanEntryFields.FILES, value = it)
        }
        fields[ScanEntryFields.FINDINGS]?.let {
            ScanEntryLine(label = ScanEntryFields.FINDINGS, value = it)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScanEntryLabel(stringResource(R.string.scan_entry_tags))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ScanCountTags(entry)
            }
        }
        fields[ScanEntryFields.VERDICT]?.let {
            ScanEntryWrappedLine(label = ScanEntryFields.VERDICT, value = it)
        }
    }
}

/** 一条发现一张卡：标题是规则名、右侧是「第几条 / 共几条」，下面依次是文件、说明与等级标签。 */
@Composable
private fun ScanFindingCard(finding: ScanDetailFinding) {
    DetailCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = finding.rule,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(),
                fontSize = 15.sp,
                fontWeight = FontWeight(550),
                color = colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            SulogEntryStatusText("${finding.position}/${finding.total}")
        }
        if (finding.file.isNotBlank()) {
            ScanEntryLine(
                label = stringResource(R.string.scan_finding_file),
                value = finding.file + if (finding.line > 0) ":" + finding.line else "",
            )
        }
        if (finding.detail.isNotBlank()) {
            ScanEntryWrappedLine(
                label = stringResource(R.string.scan_finding_detail),
                value = finding.detail,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScanEntryLabel(stringResource(R.string.scan_entry_tags))
            ScanSeverityTag(label = finding.severityLabel, severity = finding.severity)
        }
    }
}

/** 提示 / 截断那种卡：一行标题，外加一行可选的正文。 */
@Composable
private fun ScanNoteCard(title: String, body: String?) {
    DetailCard {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight(550),
            color = colorScheme.onSurface,
        )
        if (!body.isNullOrBlank()) {
            Text(
                text = body,
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 详情列表里的一张卡：与列表条目卡同一种（默认底色 + 16dp 内边距），这样两张列表看起来是一套。 */
@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

/**
 * 「标签 + 值」的一行，值**可以换行** —— 说明与结论这种长文本用它；
 * 上面那个 [ScanEntryLine] 带 marquee 且只允许一行，给短值用。
 */
@Composable
private fun ScanEntryWrappedLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        ScanEntryLabel(label)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            color = colorScheme.onSurface,
        )
    }
}

/** 概览卡那几枚计数标签：只写非零的那几档，一档都没有就写「未发现风险项」。 */
@Composable
private fun ScanCountTags(entry: SulogEntry) {
    val counts = listOf(
        "high" to (stringResource(R.string.scan_severity_high) to entry.fields[ScanEntryFields.HIGH]),
        "medium" to (stringResource(R.string.scan_severity_medium) to entry.fields[ScanEntryFields.MEDIUM]),
        "low" to (stringResource(R.string.scan_severity_low) to entry.fields[ScanEntryFields.LOW]),
        "info" to (stringResource(R.string.scan_severity_info) to entry.fields[ScanEntryFields.INFO]),
    ).mapNotNull { (severity, pair) ->
        val count = pair.second
        if (count.isNullOrBlank() || count == "0") null else severity to (pair.first + " " + count)
    }
    if (counts.isEmpty()) {
        ScanSeverityTag(label = stringResource(R.string.scan_no_findings), severity = "low")
    } else {
        counts.forEach { (severity, label) -> ScanSeverityTag(label = label, severity = severity) }
    }
}

/** 一枚按等级取色的标签（主页结论卡那套红 / 黄 / 绿；信息走主题的次级容器色）。 */
@Composable
private fun ScanSeverityTag(label: String, severity: String) {
    val dark = isInDarkTheme()
    val (background, content) = when (severity) {
        "high" -> (if (dark) Color(0xFF310808) else Color(0xFFF8E2E2)) to Color(0xFFF72727)
        "medium" -> (if (dark) Color(0xFF3E2F1B) else Color(0xFFFFF0DB)) to Color(0xFFF5A623)
        "low" -> (if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)) to Color(0xFF36D167)
        else -> colorScheme.secondaryContainer to colorScheme.onSecondaryContainer
    }
    StatusTag(label = label, backgroundColor = background, contentColor = content)
}

@Composable
private fun sulogEntryTitle(entry: SulogEntry): String {
    return when (entry.eventType) {
        SulogEventType.RootExecve -> entry.fields["comm"] ?: stringResource(R.string.sulog_filter_root_execve)
        SulogEventType.SuCompat -> stringResource(R.string.sulog_filter_sucompat)
        SulogEventType.IoctlGrantRoot -> stringResource(R.string.sulog_filter_ioctl_grant_root)
        SulogEventType.DaemonEvent -> stringResource(R.string.sulog_filter_daemon_restart)
        SulogEventType.Dropped -> "Dropped"
        SulogEventType.ScanReport -> entry.fields[ScanEntryFields.MODULE]?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.scan_result)
        SulogEventType.Unknown -> entry.fields["type"]?.replace('_', ' ')?.replaceFirstChar(Char::uppercase) ?: "Unknown"
    }
}

@Composable
private fun sulogEntryDescription(entry: SulogEntry): String? {
    return when (entry.eventType) {
        SulogEventType.DaemonEvent -> entry.fields["boot_id"]?.let { "Boot ID: $it" }
        SulogEventType.Dropped -> entry.fields["ts_ns"]?.let { "Timestamp: $it" }
        SulogEventType.ScanReport -> entry.fields[ScanEntryFields.TARGET]
        else -> entry.fields["argv"] ?: entry.fields["file"]
    }
}

@Composable
private fun sulogEntrySummaryTags(entry: SulogEntry): List<String> {
    val comm = entry.fields["comm"]
    val pid = entry.fields["pid"]
    val uid = entry.fields["uid"]
    return when (entry.eventType) {
        SulogEventType.DaemonEvent -> listOfNotNull(entry.fields["restart"]?.let { "Restart #$it" } ?: "Daemon restarted")
        SulogEventType.Dropped -> listOfNotNull(entry.fields["dropped"]?.let { "$it lost" })
        SulogEventType.ScanReport -> listOf(
            "${stringResource(R.string.scan_severity_high)} ${entry.fields[ScanEntryFields.HIGH].orEmpty()}",
            "${stringResource(R.string.scan_severity_medium)} ${entry.fields[ScanEntryFields.MEDIUM].orEmpty()}",
            "${stringResource(R.string.scan_severity_low)} ${entry.fields[ScanEntryFields.LOW].orEmpty()}",
        )
        else -> listOfNotNull(comm?.takeIf { it.isNotBlank() }, pid?.let { "PID $it" }, uid?.let { "UID $it" })
    }
}

private fun sulogEntryDetailText(entry: SulogEntry) = buildAnnotatedString {
    entry.fields.entries.forEachIndexed { index, (key, value) ->
        if (index > 0) append('\n')
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("$key: ")
        }
        append(value)
    }
}

@Composable
private fun sulogEntryStatus(entry: SulogEntry): String? {
    // 本项目的检查条目：右侧不写结论（结论在详情里），只提示可以点开看详情（用户 2026-09-29 定）。
    if (entry.eventType == SulogEventType.ScanReport) return stringResource(R.string.scan_entry_view_detail)
    return entry.fields["retval"]?.toIntOrNull()?.let { retval -> if (retval == 0) "Success" else "Exit $retval" }
}

/** 清空检查历史前的确认弹窗（清空不可逆，不做静默删除）。 */
@Composable
private fun SulogClearConfirmDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(R.string.scan_history_clear),
        onDismissRequest = onDismiss,
        content = {
            Column {
                Text(
                    text = stringResource(R.string.scan_history_clear_summary),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(android.R.string.cancel),
                        onClick = onDismiss,
                    )
                    TextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.scan_history_clear),
                        onClick = onConfirm,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                }
            }
        },
    )
}