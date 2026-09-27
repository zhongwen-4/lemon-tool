// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/settings/SettingsMiuix.kt
// 改动（改动日期：2026-09-28）：
//   ① 上游那一整页 root 管理器的开关与子页面（SuSFS / KPM / LKM / ADB root / su compat / 卸载内核模块 /
//      主题 / Profile 模板 / 工具 / 界面模式 / 语言）全部删掉：本项目只检查模块包，没有这些数据源。
//      只留两行——「检查更新」（SwitchPreference）与「关于」（ArrowPreference）。
//   ② 「检查更新」的开关状态本页自持（rememberSaveable，默认开）。用户要求数据先空着，
//      等接上真实的自动检查逻辑，再把这行换成本页外的状态。
//   ③ 顶栏 + 一条 LazyColumn 的外壳与上游一致；毛玻璃仍是同签名占位（ui/util/BlurExt.kt，恒退化纯色）。
package com.lemon.mrs.ui.screen.settings

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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lemon.mrs.R
import com.lemon.mrs.ui.theme.LocalEnableBlur
import com.lemon.mrs.ui.util.BlurredBar
import com.lemon.mrs.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 设置页：按用户要求只留「检查更新」与「关于」两行，其余 root 管理器相关的行全部删掉。 */
@Composable
fun SettingPagerMiuix(
    onOpenAbout: () -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    var checkUpdateEnabled by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            BlurredBar(rememberBlurBackdrop(LocalEnableBlur.current)) {
                TopAppBar(
                    title = stringResource(R.string.settings),
                    scrollBehavior = scrollBehavior
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
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
            item {
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_check_update),
                        summary = stringResource(id = R.string.settings_check_update_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(id = R.string.settings_check_update),
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = checkUpdateEnabled,
                        onCheckedChange = { checkUpdateEnabled = it }
                    )
                    ArrowPreference(
                        title = stringResource(id = R.string.about),
                        startAction = {
                            Icon(
                                Icons.Rounded.Info,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(id = R.string.about),
                                tint = colorScheme.onBackground
                            )
                        },
                        onClick = onOpenAbout
                    )
                }
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}
