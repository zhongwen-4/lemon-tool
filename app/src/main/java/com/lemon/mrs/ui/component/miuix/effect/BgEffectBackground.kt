// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/BgEffectBackground.kt
// 改动：上游在 isRuntimeShaderSupported() 为真时用一整条 RuntimeShader 管线（BgEffectPainter /
//       BgEffectConfig / BgEffectModifier，共 5 个文件）画 OS3 动态背景，运行时不可用时直接退化成
//       一个纯 Box。本项目 minSdk 24、且不搬毛玻璃/着色器那一路，所以只保留那个「退化」分支：
//       铺一层主题 surface 色，参数与上游完全一致，调用点不用改。
// 改动日期：2026-09-28
package com.lemon.mrs.ui.component.miuix.effect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
@Suppress("UNUSED_PARAMETER")
fun BgEffectBackground(
    dynamicBackground: Boolean,
    modifier: Modifier = Modifier,
    bgModifier: Modifier = Modifier,
    isFullSize: Boolean = false,
    effectBackground: Boolean = true,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .background(MiuixTheme.colorScheme.surface.copy(alpha = alpha().coerceIn(0f, 1f))),
        )
        content()
    }
}