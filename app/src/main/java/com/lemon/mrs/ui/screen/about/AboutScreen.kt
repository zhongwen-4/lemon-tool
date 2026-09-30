// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/screen/about/AboutScreen.kt
// 改动清单：docs/sukisu-port-changes.md#aboutscreen
package com.lemon.mrs.ui.screen.about

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import com.lemon.mrs.R
import kotlinx.coroutines.CancellationException

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    enablePredictiveBack: Boolean = true,
    onBackProgress: (Float) -> Unit = {},
) {
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

    // 关于页开着时，系统返回手势先关它（回上一页），别让它把 App 退掉。
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
        // 普通返回：先把进度清零 —— 开关是在关于页开着的时候才可能被翻动的，
        // 不清零的话页面会留在「跟手滑到一半」的位置上。
        LaunchedEffect(Unit) { onBackProgress(0f) }
        BackHandler { onBack() }
    }

    AboutScreenMiuix(state, actions)
}

private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"