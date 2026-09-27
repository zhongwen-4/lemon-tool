// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/statustag/StatusTag.kt
// 改动：上游按 LocalUiMode 在 Miuix / Material 两套间切换；本项目只做 Miuix 一套，直接调 StatusTagMiuix。
// 改动日期：2026-09-28
package com.lemon.mrs.ui.component.statustag

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun StatusTag(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color
) {
    StatusTagMiuix(label, modifier, backgroundColor, contentColor)
}