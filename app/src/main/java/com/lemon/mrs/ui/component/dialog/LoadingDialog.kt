// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/dialog/Dialog.kt
// 改动清单：docs/sukisu-port-changes.md#loadingdialog
package com.lemon.mrs.ui.component.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

interface LoadingDialogHandle {
    suspend fun <R> withLoading(block: suspend () -> R): R
    fun showLoading()
}

@Composable
fun rememberLoadingDialog(): LoadingDialogHandle = remember {
    object : LoadingDialogHandle {
        override suspend fun <R> withLoading(block: suspend () -> R): R = block()
        override fun showLoading() = Unit
    }
}