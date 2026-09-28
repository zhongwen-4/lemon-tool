// Mirrored from compose-miuix-ui example.
//
// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/effect/BgEffectBackground.kt
// 改动（改动日期：2026-09-28）：
//   ① 只改包名；RuntimeShader 从 `top.yukonga.miuix.kmp.shader` 取（上游走 miuix-blur，本项目没这个坐标，
//      理由见 BgEffectPainter.kt 的文件头）。
//   ② `isInDarkTheme()` / `shouldShowSplitPane()` 换成本项目的实现（同名同义，上游那两个在
//      `ui/theme/Theme.kt` 与 `ui/util/WindowSize.kt`）。
//   ③ 运行时不支持 RuntimeShader（SDK < 33）时，上游是**直接退化成纯 Box**什么都不画；
//      2026-09-28 改为补一层静态渐变（同一套 BgEffectConfig 调色板），低版本也看得到背景。
package com.lemon.mrs.ui.component.miuix.effect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.lemon.mrs.ui.theme.isInDarkTheme
import com.lemon.mrs.ui.util.shouldShowSplitPane
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.floor
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BgEffectBackground(
    dynamicBackground: Boolean,
    modifier: Modifier = Modifier,
    bgModifier: Modifier = Modifier,
    isFullSize: Boolean = false,
    effectBackground: Boolean = true,
    alpha: () -> Float = { 1f },
    content: @Composable BoxScope.() -> Unit,
) {
    // 设备没有 AGSL（API < 33）：上游这里直接退化成纯 Box、什么都不画，底色交给平台窗口。
    // 用户 2026-09-28 反馈「关于页的背景也没实现」，这里补一层静态渐变当压阵——同一套调色板
    // （BgEffectConfig 的 colors1），只是不会动；有 RuntimeShader 的机器仍走下面的动态那一路。
    if (!isRuntimeShaderSupported()) {
        val fallbackDeviceType = if (shouldShowSplitPane()) DeviceType.PAD else DeviceType.PHONE
        val fallbackDark = isInDarkTheme()
        val fallbackColors = remember(fallbackDeviceType, fallbackDark) {
            val raw = BgEffectConfig.get(fallbackDeviceType, fallbackDark).colors1
            (0 until raw.size / 4).map { index ->
                Color(raw[index * 4], raw[index * 4 + 1], raw[index * 4 + 2], raw[index * 4 + 3])
            }
        }
        Box(
            modifier = modifier
                .background(MiuixTheme.colorScheme.surface)
                .then(bgModifier)
                .background(Brush.verticalGradient(colors = fallbackColors)),
            content = content,
        )
        return
    }
    Box(
        modifier = modifier,
    ) {
        val surface = MiuixTheme.colorScheme.surface
        val deviceType = if (shouldShowSplitPane()) DeviceType.PAD else DeviceType.PHONE
        val isDarkTheme = isInDarkTheme()
        val painter = remember { BgEffectPainter() }

        val preset = remember(deviceType, isDarkTheme) {
            BgEffectConfig.get(deviceType, isDarkTheme)
        }

        val colorStage = remember { Animatable(0f) }

        LaunchedEffect(dynamicBackground, preset) {
            if (!dynamicBackground) return@LaunchedEffect
            val animatesColors = preset.colors1 !== preset.colors2 || preset.colors2 !== preset.colors3
            if (!animatesColors) return@LaunchedEffect

            var targetStage = floor(colorStage.value) + 1f
            while (isActive) {
                delay((preset.colorInterpPeriod * 500).toLong().milliseconds)
                colorStage.animateTo(
                    targetValue = targetStage,
                    animationSpec = spring(dampingRatio = 0.9f, stiffness = 35f),
                )
                targetStage += 1f
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .bgEffectDraw(
                    painter = painter,
                    preset = preset,
                    deviceType = deviceType,
                    isDarkTheme = isDarkTheme,
                    surface = surface,
                    effectBackground = effectBackground,
                    isFullSize = isFullSize,
                    playing = dynamicBackground,
                    colorStage = { colorStage.value },
                    alpha = alpha,
                ),
        )
        content()
    }
}
