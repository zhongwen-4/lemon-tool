// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/home/HomeMiuix.kt
// 改动清单：docs/sukisu-port-changes.md#homemiuix
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.CheckCircleOutline
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
import com.lemon.mrs.R
import com.lemon.mrs.ScanState
import com.lemon.mrs.ScanReport
import com.lemon.mrs.ui.component.WarningLevel
import com.lemon.mrs.ui.component.dialog.rememberConfirmDialog
import com.lemon.mrs.ui.component.miuix.WarningCard
import com.lemon.mrs.ui.component.rebootlistpopup.RebootListPopupMiuix
import com.lemon.mrs.ui.theme.LocalEnableBlur
import com.lemon.mrs.ui.theme.isInDarkTheme
import com.lemon.mrs.ui.util.BlurredBar
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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
                        CheckCard(scanState = scanState, actions = actions)
                        InfoCard(modifier = Modifier.fillMaxWidth())
                        SupportLinks(
                            onOpenUrl = actions.onOpenUrl,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
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

/**
 * 检查卡：主页上唯一的检查入口，长相跟着 [scanState] 走。
 *
 * 待机 = 上游那张「点此开始检测」行卡；检测中 = 转圈 + 文案；
 * 出结果 = 照 SukiSU 主页「工作中」那张卡的版式，整卡按最高风险换成红 / 黄 / 绿 ——
 * 左上是大字结论加一行计数，左下角是模块名，右下角一枚被卡片裁掉一角的 110dp 大图标。
 * **结果明细不在这里**（用户口径：结果全部进「检查历史」），主页只留这张结论卡；
 * 点它一律是「重新选包检测」。
 */
@Composable
private fun CheckCard(
    scanState: ScanState,
    actions: HomeActions,
) {
    when (scanState) {
        is ScanState.Idle -> CheckEntryCard(actions = actions)

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

        is ScanState.Done -> ScanResultStatusCard(
            report = scanState.report,
            onClick = actions.onInstallClick,
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

/** 结论卡：整卡按最高风险着色（高危红 / 中危黄 / 其余绿），版式与上游「工作中」那张卡一致。 */
@Composable
private fun ScanResultStatusCard(
    report: ScanReport,
    onClick: () -> Unit,
) {
    val dark = isInDarkTheme()
    // 卡片按「最高那一档」定色、定标题：高危 -> 中危 -> 低危（只有低危 / 信息也算低危），
    // 一档发现都没有时也归到最低那档；「未发现风险项」那句只留给下面那行计数用。
    val level = when {
        report.high > 0 -> "high"
        report.medium > 0 -> "medium"
        report.low > 0 || report.info > 0 -> "low"
        else -> "none"
    }
    val container = when (level) {
        "high" -> if (dark) Color(0xFF310808) else Color(0xFFF8E2E2)
        "medium" -> if (dark) Color(0xFF3E2F1B) else Color(0xFFFFF0DB)
        else -> if (dark) Color(0xFF1A3825) else Color(0xFFDFFAE4)
    }
    val accent = when (level) {
        "high" -> Color(0xFFF72727)
        "medium" -> Color(0xFFF5A623)
        else -> Color(0xFF36D167)
    }
    val icon = when (level) {
        "high" -> Icons.Rounded.ErrorOutline
        "medium" -> Icons.Rounded.Warning
        else -> Icons.Rounded.CheckCircleOutline
    }
    // 标题只写「高危模块 / 中危模块 / 低危模块」三档（用户 2026-09-29 定）。
    val title = when (level) {
        "high" -> stringResource(R.string.scan_result_high)
        "medium" -> stringResource(R.string.scan_result_medium)
        else -> stringResource(R.string.scan_result_low)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.defaultColors(color = container),
        onClick = onClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Box {
            // 装饰层：右下角那枚「溢出、被卡片裁掉一角」的大图标。
            // 这里必须用 matchParentSize()、不能用 fillMaxSize() —— 后者会让这一层也参与定尺寸，
            // 三块内容于是被塞进同一格、文字互相压住（0.11.0 那个观感 bug 就是这么来的）。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(27.dp, 31.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Icon(
                    modifier = Modifier.size(110.dp),
                    imageVector = icon,
                    tint = accent,
                    contentDescription = null,
                )
            }
            // 内容层自己定卡片高度：大字等级 -> 计数 -> 模块名，靠 Spacer 拉开，不会重叠。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = severityCountsText(report),
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = report.moduleName.ifBlank { report.moduleId },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 给右下角那枚大图标留出位置：它可见的那一角从右边起约 83dp 宽。
                    modifier = Modifier.padding(end = 96.dp),
                )
            }
        }
    }
}

/** 「高危 n · 中危 n …」：只列非零的那几档；一档都没有就说「未发现风险项」。 */
@Composable
private fun severityCountsText(report: ScanReport): String {
    val parts = buildList {
        if (report.high > 0) add(stringResource(R.string.scan_severity_high) + " " + report.high)
        if (report.medium > 0) add(stringResource(R.string.scan_severity_medium) + " " + report.medium)
        if (report.low > 0) add(stringResource(R.string.scan_severity_low) + " " + report.low)
        if (report.info > 0) add(stringResource(R.string.scan_severity_info) + " " + report.info)
    }
    return if (parts.isEmpty()) stringResource(R.string.scan_no_findings) else parts.joinToString(" · ")
}