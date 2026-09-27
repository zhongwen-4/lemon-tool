// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/miuix/SendLogDialog.kt
// 改动（改动日期：2026-09-28）：上游这里要收集日志文件、弹列表让你选发给谁，本项目还没有这套东西，
//   只保留同名同签名的空壳，设置页的调用点与上游一致。
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