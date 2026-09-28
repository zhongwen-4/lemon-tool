// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeMiuix.kt
// 改动（改动日期：2026-09-28）：
//   ① 只改包名与 import；Scaffold / TopAppBar / LazyColumn / UpdateCard 的结构、尺寸、配色与上游一字不差。
//   ② 数据先空着：HomeUiState 全默认（无内核信息、无更新信息），相应的卡片渲染出来是空的。
//   ③ 去内核依赖：Natives.isFullFeatured() 的底边距判断改成直接用 bottomInnerPadding；
//      Natives / KernelVersion 的取数、Preview 相关的 import 与预览块删除。
//   ④ 去毛玻璃：LayerBackdrop / layerBackdrop 不搬（miuix-blur 要求 minSdk 33），
//      backdrop 恒为 null，BlurredBar 退化成纯色（见 ui/util/BlurExt.kt）。
//   ⑤ 按用户口径精简主页，组件骨架保留、只改内容：
//      - StatusCard（上游的「工作中 / 内核支持 / 不支持」三分支）改名 CheckEntryCard 且只留一支：
//        标题「不支持」换成「点此开始检测」，动作仍是选模块 zip；另两支是 KernelSU 内核取数，本项目没有数据源。
//      - InfoCard 只留一行「应用版本」（版本号从本机 PackageManager 取），上游那一堆内核/设备信息
//        与 SELinux + Seccomp 两张信息卡全删。
//      - SupportLinks（支持开发 / 了解 KernelSU）换成一行「提交 BUG」，指向本项目 issues。
//   ⑥ 2026-09-28 接上「检查模块」：新增 ScanSummarySection（检测中转圈 / 失败报错 / 成功出结论卡）
//      与 scanFindingsSection（发现逐条一卡，按等级配色）——扫描本身在 ScanUi.kt 里跑，
//      这里只负责把 ScanState 画出来。findings 走 LazyColumn 的独立 item，
//      这样几百条也是懒加载，不会把首屏撑爆。
package com.lemon.mrs.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lemon.mrs.Finding
import com.lemon.mrs.R
import com.lemon.mrs.ScanState
import com.lemon.mrs.ui.component.WarningLevel
import com.lemon.mrs.ui.component.dialog.rememberConfirmDialog
import com.lemon.mrs.ui.component.miuix.WarningCard
import com.lemon.mrs.ui.component.rebootlistpopup.RebootListPopupMiuix
import com.lemon.mrs.ui.component.statustag.StatusTag
import com.lemon.mrs.ui.theme.LocalEnableBlur
import com.lemon.mrs.ui.theme.isInDarkTheme
import com.lemon.mrs.ui.util.BlurredBar
import com.lemon.mrs.ui.util.LayerBackdrop
import com.lemon.mrs.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp,
    scanState: ScanState,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                barColor = barColor,
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Box(modifier = Modifier) {
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
                item {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.checkUpdateEnabled) {
                            UpdateCard(state = state, actions = actions)
                        }
                        if (state.showManagerPrBuildWarning && state.showFullStatus) {
                            WarningCard(stringResource(id = R.string.home_pr_build_warning), level = WarningLevel.Notice)
                        } else if (state.showKernelPrBuildWarning && state.showFullStatus) {
                            WarningCard(stringResource(id = R.string.home_pr_kernel_warning), level = WarningLevel.Notice)
                        }
                        if (state.requiresNewKernel && state.showFullStatus) {
                            WarningCard(
                                stringResource(
                                    id = if (state.lkmMode == true) R.string.require_kernel_version else R.string.require_kernel_version_gki
                                ),
                                onClick = if (state.lkmMode == true) actions.onInstallClick else null
                            )
                        }
                        if (state.requiresNewManager) {
                            WarningCard(
                                stringResource(
                                    id = R.string.require_manager_version
                                )
                            )
                        }
                        if (state.showLkmUpdate && state.showFullStatus) {
                            WarningCard(
                                message = stringResource(R.string.home_lkm_update_available),
                                level = WarningLevel.Notice,
                                onClick = actions.onInstallClick,
                            )
                        }
                        if (state.showRootWarning) {
                            WarningCard(stringResource(id = R.string.grant_root_failed))
                        }
                        CheckEntryCard(actions = actions)
                        ScanSummarySection(scanState = scanState)
                        InfoCard(modifier = Modifier.fillMaxWidth())
                        SupportLinks(
                            onOpenUrl = actions.onOpenUrl,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                scanFindingsSection(scanState = scanState)
                item {
                    Spacer(Modifier.height(bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val newVersion = state.latestVersionInfo
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)
    val updateDialog = rememberConfirmDialog(onConfirm = { actions.onOpenUrl(newVersion.downloadUrl) })

    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        WarningCard(
            message = stringResource(id = R.string.new_version_available, newVersion.versionCode),
            level = WarningLevel.Notice,
            onClick = {
                if (newVersion.changelog.isEmpty()) {
                    actions.onOpenUrl(newVersion.downloadUrl)
                } else {
                    updateDialog.showConfirm(
                        title = title,
                        content = newVersion.changelog,
                        markdown = true,
                        confirm = updateText
                    )
                }
            }
        )
    }
}

@Composable
private fun TopBar(
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    barColor: Color,
) {
    BlurredBar(backdrop) {
        TopAppBar(
            color = barColor,
            title = stringResource(R.string.app_name),
            actions = {
                RebootListPopupMiuix()
            },
            scrollBehavior = scrollBehavior
        )
    }
}
@Composable
private fun CheckEntryCard(
    actions: HomeActions,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = actions.onInstallClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        BasicComponent(
            title = stringResource(R.string.home_click_to_check),
            summary = stringResource(R.string.home_click_to_check_summary),
            startAction = {
                Icon(
                    Icons.Rounded.Search,
                    stringResource(R.string.home_click_to_check),
                    modifier = Modifier.padding(end = 16.dp),
                    tint = colorScheme.onBackground,
                )
            }
        )
    }
}

@Composable
private fun SupportLinks(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val submitBugUrl = stringResource(R.string.home_submit_bug_url)

    Card(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_submit_bug),
            summary = stringResource(R.string.home_submit_bug_summary),
            startAction = {
                Icon(
                    imageVector = Icons.Rounded.BugReport,
                    contentDescription = stringResource(R.string.home_submit_bug),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground,
                )
            },
            onClick = { onOpenUrl(submitBugUrl) },
        )
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
) {
    @Composable
    fun InfoText(
        icon: ImageVector,
        title: String,
        content: String,
        bottomPadding: Dp = 24.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = bottomPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(24.dp),
                tint = colorScheme.onSurface,
            )
            Column {
                Text(
                    text = title,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = content,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }

    val context = LocalContext.current
    val appVersion = remember(context) { getManagerVersion(context).versionName }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.home_app_version),
                    content = appVersion,
                    bottomPadding = 0.dp,
                )
            }
        }
    }
}

// ---- 检查模块：把 ScanUi.kt 里跑出来的 ScanState 画出来 ----

/** 检测中转圈 / 失败报错 / 成功出结论卡。Idle 时什么都不画。 */
@Composable
private fun ScanSummarySection(scanState: ScanState) {
    when (scanState) {
        is ScanState.Idle -> Unit

        is ScanState.Scanning -> Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    progress = null,
                )
                Text(
                    text = stringResource(R.string.scan_running),
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        is ScanState.Failed -> WarningCard(
            message = stringResource(R.string.scan_failed) + "：" + scanState.message,
            modifier = Modifier.fillMaxWidth(),
        )

        is ScanState.Done -> ScanResultCard(scanState)
    }
}

/** 结论卡：整卡按最高等级换底色，里面是结论大字 + 模块信息 + 四级计数 + 提示。 */
@Composable
private fun ScanResultCard(done: ScanState.Done) {
    val report = done.report
    val container = when {
        report.high > 0 -> severityContainerColor("high")
        report.medium > 0 -> severityContainerColor("medium")
        else -> colorScheme.surfaceContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(
            color = container,
            contentColor = colorScheme.onSurfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = report.verdict,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = report.moduleName.ifBlank { report.moduleId },
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
            val meta = listOfNotNull(
                report.version.takeIf { it.isNotBlank() },
                report.author.takeIf { it.isNotBlank() }
                    ?.let { stringResource(R.string.scan_author) + "：" + it },
                stringResource(R.string.scan_files) + "：" + report.fileCount,
            ).joinToString(" · ")
            Text(
                text = meta,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = colorScheme.onSurfaceVariantSummary,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                CountTag(stringResource(R.string.scan_severity_high), report.high, "high")
                CountTag(stringResource(R.string.scan_severity_medium), report.medium, "medium")
                CountTag(stringResource(R.string.scan_severity_low), report.low, "low")
                CountTag(stringResource(R.string.scan_severity_info), report.info, "info")
            }
            if (report.findings.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.scan_findings_count, report.findings.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (report.truncated) {
                Text(
                    text = stringResource(R.string.scan_truncated),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            report.notes.forEach { note ->
                Text(
                    text = note,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 一条发现：等级小标签 + 规则名 + 文件:行 + 说明。 */
@Composable
private fun ScanFindingCard(finding: Finding) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusTag(
                    label = finding.severityLabel(),
                    backgroundColor = severityContainerColor(finding.severity),
                    contentColor = severityAccentColor(finding.severity),
                )
                Text(
                    text = finding.rule,
                    fontSize = MiuixTheme.textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (finding.file.isNotBlank()) {
                Text(
                    text = if (finding.line > 0) finding.file + "：" + finding.line else finding.file,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (finding.detail.isNotBlank()) {
                Text(
                    text = finding.detail,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 发现逐条一卡；走 LazyColumn 的独立 item，几百条也不会把首屏撑爆。 */
private fun LazyListScope.scanFindingsSection(scanState: ScanState) {
    val findings = (scanState as? ScanState.Done)?.report?.findings.orEmpty()
    itemsIndexed(findings) { _, finding ->
        ScanFindingCard(finding)
    }
}

/** 计数小标签：底色与前景色都按等级走，跟结论卡、发现卡同一套。 */
@Composable
private fun CountTag(label: String, count: Int, severity: String) {
    StatusTag(
        label = if (count > 0) label + " " + count else label,
        backgroundColor = severityContainerColor(severity),
        contentColor = severityAccentColor(severity),
    )
}

/** 等级底色：高危 / 中危与 WarningCard 那两套一致，低危另给一档蓝，信息用卡片底色。 */
@Composable
private fun severityContainerColor(severity: String): Color {
    val dark = isInDarkTheme()
    return when (severity) {
        "high" -> if (dark) Color(0xFF310808) else Color(0xFFF8E2E2)
        "medium" -> if (dark) Color(0xFF3E2F1B) else Color(0xFFFFF0DB)
        "low" -> if (dark) Color(0xFF152238) else Color(0xFFE3ECFB)
        else -> colorScheme.surfaceContainer
    }
}

/** 等级前景色：与上面那几档底色配对，保证对比度。 */
@Composable
private fun severityAccentColor(severity: String): Color = when (severity) {
    "high" -> Color(0xFFF72727)
    "medium" -> Color(0xFFF5A623)
    "low" -> Color(0xFF2A6BE0)
    else -> colorScheme.onSurfaceVariantSummary
}

/** 核心给的 severity 是英文小写（high / medium / low / info），这里翻成给人看的词。 */
@Composable
private fun Finding.severityLabel(): String = when (severity) {
    "high" -> stringResource(R.string.scan_severity_high)
    "medium" -> stringResource(R.string.scan_severity_medium)
    "low" -> stringResource(R.string.scan_severity_low)
    else -> stringResource(R.string.scan_severity_info)
}