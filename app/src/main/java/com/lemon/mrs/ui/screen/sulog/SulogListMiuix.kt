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
//      再加一段正文（发现逐条 + 提示，ScanUi.kt 的 scanRecordDetail 生成、走 SulogEntry.extraDetail），
//      主页那边只留一张按风险着色的结论卡。
//   ⑧ 2026-09-29：检查条目的右侧状态由「结论」改成「点击查看详情」——结论本身挪进详情弹窗，
//      卡片右侧只当点击提示（用户要求「右侧写点击查看详情」）。
package com.lemon.mrs.ui.screen.sulog

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.lemon.mrs.PageScaffold
import com.lemon.mrs.R
import com.lemon.mrs.ui.component.statustag.StatusTag
import com.lemon.mrs.ui.util.sulog.ScanEntryFields
import com.lemon.mrs.ui.util.sulog.SulogEntry
import com.lemon.mrs.ui.util.sulog.SulogEventType
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
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
) {
    var selectedEntry by remember { mutableStateOf<SulogEntry?>(null) }
    var clearConfirming by remember { mutableStateOf(false) }

    SulogDetailDialog(
        show = selectedEntry != null,
        entry = selectedEntry,
        onDismiss = { selectedEntry = null },
    )

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
            onEntryClick = { selectedEntry = it },
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
        val layoutDirection = LocalLayoutDirection.current
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
            sulogEntryStatus(entry)?.let {
                Text(
                    text = it,
                    color = colorScheme.onSurfaceVariantActions,
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
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
    }
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

@Composable
private fun SulogDetailDialog(
    show: Boolean,
    entry: SulogEntry?,
    onDismiss: () -> Unit,
) {
    var lastEntry by remember { mutableStateOf(entry) }
    if (entry != null) lastEntry = entry
    val displayEntry = lastEntry ?: return
    OverlayDialog(
        show = show,
        title = sulogEntryTitle(displayEntry),
        onDismissRequest = onDismiss,
        content = {
            Column {
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = sulogEntryDetailText(displayEntry),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    // 本项目自己的检查结果：发现逐条 + 提示（由 ScanUi.kt 填进 extraDetail）。
                    displayEntry.extraDetail?.let { detail ->
                        Text(
                            text = detail,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(android.R.string.ok),
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        },
    )
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