// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/dialog/Dialog.kt
// 改动（改动日期：2026-09-28）：上游的 loading 弹窗是整套 Dialog 框架（DialogHandleBase / OverlayDialog 等）里的一环，
//   本项目暂时只搬骨架、不搬弹窗框架。这里保留同名的句柄类型与 rememberLoadingDialog，
//   句柄照常能 withLoading / showLoading，只是当前不弹任何东西；往后接真弹窗时换掉实现即可。
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