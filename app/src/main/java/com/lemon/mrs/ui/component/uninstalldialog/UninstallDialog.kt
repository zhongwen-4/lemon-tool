// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/uninstalldialog/UninstallDialog.kt
// 改动（改动日期：2026-09-28）：上游这个是 LKM 模式下「卸载内核模块」的确认弹窗，本项目没有这个功能，
//   只保留同名同签名的空壳，设置页的调用点与上游一致。
package com.lemon.mrs.ui.component.uninstalldialog

import androidx.compose.runtime.Composable

@Composable
@Suppress("UNUSED_PARAMETER")
fun UninstallDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
) = Unit