// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/statustag/StatusTag.kt
// 改动清单：docs/sukisu-port-changes.md#statustag
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