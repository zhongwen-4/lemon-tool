// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/settings/SettingsMiuix.kt
// 改动（改动日期：2026-09-28）：
//   ① 上游那一整页 root 管理器的开关与子页面（SuSFS / KPM / LKM / ADB root / su compat / 卸载内核模块 /
//      主题 / Profile 模板 / 工具 / 界面模式 / 语言）全部删掉：本项目只检查模块包，没有这些数据源。
//      只留两行——「检查更新」（SwitchPreference）与「关于」（ArrowPreference）。
//   ② 「检查更新」的开关状态本页自持（rememberSaveable，默认开）。用户要求数据先空着，
//      等接上真实的自动检查逻辑，再把这行换成本页外的状态。
//   ③ 顶栏 + 一条 LazyColumn 的外壳与上游一致；毛玻璃已接上（ui/util/BlurExt.kt 换成上游原文）。
//   ④ 2026-09-28 用户要「磨砂玻璃 / 液态玻璃」两个效果，补两行 SwitchPreference（上游同名开关），
//      状态放进 DisplaySettings（SharedPreferences），主壳读它决定顶栏毛玻璃与底栏液态玻璃。
//   ⑤ 2026-09-29「预测性返回手势」开关（上游同名行），2026-09-30 按用户口径改成**应用级**：
//      翻动时除了存开关，还要照上游 `ColorPaletteScreen` 那套翻平台的预测性返回标志并 `recreate()`，
//      这样它管的是所有预测性返回手势，不只是关于页那一处。
package com.lemon.mrs.ui.screen.settings

import android.app.Activity
import android.os.Build
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
import androidx.compose.material.icons.automirrored.rounded.MenuOpen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lemon.mrs.MrsApplication
import com.lemon.mrs.R
import com.lemon.mrs.ui.theme.LocalEnableBlur
import com.lemon.mrs.ui.util.BlurredBar
import com.lemon.mrs.ui.util.DisplaySettings
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
    display: DisplaySettings,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    val context = LocalContext.current
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
                        title = stringResource(id = R.string.settings_enable_blur),
                        summary = stringResource(id = R.string.settings_enable_blur_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.BlurOn,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(id = R.string.settings_enable_blur),
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = display.enableBlur,
                        onCheckedChange = { display.updateEnableBlur(it) }
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_enable_glass),
                        summary = stringResource(id = R.string.settings_enable_glass_summary),
                        startAction = {
                            Icon(
                                Icons.Rounded.Opacity,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(id = R.string.settings_enable_glass),
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = display.enableFloatingBottomBar,
                        onCheckedChange = { display.updateEnableFloatingBottomBar(it) }
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.settings_enable_predictive_back),
                        summary = stringResource(id = R.string.settings_enable_predictive_back_summary),
                        startAction = {
                            Icon(
                                Icons.AutoMirrored.Rounded.MenuOpen,
                                modifier = Modifier.padding(end = 6.dp),
                                contentDescription = stringResource(id = R.string.settings_enable_predictive_back),
                                tint = colorScheme.onBackground
                            )
                        },
                        checked = display.enablePredictiveBack,
                        onCheckedChange = { value ->
                            display.updateEnablePredictiveBack(value)
                            // 上游 ColorPaletteScreen 同款：翻平台的预测性返回标志 + 重建 Activity，
                            // 让**所有**预测性返回手势（系统动画 + app 内返回进度）跟着这个开关走。
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                MrsApplication.setEnableOnBackInvokedCallback(context.applicationInfo, value)
                                (context as? Activity)?.recreate()
                            }
                        }
                    )
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
