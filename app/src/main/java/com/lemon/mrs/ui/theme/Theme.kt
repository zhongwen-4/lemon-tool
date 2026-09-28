// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/theme/Theme.kt
// 改动（改动日期：2026-09-28）：只取本项目真用得到的三个 CompositionLocal——LocalEnableBlur /
//   LocalEnableFloatingBottomBar / LocalEnableFloatingBottomBarBlur（上游还带 LocalColorMode、
//   LocalEnableNavigationBadge、LocalModuleDescriptionMaxLines 等，本项目没有那些开关）；
//   isInDarkTheme 仍按「跟随系统」实现（上游读它自己的 LocalColorMode）。
package com.lemon.mrs.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** 顶栏 / 底栏的毛玻璃（上游 settings_enable_blur）。 */
val LocalEnableBlur = staticCompositionLocalOf { false }

/** 悬浮底栏（液态玻璃）——上游 settings_enable_glass 那一支。 */
val LocalEnableFloatingBottomBar = staticCompositionLocalOf { false }

/** 悬浮底栏底下那层是否走毛玻璃（上游 enableFloatingBottomBarBlur）。 */
val LocalEnableFloatingBottomBarBlur = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean = isSystemInDarkTheme()