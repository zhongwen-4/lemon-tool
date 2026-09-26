/*
 * 设置页/信息页的「一行一项」。底座是 MiuiX 自带的 BasicComponent 与 ArrowPreference ——
 * SukiSU Ultra 的设置页也是这么写的（manager/app/src/main/java/com/sukisu/ultra/ui/screen/settings/SettingsMiuix.kt），
 * 这里只是把它重复的排版收成两个函数，没有改动它任何行为。
 *
 * 改动日期：2026-09-27。
 */

package com.lemon.mrs.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 行的开头图标：24dp，跟正文同色（与 SukiSU 的 InfoText 一致）。 */
@Composable
private fun RowIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .padding(end = 12.dp)
            .size(24.dp),
        tint = MiuixTheme.colorScheme.onSurface,
    )
}

/** 尾部的小字（版本号、路径、风险尾值这类）。 */
@Composable
private fun RowTail(text: String, color: Color? = null) {
    Text(
        text = text,
        fontSize = MiuixTheme.textStyles.footnote1.fontSize,
        color = color ?: MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

/** 只读的信息行：图标 + 标题 + 副标题 +（可选）尾部值，没有箭头。 */
@Composable
fun InfoRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    tail: String? = null,
    tailColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    BasicComponent(
        modifier = modifier,
        title = title,
        summary = summary,
        startAction = { RowIcon(icon) },
        endActions = {
            if (tail != null) RowTail(tail, tailColor)
        },
    )
}

/** 可点的行：图标 + 标题 + 副标题 +（可选）尾部值，尾部带箭头。 */
@Composable
fun ActionRow(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    tail: String? = null,
    tailColor: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        startAction = { RowIcon(icon) },
        endActions = {
            if (tail != null) RowTail(tail, tailColor)
        },
        enabled = enabled,
        onClick = onClick,
    )
}
