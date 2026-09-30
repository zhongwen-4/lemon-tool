// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/SendLogDialog.kt
// 改动清单：docs/sukisu-port-changes.md#sendlogdialog
package com.lemon.mrs.ui.component.miuix

import androidx.compose.runtime.Composable
import com.lemon.mrs.ui.component.dialog.LoadingDialogHandle

@Composable
@Suppress("UNUSED_PARAMETER")
fun SendLogDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    loadingDialog: LoadingDialogHandle,
) = Unit