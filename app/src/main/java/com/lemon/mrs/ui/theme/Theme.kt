// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/theme/Theme.kt
// 改动：只取 LocalEnableBlur 这一个 CompositionLocal（上游还带 LocalColorMode / isInDarkTheme /
//       底栏徽标等开关，本项目用不上）；本项目不搬毛玻璃那一路，所以它恒为 false，
//       调用点写法与上游保持一致。
// 改动日期：2026-09-28
package com.lemon.mrs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalEnableBlur = staticCompositionLocalOf { false }
@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean = isSystemInDarkTheme()