// 移植自 SukiSU Ultra（GPL-3.0）：上游 manager/app/src/main/java/com/sukisu/ultra/ui/util/LocaleHelper.kt
// 改动（改动日期：2026-09-28）：
//   ① 只留设置页用到的那三个东西：SYSTEM / SUPPORTED_TAGS / displayName。
//      上游的 persistLanguage / loadLanguage / applyLanguage / 重启 Activity 那一套不搬。
//   ② SUPPORTED_TAGS 先给空表：本项目现在只有默认资源 + 简体中文，还没做语言切换，
//      所以设置页「语言」那行下拉是空的（数据先空着，往后接）。
package com.lemon.mrs.ui.util

object LocaleHelper {
    /** 「跟随系统」在语言标签列表里用空串表示。 */
    const val SYSTEM = ""

    val SUPPORTED_TAGS: List<String> = emptyList()

    fun displayName(tag: String): String = tag
}