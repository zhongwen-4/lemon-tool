// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/liquid/Vibrancy.kt
// 改动清单：docs/sukisu-port-changes.md#vibrancy

// Adapted from Kyant0/AndroidLiquidGlass — https://github.com/Kyant0/AndroidLiquidGlass (Apache 2.0).
// Mirrored from compose-miuix-ui example.

package com.lemon.mrs.ui.component.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

fun BackdropEffectScope.vibrancy() {
    colorControls(
        brightness = 0f,
        contrast = 1f,
        saturation = 1.5f,
    )
}
