// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/KsuValidCheck.kt
// 改动清单：docs/sukisu-port-changes.md#ksuisvalid
package com.lemon.mrs.ui.component

import androidx.compose.runtime.Composable

@Composable
fun KsuIsValid(
    content: @Composable () -> Unit,
) {
    content()
}