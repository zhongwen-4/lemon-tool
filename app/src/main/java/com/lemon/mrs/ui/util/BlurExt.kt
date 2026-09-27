// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/BlurExt.kt
// 改动：上游这两个东西是靠 miuix-blur 的 LayerBackdrop / textureBlur 实现的，而 miuix-blur 的
//       manifest 硬要求 minSdk 33（本项目 minSdk 24），且上游的毛玻璃路径本项目不要。
//       这里保留同名同签名的占位实现：拿不到 backdrop（恒为 null），BlurredBar 退化成纯色顶栏。
//       页面调用点因此可以跟上游保持一字不差。
// 改动日期：2026-09-28
package com.lemon.mrs.ui.util

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 上游这个类型来自 miuix-blur；本项目不搬毛玻璃，用空类占位。 */
class LayerBackdrop

@Composable
@Suppress("UNUSED_PARAMETER")
fun rememberBlurBackdrop(enableBlur: Boolean): LayerBackdrop? = null

@Composable
@Suppress("UNUSED_PARAMETER")
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurActive: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier) {
        content()
    }
}