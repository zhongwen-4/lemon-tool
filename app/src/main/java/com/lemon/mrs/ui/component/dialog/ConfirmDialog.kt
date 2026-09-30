// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/dialog/Dialog.kt
// 改动清单：docs/sukisu-port-changes.md#confirmdialog
package com.lemon.mrs.ui.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class ConfirmDialogState(private val onConfirm: () -> Unit) {
    @Suppress("UNUSED_PARAMETER")
    fun showConfirm(
        title: String,
        content: String,
        markdown: Boolean = false,
        confirm: String? = null,
    ) {
        onConfirm()
    }
}

@Composable
fun rememberConfirmDialog(onConfirm: () -> Unit = {}): ConfirmDialogState =
    remember(onConfirm) { ConfirmDialogState(onConfirm) }