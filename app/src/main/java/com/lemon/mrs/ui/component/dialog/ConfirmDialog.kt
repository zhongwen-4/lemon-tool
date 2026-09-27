// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/dialog/Dialog.kt
// 改动：上游的 rememberConfirmDialog 是一整套带 markdown 渲染的确认弹窗（Dialog.kt + DialogMiuix.kt
//       + MarkdownContent，共约 17 KB）。本次只照搬页面骨架、数据先空着，所以这里先给一个同签名的
//       空实现：调用点（HomeMiuix 的 UpdateCard）保持与上游一致，弹窗以后接。
// 改动日期：2026-09-28
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