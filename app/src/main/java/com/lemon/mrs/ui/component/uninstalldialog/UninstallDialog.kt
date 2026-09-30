// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/component/uninstalldialog/UninstallDialog.kt
// 改动清单：docs/sukisu-port-changes.md#uninstalldialog
package com.lemon.mrs.ui.component.uninstalldialog

import androidx.compose.runtime.Composable

@Composable
@Suppress("UNUSED_PARAMETER")
fun UninstallDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
) = Unit