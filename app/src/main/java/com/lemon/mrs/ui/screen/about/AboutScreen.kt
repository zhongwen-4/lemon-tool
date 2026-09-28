// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutScreen.kt
// 改动（改动日期：2026-09-28）：
//   ① 上游用 navigation3 的 Navigator.push/pop 进这条路由；本项目没有那套导航，改成上层传 onBack。
//   ② 上游的 BuildConfig.VERSION_NAME、R.string.about_source_code 文案（含它自己的图标许可说明）
//      换成我们的：版本号从 PackageManager 读，链接指向本仓库。
//   ③ 其余（state/actions 的构造方式、extractLinks 的用法）与上游一致。
//   ④ 补 `BackHandler`：上游的「返回」由 navigation3 的路由栈接管，本项目没有导航库，系统返回键
//      得自己接——不接的话在关于页按返回会直接退出 App（2026-09-28 修）。
package com.lemon.mrs.ui.screen.about

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.lemon.mrs.R

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val htmlString = stringResource(
        id = R.string.about_source_code,
        "<b><a href=\"https://github.com/zhongwen-4/lemon-tool\">GitHub</a></b>",
        "<b><a href=\"https://github.com/zhongwen-4/lemon-tool/releases\">Releases</a></b>",
    )
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = appVersion(context),
        links = extractLinks(htmlString),
    )
    val actions = AboutScreenActions(
        onBack = onBack,
        onOpenLink = { url -> runCatching { uriHandler.openUri(url) } },
    )

    // 关于页开着时，系统返回键先关它（回主页），别让它把 App 退掉。
    BackHandler(enabled = true) { onBack() }

    AboutScreenMiuix(state, actions)
}

private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"