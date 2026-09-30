// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/WindowSize.kt
// 改动清单：docs/sukisu-port-changes.md#windowsize
package com.lemon.mrs.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
fun shouldShowSplitPane(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val deviceDensity = LocalResources.current.displayMetrics.density
    val widthDp = windowInfo.containerSize.width / deviceDensity
    val heightDp = windowInfo.containerSize.height / deviceDensity
    return widthDp >= 840f || (widthDp >= 600f && heightDp / widthDp < 1.2f)
}
