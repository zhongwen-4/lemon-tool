// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/KsuValidCheck.kt
// 改动（改动日期：2026-09-28）：上游这里要问内核（Natives.isManager / Natives.version）才决定是否渲染，
//   本项目没有这套内核取数，所以直接渲染 content。留着同名同签名，设置页的调用点就能跟上游一字不差。
package com.lemon.mrs.ui.component

import androidx.compose.runtime.Composable

@Composable
fun KsuIsValid(
    content: @Composable () -> Unit,
) {
    content()
}