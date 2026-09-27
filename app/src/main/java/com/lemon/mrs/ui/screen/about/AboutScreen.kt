// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutScreen.kt
// 改动（改动日期：2026-09-28）：
//   ① 上游用 navigation3 的 Navigator.push/pop 进这条路由；本项目没有那套导航，改成上层传 onBack。
//   ② 上游的 BuildConfig.VERSION_NAME、R.string.about_source_code 文案（含它自己的图标许可说明）
//      换成我们的：版本号从 PackageManager 读，链接指向本仓库。
//   ③ 其余（state/actions 的构造方式、extractLinks 的用法）与上游一致。
package com.lemon.mrs.ui.screen.about

import android.content.Context
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

    AboutScreenMiuix(state, actions)
}

private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"