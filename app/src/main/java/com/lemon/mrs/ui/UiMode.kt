// 本项目自己的文件（不是移植件）：本项目只做 MiuiX 一套界面。
// SukiSU 上游用 UiMode 在 Miuix / Material 两套界面间切换，它的设置页「界面模式」下拉就是列它。
// 这里保留同名同形的枚举，但只有 Miuix 一项，所以那行下拉仍然成立、只是没得选。
// 建立日期：2026-09-28
package com.lemon.mrs.ui

enum class UiMode(val value: String) {
    Miuix("miuix"),
    ;

    companion object {
        const val DEFAULT_VALUE = "miuix"
    }
}